<template>
  <div class="roe">
    <div class="fe-info"><span v-html="BL.icon('info', 14, '#165DFF')"></span><span>规则按从上到下顺序依次执行,后行的规则会覆盖前序规则对同一对象同属性的修改。拖拽卡片可调整规则顺序。</span></div>

    <!-- 1 基础配置 -->
    <div class="adw-card"><div class="adw-card-hd">基础配置</div>
      <div class="adw-grid">
        <template v-if="!rule.kind.includes('link')">
          <!-- 与新建动作向导同一个入口: 整行即选择器, 点开对象类型库挑 -->
          <div class="adw-fld"><span class="adw-lbl">对象类型 <i>*</i></span>
            <div class="roe-pick" :class="{ 'is-empty': !pickedClass }"
                 :title="pickedClass ? '点击更换对象类' : '点击从对象类型库中选择'" @click="classPickerOpen = true">
              <template v-if="pickedClass">
                <span class="roe-pick-ic" :style="{ background: pickedClass.color || '#165DFF' }"
                      v-html="BL.icon(pickedClass.icon || 'cube', 13, '#fff')"></span>
                <span class="roe-pick-text bl-truncate">
                  {{ pickedClass.cn }}<span class="roe-pick-api bl-mono bl-muted">{{ pickedClass.api_name }}</span>
                </span>
                <button class="roe-pick-x" title="清除" @click.stop="clearClass" v-html="BL.icon('x', 11)"></button>
              </template>
              <span v-else class="roe-pick-text is-ph">点击选择对象类</span>
              <span class="roe-pick-act"><span v-html="BL.icon('search', 12)"></span>{{ pickedClass ? '更换' : '选择' }}</span>
            </div>
          </div>
          <label v-if="rule.kind === 'create_object'" class="adw-fld"><span class="adw-lbl">主键属性</span>
            <BlSelect v-model="rule.obj_pk_property" :options="pkOptions" placeholder="系统自动生成" />
          </label>
          <label v-else class="adw-fld"><span class="adw-lbl">目标对象参数 <i>*</i></span>
            <BlSelect v-model="rule.target_param_code" :options="objectParamOptions" clearable
                      :placeholder="objectParamOptions.length ? '选择对象引用参数' : '暂无对象引用参数,请先在表单添加'" />
          </label>
        </template>
        <template v-else>
          <label class="adw-fld"><span class="adw-lbl">链接类型 <i>*</i></span>
            <BlSelect v-model="rule.link_type_code" :options="linkTypeOptions" clearable placeholder="选择链接类型" />
          </label>
          <label class="adw-fld"><span class="adw-lbl">链接基数</span><input class="bl-input" :value="linkCardLabel(rule.link_type_code)" disabled /></label>
          <label class="adw-fld"><span class="adw-lbl">源对象参数 <i>*</i></span>
            <BlSelect v-model="rule.link_src_param" :options="objectParamOptions" clearable placeholder="源对象引用参数" />
          </label>
          <label class="adw-fld"><span class="adw-lbl">对端对象参数 <i>*</i></span>
            <BlSelect v-model="rule.link_dst_param" :options="objectParamOptions" clearable placeholder="目标对象引用参数" />
          </label>
        </template>
      </div>
    </div>

    <!-- 2 属性映射 -->
    <div v-if="!rule.kind.startsWith('delete')" class="adw-card">
      <div class="adw-card-hd adw-card-hd-flex"><span>属性映射</span>
        <span class="adw-card-hd-act">
          <button v-if="unmappedCount" class="bl-btn bl-btn-text bl-btn-sm" :title="`把该对象类剩余 ${unmappedCount} 个未映射属性一次性加进来`" @click="importAllProps">
            <span v-html="BL.icon('download', 11)"></span><span style="margin-left:3px">导入全部属性({{ unmappedCount }})</span></button>
          <span class="bl-muted" style="font-size:11.5px;font-weight:400;line-height:26px">配置对象属性的取值来源</span>
        </span>
      </div>
      <div class="roe-map-wrap">
      <table class="bl-table roe-map">
        <!-- 「对象及属性」「默认值配置」不定宽, 由它们吸收富余/不足, 其余列按内容给固定宽 -->
        <colgroup>
          <col style="width:34px" /><col style="width:150px" /><col style="width:62px" />
          <col v-if="isModify" style="width:96px" /><col style="width:106px" /><col style="width:92px" /><col style="width:118px" />
          <col /><col style="width:76px" /><col /><col style="width:46px" /><col style="width:34px" />
        </colgroup>
        <thead><tr>
          <th class="t-left">序号</th><th class="t-left">属性</th><th class="t-left">数据类型</th>
          <th v-if="isModify" class="t-left">操作</th>
          <th class="t-left">赋值方式</th><th class="t-left">参数名称</th><th class="t-left">参数代码</th>
          <th class="t-left">对象及属性</th><th class="t-left">默认值类型</th><th class="t-left">默认值配置</th>
          <th class="t-center">必填</th><th class="t-center"></th>
        </tr></thead>
        <tbody>
          <tr v-for="(m, mi) in rule.prop_mappings" :key="mi">
            <td class="bl-muted">{{ mi + 1 }}</td>
            <!-- 属性是每行的主体; 绑定的参数作为只读引用挂在下面, 参数本身在表单页维护 -->
            <td>
              <div v-if="propOptions.length && propMetaOf(m)" class="roe-prop-cell" title="点击更换属性">
                <div class="roe-prop-name">
                  {{ propMetaOf(m).name }}
                  <span v-if="propMetaOf(m).isPrimary" class="roe-pk" title="主键">主键</span>
                </div>
                <div class="roe-prop-code bl-mono bl-muted">{{ m.property_code }}</div>
                <BlSelect class="roe-prop-sel" v-model="m.property_code" :options="propOptionsFor(m)" size="sm" clearable placeholder="选择属性" @change="syncRequired(m)" />
              </div>
              <BlSelect v-else-if="propOptions.length" v-model="m.property_code" :options="propOptionsFor(m)" size="sm" clearable placeholder="选择属性" @change="syncRequired(m)" />
              <input v-else class="bl-input bl-input-xs bl-mono" v-model="m.property_code" placeholder="属性编码" />
              <div v-if="Number(m.value_source)===1 && m.value_content" class="roe-bind bl-mono" title="该属性取自这个表单参数">↳ {{ m.value_content }}</div>
            </td>
            <td><span v-if="dtOf(m)" class="bl-tag roe-dt">{{ dtOf(m) }}</span><span v-else class="bl-muted" style="font-size:11px">—</span></td>
            <td v-if="isModify"><BlSelect v-model="m.prop_operator" :options="PROP_OPERATOR_OPTS" size="sm" /></td>
            <td><BlSelect v-model="m.value_source" :options="valueSourceOpts" size="sm" @change="onSourceChange(m)" /></td>
            <!-- 参数名称: 参数在表单页维护, 这里只回显所选参数的名字 -->
            <td>
              <span v-if="cellOn(m,'param')" class="roe-ro bl-truncate" :title="paramNameOf(m) || '未选参数'">{{ paramNameOf(m) || '—' }}</span>
              <span v-else class="roe-na">不适用</span>
            </td>
            <td>
              <BlSelect v-if="cellOn(m,'param')" v-model="m.value_content" :options="formParamOptions" size="sm" clearable
                        placeholder="选表单参数" @change="syncParamName(m)" />
              <span v-else class="roe-na">不适用</span>
            </td>
            <!-- 对象及属性: 对象引用参数 / 主对象 / 前序规则创建的对象 -->
            <td>
              <BlSelect v-if="Number(m.value_source)===5" v-model="m.value_content" :options="objectParamOptions" size="sm" clearable placeholder="选对象引用参数" />
              <BlSelect v-else-if="Number(m.value_source)===6" v-model="m.value_content" :options="mainObjectProps" size="sm" clearable
                        :placeholder="mainObjectProps.length ? '选主对象属性' : '主对象无可用属性'" />
              <BlSelect v-else-if="Number(m.value_source)===7" v-model="m.value_content" :options="priorCreatedOptions" size="sm" clearable
                        :placeholder="priorCreatedOptions.length ? '选前序规则创建的对象' : '前面没有创建对象规则'" />
              <span v-else class="roe-na">不适用</span>
            </td>
            <!-- 默认值类型: 仅静态值可在「直接填值 / 来自候选来源」间切 -->
            <td>
              <BlSelect v-if="cellOn(m,'dtype')" v-model="m.default_type" :options="DEFAULT_TYPE_OPTS" size="sm" @change="m.default_source = ''" />
              <span v-else-if="cellOn(m,'dval')" class="roe-ro">静态</span>
              <span v-else class="roe-na">不适用</span>
            </td>
            <td>
              <BlSelect v-if="Number(m.value_source)===3" v-model="m.value_content" :options="USER_ATTR_OPTS" size="sm" placeholder="取用户的哪个属性" />
              <span v-else-if="Number(m.value_source)===4" class="roe-ro">当前系统时间</span>
              <template v-else-if="Number(m.value_source)===2">
                <div v-if="m.default_type === 'source'" class="roe-src" :title="m.default_source || ''" @click="openSourcePicker(m)">
                  <span class="roe-src-t" :class="{ 'is-ph': !m.default_source }">{{ sourceLabel(m) || '点击选择' }}</span>
                  <span class="roe-src-ic" v-html="BL.icon('search', 12)"></span>
                </div>
                <input v-else class="bl-input bl-input-xs" v-model="m.value_content" placeholder="静态值" />
              </template>
              <span v-else class="roe-na">不适用</span>
            </td>
            <!-- 必填由对象类型定义, 动作规则无权放宽, 这里只读回显 -->
            <td class="t-center"><span :class="['roe-req', m.is_required ? 'is-on' : '']">{{ m.is_required ? '必填' : '可选' }}</span></td>
            <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除" @click="rule.prop_mappings.splice(mi,1)" v-html="BL.icon('x', 11)"></button></td>
          </tr>
          <tr v-if="!rule.prop_mappings.length"><td :colspan="isModify ? 12 : 11" class="bl-muted" style="text-align:center;padding:12px;font-size:12px">暂无属性映射</td></tr>
        </tbody>
      </table>
      </div>
      <div v-if="isCreate && !canUseMainObject" class="roe-tip">「主对象」需要动作挂在对象详情页上才有取值上下文,当前动作未在详情页展示,该项已禁用。</div>
      <button class="fe-add-row" @click="addMapping"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">添加属性映射</span></button>
    </div>

    <!-- 3 关联链接配置 (创建对象时顺带建链接) -->
    <div v-if="rule.kind === 'create_object'" class="adw-card">
      <div class="adw-card-hd adw-card-hd-flex"><span>关联链接配置(多对多)</span><span class="bl-muted" style="font-size:11.5px;font-weight:400">创建对象同时建立多对多关联</span></div>
      <div v-for="(lk, li) in rule.obj_links" :key="li" class="fe-link-item">
        <div class="fe-link-hd">
          <span v-html="BL.icon('link', 13, '#14C9C9')"></span>
          <span class="fe-link-lbl">链接类型</span>
          <BlSelect v-model="lk.link_type_code" :options="linkTypeOptions" size="sm" clearable placeholder="选择链接类型" style="width:220px" />
          <span class="bl-tag" style="margin-left:6px">{{ linkCardLabel(lk.link_type_code) }}</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon at-del-op" title="移除链接" @click="rule.obj_links.splice(li,1)" v-html="BL.icon('trash2', 12)"></button>
        </div>
        <div class="fe-link-bd">
          <div class="fe-row"><span class="fe-lbl">本端对象</span><span class="bl-muted" style="font-size:12.5px">当前创建的{{ className || '对象' }}(本规则生成)</span></div>
          <div class="fe-row"><span class="fe-lbl">对端对象来源</span>
            <BlSelect v-model="lk.peer_param" :options="objectParamOptions" size="sm" clearable
                      :placeholder="objectParamOptions.length ? '表单参数:对端对象' : '暂无对象引用参数,请先在表单添加'" style="flex:1;max-width:320px" /></div>
        </div>
      </div>
      <div v-if="!rule.obj_links.length" class="bl-muted" style="font-size:12px;padding:8px 2px">暂无关联链接</div>
      <button class="fe-add-row" @click="addObjLink"><span v-html="BL.icon('link', 12)"></span><span style="margin-left:4px">添加多对多链接</span></button>
    </div>

    <!-- 删除类规则说明 -->
    <div v-if="rule.kind.startsWith('delete')" class="fe-warn">⚠ 删除规则只支持通过对象引用参数指定已存在的目标,不能删除本次提交中新建的临时对象;删除对象会级联移除其所有链接。</div>

    <EnumValuePickerModal v-model:open="enumPickerOpen" :subtitle="enumPickerSubtitle"
                          :model-value="parseSourceList(enumPickerRow)" @confirm="onEnumValuesPicked" />

    <ObjectTypePickerModal v-model:open="classPickerOpen" :multi="false" required
                           :model-value="rule.obj_class_id ? [rule.obj_class_id] : []" :exclude-ids="excludeClassIds"
                           :subtitle="pickerSubtitle" @confirm="onClassPicked" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import ObjectTypePickerModal from '@/components/ObjectTypePickerModal.vue'
import EnumValuePickerModal from '@/components/EnumValuePickerModal.vue'
import { VALUE_SOURCE_OPTS } from './funcParamModel.js'
import { dtLabel } from './conditionModel.js'
import { PROP_OPERATOR_OPTS, USER_ATTR_OPTS, newMapping, newObjLink } from './ruleModel.js'

const props = defineProps({
  rule: { type: Object, required: true },              // 规则对象, 就地修改
  allClasses: { type: Array, default: () => [] },      // 对象类完整行 (弹框回填名称/图标/配色)
  mainClassId: { type: String, default: '' },          // 动作主体对象类 — 创建类规则里要排除
  mainClassName: { type: String, default: '' },
  pkOptions: { type: Array, default: () => [] },       // 主键属性候选
  propOptions: { type: Array, default: () => [] },     // 该规则对象类的属性
  className: { type: String, default: '' },
  formParamOptions: { type: Array, default: () => [] },
  objectParamOptions: { type: Array, default: () => [] },
  linkTypeOptions: { type: Array, default: () => [] },
  linkCardLabel: { type: Function, default: () => '' },
  mainObjectProps: { type: Array, default: () => [] },   // 主对象(动作主体)的属性候选
  showOnDetail: { type: Number, default: 0 },            // 动作是否在对象详情页展示
  priorCreated: { type: Array, default: () => [] },      // 前序创建对象规则 [{ classId, label }]
})
const emit = defineEmits(['load-props'])

const isModify = computed(() => props.rule.kind === 'modify_object')

/* 对象类选择: 与向导同一个弹框, 选完把属性拉起来 */
const classPickerOpen = ref(false)
const KIND_LABEL = { create_object: '创建', modify_object: '修改', delete_object: '删除' }
const kindLabel = computed(() => KIND_LABEL[props.rule.kind] || '操作')
/* 主对象由动作本身创建, 规则里再建一遍同类没有意义, 故创建类规则不列出它 */
const excludeClassIds = computed(() => (isCreate.value && props.mainClassId) ? [props.mainClassId] : [])
const pickerSubtitle = computed(() =>
  `选择本规则要${kindLabel.value}的对象类${excludeClassIds.value.length ? `(已排除主对象${props.mainClassName ? '「' + props.mainClassName + '」' : ''})` : ''}`)
const pickedClass = computed(() => {
  const c = (props.allClasses || []).find(x => x.id === props.rule.obj_class_id)
  if (!c) return null
  return { cn: c.display_name || c.rdfs_label || c.api_name, api_name: c.api_name, icon: c.icon, color: c.color }
})
function onClassPicked({ ids }) {
  const id = ids?.[0] || ''
  if (!id || id === props.rule.obj_class_id) return
  props.rule.obj_class_id = id
  /* 属性来自旧对象类, 换类后一律失效 */
  props.rule.obj_pk_property = ''
  props.rule.prop_mappings = []
  emit('load-props', id)
}
function clearClass() {
  props.rule.obj_class_id = ''
  props.rule.obj_pk_property = ''
  props.rule.prop_mappings = []
}

/* 主对象 = 动作绑定的对象实例。只有「创建对象」规则用得上(在 A 的详情页上新建 B, B 的某属性取 A),
 * 且必须动作本身挂在详情页才有这个实例。 */
const isCreate = computed(() => props.rule.kind === 'create_object')
const canUseMainObject = computed(() => isCreate.value && props.showOnDetail === 1 && props.mainObjectProps.length > 0)
/* 本动作中排在当前规则之前的「创建对象」规则 — 它们的产物才可能已经建好 */
const priorCreatedOptions = computed(() => props.priorCreated.map(r => ({ value: r.classId, label: r.label })))
const valueSourceOpts = computed(() => VALUE_SOURCE_OPTS
  .filter(o => o.value !== 6 || isCreate.value)
  .map(o => {
    if (o.value === 6 && !canUseMainObject.value) return { ...o, disabled: true }
    if (o.value === 7 && !priorCreatedOptions.value.length) return { ...o, disabled: true }
    return o
  }))
/* 换赋值方式 = 换了取值语义, 把上一模式留下的残值一并清掉 */
function onSourceChange(m) {
  m.value_content = Number(m.value_source) === 3 ? 'user_id' : ''
  m.param_name = ''
  m.default_source = ''
  if (Number(m.value_source) !== 2) m.default_type = 'static'
}
/* 必填跟随属性自身的约束, 不允许在规则里放宽 */
function syncRequired(m) {
  const p = props.propOptions.find(o => o.value === m.property_code)
  m.is_required = p?.required ? 1 : 0
}
function dtOf(m) {
  const p = props.propOptions.find(o => o.value === m.property_code)
  return p?.dataType ? dtLabel(p.dataType) : ''
}
/* 选中属性的元信息 — 有它才铺「中文名 + 小字编码」两行, 否则退回纯下拉 */
function propMetaOf(m) {
  return m.property_code ? props.propOptions.find(o => o.value === m.property_code) : null
}
/* 单元格可用性 — 与向导第 2 步同一套规则 */
function cellOn(m, cell) {
  const vs = Number(m.value_source)
  if (cell === 'param') return vs === 1
  if (cell === 'dtype') return vs === 2
  if (cell === 'dval') return vs === 2 || vs === 3 || vs === 4
  return false
}
function paramNameOf(m) {
  if (!m.value_content) return ''
  const p = props.formParamOptions.find(o => o.value === m.value_content)
  /* 选项 label 是「名称 (编码)」, 取前半截即可 */
  return p ? String(p.label).replace(/\s*\([^)]*\)\s*$/, '') : (m.param_name || m.value_content)
}
function syncParamName(m) { m.param_name = paramNameOf(m) }

/* 「静态值 + 来源」: 枚举属性挑候选枚举值, 存 [{code,label}] JSON */
const DEFAULT_TYPE_OPTS = [{ value: 'static', label: '静态' }, { value: 'source', label: '来源' }]
const enumPickerOpen = ref(false)
const enumPickerRow = ref(null)
const enumPickerSubtitle = computed(() => {
  const m = enumPickerRow.value
  if (!m) return ''
  const meta = propMetaOf(m)
  return `属性 ${meta?.name || m.property_code} — 勾选的值即本规则可选范围`
})
function parseSourceList(m) {
  const raw = String(m?.default_source || '')
  if (!raw.startsWith('[')) return []
  try {
    const a = JSON.parse(raw)
    return Array.isArray(a) ? a.map(x => (x && typeof x === 'object') ? x : { code: String(x), label: String(x) }) : []
  } catch { return [] }
}
function sourceLabel(m) {
  const raw = String(m.default_source || '')
  if (!raw.startsWith('[')) return raw
  const a = parseSourceList(m)
  if (!a.length) return raw
  const first = a[0].label || a[0].code
  return a.length > 1 ? `${first} 等 ${a.length} 项` : first
}
function openSourcePicker(m) { enumPickerRow.value = m; enumPickerOpen.value = true }
function onEnumValuesPicked({ values }) {
  const m = enumPickerRow.value
  if (!m) return
  m.default_source = (values && values.length) ? JSON.stringify(values) : ''
}
function addMapping() { props.rule.prop_mappings.push(newMapping()) }
function addObjLink() { (props.rule.obj_links ||= []).push(newObjLink()) }

/* 每行只列还没被别的行占用的属性(保留本行已选的), 同一属性不会映射两次 */
function propOptionsFor(m) {
  const used = new Set(props.rule.prop_mappings.filter(x => x !== m).map(x => x.property_code).filter(Boolean))
  return props.propOptions.filter(o => !used.has(o.value))
}
/* 尚未出现在映射表里的属性 */
const unmapped = computed(() => {
  const used = new Set((props.rule.prop_mappings || []).map(m => m.property_code).filter(Boolean))
  return props.propOptions.filter(o => !used.has(o.value))
})
const unmappedCount = computed(() => unmapped.value.length)

/* 批量铺入: 值来源默认「来自参数」, 同名表单参数自动配上, 省去逐行选 */
function importAllProps() {
  const list = props.rule.prop_mappings
  /* 先清掉用户没填任何内容的空行, 免得导入后夹一堆空行 */
  for (let i = list.length - 1; i >= 0; i--) {
    const m = list[i]
    if (!m.property_code && !m.value_content) list.splice(i, 1)
  }
  unmapped.value.forEach(o => {
    const hit = props.formParamOptions.find(p => p.value === o.value || p.value === 'p_' + o.value)
    list.push({ ...newMapping(), property_code: o.value, value_content: hit ? hit.value : '' })
  })
}

/* 「创建对象」选定对象类后自动铺满: 建对象通常要给大部分属性赋值 */
watch(() => [props.rule.obj_class_id, props.propOptions.length], () => {
  if (props.rule.kind !== 'create_object') return
  if (!props.propOptions.length) return
  const list = props.rule.prop_mappings || []
  if (list.some(m => m.property_code)) return      // 已配过就不动
  importAllProps()
})
</script>

<style scoped src="./ruleEditorShared.css"></style>
<style scoped>
/* 属性下方的参数绑定引用: 只读, 参数本身在表单页维护 */
.roe-bind { font-size: 10.5px; color: var(--bl-text-3); padding: 2px 0 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.roe-dt { font-size: 11px; padding: 0 6px; height: 20px; }
.roe-auto { font-size: 12px; padding-left: 4px; }
/* 表格观感对齐向导第 2 步的属性映射矩阵 */
/* 11 列在窄抽屉里塞不下, 容器横向滚动; 抽屉拉宽后不定宽的两列自动吃掉富余 */
.roe-map-wrap { overflow-x: auto; }
.roe-map { width: 100%; min-width: 900px; font-size: 12px; table-layout: fixed; }
.roe-map thead th { background: var(--bl-thead-bg); font-weight: 600; height: 34px; padding: 0 5px; white-space: nowrap; color: var(--bl-text-1); }
.roe-map thead th.t-left { text-align: left; }
.roe-map td { padding: 3px 4px; border-top: 1px solid var(--bl-divider); vertical-align: middle; }
.roe-map td.t-center { text-align: center; }
.roe-map .bl-input-xs { height: 28px; padding: 0 6px; font-size: 12px; }
/* 属性单元格: 中文名 + 小字编码两行, 下拉透明覆盖在上面, 点哪都能换属性 */
.roe-prop-cell { position: relative; display: flex; flex-direction: column; gap: 1px; min-width: 0; padding: 3px 4px; border-radius: 6px; }
.roe-prop-cell:hover { background: var(--bl-bg-hover); }
.roe-prop-name { font-size: 12.5px; color: var(--bl-text-1); display: flex; align-items: center; gap: 5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.roe-prop-code { font-size: 11px; line-height: 1.3; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.roe-pk { flex-shrink: 0; font-size: 10px; font-weight: 600; color: #D97706; background: color-mix(in srgb, #D97706 14%, transparent); border-radius: 3px; padding: 0 4px; }
.roe-prop-cell :deep(.bs) { position: absolute; inset: 0; opacity: 0; }
.roe-prop-cell :deep(.bs-control) { height: 100%; }
/* 与向导一致: 该模式下用不到的单元格标「不适用」而不是留空 */
.roe-na { font-size: 12px; color: var(--bl-text-3); padding-left: 4px; }
.roe-ro { display: inline-block; max-width: 100%; font-size: 12px; color: var(--bl-text-2); padding-left: 4px; }
/* 「来源」候选值选择框 — 形似只读输入, 点开弹枚举多选 */
.roe-src { display: flex; align-items: center; gap: 4px; height: 28px; padding: 0 6px; cursor: pointer;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px; }
.roe-src:hover { border-color: var(--bl-primary); }
.roe-src-t { flex: 1; min-width: 0; font-size: 12px; color: var(--bl-text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.roe-src-t.is-ph { color: var(--bl-text-3); }
.roe-src-ic { flex-shrink: 0; display: inline-flex; color: var(--bl-text-3); }
.roe-tip { font-size: 11.5px; color: var(--bl-text-3); line-height: 1.6; margin-top: 8px; }
.roe-req { font-size: 11px; color: var(--bl-text-3); }
.roe-req.is-on { color: #f53f3f; font-weight: 600; }
/* 对象类选择器 — 与向导 .acw-pick 同形, 整行可点 */
.roe-pick { display: flex; align-items: center; gap: 8px; height: 34px; padding: 0 6px 0 8px; font-size: 13px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; cursor: pointer; }
.roe-pick:hover { border-color: var(--bl-primary); }
.roe-pick.is-empty { border-style: dashed; }
.roe-pick-ic { width: 22px; height: 22px; border-radius: 5px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.roe-pick-text { flex: 1; min-width: 0; color: var(--bl-text-1); }
.roe-pick-text.is-ph { color: var(--bl-text-3); }
.roe-pick-api { margin-left: 7px; font-size: 12px; }
.roe-pick-x { flex-shrink: 0; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer;
  display: inline-flex; align-items: center; padding: 3px; border-radius: 4px; }
.roe-pick-x:hover { color: #f53f3f; background: var(--bl-bg-2); }
.roe-pick-act { flex-shrink: 0; display: inline-flex; align-items: center; gap: 3px; height: 24px; padding: 0 9px;
  border-radius: 6px; background: var(--bl-bg-2); color: var(--bl-text-2); font-size: 12px; }
.roe-pick:hover .roe-pick-act { background: var(--bl-primary-soft); color: var(--bl-primary); }
</style>
