<template>
  <div class="fs" ref="rootRef">
    <div :class="['bl-input', 'fs-control', open && 'is-open', disabled && 'is-disabled', mono && 'bl-mono']"
         @click="toggle">
      <span :class="['fs-val', !selLabel && 'fs-ph']">{{ selLabel || placeholder }}</span>
      <span class="fs-arrow" v-html="BL.icon('chevronDown', 12)"></span>
    </div>
    <Teleport to="body">
      <div v-if="open" class="fs-panel" :style="panelStyle" ref="panelRef">
        <div class="fs-search">
          <span class="fs-search-ic" v-html="BL.icon('search', 13)"></span>
          <input ref="searchRef" v-model="kw" class="fs-search-input" :placeholder="searchPlaceholder"
                 @keydown.esc="close" @keydown.enter.prevent="onEnter" />
        </div>
        <div class="fs-list">
          <div v-if="clearable && modelValue" class="fs-opt fs-clear" @click="pick('')">— 清除 —</div>
          <div v-for="o in filtered" :key="o.value"
               :class="['fs-opt', o.value === modelValue && 'is-sel', mono && 'bl-mono']"
               @click="pick(o.value)">{{ o.label }}</div>
          <div v-if="!filtered.length" class="fs-empty">无匹配项</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
/* 通用可筛选下拉: 原生 CSS, Teleport 到 body 避免被抽屉/滚动容器裁剪。
   options 支持对象数组(配 value-key/label-key)或字符串数组。 */
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  valueKey: { type: String, default: 'value' },
  labelKey: { type: String, default: 'label' },
  placeholder: { type: String, default: '— 请选择 —' },
  searchPlaceholder: { type: String, default: '输入关键字筛选' },
  disabled: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
  mono: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'change'])

const norm = computed(() => props.options.map(o =>
  (o && typeof o === 'object')
    ? { value: o[props.valueKey], label: String(o[props.labelKey] ?? o[props.valueKey] ?? '') }
    : { value: o, label: String(o) }
))
const selLabel = computed(() => norm.value.find(o => o.value === props.modelValue)?.label || '')

const kw = ref('')
const filtered = computed(() => {
  const k = kw.value.trim().toLowerCase()
  return k ? norm.value.filter(o => o.label.toLowerCase().includes(k)) : norm.value
})

const open = ref(false)
const rootRef = ref(null), panelRef = ref(null), searchRef = ref(null)
const panelStyle = ref({})
function position() {
  const el = rootRef.value?.querySelector('.fs-control')
  if (!el) return
  const r = el.getBoundingClientRect()
  panelStyle.value = { position: 'fixed', left: r.left + 'px', top: (r.bottom + 4) + 'px', width: r.width + 'px' }
}
function toggle() { if (props.disabled) return; open.value ? close() : openPanel() }
function openPanel() {
  open.value = true; kw.value = ''
  position()
  nextTick(() => searchRef.value?.focus())
  window.addEventListener('scroll', position, true)
  window.addEventListener('resize', position)
  document.addEventListener('mousedown', onDocDown, true)
}
function close() {
  if (!open.value) return
  open.value = false
  window.removeEventListener('scroll', position, true)
  window.removeEventListener('resize', position)
  document.removeEventListener('mousedown', onDocDown, true)
}
function onDocDown(e) {
  if (rootRef.value?.contains(e.target) || panelRef.value?.contains(e.target)) return
  close()
}
function onEnter() {
  if (filtered.value.length) pick(filtered.value[0].value)
}
function pick(v) { emit('update:modelValue', v); emit('change', v); close() }
onBeforeUnmount(close)
</script>

<style scoped>
.fs { position: relative; flex: 1; min-width: 0; }
.fs-control {
  display: flex; align-items: center; gap: 6px; cursor: pointer;
  user-select: none; overflow: hidden;
}
.fs-control.is-open { border-color: var(--bl-primary); }
.fs-control.is-disabled { background: var(--bl-bg-2); color: var(--bl-text-4); cursor: not-allowed; }
.fs-val { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fs-ph { color: var(--bl-text-4); }
.fs-arrow { display: inline-flex; color: var(--bl-text-3); flex-shrink: 0; }

/* Teleport 到 body 的浮层: z-index 高于详情抽屉(1010)与大模态(≥1200) */
.fs-panel {
  z-index: 3000;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-2);
  box-shadow: 0 6px 20px rgba(0, 0, 0, .14);
  overflow: hidden;
  display: flex; flex-direction: column;
}
.fs-search { position: relative; padding: 6px; border-bottom: 1px solid var(--bl-divider); }
.fs-search-ic { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.fs-search-input {
  width: 100%; height: 28px; padding: 0 8px 0 26px;
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  font-size: 12px; outline: none; box-sizing: border-box;
}
.fs-search-input:focus { border-color: var(--bl-primary); }
.fs-list { max-height: 240px; overflow: auto; padding: 4px; }
.fs-opt {
  padding: 6px 10px; border-radius: 4px; cursor: pointer;
  font-size: 13px; color: var(--bl-text-1);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.fs-opt:hover { background: var(--bl-bg-hover); }
.fs-opt.is-sel { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.fs-clear { color: var(--bl-text-3); }
.fs-empty { padding: 16px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
</style>
