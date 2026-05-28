<template>
  <div class="tab-pr">
    <!-- 顶部工具栏: 模块名 + ? 提示 + 检索 + 新增 + 删除 (spec §二.2) -->
    <div class="pr-hd">
      <div class="pr-title">
        {{ title }}
        <span class="bl-help" :title="tipText" v-html="BL.icon('help', 14)"></span>
      </div>
      <div class="bl-row" style="gap:8px">
        <div class="pr-search">
          <span class="ot-ic" v-html="BL.icon('search', 13)"></span>
          <input class="bl-input" v-model="q" placeholder="搜索: 属性 / 对象 (中文/编码/拼音)" />
        </div>
        <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onAdd">
          <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">新增</span>
        </button>
        <button class="bl-btn bl-btn-sm pr-del-btn" :disabled="!checked.size" @click="batchRemove">
          <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除 ({{ checked.size }})</span>
        </button>
      </div>
    </div>

    <!-- 主体表格: 单层表头, 10 列, L 蓝 / R 绿 色块,左侧冻结 -->
    <div class="pr-table-wrap">
      <table class="bl-table pr-table">
        <colgroup>
          <col style="width:50px" />     <!-- ☐ -->
          <col style="width:200px" />    <!-- L 组合信息 -->
          <col style="width:160px" />    <!-- L 对象代码 -->
          <col style="width:160px" />    <!-- L 属性代码 -->
          <col style="width:50px" />     <!-- 关系 -->
          <col style="width:160px" />    <!-- R 对象代码 -->
          <col style="width:160px" />    <!-- R 属性代码 -->
          <col style="width:200px" />    <!-- R 组合信息 -->
          <col style="width:100px" />    <!-- 状态 -->
          <col />                        <!-- 注释 自适应 -->
          <col style="width:60px" />     <!-- 操作 -->
        </colgroup>
        <thead>
          <tr>
            <th class="t-center pr-stk-l1"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
            <th class="t-left pr-stk-l2 bg-l">属性 L 组合信息</th>
            <th class="t-center pr-stk-l3 bg-l">对象代码</th>
            <th class="t-center pr-stk-l4 bg-l">属性代码</th>
            <th class="t-center pr-stk-l5">{{ symbol }}</th>
            <th class="t-center bg-r">对象代码</th>
            <th class="t-center bg-r">属性代码</th>
            <th class="t-left bg-r">属性 R 组合信息</th>
            <th class="t-center">状态</th>
            <th class="t-left">注释</th>
            <th class="t-center"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in paged" :key="r.id" class="pr-row">
            <td class="t-center pr-stk-l1" @click.stop>
              <input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" />
            </td>
            <!-- L 组合信息: 3-line stacked -->
            <td class="t-left pr-stk-l2 bg-l">
              <div class="pr-combo">
                <div class="pr-combo-l1 bl-truncate" :title="r.class1_name || r.class1_api">{{ r.class1_name || r.class1_api || '—' }}</div>
                <div class="pr-combo-l2 bl-truncate" :title="r.name1">{{ r.name1 || '—' }}</div>
                <div class="pr-combo-l3"><span :class="['pr-type-tag', typeClass(r.type1)]">{{ typeLabel(r.type1) }}</span></div>
              </div>
            </td>
            <!-- L 对象代码 -->
            <td class="t-center pr-stk-l3 bg-l">
              <span class="bl-mono bl-truncate" :title="r.class1_api">{{ r.class1_api || '—' }}</span>
            </td>
            <!-- L 属性代码 -->
            <td class="t-center pr-stk-l4 bg-l">
              <span class="bl-mono bl-truncate" :title="r.api1">{{ r.api1 || '—' }}</span>
            </td>
            <!-- 关系符号 -->
            <td class="t-center pr-stk-l5 pr-sym">{{ symbol }}</td>
            <!-- R 对象代码 -->
            <td class="t-center bg-r">
              <span class="bl-mono bl-truncate" :title="r.class2_api">{{ r.class2_api || '—' }}</span>
            </td>
            <!-- R 属性代码 -->
            <td class="t-center bg-r">
              <span class="bl-mono bl-truncate" :title="r.api2">{{ r.api2 || '—' }}</span>
            </td>
            <!-- R 组合信息: 3-line stacked -->
            <td class="t-left bg-r">
              <div class="pr-combo">
                <div class="pr-combo-l1 bl-truncate" :title="r.class2_name || r.class2_api">{{ r.class2_name || r.class2_api || '—' }}</div>
                <div class="pr-combo-l2 bl-truncate" :title="r.name2">{{ r.name2 || '—' }}</div>
                <div class="pr-combo-l3"><span :class="['pr-type-tag', typeClass(r.type2)]">{{ typeLabel(r.type2) }}</span></div>
              </div>
            </td>
            <!-- 状态: 下拉切换 -->
            <td class="t-center" @click.stop>
              <div class="pr-status" @click="toggleStatusMenu(r.id)">
                <span :class="['pr-status-dot', r.status === 1 ? 'is-on' : 'is-off']"></span>
                <span :class="['pr-status-text', r.status === 1 ? 'is-on' : 'is-off']">{{ r.status === 1 ? '激活' : '禁用' }}</span>
                <span class="pr-status-chev" v-html="BL.icon('chevronDown', 10)"></span>
                <div v-if="statusMenuId === r.id" class="pr-status-menu" @click.stop>
                  <div :class="['pr-status-item', r.status === 1 && 'is-sel']" @click="updateStatus(r, 1)">
                    <span class="pr-status-dot is-on"></span><span>激活</span>
                  </div>
                  <div :class="['pr-status-item', r.status === 0 && 'is-sel']" @click="updateStatus(r, 0)">
                    <span class="pr-status-dot is-off"></span><span>禁用</span>
                  </div>
                </div>
              </div>
            </td>
            <!-- 注释 -->
            <td class="t-left">
              <span class="bl-muted bl-truncate" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</span>
            </td>
            <!-- 操作 -->
            <td class="t-center" @click.stop>
              <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeOne(r)" v-html="BL.icon('trash', 12)"></button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!filtered.length" class="bl-empty" style="padding:32px">{{ emptyText }}</div>
    </div>

    <!-- 分页栏 -->
    <div v-if="filtered.length" class="pr-pager">
      <div class="bl-muted" style="font-size:12px">共 <b style="color:var(--bl-text-1)">{{ filtered.length }}</b> 条</div>
      <div class="bl-row" style="gap:6px;align-items:center">
        <span class="bl-muted" style="font-size:12px;margin-right:4px">每页</span>
        <select class="bl-input pr-page-size" v-model.number="pageSize">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
          <option :value="100">100</option>
        </select>
        <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page<=1" @click="page--">‹</button>
        <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
        <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page>=totalPages" @click="page++">›</button>
      </div>
    </div>

    <!-- 新增弹窗: 选左属性 + 右对象 + 右属性 + 注释 -->
    <div v-if="addOpen" class="bl-modal-mask" @click.self="addOpen=false">
      <div class="bl-modal" style="width:520px">
        <div class="bl-modal-hd">新增{{ title }}</div>
        <div class="bl-modal-body bl-col" style="gap:10px">
          <div class="bl-muted" style="font-size:12px">{{ tipText }}</div>
          <FieldRow label="左 · 对象" inline>
            <input class="bl-input" readonly :value="leftClassName" />
          </FieldRow>
          <FieldRow label="左 · 属性 *" inline>
            <select class="bl-input" v-model="draft.prop_id1">
              <option value="">— 请选择 —</option>
              <option v-for="p in leftProps" :key="p.id" :value="p.id">{{ p.display_name || p.api_name }} ({{ propTypeLabel(p.prop_type) }})</option>
            </select>
          </FieldRow>
          <FieldRow label="右 · 对象 *" inline>
            <div class="bl-row" style="gap:6px;flex:1">
              <input class="bl-input" readonly :value="rightClassLabel" placeholder="点击右侧按钮选择对象" />
              <button class="bl-btn bl-btn-sm" @click="pickerOpen=true">
                <span v-html="BL.icon('search', 11)"></span><span style="margin-left:4px">选择</span>
              </button>
            </div>
          </FieldRow>
          <FieldRow label="右 · 属性 *" inline>
            <select class="bl-input" v-model="draft.prop_id2" :disabled="!draft.class_id2">
              <option value="">— 请选择 —</option>
              <option v-for="p in compatibleRightProps" :key="p.id" :value="p.id">{{ p.display_name || p.api_name }} ({{ propTypeLabel(p.prop_type) }})</option>
            </select>
          </FieldRow>
          <FieldRow label="注释"><textarea class="bl-textarea" rows="2" v-model="draft.rdfs_comment"></textarea></FieldRow>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="addOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!canSubmit" @click="submit">添加</button>
        </div>
      </div>
    </div>

    <ObjectTypePickerModal
      v-model:open="pickerOpen"
      :multi="false"
      :exclude-ids="[]"
      :subtitle="`选择${title}另一侧的对象类`"
      @confirm="onPickRight"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, reactive, onMounted, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'
import { classMetaApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'
import ObjectTypePickerModal from '@/components/ObjectTypePickerModal.vue'

const props = defineProps({
  classId: { type: String, default: '' },
  classLabel: { type: String, default: '' },
  type: { type: String, default: 'equivalent' } // equivalent | disjoint
})

const title = computed(() => props.type === 'equivalent' ? '等价属性' : '互斥属性')
const symbol = computed(() => props.type === 'equivalent' ? '=' : '≠')
const tipText = computed(() => props.type === 'equivalent'
  ? '只能添加同类型属性 (数据 ↔ 数据,对象 ↔ 对象),语义完全相同可互相替换。禁止与自身建立关系。'
  : '只能添加同类型属性,两个属性不能同时为真。禁止与自身建立关系。')
const emptyText = computed(() => props.type === 'equivalent' ? '尚未维护等价属性关系' : '尚未维护互斥属性关系')

function propTypeLabel(t) {
  return ({ data: '数据', object: '对象', annotation: '注释' })[t] || (t || 'data')
}
function typeLabel(t) {
  return propTypeLabel(t)
}
function typeClass(t) {
  return ({ data: 'is-data', object: 'is-object', annotation: 'is-annotation' })[t] || 'is-data'
}

const rows = ref([])
const checked = ref(new Set())
const q = ref('')

async function load() {
  if (!props.classId) { rows.value = []; return }
  const api = props.type === 'equivalent' ? classMetaApi.listPropEquiv : classMetaApi.listPropDisjoint
  rows.value = await api(props.classId).catch(() => []) || []
  checked.value = new Set()
}
watch(() => [props.classId, props.type], load, { immediate: true })

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return rows.value
  return rows.value.filter(r => [r.api1, r.api2, r.name1, r.name2, r.class1_api, r.class2_api, r.class1_name, r.class2_name, r.rdfs_comment]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})

/* —— 分页 —— */
const page = ref(1)
const pageSize = ref(20)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
watch([q, () => props.classId, () => props.type], () => { page.value = 1 })
watch(totalPages, (n) => { if (page.value > n) page.value = n })

const allChecked = computed(() => paged.value.length > 0 && paged.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) { const s = new Set(checked.value); s.has(id) ? s.delete(id) : s.add(id); checked.value = s }
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) paged.value.forEach(r => s.delete(r.id))
  else paged.value.forEach(r => s.add(r.id))
  checked.value = s
}

async function removeOne(r) {
  const ok = await BL.confirm({ title: '删除', content: '确定删除该关系？此操作不可恢复', danger: true, okText: '删除' })
  if (!ok) return
  const api = props.type === 'equivalent' ? classMetaApi.removePropEquiv : classMetaApi.removePropDisjoint
  await api(r.id)
  await load()
}
async function batchRemove() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中 ${ids.length} 条关系？此操作不可恢复`, danger: true, okText: '删除' })
  if (!ok) return
  const api = props.type === 'equivalent' ? classMetaApi.removePropEquiv : classMetaApi.removePropDisjoint
  for (const id of ids) await api(id).catch(() => {})
  await load()
}

/* —— 状态下拉菜单 (同时仅允许一个展开) —— */
const statusMenuId = ref(null)
function toggleStatusMenu(id) {
  statusMenuId.value = statusMenuId.value === id ? null : id
}
function onDocClick(ev) {
  // 点击页面其他区域自动关闭下拉
  if (!ev.target.closest?.('.pr-status')) statusMenuId.value = null
}
onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))

async function updateStatus(r, status) {
  statusMenuId.value = null
  if (r.status === status) return
  const api = props.type === 'equivalent' ? classMetaApi.updatePropEquiv : classMetaApi.updatePropDisjoint
  await api(r.id, { ...r, status }).catch(() => {})
  await load()
}

/* —— 新增弹窗 —— */
const addOpen = ref(false)
const pickerOpen = ref(false)
const draft = reactive({ prop_id1: '', class_id2: '', prop_id2: '', rdfs_comment: '' })
const leftClassName = computed(() => props.classLabel || '当前对象')
const rightClassLabel = ref('')

const leftProps = ref([])
const rightProps = ref([])

async function onAdd() {
  draft.prop_id1 = ''; draft.class_id2 = ''; draft.prop_id2 = ''; draft.rdfs_comment = ''
  rightClassLabel.value = ''
  if (!leftProps.value.length) leftProps.value = await classMetaApi.listProps(props.classId).catch(() => []) || []
  addOpen.value = true
}

async function onPickRight({ ids, rows: picked }) {
  if (!ids?.length) return
  draft.class_id2 = ids[0]
  rightClassLabel.value = (picked && picked[0]) ? (picked[0].rdfs_label || picked[0].display_name || picked[0].api_name) : ''
  rightProps.value = await classMetaApi.listProps(ids[0]).catch(() => []) || []
  draft.prop_id2 = ''
}

/* 仅显示与左侧属性同类型的右侧属性 (spec §一.2 类型一致校验) */
const leftSelected = computed(() => leftProps.value.find(p => p.id === draft.prop_id1))
const compatibleRightProps = computed(() => {
  if (!leftSelected.value) return rightProps.value
  const t = leftSelected.value.prop_type
  return rightProps.value.filter(p => p.prop_type === t)
})

const canSubmit = computed(() => draft.prop_id1 && draft.class_id2 && draft.prop_id2)
async function submit() {
  const lp = leftProps.value.find(p => p.id === draft.prop_id1)
  const rp = rightProps.value.find(p => p.id === draft.prop_id2)
  if (lp && rp && lp.prop_type !== rp.prop_type) {
    BL.warning('两侧属性类型必须一致 (data ↔ data 或 object ↔ object)'); return
  }
  // 禁止自关联 (spec §一.2)
  if (props.classId === draft.class_id2 && draft.prop_id1 === draft.prop_id2) {
    BL.warning('禁止属性与自身建立关系'); return
  }
  // 重复校验
  const dup = rows.value.some(r =>
    (r.prop_id1 === draft.prop_id1 && r.prop_id2 === draft.prop_id2) ||
    (r.prop_id1 === draft.prop_id2 && r.prop_id2 === draft.prop_id1)
  )
  if (dup) { BL.warning('该关系已存在'); return }
  const body = {
    class_id1: props.classId, prop_id1: draft.prop_id1,
    class_id2: draft.class_id2, prop_id2: draft.prop_id2,
    status: 1, rdfs_comment: draft.rdfs_comment
  }
  const api = props.type === 'equivalent' ? classMetaApi.addPropEquiv : classMetaApi.addPropDisjoint
  try {
    await api(body); BL.success('已添加'); addOpen.value = false; await load()
  } catch (e) { BL.error(e?.msg || '添加失败') }
}
</script>

<style scoped>
.tab-pr {
  display: flex; flex-direction: column; gap: 10px;
  height: 100%; min-height: 0;
}

/* —— 顶部 —— */
.pr-hd {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;

  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.pr-title { font-size: 14px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; color: #333; }
.bl-help { color: #999; cursor: help; display: inline-flex; }
.bl-help:hover { color: var(--bl-primary); }
.pr-search { position: relative; width: 240px; }
.pr-search .bl-input { padding-left: 28px; height: 32px; }
.pr-search .ot-ic { position: absolute; left: 9px; top: 50%; transform: translateY(-50%); color: #999; }
.pr-del-btn {
  background: #fff; border: 1px solid var(--bl-border); color: var(--bl-text-2);
}
.pr-del-btn:not(:disabled):hover { border-color: #f56c6c; color: #f56c6c; }
.pr-del-btn:disabled { opacity: .4; cursor: not-allowed; }

/* —— 表格主体: 占满 toolbar / pager 之间剩余空间 —— */
.pr-table-wrap {
  flex: 1; min-height: 0;
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: auto;
  position: relative;
}

/* —— 分页栏 —— */
.pr-pager {
  flex-shrink: 0;
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px;
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.pr-page-size { width: 64px; height: 26px; }
.pr-table {
  width: 100%; min-width: 1300px;
  border-collapse: separate; border-spacing: 0;
  table-layout: fixed;
  font-size: 12px;
}
/* 表头 */
.pr-table thead th {
  position: sticky; top: 0; z-index: 3;
  background: #f5f7fa;
  font-weight: 600; padding: 0 10px;
  font-size: 12px; height: 38px;
  color: #333;
  white-space: nowrap;
  border-bottom: 1px solid #eee;
}
.pr-table thead th.t-left { text-align: left; }
.pr-table thead th.t-center { text-align: center; }
/* L 蓝 / R 绿色块 */
.pr-table thead th.bg-l { background: #e8f4ff; }
.pr-table thead th.bg-r { background: #f0fff4; }

/* 表体 */
.pr-table tbody tr.pr-row { background: #fff; }
.pr-table tbody tr.pr-row:hover { background: #f5f7fa; }
.pr-table td {
  padding: 8px 10px; vertical-align: middle;
  height: 76px;  /* 容纳 3 行组合信息 */
  border-bottom: 1px solid #eee;
  background: inherit;
}
.pr-table td.t-left { text-align: left; }
.pr-table td.t-center { text-align: center; }
.pr-table td.bg-l { background: #f3f9ff; }
.pr-table td.bg-r { background: #f5fff7; }
.pr-table tbody tr:hover td.bg-l { background: #e0eeff; }
.pr-table tbody tr:hover td.bg-r { background: #e6fbe9; }
.pr-table td .bl-truncate { display: inline-block; max-width: 100%; vertical-align: middle; }

/* 关系符号列 */
.pr-sym {
  font-size: 18px; font-weight: 700;
  color: var(--bl-primary);
  background: #fafafa;
}

/* 组合信息 cell: 3 行垂直层级 */
.pr-combo { display: flex; flex-direction: column; gap: 2px; line-height: 1.2; }
.pr-combo-l1 { color: #333; font-weight: 500; font-size: 13px; }
.pr-combo-l2 { color: #666; font-size: 12px; }
.pr-combo-l3 { font-size: 11px; }

.pr-type-tag {
  display: inline-block; padding: 1px 6px;
  font-size: 10.5px; border-radius: 9px; font-weight: 500;
}
.pr-type-tag.is-data { background: #e8f3ff; color: #1677ff; }
.pr-type-tag.is-object { background: #fff5e6; color: #FF7D00; }
.pr-type-tag.is-annotation { background: #e8fff4; color: #00B42A; }

/* —— 状态: 下拉切换 —— */
.pr-status {
  position: relative;
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 8px; border-radius: 4px; cursor: pointer;
  border: 1px solid transparent;
}
.pr-status:hover { background: #f5f7fa; border-color: #eee; }
.pr-status-dot {
  width: 6px; height: 6px; border-radius: 50%;
  display: inline-block; flex-shrink: 0;
}
.pr-status-dot.is-on  { background: #52c41a; }
.pr-status-dot.is-off { background: #ff4d4f; }
.pr-status-text { font-size: 12px; }
.pr-status-text.is-on  { color: #52c41a; }
.pr-status-text.is-off { color: #ff4d4f; }
.pr-status-chev { color: #999; display: inline-flex; }
.pr-status-menu {
  position: absolute; top: 100%; left: 50%; transform: translateX(-50%);
  margin-top: 4px; min-width: 120px;
  background: #fff; border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  z-index: 20;
  padding: 4px;
}
.pr-status-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; border-radius: 4px; cursor: pointer;
  font-size: 12px;
}
.pr-status-item:hover { background: #f5f7fa; }
.pr-status-item.is-sel { background: #e8f3ff; color: #409eff; font-weight: 500; }

/* —— 冻结列: ☐ + L 全部 + 关系 列 (spec §二.6 锁定列) —— */
.pr-stk-l1, .pr-stk-l2, .pr-stk-l3, .pr-stk-l4, .pr-stk-l5 {
  position: sticky; z-index: 2;
}
.pr-stk-l1 { left: 0; }
.pr-stk-l2 { left: 50px; }
.pr-stk-l3 { left: 250px; }
.pr-stk-l4 { left: 410px; }
.pr-stk-l5 { left: 570px; box-shadow: 2px 0 4px -2px rgba(0,0,0,0.08); }
/* 表头粘性列背景 */
.pr-table thead th.pr-stk-l1 { background: #f5f7fa; z-index: 4; }
.pr-table thead th.pr-stk-l2 { background: #e8f4ff; z-index: 4; }
.pr-table thead th.pr-stk-l3 { background: #e8f4ff; z-index: 4; }
.pr-table thead th.pr-stk-l4 { background: #e8f4ff; z-index: 4; }
.pr-table thead th.pr-stk-l5 { background: #fafafa; z-index: 4; box-shadow: 2px 0 4px -2px rgba(0,0,0,0.08); }
/* 表体粘性列默认背景 */
.pr-table tbody td.pr-stk-l1 { background: #fff; }
.pr-table tbody td.pr-stk-l2.bg-l { background: #f3f9ff; }
.pr-table tbody td.pr-stk-l3.bg-l { background: #f3f9ff; }
.pr-table tbody td.pr-stk-l4.bg-l { background: #f3f9ff; }
.pr-table tbody td.pr-stk-l5 { background: #fafafa; }
.pr-table tbody tr.pr-row:hover td.pr-stk-l1 { background: #f5f7fa; }
.pr-table tbody tr.pr-row:hover td.pr-stk-l2.bg-l,
.pr-table tbody tr.pr-row:hover td.pr-stk-l3.bg-l,
.pr-table tbody tr.pr-row:hover td.pr-stk-l4.bg-l { background: #e0eeff; }
.pr-table tbody tr.pr-row:hover td.pr-stk-l5 { background: #f0f1f4; }

/* —— 新增弹窗 label 宽度 —— */
.bl-modal :deep(.fr.fr-inline .fr-label) { width: 90px; }
</style>
