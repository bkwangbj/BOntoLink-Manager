<template>
  <Teleport to="body">
    <div v-if="open" class="ovm-mask" @click.self="tryClose">
      <div class="ovm-modal">
        <div class="ovm-hd">
          <span>{{ isNew ? '添加覆盖规则' : '编辑覆盖规则' }}</span>
          <span class="bl-muted" style="font-size:12px;font-weight:400;margin-left:8px">{{ paramName }}</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="tryClose" v-html="BL.icon('x', 14)"></button>
        </div>

        <div class="ovm-body">
          <!-- 左 60%: If 条件 + Then 动作 -->
          <div class="ovm-left">
            <div class="ovm-tag ovm-tag-if">If</div>
            <OverrideCondGroup :node="draft.cond" :selected="selCond" @select="selectCond" />

            <div class="ovm-tag ovm-tag-then">Then</div>
            <div class="ovm-acts">
              <div v-for="(a, ai) in draft.actions" :key="a._k"
                   :class="['ovm-act', isRedundant(a, defaults) && 'is-redundant']">
                <span class="ovm-act-lbl">设为</span>
                <BlSelect v-model="a.type" :options="ACTION_OPTS" size="sm" style="width:112px" @change="onActTypeChange(a)" />
                <template v-if="boolAction(a.type)">
                  <span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': Number(a.value) === 1 }"
                        @click="a.value = Number(a.value) ? 0 : 1"><span class="adw-showsw-dot"></span></span>
                  <span class="bl-muted" style="font-size:12px">{{ Number(a.value) ? '是' : '否' }}</span>
                </template>
                <input v-else class="bl-input bl-input-sm" style="flex:1;min-width:0" v-model="a.value"
                       :placeholder="a.type === 'default' ? '默认值内容' : '约束说明 / 正则'" />
                <span v-if="isRedundant(a, defaults)" class="ovm-redundant" title="与该字段的全局默认配置一致, 此覆盖不产生任何变化">与默认值相同,配置冗余</span>
                <span style="flex:1"></span>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除覆盖动作"
                        @click="draft.actions.splice(ai,1)" v-html="BL.icon('x', 11)"></button>
              </div>
              <div v-if="!draft.actions.length" class="ovm-empty-line">暂无覆盖动作,点下方添加</div>
              <a class="ovm-add" @click="addAction">+ 添加覆盖</a>
            </div>
          </div>

          <!-- 右 40%: 线性四步条件编辑 -->
          <div class="ovm-right">
            <div class="ovm-step-hd">
              <button v-if="step !== 'template'" class="ovm-back" @click="stepBack">
                <span v-html="BL.icon('chevronLeft', 12)"></span><span>返回</span>
              </button>
              <span class="ovm-step-title">{{ STEP_TITLE[step] }}</span>
              <span style="flex:1"></span>
              <button v-if="selCond" class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="取消选中"
                      @click="selCond = null; step = 'template'" v-html="BL.icon('x', 13)"></button>
            </div>

            <div v-if="!selCond" class="ovm-step-body ovm-hint">
              在左侧点击「+ 添加条件」新建条件,或点击已有条件进行编辑。
            </div>

            <!-- 步骤 1: 选择条件模板 -->
            <div v-else-if="step === 'template'" class="ovm-step-body">
              <div class="fd-warn" style="margin-bottom:12px">覆盖条件仅可引用表单中位于当前字段<b>上方</b>的参数,禁止引用后续字段,避免循环依赖。</div>
              <div v-for="s in OV_SUBJECTS" :key="s.key" class="ovm-tpl" @click="pickSubject(s.key)">
                <span class="ovm-tpl-ic" :style="{ background: s.color }" v-html="BL.icon(s.icon, 13, '#fff')"></span>
                <div style="min-width:0">
                  <div class="ovm-tpl-lbl">{{ s.label }}</div>
                  <div class="ovm-tpl-desc">{{ s.desc }}</div>
                </div>
                <span style="flex:1"></span>
                <span class="bl-muted" v-html="BL.icon('chevronRight', 12)"></span>
              </div>
            </div>

            <!-- 步骤 2: 选择属性 -->
            <div v-else-if="step === 'field'" class="ovm-step-body">
              <div v-if="selCond.subject === 'param' && !paramFields.length" class="fd-warn">
                当前字段上方没有可引用的参数。请先在「表单内容」把要引用的参数排到本字段之前。
              </div>
              <div v-for="f in fieldList" :key="f.code" :class="['ovm-opt', selCond.field === f.code && 'is-on']" @click="pickField(f)">
                <span class="bl-truncate">{{ f.name }}</span>
                <span class="bl-muted ovm-opt-dt">{{ dtLabel(f.dataType) }}</span>
              </div>
            </div>

            <!-- 步骤 3: 选择比较运算符 -->
            <div v-else-if="step === 'operator'" class="ovm-step-body">
              <div v-for="o in opsFor(selCond.dataType)" :key="o.key"
                   :class="['ovm-opt', selCond.operator === o.key && 'is-on']" @click="pickOp(o)">{{ o.label }}</div>
            </div>

            <!-- 步骤 4: 配置比较值 -->
            <div v-else class="ovm-step-body">
              <div class="ovm-vlabel">{{ condText(selCond) }}</div>
              <template v-if="OV_NO_VALUE_OPS.includes(selCond.operator)">
                <div class="bl-muted" style="font-size:12px;margin:8px 0 12px">该运算符无需比较值。</div>
              </template>
              <template v-else>
                <div class="ovm-vlabel-sm">输入比较值</div>
                <BlSelect v-if="valueOptions.length" v-model="selCond.value" :options="valueOptions" clearable placeholder="选择值" />
                <input v-else class="bl-input" v-model="selCond.value" placeholder="请输入值" />
              </template>
              <button class="bl-btn bl-btn-primary" style="width:100%;margin-top:12px" @click="confirmCond">确认条件</button>
            </div>
          </div>
        </div>

        <div class="ovm-ft">
          <span v-if="err" class="ovm-err">{{ err }}</span>
          <span style="flex:1"></span>
          <button class="bl-btn" @click="tryClose">取消</button>
          <button class="bl-btn bl-btn-primary" @click="finish">完成</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import OverrideCondGroup from './OverrideCondGroup.vue'
import { OV_ACTIONS, OV_SUBJECTS, OV_USER_FIELDS, OV_NO_VALUE_OPS, opsFor, condText, condReady,
  isRedundant, emptyBlock, ovUid } from './overrideModel.js'

const props = defineProps({
  open: Boolean,
  block: { type: Object, default: null },          // 编辑态传入的原始块 (不直接改, 内部克隆)
  presetType: { type: String, default: '' },       // 快捷入口预选的覆盖类型
  paramName: { type: String, default: '' },
  paramFields: { type: Array, default: () => [] }, // 仅当前字段「上方」的参数
  defaults: { type: Object, default: () => ({}) }, // { visible, required, disabled } 全局默认, 用于冗余提示
})
const emit = defineEmits(['update:open', 'save'])

const ACTION_OPTS = OV_ACTIONS.map(a => ({ value: a.value, label: a.label }))
const STEP_TITLE = { template: '选择条件模板', field: '选择属性', operator: '选择运算符', value: '配置比较值' }
const boolAction = t => !!OV_ACTIONS.find(a => a.value === t)?.bool

const draft = ref(emptyBlock())
const isNew = ref(true)
const selCond = ref(null)
const step = ref('template')
const err = ref('')
/* 用打开时的快照比对判断是否有改动 — 深监听会被「打开时重建 draft」误触发 */
let snapshot = ''
const dirty = () => JSON.stringify(draft.value) !== snapshot

watch(() => props.open, v => {
  if (!v) return
  isNew.value = !props.block
  draft.value = props.block ? JSON.parse(JSON.stringify(props.block)) : emptyBlock(props.presetType)
  snapshot = JSON.stringify(draft.value)
  selCond.value = null; step.value = 'template'; err.value = ''
  /* 编辑态默认选中第一个条件 (文档: 回显并默认选中第一个条件) */
  const first = draft.value.cond.children.find(c => c.type !== 'group')
  if (first) selectCond(first)
})

const fieldList = computed(() => selCond.value?.subject === 'user' ? OV_USER_FIELDS : props.paramFields)
/* 枚举类参数给出候选值, 其余自由输入 */
const valueOptions = computed(() => {
  const c = selCond.value; if (!c || c.subject !== 'param') return []
  const f = props.paramFields.find(x => x.code === c.field)
  return (f?.options || []).filter(o => String(o.value ?? '').trim()).map(o => ({ value: o.value, label: o.label || o.value }))
})
function dtLabel(dt) {
  return ({ string: '字符串', number: '数值', boolean: '布尔', enum: '枚举', object: '对象引用', date: '日期', multi: '多值' })[dt] || dt || ''
}

function selectCond(c) {
  selCond.value = c
  step.value = !c.subject ? 'template' : !c.field ? 'field' : !c.operator ? 'operator' : 'value'
}
function stepBack() { step.value = step.value === 'value' ? 'operator' : step.value === 'operator' ? 'field' : 'template' }
function pickSubject(k) { selCond.value.subject = k; selCond.value.field = ''; selCond.value.fieldName = ''; step.value = 'field' }
function pickField(f) {
  Object.assign(selCond.value, { field: f.code, fieldName: f.name, dataType: f.dataType || '', operator: '', value: '' })
  step.value = 'operator'
}
function pickOp(o) { selCond.value.operator = o.key; step.value = 'value' }
function confirmCond() { selCond.value = null; step.value = 'template' }

function addAction() {
  const used = draft.value.actions.map(a => a.type)
  const next = OV_ACTIONS.find(a => !used.includes(a.value)) || OV_ACTIONS[0]
  draft.value.actions.push({ _k: ovUid(), type: next.value, value: next.bool ? 1 : '' })
}
function onActTypeChange(a) { a.value = boolAction(a.type) ? (Number(a.value) ? 1 : 0) : '' }

function collectConds(g, out = []) {
  g.children.forEach(ch => ch.type === 'group' ? collectConds(ch, out) : out.push(ch))
  return out
}
function finish() {
  const conds = collectConds(draft.value.cond)
  if (!conds.length) return (err.value = '请至少配置一个条件')
  const bad = conds.find(c => !condReady(c))
  if (bad) { selectCond(bad); return (err.value = '存在未配置完整的条件') }
  if (!draft.value.actions.length) return (err.value = 'Then 区至少需要一条覆盖动作')
  err.value = ''
  emit('save', JSON.parse(JSON.stringify(draft.value)))
  emit('update:open', false)
}
async function tryClose() {
  if (dirty()) {
    const ok = await BL.confirm({ title: '放弃修改', content: '覆盖规则尚未保存,确定关闭?', danger: true, okText: '放弃' })
    if (!ok) return
  }
  emit('update:open', false)
}
</script>

<style scoped>
.ovm-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 1350; display: flex; align-items: center; justify-content: center; }
.ovm-modal { width: 960px; max-width: 95vw; height: 620px; max-height: 90vh; background: var(--bl-bg-1); border-radius: 12px;
  box-shadow: 0 24px 56px rgba(0,0,0,.34); display: flex; flex-direction: column; overflow: hidden; }
.ovm-hd { display: flex; align-items: center; padding: 13px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.ovm-body { flex: 1; min-height: 0; display: flex; }
.ovm-left { flex: 0 0 60%; min-width: 0; overflow-y: auto; padding: 14px 16px; border-right: 1px solid var(--bl-divider); }
.ovm-right { flex: 1; min-width: 0; display: flex; flex-direction: column; background: var(--bl-bg-2); }
.ovm-tag { display: inline-block; font-size: 11.5px; font-weight: 700; padding: 2px 8px; border-radius: 4px; margin-bottom: 8px; }
.ovm-tag-if { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ovm-tag-then { background: #FEF3C7; color: #92400E; margin-top: 16px; }
.ovm-acts { display: flex; flex-direction: column; gap: 8px; }
.ovm-act { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: 8px; }
.ovm-act.is-redundant { border-color: #FDE68A; background: #FEF3C7; }
.ovm-act-lbl { font-size: 12.5px; color: var(--bl-text-2); flex-shrink: 0; }
.ovm-redundant { font-size: 11.5px; color: #92400E; flex-shrink: 0; }
.ovm-empty-line { font-size: 12px; color: var(--bl-text-3); }
.ovm-add { font-size: 12.5px; color: var(--bl-primary); cursor: pointer; align-self: flex-start; }
.ovm-add:hover { text-decoration: underline; }
.ovm-step-hd { display: flex; align-items: center; gap: 8px; padding: 11px 14px; border-bottom: 1px solid var(--bl-divider); }
.ovm-step-title { font-size: 13px; font-weight: 600; }
.ovm-back { display: inline-flex; align-items: center; gap: 2px; border: 0; background: transparent; color: var(--bl-text-2); font-size: 12.5px; cursor: pointer; padding: 0; }
.ovm-back:hover { color: var(--bl-primary); }
.ovm-step-body { flex: 1; min-height: 0; overflow-y: auto; padding: 14px; }
.ovm-hint { font-size: 12.5px; color: var(--bl-text-3); line-height: 1.7; }
.ovm-tpl { display: flex; align-items: center; gap: 10px; padding: 11px 12px; background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 8px; margin-bottom: 10px; cursor: pointer; }
.ovm-tpl:hover { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.ovm-tpl-ic { width: 26px; height: 26px; border-radius: 7px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ovm-tpl-lbl { font-size: 13px; font-weight: 600; }
.ovm-tpl-desc { font-size: 11.5px; color: var(--bl-text-3); margin-top: 2px; line-height: 1.4; }
.ovm-opt { display: flex; align-items: center; gap: 8px; padding: 9px 11px; background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 7px; margin-bottom: 7px; cursor: pointer; font-size: 13px; }
.ovm-opt:hover { border-color: var(--bl-primary); }
.ovm-opt.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.ovm-opt-dt { flex-shrink: 0; font-size: 11px; }
.ovm-vlabel { font-size: 12.5px; color: var(--bl-text-2); line-height: 1.6; margin-bottom: 10px; }
.ovm-vlabel-sm { font-size: 12px; color: var(--bl-text-3); margin-bottom: 6px; }
.ovm-ft { display: flex; align-items: center; gap: 8px; padding: 11px 16px; border-top: 1px solid var(--bl-divider); }
.ovm-err { font-size: 12.5px; color: #f53f3f; }
.fd-warn { background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 6px; padding: 8px 12px; font-size: 12px; color: #92400E; line-height: 1.6; }
/* 小号开关 (与主页面一致) */
.adw-showsw { display: inline-block; width: 32px; height: 18px; border-radius: 9px; background: var(--bl-bg-3, #c9cdd4); position: relative; cursor: pointer; transition: background .15s; flex-shrink: 0; }
.adw-showsw.is-on { background: var(--bl-primary); }
.adw-showsw-dot { position: absolute; left: 2px; top: 2px; width: 14px; height: 14px; border-radius: 50%; background: #fff; transition: left .15s; box-shadow: 0 1px 2px rgba(0,0,0,.3); }
.adw-showsw.is-on .adw-showsw-dot { left: 16px; }
</style>
