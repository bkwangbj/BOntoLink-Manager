<template>
  <div class="fci" ref="rootEl">
    <input class="bl-input fci-input" :value="modelValue" :placeholder="placeholder"
           @input="onInput" @focus="open = true" @keydown.enter.prevent="close" @keydown.esc="close" />
    <span class="fci-arrow" :class="{ 'is-open': open }" @click="toggle" v-html="BL.icon('chevronDown', 12)"></span>
    <div v-if="open" class="fci-pop">
      <div v-for="o in filtered" :key="o" class="fci-opt" :class="{ 'is-sel': o === modelValue }"
           @mousedown.prevent="pick(o)">
        <span class="bl-truncate">{{ o }}</span>
        <span v-if="o === modelValue" v-html="BL.icon('check', 11)"></span>
      </div>
      <div v-if="isNew" class="fci-new" @mousedown.prevent="close">
        <span v-html="BL.icon('plus', 11)"></span>「{{ modelValue }}」不存在,创建函数时将自动新建该目录
      </div>
      <div v-if="!filtered.length && !isNew" class="fci-empty">无可选项,可直接输入新建</div>
    </div>
  </div>
</template>

<script setup>
/**
 * 可输入下拉 (combobox)
 *
 * 文档 3.2.1:行业 / 领域下拉「支持直接输入,输入不存在的值将自动创建」——
 * 既不是纯下拉也不是纯输入框, 故不用 BlSelect, 单独做一个轻量 combobox。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  modelValue: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '选择或直接输入' }
})
const emit = defineEmits(['update:modelValue'])

const open = ref(false)
const rootEl = ref(null)

const filtered = computed(() => {
  const k = String(props.modelValue || '').trim().toLowerCase()
  if (!k) return props.options
  return props.options.filter(o => String(o).toLowerCase().includes(k))
})
/** 当前输入值不在已有选项里 → 提示将自动创建 */
const isNew = computed(() => {
  const v = String(props.modelValue || '').trim()
  return !!v && !props.options.some(o => String(o) === v)
})

function onInput(e) { emit('update:modelValue', e.target.value); open.value = true }
function pick(o) { emit('update:modelValue', o); close() }
function toggle() { open.value = !open.value }
function close() { open.value = false }
function onDocClick(e) { if (rootEl.value && !rootEl.value.contains(e.target)) close() }
onMounted(() => window.addEventListener('click', onDocClick))
onUnmounted(() => window.removeEventListener('click', onDocClick))
</script>

<style scoped>
.fci { position: relative; }
.fci-input { width: 100%; padding-right: 26px; }
.fci-arrow {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  color: var(--bl-text-3); cursor: pointer; display: inline-flex; transition: transform .15s;
}
.fci-arrow.is-open { transform: translateY(-50%) rotate(180deg); }
.fci-pop {
  position: absolute; top: calc(100% + 4px); left: 0; right: 0; z-index: 1310;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, .14); padding: 4px; max-height: 220px; overflow: auto;
}
.fci-opt {
  display: flex; align-items: center; justify-content: space-between; gap: 6px;
  padding: 6px 8px; border-radius: 4px; font-size: 13px; cursor: pointer; color: var(--bl-text-1);
}
.fci-opt:hover { background: var(--bl-bg-hover); }
.fci-opt.is-sel { color: var(--bl-primary); }
.fci-new {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px; font-size: 12px; color: var(--bl-primary); cursor: pointer;
  border-top: 1px solid var(--bl-divider); margin-top: 4px;
}
.fci-empty { padding: 8px; font-size: 12px; color: var(--bl-text-3); }
</style>
