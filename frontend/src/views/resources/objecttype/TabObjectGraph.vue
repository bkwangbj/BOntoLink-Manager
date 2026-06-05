<template>
  <div ref="rootRef" class="og-root" :class="{ 'is-full': isFull }">

    <!-- 左上画布操作栏 -->
    <aside class="og-toolbar">
      <button class="og-tool" @click="relayout" title="自动重排"><span v-html="BL.icon('grid', 14)"></span></button>
      <button class="og-tool" @click="fitView" title="适配视图"><span v-html="BL.icon('move', 14)"></span></button>
      <div class="og-tool-div"></div>
      <button class="og-tool" @click="zoomBy(0.1)" title="放大"><span v-html="BL.icon('zoomIn', 14)"></span></button>
      <button class="og-tool" @click="zoomBy(-0.1)" title="缩小"><span v-html="BL.icon('zoomOut', 14)"></span></button>
      <button class="og-tool" @click="resetZoom" title="1:1"><span v-html="BL.icon('refresh', 14)"></span></button>
      <div class="og-tool-div"></div>
      <button class="og-tool" @click="toggleFull" :title="isFull ? '退出全屏' : '全屏'"><span v-html="BL.icon(isFull ? 'minimize' : 'maximize', 14)"></span></button>
    </aside>

    <!-- 右上导出 / 共享 -->
    <div class="og-export">
      <button class="bl-btn bl-btn-sm" v-for="f in ['PNG','PDF','OWL','JSON']" :key="f"
              :title="`导出 ${f}`" @click="onExport(f)">{{ f }}</button>
      <button class="bl-btn bl-btn-sm" @click="onShare" title="复制图谱分享链接">
        <span v-html="BL.icon('externalLink', 12)"></span><span style="margin-left:4px">分享</span>
      </button>
    </div>

    <!-- 主画布 -->
    <svg ref="svgRef" class="og-svg"
         :viewBox="`0 0 ${svgW} ${svgH}`"
         preserveAspectRatio="xMidYMid meet"
         @mousedown="onCanvasDown"
         @wheel.prevent="onWheel">
      <defs>
        <marker id="og-arrow" viewBox="0 0 10 10" refX="9" refY="5"
                markerWidth="6" markerHeight="6" orient="auto">
          <path d="M0 0L10 5L0 10z" fill="#86909c" />
        </marker>
      </defs>
      <g :transform="`translate(${pan.x},${pan.y}) scale(${zoom})`">

        <!-- 连线 (一条边 = 可见细线 + 透明粗线命中区, group hover 时高亮) -->
        <g v-for="e in visibleEdges" :key="'E-'+e.id"
           class="og-edge-group"
           :class="{ 'is-hot': hoverEdgeId === e.id }"
           @mouseenter="hoverEdgeId = e.id"
           @mouseleave="hoverEdgeId = null"
           @click.stop="onEdgeClick(e)">
          <!-- 可视细线 (按规范配色 / 线型) -->
          <path :d="edgePath(e)"
                :stroke="e.stroke"
                :stroke-dasharray="e.dash"
                :stroke-width="e.width"
                fill="none"
                :marker-end="e.arrow ? 'url(#og-arrow)' : ''"
                class="og-edge" />
          <!-- 透明粗线 (扩大点击命中区域 ~14px) -->
          <path :d="edgePath(e)" class="og-edge-hit" />
          <text v-if="e.label" :x="(e.x1 + e.x2) / 2" :y="(e.y1 + e.y2) / 2 - 4"
                class="og-edge-lbl" text-anchor="middle">{{ e.label }}</text>
        </g>

        <!-- 节点 -->
        <g v-for="n in visibleNodes" :key="'N-'+n.id"
           :transform="`translate(${n.x - n.w/2},${n.y - n.h/2})`"
           :class="['og-node-g', n.deprecated && 'is-deprecated', selectedId === n.id && 'is-selected']"
           @mousedown.stop="onNodeDown(n, $event)"
           @click.stop="onNodeClick(n)">

          <!-- 形状 -->
          <path :d="shapePath(n.shape, n.w, n.h)"
                :fill="n.deprecated ? '#C0C4CC22' : n.fill"
                :stroke="n.deprecated ? '#C0C4CC' : n.stroke"
                :stroke-width="n.id === 'center' ? 2 : 1.5" />

          <!-- 中心节点 (展开): 用 foreignObject 渲染卡片 + 属性列表 -->
          <foreignObject v-if="n.id === 'center' && centerExpanded"
                         x="0" y="0" :width="n.w" :height="n.h">
            <div class="og-center" xmlns="http://www.w3.org/1999/xhtml">
              <div class="og-center-hd">
                <div class="og-center-name">
                  <span class="og-center-cn">{{ n.cn }}</span>
                  <span class="og-center-en">({{ n.en }})</span>
                </div>
                <span class="og-center-tag">{{ n.properties.length }} 个属性</span>
              </div>
              <ul class="og-prop-list">
                <li v-for="p in n.properties" :key="p.code" class="og-prop-item">
                  <span class="og-prop-tag" :class="'is-' + p.kind">{{ p.tag }}</span>
                  <span class="og-prop-cn">{{ p.cn }}</span>
                  <span class="og-prop-en">({{ p.code }})</span>
                  <span class="og-prop-dot" :class="p.isKey && 'is-key'"></span>
                </li>
              </ul>
            </div>
          </foreignObject>

          <!-- 中心节点 (紧凑): 仅显示名称 + 属性数 -->
          <foreignObject v-else-if="n.id === 'center'"
                         x="0" y="0" :width="n.w" :height="n.h">
            <div class="og-center og-center-compact" xmlns="http://www.w3.org/1999/xhtml">
              <div class="og-center-hd">
                <div class="og-center-name">
                  <span class="og-center-cn">{{ n.cn }}</span>
                  <span class="og-center-en">({{ n.en }})</span>
                </div>
                <span class="og-center-tag">{{ n.properties.length }} 个属性</span>
              </div>
            </div>
          </foreignObject>

          <!-- 普通节点 -->
          <g v-else>
            <text class="og-node-cn" :x="n.w/2" :y="n.h/2 - 2"
                  text-anchor="middle">{{ truncate(n.cn, 10) }}</text>
            <text class="og-node-en" :x="n.w/2" :y="n.h/2 + 11"
                  text-anchor="middle">{{ truncate(n.en, 16) }}</text>
          </g>
        </g>
      </g>
    </svg>

    <!-- 左下图例 (七大维度开关) -->
    <aside class="og-legend">
      <div class="og-legend-hd">
        <span>维度图例</span>
        <button class="bl-btn bl-btn-text bl-btn-sm" @click="toggleAll" title="全选 / 清空">
          {{ allOn ? '清空' : '全选' }}
        </button>
      </div>
      <div v-for="d in DIMENSIONS" :key="d.key"
           class="og-legend-item" :class="!dimOn[d.key] && 'is-off'"
           @click="dimOn[d.key] = !dimOn[d.key]"
           :title="(dimOn[d.key] ? '点击关闭 · ' : '点击开启 · ') + d.label">
        <svg class="og-legend-ic" width="22" height="14">
          <path :d="shapePath(d.shape, 22, 14)" :fill="d.color" :opacity="dimOn[d.key] ? 1 : .25" />
        </svg>
        <span class="og-legend-lbl">{{ d.label }}</span>
        <span class="og-legend-cnt">{{ dimCounts[d.key] || 0 }}</span>
      </div>
      <div class="og-legend-tip">实/虚线 = 关系类型;弃用节点统一灰色 #C0C4CC</div>
    </aside>

    <!-- 右侧详情抽屉 -->
    <aside class="og-drawer" :class="{ 'is-open': !!drawerNode }">
      <div v-if="drawerNode" class="og-drawer-inner">
        <header class="og-drawer-hd">
          <span class="og-drawer-shape">
            <svg width="20" height="14">
              <path :d="shapePath(drawerNode.shape, 20, 14)" :fill="drawerNode.fill" :stroke="drawerNode.stroke" />
            </svg>
          </span>
          <div class="og-drawer-title">
            <div class="og-drawer-cn">{{ drawerNode.cn }}</div>
            <div class="og-drawer-en bl-mono">{{ drawerNode.en }}</div>
          </div>
          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm"
                  @click="drawerNode = null" v-html="BL.icon('x', 14)"></button>
        </header>
        <div class="og-drawer-body">
          <div class="og-kv"><span class="og-kv-l">维度</span><span class="og-kv-r">
            <span class="bl-tag" :style="{ background: dimColor(drawerNode.dim) + '22', color: dimColor(drawerNode.dim) }">{{ dimLabel(drawerNode.dim) }}</span>
          </span></div>
          <div class="og-kv"><span class="og-kv-l">类别</span><span class="og-kv-r">{{ kindLabel(drawerNode) }}</span></div>
          <div v-if="drawerNode.rid" class="og-kv"><span class="og-kv-l">RID</span><span class="og-kv-r bl-mono">{{ drawerNode.rid }}</span></div>
          <div v-if="drawerNode.namespace" class="og-kv"><span class="og-kv-l">命名空间</span><span class="og-kv-r bl-mono">{{ drawerNode.namespace }}</span></div>
          <div v-if="drawerNode.version" class="og-kv"><span class="og-kv-l">版本</span><span class="og-kv-r">{{ drawerNode.version }}</span></div>
          <div v-if="drawerNode.deprecated" class="og-kv"><span class="og-kv-l">状态</span><span class="og-kv-r"><span class="bl-tag bl-tag-danger">已弃用</span></span></div>
          <div v-else-if="drawerNode.status" class="og-kv"><span class="og-kv-l">状态</span><span class="og-kv-r"><span class="bl-tag bl-tag-success">{{ drawerNode.status }}</span></span></div>
          <div v-if="drawerNode.cardinality" class="og-kv"><span class="og-kv-l">关联基数</span><span class="og-kv-r"><span class="bl-tag">{{ cardLabel(drawerNode.cardinality) }}</span></span></div>
          <div v-if="drawerNode.desc" class="og-kv og-kv-block">
            <div class="og-kv-l">说明</div>
            <div class="og-kv-text bl-muted">{{ drawerNode.desc }}</div>
          </div>
          <div v-if="drawerNode.properties" class="og-kv og-kv-block">
            <div class="og-kv-l">字段 ({{ drawerNode.properties.length }})</div>
            <ul class="og-drawer-props">
              <li v-for="p in drawerNode.properties" :key="p.code">
                <span class="og-prop-tag" :class="'is-' + p.kind">{{ p.tag }}</span>
                <span>{{ p.cn }}</span>
                <span class="bl-mono bl-muted" style="margin-left:auto;font-size:11px">{{ p.code }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </aside>

    <!-- 链接关系编辑大抽屉 (复用 LinkTypeEditor, 不要 tab) -->
    <LinkTypeEditor v-model:open="editorOpen"
                    :link-id="editorLinkId"
                    :all-classes="allClasses"
                    :show-tabs="false" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'
import LinkTypeEditor from '@/views/resources/linktype/LinkTypeEditor.vue'

const props = defineProps({
  classId: { type: String, default: '' }
})

/* ============ 七大维度规范 (颜色/形状/默认状态 严格按需求文档) ============ */
const DIMENSIONS = [
  { key: 'inherit', label: '继承实现', color: '#409EFF', shape: 'rect',     default: true  },
  { key: 'biz',     label: '行业层级', color: '#9C88FF', shape: 'pentagon', default: true  },
  { key: 'app',     label: '应用能力', color: '#29CCB1', shape: 'parallel', default: true  },
  { key: 'ext',     label: '外部关联', color: '#36CFC9', shape: 'cloud',    default: true  },
  { key: 'prop',    label: '属性体系', color: '#67C23A', shape: 'circle',   default: false },
  { key: 'rule',    label: '规则约束', color: '#E6A23C', shape: 'diamond',  default: false },
  { key: 'ds',      label: '数据来源', color: '#F56C6C', shape: 'hexagon',  default: false },
]
const DIM_BY_KEY = Object.fromEntries(DIMENSIONS.map(d => [d.key, d]))
const dimOn = reactive(Object.fromEntries(DIMENSIONS.map(d => [d.key, d.default])))

/* 连线样式: 严格按文档规范 */
const EDGE_STYLES = {
  inherit:   { stroke: '#409EFF', width: 1, dash: '0',   arrow: true,  label: '继承' },        // 实线 1px
  interface: { stroke: '#409EFF', width: 1, dash: '4,3', arrow: true,  label: '实现' },        // 虚线 1px
  equiv:     { stroke: '#E6A23C', width: 2, dash: '0',   arrow: false, label: '等价' },        // 粗实线 2px
  disjoint:  { stroke: '#E6A23C', width: 1, dash: '4,3', arrow: false, label: '不等价' },
  exclude:   { stroke: '#E6A23C', width: 1, dash: '4,3', arrow: false, label: '互斥' },
  union:     { stroke: '#E6A23C', width: 1, dash: '4,3', arrow: false, label: '并集' },
  prop:      { stroke: '#67C23A', width: 1, dash: '0',   arrow: true,  label: '属性' },
  biz:       { stroke: '#9C88FF', width: 1, dash: '0',   arrow: true,  label: '归属' },
  app:       { stroke: '#29CCB1', width: 1, dash: '0',   arrow: true,  label: '应用' },
  ds:        { stroke: '#F56C6C', width: 1, dash: '0',   arrow: true,  label: '来源' },
  ext:       { stroke: '#36CFC9', width: 1, dash: '0',   arrow: true,  label: '' },
}

/* ============ 演示数据 (mock, 以"水文测站"为中心) ============ */
function buildMock() {
  return {
    center: {
      id: 'center', cn: '水文测站', en: 'HydrologyStation',
      rid: 'ri.ont.water.hydrology-station', namespace: 'water:hydrology',
      shape: 'rect', dim: 'center', kind: 'class',
      desc: '水文领域的核心监测对象, 承载水位 / 流量 / 水质等观测数据。',
      properties: [
        { code: 'latitude',           cn: '纬度',     tag: '123', kind: 'num', isKey: false },
        { code: 'longitude',          cn: '经度',     tag: '123', kind: 'num', isKey: false },
        { code: 'stationCode',        cn: '测站编码', tag: 'abc', kind: 'str', isKey: true  },
        { code: 'stationName',        cn: '测站名称', tag: 'abc', kind: 'str', isKey: false },
        { code: 'waterLevel',         cn: '水位',     tag: '123', kind: 'num', isKey: false },
        { code: 'affiliatedRiverName', cn: '所属河流', tag: 'abc', kind: 'str', isKey: false }
      ]
    },
    inherit: [
      { id: 'inh-p1', cn: '基础实体',     en: 'BaseEntity',           kind: 'parent',    desc: '所有领域实体的根类' },
      { id: 'inh-p2', cn: '监测设施',     en: 'MonitorFacility',      kind: 'parent',    desc: '观测/检测类设施的抽象父类' },
      { id: 'inh-c1', cn: '地表水测站',   en: 'SurfaceWaterStation',  kind: 'child' },
      { id: 'inh-c2', cn: '地下水测站',   en: 'GroundwaterStation',   kind: 'child' },
      { id: 'inh-i1', cn: '可观测',       en: 'IObservable',          kind: 'interface', desc: '声明可被监测/采样的契约' },
      { id: 'inh-i2', cn: '地理定位',     en: 'IGeoLocated',          kind: 'interface', desc: '声明含经纬度/Geohash 字段' }
    ],
    biz: [
      { id: 'biz-ind', cn: '水利行业', en: 'water-industry', kind: 'industry',  namespace: 'industry', version: 'v1.0' },
      { id: 'biz-dom', cn: '水文领域', en: 'hydrology',      kind: 'domain',    namespace: 'water:hydrology', version: 'v1.0' },
      { id: 'biz-sub', cn: '水文监测', en: 'hydro-monitor',  kind: 'subdomain', namespace: 'water:hydrology.monitor', version: 'v1.0' },
      { id: 'biz-grp', cn: '地表水站', en: 'surface-water',  kind: 'group',     namespace: 'water:hydrology.monitor.surface', version: 'v1.0' }
    ],
    app: [
      { id: 'app-a1', cn: '新增测站',    en: 'addStation',          kind: 'action',    desc: '触发测站建档流程, 校验编码唯一性' },
      { id: 'app-a2', cn: '校准设备',    en: 'calibrate',           kind: 'action',    desc: '对测站观测设备执行远程校准' },
      { id: 'app-a3', cn: '导出报告',    en: 'exportReport',        kind: 'action' },
      { id: 'app-f1', cn: '平均水位计算', en: 'avgWaterLevel',       kind: 'function',  desc: '按日/月聚合计算测站平均水位' },
      { id: 'app-f2', cn: '趋势预测',    en: 'trendForecast',       kind: 'function' },
      { id: 'app-t1', cn: '标准测站',    en: 'StandardStation',     kind: 'typeClass', status: '启用' },
      { id: 'app-t2', cn: '应急测站',    en: 'EmergencyStation',    kind: 'typeClass', deprecated: true, desc: '应急场景临时测站类型, 已弃用' }
    ],
    ext: [
      { id: 'ext-1', cn: '河流',     en: 'River',         kind: 'oneToMany',  cardinality: 'one_to_many',  desc: '所属河流(归属关系)' },
      { id: 'ext-2', cn: '流域',     en: 'Basin',         kind: 'oneToOne',   cardinality: 'one_to_one' },
      { id: 'ext-3', cn: '管理单位', en: 'AdminUnit',     kind: 'manyToOne',  cardinality: 'many_to_one' },
      { id: 'ext-4', cn: '监测断面', en: 'Section',       kind: 'oneToMany',  cardinality: 'one_to_many' },
      { id: 'ext-5', cn: '水利工程', en: 'WaterProject',  kind: 'manyToMany', cardinality: 'many_to_many', desc: '复合主键关联 (project_id + station_code)' }
    ],
    prop: [
      { id: 'prop-sh1', cn: '地理坐标',   en: 'GeoCoords',     kind: 'shared',   desc: '共享属性: 经纬度 + 高程' },
      { id: 'prop-sh2', cn: 'Geohash',    en: 'GeohashIndex',  kind: 'shared',   desc: '空间索引地理属性 (派生)' },
      { id: 'prop-en1', cn: '测站类型',   en: 'StationTypeEnum', kind: 'enum',   desc: '枚举: 水文/水位/流量/水质 4 类' },
      { id: 'prop-en2', cn: '运行状态',   en: 'StatusEnum',    kind: 'enum' },
      { id: 'prop-vt1', cn: '数值',       en: 'xsd:decimal',   kind: 'value' },
      { id: 'prop-vt2', cn: '字符串',     en: 'xsd:string',    kind: 'value' }
    ],
    rule: [
      { id: 'rule-eq1', cn: '监测站',     en: 'MonitoringStation',    kind: 'equiv',    desc: 'OWL 等价类: 与本对象 owl:equivalentClass' },
      { id: 'rule-dj1', cn: '行政单位',   en: 'AdminUnit',            kind: 'disjoint', desc: 'owl:disjointWith 声明不等价' },
      { id: 'rule-ex1', cn: '人员',       en: 'HumanBeing',           kind: 'exclude',  desc: 'SWRL 互斥规则' },
      { id: 'rule-un1', cn: '测站或仪器', en: 'StationOrInstrument',  kind: 'union',    desc: '并集类: HydrologyStation ∪ Instrument' }
    ],
    ds: [
      { id: 'ds-main', cn: '水文测站表', en: 't_hydrology_station', kind: 'mainTable',  status: '连通', namespace: 'mysql.water_db' },
      { id: 'ds-dm',   cn: '达梦数据库', en: 'DM-DB',                kind: 'nativeDB',   status: '连通', desc: '国产数据库' },
      { id: 'ds-api',  cn: '水文局接口', en: 'HydroAPI',             kind: 'api',       status: '连通', desc: '外部 REST 接口' },
      { id: 'ds-dev',  cn: '自动监测设备', en: 'AutoSensor',         kind: 'device',    status: '连通' },
      { id: 'ds-man',  cn: '人工录入',   en: 'ManualEntry',          kind: 'manual',    status: '启用' }
    ]
  }
}
const MOCK = ref(buildMock())

/* 边定义 (中心 → 各维度节点, 行业链特殊处理) */
function buildEdges() {
  const m = MOCK.value
  const arr = []
  let id = 0
  const push = (source, target, style) => arr.push({ id: ++id, source, target, ...EDGE_STYLES[style] })

  // 继承实现
  m.inherit.forEach(n => {
    if (n.kind === 'parent')    push('center', n.id, 'inherit')   // 子→父 (向上)
    if (n.kind === 'child')     push(n.id, 'center', 'inherit')   // 子→父
    if (n.kind === 'interface') push('center', n.id, 'interface')
  })
  // 规则约束
  m.rule.forEach(n => push('center', n.id, n.kind))
  // 属性体系
  m.prop.forEach(n => push('center', n.id, 'prop'))
  // 行业层级 (链式: industry → domain → subdomain → group → center)
  const chain = ['biz-ind','biz-dom','biz-sub','biz-grp']
  for (let i = 0; i < chain.length - 1; i++) push(chain[i], chain[i+1], 'biz')
  push('biz-grp', 'center', 'biz')
  // 应用能力
  m.app.forEach(n => push('center', n.id, 'app'))
  // 数据来源
  m.ds.forEach(n => push('center', n.id, 'ds'))
  // 外部关联 (带基数 label)
  m.ext.forEach(n => {
    const e = { id: ++id, source: 'center', target: n.id, ...EDGE_STYLES.ext, label: cardSym(n.cardinality) }
    arr.push(e)
  })
  return arr
}
const allEdges = computed(() => buildEdges())

/* ============ 布局 (中心聚焦 + 分区固定) ============ */
const svgW = 1600, svgH = 900
const NODE_W = 150, NODE_H = 46
const CENTER_W_COMPACT = 240, CENTER_H_COMPACT = 56
const CENTER_W_EXPAND  = 280, CENTER_H_EXPAND  = 250

const pos = reactive({})

function computeLayout() {
  const m = MOCK.value
  const cx = svgW/2, cy = svgH/2
  Object.keys(pos).forEach(k => delete pos[k])
  pos['center'] = { x: cx, y: cy }

  // 继承: 父 (上)、子 (下)、接口 (左下/右下)
  spreadGroup(m.inherit.filter(n => n.kind === 'parent'),    cx,        cy - 260, 'horiz', 200)
  spreadGroup(m.inherit.filter(n => n.kind === 'child'),     cx - 120,  cy + 260, 'horiz', 200)
  spreadGroup(m.inherit.filter(n => n.kind === 'interface'), cx + 200,  cy + 260, 'horiz', 200)

  // 行业链 (顶部右侧, 链式排列)
  const bizChain = ['biz-ind','biz-dom','biz-sub','biz-grp'].map(id => m.biz.find(n => n.id === id)).filter(Boolean)
  bizChain.forEach((n, i) => {
    pos[n.id] = { x: cx - 360 + i * 180, y: cy - 380 }
  })

  // 数据源 (左上)
  spreadGroup(m.ds, cx - 600, cy - 200, 'vert', 80)

  // 规则约束 (左)
  spreadGroup(m.rule, cx - 600, cy + 80, 'vert', 80)

  // 属性体系 (左下扇区)
  spreadGroup(m.prop, cx - 380, cy + 240, 'grid', 0, 3)

  // 应用能力 (右)
  spreadGroup(m.app, cx + 580, cy, 'vert', 80)

  // 外部关联 (右上 + 右下圆弧)
  spreadGroup(m.ext, cx + 380, cy - 240, 'vert', 110)
}

function spreadGroup(list, baseX, baseY, mode, gap, cols) {
  if (!list || !list.length) return
  if (mode === 'horiz') {
    const startX = baseX - (list.length - 1) * gap / 2
    list.forEach((n, i) => { pos[n.id] = { x: startX + i * gap, y: baseY } })
  } else if (mode === 'vert') {
    const startY = baseY - (list.length - 1) * gap / 2
    list.forEach((n, i) => { pos[n.id] = { x: baseX, y: startY + i * gap } })
  } else if (mode === 'grid') {
    const c = cols || 3
    list.forEach((n, i) => {
      const r = Math.floor(i / c), col = i % c
      pos[n.id] = { x: baseX + col * 170, y: baseY + r * 60 }
    })
  }
}

const centerExpanded = computed(() => dimOn.prop)

/* 把节点装上 viewport 信息 (尺寸 + 染色) */
const visibleNodes = computed(() => {
  const all = []
  const m = MOCK.value
  // center
  const cw = centerExpanded.value ? CENTER_W_EXPAND : CENTER_W_COMPACT
  const ch = centerExpanded.value ? CENTER_H_EXPAND : CENTER_H_COMPACT
  const cd = DIM_BY_KEY['inherit']
  all.push({
    ...m.center,
    x: pos['center']?.x || svgW/2, y: pos['center']?.y || svgH/2,
    w: cw, h: ch,
    shape: 'rect',
    fill: '#fff',
    stroke: cd.color
  })
  // 七大维度节点
  DIMENSIONS.forEach(d => {
    if (!dimOn[d.key]) return
    const list = m[d.key] || []
    list.forEach(n => {
      const p = pos[n.id]
      if (!p) return
      all.push({
        ...n,
        dim: d.key,
        x: p.x, y: p.y,
        w: NODE_W, h: NODE_H,
        shape: n.shape || d.shape,
        fill: d.color + '1A',     // 10% 透明背景
        stroke: d.color
      })
    })
  })
  return all
})

const visibleEdges = computed(() => {
  const ids = new Set(visibleNodes.value.map(n => n.id))
  return allEdges.value
    .filter(e => ids.has(e.source) && ids.has(e.target))
    .map(e => {
      const s = visibleNodes.value.find(n => n.id === e.source)
      const t = visibleNodes.value.find(n => n.id === e.target)
      // 计算从矩形边缘出发 (不从中心) 的路径端点
      const seg = clipToBox(s, t)
      return { ...e, x1: seg.x1, y1: seg.y1, x2: seg.x2, y2: seg.y2 }
    })
})

const dimCounts = computed(() => {
  const m = MOCK.value
  const out = {}
  DIMENSIONS.forEach(d => { out[d.key] = (m[d.key] || []).length })
  return out
})
const allOn = computed(() => DIMENSIONS.every(d => dimOn[d.key]))
function toggleAll() {
  const target = !allOn.value
  DIMENSIONS.forEach(d => { dimOn[d.key] = target })
}

/* ============ 形状路径 ============ */
function shapePath(shape, w, h) {
  const cx = w/2, cy = h/2
  if (shape === 'rect') return `M4 0 H${w-4} A4 4 0 0 1 ${w} 4 V${h-4} A4 4 0 0 1 ${w-4} ${h} H4 A4 4 0 0 1 0 ${h-4} V4 A4 4 0 0 1 4 0 Z`
  if (shape === 'diamond') return `M${cx} 0 L${w} ${cy} L${cx} ${h} L0 ${cy} Z`
  if (shape === 'circle') {
    const rx = w/2, ry = h/2
    return `M0 ${cy} a${rx} ${ry} 0 1 0 ${w} 0 a${rx} ${ry} 0 1 0 -${w} 0 Z`
  }
  if (shape === 'pentagon') {
    const rx = w/2 - 2, ry = h/2 - 2
    const pts = []
    for (let i = 0; i < 5; i++) {
      const a = -Math.PI/2 + i * 2*Math.PI/5
      pts.push(`${(cx + rx*Math.cos(a)).toFixed(1)},${(cy + ry*Math.sin(a)).toFixed(1)}`)
    }
    return `M${pts.join(' L')} Z`
  }
  if (shape === 'parallel') {
    const sk = Math.min(14, w/4)
    return `M${sk} 0 L${w} 0 L${w-sk} ${h} L0 ${h} Z`
  }
  if (shape === 'hexagon') {
    const rx = w/2 - 1, ry = h/2 - 1
    const pts = []
    for (let i = 0; i < 6; i++) {
      const a = i * 2*Math.PI/6
      pts.push(`${(cx + rx*Math.cos(a)).toFixed(1)},${(cy + ry*Math.sin(a)).toFixed(1)}`)
    }
    return `M${pts.join(' L')} Z`
  }
  if (shape === 'cloud') {
    const u = h * 0.32
    return `M${u},${h-2}
            C ${-u*0.2},${h*0.5} ${-u*0.1},${u*0.4} ${u*1.2},${u*0.7}
            C ${u*1.6},${-u*0.4} ${u*3.2},${-u*0.4} ${u*3.7},${u*0.7}
            C ${u*5},${-u*0.2} ${w + u*0.3},${u*1.5} ${w-u*0.3},${h*0.55}
            C ${w + u*0.2},${h*1.1} ${u*0.5},${h*1.2} ${u},${h-2} Z`
  }
  return `M0 0 H${w} V${h} H0 Z`
}

/* 把线段从矩形外接框中心裁到边界 */
function clipToBox(a, b) {
  if (!a || !b) return { x1: 0, y1: 0, x2: 0, y2: 0 }
  const dx = b.x - a.x, dy = b.y - a.y
  const angle = Math.atan2(dy, dx)
  const aHalfW = (a.w || NODE_W)/2 + 2
  const aHalfH = (a.h || NODE_H)/2 + 2
  const bHalfW = (b.w || NODE_W)/2 + 6   // 留 marker 头空间
  const bHalfH = (b.h || NODE_H)/2 + 6
  const x1 = a.x + clampToBox(angle, aHalfW, aHalfH).x
  const y1 = a.y + clampToBox(angle, aHalfW, aHalfH).y
  const x2 = b.x - clampToBox(angle, bHalfW, bHalfH).x
  const y2 = b.y - clampToBox(angle, bHalfW, bHalfH).y
  return { x1, y1, x2, y2 }
}
function clampToBox(angle, halfW, halfH) {
  const cos = Math.cos(angle), sin = Math.sin(angle)
  const tx = halfW / Math.abs(cos || 1e-6)
  const ty = halfH / Math.abs(sin || 1e-6)
  const t = Math.min(tx, ty)
  return { x: cos * t, y: sin * t }
}
function edgePath(e) {
  return `M${e.x1.toFixed(1)},${e.y1.toFixed(1)} L${e.x2.toFixed(1)},${e.y2.toFixed(1)}`
}

/* ============ 交互: 平移 / 缩放 / 拖拽 ============ */
const zoom = ref(1)
const pan = reactive({ x: 0, y: 0 })
const svgRef = ref(null)
let panFrom = null
function onCanvasDown(ev) {
  if (ev.target.closest('.og-node-g') || ev.target.closest('.og-edge')) return
  panFrom = { x: ev.clientX - pan.x, y: ev.clientY - pan.y }
  window.addEventListener('mousemove', onCanvasMove)
  window.addEventListener('mouseup', onCanvasUp)
}
function onCanvasMove(ev) { if (panFrom) { pan.x = ev.clientX - panFrom.x; pan.y = ev.clientY - panFrom.y } }
function onCanvasUp() { panFrom = null; window.removeEventListener('mousemove', onCanvasMove); window.removeEventListener('mouseup', onCanvasUp) }
function onWheel(ev) {
  const delta = -Math.sign(ev.deltaY) * 0.08
  zoomBy(delta)
}
function zoomBy(d) { zoom.value = Math.max(0.3, Math.min(3, zoom.value + d)) }
function resetZoom() { zoom.value = 1; pan.x = 0; pan.y = 0 }
function fitView() { resetZoom() }
function relayout() { computeLayout(); BL.success('已重新布局') }

/* 节点拖拽 */
let nodeDrag = null
function onNodeDown(n, ev) {
  if (n.id === 'center') return  // 中心固定
  ev.preventDefault()
  nodeDrag = { id: n.id, sx: ev.clientX, sy: ev.clientY, nx: n.x, ny: n.y }
  window.addEventListener('mousemove', onNodeDragMove)
  window.addEventListener('mouseup', onNodeDragUp)
}
function onNodeDragMove(ev) {
  if (!nodeDrag) return
  const dx = (ev.clientX - nodeDrag.sx) / zoom.value
  const dy = (ev.clientY - nodeDrag.sy) / zoom.value
  pos[nodeDrag.id] = { x: nodeDrag.nx + dx, y: nodeDrag.ny + dy }
}
function onNodeDragUp() { nodeDrag = null; window.removeEventListener('mousemove', onNodeDragMove); window.removeEventListener('mouseup', onNodeDragUp) }

/* ============ 详情抽屉 ============ */
const drawerNode = ref(null)
const selectedId = ref('')
function onNodeClick(n) {
  drawerNode.value = n
  selectedId.value = n.id
}
/* ============ 链接关系编辑抽屉 (复用 LinkTypeEditor) ============ */
const editorOpen = ref(false)
const editorLinkId = ref('')
const hoverEdgeId = ref(null)
const allClasses = ref([])

async function loadClasses() {
  const list = await resourceApi.classes().catch(() => [])
  allClasses.value = Array.isArray(list) ? list : (list?.rows || list?.data || [])
}

function onEdgeClick(e) {
  editorLinkId.value = e.linkId || ''   // mock 暂无真实 id, 进入创建模式; 联调后 ext 边带上 linkId 即可加载详情
  editorOpen.value = true
}
/* ============ 全屏 / 导出 ============ */
const rootRef = ref(null)
const isFull = ref(false)
function toggleFull() {
  const el = rootRef.value
  if (!el) return
  if (!document.fullscreenElement) {
    el.requestFullscreen?.()
    isFull.value = true
  } else {
    document.exitFullscreen?.()
    isFull.value = false
  }
}
function onFsChange() { isFull.value = !!document.fullscreenElement }
onMounted(() => { document.addEventListener('fullscreenchange', onFsChange); computeLayout(); loadClasses() })
onBeforeUnmount(() => document.removeEventListener('fullscreenchange', onFsChange))

function onExport(fmt) { BL.info(`导出 ${fmt} 待联调 (后端 ${fmt === 'OWL' ? 'OWL/RDF 序列化' : '图像/JSON 快照'} 接口对接中)`) }
function onShare() { BL.success('分享链接已复制到剪贴板 (待联调)') }

/* ============ 辅助标签 ============ */
function truncate(s, n) { if (!s) return ''; return s.length > n ? s.slice(0, n) + '…' : s }
function dimLabel(k) { return DIM_BY_KEY[k]?.label || '—' }
function dimColor(k) { return DIM_BY_KEY[k]?.color || '#86909c' }
function kindLabel(n) {
  const map = {
    parent: '父类', child: '子类', interface: '实现接口',
    equiv: '等价类', disjoint: '不等价类', exclude: '互斥类', union: '并集类',
    shared: '共享属性', enum: '枚举类型', value: '值类型',
    industry: '行业', domain: '领域', subdomain: '子领域', group: '分组',
    action: '应用动作', function: '自定义函数', typeClass: '类型类',
    mainTable: '主数据集', nativeDB: '国产数据库', api: '接口数据源', device: '设备数据源', manual: '人工录入',
    oneToOne: '一对一关联', oneToMany: '一对多关联', manyToOne: '多对一关联', manyToMany: '多对多关联',
    class: '本体类'
  }
  return map[n.kind] || n.kind || '—'
}
function cardLabel(c) {
  return ({ one_to_one: '一对一', one_to_many: '一对多', many_to_one: '多对一', many_to_many: '多对多' })[c] || c
}
function cardSym(c) {
  const m = { one_to_one: '1:1', one_to_many: '1:*', many_to_one: '*:1', many_to_many: '*:*' }
  return m[c] || ''
}

/* classId 切换 → 重建 mock + 布局 (真实联调时这里改成 fetch) */
watch(() => props.classId, () => {
  MOCK.value = buildMock()
  nextTick(computeLayout)
})
</script>

<style scoped>
/* 根容器 */
.og-root {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 600px;
  background: #fafbfc;
  overflow: hidden;
  font-family: "Microsoft YaHei", "微软雅黑", sans-serif;
}
.og-root.is-full { background: #fff; }

/* 画布 */
.og-svg {
  width: 100%; height: 100%;
  cursor: grab;
  user-select: none;
}
.og-svg:active { cursor: grabbing; }

/* —— 节点 (SVG) —— */
.og-node-g { cursor: pointer; }
.og-node-g.is-selected path:first-child { stroke-width: 2.5; filter: drop-shadow(0 2px 6px rgba(22, 93, 255, .35)); }
.og-node-g.is-deprecated { opacity: .55; }
.og-node-cn { font-size: 12px; font-weight: 500; fill: #303133; pointer-events: none; }
.og-node-en { font-size: 10px; fill: #909399; font-family: "Consolas", "Monaco", monospace; pointer-events: none; }

/* —— 中心节点 (HTML via foreignObject) —— */
.og-center {
  width: 100%; height: 100%;
  background: #fff;
  border-radius: 8px;
  box-sizing: border-box;
  display: flex; flex-direction: column;
  padding: 8px 10px;
  overflow: hidden;
}
.og-center-compact { justify-content: center; }
.og-center-hd { display: flex; align-items: center; gap: 8px; padding-bottom: 6px; }
.og-center-name { flex: 1; display: flex; align-items: baseline; gap: 4px; min-width: 0; }
.og-center-cn { font-size: 14px; font-weight: 700; color: #303133; }
.og-center-en { font-size: 11px; color: #909399; font-family: Consolas, Monaco, monospace; }
.og-center-tag {
  font-size: 10px; padding: 2px 6px; border-radius: 10px;
  background: #ecf3ff; color: #409EFF; flex-shrink: 0;
}

/* 中心展开: 属性列表 */
.og-prop-list { list-style: none; margin: 4px 0 0; padding: 0; overflow-y: auto; }
.og-prop-list::-webkit-scrollbar { width: 4px; }
.og-prop-list::-webkit-scrollbar-thumb { background: #e0e0e0; border-radius: 2px; }
.og-prop-item {
  display: flex; align-items: center; gap: 6px;
  padding: 3px 4px; border-radius: 3px;
  font-size: 11px;
}
.og-prop-item:hover { background: #f5f7fa; }
.og-prop-tag {
  width: 22px; height: 16px; line-height: 16px;
  text-align: center; border-radius: 3px;
  font-size: 9px; font-weight: 600;
  flex-shrink: 0;
}
.og-prop-tag.is-num { background: #e8f3ff; color: #409EFF; }
.og-prop-tag.is-str { background: #fef0e6; color: #FF7D00; }
.og-prop-cn { color: #303133; flex-shrink: 0; }
.og-prop-en { color: #909399; font-family: Consolas, Monaco, monospace; }
.og-prop-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: #c0c4cc;
  margin-left: auto; flex-shrink: 0;
}
.og-prop-dot.is-key { background: #f56c6c; }

/* —— 连线 (group: 可见细线 + 透明粗线命中区) —— */
.og-edge-group { cursor: pointer; }
.og-edge {
  pointer-events: none;        /* 命中由 .og-edge-hit 处理, 避免双重事件 */
  transition: stroke-width .15s, filter .15s;
}
.og-edge-hit {
  fill: none;
  stroke: transparent;
  stroke-width: 14;             /* 命中区 14px, 比可见线粗 7-14 倍, 容易点中 */
  pointer-events: stroke;
}
/* hover 时: 加粗可见线 + 微微发光 */
.og-edge-group:hover .og-edge,
.og-edge-group.is-hot .og-edge {
  stroke-width: 2.5;
  filter: drop-shadow(0 0 3px currentColor);
}
.og-edge-lbl {
  font-size: 10px; fill: #606266;
  background: rgba(255,255,255,.8);
  pointer-events: none;
}

/* —— 工具栏 (左上) —— */
.og-toolbar {
  position: absolute; top: 12px; left: 12px; z-index: 5;
  display: flex; flex-direction: column; gap: 2px;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 4px; box-shadow: 0 2px 6px rgba(0,0,0,.06);
}
.og-tool {
  width: 30px; height: 30px;
  background: transparent; border: 0; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  cursor: pointer; color: #606266;
  transition: background-color .15s;
}
.og-tool:hover { background: #f0f4ff; color: #409EFF; }
.og-tool.is-on { background: #409EFF; color: #fff; }
.og-tool-div { height: 1px; background: #ebeef5; margin: 2px 0; }

/* —— 导出 (右上) —— */
.og-export {
  position: absolute; top: 12px; right: 12px; z-index: 5;
  display: inline-flex; gap: 4px;
}

/* —— 图例 (左下) —— */
.og-legend {
  position: absolute; bottom: 16px; left: 16px; z-index: 5;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 6px;
  padding: 8px 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
  min-width: 200px;
  font-size: 11px;
}
.og-legend-hd {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; font-weight: 600; color: #303133;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 6px;
}
.og-legend-item {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 6px; border-radius: 3px;
  cursor: pointer; user-select: none;
  transition: background-color .15s;
}
.og-legend-item:hover { background: #f5f7fa; }
.og-legend-item.is-off { opacity: .4; }
.og-legend-item.is-off .og-legend-lbl { color: #909399; text-decoration: line-through; }
.og-legend-ic { flex-shrink: 0; }
.og-legend-lbl { flex: 1; color: #303133; }
.og-legend-cnt {
  font-size: 10px; padding: 1px 6px; border-radius: 8px;
  background: #f0f2f5; color: #606266; min-width: 18px; text-align: center;
}
.og-legend-tip {
  font-size: 10px; color: #909399;
  padding-top: 6px; border-top: 1px dashed #ebeef5; margin-top: 4px;
}

/* —— 右侧详情抽屉 —— */
.og-drawer {
  position: absolute; top: 0; right: 0; bottom: 0;
  width: 360px;
  background: #fff;
  border-left: 1px solid #ebeef5;
  box-shadow: -4px 0 12px rgba(0,0,0,.06);
  transform: translateX(100%);
  transition: transform .25s ease;
  z-index: 6;
  display: flex; flex-direction: column;
}
.og-drawer.is-open { transform: translateX(0); }
.og-drawer-inner { height: 100%; display: flex; flex-direction: column; }
.og-drawer-hd {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 16px 16px 12px;
  border-bottom: 1px solid #ebeef5;
}
.og-drawer-shape { flex-shrink: 0; padding-top: 4px; }
.og-drawer-title { flex: 1; min-width: 0; }
.og-drawer-cn { font-size: 14px; font-weight: 700; color: #303133; }
.og-drawer-en { font-size: 11px; color: #909399; margin-top: 2px; }
.og-drawer-body {
  flex: 1; overflow-y: auto;
  padding: 12px 16px 16px;
}
.og-kv {
  display: flex; padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
  font-size: 12px;
}
.og-kv:last-child { border-bottom: 0; }
.og-kv-l { width: 80px; color: #909399; flex-shrink: 0; }
.og-kv-r { flex: 1; color: #303133; word-break: break-all; }
.og-kv-block { display: block; }
.og-kv-block .og-kv-l { width: auto; margin-bottom: 4px; font-weight: 500; }
.og-kv-text { line-height: 1.55; }
.og-drawer-props { list-style: none; margin: 4px 0 0; padding: 0; }
.og-drawer-props li {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; border-bottom: 1px dashed #f0f2f5;
  font-size: 12px;
}

/* 全屏适配 */
.og-root.is-full .og-svg { width: 100vw; height: 100vh; }
</style>
