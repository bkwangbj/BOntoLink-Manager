<template>
  <div class="igs" ref="rootEl">
    <div :class="['igs-control', 'bl-input', size === 'sm' && 'bl-input-sm', { 'is-open': open, 'is-disabled': disabled }]" @click="toggle">
      <span v-if="curOpt" class="igs-cur-ic" v-html="BL.icon(curOpt.icon, 13)"></span>
      <span :class="['igs-value', !curOpt && 'is-ph']">{{ curOpt ? curOpt.label : placeholder }}</span>
      <span class="igs-arrow" :class="{ 'is-open': open }" v-html="BL.icon('chevronDown', 12)"></span>
    </div>

    <Teleport to="body">
      <div v-if="open" ref="popEl" class="igs-pop" :style="popStyle">
        <button v-for="o in options" :key="o.value" type="button"
                :class="['igs-opt', String(o.value) === String(modelValue) && 'is-on']" @click="pick(o)">
          <span class="igs-opt-ic" v-html="BL.icon(o.icon, 15)"></span>
          <span class="bl-truncate">{{ o.label }}</span>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },      // [{ value, label, icon }]
  placeholder: { type: String, default: '请选择' },
  columns: { type: Number, default: 2 },
  size: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)
const rootEl = ref(null), popEl = ref(null)
const popStyle = ref({})
const curOpt = computed(() => props.options.find(o => String(o.value) === String(props.modelValue)) || null)

function computePos() {
  const el = rootEl.value
  if (!el) return
  const r = el.getBoundingClientRect()
  const below = window.innerHeight - r.bottom
  const openUp = below < 260 && r.top > below
  popStyle.value = {
    position: 'fixed', left: r.left + 'px', width: Math.max(r.width, 320) + 'px',
    [openUp ? 'bottom' : 'top']: (openUp ? (window.innerHeight - r.top + 4) : (r.bottom + 4)) + 'px',
    gridTemplateColumns: `repeat(${props.columns}, 1fr)`,
    zIndex: 1360,
  }
}
function toggle() { if (props.disabled) return; open.value ? close() : openPop() }
function openPop() {
  computePos(); open.value = true
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
  close()
}
function pick(o) { emit('update:modelValue', o.value); emit('change', o.value); close() }
onBeforeUnmount(close)
</script>

<style scoped>
.igs { position: relative; }
.igs-control { display: flex; align-items: center; gap: 7px; cursor: pointer; user-select: none; padding-right: 8px; }
.igs-control.is-disabled { cursor: not-allowed; opacity: .6; }
.igs-control.is-open { border-color: var(--bl-primary); }
.igs-cur-ic { display: inline-flex; color: var(--bl-text-2); flex-shrink: 0; }
.igs-value { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.igs-value.is-ph { color: var(--bl-text-3); }
.igs-arrow { display: inline-flex; color: var(--bl-text-3); transition: transform .15s; flex-shrink: 0; }
.igs-arrow.is-open { transform: rotate(180deg); }

.igs-pop { max-width: 96vw; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px;
  box-shadow: var(--bl-shadow-3, 0 12px 32px rgba(0,0,0,.18)); padding: 8px; display: grid; gap: 8px; }
.igs-opt { display: flex; align-items: center; gap: 8px; padding: 9px 11px; border: 1px solid var(--bl-border); border-radius: 8px;
  background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; font-size: 12.5px; text-align: left; min-width: 0; }
.igs-opt:hover { border-color: var(--bl-primary); }
.igs-opt.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.igs-opt-ic { display: inline-flex; flex-shrink: 0; }
</style>
