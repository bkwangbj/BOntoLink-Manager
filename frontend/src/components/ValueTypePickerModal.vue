<template>
  <Teleport to="body">
    <transition name="vtp-fade">
      <div v-if="open" :class="['vtp-mask', dm.state.free && 'is-free']" @click.self="onCancel">
        <div class="vtp-modal" :class="{ 'is-max': dm.state.maximized }" :style="dm.modalStyle.value" data-dm-root>
          <!-- 标题栏: 整体作为拖动把手 -->
          <div class="vtp-hd" :class="{ 'is-draggable': !dm.state.maximized }" @mousedown="dm.startDrag">
            <div class="vtp-hd-l">
              <span class="vtp-title">值类型选择</span>
              <span class="bl-muted vtp-sub" v-if="subtitle">{{ subtitle }}</span>
            </div>
            <div class="vtp-hd-r" @mousedown.stop>
              <button class="bl-btn bl-btn-text bl-btn-icon vtp-tbtn"
                      :title="dm.state.maximized ? '还原' : '最大化'"
                      @click="dm.toggleMax"
                      v-html="BL.icon(dm.state.maximized ? 'minimize' : 'maximize', 13)"></button>
              <button class="bl-btn bl-btn-text bl-btn-icon vtp-close" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
            </div>
          </div>

          <!-- 主体: 左树 + 右内容 -->
          <div class="vtp-body">
            <!-- 左侧导航 -->
            <aside class="vtp-tree">
              <div class="vtp-tree-hd">业务领域</div>
              <div class="vtp-tree-wrap">
                <div :class="['vtp-tn', !activeNode && 'is-active']" @click="activeNode = null">
                  <span class="vtp-tn-toggle vtp-tn-toggle-empty"></span>
                  <span class="vtp-tn-ico" style="background: var(--bl-primary)" v-html="BL.icon('grid', 12, '#fff')"></span>
                  <span class="vtp-tn-label">全部</span>
                  <span class="vtp-tn-count">{{ allRows.length }}</span>
                </div>
                <VtpTreeRow v-for="n in treeRoot" :key="n.id"
                            :node="n"
                            :active="activeNode"
                            :counts="catCounts"
                            :expanded="expandedSet"
                            @pick="onPickNode"
                            @toggle="onToggleNode" />
              </div>
            </aside>

            <!-- 右侧内容 -->
            <section class="vtp-pane">
              <!-- 顶部工具栏: 面包屑 + 搜索 -->
              <div class="vtp-pane-hd">
                <div class="vtp-bread">
                  <span class="bl-muted">范围:</span>
                  <span v-for="(seg, i) in activePath" :key="i" class="vtp-bread-seg">
                    <span v-if="i > 0" class="vtp-bread-sep">/</span>
                    <span class="vtp-bread-label">{{ seg }}</span>
                  </span>
                  <span class="bl-muted vtp-count">共 {{ filtered.length }} 项</span>
                </div>
                <div class="vtp-search">
                  <span class="vtp-search-ic" v-html="BL.icon('search', 13)"></span>
                  <input class="bl-input" placeholder="搜索: 名称 / API / RID / 说明" v-model="q" />
                  <button v-if="q" class="vtp-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
                </div>
              </div>

              <!-- 列表 -->
              <div class="vtp-list-wrap">
                <table class="bl-table vtp-table">
                  <colgroup>
                    <col style="width:36px" />
                    <col style="width:170px" />
                    <col style="width:140px" />
                    <col style="width:100px" />
                    <col style="width:90px" />
                    <col style="width:130px" />
                    <col style="width:160px" />
                    <col />
                    <col style="width:140px" />
                    <col style="width:80px" />
                    <col style="width:130px" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th class="t-center">
                        <input v-if="multi" type="checkbox" :checked="allChecked" @change="toggleAll" />
                      </th>
                      <th class="t-left">名称</th>
                      <th class="t-left">所在领域</th>
                      <th class="t-center">数据类型</th>
                      <th class="t-center">约束</th>
                      <th class="t-left">枚举</th>
                      <th class="t-left">RID</th>
                      <th class="t-left">说明</th>
                      <th class="t-left">API</th>
                      <th class="t-center">状态</th>
                      <th class="t-left">更新时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="r in filtered" :key="r.id"
                        :class="['vtp-row', selectedIds.has(r.id) && 'is-on']"
                        @click="toggle(r)">
                      <td class="t-center" @click.stop>
                        <input :type="multi ? 'checkbox' : 'radio'"
                               :name="multi ? undefined : 'vtp-radio'"
                               :checked="selectedIds.has(r.id)"
                               @change="toggle(r)" />
                      </td>
                      <td class="t-left">
                        <div class="vtp-name-cell">
                          <span class="vtp-ic" style="background:#1677ff" v-html="BL.icon('layers', 11, '#fff')"></span>
                          <span class="vtp-name bl-truncate" :title="r.rdfs_label || r.api_name">{{ r.rdfs_label || r.api_name }}</span>
                        </div>
                      </td>
                      <td class="t-left">
                        <span class="bl-truncate vtp-domain" :title="categoryLabel(r.category_code)">{{ categoryLabel(r.category_code) || '—' }}</span>
                      </td>
                      <td class="t-center"><span class="bl-tag">{{ r.base_type || '—' }}</span></td>
                      <td class="t-center">
                        <span :class="['bl-tag', constraintTagCls(r.constraint_type)]">{{ r.constraint_type || '—' }}</span>
                      </td>
                      <td class="t-left">
                        <span v-if="r.constraint_type === 'Enum'" class="bl-tag bl-truncate" :title="r.enum_label || r.enum_api_name">{{ r.enum_label || r.enum_api_name || '—' }}</span>
                        <span v-else class="bl-muted">—</span>
                      </td>
                      <td class="t-left"><span class="bl-mono bl-muted bl-truncate" :title="r.rid">{{ r.rid }}</span></td>
                      <td class="t-left">
                        <span class="bl-muted bl-truncate vtp-comment" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</span>
                      </td>
                      <td class="t-left"><span class="bl-mono bl-truncate" :title="r.api_name">{{ r.api_name }}</span></td>
                      <td class="t-center">
                        <span :class="['bl-tag', r.status === 1 ? 'bl-tag-success' : 'bl-tag-danger']"><StatusTag :status="r.status" /></span>
                      </td>
                      <td class="t-left"><span class="bl-muted vtp-time">{{ shortTime(r.update_time) }}</span></td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="!filtered.length" class="bl-empty" style="padding:48px">未匹配到值类型</div>
              </div>
            </section>
          </div>

          <!-- 底部操作栏 -->
          <div class="vtp-ft">
            <div class="vtp-ft-l bl-muted">
              <span v-if="selectedIds.size">已选 <b style="color:var(--bl-primary)">{{ selectedIds.size }}</b> 项</span>
            </div>
            <div class="vtp-ft-r">
              <button class="bl-btn" @click="onCancel">取消</button>
              <button class="bl-btn bl-btn-primary" :disabled="required && !selectedIds.size" @click="onConfirm">
                确定<span v-if="multi && selectedIds.size"> ({{ selectedIds.size }})</span>
              </button>
            </div>
          </div>

          <!-- 八向缩放热区 (最大化时隐藏) -->
          <DraggableHandles v-if="!dm.state.maximized" :on="dm.startResize" />
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch, h } from 'vue'
import { BL } from '@/lib/bl.js'
import StatusTag from '@/components/StatusTag.vue'
import { valueTypeApi, categoryApi } from '@/api'
import { useDraggableModal } from '@/lib/useDraggableModal.js'
import DraggableHandles from '@/components/DraggableHandles.vue'

/* 拖动 / 最大化 / 八向缩放 */
const dm = useDraggableModal({ minWidth: 720, minHeight: 480 })

const props = defineProps({
  open: { type: Boolean, default: false },
  multi: { type: Boolean, default: false },        // 默认单选
  required: { type: Boolean, default: false },
  modelValue: { type: [Array, String], default: () => [] },  // 已选 id 列表 (单选时为单个 id)
  excludeIds: { type: Array, default: () => [] },
  subtitle: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'update:modelValue', 'confirm', 'cancel'])

/* —— 数据 —— */
const allRows = ref([])
const tree = ref([])             // categoryApi.tree 返回的树
const catLabelMap = ref({})      // category_code → label

const activeNode = ref(null)     // 当前选中的分类节点 (null=全部)
const expandedSet = ref(new Set())
const q = ref('')
const selectedIds = ref(new Set())

/* —— 加载 —— */
async function load() {
  // 值类型 (含 enum_label 等冗余字段,已在后端 JOIN)
  allRows.value = await valueTypeApi.list().catch(() => [])
  // 分类树 (行业/领域)
  if (!tree.value.length) {
    tree.value = await categoryApi.tree().catch(() => [])
    // 构建 code → label 映射
    const m = {}
    const walk = (ns) => ns.forEach(n => {
      if (n.categoryCode) m[n.categoryCode] = n.label
      if (n.children) walk(n.children)
    })
    walk(tree.value)
    catLabelMap.value = m
  }
  // 默认展开顶级
  expandedSet.value = new Set(tree.value.map(n => n.id))
}

/* —— 树展开 / 点击 —— */
function onToggleNode(id) {
  const s = new Set(expandedSet.value)
  s.has(id) ? s.delete(id) : s.add(id)
  expandedSet.value = s
}
function onPickNode(node) {
  activeNode.value = node
}

/* 当前面包屑 (沿父链回溯) */
const activePath = computed(() => {
  if (!activeNode.value) return ['全部']
  // 在 tree 中查找节点的路径
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
  // 收集节点及其所有后代的 categoryCode
  const codes = new Set()
  const walk = (n) => {
    if (n.categoryCode) codes.add(n.categoryCode)
    if (n.children) n.children.forEach(walk)
  }
  walk(node)
  if (codes.has(row.category_code)) return true
  // 还需检查：如果 row 的 category_code 属于选中节点的某个祖先，也应显示
  // (例如值类型挂在"水利统计"，点击子节点"服务业统计"时仍应可见)
  function findAncestorCodes(nodes, target, chain) {
    for (const n of nodes) {
      if (n.id === target.id) {
        chain.forEach(a => { if (a.categoryCode) codes.add(a.categoryCode) })
        return true
      }
      if (n.children && findAncestorCodes(n.children, target, [...chain, n])) return true
    }
    return false
  }
  findAncestorCodes(tree.value, node, [])
  return codes.has(row.category_code)
}
const filtered = computed(() => {
  let list = allRows.value
  if (props.excludeIds?.length) {
    const excl = new Set(props.excludeIds)
    list = list.filter(r => !excl.has(r.id))
  }
  if (activeNode.value) list = list.filter(r => isUnderNode(r, activeNode.value))
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.api_name, r.rdfs_label, r.rid, r.rdfs_comment, r.base_type, r.constraint_type, r.enum_label]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  return list
})

/* —— 选择 —— */
function toggle(r) {
  const s = new Set(selectedIds.value)
  if (props.multi) {
    s.has(r.id) ? s.delete(r.id) : s.add(r.id)
  } else {
    s.clear()
    s.add(r.id)
  }
  selectedIds.value = s
}
const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(r => selectedIds.value.has(r.id)))
function toggleAll() {
  const s = new Set(selectedIds.value)
  if (allChecked.value) filtered.value.forEach(r => s.delete(r.id))
  else filtered.value.forEach(r => s.add(r.id))
  selectedIds.value = s
}

/* —— 类别 count + label —— */
const catCounts = computed(() => {
  const out = {}
  for (const r of allRows.value) {
    if (!r.category_code) continue
    out[r.category_code] = (out[r.category_code] || 0) + 1
  }
  // 累计到祖先 (用于树节点徽标)
  const memo = {}
  function calc(n) {
    if (memo[n.id] != null) return memo[n.id]
    let s = n.categoryCode ? (out[n.categoryCode] || 0) : 0
    if (n.children) n.children.forEach(c => { s += calc(c) })
    memo[n.id] = s
    return s
  }
  tree.value.forEach(calc)
  return memo
})
function categoryLabel(code) {
  if (!code) return ''
  return catLabelMap.value[code] || code
}

/* —— 工具 —— */
function constraintTagCls(t) {
  if (t === 'Enum')  return 'bl-tag-success'
  if (t === 'Regex') return 'bl-tag-warning'
  if (t === 'RID' || t === 'UUID') return 'bl-tag-primary'
  return ''
}
function shortTime(t) {
  if (!t) return '—'
  return String(t).slice(0, 16).replace('T', ' ')
}

/* —— 打开生命周期 —— */
watch(() => props.open, async (v) => {
  if (!v) { dm.reset(); return }
  // 初始化已选
  const init = Array.isArray(props.modelValue) ? props.modelValue : (props.modelValue ? [props.modelValue] : [])
  selectedIds.value = new Set(init.filter(Boolean))
  q.value = ''
  activeNode.value = null
  dm.reset()  // 每次打开都回到居中默认尺寸
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

/* —— 递归树节点 (h() 渲染) —— */
const VtpTreeRow = {
  name: 'VtpTreeRow',
  props: ['node', 'active', 'counts', 'expanded', 'level'],
  emits: ['pick', 'toggle'],
  setup(p, { emit: emitChild }) {
    return () => {
      const n = p.node
      const isOpen = p.expanded.has(n.id)
      const hasKids = (n.children || []).length > 0
      const isActive = p.active?.id === n.id
      const level = p.level || 0
      return h('div', { class: 'vtp-tnode' }, [
        h('div', {
          class: ['vtp-tn', isActive && 'is-active'],
          style: { paddingLeft: (level * 14) + 'px' },
          onClick: () => emitChild('pick', n)
        }, [
          hasKids
            ? h('span', {
                class: ['vtp-tn-toggle', isOpen && 'is-open'],
                onClick: (ev) => { ev.stopPropagation(); emitChild('toggle', n.id) },
                innerHTML: BL.icon('chevronRight', 11)
              })
            : h('span', { class: 'vtp-tn-toggle vtp-tn-toggle-empty' }),
          h('span', { class: 'vtp-tn-ico', style: { background: n.color || '#1677ff' }, innerHTML: BL.icon(n.icon || 'folder', 11, '#fff') }),
          h('span', { class: 'vtp-tn-label' }, n.label),
          h('span', { class: 'vtp-tn-count' }, p.counts[n.id] || 0)
        ]),
        hasKids && isOpen
          ? h('div', { class: 'vtp-tn-kids' }, (n.children).map(c => h(VtpTreeRow, { node: c, active: p.active, counts: p.counts, expanded: p.expanded, level: level + 1, key: c.id, onPick: (x) => emitChild('pick', x), onToggle: (x) => emitChild('toggle', x) })))
          : null
      ])
    }
  }
}
</script>

<style scoped>
.vtp-mask {
  /* 必须高于对象类型详情抽屉(1000) + 属性详情抽屉(1010) + resize 把手(1011) */
  position: fixed; inset: 0; z-index: 1200;
  background: rgba(0,0,0,.32);
  display: flex; align-items: center; justify-content: center;
}
/* free 模式下不再用 flex 居中,避免与 fixed 定位的弹框冲突 */
.vtp-mask.is-free { display: block; }
.vtp-modal {
  position: relative;  /* 缩放热区基于此绝对定位 */
  width: 1180px; max-width: 95vw;
  height: calc(100vh - 80px); max-height: 800px;
  background: var(--bl-bg-1); border-radius: 12px;
  box-shadow: 0 12px 48px rgba(0,0,0,.18);
  display: flex; flex-direction: column;
  overflow: hidden;
}
/* 最大化: 去阴影 / 圆角 */
.vtp-modal.is-max { box-shadow: none; }

/* 标题栏: 左侧 标题+副标题 一组,右侧操作按钮组 */
.vtp-hd {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--bl-divider);
  user-select: none;
}
.vtp-hd.is-draggable { cursor: move; }
.vtp-hd.is-draggable:active { cursor: grabbing; }
.vtp-hd-l { display: inline-flex; align-items: baseline; gap: 10px; min-width: 0; flex: 1; }
.vtp-hd-r { display: inline-flex; align-items: center; gap: 2px; flex-shrink: 0; margin-left: 12px; }
.vtp-title { font-size: 15px; font-weight: 600; color: var(--bl-text-1); white-space: nowrap; }
.vtp-sub { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vtp-tbtn, .vtp-close { flex-shrink: 0; }

/* 主体 */
.vtp-body {
  flex: 1; min-height: 0;
  display: grid; grid-template-columns: 220px 1fr;
  gap: 0;
}

/* 左树 */
.vtp-tree {
  background: color-mix(in srgb, var(--bl-bg-2) 45%, var(--bl-bg-1));
  border-right: 1px solid var(--bl-divider);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.vtp-tree-hd {
  flex-shrink: 0;
  padding: 8px 12px;
  font-size: 12px; color: var(--bl-text-3);
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-1);
}
.vtp-tree-wrap { flex: 1; min-height: 0; overflow: auto; padding: 6px 8px; }

/* 右内容 */
.vtp-pane { display: flex; flex-direction: column; min-width: 0; overflow: hidden; }
.vtp-pane-hd {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-1);
}
.vtp-bread { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--bl-text-1); flex-wrap: wrap; }
.vtp-bread-sep { color: var(--bl-text-3); }
.vtp-bread-label { color: var(--bl-text-1); font-weight: 500; }
.vtp-count { font-size: 12px; margin-left: 12px; }
.vtp-search { position: relative; width: 260px; }
.vtp-search .bl-input { padding-left: 28px; padding-right: 28px; height: 30px; font-size: 12px; }
.vtp-search-ic { position: absolute; left: 9px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.vtp-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 16px; height: 16px; border: 0;
  background: var(--bl-bg-3); color: var(--bl-text-3);
  border-radius: 50%; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
}

/* 列表 */
.vtp-list-wrap { flex: 1; min-height: 0; overflow: auto; background: var(--bl-bg-1); }
.vtp-table {
  width: 100%; min-width: 1280px;
  border-collapse: separate; border-spacing: 0;
  table-layout: fixed;
  font-size: 12px;
}
.vtp-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  padding: 0 10px; height: 36px;
  font-weight: 600; color: var(--bl-text-1);
  border-bottom: 1px solid var(--bl-divider);
  white-space: nowrap;
}
.vtp-table thead th.t-left { text-align: left; }
.vtp-table thead th.t-center { text-align: center; }
.vtp-table tbody tr { background: var(--bl-bg-1); cursor: pointer; }
.vtp-table tbody tr:nth-child(even) { background: var(--bl-bg-2); }
.vtp-table tbody tr:hover { background: var(--bl-bg-2); }
.vtp-table tbody tr.is-on { background: color-mix(in srgb, var(--bl-primary) 12%, var(--bl-bg-1)) !important; }
.vtp-table td { padding: 0 10px; height: 38px; border-bottom: 1px solid #f0f0f0; vertical-align: middle; }
.vtp-table td.t-left { text-align: left; }
.vtp-table td.t-center { text-align: center; }
.vtp-table td .bl-truncate { display: inline-block; max-width: 100%; vertical-align: middle; }

.vtp-name-cell { display: flex; align-items: center; gap: 6px; min-width: 0; }
.vtp-ic { width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.vtp-name { font-weight: 500; color: var(--bl-text-1); flex: 1; min-width: 0; }
.vtp-domain { max-width: 100%; }
.vtp-comment { max-width: 100%; }
.vtp-time { font-size: 11.5px; }

/* 底部 */
.vtp-ft {
  flex-shrink: 0;
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 16px;
  border-top: 1px solid var(--bl-divider);
}
.vtp-ft-l { font-size: 12px; }
.vtp-ft-r { display: inline-flex; gap: 8px; }

.vtp-fade-enter-active, .vtp-fade-leave-active { transition: opacity .18s; }
.vtp-fade-enter-from, .vtp-fade-leave-to { opacity: 0; }
</style>

<!-- 非 scoped: VtpTreeRow 通过 h() 渲染,需要全局选择器 -->
<style>
.vtp-tnode { position: relative; }
.vtp-tn {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 8px;
  border-radius: 4px;
  font-size: 12.5px; color: var(--bl-text-1);
  cursor: pointer; user-select: none;
}
.vtp-tn:hover { background: var(--bl-bg-hover); }
.vtp-tn.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.vtp-tn-toggle {
  width: 14px; height: 14px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-3);
  transition: transform .15s;
}
.vtp-tn-toggle.is-open { transform: rotate(90deg); }
.vtp-tn-toggle-empty { color: transparent; cursor: default; }
.vtp-tn-ico {
  width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.vtp-tn-label { flex: 1; min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.vtp-tn-count {
  flex-shrink: 0;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2);
  border-radius: 9px; padding: 0 7px; min-width: 18px;
  height: 16px; line-height: 16px; text-align: center;
}
.vtp-tn.is-active .vtp-tn-count { background: var(--bl-bg-1); color: var(--bl-primary); }
.vtp-tn-kids { /* 子级缩进由 inline padding-left 控制 */ }
</style>
