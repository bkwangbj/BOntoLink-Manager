<template>
  <aside class="lgt-pane" :style="{ width: width + 'px' }">
    <!-- 顶部工具栏: 标题 + 搜索 + 新建 -->
    <div class="lgt-hd">
      <span class="lgt-title">{{ title }}</span>
      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :class="searchOpen && 'is-on'" title="搜索" @click="toggleSearch">
        <span v-html="BL.icon('search', 12)"></span>
      </button>
      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="新建分组" @click="$emit('add-group')">
        <span v-html="BL.icon('plus', 12)"></span>
      </button>
    </div>
    <!-- 搜索框 -->
    <div v-if="searchOpen" class="lgt-search-row">
      <span class="lgt-search-ic" v-html="BL.icon('search', 11)"></span>
      <input ref="searchInput" class="bl-input lgt-search-input" v-model="q" placeholder="过滤分组..." @keydown.esc="closeSearch" />
      <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="closeSearch" v-html="BL.icon('x', 10)"></button>
    </div>
    <!-- 树体 -->
    <div class="lgt-scroll">
      <!-- 全部根节点 -->
      <div :class="['lgt-row', selected === null && 'is-active']" @click="onSelect(null)">
        <span class="lgt-toggle lgt-toggle-empty"></span>
        <span class="lgt-ic" style="background:#86909C" v-html="BL.icon('grid', 11, '#fff')"></span>
        <span class="lgt-label">全部</span>
        <span class="lgt-cnt">{{ totalCount }}</span>
      </div>
      <!-- 树形分组 -->
      <LgtNode v-for="n in filteredTree" :key="n.id"
               :node="n"
               :selected="selected"
               :expanded="expandedSet"
               :counts="countMap"
               @select="onSelect"
               @toggle="onToggle" />
      <div v-if="!filteredTree.length && !q" class="bl-empty" style="padding:30px 12px;font-size:12px">尚未配置分组</div>
      <div v-if="!filteredTree.length && q" class="bl-empty" style="padding:30px 12px;font-size:12px">无匹配分组</div>
    </div>
    <!-- 拖拽手柄 -->
    <div class="lgt-drag" :class="resizing && 'is-resizing'" @mousedown="onDragStart"></div>
  </aside>
</template>

<script setup>
import { ref, reactive, computed, watch, h, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { groupApi, groupRefApi } from '@/api'

const props = defineProps({
  /** 资源类型: object_types / value_types / interface / datasources / enum_types / action_types / link_types / shared_props / functions */
  type: { type: String, required: true },
  /** 顶部标题 */
  title: { type: String, default: '分组' },
  /** 全部记录总数 (上层传入便于显示) */
  totalCount: { type: Number, default: 0 },
  /** localStorage 键名前缀,用于持久化宽度 */
  storeKey: { type: String, default: '' }
})
const emit = defineEmits(['change', 'add-group'])

const TYPE_DEFAULT_WIDTH = 220
const MIN_W = 180, MAX_W = 360
const lsKey = props.storeKey ? `bl.lgt.${props.storeKey}.width` : ''
const width = ref(lsKey ? (parseInt(localStorage.getItem(lsKey) || '0', 10) || TYPE_DEFAULT_WIDTH) : TYPE_DEFAULT_WIDTH)

/* —— 数据 —— */
const groups = ref([])      // 全部分组 (扁平)
const refs   = ref([])      // 当前 type 下的全部成员
const countMap = computed(() => {
  /* 统计每个分组下属于当前 type 的成员数 (含子分组累计) */
  const direct = {}
  refs.value.forEach(r => { direct[r.group_id] = (direct[r.group_id] || 0) + 1 })
  // 累计到父
  const childrenMap = {}
  groups.value.forEach(g => {
    const p = gParent(g)
    if (p) (childrenMap[p] = childrenMap[p] || []).push(g.id)
  })
  const memo = {}
  function calc(id) {
    if (memo[id] != null) return memo[id]
    let s = direct[id] || 0
    for (const c of (childrenMap[id] || [])) s += calc(c)
    memo[id] = s
    return s
  }
  groups.value.forEach(g => calc(g.id))
  return memo
})

/* 字段适配 (Jackson 把 gName → gname, gSort → gsort) */
const gLabel = (g) => g.gname || g.gName || g.g_name || g.group_name || g.label || ''
const gParent = (g) => g.parentId || g.parent_id || ''

/* 树形组装 */
const tree = computed(() => {
  const byParent = {}
  groups.value.forEach(g => {
    const p = gParent(g)
    if (!byParent[p]) byParent[p] = []
    byParent[p].push({
      id: g.id,
      label: gLabel(g),
      icon: g.icon || 'folder',
      color: g.color || '#1677ff',
      children: []
    })
  })
  function attach(nodes) {
    nodes.forEach(n => {
      n.children = byParent[n.id] || []
      attach(n.children)
    })
  }
  const roots = byParent[''] || []
  attach(roots)
  return roots
})

/* 搜索过滤 */
const q = ref('')
const searchOpen = ref(false)
const searchInput = ref(null)
function toggleSearch() {
  searchOpen.value = !searchOpen.value
  if (searchOpen.value) nextTick(() => searchInput.value?.focus())
  else q.value = ''
}
function closeSearch() { searchOpen.value = false; q.value = '' }

function pruneTree(nodes, kw) {
  if (!kw) return nodes
  const k = kw.toLowerCase()
  const walk = (ns) => ns.map(n => {
    const kids = walk(n.children)
    if (n.label.toLowerCase().includes(k) || kids.length) {
      return { ...n, children: kids, _matched: true }
    }
    return null
  }).filter(Boolean)
  return walk(nodes)
}
const filteredTree = computed(() => pruneTree(tree.value, q.value))

/* 展开 / 选中 */
const expandedSet = ref(new Set())
const selected = ref(null)
function onToggle(id) {
  const s = new Set(expandedSet.value)
  s.has(id) ? s.delete(id) : s.add(id)
  expandedSet.value = s
}
function onSelect(id) {
  selected.value = id
  emit('change', id)
}
watch(filteredTree, (v) => {
  // 搜索时自动展开所有匹配
  if (q.value) expandedSet.value = new Set(flatten(v).map(n => n.id))
})
function flatten(ns) { const out = []; const walk = (l) => l.forEach(n => { out.push(n); walk(n.children) }); walk(ns); return out }

/* 数据加载 */
async function load() {
  const [allGroups, typeRefs] = await Promise.all([
    groupApi.listAll ? groupApi.listAll().catch(() => []) : groupApi.listByParent('').catch(() => []),
    groupRefApi.list(props.type).catch(() => [])
  ])
  groups.value = allGroups || []
  refs.value = typeRefs || []
  // 默认展开顶级
  expandedSet.value = new Set(tree.value.map(n => n.id))
}
onMounted(load)
watch(() => props.type, load)

/* 父组件查询: 给定资源 id, 判断是否属于当前选中分组 */
function refIdsInSelectedGroup() {
  if (selected.value === null) return null  // 全部
  // 递归收集子分组
  const target = new Set([selected.value])
  const allKids = new Map()
  groups.value.forEach(g => {
    const p = g.parentId || g.parent_id
    if (p) {
      if (!allKids.has(p)) allKids.set(p, [])
      allKids.get(p).push(g.id)
    }
  })
  const stack = [selected.value]
  while (stack.length) {
    const id = stack.pop()
    const kids = allKids.get(id) || []
    for (const k of kids) { if (!target.has(k)) { target.add(k); stack.push(k) } }
  }
  return new Set(refs.value.filter(r => target.has(r.group_id)).map(r => r.ref_id))
}
defineExpose({ reload: load, refIdsInSelectedGroup })

/* —— 拖拽宽度 —— */
const resizing = ref(false)
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX; dragStartW = width.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const dx = e.clientX - dragStartX
  width.value = Math.min(MAX_W, Math.max(MIN_W, dragStartW + dx))
}
function onDragEnd() {
  resizing.value = false
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  if (lsKey) localStorage.setItem(lsKey, String(width.value))
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
})

/* —— 递归节点 (h() 渲染) —— */
const LgtNode = {
  name: 'LgtNode',
  props: ['node', 'selected', 'expanded', 'counts'],
  emits: ['select', 'toggle'],
  setup(p, { emit: emitChild }) {
    return () => {
      const n = p.node
      const isOpen = p.expanded.has(n.id)
      const hasKids = (n.children || []).length > 0
      const isActive = p.selected === n.id
      return h('div', { class: 'lgt-nwrap' }, [
        h('div', {
          class: ['lgt-row', isActive && 'is-active'],
          onClick: () => emitChild('select', n.id)
        }, [
          hasKids
            ? h('span', {
                class: ['lgt-toggle', isOpen && 'is-open'],
                onClick: (ev) => { ev.stopPropagation(); emitChild('toggle', n.id) },
                innerHTML: BL.icon('chevronRight', 11)
              })
            : h('span', { class: 'lgt-toggle lgt-toggle-empty' }),
          h('span', { class: 'lgt-ic', style: { background: n.color }, innerHTML: BL.icon(n.icon || 'folder', 11, '#fff') }),
          h('span', { class: 'lgt-label' }, n.label),
          h('span', { class: 'lgt-cnt' }, p.counts[n.id] || 0)
        ]),
        hasKids && isOpen
          ? h('div', { class: 'lgt-children' }, (n.children).map(c => h(LgtNode, { node: c, selected: p.selected, expanded: p.expanded, counts: p.counts, key: c.id, onSelect: (id) => emitChild('select', id), onToggle: (id) => emitChild('toggle', id) })))
          : null
      ])
    }
  }
}
</script>

<style scoped>
.lgt-pane {
  flex-shrink: 0;
  position: relative;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column;
  overflow: hidden;
  min-width: 180px;
}
.lgt-hd {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
}
.lgt-title { flex: 1; font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.lgt-search-row {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
}
.lgt-search-ic { color: var(--bl-text-3); display: inline-flex; }
.lgt-search-input { height: 26px; flex: 1; font-size: 12px; }
.lgt-scroll { flex: 1; overflow: auto; padding: 6px; }
.lgt-drag {
  position: absolute; top: 0; bottom: 0; right: -2px; width: 5px;
  cursor: col-resize; transition: background-color .15s;
}
.lgt-drag:hover, .lgt-drag.is-resizing { background: var(--bl-primary); }
</style>

<!-- 非 scoped: LgtNode 通过 h() 渲染,需要全局选择器 -->
<style>
.lgt-nwrap { position: relative; }
.lgt-row {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  color: var(--bl-text-1);
}
.lgt-row:hover { background: var(--bl-bg-hover); }
.lgt-row.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.lgt-toggle {
  width: 16px; height: 16px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-3);
  transition: transform .15s;
}
.lgt-toggle.is-open { transform: rotate(90deg); }
.lgt-toggle:hover:not(.lgt-toggle-empty) { color: var(--bl-primary); }
.lgt-toggle-empty { color: transparent; cursor: default; }
.lgt-ic {
  width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.lgt-label { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.lgt-cnt {
  flex-shrink: 0;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2);
  border-radius: 9px; padding: 0 7px; min-width: 20px;
  height: 17px; line-height: 17px; text-align: center;
}
.lgt-row.is-active .lgt-cnt { background: #fff; color: var(--bl-primary); }
.lgt-children {
  margin-left: 18px; padding-left: 8px;
  border-left: 1px dashed var(--bl-divider);
}
</style>
