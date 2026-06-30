<template>
  <div class="cfb" :class="expanded && 'is-expanded'" ref="root">
    <!-- 全部X + 搜索 -->
    <button :class="['cfb-all', !selected.length && 'is-on']" @click="$emit('select-all')">{{ allLabel }}</button>
    <button class="cfb-search-btn" :class="searchOpen && 'is-on'" :title="'搜索'+pureLabel" @click="toggleSearch"
            v-html="BL.icon('search', 14)"></button>
    <transition name="cfb-fade">
      <div v-if="searchOpen" class="cfb-search-box">
        <input ref="searchInput" v-model="kw" class="cfb-search bl-input bl-input-sm"
               placeholder="搜索…" @keydown.esc="closeSearch" />
        <button v-if="kw" class="cfb-clear" title="清空" @mousedown.prevent="clearKw"
                v-html="BL.icon('x', 13)"></button>
      </div>
    </transition>

    <!-- chips 容器:折叠=单行横滚(hover 左右箭头);展开=换行显示全部 -->
    <div class="cfb-scroll" @mouseenter="hovering=true" @mouseleave="hovering=false">
      <div class="cfb-chips" :class="expanded ? 'is-expanded' : 'is-row1'"
           ref="scroller" @scroll="!expanded && updateArrows()">
        <button v-for="it in filtered" :key="it.code"
                :class="['cfb-chip', selected.includes(it.code) && 'is-on']"
                @click="$emit('toggle', it.code)">{{ it.name }}</button>
        <span v-if="!filtered.length" class="cfb-empty">无匹配{{ pureLabel }}</span>
      </div>
      <button v-if="!expanded && canLeft && hovering" class="cfb-edge cfb-edge-l" title="向左"
              @click="scrollStep(-1)" v-html="BL.icon('chevronLeft', 16)"></button>
      <button v-if="!expanded && canRight && hovering" class="cfb-edge cfb-edge-r" title="向右"
              @click="scrollStep(1)" v-html="BL.icon('chevronRight', 16)"></button>
    </div>

    <!-- 收起 ∧ / 展开 ∨(仅在内容溢出时出现) -->
    <button v-if="needToggle" class="cfb-toggle" :title="expanded ? '收起' : '展开全部'" @click="toggleExpand"
            v-html="BL.icon(expanded ? 'chevronUp' : 'chevronDown', 15)"></button>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  allLabel: { type: String, default: '全部' },
  items: { type: Array, default: () => [] },     // [{ code, name }]
  selected: { type: Array, default: () => [] },  // 选中的 codes
  rows: { type: Number, default: 1 }             // 兼容旧用法,当前统一为"单行横滚 + 展开全部"
})
defineEmits(['toggle', 'select-all'])

const pureLabel = computed(() => props.allLabel.replace(/^全部/, ''))

/* —— 搜索 —— */
const searchOpen = ref(false)
const kw = ref('')
const searchInput = ref(null)
function toggleSearch () { searchOpen.value = !searchOpen.value; if (searchOpen.value) nextTick(() => searchInput.value?.focus()); else kw.value = '' }
function closeSearch () { searchOpen.value = false; kw.value = '' }
/* 只清空输入内容,保持查询框打开并重新聚焦 */
function clearKw () { kw.value = ''; nextTick(() => searchInput.value?.focus()) }
const filtered = computed(() => {
  const q = kw.value.trim().toLowerCase()
  if (!q) return props.items
  return props.items.filter(it => (it.name || '').toLowerCase().includes(q))
})

/* —— 展开/收起 —— */
const expanded = ref(false)
function toggleExpand () { expanded.value = !expanded.value; nextTick(recompute) }

/* —— 单行横滚箭头(hover 才显示,悬浮叠加不挤动) —— */
const root = ref(null)
const scroller = ref(null)
const canLeft = ref(false)
const canRight = ref(false)
const hovering = ref(false)
function updateArrows () {
  const el = scroller.value
  if (!el || expanded.value) { canLeft.value = canRight.value = false; return }
  canLeft.value = el.scrollLeft > 2
  canRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 2
}
function scrollStep (dir) {
  const el = scroller.value
  if (el) el.scrollBy({ left: dir * Math.max(160, el.clientWidth * 0.7), behavior: 'smooth' })
}

/* —— 是否需要展开按钮:折叠态下内容横向溢出即可展开 —— */
const needToggle = ref(false)
function updateToggle () {
  const el = scroller.value
  if (!el) { needToggle.value = false; return }
  // 展开态:始终给收起入口;折叠态:内容溢出一行才显示展开
  needToggle.value = expanded.value || el.scrollWidth > el.clientWidth + 2
}

function recompute () { updateArrows(); updateToggle() }

let ro = null
onMounted(() => {
  ro = new ResizeObserver(() => recompute())
  if (root.value) ro.observe(root.value)
  if (scroller.value) ro.observe(scroller.value)
  nextTick(recompute)
})
onBeforeUnmount(() => ro && ro.disconnect())
watch(() => [props.items, filtered.value, expanded.value], () => nextTick(recompute), { deep: false })
</script>

<style scoped>
.cfb { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; }
.cfb.is-expanded { align-items: flex-start; }
.cfb-all {
  flex-shrink: 0; height: 28px; padding: 0 14px; border: 1px solid var(--bl-border); border-radius: 14px;
  background: var(--bl-bg-1); color: var(--bl-text-2); font-size: 13px; cursor: pointer;
}
.cfb-all:hover { border-color: var(--bl-primary-border); }
.cfb-all.is-on { background: var(--bl-primary); border-color: var(--bl-primary); color: #fff; }
.cfb-search-btn {
  flex-shrink: 0; width: 28px; height: 28px; border: 1px solid var(--bl-border); border-radius: 50%;
  background: var(--bl-bg-1); color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.cfb-search-btn:hover, .cfb-search-btn.is-on { border-color: var(--bl-primary); color: var(--bl-primary); }
/* 查询框 ≈ 4 个汉字宽,带内嵌清空 × */
.cfb-search-box { position: relative; flex-shrink: 0; display: inline-flex; }
.cfb-search { width: 92px; height: 28px; padding-right: 26px; }
/* 与顶部全局搜索清空按钮一致:浅灰圆底 + × */
.cfb-clear {
  position: absolute; right: 5px; top: 50%; transform: translateY(-50%);
  width: 18px; height: 18px; border: 0; padding: 0; border-radius: 50%;
  background: var(--bl-fill-2, rgba(0,0,0,.06)); color: var(--bl-text-3);
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.cfb-clear:hover { background: color-mix(in srgb, #f53f3f 14%, var(--bl-bg-1)); color: #f53f3f; }

/* chips 横滚容器(相对定位,承载悬浮箭头) */
.cfb-scroll { position: relative; flex: 1; min-width: 0; }
.cfb-chips { width: 100%; min-width: 0; display: flex; gap: 8px; }
.cfb-chips.is-row1 { flex-wrap: nowrap; overflow-x: auto; scrollbar-width: none; }
.cfb-chips.is-row1::-webkit-scrollbar { display: none; }
.cfb-chips.is-expanded { flex-wrap: wrap; align-content: flex-start; }
.cfb-chip {
  flex-shrink: 0; height: 28px; padding: 0 14px; border: 1px solid var(--bl-border); border-radius: 14px;
  background: var(--bl-bg-1); color: var(--bl-text-2); font-size: 13px; cursor: pointer; white-space: nowrap;
}
.cfb-chip:hover { border-color: var(--bl-primary-border); color: var(--bl-text-1); }
.cfb-chip.is-on { background: var(--bl-primary-soft); border-color: var(--bl-primary); color: var(--bl-primary); font-weight: 500; }
.cfb-empty { font-size: 12.5px; color: var(--bl-text-3); align-self: center; }

/* 悬浮叠加的左右滚动箭头(hover 才出现) */
.cfb-edge {
  position: absolute; top: 50%; transform: translateY(-50%); z-index: 2;
  width: 28px; height: 28px; border: 1px solid var(--bl-border); border-radius: 50%;
  background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  box-shadow: 0 1px 6px rgba(0,0,0,.12);
}
.cfb-edge:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
/* 左箭头左缘对齐第一个页签起始、右箭头右缘对齐最后一个页签结束(= 滚动区左右边界) */
.cfb-edge-l { left: 0; }
.cfb-edge-r { right: 0; }

.cfb-toggle {
  flex-shrink: 0; width: 26px; height: 26px; border: 1px solid var(--bl-border); border-radius: 6px;
  background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
  align-self: flex-start;
}
.cfb-toggle:hover { border-color: var(--bl-primary); color: var(--bl-primary); }

.cfb-fade-enter-active, .cfb-fade-leave-active { transition: opacity .15s, width .15s; }
.cfb-fade-enter-from, .cfb-fade-leave-to { opacity: 0; width: 0 !important; }
</style>
