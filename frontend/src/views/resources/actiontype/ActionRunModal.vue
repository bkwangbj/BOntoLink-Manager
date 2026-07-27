<template>
  <Teleport to="body">
    <transition name="arm-fade">
      <div v-if="open" class="arm-mask" @click.self="onClose">
        <div class="arm-modal">
          <div class="arm-hd">
            <div class="arm-hd-l">
              <span class="arm-ic" :style="{ background: headColor }" v-html="BL.icon('play', 15, '#fff')"></span>
              <div>
                <div class="arm-title">试运行 · {{ action?.rdfs_label || '动作' }}</div>
                <div class="bl-muted" style="font-size:12px">{{ opLabel }} · <span class="bl-mono">{{ action?.api_name }}</span></div>
              </div>
            </div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onClose" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="arm-body">
            <div class="arm-note">模拟执行:系统当前无真实实例存储,此处按动作的校验/规则/值来源计算出「将写入的实例记录」与「将触发的副作用」,并记入执行历史,不写物理表。</div>

            <!-- 输入参数 -->
            <div class="sec">输入参数</div>
            <div v-if="loading" class="bl-muted" style="font-size:12px;padding:8px">加载中…</div>
            <div v-else-if="!formParams.length" class="bl-muted" style="font-size:12px;padding:8px">该动作无表单参数,可直接试运行。</div>
            <div v-else class="arm-form">
              <label v-for="p in formParams" :key="p.param_code" class="arm-fld">
                <span class="arm-lbl">{{ p.param_name || p.param_code }} <i v-if="p.is_required">*</i>
                  <span class="bl-mono bl-muted" style="font-size:10.5px">{{ p.param_code }}</span></span>
                <input v-if="p.param_type==='number'" class="bl-input" type="number" v-model="inputs[p.param_code]" :placeholder="p.placeholder||''" />
                <label v-else-if="p.param_type==='boolean'" class="arm-bool"><input type="checkbox" v-model="inputs[p.param_code]" /> 是</label>
                <input v-else-if="p.param_type==='date'" class="bl-input" type="date" v-model="inputs[p.param_code]" />
                <input v-else class="bl-input" v-model="inputs[p.param_code]" :placeholder="p.placeholder||''" />
              </label>
            </div>

            <!-- 结果 -->
            <template v-if="result">
              <div class="sec">执行结果
                <span :class="['bl-tag', statusCls(result.status)]" style="margin-left:8px">{{ statusLabel(result.status) }}</span>
              </div>
              <div v-if="result.errors && result.errors.length" class="arm-errors">
                <div v-for="(e,i) in result.errors" :key="i">• {{ e }}</div>
              </div>
              <template v-else>
                <div class="arm-kv"><span class="bl-muted">操作</span> <span class="bl-tag" :style="typeTagStyle">{{ result.op_label }}</span> <span class="bl-muted" style="margin-left:8px">主体</span> {{ result.object_class_name || '—' }}</div>
                <div class="arm-kv"><span class="bl-muted">提交校验</span> {{ result.submit_check }}</div>
                <!-- 将写入的实例 -->
                <div class="arm-sub">将写入的实例记录</div>
                <table v-if="instanceRows.length" class="bl-table arm-table">
                  <thead><tr><th class="t-left">属性</th><th class="t-left">值</th></tr></thead>
                  <tbody><tr v-for="r in instanceRows" :key="r.k"><td class="bl-mono">{{ r.k }}</td><td>{{ fmt(r.v) }}</td></tr></tbody>
                </table>
                <div v-else class="bl-muted" style="font-size:12px;padding:6px">无属性写入(未配置编辑规则/参数)</div>
                <!-- 解析明细 -->
                <template v-if="result.resolution && result.resolution.length">
                  <div class="arm-sub">解析明细</div>
                  <table class="bl-table arm-table">
                    <thead><tr><th class="t-left">属性</th><th class="t-left">操作</th><th class="t-left">值来源</th><th class="t-left">值</th></tr></thead>
                    <tbody><tr v-for="(d,i) in result.resolution" :key="i"><td class="bl-mono">{{ d.property }}</td><td>{{ OP_LABEL[d.operator]||d.operator }}</td><td>{{ d.source }}</td><td>{{ fmt(d.value) }}</td></tr></tbody>
                  </table>
                </template>
                <!-- 副作用 -->
                <template v-if="result.side_effects && result.side_effects.length">
                  <div class="arm-sub">将触发的副作用</div>
                  <div v-for="(s,i) in result.side_effects" :key="i" class="arm-se">
                    <span class="bl-tag" style="background:#722ED11a;color:#722ED1">副作用</span>
                    <b style="margin:0 6px">{{ s.rule_name || '—' }}</b>
                    <span class="bl-mono bl-muted">{{ s.target || '' }}</span>
                  </div>
                </template>
              </template>
            </template>

            <!-- 执行历史 -->
            <div class="sec" style="cursor:pointer" @click="showHist = !showHist">
              执行历史 <span class="bl-muted" style="font-size:11px">({{ history.length }})</span>
              <span v-html="BL.icon(showHist?'chevronDown':'chevronRight', 11)" style="margin-left:4px"></span>
            </div>
            <table v-show="showHist" class="bl-table arm-table">
              <thead><tr><th class="t-left">时间</th><th class="t-left">状态</th><th class="t-center">试运行</th><th class="t-left">说明</th></tr></thead>
              <tbody>
                <tr v-for="h in history" :key="h.id">
                  <td class="bl-mono" style="font-size:11px">{{ (h.execute_time||'').slice(0,19) }}</td>
                  <td><span :class="['bl-tag', statusCls(h.status)]">{{ statusLabel(h.status) }}</span></td>
                  <td class="t-center">{{ Number(h.dry_run)===1 ? '是' : '否' }}</td>
                  <td class="bl-muted">{{ h.message || '—' }}</td>
                </tr>
                <tr v-if="!history.length"><td colspan="4" class="bl-muted" style="text-align:center;padding:12px;font-size:12px">暂无执行记录</td></tr>
              </tbody>
            </table>
          </div>

          <div class="arm-ft">
            <span style="flex:1"></span>
            <button class="bl-btn" @click="onClose">关闭</button>
            <button class="bl-btn bl-btn-primary" :disabled="running" @click="run">
              <span v-html="BL.icon('play', 12, '#fff')"></span><span style="margin-left:4px">{{ running ? '执行中…' : '试运行' }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { actionTypeApi } from '@/api'

const props = defineProps({ open: Boolean, action: Object })
const emit = defineEmits(['update:open', 'executed'])

const OP_LABEL = { set:'赋值', add:'增加', sub:'减少', append:'追加', clear:'清空' }
const formParams = ref([])
const inputs = reactive({})
const result = ref(null)
const history = ref([])
const loading = ref(false)
const running = ref(false)
const showHist = ref(false)

const opLabel = computed(() => result.value?.op_label || typeName(props.action?.action_type))
const headColor = computed(() => props.action?.color || '#00B42A')
const typeTagStyle = computed(() => ({ background:'color-mix(in srgb, #165DFF 12%, transparent)', color:'#165DFF', border:'1px solid color-mix(in srgb, #165DFF 30%, transparent)' }))
const instanceRows = computed(() => {
  const inst = result.value?.instance || {}
  return Object.keys(inst).map(k => ({ k, v: inst[k] }))
})

function typeName(t){ return ({11:'创建对象',12:'修改对象',13:'Upsert',14:'删除对象',21:'创建链接',22:'删除链接',30:'函数',40:'Webhook',60:'通知'})[Number(t)]||'动作' }

async function loadAction() {
  if (!props.action?.id) return
  loading.value = true
  result.value = null
  Object.keys(inputs).forEach(k => delete inputs[k])
  try {
    const res = await actionTypeApi.get(props.action.id).catch(() => null)
    formParams.value = res?.form_params || []
    for (const p of formParams.value) {
      inputs[p.param_code] = p.param_type === 'boolean' ? false : (p.default_value || '')
    }
    history.value = await actionTypeApi.executions(props.action.id).catch(() => [])
  } finally { loading.value = false }
}
watch(() => props.open, v => { if (v) loadAction() })

async function run() {
  running.value = true
  try {
    const payload = {}
    for (const p of formParams.value) {
      let v = inputs[p.param_code]
      if (p.param_type === 'boolean') v = v ? true : false
      payload[p.param_code] = v
    }
    result.value = await actionTypeApi.execute(props.action.id, payload, true)
    history.value = await actionTypeApi.executions(props.action.id).catch(() => history.value)
    showHist.value = true
    if (result.value?.status === 'success') BL.success('试运行成功')
    else if (result.value?.status === 'validation_failed') BL.warning('校验未通过')
    emit('executed')
  } catch (e) { BL.error(e?.msg || '执行失败') } finally { running.value = false }
}
function onClose() { emit('update:open', false) }

function fmt(v){ if (v===null||v===undefined) return '(空)'; if (v===true) return '是'; if (v===false) return '否'; return String(v) }
function statusLabel(s){ return ({success:'成功', validation_failed:'校验未过', failed:'失败'})[s] || s }
function statusCls(s){ return ({success:'bl-tag-success', validation_failed:'bl-tag-warning', failed:'bl-tag-danger'})[s] || '' }
</script>

<style scoped>
.arm-mask { position: fixed; inset: 0; background: rgba(0,0,0,.5); backdrop-filter: blur(3px); z-index: 1250; display: flex; align-items: center; justify-content: center; }
:root[data-theme="dark"] .arm-mask { background: rgba(0,0,0,.62); }
.arm-modal { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 12px; width: 720px; max-width: 96vw; max-height: 88vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 24px 56px rgba(0,0,0,0.5); }
.arm-hd { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.arm-hd-l { display: flex; align-items: center; gap: 10px; }
.arm-ic { width: 32px; height: 32px; border-radius: 7px; display: inline-flex; align-items: center; justify-content: center; }
.arm-title { font-size: 15px; font-weight: 600; }
.arm-body { flex: 1; min-height: 0; overflow: auto; padding: 14px 16px; }
.arm-note { font-size: 12px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 6px; padding: 8px 10px; margin-bottom: 6px; }
.sec { font-size: 12px; color: var(--bl-text-3); font-weight: 600; padding-left: 8px; border-left: 3px solid var(--bl-primary); margin: 16px 0 10px; line-height: 1; display: flex; align-items: center; }
.arm-form { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; }
.arm-fld { display: flex; flex-direction: column; gap: 4px; }
.arm-lbl { font-size: 12px; color: var(--bl-text-2); display: flex; align-items: center; gap: 6px; }
.arm-lbl i { color: #f53f3f; font-style: normal; }
.arm-bool { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; height: 32px; }
.arm-errors { background: color-mix(in srgb, #f53f3f 8%, var(--bl-bg-1)); border: 1px solid color-mix(in srgb, #f53f3f 30%, transparent); border-radius: 6px; padding: 8px 12px; font-size: 12px; color: #f53f3f; display: flex; flex-direction: column; gap: 3px; }
.arm-kv { font-size: 13px; margin: 4px 0; }
.arm-kv .bl-muted { display: inline-block; min-width: 60px; }
.arm-sub { font-size: 12px; font-weight: 600; color: var(--bl-text-2); margin: 12px 0 6px; }
.arm-table { width: 100%; font-size: 12px; }
.arm-table thead th { background: var(--bl-bg-2); font-weight: 600; height: 30px; padding: 0 8px; color: var(--bl-text-2); }
.arm-table thead th.t-left { text-align: left; }
.arm-table thead th.t-center { text-align: center; }
.arm-table td { padding: 4px 8px; border-top: 1px solid var(--bl-divider); }
.arm-table td.t-center { text-align: center; }
.arm-se { font-size: 12.5px; padding: 5px 0; display: flex; align-items: center; }
.arm-ft { flex-shrink: 0; padding: 10px 16px; display: flex; align-items: center; gap: 8px; border-top: 1px solid var(--bl-divider); }
.arm-fade-enter-active, .arm-fade-leave-active { transition: opacity .15s; }
.arm-fade-enter-from, .arm-fade-leave-to { opacity: 0; }
</style>
