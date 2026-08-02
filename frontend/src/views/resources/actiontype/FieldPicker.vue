<template>
  <div class="fp" ref="rootEl">
    <div :class="['fp-control', 'bl-input', size === 'sm' && 'bl-input-sm', { 'is-open': open, 'is-disabled': disabled }]" @click="toggle">
      <span class="fp-sub-ic" :style="{ background: curSubjMeta.color }" v-html="BL.icon(curSubjMeta.icon, 11, '#fff')"></span>
      <span :class="['fp-value', !curLabel && 'is-ph']">{{ curLabel || placeholder }}</span>
      <span class="fp-arrow" :class="{ 'is-open': open }" v-html="BL.icon('chevronDown', 12)"></span>
    </div>

    <Teleport to="body">
      <div v-if="open" ref="popEl" class="fp-pop" :style="popStyle">
        <!-- 左: 主体分类 -->
        <div class="fp-left">
          <div v-for="s in subjectList" :key="s.key" :class="['fp-subj', tab === s.key && 'is-on']" @click="tab = s.key">
            <span class="fp-subj-ic" :style="{ background: s.color }" v-html="BL.icon(s.icon, 12, '#fff')"></span>
            <span class="bl-truncate" :title="labelOf(s)">{{ labelOf(s) }}</span>
            <span class="fp-subj-n">{{ countOf(s.key) }}</span>
          </div>
        </div>

        <!-- 右: 搜索 + 兼容开关 + 列表 -->
        <div class="fp-right">
          <div class="fp-search">
            <span class="fp-search-ic" v-html="BL.icon('search', 12)"></span>
            <input ref="searchEl" v-model="q" placeholder="搜索属性 (名称 / 编码)" @keydown.esc="close" />
          </div>
          <label class="fp-compat"><input type="checkbox" v-model="compatOnly" /> 仅显示兼容选项
            <span class="bl-muted">(可用于条件比较的类型)</span></label>
          <div class="fp-list">
            <div v-for="f in filtered" :key="f.code"
                 :class="['fp-opt', isCur(f) && 'is-sel']" @click="pick(f)">
              <span class="bl-truncate">{{ f.name }}</span>
              <span class="fp-dt bl-muted">{{ dtLabel(f.dataType ?? f.dt) }}</span>
              <span v-if="isCur(f)" class="fp-check" v-html="BL.icon('check', 13)"></span>
            </div>
            <div v-if="!filtered.length" class="fp-empty">{{ compatOnly ? '无兼容属性 (可取消勾选查看全部)' : '无匹配属性' }}</div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'
import { SUBJECTS, SUBJECT_META, USERGROUP_FIELDS, comparable, dtLabel } from './conditionModel.js'

const props = defineProps({
  subject: { type: String, default: '' },              // 当前选中项的主体
  field: { type: String, default: '' },                // 当前选中项的编码
  objectFields: { type: Array, default: () => [] },    // [{ code, name, dataType|dt }]
  userFields: { type: Array, default: () => [] },
  groupFields: { type: Array, default: () => USERGROUP_FIELDS },
  paramFields: { type: Array, default: () => [] },
  subjects: { type: Array, default: () => ['object', 'user', 'param'] },   // 允许选择的主体
  subjectLabels: { type: Object, default: () => ({}) },                    // 覆盖主体显示名, 如 object → 具体对象类名
  placeholder: { type: String, default: '选择属性' },
  size: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['pick'])

const open = ref(false)
const q = ref('')
const tab = ref(props.subjects[0] || 'object')
const compatOnly = ref(true)
const rootEl = ref(null), popEl = ref(null), searchEl = ref(null)
const popStyle = ref({})

const subjectList = computed(() => SUBJECTS.filter(s => props.subjects.includes(s.key)))
const curSubjMeta = computed(() => SUBJECT_META[props.subject] || SUBJECT_META[props.subjects[0]] || SUBJECTS[0])

function labelOf(s) { return props.subjectLabels[s.key] || s.label }
function fieldsOf(key) {
  return (key === 'user' ? props.userFields : key === 'usergroup' ? props.groupFields
    : key === 'param' ? props.paramFields : props.objectFields) || []
}
function countOf(key) { return fieldsOf(key).filter(f => !compatOnly.value || comparable(f.dataType ?? f.dt)).length }

const curLabel = computed(() => {
  if (!props.field) return ''
  const f = fieldsOf(props.subject).find(x => x.code === props.field)
  return f?.name || props.field
})
function isCur(f) { return props.subject === tab.value && props.field === f.code }

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  return fieldsOf(tab.value).filter(f => {
    if (compatOnly.value && !comparable(f.dataType ?? f.dt)) return false
    return !k || [f.name, f.code].filter(Boolean).some(s => String(s).toLowerCase().includes(k))
  })
})

function computePos() {
  const el = rootEl.value
  if (!el) return
  const r = el.getBoundingClientRect()
  const below = window.innerHeight - r.bottom
  const openUp = below < 320 && r.top > below
  popStyle.value = {
    position: 'fixed',
    left: Math.min(r.left, window.innerWidth - 484) + 'px',
    [openUp ? 'bottom' : 'top']: (openUp ? (window.innerHeight - r.top + 4) : (r.bottom + 4)) + 'px',
    zIndex: 1360,
  }
}
function toggle() { if (props.disabled) return; open.value ? close() : openPop() }
function openPop() {
  q.value = ''
  if (props.subject && props.subjects.includes(props.subject)) tab.value = props.subject
  computePos()
  open.value = true
  nextTick(() => searchEl.value?.focus())
  window.addEventListener('mousedown', onDocDown, true)
  window.addEventListener('scroll', onReposition, true)
  window.addEventListener('resize', onReposition)
}
function onReposition() { if (open.value) computePos() }
function close() {
  if (!open.value) return
  open.value = false
  window.removeEventListener('mousedown', onDocDown, true)
  window.removeEventListener('scroll', onReposition, true)
  window.removeEventListener('resize', onReposition)
}
function onDocDown(e) {
  if (rootEl.value?.contains(e.target) || popEl.value?.contains(e.target)) return
  close()
}
function pick(f) {
  emit('pick', { subject: tab.value, field: f.code, fieldName: f.name, dataType: f.dataType ?? f.dt ?? '' })
  close()
}
watch(() => q.value, () => { if (open.value) nextTick(computePos) })
onBeforeUnmount(close)
</script>

<style scoped>
.fp { position: relative; }
.fp-control { display: flex; align-items: center; gap: 6px; cursor: pointer; user-select: none; padding-right: 8px; }
.fp-control.is-disabled { cursor: not-allowed; opacity: .6; }
.fp-control.is-open { border-color: var(--bl-primary); }
.fp-sub-ic { width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.fp-value { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fp-value.is-ph { color: var(--bl-text-3); }
.fp-arrow { display: inline-flex; color: var(--bl-text-3); transition: transform .15s; flex-shrink: 0; }
.fp-arrow.is-open { transform: rotate(180deg); }

.fp-pop { width: 468px; max-width: 96vw; height: 320px; display: flex; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: 10px; box-shadow: var(--bl-shadow-3, 0 12px 32px rgba(0,0,0,.18)); overflow: hidden; }
.fp-left { flex: 0 0 132px; border-right: 1px solid var(--bl-divider); padding: 6px; overflow-y: auto; }
.fp-subj { display: flex; align-items: center; gap: 7px; padding: 7px 8px; border-radius: 6px; cursor: pointer; font-size: 12.5px; }
.fp-subj:hover { background: var(--bl-bg-hover); }
.fp-subj.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.fp-subj-ic { width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.fp-subj-n { margin-left: auto; font-size: 11px; color: var(--bl-text-3); }
.fp-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.fp-search { display: flex; align-items: center; gap: 6px; padding: 9px 12px; border-bottom: 1px solid var(--bl-divider); }
.fp-search-ic { color: var(--bl-text-3); display: inline-flex; }
.fp-search input { flex: 1; min-width: 0; border: 0; outline: none; background: transparent; font-size: 12.5px; color: var(--bl-text-1); }
.fp-compat { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); padding: 7px 12px 5px; }
.fp-compat .bl-muted { font-size: 11px; }
.fp-list { flex: 1; overflow-y: auto; padding: 4px 6px 6px; }
.fp-opt { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 6px; cursor: pointer; font-size: 12.5px; }
.fp-opt:hover { background: var(--bl-bg-hover); }
.fp-opt.is-sel { background: var(--bl-primary-soft); color: var(--bl-primary); }
.fp-opt > .bl-truncate { flex: 1; min-width: 0; }
.fp-dt { flex-shrink: 0; font-size: 11px; }
.fp-check { color: var(--bl-primary); display: inline-flex; flex-shrink: 0; }
.fp-empty { padding: 20px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
</style>
