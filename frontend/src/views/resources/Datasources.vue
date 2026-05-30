<template>
  <div class="page">
    <PageHeader title="数据源管理" subtitle="配置 | 测试 | 监控 数据源">
      <template #actions>
        <div class="ov">
          <span class="ov-item"><span class="ov-lbl">总数</span><b>{{ overview.total ?? 0 }}</b></span>
          <span class="ov-item ov-ok"><span class="ov-lbl">正常</span><b>{{ overview.normal ?? 0 }}</b></span>
          <span class="ov-item ov-risk"><span class="ov-lbl">风险</span><b>{{ overview.risk ?? 0 }}</b></span>
        </div>
        <div class="ds-view-toggle">
          <button :class="['vt-btn', !groupMode && 'is-on']" @click="groupMode=false" title="列表视图">
            <span v-html="BL.icon('list', 13)"></span><span>列表</span>
          </button>
          <button :class="['vt-btn', groupMode && 'is-on']" @click="groupMode=true" title="按领域分组">
            <span v-html="BL.icon('layers', 13)"></span><span>分组</span>
          </button>
        </div>
        <select class="bl-input hd-filter" v-model="filterIndustry" title="所属领域">
          <option value="">全部领域</option>
          <option v-for="i in industryFilterOptions" :key="i" :value="i">{{ i }}</option>
        </select>
        <select class="bl-input hd-filter" v-model="filterStatus" title="状态">
          <option value="">全部状态</option>
          <option value="online">在线</option>
          <option value="risk">风险</option>
          <option value="offline">离线</option>
          <option value="disabled">禁用</option>
        </select>
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

    <div class="ds-main">
      <CategoryTreeFilter :rows="rows"
                          title="行业分类"
                          total-label="全部数据源"
                          store-key="datasources"
                          @change="onCategoryChange" />
      <!-- 左栏：数据源列表 -->
      <section class="pane pane-list">
        <div class="ds-list-scroll">
        <table class="bl-table ds-table">
          <thead>
            <tr>
              <th style="width:36px" class="t-center"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
              <th>
                <span class="th-sort" @click="toggleSort('dsName')">数据库名<span class="th-arrow">{{ sortArrow('dsName') }}</span></span>
              </th>
              <th>
                <span class="th-sort" @click="toggleSort('industry')">所属领域<span class="th-arrow">{{ sortArrow('industry') }}</span></span>
              </th>
              <th>
                <span class="th-sort" @click="toggleSort('refCount')">引用数<span class="th-arrow">{{ sortArrow('refCount') }}</span></span>
              </th>
              <th>
                <span class="th-sort" @click="toggleSort('status')">状态<span class="th-arrow">{{ sortArrow('status') }}</span></span>
              </th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <template v-for="row in displayRows" :key="row.key">
              <!-- 分组标题行 -->
              <tr v-if="row.type==='group'" class="ds-group-row" @click="toggleDomain(row.key)">
                <td colspan="6">
                  <span class="ds-group-chev" v-html="BL.icon(row.collapsed ? 'chevronRight' : 'chevronDown', 12)"></span>
                  <span class="ds-group-label">{{ row.label }}</span>
                  <span class="ds-group-count">{{ row.count }}</span>
                </td>
              </tr>
              <!-- 数据源行 -->
              <tr v-else :class="['ds-row', groupMode && 'is-grouped', selected?.id===row.data.id && 'is-active']" @click="select(row.data)" @dblclick="openEdit(row.data)">
                <td class="t-center" @click.stop>
                  <input type="checkbox" :checked="checked.has(row.data.id)" @change="toggleCheck(row.data.id)" />
                </td>
                <td>
                  <div class="ds-name-cell">
                    <span class="ds-ic" :style="{ background: typeColor(row.data.dsType) }" v-html="BL.icon(typeIcon(row.data.dsType), 12, '#fff')"></span>
                    <div class="ds-name-text">
                      <div class="ds-name bl-truncate" :title="row.data.dsName">{{ row.data.dsName }}</div>
                      <div class="ds-code bl-mono bl-muted">{{ row.data.dsCode }} · {{ row.data.dsType.toUpperCase() }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="bl-truncate" :title="dsDomainPath(row.data)">{{ dsIndustry(row.data) }}</span>
                </td>
                <td><span class="bl-tag">{{ row.data.refCount || 0 }}</span></td>
                <td>
                  <span :class="['bl-tag', statusTag(row.data).cls]">{{ statusTag(row.data).text }}</span>
                </td>
                <td style="width:80px">
                  <div class="bl-row" style="gap:0">
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="测试" @click.stop="doTest(row.data)" v-html="BL.icon('zap', 12)"></button>
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click.stop="openEdit(row.data)" v-html="BL.icon('edit', 12)"></button>
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click.stop="doDelete(row.data)" v-html="BL.icon('trash', 12)"></button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
        <div v-if="!filtered.length" class="bl-empty" style="padding:48px">暂无匹配数据源，请调整检索条件</div>
        </div>
        <!-- 分页钉底 -->
        <div class="ds-pager">
          <div class="ds-pager-l">
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm ds-del-btn" style="margin-left:8px" @click="batchRemove">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除</span>
              </button>
              <button class="bl-btn bl-btn-sm ds-ena-btn" style="margin-left:6px" @click="batchSetStatus(1)">
                <span v-html="BL.icon('check', 12)"></span><span style="margin-left:4px">启用</span>
              </button>
              <button class="bl-btn bl-btn-sm ds-dis-btn" style="margin-left:6px" @click="batchSetStatus(0)">
                <span v-html="BL.icon('power', 12)"></span><span style="margin-left:4px">禁用</span>
              </button>
              <button class="bl-btn bl-btn-sm bl-btn-text ds-clear-btn" style="margin-left:6px" @click="checked = new Set()">取消选择</button>
            </template>
            <template v-else>
              共 {{ filtered.length }} 项
              <span v-if="groupMode" class="bl-muted" style="margin-left:8px">· {{ groupCount }} 个领域</span>
            </template>
          </div>
          <div class="ds-pager-r" v-if="!groupMode">
            <span class="bl-muted" style="font-size:12px;margin-right:6px">每页</span>
            <select class="bl-input ds-page-size" v-model.number="pageSize">
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page<=1" @click="page--">‹</button>
            <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page>=totalPages" @click="page++">›</button>
          </div>
          <div class="ds-pager-r" v-else>
            <button class="bl-btn bl-btn-sm bl-btn-text" @click="expandAllGroups">展开全部</button>
            <button class="bl-btn bl-btn-sm bl-btn-text" @click="collapseAllGroups">折叠全部</button>
          </div>
        </div>
      </section>

      <!-- 全局抽屉: 数据源 / 监控 双页签 (覆盖整个视口高度) -->
      <transition name="ds-drawer">
      <aside v-if="drawerOpen" class="ds-drawer" :style="{ width: drawerWidth + 'px' }">
        <!-- 左侧拖拽手柄 -->
        <div class="ds-drag-handle" :class="resizing && 'is-resizing'" @mousedown="onDragStart"></div>
        <!-- 顶部: 行图标 + 数据库名 + 编码·类型 (随选中行,不随页签切换) -->
        <div class="ds-drawer-hd">
          <div class="ds-drawer-title">
            <span class="ds-drawer-ic" :style="{ background: selected ? typeColor(selected.dsType) : '#1677ff' }"
                  v-html="BL.icon(selected ? typeIcon(selected.dsType) : 'database', 16, '#fff')"></span>
            <div style="min-width:0">
              <div class="bl-truncate" style="font-weight:600;font-size:14px"
                   :title="selected?.dsName || form.dsName || '新建数据源'">
                {{ selected?.dsName || form.dsName || '新建数据源' }}
              </div>
              <div class="bl-mono bl-muted bl-truncate" style="font-size:11px"
                   :title="`${selected?.dsCode || form.dsCode || '—'}${selected?.dsType ? ' · ' + selected.dsType.toUpperCase() : ''}`">
                {{ selected?.dsCode || form.dsCode || '—' }}
                <span v-if="selected?.dsType"> · {{ selected.dsType.toUpperCase() }}</span>
              </div>
            </div>
          </div>
          <button v-if="selected" class="bl-btn bl-btn-sm ds-test-btn" @click="doTest(selected)" title="测试连接">
            <span v-html="BL.icon('zap', 12)"></span><span style="margin-left:4px">测试</span>
          </button>
          <button class="bl-btn bl-btn-text bl-btn-icon" @click="drawerOpen=false" v-html="BL.icon('x', 14)"></button>
        </div>

        <!-- 子页签 -->
        <div class="ds-drawer-tabs">
          <button :class="['ds-drawer-tab', drawerTab==='config' && 'is-on']" @click="switchTab('config')">
            <span v-html="BL.icon('sliders', 13)"></span>
            <span style="margin-left:4px">数据源</span>
          </button>
          <button :class="['ds-drawer-tab', drawerTab==='monitor' && 'is-on']" :disabled="!selected" @click="switchTab('monitor')">
            <span v-html="BL.icon('zap', 13)"></span>
            <span style="margin-left:4px">监控</span>
          </button>
        </div>

        <!-- 数据源 (编辑) 页签 -->
        <div v-show="drawerTab==='config'" class="cfg-body">
          <div class="cfg-section">基础信息</div>
          <FieldRow label="名称 *" inline><input class="bl-input" v-model="form.dsName" placeholder="例：水利主业务库" /></FieldRow>
          <FieldRow label="编码 *" inline hint="同领域内唯一，关联 ont_class 引用"><input class="bl-input bl-mono" v-model="form.dsCode" /></FieldRow>
          <FieldRow label="所属领域" inline>
            <select class="bl-input" v-model="form.categoryCode">
              <option value="">— 无 —</option>
              <option v-for="c in domainOptions" :key="c.code" :value="c.code">{{ c.name }}</option>
            </select>
          </FieldRow>
          <FieldRow label="类型 *" inline>
            <select class="bl-input" v-model="form.dsType" @change="onTypeChange">
              <option v-for="t in dsTypes" :key="t" :value="t">{{ t.toUpperCase() }}</option>
            </select>
          </FieldRow>

          <div class="cfg-section">连接配置</div>
          <template v-if="form.dsType !== 'mongodb'">
            <FieldRow label="JDBC 驱动" inline>
              <select class="bl-input" v-model="form.jdbcDriver">
                <option v-for="d in driversForType" :key="d" :value="d">{{ d }}</option>
              </select>
            </FieldRow>
            <FieldRow label="连接地址" inline><input class="bl-input bl-mono" v-model="form.jdbcUrl" placeholder="jdbc:mysql://127.0.0.1:3306/xxx" /></FieldRow>
            <FieldRow label="账号" inline><input class="bl-input" v-model="form.username" /></FieldRow>
            <FieldRow label="密码" inline><input class="bl-input" type="password" v-model="form.password" /></FieldRow>
          </template>
          <template v-else>
            <FieldRow label="Mongo URL" inline><input class="bl-input bl-mono" v-model="form.mongoUrl" placeholder="mongodb://127.0.0.1:27017/xxx" /></FieldRow>
            <FieldRow label="账号" inline><input class="bl-input" v-model="form.username" /></FieldRow>
            <FieldRow label="密码" inline><input class="bl-input" type="password" v-model="form.password" /></FieldRow>
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


        <!-- 监控 页签 -->
        <div v-show="drawerTab==='monitor'" class="mon-pane">
        <div v-if="!selected" class="bl-empty" style="padding:60px">
          <div class="bl-empty-icon" v-html="BL.icon('station', 36)"></div>
          请选择左侧数据源查看监控
        </div>
        <template v-else>
          <div class="mon-content">
          <!-- 第一行：元信息 + 状态图例 -->
          <div class="mon-metabar">
            <div class="mon-meta-left">
              <span class="bl-tag mon-tag" :style="{ color: typeColor(selected.dsType), background: typeColor(selected.dsType) + '14' }">{{ selected.dsType.toUpperCase() }}</span>
              <span class="bl-mono bl-muted">{{ mon.basic?.host || '—' }}</span>
              <span class="bl-muted">版本: <span class="bl-mono">{{ mon.basic?.version || '—' }}</span></span>
            </div>
            <div class="mon-legend">
              <span class="legend-item"><span class="ld" style="background:#00B42A"></span>正常</span>
              <span class="legend-item"><span class="ld" style="background:#FF7D00"></span>预警</span>
              <span class="legend-item"><span class="ld" style="background:#F53F3F"></span>异常</span>
              <span class="legend-item"><span class="ld" style="background:#C9CDD4"></span>闲置</span>
            </div>
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
        </div><!-- /监控 页签 -->

        <!-- 底部按钮: 仅数据源页签显示保存/取消 -->
        <div v-if="drawerTab==='config'" class="cfg-ft">
          <button class="bl-btn" @click="drawerOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="submitForm">保存</button>
        </div>
      </aside>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FieldRow from '@/views/config/category/FieldRow.vue'
import { BL } from '@/lib/bl.js'
import { datasourceApi, namespaceApi, categoryApi } from '@/api'
import CategoryTreeFilter from '@/components/CategoryTreeFilter.vue'

const rows = ref([])
const overview = ref({})
const q = ref('')
const selected = ref(null)
const checked = ref(new Set())   // 批量选择
const cfgOpen = ref(false)   // 兼容旧字段(用于其他历史代码引用)
const driverMap = ref({})
const monTab = ref('basic')
const mon = ref({})

/* 全局抽屉: 数据源 (编辑) / 监控 双页签 */
const drawerOpen = ref(false)
const drawerTab = ref('config')  // 'config' | 'monitor'
function switchTab(t) {
  if (t === 'monitor' && !selected.value) { BL.warning('请先选择一个数据源'); return }
  drawerTab.value = t
  if (t === 'monitor') loadMonitor()
}

/* 抽屉宽度拖拽 (持久化到 localStorage) */
const DRAWER_MIN = 520
function defaultDrawerWidth() {
  return Math.max(DRAWER_MIN, Math.min(760, Math.floor(window.innerWidth * 0.50)))
}
function drawerMaxPx() { return Math.floor(window.innerWidth * 0.90) }
const drawerWidth = ref(parseInt(localStorage.getItem('bl.ds-drawer.width') || '0', 10) || defaultDrawerWidth())
const resizing = ref(false)
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX
  dragStartW = drawerWidth.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const dx = dragStartX - e.clientX
  drawerWidth.value = Math.min(drawerMaxPx(), Math.max(DRAWER_MIN, dragStartW + dx))
}
function onDragEnd() {
  resizing.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  localStorage.setItem('bl.ds-drawer.width', String(drawerWidth.value))
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

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
function statusKey(d) {
  if (!d) return ''
  if (d.status === 0) return 'disabled'
  if (d.connectStatus === 'offline') return 'offline'
  if (d.connectStatus === 'risk') return 'risk'
  return 'online'
}
const STATUS_ORDER = { online: 0, risk: 1, offline: 2, disabled: 3 }

/* —— 所属领域(由 categoryCode 解析) —— */
const catInfoByCode = ref({})   // code -> { label, industryLabel, path }
function dsIndustry(d) {
  const info = catInfoByCode.value[d.categoryCode]
  return info ? (info.label || info.industryLabel) : (d.categoryCode || '—')
}
function dsDomainPath(d) {
  const info = catInfoByCode.value[d.categoryCode]
  return info ? info.path : (d.categoryCode || '')
}

/* —— 排序 / 筛选 —— */
const sortKey = ref('')
const sortDir = ref('')   // 'asc' | 'desc' | ''
function toggleSort(key) {
  if (sortKey.value !== key) { sortKey.value = key; sortDir.value = 'asc' }
  else if (sortDir.value === 'asc') sortDir.value = 'desc'
  else { sortKey.value = ''; sortDir.value = '' }
}
function sortArrow(key) {
  if (sortKey.value !== key) return ' ⇅'
  return sortDir.value === 'asc' ? ' ↑' : ' ↓'
}
const filterIndustry = ref('')
const filterStatus = ref('')
const industryFilterOptions = computed(() => {
  const set = new Set()
  rows.value.forEach(d => { const i = dsIndustry(d); if (i && i !== '—') set.add(i) })
  return [...set].sort((a, b) => a.localeCompare(b))
})

/* —— 左侧行业分类树 (统一组件) —— */
const selectedCategoryCodes = ref(null)
function onCategoryChange({ codes }) { selectedCategoryCodes.value = codes || null }

const filtered = computed(() => {
  let list = rows.value
  if (selectedCategoryCodes.value) list = list.filter(r => selectedCategoryCodes.value.has(r.categoryCode))
  const k = q.value.trim().toLowerCase()
  if (k) {
    list = list.filter(r =>
      [r.dsName, r.dsCode, r.dsType, r.remark, dsIndustry(r)].filter(Boolean)
        .some(s => String(s).toLowerCase().includes(k)))
  }
  if (filterIndustry.value) list = list.filter(d => dsIndustry(d) === filterIndustry.value)
  if (filterStatus.value) list = list.filter(d => statusKey(d) === filterStatus.value)
  if (sortKey.value && sortDir.value) {
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      let va, vb
      if (sortKey.value === 'dsName') { va = a.dsName || ''; vb = b.dsName || '' }
      else if (sortKey.value === 'industry') { va = dsIndustry(a); vb = dsIndustry(b) }
      else if (sortKey.value === 'refCount') { va = a.refCount || 0; vb = b.refCount || 0 }
      else if (sortKey.value === 'status') { va = STATUS_ORDER[statusKey(a)] ?? 9; vb = STATUS_ORDER[statusKey(b)] ?? 9 }
      if (typeof va === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }
  return list
})

/* —— 分页 —— */
const page = ref(1)
const pageSize = ref(20)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})
watch([filtered, pageSize], () => { if (page.value > totalPages.value) page.value = 1 })

/* —— 按所属领域分组显示 —— */
const groupMode = ref(false)
const collapsedDomains = ref(new Set())
function toggleDomain(key) {
  const s = new Set(collapsedDomains.value)
  if (s.has(key)) s.delete(key); else s.add(key)
  collapsedDomains.value = s
}
const groupedRows = computed(() => {
  const groups = new Map()
  filtered.value.forEach(d => {
    const key = dsIndustry(d) || '—'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(d)
  })
  const out = []
  for (const [key, items] of groups) {
    const collapsed = collapsedDomains.value.has(key)
    out.push({ type: 'group', key, label: key, count: items.length, collapsed })
    if (!collapsed) items.forEach(d => out.push({ type: 'item', key: 'i' + d.id, data: d }))
  }
  return out
})
const groupCount = computed(() => new Set(filtered.value.map(d => dsIndustry(d) || '—')).size)
const displayRows = computed(() =>
  groupMode.value
    ? groupedRows.value
    : paged.value.map(d => ({ type: 'item', key: 'i' + d.id, data: d }))
)
function expandAllGroups() { collapsedDomains.value = new Set() }
function collapseAllGroups() {
  collapsedDomains.value = new Set(filtered.value.map(d => dsIndustry(d) || '—'))
}

async function loadAll() {
  rows.value    = await datasourceApi.list().catch(() => [])
  overview.value= await datasourceApi.overview().catch(() => ({}))
  driverMap.value = await datasourceApi.drivers().catch(() => ({}))
  if (!domainOptions.value.length) {
    const ns = await namespaceApi.list().catch(() => [])
    const tree = await categoryApi.tree().catch(() => [])
    const list = []
    const info = {}
    const walk = (nodes, ancestors) => nodes.forEach(n => {
      const chain = [...ancestors, n]
      if (n.categoryCode) {
        list.push({ code: n.categoryCode, name: n.label })
        const industry = chain.find(x => x.categoryType === 1)
        info[n.categoryCode] = {
          label: n.label,
          industryLabel: industry ? industry.label : n.label,
          path: chain.map(x => x.label).join(' / ')
        }
      }
      if (n.children) walk(n.children, chain)
    })
    walk(tree, [])
    domainOptions.value = list
    catInfoByCode.value = info
  }
}

async function select(d) {
  selected.value = d
  // 点击行: 打开抽屉到"监控"页签
  drawerOpen.value = true
  if (drawerTab.value === 'config') {
    // 已经处于编辑态时,同步切换为当前选中的数据源
    Object.keys(form).forEach(k => delete form[k])
    Object.assign(form, d)
  } else {
    drawerTab.value = 'monitor'
  }
  await loadMonitor()
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
  selected.value = null
  drawerTab.value = 'config'
  drawerOpen.value = true
}
function openEdit(d) {
  // 先清掉旧字段，再覆盖；避免新数据缺字段时残留前一条的值
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, d)
  selected.value = d
  drawerTab.value = 'config'
  drawerOpen.value = true
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
  drawerOpen.value = false
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

/* —— 批量选择 + 删除 —— */
const dataRowsFiltered = computed(() => filtered.value)  // 简化引用; filtered 已是过滤后的数据源行
const allChecked = computed(() =>
  dataRowsFiltered.value.length > 0 && dataRowsFiltered.value.every(r => checked.value.has(r.id))
)
function toggleCheck(id) {
  const s = new Set(checked.value)
  s.has(id) ? s.delete(id) : s.add(id)
  checked.value = s
}
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) dataRowsFiltered.value.forEach(r => s.delete(r.id))
  else dataRowsFiltered.value.forEach(r => s.add(r.id))
  checked.value = s
}
async function batchRemove() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title:'批量删除', content:`确定删除选中 ${ids.length} 个数据源？删除后关联引用会失效。`, danger:true, okText:'删除' })
  if (!ok) return
  for (const id of ids) await datasourceApi.remove(id).catch(() => {})
  BL.success('已删除')
  checked.value = new Set()
  if (selected.value && ids.includes(selected.value.id)) selected.value = null
  await loadAll()
}

/* —— 批量启用 / 禁用 (status: 1 / 0) —— */
async function batchSetStatus(targetStatus) {
  const ids = [...checked.value]
  if (!ids.length) return
  const targets = rows.value.filter(r => ids.includes(r.id))
  const toChange = targets.filter(r => r.status !== targetStatus)
  if (!toChange.length) {
    BL.warning(`所选 ${ids.length} 项已经全部是「${targetStatus === 1 ? '启用' : '禁用'}」状态`)
    return
  }
  const label = targetStatus === 1 ? '启用' : '禁用'
  const ok = await BL.confirm({
    title: `批量${label}`,
    content: `确定将选中的 ${toChange.length} 个数据源${label}?`,
    okText: label
  })
  if (!ok) return
  let okCount = 0, failCount = 0
  for (const r of toChange) {
    try {
      await datasourceApi.update(r.id, { ...r, status: targetStatus })
      okCount++
    } catch { failCount++ }
  }
  if (failCount) BL.warning(`成功 ${okCount} 个,失败 ${failCount} 个`)
  else BL.success(`已${label} ${okCount} 个`)
  await loadAll()
}
// 切换筛选 / 搜索 / 分组时清空选择
watch([q, filterIndustry, filterStatus], () => { checked.value = new Set() })

onMounted(async () => {
  await loadAll()
  // 进入页面不自动选中也不自动打开抽屉 — 由用户主动点击行或"新建"打开
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

/* 主体: 列表单独占满整个 body (配置 + 监控 已迁移到全局抽屉) */
.ds-main {
  flex: 1;
  display: flex;
  gap: 12px; padding: 12px;
  overflow: hidden;
}
.pane {
  flex: 1;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  overflow: hidden;
  display: flex; flex-direction: column;
}
.pane-list { display: flex; flex-direction: column; overflow: hidden; }

/* —— 全局抽屉 (与系统其他抽屉风格一致) —— */
.ds-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  min-width: 520px; max-width: 90vw;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0,0,0,0.10);
  display: flex; flex-direction: column;
  z-index: 1000;
}
/* 左边缘拖拽手柄 */
.ds-drag-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; background: transparent;
  transition: background-color .15s;
  z-index: 1001;
}
.ds-drag-handle:hover, .ds-drag-handle.is-resizing { background: var(--bl-primary); }
.ds-drawer-hd {
  flex-shrink: 0; display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider);
}
.ds-drawer-title { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
/* 头部"测试"按钮: 主色描边,与右上角 × 关闭并列 */
.ds-test-btn {
  background: #fff; border: 1px solid var(--bl-primary); color: var(--bl-primary);
  flex-shrink: 0;
}
.ds-test-btn:hover { background: var(--bl-primary-soft); }
.ds-drawer-ic {
  width: 32px; height: 32px; border-radius: 6px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
/* 抽屉子页签 */
.ds-drawer-tabs {
  flex-shrink: 0; display: flex; align-items: center; gap: 2px;
  padding: 0 14px; border-bottom: 1px solid var(--bl-divider);
  height: 36px;
}
.ds-drawer-tab {
  padding: 0 14px; height: 36px;
  border: 0; background: transparent; cursor: pointer;
  font-size: 13px; color: var(--bl-text-2);
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  display: inline-flex; align-items: center;
  line-height: 1;
}
.ds-drawer-tab:hover:not(:disabled) { color: var(--bl-text-1); }
.ds-drawer-tab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
.ds-drawer-tab:disabled { opacity: .4; cursor: not-allowed; }
.bl-grow { flex: 1; }
.mon-pane { flex: 1; overflow: auto; }

.ds-drawer-enter-active, .ds-drawer-leave-active { transition: transform .22s, opacity .18s; }
.ds-drawer-enter-from, .ds-drawer-leave-to { transform: translateX(20px); opacity: 0; }
.ds-list-scroll { flex: 1; min-height: 0; overflow: auto; }
.ds-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  font-size: var(--bl-fs-12);
}
.ds-pager-l { display: inline-flex; align-items: center; flex-wrap: wrap; gap: 0; }
.ds-pager-r { display: inline-flex; align-items: center; gap: 4px; }
.ds-page-size { width: 64px; height: 26px; }

/* 批量操作按钮 (统一 outline 配色) */
.ds-del-btn { background: #fff; border: 1px solid #f53f3f; color: #f53f3f; }
.ds-del-btn:hover { background: #fff1f0; }
.ds-ena-btn { background: #fff; border: 1px solid #00b42a; color: #00b42a; }
.ds-ena-btn:hover { background: #e8fff4; }
.ds-dis-btn { background: #fff; border: 1px solid #86909c; color: #4e5969; }
.ds-dis-btn:hover { background: #f7f8fa; }
.ds-clear-btn { color: var(--bl-text-3); }
.ds-clear-btn:hover { color: var(--bl-primary); }
.t-center { text-align: center; }
.pane-cfg { transition: width .2s; }

.ds-table { width: 100%; }
/* 滚动时表头固定 */
.ds-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  box-shadow: inset 0 -1px 0 var(--bl-divider);   /* 用 inset 阴影代替 border，避免 sticky 下 border 失效 */
}
.ds-row { cursor: pointer; }
.ds-row.is-active { background: var(--bl-primary-soft); }
.ds-row.is-active td { color: var(--bl-primary); }
.ds-ic {
  width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ds-name-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.ds-name-text { min-width: 0; }

/* 表头排序 */
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; white-space: nowrap; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }

/* 列表 / 分组 视图切换 */
.ds-view-toggle {
  display: inline-flex; align-items: center;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  padding: 2px;
}
.vt-btn {
  display: inline-flex; align-items: center; gap: 4px;
  height: 26px; padding: 0 10px;
  border: 0; background: transparent; cursor: pointer;
  font-size: var(--bl-fs-12); color: var(--bl-text-2);
  border-radius: var(--bl-radius-1, 4px);
}
.vt-btn:hover { color: var(--bl-text-1); }
.vt-btn.is-on {
  background: var(--bl-bg-1); color: var(--bl-primary);
  font-weight: 500; box-shadow: var(--bl-shadow-1, 0 1px 2px rgba(0,0,0,.08));
}

/* 分组标题行 */
.ds-group-row { cursor: pointer; background: var(--bl-bg-2); }
.ds-group-row:hover { background: var(--bl-bg-hover); }
.ds-group-row td {
  padding: 7px 12px !important;
  border-bottom: 1px solid var(--bl-border);
}
.ds-group-chev {
  display: inline-flex; vertical-align: middle;
  color: var(--bl-text-3); margin-right: 6px;
}
.ds-group-label {
  font-weight: 600; font-size: var(--bl-fs-13); color: var(--bl-text-1);
  vertical-align: middle;
}
.ds-group-count {
  margin-left: 8px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 9px; padding: 0 8px; line-height: 16px;
  vertical-align: middle; font-feature-settings: "tnum";
}
.ds-row.is-grouped td:first-child { padding-left: 24px; }

/* 顶部筛选下拉 */
.hd-filter { width: 130px; }
.ds-name { font-weight: 500; color: var(--bl-text-1); }
.ds-code { font-size: var(--bl-fs-11); }

.cfg-hd {
  height: 48px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  font-weight: 600;
  border-bottom: 1px solid var(--bl-divider);
}
.cfg-body { flex: 1; overflow: auto; padding: 8px 14px; }
/* 表单 inline 行:收窄 label 列、缩小间距,让输入框(值)更宽 */
.cfg-body :deep(.fr.fr-inline) { gap: 8px; }
.cfg-body :deep(.fr.fr-inline .fr-label) { width: 72px; }
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
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
.mon-name { font-size: var(--bl-fs-14); font-weight: 600; flex: 1; min-width: 0; }
.mon-tag { font-weight: 600; }

/* 第一行：元信息 + 图例 */
.mon-metabar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; flex-wrap: wrap;
}
.mon-meta-left { display: inline-flex; align-items: center; gap: 10px; min-width: 0; flex-wrap: wrap; }

/* 状态图例 */
.mon-legend {
  display: flex; gap: 16px;
  padding: 0 4px;
  font-size: var(--bl-fs-12); color: var(--bl-text-2);
  flex-shrink: 0;
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
