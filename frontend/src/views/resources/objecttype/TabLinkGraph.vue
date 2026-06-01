<template>
  <div class="lg-root">
    <!-- 顶部导航栏 -->
    <header class="lg-topnav">
      <div class="lg-title-l">
        <span class="lg-title">链接类型</span>
        <span class="bl-muted lg-title-en">(Link Types)</span>
        <span class="lg-count-tag">共 {{ links.length }} 条 <span class="bl-muted">(Total: {{ links.length }})</span></span>
      </div>
      <button class="bl-btn bl-btn-primary bl-btn-sm" @click="openCreate" :disabled="mode === 'view'">
        <span v-html="BL.icon('plus', 12, '#fff')"></span>
        <span style="margin-left:4px">新建链接 (New Link)</span>
      </button>
    </header>

    <!-- 主画布 + 浮动工具栏 -->
    <div ref="canvasWrap" class="lg-canvas-wrap"
         @mousedown="onCanvasMouseDown"
         @wheel.prevent="onWheel">
      <!-- 浮动工具栏 (左侧) -->
      <aside class="lg-toolbar">
        <button :class="['lg-tool', mode==='select' && 'is-on']" title="选择模式" @click="mode='select'">
          <span v-html="BL.icon('cursor', 14)"></span>
        </button>
        <button :class="['lg-tool', mode==='view' && 'is-on']" title="查看模式 (只读)" @click="mode='view'">
          <span v-html="BL.icon('eye', 14)"></span>
        </button>
        <div class="lg-tool-divider"></div>
        <button class="lg-tool" title="重新布局" @click="relayout">
          <span v-html="BL.icon('grid', 14)"></span>
        </button>
        <button class="lg-tool" title="适配最优视角" @click="fitView">
          <span v-html="BL.icon('move', 14)"></span>
        </button>
        <div class="lg-tool-divider"></div>
        <button class="lg-tool" title="放大" @click="zoomBy(0.1)">
          <span v-html="BL.icon('zoomIn', 14)"></span>
        </button>
        <button class="lg-tool" title="缩小" @click="zoomBy(-0.1)">
          <span v-html="BL.icon('zoomOut', 14)"></span>
        </button>
        <button class="lg-tool" title="原始比例 1:1" @click="resetZoom">
          <span v-html="BL.icon('maximize', 14)"></span>
        </button>
      </aside>

      <!-- SVG 画布 -->
      <svg ref="svgRef" class="lg-svg" :viewBox="`0 0 ${svgW} ${svgH}`">
        <defs>
          <!-- 箭头标记 (虚线: 虚拟节点连线) -->
          <marker id="lg-arrow" viewBox="0 0 10 10" refX="6" refY="5" markerWidth="6" markerHeight="6" orient="auto">
            <path d="M0 0L10 5L0 10z" fill="#86909c" />
          </marker>
        </defs>
        <g :transform="`translate(${pan.x},${pan.y}) scale(${zoom})`">
          <!-- 连线 -->
          <g v-for="l in viewLinks" :key="'L' + l.id">
            <path :d="linkPath(l)"
                  :class="['lg-link', l.isVirtual && 'is-virtual', selectedLinkId === l.id && 'is-selected']"
                  :stroke-dasharray="l.isVirtual ? '6,5' : '0'"
                  @click.stop="onLinkClick(l)" />
            <!-- 链接点击容错: 宽透明 hitline (10px) -->
            <path :d="linkPath(l)" class="lg-link-hit" @click.stop="onLinkClick(l)" />
            <!-- 基数标注: 源端 / 目标端 -->
            <text v-if="!l.isVirtual"
                  :x="l.sourceLabel.x" :y="l.sourceLabel.y"
                  class="lg-card-label">{{ cardSymbol(l, 'source') }}</text>
            <text v-if="!l.isVirtual"
                  :x="l.targetLabel.x" :y="l.targetLabel.y"
                  class="lg-card-label">{{ cardSymbol(l, 'target') }}</text>
          </g>

          <!-- 节点 -->
          <g v-for="n in viewNodes" :key="'N' + n.id"
             :transform="`translate(${n.x - NODE_W/2},${n.y - NODE_H/2})`"
             class="lg-node-g"
             :class="{ 'is-center': n.isCenter, 'is-virtual': n.isVirtual }"
             @mousedown.stop="onNodeMouseDown(n, $event)"
             @click.stop="onNodeClick(n)">
            <!-- 卡片背景 -->
            <rect class="lg-node-bg" :width="NODE_W" :height="NODE_H" rx="6" />
            <!-- 左侧彩色图标块 -->
            <rect class="lg-node-icbg" x="6" y="6" width="22" height="22" rx="4" :fill="n.color || '#86909c'" />
            <foreignObject x="6" y="6" width="22" height="22" v-html="`<div style='display:flex;align-items:center;justify-content:center;width:100%;height:100%;color:#fff'>${BL.icon(n.icon || 'box', 12, '#fff')}</div>`" />
            <!-- 中文名 -->
            <text class="lg-node-cn" x="34" y="20" :text-anchor="'start'">{{ truncate(n.cn || n.api_name, 16) }}</text>
            <!-- 英文名 -->
            <text class="lg-node-en" x="34" y="34" :text-anchor="'start'">{{ truncate(n.en || '', 18) }}</text>
          </g>
        </g>
      </svg>
    </div>

    <!-- 节点详情弹窗 -->
    <div v-if="popupNode" class="bl-modal-mask" @click.self="popupNode = null">
      <div class="bl-modal lg-popup">
        <div class="bl-modal-hd">对象详情</div>
        <div class="bl-modal-body">
          <div class="lg-kv"><span class="lg-kv-l">中文名</span><span class="lg-kv-r">{{ popupNode.cn || '—' }}</span></div>
          <div class="lg-kv"><span class="lg-kv-l">英文名</span><span class="lg-kv-r bl-mono">{{ popupNode.en || popupNode.api_name }}</span></div>
          <div class="lg-kv"><span class="lg-kv-l">类型</span>
            <span :class="['bl-tag', popupNode.isCenter ? 'bl-tag-primary' : popupNode.isVirtual ? 'bl-tag-warning' : '']">
              {{ popupNode.isCenter ? '中心主对象' : popupNode.isVirtual ? '虚拟节点' : '关联对象' }}
            </span>
          </div>
          <div v-if="popupNode.category_code" class="lg-kv">
            <span class="lg-kv-l">所属领域</span><span class="lg-kv-r">{{ popupNode.category_code }}</span>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="popupNode = null">关闭</button>
        </div>
      </div>
    </div>

    <!-- 链接详情弹窗 -->
    <div v-if="popupLink" class="bl-modal-mask" @click.self="popupLink = null">
      <div class="bl-modal lg-popup">
        <div class="bl-modal-hd">链接详情</div>
        <div class="bl-modal-body">
          <div class="lg-kv"><span class="lg-kv-l">名称</span><span class="lg-kv-r">{{ popupLink.display_name || popupLink.api_name }}</span></div>
          <div class="lg-kv"><span class="lg-kv-l">API</span><span class="lg-kv-r bl-mono">{{ popupLink.api_name }}</span></div>
          <div class="lg-kv"><span class="lg-kv-l">源对象</span><span class="lg-kv-r">{{ classLabel(popupLink.source_class_id) }}</span></div>
          <div class="lg-kv"><span class="lg-kv-l">目标对象</span><span class="lg-kv-r">{{ classLabel(popupLink.target_class_id) }}</span></div>
          <div class="lg-kv"><span class="lg-kv-l">基数</span>
            <span class="bl-tag">{{ cardLabel(popupLink.cardinality) }}</span>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button v-if="mode === 'select'" class="bl-btn bl-btn-danger" @click="onDeleteLink(popupLink)">
            <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除</span>
          </button>
          <span style="flex:1"></span>
          <button class="bl-btn" @click="popupLink = null">关闭</button>
        </div>
      </div>
    </div>

    <!-- 新建链接弹窗 -->
    <div v-if="createOpen" class="bl-modal-mask" @click.self="createOpen = false">
      <div class="bl-modal lg-create">
        <div class="bl-modal-hd">新建链接</div>
        <div class="bl-modal-body bl-col" style="gap:10px">
          <FieldRow label="名称 *" inline>
            <input class="bl-input" v-model="newLink.display_name" placeholder="例: 测站所属河流" />
          </FieldRow>
          <FieldRow label="API *" inline hint="snake_case · 全局唯一">
            <input class="bl-input bl-mono" v-model="newLink.api_name" placeholder="例: locatedInRiver" />
          </FieldRow>
          <FieldRow label="" inline>
            <div class="lg-source-target">
              <select class="bl-input" v-model="newLink.source_class_id">
                <option value="">— 源对象 —</option>
                <option v-for="c in classOptions" :key="'s' + c.id" :value="c.id">{{ c.cn || c.api_name }}</option>
              </select>
              <button class="bl-btn bl-btn-text bl-btn-icon lg-swap" title="互换源/目标"
                      @click="swapSourceTarget" v-html="BL.icon('refresh', 13)"></button>
              <select class="bl-input" v-model="newLink.target_class_id">
                <option value="">— 目标对象 —</option>
                <option v-for="c in classOptions" :key="'t' + c.id" :value="c.id">{{ c.cn || c.api_name }}</option>
              </select>
            </div>
          </FieldRow>
          <FieldRow label="基数" inline>
            <div class="radio-group">
              <label class="radio-item"><input type="radio" value="one_to_one" v-model="newLink.cardinality" /> 1 : 1</label>
              <label class="radio-item"><input type="radio" value="one_to_many" v-model="newLink.cardinality" /> 1 : *</label>
              <label class="radio-item"><input type="radio" value="many_to_one" v-model="newLink.cardinality" /> * : 1</label>
              <label class="radio-item"><input type="radio" value="many_to_many" v-model="newLink.cardinality" /> * : *</label>
            </div>
          </FieldRow>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="createOpen = false">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!canCreate" @click="onCreate">确定创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'

const props = defineProps({
  classId: { type: String, default: '' }
})

/* —— 数据 —— */
const allClasses = ref([])
const allLinks = ref([])
const centerClass = computed(() => allClasses.value.find(c => c.id === props.classId))

/* 与当前 class 相关的 links */
const links = computed(() => {
  if (!props.classId) return []
  return allLinks.value.filter(l =>
    l.source_class_id === props.classId || l.target_class_id === props.classId
  )
})

/* 关联节点的 class id 集合 (排除中心) */
const relatedClassIds = computed(() => {
  const set = new Set()
  links.value.forEach(l => {
    if (l.source_class_id !== props.classId) set.add(l.source_class_id)
    if (l.target_class_id !== props.classId) set.add(l.target_class_id)
  })
  return [...set]
})

/* —— 状态 —— */
const mode = ref('select')           // 'select' | 'view'
const zoom = ref(1)
const pan = reactive({ x: 0, y: 0 })
const svgW = 1200
const svgH = 700

const NODE_W = 180
const NODE_H = 44

/* 节点位置存储 (id → {x,y}) */
const nodePos = reactive({})

/* 弹窗 */
const popupNode = ref(null)
const popupLink = ref(null)
const selectedLinkId = ref(null)
const createOpen = ref(false)
const newLink = reactive(defaultNewLink())
function defaultNewLink() {
  return { display_name: '', api_name: '', source_class_id: props.classId, target_class_id: '', cardinality: 'many_to_many' }
}

/* —— 加载 —— */
async function loadAll() {
  const [classes, ls] = await Promise.all([
    resourceApi.classes().catch(() => ({})),
    resourceApi.links().catch(() => [])
  ])
  // resourceApi.classes 可能返回 { data: [...] } 或 { rows: [...] } 视后端而定
  allClasses.value = Array.isArray(classes) ? classes : (classes?.rows || classes?.data || classes?.items || [])
  // 字段标准化 (兼容 snake / camel)
  allClasses.value = allClasses.value.map(c => ({
    ...c,
    cn: c.display_name || c.displayName || c.rdfs_label || c.rdfsLabel || c.api_name,
    en: c.api_name || c.apiName || c.rdfs_label,
    color: c.color,
    icon: c.icon || 'box'
  }))
  allLinks.value = ls || []
  relayout()
}
onMounted(loadAll)
watch(() => props.classId, () => { nodePos && Object.keys(nodePos).forEach(k => delete nodePos[k]); loadAll() })

/* —— 布局 (重新排布) —— */
function relayout() {
  Object.keys(nodePos).forEach(k => delete nodePos[k])
  if (!props.classId) return
  const cx = svgW / 2
  const cy = svgH / 2
  // 中心主对象
  nodePos[props.classId] = { x: cx, y: cy }
  // 把相关类左右分组: 偶数索引左,奇数索引右
  const related = [...relatedClassIds.value]
  // 虚拟节点 "新建链接类型" 始终最后,放在右侧最后一行
  const VIRTUAL_ID = '__virtual_new_link__'
  const all = [...related, VIRTUAL_ID]
  const leftIds = all.filter((_, i) => i % 2 === 0)
  const rightIds = all.filter((_, i) => i % 2 === 1)
  layoutColumn(leftIds, cx - 320, cy)
  layoutColumn(rightIds, cx + 320, cy)
}
function layoutColumn(ids, x, centerY) {
  const gap = 70
  const startY = centerY - (ids.length - 1) * gap / 2
  ids.forEach((id, i) => { nodePos[id] = { x, y: startY + i * gap } })
}

/* 视图节点 (含中心 + 关联类 + 虚拟节点) */
const VIRTUAL_NODE = {
  id: '__virtual_new_link__',
  cn: '新建链接类型',
  en: 'Create New Link',
  icon: 'plus',
  color: '#86909c',
  isVirtual: true
}
const viewNodes = computed(() => {
  if (!centerClass.value) return []
  const list = []
  list.push({ ...centerClass.value, isCenter: true, x: nodePos[centerClass.value.id]?.x || svgW/2, y: nodePos[centerClass.value.id]?.y || svgH/2 })
  relatedClassIds.value.forEach(id => {
    const c = allClasses.value.find(x => x.id === id)
    if (!c) return
    const p = nodePos[id] || { x: 0, y: 0 }
    list.push({ ...c, x: p.x, y: p.y })
  })
  const vp = nodePos[VIRTUAL_NODE.id] || { x: 0, y: 0 }
  list.push({ ...VIRTUAL_NODE, x: vp.x, y: vp.y })
  return list
})

/* 视图连线 (含每条 link 的端点 + 基数标签位置) */
const viewLinks = computed(() => {
  const out = []
  links.value.forEach(l => {
    const s = nodePos[l.source_class_id]
    const t = nodePos[l.target_class_id]
    if (!s || !t) return
    const sourceLabel = { x: s.x + (t.x > s.x ? NODE_W/2 + 8 : -NODE_W/2 - 16), y: s.y - 6 }
    const targetLabel = { x: t.x + (s.x > t.x ? NODE_W/2 + 8 : -NODE_W/2 - 16), y: t.y - 6 }
    out.push({ ...l, s, t, sourceLabel, targetLabel })
  })
  // 虚拟连线: 中心 → 虚拟新建节点 (虚线)
  const center = nodePos[props.classId]
  const virtual = nodePos[VIRTUAL_NODE.id]
  if (center && virtual) {
    out.push({
      id: '__virtual_link__', isVirtual: true,
      source_class_id: props.classId, target_class_id: VIRTUAL_NODE.id,
      s: center, t: virtual,
      sourceLabel: { x: 0, y: 0 }, targetLabel: { x: 0, y: 0 }
    })
  }
  return out
})

/* 连线 path: 直线; 中心点连接到节点的"卡片边沿"而不是中心 */
function linkPath(l) {
  const { s, t } = l
  // 计算 source 端: 朝 t 方向退到卡片边沿
  const a = edgePoint(s, t)
  const b = edgePoint(t, s)
  return `M${a.x},${a.y} L${b.x},${b.y}`
}
function edgePoint(from, to) {
  // 算从 from 中心朝 to 方向交到 from 卡片矩形的边沿点
  const dx = to.x - from.x, dy = to.y - from.y
  const hw = NODE_W / 2, hh = NODE_H / 2
  if (dx === 0 && dy === 0) return { x: from.x, y: from.y }
  const tx = dx === 0 ? Infinity : (dx > 0 ? hw / dx : -hw / dx)
  const ty = dy === 0 ? Infinity : (dy > 0 ? hh / dy : -hh / dy)
  const k = Math.min(tx, ty)
  return { x: from.x + k * dx, y: from.y + k * dy }
}

/* —— 缩放 / 平移 —— */
function zoomBy(delta) {
  zoom.value = Math.max(0.5, Math.min(2.0, +(zoom.value + delta).toFixed(2)))
}
function resetZoom() {
  zoom.value = 1; pan.x = 0; pan.y = 0
}
function fitView() {
  if (!viewNodes.value.length) return
  // 计算所有节点的 bbox
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  viewNodes.value.forEach(n => {
    minX = Math.min(minX, n.x - NODE_W/2)
    minY = Math.min(minY, n.y - NODE_H/2)
    maxX = Math.max(maxX, n.x + NODE_W/2)
    maxY = Math.max(maxY, n.y + NODE_H/2)
  })
  const w = maxX - minX, h = maxY - minY
  if (w <= 0 || h <= 0) return
  const padding = 60
  const scaleW = (svgW - padding * 2) / w
  const scaleH = (svgH - padding * 2) / h
  const newZoom = Math.max(0.5, Math.min(1.5, Math.min(scaleW, scaleH)))
  zoom.value = newZoom
  // 居中
  const cx = (minX + maxX) / 2
  const cy = (minY + maxY) / 2
  pan.x = svgW / 2 - cx * newZoom
  pan.y = svgH / 2 - cy * newZoom
}
function onWheel(ev) {
  const delta = ev.deltaY < 0 ? 0.1 : -0.1
  zoomBy(delta)
}

/* —— 平移 (画布拖拽空白) —— */
let panFrom = null
function onCanvasMouseDown(ev) {
  if (mode.value === 'view') return
  // 只在直接点击画布(非节点)时启动平移
  if (ev.target.closest('.lg-node-g, .lg-link, .lg-link-hit, .lg-toolbar')) return
  selectedLinkId.value = null
  panFrom = { x: ev.clientX - pan.x, y: ev.clientY - pan.y }
  window.addEventListener('mousemove', onCanvasMouseMove)
  window.addEventListener('mouseup', onCanvasMouseUp)
}
function onCanvasMouseMove(ev) {
  if (!panFrom) return
  pan.x = ev.clientX - panFrom.x
  pan.y = ev.clientY - panFrom.y
}
function onCanvasMouseUp() {
  panFrom = null
  window.removeEventListener('mousemove', onCanvasMouseMove)
  window.removeEventListener('mouseup', onCanvasMouseUp)
}

/* —— 节点拖拽 —— */
let nodeDragFrom = null
function onNodeMouseDown(node, ev) {
  if (mode.value === 'view') return
  if (node.isCenter) return  // 中心永久固定
  ev.preventDefault()
  const start = { x: ev.clientX, y: ev.clientY, nx: node.x, ny: node.y, id: node.id }
  nodeDragFrom = start
  function move(e) {
    if (!nodeDragFrom) return
    const dx = (e.clientX - start.x) / zoom.value
    const dy = (e.clientY - start.y) / zoom.value
    nodePos[start.id] = { x: start.nx + dx, y: start.ny + dy }
  }
  function up() {
    nodeDragFrom = null
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
  }
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

/* —— 点击交互 —— */
function onNodeClick(n) {
  if (mode.value === 'view') return
  if (n.isVirtual) { openCreate(); return }
  popupNode.value = n
}
function onLinkClick(l) {
  if (mode.value === 'view') return
  if (l.isVirtual) { openCreate(); return }
  popupLink.value = l
  selectedLinkId.value = l.id
}

/* —— 新建链接 —— */
const classOptions = computed(() => allClasses.value
  .filter(c => c.id && c.id !== '__virtual_new_link__')
  .map(c => ({ id: c.id, cn: c.cn || c.display_name, api_name: c.api_name }))
)
function openCreate() {
  if (mode.value === 'view') return
  Object.assign(newLink, defaultNewLink())
  createOpen.value = true
}
function swapSourceTarget() {
  const s = newLink.source_class_id
  newLink.source_class_id = newLink.target_class_id
  newLink.target_class_id = s
}
const canCreate = computed(() =>
  newLink.display_name && newLink.api_name && newLink.source_class_id && newLink.target_class_id
  && newLink.source_class_id !== newLink.target_class_id
)
async function onCreate() {
  // TODO: 实际 POST 到后端;当前先在前端列表里插入 (后端 linkApi.create 待补充)
  const newItem = {
    id: 'cl-tmp-' + Date.now(),
    rid: '',
    api_name: newLink.api_name,
    source_class_id: newLink.source_class_id,
    target_class_id: newLink.target_class_id,
    cardinality: newLink.cardinality,
    display_name: newLink.display_name,
    status: 1
  }
  allLinks.value = [...allLinks.value, newItem]
  BL.success('已新建链接 (前端模拟,后端持久化待联调)')
  createOpen.value = false
  await nextTick()
  relayout()
}

/* —— 删除链接 —— */
async function onDeleteLink(l) {
  const ok = await BL.confirm({ title: '删除链接', content: `确定删除「${l.display_name || l.api_name}」?`, danger: true, okText: '删除' })
  if (!ok) return
  allLinks.value = allLinks.value.filter(x => x.id !== l.id)
  popupLink.value = null
  BL.success('已删除链接')
  await nextTick()
  relayout()
}

/* —— 工具 —— */
function classLabel(id) {
  const c = allClasses.value.find(x => x.id === id)
  return c ? (c.cn || c.api_name) : id
}
function cardLabel(c) {
  return ({ one_to_one: '一对一 (1:1)', one_to_many: '一对多 (1:*)', many_to_one: '多对一 (*:1)', many_to_many: '多对多 (*:*)' })[c] || c
}
function cardSymbol(link, side) {
  // side === 'source': 显示源端基数; side === 'target': 显示目标端
  // cardinality 格式: one_to_many → 源=1, 目标=*
  const [s, t] = (link.cardinality || 'many_to_many').split('_to_')
  const sym = side === 'source' ? s : t
  return sym === 'one' ? '1' : '*'
}
function truncate(s, n) {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '…' : s
}

/* —— 窗口自适应 —— */
function onResize() {
  // 维持原有 viewBox, SVG 自适应 wrapper; 这里不强制重布局
}
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>

<style scoped>
.lg-root {
  flex: 1; min-height: 0;
  display: flex; flex-direction: column;
  background: var(--bl-bg-1);
}

/* 顶部导航 */
.lg-topnav {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 14px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-1);
}
.lg-title-l { display: inline-flex; align-items: baseline; gap: 8px; }
.lg-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.lg-title-en { font-size: 12px; }
.lg-count-tag {
  margin-left: 6px; padding: 1px 8px;
  background: var(--bl-bg-2); border-radius: 9px;
  font-size: 11px; color: var(--bl-text-2);
}

/* 画布区 */
.lg-canvas-wrap {
  flex: 1; position: relative;
  background:
    linear-gradient(rgba(150,150,150,.08) 1px, transparent 1px) 0 0 / 20px 20px,
    linear-gradient(90deg, rgba(150,150,150,.08) 1px, transparent 1px) 0 0 / 20px 20px,
    #fafbfc;
  overflow: hidden;
  user-select: none;
}
.lg-svg {
  width: 100%; height: 100%;
  display: block;
}

/* 浮动工具栏 */
.lg-toolbar {
  position: absolute; top: 12px; left: 12px; z-index: 10;
  display: flex; flex-direction: column; gap: 4px;
  padding: 4px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
}
.lg-tool {
  width: 30px; height: 30px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 0; background: transparent; cursor: pointer;
  color: var(--bl-text-2);
  border-radius: 4px;
  transition: background-color .12s, color .12s;
}
.lg-tool:hover { background: var(--bl-bg-2); color: var(--bl-text-1); }
.lg-tool.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.lg-tool-divider {
  height: 1px; background: var(--bl-divider); margin: 2px 4px;
}

/* 节点 */
.lg-node-g { cursor: pointer; }
.lg-node-g:active { cursor: grabbing; }
.lg-node-bg {
  fill: #fff;
  stroke: var(--bl-border);
  stroke-width: 1;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,.08));
}
.lg-node-g.is-center .lg-node-bg {
  stroke: var(--bl-primary); stroke-width: 2;
}
.lg-node-g.is-virtual .lg-node-bg {
  fill: #fafbfc; stroke-dasharray: 4,3;
}
.lg-node-g:hover .lg-node-bg { stroke: var(--bl-primary); }
.lg-node-cn {
  font-size: 13px; font-weight: 500; fill: var(--bl-text-1);
  pointer-events: none;
}
.lg-node-en {
  font-size: 11px; fill: var(--bl-text-3);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  pointer-events: none;
}

/* 连线 */
.lg-link {
  fill: none;
  stroke: #86909c;
  stroke-width: 1.5;
  cursor: pointer;
  transition: stroke .12s;
}
.lg-link:hover { stroke: var(--bl-primary); }
.lg-link.is-selected { stroke: var(--bl-primary); stroke-width: 2.5; }
.lg-link.is-virtual { stroke: #c9cdd4; }
.lg-link-hit {
  fill: none; stroke: transparent; stroke-width: 10;
  cursor: pointer;
}

/* 基数标签 */
.lg-card-label {
  font-size: 12px; font-weight: 600;
  fill: var(--bl-primary);
  pointer-events: none;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

/* 弹窗 KV */
.lg-kv { display: flex; padding: 5px 0; font-size: 13px; border-bottom: 1px dashed var(--bl-divider); }
.lg-kv:last-child { border-bottom: 0; }
.lg-kv-l { width: 80px; color: var(--bl-text-3); font-size: 12px; flex-shrink: 0; }
.lg-kv-r { color: var(--bl-text-1); }
.lg-popup { width: 380px; }
.lg-create { width: 520px; }
.bl-modal-ft { padding: 10px 14px; border-top: 1px solid var(--bl-divider); display: flex; gap: 8px; align-items: center; justify-content: flex-end; }

/* 新建链接的源/目标行 */
.lg-source-target { display: flex; align-items: center; gap: 6px; flex: 1; }
.lg-source-target .bl-input { flex: 1; min-width: 0; }
.lg-swap { color: var(--bl-text-3); }
.lg-swap:hover { color: var(--bl-primary); background: var(--bl-bg-2); }

.radio-group { display: inline-flex; gap: 14px; flex-wrap: wrap; }
.radio-item { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; font-size: 13px; }
</style>
