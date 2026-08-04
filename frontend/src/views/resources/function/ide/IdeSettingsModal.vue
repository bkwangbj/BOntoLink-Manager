<template>
  <Teleport to="body">
    <transition name="idst-fade">
      <div v-if="open" class="fnide idst-mask" :data-ide-theme="draft.theme" @click.self="onCancel">
        <div class="idst-modal">
          <div class="idst-hd">
            <span class="idst-title">通用设置</span>
            <button class="idst-x" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="idst-body">
            <!-- 左侧导航 (9 组, 与右侧分组一一对应) -->
            <aside class="idst-nav">
              <div v-for="g in GROUPS" :key="g.k"
                   :class="['idst-nav-item', activeGroup === g.k && 'is-on']"
                   @click="scrollTo(g.k)">
                <span class="idst-nav-ic" v-html="BL.icon(g.icon, 13)"></span>{{ g.label }}
              </div>
            </aside>

            <!-- 右侧设置项 -->
            <div class="idst-content" ref="contentEl" @scroll="onScroll">
              <section v-for="g in GROUPS" :key="g.k" :ref="el => sectionEls[g.k] = el" class="idst-sec">
                <h3 class="idst-sec-t">{{ g.label }}</h3>

                <div v-for="it in g.items" :key="it.k" class="idst-row">
                  <div class="idst-row-l">
                    <div class="idst-row-lbl">{{ it.label }}</div>
                    <div v-if="it.sub" class="idst-row-sub">{{ it.sub }}</div>
                  </div>
                  <span class="idst-help" :title="it.help">!</span>

                  <div class="idst-row-c">
                    <!-- 主题色块卡片 -->
                    <div v-if="it.type === 'theme'" class="idst-themes">
                      <button v-for="t in THEME_CARDS" :key="t.v"
                              :class="['idst-theme', draft.theme === t.v && 'is-on']"
                              @click="patch('theme', t.v)">
                        <span class="idst-theme-box" :style="{ background: t.bg, borderColor: t.border }"></span>
                        <span class="idst-theme-lbl">{{ t.label }}</span>
                      </button>
                    </div>

                    <!-- 开关 -->
                    <span v-else-if="it.type === 'switch'"
                          :class="['idst-toggle', draft[it.k] && 'is-on']"
                          @click="patch(it.k, !draft[it.k])"><i></i></span>

                    <!-- 下拉 -->
                    <select v-else-if="it.type === 'select'" class="idst-select"
                            :value="String(draft[it.k])"
                            @change="patch(it.k, castLike(it.options[0].v, $event.target.value))">
                      <option v-for="o in it.options" :key="String(o.v)" :value="String(o.v)">{{ o.label }}</option>
                    </select>

                    <!-- 数字 -->
                    <input v-else-if="it.type === 'number'" class="idst-num" type="number"
                           :min="it.min" :max="it.max" :step="it.step || 1"
                           :value="draft[it.k]"
                           @input="patch(it.k, clampNum($event.target.value, it))" />

                    <!-- 双数字(代码长度标尺) -->
                    <span v-else-if="it.type === 'rulers'" class="idst-rulers">
                      <input class="idst-num" type="number" min="0" max="200" :value="draft.ruler1"
                             @input="patch('ruler1', clampNum($event.target.value, { min: 0, max: 200 }))" />
                      <input class="idst-num" type="number" min="0" max="200" :value="draft.ruler2"
                             @input="patch('ruler2', clampNum($event.target.value, { min: 0, max: 200 }))" />
                    </span>
                  </div>
                </div>
              </section>
            </div>
          </div>

          <div class="idst-ft">
            <button class="idst-btn" @click="onRestore">恢复默认</button>
            <span class="bl-grow"></span>
            <button class="idst-btn" @click="onCancel">取消</button>
            <button class="idst-btn is-primary" @click="onOk">确定</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 通用设置弹窗 (P5c · 文档「外观 → 通用设置」)
 *
 * 880 × 80vh、左导航 9 组 + 右内容区、滚动联动高亮、所有改动实时预览。
 * 确定 = 落 localStorage;取消 / 点遮罩 = 回滚到打开弹窗时的快照(文档 4.2 取消回滚机制)。
 * 弹窗自身色系跟随 IDE 主题(挂 .fnide[data-ide-theme]),与编辑器实时同步。
 */
import { ref, reactive, watch, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { DEFAULTS } from './ideSettings.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  settings: { type: Object, required: true }
})
const emit = defineEmits(['update:open', 'apply'])

const THEME_CARDS = [
  { v: 'light', label: '浅色', bg: '#ffffff', border: '#d4d4d4' },
  { v: 'dark', label: '深色', bg: '#1e1e1e', border: '#3c3c3c' },
  { v: 'hc', label: '高对比', bg: '#000000', border: '#6fc3df' },
]

const GROUPS = [
  {
    k: 'visual', label: '全局视觉外观', icon: 'settings', items: [
      { k: 'theme', label: '主题', sub: '选择编辑器整体配色方案', type: 'theme',
        help: '切换编辑器明暗主题,影响代码高亮、背景、边框等所有视觉元素' },
      { k: 'fontSize', label: '字体大小', sub: '编辑器文字显示尺寸', type: 'select',
        options: [{ v: 12, label: '小 (12px)' }, { v: 14, label: '中 (推荐,14px)' }, { v: 16, label: '大 (16px)' }, { v: 18, label: '特大 (18px)' }],
        help: '调整代码字体像素大小,也可通过 Ctrl +/- 快捷键快速缩放' },
      { k: 'fontFamily', label: '字体', sub: '代码显示所用的等宽字体', type: 'select',
        options: ['Consolas', 'Menlo', 'Monaco', 'Fira Code', 'JetBrains Mono', 'Source Code Pro', 'Courier New', '系统等宽'].map(v => ({ v, label: v })),
        help: '开发推荐等宽字体,确保字符对齐、可读性强;需系统已安装对应字体' },
      { k: 'fontLigatures', label: '启用字体连字', type: 'switch',
        help: '将 >= !== -> 等多字符符号渲染为连体样式,需字体支持连字特性' },
      { k: 'letterSpacing', label: '字间距', type: 'number', min: 0, max: 2, step: 0.1,
        help: '字符水平间距,调大可降低视觉拥挤感' },
      { k: 'lineHeight', label: '行高', sub: '每行文字的高度,调大可提升可读性', type: 'number', min: 1, max: 2.5, step: 0.1,
        help: '每行文字高度,调大可提升可读性' },
      { k: 'lineNumbers', label: '行号显示', type: 'select',
        options: [{ v: 'on', label: '显示' }, { v: 'off', label: '关闭' }, { v: 'relative', label: '相对行号' }],
        help: '控制左侧行号显示方式,相对行号以当前行为基准显示上下距离' },
      { k: 'lineNumbersMinChars', label: '行号区域最小宽度', type: 'number', min: 3, max: 10,
        help: '行号列最小字符宽度,避免行数增多时页面左右抖动' },
      { k: 'paddingTop', label: '顶部内边距', type: 'number', min: 0, max: 40,
        help: '编辑器首行到顶部的留白距离' },
      { k: 'paddingBottom', label: '底部内边距', type: 'number', min: 0, max: 40,
        help: '编辑器末行到底部的留白距离' },
      { k: 'renderLineHighlight', label: '当前行高亮样式', type: 'select',
        options: [{ v: 'all', label: '全部' }, { v: 'line', label: '仅线条' }, { v: 'none', label: '关闭' }],
        help: '光标所在行高亮效果,全部 = 整行背景 + 行号,仅线条 = 只高亮行号栏' },
      { k: 'renderLineHighlightOnlyWhenFocus', label: '仅聚焦时高亮当前行', type: 'switch',
        help: '编辑器失去焦点时隐藏行高亮,界面更清爽' },
    ]
  },
  {
    k: 'whitespace', label: '空白与特殊字符', icon: 'fileText', items: [
      { k: 'renderWhitespace', label: '空格显示模式', type: 'select',
        options: [{ v: 'none', label: '不显示' }, { v: 'boundary', label: '边界' }, { v: 'selection', label: '选中时' }, { v: 'all', label: '全部' }],
        help: '将空格、制表符渲染为可见符号,便于排查缩进混乱、全角空格等问题' },
      { k: 'renderControlCharacters', label: '显示控制字符', type: 'switch',
        help: '显示换行符、制表符、零宽字符等不可见符号,便于排查异常字符' },
    ]
  },
  {
    k: 'scroll', label: '滚动行为', icon: 'arrowDown', items: [
      { k: 'stickyScroll', label: '启用粘滞滚动', type: 'switch',
        help: '滚动时代码块、函数名固定在编辑器顶部,快速定位当前作用域' },
      { k: 'stickyScrollMaxLineCount', label: '粘滞滚动最大行数', type: 'number', min: 1, max: 10,
        help: '顶部最多固定几层嵌套作用域标题,过多会遮挡代码' },
      { k: 'smoothScrolling', label: '平滑滚动', type: 'switch', help: '滚动时加入缓动效果,视觉更流畅' },
      { k: 'scrollBeyondLastLine', label: '滚动超出最后一行', type: 'switch',
        help: '允许将最后一行滚动到页面中部,避免底部代码贴边阅读困难' },
      { k: 'scrollBeyondLastColumn', label: '横向滚动超出列数', type: 'number', min: 0, max: 20,
        help: '横向滚动时右侧预留的空白列数' },
      { k: 'horizontalScrollbarSize', label: '横向滚动条宽度', type: 'number', min: 6, max: 20, help: '底部横向滚动条像素宽度' },
      { k: 'verticalScrollbarSize', label: '纵向滚动条宽度', type: 'number', min: 6, max: 20, help: '右侧纵向滚动条像素宽度' },
      { k: 'mouseWheelScrollSensitivity', label: '鼠标滚轮灵敏度', type: 'number', min: 0.1, max: 3, step: 0.1,
        help: '滚轮每格滚动距离倍率,数值越大滚动越快' },
    ]
  },
  {
    k: 'minimap', label: '缩略图', icon: 'layout', items: [
      { k: 'minimap', label: '启用缩略图', type: 'switch', help: '右侧显示代码全貌缩略图,点击快速跳转,长文件必备' },
      { k: 'minimapScale', label: '缩略图缩放比例', sub: 'Monaco 原生取值 1 / 2 / 3', type: 'number', min: 1, max: 3,
        help: '缩略图渲染倍率。文档写的 0.3~1 在 Monaco 没有对应项,这里用原生的 1/2/3 档' },
      { k: 'minimapShowSlider', label: '滑块显示时机', type: 'select',
        options: [{ v: 'mouseover', label: '悬停显示' }, { v: 'always', label: '始终显示' }],
        help: '缩略图上视口滑块的显示方式' },
      { k: 'minimapSide', label: '缩略图位置', type: 'select',
        options: [{ v: 'right', label: '右侧' }, { v: 'left', label: '左侧' }],
        help: '缩略图放置在编辑器左侧或右侧' },
    ]
  },
  {
    k: 'cursor', label: '光标与选择', icon: 'cursor', items: [
      { k: 'cursorBlinking', label: '光标闪烁样式', type: 'select',
        options: [{ v: 'blink', label: '闪烁' }, { v: 'smooth', label: '平滑' }, { v: 'phase', label: '渐变' }, { v: 'solid', label: '静止' }],
        help: '插入光标的闪烁动画效果' },
      { k: 'cursorSmoothCaretAnimation', label: '光标平滑动画', type: 'select',
        options: [{ v: 'on', label: '开启' }, { v: 'explicit', label: '仅显式移动' }, { v: 'off', label: '关闭' }],
        help: '光标移动时加入过渡动画。Monaco 0.45 起该项为三态,不再是开/关' },
      { k: 'cursorWidth', label: '光标宽度', type: 'number', min: 1, max: 5, help: '插入光标的像素宽度' },
      { k: 'selectionHighlight', label: '选中文本高亮', type: 'switch', help: '选中文字时高亮显示对应区域' },
      { k: 'occurrencesHighlight', label: '匹配单词同步高亮', type: 'switch',
        help: '光标停在单词上时,全文所有相同单词自动高亮' },
    ]
  },
  {
    k: 'indent', label: '缩进与标尺', icon: 'columns', items: [
      { k: 'guidesIndentation', label: '显示缩进引导线', type: 'switch', help: '用垂直虚线标记每层缩进位置,清晰展示代码层级' },
      { k: 'guidesBracketPairs', label: '括号配对引导线', type: 'switch', help: '高亮配对括号之间的垂直连线,快速识别代码块范围' },
      { k: 'rulers', label: '代码长度标尺', sub: '第一标尺 / 第二标尺,填 0 表示不显示', type: 'rulers',
        help: '在指定列数显示垂直参考线;第一标尺为传统编码规范线(80),第二标尺为宽屏参考线(120)' },
    ]
  },
  {
    k: 'folding', label: '折叠与侧边距', icon: 'chevronDown', items: [
      { k: 'folding', label: '启用代码折叠', type: 'switch', help: '允许折叠函数、代码块,收起不关注内容,聚焦编辑区' },
      { k: 'foldingStrategy', label: '折叠策略', type: 'select',
        options: [{ v: 'auto', label: '自动' }, { v: 'indentation', label: '缩进' }],
        help: '自动 = 基于语法智能识别可折叠区域;缩进 = 仅按缩进层级折叠' },
      { k: 'showFoldingControls', label: '折叠按钮显示时机', type: 'select',
        options: [{ v: 'mouseover', label: '悬停显示' }, { v: 'always', label: '始终显示' }],
        help: '折叠加减号的显示方式' },
      { k: 'glyphMargin', label: '启用符号边距', type: 'switch', help: '最左侧预留图标区域,用于显示断点、调试标记、错误警告等' },
    ]
  },
  {
    k: 'editing', label: '编辑交互行为', icon: 'edit', items: [
      { k: 'mouseStyle', label: '鼠标样式', type: 'select',
        options: [{ v: 'text', label: '文本' }, { v: 'default', label: '默认' }], help: '编辑器区域的鼠标指针样式' },
      { k: 'multiCursorModifier', label: '多光标修饰键', type: 'select',
        options: [{ v: 'ctrlCmd', label: 'Ctrl' }, { v: 'alt', label: 'Alt' }],
        help: '按住对应按键点击可创建多个光标,用于批量编辑' },
      { k: 'tabSize', label: 'Tab 缩进宽度', type: 'number', min: 1, max: 8,
        help: '一个 Tab 对应的空格数,前端常用 2,后端常用 4' },
      { k: 'insertSpaces', label: '插入空格替代 Tab', type: 'switch',
        help: '按 Tab 键插入空格而非制表符,避免不同环境缩进错乱' },
      { k: 'autoClosingBrackets', label: '自动闭合括号', type: 'select',
        options: [{ v: 'always', label: '始终' }, { v: 'never', label: '从不' }, { v: 'beforeWhitespace', label: '空白前' }],
        help: '输入左括号时自动补全右括号' },
      { k: 'autoClosingQuotes', label: '自动闭合引号', type: 'select',
        options: [{ v: 'always', label: '始终' }, { v: 'never', label: '从不' }, { v: 'beforeWhitespace', label: '空白前' }],
        help: '输入左引号时自动补全右引号' },
      { k: 'formatOnPaste', label: '粘贴自动格式化', type: 'switch', help: '粘贴代码后自动调整缩进格式,需对应语言格式化支持' },
      { k: 'formatOnType', label: '输入自动格式化', type: 'switch', help: '输入时自动格式化当前行' },
    ]
  },
  {
    k: 'hover', label: '悬浮与代码提示', icon: 'search', items: [
      { k: 'hover', label: '启用悬浮提示', type: 'switch', help: '鼠标悬停代码时显示类型定义、注释说明等信息' },
      { k: 'hoverSticky', label: '悬浮提示粘性', type: 'switch', help: '允许鼠标移动到悬浮提示框内进行点击、复制等操作' },
      { k: 'quickSuggestions', label: '启用快速代码建议', type: 'switch', help: '输入时自动弹出代码补全、变量提示等建议列表' },
    ]
  },
]

const draft = reactive({ ...DEFAULTS })
let snapshot = null
const activeGroup = ref('visual')
const contentEl = ref(null)
const sectionEls = {}

watch(() => props.open, (v) => {
  if (!v) return
  Object.assign(draft, props.settings)
  snapshot = { ...props.settings }      // 打开即存快照, 供取消回滚
  activeGroup.value = 'visual'
  nextTick(() => { if (contentEl.value) contentEl.value.scrollTop = 0 })
})

/** 所有改动即时生效(文档:实时预览, 无需点确定) */
function patch(k, v) {
  draft[k] = v
  emit('apply', { ...draft })
}

function clampNum(raw, it) {
  let n = Number(raw)
  if (!Number.isFinite(n)) n = it.min ?? 0
  if (it.min !== undefined) n = Math.max(it.min, n)
  if (it.max !== undefined) n = Math.min(it.max, n)
  return n
}
/** select 的 value 只能是字符串, 按选项首项的类型还原 */
function castLike(sample, v) { return typeof sample === 'number' ? Number(v) : v }

function scrollTo(k) {
  activeGroup.value = k
  const el = sectionEls[k]
  if (el && contentEl.value) contentEl.value.scrollTo({ top: el.offsetTop - 8, behavior: 'smooth' })
}
/** 滚动联动:高亮当前可视分组 */
function onScroll() {
  const top = (contentEl.value?.scrollTop || 0) + 12
  let cur = GROUPS[0].k
  for (const g of GROUPS) {
    const el = sectionEls[g.k]
    if (el && el.offsetTop <= top) cur = g.k
  }
  activeGroup.value = cur
}

async function onRestore() {
  const ok = await BL.confirm({ title: '恢复默认', content: '所有编辑器设置将重置为默认值,确定?', okText: '恢复' })
  if (!ok) return
  Object.assign(draft, DEFAULTS)
  emit('apply', { ...draft })
}
function onCancel() {
  if (snapshot) emit('apply', { ...snapshot })   // 回滚预览期间的所有修改
  emit('update:open', false)
}
function onOk() {
  emit('apply', { ...draft })
  emit('update:open', false)
}
</script>

<style scoped>
.idst-mask {
  position: fixed; inset: 0; z-index: 1400;
  background: rgba(0, 0, 0, .5);
  display: flex; align-items: center; justify-content: center;
}
.idst-modal {
  width: 880px; max-width: calc(100vw - 40px); height: 80vh;
  background: var(--ide-bg-2); color: var(--ide-text);
  border: 1px solid var(--ide-border); border-radius: 6px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, .5);
  display: flex; flex-direction: column; overflow: hidden;
}
.idst-hd {
  flex-shrink: 0; display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--ide-border);
}
.idst-title { font-size: 14px; font-weight: 600; color: var(--ide-text-strong); }
.idst-x {
  width: 24px; height: 24px; border: 0; border-radius: 3px; cursor: pointer;
  background: transparent; color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center;
}
.idst-x:hover { background: var(--ide-hover); color: var(--ide-text-strong); }

.idst-body { flex: 1; min-height: 0; display: flex; }
.idst-nav {
  width: 180px; flex-shrink: 0; overflow: auto;
  border-right: 1px solid var(--ide-border); padding: 8px 0;
}
.idst-nav-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; font-size: 12.5px; color: var(--ide-text-dim); cursor: pointer;
  border-left: 3px solid transparent;
}
.idst-nav-item:hover { background: var(--ide-hover); color: var(--ide-text); }
.idst-nav-item.is-on {
  border-left-color: var(--ide-blue); background: var(--ide-hover); color: var(--ide-text-strong);
}
.idst-nav-ic { display: inline-flex; }

.idst-content { flex: 1; min-width: 0; overflow: auto; padding: 4px 16px 24px; }
.idst-sec { padding-top: 12px; }
.idst-sec-t {
  margin: 0 0 6px; padding-bottom: 6px;
  font-size: 13px; font-weight: 600; color: var(--ide-text-strong);
  border-bottom: 1px solid var(--ide-border);
}
.idst-row {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 0; border-bottom: 1px solid rgba(128, 128, 128, .12);
}
.idst-row-l { flex: 1; min-width: 0; }
.idst-row-lbl { font-size: 12.5px; color: var(--ide-text); }
.idst-row-sub { font-size: 11px; color: var(--ide-text-dim); margin-top: 2px; }
.idst-row-c { flex-shrink: 0; display: flex; align-items: center; gap: 8px; }

.idst-help {
  flex-shrink: 0; width: 15px; height: 15px; border-radius: 50%;
  background: #555555; color: #fff; cursor: help;
  font-size: 10px; font-weight: 700; line-height: 15px; text-align: center;
}
.idst-help:hover { background: #0078d4; }

.idst-themes { display: flex; gap: 8px; }
.idst-theme {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 4px; border: 2px solid transparent; border-radius: 4px;
  background: transparent; cursor: pointer; color: var(--ide-text-dim);
}
.idst-theme.is-on { border-color: var(--ide-blue); color: var(--ide-text-strong); }
.idst-theme-box { width: 54px; height: 32px; border-radius: 3px; border: 1px solid; display: block; }
.idst-theme-lbl { font-size: 11px; }

.idst-toggle {
  width: 34px; height: 18px; border-radius: 9px; cursor: pointer;
  background: #555555; position: relative; display: inline-block; transition: background-color .18s;
}
.idst-toggle i {
  position: absolute; top: 2px; left: 2px; width: 14px; height: 14px; border-radius: 50%;
  background: #fff; transition: transform .18s;
}
.idst-toggle.is-on { background: var(--ide-blue); }
.idst-toggle.is-on i { transform: translateX(16px); }

.idst-select, .idst-num {
  height: 26px; padding: 0 6px; border-radius: 3px;
  background: var(--ide-bg); border: 1px solid var(--ide-border);
  color: var(--ide-text); font-size: 12px; outline: none;
}
.idst-select { min-width: 150px; }
.idst-num { width: 84px; text-align: center; }
.idst-select:focus, .idst-num:focus { border-color: var(--ide-blue); }
.idst-rulers { display: inline-flex; gap: 6px; }

.idst-ft {
  flex-shrink: 0; display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border-top: 1px solid var(--ide-border);
}
.bl-grow { flex: 1; }
.idst-btn {
  height: 28px; padding: 0 14px; border-radius: 3px; cursor: pointer;
  background: transparent; border: 1px solid var(--ide-border);
  color: var(--ide-text); font-size: 12px;
}
.idst-btn:hover { background: var(--ide-hover); }
.idst-btn.is-primary { background: var(--ide-blue); border-color: var(--ide-blue); color: #fff; }
.idst-btn.is-primary:hover { opacity: .9; }

.idst-fade-enter-active, .idst-fade-leave-active { transition: opacity .15s; }
.idst-fade-enter-from, .idst-fade-leave-to { opacity: 0; }
</style>
