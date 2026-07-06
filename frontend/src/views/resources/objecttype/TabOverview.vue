<template>
  <div class="tab-overview">
    <!-- 顶部一级页签 -->
    <div class="ov-tabs">
      <button v-for="t in tabs" :key="t.k" :class="['ov-tab', sub===t.k && 'is-on']" @click="sub = t.k">{{ t.label }}</button>
    </div>

    <!-- 基础 -->
    <div v-if="sub==='basic'" class="ov-form">
      <div class="ov-section-title">基础信息</div>
      <div class="ov-grid">
        <FieldRow label="名称" inline hint="display_name · 给人看的展示名称"><input class="bl-input" v-model="form.display_name" /></FieldRow>
        <FieldRow label="API" inline hint="api_name · snake_case · 程序唯一标识 · 不可修改"><input class="bl-input bl-mono" :value="form.api_name" disabled /></FieldRow>
        <FieldRow label="标准名" inline hint="rdfs:label · 本体标准展示标签"><input class="bl-input" v-model="form.rdfs_label" /></FieldRow>
        <FieldRow label="命名空间" inline hint="ns_code · 绑定的命名空间编码"><input class="bl-input bl-mono" v-model="form.ns_code" /></FieldRow>
        <FieldRow label="领域" inline hint="category_code · 所属业务领域"><span class="bl-tag">{{ form.categoryLabel || form.category_code || '—' }}</span></FieldRow>
        <FieldRow label="所属分组" inline>
          <select class="bl-input" v-model="form.group_id">
            <option value="">— 未分组 —</option>
            <option v-for="g in filteredGroups" :key="g.id" :value="g.id">{{ g.group_name }}</option>
          </select>
        </FieldRow>
        <FieldRow label="父类" inline hint="parent_class_id · 本体继承父类 ID (类层次中维护)"><span>{{ form.parent_class_id ? (parentLabel || form.parent_class_id) : '—' }}</span></FieldRow>
      </div>

      <FieldRow label="注释" hint="rdfs:comment"><textarea class="bl-textarea" rows="2" v-model="form.rdfs_comment"></textarea></FieldRow>
      <FieldRow label="元数据公理" hint="JSON · 机器可读：查询约束 / 推理规则">
        <textarea class="bl-textarea bl-mono" rows="3" v-model="form.metadata" placeholder='{"query":"...","unit_normalization":true}'></textarea>
        <div class="bl-muted" style="font-size:11px;margin-top:4px">示例: {"reasoner":"hermit","cache_ttl":300}</div>
      </FieldRow>

      <div class="ov-section-title">标志位</div>
      <div class="ov-flags">
        <div class="flag-grp">
          <span class="flag-lbl">顶层类</span>
          <label class="ov-switch">
            <input type="checkbox" :checked="form.is_thing===1" @change="form.is_thing = $event.target.checked ? 1 : 0" />
            <span class="ov-switch-slider"></span>
          </label>
          <span :class="['ov-switch-tag', form.is_thing===1 ? 'is-on' : 'is-off']">{{ form.is_thing===1 ? '是' : '否' }}</span>
        </div>
        <div class="flag-grp">
          <span class="flag-lbl">底层类</span>
          <label class="ov-switch">
            <input type="checkbox" :checked="form.is_nothing===1" @change="form.is_nothing = $event.target.checked ? 1 : 0" />
            <span class="ov-switch-slider"></span>
          </label>
          <span :class="['ov-switch-tag', form.is_nothing===1 ? 'is-on' : 'is-off']">{{ form.is_nothing===1 ? '是' : '否' }}</span>
        </div>
        <div class="flag-grp">
          <span class="flag-lbl">公共类</span>
          <label class="ov-switch">
            <input type="checkbox" :checked="form.is_common===1" @change="form.is_common = $event.target.checked ? 1 : 0" />
            <span class="ov-switch-slider"></span>
          </label>
          <span :class="['ov-switch-tag', form.is_common===1 ? 'is-on' : 'is-off']">{{ form.is_common===1 ? '是' : '否' }}</span>
        </div>
        <div class="flag-grp">
          <span class="flag-lbl">启用状态</span>
          <label class="ov-switch is-status">
            <input type="checkbox" :checked="form.status===1" @change="form.status = $event.target.checked ? 1 : 0" />
            <span class="ov-switch-slider"></span>
          </label>
          <span :class="['ov-switch-tag is-status', form.status===1 ? 'is-on' : 'is-off']">{{ form.status===1 ? '启用' : '禁用' }}</span>
        </div>
      </div>
    </div>

    <!-- 显示 -->
    <div v-if="sub==='style'" class="ov-form">
      <div class="ov-section-title">分组颜色</div>
      <ColorPickerField v-model="form.color" />
      <div class="ov-section-title">图标</div>
      <IconPickerField v-model="form.icon" label="" :preset-count="32" :suggest-name="form.display_name || form.rdfs_label || form.api_name" />
    </div>

    <!-- 类表达式 -->
    <div v-if="sub==='expr'" class="ov-form">
      <div class="ov-section-title">类表达式类型</div>
      <div class="expr-types">
        <button v-for="t in exprTypes" :key="t.k" :class="['expr-btn', form.class_expr_type===t.k && 'is-on']" @click="form.class_expr_type = t.k">
          <span v-html="BL.icon(t.icon, 12)"></span><span style="margin-left:4px">{{ t.label }}</span>
        </button>
      </div>
      <FieldRow label="表达式内容" hint="JSON · 与表达式类型严格对应">
        <textarea class="bl-textarea bl-mono" rows="6" v-model="form.class_expr_content" :placeholder="exprPlaceholder"></textarea>
        <div class="bl-muted" style="font-size:11px;margin-top:4px">{{ exprHint }}</div>
      </FieldRow>
    </div>

    <!-- 其他 -->
    <div v-if="sub==='other'" class="ov-form">
      <FieldRow label="参考资料" hint="rdfs:seeAlso"><textarea class="bl-textarea" rows="2" v-model="form.rdfs_see_also"></textarea></FieldRow>
      <FieldRow label="定义来源" hint="rdfs:isDefinedBy"><textarea class="bl-textarea" rows="2" v-model="form.rdfs_defined_by"></textarea></FieldRow>
      <div class="ov-grid">
        <FieldRow label="ID" inline>
          <div class="rid-row">
            <span class="bl-mono">{{ form.id || '—' }}</span>
            <button v-if="form.id" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="复制 ID" @click="copy(form.id)" v-html="BL.icon('copy', 12)"></button>
          </div>
        </FieldRow>
        <FieldRow label="RID" inline>
          <div class="rid-row">
            <span class="bl-mono bl-truncate" :title="form.rid">{{ form.rid || '—' }}</span>
            <button v-if="form.rid" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="复制 RID" @click="copy(form.rid)" v-html="BL.icon('copy', 12)"></button>
          </div>
        </FieldRow>
        <FieldRow label="创建时间" inline><span class="bl-mono bl-muted">{{ form.create_time || '—' }}</span></FieldRow>
        <FieldRow label="更新时间" inline><span class="bl-mono bl-muted">{{ form.update_time || '—' }}</span></FieldRow>
      </div>
    </div>

    <!-- 底部保存 -->
    <div class="ov-ft">
      <button class="bl-btn" @click="onReset">重置</button>
      <button class="bl-btn bl-btn-primary" @click="onSave">保存</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, reactive, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { classMetaApi, groupApi, groupRefApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'
import IconPickerField from '@/components/IconPickerField.vue'
import ColorPickerField from '@/components/ColorPickerField.vue'

const props = defineProps({ detail: { type: Object, default: () => ({}) } })
const emit = defineEmits(['saved'])

const sub = ref('basic')
const tabs = [
  { k: 'basic', label: '基础' },
  { k: 'style', label: '显示' },
  { k: 'expr',  label: '类表达式' },
  { k: 'other', label: '其他' }
]
const exprTypes = [
  { k: '',             label: '普通业务类',     icon: 'cube' },
  { k: 'union',        label: '并集 union',      icon: 'share' },
  { k: 'intersection', label: '交集 intersection', icon: 'layers' },
  { k: 'complement',   label: '补集 complement',   icon: 'x' },
  { k: 'enumeration',  label: '枚举 enumeration', icon: 'list' }
]

const form = reactive({})
const parentLabel = ref('')

/* 分组管理 */
const groups = ref([])
const objGroupRef = ref(null)
const objGroupIds = ref(new Set())  // 对象类型的分组 id 集合(group_type='object_types')
/* 只显示对象类型的分组;领域相关组按当前领域收窄;已选分组始终保留兜底 */
const filteredGroups = computed(() => {
  const domain = form.category_code || form.categoryCode || ''
  return groups.value.filter(g =>
    g.id === form.group_id ||
    (g.domain_code ? g.domain_code === domain : g.category_code ? g.category_code === domain : true)
  )
})
const loadedDomain = ref('')  // 已加载分组的领域,避免同领域重复请求
// 对象类型的分组集合(group_type='object_types'),全量加载一次
async function loadObjGroupIds() {
  if (objGroupIds.value.size) return
  const objRefs = await groupRefApi.list('object_types').catch(() => [])
  objGroupIds.value = new Set((objRefs || []).map(r => r.groupId || r.group_id))
}
// 按当前对象所属领域加载分组(不再一次性拉取全部)
async function loadGroupsByDomain(domain) {
  if (!domain) { groups.value = []; loadedDomain.value = ''; return }
  if (loadedDomain.value === domain) return
  const bizGroups = await groupApi.byDomain(domain).catch(() => [])
  groups.value = (bizGroups || []).map(g => ({
    id: g.id, parent_id: g.parentId || g.parent_id,
    group_name: g.gname || g.gName || g.g_name || g.group_name || '',
    category_code: g.categoryCode || g.category_code,
    domain_code: g.domainCode || g.domain_code || '',
    status: g.status || 'active'
  }))
  loadedDomain.value = domain
}
async function loadObjGroupRef(id) {
  try {
    const refs = await groupRefApi.list('object_types').catch(() => [])
    const ref = (refs || []).find(r => r.ref_id === id)
    objGroupRef.value = ref || null
    form.group_id = ref ? (ref.groupId || ref.group_id || '') : ''
  } catch { form.group_id = '' }
}
onMounted(() => loadObjGroupIds())

function loadFromDetail() {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, props.detail || {})
  form.is_thing  = Number(form.is_thing  ?? 0)
  form.is_nothing= Number(form.is_nothing?? 0)
  form.is_common = Number(form.is_common ?? 0)
  form.status    = Number(form.status    ?? 1)
  if (form.id) loadObjGroupRef(form.id)
  loadGroupsByDomain(form.category_code || form.categoryCode || '')
}
watch(() => props.detail, loadFromDetail, { immediate: true, deep: true })

const exprPlaceholder = computed(() => {
  switch (form.class_expr_type) {
    case 'union':        return '{"unionIds":["class-xxx","class-yyy"]}'
    case 'intersection': return '{"intersectionIds":["class-xxx","class-yyy"]}'
    case 'complement':   return '{"complementId":"class-xxx"}'
    case 'enumeration':  return '{"enumValues":["大型","中型","小型"],"enumCodes":["large","medium","small"]}'
    default: return '{}'
  }
})
const exprHint = computed(() => {
  switch (form.class_expr_type) {
    case 'union':        return 'A 或 B 都属于当前类（满足任一条件即可）'
    case 'intersection': return 'A 且 B 才属于当前类（同时满足全部条件）'
    case 'complement':   return '不属于某一类的所有实例（排除指定类）'
    case 'enumeration':  return '类的实例是固定的几个枚举值'
    default: return '无复杂逻辑的基础业务类，class_expr_content 留空 {}'
  }
})

async function onSave() {
  if (!form.id) { BL.warning('未选中对象'); return }
  try {
    await classMetaApi.updateClass(form.id, { ...form })
    // 同步分组绑定
    try {
      if (form.group_id && form.group_id !== (objGroupRef.value?.groupId || objGroupRef.value?.group_id || '')) {
        if (objGroupRef.value) { await groupRefApi.removeByRef(form.id, 'object_types'); objGroupRef.value = null }
        await groupRefApi.create({ ref_id: form.id, group_id: form.group_id, group_type: 'object_types' })
      } else if (!form.group_id && objGroupRef.value) {
        await groupRefApi.removeByRef(form.id, 'object_types')
        objGroupRef.value = null
      }
    } catch (e) { /* 分组绑定失败不影响主流程 */ }
    BL.success('已保存')
    emit('saved', form.id)
  } catch (e) {
    BL.error(e?.msg || '保存失败')
  }
}
function onReset() { loadFromDetail() }
async function copy(t) {
  try { await navigator.clipboard.writeText(String(t || '')); BL.success('已复制') } catch {}
}
</script>

<style scoped>
.tab-overview { display: flex; flex-direction: column; gap: 12px; }

.ov-tabs {
  display: flex; gap: 2px;
  border-bottom: 1px solid var(--bl-divider);
  position: sticky; top: 0; background: var(--bl-bg-1); z-index: 1;
  height: 30px;
}
.ov-tab {
  padding: 0 14px; height: 30px;
  border: 0; background: transparent; cursor: pointer;
  color: var(--bl-text-2); font-size: 12.5px;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  display: inline-flex; align-items: center;
  line-height: 1;
}
.ov-tab:hover { color: var(--bl-text-1); }
.ov-tab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }

.ov-form { display: flex; flex-direction: column; gap: 6px; }
.ov-section-title { font-size: 12px; color: var(--bl-text-3); margin: 12px 0 4px;
  padding-left: 8px; border-left: 3px solid var(--bl-primary); font-weight: 500; }
.ov-section-title:first-child { margin-top: 0; }
.ov-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2px 16px; }

.ov-flags { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.flag-grp { display: inline-flex; align-items: center; gap: 10px; padding: 8px 12px;
  background: var(--bl-bg-2); border-radius: 4px; }
.flag-lbl { font-size: 12px; color: var(--bl-text-3); min-width: 60px; }

/* 开关键 (统一替代之前的二选一块状按钮) */
.ov-switch { position: relative; display: inline-block; width: 36px; height: 18px; flex-shrink: 0; }
.ov-switch input { opacity: 0; width: 0; height: 0; }
.ov-switch-slider {
  position: absolute; cursor: pointer; inset: 0;
  background: #cccccc; border-radius: 999px; transition: background-color .15s;
}
.ov-switch-slider::before {
  content: ''; position: absolute; top: 2px; left: 2px;
  width: 14px; height: 14px; background: #fff; border-radius: 50%;
  transition: transform .15s; box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.ov-switch input:checked + .ov-switch-slider { background: var(--bl-primary); }
.ov-switch input:checked + .ov-switch-slider::before { transform: translateX(18px); }

.ov-switch-tag { font-size: 12px; font-weight: 500; min-width: 28px; }
.ov-switch-tag.is-on  { color: var(--bl-primary); }
.ov-switch-tag.is-off { color: var(--bl-text-3); }

/* 启用状态: 启用为绿色,禁用为红色 (区别于普通开关) */
.ov-switch.is-status .ov-switch-slider { background: #f53f3f; }
.ov-switch.is-status input:checked + .ov-switch-slider { background: #00b42a; }
.ov-switch-tag.is-status.is-on  { color: #00b42a; }
.ov-switch-tag.is-status.is-off { color: #f53f3f; }

.expr-types { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.expr-btn { display: inline-flex; align-items: center; padding: 6px 12px;
  border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 4px;
  font-size: 12px; cursor: pointer; color: var(--bl-text-2); }
.expr-btn.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }

.rid-row { display: flex; align-items: center; gap: 6px;
  padding: 4px 8px; background: var(--bl-bg-2); border-radius: 3px; font-size: 12px; }

.ov-ft { position: sticky; bottom: 0; background: var(--bl-bg-1);
  border-top: 1px solid var(--bl-divider); padding: 10px 0 0;
  display: flex; justify-content: flex-end; gap: 8px; }
</style>
