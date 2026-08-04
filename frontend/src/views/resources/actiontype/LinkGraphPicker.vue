<template>
  <Teleport to="body">
    <div v-if="open" class="lgp-mask" @click.self="close">
      <div class="lgp-modal">
        <div class="lgp-hd">
          <span v-html="BL.icon('network', 14)"></span>
          <span style="margin-left:6px">关联图谱 · 选择关联路径</span>
          <span class="bl-muted lgp-sub">从「{{ startName }}」出发的一跳关联</span>
          <span style="flex:1"></span>
          <div class="lgp-search">
            <span class="bl-muted" v-html="BL.icon('search', 12)"></span>
            <input class="bl-input bl-input-sm" v-model="kw" placeholder="按关联/对象名筛选…" />
          </div>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="close" v-html="BL.icon('x', 14)"></button>
        </div>

        <div class="lgp-body">
          <aside class="lgp-toolbar">
            <button class="lgp-tool" title="重新布局" @click="relayout" v-html="BL.icon('grid', 14)"></button>
            <button class="lgp-tool" title="适配视图" @click="fitView" v-html="BL.icon('move', 14)"></button>
            <div class="lgp-tool-div"></div>
            <button class="lgp-tool" title="放大" @click="zoomBy(0.1)" v-html="BL.icon('zoomIn', 14)"></button>
            <button class="lgp-tool" title="缩小" @click="zoomBy(-0.1)" v-html="BL.icon('zoomOut', 14)"></button>
            <button class="lgp-tool" title="1:1" @click="resetView" v-html="BL.icon('refresh', 14)"></button>
          </aside>

          <svg class="lgp-svg" :viewBox="`0 0 ${VW} ${VH}`" preserveAspectRatio="xMidYMid meet"
               @mousedown="onCanvasDown" @wheel.prevent="onWheel">
            <defs>
              <marker id="lgp-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
                <path d="M0 0L10 5L0 10z" fill="#86909c" />
              </marker>
              <marker id="lgp-arrow-on" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
                <path d="M0 0L10 5L0 10z" fill="#165DFF" />
              </marker>
            </defs>
            <g :transform="`translate(${pan.x},${pan.y}) scale(${zoom})`">
              <!-- 连线 = 一条链接类型 -->
              <g v-for="e in edges" :key="e.key"
                 :class="['lgp-edge-g', e.code === picked && 'is-on', hoverKey === e.key && 'is-hot', activeNodeKey === e.nodeKey && 'is-peer-on']"
                 @mouseenter="hoverKey = e.key" @mouseleave="hoverKey = ''"
                 @click.stop="pick(e)" @dblclick.stop="pick(e); confirm()">
                <path :d="e.d" class="lgp-edge" fill="none"
                      :marker-end="e.code === picked ? 'url(#lgp-arrow-on)' : 'url(#lgp-arrow)'" />
                <path :d="e.d" class="lgp-edge-hit" />
                <text :x="e.lx" :y="e.ly" class="lgp-edge-lbl" text-anchor="middle">{{ e.label }}</text>
                <text :x="e.lx" :y="e.ly + 12" class="lgp-edge-card" text-anchor="middle">{{ e.card }}</text>
              </g>

              <!-- 起始对象 -->
              <g :transform="`translate(${center.x - CW/2},${center.y - CH/2})`" class="lgp-node-g is-center">
                <rect :width="CW" :height="CH" rx="8" class="lgp-center-box" />
                <text :x="CW/2" y="24" class="lgp-node-cn" text-anchor="middle">{{ startName }}</text>
                <text :x="CW/2" y="41" class="lgp-node-en" text-anchor="middle">{{ startApi }}</text>
                <text :x="CW/2" y="-8" class="lgp-node-role" text-anchor="middle">起始对象集</text>
              </g>

              <!-- 对端对象 -->
              <g v-for="n in nodes" :key="n.key"
                 :transform="`translate(${n.x - NW/2},${n.y - NH/2})`"
                 :class="['lgp-node-g', n.key === activeNodeKey && 'is-on']"
                 @click.stop="onNodeClick(n)">
                <rect :width="NW" :height="NH" rx="7" class="lgp-node-box" />
                <text :x="NW/2" y="20" class="lgp-node-cn" text-anchor="middle">{{ cut(n.cn, 11) }}</text>
                <text :x="NW/2" y="35" class="lgp-node-en" text-anchor="middle">{{ cut(n.api, 18) }}</text>
                <text v-if="n.links.length > 1" :x="NW - 8" y="14" class="lgp-node-n" text-anchor="end">{{ n.links.length }} 条</text>
              </g>
            </g>
          </svg>

          <div v-if="!startClassId" class="lgp-empty">请先选择起始对象集</div>
          <div v-else-if="!edges.length" class="lgp-empty">{{ kw ? '无匹配的关联' : `「${startName}」暂无可用的链接关系` }}</div>

          <div class="lgp-tip">点击连线选择关联;双击连线直接确定</div>
        </div>

        <div class="lgp-ft">
          <span v-if="pickedEdge" class="lgp-cur">
            已选:<b>{{ pickedEdge.label }}</b> → {{ pickedEdge.peerName }}
            <span class="bl-muted" style="margin-left:6px">{{ pickedEdge.card }}</span>
          </span>
          <span v-else class="bl-muted lgp-cur">未选择关联</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-sm" @click="clear">清除关联</button>
          <button class="bl-btn bl-btn-sm" @click="close">取消</button>
          <button class="bl-btn bl-btn-primary bl-btn-sm" :disabled="!picked" @click="confirm">确定</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  modelValue: { type: String, default: '' },          // 已选链接类型编码
  startClassId: { type: String, default: '' },
  classOptions: { type: Array, default: () => [] },   // [{ id, cn, api_name }]
  linkTypes: { type: Array, default: () => [] },      // 原始链接类型列表
})
const emit = defineEmits(['update:open', 'update:modelValue', 'pick'])

const VW = 940, VH = 520
const CW = 200, CH = 60, NW = 150, NH = 46
const CARD = { one_one: '一对一', one_many: '一对多', many_one: '多对一', many_many: '多对多' }

const kw = ref('')
const picked = ref('')
const hoverKey = ref('')
const activeNodeKey = ref('')
const zoom = ref(1)
const pan = reactive({ x: 0, y: 0 })
const center = { x: VW / 2, y: VH / 2 }

const codeOf = l => l.link_type_id || l.linkTypeId || l.id
const classById = id => props.classOptions.find(c => c.id === id) || null
const startName = computed(() => classById(props.startClassId)?.cn || '起始对象')
const startApi = computed(() => classById(props.startClassId)?.api_name || '')

/* 一跳关联: 链接任一端是起始对象, 另一端即对端对象 (自关联两端相同, 单独成节点) */
const relations = computed(() => {
  const sid = props.startClassId
  if (!sid) return []
  const q = kw.value.trim().toLowerCase()
  const out = []
  for (const l of props.linkTypes || []) {
    const lid = l.l_object_type_id || l.lObjectTypeId || ''
    const rid = l.r_object_type_id || l.rObjectTypeId || ''
    if (lid !== sid && rid !== sid) continue
    const peerId = lid === sid ? rid : lid
    if (!peerId) continue
    const peer = classById(peerId)
    const code = codeOf(l)
    const label = l.rdfs_label || l.rdfsLabel || code
    const peerName = peer?.cn || peerId
    if (q && !`${label} ${peerName} ${peer?.api_name || ''}`.toLowerCase().includes(q)) continue
    out.push({
      code, label, peerId, peerName,
      peerApi: peer?.api_name || '',
      selfLink: peerId === sid,
      forward: lid === sid,   // 起始对象在左端 → 箭头指向对端
      card: CARD[`${l.l_cardinality || l.lCardinality || 'one'}_${l.r_cardinality || l.rCardinality || 'one'}`] || '',
    })
  }
  return out
})

/* 同一对端对象的多条链接并成一个节点, 连线各画一条 */
const nodes = computed(() => {
  const map = new Map()
  relations.value.forEach(r => {
    const key = r.selfLink ? `self:${r.code}` : r.peerId
    if (!map.has(key)) map.set(key, { key, classId: r.peerId, cn: r.selfLink ? `${r.peerName} (自关联)` : r.peerName, api: r.peerApi, links: [] })
    map.get(key).links.push(r)
  })
  const list = [...map.values()]
  /* 椭圆环形布局, 超过一圈往外扩, 起点 -90° 让第一个节点落在正上方 */
  const PER_RING = 9
  list.forEach((n, i) => {
    const ring = Math.floor(i / PER_RING)
    const idx = i % PER_RING
    const cnt = Math.min(PER_RING, list.length - ring * PER_RING)
    const a = -Math.PI / 2 + (idx * 2 * Math.PI) / Math.max(cnt, 1) + (ring % 2 ? Math.PI / cnt : 0)
    n.x = center.x + Math.cos(a) * (300 + ring * 170)
    n.y = center.y + Math.sin(a) * (175 + ring * 95)
  })
  return list
})

const edges = computed(() => {
  const out = []
  nodes.value.forEach(n => {
    const m = n.links.length
    n.links.forEach((r, i) => {
      const off = (i - (m - 1) / 2) * 34
      const a = { x: center.x, y: center.y, w: CW, h: CH }
      const b = { x: n.x, y: n.y, w: NW, h: NH }
      const [s, t] = r.forward ? [a, b] : [b, a]
      const p = clip(s, t)
      const mx = (p.x1 + p.x2) / 2, my = (p.y1 + p.y2) / 2
      const dx = p.x2 - p.x1, dy = p.y2 - p.y1
      const len = Math.hypot(dx, dy) || 1
      const cx = mx - (dy / len) * off, cy = my + (dx / len) * off
      out.push({
        key: `${n.key}|${r.code}`, nodeKey: n.key, code: r.code, label: r.label, card: r.card, peerName: r.peerName,
        d: `M${p.x1.toFixed(1)},${p.y1.toFixed(1)} Q${cx.toFixed(1)},${cy.toFixed(1)} ${p.x2.toFixed(1)},${p.y2.toFixed(1)}`,
        lx: 0.25 * p.x1 + 0.5 * cx + 0.25 * p.x2,
        ly: 0.25 * p.y1 + 0.5 * cy + 0.25 * p.y2 - 4,
      })
    })
  })
  return out
})
const pickedEdge = computed(() => edges.value.find(e => e.code === picked.value) || null)

/* 线段从节点外接框中心裁到边框, 端点留出箭头空间 */
function clip(a, b) {
  const ang = Math.atan2(b.y - a.y, b.x - a.x)
  const s = onBox(ang, a.w / 2 + 3, a.h / 2 + 3)
  const e = onBox(ang, b.w / 2 + 8, b.h / 2 + 8)
  return { x1: a.x + s.x, y1: a.y + s.y, x2: b.x - e.x, y2: b.y - e.y }
}
function onBox(ang, hw, hh) {
  const cos = Math.cos(ang), sin = Math.sin(ang)
  const t = Math.min(hw / Math.abs(cos || 1e-6), hh / Math.abs(sin || 1e-6))
  return { x: cos * t, y: sin * t }
}

function pick(e) {
  picked.value = e.code
  activeNodeKey.value = e.nodeKey
}
function onNodeClick(n) {
  activeNodeKey.value = n.key
  if (n.links.length === 1) picked.value = n.links[0].code
  else BL.info(`「${n.cn}」有 ${n.links.length} 条关联, 请点击具体连线选择`)
}
function confirm() {
  if (!picked.value) return
  emit('update:modelValue', picked.value)
  emit('pick', pickedEdge.value)
  close()
}
function clear() {
  emit('update:modelValue', '')
  emit('pick', null)
  close()
}
function close() { emit('update:open', false) }

/* 视图操作 */
function zoomBy(d) { zoom.value = Math.max(0.4, Math.min(2.5, zoom.value + d)) }
function resetView() { zoom.value = 1; pan.x = 0; pan.y = 0 }
function relayout() { resetView(); fitView() }
function fitView() {
  const ns = nodes.value
  if (!ns.length) return resetView()
  const xs = [center.x - CW / 2, center.x + CW / 2, ...ns.map(n => n.x - NW / 2), ...ns.map(n => n.x + NW / 2)]
  const ys = [center.y - CH / 2, center.y + CH / 2, ...ns.map(n => n.y - NH / 2), ...ns.map(n => n.y + NH / 2)]
  const x0 = Math.min(...xs), x1 = Math.max(...xs), y0 = Math.min(...ys), y1 = Math.max(...ys)
  const z = Math.max(0.4, Math.min(1.4, Math.min((VW - 80) / (x1 - x0), (VH - 80) / (y1 - y0))))
  zoom.value = z
  pan.x = VW / 2 - ((x0 + x1) / 2) * z
  pan.y = VH / 2 - ((y0 + y1) / 2) * z
}
let panFrom = null
function onCanvasDown(ev) {
  if (ev.target.closest('.lgp-node-g') || ev.target.closest('.lgp-edge-g')) return
  panFrom = { x: ev.clientX - pan.x, y: ev.clientY - pan.y }
  window.addEventListener('mousemove', onCanvasMove)
  window.addEventListener('mouseup', onCanvasUp)
}
function onCanvasMove(ev) { if (panFrom) { pan.x = ev.clientX - panFrom.x; pan.y = ev.clientY - panFrom.y } }
function onCanvasUp() { panFrom = null; window.removeEventListener('mousemove', onCanvasMove); window.removeEventListener('mouseup', onCanvasUp) }
function onWheel(ev) { zoomBy(-Math.sign(ev.deltaY) * 0.08) }

function cut(s, n) { s = String(s || ''); return s.length > n ? s.slice(0, n) + '…' : s }

watch(() => props.open, v => {
  if (!v) return
  kw.value = ''
  picked.value = props.modelValue || ''
  activeNodeKey.value = edges.value.find(e => e.code === picked.value)?.nodeKey || ''
  resetView()
  fitView()
})
</script>

<style scoped>
.lgp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 1300; }
.lgp-modal { width: 960px; max-width: 94vw; height: 660px; max-height: 90vh; background: var(--bl-bg-1); border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0,0,0,.3); display: flex; flex-direction: column; overflow: hidden; }
.lgp-hd { display: flex; align-items: center; padding: 12px 14px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.lgp-sub { font-size: 12px; font-weight: 400; margin-left: 10px; }
.lgp-search { display: flex; align-items: center; gap: 6px; margin-right: 8px; }
.lgp-search .bl-input { width: 200px; }
.lgp-body { position: relative; flex: 1; min-height: 0; background: var(--bl-bg-0); }
.lgp-svg { width: 100%; height: 100%; cursor: grab; user-select: none; display: block; }
.lgp-svg:active { cursor: grabbing; }
.lgp-empty { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: var(--bl-text-3); pointer-events: none; }
.lgp-tip { position: absolute; left: 12px; bottom: 10px; font-size: 11px; color: var(--bl-text-3); pointer-events: none; }
/* 画布工具栏 */
.lgp-toolbar { position: absolute; top: 10px; left: 10px; z-index: 3; display: flex; flex-direction: column; gap: 2px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px; padding: 4px; box-shadow: var(--bl-shadow-1); }
.lgp-tool { width: 28px; height: 28px; background: transparent; border: 0; border-radius: 4px; color: var(--bl-text-2);
  display: inline-flex; align-items: center; justify-content: center; cursor: pointer; }
.lgp-tool:hover { background: var(--bl-primary-soft); color: var(--bl-primary); }
.lgp-tool-div { height: 1px; background: var(--bl-divider); margin: 2px 0; }
/* 节点 */
.lgp-node-g { cursor: pointer; }
.lgp-center-box { fill: var(--bl-primary-soft); stroke: var(--bl-primary); stroke-width: 2; }
.lgp-node-box { fill: var(--bl-bg-1); stroke: var(--bl-border-strong); stroke-width: 1.5; }
.lgp-node-g:hover .lgp-node-box { stroke: var(--bl-primary); }
.lgp-node-g.is-on .lgp-node-box { fill: var(--bl-primary-soft); stroke: var(--bl-primary); stroke-width: 2; }
.lgp-node-cn { font-size: 12.5px; font-weight: 600; fill: var(--bl-text-1); pointer-events: none; }
.lgp-node-en { font-size: 10px; fill: var(--bl-text-3); font-family: Consolas, Monaco, monospace; pointer-events: none; }
.lgp-node-role { font-size: 10px; fill: var(--bl-text-3); pointer-events: none; }
.lgp-node-n { font-size: 9.5px; fill: var(--bl-primary); pointer-events: none; }
/* 连线 */
.lgp-edge-g { cursor: pointer; }
.lgp-edge { stroke: var(--bl-border-strong); stroke-width: 1.4; transition: stroke-width .12s, stroke .12s; }
.lgp-edge-hit { fill: none; stroke: transparent; stroke-width: 14; pointer-events: stroke; }
.lgp-edge-g:hover .lgp-edge, .lgp-edge-g.is-hot .lgp-edge { stroke: var(--bl-primary); stroke-width: 2.2; }
.lgp-edge-g.is-on .lgp-edge { stroke: var(--bl-primary); stroke-width: 2.6; }
/* 点中多关联对象时, 该对象的候选连线整体提亮, 引导继续点具体那条 */
.lgp-edge-g.is-peer-on .lgp-edge { stroke: var(--bl-primary); stroke-dasharray: 5,3; }
.lgp-edge-g.is-peer-on.is-on .lgp-edge { stroke-dasharray: 0; }
.lgp-edge-lbl { font-size: 11px; fill: var(--bl-text-2); pointer-events: none;
  paint-order: stroke; stroke: var(--bl-bg-0); stroke-width: 3px; stroke-linejoin: round; }
.lgp-edge-card { font-size: 10px; fill: var(--bl-text-3); pointer-events: none;
  paint-order: stroke; stroke: var(--bl-bg-0); stroke-width: 3px; stroke-linejoin: round; }
.lgp-edge-g.is-on .lgp-edge-lbl { fill: var(--bl-primary); font-weight: 600; }
/* 底栏 */
.lgp-ft { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-top: 1px solid var(--bl-divider); }
.lgp-cur { font-size: 12.5px; color: var(--bl-text-1); }
</style>
