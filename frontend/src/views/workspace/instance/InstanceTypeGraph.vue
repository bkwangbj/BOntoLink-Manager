<template>
  <div class="itg-root" ref="root">
    <div class="itg-bar">
      <button class="itg-back" @click="$emit('back')" v-html="iconText('chevronLeft','返回卡片')"></button>
      <div class="itg-search">
        <span class="itg-search-ic" v-html="BL.icon('search', 12, 'var(--bl-text-3)')"></span>
        <input v-model="q" placeholder="搜索本体对象" @input="searchGraph" />
        <button v-if="q" class="itg-search-x" @click="q=''; searchGraph()" v-html="BL.icon('x', 10)"></button>
      </div>
      <button class="itg-ic" title="重置" @click="resetView" v-html="BL.icon('refresh', 15)"></button>
      <span class="itg-title bl-truncate">{{ title }} · 关系图</span>
      <span class="bl-grow"></span>
      <div class="itg-layout-dd" v-click-outside="()=>layoutMenu=false">
        <button class="itg-layout-btn" @click.stop="layoutMenu=!layoutMenu">
          <span>{{ curLayoutLabel }}</span><span v-html="BL.icon('chevronDown', 11)"></span>
        </button>
        <div v-if="layoutMenu" class="itg-layout-menu" @click.stop>
          <div v-for="l in LAYOUTS" :key="l.key" :class="['itg-layout-item', curLayout===l.key && 'is-on']"
               @click="applyLayout(l.key); layoutMenu=false">{{ l.label }}</div>
        </div>
      </div>
      <button class="itg-ic" title="导出图片" @click="exportImg" v-html="BL.icon('share', 15)"></button>
    </div>
    <div class="itg-canvas-wrap">
      <div ref="canvas" class="itg-canvas"></div>
      <!-- 悬浮竖排工具条 -->
      <aside class="itg-tools">
        <button class="itg-tool" title="适配视图" @click="fitView" v-html="BL.icon('move', 14)"></button>
        <div class="itg-tool-div"></div>
        <button class="itg-tool" title="放大" @click="zoomBy(1.2)" v-html="BL.icon('zoomIn', 14)"></button>
        <button class="itg-tool" title="缩小" @click="zoomBy(0.8)" v-html="BL.icon('zoomOut', 14)"></button>
        <button class="itg-tool" title="回到初始布局" @click="resetView" v-html="BL.icon('refresh', 14)"></button>
        <div class="itg-tool-div"></div>
        <button class="itg-tool" title="全屏" @click="toggleFull" v-html="BL.icon('maximize', 14)"></button>
      </aside>
      <!-- 关系图例(左下竖排,默认收起) -->
      <div class="itg-legend">
        <button v-if="!legendOpen" class="itg-legend-toggle" @click="legendOpen=true">图例 <span v-html="BL.icon('chevronRight', 12)"></span></button>
        <template v-else>
          <div class="itg-legend-hd"><span>关系类型</span><button class="itg-legend-x" @click="legendOpen=false" v-html="BL.icon('chevronDown', 12)"></button></div>
          <span v-for="r in RELATIONS" :key="r.key" class="itg-leg"><span class="itg-mark" :style="{ background:r.color }"></span>{{ r.cn }}</span>
        </template>
      </div>
      <div v-if="!loading && !nodeCount" class="itg-empty bl-empty">该分组下无对象类型关系</div>
      <!-- 全局节点详情抽屉 -->
      <ClassDetailDrawer :node="drawerNode" :relations="drawerRelations"
                         @close="drawerNode=null" @edit="onEdit" @explore="onExplore" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import G6 from '@antv/g6'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { graphApi } from '@/api'
import ClassDetailDrawer from '@/components/ClassDetailDrawer.vue'

const props = defineProps({
  title: { type: String, default: '' },
  typeIds: { type: Array, default: () => [] }
})
const emit = defineEmits(['back', 'explore'])

function iconText (ic, t) { return `${BL.icon(ic, 13)}<span style="margin-left:4px">${t}</span>` }

const RELATIONS = [
  { key: 'link', cn: '普通链接', color: '#6b7280', dash: [0], width: 1.8 },
  { key: 'sub', cn: '父子类', color: '#3b82f6', dash: [0], width: 2 },
  { key: 'eq', cn: '等价类', color: '#10b981', dash: [5, 5], width: 2 },
  { key: 'union', cn: '并集类', color: '#8b5cf6', dash: [2, 3], width: 2 },
  { key: 'dis', cn: '互斥不相交', color: '#ef4444', dash: [0], width: 2.5 }
]
const RMAP = Object.fromEntries(RELATIONS.map(r => [r.key, r]))
const LAYOUTS = [
  { key: 'force', label: '力导向', cfg: { type: 'force', preventOverlap: true, nodeSize: 130, nodeSpacing: 18, linkDistance: 130, gravity: 12, alphaDecay: 0.022 } },
  { key: 'dagre', label: '分层', cfg: { type: 'dagre', rankdir: 'LR', nodesep: 14, ranksep: 45 } },
  { key: 'circular', label: '环形图', cfg: { type: 'circular', ordering: 'degree', preventOverlap: true, nodeSize: 130, nodeSpacing: 8 } }
]
const curLayout = ref('force')
const curLayoutLabel = computed(() => (LAYOUTS.find(l => l.key === curLayout.value) || LAYOUTS[0]).label)
const layoutMenu = ref(false)
const legendOpen = ref(false)
const q = ref('')

function iconDataUri (name, size, color) {
  let svg = BL.icon(name, size, color) || ''
  if (svg.indexOf('xmlns') < 0) svg = svg.replace('<svg ', '<svg xmlns="http://www.w3.org/2000/svg" ')
  return 'data:image/svg+xml,' + encodeURIComponent(svg)
}

let registered = false
function ensureNode () {
  if (registered) return
  registered = true
  G6.registerNode('itg-node', {
    draw (cfg, group) {
      const color = cfg._color || '#165DFF'
      const cn = cfg.label || '', en = cfg.apiName || ''
      const ICON = 20, PAD = 10, GAP = 8
      const textW = Math.max(cn.length * 13, en.length * 6.5, 56)
      const w = PAD + ICON + GAP + textW + PAD, h = 40
      const rect = group.addShape('rect', {
        attrs: { x: -w / 2, y: -h / 2, width: w, height: h, radius: 6, fill: '#fff', stroke: color, lineWidth: 1.6,
          cursor: 'pointer', shadowColor: 'rgba(0,0,0,0.08)', shadowBlur: 4, shadowOffsetY: 1 },
        name: 'itg-rect', draggable: true
      })
      const bx = -w / 2 + PAD
      group.addShape('rect', { attrs: { x: bx, y: -ICON / 2, width: ICON, height: ICON, radius: 5, fill: color + '22' }, name: 'itg-ic-bg' })
      group.addShape('image', { attrs: { x: bx + 3.5, y: -6.5, width: 13, height: 13, img: iconDataUri(cfg._icon || 'cube', 13, color) }, name: 'itg-ic' })
      const tx = bx + ICON + GAP
      group.addShape('text', { attrs: { x: tx, y: en ? -6 : 0, text: cn, fontSize: 12, fontWeight: 600, fill: '#1d2129', textAlign: 'left', textBaseline: 'middle', cursor: 'pointer' }, name: 'itg-cn' })
      if (en) group.addShape('text', { attrs: { x: tx, y: 9, text: en, fontSize: 9, fill: '#86909c', textAlign: 'left', textBaseline: 'middle', cursor: 'pointer' }, name: 'itg-en' })
      return rect
    },
    setState (name, value, item) {
      const grp = item.getContainer()
      const rect = grp.find(e => e.get('name') === 'itg-rect')
      if (name === 'dim') { grp.get('children').forEach(s => s.attr('opacity', value ? 0.14 : 1)); return }
      if (!rect) return
      if (name === 'highlight' || name === 'selected') {
        if (value) { rect.attr('lineWidth', 3); rect.attr('shadowColor', 'rgba(22,93,255,0.45)'); rect.attr('shadowBlur', 12) }
        else { rect.attr('lineWidth', 1.6); rect.attr('shadowColor', 'rgba(0,0,0,0.08)'); rect.attr('shadowBlur', 4) }
      }
    }
  }, 'single-node')
}

const canvas = ref(null)
const root = ref(null)
const loading = ref(false)
const nodeCount = ref(0)
let graph = null

function destroy () { if (graph && !graph.get('destroyed')) { graph.destroy(); graph = null } }

async function build () {
  ensureNode(); destroy()
  const el = canvas.value; if (!el) return
  loading.value = true
  let ont = { nodes: [], edges: [] }
  try { ont = await graphApi.ontology() } catch { ont = { nodes: [], edges: [] } }
  loading.value = false
  const idSet = props.typeIds && props.typeIds.length ? new Set(props.typeIds) : null
  const nodes = (ont.nodes || []).filter(n => !idSet || idSet.has(n.id))
  const keep = new Set(nodes.map(n => n.id))
  const edges = (ont.edges || []).filter(e => keep.has(e.source) && keep.has(e.target))
  nodeCount.value = nodes.length
  nodeName = {}; nodes.forEach(n => { nodeName[n.id] = n.label || n.apiName || n.id })
  allEdges = edges
  drawerNode.value = null
  if (!nodes.length) return
  graph = new G6.Graph({
    container: el, width: el.clientWidth || 600, height: el.clientHeight || 400,
    fitView: true, fitViewPadding: 40, animate: true, minZoom: 0.1, maxZoom: 3,
    modes: { default: ['drag-canvas', 'zoom-canvas', 'drag-node'] },
    defaultNode: { type: 'itg-node' },
    nodeStateStyles: {},
    edgeStateStyles: { dim: { opacity: 0.07 }, highlight: { lineWidth: 3.4, opacity: 1 } },
    layout: (LAYOUTS.find(l => l.key === curLayout.value) || LAYOUTS[0]).cfg
  })
  const gEdges = edges.map((e, i) => {
    const r = RMAP[e.kind] || RMAP.link
    return { id: 'e' + i, source: e.source, target: e.target, kind: e.kind,
      label: e.kind === 'link' ? (e.label || '') : '',
      style: { stroke: r.color, lineWidth: r.width, lineDash: r.dash, endArrow: e.kind === 'sub' },
      labelCfg: e.kind === 'link' && e.label ? { autoRotate: false, style: { fontSize: 10, fill: '#86909c', background: { fill: 'rgba(255,255,255,0.95)', stroke: '#e5e6eb', lineWidth: 1, padding: [3, 6, 3, 6], radius: 4 } } } : undefined }
  })
  if (G6.Util && G6.Util.processParallelEdges) G6.Util.processParallelEdges(gEdges, 18, 'quadratic', 'cubic-horizontal', 'loop')
  graph.data({
    nodes: nodes.map(n => ({ id: n.id, label: n.label, apiName: n.apiName, _color: n.color || '#165DFF', _icon: n.icon || 'cube', size: [124, 40] })),
    edges: gEdges
  })
  graph.render()
  graph.on('node:click', (evt) => openNodeDrawer(evt.item.getModel().id))
  graph.on('canvas:click', () => { clearStates(); drawerNode.value = null })
}

/* —— 节点详情抽屉 —— */
const router = useRouter()
const drawerNode = ref(null)
const drawerRelations = ref([])
let nodeName = {}, allEdges = []
function openNodeDrawer (id) {
  const m = graph.findById(id) && graph.findById(id).getModel()
  drawerNode.value = { id, label: m ? m.label : id, apiName: m ? m.apiName : '' }
  drawerRelations.value = allEdges
    .filter(e => e.source === id || e.target === id)
    .map(e => ({ kind: e.kind, name: nodeName[e.source === id ? e.target : e.source] || (e.source === id ? e.target : e.source) }))
}
function onEdit (n) { router.push({ path: '/resources/object-types', query: { openId: n.id } }) }
function onExplore (n) { emit('explore', n.id); drawerNode.value = null }

/* —— 操作 —— */
function clearStates () {
  if (!graph || graph.get('destroyed')) return
  graph.getNodes().forEach(n => { graph.setItemState(n, 'highlight', false); graph.setItemState(n, 'dim', false) })
  graph.getEdges().forEach(e => { graph.setItemState(e, 'highlight', false); graph.setItemState(e, 'dim', false) })
}
function searchGraph () {
  if (!graph || graph.get('destroyed')) return
  const kw = q.value.trim().toLowerCase()
  if (!kw) { clearStates(); return }
  const matched = new Set(graph.getNodes().filter(n => {
    const m = n.getModel(); return ((m.label || '') + (m.apiName || '')).toLowerCase().includes(kw)
  }).map(n => n.getModel().id))
  if (!matched.size) { clearStates(); return }
  const rel = new Set(matched)
  const relEdge = new Set()
  graph.getEdges().forEach(e => { const m = e.getModel(); if (matched.has(m.source) || matched.has(m.target)) { relEdge.add(e.getID()); rel.add(m.source); rel.add(m.target) } })
  graph.getNodes().forEach(n => { const id = n.getModel().id; graph.setItemState(n, 'highlight', matched.has(id)); graph.setItemState(n, 'dim', !rel.has(id)) })
  graph.getEdges().forEach(e => { const on = relEdge.has(e.getID()); graph.setItemState(e, 'highlight', on); graph.setItemState(e, 'dim', !on) })
}
function applyLayout (key) {
  curLayout.value = key
  if (graph && !graph.get('destroyed')) { graph.updateLayout((LAYOUTS.find(l => l.key === key) || LAYOUTS[0]).cfg); setTimeout(() => graph.fitView(40), 350) }
}
function fitView () { if (graph && !graph.get('destroyed')) graph.fitView(40) }
function zoomBy (r) { if (graph && !graph.get('destroyed')) graph.zoom(r, { x: graph.getWidth() / 2, y: graph.getHeight() / 2 }) }
function resetView () { q.value = ''; clearStates(); if (graph && !graph.get('destroyed')) { graph.layout(); setTimeout(() => graph.fitView(40), 300) } }
function exportImg () { graph && graph.downloadFullImage('对象类型关系图', 'image/png', { backgroundColor: '#fff', padding: 20 }) }
function toggleFull () {
  const el = root.value; if (!el) return
  if (!document.fullscreenElement) el.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.()
}
function onFsChange () { nextTick(() => { if (graph && !graph.get('destroyed') && canvas.value) { graph.changeSize(canvas.value.clientWidth || 600, canvas.value.clientHeight || 400); graph.fitView(40) } }) }

let ro = null
onMounted(() => {
  nextTick(build)
  ro = new ResizeObserver(() => { if (graph && !graph.get('destroyed') && canvas.value) { graph.changeSize(canvas.value.clientWidth || 600, canvas.value.clientHeight || 400); graph.fitView(40) } })
  if (canvas.value) ro.observe(canvas.value)
  document.addEventListener('fullscreenchange', onFsChange)
})
onBeforeUnmount(() => { ro && ro.disconnect(); document.removeEventListener('fullscreenchange', onFsChange); destroy() })
watch(() => props.typeIds, () => nextTick(build))

/* click-outside 指令(局部) */
const vClickOutside = {
  mounted (el, binding) { el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }; setTimeout(() => document.addEventListener('mousedown', el.__h), 0) },
  unmounted (el) { document.removeEventListener('mousedown', el.__h) }
}
</script>

<style scoped>
.itg-root { flex: 1; min-width: 0; display: flex; flex-direction: column; min-height: 0; background: var(--bl-bg-2); }
.itg-bar { display: flex; align-items: center; gap: 10px; padding: 8px 14px; border-bottom: 1px solid var(--bl-divider); background: var(--bl-bg-1); flex-shrink: 0; position: relative; z-index: 6; }
.itg-back { display: inline-flex; align-items: center; border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 6px; height: 30px; padding: 0 10px; font-size: 12.5px; color: var(--bl-text-2); cursor: pointer; flex-shrink: 0; }
.itg-back:hover { border-color: var(--bl-primary-border); color: var(--bl-primary); }
.itg-search { position: relative; display: flex; align-items: center; flex-shrink: 0; }
.itg-search-ic { position: absolute; left: 8px; display: inline-flex; }
.itg-search input { width: 200px; height: 30px; box-sizing: border-box; padding: 0 22px 0 26px; border: 1px solid var(--bl-border); border-radius: 6px; font-size: 12.5px; outline: none; }
.itg-search input:focus { border-color: var(--bl-primary); }
.itg-search-x { position: absolute; right: 6px; width: 16px; height: 16px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.itg-title { font-size: 13px; font-weight: 600; color: var(--bl-text-1); max-width: 200px; }
.itg-ic { width: 30px; height: 30px; border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 6px; color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.itg-ic:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.itg-layout-dd { position: relative; flex-shrink: 0; }
.itg-layout-btn { display: inline-flex; align-items: center; gap: 4px; height: 30px; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; font-size: 12.5px; }
.itg-layout-btn:hover { border-color: var(--bl-primary-border); }
.itg-layout-menu { position: absolute; top: 100%; right: 0; margin-top: 4px; min-width: 96px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 6px 18px rgba(0,0,0,.14); padding: 4px; z-index: 20; }
.itg-layout-item { padding: 6px 10px; border-radius: 6px; font-size: 12.5px; color: var(--bl-text-2); cursor: pointer; white-space: nowrap; }
.itg-layout-item:hover:not(.is-on) { background: var(--bl-bg-hover); }
.itg-layout-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }

.itg-canvas-wrap { flex: 1; min-height: 0; position: relative; }
.itg-canvas { position: absolute; inset: 0; }
.itg-tools { position: absolute; top: 12px; left: 12px; display: flex; flex-direction: column; gap: 2px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,.08); padding: 4px; z-index: 5; }
.itg-tool { width: 28px; height: 28px; border: 0; background: transparent; color: var(--bl-text-2); border-radius: 6px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.itg-tool:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }
.itg-tool-div { height: 1px; background: var(--bl-divider); margin: 2px 4px; }

.itg-legend { position: absolute; bottom: 12px; left: 12px; display: flex; flex-direction: column; gap: 6px; background: rgba(255,255,255,.9); border: 1px solid var(--bl-border); border-radius: 8px; padding: 8px 10px; backdrop-filter: blur(2px); z-index: 5; }
.itg-legend-toggle { display: inline-flex; align-items: center; gap: 4px; border: 0; background: transparent; color: var(--bl-text-2); font-size: 12px; cursor: pointer; padding: 0; }
.itg-legend-toggle:hover { color: var(--bl-primary); }
.itg-legend-hd { display: flex; align-items: center; justify-content: space-between; font-size: 11px; color: var(--bl-text-3); margin-bottom: 1px; }
.itg-legend-x { border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; padding: 0; display: inline-flex; }
.itg-leg { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); }
.itg-mark { width: 14px; height: 3px; border-radius: 2px; }
.itg-empty { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; }
</style>
