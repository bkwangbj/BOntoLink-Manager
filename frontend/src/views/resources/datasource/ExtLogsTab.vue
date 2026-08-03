<template>
  <div class="elg">
    <!-- 筛选栏: 变更不自动查, 手动点查询 -->
    <div class="elg-filter">
      <span class="elg-lbl">时间范围</span>
      <input class="bl-input bl-input-sm" type="date" v-model="q.from" />
      <span class="elg-to">至</span>
      <input class="bl-input bl-input-sm" type="date" v-model="q.to" />
      <BlSelect v-model="q.interfaceId" :options="apiOptions" size="sm" clearable placeholder="全部接口" style="width:170px" />
      <BlSelect v-model="q.callStatus" :options="STATUS_OPTS" size="sm" clearable placeholder="全部状态" style="width:130px" />
      <input class="bl-input bl-input-sm elg-kw" v-model="q.kw" placeholder="搜索调用方 / 链路ID / URL" @keyup.enter="search" />
      <button class="bl-btn bl-btn-sm bl-btn-primary" @click="search"><span v-html="BL.icon('search', 12, '#fff')"></span><span style="margin-left:4px">查询</span></button>
      <button class="bl-btn bl-btn-sm" @click="reset"><span v-html="BL.icon('refresh', 12)"></span><span style="margin-left:4px">重置</span></button>
    </div>

    <div class="elg-table-wrap">
      <table class="bl-table elg-table">
        <thead><tr>
          <th style="width:150px">调用时间</th><th style="width:150px">接口名称</th><th style="width:64px">请求方法</th>
          <th style="width:90px">调用状态</th><th style="width:130px">HTTP 状态码</th><th style="width:90px">响应耗时</th>
          <th style="width:80px">响应大小</th><th>调用方</th><th style="width:52px" class="t-center">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="r in rows" :key="r.id">
            <td class="bl-mono elg-time">{{ r.call_time }}</td>
            <td class="bl-truncate" :title="r.api_name">{{ r.api_name || '—' }}</td>
            <td><span class="elg-method" :style="{ color: METHOD_COLOR[methodOf(r)] }">{{ methodOf(r) }}</span></td>
            <td><span class="elg-st"><i :style="{ background: STATUS_META[r.call_status]?.color }"></i>{{ STATUS_META[r.call_status]?.label || '—' }}</span></td>
            <td>{{ r.http_status ? `${r.http_status} ${httpText(r.http_status)}` : '—' }}</td>
            <td :class="costCls(r.cost_time)">{{ r.cost_time != null ? r.cost_time + ' ms' : '—' }}</td>
            <td>{{ r.response_size != null && r.call_status === 1 ? formatSize(r.response_size) : '—' }}</td>
            <td class="bl-truncate" :title="r.caller">{{ r.caller || '—' }}</td>
            <td class="t-center"><button class="elg-eye" title="查看详情" @click="openDetail(r)" v-html="BL.icon('eye', 14)"></button></td>
          </tr>
          <tr v-if="!rows.length && !loading"><td colspan="9" class="elg-empty">没有匹配的调用日志，试试调整筛选条件</td></tr>
          <tr v-if="loading"><td colspan="9" class="elg-empty">加载中…</td></tr>
        </tbody>
      </table>
    </div>

    <div class="elg-pager">
      <span class="bl-muted">共 {{ total }} 条记录</span>
      <button class="bl-btn bl-btn-sm" :disabled="exporting || !total" @click="exportCsv">
        <span v-html="BL.icon('download', 12)"></span><span style="margin-left:4px">{{ exporting ? '导出中…' : '导出' }}</span>
      </button>
      <span style="flex:1"></span>
      <BlSelect v-model="q.size" :options="SIZE_OPTS" size="sm" style="width:104px" @change="search" />
      <button class="bl-btn bl-btn-sm bl-btn-icon" :disabled="q.page <= 1" @click="go(q.page - 1)" v-html="BL.icon('chevronLeft', 12)"></button>
      <span class="elg-page">{{ q.page }} / {{ maxPage }}</span>
      <button class="bl-btn bl-btn-sm bl-btn-icon" :disabled="q.page >= maxPage" @click="go(q.page + 1)" v-html="BL.icon('chevronRight', 12)"></button>
    </div>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <div v-if="detail" class="elg-mask" @click.self="detail = null">
        <div class="elg-modal">
          <div class="elg-modal-hd"><span>调用日志详情</span><span style="flex:1"></span>
            <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="detail = null" v-html="BL.icon('x', 14)"></button></div>
          <div class="elg-modal-meta">
            <div><span>调用时间</span>{{ detail.call_time }}</div>
            <div><span>接口名称</span>{{ detail.api_name || '—' }}</div>
            <div><span>请求方法</span>{{ methodOf(detail) }}</div>
            <div><span>调用状态</span>{{ STATUS_META[detail.call_status]?.label || '—' }}</div>
            <div><span>HTTP 状态码</span>{{ detail.http_status || '—' }}</div>
            <div><span>响应耗时</span>{{ detail.cost_time }} ms</div>
            <div><span>响应大小</span>{{ formatSize(detail.response_size) }}</div>
            <div><span>调用方</span>{{ detail.caller || '—' }}</div>
            <div class="elg-meta-full"><span>链路 ID</span><b class="bl-mono">{{ detail.trace_id || '—' }}</b></div>
            <div class="elg-meta-full"><span>完整地址</span><b class="bl-mono elg-url">{{ detail.full_url || '—' }}</b></div>
          </div>
          <div class="elg-modal-body">
            <div class="elg-pane">
              <div class="elg-pane-tabs">
                <button :class="['elg-pt', reqTab === 'h' && 'is-on']" @click="reqTab = 'h'">请求头</button>
                <button :class="['elg-pt', reqTab === 'b' && 'is-on']" @click="reqTab = 'b'">请求体</button>
              </div>
              <pre class="elg-code">{{ prettyJson(reqTab === 'h' ? detail.request_header : detail.request_body) || '(空)' }}</pre>
            </div>
            <div class="elg-pane">
              <div class="elg-pane-tabs">
                <button :class="['elg-pt', respTab === 'h' && 'is-on']" @click="respTab = 'h'">响应头</button>
                <button :class="['elg-pt', respTab === 'b' && 'is-on']" @click="respTab = 'b'">响应体</button>
              </div>
              <pre class="elg-code" :class="detail.call_status === 1 ? 'is-ok' : 'is-err'">{{ respTab === 'b' ? (prettyJson(detail.response_body) || detail.error_msg || '(空)') : '(响应头未记录)' }}</pre>
            </div>
          </div>
          <div class="elg-modal-ft"><span style="flex:1"></span><button class="bl-btn bl-btn-sm" @click="detail = null">关闭</button></div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import { extDatasourceApi } from '@/api'
import { METHOD_COLOR, formatSize, prettyJson } from './apiModel.js'

const props = defineProps({
  dsId: { type: String, required: true },
  apis: { type: Array, default: () => [] },
  preset: { type: Object, default: null },   // 从监控页跳转时带入的筛选
})

const STATUS_META = {
  1: { label: '成功', color: '#00B42A' }, 2: { label: '失败', color: '#F53F3F' },
  3: { label: '超时', color: '#FF7D00' }, 4: { label: '鉴权失败', color: '#FADB14' },
}
const STATUS_OPTS = Object.entries(STATUS_META).map(([v, m]) => ({ value: Number(v), label: m.label }))
const SIZE_OPTS = [20, 50, 100].map(v => ({ value: v, label: v + ' 条/页' }))
const HTTP_TEXT = { 200:'OK', 201:'Created', 204:'No Content', 400:'Bad Request', 401:'Unauthorized',
  403:'Forbidden', 404:'Not Found', 500:'Internal Server Error', 502:'Bad Gateway', 503:'Service Unavailable' }

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const detail = ref(null)
const reqTab = ref('h')
const respTab = ref('b')

function defaultFrom() { const d = new Date(); d.setDate(d.getDate() - 7); return d.toISOString().slice(0, 10) }
const q = reactive({ from: defaultFrom(), to: new Date().toISOString().slice(0, 10),
  interfaceId: '', callStatus: '', kw: '', page: 1, size: 20 })

const apiOptions = computed(() => props.apis.map(a => ({ value: a.id, label: a.api_name })))
const maxPage = computed(() => Math.max(1, Math.ceil(total.value / q.size)))

function methodOf(r) { return (props.apis.find(a => a.id === r.interface_id)?.method) || 'GET' }
function httpText(s) { return HTTP_TEXT[s] || '' }
function costCls(c) { return Number(c) > 10000 ? 'elg-danger' : Number(c) > 3000 ? 'elg-warn' : '' }

async function load() {
  loading.value = true
  try {
    const r = await extDatasourceApi.logs(props.dsId, {
      from: q.from ? q.from + ' 00:00:00' : '', to: q.to ? q.to + ' 23:59:59' : '',
      interfaceId: q.interfaceId || '', callStatus: q.callStatus === '' ? undefined : q.callStatus,
      kw: q.kw || '', page: q.page, size: q.size,
    })
    rows.value = r?.rows || []
    total.value = r?.total || 0
  } catch { rows.value = []; total.value = 0 } finally { loading.value = false }
}
function search() { q.page = 1; load() }
function go(p) { q.page = p; load() }
function reset() {
  Object.assign(q, { from: defaultFrom(), to: new Date().toISOString().slice(0, 10),
    interfaceId: '', callStatus: '', kw: '', page: 1, size: 20 })
  load()
}
/* 导出当前筛选条件下的全部记录(不是当前页), 上限 5000 条 */
const EXPORT_MAX = 5000
const exporting = ref(false)
const EXPORT_COLS = [
  ['调用时间', r => r.call_time],
  ['接口名称', r => r.api_name || ''],
  ['请求方法', r => methodOf(r)],
  ['调用状态', r => STATUS_META[r.call_status]?.label || ''],
  ['HTTP 状态码', r => r.http_status ?? ''],
  ['响应耗时(ms)', r => r.cost_time ?? ''],
  ['响应大小(B)', r => r.response_size ?? ''],
  ['调用方', r => r.caller || ''],
  ['链路 ID', r => r.trace_id || ''],
  ['完整地址', r => r.full_url || ''],
  ['错误信息', r => r.error_msg || ''],
]
async function exportCsv() {
  exporting.value = true
  try {
    const r = await extDatasourceApi.logs(props.dsId, {
      from: q.from ? q.from + ' 00:00:00' : '', to: q.to ? q.to + ' 23:59:59' : '',
      interfaceId: q.interfaceId || '', callStatus: q.callStatus === '' ? undefined : q.callStatus,
      kw: q.kw || '', page: 1, size: EXPORT_MAX,
    })
    const list = r?.rows || []
    if (!list.length) { BL.warning('当前筛选条件下没有可导出的记录'); return }
    const csv = [EXPORT_COLS.map(c => c[0]).join(',')]
      .concat(list.map(row => EXPORT_COLS.map(c => cell(c[1](row))).join(',')))
      .join('\r\n')
    /* BOM 让 Excel 正确识别 UTF-8 中文 */
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `调用日志_${q.from}_${q.to}.csv`
    a.click()
    URL.revokeObjectURL(a.href)
    BL.success(list.length >= EXPORT_MAX ? `已导出前 ${EXPORT_MAX} 条,如需更多请缩小时间范围` : `已导出 ${list.length} 条`)
  } catch (e) { BL.error(e?.msg || '导出失败') } finally { exporting.value = false }
}
/* 逗号/引号/换行都要包引号并转义, 否则 Excel 会串列 */
function cell(v) {
  const s = String(v ?? '')
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

async function openDetail(r) {
  detail.value = await extDatasourceApi.logDetail(r.id).catch(() => r)
  reqTab.value = 'h'; respTab.value = 'b'
}

/* 从监控页点错误类型/接口跳过来时带入筛选 */
watch(() => props.preset, p => {
  if (!p) return
  if (p.callStatus !== undefined) q.callStatus = p.callStatus
  if (p.interfaceId !== undefined) q.interfaceId = p.interfaceId
  search()
}, { immediate: true, deep: true })

onMounted(load)
defineExpose({ reload: load })
</script>

<style scoped>
.elg { display: flex; flex-direction: column; height: 100%; min-height: 0; }
.elg-filter { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 12px 0; }
.elg-lbl { font-size: 12.5px; color: var(--bl-text-2); }
.elg-to { font-size: 12px; color: var(--bl-text-3); }
.elg-filter .bl-input { width: 138px; }
.elg-kw { width: 200px !important; }
.elg-table-wrap { flex: 1; min-height: 0; overflow: auto; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: 8px; }
.elg-table { width: 100%; font-size: 12px; }
.elg-table thead th { position: sticky; top: 0; z-index: 2; background: var(--bl-bg-2); text-align: left;
  font-weight: 600; color: var(--bl-text-2); padding: 8px 10px; white-space: nowrap; }
.elg-table td { padding: 7px 10px; border-top: 1px solid var(--bl-divider); }
.elg-table tbody tr:hover { background: var(--bl-bg-hover); }
.elg-time { font-size: 11.5px; white-space: nowrap; }
.elg-method { font-weight: 700; font-size: 11px; }
.elg-st { display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; }
.elg-st i { width: 7px; height: 7px; border-radius: 50%; display: inline-block; }
.elg-warn { color: #FF7D00; }
.elg-danger { color: #F53F3F; font-weight: 600; }
.elg-table .t-center { text-align: center; }
.elg-eye { border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer;
  display: inline-flex; padding: 4px; border-radius: 4px; }
.elg-eye:hover { color: var(--bl-primary); background: var(--bl-primary-soft); }
.elg-empty { text-align: center; color: var(--bl-text-3); padding: 28px; font-size: 12px; }
.elg-pager { display: flex; align-items: center; gap: 8px; padding: 10px 0 2px; font-size: 12px; }
.elg-page { font-size: 12px; color: var(--bl-text-2); min-width: 48px; text-align: center; }

.elg-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); backdrop-filter: blur(3px); z-index: 1350;
  display: flex; align-items: center; justify-content: center; }
.elg-modal { width: 940px; max-width: 96vw; height: 640px; max-height: 90vh; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.elg-modal-hd { display: flex; align-items: center; padding: 13px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.elg-modal-meta { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px 16px; padding: 12px 16px;
  border-bottom: 1px solid var(--bl-divider); font-size: 12px; }
.elg-modal-meta > div { display: flex; gap: 6px; min-width: 0; }
.elg-modal-meta span { color: var(--bl-text-3); flex-shrink: 0; }
.elg-meta-full { grid-column: span 2; }
.elg-url { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.elg-modal-body { flex: 1; min-height: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--bl-divider); }
.elg-pane { display: flex; flex-direction: column; min-height: 0; background: var(--bl-bg-1); }
.elg-pane-tabs { display: flex; gap: 2px; padding: 6px 12px 0; }
.elg-pt { padding: 5px 12px; border: 0; background: transparent; color: var(--bl-text-2); font-size: 12px; cursor: pointer; border-radius: 5px 5px 0 0; }
.elg-pt.is-on { color: var(--bl-primary); box-shadow: inset 0 -2px 0 var(--bl-primary); }
.elg-code { flex: 1; min-height: 0; overflow: auto; margin: 0; padding: 10px 14px; font-family: var(--bl-mono, monospace);
  font-size: 11.5px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; color: var(--bl-text-2); }
.elg-code.is-ok { color: #00b42a; }
.elg-code.is-err { color: #f53f3f; }
.elg-modal-ft { display: flex; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }
</style>
