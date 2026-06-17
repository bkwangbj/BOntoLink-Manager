<template>
  <!-- 燕尾角抽屉：触发框下方悬浮(燕尾指向左/中/右随窗口自适应,避免遮挡) -->
  <div class="ixf-root" :style="floatStyle" v-click-outside="onCancel">
    <span class="ixf-beak" :style="{ left: beakLeft + 'px' }"></span>

    <!-- 顶部标题栏 -->
    <div class="ixf-hd">
      <span class="ixf-title">筛选：{{ field?.label }}</span>
      <span class="ixf-help" v-html="BL.icon('help', 14)" title="组内多条件可用 与/或 组合，括号用于分组高亮"></span>
    </div>

    <!-- 条件编辑区 -->
    <div class="ixf-body">
      <div v-for="(c, idx) in rows" :key="idx" class="ixf-row" :class="rowActive===idx && 'is-active'" @click="rowActive=idx">
        <!-- 左括号 -->
        <button class="ixf-bracket" :class="c.lb && 'is-on'" @click.stop="c.lb=!c.lb">(</button>
        <!-- 操作符 -->
        <select class="ixf-op bl-input bl-input-sm" v-model="c.op">
          <option v-for="o in operators" :key="o.v" :value="o.v">{{ o.t }}</option>
        </select>
        <!-- 值控件 -->
        <div class="ixf-val">
          <template v-if="needsValue(c.op)">
            <!-- 枚举/选项 -->
            <template v-if="dataType==='enum' && options.length">
              <select v-if="isSingleOp(c.op)" class="bl-input bl-input-sm" v-model="c.value">
                <option value="" disabled>请选择</option>
                <option v-for="op in options" :key="op" :value="op">{{ op }}</option>
              </select>
              <div v-else class="ixf-multi" @click.stop="toggleMulti(idx, $event)">
                <span class="bl-truncate">{{ multiText(c) || '请选择' }}</span>
                <span v-html="BL.icon('chevronDown', 12)"></span>
              </div>
            </template>
            <!-- 介于：双输入 -->
            <template v-else-if="c.op==='between'">
              <input class="bl-input bl-input-sm" :type="htmlType" v-bind="inputAttrs" v-model="c.value" :placeholder="ph" @focus="openYearPicker(idx, 'value', $event)" @blur="onYearBlur" />
              <span class="ixf-tilde">~</span>
              <input class="bl-input bl-input-sm" :type="htmlType" v-bind="inputAttrs" v-model="c.value2" :placeholder="ph" @focus="openYearPicker(idx, 'value2', $event)" @blur="onYearBlur" />
            </template>
            <!-- 单输入 -->
            <input v-else class="bl-input bl-input-sm" :type="htmlType" v-bind="inputAttrs" v-model="c.value" :placeholder="ph" @focus="openYearPicker(idx, 'value', $event)" @blur="onYearBlur" />
          </template>
          <span v-else class="ixf-noval">—</span>
        </div>
        <!-- 右括号 -->
        <button class="ixf-bracket" :class="c.rb && 'is-on'" @click.stop="c.rb=!c.rb">)</button>
        <!-- 逻辑运算(非最后一行) -->
        <div class="ixf-logic" v-if="idx < rows.length-1">
          <label><input type="radio" :name="'lg'+idx" value="AND" v-model="groupLogic" /> 与</label>
          <label><input type="radio" :name="'lg'+idx" value="OR" v-model="groupLogic" /> 或</label>
        </div>
        <div class="ixf-logic-sp" v-else></div>
        <!-- 删除 -->
        <button class="ixf-del" :disabled="rows.length<=1" @click.stop="removeRow(idx)" v-html="BL.icon('x', 13)"></button>
      </div>

      <!-- 新增条件(改为“+”) -->
      <button class="ixf-add" title="新增条件" @click="addRow"><span v-html="BL.icon('plus', 15)"></span></button>

      <!-- 括号不匹配提示 -->
      <div v-if="bracketErr" class="ixf-err"><span v-html="BL.icon('alert', 13, '#f53f3f')"></span>{{ bracketErr }}</div>
    </div>

    <!-- 底部操作栏 -->
    <div class="ixf-ft">
      <button class="bl-btn bl-btn-sm bl-btn-text ixf-clear" @click="clearAll" v-html="iconText('trash','清空')"></button>
      <div class="bl-row" style="gap:8px">
        <button class="bl-btn bl-btn-sm" @click="onCancel">取消</button>
        <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onConfirm">确定</button>
      </div>
    </div>

    <!-- 多选下拉(固定定位,避免被滚动区裁切遮挡) -->
    <div v-if="openMulti >= 0 && rows[openMulti]" class="ixf-multi-pop" :style="multiPopStyle" @click.stop>
      <label v-for="op in options" :key="op" class="ixf-multi-item">
        <input type="checkbox" :value="op" v-model="rows[openMulti].values" /> {{ op }}
      </label>
    </div>

    <!-- 年份选择面板(年份输入框聚焦时弹出,固定定位) -->
    <div v-if="yearPicker.open" class="ixf-year-pop" :style="yearPicker.style" @mousedown.prevent @click.stop>
      <div class="ixf-year-hd">
        <button class="ixf-year-nav" title="上一组" @click="yearBase -= 12" v-html="BL.icon('chevronLeft', 14)"></button>
        <span class="ixf-year-range">{{ yearBase }} - {{ yearBase + 11 }}</span>
        <button class="ixf-year-nav" title="下一组" @click="yearBase += 12" v-html="BL.icon('chevronRight', 14)"></button>
      </div>
      <div class="ixf-year-grid">
        <button v-for="y in yearCells" :key="y" class="ixf-year-cell" :class="{ 'is-on': String(y) === String(curYearVal) }" @click="pickYear(y)">{{ y }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  field: { type: Object, required: true },        // { field, label, dataType }
  options: { type: Array, default: () => [] },    // 枚举可选值
  model: { type: Object, default: null },         // { logic, conditions:[{op,value,value2,values,lb,rb}] }
  anchor: { type: Object, default: null }         // { left, top } 触发框定位
})
const emit = defineEmits(['confirm', 'cancel'])

const dataType = computed(() => props.field?.dataType || 'string')
function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

/* —— 操作符表(按类型) —— */
const OPS = {
  string: [['eq','等于'],['ne','不等于'],['startsWith','开头是'],['notStartsWith','开头不是'],['endsWith','结尾是'],['notEndsWith','结尾不是'],['contains','包含'],['notContains','不包含'],['empty','为空'],['notEmpty','不为空']],
  enum:   [['eq','等于'],['ne','不等于'],['contains','包含'],['notContains','不包含'],['empty','为空'],['notEmpty','不为空']],
  int:    [['eq','等于'],['ne','不等于'],['gt','大于'],['lt','小于'],['gte','大于等于'],['lte','小于等于'],['between','介于'],['empty','为空'],['notEmpty','不为空']],
  decimal:[['eq','等于'],['ne','不等于'],['gt','大于'],['lt','小于'],['gte','大于等于'],['lte','小于等于'],['between','介于'],['empty','为空'],['notEmpty','不为空']],
  date:   [['eq','等于'],['ne','不等于'],['after','在之后'],['before','在之前'],['between','介于'],['empty','为空'],['notEmpty','不为空']],
  datetime:[['eq','等于'],['ne','不等于'],['after','在之后'],['before','在之前'],['between','介于'],['empty','为空'],['notEmpty','不为空']],
  time:   [['eq','等于'],['ne','不等于'],['after','晚于'],['before','早于'],['between','介于'],['empty','为空'],['notEmpty','不为空']],
}
/* —— 日期粒度识别:按字段语义判断要 年 / 年月 / 年月日 / 年月日时分,通用处理 —— */
const dateKind = computed(() => {
  const f = props.field || {}
  const s = ((f.label || '') + ' ' + (f.field || '')).toLowerCase()
  if (f.dataType === 'datetime') return 'datetime'
  if (f.dataType === 'date') return 'date'
  // int/string 存储但语义是时间的字段
  if (/年月|月份/.test(s) || /\bmonth\b/.test(s)) return 'month'
  if (/年份|年度/.test(s) || /year/.test(s) || (f.dataType === 'int' && /年/.test(s))) return 'year'
  return null
})
// 算子取值类型:年→数值算子;年月/年月日→日期算子
const opType = computed(() => {
  const k = dateKind.value
  if (k === 'year') return 'int'
  if (k === 'month' || k === 'date') return 'date'
  if (k === 'datetime') return 'datetime'
  return dataType.value
})
const operators = computed(() => (OPS[opType.value] || OPS.string).map(([v, t]) => ({ v, t })))

const htmlType = computed(() => {
  switch (dateKind.value) {
    case 'year': return 'number'
    case 'month': return 'month'
    case 'date': return 'date'
    case 'datetime': return 'datetime-local'
  }
  switch (dataType.value) {
    case 'int': case 'decimal': return 'number'
    case 'date': return 'date'
    case 'datetime': return 'datetime-local'
    case 'time': return 'time'
    default: return 'text'
  }
})
// 年份用数值框时限制为 4 位年范围
const inputAttrs = computed(() => dateKind.value === 'year' ? { min: 1000, max: 9999, step: 1 } : {})
const ph = computed(() => {
  switch (dateKind.value) {
    case 'year': return 'YYYY'
    case 'month': return 'YYYY-MM'
    case 'date': return 'YYYY-MM-DD'
    case 'datetime': return 'YYYY-MM-DD HH:mm'
  }
  switch (dataType.value) {
    case 'int': case 'decimal': return '请输入数值'
    case 'time': return 'HH:mm:ss'
    default: return '请输入关键词'
  }
})

function needsValue(op) { return op !== 'empty' && op !== 'notEmpty' }
function isSingleOp(op) { return op === 'eq' || op === 'ne' }

/* —— 行数据 —— */
function blankRow() {
  return { op: operators.value[0].v, value: '', value2: '', values: [], lb: false, rb: false }
}
const rows = ref([])
const groupLogic = ref('AND')
const rowActive = ref(0)
const openMulti = ref(-1)
const multiPopStyle = ref({})
const bracketErr = ref('')
function toggleMulti(idx, e) {
  if (openMulti.value === idx) { openMulti.value = -1; return }
  openMulti.value = idx
  const r = e.currentTarget.getBoundingClientRect()
  const popH = Math.min(220, options.value.length * 30 + 12)
  const below = r.bottom + 4
  const top = (below + popH > window.innerHeight - 8) ? (r.top - popH - 4) : below
  multiPopStyle.value = { left: r.left + 'px', top: Math.max(8, top) + 'px', width: Math.max(160, r.width) + 'px' }
}

/* —— 年份选择面板(年份输入框聚焦弹出,12 年一页) —— */
const yearPicker = ref({ open: false, idx: -1, key: 'value', style: {} })
const yearBase = ref(2020)
const yearCells = computed(() => Array.from({ length: 12 }, (_, i) => yearBase.value + i))
const curYearVal = computed(() => {
  const r = rows.value[yearPicker.value.idx]
  return r ? r[yearPicker.value.key] : ''
})
function openYearPicker(idx, key, e) {
  if (dateKind.value !== 'year') return
  const v = parseInt(rows.value[idx]?.[key], 10) || new Date().getFullYear()
  yearBase.value = Math.floor(v / 12) * 12
  const r = e.target.getBoundingClientRect()
  const popH = 232, popW = 232
  const below = r.bottom + 4
  const top = (below + popH > window.innerHeight - 8) ? (r.top - popH - 4) : below
  let left = r.left
  if (left + popW > window.innerWidth - 8) left = window.innerWidth - popW - 8
  yearPicker.value = { open: true, idx, key, style: { left: Math.max(8, left) + 'px', top: Math.max(8, top) + 'px', width: popW + 'px' } }
}
function pickYear(y) {
  const r = rows.value[yearPicker.value.idx]
  if (r) r[yearPicker.value.key] = String(y)
  yearPicker.value.open = false
}
// 点面板内已 mousedown.prevent 保持输入框焦点,blur 只在真正离开时触发 → 关闭面板
function onYearBlur() { yearPicker.value.open = false }

function initFromModel() {
  if (props.model && Array.isArray(props.model.conditions) && props.model.conditions.length) {
    rows.value = props.model.conditions.map(c => ({
      op: c.op || operators.value[0].v,
      value: c.value ?? '',
      value2: c.value2 ?? '',
      values: Array.isArray(c.values) ? [...c.values] : (Array.isArray(c.value) ? [...c.value] : []),
      lb: !!c.lb, rb: !!c.rb
    }))
    groupLogic.value = props.model.logic || 'AND'
  } else {
    rows.value = [blankRow()]
    groupLogic.value = 'AND'
  }
}
initFromModel()
watch(() => props.field, initFromModel)

function addRow() { rows.value.push(blankRow()); bracketErr.value = '' }
function removeRow(i) { if (rows.value.length > 1) rows.value.splice(i, 1); bracketErr.value = '' }
function clearAll() { rows.value = [blankRow()]; groupLogic.value = 'AND'; bracketErr.value = '' }

function multiText(c) { return (c.values || []).join('、') }

/* —— 浮层定位(跟随触发框,左/右溢出时整体回收,燕尾指向触发点) —— */
const PANEL_W = 560
const floatPos = computed(() => {
  const a = props.anchor
  if (!a) return { left: 0, top: 0 }
  const vw = typeof window !== 'undefined' ? window.innerWidth : 1280
  const left = Math.max(8, Math.min(a.left, vw - PANEL_W - 8))
  return { left, top: a.top }
})
const floatStyle = computed(() => ({ left: floatPos.value.left + 'px', top: floatPos.value.top + 'px' }))
// 燕尾相对面板左缘的偏移:指向触发点中心,夹在面板内
const beakLeft = computed(() => {
  const a = props.anchor
  if (!a) return 40
  const center = a.left + 16
  return Math.max(14, Math.min(center - floatPos.value.left, PANEL_W - 28))
})
/* —— 括号匹配校验(确定前) —— */
function checkBrackets() {
  let depth = 0
  for (const r of rows.value) {
    if (r.lb) depth++
    if (r.rb) depth--
    if (depth < 0) return '括号不匹配:出现多余的“)”'
  }
  if (depth !== 0) return '括号不匹配:“(”与“)”数量不一致'
  return ''
}

/* —— 输出：构造后端筛选组 { field, logic, conditions:[{field,op,value,value2}] } —— */
function buildGroup() {
  const conds = []
  for (const r of rows.value) {
    const leaf = { field: props.field.field, op: r.op }
    if (needsValue(r.op)) {
      if (dataType.value === 'enum' && !isSingleOp(r.op) && r.values?.length) {
        // 枚举多选: 包含→in / 不包含→notIn, 值为数组
        leaf.op = r.op === 'notContains' ? 'notIn' : 'in'
        leaf.value = r.values
      } else if (r.op === 'between') {
        leaf.value = r.value; leaf.value2 = r.value2
      } else {
        leaf.value = r.value
      }
    }
    // 保留 UI 状态(回填用)
    leaf.values = r.values; leaf.lb = r.lb; leaf.rb = r.rb
    conds.push(leaf)
  }
  return { field: props.field.field, label: props.field.label, dataType: dataType.value, logic: groupLogic.value, conditions: conds }
}

function onConfirm() {
  // 括号完整性校验
  const be = checkBrackets()
  if (be) { bracketErr.value = be; return }
  bracketErr.value = ''
  // 过滤掉完全空的条件(需要值却没填)
  const g = buildGroup()
  g.conditions = g.conditions.filter(c => !needsValue(c.op) || c.value !== '' && c.value != null || (Array.isArray(c.value) && c.value.length))
  if (!g.conditions.length) { emit('cancel'); return }
  emit('confirm', g)
}
function onCancel() { emit('cancel') }

/* click-outside 指令(局部) */
const vClickOutside = {
  mounted(el, binding) {
    el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }
    setTimeout(() => document.addEventListener('mousedown', el.__h), 0)
  },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}
</script>

<style scoped>
.ixf-root {
  position: fixed;
  width: 560px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 8px;
  box-shadow: 0 12px 40px rgba(0,0,0,.16);
  z-index: 1300;
  display: flex; flex-direction: column;
}
.ixf-beak {
  position: absolute; top: -8px; left: 40px;
  width: 14px; height: 14px;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  border-top: 1px solid var(--bl-border);
  transform: rotate(45deg);
}
.ixf-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border-bottom: 1px solid var(--bl-divider);
}
.ixf-title { font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.ixf-help { color: var(--bl-text-3); cursor: help; display: inline-flex; }

.ixf-body { padding: 10px 14px; max-height: 360px; overflow: auto; display: flex; flex-direction: column; gap: 8px; }
.ixf-row {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 6px; border-radius: 6px;
  border: 1px solid transparent;
}
.ixf-row.is-active { background: var(--bl-primary-soft); border-color: var(--bl-primary-border); }
.ixf-bracket {
  width: 24px; height: 28px; flex-shrink: 0;
  border: 1px solid var(--bl-border); background: var(--bl-bg-1);
  border-radius: 4px; color: var(--bl-text-3); font-weight: 700; cursor: pointer;
}
.ixf-bracket.is-on { color: #f53f3f; border-color: #f53f3f; background: #fff1f0; }
.ixf-op { width: 96px; flex-shrink: 0; }
.ixf-val { flex: 1; min-width: 0; display: flex; align-items: center; gap: 6px; }
.ixf-val .bl-input { flex: 1; min-width: 0; }
.ixf-tilde { color: var(--bl-text-3); }
.ixf-noval { color: var(--bl-text-3); font-size: 12px; padding-left: 4px; }
.ixf-logic { display: flex; gap: 8px; flex-shrink: 0; font-size: 12px; color: var(--bl-text-2); }
.ixf-logic label { display: inline-flex; align-items: center; gap: 2px; cursor: pointer; }
.ixf-logic-sp { width: 56px; flex-shrink: 0; }
.ixf-del {
  width: 24px; height: 24px; flex-shrink: 0;
  border: 0; background: transparent; border-radius: 4px;
  color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.ixf-del:hover:not(:disabled) { background: #fff1f0; color: #f53f3f; }
.ixf-del:disabled { opacity: .35; cursor: not-allowed; }

.ixf-multi {
  position: relative; flex: 1; min-width: 0;
  display: flex; align-items: center; gap: 4px;
  padding: 4px 8px; height: 28px;
  border: 1px solid var(--bl-border); border-radius: 4px;
  background: var(--bl-bg-1); cursor: pointer; font-size: 12px;
}
.ixf-multi span:first-child { flex: 1; min-width: 0; }
/* 固定定位:脱离滚动区,避免被裁切/遮挡 */
.ixf-multi-pop {
  position: fixed;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0,0,0,.16); z-index: 1400; padding: 6px; max-height: 220px; overflow: auto;
}
.ixf-multi-item { display: flex; align-items: center; gap: 6px; padding: 5px 6px; font-size: 12px; cursor: pointer; border-radius: 4px; }
.ixf-multi-item:hover { background: var(--bl-bg-hover); }

/* “+” 新增条件 */
.ixf-add {
  width: 100%; padding: 6px; border: 1px dashed var(--bl-border);
  background: transparent; border-radius: 6px; color: var(--bl-text-2);
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center; font-size: 12px;
}
.ixf-add:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.ixf-err { display: flex; align-items: center; gap: 5px; font-size: 11.5px; color: #f53f3f; padding: 2px 2px 0; }

.ixf-ft {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; border-top: 1px solid var(--bl-divider);
}
.ixf-clear { color: #f53f3f; }

/* 年份选择面板 */
.ixf-year-pop {
  position: fixed; z-index: 1400;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0,0,0,.16); padding: 8px;
}
.ixf-year-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 2px 4px 8px; border-bottom: 1px solid var(--bl-divider); margin-bottom: 6px;
}
.ixf-year-range { font-size: 12.5px; font-weight: 600; color: var(--bl-text-1); }
.ixf-year-nav {
  width: 24px; height: 24px; border: 0; background: transparent; border-radius: 5px;
  color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.ixf-year-nav:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }
.ixf-year-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.ixf-year-cell {
  height: 32px; border: 1px solid transparent; background: var(--bl-bg-2);
  border-radius: 6px; font-size: 12.5px; color: var(--bl-text-1); cursor: pointer;
  transition: background-color .12s, color .12s, border-color .12s;
}
.ixf-year-cell:hover { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixf-year-cell.is-on { background: var(--bl-primary); color: #fff; border-color: var(--bl-primary); }
</style>
