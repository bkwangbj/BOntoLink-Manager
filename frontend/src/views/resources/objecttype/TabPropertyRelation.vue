<template>
  <div class="tab-pr">
    <div class="pr-hd">
      <div class="pr-title">
        {{ title }}
        <span class="bl-help" :title="tipText" v-html="BL.icon('info', 12)"></span>
      </div>
      <div class="bl-row" style="gap:8px">
        <div class="pr-search">
          <span class="ot-ic" v-html="BL.icon('search', 13)"></span>
          <input class="bl-input" v-model="q" placeholder="搜索：属性 / 对象" />
        </div>
        <button v-if="checked.size" class="bl-btn bl-btn-sm bl-btn-danger" @click="batchRemove">
          <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除 ({{ checked.size }})</span>
        </button>
        <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onAdd">
          <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">新增</span>
        </button>
      </div>
    </div>

    <div class="pr-table-wrap">
      <table class="bl-table pr-table">
        <thead>
          <tr>
            <th rowspan="2" class="t-center" style="width:34px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
            <th colspan="3" class="t-center bg-l">属性 L</th>
            <th rowspan="2" class="t-center" style="width:38px"></th>
            <th colspan="3" class="t-center bg-r">属性 R</th>
            <th rowspan="2" style="width:90px">状态</th>
            <th rowspan="2">注释</th>
            <th rowspan="2" style="width:48px"></th>
          </tr>
          <tr>
            <th class="bg-l">对象</th>
            <th class="bg-l">属性</th>
            <th class="bg-l">类型</th>
            <th class="bg-r">对象</th>
            <th class="bg-r">属性</th>
            <th class="bg-r">类型</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in filtered" :key="r.id">
            <td @click.stop><input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" /></td>
            <td class="bg-l">{{ r.class1_name || r.class1_api || '—' }}</td>
            <td class="bg-l"><span class="bl-mono">{{ r.api1 }}</span><div class="bl-muted" style="font-size:11px">{{ r.name1 }}</div></td>
            <td class="bg-l"><span class="bl-tag">{{ r.type1 || 'data' }}</span></td>
            <td class="t-center bl-muted" style="font-weight:600;font-size:16px">{{ symbol }}</td>
            <td class="bg-r">{{ r.class2_name || r.class2_api || '—' }}</td>
            <td class="bg-r"><span class="bl-mono">{{ r.api2 }}</span><div class="bl-muted" style="font-size:11px">{{ r.name2 }}</div></td>
            <td class="bg-r"><span class="bl-tag">{{ r.type2 || 'data' }}</span></td>
            <td>
              <select class="bl-input bl-input-xs" :value="r.status" @change="(e) => updateStatus(r, +e.target.value)">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </td>
            <td class="bl-truncate" style="max-width:200px" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</td>
            <td @click.stop>
              <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeOne(r)" v-html="BL.icon('trash', 12)"></button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!filtered.length" class="bl-empty" style="padding:32px">{{ emptyText }}</div>
    </div>

    <!-- 新增弹窗（选对象类 → 选属性） -->
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
              <option v-for="p in leftProps" :key="p.id" :value="p.id">{{ p.display_name || p.api_name }} ({{ p.prop_type }})</option>
            </select>
          </FieldRow>
          <FieldRow label="右 · 对象 *" inline>
            <div class="bl-row" style="gap:6px;flex:1">
              <input class="bl-input" readonly :value="rightClassLabel" :placeholder="'点击右侧按钮选择对象'" />
              <button class="bl-btn bl-btn-sm" @click="pickerOpen=true">选择</button>
            </div>
          </FieldRow>
          <FieldRow label="右 · 属性 *" inline>
            <select class="bl-input" v-model="draft.prop_id2" :disabled="!draft.class_id2">
              <option value="">— 请选择 —</option>
              <option v-for="p in rightProps" :key="p.id" :value="p.id">{{ p.display_name || p.api_name }} ({{ p.prop_type }})</option>
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
import { ref, computed, watch, reactive } from 'vue'
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
const symbol = computed(() => props.type === 'equivalent' ? '≡' : '≠')
const tipText = computed(() => props.type === 'equivalent'
  ? '只能添加同类型属性（数据 ↔ 数据，对象 ↔ 对象），语义完全相同可互相替换。'
  : '只能添加同类型属性，两个属性不能同时为真。')
const emptyText = computed(() => props.type === 'equivalent' ? '尚未维护等价属性关系' : '尚未维护互斥属性关系')

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
  return rows.value.filter(r => [r.api1, r.api2, r.name1, r.name2, r.class1_api, r.class2_api, r.rdfs_comment]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})
const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) { const s = new Set(checked.value); s.has(id) ? s.delete(id) : s.add(id); checked.value = s }
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) filtered.value.forEach(r => s.delete(r.id))
  else filtered.value.forEach(r => s.add(r.id))
  checked.value = s
}

async function removeOne(r) {
  const ok = await BL.confirm({ title: '删除', content: '确定删除该关系？', danger: true, okText: '删除' })
  if (!ok) return
  const api = props.type === 'equivalent' ? classMetaApi.removePropEquiv : classMetaApi.removePropDisjoint
  await api(r.id)
  await load()
}
async function batchRemove() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中 ${ids.length} 条？`, danger: true, okText: '删除' })
  if (!ok) return
  const api = props.type === 'equivalent' ? classMetaApi.removePropEquiv : classMetaApi.removePropDisjoint
  for (const id of ids) await api(id).catch(() => {})
  await load()
}
async function updateStatus(r, status) {
  const api = props.type === 'equivalent' ? classMetaApi.updatePropEquiv : classMetaApi.updatePropDisjoint
  await api(r.id, { ...r, status }).catch(() => {})
  await load()
}

/* 新增弹窗 */
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
}

const canSubmit = computed(() => draft.prop_id1 && draft.class_id2 && draft.prop_id2)
async function submit() {
  const lp = leftProps.value.find(p => p.id === draft.prop_id1)
  const rp = rightProps.value.find(p => p.id === draft.prop_id2)
  if (lp && rp && lp.prop_type !== rp.prop_type) {
    BL.warning('两侧属性类型必须一致（data ↔ data 或 object ↔ object）'); return
  }
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
.tab-pr { display: flex; flex-direction: column; gap: 10px; }
.pr-hd { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.pr-title { font-size: 14px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; }
.bl-help { color: var(--bl-text-3); cursor: help; display: inline-flex; }
.pr-search { position: relative; width: 220px; }
.pr-search .bl-input { padding-left: 26px; height: 30px; }
.pr-search .ot-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.pr-table-wrap { overflow: auto; border: 1px solid var(--bl-border); border-radius: 6px; }
.pr-table { width: 100%; min-width: 1100px; font-size: 12px; }
.pr-table th { padding: 6px 8px; background: #fafafa; }
.pr-table td { padding: 6px 8px; }
.t-center { text-align: center; }
.bg-l { background: #e8f4ff; }
.bg-r { background: #f0fff4; }
.pr-table tbody tr:hover td { background: #f5f7fa; }
.pr-table tbody tr:hover td.bg-l { background: #d9ecff; }
.pr-table tbody tr:hover td.bg-r { background: #e0fbe7; }
.bl-input.bl-input-xs { height: 26px; padding: 0 6px; font-size: 12px; }

/* 「新增等价 / 互斥属性」弹窗里 label 比全局长（左·对象 *、右·属性 *），单独加宽避免换行 */
.bl-modal :deep(.fr.fr-inline .fr-label) { width: 84px; }
</style>
