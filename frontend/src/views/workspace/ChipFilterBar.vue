<template>
  <div class="cfb" :class="rows===2 && 'is-row2'" ref="root">
    <!-- 全部X + 搜索 -->
    <button :class="['cfb-all', !selected.length && 'is-on']" @click="$emit('select-all')">{{ allLabel }}</button>
    <button class="cfb-search-btn" :class="searchOpen && 'is-on'" :title="'搜索'+pureLabel" @click="toggleSearch"
            v-html="BL.icon('search', 14)"></button>
    <transition name="cfb-fade">
      <input v-if="searchOpen" ref="searchInput" v-model="kw" class="cfb-search bl-input bl-input-sm"
             :placeholder="'搜索'+pureLabel+'…'" @keydown.esc="closeSearch" />
    </transition>

    <!-- 单行:左箭头 -->
    <button v-if="rows===1 && canLeft" class="cfb-arrow" title="向左" @click="scrollStep(-1)" v-html="BL.icon('chevronLeft', 14)"></button>

    <!-- chips 容器 -->
    <div class="cfb-chips" :class="rows===1 ? 'is-row1' : (expanded ? 'is-expanded' : 'is-row2')"
         ref="scroller" @scroll="rows===1 && updateArrows()">
      <button v-for="it in filtered" :key="it.code"
              :class="['cfb-chip', selected.includes(it.code) && 'is-on']"
              @click="$emit('toggle', it.code)">{{ it.name }}</button>
      <span v-if="!filtered.length" class="cfb-empty">无匹配{{ pureLabel }}</span>
    </div>

    <!-- 单行:右箭头 -->
    <button v-if="rows===1 && canRight" class="cfb-arrow" title="向右" @click="scrollStep(1)" v-html="BL.icon('chevronRight', 14)"></button>
    <!-- 两行:展开/收起 -->
    <button v-if="rows===2 && needToggle" class="cfb-toggle" :title="expanded ? '收起' : '展开全部'" @click="expanded=!expanded"
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
  rows: { type: Number, default: 1 }             // 1=单行横滚 / 2=两行可展开
})
defineEmits(['toggle', 'select-all'])

const pureLabel = computed(() => props.allLabel.replace(/^全部/, ''))

/* —— 搜索 —— */
const searchOpen = ref(false)
const kw = ref('')
const searchInput = ref(null)
function toggleSearch () { searchOpen.value = !searchOpen.value; if (searchOpen.value) nextTick(() => searchInput.value?.focus()); else kw.value = '' }
function closeSearch () { searchOpen.value = false; kw.value = '' }
const filtered = computed(() => {
  const q = kw.value.trim().toLowerCase()
  if (!q) return props.items
  return props.items.filter(it => (it.name || '').toLowerCase().includes(q))
})

/* —— 单行横滚箭头 —— */
const root = ref(null)
const scroller = ref(null)
const canLeft = ref(false)
const canRight = ref(false)
function updateArrows () {
  const el = scroller.value
  if (!el || props.rows !== 1) { canLeft.value = canRight.value = false; return }
  canLeft.value = el.scrollLeft > 2
  canRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 2
}
function scrollStep (dir) {
  const el = scroller.value
  if (el) el.scrollBy({ left: dir * Math.max(160, el.clientWidth * 0.7), behavior: 'smooth' })
}

/* —— 两行展开/收起 —— */
const expanded = ref(false)
const needToggle = ref(false)
function updateToggle () {
  const el = scroller.value
  if (!el || props.rows !== 2) { needToggle.value = false; return }
  // 收起态下内容是否超过两行高度
  needToggle.value = el.scrollHeight > el.clientHeight + 2 || expanded.value
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
.cfb.is-row2 { align-items: flex-start; }
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
.cfb-search { flex-shrink: 0; width: 150px; height: 28px; }

/* chips 容器 */
.cfb-chips { flex: 1; min-width: 0; display: flex; gap: 8px; }
.cfb-chips.is-row1 { flex-wrap: nowrap; overflow-x: auto; scrollbar-width: none; }
.cfb-chips.is-row1::-webkit-scrollbar { display: none; }
.cfb-chips.is-row2 { flex-wrap: wrap; max-height: 64px; overflow: hidden; align-content: flex-start; }
.cfb-chips.is-expanded { flex-wrap: wrap; max-height: none; overflow: visible; align-content: flex-start; }
.cfb-chip {
  flex-shrink: 0; height: 28px; padding: 0 14px; border: 1px solid var(--bl-border); border-radius: 14px;
  background: var(--bl-bg-1); color: var(--bl-text-2); font-size: 13px; cursor: pointer; white-space: nowrap;
}
.cfb-chip:hover { border-color: var(--bl-primary-border); color: var(--bl-text-1); }
.cfb-chip.is-on { background: var(--bl-primary-soft); border-color: var(--bl-primary); color: var(--bl-primary); font-weight: 500; }
.cfb-empty { font-size: 12.5px; color: var(--bl-text-3); align-self: center; }

.cfb-arrow, .cfb-toggle {
  flex-shrink: 0; width: 26px; height: 26px; border: 1px solid var(--bl-border); border-radius: 6px;
  background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.cfb-arrow:hover, .cfb-toggle:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.cfb-toggle { align-self: flex-start; }

.cfb-fade-enter-active, .cfb-fade-leave-active { transition: opacity .15s, width .15s; }
.cfb-fade-enter-from, .cfb-fade-leave-to { opacity: 0; width: 0 !important; }
</style>
