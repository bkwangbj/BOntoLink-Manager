<template>
  <div class="bs" ref="rootEl">
    <div :class="['bs-control', 'bl-input', size === 'sm' && 'bl-input-sm', { 'is-disabled': disabled, 'is-open': open }]" @click="toggle">
      <span :class="['bs-value', !selectedLabel && 'is-ph']">{{ selectedLabel || placeholder }}</span>
      <span class="bs-arrow" :class="{ 'is-open': open }" v-html="BL.icon('chevronDown', 12)"></span>
    </div>
    <Teleport to="body">
      <div v-if="open" ref="popEl" class="bs-pop" :style="popStyle">
        <div v-if="showSearch" class="bs-search">
          <span class="bs-search-ic" v-html="BL.icon('search', 12)"></span>
          <input ref="searchEl" class="bs-search-input" v-model="q" :placeholder="searchPlaceholder" @keydown.esc="close" />
        </div>
        <div class="bs-list">
          <div v-if="clearable && modelValue !== '' && modelValue != null" class="bs-opt bs-opt-clear" @click="pick('')">{{ placeholder }}</div>
          <div v-for="o in filtered" :key="o.value"
               :class="['bs-opt', String(o.value) === String(modelValue) && 'is-sel', o.disabled && 'is-disabled']"
               @click="!o.disabled && pick(o.value)">
            <span v-html="highlight(o.label)"></span>
            <span v-if="String(o.value) === String(modelValue)" class="bs-check" v-html="BL.icon('check', 12)"></span>
          </div>
          <div v-if="!filtered.length" class="bs-empty">无匹配项</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },      // [{ value, label, disabled? }]
  placeholder: { type: String, default: '请选择' },
  searchPlaceholder: { type: String, default: '输入关键词搜索…' },
  disabled: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
  searchable: { type: [Boolean, String], default: 'auto' },  // true | false | 'auto'(>8 项才显示搜索)
  size: { type: String, default: '' },              // '' | 'sm'
})
const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)
const q = ref('')
const rootEl = ref(null)
const popEl = ref(null)
const searchEl = ref(null)
const popStyle = ref({})

const selectedLabel = computed(() => {
  const o = props.options.find(x => String(x.value) === String(props.modelValue))
  return o ? o.label : ''
})
const showSearch = computed(() => props.searchable === true || (props.searchable === 'auto' && props.options.length > 8))
const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return props.options
  return props.options.filter(o => String(o.label).toLowerCase().includes(k))
})
function escapeHtml(s) { return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c])) }
function highlight(label) {
  const k = q.value.trim()
  if (!k || !showSearch.value) return escapeHtml(label)
  const idx = String(label).toLowerCase().indexOf(k.toLowerCase())
  if (idx < 0) return escapeHtml(label)
  const s = String(label)
  return escapeHtml(s.slice(0, idx)) + '<b class="bs-hl">' + escapeHtml(s.slice(idx, idx + k.length)) + '</b>' + escapeHtml(s.slice(idx + k.length))
}

function computePos() {
  const el = rootEl.value
  if (!el) return
  const r = el.getBoundingClientRect()
  const below = window.innerHeight - r.bottom
  const openUp = below < 240 && r.top > below
  popStyle.value = {
    position: 'fixed',
    left: r.left + 'px',
    minWidth: r.width + 'px',
    [openUp ? 'bottom' : 'top']: (openUp ? (window.innerHeight - r.top + 4) : (r.bottom + 4)) + 'px',
    zIndex: 1360,
  }
}
function toggle() { if (props.disabled) return; open.value ? close() : openPop() }
function openPop() {
  q.value = ''
  computePos()
  open.value = true
  if (showSearch.value) nextTick(() => searchEl.value?.focus())
  window.addEventListener('mousedown', onDocDown, true)
  window.addEventListener('scroll', onReposition, true)
  window.addEventListener('resize', onReposition)
}
function onReposition() { if (open.value) computePos() }
function close() {
  if (!open.value) return
  open.value = false
  window.removeEventListener('mousedown', onDocDown, true)
  window.removeEventListener('scroll', onReposition, true)
  window.removeEventListener('resize', onReposition)
}
function onDocDown(e) {
  if (rootEl.value?.contains(e.target) || popEl.value?.contains(e.target)) return
  const t = e.target
  if (t && (t.scrollHeight > t.clientHeight || t.scrollWidth > t.clientWidth) && (e.offsetX > t.clientWidth || e.offsetY > t.clientHeight)) return
  close()
}
function pick(v) { emit('update:modelValue', v); emit('change', v); close() }
onBeforeUnmount(close)
</script>

<style scoped>
.bs { position: relative; }
.bs-control { display: flex; align-items: center; justify-content: space-between; gap: 6px; cursor: pointer; user-select: none; padding-right: 8px; }
.bs-control.is-disabled { background: var(--bl-bg-2); cursor: not-allowed; opacity: .75; }
.bs-control.is-open { border-color: var(--bl-primary); }
.bs-value { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bs-value.is-ph { color: var(--bl-text-3); }
.bs-arrow { flex-shrink: 0; color: var(--bl-text-3); transition: transform .15s; display: inline-flex; }
.bs-arrow.is-open { transform: rotate(180deg); }
</style>

<style>
.bs-pop {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0,0,0,.18); display: flex; flex-direction: column;
  max-height: 300px; max-width: 460px; overflow: hidden;
}
:root[data-theme="dark"] .bs-pop { border-color: var(--bl-border-strong); box-shadow: 0 8px 28px rgba(0,0,0,.55); }
.bs-search { display: flex; align-items: center; gap: 6px; padding: 8px 10px; border-bottom: 1px solid var(--bl-divider); }
.bs-search-ic { color: var(--bl-text-3); display: inline-flex; }
.bs-search-input { flex: 1; border: 0; outline: none; background: transparent; font-size: 13px; color: var(--bl-text-1); }
.bs-list { overflow-y: auto; overflow-x: hidden; padding: 4px; }
.bs-opt { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 7px 10px; border-radius: 6px; font-size: 13px; color: var(--bl-text-1); cursor: pointer; white-space: nowrap; }
.bs-opt:hover { background: var(--bl-bg-hover); }
.bs-opt.is-sel { background: var(--bl-primary-soft); color: var(--bl-primary); }
.bs-opt.is-disabled { color: var(--bl-text-3); cursor: not-allowed; }
.bs-opt-clear { color: var(--bl-text-3); font-style: italic; }
.bs-check { flex-shrink: 0; color: var(--bl-primary); display: inline-flex; }
.bs-empty { padding: 16px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.bs-hl { color: var(--bl-primary); font-weight: 600; }
</style>
