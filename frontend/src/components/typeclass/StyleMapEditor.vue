<!--
  样式映射编辑器(additionalProperties)。对应 event_type.type_style_map / event_level.level_style_map。
  value = { <code>: { label, color, priority, blink, ... }, ... }
  schema = additionalProperties 的对象 Schema:{ type:'object', properties:{...}, required:[...] }
-->
<template>
  <div class="sme">
    <div v-for="(entry, i) in rows" :key="i" class="sme-row">
      <input class="bl-input bl-input-sm sme-code bl-mono" :value="entry.code" :disabled="disabled"
             placeholder="编码" @change="renameCode(i, $event.target.value)" />
      <div class="sme-fields">
        <template v-for="f in subFields" :key="f.key">
          <ColorPickerField v-if="f.control === 'color'" :model-value="entry.obj[f.key]"
                            @update:model-value="setField(i, f.key, $event)" />
          <input v-else-if="f.control === 'number'" class="bl-input bl-input-sm" type="number"
                 :value="entry.obj[f.key]" :disabled="disabled" :title="f.key"
                 @input="setField(i, f.key, num($event.target.value))" />
          <button v-else-if="f.control === 'boolean'" type="button" class="sme-sw" :class="entry.obj[f.key] && 'is-on'"
                  :disabled="disabled" :title="f.key" @click="setField(i, f.key, !entry.obj[f.key])"><span></span></button>
          <input v-else class="bl-input bl-input-sm" :value="entry.obj[f.key]" :disabled="disabled" :placeholder="f.key"
                 @input="setField(i, f.key, $event.target.value)" />
        </template>
      </div>
      <button class="sme-del" type="button" :disabled="disabled" title="删除" @click="delRow(i)" v-html="BL.icon('x', 13)"></button>
    </div>
    <button v-if="!disabled" class="sme-add" type="button" @click="addRow" v-html="iconAdd"></button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { BL } from '@/lib/bl.js'
import ColorPickerField from '../ColorPickerField.vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  schema:     { type: Object, default: () => ({}) },
  disabled:   { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const iconAdd = `${BL.icon('plus', 13)}<span style="margin-left:4px">新增</span>`

const rows = computed(() => Object.entries(props.modelValue || {}).map(([code, obj]) => ({ code, obj: obj || {} })))
const subFields = computed(() => {
  const p = props.schema?.properties || {}
  return Object.entries(p).map(([key, def]) => ({ key, control: ctrl(key, def || {}) }))
})
function ctrl(key, def) {
  const t = String(def.type || 'string').toLowerCase()
  if (t === 'boolean') return 'boolean'
  if (t === 'number' || t === 'integer') return 'number'
  if (key.toLowerCase().includes('color')) return 'color'
  return 'text'
}
function num(x) { const n = Number(x); return isNaN(n) ? x : n }

function emitMap(list) {
  const out = {}
  for (const e of list) { if (e.code) out[e.code] = e.obj }
  emit('update:modelValue', out)
}
function setField(i, key, val) {
  const list = rows.value.map(r => ({ code: r.code, obj: { ...r.obj } }))
  list[i].obj[key] = val; emitMap(list)
}
function renameCode(i, code) {
  const list = rows.value.map(r => ({ code: r.code, obj: { ...r.obj } }))
  list[i].code = (code || '').trim(); emitMap(list)
}
function addRow() {
  const list = rows.value.map(r => ({ code: r.code, obj: { ...r.obj } }))
  list.push({ code: 'code_' + (list.length + 1), obj: {} }); emitMap(list)
}
function delRow(i) { emitMap(rows.value.filter((_, k) => k !== i)) }
</script>

<style scoped>
.sme { display: flex; flex-direction: column; gap: 6px; border: 1px solid var(--bl-border); border-radius: 6px; padding: 8px; background: var(--bl-bg-2); }
.sme-row { display: flex; align-items: center; gap: 6px; }
.sme-code { width: 110px; flex-shrink: 0; }
.sme-fields { flex: 1; display: flex; align-items: center; gap: 6px; min-width: 0; }
.sme-fields > * { flex: 1; min-width: 0; }
.sme-del { width: 24px; height: 24px; flex-shrink: 0; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 4px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.sme-del:hover { background: #fff1f0; color: #f53f3f; }
.sme-add { width: 100%; padding: 5px; border: 1px dashed var(--bl-border); background: transparent; border-radius: 6px; color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; }
.sme-add:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.sme-sw { width: 34px; height: 20px; flex: 0 0 34px !important; border-radius: 10px; border: 0; background: var(--bl-border-strong, #c9cdd4); position: relative; cursor: pointer; padding: 0; }
.sme-sw.is-on { background: var(--bl-primary); }
.sme-sw span { position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%; background: #fff; transition: transform .15s; }
.sme-sw.is-on span { transform: translateX(14px); }
</style>
