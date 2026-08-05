<template>
  <div class="fnide" :data-ide-theme="settings.theme">
    <!-- ===== 顶部:菜单栏 + 函数标识 ===== -->
    <header class="fnide-top">
      <IdeMenuBar :editor="editor" :language="fnLanguage" :settings="settings" :panel="panel" :panel-open="panelOpen"
                  :running="run.running.value"
                  :debugging="dbg.debugging.value" :paused="dbg.paused.value"
                  :breakpoint-count="dbg.breakpointCount.value"
                  @debug-action="onDebugAction"
                  @open-settings="settingsOpen = true"
                  @patch-settings="patchSettings"
                  @toggle-panel-tab="togglePanelTab"
                  @toggle-panel="onTogglePanel"
                  @run-file="runActiveFile"
                  @stop-run="run.stop"
                  @command-palette="onPalette"
                  @help="onHelp" />
      <div class="fnide-top-r">
        <span class="fnide-fn" :title="fn.full_access_path">
          <span class="fnide-fx">Fx</span>
          <span class="bl-mono">{{ fn.api_name || '—' }}</span>
          <span class="fnide-fn-sub">{{ fn.function_label || '' }}</span>
        </span>
        <span class="fnide-sep"></span>
        <button class="fnide-btn" title="返回函数详情" @click="backToList">
          <span v-html="BL.icon('back', 12)"></span>返回平台
        </button>
      </div>
    </header>

    <!-- ===== 主体:活动栏 + 侧边面板 + 编辑区 ===== -->
    <div class="fnide-body">
      <IdeSideBar v-show="sidebarOpen" ref="sideBar"
                  :tree="tree" :repo="repo" :dbg="dbg" :active-path="activePath"
                  :active-content="activeContent" :open-files="openFiles"
                  @open-file="openFile" @goto="gotoLine" @reload-tree="loadTree" @search-hl="setSearchHl"
                  @show-commit="showCommit"
                  @collapse="sidebarOpen = false" @pushed="loadRepoStatus"
                  @branch-switched="onBranchSwitched" />
      <button v-show="!sidebarOpen" class="fnide-expand" title="展开侧边栏" @click="sidebarOpen = true"
              v-html="BL.icon('chevronRight', 14)"></button>

      <main class="fnide-main">
        <!-- 标签栏 -->
        <div class="fnide-tabs" v-if="openFiles.length">
          <div v-for="f in visibleTabs" :key="f.path"
               :class="['fnide-tab', activePath === f.path && 'is-on']"
               :title="f.path" @click="activate(f.path)">
            <span class="fnide-tab-ic" v-html="BL.icon('fileCode', 11)"></span>
            <span class="fnide-tab-name">{{ baseName(f.path) }}</span>
            <span v-if="f.dirty" class="fnide-tab-dot" title="未保存"></span>
            <span class="fnide-tab-x" title="关闭" @click.stop="closeFile(f.path)" v-html="BL.icon('x', 10)"></span>
          </div>
        </div>

        <!-- 编辑器 -->
        <div class="fnide-editor-wrap">
          <div ref="editorEl" class="fnide-editor"></div>
          <div v-if="!openFiles.length" class="fnide-blank">
            <div class="fnide-blank-logo">Fx</div>
            <div class="fnide-blank-t">本体驱动的函数在线编排系统</div>
            <div class="fnide-blank-s">从左侧「项目文件树」打开一个文件开始编辑</div>
            <div class="fnide-blank-k">
              <span><b>Ctrl</b> + <b>S</b> 保存(仅本地提交)</span>
              <span><b>Ctrl</b> + <b>=</b> / <b>-</b> 缩放</span>
            </div>
          </div>
        </div>

        <!-- 底部面板:问题 / 输出 / 调试控制台 / 终端 -->
        <IdeBottomPanel v-if="panelOpen"
                        v-model:active="panelActive"
                        :panel="panel" :lines="run.lines.value" :problems="problems"
                        :connected="run.connected.value" :conn-error="run.connError.value"
                        :running="run.running.value" :paused="dbg.paused.value"
                        @toggle-tab="togglePanelTab" @hide="panelOpen = false"
                        @stop="onPanelStop" @clear="run.clear"
                        @run-command="run.runCommand" @goto="gotoLine"
                        @evaluate="onEvaluate" />
      </main>
    </div>

    <!-- ===== 状态栏 ===== -->
    <footer class="fnide-status">
      <span>{{ repo.ready ? (repo.current_branch || repo.branch) : '仓库不可用' }}</span>
      <span v-if="aheadCount > 0" class="fnide-status-warn" title="待推送提交数">↑{{ aheadCount }}</span>
      <span v-if="dbg.debugging.value" class="fnide-status-dbg">
        {{ dbg.paused.value ? `已暂停 · ${dbg.stoppedReason.value}` : '调试中' }}
      </span>
      <span v-if="saving">保存中…</span>
      <span class="bl-grow"></span>
      <span v-if="activePath" class="bl-mono">{{ activePath }}</span>
      <span v-if="cursorPos">行 {{ cursorPos.lineNumber }},列 {{ cursorPos.column }}</span>
      <span>{{ langLabel }}</span>
      <span>{{ settings.tabSize }} 空格</span>
    </footer>

    <IdeSettingsModal v-model:open="settingsOpen" :settings="settings" @apply="applySettingsPatch" />

    <IdeCommandPalette v-model:open="paletteOpen" v-model:mode="paletteMode"
                       :commands="commands" :bindings="bindings" :files="flatFiles" :theme="settings.theme"
                       @run="c => c.run?.()" @open-file="openFile" />

    <IdeKeybindingsModal v-model:open="keysOpen" :commands="commands" :bindings="bindings"
                         :overrides="keyOverrides" :theme="settings.theme"
                         @set-binding="onSetBinding" @reset-all="onResetBindings" />

    <IdeHelpModal v-model:open="helpOpen" :theme="settings.theme" />

    <IdeCommitDiffModal v-model:open="diffOpen" :commit="diffCommit" :theme="settings.theme" />
  </div>
</template>

<script setup>
/**
 * 本体驱动的函数在线编排系统 —— IDE 外壳 (P5b)
 *
 * 布局:顶部菜单栏 + 左侧活动栏/面板 + 编辑区(标签页 + Monaco) + 底部面板骨架 + 状态栏。
 * 文件来源是服务端的 JGit 工作区 (/api/fn-repo),保存 = 服务端本地提交;
 * 推送是显式动作,在侧边栏「版本变更」面板里做(auto-push 默认关闭)。
 *
 * 主题独立于系统深浅开关:全部色值挂在 .fnide[data-ide-theme] 上。
 */
import { ref, shallowRef, markRaw, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as monaco from 'monaco-editor'
import { BL } from '@/lib/bl.js'
import { functionApi, fnRepoApi, resourceApi } from '@/api'
import IdeMenuBar from './IdeMenuBar.vue'
import IdeSideBar from './IdeSideBar.vue'
import IdeBottomPanel from './IdeBottomPanel.vue'
import IdeSettingsModal from './IdeSettingsModal.vue'
import IdeCommandPalette from './IdeCommandPalette.vue'
import IdeKeybindingsModal from './IdeKeybindingsModal.vue'
import IdeHelpModal from './IdeHelpModal.vue'
import IdeCommitDiffModal from './IdeCommitDiffModal.vue'
import { setupTypescriptEnv } from './platformTypes.js'
import { useFnRun } from './useFnRun.js'
import { useFnDebug } from './useFnDebug.js'
import { useIdeDrafts } from './useIdeDrafts.js'
import { buildCommands } from './ideCommands.js'
import { loadOverrides, saveOverrides, resolveBindings, matchCommand } from './ideKeys.js'
import { loadSettings, saveSettings, toMonacoOptions, MONACO_THEME, langOfPath } from './ideSettings.js'
import './ideTheme.css'

// monaco worker(与 MakerStudio 同一套注册方式,全局只注册一次)
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import CssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import HtmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

if (!self.MonacoEnvironment) {
  self.MonacoEnvironment = {
    getWorker(_, label) {
      if (label === 'json') return new JsonWorker()
      if (label === 'css' || label === 'scss' || label === 'less') return new CssWorker()
      if (label === 'html' || label === 'handlebars' || label === 'razor') return new HtmlWorker()
      if (label === 'typescript' || label === 'javascript') return new TsWorker()
      return new EditorWorker()
    }
  }
}

const route = useRoute()
const router = useRouter()

/* —— 状态 —— */
const fn = ref({})
const tree = ref([])
const repo = ref({})
/*
 * openFiles 只放可序列化的轻量数据 [{ path, content, dirty }]。
 * Monaco 的 ITextModel / 编辑器实例 / viewState 一律不能进响应式系统 ——
 * 它们内部对象图巨大且带循环引用, Vue 深度代理会把主线程直接跑死(页面无响应)。
 * 因此 model 与 viewState 存在下面两个普通 Map 里, 编辑器实例用 shallowRef + markRaw。
 */
const openFiles = ref([])
const models = new Map()           // path → ITextModel
const viewStates = new Map()       // path → ICodeEditorViewState
const activePath = ref('')
/** 当前文件内容的防抖快照,只用于「文件结构大纲」解析,不参与编辑 */
const activeContent = ref('')
const editorEl = ref(null)
const editor = shallowRef(null)
const sideBar = ref(null)
const sidebarOpen = ref(true)
const settingsOpen = ref(false)
const saving = ref(false)
const cursorPos = ref(null)

const settings = reactive(loadSettings())

/* 底部面板(P7 才有实际内容, 这里维护显隐状态与菜单联动) */
const PANEL_TABS = [
  { k: 'problems', label: '问题', icon: 'warning' },
  { k: 'output', label: '输出', icon: 'list' },
  { k: 'debug', label: '调试控制台', icon: 'terminal' },
  { k: 'terminal', label: '终端', icon: 'terminal' },
]
const panel = reactive({ problems: true, output: true, debug: true, terminal: true, showIcons: true })
const panelOpen = ref(false)
const panelActive = ref('output')
const visiblePanelTabs = computed(() => PANEL_TABS.filter(t => panel[t.k]))

/* —— 运行通道(WebSocket):非调试运行 + 终端 + 调试共用一条连接 —— */
const run = useFnRun()
const dbg = useFnDebug(run)

/* —— 断点与执行行的编辑器装饰 —— */
let bpDecorations = []
let stackDecorations = []
function renderBreakpoints() {
  if (!editor.value || !activePath.value) return
  const decos = dbg.listOf(activePath.value).map(b => {
    const cls = ['fnide-bp']
    if (b.enabled === false) cls.push('fnide-bp-off')
    else if (dbg.verified.value[`${activePath.value}:${b.line}`] === false) cls.push('fnide-bp-unverified')
    if (b.logMessage) cls.push('fnide-bp-log')
    else if (b.condition || b.hitCondition) cls.push('fnide-bp-cond')
    const tips = [`第 ${b.line} 行`]
    if (b.condition) tips.push(`条件:${b.condition}`)
    if (b.hitCondition) tips.push(`命中次数:${b.hitCondition}`)
    if (b.logMessage) tips.push(`日志:${b.logMessage}`)
    if (b.enabled === false) tips.push('(已禁用)')
    return {
      range: new monaco.Range(b.line, 1, b.line, 1),
      options: {
        isWholeLine: false,
        glyphMarginClassName: cls.join(' '),
        glyphMarginHoverMessage: { value: `${b.logMessage ? '日志点' : (b.condition || b.hitCondition) ? '条件断点' : '断点'} · ${tips.join(' · ')}` },
      },
    }
  })
  bpDecorations = editor.value.deltaDecorations(bpDecorations, decos)
}
function renderStackLine() {
  if (!editor.value) return
  const cur = dbg.currentLine.value
  const here = cur && activePath.value && String(cur.path).endsWith(activePath.value)
  const decos = here ? [{
    range: new monaco.Range(cur.line, 1, cur.line, 1),
    options: { isWholeLine: true, className: 'fnide-stackline', glyphMarginClassName: 'fnide-stackarrow' },
  }] : []
  stackDecorations = editor.value.deltaDecorations(stackDecorations, decos)
  if (here) editor.value.revealLineInCenterIfOutsideViewport(cur.line)
}
watch(() => dbg.breakpoints.value, renderBreakpoints, { deep: true })
watch(() => dbg.verified.value, renderBreakpoints, { deep: true })
watch(() => dbg.currentLine.value, renderStackLine)
watch(activePath, () => { renderBreakpoints(); renderStackLine() })

/** 启动调试:先落盘再跑, 否则调的是磁盘上的旧代码 */
async function startDebug() {
  if (!activePath.value) { BL.warning('请先打开一个文件'); return }
  if (!activePath.value.toLowerCase().endsWith('.py')) {
    BL.warning('当前只支持 Python 断点调试;TypeScript 需要 js-debug 适配器(见 P8 说明)')
    return
  }
  const f = openFiles.value.find(x => x.path === activePath.value)
  if (f?.dirty) await saveActive()
  panelOpen.value = true
  panelActive.value = 'debug'
  dbg.setRepoRoot(repo.value.workdir || '')
  dbg.start(activePath.value)
}

/** 运行当前标签页的文件;有未保存改动先落盘再跑, 否则跑的是旧代码 */
async function runActiveFile() {
  if (!activePath.value) { BL.warning('请先打开一个文件'); return }
  const f = openFiles.value.find(x => x.path === activePath.value)
  if (f?.dirty) await saveActive()
  panelOpen.value = true
  panelActive.value = 'output'
  run.clear()
  run.runFile(activePath.value)
}

/* —— 问题面板:直接取 Monaco 的诊断标记 ——
 *
 * 本体类型(HydrologyStation 之类)的 .d.ts 是「资源导入」面板按需注入的,没导入之前
 * TS 语言服务必然报 "Cannot find name 'X'"。这类不是代码错误, 是缺依赖声明,
 * 混在红叉里会让人以为代码坏了 —— 所以降级成提示级并换成能指路的文案。
 * 只对**平台里真实存在的本体类名**降级, 拼错的类名仍然报错。 */
const problems = ref([])
const ontologyNames = ref(new Set())

/** 平台已有的本体类名, 用来判断一个「找不到的名字」到底是本体类型还是笔误 */
async function loadOntologyNames() {
  try {
    const list = await resourceApi.classes()
    const s = new Set()
    ;(Array.isArray(list) ? list : []).forEach(c => {
      if (c.api_name) s.add(c.api_name)
      if (c.rdfs_label) s.add(c.rdfs_label)
    })
    ontologyNames.value = s
  } catch {
    ontologyNames.value = new Set()
  }
}

/** 当前分支已导入的本体类名(与资源导入面板同一份 localStorage) */
function importedOntologyNames() {
  try {
    const branch = repo.value.current_branch || repo.value.branch || 'master'
    const d = JSON.parse(localStorage.getItem(`bl.ide.imports.${branch}`) || '{}')
    return new Set((d.ontology || []).map(r => r.api_name || r.label).filter(Boolean))
  } catch {
    return new Set()
  }
}

const MISSING_NAME_RE = /(?:Cannot find name|找不到名称)\s*['"“]([A-Za-z_$][\w$]*)['"”]/

function refreshProblems() {
  try {
    const all = monaco.editor.getModelMarkers({})
    const imported = importedOntologyNames()
    problems.value = all.map(m => {
      const uri = String(m.resource || '')
      // model 是按 path 建的 inmemory uri, 反查回相对路径
      const hit = openFiles.value.find(f => uri.endsWith(f.path)) || {}
      const row = {
        path: hit.path || uri.split('/').pop(),
        line: m.startLineNumber, column: m.startColumn,
        message: m.message, severity: m.severity,
      }
      const name = (MISSING_NAME_RE.exec(String(m.message)) || [])[1]
      if (name && ontologyNames.value.has(name) && !imported.has(name)) {
        row.severity = 2                                  // MarkerSeverity.Info
        row.kind = 'ontology-missing'
        row.message = `本体类型「${name}」尚未导入 —— 在左侧「资源导入」面板导入后即可获得补全与类型校验`
      }
      return row
    })
  } catch {
    problems.value = []
  }
}

const fnLanguage = computed(() => Number(fn.value.language) || 2)
const langLabel = computed(() => (activePath.value ? langOfPath(activePath.value) : (fnLanguage.value === 1 ? 'python' : 'typescript')))
const aheadCount = computed(() => { const n = Number(repo.value.ahead); return Number.isFinite(n) && n > 0 ? n : 0 })
/* 单标签模式只显示当前文件(文档「标签栏」单/多标签) */
const visibleTabs = computed(() =>
  settings.tabsMode === 'single' ? openFiles.value.filter(f => f.path === activePath.value) : openFiles.value)

/* —— 加载 —— */
async function loadFunction() {
  const id = route.params.id
  if (!id) return
  fn.value = await functionApi.get(id).catch(() => ({})) || {}
  document.title = `${fn.value.api_name || '函数'} · 在线编排`
}
async function loadTree() {
  tree.value = await fnRepoApi.tree().catch(() => [])
}
async function loadRepoStatus() {
  repo.value = await fnRepoApi.status().catch(() => ({}))
}

/* —— 文件打开 / 切换 / 关闭 —— */
async function openFile(path) {
  const hit = openFiles.value.find(f => f.path === path)
  if (hit) { activate(path); return }
  let content = ''
  try {
    const r = await fnRepoApi.read(path)
    content = r?.content ?? ''
  } catch (e) {
    BL.error(`打开失败:${e?.message || e}`)
    return
  }
  const model = markRaw(monaco.editor.createModel(content, langOfPath(path)))
  model.onDidChangeContent(() => {
    const f = openFiles.value.find(x => x.path === path)
    if (f) f.dirty = model.getValue() !== f.content
    if (path === activePath.value) syncActiveContent()
  })
  models.set(path, model)
  openFiles.value.push({ path, content, dirty: false })
  applySearchHl(path)
  activate(path)
}

function activate(path) {
  if (!editor.value) return
  if (activePath.value) viewStates.set(activePath.value, editor.value.saveViewState())
  const model = models.get(path)
  if (!model) return
  activePath.value = path
  editor.value.setModel(model)
  const vs = viewStates.get(path)
  if (vs) editor.value.restoreViewState(vs)
  editor.value.focus()
  syncActiveContent(0)
}

/** 大纲用的内容快照:默认 250ms 防抖, 避免每次按键都全量取一次文本 */
let contentTimer = null
function syncActiveContent(delay = 250) {
  clearTimeout(contentTimer)
  contentTimer = setTimeout(() => {
    const m = models.get(activePath.value)
    activeContent.value = m ? m.getValue() : ''
  }, delay)
}

async function closeFile(path) {
  const f = openFiles.value.find(x => x.path === path)
  if (!f) return
  if (f.dirty) {
    const ok = await BL.confirm({ title: '关闭文件', content: `「${baseName(path)}」有未保存的修改,确定关闭?`, okText: '关闭不保存' })
    if (!ok) return
  }
  models.get(path)?.dispose()
  models.delete(path)
  viewStates.delete(path)
  searchDecos.delete(path)
  openFiles.value = openFiles.value.filter(x => x.path !== path)
  if (activePath.value === path) {
    const next = openFiles.value[openFiles.value.length - 1]
    if (next) activate(next.path)
    else { activePath.value = ''; editor.value?.setModel(null) }
  }
}

/**
 * 切换分支后:磁盘工作区已经变了, 重新拉树与状态, 并把已打开文件的内容刷新;
 * 新分支上不存在的文件直接关掉标签, 免得留一个指向幽灵文件的编辑器。
 */
async function onBranchSwitched() {
  await Promise.all([loadTree(), loadRepoStatus()])
  for (const p of openFiles.value.map(f => f.path)) {
    try {
      const r = await fnRepoApi.read(p)
      const text = r?.content ?? ''
      models.get(p)?.setValue(text)
      const f = openFiles.value.find(x => x.path === p)
      if (f) { f.content = text; f.dirty = false }
    } catch {
      models.get(p)?.dispose()
      models.delete(p)
      viewStates.delete(p)
      openFiles.value = openFiles.value.filter(x => x.path !== p)
      if (activePath.value === p) {
        const next = openFiles.value[openFiles.value.length - 1]
        if (next) activate(next.path)
        else { activePath.value = ''; activeContent.value = ''; editor.value?.setModel(null) }
      }
    }
  }
  syncActiveContent(0)
  sideBar.value?.reloadHistory?.()
}

function gotoLine({ path, line }) {
  openFile(path).then(() => {
    if (!editor.value) return
    editor.value.revealLineInCenter(line)
    const range = matchOnLine(path, line)
    if (range) editor.value.setSelection(range)
    else editor.value.setPosition({ lineNumber: line, column: 1 })
    editor.value.focus()
  })
}

/* —— 全局搜索命中在编辑器里的高亮 ——
 * 装饰 id 按文件存在普通 Map 里(和 models 一样不进响应式)。 */
const searchDecos = new Map()
let searchHl = ''

function setSearchHl(k) {
  searchHl = String(k || '')
  for (const path of models.keys()) applySearchHl(path)
}

function applySearchHl(path) {
  const model = models.get(path)
  if (!model) return
  const old = searchDecos.get(path) || []
  const next = searchHl
    ? model.findMatches(searchHl, false, false, false, null, false, 2000).map(m => ({
        range: m.range,
        options: {
          className: 'ide-find-match',
          stickiness: monaco.editor.TrackedRangeStickiness.NeverGrowsWhenTypingAtEdges,
          overviewRuler: { color: 'rgba(234,92,0,.7)', position: monaco.editor.OverviewRulerLane.Center },
        },
      }))
    : []
  searchDecos.set(path, model.deltaDecorations(old, next))
}

/** 目标行上第一个命中的范围, 用来把光标直接落在词上 */
function matchOnLine(path, line) {
  const model = models.get(path)
  if (!model || !searchHl) return null
  const scope = new monaco.Range(line, 1, line, model.getLineMaxColumn(line))
  const m = model.findMatches(searchHl, scope, false, false, null, false, 1)
  return m.length ? m[0].range : null
}

/* —— 保存:写工作区 + 服务端本地提交(不推送) —— */
async function saveActive() {
  const f = openFiles.value.find(x => x.path === activePath.value)
  const model = models.get(activePath.value)
  if (!f || !model || !editor.value) return
  const content = model.getValue()
  if (!f.dirty) { BL.info('没有需要保存的修改'); return }
  saving.value = true
  try {
    const r = await fnRepoApi.write(f.path, content, `edit: 更新 ${f.path}`)
    f.content = content
    f.dirty = false
    drafts.drop(f.path)                     // 已落盘, 对应草稿作废
    if (r?.committed) BL.success(`已保存并提交 ${String(r.commit).slice(0, 8)}(未推送)`)
    else BL.info(r?.reason || '内容无变化')
    await loadRepoStatus()
    sideBar.value?.reloadHistory?.()
  } catch (e) {
    BL.error(`保存失败:${e?.message || e}`)
  } finally {
    saving.value = false
  }
}

/* —— 设置 —— */
function patchSettings(patch) {
  Object.assign(settings, patch)
  saveSettings({ ...settings })
  applyToEditor()
}
function applySettingsPatch(next) {
  Object.assign(settings, next)
  saveSettings({ ...settings })
  applyToEditor()
}
function applyToEditor() {
  if (!editor.value) return
  monaco.editor.setTheme(MONACO_THEME[settings.theme] || 'vs-dark')
  editor.value.updateOptions(toMonacoOptions(settings))
}

/** 底部面板的「停止」:调试中停调试, 否则停运行进程 */
function onPanelStop() {
  if (dbg.debugging.value) dbg.stop()
  else run.stop()
}

/** 调试控制台求值:回显输入与结果, 与文档「输入前缀带 > 标识」一致 */
async function onEvaluate(expression) {
  run.push('input', `> ${expression}`)
  const r = await dbg.evaluate(expression, 'repl')
  if (!r) return
  run.push(r.ok ? 'stdout' : 'stderr', String(r.result))
}

/** 运行菜单里的调试动作 */
function onDebugAction(action) {
  switch (action) {
    case 'debug-start': startDebug(); break
    case 'debug-stop': dbg.stop(); break
    case 'debug-restart': dbg.stop(); setTimeout(startDebug, 600); break
    case 'step-over': dbg.stepOver(); break
    case 'step-in': dbg.stepIn(); break
    case 'step-out': dbg.stepOut(); break
    case 'continue': dbg.doContinue(); break
    case 'toggle-bp':
      if (activePath.value && cursorPos.value) dbg.toggleBreakpoint(activePath.value, cursorPos.value.lineNumber)
      break
    case 'clear-bps': dbg.clearAllBreakpoints(); break
    case 'bp-conditional': newBreakpoint('condition'); break
    case 'bp-hitcount': newBreakpoint('hitCondition'); break
    case 'bp-logpoint': newBreakpoint('logMessage'); break
    case 'bp-enable-all': dbg.setAllEnabled(true); BL.success('已启用所有断点'); break
    case 'bp-disable-all': dbg.setAllEnabled(false); BL.success('已禁用所有断点'); break
    default: break
  }
}

/** 三种高级断点共用一个入口:问一句表达式, 打在光标所在行 */
const BP_PROMPT = {
  condition: { title: '条件断点', label: '表达式为真时才中断', ph: '例如 age > 20' },
  hitCondition: { title: '命中次数断点', label: '命中次数满足条件时才中断', ph: '例如 >5、==3' },
  logMessage: { title: '日志点', label: '不中断,只在调试控制台打印一行', ph: '例如 站龄={age}' },
}
async function newBreakpoint(kind) {
  if (!activePath.value || !cursorPos.value) { BL.warning('请先把光标放到要打断点的行'); return }
  const line = cursorPos.value.lineNumber
  const cfg = BP_PROMPT[kind]
  const old = dbg.bpAt(activePath.value, line)
  const v = await BL.prompt({
    title: `${cfg.title} · 第 ${line} 行`,
    label: cfg.label,
    defaultValue: old?.[kind] || '',
    placeholder: cfg.ph,
    validate: (s) => (String(s || '').trim() ? true : '不能为空'),
  })
  if (v === null || v === undefined) return
  dbg.setBreakpoint(activePath.value, line, { [kind]: String(v).trim() })
  BL.success(`已在第 ${line} 行设置${cfg.title}`)
}

/* —— 底部面板 / 菜单动作 —— */
function togglePanelTab(k) {
  panel[k] = !panel[k]
  if (k !== 'showIcons' && !panel[panelActive.value]) {
    const first = visiblePanelTabs.value[0]
    panelActive.value = first ? first.k : ''
  }
  if (k !== 'showIcons' && panel[k]) panelOpen.value = true
}
function onTogglePanel() { panelOpen.value = !panelOpen.value }
function onPalette() { paletteMode.value = 'command'; paletteOpen.value = true }
function onHelp() { helpOpen.value = true }
function backToList() {
  router.push({ path: '/resources/functions', query: { openId: route.params.id } })
}

/* ==================== P9:命令面板 / 快捷键 / 草稿 / 文档 ==================== */
const paletteOpen = ref(false)
const paletteMode = ref('command')
const keysOpen = ref(false)
const helpOpen = ref(false)
const keyOverrides = ref(loadOverrides())

/* 提交对比弹窗:侧栏「版本变更」里点一条提交就打开 */
const diffOpen = ref(false)
const diffCommit = ref('')
function showCommit(sha) {
  if (!sha) return
  diffCommit.value = sha
  diffOpen.value = true
}

/** 命令面板的「转到文件」用:把文件树拍平成路径列表 */
const flatFiles = computed(() => {
  const out = []
  const walk = (ns) => (ns || []).forEach(n => (n.dir ? walk(n.children) : out.push(n.path)))
  walk(tree.value)
  return out
})

/** 命令注册表:面板、快捷键共用同一份定义 */
const commands = computed(() => buildCommands({
  editor,
  saveActive,
  loadTree,
  runActiveFile,
  stopRun: () => run.stop(),
  debugStartOrContinue: () => (dbg.paused.value ? dbg.doContinue() : startDebug()),
  debugStop: () => dbg.stop(),
  toggleBreakpoint: () => {
    if (activePath.value && cursorPos.value) dbg.toggleBreakpoint(activePath.value, cursorPos.value.lineNumber)
  },
  stepOver: () => dbg.stepOver(),
  stepIn: () => dbg.stepIn(),
  stepOut: () => dbg.stepOut(),
  clearBreakpoints: () => dbg.clearAllBreakpoints(),
  conditionalBreakpoint: () => newBreakpoint('condition'),
  hitCountBreakpoint: () => newBreakpoint('hitCondition'),
  logpoint: () => newBreakpoint('logMessage'),
  enableAllBreakpoints: () => { dbg.setAllEnabled(true); BL.success('已启用所有断点') },
  disableAllBreakpoints: () => { dbg.setAllEnabled(false); BL.success('已禁用所有断点') },
  push: async () => {
    const r = await fnRepoApi.push().catch(e => ({ ok: false, message: e?.message }))
    r?.ok ? BL.success('推送成功') : BL.error(`推送失败:${r?.message || ''}`)
    loadRepoStatus()
  },
  showActivity: (k) => { sidebarOpen.value = true; sideBar.value?.setActivity?.(k) },
  openPalette: (mode) => { paletteMode.value = mode; paletteOpen.value = true },
  openSettings: () => { settingsOpen.value = true },
  openKeybindings: () => { keysOpen.value = true },
  openHelp: () => { helpOpen.value = true },
  togglePanel: onTogglePanel,
  toggleSidebar: () => { sidebarOpen.value = !sidebarOpen.value },
  setTheme: (t) => patchSettings({ theme: t }),
  zoom: (dir) => patchSettings({ fontSize: dir === 0 ? 14 : clampFont(settings.fontSize + dir) }),
  backToPlatform: backToList,
}))

const bindings = computed(() => resolveBindings(commands.value, keyOverrides.value))

function onSetBinding({ id, binding }) {
  const map = { ...keyOverrides.value }
  if (binding === undefined) delete map[id]      // 还原默认
  else map[id] = binding
  keyOverrides.value = map
  saveOverrides(map)
}
function onResetBindings() {
  keyOverrides.value = {}
  saveOverrides({})
  BL.success('已全部还原为默认快捷键')
}

/* —— 快捷键统一分发:全部走命令表, 不再散落 if-else —— */
/** 这些命令在任何地方都该生效;其余编辑类命令在普通输入框里要让位给浏览器默认行为 */
const GLOBAL_CMDS = new Set([
  'view.palette', 'file.goto', 'view.togglePanel', 'view.toggleSidebar', 'view.settings',
  'view.keybindings', 'help.docs', 'view.zoomIn', 'view.zoomOut', 'view.zoomReset',
  'run.file', 'run.stop', 'debug.start', 'debug.stop', 'debug.toggleBreakpoint',
  'debug.stepOver', 'debug.stepIn', 'debug.stepOut', 'debug.conditionalBreakpoint',
])
function onKeyDown(e) {
  // 弹层打开时不抢按键(命令面板/快捷键录制/设置各自处理)
  if (keysOpen.value) return
  if (paletteOpen.value && e.key !== 'Escape') return
  const id = matchCommand(e, bindings.value)
  if (!id) return
  const cmd = commands.value.find(c => c.id === id)
  if (!cmd) return
  /*
   * Monaco 的输入承载体本身就是个隐藏 textarea, 不能一刀切"输入框里不响应",
   * 否则编辑器内的 Ctrl+S / Ctrl+F 全废。判断依据改成:是否在编辑器内部。
   * 终端、筛选框这类普通输入框里, 只放行全局命令, Ctrl+A / Ctrl+F 交还浏览器。
   */
  const t = e.target
  const editable = t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.isContentEditable)
  const inEditor = typeof t?.closest === 'function' && !!t.closest('.monaco-editor')
  if (editable && !inEditor && !GLOBAL_CMDS.has(id)) return
  e.preventDefault()
  cmd.run?.()
}
function clampFont(n) { return Math.max(8, Math.min(20, Number(n) || 14)) }

/* —— 草稿自动保存 —— */
const drafts = useIdeDrafts()
/** 打开时若有上次未保存的草稿, 灌回编辑器并标脏, 由用户决定是否保存 */
async function restoreDrafts() {
  const list = drafts.list()
  if (!list.length) return
  const ok = await BL.confirm({
    title: '恢复未保存的草稿',
    content: `检测到 ${list.length} 个文件有上次未保存的改动(${list.map(d => baseName(d.path)).join('、')}),要恢复到编辑器吗?选择「否」将丢弃这些草稿。`,
    okText: '恢复'
  })
  if (!ok) { drafts.clear(); return }
  for (const d of list) {
    await openFile(d.path).catch(() => {})
    const m = models.get(d.path)
    if (m && m.getValue() !== d.content) m.setValue(d.content)
  }
  BL.info('草稿已恢复,内容尚未保存到代码仓')
}

/* —— 生命周期 —— */
let resizeObserver = null
let markerListener = null
onMounted(async () => {
  await nextTick()
  // 装饰器 + 平台虚拟模块声明:要赶在建 model 之前配好, 否则首次诊断跑的是默认配置
  setupTypescriptEnv(monaco)
  editor.value = markRaw(monaco.editor.create(editorEl.value, {
    value: '',
    language: 'typescript',
    automaticLayout: false,
    theme: MONACO_THEME[settings.theme] || 'vs-dark',
    ...toMonacoOptions(settings),
  }))
  // 只取行列两个数, 不把 monaco 的 Position 对象丢进响应式
  editor.value.onDidChangeCursorPosition(e => {
    cursorPos.value = { lineNumber: e.position.lineNumber, column: e.position.column }
  })
  // 点行号左侧的符号边距 = 打/去断点(文档 2.1 断点标记渲染)
  editor.value.onMouseDown(e => {
    if (e.target?.type !== monaco.editor.MouseTargetType.GUTTER_GLYPH_MARGIN) return
    const line = e.target.position?.lineNumber
    if (line && activePath.value) dbg.toggleBreakpoint(activePath.value, line)
  })
  // 合并到下一帧再 layout:避免 ResizeObserver 回调里改布局又触发自身, 形成回环把页面卡死
  let roPending = false
  resizeObserver = new ResizeObserver(() => {
    if (roPending) return
    roPending = true
    requestAnimationFrame(() => { roPending = false; editor.value?.layout() })
  })
  resizeObserver.observe(editorEl.value)
  window.addEventListener('keydown', onKeyDown)

  // 诊断变化 → 刷新问题面板(Monaco 自带 TS/JS 语言服务, Python 无诊断)
  markerListener = monaco.editor.onDidChangeMarkers(() => refreshProblems())
  run.connect()

  await Promise.all([loadFunction(), loadTree(), loadRepoStatus(), loadOntologyNames()])
  refreshProblems()
  // 默认打开当前函数对应的代码文件
  const p = fn.value.code_file_path
  if (p) await openFile(p).catch(() => {})

  // 草稿:按分支分桶, 先问要不要恢复, 再开定时自动保存
  drafts.setBranch(repo.value.current_branch || repo.value.branch || 'master')
  await restoreDrafts()
  drafts.start(() => openFiles.value.map(f => ({
    path: f.path, dirty: f.dirty, content: models.get(f.path)?.getValue() ?? f.content,
  })))
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
  resizeObserver?.disconnect()
  markerListener?.dispose()
  run.close()
  models.forEach(m => m.dispose())
  models.clear()
  viewStates.clear()
  editor.value?.dispose()
})

/* 侧边栏显隐/宽度变化后, 编辑器要重新 layout */
watch([sidebarOpen, panelOpen], () => nextTick(() => editor.value?.layout()))

function baseName(p) { return String(p || '').split('/').pop() }
</script>

<style scoped>
.fnide {
  position: fixed; inset: 0; z-index: 1;
  display: flex; flex-direction: column;
  background: var(--ide-bg); color: var(--ide-text);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', sans-serif;
}
.bl-grow { flex: 1; }

/* 顶部 */
.fnide-top {
  flex-shrink: 0; height: 32px;
  display: flex; align-items: center; justify-content: space-between;
  background: var(--ide-bg-3); border-bottom: 1px solid var(--ide-border);
}
.fnide-top-r { display: flex; align-items: center; gap: 10px; padding-right: 10px; }
.fnide-fn { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--ide-text); }
.fnide-fx {
  width: 18px; height: 18px; border-radius: 3px; background: var(--ide-blue); color: #fff;
  display: inline-flex; align-items: center; justify-content: center;
  font-family: Consolas, monospace; font-size: 10px; font-weight: 700;
}
.fnide-fn-sub { color: var(--ide-text-dim); }
.fnide-sep { width: 1px; height: 14px; background: var(--ide-border); }
.fnide-btn {
  display: inline-flex; align-items: center; gap: 4px;
  height: 22px; padding: 0 8px; border-radius: 3px;
  border: 1px solid var(--ide-border); background: transparent;
  color: var(--ide-text); font-size: 11.5px; cursor: pointer;
}
.fnide-btn:hover { background: var(--ide-hover); }

/* 主体 */
.fnide-body { flex: 1; min-height: 0; display: flex; align-items: stretch; }
.fnide-expand {
  width: 18px; flex-shrink: 0; border: 0; cursor: pointer;
  background: var(--ide-bg-3); border-right: 1px solid var(--ide-border);
  color: var(--ide-text-dim);
}
.fnide-expand:hover { color: var(--ide-text-strong); }
.fnide-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }

/* 标签栏 */
.fnide-tabs {
  flex-shrink: 0; display: flex; align-items: stretch;
  height: 34px; background: var(--ide-bg-3); overflow-x: auto;
}
.fnide-tab {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 0 8px 0 10px; min-width: 0; max-width: 220px;
  background: var(--ide-tab-inactive); border-right: 1px solid var(--ide-border);
  font-size: 12px; color: var(--ide-text-dim); cursor: pointer; white-space: nowrap;
}
.fnide-tab.is-on { background: var(--ide-tab-active); color: var(--ide-text-strong); box-shadow: inset 0 2px 0 var(--ide-blue); }
.fnide-tab-ic { display: inline-flex; flex-shrink: 0; }
.fnide-tab-name { overflow: hidden; text-overflow: ellipsis; }
.fnide-tab-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--ide-text); flex-shrink: 0; }
.fnide-tab-x {
  display: inline-flex; align-items: center; justify-content: center;
  width: 16px; height: 16px; border-radius: 3px; flex-shrink: 0; opacity: .6;
}
.fnide-tab-x:hover { background: var(--ide-hover); opacity: 1; }

/* 编辑器 */
.fnide-editor-wrap { flex: 1; min-height: 0; position: relative; }
.fnide-editor { position: absolute; inset: 0; }
.fnide-blank {
  position: absolute; inset: 0; z-index: 2;
  background: var(--ide-bg); color: var(--ide-text-dim);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
}
.fnide-blank-logo {
  width: 48px; height: 48px; border-radius: 10px; background: var(--ide-blue); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-family: Consolas, monospace; font-size: 20px; font-weight: 700; margin-bottom: 6px;
}
.fnide-blank-t { font-size: 15px; color: var(--ide-text); }
.fnide-blank-s { font-size: 12px; }
.fnide-blank-k { display: flex; gap: 18px; margin-top: 10px; font-size: 11.5px; }
.fnide-blank-k b {
  display: inline-block; padding: 1px 5px; margin: 0 1px; border-radius: 3px;
  background: var(--ide-bg-4); border: 1px solid var(--ide-border-2);
  font-family: Consolas, monospace; font-weight: 400;
}

/* 底部面板骨架 */
.fnide-panel {
  flex-shrink: 0; height: 220px;
  border-top: 1px solid var(--ide-border); background: var(--ide-bg-2);
  display: flex; flex-direction: column;
}
.fnide-panel-tabs {
  flex-shrink: 0; height: 34px; display: flex; align-items: center; gap: 2px;
  padding: 0 6px; background: var(--ide-bg-3);
}
.fnide-panel-tab {
  display: inline-flex; align-items: center; gap: 5px;
  height: 26px; padding: 0 10px; border: 0; border-radius: 3px;
  background: transparent; color: var(--ide-text-dim); font-size: 12px; cursor: pointer;
}
.fnide-panel-tab:hover { color: var(--ide-text); }
.fnide-panel-tab.is-on { color: var(--ide-text-strong); box-shadow: inset 0 -2px 0 var(--ide-blue); }
.fnide-panel-btn {
  width: 26px; height: 26px; border: 0; border-radius: 3px; cursor: pointer;
  background: transparent; color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center;
}
.fnide-panel-btn:hover { background: var(--ide-hover); color: var(--ide-text-strong); }
.fnide-panel-body {
  flex: 1; min-height: 0; overflow: auto; padding: 12px;
  font-family: Consolas, Monaco, monospace; font-size: 12px; color: var(--ide-text-dim);
}

/* 断点与执行行装饰(非 scoped 也能生效, 因为 monaco 把节点挂在编辑器内部) */
:deep(.fnide-bp) {
  background: #e51400; border-radius: 50%;
  width: 10px !important; height: 10px !important; margin-left: 5px; margin-top: 4px;
}
:deep(.fnide-bp-unverified) { background: transparent; border: 1px solid #848484; }
/* 禁用 = 灰点;条件断点 = 带白心的红点;日志点 = 菱形, 与普通断点一眼可分 */
:deep(.fnide-bp-off) { background: #848484; }
:deep(.fnide-bp-cond) { box-shadow: inset 0 0 0 2px rgba(255, 255, 255, .85); }
:deep(.fnide-bp-log) { border-radius: 2px; transform: rotate(45deg); background: #d18616; }
:deep(.fnide-stackline) { background: rgba(255, 216, 0, .16); }
:deep(.fnide-stackarrow) {
  background: #ffd800; width: 0 !important; height: 0 !important;
  margin-left: 4px; margin-top: 3px;
  border-left: 8px solid #ffd800; border-top: 6px solid transparent; border-bottom: 6px solid transparent;
  border-radius: 0;
}

/* 状态栏 */
.fnide-status {
  flex-shrink: 0; height: 22px;
  display: flex; align-items: center; gap: 14px; padding: 0 10px;
  background: var(--ide-status); color: #fff; font-size: 11.5px;
}
.fnide-status-warn { color: #ffe8a3; }
.fnide-status-dbg { color: #ffd800; font-weight: 600; }
</style>
