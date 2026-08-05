import { ref, shallowRef, computed } from 'vue'

/**
 * DAP 调试客户端 (P8)
 *
 * 后端只是透明管道,协议逻辑全在这里:seq 自增、请求/响应配对、事件分发。
 * 复用 useFnRun 的那条 WebSocket —— 运行与调试共用一个会话(文档「单会话单连接」)。
 *
 * 支持的调试动作:启动/停止、设置断点、继续、逐过程、单步进入、单步跳出、
 * 调用栈、作用域与变量、表达式求值。
 */
export function useFnDebug(run) {
  const debugging = ref(false)      // 会话是否存在
  const paused = ref(false)         // 是否停在断点/单步位置
  const stoppedReason = ref('')
  const threadId = ref(null)
  const frames = ref([])            // 调用栈
  const activeFrameId = ref(null)
  const scopes = ref([])            // [{ name, variablesReference, expensive }]
  const variables = ref([])         // 当前作用域展开的变量
  const watches = ref([])           // [{ expression, value, error }]
  const capabilities = shallowRef({})
  const lastError = ref('')

  let seq = 0
  let repoRoot = ''                 // 代码仓工作区绝对路径, 用来把相对路径拼成调试器认的绝对路径
  function setRepoRoot(p) { repoRoot = p || '' }
  const pending = new Map()

  /**
   * 断点:path → [{ line, condition, hitCondition, logMessage, enabled }]
   * 语言无关, 按文件维护。三种高级断点都落在同一个结构上:
   *   condition   条件断点 —— 表达式为真才停
   *   hitCondition 命中次数 —— 如 ">5" / "==3"
   *   logMessage  日志点 —— 不停下来, 只往调试控制台打一行(DAP 的 logpoint)
   */
  const breakpoints = ref({})
  /** 后端确认过的断点校验状态:`${path}:${line}` → verified */
  const verified = ref({})

  const currentLine = computed(() => {
    const f = frames.value.find(x => x.id === activeFrameId.value) || frames.value[0]
    return f ? { path: normalize(f.source?.path || ''), line: f.line } : null
  })

  /* —— 请求/响应 —— */
  function request(command, args) {
    return new Promise((resolve) => {
      const s = ++seq
      const msg = { seq: s, type: 'request', command }
      if (args !== undefined) msg.arguments = args
      pending.set(s, resolve)
      run.send('dap', { message: msg })
      // 调试器无响应时不能让 Promise 永远挂着
      setTimeout(() => {
        if (pending.has(s)) { pending.delete(s); resolve({ success: false, body: { error: 'timeout' } }) }
      }, 15000)
    })
  }

  /** 接后端转发来的 DAP 报文 */
  function onDap(body) {
    const m = body?.message
    if (!m) return
    if (m.type === 'response') {
      const fn = pending.get(m.request_seq)
      if (fn) { pending.delete(m.request_seq); fn(m) }
      if (m.success === false && m.message) lastError.value = m.message
      return
    }
    if (m.type === 'event') onDapEvent(m)
  }

  async function onDapEvent(m) {
    switch (m.event) {
      case 'initialized':
        // 时序按 DAP 规范:initialized 之后才下发断点, 最后 configurationDone
        await syncAllBreakpoints()
        await request('configurationDone')
        break
      case 'stopped': {
        paused.value = true
        stoppedReason.value = m.body?.reason || ''
        threadId.value = m.body?.threadId ?? threadId.value
        await refreshStack()
        break
      }
      case 'continued':
        paused.value = false
        clearStack()
        break
      case 'output':
        if (m.body?.output) run.push(m.body.category === 'stderr' ? 'stderr' : 'stdout', String(m.body.output).replace(/\n$/, ''))
        break
      case 'terminated':
      case 'exited':
        teardown()
        break
      default:
        break
    }
  }

  /* —— 生命周期 —— */
  async function start(path) {
    if (debugging.value) { run.push('stderr', '已有调试会话在运行'); return }
    lastError.value = ''
    debugging.value = true
    run.push('system', `⏵ 启动调试 ${path}`)
    run.send('debug-start', { path })
  }

  /** 后端 debug-attached 之后才能握手 */
  async function handshake() {
    const r = await request('initialize', {
      clientID: 'bontolink-ide', clientName: 'BOntoLink IDE', adapterID: 'python',
      locale: 'zh-cn', linesStartAt1: true, columnsStartAt1: true, pathFormat: 'path',
      supportsVariableType: true, supportsVariablePaging: false, supportsRunInTerminalRequest: false,
    })
    capabilities.value = r?.body || {}
    // debugpy 是 --wait-for-client 起的, 这里用 attach 语义接上去
    request('attach', { name: 'bontolink' })
  }

  function stop() {
    if (!debugging.value) return
    request('disconnect', { terminateDebuggee: true })
    run.send('debug-stop', {})
    teardown()
  }

  function teardown() {
    debugging.value = false
    paused.value = false
    stoppedReason.value = ''
    clearStack()
    pending.clear()
  }

  function clearStack() {
    frames.value = []
    activeFrameId.value = null
    scopes.value = []
    variables.value = []
  }

  /* —— 执行控制 —— */
  const canStep = computed(() => debugging.value && paused.value)
  function doContinue() { if (canStep.value) { paused.value = false; clearStack(); request('continue', { threadId: threadId.value }) } }
  function stepOver() { if (canStep.value) request('next', { threadId: threadId.value }) }
  function stepIn() { if (canStep.value) request('stepIn', { threadId: threadId.value }) }
  function stepOut() { if (canStep.value) request('stepOut', { threadId: threadId.value }) }

  /* —— 调用栈 / 变量 —— */
  async function refreshStack() {
    const r = await request('stackTrace', { threadId: threadId.value, startFrame: 0, levels: 20 })
    frames.value = r?.body?.stackFrames || []
    if (frames.value.length) await selectFrame(frames.value[0].id)
  }
  async function selectFrame(frameId) {
    activeFrameId.value = frameId
    const r = await request('scopes', { frameId })
    scopes.value = r?.body?.scopes || []
    const local = scopes.value[0]
    variables.value = local ? await loadVariables(local.variablesReference) : []
    refreshWatches()
  }
  async function loadVariables(ref_) {
    const r = await request('variables', { variablesReference: ref_ })
    return (r?.body?.variables || []).map(v => ({ ...v, expanded: false, children: null }))
  }
  /** 展开一个复合变量 */
  async function expandVariable(v) {
    if (!v.variablesReference) return
    if (v.expanded) { v.expanded = false; return }
    v.children = await loadVariables(v.variablesReference)
    v.expanded = true
  }

  /* —— 表达式求值 / 监视 —— */
  async function evaluate(expression, context = 'repl') {
    if (!expression?.trim()) return null
    const r = await request('evaluate', {
      expression, frameId: activeFrameId.value ?? undefined, context,
    })
    return r?.success
      ? { ok: true, result: r.body?.result, type: r.body?.type, ref: r.body?.variablesReference }
      : { ok: false, result: r?.body?.error?.format || r?.message || '求值失败' }
  }
  async function addWatch(expression) {
    if (!expression?.trim()) return
    watches.value.push({ expression, value: '', error: '' })
    refreshWatches()
  }
  function removeWatch(i) { watches.value.splice(i, 1) }
  async function refreshWatches() {
    for (const w of watches.value) {
      if (!paused.value) { w.value = ''; w.error = '未在断点处'; continue }
      const r = await evaluate(w.expression, 'watch')
      if (r?.ok) { w.value = r.result; w.error = '' }
      else { w.value = ''; w.error = r?.result || '求值失败' }
    }
  }

  /* —— 断点 —— */

  /** 写回某个文件的断点列表并按需下发 */
  function commit(p, list) {
    const map = { ...breakpoints.value }
    if (list.length) map[p] = list.sort((a, b) => a.line - b.line)
    else delete map[p]
    breakpoints.value = map
    if (debugging.value) syncBreakpoints(p)
  }

  function toggleBreakpoint(path, line) {
    const p = normalize(path)
    const list = (breakpoints.value[p] || []).slice()
    const i = list.findIndex(b => b.line === line)
    if (i >= 0) list.splice(i, 1)
    else list.push({ line, condition: '', hitCondition: '', logMessage: '', enabled: true })
    commit(p, list)
  }

  /**
   * 新建/改写一个带条件的断点。同一行已有断点时就地改写, 不再叠加一个。
   * @param {object} patch { condition, hitCondition, logMessage }
   */
  function setBreakpoint(path, line, patch = {}) {
    const p = normalize(path)
    const list = (breakpoints.value[p] || []).slice()
    const i = list.findIndex(b => b.line === line)
    const base = i >= 0 ? list[i] : { line, condition: '', hitCondition: '', logMessage: '', enabled: true }
    const next = { ...base, ...patch, line }
    if (i >= 0) list[i] = next
    else list.push(next)
    commit(p, list)
  }

  function removeBreakpoint(path, line) {
    const p = normalize(path)
    commit(p, (breakpoints.value[p] || []).filter(b => b.line !== line))
  }

  /** 启用/禁用单个断点;禁用的断点保留在列表里, 但不下发给调试器 */
  function toggleEnabled(path, line) {
    const p = normalize(path)
    const list = (breakpoints.value[p] || []).map(b =>
      b.line === line ? { ...b, enabled: !b.enabled } : b)
    commit(p, list)
  }

  /** 批量启用/禁用全部断点(运行菜单的「启用/禁用所有断点」) */
  function setAllEnabled(enabled) {
    const map = {}
    Object.entries(breakpoints.value).forEach(([p, list]) => {
      map[p] = list.map(b => ({ ...b, enabled }))
    })
    breakpoints.value = map
    if (debugging.value) Object.keys(map).forEach(p => syncBreakpoints(p))
  }

  function clearAllBreakpoints() {
    const paths = Object.keys(breakpoints.value)
    breakpoints.value = {}
    if (debugging.value) paths.forEach(p => syncBreakpoints(p))
  }

  function listOf(path) { return breakpoints.value[normalize(path)] || [] }
  function linesOf(path) { return listOf(path).map(b => b.line) }
  function bpAt(path, line) { return listOf(path).find(b => b.line === line) || null }
  const breakpointCount = computed(() =>
    Object.values(breakpoints.value).reduce((n, list) => n + list.length, 0))

  /**
   * 下发某个文件的断点;DAP 是整文件覆盖语义。
   * source.path 必须是绝对路径 —— 调试器按磁盘路径匹配, 传相对路径断点不会 verified。
   */
  async function syncBreakpoints(path) {
    const all = breakpoints.value[path] || []
    const active = all.filter(b => b.enabled !== false)     // 禁用的不下发
    const abs = repoRoot ? joinPath(repoRoot, path) : path
    const r = await request('setBreakpoints', {
      source: { path: abs },
      breakpoints: active.map(b => {
        const o = { line: b.line }
        if (b.condition) o.condition = b.condition
        if (b.hitCondition) o.hitCondition = b.hitCondition
        if (b.logMessage) o.logMessage = b.logMessage
        return o
      }),
    })
    const got = r?.body?.breakpoints || []
    const v = { ...verified.value }
    // 禁用的断点不参与校验, 标成 undefined 让界面画成普通灰点
    all.forEach(b => { delete v[`${path}:${b.line}`] })
    active.forEach((b, i) => { v[`${path}:${b.line}`] = got[i]?.verified !== false })
    verified.value = v
  }
  async function syncAllBreakpoints() {
    for (const p of Object.keys(breakpoints.value)) await syncBreakpoints(p)
  }

  function normalize(p) { return String(p || '').replace(/\\/g, '/').replace(/^\.\//, '') }
  function joinPath(root, rel) {
    const r = String(root).replace(/[\\/]+$/, '')
    return `${r}/${normalize(rel)}`.replace(/\//g, root.includes('\\') ? '\\' : '/')
  }

  /* —— 绑定后端事件 —— */
  run.on('dap', onDap)
  run.on('debug-attached', () => handshake())
  run.on('debug-terminated', (b) => {
    run.push('system', `调试会话结束(${b?.reason || ''})`)
    teardown()
  })
  run.on('debug-error', (b) => {
    lastError.value = b?.message || '调试失败'
    run.push('stderr', lastError.value)
    teardown()
  })

  return {
    debugging, paused, stoppedReason, frames, activeFrameId, scopes, variables,
    watches, capabilities, lastError, currentLine, canStep, breakpoints, verified,
    breakpointCount,
    start, stop, doContinue, stepOver, stepIn, stepOut,
    selectFrame, expandVariable, evaluate, addWatch, removeWatch, refreshWatches,
    toggleBreakpoint, setBreakpoint, removeBreakpoint, toggleEnabled, setAllEnabled,
    clearAllBreakpoints, linesOf, listOf, bpAt, setRepoRoot,
  }
}
