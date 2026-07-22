<template>
  <div class="dm-page">
    <!-- 顶部 -->
    <div class="dm-topbar">
      <div class="dm-title-wrap">
        <span class="dm-title">字典管理</span>
        <span class="dm-subtitle bl-muted">系统通用字典配置</span>
      </div>
      <div class="dm-actions">
        <button class="bl-btn bl-btn-sm" @click="refreshCache" :disabled="refreshing">
          <span v-html="BL.icon('refresh', 12)"></span>
          <span style="margin-left:4px">{{ refreshing ? '重载中...' : '发布重载' }}</span>
        </button>
        <button class="bl-btn bl-btn-sm bl-btn-primary" @click="openDefForm()">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建字典</span>
        </button>
      </div>
    </div>

    <div class="dm-body">
      <!-- 左侧：字典卡片列表 -->
      <div class="dm-side">
        <div class="dm-side-search">
          <span v-html="BL.icon('search', 12)"></span>
          <input class="bl-input dm-search-input" v-model="defQ" placeholder="搜索字典名称/编码" />
        </div>
        <div class="dm-card-list">
          <div v-for="d in filteredDefs" :key="d.id"
               :class="['dm-card', selectedDef?.id === d.id && 'is-on']"
               @click="selectDef(d)">
            <div class="dm-card-top">
              <span class="dm-card-code bl-mono">{{ d.dict_code }}</span>
              <span :class="['bl-tag', d.status === 1 ? 'bl-tag-primary' : 'bl-tag-muted']" style="font-size:11px">
                <StatusTag :status="d.status" />
              </span>
            </div>
            <div class="dm-card-name">{{ d.dict_name }}</div>
            <div class="dm-card-desc" v-if="d.rdfs_comment">{{ d.rdfs_comment }}</div>
            <div class="dm-card-ft">
              <span class="bl-muted" style="font-size:11px">{{ itemCountMap[d.id] ?? 0 }} 个条目</span>
              <span class="dm-card-actions">
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="重载该字典缓存" @click.stop="reloadDict(d)" v-html="BL.icon('refresh', 10)"></button>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click.stop="openDefForm(d)" v-html="BL.icon('edit', 11)"></button>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click.stop="removeDef(d)" v-html="BL.icon('trash', 11)"></button>
              </span>
            </div>
          </div>
          <div v-if="!filteredDefs.length" class="bl-empty" style="padding:32px;font-size:12px">暂无字典</div>
        </div>
      </div>

      <!-- 右侧：条目管理 -->
      <div class="dm-main">
        <template v-if="selectedDef">
          <div class="dm-panel-hd">
            <span class="dm-panel-title">{{ selectedDef.dict_name }} <span class="bl-muted" style="font-size:11px">({{ selectedDef.dict_code }})</span></span>
            <button class="bl-btn bl-btn-sm" @click="openItemForm()">
              <span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">新建条目</span>
            </button>
          </div>
          <div class="dm-tree-wrap">
            <table class="bl-table dm-item-table">
              <thead>
                <tr><th style="width:30px"></th><th>编码</th><th>值</th><th>上级编码</th><th>排序</th><th>状态</th><th style="width:80px">操作</th></tr>
              </thead>
              <tbody>
                <template v-if="flatRows.length">
                  <tr v-for="row in flatRows" :key="row._id">
                    <td class="t-center">
                      <span v-if="row._hasChildren" class="dm-expand" @click="toggleExpand(row)" v-html="BL.icon(row._expanded ? 'chevronDown' : 'chevronRight', 10)"></span>
                    </td>
                    <td class="bl-mono">
                      <span v-for="n in row._depth" :key="n" style="display:inline-block;width:16px"></span>
                      {{ row.item_code }}
                    </td>
                    <td>{{ row.item_value }}</td>
                    <td class="bl-mono bl-muted" style="font-size:11px">{{ parentCode(row) || '—' }}</td>
                    <td>{{ row.sort_no }}</td>
                    <td><span :class="['bl-tag', row.status === 1 ? 'bl-tag-primary' : 'bl-tag-muted']" style="font-size:11px"><StatusTag :status="row.status" /></span></td>
                    <td @click.stop>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="openItemForm(row)" v-html="BL.icon('edit', 11)"></button>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeItem(row)" v-html="BL.icon('trash', 11)"></button>
                    </td>
                  </tr>
                </template>
                <tr v-else><td colspan="7" class="bl-empty" style="padding:32px">暂无条目</td></tr>
              </tbody>
            </table>
          </div>
        </template>
        <div v-else class="bl-empty" style="flex:1;display:flex;align-items:center;justify-content:center;font-size:13px">请从左侧选择一个字典</div>
      </div>
    </div>

    <!-- 字典定义弹窗 -->
    <div v-if="defFormOpen" class="bl-modal-mask" @click.self="defFormOpen=false">
      <div class="bl-modal" style="width:480px">
        <div class="bl-modal-hd">{{ defForm.id ? '编辑字典' : '新建字典' }}</div>
        <div class="bl-modal-body" style="display:flex;flex-direction:column;gap:10px;padding:16px">
          <FieldRow label="字典编码" inline><input class="bl-input bl-mono" v-model="defForm.dict_code" placeholder="sys_xxx" /></FieldRow>
          <FieldRow label="字典名称" inline><input class="bl-input" v-model="defForm.dict_name" placeholder="字典中文名" /></FieldRow>
          <FieldRow label="说明" inline><textarea class="bl-textarea" v-model="defForm.rdfs_comment" placeholder="可选" rows="2" style="min-height:48px;font-size:12px"></textarea></FieldRow>
          <FieldRow label="排序" inline><input class="bl-input" type="number" v-model.number="defForm.sort_no" style="width:80px" /></FieldRow>
          <FieldRow label="状态" inline>
            <label class="ov-switch" style="display:inline-flex">
              <input type="checkbox" :checked="defForm.status===1" @change="defForm.status = $event.target.checked ? 1 : 0" />
              <span class="ov-switch-slider"></span>
            </label>
            <span :class="['ov-switch-tag', defForm.status===1 ? 'is-on' : 'is-off']">{{ defForm.status===1 ? '启用' : '禁用' }}</span>
          </FieldRow>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="defFormOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="submitDef">保存</button>
        </div>
      </div>
    </div>

    <!-- 条目弹窗 -->
    <div v-if="itemFormOpen" class="bl-modal-mask" @click.self="itemFormOpen=false">
      <div class="bl-modal" style="width:480px">
        <div class="bl-modal-hd">{{ itemForm.id ? '编辑条目' : '新建条目' }}</div>
        <div class="bl-modal-body" style="display:flex;flex-direction:column;gap:10px;padding:16px">
          <FieldRow label="父条目" inline>
            <select class="bl-input" v-model="itemForm.parent_id">
              <option value="">— 无（根节点）—</option>
              <option v-for="p in allFlatItems" :key="p.id" :value="p.id">{{ p.item_code }} - {{ p.item_value }}</option>
            </select>
          </FieldRow>
          <FieldRow label="编码" inline><input class="bl-input bl-mono" v-model="itemForm.item_code" placeholder="编码" /></FieldRow>
          <FieldRow label="值" inline><input class="bl-input" v-model="itemForm.item_value" placeholder="显示值" /></FieldRow>
          <FieldRow label="排序" inline><input class="bl-input" type="number" v-model.number="itemForm.sort_no" style="width:80px" /></FieldRow>
          <FieldRow label="状态" inline>
            <label class="ov-switch" style="display:inline-flex">
              <input type="checkbox" :checked="itemForm.status===1" @change="itemForm.status = $event.target.checked ? 1 : 0" />
              <span class="ov-switch-slider"></span>
            </label>
            <span :class="['ov-switch-tag', itemForm.status===1 ? 'is-on' : 'is-off']">{{ itemForm.status===1 ? '启用' : '禁用' }}</span>
          </FieldRow>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="itemFormOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="submitItem">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import StatusTag from '@/components/StatusTag.vue'
import { dictApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'

const defs = ref([])
const selectedDef = ref(null)
const treeItems = ref([])
const allFlatItems = ref([])   // 供父条目下拉使用
const expanded = ref(new Set())
const refreshing = ref(false)
const itemCountMap = ref({})
const defQ = ref('')

const filteredDefs = computed(() => {
  const q = defQ.value.trim().toLowerCase()
  if (!q) return defs.value
  return defs.value.filter(d =>
    (d.dict_code || '').toLowerCase().includes(q) ||
    (d.dict_name || '').toLowerCase().includes(q)
  )
})

function parentCode(row) {
  if (!row.parent_id) return ''
  const p = allFlatItems.value.find(i => i.id === row.parent_id)
  return p ? p.item_code : ''
}

onMounted(loadDefs)

async function loadDefs() {
  defs.value = await dictApi.list().catch(() => [])
  // 统计各字典条目数
  const map = {}
  for (const d of defs.value) {
    const items = await dictApi.listItems(d.id).catch(() => [])
    map[d.id] = items.length
  }
  itemCountMap.value = map
  // 如果当前选中的字典还在列表中，刷新其条目
  if (selectedDef.value) {
    const still = defs.value.find(d => d.id === selectedDef.value.id)
    if (still) await loadTree(still.id)
    else selectedDef.value = null
  }
}

async function selectDef(d) {
  selectedDef.value = d
  await loadTree(d.id)
}

async function loadTree(dictId) {
  treeItems.value = await dictApi.treeItems(dictId).catch(() => [])
  // 平铺所有条目（供父条目下拉）
  const flat = []
  function walk(ns) { for (const n of ns) { flat.push(n); if (n.children) walk(n.children) } }
  walk(treeItems.value)
  allFlatItems.value = flat
}

/* 展开/折叠 */
function toggleExpand(item) {
  const s = new Set(expanded.value)
  s.has(item._id || item.id) ? s.delete(item._id || item.id) : s.add(item._id || item.id)
  expanded.value = s
}

/* 树 → 平铺行（含缩进层级 + 展开状态） */
const flatRows = computed(() => {
  const result = []
  function walk(nodes, depth) {
    for (const n of nodes) {
      const hasChildren = n.children && n.children.length > 0
      result.push({ ...n, _id: n.id, _depth: depth, _hasChildren: hasChildren, _expanded: expanded.value.has(n.id) })
      if (hasChildren && expanded.value.has(n.id)) walk(n.children, depth + 1)
    }
  }
  walk(treeItems.value, 0)
  return result
})

/* —— 字典定义 CRUD —— */
const defForm = reactive({ dict_code: '', dict_name: '', rdfs_comment: '', sort_no: 0, status: 1 })
const defFormOpen = ref(false)

function openDefForm(d) {
  if (d) Object.assign(defForm, { id: d.id, dict_code: d.dict_code, dict_name: d.dict_name, rdfs_comment: d.rdfs_comment || '', sort_no: d.sort_no || 0, status: d.status ?? 1 })
  else Object.assign(defForm, { id: '', dict_code: '', dict_name: '', rdfs_comment: '', sort_no: 0, status: 1 })
  defFormOpen.value = true
}

async function submitDef() {
  if (!defForm.dict_code || !defForm.dict_name) { BL.warning('编码和名称必填'); return }
  try {
    if (defForm.id) await dictApi.update(defForm.id, { ...defForm })
    else await dictApi.create({ ...defForm })
    BL.success('已保存')
    defFormOpen.value = false
    await loadDefs()
  } catch (e) { BL.error(e?.msg || '保存失败') }
}

async function removeDef(d) {
  const ok = await BL.confirm({ title: '删除字典', content: `确定删除「${d.dict_name}」？条目将一并删除。`, danger: true, okText: '删除' })
  if (!ok) return
  try {
    await dictApi.remove(d.id)
    if (selectedDef.value?.id === d.id) selectedDef.value = null
    BL.success('已删除')
    await loadDefs()
  } catch (e) { BL.error(e?.msg || '删除失败') }
}

/* —— 条目 CRUD —— */
const itemForm = reactive({ parent_id: '', item_code: '', item_value: '', sort_no: 0, status: 1 })
const itemFormOpen = ref(false)

function openItemForm(item) {
  if (item) Object.assign(itemForm, { id: item.id, parent_id: item.parent_id || '', item_code: item.item_code, item_value: item.item_value, sort_no: item.sort_no || 0, status: item.status ?? 1 })
  else Object.assign(itemForm, { id: '', parent_id: '', item_code: '', item_value: '', sort_no: 0, status: 1 })
  itemFormOpen.value = true
}

async function submitItem() {
  if (!itemForm.item_code || !itemForm.item_value) { BL.warning('编码和值必填'); return }
  const body = {
    dictId: selectedDef.value.id,
    parent_id: itemForm.parent_id || null,
    item_code: itemForm.item_code,
    item_value: itemForm.item_value,
    sort_no: itemForm.sort_no || 0,
    status: itemForm.status ?? 1
  }
  try {
    if (itemForm.id) await dictApi.updateItem(itemForm.id, body)
    else await dictApi.addItem(body)
    BL.success('已保存')
    itemFormOpen.value = false
    await loadTree(selectedDef.value.id)
    itemCountMap.value[selectedDef.value.id] = (itemCountMap.value[selectedDef.value.id] || 0) + (itemForm.id ? 0 : 1)
  } catch (e) { BL.error(e?.msg || '保存失败') }
}

async function removeItem(item) {
  const ok = await BL.confirm({ title: '删除条目', content: `确定删除「${item.item_code}」？子条目将一并删除。`, danger: true, okText: '删除' })
  if (!ok) return
  try {
    await dictApi.removeItem(item.id)
    BL.success('已删除')
    await loadTree(selectedDef.value.id)
  } catch (e) { BL.error(e?.msg || '删除失败') }
}

/* —— 刷新缓存 —— */
async function refreshCache() {
  refreshing.value = true
  try {
    await dictApi.refreshCache()
    BL.success('所有字典缓存已刷新')
    if (selectedDef.value) await loadTree(selectedDef.value.id)
  } catch (e) { BL.warning(e?.msg || '刷新失败') }
  refreshing.value = false
}

/** 重载单个字典的缓存 */
async function reloadDict(d) {
  try {
    await dictApi.refreshCacheByCode(d.dict_code)
    BL.success(`「${d.dict_name}」缓存已重载`)
  } catch (e) { BL.warning(e?.msg || '重载失败') }
}
</script>

<style scoped>
.dm-page { display:flex;flex-direction:column;height:100%; }
.dm-topbar { display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-bottom:1px solid var(--bl-divider);flex-shrink:0;background:var(--bl-bg-1); }
.dm-title { font-size:16px;font-weight:600; }
.dm-subtitle { font-size:12px;margin-left:8px; }
.dm-actions { display:flex;gap:8px;align-items:center; }
.dm-body { flex:1;display:flex;min-height:0;overflow:hidden;gap:12px;padding:12px;background:var(--bl-bg-2); }
.dm-side { width:380px;flex-shrink:0;display:flex;flex-direction:column;gap:8px; }
.dm-side-search { position:relative;display:flex;align-items:center;gap:6px;padding:6px 10px;background:var(--bl-bg-1);border:1px solid var(--bl-border);border-radius:6px;flex-shrink:0; }
.dm-search-input { border:none!important;height:28px;padding:0;font-size:12px;flex:1;min-width:0;background:transparent; }
.dm-side-search:focus-within { border-color:var(--bl-primary); }
.dm-card-list { flex:1;overflow-y:auto;display:flex;flex-direction:column;gap:6px; }
.dm-card { background:var(--bl-bg-1);border:1px solid var(--bl-border);border-radius:8px;padding:10px 12px;cursor:pointer;transition:border-color .12s,box-shadow .12s;display:flex;flex-direction:column;gap:4px; }
.dm-card:hover { border-color:var(--bl-primary);box-shadow:var(--bl-shadow-1); }
.dm-card.is-on { border-color:var(--bl-primary);background:var(--bl-primary-soft); }
.dm-card-top { display:flex;align-items:center;justify-content:space-between;gap:6px; }
.dm-card-code { font-size:11px;color:var(--bl-text-2); }
.dm-card-name { font-size:13px;font-weight:500; }
.dm-card-desc { font-size:11px;color:var(--bl-text-3);line-height:1.4; }
.dm-card-ft { display:flex;align-items:center;justify-content:space-between;margin-top:2px; }
.dm-card-actions { display:flex;gap:2px;opacity:0;transition:opacity .12s; }
.dm-card:hover .dm-card-actions { opacity:1; }
.dm-main { flex:1;display:flex;flex-direction:column;min-width:0;overflow:hidden;background:var(--bl-bg-1);border:1px solid var(--bl-border);border-radius:8px; }
.dm-panel-hd { display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-bottom:1px solid var(--bl-divider);flex-shrink:0; }
.dm-panel-title { font-size:14px;font-weight:600; }
.dm-tree-wrap { flex:1;overflow-y:auto;padding:4px 0; }
.dm-item-table { width:100%;font-size:12px; }
.dm-item-table th { background:var(--bl-thead-bg);padding:5px 8px;font-weight:600;position:sticky;top:0;z-index:1; }
.dm-item-table td { padding:4px 8px;border-bottom:1px solid var(--bl-divider); }
.dm-item-table tr:hover td { background:var(--bl-bg-hover); }
.dm-item-table tr.is-editing td { background:var(--bl-primary-soft); }
.dm-expand { cursor:pointer;padding:2px;display:inline-flex;align-items:center;color:var(--bl-text-3); }
.dm-expand:hover { color:var(--bl-primary); }
.t-center { text-align:center; }
</style>
