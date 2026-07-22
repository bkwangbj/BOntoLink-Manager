<template>
  <Teleport to="body">
    <transition name="ep-fade">
      <div v-if="open" :class="['ep-mask', dm.state.free && 'is-free']" @click.self="onCancel">
        <div class="ep-modal" :class="{ 'is-max': dm.state.maximized }" :style="dm.modalStyle.value" data-dm-root>
          <!-- 标题栏 -->
          <div class="ep-hd" :class="{ 'is-draggable': !dm.state.maximized }" @mousedown="dm.startDrag">
            <div class="ep-hd-l">
              <span class="ep-title">枚举选择</span>
              <span class="bl-muted ep-sub" v-if="subtitle">{{ subtitle }}</span>
            </div>
            <div class="ep-hd-r" @mousedown.stop>
              <button class="bl-btn bl-btn-text bl-btn-icon"
                      :title="dm.state.maximized ? '还原' : '最大化'"
                      @click="dm.toggleMax"
                      v-html="BL.icon(dm.state.maximized ? 'minimize' : 'maximize', 13)"></button>
              <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
            </div>
          </div>

          <!-- 主体: 左树 + 右内容 -->
          <div class="ep-body">
            <!-- 左侧导航 -->
            <aside class="ep-tree">
              <div class="ep-tree-hd">业务领域</div>
              <div class="ep-tree-wrap">
                <div :class="['ep-tn', !activeNode && 'is-active']" @click="activeNode = null">
                  <span class="ep-tn-toggle ep-tn-toggle-empty"></span>
                  <span class="ep-tn-ico" style="background: var(--bl-primary)" v-html="BL.icon('grid', 12, '#fff')"></span>
                  <span class="ep-tn-label">全部</span>
                  <span class="ep-tn-count">{{ allRows.length }}</span>
                </div>
                <EpTreeRow v-for="n in treeRoot" :key="n.id"
                           :node="n"
                           :active="activeNode"
                           :counts="catCounts"
                           :expanded="expandedSet"
                           @pick="onPickNode"
                           @toggle="onToggleNode" />
              </div>
            </aside>

            <!-- 右侧内容 -->
            <section class="ep-pane">
              <!-- 顶部工具栏: 面包屑 + 搜索 -->
              <div class="ep-pane-hd">
                <div class="ep-bread">
                  <span class="bl-muted">范围:</span>
                  <span v-for="(seg, i) in activePath" :key="i" class="ep-bread-seg">
                    <span v-if="i > 0" class="ep-bread-sep">/</span>
                    <span class="ep-bread-label">{{ seg }}</span>
                  </span>
                  <span class="bl-muted ep-count">共 {{ filtered.length }} 项</span>
                </div>
                <div class="ep-search">
                  <span class="ep-search-ic" v-html="BL.icon('search', 13)"></span>
                  <input class="bl-input" placeholder="搜索: 名称 / API / RID / 说明" v-model="q" />
                  <button v-if="q" class="ep-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
                </div>
              </div>

              <!-- 卡片网格列表 -->
              <div class="ep-grid-wrap">
                <div v-if="!filtered.length" class="bl-empty" style="padding:60px">未匹配到枚举类型</div>
                <div v-else class="ep-grid">
                  <div v-for="r in filtered" :key="r.id"
                       :class="['ep-card', isPicked(r.id) && 'is-picked', r.status !== 'active' && 'is-off']"
                       @click="toggleRow(r)">
                    <!-- 卡片标题行: 图标 + 名称 + 选择控件 + 查看按钮 -->
                    <div class="ep-card-hd">
                      <span class="ep-card-ic" :style="{ background: enumColor(r) }"
                            v-html="BL.icon(enumIcon(r), 13, '#fff')"></span>
                      <div class="ep-card-title" :title="r.rdfs_label || r.api_name">
                        <span class="ep-card-name">{{ r.rdfs_label || r.api_name }}</span>
                      </div>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon ep-view"
                              title="查看详情"
                              @click.stop="onView(r)"
                              v-html="BL.icon('eye', 12)"></button>
                      <span class="ep-card-pick" @click.stop="toggleRow(r)">
                        <input v-if="multi" type="checkbox" :checked="isPicked(r.id)" @click.stop @change="toggleRow(r)" />
                        <input v-else type="radio" name="ep-pick" :checked="isPicked(r.id)" @click.stop @change="toggleRow(r)" />
                      </span>
                    </div>

                    <!-- 元数据行 1: API · 类型 · 层级 · 状态 -->
                    <div class="ep-meta">
                      <span class="ep-meta-cell">
                        <span class="ep-meta-lbl">API</span>
                        <span class="ep-meta-val bl-mono" :title="r.api_name">{{ r.api_name || '—' }}</span>
                      </span>
                      <span class="ep-meta-cell">
                        <span class="ep-meta-lbl">类型</span>
                        <span class="bl-tag">{{ enumTypeLabel(r.enum_type) }}</span>
                      </span>
                      <span class="ep-meta-cell">
                        <span class="ep-meta-lbl">层级</span>
                        <span class="ep-meta-val">≤ {{ r.max_level || 1 }}</span>
                      </span>
                      <span class="ep-meta-cell">
                        <span :class="['ep-status', r.status === 'active' ? 'is-on' : 'is-off']">
                          {{ r.status === 'active' ? '启用' : '禁用' }}
                        </span>
                      </span>
                    </div>

                    <!-- 元数据行 2: 所属领域 · RID -->
                    <div class="ep-meta">
                      <span class="ep-meta-cell" :title="catLabelMap[r.category_code] || r.category_code || ''">
                        <span class="ep-meta-lbl">领域</span>
                        <span class="ep-meta-val bl-truncate">{{ catLabelMap[r.category_code] || r.category_code || '—' }}</span>
                      </span>
                      <span class="ep-meta-cell ep-rid">
                        <span class="ep-meta-lbl">RID</span>
                        <span class="ep-meta-val bl-mono bl-truncate" :title="r.rid">{{ r.rid || '—' }}</span>
                      </span>
                    </div>

                    <!-- 说明 -->
                    <div class="ep-desc" :title="r.rdfs_comment || ''">{{ r.rdfs_comment || '—' }}</div>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <!-- 底部操作栏 -->
          <div class="ep-ft">
            <div class="ep-ft-l bl-muted">
              <span v-if="selectedIds.size">已选 <b style="color:var(--bl-primary)">{{ selectedIds.size }}</b> 项</span>
            </div>
            <div class="ep-ft-r">
              <button class="bl-btn" @click="onCancel">取消</button>
              <button class="bl-btn bl-btn-primary" :disabled="required && !selectedIds.size" @click="onConfirm">
                确定<span v-if="multi && selectedIds.size"> ({{ selectedIds.size }})</span>
              </button>
            </div>
          </div>

          <!-- 八向缩放热区 -->
          <DraggableHandles v-if="!dm.state.maximized" :on="dm.startResize" />
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, h } from 'vue'
import { BL } from '@/lib/bl.js'
import { enumTypeApi, categoryApi } from '@/api'
import { useDraggableModal } from '@/lib/useDraggableModal.js'
import DraggableHandles from '@/components/DraggableHandles.vue'

const dm = useDraggableModal({ minWidth: 760, minHeight: 500 })

const props = defineProps({
  open: { type: Boolean, default: false },
  multi: { type: Boolean, default: false },                  // 默认单选
  required: { type: Boolean, default: false },
  modelValue: { type: [Array, String], default: () => [] },  // 已选 id (单选时为单个 id)
  excludeIds: { type: Array, default: () => [] },            // 隐藏的 id
  subtitle: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'update:modelValue', 'confirm', 'cancel', 'view'])

/* —— 数据 —— */
const allRows = ref([])
const tree = ref([])
const catLabelMap = ref({})

const activeNode = ref(null)
const expandedSet = ref(new Set())
const q = ref('')
const selectedIds = ref(new Set())

async function load() {
  allRows.value = await enumTypeApi.list().catch(() => [])
  if (!tree.value.length) {
    tree.value = await categoryApi.tree().catch(() => [])
    const m = {}
    const walk = (ns) => ns.forEach(n => {
      if (n.categoryCode) m[n.categoryCode] = n.label
      if (n.children) walk(n.children)
    })
    walk(tree.value)
    catLabelMap.value = m
  }
  expandedSet.value = new Set(tree.value.map(n => n.id))
}

/* —— 树交互 —— */
function onToggleNode(id) {
  const s = new Set(expandedSet.value)
  s.has(id) ? s.delete(id) : s.add(id)
  expandedSet.value = s
}
function onPickNode(node) { activeNode.value = node }

const activePath = computed(() => {
  if (!activeNode.value) return ['全部']
  const path = []
  const walk = (ns, ancestors) => {
    for (const n of ns) {
      const chain = [...ancestors, n]
      if (n.id === activeNode.value.id) { path.push(...chain.map(x => x.label)); return true }
      if (n.children && walk(n.children, chain)) return true
    }
    return false
  }
  walk(tree.value, [])
  return path.length ? path : [activeNode.value.label || '当前分类']
})
const treeRoot = computed(() => tree.value)

/* —— 过滤 —— */
function isUnderNode(row, node) {
  if (!node) return true
  const codes = new Set()
  const walk = (n) => {
    if (n.categoryCode) codes.add(n.categoryCode)
    if (n.children) n.children.forEach(walk)
  }
  walk(node)
  if (!codes.size) return false
  return codes.has(row.category_code)
}

const filtered = computed(() => {
  let list = allRows.value
  if (props.excludeIds && props.excludeIds.length) {
    const ex = new Set(props.excludeIds)
    list = list.filter(r => !ex.has(r.id))
  }
  if (activeNode.value) list = list.filter(r => isUnderNode(r, activeNode.value))
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => {
    return [r.rdfs_label, r.api_name, r.rid, r.rdfs_comment]
      .filter(Boolean).some(s => String(s).toLowerCase().includes(k))
  })
  return list
})

/* —— 分类节点行数统计 (该节点子树命中数) —— */
const catCounts = computed(() => {
  const map = {}
  // 收集每个节点的子树 categoryCode 集合
  const collect = (n) => {
    const codes = new Set()
    if (n.categoryCode) codes.add(n.categoryCode)
    if (n.children) n.children.forEach(c => collect(c).forEach(x => codes.add(x)))
    map[n.id] = allRows.value.filter(r => codes.has(r.category_code)).length
    return codes
  }
  tree.value.forEach(collect)
  return map
})

/* —— 选择 —— */
function isPicked(id) { return selectedIds.value.has(id) }
function toggleRow(r) {
  const s = new Set(selectedIds.value)
  if (props.multi) {
    s.has(r.id) ? s.delete(r.id) : s.add(r.id)
  } else {
    if (s.has(r.id)) s.delete(r.id)
    else { s.clear(); s.add(r.id) }
  }
  selectedIds.value = s
}

/* —— 卡片视觉 —— */
function enumTypeLabel(t) {
  return ({ general_single: '一级通用', general_multi: '多级通用', biz_single: '业务一级', biz_multi: '业务多级' })[t] || (t || '—')
}
function enumIcon(e) {
  const t = e.enum_type || 'general_single'
  return t.endsWith('_multi') ? 'layers' : 'list'
}
function enumColor(e) {
  if (e.status !== 'active') return '#86909C'
  const t = e.enum_type || 'general_single'
  if (t.startsWith('biz')) return '#FF7D00'
  return '#165DFF'
}

/* —— 查看详情 —— */
function onView(r) { emit('view', r) }

/* —— 打开生命周期 —— */
watch(() => props.open, async (v) => {
  if (!v) { dm.reset(); return }
  const init = Array.isArray(props.modelValue) ? props.modelValue : (props.modelValue ? [props.modelValue] : [])
  selectedIds.value = new Set(init.filter(Boolean))
  q.value = ''
  activeNode.value = null
  dm.reset()
  await load()
})

/* —— 确定 / 取消 —— */
const selectedRows = computed(() => allRows.value.filter(r => selectedIds.value.has(r.id)))
function onConfirm() {
  const ids = [...selectedIds.value]
  const payload = props.multi ? ids : (ids[0] || '')
  emit('update:modelValue', payload)
  emit('confirm', { ids, rows: selectedRows.value })
  emit('update:open', false)
}
function onCancel() {
  emit('cancel')
  emit('update:open', false)
}

/* —— 递归树行 (h-render,样式落在非 scoped 块) —— */
const EpTreeRow = {
  name: 'EpTreeRow',
  props: ['node', 'active', 'counts', 'expanded'],
  emits: ['pick', 'toggle'],
  setup(p, { emit }) {
    return () => {
      const n = p.node
      const hasKids = !!(n.children && n.children.length)
      const isActive = p.active && p.active.id === n.id
      const isOpen = p.expanded.has(n.id)
      const count = p.counts[n.id] || 0
      return h('div', { class: 'ep-tn-wrap' }, [
        h('div', {
          class: ['ep-tn', isActive && 'is-active'],
          onClick: () => emit('pick', n)
        }, [
          hasKids
            ? h('span', {
                class: 'ep-tn-toggle',
                onClick: (e) => { e.stopPropagation(); emit('toggle', n.id) }
              }, isOpen ? '▾' : '▸')
            : h('span', { class: 'ep-tn-toggle ep-tn-toggle-empty' }),
          h('span', {
            class: 'ep-tn-ico',
            style: { background: n.color || '#86909C' },
            innerHTML: BL.icon(n.icon || 'folder', 11, '#fff')
          }),
          h('span', { class: 'ep-tn-label', title: n.label }, n.label),
          h('span', { class: 'ep-tn-count' }, count)
        ]),
        hasKids && isOpen
          ? h('div', { class: 'ep-tn-kids' },
              n.children.map(c => h(EpTreeRow, {
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
/* ============ 模态壳 ============ */
.ep-mask {
  /* 高于详情抽屉(1010) + resize 把手(1011) */
  position: fixed; inset: 0; z-index: 1200;
  background: rgba(0,0,0,.32);
  display: flex; align-items: center; justify-content: center;
}
.ep-mask.is-free { display: block; }
.ep-modal {
  position: relative;
  width: 1220px; max-width: 95vw;
  height: calc(100vh - 80px); max-height: 820px;
  background: var(--bl-bg-1); border-radius: 12px;
  box-shadow: 0 12px 48px rgba(0,0,0,.18);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.ep-modal.is-max { box-shadow: none; }

/* 标题栏 */
.ep-hd {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--bl-divider);
  user-select: none;
}
.ep-hd.is-draggable { cursor: move; }
.ep-hd.is-draggable:active { cursor: grabbing; }
.ep-hd-l { display: inline-flex; align-items: baseline; gap: 10px; min-width: 0; flex: 1; }
.ep-hd-r { display: inline-flex; align-items: center; gap: 2px; flex-shrink: 0; margin-left: 12px; }
.ep-title { font-size: 15px; font-weight: 600; color: var(--bl-text-1); white-space: nowrap; }
.ep-sub { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ============ 主体 ============ */
.ep-body { flex: 1; display: flex; min-height: 0; }

/* 左侧树 */
.ep-tree {
  width: 230px; flex-shrink: 0;
  border-right: 1px solid var(--bl-divider);
  background: color-mix(in srgb, var(--bl-bg-2) 45%, var(--bl-bg-1));
  display: flex; flex-direction: column;
}
.ep-tree-hd { padding: 10px 14px; font-size: 12px; color: var(--bl-text-3); border-bottom: 1px solid var(--bl-divider); }
.ep-tree-wrap { flex: 1; overflow: auto; padding: 6px 4px; }

/* 右侧内容 */
.ep-pane { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.ep-pane-hd {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; gap: 12px;
  border-bottom: 1px solid var(--bl-divider);
}
.ep-bread { display: inline-flex; align-items: center; gap: 6px; font-size: 12.5px; min-width: 0; flex-wrap: wrap; }
.ep-bread-seg { color: var(--bl-text-2); }
.ep-bread-sep { color: var(--bl-text-4); margin: 0 4px; }
.ep-bread-label { color: var(--bl-text-1); }
.ep-count { margin-left: 6px; font-size: 12px; }
.ep-search { position: relative; width: 280px; flex-shrink: 0; }
.ep-search-ic { position: absolute; left: 9px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.ep-search .bl-input { padding-left: 28px; padding-right: 24px; width: 100%; height: 32px; }
.ep-clear { position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  background: transparent; border: 0; cursor: pointer; color: var(--bl-text-3);
  padding: 2px; border-radius: 50%; line-height: 0; }
.ep-clear:hover { background: var(--bl-bg-2); }

/* ============ 卡片网格 ============ */
.ep-grid-wrap { flex: 1; overflow: auto; padding: 14px; background: var(--bl-bg-2); }
.ep-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 12px;
}
.ep-card {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition: border-color .12s, box-shadow .12s, background-color .12s;
  display: flex; flex-direction: column; gap: 8px;
  min-width: 0;
}
.ep-card:hover { border-color: var(--bl-primary); box-shadow: 0 2px 10px rgba(22,93,255,.10); }
.ep-card.is-picked {
  border-color: var(--bl-primary);
  background: var(--bl-primary-soft, #e6f4ff);
  box-shadow: 0 0 0 2px rgba(22,93,255,.15);
}
.ep-card.is-off { opacity: .68; }

/* 卡片标题行 */
.ep-card-hd { display: flex; align-items: center; gap: 8px; min-width: 0; }
.ep-card-ic { width: 24px; height: 24px; border-radius: 5px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ep-card-title { flex: 1; min-width: 0; }
.ep-card-name {
  font-size: 13.5px; font-weight: 600; color: var(--bl-text-1);
  display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.ep-view { color: var(--bl-text-3); flex-shrink: 0; }
.ep-view:hover { color: var(--bl-primary); background: var(--bl-bg-2); }
.ep-card-pick { display: inline-flex; align-items: center; flex-shrink: 0; cursor: pointer; padding: 0 2px; }
.ep-card-pick input { cursor: pointer; }

/* 元数据行 */
.ep-meta {
  display: flex; gap: 14px; align-items: center; flex-wrap: wrap;
  font-size: 11.5px; color: var(--bl-text-2);
  min-width: 0;
}
.ep-meta-cell { display: inline-flex; align-items: center; gap: 4px; min-width: 0; max-width: 100%; }
.ep-meta-lbl { color: var(--bl-text-3); flex-shrink: 0; }
.ep-meta-val { color: var(--bl-text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ep-rid { flex: 1; min-width: 0; max-width: 60%; }

/* 状态 pill */
.ep-status { display: inline-block; padding: 1px 8px; border-radius: 8px; font-size: 11px; font-weight: 500; }
.ep-status.is-on  { background: color-mix(in srgb, var(--bl-success) 14%, var(--bl-bg-1)); color: #00b42a; }
.ep-status.is-off { background: var(--bl-bg-2); color: var(--bl-text-3); }

/* 说明: 截断 + 悬浮 title */
.ep-desc {
  font-size: 12px; color: var(--bl-text-3);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  border-top: 1px dashed var(--bl-divider);
  padding-top: 6px;
  min-height: 2.5em;
}

/* ============ 底部 ============ */
.ep-ft {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px;
  border-top: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
}
.ep-ft-l { font-size: 12px; }
.ep-ft-r { display: inline-flex; gap: 8px; }

/* fade */
.ep-fade-enter-active, .ep-fade-leave-active { transition: opacity .18s ease; }
.ep-fade-enter-from, .ep-fade-leave-to { opacity: 0; }
.ep-fade-enter-active .ep-modal, .ep-fade-leave-active .ep-modal { transition: transform .18s ease; }
.ep-fade-enter-from .ep-modal { transform: translateY(8px) scale(.99); }
</style>

<!-- 非 scoped: 递归 h() 渲染的子组件不会带 data-v-xxx 属性,样式必须放在外面 -->
<style>
.ep-tn-wrap {}
.ep-tn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: 4px;
  cursor: pointer; font-size: 12.5px;
  color: var(--bl-text-2);
  line-height: 1.4;
}
.ep-tn:hover { background: var(--bl-bg-2); color: var(--bl-text-1); }
.ep-tn.is-active { background: var(--bl-primary-soft, #e6f4ff); color: var(--bl-primary); font-weight: 500; }
.ep-tn-toggle {
  width: 12px; flex-shrink: 0; text-align: center;
  color: var(--bl-text-3); font-size: 10px; cursor: pointer;
}
.ep-tn-toggle-empty { cursor: default; }
.ep-tn-ico { width: 18px; height: 18px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ep-tn-label { flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ep-tn-count { color: var(--bl-text-3); font-size: 11px; padding: 0 4px; flex-shrink: 0; }
.ep-tn-kids { padding-left: 14px; }
</style>
