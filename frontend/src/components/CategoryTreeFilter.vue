<template>
  <aside class="ctf-pane" :style="{ width: width + 'px' }">
    <!-- 顶部: 标题 + 搜索 (按钮) -->
    <div class="ctf-hd">
      <span class="ctf-title">{{ title }}</span>
      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :class="searchOpen && 'is-on'"
              title="搜索分类" @click="toggleSearch">
        <span v-html="BL.icon('search', 12)"></span>
      </button>
    </div>
    <!-- 搜索输入 -->
    <div v-if="searchOpen" class="ctf-search">
      <span class="ctf-search-ic" v-html="BL.icon('search', 11)"></span>
      <input ref="searchInput" class="bl-input ctf-search-input" v-model="q"
             placeholder="过滤分类..." @keydown.esc="closeSearch" />
      <button v-if="q" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="q=''"
              v-html="BL.icon('x', 10)"></button>
    </div>
    <!-- 树体 -->
    <div class="ctf-scroll">
      <!-- 全部根 -->
      <div :class="['ctf-row', selectedCode === null && 'is-active']" @click="onSelect(null, null)">
        <span class="ctf-toggle ctf-toggle-empty"></span>
        <span class="ctf-ic" style="background:#86909C" v-html="BL.icon('grid', 11, '#fff')"></span>
        <span class="ctf-label">{{ totalLabel }}</span>
        <span class="ctf-cnt">{{ rows.length }}</span>
      </div>
      <CtfNode v-for="n in filteredTree" :key="n.id"
               :node="n"
               :selected="selectedCode"
               :expanded="expandedSet"
               :counts="countMap"
               @select="onNodeSelect"
               @toggle="onToggle" />
      <div v-if="!filteredTree.length && q" class="bl-empty" style="padding:30px 12px;font-size:12px">无匹配分类</div>
      <div v-if="!tree.length" class="bl-empty" style="padding:30px 12px;font-size:12px">尚未配置行业分类</div>
    </div>
    <!-- 拖拽手柄 -->
    <div class="ctf-drag" :class="resizing && 'is-resizing'" @mousedown="onDragStart"></div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted, h, nextTick, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { categoryApi } from '@/api'

const props = defineProps({
  /** 上层数据列表 (用于按 category_code 计数 + 过滤). 字段名: category_code */
  rows: { type: Array, default: () => [] },
  /** 顶部标题 */
  title: { type: String, default: '行业分类' },
  /** "全部" 行的文案 */
  totalLabel: { type: String, default: '全部' },
  /** localStorage 键名,用于记忆宽度 */
  storeKey: { type: String, default: '' }
})
const emit = defineEmits(['change'])

const TYPE_DEFAULT_WIDTH = 230
const MIN_W = 180, MAX_W = 360
const lsKey = props.storeKey ? `bl.ctf.${props.storeKey}.width` : ''
const width = ref(lsKey ? (parseInt(localStorage.getItem(lsKey) || '0', 10) || TYPE_DEFAULT_WIDTH) : TYPE_DEFAULT_WIDTH)

/* —— 数据: 行业/领域/分组树 —— */
const tree = ref([])
const selectedCode = ref(null)  // null = 全部
const expandedSet = ref(new Set())

async function load() {
  tree.value = await categoryApi.tree().catch(() => [])
  // 默认展开 categoryType=1 (行业级) 节点
  expandedSet.value = new Set(tree.value.map(n => n.id))
}
onMounted(load)

/* —— 子树 categoryCode 集合 (递归收集自身 + 后代) —— */
function collectSubtreeCodes(node) {
  const set = new Set()
  if (node.categoryCode) set.add(node.categoryCode)
  if (node.children && node.children.length) {
    node.children.forEach(c => collectSubtreeCodes(c).forEach(x => set.add(x)))
  }
  return set
}
/* 全部节点 -> 子树 codes 缓存 */
const subtreeCodesMap = computed(() => {
  const map = new Map()
  const walk = (n) => {
    const codes = collectSubtreeCodes(n)
    map.set(n.id, codes)
    if (n.children) n.children.forEach(walk)
  }
  tree.value.forEach(walk)
  return map
})
/* 每个节点的计数: rows 中 category_code 落在该节点子树的数量 (兼容 snake/camel) */
const rowCategoryCode = (r) => r.category_code || r.categoryCode || ''
const countMap = computed(() => {
  const m = {}
  const rowCodes = props.rows.map(rowCategoryCode).filter(Boolean)
  for (const [id, codes] of subtreeCodesMap.value) {
    m[id] = rowCodes.filter(c => codes.has(c)).length
  }
  return m
})

/* —— 搜索 —— */
const searchOpen = ref(false)
const q = ref('')
const searchInput = ref(null)
function toggleSearch() {
  searchOpen.value = !searchOpen.value
  if (searchOpen.value) nextTick(() => searchInput.value?.focus())
  else q.value = ''
}
function closeSearch() { searchOpen.value = false; q.value = '' }

/* 过滤后的树 (按 label 模糊匹配,命中后自动展开链路) */
function nodeMatches(n) {
  const k = q.value.trim().toLowerCase()
  if (!k) return true
  if (String(n.label || '').toLowerCase().includes(k)) return true
  if (n.children && n.children.some(c => nodeMatches(c))) return true
  return false
}
function filterTree(ns) {
  return ns.filter(nodeMatches).map(n => ({
    ...n,
    children: n.children ? filterTree(n.children) : []
  }))
}
const filteredTree = computed(() => filterTree(tree.value))
watch(q, (v) => {
  if (!v) return
  // 搜索时自动展开命中的所有祖先链
  const ids = new Set(expandedSet.value)
  const walk = (ns) => ns.forEach(n => {
    if (nodeMatches(n)) ids.add(n.id)
    if (n.children) walk(n.children)
  })
  walk(tree.value)
  expandedSet.value = ids
})

/* —— 选择 —— */
function onSelect(node, codes) {
  selectedCode.value = node ? node.categoryCode : null
  emit('change', {
    categoryCode: node ? node.categoryCode : null,
    codes: codes || null,    // null = 全部
    node
  })
}
function onNodeSelect(node) {
  const codes = subtreeCodesMap.value.get(node.id) || new Set()
  onSelect(node, codes)
}
function onToggle(id) {
  const s = new Set(expandedSet.value)
  s.has(id) ? s.delete(id) : s.add(id)
  expandedSet.value = s
}

/* —— 拖拽宽度 —— */
const resizing = ref(false)
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX
  dragStartW = width.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const dx = e.clientX - dragStartX
  width.value = Math.max(MIN_W, Math.min(MAX_W, dragStartW + dx))
}
function onDragEnd() {
  resizing.value = false
  if (lsKey) localStorage.setItem(lsKey, String(width.value))
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

/* —— 暴露: 父组件可主动 reload / 取消选择 —— */
defineExpose({
  reload: load,
  clearSelection: () => onSelect(null, null),
  /* 给当前 codes (便于父组件做更复杂的过滤) */
  selectedCodes: () => {
    if (selectedCode.value === null) return null
    // 找到当前选中节点的 id
    let foundId = null
    const walk = (ns) => ns.some(n => {
      if (n.categoryCode === selectedCode.value) { foundId = n.id; return true }
      return n.children && walk(n.children)
    })
    walk(tree.value)
    return foundId ? subtreeCodesMap.value.get(foundId) : null
  }
})

/* —— 递归节点 (h() 渲染,样式落在非 scoped 块) —— */
const CtfNode = {
  name: 'CtfNode',
  props: ['node', 'selected', 'expanded', 'counts'],
  emits: ['select', 'toggle'],
  setup(p, { emit }) {
    return () => {
      const n = p.node
      const hasKids = !!(n.children && n.children.length)
      const isActive = p.selected === n.categoryCode
      const isOpen = p.expanded.has(n.id)
      const count = p.counts[n.id] || 0
      return h('div', { class: 'ctf-nwrap' }, [
        h('div', {
          class: ['ctf-row', isActive && 'is-active'],
          onClick: () => emit('select', n)
        }, [
          hasKids
            ? h('span', {
                class: ['ctf-toggle', isOpen && 'is-open'],
                onClick: (e) => { e.stopPropagation(); emit('toggle', n.id) },
                innerHTML: BL.icon('chevronRight', 10)
              })
            : h('span', { class: 'ctf-toggle ctf-toggle-empty' }),
          h('span', {
            class: 'ctf-ic',
            style: { background: n.color || '#86909C' },
            innerHTML: BL.icon(n.icon || 'folder', 11, '#fff')
          }),
          h('span', { class: 'ctf-label', title: n.label }, n.label),
          h('span', { class: 'ctf-cnt' }, count)
        ]),
        hasKids && isOpen
          ? h('div', { class: 'ctf-children' },
              n.children.map(c => h(CtfNode, {
                key: c.id, node: c, selected: p.selected, expanded: p.expanded, counts: p.counts,
                onSelect: (x) => emit('select', x),
                onToggle: (id) => emit('toggle', id)
              })))
          : null
      ])
    }
  }
}
</script>

<style scoped>
.ctf-pane {
  flex-shrink: 0;
  position: relative;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column;
  overflow: hidden;
  min-width: 180px;
}
.ctf-hd {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
}
.ctf-title { flex: 1; font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.ctf-search {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
}
.ctf-search-ic { color: var(--bl-text-3); display: inline-flex; }
.ctf-search-input { height: 26px; flex: 1; font-size: 12px; }
.ctf-scroll { flex: 1; overflow: auto; padding: 6px; }
.ctf-drag {
  position: absolute; top: 0; bottom: 0; right: -2px; width: 5px;
  cursor: col-resize; transition: background-color .15s;
}
.ctf-drag:hover, .ctf-drag.is-resizing { background: var(--bl-primary); }
</style>

<!-- 非 scoped: CtfNode 由 h() 渲染,需要全局选择器 -->
<style>
.ctf-nwrap { position: relative; }
.ctf-row {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  color: var(--bl-text-1);
}
.ctf-row:hover { background: var(--bl-bg-hover); }
.ctf-row.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.ctf-toggle {
  width: 16px; height: 16px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-3);
  transition: transform .15s;
}
.ctf-toggle.is-open { transform: rotate(90deg); }
.ctf-toggle:hover:not(.ctf-toggle-empty) { color: var(--bl-primary); }
.ctf-toggle-empty { color: transparent; cursor: default; }
.ctf-ic {
  width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.ctf-label { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ctf-cnt {
  flex-shrink: 0;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2);
  border-radius: 9px; padding: 0 7px; min-width: 20px;
  height: 17px; line-height: 17px; text-align: center;
}
.ctf-row.is-active .ctf-cnt { background: var(--bl-bg-1); color: var(--bl-primary); }
.ctf-children { margin-left: 18px; padding-left: 8px; }
</style>
