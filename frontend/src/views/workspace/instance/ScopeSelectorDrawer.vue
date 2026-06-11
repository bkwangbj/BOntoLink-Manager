<template>
  <!-- 燕尾角抽屉：白色卡片 + 阴影，箭头指向范围选择 -->
  <div class="ssd-pop" @click.stop>
    <span class="ssd-beak"></span>
    <div class="ssd-body">
      <!-- 左：待选 -->
      <div class="ssd-pane">
        <div class="ssd-pane-hd">待选<span class="ssd-hint">（行业、领域、分组）</span></div>
        <div class="ssd-search">
          <span class="ssd-search-ic" v-html="BL.icon('search', 13, 'var(--bl-text-3)')"></span>
          <input v-model="search" placeholder="搜索行业、领域、分组" />
        </div>
        <div class="ssd-tree">
          <div v-for="n in flatVisible" :key="n.code"
               class="ssd-node" :class="leftSel.has(n.code) && 'is-sel'"
               :style="{ paddingLeft: (8 + n.depth*16) + 'px' }"
               @click="onNodeClick(n, $event)">
            <span class="ssd-caret" @click.stop="toggleExpand(n)">
              <span v-if="n.hasChildren" v-html="BL.icon(isExp(n.code)?'chevronDown':'chevronRight', 11)"></span>
            </span>
            <span class="ssd-sq" :style="{ background: n.color || '#86909c' }"></span>
            <span class="bl-grow bl-truncate">{{ n.label }}</span>
            <span class="ssd-cnt">{{ n.count }}</span>
          </div>
          <div v-if="!flatVisible.length" class="ssd-empty">无匹配节点</div>
        </div>
      </div>

      <!-- 中：操作按钮 -->
      <div class="ssd-arrows">
        <button class="ssd-arrow" :disabled="!leftSel.size" title="添加到已选" @click="moveRight"
                v-html="BL.icon('arrowRight', 14)"></button>
        <button class="ssd-arrow" :disabled="!rightSel.size" title="移回待选" @click="moveLeft"
                v-html="BL.icon('arrowLeft', 14)"></button>
      </div>

      <!-- 右：已选 -->
      <div class="ssd-pane">
        <div class="ssd-pane-hd">已选<span class="ssd-hint">（行业、领域、分组）</span>
          <span class="bl-grow"></span>
          <span class="ssd-selcount">已选 {{ selected.length }} 项</span>
        </div>
        <div class="ssd-tags">
          <div v-for="t in selected" :key="t.code"
               class="ssd-tag" :class="rightSel.has(t.code) && 'is-sel'" @click="toggleRight(t.code)">
            <span class="ssd-tag-type">{{ t.type }}</span>
            <span class="ssd-sq" :style="{ background: t.color || '#86909c' }"></span>
            <span class="bl-grow bl-truncate">{{ t.label }}</span>
            <span class="ssd-tag-cnt">{{ t.count }}</span>
            <button class="ssd-tag-x" @click.stop="removeTag(t.code)" v-html="BL.icon('x', 11)"></button>
          </div>
          <div v-if="!selected.length" class="ssd-empty">未选择任何范围（默认全部）</div>
        </div>
        <div class="ssd-foot">提示：按住 Shift 连续多选，按住 Ctrl 逐个选择</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  tree: { type: Array, default: () => [] },          // [{code,label,type,color,count,children}]
  modelValue: { type: Array, default: () => [] }      // 选中的 codes
})
const emit = defineEmits(['update:modelValue'])

/* —— 节点索引 —— */
const flatAll = computed(() => {
  const out = []
  const walk = (ns) => ns.forEach(n => { out.push(n); if (n.children) walk(n.children) })
  walk(props.tree)
  return out
})
const byCode = computed(() => {
  const m = {}; flatAll.value.forEach(n => m[n.code] = n); return m
})
const parentOf = computed(() => {
  const m = {}
  const walk = (ns, p) => ns.forEach(n => { m[n.code] = p; if (n.children) walk(n.children, n.code) })
  walk(props.tree, null)
  return m
})

/* —— 已选(节点对象) —— */
const selected = ref([])
function syncFromModel() {
  selected.value = props.modelValue.map(c => byCode.value[c]).filter(Boolean)
}
watch(() => [props.modelValue, props.tree], syncFromModel, { immediate: true })

/* —— 展开 / 搜索 —— */
const expanded = ref(new Set(props.tree.map(n => n.code)))   // 默认展开行业
const search = ref('')
function isExp(code) { return search.value.trim() ? true : expanded.value.has(code) }
function toggleExpand(n) {
  if (!n.hasChildren) return
  const s = new Set(expanded.value)
  s.has(n.code) ? s.delete(n.code) : s.add(n.code)
  expanded.value = s
}

/* 搜索命中集合 = 匹配节点 ∪ 其祖先 */
const matchSet = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return null
  const set = new Set()
  for (const n of flatAll.value) {
    if ((n.label || '').toLowerCase().includes(q)) {
      set.add(n.code)
      let p = parentOf.value[n.code]
      while (p) { set.add(p); p = parentOf.value[p] }
    }
  }
  return set
})

/* 扁平可见列表(含 depth/hasChildren) */
const flatVisible = computed(() => {
  const out = []
  const ms = matchSet.value
  const walk = (ns, depth) => {
    for (const n of ns) {
      if (ms && !ms.has(n.code)) continue
      const hasChildren = !!(n.children && n.children.length)
      out.push({ code: n.code, label: n.label, color: n.color, count: n.count, depth, hasChildren })
      if (hasChildren && isExp(n.code)) walk(n.children, depth + 1)
    }
  }
  walk(props.tree, 0)
  return out
})

/* —— 左侧多选(单/Shift/Ctrl) —— */
const leftSel = ref(new Set())
let anchor = null
function onNodeClick(n, e) {
  const codes = flatVisible.value.map(x => x.code)
  if (e.shiftKey && anchor != null) {
    const a = codes.indexOf(anchor), b = codes.indexOf(n.code)
    if (a >= 0 && b >= 0) {
      const [lo, hi] = a < b ? [a, b] : [b, a]
      leftSel.value = new Set(codes.slice(lo, hi + 1))
    }
  } else if (e.ctrlKey || e.metaKey) {
    const s = new Set(leftSel.value)
    s.has(n.code) ? s.delete(n.code) : s.add(n.code)
    leftSel.value = s; anchor = n.code
  } else {
    leftSel.value = new Set([n.code]); anchor = n.code
  }
}

/* —— 右侧多选(逐个) —— */
const rightSel = ref(new Set())
function toggleRight(code) {
  const s = new Set(rightSel.value)
  s.has(code) ? s.delete(code) : s.add(code)
  rightSel.value = s
}

/* —— 转移 —— */
function commit() { emit('update:modelValue', selected.value.map(t => t.code)) }
function moveRight() {
  const have = new Set(selected.value.map(t => t.code))
  for (const code of leftSel.value) {
    if (!have.has(code) && byCode.value[code]) selected.value.push(byCode.value[code])
  }
  leftSel.value = new Set()
  commit()
}
function moveLeft() {
  selected.value = selected.value.filter(t => !rightSel.value.has(t.code))
  rightSel.value = new Set()
  commit()
}
function removeTag(code) {
  selected.value = selected.value.filter(t => t.code !== code)
  commit()
}
</script>

<style scoped>
.ssd-pop {
  position: absolute; top: calc(100% + 10px); left: 0; width: 640px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px;
  box-shadow: 0 16px 48px rgba(0,0,0,.18); z-index: 80; padding: 14px;
}
.ssd-beak { position: absolute; top: -7px; left: 28px; width: 12px; height: 12px; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border); border-top: 1px solid var(--bl-border); transform: rotate(45deg); }

.ssd-body { display: flex; gap: 10px; height: 340px; }
.ssd-pane { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.ssd-pane-hd { display: flex; align-items: center; font-size: 13px; font-weight: 600; color: var(--bl-text-1); margin-bottom: 8px; }
.ssd-hint { font-size: 11px; font-weight: 400; color: var(--bl-text-3); margin-left: 2px; }
.ssd-selcount { font-size: 12px; color: var(--bl-primary); font-weight: 500; }

.ssd-search { position: relative; display: flex; align-items: center; margin-bottom: 8px; }
.ssd-search-ic { position: absolute; left: 8px; display: inline-flex; }
.ssd-search input { width: 100%; height: 30px; box-sizing: border-box; padding: 0 10px 0 26px; border: 1px solid var(--bl-border); border-radius: 6px; font-size: 12.5px; outline: none; }
.ssd-search input:focus { border-color: var(--bl-primary); }

.ssd-tree { border: 1px solid var(--bl-border); border-radius: 6px; flex: 1; min-height: 0; overflow: auto; padding: 4px; }
.ssd-node { display: flex; align-items: center; gap: 6px; padding: 5px 6px; border-radius: 5px; cursor: pointer; font-size: 12.5px; color: var(--bl-text-1); user-select: none; }
.ssd-node:hover { background: var(--bl-bg-hover); }
.ssd-node.is-sel { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ssd-caret { width: 14px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; color: var(--bl-text-3); }
.ssd-sq { width: 11px; height: 11px; border-radius: 2px; flex-shrink: 0; }
.ssd-cnt { font-size: 11px; color: var(--bl-text-3); flex-shrink: 0; }
.ssd-node.is-sel .ssd-cnt { color: var(--bl-primary); }
.ssd-empty { font-size: 12px; color: var(--bl-text-3); text-align: center; padding: 20px; }

.ssd-arrows { display: flex; flex-direction: column; justify-content: center; gap: 8px; flex-shrink: 0; }
.ssd-arrow { width: 32px; height: 28px; border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 6px; color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.ssd-arrow:hover:not(:disabled) { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }
.ssd-arrow:disabled { opacity: .4; cursor: not-allowed; }

.ssd-tags { border: 1px solid var(--bl-border); border-radius: 6px; flex: 1; min-height: 0; overflow: auto; padding: 6px; display: flex; flex-direction: column; gap: 5px; }
.ssd-tag { display: flex; align-items: center; gap: 6px; padding: 6px 8px; border: 1px solid var(--bl-border); border-radius: 6px; cursor: pointer; font-size: 12.5px; background: var(--bl-bg-1); }
.ssd-tag:hover { border-color: var(--bl-primary-border); }
.ssd-tag.is-sel { background: var(--bl-primary-soft); border-color: var(--bl-primary); }
.ssd-tag-type { font-size: 10px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 3px; padding: 1px 5px; flex-shrink: 0; }
.ssd-tag-cnt { font-size: 11px; color: var(--bl-text-3); flex-shrink: 0; }
.ssd-tag-x { width: 18px; height: 18px; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 4px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ssd-tag-x:hover { background: var(--bl-danger-soft, #fff1f0); color: #f53f3f; }

.ssd-foot { font-size: 11px; color: var(--bl-text-3); margin-top: 8px; padding: 0 2px; }
</style>
