<!--
  时序图表(P1:类型类真实渲染)。
  读该对象类型的时序类型类绑定(tcRenderApi.resolve → resolveTimeseries),
  把渲染指令真实落到 ECharts:颜色/线型(solid/dashed/dotted)/左右 Y 轴/单位/小数位/
  枚举柱状(is_enum)/Y 轴倒置(is_value_inverted+depth)/断点插值(step)。
  数据为演示用合成序列(真实接入时替换为实例时序数据)。
-->
<template>
  <div class="tsc">
    <div class="tsc-hd">
      <span class="tsc-title">时序图表</span>
      <span class="tsc-sub">按绑定的时序类型类真实渲染 · 演示数据</span>
      <span class="bl-grow"></span>
      <button class="bl-btn bl-btn-sm" @click="reload" v-html="iconRefresh"></button>
    </div>

    <div v-if="loading" class="tsc-empty">加载中…</div>
    <div v-else-if="!ts.series.length" class="tsc-empty">
      该对象类型未绑定时序度量类型类。<br>
      到「属性 → 某属性 → 类型类」绑定 <b>timeseries_measure / timeseries_units / timeseries_is_enum</b> 等后,此处按配置真实渲染。
    </div>
    <template v-else>
      <!-- 绑定摘要 -->
      <div class="tsc-legend">
        <span v-for="s in ts.series" :key="s.property_id" class="tsc-chip" :style="{ borderColor: s.color || '#c9cdd4' }">
          <span class="tsc-dot" :style="{ background: s.color || '#86909c' }"></span>
          {{ s.display_name || s.api_name }}<span v-if="s.unit" class="tsc-unit">({{ s.unit }})</span>
          <span class="tsc-tag">{{ s.isEnum ? '枚举柱' : lineCn(s.lineType) }} · {{ s.axis === 'right' ? '右轴' : '左轴' }}<span v-if="s.inverted"> · 倒置</span></span>
        </span>
        <span v-if="ts.interpolation && ts.interpolation !== 'linear'" class="tsc-flag">插值:{{ interpCn(ts.interpolation) }}</span>
      </div>
      <v-chart class="tsc-chart" :option="option" autoresize />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { BL } from '@/lib/bl.js'
import { tcRenderApi } from '@/api'
import { resolveTimeseries } from '@/lib/tcResolver.js'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const props = defineProps({ classId: { type: String, default: '' } })
const iconRefresh = `${BL.icon('refresh', 13)}<span style="margin-left:4px">刷新</span>`

const loading = ref(false)
const ts = ref({ series: [], interpolation: null, deprecatedFilter: false })

const X = ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
/* 合成演示数据:每条 series 一条有辨识度的曲线(枚举→0/1/2 分类) */
function mockData(s, i) {
  if (s.isEnum) return [0, 1, 0, 2, 1, 0, 1]
  const base = [10, 14, 22, 18, 26, 20, 16]
  return base.map((v, k) => Math.round((v + i * 6 + Math.sin(k + i) * 3) * 10) / 10)
}
function lineCn(t) { return t === 'dashed' ? '虚线' : t === 'dotted' ? '点线' : '实线' }
function interpCn(t) { return { step: '阶梯', forward: '前向填充', backward: '后向填充', none: '不插值' }[t] || '线性' }

const option = computed(() => {
  const series = ts.value.series
  const leftUnit = (series.find(s => s.axis !== 'right') || {}).unit || ''
  const rightUnit = (series.find(s => s.axis === 'right') || {}).unit || ''
  const leftInv = series.some(s => s.axis !== 'right' && s.inverted)
  const rightInv = series.some(s => s.axis === 'right' && s.inverted)
  const hasRight = series.some(s => s.axis === 'right')
  const interp = ts.value.interpolation

  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, data: series.map(seriesName) },
    grid: { left: 46, right: hasRight ? 52 : 20, top: 34, bottom: 30 },
    xAxis: { type: 'category', boundaryGap: series.some(s => s.isEnum), data: X },
    yAxis: [
      { type: 'value', position: 'left', inverse: leftInv, name: leftUnit, nameTextStyle: { align: 'right' } },
      { type: 'value', position: 'right', inverse: rightInv, name: rightUnit, show: hasRight }
    ],
    series: series.map((s, i) => ({
      name: seriesName(s),
      type: s.isEnum ? 'bar' : 'line',
      yAxisIndex: s.axis === 'right' ? 1 : 0,
      data: mockData(s, i),
      smooth: false,
      step: (!s.isEnum && interp === 'step') ? 'middle' : false,
      connectNulls: !!interp && interp !== 'none',
      barWidth: s.isEnum ? '46%' : undefined,
      itemStyle: s.color ? { color: s.color } : undefined,
      lineStyle: s.isEnum ? undefined : { type: s.lineType || 'solid', color: s.color || undefined, width: 2 },
      symbol: s.isEnum ? 'none' : 'circle'
    }))
  }
})
function seriesName(s) { return (s.display_name || s.api_name || '') + (s.unit ? `(${s.unit})` : '') }

async function reload() {
  if (!props.classId) { ts.value = { series: [], interpolation: null, deprecatedFilter: false }; return }
  loading.value = true
  try {
    const payload = await tcRenderApi.resolve({ classId: props.classId })
    ts.value = resolveTimeseries(payload)
  } catch { ts.value = { series: [], interpolation: null, deprecatedFilter: false } }
  finally { loading.value = false }
}
watch(() => props.classId, reload, { immediate: true })
</script>

<style scoped>
.tsc { display: flex; flex-direction: column; height: 100%; min-height: 0; padding: 4px 2px; }
.tsc-hd { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.tsc-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.tsc-sub { font-size: 12px; color: var(--bl-text-3); }
.tsc-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; font-size: 13px; color: var(--bl-text-3); line-height: 1.9; padding: 40px; }
.tsc-legend { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.tsc-chip { display: inline-flex; align-items: center; gap: 5px; border: 1px solid; border-radius: 14px; padding: 3px 10px; font-size: 12px; color: var(--bl-text-1); }
.tsc-dot { width: 9px; height: 9px; border-radius: 50%; }
.tsc-unit { color: var(--bl-text-3); }
.tsc-tag { font-size: 10.5px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 3px; padding: 0 5px; margin-left: 2px; }
.tsc-flag { font-size: 11.5px; color: var(--bl-primary); background: var(--bl-primary-soft); border-radius: 4px; padding: 2px 8px; }
.tsc-chart { flex: 1; min-height: 320px; width: 100%; }
</style>
