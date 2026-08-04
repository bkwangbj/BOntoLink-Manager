<template>
  <div class="idemb" ref="rootEl">
    <div v-for="m in MENUS" :key="m.k" class="idemb-slot">
      <button :class="['idemb-btn', openKey === m.k && 'is-open']" @click="toggle(m.k)">{{ m.title }}</button>

      <div v-if="openKey === m.k" class="ide-menu-pop" :style="{ width: m.width + 'px' }" @click.stop>
        <template v-for="(it, i) in itemsOf(m)" :key="i">
          <div v-if="it.sep" class="ide-menu-sep"></div>
          <div v-else-if="it.groupTitle" class="ide-menu-group-title">{{ it.groupTitle }}</div>
          <div v-else
               :class="['ide-menu-item', it.disabled && 'is-disabled']"
               @click="!it.disabled && pick(m, it)">
            <!-- 勾选位:单选/多选类占位固定宽度, 保证文字左对齐 -->
            <span v-if="it.check !== undefined" class="ide-menu-check" :class="it.check ? (it.dimCheck ? 'is-dim' : 'is-on') : ''">
              <span v-if="it.check" v-html="BL.icon('check', 11)"></span>
            </span>
            <span v-if="it.dot" class="idemb-dot" :style="{ background: it.dot }"></span>
            <span class="ide-menu-label">{{ it.label }}</span>
            <span v-if="it.keysText" class="ide-keys-text">{{ it.keysText }}</span>
            <span v-else-if="it.keys" class="ide-keys">
              <template v-for="(k, ki) in it.keys" :key="ki">
                <span v-if="ki" class="ide-key-plus">+</span><span class="ide-key">{{ k }}</span>
              </template>
            </span>
            <span v-if="it.submenu" class="ide-submenu-arrow" v-html="BL.icon('chevronRight', 11)"></span>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * IDE 顶部菜单栏 (文档「菜单区」7 个菜单)
 *
 * - 语言:纯状态展示, 全部禁用, 当前语言带灰色对钩
 * - 编辑 / 选择:全部走 Monaco 原生命令 (editor.getAction().run(), 不存在时降级 trigger)
 * - 外观:主题 / 显示 / 标签栏 / 显示空格 / 缩放, 改动即时生效并持久化
 * - 运行:本期只做 UI 与禁用态 (调试内核在 P7/P8)
 * - 终端:底部面板四个标签的显隐勾选, 与底部设置下拉共用同一份状态
 * - 帮助:命令面板 / 帮助文档
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  /** monaco 编辑器实例 (未就绪时菜单项点击静默无响应, 不报错) */
  editor: { type: Object, default: null },
  /** 当前语言: 1=Python 2=TypeScript */
  language: { type: Number, default: 2 },
  /** 外观相关状态 */
  settings: { type: Object, required: true },
  /** 底部面板标签显隐 { problems, output, debug, terminal, showIcons } */
  panel: { type: Object, required: true }
})
const emit = defineEmits(['open-settings', 'patch-settings', 'toggle-panel-tab', 'toggle-panel', 'command-palette', 'help'])

const openKey = ref('')
const rootEl = ref(null)

const LANG_NAME = computed(() => (Number(props.language) === 1 ? 'Python' : 'TypeScript'))

const MENUS = computed(() => [
  { k: 'lang', title: `语言（${LANG_NAME.value}）`, width: 240 },
  { k: 'edit', title: '编辑(E)', width: 225 },
  { k: 'select', title: '选择(S)', width: 268 },
  { k: 'view', title: '外观(V)', width: 246 },
  { k: 'run', title: '运行(R)', width: 260 },
  { k: 'term', title: '终端(T)', width: 260 },
  { k: 'help', title: '帮助(H)', width: 240 },
])

function itemsOf(m) {
  switch (m.k) {
    case 'lang': return LANG_ITEMS.value
    case 'edit': return EDIT_ITEMS
    case 'select': return SELECT_ITEMS.value
    case 'view': return VIEW_ITEMS.value
    case 'run': return RUN_ITEMS
    case 'term': return TERM_ITEMS.value
    case 'help': return HELP_ITEMS
    default: return []
  }
}

/* —— 语言:全禁用, 当前项带灰对钩 —— */
const LANG_ITEMS = computed(() => [
  { label: 'TypeScript', disabled: true, check: Number(props.language) === 2, dimCheck: true },
  { label: 'Python', disabled: true, check: Number(props.language) === 1, dimCheck: true },
])

/* —— 编辑 —— */
const EDIT_ITEMS = [
  { label: '撤销', keys: ['Ctrl', 'Z'], cmd: 'undo' },
  { label: '恢复', keys: ['Ctrl', 'Y'], cmd: 'redo' },
  { sep: true },
  { label: '剪切', keys: ['Ctrl', 'X'], clip: 'cut' },
  { label: '复制', keys: ['Ctrl', 'C'], clip: 'copy' },
  { label: '粘贴', keys: ['Ctrl', 'V'], clip: 'paste' },
  { sep: true },
  { label: '查找', keys: ['Ctrl', 'F'], cmd: 'actions.find' },
  { label: '替换', keys: ['Ctrl', 'H'], cmd: 'editor.action.startFindReplaceAction' },
  { sep: true },
  { label: '切换行注释', keys: ['Ctrl', '/'], cmd: 'editor.action.commentLine' },
  { label: '切换块注释', keys: ['Shift', 'Alt', 'A'], cmd: 'editor.action.blockComment' },
  { sep: true },
  { label: 'Emmet: 展开缩写', keys: ['Tab'], cmd: 'editor.emmet.action.expandAbbreviation' },
]

/* —— 选择 —— */
const SELECT_ITEMS = computed(() => [
  { label: '全选', keys: ['Ctrl', 'A'], cmd: 'editor.action.selectAll' },
  { label: '扩大选区', keys: ['Shift', 'Alt', '→'], cmd: 'editor.action.smartSelect.expand' },
  { label: '缩小选区', keys: ['Shift', 'Alt', '←'], cmd: 'editor.action.smartSelect.shrink' },
  { sep: true },
  { label: '向上复制一行', keys: ['Shift', 'Alt', '↑'], cmd: 'editor.action.copyLinesUpAction' },
  { label: '向下复制一行', keys: ['Shift', 'Alt', '↓'], cmd: 'editor.action.copyLinesDownAction' },
  { label: '向上移动一行', keys: ['Alt', '↑'], cmd: 'editor.action.moveLinesUpAction' },
  { label: '向下移动一行', keys: ['Alt', '↓'], cmd: 'editor.action.moveLinesDownAction' },
  { sep: true },
  { label: '在上面添加光标', keys: ['Ctrl', 'Alt', '↑'], cmd: 'editor.action.insertCursorAbove' },
  { label: '在下面添加光标', keys: ['Ctrl', 'Alt', '↓'], cmd: 'editor.action.insertCursorBelow' },
  { label: '在行尾添加光标', keys: ['Shift', 'Alt', 'I'], cmd: 'editor.action.insertCursorAtEndOfEachLineSelected' },
  { sep: true },
  { label: '添加下一个匹配项', keys: ['Ctrl', 'D'], cmd: 'editor.action.addSelectionToNextFindMatch' },
  { label: '添加上一个匹配项', cmd: 'editor.action.addSelectionToPreviousFindMatch' },
  { label: '选择所有匹配项', keys: ['Ctrl', 'Shift', 'L'], cmd: 'editor.action.selectHighlights' },
  { sep: true },
  // 列选择模式:改配置项而非命令, 保证 100% 生效 (文档 3.5)
  { label: '列选择模式', check: !!props.settings.columnSelection, patch: { columnSelection: !props.settings.columnSelection } },
])

/* —— 外观 —— */
const THEMES = [
  { v: 'dark', label: 'Visual Studio Dark（暗色）', dot: '#1e1e1e' },
  { v: 'light', label: 'Visual Studio（浅色）', dot: '#ffffff' },
  { v: 'hc', label: 'High Contrast Dark（高对比黑）', dot: '#000000' },
]
const WHITESPACE = [
  { v: 'none', label: '不显示' }, { v: 'boundary', label: '边界' },
  { v: 'selection', label: '选中时' }, { v: 'all', label: '全部' },
]
const VIEW_ITEMS = computed(() => {
  const s = props.settings
  const out = [{ label: '通用设置...', action: 'settings' }, { sep: true }, { groupTitle: '主题' }]
  THEMES.forEach(t => out.push({ label: t.label, dot: t.dot, check: s.theme === t.v, patch: { theme: t.v } }))
  out.push({ sep: true }, { groupTitle: '显示' },
    { label: '缩略图', check: !!s.minimap, patch: { minimap: !s.minimap } },
    { label: '粘滞滚动', check: !!s.stickyScroll, patch: { stickyScroll: !s.stickyScroll } },
    { label: '显示控制字符', check: !!s.renderControlCharacters, patch: { renderControlCharacters: !s.renderControlCharacters } },
    { sep: true }, { groupTitle: '标签栏' },
    { label: '单标签', check: s.tabsMode === 'single', patch: { tabsMode: 'single' } },
    { label: '多标签', check: s.tabsMode === 'multi', patch: { tabsMode: 'multi' } },
    { sep: true }, { groupTitle: '显示空格' })
  WHITESPACE.forEach(w => out.push({ label: w.label, check: s.renderWhitespace === w.v, patch: { renderWhitespace: w.v } }))
  out.push({ sep: true }, { groupTitle: '缩放' },
    { label: '放大', keysText: 'Ctrl + =', action: 'zoom-in' },
    { label: '缩小', keysText: 'Ctrl + -', action: 'zoom-out' },
    { label: '重置缩放', keysText: 'Ctrl + NumPad0', action: 'zoom-reset' })
  return out
})

/* —— 运行:本期仅 UI 与初始禁用态 (文档「当前阶段仅实现菜单 UI」) —— */
const RUN_ITEMS = [
  { label: '启动调试', keysText: 'F5', todo: true },
  { label: '以非调试模式运行', keysText: 'Ctrl + F5', todo: true },
  { label: '停止调试', keysText: 'Shift + F5', disabled: true },
  { label: '重启调试', keysText: 'Ctrl + Shift + F5', disabled: true },
  { sep: true },
  { label: '逐过程', keysText: 'F10', disabled: true },
  { label: '单步执行', keysText: 'F11', disabled: true },
  { label: '单步停止', keysText: 'Shift + F11', disabled: true },
  { label: '继续', keysText: 'F5', disabled: true },
  { sep: true },
  { label: '切换断点', keysText: 'F9', todo: true },
  { label: '新建断点', submenu: true, todo: true },
  { sep: true },
  { label: '启用所有断点', todo: true },
  { label: '禁用所有断点', todo: true },
  { label: '删除所有断点', todo: true },
]

/* —— 终端:四个标签显隐(多选开关, 点了不关菜单) —— */
const TERM_ITEMS = computed(() => {
  const p = props.panel
  return [
    { groupTitle: '终端' },
    { label: '问题', keysText: 'Ctrl + Shift + M', check: !!p.problems, tab: 'problems', keepOpen: true },
    { label: '输出', keysText: 'Ctrl + Shift + U', check: !!p.output, tab: 'output', keepOpen: true },
    { label: '调试控制台', keysText: 'Ctrl + Shift + Y', check: !!p.debug, tab: 'debug', keepOpen: true },
    { label: '终端', keysText: 'Ctrl + `', check: !!p.terminal, tab: 'terminal', keepOpen: true },
    { sep: true },
    { label: '显示图标', check: !!p.showIcons, tab: 'showIcons', keepOpen: true },
    { label: '"隐藏" 面板', keysText: 'Ctrl + J', action: 'toggle-panel' },
  ]
})

const HELP_ITEMS = [
  { label: '显示所有命令', keysText: 'Ctrl + Shift + P', action: 'palette' },
  { label: '帮助文档', action: 'help' },
]

/* —— 交互 —— */
function toggle(k) { openKey.value = openKey.value === k ? '' : k }
function close() { openKey.value = '' }
function onDocClick(e) { if (rootEl.value && !rootEl.value.contains(e.target)) close() }
onMounted(() => window.addEventListener('click', onDocClick))
onUnmounted(() => window.removeEventListener('click', onDocClick))

function pick(menu, it) {
  if (it.patch) emit('patch-settings', it.patch)
  else if (it.tab) emit('toggle-panel-tab', it.tab)
  else if (it.cmd) runCommand(it.cmd)
  else if (it.clip) runClipboard(it.clip)
  else if (it.action === 'settings') emit('open-settings')
  else if (it.action === 'zoom-in') emit('patch-settings', { fontSize: clampFont(props.settings.fontSize + 1) })
  else if (it.action === 'zoom-out') emit('patch-settings', { fontSize: clampFont(props.settings.fontSize - 1) })
  else if (it.action === 'zoom-reset') emit('patch-settings', { fontSize: 14 })
  else if (it.action === 'toggle-panel') emit('toggle-panel')
  else if (it.action === 'palette') emit('command-palette')
  else if (it.action === 'help') emit('help')
  else if (it.todo) BL.info(`「${it.label}」的调试内核在 P7 / P8 落地,当前仅菜单 UI`)
  // 多选开关点了不收起菜单, 支持连续操作 (文档 2.4)
  if (!it.keepOpen) close()
}

/** 字号限制 8~20px(文档缩放规则) */
function clampFont(n) { return Math.max(8, Math.min(20, Number(n) || 14)) }

/** 优先 getAction().run(),命令不存在时降级 trigger */
function runCommand(cmd) {
  const ed = props.editor
  if (!ed) return
  ed.focus()
  const action = ed.getAction?.(cmd)
  if (action) { action.run(); return }
  try { ed.trigger('menu', cmd, null) } catch { /* 命令不存在:静默 */ }
}

/** 剪贴板独立实现, 不依赖编辑器内置命令(避免失焦导致失效) */
async function runClipboard(kind) {
  const ed = props.editor
  if (!ed) return
  ed.focus()
  const sel = ed.getSelection()
  const model = ed.getModel()
  if (!model) return
  try {
    if (kind === 'copy' || kind === 'cut') {
      const text = model.getValueInRange(sel)
      if (!text) return
      await navigator.clipboard.writeText(text)
      if (kind === 'cut') ed.executeEdits('menu-cut', [{ range: sel, text: '' }])
    } else {
      const text = await navigator.clipboard.readText()
      ed.executeEdits('menu-paste', [{ range: sel, text }])
    }
  } catch (e) {
    console.warn('[ide] 剪贴板操作失败(可能无权限):', e)
  }
}
</script>

<style scoped>
.idemb { display: flex; align-items: stretch; height: 32px; }
.idemb-slot { position: relative; }
.idemb-btn {
  height: 32px; padding: 0 12px;
  border: 0; background: transparent; cursor: pointer;
  color: var(--ide-text); font-size: 12.5px; white-space: nowrap;
}
.idemb-btn:hover, .idemb-btn.is-open { background: var(--ide-hover); }
.idemb-dot {
  width: 12px; height: 12px; flex-shrink: 0;
  border: 1px solid var(--ide-border-2); border-radius: 2px;
}
</style>
