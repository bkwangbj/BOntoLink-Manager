<!--
  参数 Schema 驱动的动态表单。
  读类型类 param_json(JSON Schema)→ 渲染字段控件,v-model 输出实例参数值对象。
  支持两种 Schema 写法:
    1) 扁平:{ field: { type, enum, default, description } , ... }
    2) 标准:{ type:'object', properties:{ field:{...} }, required:[...] }
  控件:enum→下拉 / string(含 color)→ 颜色 or 文本 / number→数字 / boolean→开关 /
        object(unit_config)→单位自动配置 / object(additionalProperties)→样式映射 / 其它 object→JSON。
-->
<template>
  <div class="psf">
    <div v-if="!fields.length" class="psf-empty">该类型类无可配置参数(纯标记型),绑定后直接生效。</div>
    <div v-for="f in fields" :key="f.key" class="psf-row">
      <div class="psf-label">
        {{ f.title || cnName(f.key) }}
        <span v-if="f.required" class="psf-req">*</span>
        <span class="psf-key bl-mono">{{ f.key }}</span>
      </div>

      <!-- 枚举下拉 -->
      <FilterableSelect v-if="f.control === 'enum'" :model-value="val(f.key)" :options="enumOpts(f)"
                        :disabled="disabled" placeholder="请选择" @update:model-value="set(f.key, $event)" />
      <!-- 颜色 -->
      <ColorPickerField v-else-if="f.control === 'color'" :swatches="false" :model-value="val(f.key)"
                        @update:model-value="set(f.key, $event)" />
      <!-- 数字 -->
      <input v-else-if="f.control === 'number'" class="bl-input bl-input-sm" type="number"
             :value="val(f.key)" :disabled="disabled" @input="set(f.key, num($event.target.value))" />
      <!-- 开关 -->
      <button v-else-if="f.control === 'boolean'" type="button" class="psf-switch" :class="val(f.key) && 'is-on'"
              :disabled="disabled" @click="set(f.key, !val(f.key))"><span class="psf-switch-dot"></span></button>
      <!-- 单位自动配置 -->
      <UnitAutoConfig v-else-if="f.control === 'unit'" :model-value="objVal(f.key)" :disabled="disabled"
                      @update:model-value="set(f.key, $event)" />
      <!-- 样式映射(additionalProperties) -->
      <StyleMapEditor v-else-if="f.control === 'stylemap'" :model-value="objVal(f.key)" :schema="f.additionalProperties"
                      :disabled="disabled" @update:model-value="set(f.key, $event)" />
      <!-- 对象/兜底:JSON -->
      <CodeEditor v-else-if="f.control === 'json'" :model-value="jsonDraft(f.key)" :rows="3" :disabled="disabled"
                  @update:model-value="onJson(f.key, $event)" />
      <!-- 文本 -->
      <input v-else class="bl-input bl-input-sm" :value="val(f.key)" :disabled="disabled"
             :placeholder="f.demo || ''" @input="set(f.key, $event.target.value)" />

      <div v-if="f.description" class="psf-desc">{{ f.description }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import CodeEditor from '@/components/CodeEditor.vue'
import FilterableSelect from '../FilterableSelect.vue'
import ColorPickerField from '../ColorPickerField.vue'
import UnitAutoConfig from './UnitAutoConfig.vue'
import StyleMapEditor from './StyleMapEditor.vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
  schema:     { type: [Object, String], default: () => ({}) },
  disabled:   { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

/* 常见枚举值中文标签(下拉显示更友好) */
const ENUM_LABEL = {
  solid: '实线', dashed: '虚线', dotted: '点线',
  left: '左轴', right: '右轴',
  linear: '线性', step: '阶梯', forward: '前向填充', backward: '后向填充', none: '不插值',
  square: '方形', circle: '圆形', rounded: '圆角',
  meter: '米(m)', foot: '英尺(ft)', sea_level: '海平面',
  info: '提示', warning: '警告', error: '错误', critical: '严重',
  parent: '父子层级', group: '分组层级', admin: '行政层级',
  metric: '度量序列', log: '日志序列', event: '事件序列'
}
/* 字段名兜底中文名 */
const KEY_CN = {
  default_color: '曲线默认颜色', line_type: '曲线线型', axis_position: '归属 Y 轴',
  interpolation_type: '插值算法', is_enum: '是否枚举序列', is_deprecated: '是否弃用',
  is_inverted: 'Y 轴数值反转', subtype_code: '子类型编码', group_color: '分组节点颜色',
  group_name: '分组显示名称', intent_type: '等级类型', color: '颜色', blink: '是否闪烁高亮',
  priority: '优先级数值', unit_code: '单位编码', unit_name: '单位显示名称', precision: '小数位数',
  measure_code: '指标编码', measure_name: '指标名称', bind_series_id: '绑定时序 ID',
  min_value: '最小阈值', max_value: '最大阈值', unit: '单位', trend_enable: '显示迷你趋势图'
}
function cnName(key) { return KEY_CN[key] || key }

function parseSchema(s) {
  if (!s) return {}
  if (typeof s === 'string') { try { return JSON.parse(s) } catch { return {} } }
  return s
}
const schemaObj = computed(() => parseSchema(props.schema))

const fields = computed(() => {
  const s = schemaObj.value || {}
  const propsMap = s.properties && typeof s.properties === 'object' ? s.properties : null
  const src = propsMap || s
  const requiredArr = Array.isArray(s.required) ? s.required : []
  const out = []
  for (const [key, def] of Object.entries(src)) {
    if (!def || typeof def !== 'object' || Array.isArray(def)) continue
    // 扁平写法下跳过 schema 元关键字
    if (!propsMap && ['type', 'required', 'properties', 'additionalProperties', 'description', '$schema'].includes(key)) continue
    out.push({
      key, ...def,
      required: def.required === true || requiredArr.includes(key),
      control: controlOf(key, def),
      demo: def.default != null ? String(def.default) : ''
    })
  }
  return out
})

function controlOf(key, def) {
  const t = String(def.type || 'string').toLowerCase()
  const k = key.toLowerCase()
  if (Array.isArray(def.enum) && def.enum.length) return 'enum'
  if (t === 'boolean') return 'boolean'
  if (t === 'number' || t === 'integer') return 'number'
  if (t === 'object') {
    if (k.includes('unit_config') || def.format === 'unit_config') return 'unit'
    if (def.additionalProperties) return 'stylemap'
    return 'json'
  }
  if (k.includes('color') || def.format === 'color') return 'color'
  return 'text'
}

function enumOpts(f) {
  return (f.enum || []).map(v => ({ value: v, label: ENUM_LABEL[v] ? `${ENUM_LABEL[v]} (${v})` : String(v) }))
}

/* —— 取值/设值 —— */
function val(key) {
  const v = props.modelValue?.[key]
  if (v !== undefined && v !== null) return v
  const f = fields.value.find(x => x.key === key)
  return f && f.default !== undefined ? f.default : ''
}
function objVal(key) {
  const v = props.modelValue?.[key]
  if (v && typeof v === 'object') return v
  const f = fields.value.find(x => x.key === key)
  return (f && f.default && typeof f.default === 'object') ? f.default : {}
}
function num(v) { const n = Number(v); return isNaN(n) ? v : n }
function set(key, value) { emit('update:modelValue', { ...props.modelValue, [key]: value }) }

function jsonText(key) {
  const v = props.modelValue?.[key]
  if (v == null) return ''
  return typeof v === 'string' ? v : JSON.stringify(v, null, 2)
}
function setJson(key, text) {
  const t = (text || '').trim()
  if (!t) { set(key, undefined); return }
  try { set(key, JSON.parse(t)) } catch { set(key, t) }
}
/* json 控件本地草稿:避免受控组件在「刚好合法」时被 jsonText 重排、跳格 */
const jsonDrafts = reactive({})
function jsonDraft(key) { return jsonDrafts[key] !== undefined ? jsonDrafts[key] : jsonText(key) }
function onJson(key, v) { jsonDrafts[key] = v; setJson(key, v) }
</script>

<style scoped>
.psf { display: flex; flex-direction: column; gap: 14px; }
.psf-empty { font-size: 12.5px; color: var(--bl-text-3); padding: 10px; text-align: center; background: var(--bl-bg-2); border-radius: 6px; }
.psf-row { display: flex; flex-direction: column; gap: 5px; padding-bottom: 12px; border-bottom: 1px solid var(--bl-divider); }
.psf-row:last-child { border-bottom: 0; padding-bottom: 0; }
.psf-label { font-size: 13px; color: var(--bl-text-1); display: flex; align-items: center; gap: 6px; }
.psf-req { color: #f53f3f; }
.psf-key { font-size: 11px; color: var(--bl-text-3); }
.psf-desc { font-size: 11.5px; color: var(--bl-text-3); line-height: 1.5; }
.psf-json { resize: vertical; min-height: 60px; font-size: 12px; line-height: 1.5; }
.psf-switch { width: 40px; height: 22px; border-radius: 11px; border: 0; background: var(--bl-border-strong, #c9cdd4); position: relative; cursor: pointer; padding: 0; transition: background-color .15s; align-self: flex-start; }
.psf-switch.is-on { background: var(--bl-primary); }
.psf-switch:disabled { opacity: .5; cursor: not-allowed; }
.psf-switch-dot { position: absolute; top: 2px; left: 2px; width: 18px; height: 18px; border-radius: 50%; background: #fff; transition: transform .15s; box-shadow: 0 1px 3px rgba(0,0,0,.2); }
.psf-switch.is-on .psf-switch-dot { transform: translateX(18px); }
</style>
