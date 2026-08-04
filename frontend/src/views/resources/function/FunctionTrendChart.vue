<template>
  <div class="ftc">
    <VChart v-if="trend.length" class="ftc-chart" :option="option" autoresize />
    <div v-else class="bl-empty ftc-empty">所选周期内暂无调用记录</div>
  </div>
</template>

<script setup>
/**
 * 调用趋势折线图 (文档 4.6.2)
 * 与外部数据源监控 Tab 用同一套 echarts 按需引入方式, 保持图表观感统一。
 */
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const props = defineProps({
  /** [{ date, calls, errors }] */
  trend: { type: Array, default: () => [] }
})

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['调用量', '错误数'], top: 0, textStyle: { fontSize: 11 } },
  grid: { left: 46, right: 24, top: 32, bottom: 26 },
  xAxis: {
    type: 'category',
    data: props.trend.map(t => String(t.date || '').slice(5)),   // MM-DD
    axisLabel: { fontSize: 10 }
  },
  yAxis: { type: 'value', name: '次', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 } },
  series: [
    {
      name: '调用量', type: 'line', smooth: true, showSymbol: false,
      data: props.trend.map(t => Number(t.calls) || 0),
      itemStyle: { color: '#165DFF' },
      areaStyle: { color: 'rgba(22,93,255,.10)' }
    },
    {
      name: '错误数', type: 'line', smooth: true, showSymbol: false,
      data: props.trend.map(t => Number(t.errors) || 0),
      itemStyle: { color: '#F53F3F' }
    },
  ],
}))
</script>

<style scoped>
.ftc { height: 220px; }
.ftc-chart { width: 100%; height: 100%; }
.ftc-empty { height: 100%; display: flex; align-items: center; justify-content: center; font-size: 12px; }
</style>
