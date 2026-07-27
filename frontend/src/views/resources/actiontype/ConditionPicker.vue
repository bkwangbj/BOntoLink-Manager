<template>
  <Teleport to="body">
    <transition name="cp-fade">
      <div v-if="open" class="cp-mask" @click.self="close">
        <div class="cp-modal">
          <div class="cp-hd">
            <span class="cp-title">{{ mode === 'operator' ? '选择运算符' : '选择属性' }}</span>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- 字段选择: 左主体 + 右属性 -->
          <div v-if="mode === 'field'" class="cp-body cp-body-field">
            <div class="cp-left">
              <div v-for="s in SUBJECTS" :key="s.key"
                   :class="['cp-subj', subject === s.key && 'is-on']" @click="subject = s.key">
                <span class="cp-subj-ic" :style="{ background: s.color }" v-html="BL.icon(s.icon, 12, '#fff')"></span>
                {{ s.label }}
              </div>
            </div>
            <div class="cp-right">
              <div class="cp-search">
                <span class="cp-search-ic" v-html="BL.icon('search', 12)"></span>
                <input v-model="q" placeholder="搜索属性 (名称 / 编码)" />
              </div>
              <label class="cp-compat"><input type="checkbox" v-model="fieldCompatOnly" /> 仅显示兼容选项<span class="bl-muted" style="margin-left:4px">(可用于条件比较的类型)</span></label>
              <div class="cp-list">
                <div v-for="f in filteredFields" :key="f.code"
                     :class="['cp-opt', current === f.code && 'is-sel']" @click="pickField(f)">
                  <span class="bl-truncate">{{ f.name }}</span>
                  <span class="cp-dt bl-muted">{{ dtLabel(f.dataType ?? f.dt) }}</span>
                  <span v-if="current === f.code" class="cp-check" v-html="BL.icon('check', 13)"></span>
                </div>
                <div v-if="!filteredFields.length" class="cp-empty">{{ fieldCompatOnly ? '无兼容属性 (可取消勾选查看全部)' : '无匹配属性' }}</div>
              </div>
            </div>
          </div>

          <!-- 运算符选择 -->
          <div v-else class="cp-body cp-body-op">
            <div class="cp-search">
              <span class="cp-search-ic" v-html="BL.icon('search', 12)"></span>
              <input v-model="q" placeholder="搜索运算符" />
            </div>
            <label class="cp-compat"><input type="checkbox" v-model="compatOnly" /> 仅显示兼容运算符</label>
            <div class="cp-list">
              <div v-for="o in filteredOps" :key="o.key"
                   :class="['cp-opt', current === o.key && 'is-sel']" @click="pickOp(o)">
                <span>{{ o.label }}</span>
                <span v-if="current === o.key" class="cp-check" v-html="BL.icon('check', 13)"></span>
              </div>
              <div v-if="!filteredOps.length" class="cp-empty">无匹配运算符</div>
            </div>
          </div>

          <div class="cp-ft"><span style="flex:1"></span><button class="bl-btn" @click="close">取消</button></div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  open: Boolean,
  mode: { type: String, default: 'field' },   // 'field' | 'operator'
  objectFields: { type: Array, default: () => [] },   // [{code,name,dataType?}]
  paramFields: { type: Array, default: () => [] },
  dataType: { type: String, default: '' },     // 运算符模式: 当前字段数据类型
  current: { type: String, default: '' },
})
const emit = defineEmits(['update:open', 'pick'])

const SUBJECTS = [
  { key: 'object', label: '对象', icon: 'box', color: '#165DFF' },
  { key: 'user', label: '用户属性', icon: 'user', color: '#722ED1' },
  { key: 'param', label: '参数', icon: 'edit', color: '#00B42A' },
]
const USER_FIELDS = [{ code: 'user_group', name: '所属用户组', dt: 'string' }, { code: 'username', name: '用户名', dt: 'string' }, { code: 'role', name: '角色', dt: 'string' }, { code: 'org', name: '所属组织', dt: 'string' }]
const OPERATORS = [
  { key: 'eq', label: '等于', dt: 'any' }, { key: 'ne', label: '不等于', dt: 'any' },
  { key: 'regex', label: '匹配正则', dt: 'string' }, { key: 'contains', label: '包含', dt: 'string' },
  { key: 'in', label: '包含于', dt: 'any' }, { key: 'gt', label: '大于', dt: 'num' },
  { key: 'lt', label: '小于', dt: 'num' }, { key: 'ge', label: '大于等于', dt: 'num' }, { key: 'le', label: '小于等于', dt: 'num' },
  { key: 'empty', label: '为空', dt: 'any' }, { key: 'notempty', label: '非空', dt: 'any' },
]
const q = ref('')
const subject = ref('object')
const compatOnly = ref(true)         // 运算符兼容
const fieldCompatOnly = ref(true)    // 属性兼容 (仅可比较标量类型)

/* 可用于条件比较的标量类型: 排除对象引用/结构/注解等 */
function comparable(dt) { const s = String(dt || '').toLowerCase(); return !/(object|ref|struct|annotation|entity)/.test(s) }
function dtLabel(dt) {
  const s = String(dt || '').toLowerCase()
  if (!s) return ''
  if (/(int|decimal|double|float|number|numeric)/.test(s)) return '数值'
  if (s.includes('bool')) return '布尔'
  if (s.includes('datetime') || s.includes('timestamp')) return '日期时间'
  if (s.includes('date')) return '日期'
  if (s.includes('enum')) return '枚举'
  if (/(object|ref|entity)/.test(s)) return '对象引用'
  if (s.includes('text') || s.includes('clob')) return '长文本'
  return '字符串'
}

const fieldsOfSubject = computed(() => {
  if (subject.value === 'user') return USER_FIELDS
  if (subject.value === 'param') return props.paramFields || []
  return props.objectFields || []
})
const filteredFields = computed(() => {
  const k = q.value.trim().toLowerCase()
  return fieldsOfSubject.value.filter(f => {
    if (fieldCompatOnly.value && !comparable(f.dataType ?? f.dt)) return false
    return !k || [f.name, f.code].filter(Boolean).some(s => String(s).toLowerCase().includes(k))
  })
})
function numLike(dt) { const s = String(dt || '').toLowerCase(); return /(int|decimal|double|float|number|numeric|date|time)/.test(s) }
const filteredOps = computed(() => {
  const k = q.value.trim().toLowerCase()
  const isNum = numLike(props.dataType)
  return OPERATORS.filter(o => {
    if (compatOnly.value && o.dt === 'num' && !isNum) return false
    if (compatOnly.value && o.dt === 'string' && isNum) return false
    return !k || o.label.toLowerCase().includes(k)
  })
})

function pickField(f) {
  emit('pick', { subject: subject.value, field: f.code, fieldName: f.name, dataType: f.dataType || f.dt || '' })
  close()
}
function pickOp(o) { emit('pick', { operator: o.key, label: o.label }); close() }
function close() { emit('update:open', false) }

watch(() => props.open, v => { if (v) { q.value = ''; if (props.mode === 'field') subject.value = 'object' } })
</script>

<style scoped>
.cp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.5); backdrop-filter: blur(3px); z-index: 1350; display: flex; align-items: center; justify-content: center; }
:root[data-theme="dark"] .cp-mask { background: rgba(0,0,0,.62); }
.cp-modal { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 12px; width: 560px; max-width: 96vw; height: 440px; max-height: 88vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 24px 56px rgba(0,0,0,0.5); }
.cp-hd { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.cp-title { font-size: 15px; font-weight: 600; }
.cp-body { flex: 1; min-height: 0; }
.cp-body-field { display: flex; }
.cp-body-op { display: flex; flex-direction: column; padding: 10px 12px; }
.cp-left { flex: 0 0 150px; border-right: 1px solid var(--bl-divider); padding: 8px 6px; overflow-y: auto; }
.cp-subj { display: flex; align-items: center; gap: 8px; padding: 8px 8px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.cp-subj:hover { background: var(--bl-bg-hover); }
.cp-subj.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.cp-subj-ic { width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.cp-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.cp-search { display: flex; align-items: center; gap: 6px; padding: 10px 12px; border-bottom: 1px solid var(--bl-divider); }
.cp-search-ic { color: var(--bl-text-3); display: inline-flex; }
.cp-search input { flex: 1; border: 0; outline: none; background: transparent; font-size: 13px; color: var(--bl-text-1); }
.cp-compat { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); padding: 8px 2px; }
.cp-list { flex: 1; overflow-y: auto; padding: 6px; }
.cp-opt { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.cp-opt:hover { background: var(--bl-bg-hover); }
.cp-opt.is-sel { background: var(--bl-primary-soft); color: var(--bl-primary); }
.cp-opt > .bl-truncate { flex: 1; min-width: 0; }
.cp-dt { flex-shrink: 0; font-size: 11px; }
.cp-check { color: var(--bl-primary); display: inline-flex; }
.cp-empty { padding: 20px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.cp-ft { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }
.cp-fade-enter-active, .cp-fade-leave-active { transition: opacity .15s; }
.cp-fade-enter-from, .cp-fade-leave-to { opacity: 0; }
</style>
