<template>
  <div class="ixe-root">
    <!-- 头部:对象名/类型(标题) + 搜索(含菜单面板) + 保存 -->
    <div class="ixe-head">
      <!-- 从某实例进入:固定标题(不可选择) -->
      <div v-if="fixedTitle" class="ixe-fixed-title" :title="fixedTitle">
        <span class="bl-truncate" style="font-weight:600;max-width:200px">{{ fixedTitle }}</span>
      </div>
      <!-- 否则:对象类型下拉(可选择) -->
      <div v-else class="ixe-type" @click.stop="typeMenu=!typeMenu" v-click-outside="()=>typeMenu=false">
        <span class="ixe-type-ic" :style="{ background:(curType?.color||'#86909c')+'1f', color:curType?.color||'#86909c' }"
              v-html="BL.icon(curType?.icon||'cube', 14, curType?.color||'#86909c')"></span>
        <span class="bl-truncate" style="max-width:160px;font-weight:600">{{ curType?.display_name || '选择对象类型' }}</span>
        <span v-html="BL.icon('chevronDown', 12)"></span>
        <div v-if="typeMenu" class="ixe-type-menu" @click.stop>
          <input class="bl-input bl-input-sm" v-model="typeKw" placeholder="搜索对象类型…" style="margin-bottom:6px" />
          <div class="ixe-type-list">
            <div v-for="t in typeOptions" :key="t.id" class="ixe-type-item" :class="t.id===classId&&'is-on'" @click="selectType(t.id)">
              <span class="ixe-type-ic" :style="{ background:(t.color||'#165DFF')+'1f', color:t.color||'#165DFF' }"
                    v-html="BL.icon(t.icon||'cube', 12, t.color||'#165DFF')"></span>
              <span class="bl-grow bl-truncate">{{ t.display_name }}</span>
              <span class="bl-muted" style="font-size:11px">{{ t.instanceCount }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索栏(内含筛选条件块,依次排列) + 菜单面板 -->
      <div class="ixe-head-search" v-click-outside="()=>searchPanelOpen=false">
        <span class="ixe-hs-ic" v-html="BL.icon('search', 14, 'var(--bl-text-3)')"></span>
        <span v-for="p in pills" :key="p.id" class="ixe-pill" @click.stop="reopenFilter(p, $event)" :title="pillText(p)">
          <span class="bl-truncate">{{ pillText(p) }}</span>
          <button class="ixe-pill-x" @click.stop="removePill(p.id)" v-html="BL.icon('x', 11)"></button>
        </span>
        <input class="ixe-hs-input" v-model="kw" :placeholder="pills.length ? '点击搜索以展开属性筛选面板…' : '搜索属性以添加图表或筛选…'"
               @keyup.enter="reload" @focus="searchPanelOpen=true" />
        <button v-if="pills.length || kw.trim()" class="ixe-hs-clear" @click.stop="clearAll"
                v-html="iconText3('x','清空')"></button>
        <!-- 搜索菜单面板:左 关联对象类型 + 右 属性列表 -->
        <div v-if="searchPanelOpen && classId" class="ixe-sp" @click.stop>
          <aside class="ixe-sp-left">
            <div class="ixe-sp-cur">
              <span class="ixe-type-ic" :style="{ background:(curType?.color||'#165DFF')+'1f', color:curType?.color||'#165DFF' }"
                    v-html="BL.icon(curType?.icon||'cube', 13, curType?.color||'#165DFF')"></span>
              <span class="bl-truncate bl-grow" style="font-weight:600">{{ curType?.display_name }}</span>
            </div>
            <div class="ixe-sp-hd">关联对象类型 <span class="ixe-sp-n">{{ links.length }}</span></div>
            <div class="ixe-sp-scroll">
              <div v-if="!links.length" class="ixe-sp-empty">暂无关联</div>
              <div v-for="l in links" :key="l.linkId+l.targetClassId" class="ixe-sp-link"
                   :title="l.linkLabel" @click="selectTypeFromPanel(l.targetClassId)">
                <span class="ixe-link-ic" :style="{ background:(l.targetColor||'#86909c')+'1f', color:l.targetColor||'#86909c' }"
                      v-html="BL.icon(l.targetIcon||'cube', 12, l.targetColor||'#86909c')"></span>
                <span class="bl-grow bl-truncate">{{ l.targetClassName }}</span>
                <span class="ixe-link-cnt">{{ l.count }}</span>
              </div>
            </div>
          </aside>
          <div class="ixe-sp-right">
            <div class="ixe-sp-hd">属性列表 (Properties) <span class="ixe-sp-n">{{ filteredCols.length }}</span></div>
            <input class="ixe-sp-search" v-model="propKw" placeholder="搜索属性以添加图表或筛选…" />
            <div class="ixe-sp-scroll">
              <div v-for="c in filteredCols" :key="c.field" class="ixe-sp-prop" :class="hasPill(c.field)&&'is-filtered'"
                   @click="openFilterFromPanel(c, $event)">
                <span class="ixe-sp-prop-ic" v-html="BL.icon(typeIcon(c.dataType), 13, 'var(--bl-primary)')"></span>
                <span class="bl-grow bl-truncate">{{ c.label }}</span>
                <span v-if="hasPill(c.field)" v-html="BL.icon('filter', 11, 'var(--bl-primary)')"></span>
              </div>
              <div v-if="!filteredCols.length" class="ixe-sp-empty">无匹配属性</div>
            </div>
          </div>
        </div>

        <!-- 多条件筛选抽屉:放在搜索容器内,与菜单面板并存(点抽屉不收起面板) -->
        <FilterDrawer v-if="filterField" :field="filterField" :options="filterOptions" :model="filterModel" :anchor="filterAnchor"
                      @confirm="onFilterConfirm" @cancel="filterField=null" />
      </div>
    </div>

    <!-- 子头:布局名 + 结果数 + 筛选 pills + 视图切换/清空 -->
    <div class="ixe-subhead" v-if="classId">
      <div class="ixe-layout-sel" @click.stop="layoutMenu=!layoutMenu" v-click-outside="()=>layoutMenu=false">
        <span class="bl-truncate">{{ designName }}</span>
        <span v-html="BL.icon('chevronDown', 11)"></span>
        <div v-if="layoutMenu" class="ixe-layout-menu" @click.stop>
          <div class="ixe-layout-item" :class="!currentDesignId && 'is-on'" @click="resetDesign">默认探索布局</div>
          <template v-if="designsForType.length">
            <div class="ixe-layout-sep"></div>
            <div v-for="d in designsForType" :key="d.id" class="ixe-layout-item" :class="d.id===currentDesignId && 'is-on'"
                 @click="applyDesign(d)">
              <span class="bl-grow bl-truncate">{{ d.name }}</span>
              <button class="ixe-layout-del" title="删除" @click.stop="removeDesign(d)" v-html="BL.icon('trash', 12)"></button>
            </div>
          </template>
          <div class="ixe-layout-sep"></div>
          <div class="ixe-layout-item ixe-layout-new" @click="onSave"
               v-html="iconText2('plus','另存为新设计…')"></div>
        </div>
      </div>
      <span class="ixe-result-badge">{{ total.toLocaleString() }} 条结果</span>
      <span class="bl-grow"></span>
      <div class="ixe-seg">
        <button :class="viewMode==='charts'&&'is-on'" title="图表看板" @click="viewMode='charts'"><span v-html="BL.icon('barChart',13)"></span>看板</button>
        <button :class="viewMode==='list'&&'is-on'" title="结果列表" @click="viewMode='list'"><span v-html="BL.icon('list',13)"></span>列表</button>
      </div>
      <button class="ixe-save ixe-save-sm" @click="onSave" v-html="iconText('save','保存')"></button>
    </div>

    <!-- 主体:图表看板 + 右结果列 -->
    <div class="ixe-main" v-if="classId">
      <!-- 看板模式:图表看板(中) + 结果列(右) -->
      <template v-if="viewMode==='charts'">
        <InstanceCharts ref="chartsRef" class="ixe-dash" :class-id="classId" :type-name="curType?.display_name"
                        :columns="columns" :filter-params="filterParams" />
        <aside class="ixe-results">
          <div class="ixe-results-hd">
            <span class="ixe-results-title">结果</span>
            <span class="ixe-results-cnt">{{ total.toLocaleString() }}</span>
            <span class="bl-grow"></span>
            <span class="ixe-results-sort">排序方式 <span v-html="BL.icon('chevronDown', 11)"></span></span>
          </div>
          <div class="ixe-results-list">
            <div v-for="r in rows" :key="r.id" class="ixe-results-item" @click="$emit('open-instance', { classId, row: r })">
              <span class="ixe-results-ic" :style="{ background:(r.color||'#165DFF')+'1f', color:r.color||'#165DFF' }"
                    v-html="BL.icon(r.icon||'cube', 12, r.color||'#165DFF')"></span>
              <div class="bl-grow" style="min-width:0">
                <div class="bl-truncate" style="font-size:12.5px">{{ r.title }}</div>
                <div class="bl-truncate bl-mono" style="font-size:10.5px;color:var(--bl-text-3)">{{ r.code }}</div>
              </div>
            </div>
            <div v-if="!rows.length" class="bl-empty" style="padding:24px">无匹配实例</div>
          </div>
          <div class="ixe-results-more" @click="viewMode='list'">查看全部结果 →</div>
        </aside>
      </template>

      <!-- 列表模式:全量结果表 -->
      <section v-else class="ixe-right">
        <div class="ixe-table-wrap" v-if="rows.length">
          <table class="bl-table ixe-table">
            <thead>
              <tr>
                <th class="ixe-sticky-col">名称</th>
                <th class="ixe-code-col">编码</th>
                <th v-for="c in columns" :key="c.field" @click="sortBy(c.field)">
                  {{ c.label }}<span v-if="sort===c.field" style="color:var(--bl-primary);font-size:10px">{{ order==='desc'?' ▼':' ▲' }}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.id" @click="$emit('open-instance', { classId, row: r })">
                <td class="ixe-sticky-col"><span class="bl-truncate" style="font-weight:500">{{ r.title }}</span></td>
                <td class="ixe-code-col bl-mono bl-muted">{{ r.code }}</td>
                <td v-for="c in columns" :key="c.field">{{ fmt(r[c.field], c.dataType) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="bl-empty" style="padding:48px">无匹配实例</div>
        <div class="ixe-pager">
          <button class="bl-btn bl-btn-sm" :disabled="page<=1" @click="go(page-1)" v-html="BL.icon('chevronLeft',13)"></button>
          <span class="bl-muted" style="font-size:12px">{{ (page-1)*size+1 }}–{{ Math.min(page*size,total) }} / {{ total.toLocaleString() }}</span>
          <button class="bl-btn bl-btn-sm" :disabled="page>=totalPages" @click="go(page+1)" v-html="BL.icon('chevronRight',13)"></button>
        </div>
      </section>
    </div>
    <div v-else class="ixe-pick bl-empty">从左上角下拉选择一个对象类型开始探索</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { instanceApi } from '@/api'
import FilterDrawer from './FilterDrawer.vue'
import InstanceCharts from './InstanceCharts.vue'
import { useDesigns } from './designs.js'

const props = defineProps({
  types: { type: Array, default: () => [] },
  initialClassId: { type: String, default: '' },
  fixedTitle: { type: String, default: '' }   // 从某实例进入时的固定标题(对象类型不可选)
})
defineEmits(['open-instance'])

const viewMode = ref('charts')   // charts(看板) | list(全量表)
const searchPanelOpen = ref(false)
const propKw = ref('')
const layoutMenu = ref(false)
const designName = ref('默认探索布局')
const chartsRef = ref(null)
function iconText(ic, t) { return `${BL.icon(ic, 13, '#fff')}<span style="margin-left:5px">${t}</span>` }
function iconText2(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:5px">${t}</span>` }
function iconText3(ic, t) { return `${BL.icon(ic, 12, 'var(--bl-text-3)')}<span style="margin-left:3px">${t}</span>` }

/* —— 保存设计 / 模版(localStorage) —— */
const { listFor, save: saveDesign, remove: removeDesignStore } = useDesigns()
const currentDesignId = ref(null)
const designsForType = computed(() => listFor(classId.value))

async function onSave() {
  layoutMenu.value = false
  const name = await BL.prompt({
    title: '保存为探索设计',
    label: '设计名称',
    defaultValue: designName.value === '默认探索布局' ? '' : designName.value,
    placeholder: `如「${curType.value?.display_name || ''}高级筛选」`,
    validate: (v) => (v && v.trim() ? true : '请输入名称')
  })
  if (!name) return
  const d = saveDesign({
    name: name.trim(),
    classId: classId.value,
    className: curType.value?.display_name || '',
    icon: curType.value?.icon || 'search',
    color: curType.value?.color || '#165DFF',
    fixedTitle: props.fixedTitle || '',
    kw: kw.value,
    pills: JSON.parse(JSON.stringify(pills.value)),
    sort: sort.value, order: order.value,
    viewMode: viewMode.value,
    charts: chartsRef.value ? chartsRef.value.getConfig() : []
  })
  currentDesignId.value = d.id
  designName.value = d.name
  BL.success(`已保存设计「${d.name}」`)
}
function applyDesign(d) {
  layoutMenu.value = false
  currentDesignId.value = d.id
  designName.value = d.name
  kw.value = d.kw || ''
  sort.value = d.sort || ''
  order.value = d.order || 'asc'
  viewMode.value = d.viewMode || 'charts'
  // pills 补回唯一 id(老数据可能无 id)
  pills.value = (d.pills || []).map(p => ({ ...p, id: p.id != null ? p.id : ++pidSeq }))
  page.value = 1
  reload()
  // 图表需等看板渲染后注入
  nextTick(() => { if (chartsRef.value) chartsRef.value.setConfig(d.charts || []) })
}
function resetDesign() {
  layoutMenu.value = false
  currentDesignId.value = null
  designName.value = '默认探索布局'
  pills.value = []; kw.value = ''; sort.value = ''; page.value = 1
  if (chartsRef.value) chartsRef.value.setConfig([])
  reload()
}
async function removeDesign(d) {
  const ok = await BL.confirm({ title: '删除设计', content: `确定删除「${d.name}」?`, danger: true, okText: '删除' })
  if (!ok) return
  removeDesignStore(d.id)
  if (currentDesignId.value === d.id) resetDesign()
  BL.success('已删除')
}

/* —— 状态 —— */
const classId = ref(props.initialClassId || '')
const typeMenu = ref(false)
const typeKw = ref('')
const columns = ref([])
const links = ref([])
const pills = ref([])          // [{ field, label, dataType, logic, conditions:[...] }]
const kw = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(25)
const sort = ref('')
const order = ref('asc')

const curType = computed(() => props.types.find(t => t.id === classId.value))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const typeOptions = computed(() => {
  const q = typeKw.value.trim().toLowerCase()
  return props.types.filter(t => !q || (t.display_name || '').toLowerCase().includes(q) || (t.api_name || '').toLowerCase().includes(q))
})
/* 搜索菜单面板:属性列表(按关键词过滤) */
const filteredCols = computed(() => {
  const q = propKw.value.trim().toLowerCase()
  if (!q) return columns.value
  return columns.value.filter(c => (c.label || '').toLowerCase().includes(q) || (c.field || '').toLowerCase().includes(q))
})
function selectTypeFromPanel(id) { selectType(id); propKw.value = '' }
function openFilterFromPanel(c, e) { editingPillId.value = null; openFilter(c, e) }   // 新增独立条件块,保持面板展开

/* —— 筛选抽屉 —— */
const filterField = ref(null)
const filterOptions = ref([])
const filterModel = ref(null)
const filterAnchor = ref(null)
let pidSeq = 0
const editingPillId = ref(null)   // 非空=编辑某条件块,空=新增

/* —— 类型/数据加载 —— */
function selectType(id) {
  if (classId.value === id) { typeMenu.value = false; return }
  classId.value = id
  typeMenu.value = false
  pills.value = []
  kw.value = ''
  sort.value = ''
  page.value = 1
  currentDesignId.value = null
  designName.value = '默认探索布局'
  if (chartsRef.value) chartsRef.value.setConfig([])
  loadMeta()
}

async function loadMeta() {
  if (!classId.value) return
  try {
    const [cols, lk] = await Promise.all([
      instanceApi.columns(classId.value),
      instanceApi.links(classId.value)
    ])
    columns.value = cols || []
    links.value = lk || []
  } catch { columns.value = []; links.value = [] }
  reload()
}

/* 当前 q + filter(图表与列表共用，保证图表反映筛选) */
const filterParams = computed(() => {
  const p = {}
  if (kw.value.trim()) p.q = kw.value.trim()
  if (pills.value.length) {
    p.filter = JSON.stringify({
      logic: 'AND',
      conditions: pills.value.map(x => ({ logic: x.logic, conditions: x.conditions }))
    })
  }
  return p
})

async function reload() {
  if (!classId.value) return
  const params = { classId: classId.value, page: page.value, size: size.value, ...filterParams.value }
  if (sort.value) { params.sort = sort.value; params.order = order.value }
  try {
    const res = await instanceApi.list(params)
    rows.value = res.rows || []
    total.value = res.total || 0
    if (res.columns) columns.value = res.columns
  } catch { rows.value = []; total.value = 0 }
}

function go(p) { if (p >= 1 && p <= totalPages.value) { page.value = p; reload() } }
function sortBy(f) {
  if (sort.value === f) order.value = order.value === 'desc' ? 'asc' : 'desc'
  else { sort.value = f; order.value = 'asc' }
  page.value = 1; reload()
}

/* —— 筛选抽屉交互 —— */
async function openFilter(col, ev) {
  const rect = ev.currentTarget.getBoundingClientRect()
  // 出现在所点触发元素的正下方
  filterAnchor.value = { left: Math.max(8, Math.min(rect.left, window.innerWidth - 580)), top: rect.bottom + 6 }
  filterField.value = { field: col.field, label: col.label, dataType: detectType(col) }
  // 从面板点属性 = 新增独立条件块(model 为空);编辑已有块由 reopenFilter 设置 editingId/model
  filterModel.value = null
  filterOptions.value = []
  // 字符串/枚举：拉取去重值作为可选项(≤15 视作枚举)
  if (col.dataType === 'string' || col.dataType === 'enum') {
    try {
      const agg = await instanceApi.aggregate({ classId: classId.value, groupBy: col.field, limit: 30 })
      if (agg && agg.length && agg.length <= 15) {
        filterOptions.value = agg.map(a => a.key)
        filterField.value.dataType = 'enum'
      }
    } catch {}
  }
}
function detectType(col) {
  return col.dataType || 'string'
}
function reopenFilter(pill, ev) {
  const col = columns.value.find(c => c.field === pill.field)
  if (!col) return
  editingPillId.value = pill.id            // 编辑该块
  openFilter(col, { currentTarget: ev.currentTarget })
  filterModel.value = pill                 // 回填该块已有条件(openFilter 内置空,这里覆盖)
}
function onFilterConfirm(group) {
  if (editingPillId.value != null) {
    const i = pills.value.findIndex(p => p.id === editingPillId.value)
    if (i >= 0) pills.value[i] = { ...group, id: editingPillId.value }
    else pills.value.push({ ...group, id: ++pidSeq })
  } else {
    pills.value.push({ ...group, id: ++pidSeq })   // 永远新增独立块(同字段也各自成块)
  }
  editingPillId.value = null
  filterField.value = null
  page.value = 1
  reload()
}
function removePill(id) {
  pills.value = pills.value.filter(p => p.id !== id)
  page.value = 1; reload()
}
function hasPill(field) { return pills.value.some(p => p.field === field) }
function clearAll() {
  pills.value = []; kw.value = ''; sort.value = ''; page.value = 1
  reload()
}

/* —— 展示辅助 —— */
const OP_LABEL = { eq:'=', ne:'≠', gt:'>', lt:'<', gte:'≥', lte:'≤', between:'介于', contains:'含', notContains:'不含',
  startsWith:'开头', endsWith:'结尾', empty:'为空', notEmpty:'非空', in:'∈', notIn:'∉', after:'晚于', before:'早于',
  notStartsWith:'非开头', notEndsWith:'非结尾' }
function pillText(p) {
  const c = p.conditions[0]
  if (!c) return p.label
  const op = OP_LABEL[c.op] || c.op
  let v = c.op === 'between' ? `${c.value}~${c.value2}` : (Array.isArray(c.value) ? c.value.join('/') : (c.value ?? ''))
  if (c.op === 'empty' || c.op === 'notEmpty') v = ''
  const more = p.conditions.length > 1 ? ` +${p.conditions.length - 1}` : ''
  return `${p.label} ${op} ${v}${more}`.trim()
}
function typeIcon(dt) {
  switch (dt) {
    case 'int': case 'decimal': return 'hash'
    case 'date': case 'datetime': return 'calendar'
    case 'time': return 'clock'
    case 'boolean': return 'check'
    case 'enum': return 'list'
    default: return 'fileText'
  }
}
function fmt(v, dt) {
  if (v === null || v === undefined || v === '') return '—'
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (dt === 'decimal' && typeof v === 'number') return v.toLocaleString(undefined, { maximumFractionDigits: 2 })
  return v
}

/* click-outside 局部指令 */
const vClickOutside = {
  mounted(el, binding) { el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }; setTimeout(() => document.addEventListener('mousedown', el.__h), 0) },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}

watch(() => props.initialClassId, (v) => { if (v && v !== classId.value) selectType(v) })
onMounted(() => { if (classId.value) loadMeta() })
</script>

<style scoped>
.ixe-root { flex: 1; display: flex; flex-direction: column; min-height: 0; background: var(--bl-bg-2); }

/* —— 头部:对象类型 + 搜索 + 保存 —— */
.ixe-head { flex-shrink: 0; display: flex; align-items: center; gap: 10px; padding: 8px 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); }
.ixe-head-search { position: relative; flex: 1; min-width: 0; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; min-height: 34px; box-sizing: border-box; padding: 4px 12px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); }
.ixe-head-search .ixe-pill { flex-shrink: 0; max-width: 240px; }
.ixe-head-search .ixe-hs-input { min-width: 140px; }
.ixe-hs-clear { flex-shrink: 0; display: inline-flex; align-items: center; gap: 2px; height: 22px; padding: 0 8px; border: 0; border-radius: 4px; background: var(--bl-bg-2); color: var(--bl-text-3); font-size: 12px; cursor: pointer; }
.ixe-hs-clear:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.ixe-head-search:focus-within { border-color: var(--bl-primary); }
.ixe-hs-ic { display: inline-flex; flex-shrink: 0; }
.ixe-hs-input { flex: 1; border: 0; outline: none; background: transparent; font-size: 13px; }
.ixe-fixed-title { display: flex; align-items: center; gap: 6px; flex-shrink: 0; font-size: 14px; color: var(--bl-text-1); padding: 4px 2px; }
.ixe-save { flex-shrink: 0; height: 34px; padding: 0 16px; border: 0; border-radius: 6px; background: var(--bl-primary); color: #fff; font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; }
.ixe-save:hover { background: var(--bl-primary-hover, #0e42d2); }
.ixe-save-sm { height: 28px; box-sizing: border-box; padding: 0 14px; font-size: 12.5px; }

/* —— 子头:布局名 + 结果数 + pills —— */
.ixe-subhead { flex-shrink: 0; display: flex; align-items: center; gap: 10px; padding: 6px 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); }
.ixe-layout-sel { position: relative; display: flex; align-items: center; gap: 4px; height: 28px; box-sizing: border-box; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 6px; cursor: pointer; font-size: 13px; flex-shrink: 0; max-width: 200px; }
.ixe-layout-sel:hover { border-color: var(--bl-primary-border); }
.ixe-layout-menu { position: absolute; top: 100%; left: 0; margin-top: 6px; width: 200px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 8px 28px rgba(0,0,0,.14); z-index: 50; padding: 6px; }
.ixe-layout-item { display: flex; align-items: center; gap: 6px; padding: 7px 8px; border-radius: 6px; cursor: pointer; font-size: 12.5px; }
.ixe-layout-item:hover { background: var(--bl-bg-hover); }
.ixe-layout-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixe-layout-sep { height: 1px; background: var(--bl-divider); margin: 4px 0; }
.ixe-layout-del { border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; padding: 2px; border-radius: 4px; display: none; flex-shrink: 0; }
.ixe-layout-item:hover .ixe-layout-del { display: inline-flex; }
.ixe-layout-del:hover { color: #f53f3f; background: #fff1f0; }
.ixe-layout-new { color: var(--bl-primary); }
.ixe-result-badge { font-size: 12px; color: var(--bl-primary); background: var(--bl-primary-soft); border-radius: 4px; padding: 3px 8px; flex-shrink: 0; }
.ixe-pills-bar { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; min-width: 0; }

/* —— 主体:看板 + 结果列 —— */
.ixe-main { flex: 1; display: flex; min-height: 0; overflow: hidden; }
.ixe-dash { flex: 1; min-width: 0; }
.ixe-results { width: 300px; flex-shrink: 0; border-left: 1px solid var(--bl-border); background: var(--bl-bg-1); display: flex; flex-direction: column; }
.ixe-results-hd { display: flex; align-items: center; gap: 8px; padding: 12px 14px 8px; border-bottom: 1px solid var(--bl-divider); }
.ixe-results-title { font-size: 13px; font-weight: 600; }
.ixe-results-cnt { font-size: 11px; color: var(--bl-text-3); }
.ixe-results-sort { font-size: 12px; color: var(--bl-text-2); display: inline-flex; align-items: center; gap: 2px; cursor: pointer; }
.ixe-results-list { flex: 1; overflow: auto; padding: 4px 8px; }
.ixe-results-item { display: flex; align-items: center; gap: 8px; padding: 7px 6px; border-radius: 6px; cursor: pointer; }
.ixe-results-item:hover { background: var(--bl-bg-hover); }
.ixe-results-ic { width: 22px; height: 22px; border-radius: 5px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ixe-results-more { padding: 10px 14px; font-size: 12.5px; color: var(--bl-primary); text-align: center; cursor: pointer; border-top: 1px solid var(--bl-divider); }
.ixe-results-more:hover { background: var(--bl-primary-soft); }

/* 搜索菜单面板:左 关联对象类型(280px) + 右 属性列表(图4) */
.ixe-sp {
  position: absolute; top: calc(100% + 8px); left: 0; width: 720px; max-width: 92vw; height: 360px;
  display: flex; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px;
  box-shadow: 0 16px 48px rgba(0,0,0,.16); z-index: 60; overflow: hidden;
}
.ixe-sp-left { width: 280px; flex-shrink: 0; border-right: 1px solid var(--bl-border); display: flex; flex-direction: column; }
.ixe-sp-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.ixe-sp-cur { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: var(--bl-primary-soft); font-size: 13px; }
.ixe-sp-hd { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); font-weight: 500; padding: 10px 12px 6px; }
.ixe-sp-n { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 9px; padding: 0 6px; }
.ixe-sp-search { margin: 0 12px 6px; height: 28px; border: 1px solid var(--bl-border); border-radius: 6px; padding: 0 8px; font-size: 12.5px; outline: none; }
.ixe-sp-search:focus { border-color: var(--bl-primary); }
.ixe-sp-scroll { flex: 1; overflow: auto; padding: 0 6px 8px; }
.ixe-sp-empty { font-size: 12px; color: var(--bl-text-3); padding: 16px; text-align: center; }
.ixe-sp-link, .ixe-sp-prop { display: flex; align-items: center; gap: 8px; padding: 7px 8px; border-radius: 6px; cursor: pointer; font-size: 12.5px; color: var(--bl-text-1); }
.ixe-sp-link:hover, .ixe-sp-prop:hover { background: var(--bl-bg-hover); }
.ixe-sp-prop.is-filtered { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixe-sp-prop-ic { display: inline-flex; flex-shrink: 0; }
.ixe-type {
  position: relative; display: flex; align-items: center; gap: 6px;
  height: 30px; box-sizing: border-box; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 6px;
  cursor: pointer; flex-shrink: 0; font-size: 13px; background: var(--bl-bg-1);
}
.ixe-type:hover { border-color: var(--bl-primary-border); }
.ixe-type-ic { width: 22px; height: 22px; border-radius: 5px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ixe-type-menu {
  position: absolute; top: 100%; left: 0; margin-top: 6px; width: 280px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px;
  box-shadow: 0 8px 28px rgba(0,0,0,.14); z-index: 50; padding: 8px;
}
.ixe-type-list { max-height: 320px; overflow: auto; }
.ixe-type-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.ixe-type-item:hover { background: var(--bl-bg-hover); }
.ixe-type-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }

.ixe-pills {
  flex: 0 1 260px; min-width: 160px; max-width: 260px;
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  border: 1px solid var(--bl-border); border-radius: 6px; padding: 0 8px; min-height: 30px; box-sizing: border-box; background: var(--bl-bg-1);
}
.ixe-pill {
  display: inline-flex; align-items: center; gap: 4px; max-width: 220px;
  background: var(--bl-primary-soft); color: var(--bl-primary);
  border-radius: 4px; padding: 2px 6px; font-size: 12px; cursor: pointer;
}
.ixe-pill-x { border: 0; background: transparent; color: inherit; cursor: pointer; display: inline-flex; padding: 0; opacity: .7; }
.ixe-pill-x:hover { opacity: 1; }
.ixe-kw { flex: 1; min-width: 120px; border: 0; outline: none; background: transparent; font-size: 13px; padding: 4px; }
.ixe-icon-btn {
  width: 30px; height: 30px; flex-shrink: 0; border: 1px solid var(--bl-border); background: var(--bl-bg-1);
  border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.ixe-icon-btn:hover { background: var(--bl-bg-hover); color: var(--bl-primary); border-color: var(--bl-primary-border); }
.ixe-seg { display: inline-flex; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; flex-shrink: 0; }
.ixe-seg button {
  display: inline-flex; align-items: center; gap: 4px; height: 28px; box-sizing: border-box; padding: 0 12px; font-size: 12.5px;
  border: 0; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer;
}
.ixe-seg button + button { border-left: 1px solid var(--bl-border); }
.ixe-seg button span { display: inline-flex; }
.ixe-seg button.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.ixe-seg button:hover:not(.is-on) { background: var(--bl-bg-hover); }

/* 主体 */
.ixe-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }
.ixe-left {
  width: 260px; flex-shrink: 0; border-right: 1px solid var(--bl-border);
  overflow: auto; padding: 8px; background: var(--bl-bg-1);
}
.ixe-cur {
  display: flex; align-items: center; gap: 8px; padding: 8px;
  background: var(--bl-primary-soft); border-radius: 6px; margin-bottom: 8px; font-size: 13px;
}
.ixe-sec {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: var(--bl-text-3); padding: 10px 8px 4px; letter-spacing: .3px;
}
.ixe-sec-n { background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 0 6px; font-size: 10px; }
.ixe-sec-empty { font-size: 12px; color: var(--bl-text-3); padding: 4px 8px; }
.ixe-link, .ixe-prop {
  display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px;
  cursor: pointer; font-size: 12.5px; color: var(--bl-text-2);
}
.ixe-link:hover, .ixe-prop:hover { background: var(--bl-bg-hover); }
.ixe-prop.is-filtered { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixe-link-ic, .ixe-prop-ic { display: inline-flex; flex-shrink: 0; }
.ixe-link-ic { width: 20px; height: 20px; border-radius: 5px; align-items: center; justify-content: center; }
.ixe-link-cnt { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 0 6px; }

.ixe-right { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.ixe-result-hd { display: flex; align-items: center; padding: 8px 14px; border-bottom: 1px solid var(--bl-divider); font-size: 13px; }
.ixe-table-wrap { flex: 1; overflow: auto; background: var(--bl-bg-1); }
.ixe-table { font-size: 12.5px; border-collapse: separate; border-spacing: 0; width: 100%; }
/* 表头：吸顶 + 灰底，与内容拉开层次 */
.ixe-table thead th {
  position: sticky; top: 0; z-index: 3;
  background: var(--bl-bg-2); color: var(--bl-text-2); font-weight: 500; text-align: left;
  white-space: nowrap; cursor: pointer; user-select: none;
  padding: 8px 12px; border-bottom: 1px solid var(--bl-border);
}
/* 数据行：白底 + 浅灰行分隔线 */
.ixe-table tbody td {
  background: var(--bl-bg-1);
  padding: 8px 12px; border-bottom: 1px solid var(--bl-divider);
  white-space: nowrap; max-width: 220px; overflow: hidden; text-overflow: ellipsis;
}
.ixe-table tbody tr { cursor: pointer; }
.ixe-table tbody tr:hover td { background: var(--bl-bg-hover); }
/* 左侧固定列 */
.ixe-sticky-col { position: sticky; left: 0; z-index: 2; min-width: 180px; box-shadow: 1px 0 0 var(--bl-divider); }
.ixe-table thead .ixe-sticky-col { z-index: 4; }
.ixe-table tbody tr:hover .ixe-sticky-col { background: var(--bl-bg-hover); }
.ixe-code-col { width: 90px; }
.ixe-pager { display: flex; align-items: center; gap: 12px; justify-content: center; padding: 8px; border-top: 1px solid var(--bl-divider); }
.ixe-pick { flex: 1; padding: 80px 20px; }
</style>
