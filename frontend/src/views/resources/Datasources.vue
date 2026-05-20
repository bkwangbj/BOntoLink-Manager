<template>
  <div class="page">
    <PageHeader title="数据源管理" subtitle="配置 | 测试 | 监控 数据源">
      <template #actions>
        <div class="ov">
          <span class="ov-item"><span class="ov-lbl">总数</span><b>{{ overview.total ?? 0 }}</b></span>
          <span class="ov-item ov-ok"><span class="ov-lbl">正常</span><b>{{ overview.normal ?? 0 }}</b></span>
          <span class="ov-item ov-risk"><span class="ov-lbl">风险</span><b>{{ overview.risk ?? 0 }}</b></span>
        </div>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索数据源（名称 / 编码 / 类型）" v-model="q" />
        </div>
        <button class="bl-btn bl-btn-primary" @click="openCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建数据源</span>
        </button>
      </template>
    </PageHeader>

    <div :class="['three-pane', !cfgOpen && 'no-cfg']">
      <!-- 左栏：数据源列表 -->
      <section class="pane pane-list">
        <table class="bl-table ds-table">
          <thead>
            <tr>
              <th></th>
              <th>数据库名</th>
              <th>引用数</th>
              <th>状态</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in filtered" :key="d.id" :class="['ds-row', selected?.id===d.id && 'is-active']" @click="select(d)" @dblclick="openEdit(d)">
              <td style="width:36px">
                <span class="ds-ic" :style="{ background: typeColor(d.dsType) }" v-html="BL.icon(typeIcon(d.dsType), 12, '#fff')"></span>
              </td>
              <td>
                <div class="ds-name bl-truncate" :title="d.dsName">{{ d.dsName }}</div>
                <div class="ds-code bl-mono bl-muted">{{ d.dsCode }} · {{ d.dsType.toUpperCase() }}</div>
              </td>
              <td><span class="bl-tag">{{ d.refCount || 0 }}</span></td>
              <td>
                <span :class="['bl-tag', statusTag(d).cls]">{{ statusTag(d).text }}</span>
              </td>
              <td style="width:80px">
                <div class="bl-row" style="gap:0">
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="测试" @click.stop="doTest(d)" v-html="BL.icon('zap', 12)"></button>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click.stop="openEdit(d)" v-html="BL.icon('edit', 12)"></button>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click.stop="doDelete(d)" v-html="BL.icon('trash', 12)"></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filtered.length" class="bl-empty" style="padding:48px">暂无匹配数据源，请调整检索条件</div>
      </section>

      <!-- 中栏：配置 -->
      <section class="pane pane-cfg" v-if="cfgOpen">
        <div class="cfg-hd">
          <span>{{ form.id ? '编辑数据源' : '新建数据源' }}</span>
          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="cfgOpen=false" v-html="BL.icon('x', 14)"></button>
        </div>
        <div class="cfg-body">
          <div class="cfg-section">基础信息</div>
          <FieldRow label="名称 *"><input class="bl-input" v-model="form.dsName" placeholder="例：水利主业务库" /></FieldRow>
          <FieldRow label="编码 *" hint="同领域内唯一，关联 ont_class 引用"><input class="bl-input bl-mono" v-model="form.dsCode" /></FieldRow>
          <FieldRow label="所属领域">
            <select class="bl-input" v-model="form.categoryCode">
              <option value="">— 无 —</option>
              <option v-for="c in domainOptions" :key="c.code" :value="c.code">{{ c.name }}</option>
            </select>
          </FieldRow>
          <FieldRow label="类型 *">
            <select class="bl-input" v-model="form.dsType" @change="onTypeChange">
              <option v-for="t in dsTypes" :key="t" :value="t">{{ t.toUpperCase() }}</option>
            </select>
          </FieldRow>

          <div class="cfg-section">连接配置</div>
          <template v-if="form.dsType !== 'mongodb'">
            <FieldRow label="JDBC 驱动">
              <select class="bl-input" v-model="form.jdbcDriver">
                <option v-for="d in driversForType" :key="d" :value="d">{{ d }}</option>
              </select>
            </FieldRow>
            <FieldRow label="连接地址"><input class="bl-input bl-mono" v-model="form.jdbcUrl" placeholder="jdbc:mysql://127.0.0.1:3306/xxx" /></FieldRow>
            <FieldRow label="账号"><input class="bl-input" v-model="form.username" /></FieldRow>
            <FieldRow label="密码"><input class="bl-input" type="password" v-model="form.password" /></FieldRow>
          </template>
          <template v-else>
            <FieldRow label="Mongo URL"><input class="bl-input bl-mono" v-model="form.mongoUrl" placeholder="mongodb://127.0.0.1:27017/xxx" /></FieldRow>
            <FieldRow label="账号"><input class="bl-input" v-model="form.username" /></FieldRow>
            <FieldRow label="密码"><input class="bl-input" type="password" v-model="form.password" /></FieldRow>
          </template>

          <div class="cfg-section">扩展信息</div>
          <FieldRow label="备注"><textarea class="bl-textarea" rows="3" v-model="form.remark"></textarea></FieldRow>
          <FieldRow label="状态" inline>
            <div class="radio-group">
              <label class="radio-item"><input type="radio" :value="1" v-model="form.status" /> 启用</label>
              <label class="radio-item"><input type="radio" :value="0" v-model="form.status" /> 禁用</label>
            </div>
          </FieldRow>
        </div>
        <div class="cfg-ft">
          <button class="bl-btn" @click="cfgOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="submitForm">保存</button>
        </div>
      </section>

      <!-- 右栏：监控 -->
      <section class="pane pane-mon">
        <div v-if="!selected" class="bl-empty" style="padding:60px">
          <div class="bl-empty-icon" v-html="BL.icon('station', 36)"></div>
          请选择左侧数据源查看监控
        </div>
        <template v-else>
          <!-- 顶部信息条（撑满、只有底部线） -->
          <div class="mon-info bare">
            <div class="bl-row" style="gap:10px;flex:1;min-width:0">
              <span class="mon-name">{{ selected.dsName }} | {{ selected.dsCode }}</span>
              <span class="bl-tag mon-tag" :style="{ color: typeColor(selected.dsType), background: typeColor(selected.dsType) + '14' }">{{ selected.dsType.toUpperCase() }}</span>
              <span class="bl-mono bl-muted">{{ mon.basic?.host || '—' }}</span>
              <span class="bl-muted">版本: <span class="bl-mono">{{ mon.basic?.version || '—' }}</span></span>
            </div>
            <button class="bl-btn bl-btn-sm" @click="doTest(selected)" v-html="iconText('zap','测试')"></button>
          </div>

          <div class="mon-content">
          <!-- 状态图例 -->
          <div class="mon-legend">
            <span class="legend-item"><span class="ld" style="background:#00B42A"></span>正常</span>
            <span class="legend-item"><span class="ld" style="background:#FF7D00"></span>预警</span>
            <span class="legend-item"><span class="ld" style="background:#F53F3F"></span>异常</span>
            <span class="legend-item"><span class="ld" style="background:#C9CDD4"></span>闲置</span>
          </div>

          <!-- 2x2 监控面板 -->
          <div class="mon-grid2">
            <!-- 全局状态 -->
            <div class="mon-panel">
              <div class="mp-hd"><span class="mp-ic" style="color:#F53F3F" v-html="BL.icon('bell', 14)"></span>全局状态</div>
              <div class="seg-bar">
                <div class="seg-fill" :style="{ width: (mon.basic?.loadPct || 0) + '%', background: loadColor() }"></div>
                <div class="seg-text">负载占比 {{ mon.basic?.loadPct ?? 0 }}%</div>
              </div>
              <table class="kv-table">
                <tr><td>数据源连通状态</td><td><span :class="['bl-tag', statusTag(selected).cls]">{{ statusTag(selected).text }}</span></td></tr>
                <tr><td>连接池负载占比</td><td><b>{{ mon.basic?.loadPct ?? 0 }}</b> <span class="bl-muted">%</span></td></tr>
              </table>
            </div>

            <!-- 基础配置 -->
            <div class="mon-panel">
              <div class="mp-hd"><span class="mp-ic" style="color:#165DFF" v-html="BL.icon('sliders', 14)"></span>基础配置</div>
              <div class="bar-row">
                <div class="bar-track"><div class="bar-fill" :style="{ width: pctMinIdle + '%', background: '#C9CDD4' }"></div><span class="bar-label">最小空闲</span></div>
              </div>
              <div class="bar-row">
                <div class="bar-track"><div class="bar-fill" :style="{ width: '100%', background: statusColor }"></div><span class="bar-label">最大连接</span></div>
              </div>
              <table class="kv-table">
                <tr><td>最小空闲连接</td><td><b>{{ mon.basic?.minIdle ?? 0 }}</b> <span class="bl-muted">个</span></td></tr>
                <tr><td>最大连接数</td><td><b>{{ mon.basic?.maxConn ?? 0 }}</b> <span class="bl-muted">个</span></td></tr>
                <tr><td>空闲连接超时时间</td><td><b>{{ mon.basic?.idleTimeoutMs ?? 0 }}</b> <span class="bl-muted">ms</span></td></tr>
                <tr><td>获取连接超时时间</td><td><b>{{ mon.basic?.connectTimeoutMs ?? 0 }}</b> <span class="bl-muted">ms</span></td></tr>
              </table>
            </div>

            <!-- 实时状态 -->
            <div class="mon-panel">
              <div class="mp-hd"><span class="mp-ic" style="color:#722ED1" v-html="BL.icon('zap', 14)"></span>实时状态</div>
              <div class="bar-row">
                <div class="bar-track"><div class="bar-fill" :style="{ width: pctActive + '%', background: statusColor }"></div><span class="bar-label">活跃连接</span></div>
              </div>
              <div class="bar-row">
                <div class="bar-track"><div class="bar-fill" :style="{ width: pctWaiting + '%', background: waitingColor }"></div><span class="bar-label">排队等待</span></div>
              </div>
              <table class="kv-table">
                <tr><td>当前活跃连接</td><td><b>{{ mon.detail?.logicConnect ?? 0 }}</b> <span class="bl-muted">个</span></td></tr>
                <tr><td>当前空闲连接</td><td><b>{{ mon.detail?.idleConn ?? 0 }}</b> <span class="bl-muted">个</span></td></tr>
                <tr><td>排队等待连接</td><td><b>{{ mon.detail?.waitingConn ?? 0 }}</b> <span class="bl-muted">个</span></td></tr>
                <tr><td>已创建总连接</td><td><b>{{ mon.detail?.totalCreated ?? 0 }}</b> <span class="bl-muted">个</span></td></tr>
              </table>
            </div>

            <!-- 响应耗时 -->
            <div class="mon-panel">
              <div class="mp-hd"><span class="mp-ic" style="color:#FF7D00" v-html="BL.icon('station', 14)"></span>响应耗时</div>
              <div class="bar-row">
                <div class="bar-track"><div class="bar-fill" :style="{ width: pctRtMin + '%', background: '#00B42A' }"></div><span class="bar-label">最小耗时</span></div>
              </div>
              <div class="bar-row">
                <div class="bar-track"><div class="bar-fill" :style="{ width: '100%', background: rtColor }"></div><span class="bar-label">最大耗时</span></div>
              </div>
              <table class="kv-table">
                <tr><td>最小获取耗时</td><td><b>{{ mon.rt?.min ?? 0 }}</b> <span class="bl-muted">ms</span></td></tr>
                <tr><td>平均获取耗时</td><td><b>{{ mon.rt?.avg ?? 0 }}</b> <span class="bl-muted">ms</span></td></tr>
                <tr><td>最大获取耗时</td><td><b>{{ mon.rt?.max ?? 0 }}</b> <span class="bl-muted">ms</span></td></tr>
              </table>
            </div>
          </div>

          <!-- 健康统计 -->
          <div class="mon-health">
            <div class="mp-hd"><span class="mp-ic" style="color:#00B42A" v-html="BL.icon('check', 14)"></span>健康统计</div>
            <div class="health-grid">
              <div class="health-cell"><div class="hc-lbl">获取成功次数</div><div class="hc-val">{{ fmt(mon.health?.acquireSuccess || 0) }}</div></div>
              <div class="health-cell"><div class="hc-lbl">获取失败次数</div><div class="hc-val" :class="(mon.health?.acquireFail||0) > 0 && 'is-warn'">{{ mon.health?.acquireFail ?? 0 }}</div></div>
              <div class="health-cell"><div class="hc-lbl">自动重连次数</div><div class="hc-val">{{ mon.health?.autoReconnect ?? 0 }}</div></div>
              <div class="health-cell"><div class="hc-lbl">无效连接剔除数</div><div class="hc-val">{{ mon.health?.killedInvalid ?? 0 }}</div></div>
              <div class="health-cell"><div class="hc-lbl">连接泄露次数</div><div class="hc-val" :class="(mon.health?.leak||0) > 0 && 'is-warn'">{{ mon.health?.leak ?? 0 }}</div></div>
            </div>
          </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FieldRow from '@/views/config/category/FieldRow.vue'
import { BL } from '@/lib/bl.js'
import { datasourceApi, namespaceApi, categoryApi } from '@/api'

const rows = ref([])
const overview = ref({})
const q = ref('')
const selected = ref(null)
const cfgOpen = ref(false)
const driverMap = ref({})
const monTab = ref('basic')
const mon = ref({})

const dsTypes = ['mysql','postgresql','oracle','mongodb','dm','kingbase','oscar','gbase','polardb','tdsql','gaussdb','oceanbase']

const monTabs = [
  { key:'basic',  label:'基础监控' },
  { key:'detail', label:'详细监控' },
  { key:'trend',  label:'历史趋势' }
]
const detailLabels = {
  logicConnect:'逻辑连接数', physicalConnect:'物理连接数',
  waitThreadCount:'等待线程数', notEmptyWaitMs:'NotEmpty 等待 (ms)',
  executeCount:'执行次数', errorCount:'错误次数',
  commitCount:'提交次数', rollbackCount:'回滚次数',
  recycleErrorCount:'回收异常次数'
}

const form = reactive({})

const domainOptions = ref([])

const driversForType = computed(() => driverMap.value[form.dsType] || [])

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

function typeColor(t) {
  const m = { mysql:'#FF7D00', postgresql:'#165DFF', oracle:'#F53F3F', mongodb:'#00B42A',
    dm:'#722ED1', kingbase:'#13C2C2', oscar:'#EB2F96', gbase:'#FADB14',
    polardb:'#165DFF', tdsql:'#00B42A', gaussdb:'#722ED1', oceanbase:'#FF7D00' }
  return m[t] || '#86909C'
}
function typeIcon(t) {
  if (t === 'mongodb') return 'leaf'
  if (['polardb','tdsql','gaussdb','oceanbase'].includes(t)) return 'network'
  return 'database'
}

function statusTag(d) {
  if (!d) return { cls:'', text:'—' }
  if (d.status === 0) return { cls:'', text:'禁用' }
  if (d.connectStatus === 'offline') return { cls:'bl-tag-danger', text:'离线' }
  if (d.connectStatus === 'risk')    return { cls:'bl-tag-warning', text:'风险' }
  return { cls:'bl-tag-success', text:'在线' }
}

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return rows.value
  return rows.value.filter(r =>
    [r.dsName, r.dsCode, r.dsType, r.remark].filter(Boolean)
      .some(s => String(s).toLowerCase().includes(k)))
})

async function loadAll() {
  rows.value    = await datasourceApi.list().catch(() => [])
  overview.value= await datasourceApi.overview().catch(() => ({}))
  driverMap.value = await datasourceApi.drivers().catch(() => ({}))
  if (!domainOptions.value.length) {
    const ns = await namespaceApi.list().catch(() => [])
    const tree = await categoryApi.tree().catch(() => [])
    const list = []
    const walk = (nodes) => nodes.forEach(n => { if (n.categoryCode) list.push({ code: n.categoryCode, name: n.label }); if (n.children) walk(n.children) })
    walk(tree)
    domainOptions.value = list
  }
}

async function select(d) {
  selected.value = d
  await loadMonitor()
  // 中间编辑面板若已打开，同步切换为当前选中的数据源
  if (cfgOpen.value) openEdit(d)
}

async function loadMonitor() {
  if (!selected.value) return
  mon.value = await datasourceApi.monitor(selected.value.id).catch(() => ({}))
}

/* 状态色:正常绿 / 预警橙 / 异常红 / 闲置灰 */
const STATUS_GREEN  = '#00B42A'
const STATUS_ORANGE = '#FF7D00'
const STATUS_RED    = '#F53F3F'
const STATUS_GREY   = '#C9CDD4'

function loadColor() {
  const p = mon.value?.basic?.loadPct ?? 0
  if (p >= 80) return STATUS_RED
  if (p >= 60) return STATUS_ORANGE
  return STATUS_GREEN
}

/* 当前数据源整体健康色 */
const statusColor = computed(() => {
  if (!selected.value || selected.value.status === 0) return STATUS_GREY
  switch (selected.value.connectStatus) {
    case 'online':  return STATUS_GREEN
    case 'risk':    return STATUS_ORANGE
    case 'offline': return STATUS_RED
    default:        return STATUS_GREY
  }
})
/* 排队等待:0 = 闲置灰,>0 = 预警橙,过多 = 异常红 */
const waitingColor = computed(() => {
  const w = mon.value?.detail?.waitingConn ?? 0
  if (w === 0) return STATUS_GREY
  if (w >= 5)  return STATUS_RED
  return STATUS_ORANGE
})
/* 响应耗时统一橙色基调（性能警示色）；超阈值才变红 */
const rtColor = computed(() => {
  const max = mon.value?.rt?.max ?? 0
  if (max >= 200) return STATUS_RED
  return STATUS_ORANGE
})

/* 进度条占比计算 */
const pctMinIdle = computed(() => {
  const min = mon.value?.basic?.minIdle ?? 0
  const max = mon.value?.basic?.maxConn ?? 0
  return max > 0 ? Math.min(100, Math.round(min * 100 / max)) : 0
})
/* 活跃连接占已创建总连接（用户口径：8/61 ≈ 13%） */
const pctActive = computed(() => {
  const a = mon.value?.detail?.logicConnect ?? 0
  const t = mon.value?.detail?.totalCreated ?? 0
  return t > 0 ? Math.min(100, Math.round(a * 100 / t)) : 0
})
const pctWaiting = computed(() => {
  const w = mon.value?.detail?.waitingConn ?? 0
  const t = mon.value?.detail?.totalCreated ?? 0
  return t > 0 ? Math.min(100, Math.round(w * 100 / t)) : 0
})
const pctRtMin = computed(() => {
  const min = mon.value?.rt?.min ?? 0
  const max = mon.value?.rt?.max ?? 0
  return max > 0 ? Math.min(100, Math.round(min * 100 / max)) : 0
})
function fmt(n) { return Number(n || 0).toLocaleString('en-US') }

function trendPoints(key) {
  const arr = mon.value?.trend || []
  if (!arr.length) return ''
  const ws = 480, hs = 120
  const xs = arr.map((_, i) => (i / (arr.length - 1)) * ws)
  const vs = arr.map(p => Number(p[key]) || 0)
  const max = Math.max(...vs, 1), min = Math.min(...vs, 0)
  const norm = vs.map(v => hs - ((v - min) / Math.max(max - min, 1)) * (hs - 10) - 5)
  return arr.map((_, i) => `${xs[i].toFixed(1)},${norm[i].toFixed(1)}`).join(' ')
}

function openCreate() {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { dsType:'mysql', status:1, refCount:0 })
  cfgOpen.value = true
}
function openEdit(d) {
  // 先清掉旧字段，再覆盖；避免新数据缺字段时残留前一条的值
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, d)
  cfgOpen.value = true
}
function onTypeChange() {
  const list = driverMap.value[form.dsType] || []
  if (list.length) form.jdbcDriver = list[0]
}

async function submitForm() {
  if (!form.dsName || !form.dsCode || !form.dsType) { BL.warning('名称、编码、类型为必填'); return }
  if (form.id) await datasourceApi.update(form.id, form)
  else         await datasourceApi.create(form)
  BL.success('已保存')
  cfgOpen.value = false
  await loadAll()
}

async function doTest(d) {
  const r = await datasourceApi.test(d.id)
  if (r.ok) BL.success(`${r.msg} · ${r.responseMs}ms`)
  else      BL.error(r.msg)
  await loadAll()
  if (selected.value?.id === d.id) await loadMonitor()
}

async function doDelete(d) {
  const ok = await BL.confirm({ title:'删除数据源', content:`确定删除「${d.dsName}」？删除后关联引用会失效。`, danger:true, okText:'删除' })
  if (!ok) return
  await datasourceApi.remove(d.id)
  BL.success('已删除')
  if (selected.value?.id === d.id) selected.value = null
  await loadAll()
}

onMounted(async () => {
  await loadAll()
  if (rows.value.length && !selected.value) {
    await select(rows.value[0])
    openEdit(rows.value[0])
  }
})
watch(() => selected.value?.id, () => { monTab.value = 'basic' })
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }

.ov { display: inline-flex; gap: 14px; padding: 4px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); }
.ov-item { font-size: var(--bl-fs-13); color: var(--bl-text-2); }
.ov-item .ov-lbl { color: var(--bl-text-3); margin-right: 6px; }
.ov-item b { font-weight: 600; color: var(--bl-text-1); }
.ov-ok b { color: var(--bl-success); }
.ov-risk b { color: var(--bl-warning); }

.search-wrap { position: relative; width: 260px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

.three-pane {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(420px, 1.2fr) 380px 1.4fr;
  gap: 12px; padding: 12px; overflow: hidden;
}
/* 中间编辑面板关闭时，右侧监控扩展占满 */
.three-pane.no-cfg { grid-template-columns: minmax(420px, 1.2fr) 1fr; }
.three-pane.no-cfg > .pane-cfg { display: none; }
.pane {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  overflow: hidden;
  display: flex; flex-direction: column;
}
.pane-list { overflow: auto; }
.pane-cfg { transition: width .2s; }

.ds-table { width: 100%; }
.ds-row { cursor: pointer; }
.ds-row.is-active { background: var(--bl-primary-soft); }
.ds-row.is-active td { color: var(--bl-primary); }
.ds-ic {
  width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
}
.ds-name { font-weight: 500; color: var(--bl-text-1); }
.ds-code { font-size: var(--bl-fs-11); }

.cfg-hd {
  height: 48px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  font-weight: 600;
  border-bottom: 1px solid var(--bl-divider);
}
.cfg-body { flex: 1; overflow: auto; padding: 8px 14px; }
.cfg-section {
  font-size: var(--bl-fs-12); color: var(--bl-text-3);
  margin: 12px 0 6px; padding-left: 8px;
  border-left: 3px solid var(--bl-primary);
}
.cfg-ft {
  padding: 10px 14px; display: flex; justify-content: flex-end; gap: 8px;
  border-top: 1px solid var(--bl-divider);
}

.pane-mon { display: flex; flex-direction: column; overflow: hidden; padding: 0; }
.mon-content { flex: 1; overflow: auto; padding: 12px 14px; display: flex; flex-direction: column; gap: 10px; }

/* 顶部信息条：撑满、只有底部一条分隔线，与系统其他面板头部一致 */
.mon-info {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
.mon-name { font-size: var(--bl-fs-14); font-weight: 600; }
.mon-tag { font-weight: 600; }

/* 状态图例 */
.mon-legend {
  display: flex; gap: 16px;
  padding: 0 4px;
  font-size: var(--bl-fs-12); color: var(--bl-text-2);
}
.legend-item { display: inline-flex; align-items: center; gap: 6px; }
.legend-item .ld { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }

/* 2x2 监控面板网格 */
.mon-grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.mon-panel {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  padding: 12px;
}
.mp-hd {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--bl-fs-13); font-weight: 600;
  margin-bottom: 10px;
}
.mp-ic { display: inline-flex; }

/* 负载占比 bar */
.seg-bar {
  position: relative;
  height: 20px;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  overflow: hidden;
  margin-bottom: 10px;
}
.seg-fill { position: absolute; left: 0; top: 0; bottom: 0; transition: width .3s ease; }
.seg-text {
  position: relative; z-index: 1;
  height: 100%; display: flex; align-items: center; justify-content: center;
  font-size: var(--bl-fs-12); color: var(--bl-text-1); font-weight: 600;
  text-shadow:
    0 0 2px rgba(255,255,255,0.9),
    0 0 4px rgba(255,255,255,0.7);
}

/* 进度条（带居中标签） */
.bar-row { margin-bottom: 6px; }
.bar-row:last-child { margin-bottom: 10px; }
.bar-track {
  position: relative;
  height: 20px;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  overflow: hidden;
}
.bar-fill {
  position: absolute; left: 0; top: 0; bottom: 0;
  transition: width .3s ease;
}
.bar-label {
  position: relative; z-index: 1;
  display: flex; align-items: center; justify-content: center;
  height: 100%;
  font-size: var(--bl-fs-12); font-weight: 500;
  color: var(--bl-text-1);
  text-shadow:
    0 0 2px rgba(255,255,255,0.9),
    0 0 4px rgba(255,255,255,0.7);
}

/* KV 表 */
.kv-table { width: 100%; border-collapse: collapse; }
.kv-table td {
  padding: 7px 0;
  font-size: var(--bl-fs-12);
  border-bottom: 1px dashed var(--bl-divider);
}
.kv-table tr:last-child td { border-bottom: 0; }
.kv-table td:first-child { color: var(--bl-text-3); }
.kv-table td:last-child  { text-align: right; }
.kv-table td b { font-weight: 600; color: var(--bl-text-1); font-feature-settings: "tnum"; }

/* 健康统计 */
.mon-health {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  padding: 12px;
}
.health-grid {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px;
}
.health-cell {
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  padding: 12px 10px;
  text-align: center;
}
.hc-lbl { font-size: var(--bl-fs-12); color: var(--bl-text-3); margin-bottom: 6px; }
.hc-val { font-size: var(--bl-fs-20); font-weight: 700; color: var(--bl-text-1); font-feature-settings: "tnum"; }
.hc-val.is-warn { color: var(--bl-danger); }

.radio-group { display: inline-flex; align-items: center; gap: 20px; }
.radio-item {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: var(--bl-fs-13);
  cursor: pointer;
  white-space: nowrap;
}
.radio-item input { accent-color: var(--bl-primary); }
</style>
