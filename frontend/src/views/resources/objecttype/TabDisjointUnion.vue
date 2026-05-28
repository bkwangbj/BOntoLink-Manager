<template>
  <div class="tab-du">
    <!-- 顶部: 单行布局 (标题 + ? + 副标题 / 右侧操作),参考等价属性 -->
    <div class="du-head">
      <div class="du-head-l">
        <span class="du-title">互斥并集</span>
        <span class="bl-help" title="子类互不重叠,合并完整覆盖总类" v-html="BL.icon('help', 14)"></span>
        <span class="du-subtitle bl-muted">子类互不重叠,合并完整覆盖总类</span>
      </div>
      <div class="bl-row" style="gap:8px">
        <button class="bl-btn bl-btn-sm du-danger-btn" :disabled="!rows.length" @click="clearAll">
          <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">清空全部</span>
        </button>
        <button class="bl-btn bl-btn-sm" :disabled="!rows.length" @click="batchDisable">
          <span v-html="BL.icon('power', 12)"></span><span style="margin-left:4px">全部禁用</span>
        </button>
      </div>
    </div>

    <!-- 主体：左总类 + 中分割 + 右子类 -->
    <div class="du-body">
      <!-- 左 -->
      <div class="du-pane du-left">
        <div class="du-pane-hd">
          <div>
            <div class="du-pane-title">总类</div>
            <div class="bl-muted" style="font-size:11px">被划分的类，子类合集等于总类</div>
          </div>
        </div>
        <div class="du-pane-body">
          <div class="du-parent-card">
            <span class="du-ic" :style="{ background: parentColor }" v-html="BL.icon(parentIcon, 14, '#fff')"></span>
            <div style="min-width:0;flex:1">
              <div style="font-weight:600">{{ parentLabel || '当前对象' }}</div>
              <div class="bl-mono bl-muted" style="font-size:11px">{{ parentApi || '—' }}</div>
              <div class="bl-muted" style="font-size:11px;margin-top:4px">包含 {{ rows.length }} 个子类</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中分割 -->
      <div class="du-divider">
        <span class="du-divider-txt">包含</span>
      </div>

      <!-- 右 -->
      <div class="du-pane du-right">
        <div class="du-pane-hd">
          <div>
            <div class="du-pane-title">不相交子类</div>
            <div class="bl-muted" style="font-size:11px">子类之间互斥无交集，共同构成总类</div>
          </div>
          <div class="bl-row" style="gap:6px">
            <div class="du-search">
              <span class="ot-ic" v-html="BL.icon('search', 12)"></span>
              <input class="bl-input" v-model="q" placeholder="搜索" />
            </div>
            <button v-if="checked.size" class="bl-btn bl-btn-sm bl-btn-danger" @click="batchRemove">删除 ({{ checked.size }})</button>
            <button v-if="checked.size" class="bl-btn bl-btn-sm" @click="batchSetStatus(1)">启用</button>
            <button v-if="checked.size" class="bl-btn bl-btn-sm" @click="batchSetStatus(0)">停用</button>
            <button class="bl-btn bl-btn-sm bl-btn-primary" @click="pickerOpen=true">
              <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">新增</span>
            </button>
          </div>
        </div>
        <div class="du-pane-body">
          <table class="bl-table du-table" v-if="filtered.length">
            <thead>
              <tr>
                <th style="width:34px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th>对象类型</th>
                <th>注释</th>
                <th>状态</th>
                <th style="width:60px"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in filtered" :key="r.id">
                <td @click.stop><input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" /></td>
                <td>
                  <div class="bl-row" style="gap:6px;align-items:center;min-width:0">
                    <span class="du-ic" :style="{ background: r.color || '#165DFF' }" v-html="BL.icon(r.icon || 'cube', 11, '#fff')"></span>
                    <div style="min-width:0">
                      <div class="bl-truncate">{{ r.rdfs_label || r.display_name || r.api_name }}</div>
                      <div class="bl-mono bl-muted" style="font-size:11px">{{ r.api_name }}</div>
                    </div>
                  </div>
                </td>
                <td class="bl-truncate" style="max-width:240px" :title="r.rdfs_comment || r.sub_rdfs_comment">{{ r.rdfs_comment || r.sub_rdfs_comment || '—' }}</td>
                <td><span :class="['bl-tag', r.status===1?'bl-tag-success':'bl-tag-danger']">{{ r.status===1?'启用':'禁用' }}</span></td>
                <td @click.stop>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeOne(r)" v-html="BL.icon('trash', 12)"></button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="bl-empty" style="padding:32px">尚未配置互斥并集子类，点击「新增」添加</div>
        </div>
      </div>
    </div>

    <!-- 对象选择面板 -->
    <ObjectTypePickerModal
      v-model:open="pickerOpen"
      :multi="true"
      :exclude-ids="excludeIds"
      subtitle="选择构成互斥并集的子类（须语义互斥、合并覆盖总类）"
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
  parentLabel: { type: String, default: '' },
  parentApi: { type: String, default: '' },
  parentIcon: { type: String, default: 'cube' },
  parentColor: { type: String, default: '#165DFF' }
})

const rows = ref([])
const checked = ref(new Set())
const q = ref('')
const pickerOpen = ref(false)

async function load() {
  if (!props.classId) { rows.value = []; return }
  rows.value = await classMetaApi.listDisjointUnion(props.classId).catch(() => []) || []
  checked.value = new Set()
}
watch(() => props.classId, load, { immediate: true })

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return rows.value
  return rows.value.filter(r => [r.api_name, r.display_name, r.rdfs_label, r.rdfs_comment].filter(Boolean)
    .some(s => String(s).toLowerCase().includes(k)))
})
const excludeIds = computed(() => {
  const s = new Set([props.classId])
  rows.value.forEach(r => s.add(r.sub_class_id))
  return [...s]
})
const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) { const s = new Set(checked.value); s.has(id) ? s.delete(id) : s.add(id); checked.value = s }
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) filtered.value.forEach(r => s.delete(r.id))
  else filtered.value.forEach(r => s.add(r.id))
  checked.value = s
}

async function onPickerConfirm({ ids }) {
  if (!ids?.length) return
  let ok = 0
  for (const subId of ids) {
    try {
      await classMetaApi.addDisjointUnion({ parent_class_id: props.classId, sub_class_id: subId, status: 1 })
      ok++
    } catch {}
  }
  if (ok) BL.success(`已添加 ${ok} 个子类`)
  await load()
}
async function removeOne(r) {
  const ok = await BL.confirm({ title: '删除子类', content: `从互斥并集中移除「${r.rdfs_label || r.api_name}」？`, danger: true, okText: '删除' })
  if (!ok) return
  await classMetaApi.removeDisjointUnion(r.id)
  await load()
}
async function batchRemove() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中 ${ids.length} 个子类？`, danger: true, okText: '删除' })
  if (!ok) return
  for (const id of ids) await classMetaApi.removeDisjointUnion(id).catch(() => {})
  await load()
}
async function batchSetStatus(status) {
  for (const id of checked.value) {
    const row = rows.value.find(r => r.id === id)
    if (row) await classMetaApi.updateDisjointUnion(id, { ...row, status }).catch(() => {})
  }
  await load()
}
async function clearAll() {
  if (!rows.value.length) return
  const ok = await BL.confirm({ title: '清空全部', content: '将清空所有子类，确定？', danger: true, okText: '清空' })
  if (!ok) return
  for (const r of rows.value) await classMetaApi.removeDisjointUnion(r.id).catch(() => {})
  await load()
}
async function batchDisable() {
  for (const r of rows.value) await classMetaApi.updateDisjointUnion(r.id, { ...r, status: 0 }).catch(() => {})
  await load()
}
</script>

<style scoped>
.tab-du {
  display: flex; flex-direction: column; gap: 10px;
  height: 100%; min-height: 0;
}
/* 顶部: 单行紧凑布局 (与等价属性头部风格一致) */
.du-head {
  flex-shrink: 0;
  display: flex; justify-content: space-between; align-items: center; gap: 12px;

  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.du-head-l { display: inline-flex; align-items: center; gap: 8px; min-width: 0; }
.du-title { font-size: 14px; font-weight: 600; color: #333; }
.bl-help { color: #999; cursor: help; display: inline-flex; }
.bl-help:hover { color: var(--bl-primary); }
.du-subtitle { font-size: 12px; color: #999; }
/* 危险按钮: 白底红框 (与属性列表 / 等价属性 一致) */
.du-danger-btn { background: #fff; border: 1px solid #f5222d; color: #f5222d; }
.du-danger-btn:not(:disabled):hover { background: #fff1f0; }
.du-danger-btn:disabled { opacity: .4; cursor: not-allowed; }

/* 主体: 占满 toolbar 之下的全部剩余空间 */
.du-body {
  flex: 1; min-height: 0;
  display: grid; grid-template-columns: 280px 60px 1fr; gap: 0;
  border: 1px solid var(--bl-border); border-radius: 6px;
  overflow: hidden; background: var(--bl-bg-1);
}
.du-pane { display: flex; flex-direction: column; min-width: 0; overflow: hidden; }
.du-pane-hd { display: flex; justify-content: space-between; align-items: center; gap: 8px;
  padding: 8px 12px; background: #fafafa; border-bottom: 1px solid var(--bl-divider); }
.du-pane-title { font-size: 13px; font-weight: 600; }
.du-pane-body { flex: 1; overflow: auto; padding: 10px; }

.du-parent-card { display: flex; gap: 10px; padding: 12px; border: 1px solid var(--bl-border);
  border-radius: 6px; background: var(--bl-bg-1); }
.du-ic { width: 24px; height: 24px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }

.du-divider { background: var(--bl-bg-2); border-left: 1px dashed var(--bl-border-strong);
  border-right: 1px dashed var(--bl-border-strong); position: relative; }
.du-divider-txt { position: absolute; top: 30%; left: 50%; transform: translate(-50%, -50%);
  writing-mode: vertical-rl; font-weight: 600; letter-spacing: 4px; color: var(--bl-text-2); }

.du-search { position: relative; width: 180px; }
.du-search .bl-input { padding-left: 26px; height: 28px; font-size: 12px; }
.du-search .ot-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }

.du-table { width: 100%; font-size: 12px; }
.du-table th { background: #e8f3ff; padding: 8px; font-weight: 600; }
.du-table td { padding: 6px 8px; }
</style>
