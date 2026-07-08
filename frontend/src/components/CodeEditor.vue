<!--
  轻量代码编辑器(零依赖)。高亮 textarea 技术:透明输入层叠在高亮 <pre> 之上,滚动同步。
  用于系统里所有「需要标准化格式」的 JSON / Schema / 配置 / 表达式 字段。
  props:
    v-model            文本
    language           'json'(默认:高亮+格式化+校验) | 'text'(仅等宽+行号,不校验)
    rows               初始行高(默认 6)
    disabled           只读(隐藏工具栏、只读输入)
    placeholder
  emits: update:modelValue, valid(boolean)
-->
<template>
  <div class="ce" :class="{ 'ce-err': !!error, 'ce-ro': disabled }" :style="{ height: pxHeight }">
    <div class="ce-body">
      <div class="ce-gutter" ref="gutter"><div v-for="n in lineCount" :key="n" class="ce-ln">{{ n }}</div></div>
      <pre class="ce-hl" ref="hl" aria-hidden="true"><code v-html="highlighted"></code></pre>
      <textarea ref="ta" class="ce-ta" :value="modelValue" :disabled="disabled" :placeholder="placeholder"
                spellcheck="false" wrap="off" @input="onInput" @scroll="onScroll" @keydown.tab.prevent="onTab"></textarea>
    </div>
    <div v-if="!disabled || error" class="ce-bar">
      <span v-if="error" class="ce-msg">⚠ {{ error }}</span>
      <span v-else-if="language==='json' && String(modelValue||'').trim()" class="ce-ok">✓ JSON 有效</span>
      <span class="ce-grow"></span>
      <button v-if="!disabled && language==='json'" type="button" class="ce-btn" @click="format">格式化</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  language: { type: String, default: 'json' },
  rows: { type: Number, default: 6 },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  autoFormat: { type: Boolean, default: true }   // 加载/外部换值时自动 pretty-print 一次
})
const emit = defineEmits(['update:modelValue', 'valid'])
let lastEmitted = null
function emitVal (v) { lastEmitted = v; emit('update:modelValue', v) }

const ta = ref(null), hl = ref(null), gutter = ref(null)
const error = ref('')

const pxHeight = computed(() => (props.rows * 21 + 16 + (props.disabled ? 0 : 26)) + 'px')
const lineCount = computed(() => Math.max(1, String(props.modelValue || '').split('\n').length))

function esc (s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') }
function hlJson (src) {
  return esc(src).replace(/(&quot;(?:\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\&])*?&quot;(\s*:)?|\b(?:true|false)\b|\bnull\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (m) {
    let cls
    if (m.indexOf('&quot;') === 0) cls = /:\s*$/.test(m) ? 'ce-key' : 'ce-str'
    else if (/^(true|false)$/.test(m)) cls = 'ce-bool'
    else if (m === 'null') cls = 'ce-null'
    else cls = 'ce-num'
    return '<span class="' + cls + '">' + m + '</span>'
  })
}
const highlighted = computed(() => {
  const raw = String(props.modelValue || '')
  const body = props.language === 'json' ? hlJson(raw) : esc(raw)
  return body + (raw.endsWith('\n') ? ' ' : '')
})

function validate (val) {
  if (props.language !== 'json') { error.value = ''; emit('valid', true); return }
  const t = String(val || '').trim()
  if (!t) { error.value = ''; emit('valid', true); return }
  try { JSON.parse(t); error.value = ''; emit('valid', true) }
  catch (e) { error.value = String(e.message || 'JSON 格式错误').slice(0, 80); emit('valid', false) }
}
function onInput (e) { emitVal(e.target.value); validate(e.target.value) }
function onScroll () { if (!ta.value) return; const t = ta.value.scrollTop, l = ta.value.scrollLeft; if (hl.value) { hl.value.scrollTop = t; hl.value.scrollLeft = l } if (gutter.value) gutter.value.scrollTop = t }
function onTab (e) {
  const el = e.target, s = el.selectionStart, en = el.selectionEnd
  const nv = el.value.slice(0, s) + '  ' + el.value.slice(en)
  emitVal(nv); validate(nv)
  requestAnimationFrame(() => { el.selectionStart = el.selectionEnd = s + 2 })
}
function format () {
  const t = String(props.modelValue || '').trim()
  if (!t) return
  try { const nv = JSON.stringify(JSON.parse(t), null, 2); emitVal(nv); validate(nv) }
  catch (e) { validate(t) }
}
/* 加载 / 外部换值(非用户输入回声)→ 自动格式化一次。用户输入的回声(=lastEmitted)不重排,避免打断输入。 */
watch(() => props.modelValue, (nv) => {
  if (nv === lastEmitted) return
  if (props.autoFormat && props.language === 'json') {
    const t = String(nv || '').trim()
    if (t) { try { const p = JSON.stringify(JSON.parse(t), null, 2); if (p !== nv) { emitVal(p); return } } catch { /* 非法 JSON 保持原样 */ } }
  }
  validate(nv)
}, { immediate: true })
</script>

<style scoped>
.ce { display: flex; flex-direction: column; border: 1px solid var(--bl-border); border-radius: 8px; overflow: hidden; background: var(--bl-bg-1); position: relative; resize: vertical; min-height: 88px; }
.ce.ce-ro { resize: none; background: var(--bl-bg-2); }
.ce.ce-err { border-color: #f53f3f; }
.ce-body { position: relative; flex: 1; min-height: 0; overflow: hidden; font-family: var(--bl-mono, ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace); }
.ce-gutter { position: absolute; left: 0; top: 0; bottom: 0; width: 42px; padding: 8px 8px 8px 0; overflow: hidden; text-align: right; color: var(--bl-text-3); background: var(--bl-bg-2); border-right: 1px solid var(--bl-divider); user-select: none; z-index: 1; box-sizing: border-box; font-size: 12.5px; line-height: 21px; }
.ce-ln { height: 21px; }
.ce-hl, .ce-ta { position: absolute; top: 0; right: 0; bottom: 0; left: 42px; margin: 0; padding: 8px 10px; border: 0; white-space: pre; overflow: auto; box-sizing: border-box; font-family: inherit; font-size: 12.5px; line-height: 21px; tab-size: 2; }
.ce-hl { pointer-events: none; color: var(--bl-text-1); z-index: 0; }
.ce-hl code { font: inherit; }
.ce-ta { background: transparent; color: transparent; caret-color: var(--bl-text-1); outline: none; resize: none; z-index: 2; }
.ce-ta::placeholder { color: var(--bl-text-3); font-style: italic; opacity: .7; }
.ce-bar { flex: none; display: flex; align-items: center; gap: 8px; height: 26px; padding: 0 8px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-1); }
.ce-grow { flex: 1; }
.ce-msg { font-size: 11.5px; color: #f53f3f; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ce-ok { font-size: 11.5px; color: var(--bl-success, #00b42a); }
.ce-btn { height: 20px; padding: 0 9px; border: 1px solid var(--bl-border); border-radius: 5px; background: var(--bl-bg-1); color: var(--bl-text-2); font-size: 11.5px; cursor: pointer; }
.ce-btn:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.ce-key { color: #7c3aed; }
.ce-str { color: #067d3f; }
.ce-num { color: #b45309; }
.ce-bool { color: #0e7490; }
.ce-null { color: #6b7280; }
:root[data-theme="dark"] .ce-key { color: #c792ea; }
:root[data-theme="dark"] .ce-str { color: #8fd694; }
:root[data-theme="dark"] .ce-num { color: #f0a45d; }
:root[data-theme="dark"] .ce-bool { color: #4dd0e1; }
:root[data-theme="dark"] .ce-null { color: #9aa0a6; }
</style>
