<template>
  <div class="cg" :class="[depth === 0 ? 'cg-root' : 'cg-nested', node.logic === 'any' ? 'cg-any' : 'cg-all']">
    <div class="cg-hd">
      <span class="cg-hd-txt">
        <span v-if="depth > 0" class="cg-badge">{{ depth === 1 ? '子组' : '嵌套组' }}</span>
        满足以下
        <select class="cg-logic" :class="node.logic === 'any' ? 'is-any' : 'is-all'" v-model="node.logic"><option value="all">全部</option><option value="any">任一</option></select>
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
          <ConditionGroup v-if="child.type === 'group'" :node="child" :depth="depth + 1" :object-fields="objectFields" :param-fields="paramFields" @remove="removeChild(i)" />
          <!-- 条件行 -->
          <div v-else class="cg-cond">
            <div class="cg-chip cg-chip-field" @click="openField(child)">
              <span class="cg-sub-ic" :style="{ background: subjColor(child.subject) }" v-html="BL.icon(subjIcon(child.subject), 11, '#fff')"></span>
              <span :class="{ 'cg-ph': !child.field }" class="bl-truncate">{{ fieldLabel(child) }}</span>
            </div>
            <div class="cg-chip cg-chip-op" @click="openOp(child)">
              <span :class="{ 'cg-ph': !child.operator }">{{ opLabel(child.operator) }}</span>
            </div>
            <input v-if="!NO_VALUE_OPS.includes(child.operator)" class="cg-in cg-val" v-model="child.value" placeholder="值" />
            <span v-else class="cg-noval">—</span>
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

    <ConditionPicker v-model:open="fieldPickerOpen" mode="field" :object-fields="objectFields" :param-fields="paramFields"
                     :current="pickerChild && pickerChild.field" @pick="onFieldPick" />
    <ConditionPicker v-model:open="opPickerOpen" mode="operator"
                     :data-type="pickerChild && pickerChild.dataType" :current="pickerChild && pickerChild.operator" @pick="onOpPick" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { BL } from '@/lib/bl.js'
import ConditionPicker from './ConditionPicker.vue'

defineOptions({ name: 'ConditionGroup' })
const props = defineProps({
  node: { type: Object, required: true },       // { logic:'all'|'any', children:[] }
  depth: { type: Number, default: 0 },
  objectFields: { type: Array, default: () => [] },  // [{ code, name }]
  paramFields: { type: Array, default: () => [] },
})
defineEmits(['remove'])

const OPERATORS = { eq: '等于', ne: '不等于', gt: '大于', lt: '小于', ge: '大于等于', le: '小于等于', contains: '包含', in: '包含于', regex: '匹配正则', empty: '为空', notempty: '非空' }
const NO_VALUE_OPS = ['empty', 'notempty']
const USER_FIELDS = [{ code: 'user_group', name: '所属用户组' }, { code: 'username', name: '用户名' }, { code: 'role', name: '角色' }, { code: 'org', name: '所属组织' }]

let seq = 0
function uid() { return 'cn-' + Date.now().toString(36) + '-' + (seq++) }
function addCond() { props.node.children.push({ _k: uid(), type: 'cond', subject: 'object', field: '', operator: 'eq', value: '' }) }
function addGroup() { props.node.children.push({ _k: uid(), type: 'group', logic: 'any', children: [] }) }
function removeChild(i) { props.node.children.splice(i, 1) }

/* 主体图标/配色 + 字段/运算符显示 */
function subjIcon(s) { return s === 'user' ? 'user' : s === 'param' ? 'edit' : 'box' }
function subjColor(s) { return s === 'user' ? '#722ED1' : s === 'param' ? '#00B42A' : '#165DFF' }
function fieldLabel(child) {
  if (!child.field) return '选择属性'
  if (child.fieldName) return child.fieldName
  const list = child.subject === 'user' ? USER_FIELDS : child.subject === 'param' ? props.paramFields : props.objectFields
  const f = (list || []).find(x => x.code === child.field)
  return f ? f.name : child.field
}
function opLabel(op) { return OPERATORS[op] || '运算符' }

/* 弹窗选择器 */
const fieldPickerOpen = ref(false)
const opPickerOpen = ref(false)
const pickerChild = ref(null)
function openField(child) { pickerChild.value = child; fieldPickerOpen.value = true }
function openOp(child) { pickerChild.value = child; opPickerOpen.value = true }
function onFieldPick(res) {
  const c = pickerChild.value; if (!c) return
  c.subject = res.subject; c.field = res.field; c.fieldName = res.fieldName; c.dataType = res.dataType
}
function onOpPick(res) { const c = pickerChild.value; if (c) c.operator = res.operator }

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
.cg-sub-ic { width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.cg-in { height: 28px; padding: 0 8px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-1); font-size: 12px; color: var(--bl-text-1); outline: none; transition: border-color .15s, box-shadow .15s; }
.cg-in:focus { border-color: var(--bl-primary); box-shadow: 0 0 0 2px var(--bl-primary-soft); }
.cg-chip { display: inline-flex; align-items: center; gap: 6px; height: 28px; padding: 0 8px; border: 1px solid var(--bl-border); border-radius: 4px; background: var(--bl-bg-1); cursor: pointer; font-size: 12px; color: var(--bl-text-1); min-width: 0; }
.cg-chip:hover { border-color: var(--bl-primary); }
.cg-chip-field { max-width: 200px; }
.cg-chip-op { min-width: 76px; justify-content: center; }
.cg-ph { color: var(--bl-text-3); }
.cg-val { flex: 1; min-width: 80px; }
.cg-noval { flex: 1; color: var(--bl-text-3); font-size: 12px; }
.cg-x { flex-shrink: 0; width: 26px; height: 26px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; }
.cg-x:hover { background: var(--bl-bg-hover); color: #f53f3f; }
.cg-empty { padding: 10px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.cg-add { font-size: 13px; color: var(--bl-text-3); display: inline-flex; align-items: center; gap: 6px; padding-top: 2px; }
.cg-add-link { color: var(--bl-primary); cursor: pointer; }
.cg-add-link:hover { text-decoration: underline; }
.cg-add-or { color: var(--bl-text-3); }
</style>
