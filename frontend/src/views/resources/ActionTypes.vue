<template>
  <div class="page at-page">
    <PageHeader title="动作类型" subtitle="Action types · 定义对象/链接上的参数化操作,创建 / 修改 / 删除实例及副作用">
      <template #actions>
        <div class="ov">
          <span class="ov-item"><span class="ov-lbl">总数</span><b>{{ rows.length }}</b></span>
          <span class="ov-item ov-ok"><span class="ov-lbl">已发布</span><b>{{ publishedCount }}</b></span>
          <span class="ov-item"><span class="ov-lbl">草稿</span><b style="color:#86909c">{{ draftCount }}</b></span>
          <span class="ov-item ov-risk"><span class="ov-lbl">已停用</span><b>{{ disabledCount }}</b></span>
        </div>
        <button :class="['bl-btn at-group-btn', groupMode && 'is-on']" :title="groupMode ? '取消分组' : '按领域分组'" @click="groupMode = !groupMode">
          <span v-html="BL.icon('layers', 13)"></span><span style="margin-left:4px">分组</span>
        </button>
        <BlSelect v-model="filterActionType" :options="ACTION_TYPE_FILTER_OPTS" placeholder="全部类型" clearable style="width:130px" />
        <SearchSelect v-model="filterObjectClass" :options="objectFilterOptions" placeholder="全部对象类型"
                      search-placeholder="搜索对象名称 / 编码" style="width:168px" />
        <SearchSelect v-model="filterLinkType" :options="linkFilterOptions" placeholder="全部链接类型"
                      search-placeholder="搜索链接名称 / 编码" style="width:150px" />
        <div class="at-msel" ref="statusMsel">
          <button type="button" class="bl-input hd-filter at-msel-btn" @click="statusOpen = !statusOpen" title="状态 (多选)">
            <span :class="{ 'is-ph': statusFilterLabel === '全部状态' }">{{ statusFilterLabel }}</span>
            <span class="at-msel-arrow" :class="{ 'is-open': statusOpen }" v-html="BL.icon('chevronDown', 12)"></span>
          </button>
          <div v-if="statusOpen" class="at-msel-pop">
            <label v-for="s in STATUS_OPTS" :key="s.v" class="at-msel-opt">
              <input type="checkbox" :checked="filterStatuses.has(s.v)" @change="toggleStatus(s.v)" /> {{ s.label }}
            </label>
          </div>
        </div>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索动作 (名称 / 编码 / 主体 / RID)" v-model="q" />
        </div>
        <button class="bl-btn bl-btn-primary" @click="openCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建动作</span>
        </button>
      </template>
    </PageHeader>

    <div class="at-main">
      <CategoryTreeFilter :rows="rows"
                          :custom-counts="customCategoryCounts"
                          title="行业领域分组"
                          total-label="全部动作"
                          store-key="action-types"
                          @change="onCatChange" />

      <section class="pane pane-list">
        <div class="at-list-scroll">
          <table class="bl-table at-table">
            <colgroup>
              <col style="width:36px" /><col style="width:210px" /><col style="width:170px" />
              <col style="width:110px" /><col style="width:180px" /><col style="width:120px" />
              <col style="width:120px" /><col style="width:230px" /><col style="width:90px" />
              <col style="width:80px" /><col style="width:80px" /><col style="width:190px" /><col style="width:120px" />
            </colgroup>
            <thead>
              <tr>
                <th class="t-center"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th class="t-left">动作名称</th>
                <th class="t-left">动作编码</th>
                <th class="t-center">动作类型</th>
                <th class="t-left">关联主体</th>
                <th class="t-center">展示场景</th>
                <th class="t-center">配置能力</th>
                <th class="t-center">规则数</th>
                <th class="t-center">编译状态</th>
                <th class="t-center">当前版本</th>
                <th class="t-center">状态</th>
                <th class="t-left">RID</th>
                <th class="t-center">操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="row in displayRows" :key="row.key">
              <tr v-if="row.type === 'group'" class="at-group-row" @click="toggleGroup(row.key)">
                <td :colspan="13">
                  <span class="at-group-chev" v-html="BL.icon(row.collapsed ? 'chevronRight' : 'chevronDown', 12)"></span>
                  <span class="at-group-label">{{ row.label }}</span>
                  <span class="at-group-count">{{ row.count }}</span>
                </td>
              </tr>
              <tr v-else v-for="r in [row.data]" :key="r.id"
                  :class="['at-row', selectedId === r.id && 'is-active']"
                  @click="onRowClick(r)">
                <td class="t-center" @click.stop>
                  <input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" />
                </td>
                <td class="t-left">
                  <div class="at-name-cell">
                    <span class="at-card-ic" :style="{ background: typeColor(r) }">
                      <span v-html="BL.icon(r.icon || typeIcon(r), 13, '#fff')"></span>
                    </span>
                    <div class="at-name-txt">
                      <div class="bl-truncate" style="font-weight:500" :title="r.rdfs_label">{{ r.rdfs_label || '—' }}</div>
                      <div class="bl-muted bl-truncate" style="font-size:11px" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</div>
                    </div>
                  </div>
                </td>
                <td class="t-left"><span class="bl-mono bl-muted bl-truncate" :title="r.api_name">{{ r.api_name }}</span></td>
                <td class="t-center">
                  <span class="bl-tag" :style="typeTagStyle(r)">{{ typeLabel(r) }}</span>
                </td>
                <td class="t-left">
                  <div class="at-ent-cell" v-if="subjectName(r)">
                    <span class="at-ent-ic" :style="{ background: r.object_class_color || '#165DFF' }"
                          v-html="BL.icon(r.object_class_icon || subjectIcon(r), 11, '#fff')"></span>
                    <div class="at-ent-txt">
                      <div class="bl-truncate" :title="subjectName(r)">{{ subjectName(r) }}</div>
                      <div class="bl-mono bl-muted bl-truncate">{{ subjectCode(r) || '—' }}</div>
                    </div>
                  </div>
                  <span v-else class="bl-muted">—</span>
                </td>
                <td class="t-center">
                  <span v-if="r.show_on_detail" class="bl-tag at-mini-tag" title="详情页展示">详情</span>
                  <span v-if="r.show_on_batch" class="bl-tag at-mini-tag" title="批量场景展示">批量</span>
                  <span v-if="!r.show_on_detail && !r.show_on_batch" class="bl-muted">—</span>
                </td>
                <td class="t-center">
                  <span class="at-cap" :class="{ 'is-on': r.form_enabled }" title="表单">表单<span v-html="BL.icon('check', 11)"></span></span>
                  <span class="at-cap" :class="{ 'is-on': r.submit_criteria_enabled }" title="提交标准">校验<span v-html="BL.icon('check', 11)"></span></span>
                </td>
                <td class="t-center">
                  <div class="at-rulecnt" v-if="r.rule_count">
                    <span v-if="r.rule_object" title="对象规则">对象<b>{{ r.rule_object }}</b></span>
                    <span v-if="r.rule_link" title="链接规则">链接<b>{{ r.rule_link }}</b></span>
                    <span v-if="r.rule_notify" title="通知规则">通知<b>{{ r.rule_notify }}</b></span>
                    <span v-if="r.rule_function" title="函数规则">函数<b>{{ r.rule_function }}</b></span>
                    <span v-if="r.rule_webhook" title="Webhook 规则">WH<b>{{ r.rule_webhook }}</b></span>
                  </div>
                  <span v-else class="bl-muted">—</span>
                </td>
                <td class="t-center"><span :class="['bl-tag', compileTagCls(r.compile_status)]">{{ compileLabel(r.compile_status) }}</span></td>
                <td class="t-center"><span class="bl-mono bl-muted">{{ r.current_version || '—' }}</span></td>
                <td class="t-center"><span :class="['bl-tag', statusTagCls(r.status)]">{{ statusLabel(r.status) }}</span></td>
                <td class="t-left" @click.stop>
                  <div class="at-rid">
                    <span class="bl-mono bl-muted bl-truncate" :title="r.rid">{{ ridShort(r.rid) }}</span>
                    <button v-if="r.rid" class="at-rid-copy" title="复制完整 RID" @click="copyRid(r.rid)" v-html="BL.icon('copy', 12)"></button>
                  </div>
                </td>
                <td class="t-center" @click.stop>
                  <div class="at-ops">
                    <button class="bl-btn bl-btn-sm bl-btn-text at-op-run" @click="openRun(r)" title="试运行">
                      <span v-html="BL.icon('play', 12)"></span>
                    </button>
                    <button v-if="r.status !== 1" class="bl-btn bl-btn-sm bl-btn-text at-op-pub" @click="onSetStatus(r, 1)">发布</button>
                    <button v-else class="bl-btn bl-btn-sm bl-btn-text at-op-off" @click="onSetStatus(r, 2)">停用</button>
                    <button class="bl-btn bl-btn-sm bl-btn-text at-op-del" @click="onDeleteOne(r)" title="删除">
                      <span v-html="BL.icon('trash', 12)"></span>
                    </button>
                  </div>
                </td>
              </tr>
              </template>
            </tbody>
          </table>
          <div v-if="!filtered.length" class="bl-empty" style="padding:48px">暂无匹配的动作,请调整筛选条件或点击「新建动作」</div>
        </div>

        <!-- 底部分页 + 批量 -->
        <div class="at-pager">
          <div class="at-pager-l">
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm at-del-btn" style="margin-left:8px" @click="onBatchDelete">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除</span>
              </button>
              <button class="bl-btn bl-btn-sm bl-btn-text" style="margin-left:6px" @click="checked = new Set()">取消选择</button>
            </template>
            <template v-else>
              共 {{ filtered.length }} 项<span v-if="groupMode" class="bl-muted" style="margin-left:8px">· {{ groupCount }} 个领域</span>
            </template>
          </div>
          <Pager v-if="!groupMode" v-model:page="atPage" v-model:page-size="atPageSize" :total-pages="atTotalPages" />
        </div>
      </section>
    </div>

    <!-- 试运行弹窗 -->
    <ActionRunModal v-model:open="runOpen" :action="runAction" @executed="load" />

    <!-- 新建向导 (5 步) -->
    <ActionCreateWizard v-model:open="wizardOpen"
                        :all-classes="allClasses"
                        :all-link-types="allLinkTypes"
                        :init-category="selectedCategoryCode"
                        @created="onWizardCreated" />

    <!-- 编辑详情: 全屏三栏工作台 -->
    <ActionDetailWorkspace v-model:open="editorOpen"
                           :action-id="editorActionId"
                           :all-classes="allClasses"
                           :all-link-types="allLinkTypes"
                           @saved="onSaved"
                           @deleted="onDeleted" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { actionTypeApi, linkTypeApi, resourceApi, categoryApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import CategoryTreeFilter from '@/components/CategoryTreeFilter.vue'
import ActionDetailWorkspace from './actiontype/ActionDetailWorkspace.vue'
import ActionCreateWizard from './actiontype/ActionCreateWizard.vue'
import ActionRunModal from './actiontype/ActionRunModal.vue'
import SearchSelect from '@/components/SearchSelect.vue'
import BlSelect from '@/components/BlSelect.vue'
import Pager from '@/components/Pager.vue'
import { usePagination } from '@/lib/usePagination'
import { useCategoryGroupFilter } from '@/composables/useCategoryGroupFilter'

/* —— 动作大类 (m_type) 与 细分类型 (action_type) 映射 —— */
const M_TYPES = {
  1: { label: '对象' }, 2: { label: '链接' }, 3: { label: '函数' },
  4: { label: 'Webhook' }, 5: { label: '接口' }, 6: { label: '通知' },
}
const ACTION_TYPES = {
  11: { label: '创建对象', color: '#00B42A', icon: 'plus' },
  12: { label: '修改对象', color: '#165DFF', icon: 'edit' },
  13: { label: 'Upsert 对象', color: '#165DFF', icon: 'edit' },
  14: { label: '删除对象', color: '#F53F3F', icon: 'trash' },
  21: { label: '创建链接', color: '#14C9C9', icon: 'link' },
  22: { label: '删除链接', color: '#F53F3F', icon: 'link' },
  30: { label: '函数', color: '#722ED1', icon: 'code' },
  40: { label: 'Webhook', color: '#FF7D00', icon: 'zap' },
  51: { label: '接口·创建', color: '#0FC6C2', icon: 'plug' },
  52: { label: '接口·修改', color: '#0FC6C2', icon: 'plug' },
  53: { label: '接口·删除', color: '#0FC6C2', icon: 'plug' },
  54: { label: '接口·查询', color: '#0FC6C2', icon: 'plug' },
  60: { label: '通知', color: '#B71DE8', icon: 'bell' },
}

const rows = ref([])
const groups = ref([])
const allClasses = ref([])
const allLinkTypes = ref([])
const checked = ref(new Set())
const selectedId = ref(null)
const q = ref('')
const filterActionType = ref('')
const ACTION_TYPE_FILTER_OPTS = [
  { value: '11', label: '创建对象' }, { value: '12', label: '修改对象' }, { value: '14', label: '删除对象' },
  { value: '21', label: '创建链接' }, { value: '22', label: '删除链接' }, { value: '30', label: '函数执行' },
]
const filterObjectClass = ref('')
const filterLinkType = ref('')
const objectFilterOptions = computed(() => (allClasses.value || []).map(c => ({ value: c.id, label: `${c.display_name || c.rdfs_label || c.api_name} (${c.api_name})` })))
const linkFilterOptions = computed(() => (allLinkTypes.value || []).map(l => ({ value: l.id, label: l.rdfs_label || l.link_type_id })))
/* 状态多选 */
const STATUS_OPTS = [{ v: 0, label: '草稿' }, { v: 1, label: '已发布' }, { v: 2, label: '已停用' }]
const filterStatuses = ref(new Set([0, 1, 2]))
const statusOpen = ref(false)
const statusMsel = ref(null)
function toggleStatus(v) { const s = new Set(filterStatuses.value); s.has(v) ? s.delete(v) : s.add(v); filterStatuses.value = s }
const statusFilterLabel = computed(() => {
  const n = filterStatuses.value.size
  if (n === 0 || n === 3) return '全部状态'
  if (n === 1) return STATUS_OPTS.find(s => filterStatuses.value.has(s.v))?.label || '全部状态'
  return `已选 ${n} 项`
})
function onStatusDocClick(e) { if (statusMsel.value && !statusMsel.value.contains(e.target)) statusOpen.value = false }
onMounted(() => window.addEventListener('click', onStatusDocClick))
onUnmounted(() => window.removeEventListener('click', onStatusDocClick))

const {
  customCategoryCounts,
  onCategoryChange,
  loadCategoryTree,
  filterByCategory
} = useCategoryGroupFilter({ items: rows, groups })

const editorOpen = ref(false)
const editorActionId = ref('')
const wizardOpen = ref(false)
const runOpen = ref(false)
const runAction = ref(null)
function openRun(r) { runAction.value = r; runOpen.value = true }
const selectedCategoryCode = ref('')
function onCatChange(p) {
  selectedCategoryCode.value = p?.categoryCode || ''
  onCategoryChange(p)
}

const route = useRoute()
const router = useRouter()

async function load() {
  rows.value = await actionTypeApi.list().catch(() => [])
}
async function loadRefs() {
  const list = await resourceApi.classes().catch(() => [])
  allClasses.value = Array.isArray(list) ? list : (list?.data || list?.rows || [])
  allLinkTypes.value = await linkTypeApi.list().catch(() => [])
}
function applyOpenId(id) {
  if (!id) return
  const row = rows.value.find(r => r.id === id)
  if (row) { onRowClick(row); router.replace({ query: {} }) }
}
onMounted(async () => {
  await Promise.all([load(), loadCategoryTree()])
  loadRefs()
  loadCategoryLabels()
  applyOpenId(route.query.openId)
})
watch(() => route.query.openId, applyOpenId)

/* 统计 */
const publishedCount = computed(() => rows.value.filter(r => Number(r.status) === 1).length)
const draftCount = computed(() => rows.value.filter(r => Number(r.status) === 0).length)
const disabledCount = computed(() => rows.value.filter(r => Number(r.status) === 2).length)

/* 过滤 */
const filtered = computed(() => {
  let list = rows.value
  list = filterByCategory(list)
  if (filterActionType.value !== '') list = list.filter(r => String(r.action_type) === filterActionType.value)
  if (filterObjectClass.value) list = list.filter(r => r.object_class_id === filterObjectClass.value)
  if (filterLinkType.value) list = list.filter(r => r.link_type_id === filterLinkType.value)
  const ss = filterStatuses.value
  if (ss.size > 0 && ss.size < 3) list = list.filter(r => ss.has(Number(r.status)))
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.rdfs_label, r.api_name, r.object_class_name, r.link_type_name, r.rid, typeLabel(r)]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  return list
})

const { page: atPage, pageSize: atPageSize, totalPages: atTotalPages, paged } = usePagination(filtered)

/* —— 按领域分组 —— */
const groupMode = ref(false)
const collapsedGroups = ref(new Set())
function toggleGroup(key) { const s = new Set(collapsedGroups.value); s.has(key) ? s.delete(key) : s.add(key); collapsedGroups.value = s }
const categoryLabelMap = ref({})       // category_code -> "行业 / 领域"
async function loadCategoryLabels() {
  const tree = await categoryApi.tree().catch(() => [])
  const map = {}
  const walk = (nodes, parentLabel) => (nodes || []).forEach(n => {
    const label = n.label || n.rdfsLabel || n.categoryCode
    const full = parentLabel ? `${parentLabel} / ${label}` : label
    if (n.categoryCode) map[n.categoryCode] = full
    if (n.children) walk(n.children, full)
  })
  walk(tree, '')
  categoryLabelMap.value = map
}
function groupKeyOf(r) { return categoryLabelMap.value[r.category_code] || '未分类' }
const groupedRows = computed(() => {
  const groups = new Map()
  filtered.value.forEach(r => { const k = groupKeyOf(r); if (!groups.has(k)) groups.set(k, []); groups.get(k).push(r) })
  const out = []
  for (const [key, items] of groups) {
    const collapsed = collapsedGroups.value.has(key)
    out.push({ type: 'group', key, label: key, count: items.length, collapsed })
    if (!collapsed) items.forEach(r => out.push({ type: 'item', key: 'i' + r.id, data: r }))
  }
  return out
})
const groupCount = computed(() => new Set(filtered.value.map(groupKeyOf)).size)
const displayRows = computed(() => groupMode.value
  ? groupedRows.value
  : paged.value.map(r => ({ type: 'item', key: 'i' + r.id, data: r })))

const allChecked = computed(() => paged.value.length > 0 && paged.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) {
  const s = new Set(checked.value); s.has(id) ? s.delete(id) : s.add(id); checked.value = s
}
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) paged.value.forEach(r => s.delete(r.id))
  else paged.value.forEach(r => s.add(r.id))
  checked.value = s
}

function onRowClick(r) {
  selectedId.value = r.id
  editorActionId.value = r.id
  editorOpen.value = true
}
function openCreate() {
  wizardOpen.value = true
}
async function onWizardCreated(id) {
  await load()
  if (id) { selectedId.value = id; editorActionId.value = id; editorOpen.value = true }
}
async function onSaved() { await load() }
async function onDeleted() { editorOpen.value = false; await load() }

async function onBatchDelete() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中的 ${ids.length} 个动作?`, danger: true, okText: '删除' })
  if (!ok) return
  await actionTypeApi.batchRemove(ids).catch(() => null)
  BL.success(`已删除 ${ids.length} 个`)
  checked.value = new Set()
  await load()
}
async function onDeleteOne(r) {
  const ok = await BL.confirm({ title: '删除动作', content: `确定删除「${r.rdfs_label || r.api_name}」?`, danger: true, okText: '删除' })
  if (!ok) return
  await actionTypeApi.remove(r.id).catch(() => null)
  BL.success('已删除')
  await load()
}
async function onSetStatus(r, status) {
  await actionTypeApi.setStatus(r.id, status).catch(() => null)
  BL.success(status === 1 ? '已发布' : '已停用')
  await load()
}

/* 工具:动作类型 / 关联主体 / 状态 / 编译 */
function typeMeta(r) { return ACTION_TYPES[Number(r.action_type)] || { label: '—', color: '#86909c', icon: 'zap' } }
function typeLabel(r) { return typeMeta(r).label }
function typeColor(r) { return r.color || typeMeta(r).color }
function typeIcon(r) { return typeMeta(r).icon }
function typeTagStyle(r) {
  const c = typeMeta(r).color
  return { background: `color-mix(in srgb, ${c} 12%, transparent)`, color: c, border: `1px solid color-mix(in srgb, ${c} 30%, transparent)` }
}
function subjectName(r) { return Number(r.m_type) === 2 ? r.link_type_name : r.object_class_name }
function subjectCode(r) { return Number(r.m_type) === 2 ? r.link_type_code : r.object_class_api }
function subjectIcon(r) { return Number(r.m_type) === 2 ? 'link' : 'box' }
function statusLabel(s) { return ({ 0: '草稿', 1: '已发布', 2: '已停用' })[Number(s)] || '—' }
function statusTagCls(s) { return ({ 0: '', 1: 'bl-tag-success', 2: 'bl-tag-warning' })[Number(s)] || '' }
function compileLabel(s) { return ({ 0: '未编译', 1: '通过', 2: '失败' })[Number(s)] || '—' }
function compileTagCls(s) { return ({ 0: '', 1: 'bl-tag-success', 2: 'bl-tag-danger' })[Number(s)] || '' }
function ridShort(rid) { if (!rid) return '—'; const s = String(rid); return s.length > 24 ? '…' + s.slice(-22) : s }
async function copyRid(rid) { try { await navigator.clipboard.writeText(rid); BL.success('已复制 RID') } catch { BL.warning('复制失败') } }
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }

/* 顶部统计 */
.ov { display: inline-flex; gap: 14px; padding: 4px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); }
.ov-item { font-size: var(--bl-fs-13); color: var(--bl-text-2); }
.ov-item .ov-lbl { color: var(--bl-text-3); margin-right: 6px; }
.ov-item b { font-weight: 600; color: var(--bl-text-1); }
.ov-ok b { color: var(--bl-success); }
.ov-risk b { color: var(--bl-warning); }

.hd-filter { width: 130px; }
/* 原生 select 自定义箭头 (与 SearchSelect / 状态按钮一致) */
select.bl-input {
  appearance: none; -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 10px center; padding-right: 28px;
}
:root[data-theme="dark"] select.bl-input {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23a9b0bd' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
}
.at-msel { position: relative; }
.at-msel-btn { display: inline-flex; align-items: center; justify-content: space-between; gap: 6px; width: 118px; cursor: pointer; }
.at-msel-btn .is-ph { color: var(--bl-text-3); }
.at-msel-arrow { color: var(--bl-text-3); display: inline-flex; transition: transform .15s; }
.at-msel-arrow.is-open { transform: rotate(180deg); }
.at-msel-pop { position: absolute; top: calc(100% + 4px); left: 0; z-index: 100; min-width: 134px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px; box-shadow: 0 6px 18px rgba(0,0,0,.14); padding: 4px; }
.at-msel-opt { display: flex; align-items: center; gap: 6px; padding: 6px 8px; border-radius: 4px; font-size: 13px; cursor: pointer; color: var(--bl-text-1); }
.at-msel-opt:hover { background: var(--bl-bg-hover); }
.search-wrap { position: relative; width: 280px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

/* 主体 */
.at-main { flex: 1; display: flex; gap: 12px; padding: 12px; overflow: hidden; }
.pane {
  flex: 1; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3);
  overflow: hidden; display: flex; flex-direction: column;
}
.pane-list { display: flex; flex-direction: column; overflow: hidden; }

.at-list-scroll { flex: 1; min-height: 0; overflow: auto; }
.at-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  font-size: 12px; display: flex; justify-content: space-between; align-items: center;
}
.at-pager-l { display: inline-flex; align-items: center; }
.at-del-btn { background: var(--bl-bg-1); border: 1px solid #f53f3f; color: #f53f3f; }
.at-del-btn:hover { background: color-mix(in srgb, #f53f3f 8%, var(--bl-bg-1)); }

/* 表格 */
.at-table { width: 100%; min-width: 1680px; table-layout: fixed; }
.at-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-thead-bg);
  font-weight: 600; font-size: 12px; height: 40px; padding: 0 8px;
  color: var(--bl-text-1); white-space: nowrap;
}
.at-table thead th.t-left { text-align: left; }
.at-table tbody tr { background: var(--bl-bg-1); cursor: pointer; }
.at-table tbody tr:hover { background: var(--bl-bg-hover); }
.at-table tbody tr.is-active { background: var(--bl-primary-soft); }
.at-table td { padding: 0 8px; font-size: 12px; height: 46px; vertical-align: middle; }
.at-table td.t-center { text-align: center; }
.at-table td.t-left { text-align: left; }

.at-card-ic {
  width: 28px; height: 28px; border-radius: 6px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.at-name-cell { display: inline-flex; align-items: center; gap: 8px; min-width: 0; max-width: 100%; }
.at-name-txt { min-width: 0; }

.at-ent-cell { display: inline-flex; align-items: center; gap: 8px; min-width: 0; max-width: 100%; }
.at-ent-ic { width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.at-ent-txt { min-width: 0; }
.at-ent-txt .bl-truncate:first-child { font-weight: 500; color: var(--bl-text-1); }
.at-ent-txt .bl-mono { font-size: 11px; }

.at-mini-tag { margin: 0 2px; padding: 0 6px; }
.at-cap { display: inline-flex; align-items: center; gap: 1px; margin: 0 5px; font-size: 11px; color: var(--bl-text-3); }
.at-cap.is-on { color: var(--bl-success); font-weight: 600; }
.at-rulecnt { display: flex; flex-wrap: wrap; gap: 3px 4px; justify-content: center; align-items: center; font-size: 11px; }
.at-rulecnt span { white-space: nowrap; padding: 1px 5px; border-radius: 4px; background: var(--bl-bg-2); color: var(--bl-text-2); line-height: 16px; }
.at-rulecnt b { margin-left: 2px; font-weight: 700; color: var(--bl-text-1); }
.at-rid { display: inline-flex; align-items: center; gap: 4px; max-width: 100%; }
.at-rid-copy { flex-shrink: 0; width: 22px; height: 22px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; }
.at-rid-copy:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }

/* 分组 */
.at-group-btn { display: inline-flex; align-items: center; }
.at-group-btn.is-on { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }
.at-group-row { background: var(--bl-bg-2); cursor: pointer; }
.at-group-row:hover { background: var(--bl-bg-hover); }
.at-group-row td { height: 34px; padding: 0 12px; }
.at-group-chev { color: var(--bl-text-3); display: inline-flex; vertical-align: middle; margin-right: 6px; }
.at-group-label { font-weight: 600; color: var(--bl-text-1); font-size: 12.5px; }
.at-group-count { margin-left: 8px; padding: 0 7px; border-radius: 10px; background: var(--bl-bg-3); color: var(--bl-text-2); font-size: 11px; }

.at-ops { display: inline-flex; align-items: center; gap: 2px; }
.at-op-run { color: var(--bl-primary); }
.at-op-pub { color: var(--bl-success); }
.at-op-off { color: #86909c; }
.at-op-del { color: #f53f3f; }
</style>
