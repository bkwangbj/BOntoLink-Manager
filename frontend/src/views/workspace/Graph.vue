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

      <!-- 左画布: 行业层级图谱 (leftCollapsed 时收起为 0 宽) -->
      <section v-show="!leftCollapsed" class="gr-pane gr-pane-l" :style="{ flex: `0 0 ${leftPct}%` }">
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
          <button class="bl-btn bl-btn-sm gr-btn-end" @click="exportLeft" title="导出 PNG (行业图谱.png)">
            <span v-html="BL.icon('download', 12)"></span><span style="margin-left:4px">导出</span>
          </button>
        </div>
        <!-- 左 SVG 画布 -->
        <div class="gr-canvas-wrap" ref="leftWrapRef"
             @mousedown="onPanDown('L', $event)"
             @wheel.prevent="onWheel('L', $event)">
          <svg ref="leftSvgRef" class="gr-svg"
               :viewBox="leftViewBox" preserveAspectRatio="xMidYMid meet">
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
                <!-- 左画布圆形节点 fill 始终是浅色 pastel, 文字强制深色保证暗模式下也可读 -->
                <text class="gr-node-lbl gr-node-lbl-on-pastel" text-anchor="middle" y="4">{{ truncate(n.label, 8) }}</text>
                <text class="gr-node-sub" text-anchor="middle" :y="nodeRadius(n) + 14">{{ KIND_CN[n.kind] }}</text>
              </g>
            </g>
          </svg>
          <!-- 浮动图例: 画布右上角, 半透明背景, 横向 4 类节点 -->
          <div class="gr-float-legend">
            <span v-for="k in ['industry','domain','subdomain','group']" :key="k" class="gr-float-leg-item">
              <span class="gr-float-leg-dot" :style="{ background: LEFT_STYLE[k].fill, border: '2px solid ' + LEFT_STYLE[k].stroke }"></span>
              <span>{{ KIND_CN[k] }}</span>
            </span>
          </div>
          <!-- 左画布左侧浮动工具栏 -->
          <aside class="gr-canvas-tools">
            <button class="gr-tool-btn" title="重新布局" @click="relayoutSide('L')"><span v-html="BL.icon('grid', 14)"></span></button>
            <button class="gr-tool-btn" title="适配视图" @click="fitViewSide('L')"><span v-html="BL.icon('move', 14)"></span></button>
            <div class="gr-tool-div"></div>
            <button class="gr-tool-btn" title="放大" @click="zoomSide('L', 0.1)"><span v-html="BL.icon('zoomIn', 14)"></span></button>
            <button class="gr-tool-btn" title="缩小" @click="zoomSide('L', -0.1)"><span v-html="BL.icon('zoomOut', 14)"></span></button>
            <button class="gr-tool-btn" title="1:1 原始" @click="resetZoomSide('L')"><span v-html="BL.icon('refresh', 14)"></span></button>
            <div class="gr-tool-div"></div>
            <button class="gr-tool-btn" title="全屏" @click="toggleFullSide('L')"><span v-html="BL.icon('maximize', 14)"></span></button>
          </aside>
        </div>
      </section>

      <!-- 拖拽分割线 + 收/展按钮 -->
      <div class="gr-divider" :class="{ 'is-active': isSplitDragging, 'is-collapsed': leftCollapsed }"
           @mousedown="onSplitDown" title="拖拽调整左右比例 (18% ~ 60%)">
        <div class="gr-divider-grip"></div>
        <button class="gr-collapse-btn" @click.stop="toggleLeftCollapse"
                :title="leftCollapsed ? '展开左画布' : '收起左画布'">
          <span v-html="BL.icon(leftCollapsed ? 'chevronRight' : 'chevronLeft', 11)"></span>
        </button>
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
          <!-- 5 类关系筛选 -->
          <div class="gr-rel-filter">
            <template v-for="r in RELATIONS" :key="r.key">
              <!-- 普通链接: 纯图例展示 (始终开启, 不提供 checkbox) -->
              <span v-if="r.key === 'link'" class="gr-rel-legend" :title="'普通链接 (始终展示, 不可关闭)'">
                <span class="gr-rel-mark" :style="{ background: r.color }"></span>
                <span class="gr-rel-lbl">{{ r.cn }}</span>
              </span>
              <!-- 其他 4 类: 可勾选筛选 -->
              <label v-else class="gr-rel-chk">
                <input type="checkbox" v-model="relOn[r.key]" />
                <span class="gr-rel-mark" :style="{ background: r.color }"></span>
                <span class="gr-rel-lbl">{{ r.cn }}</span>
              </label>
            </template>
          </div>
          <button class="bl-btn bl-btn-sm gr-btn-end" @click="exportRight" title="导出 PNG (对象图谱.png)">
            <span v-html="BL.icon('download', 12)"></span><span style="margin-left:4px">导出</span>
          </button>
        </div>
        <!-- 右 SVG 画布 -->
        <div class="gr-canvas-wrap" ref="rightWrapRef"
             @mousedown="onPanDown('R', $event)"
             @wheel.prevent="onWheel('R', $event)">
          <svg ref="rightSvgRef" class="gr-svg"
               :viewBox="rightViewBox" preserveAspectRatio="xMidYMid meet">
            <g :transform="`translate(${R.pan.x},${R.pan.y}) scale(${R.zoom})`">
              <g v-for="e in rightEdges" :key="'re-'+e.id">
                <path :d="e.path" fill="none"
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
                 :class="['gr-node-g', 'gr-class',
                          selectedRightId === n.id && 'is-selected',
                          highlightedRightIds.has(n.id) && 'is-hl',
                          n.isOrphan && 'is-faded']"
                 @click.stop="onRightNodeClick(n)">
                <rect :x="-n.w/2" :y="-n.h/2" :width="n.w" :height="n.h" rx="6"
                      :style="{ fill: n.fill, stroke: n.stroke }"
                      :stroke-width="n.isFocus ? 3 : 1.5" />
                <text :class="['gr-node-lbl', n.isFocus && 'is-focus']" text-anchor="middle" y="2"
                      :style="{ fontSize: (11 * n.fontScale).toFixed(1) + 'px' }">{{ truncate(n.label, 10) }}</text>
                <text v-if="n.apiName" :class="['gr-node-en', n.isFocus && 'is-focus']" text-anchor="middle" y="14"
                      :style="{ fontSize: (9 * n.fontScale).toFixed(1) + 'px' }">{{ truncate(n.apiName, 14) }}</text>
              </g>
            </g>
          </svg>
          <!-- 右上角模式切换 (仅在有选中节点时显示) -->
          <div v-if="selectedRightId || highlightedRightIds.size" class="gr-mode-toggle"
               @mousedown.stop>
            <button :class="['gr-mode-btn', rightLayoutMode === 'radial' && 'is-on']"
                    @click.stop="setRightLayoutMode('radial')" title="环状布局 (默认, 全部对象环绕)">环状</button>
            <button :class="['gr-mode-btn', rightLayoutMode === 'tree' && 'is-on']"
                    @click.stop="setRightLayoutMode('tree')" title="树状布局 (以选中对象为根 BFS 同心环)">树状</button>
          </div>
          <!-- 右画布左侧浮动工具栏 -->
          <aside class="gr-canvas-tools">
            <button class="gr-tool-btn" title="重新布局" @click="relayoutSide('R')"><span v-html="BL.icon('grid', 14)"></span></button>
            <button class="gr-tool-btn" title="适配视图" @click="fitViewSide('R')"><span v-html="BL.icon('move', 14)"></span></button>
            <div class="gr-tool-div"></div>
            <button class="gr-tool-btn" title="放大" @click="zoomSide('R', 0.1)"><span v-html="BL.icon('zoomIn', 14)"></span></button>
            <button class="gr-tool-btn" title="缩小" @click="zoomSide('R', -0.1)"><span v-html="BL.icon('zoomOut', 14)"></span></button>
            <button class="gr-tool-btn" title="1:1 原始" @click="resetZoomSide('R')"><span v-html="BL.icon('refresh', 14)"></span></button>
            <div class="gr-tool-div"></div>
            <button class="gr-tool-btn" title="全屏" @click="toggleFullSide('R')"><span v-html="BL.icon('maximize', 14)"></span></button>
          </aside>
        </div>
      </section>
    </div>

    <!-- 右侧悬浮详情抽屉 (固定定位, 不随画布滚动) -->
    <aside class="gr-drawer" :class="{ 'is-open': !!drawerNode }">
      <div v-if="drawerNode" class="gr-drawer-inner">
        <header class="gr-drawer-hd">
          <div class="gr-drawer-title">
            <div class="gr-drawer-cn">
              {{ drawerNode.label }}
              <span v-if="drawerNode._side === 'R' && drawerClassDetail" class="bl-tag bl-tag-success" style="margin-left:8px;font-size:11px">
                {{ drawerClassDetail.status === 1 ? '启用' : '禁用' }}
              </span>
            </div>
            <div class="gr-drawer-en bl-mono">{{ drawerNode.apiName || drawerNode.categoryCode || drawerNode.id }}</div>
          </div>
          <button class="gr-drawer-close" title="关闭详情 (Esc / 点画布空白也可关闭)"
                  @click="drawerNode = null">
            <span v-html="BL.icon('x', 16)"></span>
          </button>
        </header>
        <div class="gr-drawer-body">
          <!-- 左画布节点 (行业/领域/分组): 基础信息 -->
          <template v-if="drawerNode._side === 'L'">
            <div class="gr-kv"><span class="gr-kv-l">类型</span><span class="gr-kv-r">
              <span class="bl-tag">{{ KIND_CN[drawerNode.kind] }}</span>
            </span></div>
            <div v-if="drawerNode.categoryCode" class="gr-kv">
              <span class="gr-kv-l">命名编码</span><span class="gr-kv-r bl-mono">{{ drawerNode.categoryCode }}</span>
            </div>
            <div v-if="drawerNode.boundClassIds?.length" class="gr-kv gr-kv-block">
              <div class="gr-kv-l">绑定对象 ({{ drawerNode.boundClassIds.length }})</div>
              <div class="bl-muted gr-kv-text">已联动到右侧对象图谱;首位对象已居中并高亮</div>
            </div>
          </template>

          <!-- 右画布节点 (对象类型): 完整详情 -->
          <template v-else-if="drawerNode._side === 'R'">
            <!-- 基础信息 (从 classDetail 异步加载) -->
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
                <div v-if="drawerClassDetail.rdfs_comment" class="gr-kv gr-kv-block">
                  <div class="gr-kv-l">注释</div>
                  <div class="gr-kv-text bl-muted">{{ drawerClassDetail.rdfs_comment }}</div>
                </div>
              </template>
            </div>

            <!-- 标志位 -->
            <div v-if="drawerClassDetail" class="gr-section">
              <div class="gr-section-hd">标志位</div>
              <div class="gr-flags">
                <span class="gr-flag" :class="drawerClassDetail.is_thing && 'is-on'">顶层类: {{ drawerClassDetail.is_thing ? '是' : '否' }}</span>
                <span class="gr-flag" :class="drawerClassDetail.is_nothing && 'is-on'">底层类: {{ drawerClassDetail.is_nothing ? '是' : '否' }}</span>
                <span class="gr-flag" :class="drawerClassDetail.is_common && 'is-on'">公共类: {{ drawerClassDetail.is_common ? '是' : '否' }}</span>
                <span class="gr-flag" :class="drawerClassDetail.status === 1 && 'is-on'">{{ drawerClassDetail.status === 1 ? '启用' : '禁用' }}</span>
              </div>
            </div>

            <!-- 关联关系 -->
            <div class="gr-section">
              <div class="gr-section-hd">关联关系 ({{ nodeRelations(drawerNode.id).length }})</div>
              <ul class="gr-rels">
                <li v-for="(r, i) in nodeRelations(drawerNode.id)" :key="'rel-'+i">
                  <span class="gr-rel-mark" :style="{ background: RELATION_MAP[r.kind].color }"></span>
                  <span>{{ RELATION_MAP[r.kind].cn }}</span>
                  <span class="bl-muted" style="margin-left:auto">{{ otherEnd(drawerNode.id, r) }}</span>
                </li>
                <li v-if="!nodeRelations(drawerNode.id).length" class="bl-muted">无关联</li>
              </ul>
            </div>

            <!-- 跳转 -->
            <button class="bl-btn bl-btn-primary gr-goto-btn" @click="gotoObjectTypePage">
              <span v-html="BL.icon('externalLink', 12, '#fff')"></span>
              <span style="margin-left:6px">在对象类型管理中编辑</span>
            </button>
          </template>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { graphApi, resourceApi } from '@/api'

const router = useRouter()

/* ========== 视觉规范 (严格按 图谱6.5.pdf §1.1.1.4) ========== */
// 左节点分级样式
const LEFT_STYLE = {
  industry:  { fill: '#dbeafe', stroke: '#3b82f6', r: 36 },
  domain:    { fill: '#dcfce7', stroke: '#10b981', r: 32 },
  subdomain: { fill: '#fff7ed', stroke: '#f97316', r: 30 },
  group:     { fill: '#f3e8ff', stroke: '#8b5cf6', r: 28 }
}
const KIND_CN = { industry: '行业', domain: '领域', subdomain: '子领域', group: '分组' }

// 右图本体关系 (5 类, 顺序: 普通链接 / 父子 / 等价 / 并集 / 互斥)
const RELATIONS = [
  { key: 'link',  cn: '普通链接',   color: '#6b7280', width: 1.8, dash: '0',   defaultOn: true  },
  { key: 'sub',   cn: '父子类',     color: '#3b82f6', width: 2,   dash: '0',   defaultOn: true  },
  { key: 'eq',    cn: '等价类',     color: '#10b981', width: 2,   dash: '5,5', defaultOn: false },
  { key: 'union', cn: '并集类',     color: '#8b5cf6', width: 2,   dash: '2,3', defaultOn: false },
  { key: 'dis',   cn: '互斥不相交', color: '#ef4444', width: 2.5, dash: '0',   defaultOn: false }
]
const RELATION_MAP = Object.fromEntries(RELATIONS.map(r => [r.key, r]))
// 默认仅前两项 (普通链接 + 父子类) 勾选, 减少初次进入的视觉噪音
const relOn = reactive(Object.fromEntries(RELATIONS.map(r => [r.key, r.defaultOn])))

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
const LW = 720,  LH = 1100    // 左画布逻辑尺寸 (4 列从左到右, 每列垂直堆叠同级)
const RW = 1400, RH = 900     // 右画布逻辑尺寸
const leftPos = reactive({})
const rightPos = reactive({})

/* 左画布: 4 列从左到右 — industry / domain / subdomain / group
   每列垂直堆叠同级所有节点, 连线从左 → 右 (与上次"上→下"是 90° 旋转)
   节省左侧画布横向空间 (4 × 160 = 640px), 高度按最长一列自动延伸 */
function layoutLeft() {
  Object.keys(leftPos).forEach(k => delete leftPos[k])
  const nodes = leftData.value.nodes
  if (!nodes.length) return

  const byKind = { industry: [], domain: [], subdomain: [], group: [] }
  nodes.forEach(n => byKind[n.kind]?.push(n))

  // 同一父节点的子节点尽量按父节点的 Y 顺序展开 (使连线少交叉)
  const childrenOf = new Map()
  leftData.value.edges.forEach(e => {
    if (!childrenOf.has(e.source)) childrenOf.set(e.source, [])
    childrenOf.get(e.source).push(e.target)
  })
  const ORDER = ['industry', 'domain', 'subdomain', 'group']
  // BFS 排序: 从 industry 开始, 同层节点按父节点顺序遍历
  const sortedByKind = { industry: [...byKind.industry], domain: [], subdomain: [], group: [] }
  ;[['industry','domain'], ['domain','subdomain'], ['subdomain','group']].forEach(([parentKind, childKind]) => {
    const placed = new Set()
    sortedByKind[parentKind].forEach(p => {
      const kids = (childrenOf.get(p.id) || [])
        .map(id => nodes.find(n => n.id === id))
        .filter(n => n && n.kind === childKind)
      kids.forEach(k => { if (!placed.has(k.id)) { sortedByKind[childKind].push(k); placed.add(k.id) }})
    })
    // 兜底: 没被遍历到的 (孤儿节点)
    byKind[childKind].forEach(n => { if (!placed.has(n.id)) sortedByKind[childKind].push(n) })
  })

  // 动态列位置: 跳过节点数为 0 的 kind, 避免出现大块空白列
  const COL_GAP = 140
  const ROW_GAP = 70
  const startX = 70
  const presentKinds = ORDER.filter(k => sortedByKind[k].length > 0)
  const xByKind = {}
  presentKinds.forEach((k, idx) => { xByKind[k] = startX + idx * COL_GAP })

  ORDER.forEach(k => {
    const arr = sortedByKind[k]
    if (!arr.length) return
    // 垂直居中该列
    const totalH = (arr.length - 1) * ROW_GAP
    const startY = Math.max(60, (LH - totalH) / 2)
    arr.forEach((n, i) => {
      leftPos[n.id] = { x: xByKind[k], y: startY + i * ROW_GAP }
    })
  })
}

/* 右画布: 大圆环 + 中心聚焦的 hub-spoke 布局 (默认 "环状" 模式) */
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

/* 共享: BFS 构建生成树 + 叶子数统计 (环状 / 树状 两模式都用) */
function buildSpanningTree(focusId) {
  const nodes = rightData.value.nodes
  const edges = rightData.value.edges
  spanningTreeKeys.value = new Set()
  const visited = new Set([focusId])
  const childrenOf = new Map()
  const depthOf = new Map([[focusId, 0]])
  const queue = [focusId]
  while (queue.length) {
    const cur = queue.shift()
    edges.forEach(e => {
      let nb = null
      if (e.source === cur) nb = e.target
      else if (e.target === cur) nb = e.source
      if (nb && !visited.has(nb) && nodes.some(n => n.id === nb)) {
        visited.add(nb)
        depthOf.set(nb, depthOf.get(cur) + 1)
        if (!childrenOf.has(cur)) childrenOf.set(cur, [])
        childrenOf.get(cur).push(nb)
        queue.push(nb)
        spanningTreeKeys.value.add([cur, nb].sort().join('|'))
      }
    })
  }
  const leafCount = new Map()
  function countLeaves(id) {
    const kids = childrenOf.get(id) || []
    if (!kids.length) { leafCount.set(id, 1); return 1 }
    let sum = 0
    kids.forEach(k => { sum += countLeaves(k) })
    leafCount.set(id, sum)
    return sum
  }
  countLeaves(focusId)
  return { childrenOf, depthOf, leafCount }
}

/* 右画布: 放射性 radial tree (focus 居中, 子树按叶子数分配角度区间, 各分支朝不同方向)
   类似 D3 radial tidy tree — 不同子树往不同方向辐射, 视觉上像有机网络 */
function layoutRightRing(focusId) {
  Object.keys(rightPos).forEach(k => delete rightPos[k])
  if (!rightData.value.nodes.length || !focusId) return
  const { childrenOf, depthOf, leafCount } = buildSpanningTree(focusId)
  const cx = RW / 2, cy = RH / 2
  // 焦点居中
  rightPos[focusId] = { x: cx, y: cy }
  // 每层半径: 焦点=0, 1 层=240, 2 层=420, 3 层=580, 4 层=720, 5 层+=850
  const RING_RADII = [0, 240, 420, 580, 720, 850]
  // 递归: 每个节点占用一个角度区间 [aStart, aEnd], 其孩子在区间内按叶子比例细分
  function layoutSubtree(id, aStart, aEnd) {
    const kids = childrenOf.get(id) || []
    if (!kids.length) return
    const sumLeaves = leafCount.get(id) || 1
    let cursor = aStart
    kids.forEach(k => {
      const span = (aEnd - aStart) * (leafCount.get(k) / sumLeaves)
      const angle = cursor + span / 2          // 子节点位于区间中心角度
      const d = depthOf.get(k)
      const r = RING_RADII[d] !== undefined ? RING_RADII[d] : (d * 180)
      rightPos[k] = { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
      layoutSubtree(k, cursor, cursor + span)  // 该子树在自己区间内继续递归
      cursor += span
    })
  }
  // 从顶部 -π/2 开始绕一圈 (360°)
  layoutSubtree(focusId, -Math.PI / 2, 3 * Math.PI / 2)
}

/* 生成树边集合 (树状模式用, 仅画生成树边减少视觉混乱) */
const spanningTreeKeys = ref(new Set())

/* 右画布: 双向放射树 (焦点居中, 左右两侧均衡发散)
   - 焦点在画布中心 (cx, cy)
   - 直接子节点按叶子数贪心打包分配到左/右两半, 保持左右平衡
   - 每侧子树独立做 D3-dendrogram 风格布局, 深度沿对应方向延展
   - 仅画生成树边, S 曲线连接父子 */
function layoutRightTree(focusId) {
  Object.keys(rightPos).forEach(k => delete rightPos[k])
  if (!rightData.value.nodes.length || !focusId) return
  // 1+2. BFS 生成树 + 叶子数
  const { childrenOf, depthOf, leafCount } = buildSpanningTree(focusId)

  // 3. 把焦点的直接子节点贪心分到左/右两半 (按叶子数大到小, 总叶子少的那侧优先放)
  const focusChildren = childrenOf.get(focusId) || []
  const sortedKids = [...focusChildren].sort((a, b) => (leafCount.get(b) || 0) - (leafCount.get(a) || 0))
  const leftKids = [], rightKids = []
  let leftLeaves = 0, rightLeaves = 0
  sortedKids.forEach(k => {
    const cnt = leafCount.get(k) || 1
    if (rightLeaves <= leftLeaves) { rightKids.push(k); rightLeaves += cnt }
    else                            { leftKids.push(k);  leftLeaves  += cnt }
  })

  // 4. 布局参数 — 列宽足够容纳焦点(1.5x=195px) + 邻居(130px) 双侧半宽, 避免重叠
  const cx = RW / 2, cy = RH / 2
  const X_GAP = 230     // 列宽 (焦点半宽 98 + 邻居半宽 65 + 50 buffer ≈ 213, 取 230 安全)
  const Y_GAP = 100     // 行高 (兄弟间垂直间距, 100 比 95 略松, 文字更舒展)

  // 5. 焦点居中
  rightPos[focusId] = { x: cx, y: cy }

  // 6. 递归布局: direction = +1 向右 / -1 向左
  function layoutSubtree(id, direction, yStart, yEnd) {
    const d = depthOf.get(id) ?? 0
    const x = cx + direction * d * X_GAP
    const y = (yStart + yEnd) / 2
    rightPos[id] = { x, y }
    const kids = childrenOf.get(id) || []
    if (!kids.length) return
    const sumLeaves = leafCount.get(id) || 1
    let cursor = yStart
    kids.forEach(k => {
      const span = (yEnd - yStart) * (leafCount.get(k) / sumLeaves)
      layoutSubtree(k, direction, cursor, cursor + span)
      cursor += span
    })
  }

  // 7. 给左右两侧各自分配 Y 区间 (按需求总高 = 大半侧叶子数)
  const maxSide = Math.max(rightLeaves, leftLeaves, 1)
  const totalH = maxSide * Y_GAP
  const yTop = cy - totalH / 2
  // 右侧: 按各子树叶子比例切分 yTop..yTop+totalH 区间
  let cursor = yTop + (totalH - rightLeaves * Y_GAP) / 2     // 居中对齐 (当一侧叶子少时, 该侧自然居中)
  rightKids.forEach(k => {
    const span = (leafCount.get(k) || 1) * Y_GAP
    layoutSubtree(k, +1, cursor, cursor + span)
    cursor += span
  })
  cursor = yTop + (totalH - leftLeaves * Y_GAP) / 2
  leftKids.forEach(k => {
    const span = (leafCount.get(k) || 1) * Y_GAP
    layoutSubtree(k, -1, cursor, cursor + span)
    cursor += span
  })
}

/* ========== 视图状态: 缩放 + 平移 (左右独立) ========== */
const L = reactive({ zoom: 1, pan: { x: 0, y: 0 } })
const R = reactive({ zoom: 1, pan: { x: 0, y: 0 } })
/* 右画布布局模式 — 两种模式都是"焦点居中布局", 仅排布形态不同
   'radial' (环状): focus 居中, 全部连通节点在单一大圆环上
   'tree'   (树状): focus 居中, 按 BFS 距离在 1/2/3 同心环上分层
   无选中时, 两模式都退化为全局 2-圈展示 (layoutRight) */
const rightLayoutMode = ref('radial')
function setRightLayoutMode(mode) {
  rightLayoutMode.value = mode
  applyRightLayout()
}
function applyRightLayout() {
  if (selectedRightId.value) {
    if (rightLayoutMode.value === 'tree') layoutRightTree(selectedRightId.value)
    else                                  layoutRightRing(selectedRightId.value)
  } else {
    layoutRight()    // 无选中: 全局 2-圈
  }
  R.zoom = 1; R.pan.x = 0; R.pan.y = 0
}
function resetLeft()  { L.zoom = 1; L.pan.x = 0; L.pan.y = 0; selectedLeftId.value = null }
function resetRight() { R.zoom = 1; R.pan.x = 0; R.pan.y = 0; selectedRightId.value = null }

let panFrom = null
function onPanDown(side, ev) {
  if (ev.target.closest('.gr-node-g')) return
  panFrom = {
    side, x: ev.clientX, y: ev.clientY,
    p: { ...(side === 'L' ? L.pan : R.pan) },
    moved: false           // 区分 "click" vs "drag"
  }
  window.addEventListener('mousemove', onPanMove)
  window.addEventListener('mouseup', onPanUp)
}
function onPanMove(ev) {
  if (!panFrom) return
  const dx = ev.clientX - panFrom.x
  const dy = ev.clientY - panFrom.y
  // 移动超过 3px 视为拖拽 (不再当作 click 处理)
  if (!panFrom.moved && (Math.abs(dx) > 3 || Math.abs(dy) > 3)) panFrom.moved = true
  const target = panFrom.side === 'L' ? L.pan : R.pan
  target.x = panFrom.p.x + dx
  target.y = panFrom.p.y + dy
}
function onPanUp() {
  // 点击空白处只关闭抽屉, 不清除聚焦/布局 (防止用户误点丢失探索上下文)
  // 焦点切换/清除请用: 点击其他节点 / Esc 键
  if (panFrom && !panFrom.moved) {
    drawerNode.value = null
  }
  panFrom = null
  window.removeEventListener('mousemove', onPanMove)
  window.removeEventListener('mouseup', onPanUp)
}
function onWheel(side, ev) {
  const delta = -Math.sign(ev.deltaY) * 0.08
  const t = side === 'L' ? L : R
  t.zoom = Math.max(0.3, Math.min(3, t.zoom + delta))
}

/* ========== 动态 viewBox: 计算节点边界 + padding, 默认视角填满画布 ========== */
function computeViewBox(posMap, pad) {
  const keys = Object.keys(posMap)
  if (!keys.length) return { x: 0, y: 0, w: 100, h: 100, cx: 50, cy: 50 }
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  keys.forEach(k => {
    const p = posMap[k]
    if (p.x < minX) minX = p.x
    if (p.x > maxX) maxX = p.x
    if (p.y < minY) minY = p.y
    if (p.y > maxY) maxY = p.y
  })
  const x = minX - pad, y = minY - pad
  const w = (maxX - minX) + pad * 2
  const h = (maxY - minY) + pad * 2
  return { x, y, w, h, cx: x + w / 2, cy: y + h / 2 }
}
const leftViewMeta  = computed(() => computeViewBox(leftPos,  50))   // 左节点圆形 r=36, 留 50 余量
const rightViewMeta = computed(() => computeViewBox(rightPos, 55))   // 右节点矩形 65×36 + 标签, 留 55 余量 (收紧, 让视区更贴内容)
const leftViewBox   = computed(() => {
  const m = leftViewMeta.value;  return `${m.x.toFixed(1)} ${m.y.toFixed(1)} ${m.w.toFixed(1)} ${m.h.toFixed(1)}`
})
const rightViewBox  = computed(() => {
  const m = rightViewMeta.value; return `${m.x.toFixed(1)} ${m.y.toFixed(1)} ${m.w.toFixed(1)} ${m.h.toFixed(1)}`
})

/* ========== 画布工具栏: 双向通用 (relayout / fit / zoom / fullscreen) ========== */
function zoomSide(side, delta) {
  const t = side === 'L' ? L : R
  t.zoom = Math.max(0.3, Math.min(3, t.zoom + delta))
}
function resetZoomSide(side) {
  const t = side === 'L' ? L : R
  t.zoom = 1
}
function fitViewSide(side) {
  const t = side === 'L' ? L : R
  t.zoom = 1; t.pan.x = 0; t.pan.y = 0
}
function relayoutSide(side) {
  if (side === 'L') layoutLeft()
  else layoutRight()
  fitViewSide(side)
  BL.success('已重新布局')
}

/* 单画布全屏: 让对应 pane DOM 进入 Fullscreen API */
const leftWrapRef  = ref(null)
const rightWrapRef = ref(null)
async function toggleFullSide(side) {
  const el = side === 'L' ? leftWrapRef.value : rightWrapRef.value
  if (!el) return
  if (document.fullscreenElement) await document.exitFullscreen().catch(()=>{})
  else await el.requestFullscreen?.().catch(()=>{})
}

/* ========== 分栏拖拽 (18% ~ 60%) + 左画布收/展 ========== */
const splitRef = ref(null)
const leftPct = ref(22)          // 默认 22% (按需求文档调整, 比原 33% 更紧凑)
const leftCollapsed = ref(false)
const lastLeftPct = ref(22)      // 收起前记住宽度, 展开时还原
function toggleLeftCollapse() {
  if (leftCollapsed.value) {
    leftPct.value = lastLeftPct.value || 22
    leftCollapsed.value = false
  } else {
    lastLeftPct.value = leftPct.value
    leftCollapsed.value = true
  }
}
const isSplitDragging = ref(false)
let splitFrom = null
function onSplitDown(ev) {
  if (leftCollapsed.value) return    // 收起态下不响应拖拽
  ev.preventDefault()
  splitFrom = { x: ev.clientX, pct: leftPct.value, w: splitRef.value?.clientWidth || 800 }
  isSplitDragging.value = true
}
function onSplitMove(ev) {
  if (!splitFrom) return
  const dxPct = (ev.clientX - splitFrom.x) / splitFrom.w * 100
  leftPct.value = Math.max(18, Math.min(60, splitFrom.pct + dxPct))
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

/* 当前 relOn 关系筛选下, 仍有可见边连接的节点集合 — 无连接的节点会弱化显示 */
const visibleConnectedIds = computed(() => {
  const set = new Set()
  rightData.value.edges.forEach(e => {
    if (relOn[e.kind]) { set.add(e.source); set.add(e.target) }
  })
  return set
})

/* BFS 距离 computed — 焦点节点到每个节点的层级 (0/1/2/3/...) 用于尺寸分层 */
const focusDistance = computed(() => {
  const focus = selectedRightId.value
  const dist = new Map()
  if (!focus) return dist
  dist.set(focus, 0)
  const queue = [focus]
  while (queue.length) {
    const cur = queue.shift()
    rightData.value.edges.forEach(e => {
      let nb = null
      if (e.source === cur) nb = e.target
      else if (e.target === cur) nb = e.source
      if (nb && !dist.has(nb)) {
        dist.set(nb, dist.get(cur) + 1)
        queue.push(nb)
      }
    })
  }
  return dist
})

/* 节点尺寸按距离分层 — 收敛差值, 避免焦点过大撞到邻居 */
function nodeSizeFactor(id) {
  if (!selectedRightId.value) return 1.0      // 无选中: 统一尺寸
  const d = focusDistance.value.get(id)
  if (d === undefined) return 0.85            // 与焦点不连通
  if (d === 0) return 1.5                     // 焦点 (1.85→1.5, 减小避免重叠)
  if (d === 1) return 1.15                    // 第一层 (1.3→1.15)
  if (d === 2) return 1.0                     // 第二层 (1.05→1.0, 基准)
  return 0.85                                  // 第三层及以远
}

const rightViewNodes = computed(() => {
  const k = qRight.value.trim().toLowerCase()
  return rightData.value.nodes
    .filter(n => !k
      || (n.label || '').toLowerCase().includes(k)
      || (n.apiName || '').toLowerCase().includes(k))
    // 有焦点时 (任一模式): 只展示已布局的节点 (BFS 连通子图);
    // 无焦点时: 显示全部 (全局 2 圈展示)
    .filter(n => !selectedRightId.value || rightPos[n.id])
    .map(n => {
      const f = nodeSizeFactor(n.id)
      const isFocus = selectedRightId.value === n.id
      // 全图视图下 (无 focus), 当前关系筛选下没有可见边的节点 → 弱化显示
      const isOrphan = !selectedRightId.value && !visibleConnectedIds.value.has(n.id)
      return {
        ...n,
        x: rightPos[n.id]?.x ?? RW / 2,
        y: rightPos[n.id]?.y ?? RH / 2,
        w: Math.round(130 * f),
        h: Math.round(36 * f),
        fontScale: f,
        isFocus,
        isOrphan,
        // fill 用 CSS 变量, 自动跟随浅/深主题切换
        fill:   isFocus ? 'var(--bl-primary-soft)' : 'var(--bl-bg-1)',
        stroke: isFocus ? 'var(--bl-primary)'      : 'var(--bl-primary)'
      }
    })
})
const rightEdges = computed(() => {
  const ids = new Set(rightViewNodes.value.map(n => n.id))
  const isTreeMode = rightLayoutMode.value === 'tree' && selectedRightId.value
  const isFocused = !!selectedRightId.value      // 两种聚焦模式 (环状/树状) 都过滤到生成树边
  let id = 0
  return rightData.value.edges
    .filter(e => relOn[e.kind])
    .filter(e => ids.has(e.source) && ids.has(e.target))
    // 任一聚焦模式下: 仅显示生成树边 (避免 cross-link 打乱辐射 / 树形)
    .filter(e => {
      if (!isFocused) return true
      const key = [e.source, e.target].sort().join('|')
      return spanningTreeKeys.value.has(key)
    })
    .map(e => {
      const s = rightPos[e.source], t = rightPos[e.target]
      if (!s || !t) return null
      // 计算两端节点的半宽/半高 (用于把端点裁到矩形边缘, 不再从中心引出穿过节点内部)
      const sw = 130 * nodeSizeFactor(e.source) / 2 + 2     // +2 给描边
      const sh = 36  * nodeSizeFactor(e.source) / 2 + 2
      const tw = 130 * nodeSizeFactor(e.target) / 2 + 2
      const th = 36  * nodeSizeFactor(e.target) / 2 + 2
      // 按方向把端点拉到对应边缘上
      const dx = t.x - s.x, dy = t.y - s.y
      let x1, y1, x2, y2
      if (Math.abs(dx) >= Math.abs(dy)) {
        // 主轴横向: 源右/左边出, 目标左/右边入, y 保持中线
        if (dx > 0) { x1 = s.x + sw; x2 = t.x - tw }
        else         { x1 = s.x - sw; x2 = t.x + tw }
        y1 = s.y; y2 = t.y
      } else {
        // 主轴纵向: 源下/上边出, 目标上/下边入, x 保持中线
        if (dy > 0) { y1 = s.y + sh; y2 = t.y - th }
        else         { y1 = s.y - sh; y2 = t.y + th }
        x1 = s.x; x2 = t.x
      }
      // 路径: 树状用横向 cubic bezier (S 曲线), 其他用直线
      let path
      if (isTreeMode) {
        const cpx = (x1 + x2) / 2
        path = `M${x1.toFixed(1)},${y1.toFixed(1)} C${cpx.toFixed(1)},${y1.toFixed(1)} ${cpx.toFixed(1)},${y2.toFixed(1)} ${x2.toFixed(1)},${y2.toFixed(1)}`
      } else {
        path = `M${x1.toFixed(1)},${y1.toFixed(1)} L${x2.toFixed(1)},${y2.toFixed(1)}`
      }
      return { id: ++id, source: e.source, target: e.target, kind: e.kind, label: e.label,
               path, x1, y1, x2, y2 }
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
  // 联动: 取 boundClassIds 首位, 按当前模式 (环状/树状) 重新聚焦
  const boundIds = n.boundClassIds || []
  if (boundIds.length) {
    const targetId = boundIds[0]
    selectedRightId.value = targetId
    applyRightLayout()    // 当前模式下重新聚焦该对象
    // 高亮焦点 + 一阶邻居
    const hl = new Set([targetId])
    rightData.value.edges.forEach(e => {
      if (e.source === targetId) hl.add(e.target)
      if (e.target === targetId) hl.add(e.source)
    })
    highlightedRightIds.value = hl
    BL.success(`已联动到对象: ${rightData.value.nodes.find(x => x.id === targetId)?.label || targetId}`)
  } else {
    // 无绑定: 仅高亮左节点, 右画布保持不动 (不重置用户的探索上下文)
    BL.info(`「${n.label}」无绑定的本体对象, 右侧图谱保持原状`)
  }
}
function onRightNodeClick(n) {
  selectedRightId.value = n.id
  drawerNode.value = { ...n, _side: 'R' }
  drawerClassDetail.value = null     // 先清旧详情
  // 单击右节点: 按当前模式重新聚焦 (环状/树状都以新节点为中心)
  applyRightLayout()
  // 高亮一阶邻居 (用于 edge 加粗显示)
  const hl = new Set([n.id])
  rightData.value.edges.forEach(e => {
    if (e.source === n.id) hl.add(e.target)
    if (e.target === n.id) hl.add(e.source)
  })
  highlightedRightIds.value = hl
  // 异步拉完整对象类型详情 (用于抽屉展示)
  fetchClassDetail(n.id)
}

/* 右节点详情缓存 — 抽屉里展示完整对象类型信息 */
const drawerClassDetail = ref(null)
async function fetchClassDetail(id) {
  if (!id) return
  try {
    const res = await resourceApi.classDetail(id)
    if (drawerNode.value?.id === id) {     // 防止并发: 用户已切换到别的节点就丢弃
      drawerClassDetail.value = res
    }
  } catch {
    drawerClassDetail.value = null
  }
}
function gotoObjectTypePage() {
  if (!drawerNode.value?.id) return
  router.push({ path: '/resources/object-types', query: { openId: drawerNode.value.id } })
}

function centerRightOn(id, withZoom = false) {
  const p = rightPos[id]
  if (!p) return
  if (withZoom) R.zoom = 1.6     // 联动选中时聚焦放大, 让目标节点更突出
  const m = rightViewMeta.value
  R.pan.x = (m.cx - p.x) * R.zoom
  R.pan.y = (m.cy - p.y) * R.zoom
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
    // 回到无焦点的全局视图 (保持当前模式偏好不动)
    applyRightLayout()
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
.gr-pane-l { background: var(--bl-bg-2); }     /* 左侧底色 (浅模式淡灰, 暗模式深底) */
.gr-pane-r { background: var(--bl-bg-1); }     /* 右侧底色 (浅模式纯白, 暗模式深底) */

/* 分割线 6px 默认 主题中灰 hover 主题色 */
.gr-divider {
  width: 6px; flex-shrink: 0;
  background: var(--bl-border);
  cursor: ew-resize;
  position: relative;
  transition: background-color .15s;
}
.gr-divider:hover, .gr-divider.is-active { background: var(--bl-primary); }
.gr-divider.is-collapsed {
  width: 10px;
  cursor: default;
  background: var(--bl-bg-2);
  border-right: 1px solid var(--bl-border);
}
.gr-divider.is-collapsed:hover { background: var(--bl-bg-hover); }
.gr-divider-grip {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 2px; height: 30px;
  background: rgba(255,255,255,.5);
  border-radius: 1px;
}
.gr-divider.is-collapsed .gr-divider-grip { display: none; }

/* 收/展按钮 (悬浮在分割线中间) */
.gr-collapse-btn {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 18px; height: 40px;
  border: 1px solid var(--bl-border);
  background: var(--bl-bg-1);
  border-radius: 3px;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-2);
  cursor: pointer;
  z-index: 3;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  transition: background-color .15s, color .15s;
}
.gr-collapse-btn:hover {
  background: var(--bl-primary);
  color: #fff;
  border-color: var(--bl-primary);
}

/* —— 工具栏 (min 48px, 允许换行避免横滚) —— */
.gr-toolbar {
  flex-shrink: 0;
  min-height: 48px;
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--bl-divider);
  background: color-mix(in srgb, var(--bl-bg-1) 70%, transparent);
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

/* 浮动图例 (画布左下角, 半透明) */
.gr-float-legend {
  position: absolute; bottom: 10px; left: 10px;
  display: inline-flex; align-items: center; gap: 10px;
  padding: 6px 10px;
  background: color-mix(in srgb, var(--bl-bg-1) 85%, transparent);
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  font-size: 11.5px; color: var(--bl-text-2);
  pointer-events: none;
  z-index: 2;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

/* 工具栏右对齐按钮 — 用于把"导出"挤到工具栏最右 */
.gr-btn-end { margin-left: auto; }

/* 右画布右上角模式切换按钮组 (节点选中时显示) */
.gr-mode-toggle {
  position: absolute; top: 12px; right: 12px;
  z-index: 5;
  display: inline-flex;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}
.gr-mode-btn {
  padding: 5px 12px;
  border: 0; background: transparent;
  font-size: 12px;
  color: var(--bl-text-2);
  cursor: pointer;
  transition: background-color .15s, color .15s;
}
.gr-mode-btn + .gr-mode-btn { border-left: 1px solid var(--bl-border); }
.gr-mode-btn:hover { background: var(--bl-primary-soft); color: var(--bl-primary); }
.gr-mode-btn.is-on {
  background: var(--bl-primary);
  color: #fff;
  font-weight: 500;
}
.gr-float-leg-item {
  display: inline-flex; align-items: center; gap: 4px;
  white-space: nowrap;
}
.gr-float-leg-dot {
  width: 10px; height: 10px; border-radius: 50%;
  flex-shrink: 0;
}

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
/* 普通链接: 纯图例样式 (无 checkbox), 与可勾选项区分 */
.gr-rel-legend {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11.5px; color: var(--bl-text-2);
  white-space: nowrap;
  cursor: default;
  user-select: none;
}
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

/* Fullscreen: 显式声明底色, 否则浏览器默认黑底盖掉主题色 */
.gr-canvas-wrap:fullscreen,
.gr-canvas-wrap:-webkit-full-screen {
  background: var(--bl-bg-1);
  padding: 8px;
}

/* —— 节点 —— */
.gr-node-g { cursor: pointer; transition: opacity .2s; }
/* 弱化: 当前关系筛选下无连通边的孤儿节点 → 半透明 */
.gr-node-g.is-faded { opacity: 0.25; }
.gr-node-g.is-faded:hover { opacity: 1; }      /* hover 时还原便于查看 */
.gr-node-g.is-selected circle { filter: drop-shadow(0 0 8px rgba(59, 130, 246, .45)); }
.gr-node-g.is-selected rect   { filter: drop-shadow(0 0 8px rgba(59, 130, 246, .45)); }
.gr-class.is-hl rect { stroke-width: 2.5; filter: drop-shadow(0 0 6px rgba(59, 130, 246, .35)); }
.gr-node-lbl { font-size: 11px; fill: var(--bl-text-1); font-weight: 500; pointer-events: none; }
.gr-node-en  { font-size: 9px;  fill: var(--bl-text-3); font-family: Consolas, monospace; pointer-events: none; }
/* 焦点节点文字: 加粗 + 主题色, 自适应深浅 */
.gr-node-lbl.is-focus { fill: var(--bl-primary); font-weight: 700; letter-spacing: 0.3px; }
.gr-node-en.is-focus  { fill: var(--bl-primary); font-weight: 600; opacity: .85; }
.gr-node-sub { font-size: 9px;  fill: var(--bl-text-3); pointer-events: none; }
/* 左画布圆形节点 (pastel 底色固定浅) - 文字强制深色保证暗模式可读 */
.gr-node-lbl-on-pastel { fill: #1f2937 !important; }
/* 左画布选中节点: 文字加大加粗, 与节点放大 (radius +8px) 配套 */
.gr-node-g.is-selected .gr-node-lbl-on-pastel {
  font-size: 14px;
  font-weight: 700;
  fill: var(--bl-primary) !important;
}

/* —— 连线 —— */
.gr-edge { transition: stroke-width .15s, opacity .15s; }
.gr-edge.is-hl { opacity: 1; }
.gr-edge-lbl { font-size: 9px; fill: var(--bl-text-3); pointer-events: none; }

/* —— 浮动垂直工具栏 (画布左上角, 与 TabObjectGraph 同款) —— */
.gr-canvas-tools {
  position: absolute; top: 12px; left: 12px;
  z-index: 5;
  display: flex; flex-direction: column; gap: 2px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 6px;
  padding: 4px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}
.gr-tool-btn {
  width: 30px; height: 30px;
  background: transparent; border: 0; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  cursor: pointer; color: var(--bl-text-2);
  transition: background-color .15s, color .15s;
}
.gr-tool-btn:hover { background: var(--bl-primary-soft); color: var(--bl-primary); }
.gr-tool-div { height: 1px; background: var(--bl-divider); margin: 2px 0; }

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
  background: color-mix(in srgb, #f53f3f 12%, var(--bl-bg-1));
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

/* 抽屉小节: 基础信息 / 标志位 / 关联关系 等 */
.gr-section {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--bl-divider);
}
.gr-section:first-child { margin-top: 0; padding-top: 0; border-top: 0; }
.gr-section-hd {
  font-size: 12px; font-weight: 600;
  color: var(--bl-text-1);
  padding-left: 8px;
  border-left: 3px solid var(--bl-primary);
  margin-bottom: 6px;
}
.gr-loading { padding: 8px 0; font-size: 12px; }
.gr-flags { display: flex; flex-wrap: wrap; gap: 6px; }
.gr-flag {
  display: inline-flex; align-items: center;
  padding: 3px 8px;
  background: var(--bl-bg-2);
  color: var(--bl-text-3);
  border-radius: 3px;
  font-size: 11px;
}
.gr-flag.is-on {
  background: var(--bl-primary-soft);
  color: var(--bl-primary);
  font-weight: 500;
}
.gr-goto-btn {
  margin-top: 14px;
  width: 100%;
  display: inline-flex; align-items: center; justify-content: center;
}
</style>
