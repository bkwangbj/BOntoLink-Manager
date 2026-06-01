<template>
  <Teleport to="body">
    <transition name="spp-fade">
      <div v-if="open" :class="['spp-mask', dm.state.free && 'is-free']" @click.self="onCancel">
        <div class="spp-modal" :class="{ 'is-max': dm.state.maximized }" :style="dm.modalStyle.value" data-dm-root>
          <!-- 标题 -->
          <div class="spp-hd" :class="{ 'is-draggable': !dm.state.maximized }" @mousedown="dm.startDrag">
            <div class="spp-hd-l">
              <span class="spp-title">共享属性选择</span>
              <span class="bl-muted spp-sub" v-if="subtitle">{{ subtitle }}</span>
            </div>
            <div class="spp-hd-r" @mousedown.stop>
              <button class="bl-btn bl-btn-text bl-btn-icon" :title="dm.state.maximized ? '还原' : '最大化'"
                      @click="dm.toggleMax"
                      v-html="BL.icon(dm.state.maximized ? 'minimize' : 'maximize', 13)"></button>
              <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
            </div>
          </div>

          <!-- 上层: 已选展示区 + 搜索 -->
          <section class="spp-top">
            <div class="spp-top-hd">
              <span style="font-weight:500">已选 <b style="color:var(--bl-primary)">{{ selectedIds.size }}</b> 项</span>
              <span class="bl-muted" style="margin:0 8px;font-size:12px">·</span>
              <span class="bl-muted" style="font-size:12px">本次新增 <b style="color:#00b42a">{{ newPickedCount }}</b> 个</span>
              <div class="bl-grow"></div>
              <div class="spp-search">
                <span class="spp-search-ic" v-html="BL.icon('search', 13)"></span>
                <input class="bl-input" v-model="q" placeholder="搜索属性 (名称 / 编码 / 拼音 / RID)" />
                <button v-if="q" class="spp-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
              </div>
            </div>
            <div class="spp-chips">
              <span v-for="p in selectedRows" :key="p.id"
                    :class="['spp-chip', excludeSet.has(p.id) && 'is-locked']"
                    :title="`${p.rdfs_label || p.prop_code} · ${p.prop_code}`">
                <span class="spp-chip-ic" :style="{ background: propColor(p) }"
                      v-html="BL.icon(propIcon(p), 10, '#fff')"></span>
                <span class="spp-chip-name bl-truncate">{{ p.rdfs_label || p.prop_code }}</span>
                <button v-if="!excludeSet.has(p.id)" class="spp-chip-x" @click="removeSelection(p.id)" v-html="BL.icon('x', 9)"></button>
                <span v-else class="spp-chip-lock" title="已在结构中,不可移除" v-html="BL.icon('lock', 9)"></span>
              </span>
              <span v-if="!selectedIds.size" class="bl-muted" style="font-size:12px">未选中,从下方列表勾选属性</span>
            </div>
          </section>

          <!-- 下层: 左树 + 右列表 -->
          <div class="spp-body">
            <!-- 左侧行业分类树 -->
            <aside class="spp-tree">
              <div class="spp-tree-hd">行业领域</div>
              <div class="spp-tree-wrap">
                <div :class="['spp-tn', activeCode === null && 'is-active']" @click="activeCode = null; activeNode = null">
                  <span class="spp-tn-toggle spp-tn-toggle-empty"></span>
                  <span class="spp-tn-ic" style="background: #86909C" v-html="BL.icon('grid', 11, '#fff')"></span>
                  <span class="spp-tn-label">全部</span>
                  <span class="spp-tn-cnt">{{ allShared.length }}</span>
                </div>
                <SppTreeRow v-for="n in tree" :key="n.id"
                            :node="n"
                            :active="activeCode"
                            :counts="catCounts"
                            :expanded="expandedSet"
                            @pick="onPickNode"
                            @toggle="onToggleNode" />
              </div>
            </aside>

            <!-- 右侧: 共享属性列表 -->
            <section class="spp-list">
              <div class="spp-list-hd">
                <span class="bl-muted" style="font-size:12px">
                  范围: <b style="color:var(--bl-text-1)">{{ activeLabel }}</b> ·
                  共 <b style="color:var(--bl-text-1)">{{ filtered.length }}</b> 项
                </span>
                <div class="bl-grow"></div>
                <button class="bl-btn bl-btn-sm bl-btn-text" @click="selectAllVisible">
                  <span v-html="BL.icon('check', 11)"></span><span style="margin-left:4px">全选可见 ({{ visibleSelectableCount }})</span>
                </button>
              </div>
              <div class="spp-list-scroll">
                <table class="bl-table spp-table">
                  <thead>
                    <tr>
                      <th class="t-center" style="width:34px">
                        <input type="checkbox" :checked="allVisibleChecked" @change="toggleAllVisible" />
                      </th>
                      <th class="t-left">名称</th>
                      <th class="t-left" style="width:140px">编码</th>
                      <th class="t-left" style="width:120px">所属领域</th>
                      <th class="t-center" style="width:110px">数据类型</th>
                      <th class="t-center" style="width:80px">状态</th>
                      <th class="t-left">说明</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="p in filtered" :key="p.id"
                        :class="['spp-row', selectedIds.has(p.id) && 'is-picked', excludeSet.has(p.id) && 'is-locked']"
                        @click="onRowClick(p)">
                      <td class="t-center" @click.stop>
                        <input v-if="multi" type="checkbox"
                               :checked="selectedIds.has(p.id)"
                               :disabled="excludeSet.has(p.id)"
                               @change="toggleSelect(p)" />
                        <input v-else type="radio" name="spp-pick"
                               :checked="selectedIds.has(p.id)"
                               :disabled="excludeSet.has(p.id)"
                               @change="toggleSelect(p)" />
                      </td>
                      <td class="t-left">
                        <div class="spp-name-cell">
                          <span class="spp-pic" :style="{ background: propColor(p) }"
                                v-html="BL.icon(propIcon(p), 10, '#fff')"></span>
                          <span class="bl-truncate" :title="p.rdfs_label || p.prop_code">{{ p.rdfs_label || p.prop_code }}</span>
                          <span v-if="excludeSet.has(p.id)" class="spp-locked-tag" title="已在结构中">已选</span>
                        </div>
                      </td>
                      <td class="t-left"><span class="bl-mono bl-truncate" :title="p.prop_code">{{ p.prop_code }}</span></td>
                      <td class="t-left"><span class="bl-truncate" :title="domainLabel(p.category_code)">{{ domainLabel(p.category_code) || '通用' }}</span></td>
                      <td class="t-center"><span class="bl-tag">{{ p.data_type || '—' }}</span></td>
                      <td class="t-center">
                        <span :class="['bl-tag', p.status === 1 ? 'bl-tag-success' : 'bl-tag-muted']">{{ p.status === 1 ? '启用' : '禁用' }}</span>
                      </td>
                      <td class="t-left"><span class="bl-muted bl-truncate" :title="p.rdfs_comment">{{ p.rdfs_comment || '—' }}</span></td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="!filtered.length" class="bl-empty" style="padding:48px">未匹配到共享属性</div>
              </div>
            </section>
          </div>

          <!-- 底部 -->
          <div class="spp-ft">
            <div class="spp-ft-l bl-muted">
              <span>已选 <b style="color:var(--bl-primary)">{{ selectedIds.size }}</b> 项</span>
              <span v-if="excludeSet.size" class="bl-muted" style="margin-left:10px;font-size:12px">
                · {{ excludeSet.size }} 个原有已锁定
              </span>
            </div>
            <div class="spp-ft-r">
              <button class="bl-btn" @click="onCancel">取消</button>
              <button class="bl-btn bl-btn-primary" :disabled="!newPickedCount" @click="onConfirm">
                确定<span v-if="newPickedCount"> (新增 {{ newPickedCount }})</span>
              </button>
            </div>
          </div>

          <DraggableHandles v-if="!dm.state.maximized" :on="dm.startResize" />
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, h } from 'vue'
import { BL } from '@/lib/bl.js'
import { sharedPropertyApi, categoryApi } from '@/api'
import { useDraggableModal } from '@/lib/useDraggableModal.js'
import DraggableHandles from '@/components/DraggableHandles.vue'

const dm = useDraggableModal({ minWidth: 880, minHeight: 560 })

const props = defineProps({
  open: { type: Boolean, default: false },
  multi: { type: Boolean, default: true },
  /** 已经在结构中的共享属性 id 列表 — 显示为灰色已选(锁定) */
  excludeIds: { type: Array, default: () => [] },
  /** 最近添加过的属性 id 列表(可用作"已选"标记区分) — 当前与 excludeIds 等同处理 */
  recentlyUsedIds: { type: Array, default: () => [] },
  subtitle: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'confirm', 'cancel'])

const allShared = ref([])
const tree = ref([])
const catLabelMap = ref({})
const activeNode = ref(null)
const activeCode = ref(null)  // category_code
const expandedSet = ref(new Set())
const q = ref('')
const selectedIds = ref(new Set())

const excludeSet = computed(() => new Set(props.excludeIds || []))

/* —— 加载 —— */
async function load() {
  allShared.value = await sharedPropertyApi.list().catch(() => [])
  if (!tree.value.length) {
    tree.value = await categoryApi.tree().catch(() => [])
    const m = {}
    const walk = (ns) => ns.forEach(n => { if (n.categoryCode) m[n.categoryCode] = n.label; if (n.children) walk(n.children) })
    walk(tree.value)
    catLabelMap.value = m
  }
  expandedSet.value = new Set(tree.value.map(n => n.id))
}
function domainLabel(code) { return code ? (catLabelMap.value[code] || code) : '' }

/* —— 树交互 —— */
function onPickNode(node) {
  activeCode.value = node.categoryCode
  activeNode.value = node
}
function onToggleNode(id) {
  const s = new Set(expandedSet.value)
  s.has(id) ? s.delete(id) : s.add(id)
  expandedSet.value = s
}
const activeLabel = computed(() => activeNode.value?.label || '全部')

/* —— 子树 codes —— */
function subtreeCodes(node) {
  const set = new Set()
  if (node.categoryCode) set.add(node.categoryCode)
  if (node.children) node.children.forEach(c => subtreeCodes(c).forEach(x => set.add(x)))
  return set
}
const subtreeCodesMap = computed(() => {
  const m = new Map()
  const walk = (n) => { m.set(n.id, subtreeCodes(n)); if (n.children) n.children.forEach(walk) }
  tree.value.forEach(walk)
  return m
})
const catCounts = computed(() => {
  const m = {}
  const codes = allShared.value.map(p => p.category_code).filter(Boolean)
  for (const [id, set] of subtreeCodesMap.value) m[id] = codes.filter(c => set.has(c)).length
  return m
})

/* —— 过滤 —— */
const filtered = computed(() => {
  let list = allShared.value
  // 行业过滤(子树)
  if (activeNode.value) {
    const set = subtreeCodesMap.value.get(activeNode.value.id)
    if (set) list = list.filter(p => set.has(p.category_code))
  }
  // 关键词过滤
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(p => [p.rdfs_label, p.prop_code, p.rdfs_comment, p.rid].filter(Boolean)
    .some(s => String(s).toLowerCase().includes(k)))
  return list
})

const selectedRows = computed(() => allShared.value.filter(p => selectedIds.value.has(p.id)))
const newPickedCount = computed(() => [...selectedIds.value].filter(id => !excludeSet.value.has(id)).length)
const visibleSelectableCount = computed(() => filtered.value.filter(p => !excludeSet.value.has(p.id)).length)
const allVisibleChecked = computed(() => {
  const candidates = filtered.value.filter(p => !excludeSet.value.has(p.id))
  return candidates.length > 0 && candidates.every(p => selectedIds.value.has(p.id))
})

/* —— 选择 —— */
function toggleSelect(p) {
  if (excludeSet.value.has(p.id)) return
  const s = new Set(selectedIds.value)
  if (props.multi) {
    s.has(p.id) ? s.delete(p.id) : s.add(p.id)
  } else {
    // 单选: 清掉非 locked 的旧选,只保留当前
    s.clear()
    s.add(p.id)
    // 恢复 locked (始终保留)
    excludeSet.value.forEach(id => s.add(id))
  }
  selectedIds.value = s
}
function onRowClick(p) { toggleSelect(p) }
function removeSelection(id) {
  if (excludeSet.value.has(id)) return
  const s = new Set(selectedIds.value)
  s.delete(id); selectedIds.value = s
}
function toggleAllVisible() {
  const candidates = filtered.value.filter(p => !excludeSet.value.has(p.id))
  const s = new Set(selectedIds.value)
  if (allVisibleChecked.value) candidates.forEach(p => s.delete(p.id))
  else candidates.forEach(p => s.add(p.id))
  selectedIds.value = s
}
function selectAllVisible() {
  const s = new Set(selectedIds.value)
  filtered.value.filter(p => !excludeSet.value.has(p.id)).forEach(p => s.add(p.id))
  selectedIds.value = s
}

/* —— 打开生命周期: 把 excludeIds 预置为已选(锁定态),便于 chips 显示 —— */
watch(() => props.open, async (v) => {
  if (!v) { dm.reset(); return }
  dm.reset()
  q.value = ''
  activeNode.value = null
  activeCode.value = null
  // 初始已选: 含锁定项(原有)
  selectedIds.value = new Set(props.excludeIds || [])
  await load()
})

/* —— 确认 / 取消 —— */
function onConfirm() {
  // 只返回新增的 id (排除已锁定的原有)
  const newIds = [...selectedIds.value].filter(id => !excludeSet.value.has(id))
  const newRows = newIds.map(id => allShared.value.find(p => p.id === id)).filter(Boolean)
  emit('confirm', { ids: newIds, rows: newRows })
  emit('update:open', false)
}
function onCancel() {
  emit('cancel')
  emit('update:open', false)
}

/* —— 属性视觉 —— */
function propIcon(p) {
  if (p?.prop_type === 'object') return 'link'
  if (p?.prop_type === 'annotation') return 'chat'
  if (p?.prop_type === 'struct') return 'layers'
  return 'database'
}
function propColor(p) {
  if (p?.prop_type === 'object') return '#FF7D00'
  if (p?.prop_type === 'annotation') return '#00B42A'
  if (p?.prop_type === 'struct') return '#722ED1'
  return '#1677ff'
}

/* —— 递归树节点 —— */
const SppTreeRow = {
  name: 'SppTreeRow',
  props: ['node', 'active', 'counts', 'expanded'],
  emits: ['pick', 'toggle'],
  setup(p, { emit }) {
    return () => {
      const n = p.node
      const hasKids = !!(n.children && n.children.length)
      const isActive = p.active === n.categoryCode
      const isOpen = p.expanded.has(n.id)
      const count = p.counts[n.id] || 0
      return h('div', { class: 'spp-tn-wrap' }, [
        h('div', {
          class: ['spp-tn', isActive && 'is-active'],
          onClick: () => emit('pick', n)
        }, [
          hasKids
            ? h('span', {
                class: ['spp-tn-toggle', isOpen && 'is-open'],
                onClick: (e) => { e.stopPropagation(); emit('toggle', n.id) },
                innerHTML: BL.icon('chevronRight', 10)
              })
            : h('span', { class: 'spp-tn-toggle spp-tn-toggle-empty' }),
          h('span', {
            class: 'spp-tn-ic',
            style: { background: n.color || '#86909C' },
            innerHTML: BL.icon(n.icon || 'folder', 11, '#fff')
          }),
          h('span', { class: 'spp-tn-label', title: n.label }, n.label),
          h('span', { class: 'spp-tn-cnt' }, count)
        ]),
        hasKids && isOpen
          ? h('div', { class: 'spp-tn-kids' },
              n.children.map(c => h(SppTreeRow, {
                key: c.id, node: c, active: p.active, counts: p.counts, expanded: p.expanded,
                onPick: (x) => emit('pick', x),
                onToggle: (id) => emit('toggle', id)
              })))
          : null
      ])
    }
  }
}
</script>

<style scoped>
.spp-mask {
  position: fixed; inset: 0; z-index: 1200;
  background: rgba(0,0,0,.32);
  display: flex; align-items: center; justify-content: center;
}
.spp-mask.is-free { display: block; }
.spp-modal {
  position: relative;
  width: 1280px; max-width: 96vw;
  height: calc(100vh - 60px); max-height: 880px;
  background: #fff; border-radius: 12px;
  box-shadow: 0 12px 48px rgba(0,0,0,.18);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.spp-modal.is-max { box-shadow: none; }

/* 标题栏 */
.spp-hd {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--bl-divider);
  user-select: none;
}
.spp-hd.is-draggable { cursor: move; }
.spp-hd-l { display: inline-flex; align-items: baseline; gap: 10px; min-width: 0; flex: 1; }
.spp-hd-r { display: inline-flex; align-items: center; gap: 2px; flex-shrink: 0; margin-left: 12px; }
.spp-title { font-size: 15px; font-weight: 600; color: var(--bl-text-1); }
.spp-sub { font-size: 12px; }

/* 上层: 已选 + 搜索 */
.spp-top {
  flex-shrink: 0; padding: 10px 16px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
  display: flex; flex-direction: column; gap: 8px;
}
.spp-top-hd { display: flex; align-items: center; font-size: 13px; }
.spp-search { position: relative; width: 320px; }
.spp-search-ic { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.spp-search .bl-input { padding-left: 30px; padding-right: 26px; height: 30px; width: 100%; }
.spp-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  background: transparent; border: 0; cursor: pointer;
  color: var(--bl-text-3); padding: 2px; line-height: 0;
}
.spp-clear:hover { background: var(--bl-bg-1); border-radius: 50%; }

.spp-chips {
  display: flex; flex-wrap: wrap; gap: 6px;
  max-height: 84px; overflow: auto;
  padding: 4px 0;
}
.spp-chip {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; border-radius: 12px;
  background: #fff; border: 1px solid var(--bl-primary);
  color: var(--bl-primary); font-size: 12px;
  max-width: 200px;
}
.spp-chip.is-locked { background: #f2f3f5; border-color: #c9cdd4; color: var(--bl-text-3); }
.spp-chip-ic { width: 14px; height: 14px; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.spp-chip-name { flex: 1; min-width: 0; }
.spp-chip-x, .spp-chip-lock {
  border: 0; background: transparent; cursor: pointer;
  color: inherit; padding: 0; line-height: 0;
  display: inline-flex; align-items: center;
}
.spp-chip-x:hover { color: #f53f3f; }

/* 主体: 左树 + 右列表 */
.spp-body { flex: 1; display: flex; min-height: 0; }

.spp-tree {
  width: 230px; flex-shrink: 0;
  border-right: 1px solid var(--bl-divider);
  background: #fafbfc;
  display: flex; flex-direction: column;
}
.spp-tree-hd { padding: 10px 14px; font-size: 12px; color: var(--bl-text-3); border-bottom: 1px solid var(--bl-divider); }
.spp-tree-wrap { flex: 1; overflow: auto; padding: 6px 4px; }

.spp-list {
  flex: 1; display: flex; flex-direction: column;
  min-width: 0; background: #fff;
}
.spp-list-hd {
  flex-shrink: 0; display: flex; align-items: center;
  padding: 8px 14px; border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
}
.spp-list-scroll { flex: 1; overflow: auto; min-height: 0; }
.spp-table { width: 100%; border-collapse: separate; border-spacing: 0; }
.spp-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  box-shadow: inset 0 -1px 0 var(--bl-divider);
  font-weight: 600; padding: 0 8px;
  font-size: 12px; height: 32px; color: #333;
  white-space: nowrap;
}
.spp-table thead th.t-left { text-align: left; }
.spp-table tbody tr { background: #fff; cursor: pointer; }
.spp-table tbody tr:hover { background: #f5f7fa; }
.spp-table tbody tr.is-picked { background: var(--bl-primary-soft); }
.spp-table tbody tr.is-picked.is-locked { background: #f2f3f5; opacity: .85; cursor: not-allowed; }
.spp-table td { padding: 0 8px; font-size: 12px; height: 34px; vertical-align: middle; }
.spp-table td.t-left { text-align: left; }
.spp-table td.t-center { text-align: center; }

.spp-name-cell { display: inline-flex; align-items: center; gap: 6px; min-width: 0; max-width: 100%; }
.spp-pic { width: 18px; height: 18px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.spp-locked-tag {
  font-size: 10px; color: #86909c; background: #f2f3f5;
  padding: 1px 6px; border-radius: 8px; margin-left: 4px; flex-shrink: 0;
}

/* 底部 */
.spp-ft {
  flex-shrink: 0; padding: 10px 16px;
  border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  background: var(--bl-bg-2);
}
.spp-ft-l { font-size: 13px; }
.spp-ft-r { display: inline-flex; gap: 8px; }

/* tag-muted */
:deep(.bl-tag-muted) { background: #f2f3f5; color: #86909c; }

/* fade */
.spp-fade-enter-active, .spp-fade-leave-active { transition: opacity .18s ease; }
.spp-fade-enter-from, .spp-fade-leave-to { opacity: 0; }
</style>

<!-- 非 scoped: 递归树节点 -->
<style>
.spp-tn-wrap {}
.spp-tn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: 4px;
  cursor: pointer; font-size: 12.5px;
  color: var(--bl-text-1);
  line-height: 1.4;
}
.spp-tn:hover { background: var(--bl-bg-hover); }
.spp-tn.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.spp-tn-toggle {
  width: 14px; flex-shrink: 0; text-align: center;
  color: var(--bl-text-3); font-size: 10px; cursor: pointer;
  transition: transform .15s;
}
.spp-tn-toggle.is-open { transform: rotate(90deg); }
.spp-tn-toggle-empty { cursor: default; color: transparent; }
.spp-tn-ic { width: 18px; height: 18px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.spp-tn-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.spp-tn-cnt {
  flex-shrink: 0; font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border-radius: 9px;
  padding: 0 7px; min-width: 18px; height: 17px;
  line-height: 17px; text-align: center;
}
.spp-tn.is-active .spp-tn-cnt { background: #fff; color: var(--bl-primary); }
.spp-tn-kids { padding-left: 14px; }
</style>
