<template>
  <transition name="modal-fade">
    <div v-if="app.domainPickerOpen" class="dom-mask" :style="{ left: sidebarLeftOffset }" @click.self="cancel">
      <div class="dom-modal">
        <!-- Header -->
        <header class="dom-hd">
          <span class="hd-title">
            <span class="hd-ic" v-html="BL.icon('cube', 16)"></span>
            常用领域选择
          </span>
          <button class="bl-btn bl-btn-text bl-btn-icon" @click="cancel" v-html="BL.icon('x', 14)"></button>
        </header>

        <!-- 上部：已选区域 -->
        <section class="dom-top">
          <div class="dom-top-tags" :class="!chosen.length && 'is-empty'">
            <template v-if="chosen.length">
              <div v-for="leaf in chosenSorted" :key="leaf.id" class="chip" :style="chipStyle(leaf)">
                <span class="chip-ic" :style="{ background: profile(leaf).color }" v-html="BL.icon(profile(leaf).icon, 12, '#fff')"></span>
                <div class="chip-body">
                  <div class="chip-title">{{ leaf.industryLabel }} <span class="bl-muted">|</span> {{ leaf.label }}</div>
                  <div class="chip-desc bl-truncate" :title="leaf.description || ''">{{ leaf.description || '—' }}</div>
                </div>
                <button class="chip-del" title="移除" @click="toggle(leaf)" v-html="BL.icon('x', 12)"></button>
              </div>
            </template>
            <div v-else class="dom-top-empty">请在下方选择参与的工作领域</div>
          </div>
          <div class="dom-top-act">
            <div class="dom-top-act-btns">
              <button class="bl-btn" @click="clearAll" :disabled="!chosen.length">清空</button>
              <button class="bl-btn bl-btn-primary" @click="confirm">确认</button>
            </div>
            <span class="dom-top-act-count">已选 <b style="color:var(--bl-primary)">{{ chosen.length }}</b> 个领域</span>
          </div>
        </section>

        <!-- 下部：选择操作 -->
        <section class="dom-body">
          <!-- 左侧树 -->
          <div class="dom-tree">
            <div class="tree-hd">行业 / 领域</div>
            <div class="tree-scroll">
              <PickerNode
                v-for="node in tree" :key="node.id"
                :node="node" :selected-id="selectedNodeId"
                :expanded="treeExpanded"
                @select="onTreeSelect" @toggle="onTreeToggle" />
            </div>
          </div>

          <!-- 右侧待选区 -->
          <div class="dom-list">
            <div class="list-hd">
              <div class="search-wrap">
                <span class="search-icon" v-html="BL.icon('search', 14)"></span>
                <input class="bl-input search-input" v-model="q" placeholder="搜索：支持中文、全拼、简拼" />
                <button v-if="q" class="search-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
              </div>
              <span class="bl-muted" style="font-size:12px">{{ filteredLeaves.length }} 个可选</span>
            </div>
            <div class="list-scroll">
              <div v-for="leaf in filteredLeaves" :key="leaf.id"
                   :class="['leaf-card', isChosen(leaf) && 'is-chosen']"
                   @click="toggle(leaf)">
                <div class="leaf-ic" :style="{ background: profile(leaf).color }" v-html="BL.icon(profile(leaf).icon, 14, '#fff')"></div>
                <div class="leaf-body">
                  <div class="leaf-title-row">
                    <span class="leaf-title">{{ leaf.label }}</span>
                    <span class="leaf-industry" :style="{ color: leaf.industryColor }">· {{ leaf.industryLabel }}</span>
                  </div>
                  <div class="leaf-desc bl-truncate" :title="leaf.description || ''">{{ leaf.description || '—' }}</div>
                  <div class="leaf-meta">
                    <span><b>{{ leaf.objectCount || 0 }}</b> 个对象</span>
                    <span><b>{{ leaf.linkCount || 0 }}</b> 个关系</span>
                    <span><b>{{ leaf.propertyCount || 0 }}</b> 个属性</span>
                  </div>
                </div>
                <div class="leaf-check">
                  <span v-if="isChosen(leaf)" class="check-on" v-html="BL.icon('check', 12, '#fff')"></span>
                </div>
              </div>
              <div v-if="!filteredLeaves.length" class="bl-empty" style="padding:48px">暂无匹配数据</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { useAppStore } from '@/stores/app.js'
import { categoryApi } from '@/api'
import { nodeProfile } from '@/lib/domain.js'
import PickerNode from './DomainPickerNode.vue'

const app = useAppStore()
const sidebarLeftOffset = computed(() => app.sidebarCollapsed ? 'var(--bl-sidebar-w-collapsed)' : 'var(--bl-sidebar-w)')
const tree = ref([])
const leaves = ref([])
const treeExpanded = ref(new Set())
const selectedNodeId = ref(null)
const q = ref('')

// 本地草稿(确认时才写回 store)
const draft = reactive(new Map())

watch(() => app.domainPickerOpen, async (open) => {
  if (!open) return
  await load()
  draft.clear()
  for (const d of app.selectedDomains) draft.set(d.id, d)
})

async function load() {
  const data = await categoryApi.picker().catch(() => ({ tree: [], leaves: [] }))
  tree.value = data.tree || []
  leaves.value = data.leaves || []
  // 默认全部展开
  const ids = new Set()
  const walk = (ns) => ns.forEach(n => { ids.add(n.id); if (n.children) walk(n.children) })
  walk(tree.value)
  treeExpanded.value = ids
}

const chosen = computed(() => Array.from(draft.values()))
const chosenSorted = computed(() => [...chosen.value].sort((a, b) => {
  return (a.industryLabel || '').localeCompare(b.industryLabel || '') || (a.label || '').localeCompare(b.label || '')
}))

function isChosen(leaf) { return draft.has(leaf.id) }
function toggle(leaf) {
  if (draft.has(leaf.id)) draft.delete(leaf.id)
  else draft.set(leaf.id, { ...leaf })
}
function clearAll() { draft.clear() }

function onTreeSelect(node) {
  selectedNodeId.value = (selectedNodeId.value === node.id) ? null : node.id
}
function onTreeToggle(node) {
  const s = new Set(treeExpanded.value)
  s.has(node.id) ? s.delete(node.id) : s.add(node.id)
  treeExpanded.value = s
}

// 待选区:左侧树过滤 + 搜索过滤
const filteredLeaves = computed(() => {
  let list = leaves.value
  if (selectedNodeId.value) {
    // 找选中节点所在子树下的叶子(基于 industry/parent label/code 做 OR 匹配)
    const sel = findNodeById(tree.value, selectedNodeId.value)
    if (sel) {
      const allowed = collectLeafFilter(sel)
      list = list.filter(l => allowed.industry === l.industryId || allowed.nsPrefixes.some(p => (l.nsCode || '').startsWith(p)))
    }
  }
  const k = q.value.trim().toLowerCase()
  if (k) {
    list = list.filter(l => {
      const hay = [l.label, l.industryLabel, l.description, l.categoryCode, l.nsCode, l.rid]
        .filter(Boolean).map(String).map(s => s.toLowerCase()).join(' ')
      if (hay.includes(k)) return true
      // 简拼：用 category_code/ns_code 取首字母也可匹配
      const initials = (l.categoryCode || '').split('_').map(s => s[0]).filter(Boolean).join('')
      return initials.toLowerCase().includes(k)
    })
  }
  return list
})

function findNodeById(nodes, id) {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children?.length) { const r = findNodeById(n.children, id); if (r) return r }
  }
  return null
}
function collectLeafFilter(node) {
  // 简单基于 nsCode 前缀
  const nsPrefixes = []
  const walk = (n) => {
    if (n.nsCode) nsPrefixes.push(n.nsCode)
    if (n.children) n.children.forEach(walk)
  }
  walk(node)
  return { industry: node.categoryType === 1 ? node.id : null, nsPrefixes }
}

function profile(leaf) {
  return nodeProfile({ icon: leaf.icon, color: leaf.color, nsCode: leaf.nsCode })
}

function chipStyle(leaf) {
  const p = profile(leaf)
  return { borderLeft: `3px solid ${p.color}` }
}

function confirm() {
  app.setSelectedDomains(chosenSorted.value)
  app.closeDomainPicker()
  BL.success(`已应用 ${chosenSorted.value.length} 个工作领域`)
}
function cancel() { app.closeDomainPicker() }
</script>

<style scoped>
.dom-mask {
  position: fixed;
  top: var(--bl-topbar-h);
  right: 0;
  bottom: 40px;                /* 与视口底部保持 40px 距离 */
  /* left 由组件根据 sidebar 折叠状态动态注入 */
  background: transparent;
  pointer-events: none;
  z-index: 1300;
  display: flex; align-items: stretch; justify-content: flex-start;
  padding: 8px 0 0 6px;
  transition: left .18s ease;
  overflow: hidden;
}
.dom-modal {
  pointer-events: auto;
  width: 980px; max-width: calc(100% - 24px);
  height: 100%;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border-strong);
  border-radius: var(--bl-radius-3);
  box-shadow: var(--bl-shadow-3);
  display: flex; flex-direction: column;
  overflow: hidden;
  position: relative;
}
/* 左侧小三角，指向 sidebar 上的触发按钮（挂在 mask 上，避开 modal 的 overflow:hidden 裁剪） */
.dom-mask::before {
  content: '';
  position: absolute;
  top: 28px;
  left: 0;
  width: 12px; height: 12px;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border-strong);
  border-bottom: 1px solid var(--bl-border-strong);
  transform: rotate(45deg);
  pointer-events: none;
  z-index: 2;
}
.dom-hd {
  height: 52px; padding: 0 16px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--bl-divider);
}
.hd-title { font-weight: 600; display: inline-flex; align-items: center; gap: 8px; }
.hd-ic { color: var(--bl-primary); }

.dom-top {
  padding: 12px 16px;
  display: flex; align-items: flex-start; gap: 12px;
  background: var(--bl-bg-2);
  border-bottom: 1px solid var(--bl-divider);
  max-height: 32%;
  overflow: hidden;
}
.dom-top-tags {
  flex: 1;
  display: flex; flex-wrap: wrap; gap: 8px;
  overflow: auto;
  min-height: 56px;
  max-height: 200px;
}
.dom-top-tags.is-empty {
  align-items: center; justify-content: center;
  color: var(--bl-text-3); font-size: var(--bl-fs-13);
  border: 1px dashed var(--bl-border-strong);
  border-radius: var(--bl-radius-2);
  background: var(--bl-bg-1);
}
.dom-top-empty {
  width: 100%; text-align: center;
}
.dom-top-act {
  display: flex; flex-direction: column; align-items: flex-end; gap: 6px; flex-shrink: 0;
}
.dom-top-act-btns { display: flex; align-items: center; gap: 8px; }
.dom-top-act-count {
  font-size: 12px;
  color: var(--bl-text-3);
  align-self: flex-start;
}
.chip {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px 6px 10px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-2);
  min-width: 220px; max-width: 280px;
  position: relative;
}
.chip-ic {
  width: 24px; height: 24px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.chip-body { flex: 1; min-width: 0; }
.chip-title { font-size: var(--bl-fs-13); font-weight: 500; }
.chip-desc { font-size: var(--bl-fs-11); color: var(--bl-text-3); margin-top: 2px; }
.chip-del {
  width: 20px; height: 20px;
  border: 0; background: transparent;
  border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-3);
  cursor: pointer;
  opacity: 0;
  transition: opacity .15s;
}
.chip:hover .chip-del { opacity: 1; }
.chip-del:hover { background: var(--bl-danger); color: #fff; }

.dom-body {
  flex: 1; display: grid;
  grid-template-columns: 220px 1fr;
  gap: 12px; padding: 12px 16px;
  overflow: hidden;
  background: var(--bl-bg-2);     /* 与上方已选区一致的灰底 */
}
.dom-tree {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.tree-hd {
  padding: 10px 12px; font-size: var(--bl-fs-12); color: var(--bl-text-3);
  border-bottom: 1px solid var(--bl-divider);
}
.tree-scroll { flex: 1; overflow: auto; padding: 6px; }

.dom-list {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.list-hd {
  padding: 10px 12px;
  display: flex; align-items: center; gap: 12px;
  border-bottom: 1px solid var(--bl-divider);
}
.search-wrap { position: relative; flex: 1; max-width: 360px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; padding-right: 24px; }
.search-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  border: 0; background: transparent; cursor: pointer; color: var(--bl-text-3);
  width: 16px; height: 16px;
  display: inline-flex; align-items: center; justify-content: center;
}
.list-scroll {
  flex: 1; overflow: auto; padding: 8px;
  display: grid; gap: 8px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-content: start;
}

.leaf-card {
  display: flex; gap: 10px;
  padding: 10px 12px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  cursor: pointer;
  transition: border-color .15s, background-color .15s;
  position: relative;
  min-height: 80px;
}
.leaf-card:hover { border-color: var(--bl-primary); }
.leaf-card.is-chosen {
  border-color: var(--bl-primary);
  background: var(--bl-primary-soft);
}
.leaf-ic {
  width: 32px; height: 32px; border-radius: var(--bl-radius-3);
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.leaf-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.leaf-title-row { display: flex; align-items: baseline; gap: 8px; }
.leaf-title { font-size: var(--bl-fs-14); font-weight: 600; }
.leaf-industry { font-size: var(--bl-fs-11); font-weight: 500; }
.leaf-desc { font-size: var(--bl-fs-12); color: var(--bl-text-3); }
.leaf-meta {
  display: flex; align-items: center; gap: 10px;
  font-size: var(--bl-fs-12); color: var(--bl-text-2);
  margin-top: 2px;
}
.leaf-meta b { color: var(--bl-text-1); font-weight: 600; }
.leaf-rid { font-size: var(--bl-fs-11); margin-top: 2px; }

.leaf-check {
  position: absolute; top: 10px; right: 10px;
  width: 18px; height: 18px;
  border-radius: 50%;
  border: 1.5px solid var(--bl-border-strong);
  display: flex; align-items: center; justify-content: center;
  background: var(--bl-bg-1);
}
.leaf-card.is-chosen .leaf-check {
  background: var(--bl-primary);
  border-color: var(--bl-primary);
}

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity .15s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
