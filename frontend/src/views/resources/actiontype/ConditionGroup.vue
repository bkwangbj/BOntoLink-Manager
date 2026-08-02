<template>
  <div class="cg" :class="[depth === 0 ? 'cg-root' : 'cg-nested', node.logic === 'any' ? 'cg-any' : 'cg-all']">
    <div class="cg-hd">
      <span class="cg-hd-txt">
        <span v-if="depth > 0" class="cg-badge">{{ depth === 1 ? '子组' : '嵌套组' }}</span>
        满足以下
        <select class="cg-logic" :class="node.logic === 'all' ? 'is-all' : 'is-any'" v-model="node.logic">
          <option v-for="l in logics" :key="l" :value="l">{{ LOGIC_LABEL[l] }}</option>
        </select>
        条件:</span>
      <button v-if="depth > 0" class="cg-x" title="删除该条件组" @click="$emit('remove')" v-html="BL.icon('x', 12)"></button>
    </div>

    <div class="cg-body">
      <div v-for="(child, i) in node.children" :key="child._k" class="cg-item"
           :draggable="dragArmed" @dragstart="onDragStart(i, $event)" @dragover.prevent="onDragOver(i)"
           @drop="onDrop(i)" @dragend="onDragEnd"
           :class="{ 'is-dragging': dragIdx === i, 'is-over': overIdx === i && dragIdx !== null && dragIdx !== i }">
        <span class="cg-grip cg-grip-item" title="拖拽排序" @mousedown="dragArmed = true" @mouseup="dragArmed = false" v-html="BL.icon('move', 12)"></span>
        <div class="cg-item-body">
          <!-- 嵌套逻辑组 -->
          <ConditionGroup v-if="child.type === 'group'" :node="child" :depth="depth + 1" v-bind="passThrough" @remove="removeChild(i)" />
          <!-- 条件行: 属性 / 运算符 均为内联下拉, 不弹框 -->
          <div v-else :class="['cg-cond', showReady && !condReady(child) && 'is-todo']">
            <FieldPicker class="cg-sel-field" size="sm" :subject="child.subject" :field="child.field" :subjects="subjects"
                         :subject-labels="subjectLabels" :object-fields="objectFields" :user-fields="userFieldList" :param-fields="paramFields"
                         @pick="f => onFieldPick(child, f)" />
            <BlSelect class="cg-sel-op" :model-value="child.operator || ''" @update:modelValue="v => child.operator = v"
                      :options="opOptions(child)" size="sm" placeholder="运算符" />
            <template v-if="needValue(child.operator)">
              <BlSelect v-if="valueOptions(child).length" class="cg-val" :model-value="child.value ?? ''"
                        @update:modelValue="v => child.value = v" :options="valueOptions(child)" size="sm" clearable placeholder="选择值" />
              <input v-else class="cg-in cg-val" v-model="child.value" placeholder="值" />
            </template>
            <span v-else class="cg-noval">该运算符无需比较值</span>
            <span v-if="showReady && !condReady(child)" class="cg-todo" title="条件未配置完整" v-html="BL.icon('info', 12)"></span>
            <button class="cg-x" title="删除条件" @click="removeChild(i)" v-html="BL.icon('trash', 12)"></button>
          </div>
        </div>
      </div>

      <div v-if="!node.children.length" class="cg-empty">暂无条件,点下方添加</div>

      <div class="cg-add">
        <span>+ 添加</span>
        <a class="cg-add-link" @click="addCond">条件</a>
        <span class="cg-add-or">或</span>
        <a class="cg-add-link" @click="addGroup">逻辑运算符</a>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import FieldPicker from './FieldPicker.vue'
import { pickOps, opsFor, needValue, condReady, condUid, USER_FIELDS } from './conditionModel.js'

defineOptions({ name: 'ConditionGroup' })
const props = defineProps({
  node: { type: Object, required: true },       // { logic, children:[] }
  depth: { type: Number, default: 0 },
  objectFields: { type: Array, default: () => [] },  // [{ code, name, dataType }]
  paramFields: { type: Array, default: () => [] },
  userFields: { type: Array, default: null },        // 不传则用内置的通用用户属性
  subjects: { type: Array, default: () => ['object', 'user', 'param'] },
  subjectLabels: { type: Object, default: () => ({}) },   // 覆盖主体显示名, 如 object → 具体对象类名
  logics: { type: Array, default: () => ['all', 'any'] },
  operators: { type: Array, default: () => ['eq', 'ne', 'regex', 'contains', 'in', 'gt', 'lt', 'ge', 'le', 'empty', 'notEmpty'] },
  valueOptionsOf: { type: Function, default: () => [] },   // (cond) => [{value,label}], 给枚举类字段候选值
  showReady: { type: Boolean, default: false },            // 标记未配置完整的条件
})
defineEmits(['remove'])

const LOGIC_LABEL = { all: '全部', any: '任一', none: '全部不' }
const userFieldList = computed(() => props.userFields || USER_FIELDS)
const OPERATORS = computed(() => pickOps(props.operators))
/* 嵌套子组透传全部配置, 保证深层条件行与顶层行为一致 */
const passThrough = computed(() => ({
  objectFields: props.objectFields, paramFields: props.paramFields, userFields: props.userFields,
  subjects: props.subjects, logics: props.logics, operators: props.operators,
  valueOptionsOf: props.valueOptionsOf, showReady: props.showReady,
}))
function valueOptions(c) { return props.valueOptionsOf(c) || [] }

function addCond() { props.node.children.push({ _k: condUid(), type: 'cond', subject: props.subjects[0] || 'object', field: '', fieldName: '', dataType: '', operator: '', value: '' }) }
function addGroup() { props.node.children.push({ _k: condUid(), type: 'group', logic: props.logics.includes('any') ? 'any' : props.logics[0], children: [] }) }
function removeChild(i) { props.node.children.splice(i, 1) }

function onFieldPick(c, f) {
  c.subject = f.subject; c.field = f.field; c.fieldName = f.fieldName; c.dataType = f.dataType
  /* 换了字段类型后, 原运算符可能不再适用(如字符串选了「大于」), 清掉让用户重选 */
  if (c.operator && !opOptions(c).some(o => o.value === c.operator)) c.operator = ''
}
function opOptions(c) { return opsFor(c.dataType, OPERATORS.value).map(o => ({ value: o.key, label: o.label })) }

/* 拖拽排序 (仅在同组内重排 children) */
const dragArmed = ref(false)
const dragIdx = ref(null)
const overIdx = ref(null)
function onDragStart(i, ev) {
  dragIdx.value = i
  if (ev && ev.dataTransfer) { ev.dataTransfer.effectAllowed = 'move'; try { ev.dataTransfer.setData('text/plain', String(i)) } catch {} }
}
function onDragOver(i) { overIdx.value = i }
function onDrop(target) {
  const from = dragIdx.value
  if (from === null || from === target) return
  const arr = props.node.children
  const [item] = arr.splice(from, 1)
  arr.splice(target, 0, item)
}
function onDragEnd() { dragIdx.value = null; overIdx.value = null; dragArmed.value = false }
</script>

<style scoped>
.cg { border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-1); }
.cg-root { border-color: var(--bl-border); }
/* 嵌套组: 彩色左边条(全部=蓝 / 任一=橙)+ 底色区分, 清晰体现"组里套组" */
.cg-nested { background: var(--bl-bg-2); border-left-width: 3px; }
.cg-nested.cg-all { border-left-color: var(--bl-primary); }
.cg-nested.cg-any { border-left-color: var(--bl-warning); }
.cg-nested > .cg-hd { background: var(--bl-bg-1); }
.cg-badge { font-size: 11px; font-weight: 600; padding: 1px 7px; border-radius: 10px; background: var(--bl-bg-3); color: var(--bl-text-2); }
.cg-nested.cg-all .cg-badge { background: var(--bl-primary-soft); color: var(--bl-primary); }
.cg-nested.cg-any .cg-badge { background: color-mix(in srgb, var(--bl-warning) 16%, transparent); color: var(--bl-warning); }
.cg-item { display: flex; align-items: stretch; gap: 4px; margin-bottom: 8px; border-radius: 6px; }
.cg-item.is-dragging { opacity: .45; }
.cg-item.is-over { outline: 2px dashed var(--bl-primary); outline-offset: 2px; }
.cg-grip-item { align-self: center; width: 18px; justify-content: center; cursor: grab; }
.cg-grip-item:active { cursor: grabbing; }
.cg-item-body { flex: 1; min-width: 0; }
.cg-hd { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-bottom: 1px solid var(--bl-divider); }
.cg-hd-txt { font-size: 13px; color: var(--bl-text-2); display: inline-flex; align-items: center; gap: 6px; }
.cg-logic { height: 26px; padding: 0 6px; border: 1px solid var(--bl-primary); border-radius: 4px; font-weight: 600; font-size: 12px; }
.cg-logic.is-all { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); }
.cg-logic.is-any { border-color: var(--bl-warning); background: color-mix(in srgb, var(--bl-warning) 14%, transparent); color: var(--bl-warning); }
.cg-body { padding: 10px 12px; }
.cg-cond { display: flex; align-items: center; gap: 6px; padding: 7px 8px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); }
.cg-grip { color: var(--bl-text-3); cursor: grab; display: inline-flex; flex-shrink: 0; }
.cg-in { height: 28px; padding: 0 8px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-1); font-size: 12px; color: var(--bl-text-1); outline: none; transition: border-color .15s, box-shadow .15s; }
.cg-in:focus { border-color: var(--bl-primary); box-shadow: 0 0 0 2px var(--bl-primary-soft); }
.cg-sel-field { flex: 0 0 200px; min-width: 0; }
.cg-sel-op { flex: 0 0 116px; min-width: 0; }
.cg-val { flex: 1; min-width: 80px; }
.cg-noval { flex: 1; color: var(--bl-text-3); font-size: 12px; }
.cg-cond.is-todo { border-style: dashed; }
.cg-todo { color: #D97706; display: inline-flex; flex-shrink: 0; }
.cg-x { flex-shrink: 0; width: 26px; height: 26px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; }
.cg-x:hover { background: var(--bl-bg-hover); color: #f53f3f; }
.cg-empty { padding: 10px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.cg-add { font-size: 13px; color: var(--bl-text-3); display: inline-flex; align-items: center; gap: 6px; padding-top: 2px; }
.cg-add-link { color: var(--bl-primary); cursor: pointer; }
.cg-add-link:hover { text-decoration: underline; }
.cg-add-or { color: var(--bl-text-3); }
</style>
