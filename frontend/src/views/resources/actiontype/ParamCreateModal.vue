<template>
  <Teleport to="body">
    <div v-if="open" class="pcm-mask" @click.self="close">
      <div class="pcm-modal">
        <div class="pcm-hd"><span>新建参数</span><span style="flex:1"></span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="close" v-html="BL.icon('x', 14)"></button></div>

        <div class="pcm-tools">
          <button class="bl-btn bl-btn-sm" :disabled="!canPickProp" :title="canPickProp ? '从主对象 / 规则创建的对象里挑属性' : '无可选属性来源'"
                  @click="$emit('pick-prop')"><span v-html="BL.icon('search', 12)"></span><span style="margin-left:4px">从对象属性选择</span></button>
          <button class="bl-btn bl-btn-sm" @click="addBlank"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">手动添加一行</span></button>
          <span style="flex:1"></span>
          <span class="bl-muted" style="font-size:12px">待创建 <b>{{ rows.length }}</b> 条</span>
        </div>

        <div class="pcm-body">
          <div class="pcm-th">
            <span class="pcm-c-name"><i class="pcm-req">*</i>参数名称</span>
            <span class="pcm-c-code"><i class="pcm-req">*</i>参数编码</span>
            <span class="pcm-c-disp"><i class="pcm-req">*</i>组件类型</span>
            <span class="pcm-c-op"></span>
          </div>
          <div class="pcm-rows">
            <div v-for="(r, i) in rows" :key="r._k" :class="['pcm-row', errIdx === i && 'is-err']">
              <div class="pcm-c-name">
                <input class="bl-input bl-input-xs" v-model="r.name" placeholder="参数名称" @input="errIdx = -1" />
                <div v-if="r.propertyCode" class="pcm-from" :title="`取自 ${r.srcName || '对象'} 的属性 ${r.propertyCode}`">
                  <span class="bl-truncate">取自 {{ r.srcName || '对象' }}·<span class="bl-mono">{{ r.propertyCode }}</span></span>
                  <button class="pcm-unlink" title="断开来源, 作为普通参数" @click="unlink(r)" v-html="BL.icon('x', 10)"></button>
                </div>
              </div>
              <div class="pcm-c-code">
                <input class="bl-input bl-input-xs bl-mono" v-model="r.code" placeholder="如 p_buildYear" @input="errIdx = -1" />
              </div>
              <div class="pcm-c-disp">
                <BlSelect v-model="r.display" :options="displayTypes" size="sm" />
              </div>
              <div class="pcm-c-op">
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除该行" @click="removeRow(i)" v-html="BL.icon('trash', 12)"></button>
              </div>
            </div>
            <div v-if="!rows.length" class="pcm-empty">还没有待创建的参数 —— 点上方「从对象属性选择」或「手动添加一行」</div>
          </div>
          <div v-if="err" class="pcm-err">{{ err }}</div>
        </div>

        <div class="pcm-ft">
          <span class="bl-muted pcm-ft-tip">参数将加入分区「{{ section }}」</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-sm" @click="close">取消</button>
          <button class="bl-btn bl-btn-sm bl-btn-primary" :disabled="!rows.length" @click="submit">确定{{ rows.length ? `(${rows.length})` : '' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'

const props = defineProps({
  open: Boolean,
  section: { type: String, default: '基础参数' },
  displayTypes: { type: Array, default: () => [] },
  usedCodes: { type: Array, default: () => [] },   // 表单里已存在的参数编码
  canPickProp: { type: Boolean, default: false },
})
const emit = defineEmits(['update:open', 'submit', 'pick-prop', 'rows-change'])

const rows = ref([])
const err = ref('')
const errIdx = ref(-1)
let seq = 0

/* 手输时按组件类型猜数据类型; 从属性来的以属性数据类型为准 */
const TYPE_BY_DISPLAY = { number: 'number', switch: 'boolean', select: 'enum', radio: 'enum', checkbox: 'enum' }

function blankRow() {
  return { _k: ++seq, name: '', code: '', display: 'input', paramType: '', required: 1, propertyCode: '', classId: '', srcName: '', srcApi: '' }
}
function addBlank() { rows.value = [...rows.value, blankRow()]; err.value = '' }
function removeRow(i) { rows.value = rows.value.filter((_, x) => x !== i); err.value = ''; errIdx.value = -1 }
function unlink(r) { r.propertyCode = ''; r.classId = ''; r.srcName = ''; r.srcApi = ''; r.paramType = '' }

/* 属性面板确定后追加; 已在清单里的属性跳过, 免得重复挑同一个 */
function addFromProps(list) {
  const has = new Set(rows.value.map(r => r.propertyCode).filter(Boolean))
  const add = []
  for (const p of list) {
    if (has.has(p.code)) continue
    has.add(p.code)
    add.push({ _k: ++seq, name: p.name || '', code: p.param_code || '', display: p.display_type || 'input',
      paramType: p.param_type || '', required: p.required ? 1 : 0,
      propertyCode: p.code || '', classId: p.classId || '', srcName: p.srcName || '', srcApi: p.srcApi || '' })
  }
  /* 手动加的空行留着没意义, 有属性进来就顶掉 */
  const keep = rows.value.filter(r => r.propertyCode || String(r.name).trim() || String(r.code).trim())
  rows.value = [...keep, ...add]
  err.value = ''
}
defineExpose({ addFromProps })

function submit() {
  const seen = new Set(props.usedCodes)
  for (let i = 0; i < rows.value.length; i++) {
    const r = rows.value[i]
    const nm = String(r.name).trim(), cd = String(r.code).trim()
    if (!nm) { fail(i, '请填写参数名称'); return }
    if (!cd) { fail(i, '请填写参数编码'); return }
    /* 属性 api_name 是 camelCase (buildYear), 带出来的 p_buildYear 必须合法, 故只约束首字符 */
    if (!/^[a-z][a-zA-Z0-9_]*$/.test(cd)) { fail(i, `「${cd}」不合法: 需小写字母开头, 只含字母/数字/下划线`); return }
    if (seen.has(cd)) { fail(i, `参数编码「${cd}」已存在`); return }
    seen.add(cd)
  }
  emit('submit', rows.value.map(r => ({
    param_name: String(r.name).trim(), param_code: String(r.code).trim(), display_type: r.display,
    param_type: r.propertyCode ? (r.paramType || 'string') : (TYPE_BY_DISPLAY[r.display] || 'string'),
    is_required: r.required ?? 1, property_code: r.propertyCode,
    src_class_id: r.classId, src_class_name: r.srcName, src_class_api: r.srcApi,
  })))
  emit('update:open', false)
}
function fail(i, msg) { errIdx.value = i; err.value = `第 ${i + 1} 行: ${msg}` }
function close() { emit('update:open', false) }

/* 属性面板据此把已在清单里的属性也排除掉 */
watch(rows, v => emit('rows-change', v.map(r => r.propertyCode).filter(Boolean)), { deep: true })
watch(() => props.open, v => {
  if (!v) return
  rows.value = [blankRow()]
  err.value = ''; errIdx.value = -1
})
</script>

<style scoped>
.pcm-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); backdrop-filter: blur(3px); z-index: 1340; display: flex; align-items: center; justify-content: center; }
.pcm-modal { width: 680px; max-width: 96vw; max-height: 82vh; background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 12px; box-shadow: 0 24px 56px rgba(0,0,0,.4); display: flex; flex-direction: column; overflow: hidden; }
.pcm-hd { display: flex; align-items: center; padding: 12px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.pcm-tools { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-bottom: 1px solid var(--bl-divider); }
.pcm-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 10px 16px 0; overflow: hidden; }
.pcm-th { display: flex; gap: 8px; padding: 0 0 6px; font-size: 12px; color: var(--bl-text-3); }
.pcm-rows { flex: 1; min-height: 0; overflow-y: auto; }
.pcm-row { display: flex; gap: 8px; align-items: flex-start; padding: 5px 0; border-radius: 6px; }
.pcm-row.is-err { background: color-mix(in srgb, #f53f3f 8%, transparent); }
.pcm-c-name { flex: 1 1 34%; min-width: 0; }
.pcm-c-code { flex: 1 1 32%; min-width: 0; }
.pcm-c-disp { flex: 0 0 150px; }
.pcm-c-op { flex: 0 0 30px; display: flex; justify-content: center; padding-top: 2px; }
.pcm-c-name .bl-input, .pcm-c-code .bl-input { width: 100%; }
.pcm-req { color: #f53f3f; font-style: normal; margin-right: 2px; }
.pcm-from { display: flex; align-items: center; gap: 4px; margin-top: 3px; font-size: 11px; color: var(--bl-text-3); }
.pcm-unlink { border: 0; background: transparent; cursor: pointer; color: var(--bl-text-3); display: inline-flex; padding: 0; }
.pcm-unlink:hover { color: #f53f3f; }
.pcm-empty { padding: 28px 16px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.pcm-err { padding: 8px 0 2px; font-size: 12px; color: #f53f3f; }
.pcm-ft { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }
.pcm-ft-tip { font-size: 11.5px; }
</style>
