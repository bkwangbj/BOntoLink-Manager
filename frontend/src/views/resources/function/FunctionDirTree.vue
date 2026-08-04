<template>
  <aside class="fdt-pane" :style="{ width: width + 'px' }">
    <!-- 顶部: 标题 + 搜索 (对齐 CategoryTreeFilter) -->
    <div class="fdt-hd">
      <span class="fdt-title">{{ title }}</span>
      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :class="searchOpen && 'is-on'"
              title="搜索分类" @click="toggleSearch">
        <span v-html="BL.icon('search', 12)"></span>
      </button>
    </div>
    <div v-if="searchOpen" class="fdt-search">
      <span class="fdt-search-ic" v-html="BL.icon('search', 11)"></span>
      <input ref="searchInput" class="bl-input fdt-search-input" v-model="q"
             placeholder="过滤行业 / 领域..." @keydown.esc="closeSearch" />
      <button v-if="q" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="q = ''"
              v-html="BL.icon('x', 10)"></button>
    </div>

    <!-- 树体: 全部函数 → 行业大类 → 业务子域 -->
    <div class="fdt-scroll">
      <div :class="['fdt-row', !selected.industry && 'is-active']" @click="select(null, null)">
        <span class="fdt-toggle fdt-toggle-empty"></span>
        <span class="fdt-ic" style="background:#86909C" v-html="BL.icon('grid', 11, '#fff')"></span>
        <span class="fdt-label">{{ totalLabel }}</span>
        <span class="fdt-cnt">{{ totalCount }}</span>
      </div>

      <div v-for="node in filteredTree" :key="node.industry_dir" class="fdt-nwrap">
        <div :class="['fdt-row', selected.industry === node.industry_dir && !selected.category && 'is-active']"
             @click="select(node.industry_dir, null)">
          <span class="fdt-toggle" :class="expanded.has(node.industry_dir) && 'is-open'"
                @click.stop="toggle(node.industry_dir)" v-html="BL.icon('chevronRight', 10)"></span>
          <span class="fdt-ic" :style="{ background: colorOf(node.industry_dir) }"
                v-html="BL.icon('industry', 11, '#fff')"></span>
          <span class="fdt-label" :title="node.industry_dir">{{ node.industry_dir }}</span>
          <span class="fdt-cnt">{{ node.count }}</span>
        </div>
        <div v-if="expanded.has(node.industry_dir)" class="fdt-children">
          <div v-for="c in node.children" :key="c.category_dir"
               :class="['fdt-row', selected.industry === node.industry_dir && selected.category === c.category_dir && 'is-active']"
               @click="select(node.industry_dir, c.category_dir)">
            <span class="fdt-toggle fdt-toggle-empty"></span>
            <span class="fdt-ic" :style="{ background: colorOf(c.category_dir) }"
                  v-html="BL.icon('folder', 11, '#fff')"></span>
            <span class="fdt-label" :title="c.category_dir">{{ c.category_dir }}</span>
            <span class="fdt-cnt">{{ c.count }}</span>
          </div>
        </div>
      </div>

      <div v-if="!filteredTree.length && q" class="bl-empty" style="padding:30px 12px;font-size:12px">无匹配分类</div>
      <div v-if="!tree.length && !q" class="bl-empty" style="padding:30px 12px;font-size:12px">尚无函数目录</div>
    </div>

    <!-- 拖拽手柄 -->
    <div class="fdt-drag" :class="resizing && 'is-resizing'" @mousedown="onDragStart"></div>
  </aside>
</template>

<script setup>
/**
 * 函数模块专属「行业领域分组」树。
 *
 * 与 CategoryTreeFilter 视觉完全一致, 但数据源不同: 函数的行业/领域目录是向导里可
 * 自由输入、不存在即自动创建的自由文本 (文档 5.2 目录自动创建), 不走 ont_biz_category
 * 字典, 所以按 /api/functions/dirs 的聚合结果渲染两级树。
 */
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { functionApi } from '@/api'

const props = defineProps({
  title: { type: String, default: '行业领域分组' },
  totalLabel: { type: String, default: '全部函数' },
  storeKey: { type: String, default: 'functions' }
})
const emit = defineEmits(['change'])

const DEFAULT_W = 230, MIN_W = 180, MAX_W = 360
const lsKey = `bl.fdt.${props.storeKey}.width`
const width = ref(parseInt(localStorage.getItem(lsKey) || '0', 10) || DEFAULT_W)

const tree = ref([])
const expanded = ref(new Set())
const selected = ref({ industry: null, category: null })

async function load() {
  const data = await functionApi.dirs().catch(() => [])
  tree.value = Array.isArray(data) ? data : []
  // 默认展开全部行业节点
  expanded.value = new Set(tree.value.map(n => n.industry_dir))
}
onMounted(load)

const totalCount = computed(() => tree.value.reduce((s, n) => s + (n.count || 0), 0))

/* 分类图标底色: 按名称哈希取固定色, 保证同名跨页面颜色一致 */
const PALETTE = ['#165DFF', '#00B42A', '#FF7D00', '#722ED1', '#0FC6C2', '#F53F3F', '#B71DE8', '#14C9C9']
function colorOf(name) {
  let h = 0
  for (let i = 0; i < String(name).length; i++) h = (h * 31 + String(name).charCodeAt(i)) >>> 0
  return PALETTE[h % PALETTE.length]
}

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

const filteredTree = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return tree.value
  return tree.value
    .map(n => {
      const hitSelf = n.industry_dir.toLowerCase().includes(k)
      const kids = (n.children || []).filter(c => hitSelf || c.category_dir.toLowerCase().includes(k))
      return hitSelf || kids.length ? { ...n, children: kids } : null
    })
    .filter(Boolean)
})
watch(q, (v) => { if (v) expanded.value = new Set(tree.value.map(n => n.industry_dir)) })

function toggle(industry) {
  const s = new Set(expanded.value)
  s.has(industry) ? s.delete(industry) : s.add(industry)
  expanded.value = s
}
function select(industry, category) {
  selected.value = { industry, category }
  if (industry && !expanded.value.has(industry)) toggle(industry)
  emit('change', { industry, category })
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
  width.value = Math.max(MIN_W, Math.min(MAX_W, dragStartW + (e.clientX - dragStartX)))
}
function onDragEnd() {
  resizing.value = false
  localStorage.setItem(lsKey, String(width.value))
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

defineExpose({ reload: load, clearSelection: () => select(null, null) })
</script>

<style scoped>
.fdt-pane {
  flex-shrink: 0;
  position: relative;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column;
  overflow: hidden;
  min-width: 180px;
}
.fdt-hd {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
}
.fdt-title { flex: 1; font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.fdt-search {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
}
.fdt-search-ic { color: var(--bl-text-3); display: inline-flex; }
.fdt-search-input { height: 26px; flex: 1; font-size: 12px; }
.fdt-scroll { flex: 1; overflow: auto; padding: 6px; }
.fdt-drag {
  position: absolute; top: 0; bottom: 0; right: -2px; width: 5px;
  cursor: col-resize; transition: background-color .15s;
}
.fdt-drag:hover, .fdt-drag.is-resizing { background: var(--bl-primary); }

.fdt-nwrap { position: relative; }
.fdt-row {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  color: var(--bl-text-1);
}
.fdt-row:hover { background: var(--bl-bg-hover); }
.fdt-row.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.fdt-toggle {
  width: 16px; height: 16px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-3);
  transition: transform .15s;
}
.fdt-toggle.is-open { transform: rotate(90deg); }
.fdt-toggle:hover:not(.fdt-toggle-empty) { color: var(--bl-primary); }
.fdt-toggle-empty { color: transparent; cursor: default; }
.fdt-ic {
  width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.fdt-label { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
/* 数量标签统一靠右, 不受缩进层级影响 (文档 5.2 核心对齐规则) */
.fdt-cnt {
  flex-shrink: 0;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2);
  border-radius: 9px; padding: 0 7px; min-width: 20px;
  height: 17px; line-height: 17px; text-align: center;
  font-variant-numeric: tabular-nums;
}
.fdt-row.is-active .fdt-cnt { background: var(--bl-bg-1); color: var(--bl-primary); }
.fdt-children { margin-left: 18px; padding-left: 8px; }
</style>
