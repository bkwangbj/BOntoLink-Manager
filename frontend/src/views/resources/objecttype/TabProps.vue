<template>
  <div class="tab-props">
    <!-- 子页签: 属性列表 / 关系图 (per spec 1.1.1.1.1.1.1.3 总体布局: 顶部为双页签导航) -->
    <div class="pp-subtabs">
      <button :class="['pp-stab', subTab==='list' && 'is-on']" @click="subTab='list'">
        <span v-html="BL.icon('list', 13)"></span><span style="margin-left:4px">属性列表</span>
      </button>
      <button :class="['pp-stab', subTab==='graph' && 'is-on']" @click="subTab='graph'">
        <span v-html="BL.icon('link', 13)"></span><span style="margin-left:4px">关系图</span>
      </button>
    </div>

    <!-- 关系图: ER 风格画布 -->
    <TabPropsCanvas v-if="subTab==='graph'" :class-id="classId" class="pp-canvas" />

    <!-- 属性列表 (按 1.1.1.1.1.1.1.4 重排:顶部工具栏 / 工具栏底部说明区 / 表格展示区) -->
    <template v-if="subTab==='list'">
    <!-- (1) 顶部固定操作栏: 白色卡片 / 圆角 8px / 阴影 -->
    <div class="pp-toolbar">
      <div class="pp-filters">
        <select class="bl-input" v-model="filterType">
          <option value="">全部属性类型</option>
          <option value="data">数据属性</option>
          <option value="object">对象属性</option>
          <option value="annotation">注释属性</option>
        </select>
        <select class="bl-input" v-model="filterTable">
          <option value="">全部物理表</option>
          <option v-for="t in tableOpts" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
        <select class="bl-input" v-model="filterStatus">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
        <div class="pp-search">
          <span class="pp-search-ic" v-html="BL.icon('search', 13)"></span>
          <input class="bl-input" v-model="q" placeholder="搜索名称 / 编码" />
        </div>
      </div>
      <div class="pp-actions">
        <button v-if="checked.size" class="bl-btn bl-btn-sm pp-batch-fmt" @click="openBatchFormat">
          <span v-html="BL.icon('sliders', 12)"></span>
          <span style="margin-left:4px">批量格式化 ({{ checked.size }})</span>
        </button>
        <button v-if="checked.size" class="bl-btn bl-btn-sm pp-del-btn" @click="onBatchDelete">
          <span v-html="BL.icon('trash', 12)"></span>
          <span style="margin-left:4px">删除 ({{ checked.size }})</span>
        </button>
        <div class="pp-add-wrap" @click.stop>
          <button class="bl-btn bl-btn-sm bl-btn-primary pp-add-btn" @click="addMenu = !addMenu">
            <span v-html="BL.icon('plus', 12, '#fff')"></span>
            <span style="margin-left:4px">新增属性</span>
            <span style="margin-left:6px" v-html="BL.icon('chevronDown', 10, '#fff')"></span>
          </button>
          <div v-if="addMenu" class="pp-add-menu">
            <div class="pp-add-item" @click="addRow('data', false, true); addMenu=false">
              <span class="pp-add-dot" style="background:#722ED1" v-html="BL.icon('zap',12,'#fff')"></span>
              <div><div>新增虚拟属性</div><div class="bl-muted">virtual · 不绑定物理字段</div></div>
            </div>
            <div class="pp-add-item" @click="addRow('object'); addMenu=false">
              <span class="pp-add-dot" style="background:#FF7D00" v-html="BL.icon('link',12,'#fff')"></span>
              <div><div>新增对象属性</div><div class="bl-muted">object · 关联另一对象类</div></div>
            </div>
            <div class="pp-add-item" @click="addRow('data'); addMenu=false">
              <span class="pp-add-dot" style="background:#1677ff" v-html="BL.icon('database',12,'#fff')"></span>
              <div><div>新增物理字段</div><div class="bl-muted">data · 绑定物理表字段</div></div>
            </div>
            <div class="pp-add-item" @click="addRow('annotation'); addMenu=false">
              <span class="pp-add-dot" style="background:#00B42A" v-html="BL.icon('chat',12,'#fff')"></span>
              <div><div>新增注释属性</div><div class="bl-muted">annotation · 仅标签说明</div></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- (2) 工具栏底部说明区 -->
    <div class="pp-legend">
      <div class="pp-legend-l">
        <span><span class="lg-dot" style="background:#1677ff" v-html="BL.icon('database',10,'#fff')"></span>普通数据属性</span>
        <span><span class="lg-dot" style="background:#722ED1" v-html="BL.icon('zap',10,'#fff')"></span>计算派生属性</span>
        <span><span class="lg-dot" style="background:#FF7D00" v-html="BL.icon('link',10,'#fff')"></span>对象属性</span>
        <span><span class="lg-dot" style="background:#00B42A" v-html="BL.icon('chat',10,'#fff')"></span>注释属性</span>
      </div>
      <span class="pp-legend-r">（搜索 / 筛选 / 排序时自动禁用拖拽）</span>
    </div>

    <!-- (3) 表格展示区 -->
    <div class="pp-table-wrap">
      <table class="bl-table pp-table">
        <colgroup>
          <col style="width:40px" />
          <col style="width:170px" />
          <col style="width:140px" />
          <col style="width:140px" /><col style="width:140px" />
          <col style="width:110px" /><col style="width:110px" />
          <col style="width:110px" />
          <col style="width:70px" /><col style="width:70px" /><col style="width:70px" /><col style="width:80px" />
          <col style="width:80px" /><col style="width:200px" />
          <col style="width:80px" />
        </colgroup>
        <thead>
          <tr>
            <th class="t-center pp-stk-l1"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
            <th class="t-left pp-stk-l2">
              <span class="th-sort" @click="toggleSort('name')">名称<span class="th-arrow">{{ sortArrow('name') }}</span></span>
            </th>
            <th class="t-left pp-stk-l3">
              <span class="th-sort" @click="toggleSort('api')">编码<span class="th-arrow">{{ sortArrow('api') }}</span></span>
            </th>
            <th class="t-center t-left"><span class="th-sort" @click="toggleSort('physical_table')">物理表<span class="th-arrow">{{ sortArrow('physical_table') }}</span></span></th>
            <th class="t-center t-left"><span class="th-sort" @click="toggleSort('physical_column')">物理字段<span class="th-arrow">{{ sortArrow('physical_column') }}</span></span></th>
            <th class="t-center"><span class="th-sort" @click="toggleSort('data_type')">数据类型<span class="th-arrow">{{ sortArrow('data_type') }}</span></span></th>
            <th class="t-center">值类型</th>
            <th class="t-center">格式化</th>
            <th class="t-center"><span class="th-sort" @click="toggleSort('is_key')">主键<span class="th-arrow">{{ sortArrow('is_key') }}</span></span></th>
            <th class="t-center"><span class="th-sort" @click="toggleSort('is_required')">必填<span class="th-arrow">{{ sortArrow('is_required') }}</span></span></th>
            <th class="t-center"><span class="th-sort" @click="toggleSort('is_multi_valued_prop')">多值<span class="th-arrow">{{ sortArrow('is_multi_valued_prop') }}</span></span></th>
            <th class="t-center" title="值域约束"><span class="th-sort" @click="toggleSort('is_range_constraint_prop')">约束<span class="th-arrow">{{ sortArrow('is_range_constraint_prop') }}</span></span></th>
            <th class="t-center"><span class="th-sort" @click="toggleSort('status')">状态<span class="th-arrow">{{ sortArrow('status') }}</span></span></th>
            <th class="t-center t-left">备注</th>
            <th class="t-center pp-stk-r1" style="width:80px">操作</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="(p, idx) in displayRows" :key="p.id">
            <tr :class="['pp-row', selectedId===p.id && 'is-selected']"
                :draggable="draggable"
                @dragstart="onDragStart(idx, $event)"
                @dragover.prevent="onDragOver(idx)"
                @dragend="onDragEnd"
                @click="onRowClick(p)">
              <td class="t-center pp-stk-l1" @click.stop><input type="checkbox" :checked="checked.has(p.id)" @change="toggleCheck(p.id)" /></td>
              <td class="pp-stk-l2 t-left">
                <div class="pp-name-cell">
                  <span class="pp-ic" :style="{ background: propTypeColor(p) }" v-html="BL.icon(propTypeIcon(p), 11, '#fff')"></span>
                  <span class="bl-truncate" :title="p.display_name || p.rdfs_label">{{ p.display_name || p.rdfs_label || '—' }}</span>
                </div>
              </td>
              <td class="pp-stk-l3 t-left">
                <span class="bl-mono bl-truncate" :title="p.api_name">{{ p.api_name }}</span>
              </td>
              <td class="t-left">
                <span class="bl-mono bl-muted bl-truncate" :title="p.physical_table">{{ p.physical_table || '—' }}</span>
              </td>
              <td class="t-left">
                <span class="bl-mono bl-muted bl-truncate" :title="p.physical_column">{{ p.physical_column || '—' }}</span>
              </td>
              <td class="t-center"><span class="bl-tag">{{ p.data_type || '—' }}</span></td>
              <!-- 值类型: 点击单元格 → 唤起「值类型选择面板」 -->
              <td class="t-center vt-cell" @click.stop="openValueTypePicker(p)">
                <span :class="['vt-pill', p.value_type && 'is-on']"
                      :title="p.value_type ? `${valueTypeLabel(p.value_type)} · 点击修改` : '未设置 · 点击选择'">
                  <span v-html="BL.icon('tag', 11)"></span>
                  <span class="bl-truncate" style="margin-left:4px;max-width:100px">{{ valueTypeLabel(p.value_type) }}</span>
                </span>
              </td>
              <!-- 格式化 -->
              <td class="t-center" @click.stop="openFormat(p)">
                <span :class="['fmt-pill', propFormatMap[p.id]?.format_enabled && 'is-on']" :title="propFormatMap[p.id]?.format_enabled ? '已配置 - 点击编辑' : '未配置 - 点击设置'">
                  <span v-html="BL.icon('sliders', 11)"></span>
                  <span style="margin-left:4px">{{ formatPreview(p) }}</span>
                </span>
              </td>
              <!-- 主键 / 必填 / 多值 / 约束: 只读复选框 -->
              <td class="t-center"><input type="checkbox" :checked="p.is_key" disabled /></td>
              <td class="t-center"><input type="checkbox" :checked="p.is_required" disabled /></td>
              <td class="t-center"><input type="checkbox" :checked="p.is_multi_valued_prop" disabled /></td>
              <td class="t-center" title="值域约束"><input type="checkbox" :checked="p.is_range_constraint_prop" disabled /></td>
              <td class="t-center">
                <span :class="['pp-status', p.status===1 ? 'is-on' : 'is-off']">{{ p.status===1 ? '启用' : '禁用' }}</span>
              </td>
              <!-- 备注 -->
              <td class="t-left">
                <span class="bl-muted bl-truncate" :title="p.rdfs_comment">{{ p.rdfs_comment || '—' }}</span>
              </td>
              <td class="t-center pp-stk-r1" @click.stop>
                <div class="bl-row" style="gap:0;justify-content:center">
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="openEditDrawer(p)" v-html="BL.icon('edit', 12)"></button>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="onSingleDelete(p)" v-html="BL.icon('trash', 12)"></button>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
      <div v-if="!displayRows.length" class="bl-empty" style="padding:32px">暂无属性，点击「新增属性」添加</div>
    </div>
    </template>

    <!-- 属性格式化弹窗 (单条 / 批量共用) -->
    <PropertyFormatModal v-model:open="fmtOpen"
                         :property-id="fmtPropertyId"
                         :property-ids="fmtPropertyIds"
                         property-scope="class"
                         :data-type="fmtDataType"
                         @saved="onFormatSaved" />

    <!-- 属性详情抽屉 (右侧, spec §1.1.1.1.1.1.1.6) -->
    <PropertyDetailDrawer v-model:open="drawerOpen"
                          :class-id="classId"
                          :class-name="className"
                          :property="drawerProperty"
                          :datasources="datasources"
                          :sibling-props="rows"
                          @saved="onDrawerSaved"
                          @navigate-to-tab="$emit('navigate-tab', $event)" />

    <!-- 值类型选择面板 (居中模态,点击属性行「值类型」单元格唤起) -->
    <ValueTypePickerModal v-model:open="vtpOpen"
                          v-model="vtpSelected"
                          :multi="false"
                          :required="false"
                          :subtitle="vtpSubtitle"
                          @confirm="onValueTypeConfirm" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { classMetaApi, valueTypeApi, propertyFormatApi, resourceApi } from '@/api'
import PropertyFormatModal from '@/components/PropertyFormatModal.vue'
import ValueTypePickerModal from '@/components/ValueTypePickerModal.vue'
import TabPropsCanvas from './TabPropsCanvasX6.vue'
import PropertyDetailDrawer from './PropertyDetailDrawer.vue'

const props = defineProps({ classId: { type: String, default: '' }, className: { type: String, default: '' } })
defineEmits(['navigate-tab'])

/* 子页签: 属性列表 / 关系图 — spec 1.1.1.1.1.1.1.3 */
const subTab = ref('list')

const rows = ref([])
const checked = ref(new Set())
const selectedId = ref(null)   // 单击行选中 (蓝色背景)
const q = ref('')
const filterType = ref('')
const filterTable = ref('')    // 所属物理表 filter
const filterStatus = ref('')
const addMenu = ref(false)
const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']

/* 表头排序: 用户便捷视图排序 (与拖拽排序互斥) */
const sortKey = ref('')
const sortDir = ref('')
function toggleSort(key) {
  if (sortKey.value !== key) { sortKey.value = key; sortDir.value = 'asc' }
  else if (sortDir.value === 'asc') sortDir.value = 'desc'
  else { sortKey.value = ''; sortDir.value = '' }
}
function sortArrow(key) {
  if (sortKey.value !== key) return ' ⇅'
  return sortDir.value === 'asc' ? ' ↑' : ' ↓'
}

/* 所属物理表候选 (从当前行的 physical_table 实际值动态推算) */
const tableOpts = computed(() => {
  const set = new Set()
  rows.value.forEach(r => { if (r.physical_table) set.add(r.physical_table) })
  return [...set].map(t => ({ value: t, label: t }))
})

/* 行点击: 选中 + 打开右侧详情抽屉 (spec §1.1.1.1.1.1.1.4 三(2)) */
function onRowClick(p) {
  selectedId.value = p.id
  openEditDrawer(p)
}

const datasources = ref([])
async function load() {
  if (!props.classId) { rows.value = []; datasources.value = []; return }
  const list = await classMetaApi.listProps(props.classId).catch(() => [])
  rows.value = list || []
  checked.value = new Set()
  await loadValueTypeOptions()
  await loadPropFormats()
  // 加载该类挂接的数据集 (供详情抽屉 表映射 panel 使用)
  try {
    const detail = await resourceApi.classDetail(props.classId)
    datasources.value = detail?.classDatasources || []
  } catch { datasources.value = [] }
}

/* —— 属性详情抽屉 —— */
const drawerOpen = ref(false)
const drawerProperty = ref(null)  // null = 新建; object = 编辑
function openEditDrawer(p) {
  drawerProperty.value = { ...p }
  drawerOpen.value = true
}
function openCreateDrawer(propType, derived = false, virtual = false) {
  drawerProperty.value = {
    id: '',
    api_name: '', display_name: '', rdfs_label: '', rdfs_comment: '',
    prop_type: propType,
    data_type: propType === 'data' ? 'xsd:string' : '',
    is_required: 0, is_key: 0, is_derived: (derived || virtual) ? 1 : 0,
    is_multi_valued_prop: 0, is_range_constraint_prop: 0,
    physical_table: '', physical_column: '', class_ds_id: '',
    status: 1
  }
  drawerOpen.value = true
}
async function onDrawerSaved() {
  await load()
}

/* 值类型下拉 */
const valueTypeOptions = ref([])
async function loadValueTypeOptions() {
  if (valueTypeOptions.value.length) return
  valueTypeOptions.value = (await valueTypeApi.list().catch(() => [])).filter(v => v.status === 1)
}
function valueTypeLabel(id) {
  if (!id) return '—'
  const v = valueTypeOptions.value.find(x => x.id === id)
  return v ? (v.rdfs_label || v.api_name) : id
}

/* 属性格式化批量加载 */
const propFormatMap = ref({})
async function loadPropFormats() {
  const ids = rows.value.filter(p => !p._isNew).map(p => p.id)
  if (!ids.length) { propFormatMap.value = {}; return }
  try {
    const list = await propertyFormatApi.byProperties(ids)
    const m = {}; (list || []).forEach(f => { m[f.property_id] = f })
    propFormatMap.value = m
  } catch { propFormatMap.value = {} }
}
function formatPreview(p) {
  const f = propFormatMap.value[p.id]
  if (!f || !f.format_enabled) return '未设置'
  return ({ general:'常规', number:'数值', currency:'货币', accounting:'会计', date:'日期', time:'时间', percent:'百分比', fraction:'分数', scientific:'科学记数', text:'文本', special:'特殊', custom:'自定义' })[f.format_type] || f.format_type
}

/* 属性格式化弹窗 (单条) */
const fmtOpen = ref(false)
const fmtPropertyId = ref('')
const fmtPropertyIds = ref([])
const fmtDataType = ref('')
function openFormat(p) {
  if (p._isNew) { BL.warning('请先保存属性后再设置格式化'); return }
  fmtPropertyId.value = p.id
  fmtPropertyIds.value = []
  fmtDataType.value = p.data_type || ''
  fmtOpen.value = true
}
/* 批量格式化: 选中行中已保存的部分 */
function openBatchFormat() {
  const ids = rows.value.filter(p => checked.value.has(p.id) && !p._isNew).map(p => p.id)
  if (!ids.length) { BL.warning('选中的属性均为未保存的新增项,无法批量格式化'); return }
  // 选取一个代表性 data_type 用于分类过滤 (取众数,简化处理用第一项)
  const first = rows.value.find(p => p.id === ids[0])
  fmtPropertyId.value = ''
  fmtPropertyIds.value = ids
  fmtDataType.value = first?.data_type || ''
  fmtOpen.value = true
}
async function onFormatSaved() { await loadPropFormats() }

/* —— 值类型选择面板 —— */
const vtpOpen = ref(false)
const vtpSelected = ref('')           // 单选: 当前属性的 value_type id
const vtpSubtitle = ref('')
const vtpPropertyRef = ref(null)      // 触发选择的属性行 (用于确认后回写)
function openValueTypePicker(p) {
  if (p._isNew) { BL.warning('请先保存属性后再选择值类型'); return }
  vtpPropertyRef.value = p
  vtpSelected.value = p.value_type || ''
  vtpSubtitle.value = `为「${p.display_name || p.rdfs_label || p.api_name}」选择值类型`
  vtpOpen.value = true
}
async function onValueTypeConfirm({ ids, rows: picked }) {
  const p = vtpPropertyRef.value
  if (!p) { vtpOpen.value = false; return }
  const newId = Array.isArray(ids) ? ids[0] : ids
  if (newId === p.value_type) { vtpOpen.value = false; return }
  const pickedRow = (picked && picked[0]) || valueTypeOptions.value.find(v => v.id === newId)
  // 同步 data_type:值类型自带 data_type 时优先采用,保证表格「数据类型」列同步刷新
  const patch = { value_type: newId || null }
  if (pickedRow?.data_type) patch.data_type = pickedRow.data_type
  // 发送完整记录,避免后端全量UPDATE时丢失其他字段(physical_table/physical_column等)
  const payload = { ...p, ...patch }
  try {
    await classMetaApi.updateProp(p.id, payload)
    BL.success('值类型已更新')
    vtpOpen.value = false
    // 先把已加载的值类型选项纳入缓存,避免标签短暂回退到 id
    if (pickedRow && !valueTypeOptions.value.some(v => v.id === pickedRow.id)) {
      valueTypeOptions.value = [...valueTypeOptions.value, pickedRow]
    }
    await load()
  } catch (e) {
    BL.error(e?.msg || '保存失败')
  }
}

watch(() => props.classId, load, { immediate: true })

/* 拖拽排序激活条件: 无搜索 / 无筛选 / 无表头排序时启用 */
const draggable = computed(() => !q.value && !filterType.value && !filterTable.value && !filterStatus.value && !sortKey.value)

const displayRows = computed(() => {
  let list = rows.value
  if (filterType.value) list = list.filter(r => r.prop_type === filterType.value)
  if (filterTable.value) list = list.filter(r => r.physical_table === filterTable.value)
  if (filterStatus.value !== '') list = list.filter(r => String(r.status) === filterStatus.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.api_name, r.display_name, r.rdfs_label, r.prop_code].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  if (sortKey.value && sortDir.value) {
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      let va, vb
      switch (sortKey.value) {
        case 'name':   va = a.display_name || a.rdfs_label || ''; vb = b.display_name || b.rdfs_label || ''; break
        case 'api':    va = a.api_name || ''; vb = b.api_name || ''; break
        case 'data_type': va = a.data_type || ''; vb = b.data_type || ''; break
        case 'physical_table':  va = a.physical_table || ''; vb = b.physical_table || ''; break
        case 'physical_column': va = a.physical_column || ''; vb = b.physical_column || ''; break
        case 'status': va = a.status ?? 9; vb = b.status ?? 9; break
        case 'is_key':
        case 'is_required':
        case 'is_multi_valued_prop':
        case 'is_range_constraint_prop':
          va = a[sortKey.value] ? 1 : 0
          vb = b[sortKey.value] ? 1 : 0
          break
        default: va = ''; vb = ''
      }
      if (typeof va === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }
  return list
})

const allChecked = computed(() => displayRows.value.filter(r => !r._isNew).length > 0
  && displayRows.value.filter(r => !r._isNew).every(r => checked.value.has(r.id)))

function toggleCheck(id) {
  const s = new Set(checked.value)
  s.has(id) ? s.delete(id) : s.add(id); checked.value = s
}
function toggleAll() {
  const s = new Set(checked.value)
  const selectable = displayRows.value.filter(r => !r._isNew)
  if (allChecked.value) selectable.forEach(r => s.delete(r.id))
  else selectable.forEach(r => s.add(r.id))
  checked.value = s
}

function propTypeIcon(p) {
  if (p.is_derived) return 'zap'
  if (p.prop_type === 'object') return 'link'
  if (p.prop_type === 'annotation') return 'chat'
  return 'database'
}
function propTypeColor(p) {
  if (p.is_derived) return '#722ED1'
  if (p.prop_type === 'object') return '#FF7D00'
  if (p.prop_type === 'annotation') return '#00B42A'
  return '#1677ff'
}

/* 新增统一改为打开抽屉 (spec §1.1.1.1.1.1.1.4 三(1)) */
function addRow(propType, derived = false, virtual = false) {
  openCreateDrawer(propType, derived, virtual)
}
async function onBatchDelete() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title:'删除属性', content:`确定删除选中 ${ids.length} 个属性？`, danger:true, okText:'删除' })
  if (!ok) return
  for (const id of ids) await classMetaApi.removeProp(id).catch(() => {})
  BL.success('已删除')
  await load()
}
async function onSingleDelete(p) {
  const ok = await BL.confirm({ title:'删除属性', content:`确定删除「${p.display_name || p.api_name}」？`, danger:true, okText:'删除' })
  if (!ok) return
  await classMetaApi.removeProp(p.id).catch(() => {})
  BL.success('已删除')
  await load()
}

/* —— 拖拽排序 (仅在无搜索/筛选/排序时启用) —— */
const dragFromIdx = ref(-1)
function onDragStart(idx, ev) {
  if (!draggable.value) return
  dragFromIdx.value = idx
  ev.dataTransfer.effectAllowed = 'move'
}
function onDragOver(idx) {
  if (!draggable.value || dragFromIdx.value < 0 || dragFromIdx.value === idx) return
  // 即时调整 rows 顺序 (视觉跟随)
  const list = [...rows.value]
  const [item] = list.splice(dragFromIdx.value, 1)
  list.splice(idx, 0, item)
  rows.value = list
  dragFromIdx.value = idx
}
async function onDragEnd() {
  if (dragFromIdx.value < 0) return
  dragFromIdx.value = -1
  // 持久化 sort 顺序
  const ids = rows.value.filter(r => !r._isNew).map(r => r.id)
  try {
    await classMetaApi.reorderProps(props.classId, ids)
    BL.success('排序已保存')
  } catch (e) { BL.error(e?.msg || '排序保存失败') }
}

// 点击页面其他位置关闭新增菜单
document.addEventListener('click', () => { addMenu.value = false })
</script>

<style scoped>
.tab-props {
  display: flex; flex-direction: column; gap: 8px;
  height: 100%; min-height: 500px;
}

/* 子页签: 属性列表 / 关系图 (紧凑型,降低占高) */
.pp-subtabs {
  display: flex; gap: 2px;
  border-bottom: 1px solid var(--bl-divider);
  height: 30px;
}
.pp-stab {
  padding: 0 14px; height: 30px;
  border: 0; background: transparent; cursor: pointer;
  font-size: 12.5px; color: var(--bl-text-2);
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  display: inline-flex; align-items: center;
  line-height: 1;
}
.pp-stab:hover { color: var(--bl-text-1); }
.pp-stab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
/* 关系图画布: 占满子页签下方的全部剩余空间 */
.pp-canvas {
  flex: 1; min-height: 0;
  border: 1px solid var(--bl-divider); border-radius: 6px;
  overflow: hidden;
}

/* === (1) 顶部固定操作栏: 白色卡片 + 圆角 + 阴影 (spec §一(2)) === */
.pp-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  gap: 12px; flex-wrap: wrap;
  padding: 6px 0px;
  /* background: var(--bl-bg-1); border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05); */
}
.pp-filters { display: inline-flex; gap: 8px; align-items: center; }
.pp-filters .bl-input { height: 32px; width: 140px; }
.pp-search { position: relative; width: 200px; }
.pp-search-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.pp-search .bl-input { padding-left: 26px; width: 100%; }
.pp-actions { display: inline-flex; gap: 8px; align-items: center; }
.pp-add-wrap { position: relative; }

/* 按钮区: 批量格式化 / 删除 (仅选中后显示) / 新增属性 */
.pp-batch-fmt { background: var(--bl-bg-1); border: 1px solid var(--bl-border); color: var(--bl-text-1); }
.pp-batch-fmt:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
/* 危险按钮: 白底红框红字 (per 截图) */
.pp-del-btn {
  background: var(--bl-bg-1);
  border: 1px solid #f53f3f;
  color: #f53f3f;
}
.pp-del-btn:hover { background: #fff1f0; }
/* 新增属性: 蓝色填充 + 下拉 chevron */
.pp-add-btn { background: #1677ff; border: 1px solid #1677ff; color: #fff; display: inline-flex; align-items: center; }
.pp-add-btn:hover { background: #0e63d6; border-color: #0e63d6; }

.pp-add-menu {
  position: absolute; top: 38px; right: 0; min-width: 240px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  box-shadow: var(--bl-shadow-2); padding: 4px; z-index: 10;
}
.pp-add-item { display: flex; gap: 10px; padding: 8px 10px; border-radius: 4px; cursor: pointer; font-size: 13px; }
.pp-add-item:hover { background: var(--bl-bg-hover); }
.pp-add-item .bl-muted { font-size: 11px; }
.pp-add-dot { width: 22px; height: 22px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }

/* === (2) 工具栏底部说明区 (spec §一(3)) === */
.pp-legend {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: #999;
  padding: 0 4px;
}
.pp-legend-l { display: inline-flex; gap: 20px; align-items: center; }
.pp-legend-r { color: #999; }
.lg-dot { width: 14px; height: 14px; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; vertical-align: middle; margin-right: 6px; }

/* === (3) 表格展示区 (spec §一(3) + §五) === */
.pp-table-wrap {
  flex: 1; min-height: 0;
  background: var(--bl-bg-1); border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  overflow: auto;
  position: relative;
}
.pp-table {
  width: 100%; min-width: 1500px;
  border-collapse: separate; border-spacing: 0;
  table-layout: fixed; /* 严格按 colgroup 分配宽度,避免列错位 */
  /* 蓝调表头:跟随主题色, 深浅都成立(原来写死 #e8f3ff 深色下浅底浅字看不见) */
  --pp-th-bg: color-mix(in srgb, var(--bl-primary) 10%, var(--bl-bg-1));
  --pp-th-bg-hover: color-mix(in srgb, var(--bl-primary) 18%, var(--bl-bg-1));
  --pp-th-line: color-mix(in srgb, var(--bl-primary) 24%, var(--bl-border));
}
/* 合并图标 + 名称 单元格 */
.pp-name-cell { display: flex; align-items: center; gap: 8px; min-width: 0; }
.pp-name-cell .bl-truncate { flex: 1; min-width: 0; }
/* 表头 */
.pp-table thead th {
  position: sticky; top: 0; z-index: 3;
  background: var(--pp-th-bg);
  font-weight: 600; padding: 0 8px;
  font-size: 12px; height: 36px;
  color: var(--bl-text-1);
  text-align: center;
  white-space: nowrap;
  border-bottom: 1px solid var(--pp-th-line);
}
.pp-table thead th:hover { background: var(--pp-th-bg-hover); }
.pp-table thead th.t-left { text-align: left; }
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; white-space: nowrap; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }

/* 表体 */
.pp-table tbody tr { background: var(--bl-bg-1); }
.pp-table tbody tr:nth-child(even) { background: var(--bl-bg-2); }
.pp-table tbody tr:hover { background: var(--bl-bg-2); }
.pp-table tbody tr.is-selected { background: color-mix(in srgb, var(--bl-primary) 12%, var(--bl-bg-1)) !important; }
.pp-table tbody tr.is-editing { background: var(--bl-primary-soft) !important; }
.pp-table td { padding: 0 8px; font-size: 12px; height: 36px; vertical-align: middle; }
.pp-table td.t-left { text-align: left; }
.pp-table td.t-center { text-align: center; }
.pp-table td .bl-truncate { display: inline-block; max-width: 100%; vertical-align: middle; }

.pp-ic { width: 22px; height: 22px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; }
.pp-table .bl-input.bl-input-xs { height: 26px; padding: 0 6px; font-size: 12px; min-width: 60px; width: 100%; }

/* 状态标签 (spec §五(2)) */
.pp-status {
  display: inline-block; padding: 2px 10px; border-radius: 10px;
  font-size: 11px; font-weight: 500;
}
.pp-status.is-on  { background: color-mix(in srgb, var(--bl-success) 14%, var(--bl-bg-1)); color: #00b42a; }
.pp-status.is-off { background: var(--bl-bg-2); color: #666; }

/* 锁定列: 左侧 3 列 (复选框 / 名称+图标 / 编码) + 右侧 1 列 (操作) */
.pp-stk-l1, .pp-stk-l2, .pp-stk-l3 {
  position: sticky; background: inherit; z-index: 2;
}
.pp-stk-l1 { left: 0; }
.pp-stk-l2 { left: 40px; }
.pp-stk-l3 { left: 210px; box-shadow: 2px 0 4px -2px rgba(0,0,0,0.1); }
.pp-stk-r1 { position: sticky; right: 0; background: inherit; z-index: 2; box-shadow: -2px 0 4px -2px rgba(0,0,0,0.1); }
/* 锁定列在 hover / selected 时背景跟随父行 */
.pp-table tbody tr:hover .pp-stk-l1,
.pp-table tbody tr:hover .pp-stk-l2,
.pp-table tbody tr:hover .pp-stk-l3,
.pp-table tbody tr:hover .pp-stk-r1 { background: var(--bl-bg-2); }
.pp-table tbody tr:nth-child(even) .pp-stk-l1,
.pp-table tbody tr:nth-child(even) .pp-stk-l2,
.pp-table tbody tr:nth-child(even) .pp-stk-l3,
.pp-table tbody tr:nth-child(even) .pp-stk-r1 { background: var(--bl-bg-2); }
.pp-table tbody tr.is-selected .pp-stk-l1,
.pp-table tbody tr.is-selected .pp-stk-l2,
.pp-table tbody tr.is-selected .pp-stk-l3,
.pp-table tbody tr.is-selected .pp-stk-r1 { background: color-mix(in srgb, var(--bl-primary) 12%, var(--bl-bg-1)) !important; }
/* 表头锁定列: 角落 sticky-corner 需要 z-index 高于普通 sticky-top 表头,
   否则横向滚动时非锁定列表头会盖住锁定列表头 */
.pp-table thead th.pp-stk-l1,
.pp-table thead th.pp-stk-l2,
.pp-table thead th.pp-stk-l3,
.pp-table thead th.pp-stk-r1 {
  background: var(--pp-th-bg);
  z-index: 4 !important;  /* 角落: 同时 sticky top + left/right,层级最高 */
}
.pp-table thead th.pp-stk-l3 { box-shadow: 2px 0 4px -2px rgba(0,0,0,0.1), inset 0 -1px 0 var(--pp-th-line); }
.pp-table thead th.pp-stk-r1 { box-shadow: -2px 0 4px -2px rgba(0,0,0,0.1), inset 0 -1px 0 var(--pp-th-line); }

/* 拖拽视觉 */
.pp-table tbody tr[draggable="true"] { cursor: grab; }
.pp-table tbody tr[draggable="true"]:active { cursor: grabbing; }
.fmt-pill {
  display: inline-flex; align-items: center;
  padding: 2px 8px; border-radius: 9px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border: 1px dashed var(--bl-border);
  cursor: pointer; white-space: nowrap;
  transition: background-color .12s, color .12s, border-color .12s;
}
.fmt-pill:hover { color: var(--bl-primary); border-color: var(--bl-primary); }
.fmt-pill.is-on {
  color: var(--bl-primary); background: var(--bl-primary-soft);
  border-style: solid; border-color: var(--bl-primary); font-weight: 500;
}

/* 值类型单元格: 整格可点 — 唤起「值类型选择面板」 */
.vt-cell { cursor: pointer; }
.vt-pill {
  display: inline-flex; align-items: center;
  padding: 2px 8px; border-radius: 9px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border: 1px dashed var(--bl-border);
  max-width: 100%; white-space: nowrap;
  transition: background-color .12s, color .12s, border-color .12s;
}
.vt-pill:hover { color: var(--bl-primary); border-color: var(--bl-primary); }
.vt-pill.is-on {
  color: var(--bl-primary); background: var(--bl-primary-soft);
  border-style: solid; border-color: var(--bl-primary); font-weight: 500;
}
</style>
