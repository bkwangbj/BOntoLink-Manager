<template>
  <div class="tab-props">
    <!-- 顶部工具栏 -->
    <div class="pp-toolbar">
      <div class="pp-filters">
        <select class="bl-input" v-model="filterType">
          <option value="">全部属性类型</option>
          <option value="data">数据属性</option>
          <option value="object">对象属性</option>
          <option value="annotation">注释属性</option>
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
        <button v-if="checked.size" class="bl-btn bl-btn-sm" @click="openBatchFormat">
          <span v-html="BL.icon('sliders', 12)"></span><span style="margin-left:4px">批量格式化 ({{ checked.size }})</span>
        </button>
        <button v-if="checked.size" class="bl-btn bl-btn-sm bl-btn-danger" @click="onBatchDelete">
          <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除 ({{ checked.size }})</span>
        </button>
        <div class="pp-add-wrap" @click.stop>
          <button class="bl-btn bl-btn-sm bl-btn-primary" @click="addMenu = !addMenu">
            <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">新增属性</span>
            <span style="margin-left:6px" v-html="BL.icon('chevronDown', 10, '#fff')"></span>
          </button>
          <div v-if="addMenu" class="pp-add-menu">
            <div class="pp-add-item" @click="addRow('data'); addMenu=false">
              <span class="pp-add-dot" style="background:#1677ff" v-html="BL.icon('database',12,'#fff')"></span>
              <div><div>新增数据属性</div><div class="bl-muted">data · 普通字段</div></div>
            </div>
            <div class="pp-add-item" @click="addRow('object'); addMenu=false">
              <span class="pp-add-dot" style="background:#FF7D00" v-html="BL.icon('link',12,'#fff')"></span>
              <div><div>新增对象属性</div><div class="bl-muted">object · 关联另一对象类</div></div>
            </div>
            <div class="pp-add-item" @click="addRow('annotation'); addMenu=false">
              <span class="pp-add-dot" style="background:#00B42A" v-html="BL.icon('chat',12,'#fff')"></span>
              <div><div>新增注释属性</div><div class="bl-muted">annotation · 仅标签说明</div></div>
            </div>
            <div class="pp-add-item" @click="addRow('data', true); addMenu=false">
              <span class="pp-add-dot" style="background:#722ED1" v-html="BL.icon('zap',12,'#fff')"></span>
              <div><div>新增派生字段</div><div class="bl-muted">function · 计算属性</div></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="pp-legend">
      <span><span class="lg-dot" style="background:#1677ff" v-html="BL.icon('database',10,'#fff')"></span>数据</span>
      <span><span class="lg-dot" style="background:#722ED1" v-html="BL.icon('zap',10,'#fff')"></span>计算派生</span>
      <span><span class="lg-dot" style="background:#FF7D00" v-html="BL.icon('link',10,'#fff')"></span>对象</span>
      <span><span class="lg-dot" style="background:#00B42A" v-html="BL.icon('chat',10,'#fff')"></span>注释</span>
      <span class="bl-muted" style="margin-left:auto">双击行进入编辑</span>
    </div>

    <!-- 表格 -->
    <div class="pp-table-wrap">
      <table class="bl-table pp-table">
        <thead>
          <tr>
            <th style="width:34px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
            <th style="width:36px"></th>
            <th>名称</th>
            <th>编码</th>
            <th>类型</th>
            <th>值类型</th>
            <th>格式化</th>
            <th>主键</th>
            <th>必填</th>
            <th>多值</th>
            <th>约束</th>
            <th>物理表</th>
            <th>物理字段</th>
            <th>状态</th>
            <th style="width:88px"></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="p in displayRows" :key="p.id">
            <tr :class="['pp-row', p._editing && 'is-editing']" @dblclick="editRow(p)">
              <td @click.stop><input type="checkbox" :checked="checked.has(p.id)" :disabled="p._isNew" @change="toggleCheck(p.id)" /></td>
              <td><span class="pp-ic" :style="{ background: propTypeColor(p) }" v-html="BL.icon(propTypeIcon(p), 11, '#fff')"></span></td>
              <td>
                <input v-if="p._editing" class="bl-input bl-input-xs" v-model="p.display_name" placeholder="显示名" />
                <span v-else>{{ p.display_name || p.rdfs_label || '—' }}</span>
              </td>
              <td>
                <input v-if="p._editing && p._isNew" class="bl-input bl-input-xs bl-mono" v-model="p.api_name" placeholder="snake_case" />
                <span v-else class="bl-mono">{{ p.api_name }}</span>
              </td>
              <td>
                <select v-if="p._editing" class="bl-input bl-input-xs" v-model="p.data_type">
                  <option value="">—</option>
                  <option v-for="t in xsdTypes" :key="t" :value="t">{{ t }}</option>
                </select>
                <span v-else class="bl-tag">{{ p.data_type || '—' }}</span>
              </td>
              <!-- 值类型 -->
              <td>
                <select v-if="p._editing" class="bl-input bl-input-xs" v-model="p.value_type">
                  <option value="">— 无 —</option>
                  <option v-for="vt in valueTypeOptions" :key="vt.id" :value="vt.id">{{ vt.rdfs_label || vt.api_name }}</option>
                </select>
                <span v-else>{{ valueTypeLabel(p.value_type) }}</span>
              </td>
              <!-- 格式化 -->
              <td @click.stop="openFormat(p)">
                <span :class="['fmt-pill', propFormatMap[p.id]?.format_enabled && 'is-on']" :title="propFormatMap[p.id]?.format_enabled ? '已配置 - 点击编辑' : '未配置 - 点击设置'">
                  <span v-html="BL.icon('sliders', 11)"></span>
                  <span style="margin-left:4px">{{ formatPreview(p) }}</span>
                </span>
              </td>
              <td class="t-center"><input type="checkbox" :checked="p.is_key" :disabled="!p._editing || p.prop_type!=='data'" @change="p.is_key = p.is_key ? 0 : 1" /></td>
              <td class="t-center"><input type="checkbox" :checked="p.is_required" :disabled="!p._editing || p.prop_type!=='data'" @change="p.is_required = p.is_required ? 0 : 1" /></td>
              <td class="t-center"><input type="checkbox" :checked="p.is_multi_valued_prop" :disabled="!p._editing || p.prop_type!=='data'" @change="p.is_multi_valued_prop = p.is_multi_valued_prop ? 0 : 1" /></td>
              <td class="t-center" title="值域约束"><input type="checkbox" :checked="p.is_range_constraint_prop" :disabled="!p._editing || p.prop_type!=='data'" @change="p.is_range_constraint_prop = p.is_range_constraint_prop ? 0 : 1" /></td>
              <td>
                <input v-if="p._editing" class="bl-input bl-input-xs bl-mono" v-model="p.physical_table" />
                <span v-else class="bl-mono bl-muted">{{ p.physical_table || '—' }}</span>
              </td>
              <td>
                <input v-if="p._editing" class="bl-input bl-input-xs bl-mono" v-model="p.physical_column" />
                <span v-else class="bl-mono bl-muted">{{ p.physical_column || '—' }}</span>
              </td>
              <td>
                <span :class="['bl-tag', p.status===1 ? 'bl-tag-success' : 'bl-tag-danger']">{{ p.status===1 ? '启用' : '禁用' }}</span>
              </td>
              <td @click.stop>
                <div v-if="p._editing" class="bl-row" style="gap:0">
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="保存" @click="saveRow(p)" v-html="BL.icon('check', 13)"></button>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="取消" @click="cancelRow(p)" v-html="BL.icon('x', 13)"></button>
                </div>
                <button v-else class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="editRow(p)" v-html="BL.icon('edit', 12)"></button>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
      <div v-if="!displayRows.length" class="bl-empty" style="padding:32px">暂无属性，点击「新增属性」添加</div>
    </div>

    <!-- 属性格式化弹窗 (单条 / 批量共用) -->
    <PropertyFormatModal v-model:open="fmtOpen"
                         :property-id="fmtPropertyId"
                         :property-ids="fmtPropertyIds"
                         property-scope="class"
                         :data-type="fmtDataType"
                         @saved="onFormatSaved" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { classMetaApi, valueTypeApi, propertyFormatApi } from '@/api'
import PropertyFormatModal from '@/components/PropertyFormatModal.vue'

const props = defineProps({ classId: { type: String, default: '' } })

const rows = ref([])
const checked = ref(new Set())
const q = ref('')
const filterType = ref('')
const filterStatus = ref('')
const addMenu = ref(false)
const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']

async function load() {
  if (!props.classId) { rows.value = []; return }
  const list = await classMetaApi.listProps(props.classId).catch(() => [])
  rows.value = (list || []).map(p => ({ ...p, _editing: false, _isNew: false }))
  checked.value = new Set()
  await loadValueTypeOptions()
  await loadPropFormats()
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
watch(() => props.classId, load, { immediate: true })

const displayRows = computed(() => {
  let list = rows.value
  if (filterType.value) list = list.filter(r => r.prop_type === filterType.value)
  if (filterStatus.value !== '') list = list.filter(r => String(r.status) === filterStatus.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.api_name, r.display_name, r.rdfs_label, r.prop_code].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
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

function addRow(propType, derived = false) {
  rows.value.unshift({
    id: 'new_' + Date.now(),
    api_name: '', prop_code: '', display_name: '',
    prop_type: propType, data_type: propType === 'data' ? 'xsd:string' : '',
    is_required: 0, is_primary: 0, is_key: 0, is_derived: derived ? 1 : 0,
    is_multi_valued_prop: 0, is_range_constraint_prop: 0,
    physical_table: '', physical_column: '',
    owl_functional: 0, owl_inverse_functional: 0, owl_transitive: 0,
    owl_symmetric: 0, owl_asymmetric: 0, owl_reflexive: 0, owl_irreflexive: 0,
    status: 1, _editing: true, _isNew: true
  })
}
function editRow(p) {
  if (p._editing) return
  p._backup = JSON.parse(JSON.stringify(p))
  p._editing = true
}
function cancelRow(p) {
  if (p._isNew) {
    rows.value = rows.value.filter(x => x !== p)
  } else {
    if (p._backup) Object.assign(p, p._backup, { _editing: false })
    delete p._backup
  }
}
async function saveRow(p) {
  if (!p.api_name) { BL.warning('编码为必填'); return }
  if (p._isNew && !/^[a-z][a-z0-9_]*$/.test(p.api_name)) { BL.warning('编码需 snake_case'); return }
  const { _editing, _isNew, _backup, ...payload } = p
  try {
    if (p._isNew) {
      delete payload.id
      payload.prop_code = payload.prop_code || payload.api_name
      await classMetaApi.addProp(props.classId, payload)
    } else {
      await classMetaApi.updateProp(p.id, payload)
    }
    BL.success('已保存')
    await load()
  } catch (e) { BL.error(e?.msg || '保存失败') }
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

// 点击页面其他位置关闭新增菜单
document.addEventListener('click', () => { addMenu.value = false })
</script>

<style scoped>
.tab-props { display: flex; flex-direction: column; gap: 8px; }
.pp-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.pp-filters { display: inline-flex; gap: 8px; align-items: center; }
.pp-filters .bl-input { height: 30px; width: 140px; }
.pp-search { position: relative; width: 200px; }
.pp-search-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.pp-search .bl-input { padding-left: 26px; width: 100%; }
.pp-actions { display: inline-flex; gap: 8px; align-items: center; }
.pp-add-wrap { position: relative; }
.pp-add-menu {
  position: absolute; top: 36px; right: 0; min-width: 220px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,.10); padding: 4px; z-index: 10;
}
.pp-add-item { display: flex; gap: 10px; padding: 8px 10px; border-radius: 4px; cursor: pointer; font-size: 13px; }
.pp-add-item:hover { background: var(--bl-bg-hover); }
.pp-add-item .bl-muted { font-size: 11px; }
.pp-add-dot { width: 20px; height: 20px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }

.pp-legend { display: flex; gap: 16px; font-size: 11px; color: var(--bl-text-3); align-items: center; }
.lg-dot { width: 14px; height: 14px; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; vertical-align: middle; margin-right: 4px; }

.pp-table-wrap { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px; overflow: auto; }
.pp-table { width: 100%; min-width: 1100px; }
.pp-table th { background: #e8f3ff; font-weight: 600; padding: 8px 8px; font-size: 12px; }
.pp-table td { padding: 6px 8px; font-size: 12px; }
.pp-table tbody tr:nth-child(even) { background: #fafafa; }
.pp-table tbody tr:hover { background: #f5f7fa; }
.pp-row.is-editing { background: var(--bl-primary-soft) !important; }
.t-center { text-align: center; }
.pp-ic { width: 20px; height: 20px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; }
.pp-table .bl-input.bl-input-xs { height: 26px; padding: 0 6px; font-size: 12px; min-width: 80px; width: 100%; }
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
</style>
