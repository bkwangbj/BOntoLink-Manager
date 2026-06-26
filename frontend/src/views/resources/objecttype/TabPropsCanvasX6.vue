<template>
  <div class="erx-wrap" ref="wrapRef">
    <!-- 工具栏 -->
    <div class="erx-toolbar">
      <div class="erx-seg">
        <button v-for="m in modes" :key="m.k"
                :class="['erx-seg-btn', mode === m.k && 'is-on']" :title="m.label"
                @click="mode = m.k"><span v-html="BL.icon(m.icon, 14)"></span></button>
      </div>
      <div class="erx-tdiv"></div>
      <div class="erx-zoom">
        <button class="erx-zoom-btn" title="缩小" @click="zoomBy(-0.1)">−</button>
        <button class="erx-zoom-val" title="恢复 100%" @click="resetZoom">{{ Math.round(scale * 100) }}%</button>
        <button class="erx-zoom-btn" title="放大" @click="zoomBy(0.1)">+</button>
      </div>
      <button class="erx-tbtn" title="适配视图" @click="fitView"><span v-html="BL.icon('move', 14)"></span></button>
      <button class="erx-tbtn" title="重置布局" @click="rebuild(true)"><span v-html="BL.icon('refresh', 14)"></span></button>
      <button class="erx-tbtn" title="导出 PNG" @click="onExport"><span v-html="BL.icon('share', 14)"></span></button>
      <div class="erx-status">
        当前模式: <b>{{ currentModeLabel }}</b>
        <span style="margin-left:14px">已映射: <b style="color:var(--bl-primary)">{{ mappedCount }} / {{ totalProps }}</b></span>
      </div>
      <button class="erx-tbtn" :title="isFull ? '退出全屏' : '全屏'" @click="toggleFull">
        <span v-html="BL.icon(isFull ? 'minimize' : 'maximize', 14)"></span>
      </button>
    </div>

    <!-- 物理表切换条 (多表: 每表一图; 「全部」= 含表间关联) -->
    <div v-if="datasourcesSorted.length > 1" class="erx-tabs">
      <button :class="['erx-tab', activeTableId === ALL_ID && 'is-on']" title="显示全部物理表 (含表间 JOIN)"
              @click="activeTableId = ALL_ID">
        <span v-html="BL.icon('grid', 12)"></span><span style="margin-left:4px">全部</span>
      </button>
      <button v-for="ds in datasourcesSorted" :key="ds.id"
              :class="['erx-tab', activeTableId === ds.id && 'is-on']"
              :title="ds.physical_table" @click="activeTableId = ds.id">
        <span class="erx-tab-dot" :class="ds.rel_type === 1 ? 'is-main' : 'is-supp'"></span>
        <span class="bl-truncate">{{ ds.physical_table }}</span>
        <span class="bl-muted" style="margin-left:4px">{{ (ds.physical_fields || []).length }}</span>
      </button>
    </div>

    <!-- X6 画布 (仅连线模式显示端口圆点, 其余模式隐藏避免噪点) -->
    <div class="erx-canvas" :class="{ 'is-connect': mode === 'connect' }" ref="containerRef"></div>

    <!-- x6-vue-shape 的传送容器: 让 Vue 节点挂载进 X6 -->
    <TeleportContainer />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Graph } from '@antv/x6'
import { Export } from '@antv/x6/es/plugin/export'  // x6 v3 内置导出插件 (公开包 x6-plugin-export@3 是空包, 用内置子路径)
import { register, getTeleport } from '@antv/x6-vue-shape'
import { BL } from '@/lib/bl.js'
import { resourceApi, classMetaApi } from '@/api'
import ErCardNode from './erNodes/ErCardNode.vue'

const props = defineProps({ classId: { type: String, default: '' } })

/* —— 卡片几何 (必须与 ErCardNode.vue 的 CSS 表头/行高一致) —— */
const HH = 38, ROWH = 36
const OBJ_W = 330, TBL_W = 240
const OBJ_X = 40, OBJ_Y = 40, TBL_X = 40 + OBJ_W + 170, GAP = 30
const PORT_GAP = 12  // 端口外移量: 让连线端点与卡片边框留出间距

/* —— 注册 X6 Vue 节点 (object / table 共用一个组件, 由 data.type 区分) —— */
const SHAPE = 'erx-card-node'
register({ shape: SHAPE, width: OBJ_W, height: 100, component: ErCardNode })
const TeleportContainer = getTeleport()

/* —— 模式 —— */
const modes = [
  { k: 'select',  label: '选择模式 (拖动卡片)', icon: 'navigation' },
  { k: 'sort',    label: '排序模式 (拖属性行调顺序)', icon: 'menu' },
  { k: 'connect', label: '连线模式 (拖端口建映射)', icon: 'link' },
  { k: 'delete',  label: '删除模式 (点连线删除)', icon: 'trash' }
]
const mode = ref('select')
const currentModeLabel = computed(() => modes.find(m => m.k === mode.value)?.label || '—')

/* —— 数据 —— */
const classInfo = ref({})
const properties = ref([])
const datasources = ref([])
const propertiesSorted = computed(() => [...properties.value].sort((a, b) => (a.sort ?? 99999) - (b.sort ?? 99999) || (a.api_name || '').localeCompare(b.api_name || '')))
const datasourcesSorted = computed(() => [...datasources.value].sort((a, b) => (a.rel_type - b.rel_type) || (a.sort - b.sort)))
const totalProps = computed(() => properties.value.length)
const mappedCount = computed(() => properties.value.filter(p => p._mapped).length)

const ALL_ID = '__all__'
const activeTableId = ref('')
const visibleDatasources = computed(() => {
  const list = datasourcesSorted.value
  if (list.length <= 1) return list
  if (activeTableId.value === ALL_ID) return list
  const active = list.find(d => d.id === activeTableId.value)
  return active ? [active] : [list[0]]
})

async function loadDetail() {
  if (!props.classId) { properties.value = []; datasources.value = []; rebuild(); return }
  const r = await resourceApi.classDetail(props.classId).catch(() => null)
  if (!r) return
  classInfo.value = { id: r.id, api_name: r.api_name, display_name: r.display_name, rdfs_label: r.rdfs_label }
  properties.value = (r.properties || []).map(p => ({ ...p, _mapped: !!(p.physical_table && p.physical_column) }))
  datasources.value = (r.classDatasources || []).map(d => {
    const fields = (d.physical_fields || []).map(f => ({
      ...f,
      _mapped: properties.value.some(p => p.physical_table === d.physical_table && p.physical_column === f.name)
    }))
    return { ...d, physical_fields: fields }
  })
  if (activeTableId.value !== ALL_ID && !datasourcesSorted.value.some(d => d.id === activeTableId.value)) {
    activeTableId.value = datasourcesSorted.value[0]?.id || ''
  }
  rebuild(true)
}

/* —— 行内工具 (沿用旧版语义) —— */
function dataTypeShort(t) {
  if (!t) return '?'
  const s = String(t).toLowerCase()
  if (s.includes('string')) return 'abc'
  if (s.includes('int') || s.includes('decimal') || s.includes('double') || s.includes('float') || s === 'number') return '123'
  if (s.includes('bool')) return '✓✗'
  if (s.includes('date') || s.includes('time')) return '⏱'
  return '·'
}
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
function propConnectable(p) { return !(p.prop_type === 'object' || p.prop_type === 'annotation' || p.is_derived) }
function propPortFill(p) {
  if (!propConnectable(p)) return '#e5e6eb'
  if (p.is_key) return '#F53F3F'
  return '#C9CDD4'
}
function fieldPortFill(f) { return f.is_pk ? '#F53F3F' : (f.is_fk ? '#1677FF' : '#C9CDD4') }

/* 字段排序: 已映射在前 */
function sortedFieldsOf(ds) {
  const fields = ds.physical_fields || []
  const order = new Map()
  propertiesSorted.value.forEach((p, idx) => {
    if (p.physical_table === ds.physical_table && p.physical_column) order.set(p.physical_column, idx)
  })
  return [...fields].sort((a, b) => {
    const am = order.has(a.name) ? order.get(a.name) : 99999
    const bm = order.has(b.name) ? order.get(b.name) : 99999
    return am === bm ? (a.name || '').localeCompare(b.name || '') : am - bm
  })
}

/* join_on_keys: "主表字段=附表字段" (无 '=' 则两侧同名) */
function joinKeysOf(s) {
  const first = (s.join_on_keys || '').split(',').map(x => x.trim()).filter(Boolean)[0] || ''
  if (first.includes('=')) { const [m, sp] = first.split('='); return { main: (m || '').trim(), supp: (sp || '').trim() } }
  return { main: first, supp: first }
}
/* 表间 JOIN 关联键字段集合 (dsId -> Set), 让这些字段也长出关联端口 */
const joinAnchorFields = computed(() => {
  const map = {}
  const main = datasources.value.find(d => d.rel_type === 1)
  const supps = datasources.value.filter(d => d.rel_type !== 1)
  const add = (id, n) => { if (id && n) (map[id] || (map[id] = new Set())).add(n) }
  supps.forEach(s => { const { main: mk, supp: sk } = joinKeysOf(s); if (main) add(main.id, mk); add(s.id, sk) })
  return map
})
function showRelDot(ds, f) {
  if (f.is_pk || f.is_fk) return true
  const set = joinAnchorFields.value[ds.id]
  return !!(set && set.has(f.name))
}

/* —— X6 图实例 —— */
let graph = null
const containerRef = ref(null)
const wrapRef = ref(null)
const scale = ref(1)

function initGraph() {
  graph = new Graph({
    container: containerRef.value,
    autoResize: true,
    background: { color: '#f8f9fa' },
    grid: { visible: true, size: 20, type: 'dot', args: { color: '#e5e7eb', thickness: 1 } },
    panning: { enabled: true, eventTypes: ['leftMouseDown'] },
    mousewheel: { enabled: true, modifiers: null, minScale: 0.4, maxScale: 2 },
    interacting: () => ({ nodeMovable: mode.value === 'select' }),
    connecting: {
      snap: { radius: 24 },
      allowBlank: false, allowLoop: false, allowNode: false, allowEdge: false, allowMulti: false,
      highlight: true,
      validateMagnet: () => mode.value === 'connect',
      validateConnection: ({ sourceMagnet, targetMagnet }) => {
        const sp = sourceMagnet?.getAttribute('port') || ''
        const tp = targetMagnet?.getAttribute('port') || ''
        return sp.startsWith('prop:') && tp.startsWith('field:')
      },
      createEdge: () => graph.createEdge(mappingEdgeStyle('#1677ff'))
    }
  })
  graph.use(new Export())
  graph.on('scale', ({ sx }) => { scale.value = sx })
  graph.on('edge:connected', onEdgeConnected)
  graph.on('edge:click', onEdgeClick)
}

function mappingEdgeStyle(stroke) {
  return { attrs: { line: { stroke, strokeWidth: 2, targetMarker: null, sourceMarker: null } }, zIndex: -1, router: { name: 'normal' }, connector: { name: 'rounded' } }
}

/* —— 重建全部节点/边 —— */
function rebuild(resetView = false) {
  if (!graph) return
  graph.clearCells()
  if (!classInfo.value.id) return

  // 对象类卡片
  const propsList = propertiesSorted.value
  const objH = HH + Math.max(propsList.length, 1) * ROWH
  const objNode = graph.addNode({
    shape: SHAPE, id: 'obj', x: OBJ_X, y: OBJ_Y, width: OBJ_W, height: objH,
    data: {
      type: 'object',
      mode: mode.value,
      onReorder,
      title: `${classInfo.value.display_name || classInfo.value.api_name || '—'} (${classInfo.value.api_name || ''})`,
      countText: `${propsList.length} 个属性`,
      rows: propsList.map((p, i) => ({
        key: p.id, no: i + 1, color: propIconColor(p),
        iconSvg: BL.icon(propIcon(p), 10, '#fff'),
        dt: dataTypeShort(p.data_type),
        label: p.display_name || p.rdfs_label || p.api_name, api: p.api_name,
        mapped: p._mapped
      }))
    },
    ports: {
      groups: { out: { position: 'absolute', attrs: { circle: { r: 6, magnet: true, stroke: '#fff', strokeWidth: 2 } } } },
      items: propsList.map((p, i) => ({
        id: `prop:${p.id}`, group: 'out',
        args: { x: OBJ_W + PORT_GAP, y: HH + i * ROWH + ROWH / 2 },
        attrs: { circle: { fill: propPortFill(p) } }
      }))
    }
  })

  // 物理表卡片 (当前可见表)
  let cy = OBJ_Y
  const tblNodeIds = {}
  for (const ds of visibleDatasources.value) {
    const fields = sortedFieldsOf(ds)
    const h = HH + Math.max(fields.length, 1) * ROWH
    const inItems = fields.map((f, i) => ({
      id: `field:${ds.id}:${f.name}`, group: 'in',
      args: { x: -PORT_GAP, y: HH + i * ROWH + ROWH / 2 },
      attrs: { circle: { fill: fieldPortFill(f) } }
    }))
    const relItems = fields.map((f, i) => ({ f, i })).filter(({ f }) => showRelDot(ds, f)).map(({ f, i }) => ({
      id: `rel:${ds.id}:${f.name}`, group: 'rel',
      args: { x: TBL_W + PORT_GAP, y: HH + i * ROWH + ROWH / 2 },
      attrs: { circle: { fill: '#FF7D00' } }
    }))
    const node = graph.addNode({
      shape: SHAPE, id: `tbl:${ds.id}`, x: TBL_X, y: cy, width: TBL_W, height: h,
      data: {
        type: 'table', relType: ds.rel_type,
        title: `${ds.physical_table} (${ds.table_label || ds.alias || ''})`,
        countText: `${fields.length} 字段`,
        rows: fields.map(f => ({ key: f.name, name: f.name, dt: dataTypeShort(f.data_type), mapped: f._mapped }))
      },
      ports: {
        groups: {
          in: { position: 'absolute', attrs: { circle: { r: 6, magnet: true, stroke: '#fff', strokeWidth: 2 } } },
          rel: { position: 'absolute', attrs: { circle: { r: 5, magnet: false, stroke: '#fff', strokeWidth: 1.5 } } }
        },
        items: [...inItems, ...relItems]
      }
    })
    tblNodeIds[ds.id] = node.id
    cy += h + GAP
  }

  // 映射边: 属性 -> 字段
  for (const p of properties.value) {
    if (!p.physical_table || !p.physical_column) continue
    const ds = visibleDatasources.value.find(d => d.physical_table === p.physical_table)
    if (!ds) continue
    const isKey = p.is_key === 1 || p.is_key === true
    graph.addEdge({
      ...mappingEdgeStyle(isKey ? '#F53F3F' : '#1677ff'),
      source: { cell: 'obj', port: `prop:${p.id}` },
      target: { cell: `tbl:${ds.id}`, port: `field:${ds.id}:${p.physical_column}` },
      data: { kind: 'mapping', propId: p.id, dsId: ds.id, column: p.physical_column }
    })
  }

  // 表间关联边: 主表 rel -> 附表 rel (仅多表同框时)
  const main = visibleDatasources.value.find(d => d.rel_type === 1)
  const supps = visibleDatasources.value.filter(d => d.rel_type !== 1)
  if (main) {
    supps.forEach((s, idx) => {
      const { main: mk, supp: sk } = joinKeysOf(s)
      if (!mk || !sk) return
      const srcId = `rel:${main.id}:${mk}`, tgtId = `rel:${s.id}:${sk}`
      const hasSrc = graph.getCellById(`tbl:${main.id}`)?.hasPort?.(srcId)
      const hasTgt = graph.getCellById(`tbl:${s.id}`)?.hasPort?.(tgtId)
      if (!hasSrc || !hasTgt) return
      graph.addEdge({
        source: { cell: `tbl:${main.id}`, port: srcId },
        target: { cell: `tbl:${s.id}`, port: tgtId },
        attrs: { line: { stroke: '#FF7D00', strokeWidth: 2, targetMarker: { name: 'block', size: 6 } } },
        labels: [{ attrs: { label: { text: String(idx + 1), fill: '#fff' }, body: { fill: '#FF7D00', stroke: 'none' } } }],
        connector: { name: 'smooth' }, zIndex: -1,
        data: { kind: 'relation', mainId: main.id, suppId: s.id }
      })
    })
  }

  if (resetView) nextTick(() => fitView())
}

/* —— 连线模式: 端口拖拽建映射 —— */
async function onEdgeConnected({ edge, isNew }) {
  if (!isNew) return
  const sp = edge.getSourcePortId() || ''
  const tp = edge.getTargetPortId() || ''
  graph.removeEdge(edge) // 真正的边由 rebuild 重新生成
  if (!sp.startsWith('prop:') || !tp.startsWith('field:')) return
  const propId = sp.slice(5)
  const [, dsId, ...rest] = tp.split(':')
  const column = rest.join(':')
  const p = properties.value.find(x => x.id === propId)
  const ds = datasources.value.find(d => d.id === dsId)
  const f = ds && (ds.physical_fields || []).find(x => x.name === column)
  if (!p || !ds || !f) return
  if (!propConnectable(p)) { BL.warning('关联/注释/派生属性禁止连线'); return }
  if (p._mapped) { BL.warning('该属性已映射, 请先删除原连线'); return }
  if (f._mapped) { BL.warning('该字段已被映射'); return }
  try {
    await classMetaApi.updateProp(p.id, { ...p, _mapped: undefined, physical_table: ds.physical_table, physical_column: f.name, class_ds_id: ds.id })
    BL.success(`映射已创建: ${p.api_name} → ${ds.physical_table}.${f.name}`)
    await loadDetail()
  } catch (e) { BL.error(e?.msg || '映射保存失败') }
}

/* —— 删除模式: 点连线删除 —— */
async function onEdgeClick({ edge }) {
  const data = edge.getData() || {}
  if (mode.value !== 'delete') return
  if (data.kind === 'mapping') {
    const p = properties.value.find(x => x.id === data.propId)
    if (!p) return
    const ok = await BL.confirm({ title: '删除映射', content: `确定删除「${p.api_name} → ${data.column}」的映射？`, danger: true, okText: '删除' })
    if (!ok) return
    try {
      await classMetaApi.updateProp(p.id, { ...p, _mapped: undefined, physical_table: null, physical_column: null, class_ds_id: null })
      BL.success('映射已删除'); await loadDetail()
    } catch (e) { BL.error(e?.msg || '删除失败') }
  } else if (data.kind === 'relation') {
    BL.info('表间关联请到「对象 → 数据源」tab 修改关联键')
  }
}

/* —— 排序模式: 对象卡片内拖拽属性行 → 提交新顺序 —— */
async function onReorder(ids) {
  if (!ids || !ids.length || !props.classId) return
  try {
    await classMetaApi.reorderProps(props.classId, ids)
    BL.success('属性顺序已更新')
    await loadDetail()
  } catch (e) { BL.error(e?.msg || '排序保存失败') }
}

/* —— 导出 PNG (x6 内置 export 插件) —— */
function onExport() {
  if (!graph) return
  graph.exportPNG(`属性关系图_${classInfo.value.api_name || 'object'}.png`, { padding: 24, backgroundColor: '#ffffff', quality: 1 })
  BL.success('已导出 PNG')
}

/* —— 缩放 / 适配 —— */
function zoomBy(d) { graph && graph.zoom(d) }
function resetZoom() { graph && graph.zoomTo(1) }
function fitView() { graph && graph.zoomToFit({ padding: 40, maxScale: 1.5, minScale: 0.4 }) }

/* —— 全屏 —— */
const isFull = ref(false)
function toggleFull() {
  const el = wrapRef.value
  if (!el) return
  if (!document.fullscreenElement) el.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.()
}
function onFsChange() { isFull.value = document.fullscreenElement === wrapRef.value }

watch(activeTableId, () => rebuild(true))
watch(() => props.classId, () => loadDetail())
/* 切模式: 只更新对象节点的 mode (让属性行进入/退出可拖排序态), 不整体重建以保留拖动位置 */
watch(mode, () => {
  const obj = graph && graph.getCellById('obj')
  if (obj) obj.setData({ mode: mode.value })
})

onMounted(() => {
  initGraph()
  document.addEventListener('fullscreenchange', onFsChange)
  loadDetail()
})
onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFsChange)
  graph && graph.dispose()
})
</script>

<style scoped>
.erx-wrap { display: flex; flex-direction: column; height: 100%; min-height: 480px; }
.erx-wrap:fullscreen { background: var(--bl-bg-1); height: 100vh; }

.erx-toolbar {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; background: var(--bl-bg-2);
  border-bottom: 1px solid var(--bl-divider); flex-shrink: 0;
}
.erx-seg { display: inline-flex; height: 28px; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; background: var(--bl-bg-1); }
.erx-seg-btn { width: 32px; border: 0; border-left: 1px solid var(--bl-border); background: transparent; cursor: pointer; color: var(--bl-text-2); display: inline-flex; align-items: center; justify-content: center; }
.erx-seg-btn:first-child { border-left: 0; }
.erx-seg-btn:hover { background: var(--bl-bg-2); color: var(--bl-primary); }
.erx-seg-btn.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.erx-tbtn { width: 30px; height: 30px; border: 0; background: transparent; border-radius: 4px; cursor: pointer; color: var(--bl-text-2); display: inline-flex; align-items: center; justify-content: center; }
.erx-tbtn:hover { background: var(--bl-bg-1); color: var(--bl-primary); }
.erx-tdiv { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 4px; }
.erx-zoom { display: inline-flex; height: 28px; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; background: var(--bl-bg-1); }
.erx-zoom-btn, .erx-zoom-val { height: 100%; border: 0; background: transparent; cursor: pointer; color: var(--bl-text-2); display: inline-flex; align-items: center; justify-content: center; }
.erx-zoom-btn { width: 26px; font-weight: 600; font-size: 15px; }
.erx-zoom-val { min-width: 50px; font-size: 12px; border-left: 1px solid var(--bl-border); border-right: 1px solid var(--bl-border); color: var(--bl-text-1); }
.erx-zoom-btn:hover, .erx-zoom-val:hover { background: var(--bl-primary-soft); color: var(--bl-primary); }
.erx-status { font-size: 12px; color: var(--bl-text-2); margin-left: auto; }
.erx-status b { color: var(--bl-primary); }

.erx-tabs { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; padding: 6px 10px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.erx-tab { display: inline-flex; align-items: center; gap: 4px; max-width: 200px; height: 26px; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 13px; background: var(--bl-bg-2); color: var(--bl-text-2); font-size: 12px; cursor: pointer; }
.erx-tab:hover { background: var(--bl-bg-1); color: var(--bl-text-1); }
.erx-tab.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); border-color: var(--bl-primary); }
.erx-tab-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.erx-tab-dot.is-main { background: #00B42A; }
.erx-tab-dot.is-supp { background: #FF7D00; }

.erx-canvas { flex: 1; position: relative; overflow: hidden; min-height: 400px; }

/* 端口圆点默认隐藏 (映射线仍跟随卡片), 仅连线模式显示供拖拽吸附 */
.erx-canvas :deep(.x6-port-body) { opacity: 0; pointer-events: none; transition: opacity .12s; }
.erx-canvas.is-connect :deep(.x6-port-body) { opacity: 1; pointer-events: auto; }
</style>
