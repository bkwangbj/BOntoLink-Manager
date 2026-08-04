<template>
  <div class="fnide" :data-ide-theme="settings.theme">
    <!-- ===== 顶部:菜单栏 + 函数标识 ===== -->
    <header class="fnide-top">
      <IdeMenuBar :editor="editor" :language="fnLanguage" :settings="settings" :panel="panel"
                  @open-settings="settingsOpen = true"
                  @patch-settings="patchSettings"
                  @toggle-panel-tab="togglePanelTab"
                  @toggle-panel="onTogglePanel"
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
                  :tree="tree" :repo="repo" :active-path="activePath" :open-files="openFiles"
                  @open-file="openFile" @goto="gotoLine" @reload-tree="loadTree"
                  @collapse="sidebarOpen = false" @pushed="loadRepoStatus" />
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

        <!-- 底部面板:P7 落地, 这里先按文档给出标签条骨架 -->
        <div v-if="panelOpen" class="fnide-panel">
          <div class="fnide-panel-tabs">
            <button v-for="t in visiblePanelTabs" :key="t.k"
                    :class="['fnide-panel-tab', panelActive === t.k && 'is-on']"
                    @click="panelActive = t.k">
              <span v-if="panel.showIcons" v-html="BL.icon(t.icon, 11)"></span>{{ t.label }}
            </button>
            <span class="bl-grow"></span>
            <button class="fnide-panel-btn" title="隐藏面板 (Ctrl+J)" @click="panelOpen = false"
                    v-html="BL.icon('chevronDown', 12)"></button>
          </div>
          <div class="fnide-panel-body">
            {{ panelHint }}
          </div>
        </div>
      </main>
    </div>

    <!-- ===== 状态栏 ===== -->
    <footer class="fnide-status">
      <span>{{ repo.ready ? (repo.current_branch || repo.branch) : '仓库不可用' }}</span>
      <span v-if="aheadCount > 0" class="fnide-status-warn" title="待推送提交数">↑{{ aheadCount }}</span>
      <span v-if="saving">保存中…</span>
      <span class="bl-grow"></span>
      <span v-if="activePath" class="bl-mono">{{ activePath }}</span>
      <span v-if="cursorPos">行 {{ cursorPos.lineNumber }},列 {{ cursorPos.column }}</span>
      <span>{{ langLabel }}</span>
      <span>{{ settings.tabSize }} 空格</span>
    </footer>

    <IdeSettingsModal v-model:open="settingsOpen" :settings="settings" @apply="applySettingsPatch" />
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
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as monaco from 'monaco-editor'
import { BL } from '@/lib/bl.js'
import { functionApi, fnRepoApi } from '@/api'
import IdeMenuBar from './IdeMenuBar.vue'
import IdeSideBar from './IdeSideBar.vue'
import IdeSettingsModal from './IdeSettingsModal.vue'
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
const openFiles = ref([])          // [{ path, content, dirty, model, viewState }]
const activePath = ref('')
const editorEl = ref(null)
const editor = ref(null)
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
const panelActive = ref('debug')
const visiblePanelTabs = computed(() => PANEL_TABS.filter(t => panel[t.k]))
const panelHint = computed(() =>
  `「${(PANEL_TABS.find(t => t.k === panelActive.value) || {}).label}」的业务能力在 P7 落地,当前仅面板骨架与显隐联动。`)

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
  const model = monaco.editor.createModel(content, langOfPath(path))
  model.onDidChangeContent(() => {
    const f = openFiles.value.find(x => x.path === path)
    if (f) { f.dirty = model.getValue() !== f.content }
  })
  openFiles.value.push({ path, content, dirty: false, model, viewState: null })
  activate(path)
}

function activate(path) {
  if (!editor.value) return
  const cur = openFiles.value.find(f => f.path === activePath.value)
  if (cur) cur.viewState = editor.value.saveViewState()
  const next = openFiles.value.find(f => f.path === path)
  if (!next) return
  activePath.value = path
  editor.value.setModel(next.model)
  if (next.viewState) editor.value.restoreViewState(next.viewState)
  editor.value.focus()
}

async function closeFile(path) {
  const f = openFiles.value.find(x => x.path === path)
  if (!f) return
  if (f.dirty) {
    const ok = await BL.confirm({ title: '关闭文件', content: `「${baseName(path)}」有未保存的修改,确定关闭?`, okText: '关闭不保存' })
    if (!ok) return
  }
  f.model?.dispose()
  openFiles.value = openFiles.value.filter(x => x.path !== path)
  if (activePath.value === path) {
    const next = openFiles.value[openFiles.value.length - 1]
    if (next) activate(next.path)
    else { activePath.value = ''; editor.value?.setModel(null) }
  }
}

function gotoLine({ path, line }) {
  openFile(path).then(() => {
    if (!editor.value) return
    editor.value.revealLineInCenter(line)
    editor.value.setPosition({ lineNumber: line, column: 1 })
    editor.value.focus()
  })
}

/* —— 保存:写工作区 + 服务端本地提交(不推送) —— */
async function saveActive() {
  const f = openFiles.value.find(x => x.path === activePath.value)
  if (!f || !editor.value) return
  const content = f.model.getValue()
  if (!f.dirty) { BL.info('没有需要保存的修改'); return }
  saving.value = true
  try {
    const r = await fnRepoApi.write(f.path, content, `edit: 更新 ${f.path}`)
    f.content = content
    f.dirty = false
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
function onPalette() { BL.info('命令面板在 P9「体验增强工具集」落地') }
function onHelp() { BL.info('内置文档中心在 P9 落地;当前可参考《本体管理系统-函数Functions.pdf》') }
function backToList() {
  router.push({ path: '/resources/functions', query: { openId: route.params.id } })
}

/* —— 快捷键:保存 / 缩放 —— */
function onKeyDown(e) {
  if (!(e.ctrlKey || e.metaKey)) return
  if (e.key === 's' || e.key === 'S') { e.preventDefault(); saveActive() }
  else if (e.key === '=' || e.key === '+') { e.preventDefault(); patchSettings({ fontSize: clampFont(settings.fontSize + 1) }) }
  else if (e.key === '-') { e.preventDefault(); patchSettings({ fontSize: clampFont(settings.fontSize - 1) }) }
  else if (e.code === 'Numpad0') { e.preventDefault(); patchSettings({ fontSize: 14 }) }
  else if (e.key === 'j' || e.key === 'J') { e.preventDefault(); onTogglePanel() }
}
function clampFont(n) { return Math.max(8, Math.min(20, Number(n) || 14)) }

/* —— 生命周期 —— */
let resizeObserver = null
onMounted(async () => {
  await nextTick()
  editor.value = monaco.editor.create(editorEl.value, {
    value: '',
    language: 'typescript',
    automaticLayout: false,
    theme: MONACO_THEME[settings.theme] || 'vs-dark',
    ...toMonacoOptions(settings),
  })
  editor.value.onDidChangeCursorPosition(e => { cursorPos.value = e.position })
  resizeObserver = new ResizeObserver(() => editor.value?.layout())
  resizeObserver.observe(editorEl.value)
  window.addEventListener('keydown', onKeyDown)

  await Promise.all([loadFunction(), loadTree(), loadRepoStatus()])
  // 默认打开当前函数对应的代码文件
  const p = fn.value.code_file_path
  if (p) openFile(p).catch(() => {})
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
  resizeObserver?.disconnect()
  openFiles.value.forEach(f => f.model?.dispose())
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

/* 状态栏 */
.fnide-status {
  flex-shrink: 0; height: 22px;
  display: flex; align-items: center; gap: 14px; padding: 0 10px;
  background: var(--ide-status); color: #fff; font-size: 11.5px;
}
.fnide-status-warn { color: #ffe8a3; }
</style>
