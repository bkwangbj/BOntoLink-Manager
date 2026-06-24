<template>
  <div class="tab-cgroup">
    <div class="cg-toolbar">
      <div class="cg-title">{{ title }}<span class="bl-muted" style="margin-left:6px;font-size:12px">共 {{ rows.length }} 条</span></div>
      <div class="bl-row" style="gap:8px">
        <div class="cg-search">
          <span class="ot-ic" v-html="BL.icon('search', 13)"></span>
          <input class="bl-input" v-model="q" placeholder="搜索：名称 / 编码 / 拼音" />
        </div>
        <button v-if="checked.size" class="bl-btn bl-btn-sm bl-btn-danger" @click="batchRemove">
          <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除 ({{ checked.size }})</span>
        </button>
        <button v-if="checked.size" class="bl-btn bl-btn-sm" @click="batchToggle(1)">启用</button>
        <button v-if="checked.size" class="bl-btn bl-btn-sm" @click="batchToggle(0)">停用</button>
        <button class="bl-btn bl-btn-sm bl-btn-primary" @click="openPicker">
          <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">新增</span>
        </button>
      </div>
    </div>
    <table class="bl-table cg-table">
      <thead>
        <tr>
          <th style="width:34px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
          <th>对象类型</th>
          <th>注释</th>
          <th>参考资料</th>
          <th>定义来源</th>
          <th>状态</th>
          <th style="width:60px"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in filtered" :key="r.id">
          <td @click.stop><input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" /></td>
          <td>
            <div class="cg-cell-obj">
              <span class="cg-ic" :style="{ background: r.other_color || '#165DFF' }" v-html="BL.icon(r.other_icon || 'cube', 11, '#fff')"></span>
              <div style="min-width:0">
                <div class="bl-truncate">{{ r.other_display_name || r.other_rdfs_label || r.other_api_name }}</div>
                <div class="bl-mono bl-muted" style="font-size:11px">{{ r.other_api_name }}</div>
              </div>
            </div>
          </td>
          <td class="bl-truncate" style="max-width:220px" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</td>
          <td class="bl-truncate" style="max-width:180px">{{ r.rdfs_see_also || '—' }}</td>
          <td class="bl-truncate" style="max-width:180px">{{ r.rdfs_defined_by || '—' }}</td>
          <td>
            <span :class="['bl-tag', r.status===1 ? 'bl-tag-success' : 'bl-tag-danger']">{{ r.status===1 ? '启用' : '禁用' }}</span>
          </td>
          <td @click.stop>
            <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeOne(r)" v-html="BL.icon('trash', 12)"></button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!filtered.length" class="bl-empty" style="padding:32px">{{ emptyText }}</div>

    <!-- 对象类型选择面板 -->
    <ObjectTypePickerModal
      v-model:open="pickerOpen"
      :multi="true"
      :exclude-ids="excludeIds"
      :subtitle="`选择${title.replace('类','')}的对象类型`"
      @confirm="onPickerConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { classMetaApi } from '@/api'
import ObjectTypePickerModal from '@/components/ObjectTypePickerModal.vue'

const props = defineProps({
  classId: { type: String, default: '' },
  type: { type: String, default: 'equivalent' } // equivalent | disjoint
})

const title = computed(() => props.type === 'equivalent' ? '等价类' : '不相交类')
const emptyText = computed(() => props.type === 'equivalent' ? '尚未维护等价类关系' : '尚未维护不相交类关系')

const rows = ref([])
const checked = ref(new Set())
const q = ref('')
const pickerOpen = ref(false)

async function load() {
  if (!props.classId) { rows.value = []; return }
  const list = await classMetaApi.listGroup(props.classId, props.type).catch(() => [])
  rows.value = list || []
  checked.value = new Set()
}
watch(() => [props.classId, props.type], load, { immediate: true })

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return rows.value
  return rows.value.filter(r => [r.other_api_name, r.other_display_name, r.other_rdfs_label, r.rdfs_comment]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})

const excludeIds = computed(() => {
  const ex = new Set([props.classId])
  rows.value.forEach(r => ex.add(r.other_class_id))
  return [...ex]
})

const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) { const s = new Set(checked.value); s.has(id) ? s.delete(id) : s.add(id); checked.value = s }
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) filtered.value.forEach(r => s.delete(r.id))
  else filtered.value.forEach(r => s.add(r.id))
  checked.value = s
}

function openPicker() { pickerOpen.value = true }

async function onPickerConfirm({ ids }) {
  if (!ids?.length) return
  let ok = 0, fail = 0
  for (const targetId of ids) {
    try {
      await classMetaApi.addGroup({
        class_id: props.classId,
        ref_class_id: targetId,
        group_type: props.type,
        status: 1
      })
      ok++
    } catch (e) { fail++ }
  }
  if (ok) BL.success(`已添加 ${ok} 条${fail ? ' / ' + fail + ' 条失败' : ''}`)
  else BL.warning('未能添加新关系')
  await load()
}

async function removeOne(r) {
  const ok = await BL.confirm({ title: '删除', content: '确定删除该关系？', danger: true, okText: '删除' })
  if (!ok) return
  await classMetaApi.removeGroup(r.id)
  BL.success('已删除')
  await load()
}
async function batchRemove() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中 ${ids.length} 条？`, danger: true, okText: '删除' })
  if (!ok) return
  for (const id of ids) await classMetaApi.removeGroup(id).catch(() => {})
  BL.success('已删除')
  await load()
}
async function batchToggle(status) {
  for (const id of checked.value) {
    const row = rows.value.find(r => r.id === id)
    if (row) await classMetaApi.updateGroup(id, { ...row, status }).catch(() => {})
  }
  BL.success(status === 1 ? '已启用' : '已停用')
  await load()
}
</script>

<style scoped>
.tab-cgroup { display: flex; flex-direction: column; gap: 10px; }
.cg-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.cg-title { font-size: 14px; font-weight: 600; }
.cg-search { position: relative; width: 220px; }
.cg-search .bl-input { padding-left: 26px; height: 30px; }
.cg-search .ot-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.cg-table { width: 100%; }
.cg-ic { width: 18px; height: 18px; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cg-cell-obj { display: flex; gap: 6px; align-items: center; min-width: 0; }
</style>
