<template>
  <Teleport to="body">
    <transition name="otp-fade">
      <div v-if="open" class="otp-mask" @click.self="onCancel">
        <div class="otp-modal">
          <!-- 标题 -->
          <div class="otp-hd">
            <span class="otp-title">对象类型选择</span>
            <span class="bl-muted otp-sub" v-if="subtitle">{{ subtitle }}</span>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- 上：已选对象类型 -->
          <div class="otp-sel">
            <div v-if="selectedRows.length" class="otp-sel-grid">
              <div v-for="r in selectedRows" :key="r.id" class="otp-tag">
                <span class="otp-tag-ic" :style="{ background: r.color || '#165DFF' }" v-html="BL.icon(r.icon || 'cube', 12, '#fff')"></span>
                <div class="otp-tag-body">
                  <div class="otp-tag-title bl-truncate">
                    {{ r.display_name || r.rdfs_label || r.api_name }}
                    <span class="otp-tag-stat">属性 {{ r.propTotal ?? 0 }}</span>
                    <span :class="['bl-tag', r.status===1?'bl-tag-success':'bl-tag-danger']">{{ r.status===1?'有效':'禁用' }}</span>
                  </div>
                  <div class="otp-tag-cmt bl-muted bl-truncate2">{{ r.rdfs_comment || '—' }}</div>
                </div>
                <button class="otp-tag-x" title="移除" @click="toggle(r)" v-html="BL.icon('x', 10)"></button>
              </div>
            </div>
            <div v-else class="otp-empty-sel">请在下方选择参与的对象类型</div>
            <div class="otp-sel-actions">
              <button class="bl-btn bl-btn-text bl-btn-sm" :disabled="!selectedIds.size" @click="clearAll">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">清空</span>
              </button>
              <button class="bl-btn bl-btn-primary bl-btn-sm" :disabled="!selectedIds.size && required" @click="onConfirm">确定 ({{ selectedIds.size }})</button>
            </div>
          </div>

          <!-- 下：选择操作区（左树 + 右卡片） -->
          <div class="otp-body">
            <aside class="otp-tree">
              <div class="otp-tree-title">行业 / 领域</div>
              <div class="otp-tree-wrap">
                <div :class="['otp-tn', !activeNode && 'is-active']" @click="activeNode = null">
                  <span class="otp-tn-toggle otp-tn-toggle-empty"></span>
                  <span class="otp-tn-ico" style="background: var(--bl-primary)" v-html="BL.icon('grid', 13, '#fff')"></span>
                  <span class="otp-tn-label bl-truncate">全部</span>
                  <span class="otp-tn-count">{{ allClasses.length }}</span>
                </div>
              </div>
              <TreeRow v-for="n in treeRoot" :key="n.id" :node="n" :active="activeNode" :counts="catCounts" @pick="onPickNode" />
            </aside>
            <section class="otp-pane">
              <div class="otp-pane-hd">
                <div class="bl-row" style="gap:6px;align-items:center;font-size:13px;color:var(--bl-text-2)">
                  <span class="bl-muted">范围：</span><b>{{ activeNodeLabel }}</b>
                  <span class="bl-muted">共 {{ filtered.length }} 项</span>
                </div>
                <div class="otp-search">
                  <span class="ot-ic" v-html="BL.icon('search', 13)"></span>
                  <input class="bl-input" placeholder="搜索：支持中文、全拼、简拼" v-model="q" />
                  <button v-if="q" class="otp-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
                </div>
              </div>
              <div class="otp-grid" v-if="filtered.length">
                <div v-for="r in filtered" :key="r.id"
                     :class="['otp-card', selectedIds.has(r.id) && 'is-on']"
                     :style="{ '--otp-side': r.color || '#165DFF' }"
                     @click="toggle(r)">
                  <div class="otp-card-ic" :style="{ background: r.color || '#165DFF' }" v-html="BL.icon(r.icon || 'cube', 14, '#fff')"></div>
                  <div class="otp-card-body">
                    <div class="otp-card-title">
                      <span class="bl-truncate">{{ r.display_name || r.rdfs_label || r.api_name }}</span>
                      <span :class="['bl-tag', r.status===1?'bl-tag-success':'bl-tag-danger']">{{ r.status===1?'有效':'禁用' }}</span>
                    </div>
                    <div class="otp-card-meta bl-truncate">
                      <span class="bl-mono bl-muted">{{ r.api_name }}</span>
                      <span class="bl-muted">·</span>
                      <span class="bl-muted">属性 {{ r.propTotal ?? 0 }}</span>
                    </div>
                    <div v-if="r.rdfs_comment" class="otp-card-cmt bl-muted bl-truncate2">{{ r.rdfs_comment }}</div>
                  </div>
                  <div v-if="selectedIds.has(r.id)" class="otp-card-check" v-html="BL.icon('check', 10, '#fff')"></div>
                </div>
              </div>
              <div v-else class="bl-empty" style="padding:48px">暂无匹配数据</div>
            </section>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, h } from 'vue'
import { BL } from '@/lib/bl.js'
import { nodeProfile } from '@/lib/domain.js'
import { resourceApi, categoryApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  multi: { type: Boolean, default: true },
  required: { type: Boolean, default: false },
  modelValue: { type: Array, default: () => [] },   // 已选 id 列表（input/output）
  excludeIds: { type: Array, default: () => [] },   // 不可选的 id（已被使用、自身等）
  subtitle: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'update:modelValue', 'confirm', 'cancel'])

const allClasses = ref([])      // 全量对象类型
const tree = ref([])            // 分类树
const selectedIds = ref(new Set(props.modelValue || []))
const activeNode = ref(null)    // 当前选中的树节点（category_code 串）
const q = ref('')

const selectedRows = computed(() => allClasses.value.filter(c => selectedIds.value.has(c.id)))

// 仅显示「行业(type=1)」与「领域(type=2)」节点
const treeRoot = computed(() => {
  const trim = (nodes) => (nodes || [])
    .filter(n => n.categoryType === 1 || n.categoryType === 2)
    .map(n => ({ ...n, children: trim(n.children) }))
  return trim(tree.value)
})

// 每个 category_code 下的对象类数量统计
const catCounts = computed(() => {
  const m = {}
  for (const c of allClasses.value) {
    if (!c.category_code) continue
    m[c.category_code] = (m[c.category_code] || 0) + 1
  }
  return m
})

const activeNodeLabel = computed(() => {
  if (!activeNode.value) return '全部'
  // activeNode 是 category_code 或 node 对象
  const n = activeNode.value
  return n.rdfs_label || n.label || n.categoryCode || '全部'
})

const filtered = computed(() => {
  let list = allClasses.value
  if (props.excludeIds && props.excludeIds.length) {
    const ex = new Set(props.excludeIds)
    list = list.filter(c => !ex.has(c.id))
  }
  if (activeNode.value) {
    const codes = collectCodes(activeNode.value)
    list = list.filter(c => codes.has(c.category_code))
  }
  const k = (q.value || '').trim().toLowerCase()
  if (!k) return list
  return list.filter(c =>
    [c.api_name, c.display_name, c.rdfs_label, c.rdfs_comment].filter(Boolean)
      .some(s => String(s).toLowerCase().includes(k)))
})

function collectCodes(node) {
  const out = new Set()
  const walk = (n) => { if (n.categoryCode) out.add(n.categoryCode); (n.children || []).forEach(walk) }
  walk(node)
  return out
}

function onPickNode(node) { activeNode.value = node }

function toggle(row) {
  const s = new Set(selectedIds.value)
  if (s.has(row.id)) s.delete(row.id)
  else {
    if (!props.multi) s.clear()
    s.add(row.id)
  }
  selectedIds.value = s
}
function clearAll() { selectedIds.value = new Set() }
function onConfirm() {
  const ids = [...selectedIds.value]
  const rows = allClasses.value.filter(c => selectedIds.value.has(c.id))
  emit('update:modelValue', ids)
  emit('confirm', { ids, rows })
  emit('update:open', false)
}
function onCancel() { emit('cancel'); emit('update:open', false) }

async function load() {
  try {
    const [classes, t] = await Promise.all([
      resourceApi.classes({ aggregate: true }).catch(() => []),
      categoryApi.tree().catch(() => [])
    ])
    allClasses.value = classes || []
    tree.value = t || []
  } catch { allClasses.value = []; tree.value = [] }
}

watch(() => props.open, async (v) => {
  if (v) {
    selectedIds.value = new Set(props.modelValue || [])
    activeNode.value = null
    q.value = ''
    await load()
  }
}, { immediate: true })

watch(() => props.modelValue, (v) => {
  selectedIds.value = new Set(v || [])
})

// 内嵌树行组件（参考 Category.vue 的 TreeNode 设计：色块图标 + 树形虚线引线 + 圆角徽标）
const TreeRow = {
  props: { node: Object, active: Object, counts: Object },
  emits: ['pick'],
  setup(p, { emit }) {
    const open = ref(true)
    return () => {
      const kids = (p.node.children || []).filter(c => c.categoryType === 1 || c.categoryType === 2)
      const has = kids.length > 0
      const isOn = p.active && p.active.id === p.node.id
      const total = (() => {
        const codes = []
        const walk = (n) => { if (n.categoryCode) codes.push(n.categoryCode); (n.children || []).forEach(walk) }
        walk(p.node)
        return codes.reduce((s, c) => s + (p.counts[c] || 0), 0)
      })()
      const prof = nodeProfile(p.node)
      return h('div', { class: 'otp-tree-wrap' }, [
        h('div', {
          class: ['otp-tn', isOn && 'is-active'],
          onClick: () => emit('pick', p.node)
        }, [
          has
            ? h('span', {
                class: 'otp-tn-toggle',
                onClick: (e) => { e.stopPropagation(); open.value = !open.value }
              }, [h('span', { innerHTML: BL.icon(open.value ? 'chevronDown' : 'chevronRight', 12) })])
            : h('span', { class: 'otp-tn-toggle otp-tn-toggle-empty' }),
          h('span', {
            class: 'otp-tn-ico',
            style: { background: prof.color },
            innerHTML: BL.icon(prof.icon, 13, '#fff')
          }),
          h('span', {
            class: 'otp-tn-label bl-truncate',
            title: p.node.rdfsLabel || p.node.label || p.node.categoryCode
          }, p.node.rdfsLabel || p.node.label || p.node.categoryCode),
          h('span', { class: 'otp-tn-count', title: `${total} 个对象` }, String(total))
        ]),
        open.value && has
          ? h('div', { class: 'otp-tn-children' },
              kids.map(c => h(TreeRow, {
                node: c, active: p.active, counts: p.counts,
                onPick: (n) => emit('pick', n),
                key: c.id
              })))
          : null
      ])
    }
  }
}
</script>

<style scoped>
.otp-mask {
  /* z-index 必须高于:对象类型详情抽屉(1000) + 属性详情抽屉(1010) + 抽屉 resize 把手(1011),
     与 ValueTypePicker / EnumPicker / SharedPropertyPicker / PropertyFormatModal 等大弹框对齐 */
  position: fixed; inset: 0; background: rgba(0,0,0,.40);
  z-index: 1200; display: flex; align-items: center; justify-content: center;
}
.otp-modal {
  background: var(--bl-bg-1); border-radius: 12px;
  width: 1100px; max-width: 95vw;
  /* 固定高度：无论左侧树切到哪个节点、右侧卡片多少，弹框尺寸都保持一致 */
  height: 80vh; min-height: 560px; max-height: 92vh;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 12px 40px rgba(0,0,0,.20);
}
.otp-hd {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border-bottom: 1px solid var(--bl-divider);
}
.otp-title { font-size: var(--bl-fs-15); font-weight: 600; }
.otp-sub { font-size: 12px; flex: 1; }
.otp-hd .bl-btn { margin-left: auto; }

/* 上：已选 — 设最高 ~3 行高度，超出滚动；不挤压下方树/卡片区 */
.otp-sel {
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider);
  display: flex; flex-direction: column; gap: 8px;
  background: var(--bl-bg-2);
  flex-shrink: 0;
  max-height: 30vh;       /* 顶部已选区最高不超过弹框 30% */
  overflow: hidden;
}
.otp-sel-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
  overflow-y: auto;       /* 超出 max-height 时滚动 */
  max-height: calc(30vh - 60px);   /* 留出底部 actions 区高度 */
  padding-right: 4px;     /* 给滚动条留白 */
}
.otp-tag {
  position: relative;
  display: flex; gap: 8px; align-items: stretch;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 6px; padding: 6px 22px 6px 6px;
}
.otp-tag-ic { width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.otp-tag-body { min-width: 0; flex: 1; }
.otp-tag-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; }
.otp-tag-stat { font-size: 11px; color: var(--bl-text-3); margin-left: auto; }
.otp-tag-cmt { font-size: 11px; line-height: 1.4; }
.otp-tag-x {
  position: absolute; top: 4px; right: 4px;
  width: 16px; height: 16px; border: 0; background: var(--bl-bg-3); color: var(--bl-text-2);
  border-radius: 50%; cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.otp-tag-x:hover { background: var(--bl-danger); color: #fff; }
.otp-empty-sel {
  padding: 8px 12px; text-align: center;
  color: var(--bl-text-3); font-size: 12px;
  background: var(--bl-bg-1); border: 1px dashed var(--bl-border);
  border-radius: 4px;
}
.otp-sel-actions { display: flex; justify-content: flex-end; gap: 8px; }

/* 下：树 + 卡片 — 左侧浅灰底，右侧白底；高度由父级 flex:1 决定，左右始终同高 */
.otp-body {
  flex: 1; display: grid; grid-template-columns: 240px 1fr;
  overflow: hidden; min-height: 0;
  background: var(--bl-bg-1);
}
.otp-tree {
  border-right: 1px solid var(--bl-border);
  padding: 8px 6px; overflow: auto;
  background: color-mix(in srgb, var(--bl-bg-2) 45%, var(--bl-bg-1));
  min-height: 0;
}
.otp-tree-title {
  font-size: 11px; color: var(--bl-text-3);
  padding: 6px 10px 8px; margin-bottom: 4px;
  border-bottom: 1px dashed var(--bl-divider);
  font-weight: 600; letter-spacing: 0.3px;
  display: flex; align-items: center; gap: 4px;
}
/* 树节点 .otp-tn-* 的实际样式定义在文件末尾的非 scoped <style> 块里，
   因为 TreeRow 是通过 h() 渲染的内嵌子组件，scoped CSS 不会注入到它的子元素上。 */

.otp-pane { display: flex; flex-direction: column; overflow: hidden; min-height: 0; background: var(--bl-bg-1); }
.otp-pane-hd {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
/* 空数据态填满右侧 */
.otp-pane > .bl-empty { flex: 1; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 8px; }
.otp-search { position: relative; width: 280px; }
.otp-search .bl-input { padding-left: 28px; padding-right: 24px; height: 30px; }
.otp-search .ot-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.otp-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 14px; height: 14px; border: 0; background: var(--bl-bg-3); border-radius: 50%; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
}
.otp-grid {
  flex: 1; overflow: auto; padding: 12px;
  display: grid; gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  align-content: start;
}
.otp-card {
  position: relative;
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px 10px 10px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-left: 4px solid var(--otp-side, var(--bl-primary));
  border-radius: 6px;
  cursor: pointer; transition: border-color .15s, box-shadow .15s, background-color .15s;
  min-height: 74px; box-sizing: border-box;
}
.otp-card:hover { border-color: var(--bl-primary); border-left-color: var(--otp-side); box-shadow: var(--bl-shadow-1); }
.otp-card.is-on { border-color: var(--bl-primary); border-left-color: var(--otp-side); background: var(--bl-primary-soft); }
.otp-card-ic {
  width: 28px; height: 28px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.otp-card-body { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 3px; }
.otp-card-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 13.5px; font-weight: 500; line-height: 1.4;
}
.otp-card-title .bl-truncate { flex: 1; min-width: 0; }
.otp-card-meta {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11.5px; line-height: 1.4;
}
.otp-card-cmt { font-size: 12px; line-height: 1.4; color: var(--bl-text-3); }
.otp-card-check {
  position: absolute; right: 8px; top: 8px;
  width: 16px; height: 16px; background: var(--bl-primary); border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
}

.bl-truncate2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.otp-fade-enter-active, .otp-fade-leave-active { transition: opacity .15s; }
.otp-fade-enter-from, .otp-fade-leave-to { opacity: 0; }
</style>

<!-- 非 scoped 样式：TreeRow 是通过 h() 渲染的内嵌子组件，scoped CSS 不会注入 data-v 属性到其子元素，必须用全局选择器。
     用 .otp-tree 容器前缀做命名隔离，避免与其他组件冲突。 -->
<style>
.otp-tree .otp-tree-wrap { position: relative; }
.otp-tree .otp-tn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px;
  margin: 1px 0;
  border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-13);
  cursor: pointer;
  user-select: none;
  position: relative;
  z-index: 1;
  color: var(--bl-text-1);
  transition: background-color .12s ease, color .12s ease;
}
.otp-tree .otp-tn:hover { background: var(--bl-bg-hover); }
.otp-tree .otp-tn.is-active {
  background: var(--bl-primary-soft);
  color: var(--bl-primary);
  font-weight: 500;
}

.otp-tree .otp-tn-toggle {
  width: 16px; height: 16px;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-1);
  flex-shrink: 0;
  position: relative; z-index: 2;
}
.otp-tree .otp-tn-toggle:hover { color: var(--bl-primary); }
/* 有箭头的 toggle 盖一层本地背景色，让竖虚线被向外延展 3px 遮住 */
.otp-tree .otp-tn-toggle:not(.otp-tn-toggle-empty) {
  background: var(--bl-bg-2);
  border-radius: 3px;
  outline: 3px solid #f5f7fa;
}
.otp-tree .otp-tn:hover .otp-tn-toggle:not(.otp-tn-toggle-empty) {
  background: var(--bl-bg-hover);
  outline-color: var(--bl-bg-hover);
}
.otp-tree .otp-tn.is-active .otp-tn-toggle:not(.otp-tn-toggle-empty) {
  background: var(--bl-primary-soft);
  outline-color: var(--bl-primary-soft);
}
.otp-tree .otp-tn-toggle-empty {
  width: 16px; height: 16px; background: transparent; flex-shrink: 0;
}

.otp-tree .otp-tn-ico {
  width: 20px; height: 20px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.otp-tree .otp-tn-label {
  flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.otp-tree .otp-tn-count {
  flex-shrink: 0; margin-left: auto;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2);
  border-radius: 9px; padding: 0 7px; min-width: 20px;
  height: 17px; line-height: 17px; text-align: center;
  font-feature-settings: "tnum";
  transition: background-color .12s ease, color .12s ease;
}
.otp-tree .otp-tn.is-active .otp-tn-count {
  background: var(--bl-primary-soft-ss, var(--bl-bg-1));
  color: var(--bl-primary);
}

/* 树引线（与 Category TreeNode 完全对齐） */
.otp-tree .otp-tn-children {
  margin-left: 20px;
  padding-left: 0;
  position: relative;
}
.otp-tree .otp-tn-children::before {
  content: '';
  position: absolute;
  left: 14px; top: -2px; bottom: 18px;
  border-left: 1px dashed var(--bl-border-strong);
}
.otp-tree .otp-tn-children > .otp-tree-wrap > .otp-tn::before {
  content: '';
  position: absolute;
  left: 14px; top: 50%;
  width: 14px;
  border-top: 1px dashed var(--bl-border-strong);
}
</style>
