<template>
  <Teleport to="body">
    <transition name="pf-fade">
      <div v-if="open" :class="['pf-mask', dm.state.free && 'is-free']" @click.self="onCancel">
        <div class="pf-modal" :class="{ 'is-max': dm.state.maximized }" :style="dm.modalStyle.value" data-dm-root>
          <div class="pf-hd" :class="{ 'is-draggable': !dm.state.maximized }" @mousedown="dm.startDrag">
            <div class="pf-hd-l">
              <div class="pf-title">
                {{ isBatch ? `批量设置格式化 (${propertyIds.length} 项)` : '属性格式化配置' }}
              </div>
              <div class="pf-sub bl-muted">{{ subtitle || '一次定义全系统生效 · 数据语义与展示规范固化' }}</div>
            </div>
            <div class="pf-hd-r" @mousedown.stop>
              <button class="bl-btn bl-btn-text bl-btn-icon"
                      :title="dm.state.maximized ? '还原' : '最大化'"
                      @click="dm.toggleMax"
                      v-html="BL.icon(dm.state.maximized ? 'minimize' : 'maximize', 13)"></button>
              <button class="bl-btn bl-btn-text bl-btn-icon" @click="onCancel" v-html="BL.icon('x', 14)"></button>
            </div>
          </div>

          <div class="pf-toggle">
            <label><input type="checkbox" v-model="form.format_enabled" :true-value="1" :false-value="0" @change="onToggleEnable" /> 启用格式化</label>
            <span class="bl-muted" style="font-size:11px;margin-left:6px">未启用时使用系统默认格式</span>
            <span v-if="isBatch" class="bl-tag bl-tag-warning" style="margin-left:auto;font-size:11px">批量模式 · 将覆盖已有配置</span>
          </div>

          <div :class="['pf-body', !form.format_enabled && 'is-disabled']">
            <!-- 左：分类列表 -->
            <aside class="pf-cats">
              <div class="pf-cats-title">分类 (C)</div>
              <button v-for="c in categories" :key="c.k"
                      :class="['pf-cat', form.format_type === c.k && 'is-on']"
                      :disabled="!form.format_enabled || (dataType && c.disabled && c.disabled(dataType))"
                      @click="onPickCategory(c.k)">{{ c.label }}</button>
            </aside>

            <!-- 右：示例 + 配置 -->
            <section class="pf-main">
              <div class="pf-preview">
                <div class="pf-preview-lbl">示例</div>
                <div class="pf-preview-val">{{ preview }}</div>
              </div>

              <!-- 数值 -->
              <div v-if="form.format_type === 'number' || form.format_type === 'percent' || form.format_type === 'scientific'" class="pf-cfg">
                <FieldRow label="小数位数" inline><input type="number" class="bl-input" min="0" max="10" v-model.number="form.decimal_places" /></FieldRow>
                <FieldRow v-if="form.format_type === 'number'" label="千位分隔符" inline>
                  <label><input type="checkbox" v-model="form.use_thousand_sep" :true-value="1" :false-value="0" /> 使用千位分隔符 (,)</label>
                </FieldRow>
                <FieldRow v-if="form.format_type === 'percent'" label="自动 ×100" inline>
                  <label><input type="checkbox" v-model="form.percent_auto_multiply" :true-value="1" :false-value="0" /> 原始值自动乘以 100</label>
                </FieldRow>
                <FieldRow v-if="form.format_type === 'number'" label="负数显示" inline>
                  <select class="bl-input" v-model.number="form.negative_mode">
                    <option :value="0">红色括号 (1,234.10)</option>
                    <option :value="1">黑色括号 (1,234.10)</option>
                    <option :value="2">红色无符号 1,234.10</option>
                    <option :value="3">黑色负号 -1,234.10</option>
                    <option :value="4">红色负号 -1,234.10</option>
                  </select>
                </FieldRow>
              </div>

              <!-- 货币 / 会计 -->
              <div v-if="form.format_type === 'currency' || form.format_type === 'accounting'" class="pf-cfg">
                <FieldRow label="小数位数" inline><input type="number" class="bl-input" min="0" max="10" v-model.number="form.decimal_places" /></FieldRow>
                <FieldRow label="货币符号" inline>
                  <select class="bl-input" v-model="form.currency_symbol">
                    <option>¥</option><option>$</option><option>€</option><option>£</option><option>US$</option><option>无</option>
                  </select>
                </FieldRow>
                <FieldRow v-if="form.format_type === 'accounting'" label="左对齐" inline>
                  <label><input type="checkbox" v-model="form.accounting_align" :true-value="1" :false-value="0" /> 货币符号左对齐</label>
                </FieldRow>
              </div>

              <!-- 日期 -->
              <div v-if="form.format_type === 'date'" class="pf-cfg">
                <FieldRow label="格式模板" inline>
                  <select class="bl-input" v-model="form.date_pattern">
                    <option>yyyy-MM-dd</option>
                    <option>yyyy/MM/dd</option>
                    <option>yyyy年MM月dd日</option>
                    <option>MM/dd/yyyy</option>
                    <option>dd/MM/yyyy</option>
                    <option>yyyy-MM-dd HH:mm:ss</option>
                    <option>yyyy/MM/dd HH:mm</option>
                    <option>yyyy.MM.dd</option>
                  </select>
                </FieldRow>
                <FieldRow label="区域" inline>
                  <select class="bl-input" v-model="form.locale">
                    <option v-for="l in locales" :key="l.code" :value="l.code">{{ l.name }} {{ l.code }}</option>
                  </select>
                </FieldRow>
              </div>

              <!-- 时间 -->
              <div v-if="form.format_type === 'time'" class="pf-cfg">
                <FieldRow label="格式模板" inline>
                  <select class="bl-input" v-model="form.time_pattern">
                    <option>HH:mm:ss</option>
                    <option>HH:mm</option>
                    <option>h:mm AM/PM</option>
                    <option>h:mm:ss AM/PM</option>
                    <option>HH 时 mm 分</option>
                    <option>HH 时 mm 分 ss 秒</option>
                    <option>下午 h:mm</option>
                    <option>HH:mm:ss.SSS</option>
                  </select>
                </FieldRow>
                <FieldRow label="区域" inline>
                  <select class="bl-input" v-model="form.locale">
                    <option v-for="l in locales" :key="l.code" :value="l.code">{{ l.name }} {{ l.code }}</option>
                  </select>
                </FieldRow>
              </div>

              <!-- 分数 -->
              <div v-if="form.format_type === 'fraction'" class="pf-cfg">
                <FieldRow label="分数类型" inline>
                  <select class="bl-input" v-model="form.fraction_type">
                    <option># ?/?</option>
                    <option># ??/??</option>
                    <option># ???/???</option>
                    <option># ?/2</option>
                    <option># ?/4</option>
                    <option># ?/8</option>
                    <option># ?/10</option>
                    <option># ?/16</option>
                    <option># ?/100</option>
                  </select>
                </FieldRow>
              </div>

              <!-- 文本 -->
              <div v-if="form.format_type === 'text'" class="pf-cfg">
                <FieldRow label="强制文本" inline>
                  <label><input type="checkbox" v-model="form.text_force" :true-value="1" :false-value="0" /> 以文本方式处理数字</label>
                </FieldRow>
                <FieldRow label="最大长度" inline><input type="number" class="bl-input" v-model.number="form.text_max_length" placeholder="不限制" /></FieldRow>
                <FieldRow label="正则校验"><input class="bl-input bl-mono" v-model="form.text_regex" placeholder="例: ^[A-Z]{2}\d{6}$" /></FieldRow>
              </div>

              <!-- 特殊 -->
              <div v-if="form.format_type === 'special'" class="pf-cfg">
                <FieldRow label="特殊类型" inline>
                  <select class="bl-input" v-model="form.special_type">
                    <option value="zipcode">邮政编码</option>
                    <option value="lowerChinese">中文小写</option>
                    <option value="upperChinese">中文大写</option>
                    <option value="rmbUpper">人民币大写</option>
                    <option value="wanUnit">万为单位</option>
                    <option value="plusMinus">正负号</option>
                  </select>
                </FieldRow>
              </div>

              <!-- 自定义 -->
              <div v-if="form.format_type === 'custom'" class="pf-cfg">
                <FieldRow label="格式代码">
                  <div class="bl-row" style="gap:6px;flex:1">
                    <input class="bl-input bl-mono" v-model="form.custom_format" placeholder="G/通用格式" />
                    <button class="bl-btn bl-btn-sm bl-btn-text" title="清空" @click="form.custom_format=''" v-html="BL.icon('trash', 11)"></button>
                  </div>
                </FieldRow>
                <div class="pf-custom-list">
                  <div class="bl-muted" style="font-size:11px;margin:6px 0 4px">内置格式代码</div>
                  <button v-for="c in customCodes" :key="c.code" class="pf-custom-item" @click="form.custom_format = c.code">
                    <span class="bl-mono">{{ c.code }}</span>
                    <span class="bl-muted">{{ c.desc }}</span>
                  </button>
                </div>
              </div>

              <!-- 常规无配置 -->
              <div v-if="form.format_type === 'general'" class="bl-muted" style="padding:20px;text-align:center">
                常规单元格格式不包含任何特定的数字格式。
              </div>

              <div class="pf-bottom-note">{{ categoryDesc[form.format_type] || '' }}</div>
            </section>
          </div>

          <div class="pf-ft">
            <div class="pf-ft-l">
              <select class="bl-input" v-model="preset" @change="onPickPreset" style="width:200px">
                <option value="">预设模板 ▾</option>
                <option v-for="p in presets" :key="p.k" :value="p.k">{{ p.label }}</option>
              </select>
              <button class="bl-btn bl-btn-sm bl-btn-text" @click="resetCurrent">重置为默认</button>
            </div>
            <div class="pf-ft-r">
              <button class="bl-btn" @click="onCancel">取消</button>
              <button class="bl-btn" @click="onApply">应用</button>
              <button class="bl-btn bl-btn-primary" @click="onSave">确定</button>
            </div>
          </div>

          <!-- 八向缩放热区 -->
          <DraggableHandles v-if="!dm.state.maximized" :on="dm.startResize" />
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { propertyFormatApi } from '@/api'
import { useDraggableModal } from '@/lib/useDraggableModal.js'
import DraggableHandles from '@/components/DraggableHandles.vue'

/* 拖动 / 最大化 / 八向缩放 */
const dm = useDraggableModal({ minWidth: 600, minHeight: 420 })
import FieldRow from '@/views/config/category/FieldRow.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  propertyId: { type: String, default: '' },
  propertyIds: { type: Array, default: () => [] },   // 批量模式: 不为空时优先生效
  propertyScope: { type: String, default: 'class' }, // class | interface
  dataType: { type: String, default: '' },           // xsd 类型,用于过滤分类
  subtitle: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'saved', 'cancel'])

const isBatch = computed(() => Array.isArray(props.propertyIds) && props.propertyIds.length > 1)

/* 数据类型分类过滤 (按 spec §8 数据类型过滤):
   string  → 隐藏 数值/货币/会计专用/百分比/分数/科学记数
   date    → 隐藏 数值/货币/会计专用/百分比/分数/科学记数/时间
   boolean → 只显示 常规/文本/自定义 (其余全部禁用) */
const STR_DT = new Set(['xsd:string', 'string'])
const DATE_DT = new Set(['xsd:date', 'xsd:dateTime', 'date', 'datetime'])
const BOOL_DT = new Set(['xsd:boolean', 'boolean'])
const isStr  = dt => STR_DT.has(dt)
const isDate = dt => DATE_DT.has(dt)
const isBool = dt => BOOL_DT.has(dt)
const disabledForNumeric = dt => isStr(dt) || isDate(dt) || isBool(dt)
const categories = [
  { k: 'general',    label: '常规',     disabled: () => false },
  { k: 'number',     label: '数值',     disabled: disabledForNumeric },
  { k: 'currency',   label: '货币',     disabled: disabledForNumeric },
  { k: 'accounting', label: '会计专用', disabled: disabledForNumeric },
  { k: 'date',       label: '日期',     disabled: dt => isStr(dt) || isBool(dt) },
  { k: 'time',       label: '时间',     disabled: dt => isStr(dt) || isBool(dt) || dt === 'xsd:date' || dt === 'date' },
  { k: 'percent',    label: '百分比',   disabled: disabledForNumeric },
  { k: 'fraction',   label: '分数',     disabled: disabledForNumeric },
  { k: 'scientific', label: '科学记数', disabled: disabledForNumeric },
  { k: 'text',       label: '文本',     disabled: () => false },
  { k: 'special',    label: '特殊',     disabled: dt => isBool(dt) },
  { k: 'custom',     label: '自定义',   disabled: () => false }
]

/* spec §6 常用区域设置对照表 (12 项) */
const locales = [
  { code: 'zh-CN', name: '中文 (中国)' },
  { code: 'zh-TW', name: '中文 (台湾)' },
  { code: 'zh-HK', name: '中文 (香港)' },
  { code: 'en-US', name: '英语 (美国)' },
  { code: 'en-GB', name: '英语 (英国)' },
  { code: 'en-AU', name: '英语 (澳大利亚)' },
  { code: 'en-CA', name: '英语 (加拿大)' },
  { code: 'en-SG', name: '英语 (新加坡)' },
  { code: 'ja-JP', name: '日语 (日本)' },
  { code: 'ko-KR', name: '韩语 (韩国)' },
  { code: 'de-DE', name: '德语 (德国)' },
  { code: 'fr-FR', name: '法语 (法国)' }
]

/* 自定义分类: 内置格式代码列表 (spec §5 自定义) */
const customCodes = [
  { code: 'G/通用格式',  desc: '通用 / 自动判断' },
  { code: '0',           desc: '整数,无小数' },
  { code: '0.00',        desc: '保留 2 位小数' },
  { code: '#,##0',       desc: '千位分隔,整数' },
  { code: '#,##0.00',    desc: '千位分隔,2 位小数' },
  { code: '0.00%',       desc: '百分比,2 位' },
  { code: '0.00E+00',    desc: '科学记数' },
  { code: 'yyyy-MM-dd',  desc: '日期 ISO 格式' }
]
const categoryDesc = {
  general:    '常规：不包含任何特定的数字格式。',
  number:     '数值格式：用于一般数字的表示。',
  currency:   '货币格式：在数字前后附加货币符号，便于金额展示。',
  accounting: '会计专用：货币符号左对齐，便于报表列对齐。',
  date:       '日期格式：标准化日期展示与导出。',
  time:       '时间格式：精确到分/秒。',
  percent:    '百分比：原始值自动乘 100 并附加 %。',
  fraction:   '分数：以分数形式呈现非整数值。',
  scientific: '科学记数：大数/小数的紧凑表示。',
  text:       '文本：强制以文本方式处理；可校验长度与正则。',
  special:    '特殊：邮政编码 / 中文大小写 / 人民币大写 等。',
  custom:     '自定义：手工编辑格式代码（参考 Excel 自定义格式语法）。'
}
const presets = [
  { k: 'water-num',  label: '水利通用数值',  apply: f => Object.assign(f, { format_type: 'number',   decimal_places: 2, use_thousand_sep: 0, negative_mode: 3 }) },
  { k: 'eng-money',  label: '工程投资 (货币)', apply: f => Object.assign(f, { format_type: 'currency', decimal_places: 2, use_thousand_sep: 1, currency_symbol: '¥' }) },
  { k: 'monitor-ts', label: '监测时间',        apply: f => Object.assign(f, { format_type: 'date',     date_pattern: 'yyyy-MM-dd HH:mm:ss' }) },
  { k: 'pct-1',      label: '百分比 (1 位)',  apply: f => Object.assign(f, { format_type: 'percent',  decimal_places: 1, percent_auto_multiply: 1 }) },
  { k: 'code-text',  label: '设施编号 (强制文本)', apply: f => Object.assign(f, { format_type: 'text', text_force: 1 }) },
]

const defaultForm = () => ({
  format_enabled: 0,
  format_type: 'general',
  decimal_places: 2,
  use_thousand_sep: 0,
  negative_mode: 3,
  currency_symbol: '¥',
  accounting_align: 1,
  date_pattern: 'yyyy-MM-dd',
  time_pattern: 'HH:mm:ss',
  locale: 'zh-CN',
  fraction_type: '# ?/?',
  special_type: 'zipcode',
  custom_format: 'G/通用格式',
  text_force: 0,
  text_max_length: null,
  text_regex: '',
  percent_auto_multiply: 1
})
const form = reactive(defaultForm())
const preset = ref('')

watch(() => props.open, async (v) => {
  if (!v) { dm.reset(); return }
  dm.reset()  // 每次打开恢复默认尺寸
  preset.value = ''
  Object.assign(form, defaultForm())
  // 批量模式: 不预读单条配置,使用全默认值 (用户的设置将覆盖到所有选中项)
  if (isBatch.value) return
  if (props.propertyId) {
    try {
      const r = await propertyFormatApi.get(props.propertyId)
      if (r && r.format_id) {
        Object.assign(form, r)
        form.format_enabled = Number(form.format_enabled ? 1 : 0)
      }
    } catch {}
  }
})

/* spec §2: 启用格式化时根据 data_type 自动选中默认分类 */
function defaultCategoryForDataType(dt) {
  if (!dt) return 'general'
  if (isStr(dt))  return 'text'
  if (isBool(dt)) return 'general'
  if (dt === 'xsd:dateTime' || dt === 'datetime') return 'date'
  if (isDate(dt)) return 'date'
  // integer/decimal/double/float 等数值类型
  if (['xsd:integer','xsd:int','xsd:long','xsd:short','xsd:decimal','xsd:double','xsd:float',
       'integer','decimal','double','float','int','long'].includes(dt)) return 'number'
  return 'general'
}
function onToggleEnable(e) {
  // 由 0 → 1 时,自动套用数据类型对应的默认分类
  const enabled = e?.target?.checked ?? !!form.format_enabled
  if (enabled) {
    const k = defaultCategoryForDataType(props.dataType)
    // 若当前已是一个有效分类则保留,否则切到默认
    const def = categories.find(c => c.k === form.format_type)
    if (!def || (def.disabled && def.disabled(props.dataType))) {
      form.format_type = k
    }
  }
}

function onPickCategory(k) { if (form.format_enabled) form.format_type = k }
function onPickPreset() {
  if (!preset.value) return
  const p = presets.find(x => x.k === preset.value)
  if (p) { p.apply(form); form.format_enabled = 1 }
}
function resetCurrent() {
  const t = form.format_type
  Object.assign(form, defaultForm())
  form.format_type = t
  form.format_enabled = 1
}

/* 实时预览 */
const preview = computed(() => {
  if (!form.format_enabled) return '1234.10'
  switch (form.format_type) {
    case 'general':    return '1234.10'
    case 'number': {
      const v = Number(1234.1)
      let s = v.toFixed(form.decimal_places ?? 2)
      if (form.use_thousand_sep) {
        const parts = s.split('.')
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
        s = parts.join('.')
      }
      return s
    }
    case 'currency':   return (form.currency_symbol || '¥') + (1234.1).toFixed(form.decimal_places ?? 2)
    case 'accounting': return (form.currency_symbol || '¥') + '    ' + (1234.1).toFixed(form.decimal_places ?? 2)
    case 'date':       return form.date_pattern.replace('yyyy','2026').replace('MM','05').replace('dd','27').replace('HH','14').replace('mm','35').replace('ss','22')
    case 'time':       return form.time_pattern.replace('HH','14').replace('mm','35').replace('ss','22')
    case 'percent':    return ((form.percent_auto_multiply ? 12.34 : 0.1234).toFixed(form.decimal_places ?? 1)) + '%'
    case 'fraction':   return '1234 1/10'
    case 'scientific': return '1.234E+03'
    case 'text':       return form.text_force ? '"1234"' : '1234.10'
    case 'special': {
      switch (form.special_type) {
        case 'zipcode':       return '100086'
        case 'lowerChinese':  return '一千二百三十四点一'
        case 'upperChinese':  return '壹仟贰佰叁拾肆点壹'
        case 'rmbUpper':      return '壹仟贰佰叁拾肆元壹角'
        case 'wanUnit':       return '0.1234 万'
        case 'plusMinus':     return '+1234.10'
        default: return '1234.10'
      }
    }
    case 'custom':     return form.custom_format
    default: return '1234.10'
  }
})

async function persist() {
  if (isBatch.value) {
    // 批量模式: 转交后端循环 upsert
    const { format_id, property_id, ...payload } = form
    const r = await propertyFormatApi.batchSave(props.propertyIds, payload, props.propertyScope)
    BL.info(`已应用 ${r?.total ?? props.propertyIds.length} 项 (新增 ${r?.created ?? 0} · 更新 ${r?.updated ?? 0})`)
    return true
  }
  if (!props.propertyId) return false
  await propertyFormatApi.save(props.propertyId, { ...form, property_scope: props.propertyScope })
  return true
}
async function onApply() {
  if (await persist()) BL.success('已保存（继续编辑）')
}
async function onSave() {
  if (await persist()) { BL.success('已保存'); emit('saved', { ...form }); emit('update:open', false) }
}
function onCancel() { emit('cancel'); emit('update:open', false) }
</script>

<style scoped>
/* z-index 必须高于:对象类型详情抽屉(1000) + 属性详情抽屉(1010) + 抽屉 resize 把手(1011),
   并低于全局 toast/confirm。设为 1200 保证模态完整覆盖于所有抽屉之上 */
.pf-mask { position: fixed; inset: 0; background: rgba(0,0,0,.40); z-index: 1200;
  display: flex; align-items: center; justify-content: center; }
/* free 模式: 关闭 flex 居中,让弹框按显式 fixed 坐标定位 */
.pf-mask.is-free { display: block; }
.pf-modal { position: relative; background: var(--bl-bg-1); border-radius: 12px;
  width: 800px; max-width: 95vw; height: calc(100vh - 120px); min-height: 520px;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 12px 40px rgba(0,0,0,.20); }
.pf-modal.is-max { box-shadow: none; }
.pf-hd { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); user-select: none; }
.pf-hd.is-draggable { cursor: move; }
.pf-hd.is-draggable:active { cursor: grabbing; }
.pf-hd-l { min-width: 0; flex: 1; }
.pf-hd-r { display: inline-flex; align-items: center; gap: 2px; flex-shrink: 0; margin-left: 12px; }
.pf-title { font-size: 15px; font-weight: 600; }
.pf-sub { font-size: 12px; }
.pf-toggle { padding: 10px 16px; background: var(--bl-bg-2); border-bottom: 1px solid var(--bl-divider); font-size: 13px; }
.pf-body { flex: 1; display: grid; grid-template-columns: 130px 1fr; gap: 0; overflow: hidden; }
.pf-body.is-disabled { opacity: .55; pointer-events: none; }
.pf-cats { border-right: 1px solid var(--bl-divider); padding: 6px 4px; overflow: auto; background: var(--bl-bg-2); display: flex; flex-direction: column; gap: 2px; }
.pf-cats-title { font-size: 11px; color: var(--bl-text-3); padding: 4px 8px 6px; }
.pf-cat { text-align: left; padding: 7px 12px; border: 0; background: transparent; cursor: pointer;
  font-size: 13px; color: var(--bl-text-2); border-radius: 4px; border-left: 3px solid transparent; }
.pf-cat:hover:not(:disabled) { background: var(--bl-bg-1); color: var(--bl-text-1); }
.pf-cat.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 500; border-left-color: var(--bl-primary); }
.pf-cat:disabled { opacity: .4; cursor: not-allowed; }

.pf-main { display: flex; flex-direction: column; padding: 12px 16px; overflow: auto; }
.pf-preview {
  padding: 14px; background: var(--bl-bg-2); border-radius: 6px; margin-bottom: 12px;
  display: flex; align-items: center; gap: 12px;
}
.pf-preview-lbl { font-size: 11px; color: var(--bl-text-3); }
.pf-preview-val { font-size: 16px; font-weight: 600; color: var(--bl-text-1); font-family: var(--bl-font-mono); }

.pf-cfg { display: flex; flex-direction: column; gap: 4px; }
.pf-bottom-note { margin-top: auto; padding: 10px 0 0; font-size: 11.5px; color: var(--bl-text-3); border-top: 1px dashed var(--bl-divider); margin-top: 14px; }

.pf-ft { display: flex; justify-content: space-between; padding: 10px 16px; border-top: 1px solid var(--bl-divider); gap: 8px; }
.pf-ft-l, .pf-ft-r { display: flex; gap: 8px; align-items: center; }

.pf-fade-enter-active, .pf-fade-leave-active { transition: opacity .15s; }
.pf-fade-enter-from, .pf-fade-leave-to { opacity: 0; }

/* 自定义分类: 内置格式代码列表 */
.pf-custom-list { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; margin-top: 4px; }
.pf-custom-item {
  display: flex; justify-content: space-between; align-items: center; gap: 8px;
  padding: 6px 10px; border: 1px solid var(--bl-divider); border-radius: 4px;
  background: var(--bl-bg-1); cursor: pointer; font-size: 12px;
  text-align: left;
}
.pf-custom-item:hover { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.pf-custom-item .bl-mono { color: var(--bl-text-1); }
.pf-custom-item .bl-muted { font-size: 11px; }

.pf-toggle { display: flex; align-items: center; }
</style>
