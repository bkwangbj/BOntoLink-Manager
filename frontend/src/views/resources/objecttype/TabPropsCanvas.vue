<template>
  <div class="er-wrap" ref="wrapRef">
    <!-- 工具栏 -->
    <div class="er-toolbar">
      <div class="er-tools">
        <!-- 模式连体按钮组 -->
        <div class="er-seg">
          <button v-for="m in modes" :key="m.k"
                  :class="['er-seg-btn', mode===m.k && 'is-on']"
                  :title="`${m.label} (${m.key})`" @click="mode = m.k">
            <span v-html="BL.icon(m.icon, 14)"></span>
          </button>
        </div>
        <div class="er-tdiv"></div>
        <!-- 添加辅助表 (在画布中插入一个辅助表卡片) -->
        <button class="er-tbtn" title="添加辅助表" @click="onAddSupplement"><span v-html="BL.icon('folderPlus', 14)"></span></button>
        <!-- 清除所有映射 (scissors 区别于"删除模式"的 trash) -->
        <button class="er-tbtn" title="清除所有映射" @click="onClearMappings"><span v-html="BL.icon('scissors', 14)"></span></button>
        <!-- 重置布局 -->
        <button class="er-tbtn" title="重置布局 (R)" @click="onResetLayout"><span v-html="BL.icon('refresh', 14)"></span></button>
        <div class="er-tdiv"></div>
        <!-- 缩放连体组:[−][百分比(点击恢复100%)][+] -->
        <div class="er-zoom">
          <button class="er-zoom-btn" title="缩小 (Ctrl+-)" :disabled="zoom <= 0.5" @click="zoomBy(-0.1)">−</button>
          <button class="er-zoom-val" :title="`缩放 ${Math.round(zoom*100)}% · 点击恢复 100% (Ctrl+0)`" @click="zoom = 1">{{ Math.round(zoom*100) }}%</button>
          <button class="er-zoom-btn" title="放大 (Ctrl++)" :disabled="zoom >= 2" @click="zoomBy(0.1)">+</button>
        </div>
        <div class="er-tdiv"></div>
        <button class="er-tbtn" :class="showGrid && 'is-on'" title="显示网格" @click="showGrid = !showGrid"><span v-html="BL.icon('grid', 14)"></span></button>
        <div class="er-tdiv"></div>
        <!-- 新增物理表: 文本标签按钮 +物理表 (spec §5 item 13) -->
        <button class="er-tbtn er-tbtn-text" title="新增物理表 (创建新表 + 字段映射,不设置属性关联)" @click="onNewPhysicalTable">
          <span v-html="BL.icon('plus', 12)"></span>
          <span style="margin-left:4px">物理表</span>
        </button>
      </div>
      <div class="er-status">
        <span class="er-mode-label">当前模式: <b>{{ currentModeLabel }}</b></span>
        <span class="bl-muted" style="margin-left:14px">已映射: <b style="color:var(--bl-primary)">{{ mappedCount }} / {{ totalProps }}</b> 个属性</span>
      </div>
      <!-- 右侧:导出 + 全屏 -->
      <div class="er-tools-right">
        <button class="er-tbtn" title="导出图片 (PNG)" @click="onExport"><span v-html="BL.icon('share', 14)"></span></button>
        <button class="er-tbtn" :title="isFull ? '退出全屏' : '全屏'" @click="toggleFull"><span v-html="BL.icon(isFull ? 'minimize' : 'maximize', 14)"></span></button>
      </div>
    </div>

    <!-- 物理表切换条 (多表时: 每个表一张独立的图, 一次只画 对象 ↔ 当前表) -->
    <div v-if="datasourcesSorted.length > 1" class="er-tabs">
      <button v-for="ds in datasourcesSorted" :key="ds.id"
              :class="['er-tab', activeTableId === ds.id && 'is-on']"
              :title="`${ds.physical_table} · ${ds.rel_type === 1 ? '主表' : '辅助表'}`"
              @click="activeTableId = ds.id">
        <span class="er-tab-dot" :class="ds.rel_type === 1 ? 'is-main' : 'is-supp'"></span>
        <span class="bl-truncate">{{ ds.physical_table }}</span>
        <span class="bl-muted" style="margin-left:4px">{{ (ds.physical_fields || []).length }}</span>
      </button>
    </div>

    <!-- 画布 -->
    <div :class="['er-canvas', showGrid && 'is-grid', `is-mode-${mode}`]"
         ref="canvasRef"
         @click.self="onCanvasClick"
         @mousedown="onCanvasMouseDown">
      <div class="er-stage" :style="{ transform: `scale(${zoom})`, transformOrigin: 'top left' }">
        <!-- SVG 连线层 (提到卡片上层,端点不被卡片遮挡;见 .er-svg z-index) -->
        <svg class="er-svg" :width="stageW" :height="stageH">
          <!-- 字段映射连线 (直线) -->
          <g v-for="ln in mappingLines" :key="ln.key">
            <line :x1="ln.x1" :y1="ln.y1" :x2="ln.x2" :y2="ln.y2"
                  :stroke="ln.color" :stroke-width="ln.selected ? 3 : 2"
                  class="er-line" @click="onSelectLineForDelete(ln)" />
            <circle :cx="ln.x1" :cy="ln.y1" r="4" :fill="ln.color" />
            <circle :cx="ln.x2" :cy="ln.y2" r="4" :fill="ln.color" />
          </g>
          <!-- 表间关联曲线 (向外贝塞尔) -->
          <g v-for="(rel, i) in tableRelLines" :key="'rel-'+i">
            <path :d="rel.d" stroke="#FF7D00" :stroke-width="rel.selected ? 3 : 2" fill="none"
                  class="er-line er-curve" @click="onSelectRelLine(rel)" />
            <text :x="rel.labelX" :y="rel.labelY"
                  fill="#000" font-size="11" font-weight="600"
                  text-anchor="middle" dominant-baseline="middle"
                  class="er-rel-num">
              <tspan dx="0" dy="0">{{ rel.idx }}</tspan>
            </text>
          </g>
          <!-- 连线模式拖拽中的临时线 -->
          <g v-if="dragConnect.active">
            <line :x1="dragConnect.x1" :y1="dragConnect.y1" :x2="dragConnect.x2" :y2="dragConnect.y2"
                  :stroke="dragConnect.valid ? '#1677ff' : '#f53f3f'"
                  stroke-width="2" stroke-dasharray="4 4" />
            <circle :cx="dragConnect.x1" :cy="dragConnect.y1" r="4" :fill="dragConnect.valid ? '#1677ff' : '#f53f3f'" />
            <circle :cx="dragConnect.x2" :cy="dragConnect.y2" r="4" :fill="dragConnect.valid ? '#1677ff' : '#f53f3f'" />
          </g>
        </svg>

        <!-- 对象类卡片 -->
        <div ref="objCardRef" class="er-card er-card-obj"
             :style="{ left: layout.obj.x + 'px', top: layout.obj.y + 'px', width: layout.obj.w + 'px' }">
          <div class="er-card-hd er-hd-obj">
            <span class="bl-truncate">{{ classInfo.display_name || classInfo.api_name || '—' }} ({{ classInfo.api_name }})</span>
            <span class="er-hd-cnt">{{ properties.length }} 个属性</span>
          </div>
          <div class="er-prop-list" ref="propListRef">
            <div v-for="(p, i) in propertiesSorted" :key="p.id"
                 :class="['er-prow',
                          selected.kind==='prop' && selected.id===p.id && 'is-sel',
                          p._mapped && 'is-mapped',
                          highlightProps.has(p.id) && 'is-hl',
                          mode==='sort' && 'is-sortable']"
                 :data-prop-id="p.id"
                 :draggable="mode==='sort'"
                 @dragstart="onPropDragStart(i, $event)"
                 @dragover="onPropDragOver(i, $event)"
                 @dragend="onPropDragEnd"
                 @click.stop="onSelectProp(p)">
              <!-- 拖拽插入指示线 (排序模式) -->
              <div v-if="mode==='sort' && sortInsertIdx===i && sortDragFromIdx!==-1 && sortDragFromIdx!==i" class="er-sort-hint"></div>
              <span class="er-row-no">{{ i + 1 }}</span>
              <span class="er-prop-ic" :style="{ background: propIconColor(p) }" v-html="BL.icon(propIcon(p), 10, '#fff')"></span>
              <span class="er-dt-ic" :title="p.data_type">{{ dataTypeShort(p.data_type) }}</span>
              <div class="er-row-text bl-truncate">
                {{ p.display_name || p.rdfs_label || p.api_name }}<span class="bl-muted bl-mono"> ({{ p.api_name }})</span>
              </div>
              <span class="er-dot er-dot-out"
                    :class="propDotClass(p)"
                    :data-anchor="`prop:${p.id}`"
                    @mousedown="onAnchorMouseDown($event, 'prop', p)"
                    @click.stop="onAnchorClick('prop', p)"></span>
            </div>
            <div v-if="!properties.length" class="bl-empty" style="padding:24px">尚无属性</div>
          </div>
        </div>

        <!-- 物理表卡片 (当前选中表; 多表时由顶部切换条决定) -->
        <div v-for="(ds, dsIdx) in visibleDatasources" :key="ds.id"
             :ref="el => tableRefs[ds.id] = el"
             :class="['er-card', 'er-card-tbl', ds.rel_type === 1 ? 'is-main' : 'is-supp']"
             :style="{ left: layout.tables[ds.id]?.x + 'px', top: layout.tables[ds.id]?.y + 'px', width: layout.obj.w + 'px' }">
          <div :class="['er-card-hd', ds.rel_type === 1 ? 'er-hd-main' : 'er-hd-supp']">
            <span class="bl-truncate">{{ ds.physical_table }} ({{ ds.table_label || ds.alias }})</span>
            <span class="er-hd-cnt">{{ (ds.physical_fields || []).length }} 字段</span>
          </div>
          <div class="er-field-list">
            <div v-for="f in sortedFieldsOf(ds)" :key="ds.id + '_' + f.name"
                 :class="['er-frow',
                          selected.kind==='field' && selected.id===`${ds.id}:${f.name}` && 'is-sel',
                          f._mapped && 'is-mapped',
                          highlightFields.has(`${ds.id}:${f.name}`) && 'is-hl']"
                 :data-field-id="`${ds.id}:${f.name}`"
                 @click.stop="onSelectField(ds, f)">
              <span class="er-dot er-dot-in"
                    :class="fieldDotClass(f)"
                    :data-anchor="`field:${ds.id}:${f.name}`"
                    @click.stop="onAnchorClick('field', { dsId: ds.id, name: f.name, f })"></span>
              <span class="er-row-text bl-mono">{{ f.name }}</span>
              <span class="er-dt-ic" :title="f.data_type">{{ dataTypeShort(f.data_type) }}</span>
              <!-- 表间关联圆点 (右侧,橙色) - 只在主键/外键显示 -->
              <span v-if="ds.rel_type === 1 && (f.is_pk || f.is_fk)" class="er-dot er-dot-rel"
                    :data-anchor="`rel:${ds.id}:${f.name}`"></span>
              <span v-if="ds.rel_type !== 1 && (f.is_pk || f.is_fk)" class="er-dot er-dot-rel"
                    :data-anchor="`rel:${ds.id}:${f.name}`"></span>
            </div>
            <div v-if="!(ds.physical_fields || []).length" class="bl-empty" style="padding:18px;font-size:12px">该数据集无字段定义</div>
          </div>
        </div>
      </div>

      <!-- 选中信息浮层 -->
      <div v-if="selected.kind && selected.summary" class="er-sel-card">
        <div class="er-sel-hd">
          <span v-html="BL.icon('info', 12, '#1677ff')"></span>
          <span style="margin-left:6px;font-weight:600">{{ selected.summary.title }}</span>
          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" style="margin-left:auto"
                  @click="selected = { kind: null, id: null, summary: null }"
                  v-html="BL.icon('x', 12)"></button>
        </div>
        <div class="er-sel-body">
          <div v-for="kv in selected.summary.kv" :key="kv.k" class="er-sel-kv">
            <span class="er-sel-k">{{ kv.k }}</span>
            <span class="er-sel-v">{{ kv.v }}</span>
          </div>
          <div v-if="mode === 'delete' && (selected.kind === 'line' || selected.kind === 'rel')" style="margin-top:10px">
            <button class="bl-btn bl-btn-sm bl-btn-danger" @click="onDeleteSelected">
              <span v-html="BL.icon('trash', 11, '#fff')"></span><span style="margin-left:4px">删除连线</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { resourceApi, classMetaApi } from '@/api'

const props = defineProps({ classId: { type: String, default: '' } })

/* 操作模式 */
const modes = [
  { k: 'select',  label: '选择模式', icon: 'navigation', key: 'V' },  // 箭头光标
  { k: 'sort',    label: '排序模式', icon: 'menu',       key: 'S' },  // 三横线 (列表)
  { k: 'connect', label: '连线模式', icon: 'link',       key: 'L' },
  { k: 'delete',  label: '删除模式', icon: 'trash',      key: 'D' }
]
const mode = ref('select')
const currentModeLabel = computed(() => modes.find(m => m.k === mode.value)?.label || '—')

/* 数据 */
const classInfo = ref({})
const properties = ref([])
const datasources = ref([])
const propertiesSorted = computed(() => [...properties.value].sort((a, b) => (a.sort ?? 99999) - (b.sort ?? 99999) || (a.api_name || '').localeCompare(b.api_name || '')))
const datasourcesSorted = computed(() => [...datasources.value].sort((a, b) => (a.rel_type - b.rel_type) || (a.sort - b.sort)))

/* 当前选中的物理表: 每个表一张独立的图 (单表直接显示, 多表用顶部切换条切换) */
const activeTableId = ref('')
const visibleDatasources = computed(() => {
  const list = datasourcesSorted.value
  if (list.length <= 1) return list
  const active = list.find(d => d.id === activeTableId.value)
  return active ? [active] : [list[0]]
})

const totalProps = computed(() => properties.value.length)
const mappedCount = computed(() => properties.value.filter(p => p._mapped).length)

async function loadDetail() {
  if (!props.classId) return
  const r = await resourceApi.classDetail(props.classId).catch(() => null)
  if (!r) return
  classInfo.value = { id: r.id, api_name: r.api_name, display_name: r.display_name, rdfs_label: r.rdfs_label }
  properties.value = (r.properties || []).map(p => ({ ...p, _mapped: !!(p.physical_table && p.physical_column) }))
  // 标记每个字段是否被映射 (服务端不一定提供,前端推算)
  datasources.value = (r.classDatasources || []).map(d => {
    const fields = (d.physical_fields || []).map(f => ({
      ...f,
      _mapped: properties.value.some(p => p.physical_table === d.physical_table && p.physical_column === f.name)
    }))
    return { ...d, physical_fields: fields }
  })
  // 默认选中第一张表 (主表优先, datasourcesSorted 已按 rel_type 排序)
  if (!datasourcesSorted.value.some(d => d.id === activeTableId.value)) {
    activeTableId.value = datasourcesSorted.value[0]?.id || ''
  }
  resetLayout()
}
onMounted(loadDetail)
watch(() => props.classId, loadDetail)
/* 切换物理表 → 重新布局当前表的图 */
watch(activeTableId, () => resetLayout())

/* 布局: 自适应宽度,对象类左,物理表右垂直排布 */
const wrapRef = ref(null)
/* 整块关系图全屏 */
const isFull = ref(false)
function toggleFull() {
  const el = wrapRef.value
  if (!el) return
  if (!document.fullscreenElement) el.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.()
}
function onFsChange() {
  isFull.value = document.fullscreenElement === wrapRef.value
  nextTick(() => recomputeLines())
}
const canvasRef = ref(null)
const propListRef = ref(null)
const objCardRef = ref(null)
const tableRefs = reactive({})  // dsId -> DOM
const layout = reactive({ obj: { x: 24, y: 24, w: 380 }, tables: {} })
const stageW = ref(1200)
const stageH = ref(800)

function resetLayout() {
  layout.obj.x = 24
  layout.obj.y = 24
  layout.obj.w = 380
  const tableX = layout.obj.x + layout.obj.w + 180
  let cursorY = layout.obj.y
  for (const d of visibleDatasources.value) {
    layout.tables[d.id] = { x: tableX, y: cursorY }
    // 估算高度: 44 头 + 36 * fields + 16 边距
    const h = 44 + Math.max(1, (d.physical_fields || []).length) * 38 + 18
    cursorY += h + 24
  }
  stageW.value = Math.max(1200, tableX + layout.obj.w + 80)
  stageH.value = Math.max(600, cursorY + 80)
  // 让连线层在卡片渲染完后再算位置
  nextTick(recomputeLines)
}
function onResetLayout() { resetLayout() }

/* 缩放 */
const zoom = ref(1)
function zoomBy(d) { zoom.value = Math.min(2.0, Math.max(0.5, +(zoom.value + d).toFixed(2))) }
const showGrid = ref(false)

/* 数据类型简化图标 */
function dataTypeShort(t) {
  if (!t) return '?'
  const s = String(t).toLowerCase()
  if (s.includes('string')) return 'abc'
  if (s.includes('int') || s.includes('decimal') || s.includes('double') || s.includes('float') || s === 'number') return '123'
  if (s.includes('bool')) return '✓✗'
  if (s.includes('date') || s.includes('time')) return '📅'
  return '·'
}

/* 属性图标 + 颜色 (按 prop_type / is_key) */
function propIcon(p) {
  if (p.is_key) return 'key'
  if (p.prop_type === 'object') return 'link'
  if (p.prop_type === 'annotation') return 'chat'
  if (p.is_derived) return 'zap'
  return 'database'
}
function propIconColor(p) {
  if (p.is_key) return '#F53F3F'
  if (p.prop_type === 'object') return '#FF7D00'
  if (p.prop_type === 'annotation') return '#00B42A'
  if (p.is_derived) return '#722ED1'
  return '#1677FF'
}
function propDotClass(p) {
  if (p.prop_type === 'object' || p.prop_type === 'annotation' || p.is_derived) return 'is-no-map'   // 半透明
  if (p.is_key) return 'is-key'                                                                       // 红色
  return ''                                                                                            // 灰色
}
function fieldDotClass(f) {
  if (f.is_pk) return 'is-key'
  if (f.is_fk) return 'is-fk'
  return ''
}

/* 字段排序: 已映射在前 (按属性顺序),未映射在后 (按原始顺序) */
function sortedFieldsOf(ds) {
  const fields = ds.physical_fields || []
  const mappedOrder = new Map()
  propertiesSorted.value.forEach((p, idx) => {
    if (p.physical_table === ds.physical_table && p.physical_column) {
      mappedOrder.set(p.physical_column, idx)
    }
  })
  return [...fields].sort((a, b) => {
    const aM = mappedOrder.has(a.name) ? mappedOrder.get(a.name) : 99999
    const bM = mappedOrder.has(b.name) ? mappedOrder.get(b.name) : 99999
    if (aM === bM) return (a.name || '').localeCompare(b.name || '')
    return aM - bM
  })
}

/* —— 连线计算 —— */
const mappingLines = ref([])
const tableRelLines = ref([])
const selected = reactive({ kind: null, id: null, summary: null })

function recomputeLines() {
  if (!canvasRef.value) { mappingLines.value = []; tableRelLines.value = []; return }
  const canvasBox = canvasRef.value.getBoundingClientRect()
  const z = zoom.value || 1
  /** 把元素的页面坐标换算到 stage (unscaled) 坐标 */
  const xy = (el) => {
    const b = el.getBoundingClientRect()
    return {
      x: (b.left - canvasBox.left + canvasRef.value.scrollLeft) / z + b.width / (2 * z),
      y: (b.top - canvasBox.top + canvasRef.value.scrollTop) / z + b.height / (2 * z)
    }
  }
  const lines = []
  // 属性 -> 字段 映射 (仅当前可见表)
  for (const p of properties.value) {
    if (!p.physical_table || !p.physical_column) continue
    const ds = visibleDatasources.value.find(d => d.physical_table === p.physical_table)
    if (!ds) continue
    const aOut = canvasRef.value.querySelector(`[data-anchor="prop:${p.id}"]`)
    const aIn  = canvasRef.value.querySelector(`[data-anchor="field:${ds.id}:${p.physical_column}"]`)
    if (!aOut || !aIn) continue
    const A = xy(aOut), B = xy(aIn)
    const isKey = p.is_key === 1 || p.is_key === true
    lines.push({
      key: `m:${p.id}`,
      x1: A.x, y1: A.y, x2: B.x, y2: B.y,
      color: isKey ? '#F53F3F' : '#1677FF',
      kind: 'mapping',
      propId: p.id,
      dsId: ds.id,
      field: p.physical_column,
      selected: selected.kind === 'line' && selected.id === `m:${p.id}`
    })
  }
  mappingLines.value = lines

  // 表间关联: 主表 join 补充表 (按 join_on_keys)
  const main = datasources.value.find(d => d.rel_type === 1)
  const supps = datasources.value.filter(d => d.rel_type !== 1)
  const rels = []
  if (main) {
    supps.forEach((s, i) => {
      const joinKey = (s.join_on_keys || '').split(',').map(x => x.trim()).filter(Boolean)[0] || ''
      const aOut = canvasRef.value.querySelector(`[data-anchor="rel:${main.id}:${joinKey}"]`)
      const aIn  = canvasRef.value.querySelector(`[data-anchor="rel:${s.id}:${joinKey}"]`)
      if (!aOut || !aIn) return
      const A = xy(aOut), B = xy(aIn)
      // 向外弯曲 150px 贝塞尔
      const off = 150
      const ctrl1 = { x: A.x + off, y: A.y }
      const ctrl2 = { x: B.x + off, y: B.y }
      const d = `M ${A.x} ${A.y} C ${ctrl1.x} ${ctrl1.y}, ${ctrl2.x} ${ctrl2.y}, ${B.x} ${B.y}`
      rels.push({
        key: `rel:${main.id}->${s.id}`,
        d,
        idx: i + 1,
        labelX: Math.max(A.x, B.x) + off - 12,
        labelY: (A.y + B.y) / 2,
        mainId: main.id, suppId: s.id,
        selected: selected.kind === 'rel' && selected.id === `rel:${main.id}->${s.id}`
      })
    })
  }
  tableRelLines.value = rels
}

watch([properties, datasources, zoom], () => nextTick(recomputeLines), { deep: true })
let resizeObs = null
onMounted(() => {
  if (canvasRef.value) {
    resizeObs = new ResizeObserver(() => recomputeLines())
    resizeObs.observe(canvasRef.value)
  }
  window.addEventListener('keydown', onKey)
  document.addEventListener('fullscreenchange', onFsChange)
})
onBeforeUnmount(() => {
  if (resizeObs) resizeObs.disconnect()
  window.removeEventListener('keydown', onKey)
  document.removeEventListener('fullscreenchange', onFsChange)
})

function onKey(e) {
  if (e.target && ['INPUT','TEXTAREA','SELECT'].includes(e.target.tagName)) return
  const k = e.key.toLowerCase()
  if (k === 'v') mode.value = 'select'
  else if (k === 's') mode.value = 'sort'
  else if (k === 'l') mode.value = 'connect'
  else if (k === 'd') mode.value = 'delete'
  else if (k === 'r') resetLayout()
  else if (e.ctrlKey && (e.key === '=' || e.key === '+')) { e.preventDefault(); zoomBy(0.1) }
  else if (e.ctrlKey && e.key === '-') { e.preventDefault(); zoomBy(-0.1) }
  else if (e.ctrlKey && e.key === '0') { e.preventDefault(); zoom.value = 1 }
}

/* —— 选择 —— */
function onSelectProp(p) {
  selected.kind = 'prop'; selected.id = p.id
  selected.summary = {
    title: `属性 · ${p.display_name || p.rdfs_label || p.api_name}`,
    kv: [
      { k: '编码', v: p.api_name },
      { k: '类型', v: p.prop_type === 'object' ? '对象属性' : p.prop_type === 'annotation' ? '注释属性' : (p.is_derived ? '派生属性' : '数据属性') },
      { k: '数据类型', v: p.data_type || '—' },
      { k: '主键 / 必填', v: `${p.is_key ? '是' : '否'} / ${p.is_required ? '是' : '否'}` },
      { k: '物理映射', v: p._mapped ? `${p.physical_table}.${p.physical_column}` : '未映射' }
    ]
  }
}
function onSelectField(ds, f) {
  selected.kind = 'field'; selected.id = `${ds.id}:${f.name}`
  selected.summary = {
    title: `字段 · ${ds.physical_table}.${f.name}`,
    kv: [
      { k: '数据集', v: `${ds.physical_table} (${ds.alias})` },
      { k: '数据类型', v: f.data_type },
      { k: '约束', v: `${f.is_pk ? '主键 ' : ''}${f.is_fk ? '外键 ' : ''}` || '普通' },
      { k: '映射状态', v: f._mapped ? '已映射' : '未映射' }
    ]
  }
}
function onSelectLine(ln) {
  selected.kind = 'line'; selected.id = ln.key
  const p = properties.value.find(x => x.id === ln.propId)
  selected.summary = {
    title: `映射连线 · ${p?.api_name || ''} → ${ln.field}`,
    kv: [
      { k: '属性', v: p?.display_name || p?.api_name || '—' },
      { k: '字段', v: ln.field },
      { k: '类型', v: ln.color === '#F53F3F' ? '主键映射 (红色)' : '普通映射 (蓝色)' }
    ]
  }
  recomputeLines()
}
function onSelectRelLine(rel) {
  selected.kind = 'rel'; selected.id = rel.key
  selected.summary = {
    title: `表间关联 #${rel.idx}`,
    kv: [
      { k: '主表', v: datasources.value.find(d => d.id === rel.mainId)?.physical_table },
      { k: '辅表', v: datasources.value.find(d => d.id === rel.suppId)?.physical_table },
      { k: 'JOIN 键', v: datasources.value.find(d => d.id === rel.suppId)?.join_on_keys || '—' }
    ]
  }
  recomputeLines()
}
function onAnchorClick(/* kind, payload */) {
  // 锚点的点击交互完全由 mousedown/mouseup 拖拽流程承担,此处保留空函数兼容旧调用点
}
function onCanvasClick() {
  selected.kind = null; selected.id = null; selected.summary = null
}

/* —— 连线模式: 锚点拖拽创建映射 —— */
const dragConnect = reactive({ active: false, from: null, x1: 0, y1: 0, x2: 0, y2: 0, valid: true, msg: '' })

function getStageXY(ev) {
  const box = canvasRef.value.getBoundingClientRect()
  const z = zoom.value || 1
  return {
    x: (ev.clientX - box.left + canvasRef.value.scrollLeft) / z,
    y: (ev.clientY - box.top + canvasRef.value.scrollTop) / z
  }
}
function getAnchorCenter(el) {
  if (!el) return null
  const cBox = canvasRef.value.getBoundingClientRect()
  const z = zoom.value || 1
  const b = el.getBoundingClientRect()
  return {
    x: (b.left - cBox.left + canvasRef.value.scrollLeft) / z + b.width / (2 * z),
    y: (b.top - cBox.top + canvasRef.value.scrollTop) / z + b.height / (2 * z)
  }
}
function findAnchorAt(clientX, clientY) {
  // 通过 elementsFromPoint 查找命中的锚点
  const els = document.elementsFromPoint(clientX, clientY)
  for (const el of els) {
    const a = el.closest && el.closest('[data-anchor]')
    if (a) return a
  }
  return null
}
function parseAnchor(el) {
  const v = el?.getAttribute?.('data-anchor') || ''
  // "prop:cp-1" / "field:class-ds-01:station_code" / "rel:class-ds-01:station_code"
  if (v.startsWith('prop:')) return { kind: 'prop', propId: v.slice(5) }
  if (v.startsWith('field:')) { const [, dsId, name] = v.split(':'); return { kind: 'field', dsId, name } }
  if (v.startsWith('rel:'))   { const [, dsId, name] = v.split(':'); return { kind: 'rel',   dsId, name } }
  return null
}
function onAnchorMouseDown(ev, kind, payload) {
  if (mode.value !== 'connect') return
  if (ev.button !== 0) return
  if (kind === 'prop') {
    const p = payload
    if (p.prop_type === 'object' || p.prop_type === 'annotation' || p.is_derived) {
      BL.warning('关联属性 / 注释属性 / 计算派生属性 禁止连线')
      return
    }
    if (p._mapped) {
      BL.warning('该属性已映射,请先在删除模式下移除原连线')
      return
    }
    const el = ev.currentTarget
    const A = getAnchorCenter(el)
    if (!A) return
    dragConnect.active = true
    dragConnect.from = { kind: 'prop', propId: p.id, is_key: !!p.is_key }
    dragConnect.x1 = A.x; dragConnect.y1 = A.y
    dragConnect.x2 = A.x; dragConnect.y2 = A.y
    dragConnect.valid = true
    ev.preventDefault()
    window.addEventListener('mousemove', onConnectDragMove)
    window.addEventListener('mouseup', onConnectDragEnd)
  }
}
function onConnectDragMove(ev) {
  if (!dragConnect.active) return
  const xy = getStageXY(ev)
  dragConnect.x2 = xy.x; dragConnect.y2 = xy.y
  // 实时校验目标锚点的合法性
  const hit = findAnchorAt(ev.clientX, ev.clientY)
  const target = parseAnchor(hit)
  if (target && target.kind === 'field') {
    const ds = datasources.value.find(d => d.id === target.dsId)
    const f = ds && (ds.physical_fields || []).find(x => x.name === target.name)
    if (!ds || !f) { dragConnect.valid = false; dragConnect.msg = '无效字段'; return }
    if (f._mapped) { dragConnect.valid = false; dragConnect.msg = '该字段已被映射'; return }
    // 主键属性需要 PK 字段;非主键属性映射到任意字段
    dragConnect.valid = true; dragConnect.msg = ''
  } else {
    dragConnect.valid = true; dragConnect.msg = ''
  }
}
async function onConnectDragEnd(ev) {
  window.removeEventListener('mousemove', onConnectDragMove)
  window.removeEventListener('mouseup', onConnectDragEnd)
  if (!dragConnect.active) return
  const hit = findAnchorAt(ev.clientX, ev.clientY)
  const target = parseAnchor(hit)
  dragConnect.active = false
  if (!target || target.kind !== 'field') return
  const ds = datasources.value.find(d => d.id === target.dsId)
  const f = ds && (ds.physical_fields || []).find(x => x.name === target.name)
  if (!ds || !f) { BL.error('目标字段不存在'); return }
  if (f._mapped) { BL.warning('该字段已被映射'); return }
  const propId = dragConnect.from.propId
  const p = properties.value.find(x => x.id === propId)
  if (!p) return
  try {
    await classMetaApi.updateProp(p.id, {
      ...p, _mapped: undefined,
      physical_table: ds.physical_table,
      physical_column: f.name,
      class_ds_id: ds.id
    })
    BL.success(`映射已创建: ${p.api_name} → ${ds.physical_table}.${f.name}`)
    await loadDetail()
  } catch (e) { BL.error(e?.msg || '映射保存失败') }
}

/* —— 删除模式: 点击连线删除映射 —— */
async function onSelectLineForDelete(ln) {
  if (mode.value !== 'delete') { onSelectLine(ln); return }
  const p = properties.value.find(x => x.id === ln.propId)
  if (!p) return
  const ok = await BL.confirm({ title: '删除映射', content: `确定删除「${p.api_name} → ${ln.field}」的映射？`, danger: true, okText: '删除' })
  if (!ok) return
  try {
    await classMetaApi.updateProp(p.id, {
      ...p, _mapped: undefined,
      physical_table: null, physical_column: null, class_ds_id: null
    })
    BL.success('映射已删除')
    await loadDetail()
  } catch (e) { BL.error(e?.msg || '删除失败') }
}
async function onDeleteSelected() {
  if (selected.kind === 'line') {
    const ln = mappingLines.value.find(x => x.key === selected.id)
    if (ln) await onSelectLineForDelete(ln)
  } else if (selected.kind === 'rel') {
    BL.info('表间关联删除需更新 ont_class_ds.join_on_keys (待联调)')
  }
}

/* —— 排序模式: 拖拽属性行调整顺序 —— */
const sortDragFromIdx = ref(-1)
const sortInsertIdx = ref(-1)
function onPropDragStart(idx, ev) {
  if (mode.value !== 'sort') { ev.preventDefault(); return }
  sortDragFromIdx.value = idx
  ev.dataTransfer.effectAllowed = 'move'
}
function onPropDragOver(idx, ev) {
  if (mode.value !== 'sort' || sortDragFromIdx.value < 0) return
  ev.preventDefault()
  sortInsertIdx.value = idx
}
async function onPropDragEnd() {
  if (sortDragFromIdx.value < 0 || sortInsertIdx.value < 0 || sortDragFromIdx.value === sortInsertIdx.value) {
    sortDragFromIdx.value = -1; sortInsertIdx.value = -1; return
  }
  const list = [...propertiesSorted.value]
  const [item] = list.splice(sortDragFromIdx.value, 1)
  list.splice(sortInsertIdx.value, 0, item)
  const ids = list.map(p => p.id)
  sortDragFromIdx.value = -1; sortInsertIdx.value = -1
  try {
    await classMetaApi.reorderProps(props.classId, ids)
    BL.success('属性顺序已更新')
    await loadDetail()
  } catch (e) { BL.error(e?.msg || '排序保存失败') }
}

/* —— 清除所有映射 —— */
async function onClearMappings() {
  const mapped = properties.value.filter(p => p.physical_table && p.physical_column)
  if (!mapped.length) { BL.info('当前无映射可清除'); return }
  const ok = await BL.confirm({ title: '清除所有映射', content: `将清除 ${mapped.length} 条映射,此操作不可撤销。`, danger: true, okText: '清除' })
  if (!ok) return
  for (const p of mapped) {
    await classMetaApi.updateProp(p.id, { ...p, _mapped: undefined, physical_table: null, physical_column: null, class_ds_id: null }).catch(() => {})
  }
  BL.success('已清除全部映射')
  await loadDetail()
}

function onAddSupplement() {
  BL.info('添加辅助表: 需打开数据源/表选择对话框 (待联调). 当前可在对象详情的「数据源」tab 中添加')
}

/**
 * 新增物理表 (spec §5 工具栏 item 13): 创建新表 + 字段, 不设置属性关联.
 * 流程与"新增对象类型"类似,但仅写入 ont_class_ds 一条记录及其 physical_fields JSON.
 */
function onNewPhysicalTable() {
  BL.info('新增物理表: 待联调 (打开表-字段定义对话框,保存到 ont_class_ds, rel_type=2,不设置属性映射)')
}

/* —— 中键拖拽平移画布 —— */
let panState = { active: false, startX: 0, startY: 0, scrollLeft: 0, scrollTop: 0 }
function onCanvasMouseDown(ev) {
  if (ev.button !== 1) return  // 中键
  ev.preventDefault()
  panState = {
    active: true,
    startX: ev.clientX, startY: ev.clientY,
    scrollLeft: canvasRef.value.scrollLeft, scrollTop: canvasRef.value.scrollTop
  }
  document.body.style.cursor = 'grabbing'
  window.addEventListener('mousemove', onCanvasPanMove)
  window.addEventListener('mouseup', onCanvasPanEnd)
}
function onCanvasPanMove(ev) {
  if (!panState.active) return
  canvasRef.value.scrollLeft = panState.scrollLeft - (ev.clientX - panState.startX)
  canvasRef.value.scrollTop  = panState.scrollTop  - (ev.clientY - panState.startY)
}
function onCanvasPanEnd() {
  panState.active = false
  document.body.style.cursor = ''
  window.removeEventListener('mousemove', onCanvasPanMove)
  window.removeEventListener('mouseup', onCanvasPanEnd)
}

/* —— 导出 PNG (使用浏览器原生 SVG → PNG 转换) —— */
async function onExport() {
  if (!canvasRef.value) return
  try {
    const prev = zoom.value
    zoom.value = 1
    await nextTick()
    const node = canvasRef.value.querySelector('.er-stage')
    const w = node.scrollWidth, h = node.scrollHeight
    // 用 foreignObject 把 HTML 嵌入 SVG,再转成 PNG
    const html = node.outerHTML.replace(/<svg /g, '<svg xmlns="http://www.w3.org/2000/svg" ')
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">
      <foreignObject width="100%" height="100%">
        <div xmlns="http://www.w3.org/1999/xhtml" style="width:${w}px;height:${h}px;background:#f8f9fa">${html}</div>
      </foreignObject>
    </svg>`
    const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = w * 2; canvas.height = h * 2
      const ctx = canvas.getContext('2d')
      ctx.scale(2, 2)
      ctx.drawImage(img, 0, 0)
      URL.revokeObjectURL(url)
      canvas.toBlob((png) => {
        const a = document.createElement('a')
        const ts = new Date().toISOString().slice(0, 10)
        a.download = `本体关系图_${ts}.png`
        a.href = URL.createObjectURL(png)
        a.click()
        URL.revokeObjectURL(a.href)
        BL.success('已导出 PNG')
      })
      zoom.value = prev
    }
    img.onerror = () => {
      URL.revokeObjectURL(url); zoom.value = prev
      BL.warning('PNG 导出失败. 建议: npm i html-to-image 后切换到 html-to-image.toPng() 方案')
    }
    img.src = url
  } catch (e) {
    BL.error(e?.message || '导出失败')
  }
}

/* —— 选中映射线时,提供给模板用的"高亮端点"集合 —— */
const highlightProps = computed(() => {
  if (selected.kind !== 'line') return new Set()
  const ln = mappingLines.value.find(x => x.key === selected.id)
  return new Set(ln ? [ln.propId] : [])
})
const highlightFields = computed(() => {
  if (selected.kind !== 'line') return new Set()
  const ln = mappingLines.value.find(x => x.key === selected.id)
  return new Set(ln ? [`${ln.dsId}:${ln.field}`] : [])
})
</script>

<style scoped>
.er-wrap { display: flex; flex-direction: column; height: 100%; min-height: 480px; }

/* 工具栏 */
.er-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 10px; background: var(--bl-bg-2);
  border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
.er-tools { display: inline-flex; align-items: center; gap: 2px; }
.er-tools-right { display: inline-flex; align-items: center; gap: 2px; margin-left: 16px; }
/* 全屏时铺满屏幕,补背景避免透出黑底 */
.er-wrap:fullscreen { background: var(--bl-bg-1); height: 100vh; }

/* 模式连体按钮组 */
.er-seg {
  display: inline-flex; align-items: center; height: 28px;
  border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; background: var(--bl-bg-1);
}
.er-seg-btn {
  width: 30px; height: 100%; border: 0; border-left: 1px solid var(--bl-border);
  background: transparent; cursor: pointer; color: var(--bl-text-2);
  display: inline-flex; align-items: center; justify-content: center;
  transition: background-color .12s, color .12s;
}
.er-seg-btn:first-child { border-left: 0; }
.er-seg-btn:hover { background: var(--bl-bg-2); color: var(--bl-primary); }
.er-seg-btn.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.er-tbtn {
  width: 30px; height: 30px; border: 1px solid transparent;
  background: transparent; border-radius: 4px; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-2); transition: background-color .12s, color .12s;
}
.er-tbtn-wide { width: 52px; font-size: 12px; }
/* 文本型工具按钮: 用于 "+物理表" */
.er-tbtn-text {
  width: auto; padding: 0 10px; font-size: 12px;
  display: inline-flex; align-items: center;
}
.er-tbtn:hover { background: var(--bl-bg-1); color: var(--bl-primary); border-color: var(--bl-border); }
.er-tbtn.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); border-color: var(--bl-primary); }
.er-tdiv { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 6px; }

/* 缩放连体组 */
.er-zoom {
  display: inline-flex; align-items: center; height: 28px;
  border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden;
  background: var(--bl-bg-1);
}
.er-zoom-btn, .er-zoom-val {
  height: 100%; border: 0; background: transparent; cursor: pointer;
  color: var(--bl-text-2); display: inline-flex; align-items: center; justify-content: center;
  transition: background-color .12s, color .12s;
}
.er-zoom-btn { width: 26px; font-weight: 600; font-size: 15px; line-height: 1; }
.er-zoom-val {
  min-width: 52px; padding: 0 6px; font-size: 12px; font-variant-numeric: tabular-nums;
  color: var(--bl-text-1); border-left: 1px solid var(--bl-border); border-right: 1px solid var(--bl-border);
}
.er-zoom-btn:hover:not(:disabled), .er-zoom-val:hover { background: var(--bl-primary-soft); color: var(--bl-primary); }
.er-zoom-btn:disabled { opacity: .35; cursor: not-allowed; }
.er-status { font-size: 12px; color: var(--bl-text-2); margin-left: auto; }
.er-mode-label b { color: var(--bl-primary); margin-left: 4px; }

/* 物理表切换条 (多表时每张表一个 tab) */
.er-tabs {
  display: flex; align-items: center; gap: 4px; flex-wrap: wrap;
  padding: 6px 10px; background: var(--bl-bg-1);
  border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
.er-tab {
  display: inline-flex; align-items: center; gap: 4px; max-width: 200px;
  height: 26px; padding: 0 10px;
  border: 1px solid var(--bl-border); border-radius: 13px;
  background: var(--bl-bg-2); color: var(--bl-text-2);
  font-size: 12px; cursor: pointer;
  transition: background-color .12s, color .12s, border-color .12s;
}
.er-tab:hover { background: var(--bl-bg-1); color: var(--bl-text-1); }
.er-tab.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); border-color: var(--bl-primary); }
.er-tab-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.er-tab-dot.is-main { background: #00B42A; }
.er-tab-dot.is-supp { background: #FF7D00; }

/* 画布 */
.er-canvas {
  flex: 1; position: relative; overflow: auto;
  background: #f8f9fa;
  min-height: 400px;
}
.er-canvas.is-grid {
  background-image:
    linear-gradient(to right, #e5e7eb 1px, transparent 1px),
    linear-gradient(to bottom, #e5e7eb 1px, transparent 1px);
  background-size: 20px 20px;
}
.er-stage { position: relative; }
/* 连线层提到卡片之上(z-index > .er-card 的 2),避免端点被卡片遮挡;整体 pointer-events:none 不影响卡片交互 */
.er-svg { position: absolute; left: 0; top: 0; pointer-events: none; z-index: 6; }
.er-svg .er-line { pointer-events: auto; cursor: pointer; }
.er-svg .er-line:hover { filter: brightness(1.1); }

/* 卡片 */
.er-card {
  position: absolute; border-radius: 8px; background: #fff;
  border: 2px solid transparent;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  z-index: 2;
  overflow: hidden;
}
.er-card-obj { border-color: #722ED1; }
.er-card-tbl.is-main { border-color: #00B42A; }
.er-card-tbl.is-supp { border-color: #FF7D00; }
.er-card-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; color: #fff; font-size: 13px; font-weight: 600;
}
.er-hd-obj  { background: #722ED1; }
.er-hd-main { background: #00B42A; }
.er-hd-supp { background: #FF7D00; }
.er-hd-cnt {
  font-size: 11px; opacity: .9;
  background: rgba(255,255,255,.18); padding: 2px 8px; border-radius: 10px;
  flex-shrink: 0;
}

/* 属性行 */
.er-prop-list, .er-field-list { padding: 4px 0; }
.er-prow, .er-frow {
  display: flex; align-items: center; gap: 8px;
  height: 40px; padding: 0 10px; cursor: pointer;
  font-size: 12.5px; position: relative;
  border-top: 1px solid #f0f0f0;
}
.er-prow:first-child, .er-frow:first-child { border-top: 0; }
.er-prow:hover, .er-frow:hover { background: #f5f7fa; }
.er-prow.is-mapped, .er-frow.is-mapped { background: #f0fff4; }
.er-prow.is-sel, .er-frow.is-sel { background: #e6f7ff !important; box-shadow: inset 3px 0 0 var(--bl-primary); }

.er-row-no {
  width: 20px; height: 20px; border-radius: 50%;
  background: #1677ff; color: #fff;
  font-size: 11px; font-weight: 600;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.er-prop-ic {
  width: 16px; height: 16px; border-radius: 3px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.er-dt-ic {
  font-size: 10px; color: var(--bl-text-3);
  background: var(--bl-bg-2); padding: 1px 6px; border-radius: 3px;
  font-family: var(--bl-font-mono); flex-shrink: 0;
}
.er-row-text { flex: 1; min-width: 0; color: var(--bl-text-1); }

/* 圆点 */
.er-dot {
  width: 12px; height: 12px; border-radius: 50%;
  background: #d1d5db; flex-shrink: 0; cursor: pointer;
  transition: transform .1s;
  position: relative;
}
.er-dot:hover { transform: scale(1.25); }
.er-dot.is-key { background: #F53F3F; }
.er-dot.is-fk  { background: #1677FF; }
.er-dot.is-no-map { opacity: .35; cursor: not-allowed; }
.er-dot-out { margin-left: auto; }
.er-dot-rel { width: 10px; height: 10px; background: #FF7D00; margin-left: 4px; }

/* —— 模式光标 —— */
.er-canvas.is-mode-select { cursor: default; }
.er-canvas.is-mode-sort .er-prow { cursor: grab; }
.er-canvas.is-mode-sort .er-prow:active { cursor: grabbing; }
.er-canvas.is-mode-connect .er-dot-out:not(.is-no-map) { cursor: crosshair; }
.er-canvas.is-mode-connect .er-dot-out:not(.is-no-map):hover { transform: scale(1.4); box-shadow: 0 0 0 4px rgba(22,119,255,0.18); }
.er-canvas.is-mode-delete .er-line { cursor: pointer; }
.er-canvas.is-mode-delete .er-svg .er-line:hover { stroke-width: 4 !important; }

/* —— 选中映射线时双端高亮 (浅蓝) —— */
.er-prow.is-hl, .er-frow.is-hl { background: #e6f7ff !important; }

/* —— 排序模式拖拽插入指示 —— */
.er-sort-hint {
  position: absolute; left: 6px; right: 6px; top: -1px;
  height: 2px; background: var(--bl-primary); border-radius: 1px;
  pointer-events: none;
}
.er-prow { position: relative; }
.er-prow.is-sortable { background-image: linear-gradient(to right, transparent 60%, rgba(22,119,255,0.04)); }

/* 选中信息卡片 */
.er-sel-card {
  position: absolute; right: 16px; top: 16px;
  width: 280px; background: #fff; border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.10);
  z-index: 10; font-size: 12px;
  border: 1px solid var(--bl-divider);
}
.er-sel-hd {
  display: flex; align-items: center;
  padding: 8px 12px; border-bottom: 1px solid var(--bl-divider);
  background: #f8fbff;
}
.er-sel-body { padding: 8px 12px; }
.er-sel-kv { display: flex; gap: 8px; padding: 4px 0; }
.er-sel-k { width: 70px; color: var(--bl-text-3); flex-shrink: 0; }
.er-sel-v { color: var(--bl-text-1); word-break: break-all; }
</style>
