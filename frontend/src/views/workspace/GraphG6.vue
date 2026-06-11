<template>
  <div class="gx-root" ref="rootRef">
    <header class="gx-topbar">
      <span class="gx-title-ic" v-html="BL.icon('network', 14, 'var(--bl-primary)')"></span>
      <span class="gx-title">图谱</span>
      <span class="gx-subtitle">Graph · 行业层级 ↔ 对象本体 双画布(G6)</span>
    </header>

    <div class="gx-split" ref="splitRef"
         @mousemove="onSplitMove" @mouseup="onSplitUp" @mouseleave="onSplitUp">
      <!-- 左画布：行业层级树 -->
      <section v-show="!leftCollapsed" class="gx-pane" ref="leftPaneRef" :style="{ flex: `0 0 ${leftPct}%` }">
        <div class="gx-toolbar">
          <div class="gx-search">
            <span class="gx-search-ic" v-html="BL.icon('search', 12, 'var(--bl-text-3)')"></span>
            <input v-model="qLeft" placeholder="搜索行业 / 领域 / 子领域 / 分组" @input="searchSide('L')" />
            <button v-if="qLeft" class="gx-search-x" @click="qLeft=''; searchSide('L')" v-html="BL.icon('x', 10)"></button>
          </div>
          <button class="bl-btn bl-btn-sm" @click="fitSide('L')" v-html="iconText('refresh','重置')"></button>
          <span class="bl-grow"></span>
          <button class="bl-btn bl-btn-sm" @click="exportSide('L')" v-html="iconText('download','导出')"></button>
        </div>
        <div class="gx-canvas-wrap">
          <div class="gx-canvas" ref="leftCanvas"></div>
          <!-- 悬浮竖排工具条 -->
          <aside class="gx-cv-tools">
            <button class="gx-cv-btn" title="适配视图(缩放居中,不改位置)" @click="fitViewSide('L')" v-html="BL.icon('move', 14)"></button>
            <div class="gx-cv-div"></div>
            <button class="gx-cv-btn" title="放大" @click="zoomSide('L', 1.2)" v-html="BL.icon('zoomIn', 14)"></button>
            <button class="gx-cv-btn" title="缩小" @click="zoomSide('L', 0.8)" v-html="BL.icon('zoomOut', 14)"></button>
            <button class="gx-cv-btn" title="回到初始布局(复位拖动)" @click="resetLayoutSide('L')" v-html="BL.icon('refresh', 14)"></button>
            <div class="gx-cv-div"></div>
            <button class="gx-cv-btn" title="全屏(左画布)" @click="toggleFullSide('L')" v-html="BL.icon('maximize', 14)"></button>
          </aside>
          <!-- 图形(树布局)选择器:右下角下拉 -->
          <div class="gx-cv-sel" v-click-outside="()=>treeMenu=false">
            <div v-if="treeMenu" class="gx-cv-sel-menu">
              <div v-for="l in TREE_LAYOUTS" :key="l.key" :class="['gx-cv-sel-item', curTree===l.key && 'is-on']"
                   @click="applyTree(l.key); treeMenu=false">{{ l.label }}</div>
            </div>
            <button class="gx-cv-sel-btn" @click.stop="treeMenu=!treeMenu">
              <span class="bl-grow bl-truncate">{{ curTreeLabel }}</span>
              <span v-html="BL.icon('chevronDown', 11)"></span>
            </button>
          </div>
          <div class="gx-legend">
            <span v-for="k in ['industry','domain','subdomain','group']" :key="k" class="gx-leg">
              <span class="gx-dot" :style="{ background: LEFT_STYLE[k].fill, border:'2px solid '+LEFT_STYLE[k].stroke }"></span>{{ KIND_CN[k] }}
            </span>
          </div>
        </div>
      </section>

      <!-- 分割线(可拖拽) + 收起 -->
      <div class="gx-divider" :class="{ 'is-collapsed': leftCollapsed, 'is-active': isSplitDragging }"
           @mousedown="onSplitDown" title="拖拽调整左右比例 (18% ~ 60%)">
        <div class="gx-divider-grip"></div>
        <button class="gx-collapse" @click.stop="toggleLeft" :title="leftCollapsed?'展开左画布':'收起左画布'">
          <span v-html="BL.icon(leftCollapsed?'chevronRight':'chevronLeft', 11)"></span>
        </button>
      </div>

      <!-- 右画布：对象本体关系图 -->
      <section class="gx-pane" ref="rightPaneRef" style="flex:1 1 auto">
        <div class="gx-toolbar">
          <div class="gx-search">
            <span class="gx-search-ic" v-html="BL.icon('search', 12, 'var(--bl-text-3)')"></span>
            <input v-model="qRight" placeholder="搜索本体对象" @input="searchSide('R')" />
            <button v-if="qRight" class="gx-search-x" @click="qRight=''; searchSide('R')" v-html="BL.icon('x', 10)"></button>
          </div>
          <button class="bl-btn bl-btn-sm" @click="fitSide('R')" v-html="iconText('refresh','重置')"></button>
          <div class="gx-rel">
            <template v-for="r in RELATIONS" :key="r.key">
              <span v-if="r.key==='link'" class="gx-rel-leg" title="普通链接(始终展示)">
                <span class="gx-rel-mark" :style="{ background:r.color }"></span>{{ r.cn }}
              </span>
              <label v-else class="gx-rel-chk">
                <input type="checkbox" v-model="relOn[r.key]" @change="applyRelFilter" />
                <span class="gx-rel-mark" :style="{ background:r.color }"></span>{{ r.cn }}
              </label>
            </template>
          </div>
          <span class="bl-grow"></span>
          <div class="gx-layout-seg">
            <button v-for="l in GRAPH_LAYOUTS" :key="l.key" :class="['gx-layout-btn', curGraph===l.key && 'is-on']"
                    @click="applyGraph(l.key)">{{ l.label }}</button>
          </div>
          <button class="bl-btn bl-btn-sm" @click="exportSide('R')" v-html="iconText('download','导出')"></button>
        </div>
        <div class="gx-canvas-wrap">
          <div class="gx-canvas" ref="rightCanvas"></div>
          <aside class="gx-cv-tools">
            <button class="gx-cv-btn" title="适配视图(缩放居中,不改位置)" @click="fitViewSide('R')" v-html="BL.icon('move', 14)"></button>
            <div class="gx-cv-div"></div>
            <button class="gx-cv-btn" title="放大" @click="zoomSide('R', 1.2)" v-html="BL.icon('zoomIn', 14)"></button>
            <button class="gx-cv-btn" title="缩小" @click="zoomSide('R', 0.8)" v-html="BL.icon('zoomOut', 14)"></button>
            <button class="gx-cv-btn" title="回到初始布局(复位拖动)" @click="resetLayoutSide('R')" v-html="BL.icon('refresh', 14)"></button>
            <div class="gx-cv-div"></div>
            <button class="gx-cv-btn" title="全屏(右画布)" @click="toggleFullSide('R')" v-html="BL.icon('maximize', 14)"></button>
          </aside>
          <!-- 节点详情抽屉 -->
          <aside class="gr-drawer" :class="{ 'is-open': !!drawerNode }">
            <div v-if="drawerNode" class="gr-drawer-inner">
              <header class="gr-drawer-hd">
                <div class="gr-drawer-title">
                  <div class="gr-drawer-cn">{{ drawerNode.label }}
                    <span v-if="drawerClassDetail" class="bl-tag bl-tag-success" style="margin-left:8px;font-size:11px">{{ drawerClassDetail.status === 1 ? '启用' : '禁用' }}</span>
                  </div>
                  <div class="gr-drawer-en bl-mono">{{ drawerNode.apiName || drawerNode.id }}</div>
                </div>
                <button class="gr-drawer-close" title="关闭" @click="closeDrawer"><span v-html="BL.icon('x', 16)"></span></button>
              </header>
              <div class="gr-drawer-body">
                <div class="gr-section">
                  <div class="gr-section-hd">基础信息</div>
                  <div v-if="!drawerClassDetail" class="bl-muted gr-loading">加载中…</div>
                  <template v-else>
                    <div class="gr-kv"><span class="gr-kv-l">名称</span><span class="gr-kv-r">{{ drawerClassDetail.display_name || drawerNode.label }}</span></div>
                    <div class="gr-kv"><span class="gr-kv-l">API</span><span class="gr-kv-r bl-mono">{{ drawerClassDetail.api_name }}</span></div>
                    <div v-if="drawerClassDetail.rdfs_label" class="gr-kv"><span class="gr-kv-l">标准名</span><span class="gr-kv-r bl-mono">{{ drawerClassDetail.rdfs_label }}</span></div>
                    <div v-if="drawerClassDetail.ns_code" class="gr-kv"><span class="gr-kv-l">命名空间</span><span class="gr-kv-r bl-mono">{{ drawerClassDetail.ns_code }}</span></div>
                    <div v-if="drawerClassDetail.category_code" class="gr-kv"><span class="gr-kv-l">领域</span><span class="gr-kv-r bl-mono">{{ drawerClassDetail.category_code }}</span></div>
                    <div class="gr-kv"><span class="gr-kv-l">父类</span><span class="gr-kv-r">{{ drawerClassDetail.parent_class_id || '—' }}</span></div>
                    <div v-if="drawerClassDetail.rdfs_comment" class="gr-kv gr-kv-block"><div class="gr-kv-l">描述</div><div class="gr-kv-text bl-muted">{{ drawerClassDetail.rdfs_comment }}</div></div>
                  </template>
                </div>
                <div v-if="drawerClassDetail" class="gr-section">
                  <div class="gr-section-hd">标志位</div>
                  <div class="gr-flags">
                    <span class="gr-flag" :class="drawerClassDetail.is_thing && 'is-on'">顶层类: {{ drawerClassDetail.is_thing ? '是' : '否' }}</span>
                    <span class="gr-flag" :class="drawerClassDetail.is_nothing && 'is-on'">底层类: {{ drawerClassDetail.is_nothing ? '是' : '否' }}</span>
                    <span class="gr-flag" :class="drawerClassDetail.is_common && 'is-on'">公共类: {{ drawerClassDetail.is_common ? '是' : '否' }}</span>
                    <span class="gr-flag" :class="drawerClassDetail.status === 1 && 'is-on'">{{ drawerClassDetail.status === 1 ? '启用' : '禁用' }}</span>
                  </div>
                </div>
                <div class="gr-section">
                  <div class="gr-section-hd">关联关系 ({{ nodeRelations(drawerNode.id).length }})</div>
                  <ul class="gr-rels">
                    <li v-for="(r, i) in nodeRelations(drawerNode.id)" :key="'rel'+i">
                      <span class="gr-rel-mark" :style="{ background: RELATION_MAP[r.kind].color }"></span>
                      <span>{{ RELATION_MAP[r.kind].cn }}</span>
                      <span class="bl-muted" style="margin-left:auto">{{ otherEnd(drawerNode.id, r) }}</span>
                    </li>
                    <li v-if="!nodeRelations(drawerNode.id).length" class="bl-muted">无关联</li>
                  </ul>
                </div>
                <button class="bl-btn bl-btn-primary gr-goto-btn" @click="gotoObjectTypePage">
                  <span v-html="BL.icon('externalLink', 12, '#fff')"></span><span style="margin-left:6px">在对象类型管理中编辑</span>
                </button>
              </div>
            </div>
          </aside>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, shallowRef, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import G6 from '@antv/g6'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { graphApi, categoryApi, resourceApi } from '@/api'

const router = useRouter()

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

const rootRef = ref(null)
const leftPaneRef = ref(null)
const rightPaneRef = ref(null)
const leftCanvas = ref(null)
const rightCanvas = ref(null)
const leftG = shallowRef(null)
const rightG = shallowRef(null)
const leftCollapsed = ref(false)
const leftPct = ref(22)          // 默认左画布 22%(对齐旧版)
const lastLeftPct = ref(22)
const splitRef = ref(null)

/* —— 分割线拖拽(18% ~ 60%) —— */
const isSplitDragging = ref(false)
let splitFrom = null
function onSplitDown(ev) {
  if (leftCollapsed.value) return
  splitFrom = { x: ev.clientX, pct: leftPct.value, w: splitRef.value?.clientWidth || 800 }
  isSplitDragging.value = true
  ev.preventDefault()
}
function onSplitMove(ev) {
  if (!splitFrom) return
  const dxPct = ((ev.clientX - splitFrom.x) / splitFrom.w) * 100
  leftPct.value = Math.max(18, Math.min(60, splitFrom.pct + dxPct))
}
function onSplitUp() {
  if (!splitFrom) return
  splitFrom = null; isSplitDragging.value = false
  nextTick(resizeAll)
}
const qLeft = ref('')
const qRight = ref('')
const curTree = ref('compactBox')
const curGraph = ref('force')
const treeMenu = ref(false)
const curTreeLabel = computed(() => (TREE_LAYOUTS.find(l => l.key === curTree.value) || TREE_LAYOUTS[0]).label)

/* —— 视觉规范 —— */
const LEFT_STYLE = {
  root:      { fill: '#0E42D2', stroke: '#0E42D2', r: 18 },
  industry:  { fill: '#dbeafe', stroke: '#3b82f6', r: 18 },
  domain:    { fill: '#dcfce7', stroke: '#10b981', r: 15 },
  subdomain: { fill: '#fff7ed', stroke: '#f97316', r: 14 },
  group:     { fill: '#f3e8ff', stroke: '#8b5cf6', r: 13 }
}
const KIND_CN = { industry: '行业', domain: '领域', subdomain: '子领域', group: '分组' }
const RELATIONS = [
  { key: 'link',  cn: '普通链接',   color: '#6b7280', dash: [0],    width: 1.8, on: true },
  { key: 'sub',   cn: '父子类',     color: '#3b82f6', dash: [0],    width: 2,   on: true },
  { key: 'eq',    cn: '等价类',     color: '#10b981', dash: [5, 5], width: 2,   on: true },
  { key: 'union', cn: '并集类',     color: '#8b5cf6', dash: [2, 3], width: 2,   on: true },
  { key: 'dis',   cn: '互斥不相交', color: '#ef4444', dash: [0],    width: 2.5, on: true }
]
const RELATION_MAP = Object.fromEntries(RELATIONS.map(r => [r.key, r]))
const relOn = reactive(Object.fromEntries(RELATIONS.map(r => [r.key, r.on])))

/* —— 自定义左侧节点:圆圈 + 名称 + 折叠标识(+/-) —— */
let _g6Registered = false
function ensureNode() {
  if (_g6Registered) return
  _g6Registered = true
  G6.registerNode('cat-node', {
    draw(cfg, group) {
      const r = (cfg.size || 22) / 2
      const st = cfg.style || {}
      const circle = group.addShape('circle', {
        attrs: { x: 0, y: 0, r, fill: st.fill || '#5b8ff9', stroke: st.stroke || '#fff', lineWidth: st.lineWidth == null ? 2 : st.lineWidth, cursor: 'pointer' },
        name: 'node-circle', draggable: true
      })
      const hasCh = !!(cfg.children && cfg.children.length)
      let lx = r + 8
      if (hasCh) {
        group.addShape('marker', {
          attrs: { x: r, y: 0, r: 5.5, symbol: cfg.collapsed ? G6.Marker.expand : G6.Marker.collapse,
            stroke: '#86909c', fill: '#fff', lineWidth: 1.2, cursor: 'pointer' },
          name: 'collapse-marker'
        })
        lx = r + 12
      }
      if (cfg.label) {
        group.addShape('text', {
          attrs: { x: lx, y: 0, text: cfg.label, fontSize: 11, fill: '#4e5969', textAlign: 'left', textBaseline: 'middle', cursor: 'pointer' },
          name: 'node-label'
        })
      }
      return circle
    },
    setState(name, value, item) {
      const group = item.getContainer()
      const circle = group.find(e => e.get('name') === 'node-circle')
      if (!circle) return
      const base = item.getModel().style || {}
      if (name === 'dim') {
        group.get('children').forEach(s => s.attr('opacity', value ? 0.2 : 1))
      } else if (name === 'selected' || name === 'highlight') {
        if (value) { circle.attr('lineWidth', 4); circle.attr('stroke', '#165DFF'); circle.attr('shadowColor', '#165DFF'); circle.attr('shadowBlur', 12) }
        else { circle.attr('lineWidth', base.lineWidth == null ? 2 : base.lineWidth); circle.attr('stroke', base.stroke || '#fff'); circle.attr('shadowBlur', 0) }
      }
    }
  }, 'single-node')

  /* 右侧对象类型:卡片(中文上 + 英文下) */
  G6.registerNode('onto-node', {
    draw(cfg, group) {
      const color = cfg._color || '#165DFF'
      const cn = cfg.label || ''
      const en = cfg.apiName || ''
      const w = Math.max(cn.length * 13 + 22, en.length * 6 + 18, 72)
      const h = 38
      const rect = group.addShape('rect', {
        attrs: { x: -w / 2, y: -h / 2, width: w, height: h, radius: 6, fill: '#fff', stroke: color, lineWidth: 1.6,
          cursor: 'pointer', shadowColor: 'rgba(0,0,0,0.08)', shadowBlur: 4, shadowOffsetY: 1 },
        name: 'card-rect', draggable: true
      })
      group.addShape('text', {
        attrs: { x: 0, y: en ? -6 : 0, text: cn, fontSize: 12, fontWeight: 600, fill: '#1d2129', textAlign: 'center', textBaseline: 'middle', cursor: 'pointer' },
        name: 'card-cn'
      })
      if (en) group.addShape('text', {
        attrs: { x: 0, y: 9, text: en, fontSize: 9, fill: '#86909c', textAlign: 'center', textBaseline: 'middle', cursor: 'pointer' },
        name: 'card-en'
      })
      return rect
    },
    setState(name, value, item) {
      const group = item.getContainer()
      const rect = group.find(e => e.get('name') === 'card-rect')
      if (name === 'dim') { group.get('children').forEach(s => s.attr('opacity', value ? 0.14 : 1)); return }
      if (!rect) return
      if (name === 'highlight' || name === 'selected') {
        if (value) { rect.attr('lineWidth', 3); rect.attr('shadowColor', 'rgba(22,93,255,0.45)'); rect.attr('shadowBlur', 12) }
        else { rect.attr('lineWidth', 1.6); rect.attr('shadowColor', 'rgba(0,0,0,0.08)'); rect.attr('shadowBlur', 4) }
      }
    }
  }, 'single-node')
}
ensureNode()

const selectedLeftId = ref(null)
const coveredMap = new Map()     // categoryCode → 其覆盖的全部 code(含自身/后代)
let ontData = { nodes: [], edges: [] }   // 右侧对象本体原始数据
let leftTreeData = null                  // 左侧树原始数据(用于"回到初始布局"重建)
const drawerNode = ref(null)             // 右侧节点详情抽屉
const drawerClassDetail = ref(null)      // 异步加载的对象类型完整详情

const TREE_LAYOUTS = [
  { key: 'compactBox', label: '紧凑树', cfg: { type: 'compactBox', direction: 'LR', getHeight: () => 22, getWidth: () => 22, getVGap: () => 12, getHGap: () => 80 } },
  { key: 'dendrogram', label: '系统树', cfg: { type: 'dendrogram', direction: 'LR', nodeSep: 26, rankSep: 120 } },
  { key: 'mindmap',    label: '脑图',   cfg: { type: 'mindmap', direction: 'H', getHeight: () => 22, getWidth: () => 22, getVGap: () => 12, getHGap: () => 90 } },
  { key: 'indented',   label: '缩进树', cfg: { type: 'indented', direction: 'LR', indent: 140, getHeight: () => 18, getWidth: () => 18 } },
  { key: 'radial',     label: '辐射树', cfg: { type: 'compactBox', direction: 'LR', radial: true, getHeight: () => 22, getWidth: () => 22, getVGap: () => 26, getHGap: () => 55 } }
]
const GRAPH_LAYOUTS = [
  { key: 'force',    label: '力导向', cfg: { type: 'force', preventOverlap: true, nodeSize: 130, nodeSpacing: 14, linkDistance: 110, collideStrength: 0.9, alphaDecay: 0.022, gravity: 14 } },
  { key: 'dagre',    label: '分层',   cfg: { type: 'dagre', rankdir: 'LR', nodesep: 14, ranksep: 45 } },
  { key: 'circular', label: '环形图', cfg: { type: 'circular', ordering: 'degree', preventOverlap: true, nodeSize: 130, nodeSpacing: 8 } }
]
function graphCfg2(k) { return (GRAPH_LAYOUTS.find(l => l.key === k) || GRAPH_LAYOUTS[0]).cfg }
function treeCfg(k) { return (TREE_LAYOUTS.find(l => l.key === k) || TREE_LAYOUTS[0]).cfg }
function graphCfg(k) { return (GRAPH_LAYOUTS.find(l => l.key === k) || GRAPH_LAYOUTS[0]).cfg }

/* —— 构建左侧层级树:取分类树嵌套结构(行业 → 领域 → 分组) —— */
const KIND_BY_DEPTH = ['industry', 'domain', 'group', 'group', 'group']
function convCat(n, depth) {
  const kind = KIND_BY_DEPTH[depth] || 'group'
  const st = LEFT_STYLE[kind] || LEFT_STYLE.group
  const code = n.categoryCode || n.category_code || n.id
  return {
    id: code, label: n.rdfsLabel || n.label || n.cn || code, kind, size: (st.r || 14) * 2,
    style: { fill: st.fill, stroke: st.stroke, lineWidth: 2 },
    labelCfg: { position: 'right', offset: 6, style: { fontSize: 11, fill: '#4e5969' } },
    children: (n.children || []).map(c => convCat(c, depth + 1))
  }
}
function buildTree(catRoots) {
  const roots = (Array.isArray(catRoots) ? catRoots : []).map(c => convCat(c, 0))
  // 隐形虚拟根(TreeGraph 需单一根),渲染后隐藏 → 视觉上只剩行业森林
  return { id: '__root__', label: '', kind: 'root', size: 1,
    style: { fill: 'transparent', stroke: 'transparent', lineWidth: 0 }, children: roots }
}

/* —— 初始化 —— */
function initLeft(treeData) {
  const el = leftCanvas.value; if (!el) return
  if (leftG.value && !leftG.value.get('destroyed')) leftG.value.destroy()
  const g = new G6.TreeGraph({
    container: el, width: el.clientWidth || 500, height: el.clientHeight || 600,
    fitView: true, fitViewPadding: 30, animate: true, minZoom: 0.05, maxZoom: 3,
    modes: { default: ['drag-canvas', 'zoom-canvas'] },
    defaultNode: { type: 'cat-node', size: 22 },
    defaultEdge: { type: 'cubic-horizontal', style: { stroke: '#c9cdd4', lineWidth: 1.3 } },
    layout: treeCfg(curTree.value)
  })
  g.data(treeData); g.render()
  hideRoot(g)
  bindLeftEvents(g)
  bindFit(g, 40)
  leftG.value = g
}

/* 左画布交互:点圆圈/标识=收展,点名称=联动右画布 */
function bindLeftEvents(g) {
  g.on('node:click', (evt) => {
    const item = evt.item
    if (!item || item.getModel().id === '__root__') return
    const tname = evt.target && evt.target.get && evt.target.get('name')
    if (tname === 'collapse-marker') toggleCollapse(item)   // 仅 +/- 标识负责收展
    else selectLeftAndLink(item)                            // 点节点其它任意处 = 选中+联动
  })
  g.on('canvas:click', () => { clearLeftSel(); resetRight() })
}
function toggleCollapse(item) {
  const g = leftG.value; const m = item.getModel()
  if (!m.children || !m.children.length) { selectLeftAndLink(item); return }   // 叶子:点击也做联动
  m.collapsed = !m.collapsed
  const marker = item.getContainer().find(e => e.get && e.get('name') === 'collapse-marker')
  if (marker) marker.attr('symbol', m.collapsed ? G6.Marker.expand : G6.Marker.collapse)
  g.layout()
}
function selectLeftAndLink(item) {
  const g = leftG.value
  selectedLeftId.value = item.getModel().id
  g.getNodes().forEach(n => g.setItemState(n, 'selected', n.getModel().id === selectedLeftId.value))
  rightLink(item.getModel().id)
}
function clearLeftSel() {
  const g = leftG.value; if (!g) return
  selectedLeftId.value = null
  g.getNodes().forEach(n => g.setItemState(n, 'selected', false))
}

/* —— 右画布高亮/联动(力导向关系图,持久实例) —— */
function edgeOn(kind) { return kind === 'link' ? true : !!relOn[kind] }
/* 高亮一组命中节点 + 其关联连线,其余弱化(置灰) */
function applyHighlight(g, matchedIds) {
  if (!g) return
  if (!matchedIds || !matchedIds.size) { clearStates(g); return }
  const relNodes = new Set(matchedIds), relEdges = new Set()
  g.getEdges().forEach(e => {
    const m = e.getModel()
    if (matchedIds.has(m.source) || matchedIds.has(m.target)) { relEdges.add(e.getID()); relNodes.add(m.source); relNodes.add(m.target) }
  })
  // 命中的保持正常颜色/正常大小;未命中的置灰。靠"灰 vs 正常"的对比来突出,不再额外加粗高亮
  g.getNodes().forEach(n => {
    const id = n.getModel().id
    g.setItemState(n, 'highlight', false)
    g.setItemState(n, 'dim', !relNodes.has(id))
  })
  g.getEdges().forEach(e => {
    const on = relEdges.has(e.getID())
    g.setItemState(e, 'highlight', false)
    g.setItemState(e, 'dim', !on)
  })
}
function clearStates(g) {
  if (!g) return
  g.getNodes().forEach(n => { g.setItemState(n, 'highlight', false); g.setItemState(n, 'dim', false); g.setItemState(n, 'selected', false) })
  g.getEdges().forEach(e => { g.setItemState(e, 'highlight', false); g.setItemState(e, 'dim', false) })
}
/* 左→右联动:定位绑定对象、平移居中、高亮节点+关联线,其余弱化;无绑定仅取消高亮 */
function rightLink(code) {
  const rg = rightG.value; if (!rg) return
  const codes = coveredMap.get(code) || new Set([code])
  const matched = new Set(rg.getNodes().filter(n => codes.has(n.getModel().categoryCode)).map(n => n.getModel().id))
  if (!matched.size) { clearStates(rg); return }   // 无绑定:仅取消高亮,不清空图谱
  applyHighlight(rg, matched)
  fitToNodes(rg, matched)   // 命中子集缩放铺满视区、居中
}

/* 把一组节点缩放居中、铺满视区(用于左→右联动 / 搜索聚焦) */
function fitToNodes(g, ids, padding = 90) {
  if (!g || g.get('destroyed')) return
  const items = [...ids].map(id => g.findById(id)).filter(Boolean)
  if (!items.length) return
  // 命中子集在"模型坐标系"的包围盒(与视口矩阵无关)
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  items.forEach(it => {
    const m = it.getModel()
    const w = (Array.isArray(m.size) ? m.size[0] : m.size) || 124
    const h = (Array.isArray(m.size) ? m.size[1] : m.size) || 40
    minX = Math.min(minX, m.x - w / 2); maxX = Math.max(maxX, m.x + w / 2)
    minY = Math.min(minY, m.y - h / 2); maxY = Math.max(maxY, m.y + h / 2)
  })
  const bw = Math.max(maxX - minX, 1), bh = Math.max(maxY - minY, 1)
  const vw = g.getWidth(), vh = g.getHeight()
  const minZ = g.get('minZoom') || 0.05
  let ratio = Math.min((vw - padding * 2) / bw, (vh - padding * 2) / bh)
  ratio = Math.max(minZ, Math.min(ratio, 1.4))   // 按视口算 → 必装得下;单节点别放太大
  const cx = (minX + maxX) / 2, cy = (minY + maxY) / 2
  // 确定性 3 步:① 复位缩放 ② 把命中中心挪到屏幕正中(此时 scale=1)③ 绕屏幕中心缩放,中心点不动
  g.zoomTo(1)
  const c0 = g.getCanvasByPoint(cx, cy)
  g.translate(vw / 2 - c0.x, vh / 2 - c0.y)
  g.zoomTo(ratio, { x: vw / 2, y: vh / 2 }, true, { duration: 300 })
}
function resetRight() { clearStates(rightG.value) }

/* 隐藏虚拟根节点及其连出的边 */
function hideRoot(g) {
  const root = g.findById('__root__')
  if (root) g.hideItem(root, false)
  g.getEdges().forEach(e => { if (e.getModel().source === '__root__') g.hideItem(e, false) })
}

/* 布局算完后适配画布一次(避免初始 bbox 过小放大过头;收展节点时不重新适配) */
function bindFit(g, padding = 30) {
  const h = () => { if (g && !g.get('destroyed')) g.fitView(padding); g.off('afterlayout', h) }
  g.on('afterlayout', h)
  setTimeout(() => { if (g && !g.get('destroyed')) g.fitView(padding) }, 300)
}

function cardNode(n) { return { id: n.id, label: n.label, apiName: n.apiName, categoryCode: n.categoryCode, _color: n.color || '#165DFF', size: [124, 40] } }
function edgeStyleOf(kind) {
  const r = RELATION_MAP[kind] || RELATION_MAP.link
  return { stroke: r.color, lineWidth: r.width, lineDash: r.dash, endArrow: kind === 'sub' }
}
/* 右画布:力导向关系图(对象类型卡片 + 5 类关系) */
function initRight() {
  const el = rightCanvas.value; if (!el) return
  if (rightG.value && !rightG.value.get('destroyed')) rightG.value.destroy()
  const g = new G6.Graph({
    container: el, width: el.clientWidth || 700, height: el.clientHeight || 600,
    fitView: true, fitViewPadding: 40, animate: true, minZoom: 0.05, maxZoom: 3,
    modes: { default: ['drag-canvas', 'zoom-canvas', 'drag-node'] },
    defaultNode: { type: 'onto-node', size: [124, 40] },
    nodeStateStyles: {},
    // 高亮只加粗+提亮,不改 stroke 颜色(颜色代表关系类型,不能被染色)
    edgeStateStyles: { dim: { opacity: 0.07 }, highlight: { lineWidth: 3.4, opacity: 1 } },
    layout: graphCfg2(curGraph.value)
  })
  const edges = ontData.edges.map((e, i) => {
    const lbl = e.kind === 'link' ? (e.label || '') : ''
    return {
      id: 'e' + i, source: e.source, target: e.target, kind: e.kind, label: lbl, style: edgeStyleOf(e.kind),
      labelCfg: lbl ? { refY: 0, autoRotate: false, style: {
        fontSize: 10, fill: '#86909c',
        background: { fill: 'rgba(255,255,255,0.95)', stroke: '#e5e6eb', lineWidth: 1, padding: [3, 6, 3, 6], radius: 4 }
      } } : undefined
    }
  })
  // 同一对节点间多条不同关系的边会重叠 → 散开成不同弧度,让每种关系各显其色
  if (G6.Util && G6.Util.processParallelEdges) {
    G6.Util.processParallelEdges(edges, 20, 'quadratic', 'cubic-horizontal', 'loop')
  }
  g.data({ nodes: ontData.nodes.map(cardNode), edges })
  g.render(); bindFit(g, 50)
  g.on('node:click', (evt) => openRightDrawer(evt.item.getModel()))
  g.on('canvas:click', () => closeDrawer())
  rightG.value = g
  nextTick(applyRelFilter)
}

/* —— 右节点详情抽屉 —— */
function openRightDrawer(m) {
  drawerNode.value = { id: m.id, label: m.label, apiName: m.apiName, categoryCode: m.categoryCode }
  drawerClassDetail.value = null
  applyHighlight(rightG.value, new Set([m.id]))   // 单击节点:高亮其一阶邻居
  const it = rightG.value && rightG.value.findById(m.id)
  if (it) rightG.value.focusItem(it, true)
  fetchClassDetail(m.id)
}
function closeDrawer() { drawerNode.value = null; drawerClassDetail.value = null; resetRight() }
async function fetchClassDetail(id) {
  if (!id) return
  try { const res = await resourceApi.classDetail(id); if (drawerNode.value && drawerNode.value.id === id) drawerClassDetail.value = res }
  catch { drawerClassDetail.value = null }
}
function nodeRelations(id) { return ontData.edges.filter(e => (e.source === id || e.target === id) && edgeOn(e.kind)) }
function otherEnd(id, e) {
  const o = e.source === id ? e.target : e.source
  const node = ontData.nodes.find(n => n.id === o)
  return (node && (node.label || node.apiName)) || o
}
function gotoObjectTypePage() {
  if (!drawerNode.value || !drawerNode.value.id) return
  router.push({ path: '/resources/object-types', query: { openId: drawerNode.value.id } })
}

/* —— 数据加载 —— */
async function load() {
  const [cat, ot] = await Promise.all([
    categoryApi.tree().catch(() => []),
    graphApi.ontology().catch(() => ({ nodes: [], edges: [] }))
  ])
  // 分类覆盖映射(code → 自身+后代 codes),用于左→右联动
  coveredMap.clear()
  const cover = (node) => {
    const c = node.categoryCode || node.category_code || node.id
    const set = new Set([c])
    ;(node.children || []).forEach(ch => cover(ch).forEach(x => set.add(x)))
    coveredMap.set(c, set)
    return set
  }
  ;(Array.isArray(cat) ? cat : []).forEach(cover)

  ontData = { nodes: ot.nodes || [], edges: ot.edges || [] }

  await nextTick()
  leftTreeData = buildTree(cat)
  initLeft(leftTreeData)
  initRight()
}

/* —— 布局切换 —— */
function applyTree(k) { curTree.value = k; const g = leftG.value; if (g) { bindFit(g, 40); g.updateLayout(treeCfg(k)); hideRoot(g) } }
function applyGraph(k) { curGraph.value = k; const g = rightG.value; if (g) { bindFit(g, 50); g.updateLayout(graphCfg2(k)); nextTick(applyRelFilter) } }

/* —— 关系筛选:复选框控制连线显隐 —— */
function applyRelFilter() {
  const g = rightG.value; if (!g) return
  g.getEdges().forEach(e => { edgeOn(e.getModel().kind) ? g.showItem(e, false) : g.hideItem(e, false) })
}

/* —— 搜索高亮:命中节点高亮 + 关联连线加粗,其余弱化 —— */
function searchSide(side) {
  const g = side === 'L' ? leftG.value : rightG.value
  const q = (side === 'L' ? qLeft.value : qRight.value).trim().toLowerCase()
  if (!g) return
  if (!q) { clearStates(g); return }
  const matched = new Set(g.getNodes().filter(n => {
    const m = n.getModel()
    return (m.label || '').toLowerCase().includes(q) || (m.apiName || '').toLowerCase().includes(q)
  }).map(n => n.getModel().id))
  applyHighlight(g, matched)
  const first = [...matched].map(id => g.findById(id)).find(Boolean)
  if (first) g.focusItem(first, true)
}

/* —— 工具 —— */
function sideGraph(side) { return side === 'L' ? leftG.value : rightG.value }
function zoomSide(side, r) {
  const g = sideGraph(side)
  if (g && !g.get('destroyed')) g.zoom(r, { x: g.getWidth() / 2, y: g.getHeight() / 2 })
}
function resetZoomSide(side) {
  const g = sideGraph(side)
  if (g && !g.get('destroyed')) g.zoomTo(1, { x: g.getWidth() / 2, y: g.getHeight() / 2 })
}
/* 适配视图:整图缩放居中塞进视口(不改节点位置) */
function fitViewSide(side) {
  const g = sideGraph(side)
  if (g && !g.get('destroyed')) g.fitView(side === 'L' ? 40 : 50)
}
/* 回到初始布局:重建画布(彻底复位被拖动过的节点)+ 清搜索/高亮,保留当前布局与关系筛选 */
function resetLayoutSide(side) {
  if (side === 'L') {
    qLeft.value = ''; clearLeftSel()
    if (leftTreeData && leftCanvas.value) initLeft(leftTreeData)
  } else {
    qRight.value = ''; drawerNode.value = null; drawerClassDetail.value = null
    if (rightCanvas.value) initRight()
  }
}
/* 兼容旧调用:顶部"重置"= 回到初始布局 */
function fitSide(side) { resetLayoutSide(side) }
function exportSide(side) {
  const g = side === 'L' ? leftG.value : rightG.value
  g?.downloadFullImage(side === 'L' ? '行业图谱' : '对象图谱', 'image/png', { backgroundColor: '#fff', padding: 20 })
}
function toggleLeft() {
  if (leftCollapsed.value) { leftPct.value = lastLeftPct.value || 22; leftCollapsed.value = false }
  else { lastLeftPct.value = leftPct.value; leftCollapsed.value = true }
  nextTick(resizeAll)
}
/* 各画布独立全屏:对所在 pane 请求全屏 */
function toggleFullSide(side) {
  const el = side === 'L' ? leftPaneRef.value : rightPaneRef.value
  if (!el) return
  if (!document.fullscreenElement) el.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.()
}
function onFsChange() { nextTick(resizeAll) }

let ro = null
function resizeAll() {
  ;[[leftG.value, leftCanvas.value], [rightG.value, rightCanvas.value]].forEach(([g, el]) => {
    if (g && el && !g.get('destroyed')) { g.changeSize(el.clientWidth || 500, el.clientHeight || 600); g.fitView() }
  })
}

onMounted(async () => {
  try { await load() } catch { BL.error('图谱加载失败') }
  ro = new ResizeObserver(() => resizeAll())
  if (leftCanvas.value) ro.observe(leftCanvas.value)
  if (rightCanvas.value) ro.observe(rightCanvas.value)
  document.addEventListener('fullscreenchange', onFsChange)
})
onBeforeUnmount(() => {
  ro?.disconnect()
  document.removeEventListener('fullscreenchange', onFsChange)
  ;[leftG.value, rightG.value].forEach(g => { if (g && !g.get('destroyed')) g.destroy() })
})

/* click-outside 指令(局部) */
const vClickOutside = {
  mounted(el, binding) {
    el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }
    setTimeout(() => document.addEventListener('mousedown', el.__h), 0)
  },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}
</script>

<style scoped>
.gx-root { display: flex; flex-direction: column; height: 100%; background: var(--bl-bg-2); overflow: hidden; position: relative; }
.gx-topbar { flex-shrink: 0; display: flex; align-items: center; gap: 8px; padding: 10px 20px 12px; border-bottom: 1px solid var(--bl-border); background: var(--bl-bg-1); }
.gx-title-ic { display: inline-flex; }
.gx-title { font-size: 18px; font-weight: 600; color: var(--bl-text-1); }
.gx-subtitle { font-size: 12px; color: var(--bl-text-3); }

.gx-split { flex: 1; display: flex; min-height: 0; }
.gx-pane { display: flex; flex-direction: column; min-width: 0; position: relative; background: var(--bl-bg-1); }
.gx-pane + .gx-pane, .gx-divider + .gx-pane { border-left: 0; }

.gx-toolbar { flex-shrink: 0; display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-bottom: 1px solid var(--bl-border); flex-wrap: wrap; min-height: 46px; }
.gx-search { position: relative; display: flex; align-items: center; }
.gx-search-ic { position: absolute; left: 8px; display: inline-flex; }
.gx-search input { width: 180px; height: 30px; box-sizing: border-box; padding: 0 22px 0 26px; border: 1px solid var(--bl-border); border-radius: 6px; font-size: 12.5px; outline: none; }
.gx-search input:focus { border-color: var(--bl-primary); }
.gx-search-x { position: absolute; right: 6px; width: 16px; height: 16px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }

.gx-rel { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.gx-rel-leg, .gx-rel-chk { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--bl-text-2); cursor: default; }
.gx-rel-chk { cursor: pointer; }
.gx-rel-mark { width: 14px; height: 3px; border-radius: 2px; }

/* 画布容器 + 悬浮层 */
.gx-canvas-wrap { flex: 1; min-height: 0; position: relative; }
.gx-canvas { position: absolute; inset: 0; }

/* 悬浮竖排工具条(画布左上) */
.gx-cv-tools { position: absolute; top: 12px; left: 12px; display: flex; flex-direction: column; gap: 2px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,.08); padding: 4px; z-index: 5; }
.gx-cv-btn { width: 28px; height: 28px; border: 0; background: transparent; color: var(--bl-text-2); border-radius: 6px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.gx-cv-btn:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }
.gx-cv-div { height: 1px; background: var(--bl-divider); margin: 2px 4px; }

/* 图形(树布局)下拉选择器:左画布右下角 */
.gx-cv-sel { position: absolute; right: 12px; bottom: 12px; z-index: 5; width: 96px; }
.gx-cv-sel-btn { width: 100%; height: 28px; display: flex; align-items: center; gap: 4px; padding: 0 8px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px; box-shadow: 0 2px 10px rgba(0,0,0,.08); color: var(--bl-text-2); cursor: pointer; font-size: 12px; }
.gx-cv-sel-btn:hover { border-color: var(--bl-primary-border); }
.gx-cv-sel-menu { position: absolute; left: 0; right: 0; bottom: calc(100% + 4px); background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,.14); padding: 4px; }
.gx-cv-sel-item { padding: 6px 8px; border-radius: 6px; cursor: pointer; font-size: 12px; color: var(--bl-text-2); }
.gx-cv-sel-item:hover:not(.is-on) { background: var(--bl-bg-hover); }
.gx-cv-sel-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }

/* 工具条上的横向布局分段(右画布) */
.gx-layout-seg { display: inline-flex; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; flex-shrink: 0; }
.gx-layout-btn { height: 26px; padding: 0 12px; border: 0; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; font-size: 12px; }
.gx-layout-btn + .gx-layout-btn { border-left: 1px solid var(--bl-border); }
.gx-layout-btn:hover:not(.is-on) { background: var(--bl-bg-hover); }
.gx-layout-btn.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }

.gx-divider { width: 6px; flex-shrink: 0; background: var(--bl-border); position: relative; cursor: col-resize; }
.gx-divider:hover, .gx-divider.is-active { background: var(--bl-primary); }
.gx-divider.is-collapsed { cursor: default; }
.gx-divider.is-collapsed:hover { background: var(--bl-border); }
.gx-divider-grip { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); width: 2px; height: 28px; background: rgba(255,255,255,.6); border-radius: 2px; pointer-events: none; }
.gx-collapse { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); width: 18px; height: 36px; border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 4px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; color: var(--bl-text-3); z-index: 3; }
.gx-collapse:hover { color: var(--bl-primary); border-color: var(--bl-primary); }

.gx-legend { position: absolute; bottom: 12px; left: 12px; display: flex; gap: 12px; background: rgba(255,255,255,.85); border: 1px solid var(--bl-border); border-radius: 8px; padding: 6px 10px; backdrop-filter: blur(2px); }
.gx-leg { display: inline-flex; align-items: center; gap: 5px; font-size: 11.5px; color: var(--bl-text-2); }
.gx-dot { width: 11px; height: 11px; border-radius: 50%; }
.gx-legend-rel { flex-direction: column; gap: 5px; align-items: flex-start; }
.gx-rel-line { width: 18px; height: 3px; border-radius: 2px; display: inline-block; }

/* 节点详情抽屉 */
.gr-drawer { position: absolute; top: 0; right: 0; bottom: 0; width: 320px; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border); box-shadow: -4px 0 12px rgba(0,0,0,.06); transform: translateX(100%); transition: transform .25s ease; z-index: 10; display: flex; flex-direction: column; }
.gr-drawer.is-open { transform: translateX(0); }
.gr-drawer-inner { height: 100%; display: flex; flex-direction: column; }
.gr-drawer-hd { display: flex; align-items: flex-start; gap: 10px; padding: 14px 16px 12px; border-bottom: 1px solid var(--bl-divider); }
.gr-drawer-title { flex: 1; min-width: 0; }
.gr-drawer-close { flex-shrink: 0; width: 28px; height: 28px; border: 0; background: var(--bl-bg-2); border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; color: var(--bl-text-2); cursor: pointer; }
.gr-drawer-close:hover { background: color-mix(in srgb, #f53f3f 12%, var(--bl-bg-1)); color: #f53f3f; }
.gr-drawer-cn { font-size: 14px; font-weight: 700; color: var(--bl-text-1); }
.gr-drawer-en { font-size: 11px; color: var(--bl-text-3); margin-top: 2px; }
.gr-drawer-body { flex: 1; overflow-y: auto; padding: 12px 16px 16px; }
.gr-kv { display: flex; padding: 6px 0; border-bottom: 1px dashed var(--bl-border); font-size: 12px; }
.gr-kv-l { width: 70px; color: var(--bl-text-3); flex-shrink: 0; }
.gr-kv-r { flex: 1; color: var(--bl-text-1); word-break: break-all; }
.gr-kv-block { display: block; }
.gr-kv-block .gr-kv-l { width: auto; margin-bottom: 4px; font-weight: 500; }
.gr-kv-text { line-height: 1.55; margin-top: 4px; }
.gr-rels { list-style: none; margin: 4px 0 0; padding: 0; }
.gr-rels li { display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px dashed var(--bl-border); font-size: 12px; }
.gr-rel-mark { width: 14px; height: 3px; border-radius: 2px; flex-shrink: 0; }
.gr-section { margin-top: 12px; padding-top: 8px; border-top: 1px solid var(--bl-divider); }
.gr-section:first-child { margin-top: 0; padding-top: 0; border-top: 0; }
.gr-section-hd { font-size: 12px; font-weight: 600; color: var(--bl-text-1); padding-left: 8px; border-left: 3px solid var(--bl-primary); margin-bottom: 6px; }
.gr-loading { padding: 8px 0; font-size: 12px; }
.gr-flags { display: flex; flex-wrap: wrap; gap: 6px; }
.gr-flag { display: inline-flex; align-items: center; padding: 3px 8px; background: var(--bl-bg-2); color: var(--bl-text-3); border-radius: 3px; font-size: 11px; }
.gr-flag.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.gr-goto-btn { width: 100%; margin-top: 16px; justify-content: center; }
</style>
