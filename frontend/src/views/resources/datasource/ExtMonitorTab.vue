<template>
  <div class="emn">
    <!-- 4 个核心指标卡 -->
    <div class="emn-cards">
      <div v-for="c in cards" :key="c.k" class="emn-card">
        <div class="emn-card-t">{{ c.title }}</div>
        <div class="emn-card-v" :style="{ color: c.color }">{{ c.value }}</div>
        <div v-if="c.delta !== null && c.delta !== undefined" class="emn-card-d" :class="c.good ? 'is-good' : 'is-bad'">
          <span v-html="BL.icon(c.delta >= 0 ? 'chevronUp' : 'chevronDown', 10)"></span>
          {{ Math.abs(c.delta) }}{{ c.unit }} 较上周期
        </div>
        <div v-else class="emn-card-d is-none">暂无对比数据</div>
      </div>
    </div>

    <!-- 调用趋势 -->
    <div class="emn-panel">
      <div class="emn-panel-hd"><span>调用趋势</span><span style="flex:1"></span>
        <BlSelect v-model="days" :options="DAY_OPTS" size="sm" style="width:110px" @change="load" />
      </div>
      <div v-if="!trendReady" class="emn-blank">统计周期内暂无调用数据</div>
      <VChart v-else class="emn-chart" :option="trendOption" autoresize />
    </div>

    <div class="emn-row">
      <!-- 错误类型分布 -->
      <div class="emn-panel emn-half">
        <div class="emn-panel-hd"><span>错误类型分布</span></div>
        <div v-if="!errorList.length" class="emn-blank">统计周期内没有异常调用</div>
        <VChart v-else class="emn-chart" :option="errorOption" autoresize @click="onErrorClick" />
      </div>

      <!-- 接口调用量 TOP5 -->
      <div class="emn-panel emn-half">
        <div class="emn-panel-hd"><span>接口调用量排行 TOP5</span></div>
        <div v-if="!topList.length" class="emn-blank">统计周期内暂无调用数据</div>
        <div v-else class="emn-top">
          <div v-for="t in topList" :key="t.interface_id" class="emn-top-row" @click="$emit('goto-logs', { interfaceId: t.interface_id })">
            <span class="emn-top-name bl-truncate" :title="t.api_name">{{ t.api_name }}</span>
            <div class="emn-top-bar"><i :style="{ width: barW(t.cnt) }"></i></div>
            <span class="emn-top-n">{{ t.cnt }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { extDatasourceApi } from '@/api'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const props = defineProps({ dsId: { type: String, required: true } })
const emit = defineEmits(['goto-logs'])

const DAY_OPTS = [{ value: 7, label: '近 7 天' }, { value: 30, label: '近 30 天' }, { value: 90, label: '近 90 天' }]
const ERROR_META = {
  timeout: { label: '超时错误', color: '#FF7D00', status: 3 },
  server: { label: '服务端错误', color: '#F53F3F', status: 2 },
  auth: { label: '鉴权失败', color: '#FADB14', status: 4 },
  network: { label: '网络异常', color: '#722ED1', status: 2 },
  other: { label: '其他错误', color: '#86909C', status: 2 },
}

const days = ref(30)
const data = ref({})

async function load() {
  data.value = await extDatasourceApi.monitor(props.dsId, days.value).catch(() => ({}))
}
onMounted(load)
defineExpose({ reload: load })

/* 正向优化标绿, 负向恶化标红 */
const cards = computed(() => {
  const d = data.value
  return [
    { k: 'total', title: '总调用次数', value: d.total ?? 0, delta: d.totalDelta, unit: '%', good: (d.totalDelta ?? 0) >= 0, color: 'var(--bl-text-1)' },
    { k: 'rate', title: '调用成功率', value: (d.successRate ?? 0) + '%', delta: d.successRateDelta, unit: '%', good: (d.successRateDelta ?? 0) >= 0, color: '#00B42A' },
    { k: 'cost', title: '平均响应耗时', value: (d.avgCost ?? 0) + ' ms', delta: null, unit: 'ms', good: true, color: 'var(--bl-text-1)' },
    { k: 'fail', title: '错误次数', value: d.failed ?? 0, delta: d.failedDelta, unit: '%', good: (d.failedDelta ?? 0) < 0, color: '#F53F3F' },
  ]
})

const trend = computed(() => data.value.trend || [])
const trendReady = computed(() => trend.value.length > 0)
const errorList = computed(() => (data.value.errors || []).map(e => ({ ...e, meta: ERROR_META[e.kind] || ERROR_META.other })))
const topList = computed(() => data.value.top || [])
const topMax = computed(() => Math.max(1, ...topList.value.map(t => Number(t.cnt) || 0)))
function barW(n) { return Math.max(4, (Number(n) || 0) / topMax.value * 100) + '%' }

/* 双 Y 轴: 左调用量, 右成功率/耗时 */
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['调用量', '成功率', '平均耗时'], top: 0, textStyle: { fontSize: 11 } },
  grid: { left: 44, right: 52, top: 34, bottom: 26 },
  xAxis: { type: 'category', data: trend.value.map(t => t.day), axisLabel: { fontSize: 10 } },
  yAxis: [
    { type: 'value', name: '次', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 } },
    { type: 'value', name: '%/ms', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 } },
  ],
  series: [
    { name: '调用量', type: 'line', smooth: true, data: trend.value.map(t => Number(t.total) || 0), itemStyle: { color: '#165DFF' } },
    { name: '成功率', type: 'line', smooth: true, yAxisIndex: 1, itemStyle: { color: '#00B42A' },
      data: trend.value.map(t => { const tot = Number(t.total) || 0; return tot ? Math.round((Number(t.success) || 0) / tot * 1000) / 10 : 0 }) },
    { name: '平均耗时', type: 'line', smooth: true, yAxisIndex: 1, itemStyle: { color: '#FF7D00' },
      data: trend.value.map(t => Math.round(Number(t.avg_cost) || 0)) },
  ],
}))

const errorOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
  legend: { orient: 'vertical', right: 4, top: 'center', textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie', radius: ['46%', '70%'], center: ['38%', '52%'], avoidLabelOverlap: true,
    label: { show: true, position: 'center', formatter: () => `${totalErrors.value}\n总错误`, fontSize: 12, lineHeight: 18, color: '#86909C' },
    emphasis: { label: { show: true, fontSize: 13 } },
    data: errorList.value.map(e => ({ name: e.meta.label, value: Number(e.cnt) || 0, kind: e.kind, itemStyle: { color: e.meta.color } })),
  }],
}))
const totalErrors = computed(() => errorList.value.reduce((s, e) => s + (Number(e.cnt) || 0), 0))

/* 点饼图某块 → 跳日志页并预置该错误状态 */
function onErrorClick(p) {
  const kind = p?.data?.kind
  const st = ERROR_META[kind]?.status
  if (st) emit('goto-logs', { callStatus: st })
}
</script>

<style scoped>
.emn { display: flex; flex-direction: column; gap: 14px; padding: 14px 0; }
.emn-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.emn-card { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px; padding: 14px 16px; }
.emn-card-t { font-size: 12px; color: var(--bl-text-3); }
.emn-card-v { font-size: 24px; font-weight: 600; margin: 6px 0 4px; }
.emn-card-d { display: inline-flex; align-items: center; gap: 3px; font-size: 11.5px; }
.emn-card-d.is-good { color: #00B42A; }
.emn-card-d.is-bad { color: #F53F3F; }
.emn-card-d.is-none { color: var(--bl-text-3); }
.emn-panel { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px; padding: 12px 16px 14px; }
.emn-panel-hd { display: flex; align-items: center; font-size: 13px; font-weight: 600; color: var(--bl-text-1); margin-bottom: 8px; }
.emn-chart { height: 260px; width: 100%; }
.emn-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.emn-half .emn-chart { height: 220px; }
.emn-blank { height: 200px; display: flex; align-items: center; justify-content: center; color: var(--bl-text-3); font-size: 12px; }
.emn-top { display: flex; flex-direction: column; gap: 12px; padding: 10px 0; }
.emn-top-row { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.emn-top-row:hover .emn-top-name { color: var(--bl-primary); }
.emn-top-name { flex: 0 0 130px; font-size: 12px; color: var(--bl-text-2); }
.emn-top-bar { flex: 1; height: 10px; background: var(--bl-bg-2); border-radius: 5px; overflow: hidden; }
.emn-top-bar i { display: block; height: 100%; background: var(--bl-primary); border-radius: 5px; }
.emn-top-n { flex: 0 0 48px; text-align: right; font-size: 12px; color: var(--bl-text-2); }
</style>
