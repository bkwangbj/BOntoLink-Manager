<template>
  <div class="stv-root">
    <!-- 顶部统一搜索 (按 名称 / 代码 / 拼音 等查询, 横跨结构 + 共享) -->
    <div class="stv-search">
      <div class="stv-search-box">
        <span class="stv-search-ic" v-html="BL.icon('search', 13)"></span>
        <input class="bl-input" v-model="q" placeholder="搜索结构 / 共享属性 (名称 / 代码 / 拼音)" />
      </div>
      <span class="bl-muted" style="font-size:12px;margin-left:8px">
        共 <b style="color:var(--bl-text-1)">{{ filteredStructs.length }}</b> 个结构 ·
        含 <b style="color:var(--bl-text-1)">{{ matchedSharedCount }}</b> 个匹配共享属性
      </span>
    </div>

    <!-- 左右双栏 + 中间拖拽分隔条 -->
    <div ref="splitRef" class="stv-split">
      <!-- 左侧: 结构列表 -->
      <section class="stv-left" :style="{ flex: `0 0 ${leftRatio * 100}%` }">
        <div class="stv-tb">
          <div class="stv-tb-l">结构属性列表</div>
          <div class="stv-tb-r">
            <button class="bl-btn bl-btn-sm bl-btn-primary" @click="openCreate">
              <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">添加</span>
            </button>
            <button v-if="leftChecked.size" class="bl-btn bl-btn-sm stv-del-btn" @click="onBatchDelete">
              <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除 ({{ leftChecked.size }})</span>
            </button>
          </div>
        </div>

        <div class="stv-list-scroll">
          <table class="bl-table stv-table">
            <thead>
              <tr>
                <th class="t-center stv-c1" style="width:34px"><input type="checkbox" :checked="allLeftChecked" @change="toggleAllLeft" /></th>
                <th class="t-left stv-c2"><span class="th-sort" @click="toggleSort('rdfs_label')">名称<span class="th-arrow">{{ sortArrow('rdfs_label') }}</span></span></th>
                <th class="t-left stv-c3"><span class="th-sort" @click="toggleSort('struct_code')">编码<span class="th-arrow">{{ sortArrow('struct_code') }}</span></span></th>
                <th class="t-center" style="width:70px"><span class="th-sort" @click="toggleSort('ref_count')">引用<span class="th-arrow">{{ sortArrow('ref_count') }}</span></span></th>
                <th class="t-center" style="width:70px"><span class="th-sort" @click="toggleSort('item_count')">属性<span class="th-arrow">{{ sortArrow('item_count') }}</span></span></th>
                <th class="t-center" style="width:80px">状态</th>
                <th class="t-left" style="width:130px">所属领域</th>
                <th class="t-left" style="width:130px"><span class="th-sort" @click="toggleSort('update_time')">更新时间<span class="th-arrow">{{ sortArrow('update_time') }}</span></span></th>
                <th class="t-left" style="width:160px">备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(s, idx) in pagedStructs" :key="s.id"
                  :class="['stv-row', selectedId === s.id && 'is-active']"
                  @click="selectStruct(s)">
                <td class="t-center stv-c1" @click.stop>
                  <input type="checkbox" :checked="leftChecked.has(s.id)" @change="toggleLeftCheck(s.id)" />
                </td>
                <td class="stv-c2 t-left">
                  <span class="stv-name-cell">
                    <span class="stv-ic" v-html="BL.icon('layers', 11, '#fff')"></span>
                    <span class="bl-truncate" :title="s.rdfs_label">{{ s.rdfs_label || s.struct_code }}</span>
                  </span>
                </td>
                <td class="stv-c3 t-left"><span class="bl-mono bl-truncate" :title="s.struct_code">{{ s.struct_code }}</span></td>
                <td class="t-center">
                  <span :class="['bl-tag', (s.ref_count || 0) > 0 && 'bl-tag-primary']">{{ s.ref_count || 0 }}</span>
                </td>
                <td class="t-center"><span class="bl-tag">{{ s.item_count || 0 }}</span></td>
                <td class="t-center" @click.stop>
                  <MiniSwitch :checked="s.status === 1" @change="(v) => onToggleStatus(s, v)" />
                </td>
                <td class="t-left"><span class="bl-truncate" :title="domainLabel(s.category_code) || s.category_code">{{ domainLabel(s.category_code) || '—' }}</span></td>
                <td class="t-left"><span class="bl-muted" style="font-size:11px">{{ shortTime(s.update_time) }}</span></td>
                <td class="t-left">
                  <span class="bl-muted bl-truncate" :title="s.rdfs_comment">{{ truncate(s.rdfs_comment, 20) || '—' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!pagedStructs.length" class="bl-empty" style="padding:48px">暂无匹配的结构属性</div>
        </div>

        <!-- 翻页 -->
        <div class="stv-pager">
          <span class="bl-muted" style="font-size:12px">共 {{ filteredStructs.length }} 项</span>
          <div class="bl-row" style="gap:4px">
            <span class="bl-muted" style="font-size:12px;margin-right:4px">每页</span>
            <select class="bl-input stv-page-size" v-model.number="pageSize">
              <option :value="10">10</option><option :value="20">20</option><option :value="50">50</option>
            </select>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page <= 1" @click="page--">‹</button>
            <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page >= totalPages" @click="page++">›</button>
          </div>
        </div>
      </section>

      <!-- 拖拽手柄 -->
      <div class="stv-handle" :class="resizing && 'is-resizing'" @mousedown="onHandleDown" title="拖动调整宽度"></div>

      <!-- 右侧: 当前结构的条目表 -->
      <section class="stv-right">
        <div class="stv-tb">
          <div class="stv-tb-l">
            <template v-if="selectedStruct">
              <span class="stv-ic" style="margin-right:6px" v-html="BL.icon('layers', 11, '#fff')"></span>
              <b>{{ selectedStruct.rdfs_label || selectedStruct.struct_code }}</b>
              <span class="bl-muted" style="margin-left:6px;font-size:12px">· {{ items.length }} 个属性</span>
            </template>
            <template v-else>
              <span class="bl-muted">未选中结构</span>
            </template>
          </div>
          <div class="stv-tb-r" v-if="selectedStruct">
            <button class="bl-btn bl-btn-sm bl-btn-primary" @click="openPicker">
              <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">添加属性</span>
            </button>
            <button v-if="itemChecked.size" class="bl-btn bl-btn-sm stv-del-btn" @click="onItemBatchDelete">
              <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除 ({{ itemChecked.size }})</span>
            </button>
          </div>
        </div>

        <div class="stv-list-scroll" v-if="selectedStruct">
          <table class="bl-table stv-table">
            <thead>
              <tr>
                <th class="t-center" style="width:34px"><input type="checkbox" :checked="allItemChecked" @change="toggleAllItem" /></th>
                <th class="t-center" style="width:38px" title="拖拽排序">↕</th>
                <th class="t-center" style="width:50px">序号</th>
                <th class="t-left">属性</th>
                <th class="t-center" style="width:110px">数据类型</th>
                <th class="t-left" style="width:140px">值类型</th>
                <th class="t-center" style="width:60px" title="必填">必填</th>
                <th class="t-center stv-act-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(it, idx) in items" :key="it.id"
                  :class="['stv-row', 'stv-item-row']"
                  draggable="true"
                  @dragstart="onDragStart(idx, $event)"
                  @dragover.prevent="onDragOver(idx)"
                  @dragend="onDragEnd">
                <td class="t-center" @click.stop>
                  <input type="checkbox" :checked="itemChecked.has(it.id)" @change="toggleItemCheck(it.id)" />
                </td>
                <td class="t-center stv-drag-cell" title="按住拖动重排">
                  <span v-html="BL.icon('grip', 12)"></span>
                </td>
                <td class="t-center"><b>{{ idx + 1 }}</b></td>
                <td class="t-left">
                  <a class="stv-prop-link" @click="onClickProp(it)">
                    <span class="bl-truncate" style="font-weight:500">{{ it.prop_label || it.prop_code }}</span>
                  </a>
                  <div class="bl-mono bl-muted" style="font-size:11px">{{ it.prop_code }}</div>
                </td>
                <td class="t-center"><span class="bl-tag">{{ it.prop_data_type || '—' }}</span></td>
                <td class="t-left"><span class="bl-truncate" :title="it.prop_value_type">{{ it.prop_value_type || '—' }}</span></td>
                <td class="t-center"><input type="checkbox" :checked="!!it.prop_is_required" disabled /></td>
                <td class="t-center stv-act-col" @click.stop>
                  <div class="stv-act-row">
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="查看共享属性" @click="onClickProp(it)" v-html="BL.icon('eye', 12)"></button>
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除" @click="onItemRemove(it)" v-html="BL.icon('trash', 12)"></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!items.length" class="bl-empty" style="padding:36px">
            该结构尚无属性,点击右上「添加属性」从共享属性库选取
          </div>
        </div>
        <div v-else class="bl-empty" style="padding:48px;flex:1">请在左侧选择一个结构属性</div>
      </section>
    </div>

    <!-- 共享属性选择面板 -->
    <SharedPropertyPickerModal v-model:open="pickerOpen"
                               :multi="true"
                               :exclude-ids="excludeIdsForPicker"
                               :recently-used-ids="recentPropIds"
                               @confirm="onPickerConfirm" />

    <!-- 共享属性详情(只读浮层 - 复用主页面抽屉?暂用简单弹层) -->
    <SharedPropertyDetailDrawer v-model:open="propDrawerOpen"
                                :property="propDrawerData" />

    <!-- 新建/编辑结构属性 -->
    <div v-if="formOpen" class="bl-modal-mask" @click.self="formOpen=false">
      <div class="bl-modal" style="width:520px">
        <div class="bl-modal-hd">{{ form.id ? '编辑结构属性' : '新建结构属性' }}</div>
        <div class="bl-modal-body bl-col" style="gap:10px">
          <FieldRow label="名称 *" inline><input class="bl-input" v-model="form.rdfs_label" placeholder="如:姓名 / 地址 / 时间段" /></FieldRow>
          <FieldRow label="编码 *" inline hint="snake_case 或 CamelCase,全局唯一">
            <input class="bl-input bl-mono" v-model="form.struct_code" :disabled="!!form.id" placeholder="如:FullName" />
          </FieldRow>
          <FieldRow label="所属领域" inline>
            <select class="bl-input" v-model="form.category_code">
              <option value="">— 通用 —</option>
              <option v-for="c in flatCategories" :key="c.code" :value="c.code">{{ c.indent }}{{ c.label }}</option>
            </select>
          </FieldRow>
          <FieldRow label="状态" inline>
            <div class="radio-group">
              <label class="radio-item"><input type="radio" :value="1" v-model.number="form.status" /> 启用</label>
              <label class="radio-item"><input type="radio" :value="0" v-model.number="form.status" /> 禁用</label>
            </div>
          </FieldRow>
          <FieldRow label="备注"><textarea class="bl-textarea" rows="3" v-model="form.rdfs_comment" placeholder="结构用途 / 业务含义"></textarea></FieldRow>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="formOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!form.rdfs_label || !form.struct_code" @click="onSubmitForm">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, h } from 'vue'
import { BL } from '@/lib/bl.js'
import { structTypeApi, sharedPropertyApi, categoryApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'
import SharedPropertyPickerModal from '@/components/SharedPropertyPickerModal.vue'
import SharedPropertyDetailDrawer from './SharedPropertyDetailDrawer.vue'

const props = defineProps({
  selectedCodes: { type: Object, default: null }   // Set<category_code> | null
})
const emit = defineEmits(['counts'])

/* MiniSwitch */
const MiniSwitch = {
  name: 'MiniSwitch',
  props: { checked: Boolean, disabled: Boolean },
  emits: ['change'],
  setup(p, { emit }) {
    return () => h('span', {
      class: ['mini-sw', p.checked && 'is-on', p.disabled && 'is-disabled'],
      onClick: (e) => { e.stopPropagation(); if (!p.disabled) emit('change', !p.checked) }
    }, h('span', { class: 'mini-sw-dot' }))
  }
}

/* —— 数据 —— */
const structs = ref([])      // 全部结构属性
const allShared = ref([])    // 共享属性 (供搜索 + 排除已有)
const items = ref([])        // 当前选中结构的条目
const categoryMap = ref({})

const selectedId = ref(null)
const selectedStruct = computed(() => structs.value.find(s => s.id === selectedId.value) || null)

const q = ref('')
const leftChecked = ref(new Set())
const itemChecked = ref(new Set())
const sortKey = ref('')
const sortDir = ref('')
const page = ref(1)
const pageSize = ref(10)

const pickerOpen = ref(false)
const propDrawerOpen = ref(false)
const propDrawerData = ref(null)
const formOpen = ref(false)
const form = reactive(defaultForm())
function defaultForm() {
  return { id: '', struct_code: '', rdfs_label: '', rdfs_comment: '', category_code: '', status: 1 }
}

/* —— 加载 —— */
async function loadStructs() {
  structs.value = await structTypeApi.list().catch(() => [])
  emit('counts', structs.value.length)
  // 默认选中第一条
  if (!selectedId.value && filteredStructs.value.length) {
    selectStruct(filteredStructs.value[0])
  }
}
async function loadShared() {
  allShared.value = await sharedPropertyApi.list().catch(() => [])
}
async function loadItems(structId) {
  items.value = await structTypeApi.items(structId).catch(() => [])
  itemChecked.value = new Set()
}
async function loadCategoryMap() {
  if (Object.keys(categoryMap.value).length) return
  const tree = await categoryApi.tree().catch(() => [])
  const m = {}
  const walk = (ns) => ns.forEach(n => { if (n.categoryCode) m[n.categoryCode] = n.label; if (n.children) walk(n.children) })
  walk(tree)
  categoryMap.value = m
}
const flatCategories = computed(() => {
  const out = []
  const walk = async () => {}
  return Object.entries(categoryMap.value).map(([code, label]) => ({ code, label, indent: '' }))
})
function domainLabel(code) { return code ? (categoryMap.value[code] || code) : '' }

onMounted(async () => {
  await Promise.all([loadStructs(), loadShared(), loadCategoryMap()])
})

/* —— 选择结构 —— */
async function selectStruct(s) {
  selectedId.value = s.id
  await loadItems(s.id)
}

/* —— 搜索 + 过滤 + 排序 —— */
function lc(v) { return String(v || '').toLowerCase() }
function structMatchesQ(s, k) {
  if (!k) return true
  return [s.rdfs_label, s.struct_code, s.rdfs_comment].filter(Boolean).some(x => lc(x).includes(k))
    // 结构包含的共享属性命中也算结构命中(便于通过共享属性反查到结构)
    || sharedIdsByQ.value.some(pid => itemPropsByStruct.value[s.id]?.has(pid))
}
/* 缓存: 每个结构包含的 prop_id 集合 (仅在显示中查询时) */
const itemPropsByStruct = computed(() => {
  // 仅当 q 不为空时才需要计算; 暂时跳过严格计算返回空 map
  return {}
})
const sharedIdsByQ = computed(() => {
  const k = lc(q.value).trim()
  if (!k) return []
  return allShared.value.filter(p => [p.rdfs_label, p.prop_code, p.rdfs_comment].filter(Boolean).some(x => lc(x).includes(k))).map(p => p.id)
})
const matchedSharedCount = computed(() => sharedIdsByQ.value.length)

const filteredStructs = computed(() => {
  let list = structs.value
  if (props.selectedCodes) list = list.filter(s => props.selectedCodes.has(s.category_code))
  const k = lc(q.value).trim()
  if (k) list = list.filter(s => structMatchesQ(s, k))
  if (sortKey.value && sortDir.value) {
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      const va = a[sortKey.value]; const vb = b[sortKey.value]
      if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
      return String(va || '').localeCompare(String(vb || '')) * dir
    })
  }
  return list
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredStructs.value.length / pageSize.value)))
const pagedStructs = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredStructs.value.slice(start, start + pageSize.value)
})
watch(filteredStructs, () => {
  if (page.value > totalPages.value) page.value = totalPages.value
  // 当前选中不在过滤结果里, 自动切第一条
  if (selectedId.value && !filteredStructs.value.some(s => s.id === selectedId.value)) {
    if (filteredStructs.value.length) selectStruct(filteredStructs.value[0])
    else { selectedId.value = null; items.value = [] }
  }
})

function toggleSort(k) {
  if (sortKey.value !== k) { sortKey.value = k; sortDir.value = 'asc' }
  else if (sortDir.value === 'asc') sortDir.value = 'desc'
  else { sortKey.value = ''; sortDir.value = '' }
}
function sortArrow(k) {
  if (sortKey.value !== k) return ' ⇅'
  return sortDir.value === 'asc' ? ' ↑' : ' ↓'
}

/* —— 选择 / 全选 —— */
const allLeftChecked = computed(() => pagedStructs.value.length > 0
  && pagedStructs.value.every(s => leftChecked.value.has(s.id)))
function toggleLeftCheck(id) {
  const s = new Set(leftChecked.value)
  s.has(id) ? s.delete(id) : s.add(id); leftChecked.value = s
}
function toggleAllLeft() {
  const s = new Set(leftChecked.value)
  if (allLeftChecked.value) pagedStructs.value.forEach(x => s.delete(x.id))
  else pagedStructs.value.forEach(x => s.add(x.id))
  leftChecked.value = s
}

const allItemChecked = computed(() => items.value.length > 0
  && items.value.every(it => itemChecked.value.has(it.id)))
function toggleItemCheck(id) {
  const s = new Set(itemChecked.value)
  s.has(id) ? s.delete(id) : s.add(id); itemChecked.value = s
}
function toggleAllItem() {
  const s = new Set(itemChecked.value)
  if (allItemChecked.value) items.value.forEach(it => s.delete(it.id))
  else items.value.forEach(it => s.add(it.id))
  itemChecked.value = s
}

/* —— 状态切换 —— */
async function onToggleStatus(s, v) {
  try {
    await structTypeApi.update(s.id, { ...s, status: v ? 1 : 0 })
    s.status = v ? 1 : 0
    BL.success(v ? '已启用' : '已禁用')
  } catch (e) { BL.error(e?.msg || '保存失败') }
}

/* —— 删除 —— */
async function onBatchDelete() {
  const ids = [...leftChecked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中的 ${ids.length} 个结构属性 (其条目也将被清除)?`, danger: true, okText: '删除' })
  if (!ok) return
  await structTypeApi.batchRemove(ids).catch(() => null)
  BL.success(`已删除 ${ids.length} 个`)
  leftChecked.value = new Set()
  if (selectedId.value && ids.includes(selectedId.value)) selectedId.value = null
  await loadStructs()
}

/* —— 表单(新建/编辑) —— */
function openCreate() {
  Object.assign(form, defaultForm())
  formOpen.value = true
}
async function onSubmitForm() {
  try {
    if (form.id) {
      await structTypeApi.update(form.id, { ...form })
      BL.success('已保存')
    } else {
      await structTypeApi.create({ ...form })
      BL.success('已创建')
    }
    formOpen.value = false
    await loadStructs()
  } catch (e) { BL.error(e?.msg || '保存失败') }
}

/* —— 条目管理 —— */
const excludeIdsForPicker = computed(() => items.value.map(it => it.prop_id))
const recentPropIds = ref([])
function openPicker() {
  if (!selectedStruct.value) return
  recentPropIds.value = items.value.map(it => it.prop_id)
  pickerOpen.value = true
}
async function onPickerConfirm({ ids, rows: picked }) {
  if (!selectedStruct.value || !ids.length) return
  const startSort = items.value.length
  // 拼成新的 items 列表 (已有 + 新增) → 整体覆盖式保存
  const existing = items.value.map((it, i) => ({ prop_id: it.prop_id, sort_no: i + 1 }))
  const additions = ids.map((pid, i) => ({ prop_id: pid, sort_no: startSort + i + 1 }))
  const all = [...existing, ...additions]
  try {
    await structTypeApi.update(selectedStruct.value.id, {
      ...selectedStruct.value, items: all
    })
    BL.success(`已添加 ${ids.length} 个属性`)
    await loadItems(selectedStruct.value.id)
    await loadStructs()  // 刷新列表 item_count
  } catch (e) { BL.error(e?.msg || '添加失败') }
}
async function onItemRemove(it) {
  const ok = await BL.confirm({ title: '移除属性', content: `从结构「${selectedStruct.value.rdfs_label}」中移除「${it.prop_label || it.prop_code}」?`, danger: true, okText: '移除' })
  if (!ok) return
  const newItems = items.value.filter(x => x.id !== it.id).map((x, i) => ({ prop_id: x.prop_id, sort_no: i + 1 }))
  try {
    await structTypeApi.update(selectedStruct.value.id, { ...selectedStruct.value, items: newItems })
    BL.success('已移除')
    await loadItems(selectedStruct.value.id)
    await loadStructs()
  } catch (e) { BL.error(e?.msg || '移除失败') }
}
async function onItemBatchDelete() {
  const ids = [...itemChecked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量移除', content: `从结构中移除选中的 ${ids.length} 个属性?`, danger: true, okText: '移除' })
  if (!ok) return
  const newItems = items.value.filter(x => !ids.includes(x.id)).map((x, i) => ({ prop_id: x.prop_id, sort_no: i + 1 }))
  try {
    await structTypeApi.update(selectedStruct.value.id, { ...selectedStruct.value, items: newItems })
    BL.success(`已移除 ${ids.length} 个`)
    itemChecked.value = new Set()
    await loadItems(selectedStruct.value.id)
    await loadStructs()
  } catch (e) { BL.error(e?.msg || '移除失败') }
}

/* —— 拖拽重排 —— */
const dragFromIdx = ref(-1)
function onDragStart(idx, ev) {
  dragFromIdx.value = idx
  ev.dataTransfer.effectAllowed = 'move'
}
function onDragOver(idx) {
  if (dragFromIdx.value < 0 || dragFromIdx.value === idx) return
  const arr = [...items.value]
  const [moved] = arr.splice(dragFromIdx.value, 1)
  arr.splice(idx, 0, moved)
  items.value = arr
  dragFromIdx.value = idx
}
async function onDragEnd() {
  if (dragFromIdx.value < 0 || !selectedStruct.value) return
  dragFromIdx.value = -1
  // 持久化新顺序
  const newItems = items.value.map((it, i) => ({ prop_id: it.prop_id, sort_no: i + 1 }))
  try {
    await structTypeApi.update(selectedStruct.value.id, { ...selectedStruct.value, items: newItems })
    BL.success('顺序已更新')
    await loadItems(selectedStruct.value.id)
  } catch (e) { BL.error(e?.msg || '排序保存失败') }
}

/* —— 查看属性详情 —— */
async function onClickProp(it) {
  const full = await sharedPropertyApi.get(it.prop_id).catch(() => null)
  if (full) {
    propDrawerData.value = full
    propDrawerOpen.value = true
  }
}

/* —— 左右栏宽度拖拽 —— */
const splitRef = ref(null)
const leftRatio = ref(parseFloat(localStorage.getItem('bl.stv.leftRatio') || '0.33'))
const resizing = ref(false)
function onHandleDown(ev) {
  ev.preventDefault()
  resizing.value = true
  function move(e) {
    if (!splitRef.value) return
    const rect = splitRef.value.getBoundingClientRect()
    const ratio = (e.clientX - rect.left) / rect.width
    leftRatio.value = Math.max(0.2, Math.min(0.6, ratio))
  }
  function up() {
    resizing.value = false
    localStorage.setItem('bl.stv.leftRatio', String(leftRatio.value))
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
  }
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

/* —— 工具 —— */
function shortTime(t) { if (!t) return '—'; return String(t).slice(0, 16) }
function truncate(s, n) {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '...' : s
}
</script>

<style scoped>
.stv-root {
  flex: 1; min-height: 0;
  display: flex; flex-direction: column;
  background: var(--bl-bg-1);
}

/* 顶部统一搜索 */
.stv-search {
  flex-shrink: 0; display: flex; align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--bl-divider);
  gap: 8px;
}
.stv-search-box { position: relative; flex: 0 1 360px; }
.stv-search-ic { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.stv-search-box .bl-input { padding-left: 30px; height: 30px; width: 100%; }

/* 左右双栏 */
.stv-split {
  flex: 1; display: flex; min-height: 0;
  position: relative;
}
.stv-left, .stv-right {
  display: flex; flex-direction: column;
  min-width: 0; min-height: 0;
  overflow: hidden;
}
.stv-right { flex: 1; }
.stv-handle {
  flex-shrink: 0; width: 6px; cursor: col-resize;
  background: transparent;
  transition: background-color .15s;
  position: relative;
}
.stv-handle:hover, .stv-handle.is-resizing { background: var(--bl-primary); }
.stv-handle::before {
  content: ''; position: absolute;
  top: 50%; left: 50%; transform: translate(-50%,-50%);
  width: 2px; height: 24px;
  background: var(--bl-divider); border-radius: 2px;
}
.stv-handle:hover::before { background: #fff; }

/* 工具栏 */
.stv-tb {
  flex-shrink: 0; display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
  font-size: 13px;
}
.stv-tb-l { font-weight: 500; color: var(--bl-text-1); display: inline-flex; align-items: center; }
.stv-tb-r { display: inline-flex; gap: 6px; }
.stv-del-btn { background: #fff; border: 1px solid #f53f3f; color: #f53f3f; }
.stv-del-btn:hover { background: #fff1f0; }

/* 列表 */
.stv-list-scroll { flex: 1; overflow: auto; min-height: 0; }
.stv-table {
  width: 100%; border-collapse: separate; border-spacing: 0;
  table-layout: auto;
}
.stv-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  box-shadow: inset 0 -1px 0 var(--bl-divider);
  font-weight: 600; padding: 0 8px;
  font-size: 12px; height: 34px; color: #333;
  white-space: nowrap;
}
.stv-table thead th.t-left { text-align: left; }
.stv-table tbody tr { background: #fff; cursor: pointer; }
.stv-table tbody tr:hover { background: #f5f7fa; }
.stv-table tbody tr.is-active { background: var(--bl-primary-soft); }
.stv-table td { padding: 0 8px; font-size: 12px; height: 36px; vertical-align: middle; }
.stv-table td.t-left { text-align: left; }
.stv-table td.t-center { text-align: center; }

/* 名称单元格 */
.stv-name-cell { display: inline-flex; align-items: center; gap: 8px; max-width: 100%; min-width: 0; }
.stv-ic {
  width: 20px; height: 20px; border-radius: 4px;
  background: var(--bl-primary);
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
}

/* 列宽提示 (不再 sticky 锁定 — 表格在左侧固定面板内已无横向滚动;
   原 sticky `left: 184px` 在 table-layout: auto 下与真实列起点错位,
   产生用户看到的"空白列"假象,这里彻底移除) */
.stv-c1 { width: 34px; }
.stv-c2 { min-width: 140px; }
.stv-c3 { min-width: 110px; }

/* 右侧条目表 操作列: 两个图标按钮强制单行不换行 */
.stv-act-col { width: 80px; white-space: nowrap; }
.stv-act-row {
  display: inline-flex; align-items: center;
  gap: 2px; flex-wrap: nowrap;
  justify-content: center;
}
.stv-act-row .bl-btn { flex-shrink: 0; }

/* 排序 */
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }

/* 拖拽列 */
.stv-drag-cell { cursor: grab; color: var(--bl-text-3); }
.stv-item-row[draggable="true"]:active { cursor: grabbing; }

/* 属性名链接样式 */
.stv-prop-link { color: var(--bl-primary); cursor: pointer; }
.stv-prop-link:hover { text-decoration: underline; }

/* 翻页 */
.stv-pager {
  flex-shrink: 0; display: flex; justify-content: space-between; align-items: center;
  padding: 6px 12px; border-top: 1px solid var(--bl-divider);
  font-size: var(--bl-fs-12);
}
.stv-page-size { width: 56px; height: 26px; }

/* 模态 (使用全局 bl-modal-* 样式) */
.bl-modal-ft { padding: 10px 14px; border-top: 1px solid var(--bl-divider); display: flex; justify-content: flex-end; gap: 8px; }
.radio-group { display: inline-flex; gap: 20px; }
.radio-item { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.bl-textarea {
  width: 100%; min-height: 70px; padding: 8px 10px;
  border: 1px solid var(--bl-border); border-radius: 4px;
  background: #fff; font-family: inherit; font-size: 13px;
  line-height: 1.55; resize: vertical;
}
.bl-textarea:focus { outline: none; border-color: var(--bl-primary); }
</style>

<!-- 非 scoped: MiniSwitch h() 内联 -->
<style>
.mini-sw {
  display: inline-block; width: 26px; height: 14px;
  border-radius: 8px; background: #c9cdd4;
  position: relative; cursor: pointer;
  transition: background-color .15s;
  vertical-align: middle;
}
.mini-sw.is-on { background: var(--bl-primary); }
.mini-sw.is-disabled { opacity: .55; cursor: default; }
.mini-sw-dot {
  position: absolute; left: 2px; top: 2px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #fff; transition: left .15s;
  box-shadow: 0 1px 2px rgba(0,0,0,.2);
}
.mini-sw.is-on .mini-sw-dot { left: 14px; }
</style>
