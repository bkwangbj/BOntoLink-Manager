<!--
  实时预览:根据类型类渲染对应预览效果(文档 1.5(3))。
  已实现:曲线样式(timeseries_measure)、分组着色(component_subtype)、事件等级卡片(event_intent/level)、
          数值单位换算(*_units)。其余回退「当前类型类无可视化预览」。
-->
<template>
  <!-- 曲线样式:颜色 + 线型 + Y 轴归属 -->
  <div v-if="kind === 'curve'" class="pp-curve">
    <div class="pp-axis"><span>← 左轴</span><span>右轴 →</span></div>
    <svg viewBox="0 0 240 80" class="pp-svg" preserveAspectRatio="none">
      <polyline points="0,60 40,40 80,50 120,20 160,35 200,15 240,25" fill="none"
                :stroke="values.default_color || '#3b82f6'" stroke-width="2.5" :stroke-dasharray="dash" />
    </svg>
    <div class="pp-cap">颜色: {{ values.default_color || '自动' }} ｜ 线型: {{ values.line_type || 'solid' }} ｜ 归属: {{ values.axis_position === 'right' ? '右轴' : '左轴' }}</div>
  </div>

  <!-- 分组着色 -->
  <div v-else-if="kind === 'subtype'" class="pp-chip" :style="{ background: (values.group_color || '#3b82f6') + '22', color: values.group_color || '#3b82f6', borderColor: values.group_color || '#3b82f6' }">
    {{ values.group_name || values.subtype_code || '分组' }}
  </div>

  <!-- 事件等级卡片 -->
  <div v-else-if="kind === 'intent'" class="pp-intent" :class="values.blink && 'is-blink'"
       :style="{ background: (values.color || '#7c3aed') + '22', color: values.color || '#7c3aed' }">
    {{ intentLabel }} · 优先级 {{ values.priority ?? '-' }}
  </div>

  <!-- 数值单位换算 -->
  <div v-else-if="kind === 'unit'" class="pp-unit">
    <div class="pp-unit-num">12345.68 <span>{{ values.unit_name || values.unit_text || '' }}</span></div>
    <div class="pp-cap">保留 {{ values.precision ?? values.decimal_places ?? 2 }} 位小数</div>
  </div>

  <div v-else class="pp-none">当前类型类无可视化预览</div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  namePrefix: { type: String, default: '' },
  category:   { type: String, default: '' },
  values:     { type: Object, default: () => ({}) }
})

const kind = computed(() => {
  const n = (props.namePrefix || '').toLowerCase()
  if (n.includes('measure') || n === 'timeseries_measure') return 'curve'
  if (n.includes('subtype')) return 'subtype'
  if (n.includes('intent') || n.includes('level')) return 'intent'
  if (n.includes('unit')) return 'unit'
  return 'none'
})
const dash = computed(() => {
  const t = props.values.line_type
  return t === 'dashed' ? '6,5' : t === 'dotted' ? '2,4' : '0'
})
const INTENT_CN = { info: '提示事件', warning: '警告事件', error: '错误事件', critical: '严重事件' }
const intentLabel = computed(() => INTENT_CN[props.values.intent_type] || '事件')
</script>

<style scoped>
.pp-curve { width: 100%; padding: 10px 14px; box-sizing: border-box; }
.pp-axis { display: flex; justify-content: space-between; font-size: 11px; color: var(--bl-text-3); margin-bottom: 4px; }
.pp-svg { width: 100%; height: 70px; display: block; }
.pp-cap { font-size: 11px; color: var(--bl-text-3); text-align: center; margin-top: 6px; }
.pp-chip { padding: 8px 20px; border-radius: 8px; border: 1px solid; font-size: 14px; font-weight: 600; }
.pp-intent { padding: 10px 22px; border-radius: 8px; font-size: 14px; font-weight: 600; }
.pp-intent.is-blink { animation: pp-blink 1s steps(2) infinite; }
@keyframes pp-blink { 50% { opacity: .45 } }
.pp-unit { text-align: center; }
.pp-unit-num { font-size: 28px; font-weight: 700; color: var(--bl-text-1); }
.pp-unit-num span { font-size: 15px; color: var(--bl-text-3); }
.pp-none { font-size: 13px; color: var(--bl-text-3); }
</style>
