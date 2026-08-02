<template>
  <div class="roe">
    <div class="fe-info"><span v-html="BL.icon('info', 14, '#165DFF')"></span><span>规则按从上到下顺序依次执行,后行的规则会覆盖前序规则对同一对象同属性的修改。拖拽卡片可调整规则顺序。</span></div>

    <!-- 1 基础配置 -->
    <div class="adw-card"><div class="adw-card-hd">基础配置</div>
      <div class="adw-grid">
        <template v-if="!rule.kind.includes('link')">
          <label class="adw-fld"><span class="adw-lbl">对象类型 <i>*</i></span>
            <BlSelect v-model="rule.obj_class_id" :options="classOptions" clearable placeholder="选择对象类型" @update:modelValue="v => emit('load-props', v)" />
          </label>
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
      <table class="bl-table ate-mini-table roe-map">
        <thead><tr>
          <th class="t-left">属性</th><th class="t-left">数据类型</th>
          <th v-if="isModify" class="t-left">操作</th>
          <th class="t-left">赋值方式</th><th class="t-left">取值配置</th><th class="t-center">必填</th><th class="t-center"></th>
        </tr></thead>
        <tbody>
          <tr v-for="(m, mi) in rule.prop_mappings" :key="mi">
            <!-- 属性是每行的主体; 绑定的参数作为只读引用挂在下面, 参数本身在表单页维护 -->
            <td>
              <BlSelect v-if="propOptions.length" v-model="m.property_code" :options="propOptionsFor(m)" size="sm" clearable placeholder="选择属性" @change="syncRequired(m)" />
              <input v-else class="bl-input bl-input-xs bl-mono" v-model="m.property_code" placeholder="属性编码" />
              <div v-if="Number(m.value_source)===1 && m.value_content" class="roe-bind bl-mono" title="该属性取自这个表单参数">↳ {{ m.value_content }}</div>
            </td>
            <td><span v-if="dtOf(m)" class="bl-tag roe-dt">{{ dtOf(m) }}</span><span v-else class="bl-muted" style="font-size:11px">—</span></td>
            <td v-if="isModify"><BlSelect v-model="m.prop_operator" :options="PROP_OPERATOR_OPTS" size="sm" /></td>
            <td><BlSelect v-model="m.value_source" :options="valueSourceOpts" size="sm" @change="onSourceChange(m)" /></td>
            <td>
              <BlSelect v-if="Number(m.value_source)===1" v-model="m.value_content" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" />
              <BlSelect v-else-if="Number(m.value_source)===5" v-model="m.value_content" :options="objectParamOptions" size="sm" clearable placeholder="选对象引用参数" />
              <BlSelect v-else-if="Number(m.value_source)===6" v-model="m.value_content" :options="mainObjectProps" size="sm" clearable
                        :placeholder="mainObjectProps.length ? '选主对象属性' : '主对象无可用属性'" />
              <BlSelect v-else-if="Number(m.value_source)===7" v-model="m.value_content" :options="priorCreatedOptions" size="sm" clearable
                        :placeholder="priorCreatedOptions.length ? '选前序规则创建的对象' : '前面没有创建对象规则'" />
              <BlSelect v-else-if="Number(m.value_source)===3" v-model="m.value_content" :options="USER_ATTR_OPTS" size="sm" placeholder="取用户的哪个属性" />
              <input v-else-if="Number(m.value_source)===2" class="bl-input bl-input-xs" v-model="m.value_content" placeholder="静态值" />
              <span v-else class="bl-muted roe-auto">提交时的服务器时间</span>
            </td>
            <!-- 必填由对象类型定义, 动作规则无权放宽, 这里只读回显 -->
            <td class="t-center"><span :class="['roe-req', m.is_required ? 'is-on' : '']">{{ m.is_required ? '必填' : '可选' }}</span></td>
            <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除" @click="rule.prop_mappings.splice(mi,1)" v-html="BL.icon('x', 11)"></button></td>
          </tr>
          <tr v-if="!rule.prop_mappings.length"><td :colspan="isModify ? 7 : 6" class="bl-muted" style="text-align:center;padding:12px;font-size:12px">暂无属性映射</td></tr>
        </tbody>
      </table>
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
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import { VALUE_SOURCE_OPTS } from './funcParamModel.js'
import { dtLabel } from './conditionModel.js'
import { PROP_OPERATOR_OPTS, USER_ATTR_OPTS, newMapping, newObjLink } from './ruleModel.js'

const props = defineProps({
  rule: { type: Object, required: true },              // 规则对象, 就地修改
  classOptions: { type: Array, default: () => [] },    // 对象类候选
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
function onSourceChange(m) {
  m.value_content = Number(m.value_source) === 3 ? 'user_id' : ''
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
.roe-map td { vertical-align: top; padding-top: 5px; }
.roe-tip { font-size: 11.5px; color: var(--bl-text-3); line-height: 1.6; margin-top: 8px; }
.roe-req { font-size: 11px; color: var(--bl-text-3); }
.roe-req.is-on { color: #f53f3f; font-weight: 600; }
</style>
