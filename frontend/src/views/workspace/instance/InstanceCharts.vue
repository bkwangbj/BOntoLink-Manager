<template>
  <div class="ixc-root">
    <!-- 工具栏 -->
    <div class="ixc-toolbar">
      <span class="bl-muted" style="font-size:12px">图表探查 · {{ charts.length }} 个图表</span>
      <span class="bl-grow"></span>
      <div class="ixc-add-group">
        <button v-for="t in CHART_TYPES" :key="t.type" class="ixc-add" :title="'添加'+t.label" @click="addChart(t.type)">
          <span v-html="BL.icon(t.icon, 13)"></span> {{ t.label }}
        </button>
      </div>
      <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="!charts.length" @click="clearAll"
              v-html="iconText('trash','清空')"></button>
      <span class="ixc-tb-sep"></span>
      <button class="bl-btn bl-btn-sm bl-btn-primary" @click="$emit('save-layout')"
              v-html="iconText2('save','保存布局')"></button>
    </div>

    <!-- 画布 -->
    <div class="ixc-canvas">
      <div v-if="!charts.length" class="bl-empty ixc-empty">
        <span v-html="BL.icon('barChart', 32, 'var(--bl-text-3)')"></span>
        <div style="margin-top:10px">点击上方按钮添加图表，对「{{ typeName }}」的实例做可视化分析</div>
      </div>

      <div v-for="ch in charts" :key="ch.id" class="ixc-card" :class="ch.type==='singleStat' && 'is-stat'">
        <!-- 卡头 -->
        <div class="ixc-card-hd">
          <span class="ixc-card-ic" v-html="BL.icon(iconOf(ch.type), 13, 'var(--bl-primary)')"></span>
          <input class="ixc-title" v-model="ch.title" :placeholder="defaultTitle(ch)" />
          <button class="ixc-card-btn" :class="ch.configOpen && 'is-on'" title="配置" @click="ch.configOpen=!ch.configOpen"
                  v-html="BL.icon('settings', 13)"></button>
          <button class="ixc-card-btn" title="删除" @click="removeChart(ch.id)" v-html="BL.icon('x', 14)"></button>
        </div>

        <!-- 配置区 -->
        <div v-if="ch.configOpen" class="ixc-config">
          <div class="ixc-cfg-row">
            <label>类型</label>
            <select class="bl-input bl-input-sm" v-model="ch.type" @change="onTypeChange(ch)">
              <option v-for="t in CHART_TYPES" :key="t.type" :value="t.type">{{ t.label }}</option>
            </select>
          </div>
          <template v-if="ch.type==='grid'">
            <div class="ixc-cfg-row"><label>行维度</label>
              <select class="bl-input bl-input-sm" v-model="ch.rowField" @change="load(ch)">
                <option v-for="f in dimFields" :key="f.field" :value="f.field">{{ f.label }}</option>
              </select></div>
            <div class="ixc-cfg-row"><label>列维度</label>
              <select class="bl-input bl-input-sm" v-model="ch.colField" @change="load(ch)">
                <option v-for="f in dimFields" :key="f.field" :value="f.field">{{ f.label }}</option>
              </select></div>
          </template>
          <div v-else-if="ch.type!=='singleStat'" class="ixc-cfg-row"><label>分组依据</label>
            <select class="bl-input bl-input-sm" v-model="ch.groupBy" @change="load(ch)">
              <option v-for="f in dimFields" :key="f.field" :value="f.field">{{ f.label }}</option>
            </select></div>
          <div v-if="ch.type!=='listogram'" class="ixc-cfg-row"><label>指标</label>
            <select class="bl-input bl-input-sm" v-model="ch.metric" @change="load(ch)">
              <option value="">（计数）</option>
              <option v-for="f in numFields" :key="f.field" :value="f.field">{{ f.label }}</option>
            </select></div>
          <div class="ixc-cfg-row"><label>聚合方式</label>
            <select class="bl-input bl-input-sm" v-model="ch.agg" @change="load(ch)">
              <option v-for="a in AGGS" :key="a.v" :value="a.v">{{ a.t }}</option>
            </select></div>
        </div>

        <!-- 渲染区 -->
        <div class="ixc-card-body">
          <div v-if="ch.loading" class="ixc-loading">加载中…</div>

          <!-- 列表直方图 -->
          <template v-else-if="ch.type==='listogram'">
            <div v-if="!ch.data?.length" class="ixc-nodata">无数据</div>
            <div v-for="b in ch.data" :key="b.key" class="ixc-bar-row">
              <span class="ixc-bar-label bl-truncate" :title="b.key">{{ b.key }}</span>
              <div class="ixc-bar-track"><div class="ixc-bar-fill" :style="{ width: barW(ch, b)+'%' }"></div></div>
              <span class="ixc-bar-val">{{ fmtNum(valOf(ch, b)) }}</span>
            </div>
          </template>

          <!-- 统计表 -->
          <template v-else-if="ch.type==='statsTable'">
            <table class="bl-table ixc-stable">
              <thead><tr>
                <th @click="sortTable(ch,'key')">{{ labelOf(ch.groupBy) }}</th>
                <th class="ixc-num" @click="sortTable(ch,'count')">计数</th>
                <th class="ixc-num" @click="sortTable(ch,'sum')">总和</th>
                <th class="ixc-num" @click="sortTable(ch,'avg')">平均</th>
                <th class="ixc-num" @click="sortTable(ch,'min')">最小</th>
                <th class="ixc-num" @click="sortTable(ch,'max')">最大</th>
              </tr></thead>
              <tbody>
                <tr v-for="b in ch.data" :key="b.key">
                  <td class="bl-truncate">{{ b.key }}</td>
                  <td class="ixc-num">{{ b.count }}</td>
                  <td class="ixc-num">{{ fmtNum(b.sum) }}</td>
                  <td class="ixc-num">{{ fmtNum(b.avg) }}</td>
                  <td class="ixc-num">{{ fmtNum(b.min) }}</td>
                  <td class="ixc-num">{{ fmtNum(b.max) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="!ch.data?.length" class="ixc-nodata">无数据</div>
          </template>

          <!-- 单一统计 -->
          <template v-else-if="ch.type==='singleStat'">
            <div class="ixc-single">
              <div class="ixc-single-num">{{ fmtNum(singleVal(ch)) }}</div>
              <div class="ixc-single-label">{{ aggLabel(ch.agg) }}{{ ch.metric ? ' · '+labelOf(ch.metric) : ' · 实例数' }}</div>
            </div>
          </template>

          <!-- 网格图 -->
          <template v-else-if="ch.type==='grid'">
            <div v-if="!ch.data?.cells?.length" class="ixc-nodata">无数据</div>
            <div v-else class="ixc-grid-wrap">
              <table class="ixc-grid">
                <thead><tr><th></th><th v-for="c in ch.data.cols" :key="c" class="bl-truncate">{{ c }}</th></tr></thead>
                <tbody>
                  <tr v-for="r in ch.data.rows" :key="r">
                    <th class="ixc-grid-rowh bl-truncate">{{ r }}</th>
                    <td v-for="c in ch.data.cols" :key="c" class="ixc-grid-cell"
                        :style="cellStyle(ch, r, c)">{{ fmtNum(cellVal(ch, r, c)) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { instanceApi } from '@/api'

const props = defineProps({
  classId: { type: String, required: true },
  typeName: { type: String, default: '' },
  columns: { type: Array, default: () => [] },     // [{field,label,dataType}]
  filterParams: { type: Object, default: () => ({}) }  // { q, filter }
})
defineEmits(['save-layout'])

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }
function iconText2(ic, t) { return `${BL.icon(ic, 12, '#fff')}<span style="margin-left:4px">${t}</span>` }

const CHART_TYPES = [
  { type: 'listogram', label: '列表直方图', icon: 'barChart' },
  { type: 'statsTable', label: '统计表', icon: 'list' },
  { type: 'singleStat', label: '单一统计', icon: 'hash' },
  { type: 'grid', label: '网格图', icon: 'grid' }
]
const AGGS = [
  { v: 'count', t: '计数' }, { v: 'sum', t: '总和' }, { v: 'avg', t: '平均值' },
  { v: 'min', t: '最小值' }, { v: 'max', t: '最大值' }
]

const numFields = computed(() => props.columns.filter(c => c.dataType === 'decimal' || c.dataType === 'int'))
const dimFields = computed(() => props.columns.filter(c => c.dataType === 'string' || c.dataType === 'enum' || c.dataType === 'boolean' || c.dataType === 'date'))

let seq = 0
const charts = ref([])

function defaults() {
  return {
    groupBy: dimFields.value[0]?.field || props.columns[0]?.field || '',
    metric: numFields.value[0]?.field || '',
    rowField: dimFields.value[0]?.field || '',
    colField: dimFields.value[1]?.field || dimFields.value[0]?.field || '',
    agg: 'count'
  }
}

function addChart(type) {
  const d = defaults()
  const ch = { id: 'c' + (++seq), type, title: '', configOpen: false, loading: false, data: null,
    sortKey: 'value', sortDesc: true, ...d }
  if (type === 'statsTable' || type === 'singleStat') ch.agg = 'sum'
  charts.value.push(ch)
  load(ch)
}
function removeChart(id) { charts.value = charts.value.filter(c => c.id !== id) }
function clearAll() { charts.value = [] }
function onTypeChange(ch) {
  if ((ch.type === 'statsTable' || ch.type === 'singleStat') && ch.agg === 'count') ch.agg = 'sum'
  load(ch)
}

async function load(ch) {
  if (!props.classId) return
  ch.loading = true
  const base = { classId: props.classId, ...props.filterParams }
  try {
    if (ch.type === 'listogram') {
      ch.data = await instanceApi.aggregate({ ...base, groupBy: ch.groupBy, metric: ch.metric, agg: ch.agg, limit: 12 })
    } else if (ch.type === 'statsTable') {
      ch.data = await instanceApi.aggregate({ ...base, groupBy: ch.groupBy, metric: ch.metric, agg: ch.agg, limit: 20 })
    } else if (ch.type === 'singleStat') {
      ch.data = await instanceApi.stat({ ...base, metric: ch.metric })
    } else if (ch.type === 'grid') {
      ch.data = await instanceApi.matrix({ ...base, rowField: ch.rowField, colField: ch.colField, metric: ch.metric, agg: ch.agg, limit: 6 })
    }
  } catch { ch.data = null }
  ch.loading = false
}

/* 筛选变化 → 全部图表重载 */
watch(() => [props.filterParams, props.classId], () => charts.value.forEach(load), { deep: true })

/* —— 展示辅助 —— */
function iconOf(type) { return CHART_TYPES.find(t => t.type === type)?.icon || 'barChart' }
function labelOf(field) { return props.columns.find(c => c.field === field)?.label || field }
function aggLabel(a) { return AGGS.find(x => x.v === a)?.t || a }
function defaultTitle(ch) {
  if (ch.type === 'singleStat') return `${labelOf(ch.metric) || '实例数'} ${aggLabel(ch.agg)}`
  if (ch.type === 'grid') return `${labelOf(ch.rowField)} × ${labelOf(ch.colField)}`
  return `${labelOf(ch.groupBy)} 分布`
}
function valOf(ch, b) { return ch.metric ? b[ch.agg] : b.count }
function barW(ch, b) {
  const max = Math.max(...ch.data.map(x => valOf(ch, x)), 1)
  return Math.max(2, (valOf(ch, b) / max) * 100)
}
function singleVal(ch) {
  const d = ch.data || {}
  return ch.metric ? d[ch.agg] : d.count
}
function fmtNum(v) {
  if (v === null || v === undefined) return '—'
  const n = Number(v)
  if (Number.isNaN(n)) return v
  return n.toLocaleString(undefined, { maximumFractionDigits: 2 })
}
function sortTable(ch, key) {
  if (ch.sortKey === key) ch.sortDesc = !ch.sortDesc
  else { ch.sortKey = key; ch.sortDesc = true }
  const k = key, desc = ch.sortDesc
  ch.data = [...ch.data].sort((a, b) => {
    const va = a[k], vb = b[k]
    let c = (typeof va === 'number' && typeof vb === 'number') ? va - vb : String(va).localeCompare(String(vb))
    return desc ? -c : c
  })
}
/* 网格图单元 */
function cellMap(ch) {
  if (ch._cellMap && ch._cellMapData === ch.data) return ch._cellMap
  const m = {}
  ;(ch.data?.cells || []).forEach(c => { m[c.row + '' + c.col] = c.value })
  ch._cellMap = m; ch._cellMapData = ch.data
  return m
}
function cellVal(ch, r, c) { return cellMap(ch)[r + '' + c] ?? 0 }
function cellStyle(ch, r, c) {
  const v = cellVal(ch, r, c)
  const min = ch.data?.min ?? 0, max = ch.data?.max ?? 1
  const ratio = max > min ? (v - min) / (max - min) : 0
  // 琥珀色热力(浅→深)
  const bg = `rgba(255,125,0,${0.12 + ratio * 0.7})`
  return { background: bg, color: ratio > 0.55 ? '#fff' : 'var(--bl-text-1)' }
}

/* —— 供"保存设计"用:导出/导入图表配置(不含运行数据) —— */
const CFG_KEYS = ['type', 'title', 'groupBy', 'metric', 'rowField', 'colField', 'agg', 'sortKey', 'sortDesc']
function getConfig() {
  return charts.value.map(ch => { const o = {}; CFG_KEYS.forEach(k => o[k] = ch[k]); return o })
}
function setConfig(arr) {
  charts.value = (arr || []).map(cfg => ({
    ...defaults(), sortKey: 'value', sortDesc: true, ...cfg,
    id: 'c' + (++seq), configOpen: false, loading: false, data: null
  }))
  charts.value.forEach(load)
}
defineExpose({ getConfig, setConfig })
</script>

<style scoped>
.ixc-root { flex: 1; display: flex; flex-direction: column; min-height: 0; background: var(--bl-bg-2); }
.ixc-toolbar { display: flex; align-items: center; gap: 8px; padding: 8px 14px; border-bottom: 1px solid var(--bl-divider); background: var(--bl-bg-1); }
.ixc-tb-sep { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 2px; }
.ixc-add-group { display: flex; gap: 4px; }
.ixc-add {
  display: inline-flex; align-items: center; gap: 4px; padding: 5px 10px; font-size: 12px;
  border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 6px; cursor: pointer; color: var(--bl-text-2);
}
.ixc-add:hover { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }
.ixc-add span { display: inline-flex; }

.ixc-canvas { flex: 1; overflow: auto; padding: 14px; display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 12px; align-content: start; }
.ixc-empty { grid-column: 1/-1; padding: 70px 20px; text-align: center; color: var(--bl-text-3); flex-direction: column; }
.ixc-card { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; display: flex; flex-direction: column; overflow: hidden; }
.ixc-card.is-stat { min-height: 0; }
.ixc-card-hd { display: flex; align-items: center; gap: 6px; padding: 8px 10px; border-bottom: 1px solid var(--bl-divider); }
.ixc-card-ic { display: inline-flex; flex-shrink: 0; }
.ixc-title { flex: 1; min-width: 0; border: 0; outline: none; background: transparent; font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.ixc-title:focus { background: var(--bl-bg-2); border-radius: 4px; padding: 2px 4px; }
.ixc-card-btn { width: 26px; height: 26px; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 5px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ixc-card-btn:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.ixc-card-btn.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }

.ixc-config { padding: 10px; background: var(--bl-bg-2); border-bottom: 1px solid var(--bl-divider); display: flex; flex-direction: column; gap: 6px; }
.ixc-cfg-row { display: flex; align-items: center; gap: 8px; }
.ixc-cfg-row label { width: 64px; flex-shrink: 0; font-size: 12px; color: var(--bl-text-3); }
.ixc-cfg-row select { flex: 1; }

.ixc-card-body { padding: 12px; min-height: 60px; }
.ixc-loading, .ixc-nodata { color: var(--bl-text-3); font-size: 12px; text-align: center; padding: 24px; }

/* 列表直方图 */
.ixc-bar-row { display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.ixc-bar-label { width: 110px; flex-shrink: 0; font-size: 12px; color: var(--bl-text-2); }
.ixc-bar-track { flex: 1; height: 14px; background: var(--bl-bg-2); border-radius: 3px; overflow: hidden; }
.ixc-bar-fill { height: 100%; background: linear-gradient(90deg, #165DFF, #4080FF); border-radius: 3px; transition: width .3s; }
.ixc-bar-val { width: 64px; text-align: right; font-size: 12px; color: var(--bl-text-1); font-variant-numeric: tabular-nums; flex-shrink: 0; }

/* 统计表 */
.ixc-stable { font-size: 12px; }
.ixc-stable th { cursor: pointer; white-space: nowrap; }
.ixc-num { text-align: right; font-variant-numeric: tabular-nums; }

/* 单一统计 */
.ixc-single { text-align: center; padding: 24px 12px; }
.ixc-single-num { font-size: 38px; font-weight: 700; color: var(--bl-text-1); font-variant-numeric: tabular-nums; }
.ixc-single-label { font-size: 12px; color: var(--bl-text-3); margin-top: 6px; }

/* 网格图 */
.ixc-grid-wrap { overflow: auto; }
.ixc-grid { border-collapse: collapse; font-size: 11px; width: 100%; }
.ixc-grid th { padding: 4px 6px; color: var(--bl-text-3); font-weight: 500; max-width: 70px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ixc-grid-rowh { text-align: right; }
.ixc-grid-cell { padding: 6px 8px; text-align: center; font-variant-numeric: tabular-nums; border-radius: 3px; min-width: 48px; }
</style>
