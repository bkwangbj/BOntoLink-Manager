import { ref, shallowRef, onBeforeUnmount } from 'vue'

/**
 * 函数运行通道 (P7)
 *
 * 连 /ws/fn-run,承载「以非调试模式运行」与终端命令的下行流。
 * 协议与后端 FnRunWebSocketConfig 对齐:请求 / 响应 / 事件三类报文。
 *
 * 设计要点:
 * - 心跳 30s(文档 3.3),断开自动重连、退避到 10s 封顶,连续失败 3 次后停手并置错
 * - 输出行数封顶,超过就丢最旧的,避免长跑任务把内存吃满
 * - WebSocket 实例用 shallowRef,不进深度响应式(和 Monaco 那次卡死同一个道理)
 */
const MAX_LINES = 5000
const HEARTBEAT_MS = 30000
const MAX_RETRY = 3

export function useFnRun() {
  const ws = shallowRef(null)
  const connected = ref(false)
  const connError = ref('')
  const running = ref(false)
  const runKind = ref('')          // run | terminal
  const caps = ref({})
  /** [{ id, stream: stdout|stderr|system|input, text, ts }] */
  const lines = ref([])

  let seq = 0
  let retry = 0
  let heartbeatTimer = null
  let reconnectTimer = null
  let manuallyClosed = false

  function wsUrl() {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    return `${proto}://${location.host}/ws/fn-run`
  }

  function push(stream, text) {
    lines.value.push({ id: ++seq, stream, text, ts: Date.now() })
    if (lines.value.length > MAX_LINES) lines.value.splice(0, lines.value.length - MAX_LINES)
  }

  function connect() {
    if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) return
    manuallyClosed = false
    let sock
    try {
      sock = new WebSocket(wsUrl())
    } catch (e) {
      connError.value = `无法建立连接:${e.message}`
      return
    }
    ws.value = sock

    sock.onopen = () => {
      connected.value = true
      connError.value = ''
      retry = 0
      clearInterval(heartbeatTimer)
      heartbeatTimer = setInterval(() => send('ping'), HEARTBEAT_MS)
    }
    sock.onmessage = (ev) => {
      let msg
      try { msg = JSON.parse(ev.data) } catch { return }
      if (msg.type === 'event') onEvent(msg)
      else if (msg.type === 'response' && msg.success === false) {
        push('stderr', `[${msg.command}] ${msg.body?.message || '执行失败'}`)
      }
    }
    sock.onerror = () => { connError.value = '运行通道连接异常' }
    sock.onclose = () => {
      connected.value = false
      running.value = false
      clearInterval(heartbeatTimer)
      if (manuallyClosed) return
      if (retry >= MAX_RETRY) { connError.value = '运行通道已断开(重连失败),请刷新页面'; return }
      const delay = Math.min(10000, 1000 * Math.pow(2, retry++))
      reconnectTimer = setTimeout(connect, delay)
    }
  }

  /* 事件订阅:调试模块(useFnDebug)复用同一条连接, 靠这里拿 dap / debug-* 事件 */
  const listeners = new Map()
  function on(name, fn) {
    if (!listeners.has(name)) listeners.set(name, new Set())
    listeners.get(name).add(fn)
    return () => listeners.get(name)?.delete(fn)
  }
  function emit(name, body) {
    listeners.get(name)?.forEach(fn => { try { fn(body) } catch (e) { console.warn('[fn-run] 监听器异常', e) } })
  }

  function onEvent(msg) {
    const b = msg.body || {}
    emit(msg.event, b)
    switch (msg.event) {
      case 'ready':
        caps.value = b
        break
      case 'started':
        running.value = true
        runKind.value = b.kind || ''
        push('system', `> ${b.command}`)
        break
      case 'output':
        push(b.stream || 'stdout', b.text ?? '')
        break
      case 'exit': {
        running.value = false
        const tip = b.timeout ? '(超时被强制终止)' : (b.stopped ? '(已手动停止)' : '')
        push('system', `进程已结束,退出码 ${b.code}${tip}`)
        break
      }
      case 'error':
        push('stderr', b.message || '未知错误')
        running.value = false
        break
      default:
        break
    }
  }

  function send(command, args) {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      push('stderr', '运行通道未连接')
      return false
    }
    ws.value.send(JSON.stringify({ type: 'request', seq: ++seq, command, args: args || {} }))
    return true
  }

  function runFile(path) {
    if (running.value) { push('stderr', '已有进程在运行,请先停止'); return }
    push('system', `▶ 运行 ${path}`)
    send('run', { path })
  }
  function runCommand(command) {
    push('input', `$ ${command}`)
    send('terminal', { command })
  }
  function stop() { send('stop') }
  function clear() { lines.value = [] }

  function close() {
    manuallyClosed = true
    clearInterval(heartbeatTimer)
    clearTimeout(reconnectTimer)
    try { ws.value?.close() } catch { /* ignore */ }
    ws.value = null
    connected.value = false
  }

  onBeforeUnmount(close)

  return {
    connected, connError, running, runKind, caps, lines,
    connect, close, runFile, runCommand, stop, clear,
    /* 给调试模块复用 */
    send, on, push,
  }
}

/**
 * 极简 ANSI SGR 解析:把控制码转成带样式的片段。
 * 只覆盖前景色 / 加粗 / 重置 —— 够用来还原大多数 CLI 的彩色输出,
 * 不做完整终端仿真(那需要 xterm.js,见 P9)。
 */
const ANSI_FG = {
  30: '#666666', 31: '#f14c4c', 32: '#23d18b', 33: '#f5f543', 34: '#3b8eea',
  35: '#d670d6', 36: '#29b8db', 37: '#e5e5e5',
  90: '#888888', 91: '#f14c4c', 92: '#23d18b', 93: '#f5f543', 94: '#3b8eea',
  95: '#d670d6', 96: '#29b8db', 97: '#ffffff',
}
export function parseAnsi(text) {
  const out = []
  const re = /\[([0-9;]*)m/g
  let last = 0, color = '', bold = false, m
  const emit = (t) => { if (t) out.push({ text: t, color, bold }) }
  while ((m = re.exec(text)) !== null) {
    emit(text.slice(last, m.index))
    last = re.lastIndex
    for (const code of m[1].split(';')) {
      const n = Number(code || 0)
      if (n === 0) { color = ''; bold = false }
      else if (n === 1) bold = true
      else if (n === 22) bold = false
      else if (ANSI_FG[n]) color = ANSI_FG[n]
      else if (n === 39) color = ''
    }
  }
  emit(text.slice(last))
  return out.length ? out : [{ text: '', color: '', bold: false }]
}
