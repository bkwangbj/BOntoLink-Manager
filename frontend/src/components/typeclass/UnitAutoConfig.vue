<!--
  单位自动配置(复合控件)。对应文档「数值单位配置 / 深度单位配置」。
  value = { unit_text, conversion_ratio, decimal_places, auto_scale, scale_ranges:[[min,max,unit,ratio,decimal],...] }
  按数值范围自动切换:取数据集最大值从上到下依次匹配,命中首个符合区间后自动应用单位/倍率/精度。
-->
<template>
  <div class="uac">
    <div class="uac-base">
      <label class="uac-fld"><span>基准单位</span>
        <input class="bl-input bl-input-sm" :value="v.unit_text" :disabled="disabled"
               placeholder="如 m³/h" @input="patch('unit_text', $event.target.value)" /></label>
      <label class="uac-fld"><span>换算倍率</span>
        <input class="bl-input bl-input-sm" type="number" :value="v.conversion_ratio" :disabled="disabled"
               @input="patch('conversion_ratio', num($event.target.value, 1))" /></label>
      <label class="uac-fld"><span>小数位数</span>
        <input class="bl-input bl-input-sm" type="number" :value="v.decimal_places" :disabled="disabled"
               @input="patch('decimal_places', num($event.target.value, 2))" /></label>
    </div>

    <div class="uac-auto">
      <button type="button" class="uac-switch" :class="v.auto_scale && 'is-on'" :disabled="disabled"
              @click="patch('auto_scale', !v.auto_scale)"><span></span></button>
      <span class="uac-auto-txt">按数值范围自动切换单位</span>
    </div>

    <template v-if="v.auto_scale">
      <div class="uac-hint">匹配规则:取数据集最大值从上到下依次匹配,命中首个符合区间后自动切换对应单位与精度</div>
      <div class="uac-tbl">
        <div class="uac-th">
          <span>最小值</span><span>最大值</span><span>单位</span><span>换算倍率</span><span>小数位</span><span class="uac-op"></span>
        </div>
        <div v-for="(r, i) in ranges" :key="i" class="uac-tr">
          <input class="bl-input bl-input-sm" type="number" :value="r[0]" :disabled="disabled" @input="setCell(i,0,num($event.target.value,0))" />
          <input class="bl-input bl-input-sm" type="number" :value="r[1]" :disabled="disabled" placeholder="∞" @input="setCell(i,1,emptyNum($event.target.value))" />
          <input class="bl-input bl-input-sm" :value="r[2]" :disabled="disabled" @input="setCell(i,2,$event.target.value)" />
          <input class="bl-input bl-input-sm" type="number" :value="r[3]" :disabled="disabled" @input="setCell(i,3,num($event.target.value,1))" />
          <input class="bl-input bl-input-sm" type="number" :value="r[4]" :disabled="disabled" @input="setCell(i,4,num($event.target.value,2))" />
          <button class="uac-del" type="button" :disabled="disabled" title="删除区间" @click="delRange(i)" v-html="BL.icon('x', 13)"></button>
        </div>
        <button v-if="!disabled" class="uac-add" type="button" @click="addRange" v-html="iconAdd"></button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  disabled:   { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const v = computed(() => ({
  unit_text: '', conversion_ratio: 1, decimal_places: 2, auto_scale: false, scale_ranges: [],
  ...(props.modelValue || {})
}))
const ranges = computed(() => Array.isArray(v.value.scale_ranges) ? v.value.scale_ranges : [])
const iconAdd = `${BL.icon('plus', 13)}<span style="margin-left:4px">添加区间</span>`

function emitV(next) { emit('update:modelValue', next) }
function patch(k, val) { emitV({ ...v.value, [k]: val }) }
function num(x, d) { const n = Number(x); return isNaN(n) ? d : n }
function emptyNum(x) { const t = String(x).trim(); if (!t) return null; const n = Number(t); return isNaN(n) ? null : n }
function setCell(i, j, val) {
  const rs = ranges.value.map(r => [...r]); rs[i][j] = val; patch('scale_ranges', rs)
}
function addRange() { patch('scale_ranges', [...ranges.value, [0, null, '', 1, 2]]) }
function delRange(i) { patch('scale_ranges', ranges.value.filter((_, k) => k !== i)) }
</script>

<style scoped>
.uac { display: flex; flex-direction: column; gap: 8px; border: 1px solid var(--bl-border); border-radius: 6px; padding: 8px; background: var(--bl-bg-2); }
.uac-base { display: flex; gap: 8px; }
.uac-fld { flex: 1; display: flex; flex-direction: column; gap: 3px; font-size: 11.5px; color: var(--bl-text-3); }
.uac-auto { display: flex; align-items: center; gap: 8px; }
.uac-auto-txt { font-size: 12.5px; color: var(--bl-text-2); }
.uac-switch { width: 36px; height: 20px; border-radius: 10px; border: 0; background: var(--bl-border-strong, #c9cdd4); position: relative; cursor: pointer; padding: 0; }
.uac-switch.is-on { background: var(--bl-primary); }
.uac-switch span { position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%; background: #fff; transition: transform .15s; }
.uac-switch.is-on span { transform: translateX(16px); }
.uac-hint { font-size: 11px; color: var(--bl-text-3); line-height: 1.5; }
.uac-tbl { display: flex; flex-direction: column; gap: 5px; }
.uac-th, .uac-tr { display: grid; grid-template-columns: 1fr 1fr 0.9fr 1.1fr 0.9fr 32px; gap: 5px; align-items: center; }
.uac-th { font-size: 11px; color: var(--bl-text-3); }
.uac-op { text-align: center; }
.uac-del { width: 24px; height: 24px; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 4px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.uac-del:hover { background: #fff1f0; color: #f53f3f; }
.uac-add { width: 100%; padding: 5px; border: 1px dashed var(--bl-border); background: transparent; border-radius: 6px; color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; }
.uac-add:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
</style>
