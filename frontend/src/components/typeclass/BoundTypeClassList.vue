<!--
  「已绑定类型类」区(1.3)。放在对象属性详情 / 链接详情页。
  列出已绑定类型类 + 添加(选择弹窗)/ 参数(配置型开编辑弹窗)/ 删除。
  carrier 标识载体:
    property: { applicableType:'property', ownerType:'object'|'interface', ownerId, propertyId }
    relation: { applicableType:'relation', linkTypeId }
-->
<template>
  <div class="btl">
    <div class="btl-hd">
      <span class="btl-title">已绑定类型类 <span class="btl-cnt">({{ rows.length }})</span></span>
      <button v-if="ready" class="btl-add" @click="picker = true"><span v-html="BL.icon('plus', 13)"></span>添加</button>
    </div>

    <div v-if="!ready" class="btl-tip">保存该{{ carrier.applicableType === 'relation' ? '链接' : '属性' }}后可绑定类型类。</div>
    <div v-else-if="!rows.length" class="btl-empty">暂未绑定类型类,点击「添加」挂载。</div>

    <div v-for="b in rows" :key="b.id" class="btl-row">
      <span class="btl-ic" v-html="BL.icon(b.icon || 'tag', 14, b.color || 'var(--bl-primary)')"></span>
      <div class="btl-info">
        <div class="btl-name">
          <span class="btl-kind">{{ b.category_code }}</span>
          <span class="bl-mono btl-prefix">{{ b.name_template || b.name_prefix }}</span>
          <span class="btl-cn">{{ b.name_cn_base }}</span>
        </div>
      </div>
      <button v-if="hasParams(b) || isAnalyzer(b)" class="btl-op btl-param" title="参数" @click="editing = b"><span v-html="BL.icon('edit', 13)"></span>参数</button>
      <button class="btl-op btl-del" title="解除绑定" @click="remove(b)" v-html="BL.icon('x', 14)"></button>
    </div>

    <TypeClassPickerModal v-if="picker" :applicable-type="carrier.applicableType" :exclude-ids="boundIds"
                          @confirm="onAdd" @close="picker = false" />
    <AnalyzerConfigModal v-if="editing && isAnalyzer(editing)" :value="editing.value" @save="onSaveParam" @close="editing = null" />
    <ParamEditModal v-else-if="editing" :bind="editing" @save="onSaveParam" @close="editing = null" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { tcBindApi } from '@/api'
import TypeClassPickerModal from './TypeClassPickerModal.vue'
import ParamEditModal from './ParamEditModal.vue'
import AnalyzerConfigModal from './AnalyzerConfigModal.vue'

const props = defineProps({
  carrier: { type: Object, required: true }
})

const rows = ref([])
const picker = ref(false)
const editing = ref(null)
const boundIds = computed(() => rows.value.map(r => r.type_class_meta_id))

/* 载体是否已落库(有 id 才能绑定) */
const ready = computed(() => {
  const c = props.carrier
  if (c.applicableType === 'relation') return !!c.linkTypeId
  return !!(c.ownerId && c.propertyId)
})

/* 分词类型类走专用弹窗(not_analyzed 纯标记除外) */
function isAnalyzer(b) { return b && b.category_code === 'analyzer' && b.name_prefix !== 'not_analyzed' }

function hasParams(b) {
  const s = b.param_json
  if (!s) return false
  try {
    const o = typeof s === 'string' ? JSON.parse(s) : s
    const fields = o.properties && typeof o.properties === 'object' ? o.properties : o
    return fields && typeof fields === 'object' && Object.keys(fields).some(k => !['type', 'required', 'properties', '$schema'].includes(k))
  } catch { return false }
}

async function reload() {
  if (!ready.value) { rows.value = []; return }
  const c = props.carrier
  const params = c.applicableType === 'relation'
    ? { applicableType: 'relation', linkTypeId: c.linkTypeId }
    : { applicableType: 'property', ownerId: c.ownerId, propertyId: c.propertyId }
  try { rows.value = (await tcBindApi.byCarrier(params)) || [] } catch { rows.value = [] }
}

async function onAdd(metaIds) {
  picker.value = false
  const c = props.carrier
  for (const metaId of metaIds) {
    const payload = c.applicableType === 'relation'
      ? { type_class_meta_id: metaId, applicable_type: 'relation', link_type_id: c.linkTypeId }
      : { type_class_meta_id: metaId, applicable_type: 'property', property_owner_type: c.ownerType || 'object', property_owner_id: c.ownerId, property_id: c.propertyId }
    try { await tcBindApi.create(payload) } catch {}
  }
  BL.success(`已绑定 ${metaIds.length} 个类型类`)
  reload()
}

async function onSaveParam(value) {
  const b = editing.value
  editing.value = null
  try { await tcBindApi.update(b.id, value); BL.success('参数已保存') } catch {}
  reload()
}

async function remove(b) {
  const ok = await BL.confirm({ title: '解除绑定', content: `确定解除「${b.name_cn_base}」的绑定?`, danger: true, okText: '解除' })
  if (!ok) return
  try { await tcBindApi.remove(b.id); reload() } catch {}
}

watch(() => props.carrier, reload, { immediate: true, deep: true })
</script>

<style scoped>
.btl { display: flex; flex-direction: column; gap: 8px; }
.btl-hd { display: flex; align-items: center; }
.btl-title { flex: 1; font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.btl-cnt { color: var(--bl-text-3); font-weight: 400; }
.btl-add { display: inline-flex; align-items: center; gap: 3px; border: 0; background: transparent; color: var(--bl-primary); font-size: 12.5px; cursor: pointer; padding: 2px 4px; border-radius: 4px; }
.btl-add:hover { background: var(--bl-primary-soft); }
.btl-tip, .btl-empty { font-size: 12px; color: var(--bl-text-3); padding: 8px 10px; background: var(--bl-bg-2); border-radius: 6px; }
.btl-row { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); }
.btl-row:hover { border-color: var(--bl-primary-border); }
.btl-ic { display: inline-flex; flex-shrink: 0; }
.btl-info { flex: 1; min-width: 0; }
.btl-name { display: flex; align-items: center; gap: 6px; font-size: 12.5px; min-width: 0; }
.btl-kind { font-size: 10.5px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 3px; padding: 0 5px; flex-shrink: 0; }
.btl-prefix { color: var(--bl-text-2); }
.btl-cn { color: var(--bl-text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.btl-op { flex-shrink: 0; display: inline-flex; align-items: center; gap: 3px; border: 0; background: transparent; cursor: pointer; border-radius: 4px; padding: 3px 6px; font-size: 12px; }
.btl-param { color: #ff7d00; }
.btl-param:hover { background: rgba(255,125,0,.1); }
.btl-del { color: var(--bl-text-3); width: 26px; justify-content: center; }
.btl-del:hover { background: #fff1f0; color: #f53f3f; }
</style>
