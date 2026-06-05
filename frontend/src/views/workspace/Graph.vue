<template>
  <div class="gr-root">
    <!-- 顶部头 (项目标准 48px topbar 风格, 对齐其他页) -->
    <header class="gr-topbar">
      <div class="gr-title-wrap">
        <span class="gr-title-ic" v-html="BL.icon('chart', 14, 'var(--bl-primary)')"></span>
        <span class="gr-title">图谱</span>
        <span class="gr-subtitle">Graph · 行业层级 ↔ 对象本体 双画布联动探索</span>
      </div>
    </header>

    <!-- 主体: 左右分栏 (1/3 + 拖拽分割线 + 2/3), 拖拽区间 33% ~ 67% -->
    <div ref="splitRef" class="gr-split"
         @mousemove="onSplitMove" @mouseup="onSplitUp" @mouseleave="onSplitUp">

      <!-- 左画布: 行业层级图谱 -->
      <section class="gr-pane gr-pane-l" :style="{ flex: `0 0 ${leftPct}%` }">
        <!-- 左工具栏 (48px) -->
        <div class="gr-toolbar">
          <div class="gr-search">
            <span class="gr-search-ic" v-html="BL.icon('search', 12)"></span>
            <input class="bl-input gr-search-input" v-model="qLeft" placeholder="搜索行业 / 领域 / 子领域 / 分组" />
            <button v-if="qLeft" class="gr-search-clear" @click="qLeft=''" v-html="BL.icon('x', 10)"></button>
          </div>
          <button class="bl-btn bl-btn-sm" @click="resetLeft" title="重置视图">
            <span v-html="BL.icon('refresh', 12)"></span><span style="margin-left:4px">重置</span>
          </button>
          <button class="bl-btn bl-btn-sm" @click="exportLeft" title="导出 PNG">
            <span v-html="BL.icon('download', 12)"></span><span style="margin-left:4px">PNG</span>
          </button>
        </div>
        <!-- 左 SVG 画布 -->
        <div class="gr-canvas-wrap" ref="leftWrapRef"
             @mousedown="onPanDown('L', $event)"
             @wheel.prevent="onWheel('L', $event)">
          <svg ref="leftSvgRef" class="gr-svg"
               :viewBox="`0 0 ${LW} ${LH}`" preserveAspectRatio="xMidYMid meet">
            <g :transform="`translate(${L.pan.x},${L.pan.y}) scale(${L.zoom})`">
              <g v-for="e in leftEdges" :key="'le-'+e.id">
                <line :x1="e.x1" :y1="e.y1" :x2="e.x2" :y2="e.y2"
                      stroke="#9ca3af" stroke-width="1" stroke-dasharray="4,3"
                      :class="['gr-edge', highlightedLeftIds.has(e.source) && highlightedLeftIds.has(e.target) && 'is-hl']" />
              </g>
              <g v-for="n in leftViewNodes" :key="'ln-'+n.id"
                 :transform="`translate(${n.x},${n.y})`"
                 :class="['gr-node-g', selectedLeftId === n.id && 'is-selected']"
                 @click.stop="onLeftNodeClick(n)">
                <circle :r="nodeRadius(n)" :fill="LEFT_STYLE[n.kind]?.fill" :stroke="LEFT_STYLE[n.kind]?.stroke" stroke-width="2" />
                <text class="gr-node-lbl" text-anchor="middle" y="4">{{ truncate(n.label, 8) }}</text>
                <text class="gr-node-sub" text-anchor="middle" :y="nodeRadius(n) + 14">{{ KIND_CN[n.kind] }}</text>
              </g>
            </g>
          </svg>
          <!-- 内嵌图例 (左上角 viewport-fixed) -->
          <div class="gr-legend">
            <div class="gr-legend-hd">图例</div>
            <div v-for="k in ['industry','domain','subdomain','group']" :key="k" class="gr-legend-item">
              <span class="gr-legend-dot" :style="{ background: LEFT_STYLE[k].fill, border: '2px solid ' + LEFT_STYLE[k].stroke }"></span>
              <span>{{ KIND_CN[k] }}</span>
            </div>
            <div class="gr-legend-sep"></div>
            <div v-for="r in RELATIONS" :key="'lg-'+r.key" class="gr-legend-item">
              <svg width="22" height="6">
                <line x1="0" y1="3" x2="22" y2="3" :stroke="r.color"
                      :stroke-width="r.width" :stroke-dasharray="r.dash" />
              </svg>
              <span>{{ r.cn }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 拖拽分割线 -->
      <div class="gr-divider" :class="{ 'is-active': isSplitDragging }"
           @mousedown="onSplitDown" title="拖拽调整左右比例 (33% ~ 67%)">
        <div class="gr-divider-grip"></div>
      </div>

      <!-- 右画布: 对象本体图谱 -->
      <section class="gr-pane gr-pane-r" :style="{ flex: `1 1 auto` }">
        <!-- 右工具栏 (48px) -->
        <div class="gr-toolbar">
          <div class="gr-search">
            <span class="gr-search-ic" v-html="BL.icon('search', 12)"></span>
            <input class="bl-input gr-search-input" v-model="qRight" placeholder="搜索本体对象" />
            <button v-if="qRight" class="gr-search-clear" @click="qRight=''" v-html="BL.icon('x', 10)"></button>
          </div>
          <button class="bl-btn bl-btn-sm" @click="resetRight" title="重置视图">
            <span v-html="BL.icon('refresh', 12)"></span><span style="margin-left:4px">重置</span>
          </button>
          <button class="bl-btn bl-btn-sm" @click="exportRight" title="导出 PNG">
            <span v-html="BL.icon('download', 12)"></span><span style="margin-left:4px">PNG</span>
          </button>
          <!-- 5 类关系筛选 -->
          <div class="gr-rel-filter">
            <label v-for="r in RELATIONS" :key="r.key" class="gr-rel-chk">
              <input type="checkbox" v-model="relOn[r.key]" />
              <span class="gr-rel-mark" :style="{ background: r.color }"></span>
              <span class="gr-rel-lbl">{{ r.cn }}</span>
            </label>
          </div>
        </div>
        <!-- 右 SVG 画布 -->
        <div class="gr-canvas-wrap" ref="rightWrapRef"
             @mousedown="onPanDown('R', $event)"
             @wheel.prevent="onWheel('R', $event)">
          <svg ref="rightSvgRef" class="gr-svg"
               :viewBox="`0 0 ${RW} ${RH}`" preserveAspectRatio="xMidYMid meet">
            <g :transform="`translate(${R.pan.x},${R.pan.y}) scale(${R.zoom})`">
              <g v-for="e in rightEdges" :key="'re-'+e.id">
                <line :x1="e.x1" :y1="e.y1" :x2="e.x2" :y2="e.y2"
                      :stroke="RELATION_MAP[e.kind].color"
                      :stroke-width="edgeWidth(e)"
                      :stroke-dasharray="RELATION_MAP[e.kind].dash"
                      :class="['gr-edge', highlightedRightIds.has(e.source) && highlightedRightIds.has(e.target) && 'is-hl']" />
                <text v-if="e.label && e.kind === 'link'"
                      :x="(e.x1 + e.x2) / 2" :y="(e.y1 + e.y2) / 2 - 4"
                      class="gr-edge-lbl" text-anchor="middle">{{ e.label }}</text>
              </g>
              <g v-for="n in rightViewNodes" :key="'rn-'+n.id"
                 :transform="`translate(${n.x},${n.y})`"
                 :class="['gr-node-g', 'gr-class', selectedRightId === n.id && 'is-selected', highlightedRightIds.has(n.id) && 'is-hl']"
                 @click.stop="onRightNodeClick(n)">
                <rect :x="-n.w/2" :y="-n.h/2" :width="n.w" :height="n.h" rx="6"
                      :fill="n.fill" :stroke="n.stroke" stroke-width="1.5" />
                <text class="gr-node-lbl" text-anchor="middle" y="2">{{ truncate(n.label, 10) }}</text>
                <text v-if="n.apiName" class="gr-node-en" text-anchor="middle" y="14">{{ truncate(n.apiName, 14) }}</text>
              </g>
            </g>
          </svg>
          <!-- 右图例: 仅 5 类关系 -->
          <div class="gr-legend">
            <div class="gr-legend-hd">关系图例</div>
            <div v-for="r in RELATIONS" :key="'rg-'+r.key" class="gr-legend-item">
              <svg width="22" height="6">
                <line x1="0" y1="3" x2="22" y2="3" :stroke="r.color"
                      :stroke-width="r.width" :stroke-dasharray="r.dash" />
              </svg>
              <span>{{ r.cn }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 右侧悬浮详情抽屉 (固定定位, 不随画布滚动) -->
    <aside class="gr-drawer" :class="{ 'is-open': !!drawerNode }">
      <div v-if="drawerNode" class="gr-drawer-inner">
        <header class="gr-drawer-hd">
          <div class="gr-drawer-title">
            <div class="gr-drawer-cn">{{ drawerNode.label }}</div>
            <div class="gr-drawer-en bl-mono">{{ drawerNode.apiName || drawerNode.categoryCode || drawerNode.id }}</div>
          </div>
          <button class="gr-drawer-close" title="关闭详情 (Esc / 点画布空白也可关闭)"
                  @click="drawerNode = null">
            <span v-html="BL.icon('x', 16)"></span>
          </button>
        </header>
        <div class="gr-drawer-body">
          <div class="gr-kv"><span class="gr-kv-l">类型</span><span class="gr-kv-r">
            <span class="bl-tag">{{ drawerNode._side === 'L' ? KIND_CN[drawerNode.kind] : '对象类型' }}</span>
          </span></div>
          <div v-if="drawerNode.categoryCode" class="gr-kv">
            <span class="gr-kv-l">命名编码</span><span class="gr-kv-r bl-mono">{{ drawerNode.categoryCode }}</span>
          </div>
          <div v-if="drawerNode._side === 'L' && drawerNode.boundClassIds?.length" class="gr-kv gr-kv-block">
            <div class="gr-kv-l">绑定对象 ({{ drawerNode.boundClassIds.length }})</div>
            <div class="bl-muted gr-kv-text">点击节点已联动到右侧对象图谱;首位对象已居中并高亮</div>
          </div>
          <div v-if="drawerNode._side === 'R'" class="gr-kv gr-kv-block">
            <div class="gr-kv-l">关联关系</div>
            <ul class="gr-rels">
              <li v-for="(r, i) in nodeRelations(drawerNode.id)" :key="'rel-'+i">
                <span class="gr-rel-mark" :style="{ background: RELATION_MAP[r.kind].color }"></span>
                <span>{{ RELATION_MAP[r.kind].cn }}</span>
                <span class="bl-muted" style="margin-left:auto">{{ otherEnd(drawerNode.id, r) }}</span>
              </li>
              <li v-if="!nodeRelations(drawerNode.id).length" class="bl-muted">无关联</li>
            </ul>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { graphApi } from '@/api'

/* ========== 视觉规范 (严格按 图谱6.5.pdf §1.1.1.4) ========== */
// 左节点分级样式
const LEFT_STYLE = {
  industry:  { fill: '#dbeafe', stroke: '#3b82f6', r: 36 },
  domain:    { fill: '#dcfce7', stroke: '#10b981', r: 32 },
  subdomain: { fill: '#fff7ed', stroke: '#f97316', r: 30 },
  group:     { fill: '#f3e8ff', stroke: '#8b5cf6', r: 28 }
}
const KIND_CN = { industry: '行业', domain: '领域', subdomain: '子领域', group: '分组' }

// 右图本体关系 (5 类)
const RELATIONS = [
  { key: 'sub',   cn: '父子类',   color: '#3b82f6', width: 2,   dash: '0' },
  { key: 'eq',    cn: '等价类',   color: '#10b981', width: 2,   dash: '5,5' },
  { key: 'dis',   cn: '互斥不相交', color: '#ef4444', width: 2.5, dash: '0' },
  { key: 'union', cn: '并集类',   color: '#8b5cf6', width: 2,   dash: '2,3' },
  { key: 'link',  cn: '普通链接', color: '#6b7280', width: 1.8, dash: '0' }
]
const RELATION_MAP = Object.fromEntries(RELATIONS.map(r => [r.key, r]))
const relOn = reactive(Object.fromEntries(RELATIONS.map(r => [r.key, true])))

/* ========== 数据 ========== */
const leftData  = ref({ nodes: [], edges: [] })
const rightData = ref({ nodes: [], edges: [] })

async function loadAll() {
  const [lt, ot] = await Promise.all([
    graphApi.industryTree().catch(() => ({ nodes: [], edges: [] })),
    graphApi.ontology().catch(() => ({ nodes: [], edges: [] }))
  ])
  leftData.value  = lt || { nodes: [], edges: [] }
  rightData.value = ot || { nodes: [], edges: [] }
  layoutLeft()
  layoutRight()
}
onMounted(loadAll)

/* ========== 布局算法 ========== */
const LW = 1000, LH = 800     // 左画布逻辑尺寸
const RW = 1400, RH = 900     // 右画布逻辑尺寸
const leftPos = reactive({})
const rightPos = reactive({})

/* 左画布: 自顶向下分层 — industry → domain → subdomain → group */
function layoutLeft() {
  Object.keys(leftPos).forEach(k => delete leftPos[k])
  const byKind = { industry: [], domain: [], subdomain: [], group: [] }
  leftData.value.nodes.forEach(n => byKind[n.kind]?.push(n))
  const yByKind = { industry: 80, domain: 240, subdomain: 440, group: 640 }
  Object.keys(byKind).forEach(k => {
    const arr = byKind[k]
    if (!arr.length) return
    const startX = LW / 2 - (arr.length - 1) * 110 / 2
    arr.forEach((n, i) => {
      leftPos[n.id] = { x: Math.max(60, Math.min(LW - 60, startX + i * 110)), y: yByKind[k] }
    })
  })
}

/* 右画布: 大圆环 + 中心聚焦的 hub-spoke 布局 */
function layoutRight() {
  Object.keys(rightPos).forEach(k => delete rightPos[k])
  const cx = RW / 2, cy = RH / 2
  const nodes = rightData.value.nodes
  if (!nodes.length) return
  // 第一圈: 0..min(12, len) — 圆心半径 300
  const inner = Math.min(12, nodes.length)
  for (let i = 0; i < inner; i++) {
    const a = (i / inner) * Math.PI * 2 - Math.PI / 2
    rightPos[nodes[i].id] = { x: cx + 300 * Math.cos(a), y: cy + 300 * Math.sin(a) }
  }
  // 第二圈: 剩余的圆心半径 480
  const outer = nodes.length - inner
  if (outer > 0) {
    for (let i = 0; i < outer; i++) {
      const a = (i / outer) * Math.PI * 2
      rightPos[nodes[inner + i].id] = { x: cx + 480 * Math.cos(a), y: cy + 480 * Math.sin(a) }
    }
  }
}

/* ========== 视图状态: 缩放 + 平移 (左右独立) ========== */
const L = reactive({ zoom: 1, pan: { x: 0, y: 0 } })
const R = reactive({ zoom: 1, pan: { x: 0, y: 0 } })
function resetLeft()  { L.zoom = 1; L.pan.x = 0; L.pan.y = 0; selectedLeftId.value = null }
function resetRight() { R.zoom = 1; R.pan.x = 0; R.pan.y = 0; selectedRightId.value = null }

let panFrom = null
function onPanDown(side, ev) {
  if (ev.target.closest('.gr-node-g')) return
  panFrom = { side, x: ev.clientX, y: ev.clientY, p: { ...(side === 'L' ? L.pan : R.pan) } }
  window.addEventListener('mousemove', onPanMove)
  window.addEventListener('mouseup', onPanUp)
  // 点空白清除高亮 + 关闭抽屉
  drawerNode.value = null
  if (side === 'L') selectedLeftId.value = null
  else selectedRightId.value = null
}
function onPanMove(ev) {
  if (!panFrom) return
  const dx = ev.clientX - panFrom.x
  const dy = ev.clientY - panFrom.y
  const target = panFrom.side === 'L' ? L.pan : R.pan
  target.x = panFrom.p.x + dx
  target.y = panFrom.p.y + dy
}
function onPanUp() { panFrom = null; window.removeEventListener('mousemove', onPanMove); window.removeEventListener('mouseup', onPanUp) }
function onWheel(side, ev) {
  const delta = -Math.sign(ev.deltaY) * 0.08
  const t = side === 'L' ? L : R
  t.zoom = Math.max(0.3, Math.min(3, t.zoom + delta))
}

/* ========== 分栏拖拽 (33% ~ 67%) ========== */
const splitRef = ref(null)
const leftPct = ref(33.33)
const isSplitDragging = ref(false)
let splitFrom = null
function onSplitDown(ev) {
  ev.preventDefault()
  splitFrom = { x: ev.clientX, pct: leftPct.value, w: splitRef.value?.clientWidth || 800 }
  isSplitDragging.value = true
}
function onSplitMove(ev) {
  if (!splitFrom) return
  const dxPct = (ev.clientX - splitFrom.x) / splitFrom.w * 100
  leftPct.value = Math.max(33.33, Math.min(66.67, splitFrom.pct + dxPct))
}
function onSplitUp() { splitFrom = null; isSplitDragging.value = false }

/* ========== 搜索过滤 ========== */
const qLeft = ref('')
const qRight = ref('')

const leftViewNodes = computed(() => {
  const k = qLeft.value.trim().toLowerCase()
  return leftData.value.nodes
    .filter(n => !k || (n.label || '').toLowerCase().includes(k) || (n.categoryCode || '').toLowerCase().includes(k))
    .map(n => ({ ...n, x: leftPos[n.id]?.x ?? LW / 2, y: leftPos[n.id]?.y ?? LH / 2 }))
})
const leftEdges = computed(() => {
  const ids = new Set(leftViewNodes.value.map(n => n.id))
  let id = 0
  return leftData.value.edges
    .filter(e => ids.has(e.source) && ids.has(e.target))
    .map(e => {
      const s = leftPos[e.source], t = leftPos[e.target]
      if (!s || !t) return null
      return { id: ++id, source: e.source, target: e.target, x1: s.x, y1: s.y, x2: t.x, y2: t.y }
    })
    .filter(Boolean)
})

const rightViewNodes = computed(() => {
  const k = qRight.value.trim().toLowerCase()
  return rightData.value.nodes
    .filter(n => !k
      || (n.label || '').toLowerCase().includes(k)
      || (n.apiName || '').toLowerCase().includes(k))
    .map(n => ({
      ...n,
      x: rightPos[n.id]?.x ?? RW / 2,
      y: rightPos[n.id]?.y ?? RH / 2,
      w: 130, h: 36,
      fill: '#fff',
      stroke: '#3b82f6'
    }))
})
const rightEdges = computed(() => {
  const ids = new Set(rightViewNodes.value.map(n => n.id))
  let id = 0
  return rightData.value.edges
    .filter(e => relOn[e.kind])
    .filter(e => ids.has(e.source) && ids.has(e.target))
    .map(e => {
      const s = rightPos[e.source], t = rightPos[e.target]
      if (!s || !t) return null
      return { id: ++id, source: e.source, target: e.target, kind: e.kind, label: e.label,
               x1: s.x, y1: s.y, x2: t.x, y2: t.y }
    })
    .filter(Boolean)
})

/* ========== 高亮 / 选中 / 联动 ========== */
const selectedLeftId  = ref(null)
const selectedRightId = ref(null)
const highlightedRightIds = ref(new Set())     // 由左 → 右 联动产生
const highlightedLeftIds = computed(() => selectedLeftId.value ? new Set([selectedLeftId.value]) : new Set())

const drawerNode = ref(null)

function nodeRadius(n) {
  const base = LEFT_STYLE[n.kind]?.r || 28
  return selectedLeftId.value === n.id ? base + 8 : base
}
function edgeWidth(e) {
  const base = RELATION_MAP[e.kind]?.width || 1.5
  const hl = highlightedRightIds.value.has(e.source) && highlightedRightIds.value.has(e.target)
  return hl ? base + 1.2 : base
}

function onLeftNodeClick(n) {
  selectedLeftId.value = n.id
  drawerNode.value = { ...n, _side: 'L' }
  // 联动: 取 boundClassIds 首位, 居中右画布 + 高亮
  const boundIds = n.boundClassIds || []
  if (boundIds.length) {
    const targetId = boundIds[0]
    centerRightOn(targetId)
    const hl = new Set([targetId])
    // 加上一阶邻居 (与该对象相连的所有其他对象)
    rightData.value.edges.forEach(e => {
      if (e.source === targetId) hl.add(e.target)
      if (e.target === targetId) hl.add(e.source)
    })
    highlightedRightIds.value = hl
  } else {
    highlightedRightIds.value = new Set()
  }
}
function onRightNodeClick(n) {
  selectedRightId.value = n.id
  drawerNode.value = { ...n, _side: 'R' }
  // 单击右节点: 高亮一阶邻居
  const hl = new Set([n.id])
  rightData.value.edges.forEach(e => {
    if (e.source === n.id) hl.add(e.target)
    if (e.target === n.id) hl.add(e.source)
  })
  highlightedRightIds.value = hl
}

function centerRightOn(id) {
  const p = rightPos[id]
  if (!p) return
  // 让 (p.x, p.y) 落到画布几何中心 (RW/2, RH/2) 在当前 zoom 下
  R.pan.x = (RW / 2 - p.x) * R.zoom
  R.pan.y = (RH / 2 - p.y) * R.zoom
}

function nodeRelations(id) {
  return rightData.value.edges.filter(e => (e.source === id || e.target === id) && relOn[e.kind])
}
function otherEnd(id, e) {
  const o = e.source === id ? e.target : e.source
  const node = rightData.value.nodes.find(n => n.id === o)
  return node?.label || node?.apiName || o
}

/* ========== 导出 PNG (调用 SVG → Canvas) ========== */
const leftSvgRef = ref(null)
const rightSvgRef = ref(null)
async function exportLeft()  { await svgToPng(leftSvgRef.value,  '行业图谱.png') }
async function exportRight() { await svgToPng(rightSvgRef.value, '对象图谱.png') }
async function svgToPng(svgEl, filename) {
  if (!svgEl) { BL.warning('画布未就绪'); return }
  try {
    const xml = new XMLSerializer().serializeToString(svgEl)
    const blob = new Blob([xml], { type: 'image/svg+xml;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const vb = svgEl.viewBox?.baseVal
      canvas.width  = (vb?.width  || 1200) * 2
      canvas.height = (vb?.height || 800)  * 2
      const ctx = canvas.getContext('2d')
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      const a = document.createElement('a')
      a.href = canvas.toDataURL('image/png')
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
      BL.success(`已导出: ${filename}`)
    }
    img.src = url
  } catch (e) {
    BL.error('导出失败: ' + e.message)
  }
}

function truncate(s, n) { if (!s) return ''; return String(s).length > n ? String(s).slice(0, n) + '…' : s }

watch(() => leftData.value, layoutLeft, { deep: false })
watch(() => rightData.value, layoutRight, { deep: false })

/* Esc 键关闭抽屉 */
function onKeyDown(ev) {
  if (ev.key === 'Escape' && drawerNode.value) {
    drawerNode.value = null
    selectedLeftId.value = null
    selectedRightId.value = null
    highlightedRightIds.value = new Set()
  }
}
onMounted(() => window.addEventListener('keydown', onKeyDown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeyDown))
</script>

<style scoped>
.gr-root {
  display: flex; flex-direction: column;
  height: 100%;
  width: 100%;
  min-width: 0;
  background: var(--bl-bg-2);
  overflow: hidden;            /* 防止任何子项导致横/纵滚 */
  position: relative;          /* 给抽屉 absolute 定位用 */
}

/* —— 顶部头 (项目标准 48px) —— */
.gr-topbar {
  flex-shrink: 0;
  display: flex; align-items: center;
  padding: 10px 20px 12px;
  border-bottom: 1px solid var(--bl-border);
  background: var(--bl-bg-1);
}
.gr-title-wrap { display: flex; align-items: baseline; gap: 8px; }
.gr-title-ic { display: inline-flex; align-self: center; }
.gr-title { font-size: 18px; font-weight: 600; line-height: 1.2; color: var(--bl-text-1); }
.gr-subtitle { font-size: var(--bl-fs-12); color: var(--bl-text-3); margin-left: 4px; }

/* —— 主体 双画布 —— */
.gr-split {
  flex: 1; min-height: 0; min-width: 0;
  display: flex; flex-direction: row;
  user-select: none;
  overflow: hidden;
}
.gr-pane {
  display: flex; flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: var(--bl-bg-1);
}
.gr-pane-l { background: #f3f4f6; }            /* 左侧底色 #f3f4f6 per spec */
.gr-pane-r { background: #ffffff; }           /* 右侧底色 纯白 per spec */

/* 分割线 6px 默认 #e5e7eb hover #3b82f6 */
.gr-divider {
  width: 6px; flex-shrink: 0;
  background: #e5e7eb;
  cursor: ew-resize;
  position: relative;
  transition: background-color .15s;
}
.gr-divider:hover, .gr-divider.is-active { background: #3b82f6; }
.gr-divider-grip {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 2px; height: 30px;
  background: rgba(255,255,255,.5);
  border-radius: 1px;
}

/* —— 工具栏 (min 48px, 允许换行避免横滚) —— */
.gr-toolbar {
  flex-shrink: 0;
  min-height: 48px;
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--bl-divider);
  background: rgba(255,255,255,0.7);
  flex-wrap: wrap;
}
.gr-search {
  position: relative;
  display: flex; align-items: center;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  padding: 0 8px 0 24px;
  width: 200px;
  min-width: 0;
  max-width: 100%;
  height: 28px;
  flex-shrink: 1;
}
.gr-search-ic { position: absolute; left: 8px; color: var(--bl-text-3); }
.gr-search-input { border: 0; height: 100%; width: 100%; font-size: 12px; background: transparent; }
.gr-search-input:focus { outline: none; }
.gr-search-clear {
  background: transparent; border: 0; cursor: pointer;
  color: var(--bl-text-3); display: inline-flex;
}
.gr-search-clear:hover { color: var(--bl-text-1); }

/* 5 类关系筛选 */
.gr-rel-filter {
  display: flex; align-items: center; flex-wrap: wrap;
  gap: 4px 8px;
  margin-left: 4px;
  min-width: 0;
}
.gr-rel-chk {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11.5px; cursor: pointer; user-select: none;
  white-space: nowrap;
}
.gr-rel-chk input { accent-color: var(--bl-primary); }
.gr-rel-mark {
  display: inline-block;
  width: 12px; height: 3px; border-radius: 2px;
}
.gr-rel-lbl { color: var(--bl-text-2); }

/* —— 画布容器 —— */
.gr-canvas-wrap {
  flex: 1; min-height: 0;
  position: relative;
  overflow: hidden;
  cursor: grab;
}
.gr-canvas-wrap:active { cursor: grabbing; }
.gr-svg { width: 100%; height: 100%; display: block; }

/* —— 节点 —— */
.gr-node-g { cursor: pointer; }
.gr-node-g.is-selected circle { filter: drop-shadow(0 0 8px rgba(59, 130, 246, .45)); }
.gr-node-g.is-selected rect   { filter: drop-shadow(0 0 8px rgba(59, 130, 246, .45)); }
.gr-class.is-hl rect { stroke-width: 2.5; filter: drop-shadow(0 0 6px rgba(59, 130, 246, .35)); }
.gr-node-lbl { font-size: 11px; fill: #1f2937; font-weight: 500; pointer-events: none; }
.gr-node-en  { font-size: 9px;  fill: #6b7280; font-family: Consolas, monospace; pointer-events: none; }
.gr-node-sub { font-size: 9px;  fill: #6b7280; pointer-events: none; }

/* —— 连线 —— */
.gr-edge { transition: stroke-width .15s, opacity .15s; }
.gr-edge.is-hl { opacity: 1; }
.gr-edge-lbl { font-size: 9px; fill: #6b7280; pointer-events: none; }

/* —— 内嵌图例 (左上角 viewport-fixed) —— */
.gr-legend {
  position: absolute; top: 8px; left: 8px;
  background: rgba(255,255,255,0.85);
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 6px 8px;
  font-size: 11px;
  color: #374151;
  pointer-events: none;       /* 不遮挡图谱交互 */
  min-width: 90px;
}
.gr-legend-hd { font-size: 11px; font-weight: 600; margin-bottom: 4px; color: #1f2937; }
.gr-legend-item {
  display: flex; align-items: center; gap: 6px;
  padding: 2px 0;
}
.gr-legend-dot {
  width: 10px; height: 10px; border-radius: 50%;
  flex-shrink: 0;
}
.gr-legend-sep { height: 1px; background: #e5e7eb; margin: 4px 0; }

/* —— 右侧悬浮详情抽屉 —— */
.gr-drawer {
  position: absolute; top: 0; right: 0; bottom: 0;
  width: 320px;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 12px rgba(0,0,0,.06);
  transform: translateX(100%);
  transition: transform .25s ease;
  z-index: 10;
  display: flex; flex-direction: column;
}
.gr-drawer.is-open { transform: translateX(0); }
.gr-drawer-inner { height: 100%; display: flex; flex-direction: column; }
.gr-drawer-hd {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px 16px 12px;
  border-bottom: 1px solid var(--bl-divider);
}
.gr-drawer-title { flex: 1; min-width: 0; }
/* 抽屉关闭按钮 — 加大加显眼, hover 红底 */
.gr-drawer-close {
  flex-shrink: 0;
  width: 28px; height: 28px;
  border: 0; background: var(--bl-bg-2);
  border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-2);
  cursor: pointer;
  transition: background-color .15s, color .15s;
}
.gr-drawer-close:hover {
  background: #fff1f0;
  color: #f53f3f;
}
.gr-drawer-cn { font-size: 14px; font-weight: 700; color: var(--bl-text-1); }
.gr-drawer-en { font-size: 11px; color: var(--bl-text-3); margin-top: 2px; }
.gr-drawer-body { flex: 1; overflow-y: auto; padding: 12px 16px 16px; }
.gr-kv { display: flex; padding: 6px 0; border-bottom: 1px dashed var(--bl-divider); font-size: 12px; }
.gr-kv-l { width: 70px; color: var(--bl-text-3); flex-shrink: 0; }
.gr-kv-r { flex: 1; color: var(--bl-text-1); }
.gr-kv-block { display: block; }
.gr-kv-block .gr-kv-l { width: auto; margin-bottom: 4px; font-weight: 500; }
.gr-kv-text { line-height: 1.55; margin-top: 4px; }
.gr-rels { list-style: none; margin: 4px 0 0; padding: 0; }
.gr-rels li {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; border-bottom: 1px dashed #f0f2f5;
  font-size: 12px;
}
</style>
