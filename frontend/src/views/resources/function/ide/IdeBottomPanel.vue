<template>
  <div class="idebp" :style="{ height: maxed ? '80vh' : height + 'px' }">
    <div class="idebp-drag" :class="resizing && 'is-resizing'" @mousedown="onDragStart"></div>

    <!-- 标签栏 + 工具栏 -->
    <div class="idebp-tabs">
      <button v-for="t in visibleTabs" :key="t.k"
              :class="['idebp-tab', active === t.k && 'is-on']"
              @click="$emit('update:active', t.k)">
        <span v-if="panel.showIcons" v-html="BL.icon(t.icon, 11)"></span>{{ t.label }}
        <!-- 角标只数错误和警告:提示级(如未导入的本体类型)不该让人以为代码有 40 处毛病 -->
        <span v-if="t.k === 'problems' && realProblemCount" class="idebp-badge">{{ realProblemCount }}</span>
        <span v-else-if="t.k === 'problems' && problems.length" class="idebp-badge is-info">{{ problems.length }}</span>
      </button>

      <span class="bl-grow"></span>

      <div class="idebp-filter">
        <input class="idebp-input" v-model="filter" placeholder="筛选器(例如 text, !exclude)" />
      </div>
      <button class="idebp-btn" title="搜索(跳到下一个匹配)" @click="scrollToMatch" v-html="BL.icon('search', 12)"></button>
      <button class="idebp-btn" title="清空" @click="clearCurrent" v-html="BL.icon('trash', 12)"></button>
      <span class="idebp-sep"></span>
      <div class="idebp-set-wrap" ref="setWrap">
        <button class="idebp-btn" title="设置" @click="setOpen = !setOpen" v-html="BL.icon('settings', 12)"></button>
        <!-- 与顶部「终端」菜单共用同一份勾选状态(文档 4.1 状态联动) -->
        <div v-if="setOpen" class="idebp-set-pop">
          <div v-for="t in TABS" :key="t.k" class="idebp-set-item" @click="$emit('toggle-tab', t.k)">
            <span class="idebp-set-check">
              <span v-if="panel[t.k]" v-html="BL.icon('check', 11)"></span>
            </span>{{ t.label }}
          </div>
          <div class="idebp-set-sep"></div>
          <div class="idebp-set-item" @click="$emit('toggle-tab', 'showIcons')">
            <span class="idebp-set-check"><span v-if="panel.showIcons" v-html="BL.icon('check', 11)"></span></span>显示图标
          </div>
          <div class="idebp-set-item" @click="setOpen = false; $emit('hide')">
            <span class="idebp-set-check"></span>"隐藏" 面板
          </div>
        </div>
      </div>
      <button class="idebp-btn" :title="maxed ? '还原' : '最大化'" @click="maxed = !maxed"
              v-html="BL.icon(maxed ? 'minimize' : 'maximize', 12)"></button>
      <button class="idebp-btn" title="隐藏面板 (Ctrl+J)" @click="$emit('hide')" v-html="BL.icon('chevronDown', 12)"></button>
    </div>

    <!-- 内容区 -->
    <div class="idebp-body" ref="bodyEl" @scroll="onScroll">
      <!-- 问题:来自 Monaco 的语法/类型标记 -->
      <template v-if="active === 'problems'">
        <div v-if="!filteredProblems.length" class="idebp-empty">未检测到问题</div>
        <div v-else-if="ontologyHintCount" class="idebp-prob-tip">
          <span v-html="BL.icon('info', 12)"></span>
          有 {{ ontologyHintCount }} 处引用了尚未导入的本体类型 —— 这不是代码错误,在左侧「资源导入」面板导入对应本体对象即可消除。
        </div>
        <div v-for="(p, i) in filteredProblems" :key="i" class="idebp-prob" @click="$emit('goto', { path: p.path, line: p.line })">
          <span class="idebp-prob-ic" :style="{ color: SEVERITY[p.severity].color }"
                v-html="BL.icon(SEVERITY[p.severity].icon, 12)"></span>
          <span class="idebp-prob-msg">{{ p.message }}</span>
          <span class="idebp-prob-loc">{{ p.path }}:{{ p.line }}:{{ p.column }}</span>
        </div>
      </template>

      <!-- 终端:带输入行 -->
      <template v-else-if="active === 'terminal'">
        <div v-for="l in filteredLines" :key="l.id" class="idebp-line" :class="'is-' + l.stream">
          <span v-for="(seg, si) in parseAnsi(l.text)" :key="si"
                :style="{ color: seg.color || undefined, fontWeight: seg.bold ? 600 : undefined }">{{ seg.text }}</span>
        </div>
        <div class="idebp-prompt">
          <span class="idebp-prompt-sign">$</span>
          <input class="idebp-prompt-input" v-model="cmd" :disabled="!connected"
                 :placeholder="connected ? '输入命令后回车,在代码仓工作区执行' : '运行通道未连接'"
                 @keydown.enter="submitCmd" @keydown.up.prevent="historyPrev" @keydown.down.prevent="historyNext"
                 @keydown.ctrl.c="$emit('stop')" />
          <button v-if="running" class="idebp-stop" title="停止 (Ctrl+C)" @click="$emit('stop')">
            <span v-html="BL.icon('stop', 11)"></span>停止
          </button>
        </div>
      </template>

      <!-- 输出 / 调试控制台:共用运行流 -->
      <template v-else>
        <div v-if="!filteredLines.length" class="idebp-empty">
          {{ active === 'debug' ? '调试控制台:启动调试后显示调试日志与表达式求值结果(DAP 内核在 P8 落地)' : '暂无输出。运行菜单里的「以非调试模式运行」会把日志推到这里。' }}
        </div>
        <div v-for="l in filteredLines" :key="l.id" class="idebp-line" :class="'is-' + l.stream">
          <span v-for="(seg, si) in parseAnsi(l.text)" :key="si"
                :style="{ color: seg.color || undefined, fontWeight: seg.bold ? 600 : undefined }">{{ seg.text }}</span>
        </div>
        <!-- 调试控制台:只有停在断点处才允许求值(文档 3. 交互规则) -->
        <div v-if="active === 'debug'" class="idebp-prompt" :class="!paused && 'is-disabled'">
          <span class="idebp-prompt-sign">&gt;</span>
          <input class="idebp-prompt-input" v-model="expr" :disabled="!paused"
                 :placeholder="paused ? '输入表达式,回车求值(当前栈帧上下文)' : '调试暂停时方可执行表达式'"
                 @keydown.enter="submitExpr" @keydown.up.prevent="exprPrev" @keydown.down.prevent="exprNext" />
        </div>
      </template>
    </div>

    <!-- 状态条 -->
    <div class="idebp-foot">
      <span :class="['idebp-dot', connected ? 'is-ok' : 'is-off']"></span>
      {{ connected ? (running ? '运行中…' : '运行通道已连接') : (connError || '运行通道未连接') }}
      <span class="bl-grow"></span>
      <span v-if="active !== 'problems'">{{ filteredLines.length }} / {{ lines.length }} 行</span>
      <span v-else>{{ filteredProblems.length }} / {{ problems.length }} 条</span>
    </div>
  </div>
</template>

<script setup>
/**
 * 底部面板 (P7 · 文档「底部面板」+「底部面板核心功能」)
 *
 * 四个标签:
 * - 问题:直接取 Monaco 的 marker(TS/JS 有真实语法与类型诊断;Python 无语言服务,恒为空)
 * - 输出:非调试运行的 stdout/stderr 流,支持 ANSI 着色与智能滚动
 * - 调试控制台:P8 的 DAP 内核落地前,复用运行流并把输入框按文档置灰
 * - 终端:在代码仓工作区执行命令,带历史回溯
 *
 * 工具栏筛选支持 `!` 排除语法(文档三、(1));标签显隐与顶部「终端」菜单共用一份状态。
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { parseAnsi } from './useFnRun.js'

const props = defineProps({
  active: { type: String, default: 'output' },
  panel: { type: Object, required: true },
  lines: { type: Array, default: () => [] },
  problems: { type: Array, default: () => [] },
  connected: { type: Boolean, default: false },
  connError: { type: String, default: '' },
  running: { type: Boolean, default: false },
  /** 调试是否停在断点处 —— 决定调试控制台能否求值 */
  paused: { type: Boolean, default: false },
})
const emit = defineEmits(['update:active', 'toggle-tab', 'hide', 'stop', 'goto', 'run-command', 'clear', 'evaluate'])

const TABS = [
  { k: 'problems', label: '问题', icon: 'warning' },
  { k: 'output', label: '输出', icon: 'list' },
  { k: 'debug', label: '调试控制台', icon: 'terminal' },
  { k: 'terminal', label: '终端', icon: 'terminal' },
]
const SEVERITY = {
  8: { icon: 'error', color: '#f14c4c' },     // MarkerSeverity.Error
  4: { icon: 'warning', color: '#cca700' },   // Warning
  2: { icon: 'info', color: '#3794ff' },      // Info
  1: { icon: 'info', color: '#3794ff' },      // Hint
}
const visibleTabs = computed(() => TABS.filter(t => props.panel[t.k]))

/* —— 筛选:支持 !排除 —— */
const filter = ref('')
function matches(text) {
  const raw = filter.value.trim()
  if (!raw) return true
  const terms = raw.split(/\s+/)
  for (const t of terms) {
    if (t.startsWith('!')) {
      const k = t.slice(1).toLowerCase()
      if (k && String(text).toLowerCase().includes(k)) return false
    } else if (!String(text).toLowerCase().includes(t.toLowerCase())) {
      return false
    }
  }
  return true
}
const filteredLines = computed(() => props.lines.filter(l => matches(l.text)))
const filteredProblems = computed(() => props.problems.filter(p => matches(`${p.message} ${p.path}`)))
/** 错误 + 警告数(严重度 >= 4);提示级不计入角标 */
const realProblemCount = computed(() => props.problems.filter(p => Number(p.severity) >= 4).length)
/** 未导入本体类型的提示条数 —— 用来在列表顶部给一句可操作的说明 */
const ontologyHintCount = computed(() => props.problems.filter(p => p.kind === 'ontology-missing').length)

/* —— 智能滚动:用户往上翻后暂停自动滚动, 回到底部再恢复 —— */
const bodyEl = ref(null)
const stickBottom = ref(true)
function onScroll() {
  const el = bodyEl.value
  if (!el) return
  stickBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 24
}
watch(() => props.lines.length, () => {
  if (!stickBottom.value) return
  nextTick(() => { const el = bodyEl.value; if (el) el.scrollTop = el.scrollHeight })
})
watch(() => props.active, () => {
  stickBottom.value = true
  nextTick(() => { const el = bodyEl.value; if (el) el.scrollTop = el.scrollHeight })
})
function scrollToMatch() {
  const el = bodyEl.value
  if (!el) return
  const hit = el.querySelector('.idebp-line, .idebp-prob')
  if (hit) hit.scrollIntoView({ block: 'center' })
}
function clearCurrent() {
  if (props.active === 'problems') BL.info('问题列表由编辑器诊断实时生成,无需手动清空')
  else emit('clear')
}

/* —— 终端输入与历史 —— */
const cmd = ref('')
const history = ref([])
let historyIdx = -1
function submitCmd() {
  const c = cmd.value.trim()
  if (!c) return
  history.value.push(c)
  if (history.value.length > 50) history.value.shift()
  historyIdx = history.value.length
  emit('run-command', c)
  cmd.value = ''
}
function historyPrev() {
  if (!history.value.length) return
  historyIdx = Math.max(0, historyIdx - 1)
  cmd.value = history.value[historyIdx] || ''
}
function historyNext() {
  if (!history.value.length) return
  historyIdx = Math.min(history.value.length, historyIdx + 1)
  cmd.value = history.value[historyIdx] || ''
}

/* —— 调试控制台表达式历史 —— */
const expr = ref('')
const exprHistory = ref([])
let exprIdx = -1
function submitExpr() {
  const e = expr.value.trim()
  if (!e || !props.paused) return
  exprHistory.value.push(e)
  if (exprHistory.value.length > 50) exprHistory.value.shift()
  exprIdx = exprHistory.value.length
  emit('evaluate', e)
  expr.value = ''
}
function exprPrev() {
  if (!exprHistory.value.length) return
  exprIdx = Math.max(0, exprIdx - 1)
  expr.value = exprHistory.value[exprIdx] || ''
}
function exprNext() {
  if (!exprHistory.value.length) return
  exprIdx = Math.min(exprHistory.value.length, exprIdx + 1)
  expr.value = exprHistory.value[exprIdx] || ''
}

/* —— 设置下拉 —— */
const setOpen = ref(false)
const setWrap = ref(null)
function onDocClick(e) { if (setWrap.value && !setWrap.value.contains(e.target)) setOpen.value = false }
onMounted(() => window.addEventListener('click', onDocClick))
onUnmounted(() => window.removeEventListener('click', onDocClick))

/* —— 高度:拖拽 + 最大化 —— */
const maxed = ref(false)
const height = ref(Number(localStorage.getItem('bl.ide.panel.h')) || 220)
const resizing = ref(false)
let startY = 0, startH = 0
function onDragStart(e) {
  if (maxed.value) return
  resizing.value = true; startY = e.clientY; startH = height.value
  document.body.style.cursor = 'row-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  height.value = Math.max(120, Math.min(window.innerHeight - 200, startH + (startY - e.clientY)))
}
function onDragEnd() {
  resizing.value = false
  localStorage.setItem('bl.ide.panel.h', String(height.value))
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}
onUnmounted(() => {
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
})
</script>

<style scoped>
.idebp {
  position: relative; flex-shrink: 0;
  border-top: 1px solid var(--ide-border); background: var(--ide-bg-2);
  display: flex; flex-direction: column; min-height: 0;
}
.idebp-drag {
  position: absolute; left: 0; right: 0; top: -2px; height: 5px;
  cursor: row-resize; z-index: 5; transition: background-color .15s;
}
.idebp-drag:hover, .idebp-drag.is-resizing { background: var(--ide-blue); }
.bl-grow { flex: 1; }

.idebp-tabs {
  flex-shrink: 0; height: 34px; display: flex; align-items: center; gap: 2px;
  padding: 0 6px; background: var(--ide-bg-3);
}
.idebp-tab {
  display: inline-flex; align-items: center; gap: 5px;
  height: 26px; padding: 0 10px; border: 0; border-radius: 3px;
  background: transparent; color: var(--ide-text-dim); font-size: 12px; cursor: pointer;
}
.idebp-tab:hover { color: var(--ide-text); }
.idebp-tab.is-on { color: var(--ide-text-strong); box-shadow: inset 0 -2px 0 var(--ide-blue); }
.idebp-badge {
  min-width: 15px; padding: 0 4px; border-radius: 7px;
  background: #f14c4c; color: #fff; font-size: 10px; line-height: 15px; text-align: center;
}
.idebp-badge.is-info { background: #3794ff; }
.idebp-filter { width: 190px; }
.idebp-input {
  width: 100%; height: 22px; padding: 0 6px;
  background: var(--ide-bg); border: 1px solid var(--ide-border);
  color: var(--ide-text); font-size: 11.5px; border-radius: 3px; outline: none;
}
.idebp-input:focus { border-color: var(--ide-blue); }
.idebp-btn {
  width: 26px; height: 26px; border: 0; border-radius: 3px; cursor: pointer;
  background: transparent; color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center;
}
.idebp-btn:hover { background: var(--ide-hover); color: var(--ide-text-strong); }
.idebp-sep { width: 1px; height: 16px; background: var(--ide-border); margin: 0 2px; }

.idebp-set-wrap { position: relative; }
.idebp-set-pop {
  position: absolute; right: 0; top: calc(100% + 4px); z-index: 40; width: 220px;
  background: var(--ide-menu-bg); border: 1px solid var(--ide-border);
  border-radius: 4px; box-shadow: 0 8px 24px rgba(0, 0, 0, .45); padding: 4px 0;
}
.idebp-set-item {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; font-size: 12px; color: var(--ide-text); cursor: pointer;
}
.idebp-set-item:hover { background: var(--ide-active); }
.idebp-set-check { width: 16px; flex-shrink: 0; color: var(--ide-green); display: inline-flex; justify-content: center; }
.idebp-set-sep { height: 1px; margin: 4px 0; background: var(--ide-border); }

.idebp-body {
  flex: 1; min-height: 0; overflow: auto; padding: 6px 10px;
  font-family: Consolas, Monaco, monospace; font-size: 12px; line-height: 18px;
}
.idebp-empty { padding: 16px 2px; color: var(--ide-text-dim); font-size: 12px; line-height: 19px; }
.idebp-line { white-space: pre-wrap; word-break: break-all; color: var(--ide-text); }
.idebp-line.is-stderr { color: #f14c4c; }
.idebp-line.is-system { color: var(--ide-text-dim); }
.idebp-line.is-input { color: #23d18b; }

.idebp-prob-tip {
  display: flex; align-items: flex-start; gap: 6px;
  margin: 2px 0 6px; padding: 6px 8px; border-radius: 3px;
  background: rgba(55, 148, 255, .12); color: #3794ff; font-size: 11.5px; line-height: 18px;
}
.idebp-prob {
  display: flex; align-items: baseline; gap: 6px; padding: 2px 0; cursor: pointer;
}
.idebp-prob:hover { background: var(--ide-hover); }
.idebp-prob-ic { flex-shrink: 0; display: inline-flex; }
.idebp-prob-msg { flex: 1; min-width: 0; color: var(--ide-text); }
.idebp-prob-loc { flex-shrink: 0; color: var(--ide-text-dim); font-size: 11px; }

.idebp-prompt { display: flex; align-items: center; gap: 6px; padding-top: 4px; }
.idebp-prompt.is-disabled { opacity: .6; }
.idebp-prompt-sign { color: #23d18b; flex-shrink: 0; }
.idebp-prompt-input {
  flex: 1; min-width: 0; border: 0; outline: none; background: transparent;
  color: var(--ide-text); font-family: inherit; font-size: inherit;
}
.idebp-stop {
  display: inline-flex; align-items: center; gap: 3px; flex-shrink: 0;
  height: 20px; padding: 0 7px; border-radius: 3px; cursor: pointer;
  background: transparent; border: 1px solid #f14c4c; color: #f14c4c; font-size: 11px;
}

.idebp-foot {
  flex-shrink: 0; height: 20px; display: flex; align-items: center; gap: 6px;
  padding: 0 10px; font-size: 11px; color: var(--ide-text-dim);
  border-top: 1px solid var(--ide-border);
}
.idebp-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.idebp-dot.is-ok { background: #23d18b; }
.idebp-dot.is-off { background: #888888; }
</style>
