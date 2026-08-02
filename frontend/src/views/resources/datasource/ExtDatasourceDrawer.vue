<template>
  <Teleport to="body">
    <transition name="eds-drawer">
      <aside v-if="open" class="eds-drawer">
        <!-- 头部 -->
        <div class="eds-hd">
          <span class="eds-ic" v-html="BL.icon('plug', 18, '#fff')"></span>
          <div class="bl-grow" style="min-width:0">
            <div class="eds-title">
              <span class="bl-truncate">{{ form.ds_name || '新建外部数据源' }}</span>
              <span v-if="form.ds_code" class="bl-mono bl-muted" style="font-size:12px">({{ form.ds_code }})</span>
              <span :class="['bl-tag', form.status === 1 ? 'bl-tag-success' : 'bl-tag-muted']">{{ form.status === 1 ? '启用' : '禁用' }}</span>
            </div>
            <div class="eds-sub">{{ DS_TYPE_LABEL[form.ds_type] || form.ds_type }} · {{ AUTH_META[form.auth_type]?.label || '无鉴权' }}</div>
          </div>
          <button v-if="isEdit" class="bl-btn bl-btn-sm" @click="onTest"><span v-html="BL.icon('zap', 12)"></span><span style="margin-left:4px">测试连接</span></button>
          <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
        </div>

        <div class="eds-body">
          <!-- 1 基础信息 -->
          <div class="eds-card"><div class="eds-card-hd">基础信息</div>
            <div class="eds-grid">
              <label class="eds-fld"><span class="eds-lbl">名称 <i>*</i></span>
                <input class="bl-input" v-model="form.ds_name" maxlength="128" placeholder="数据源中文显示名" /></label>
              <label class="eds-fld"><span class="eds-lbl">Source API Name <i>*</i></span>
                <input class="bl-input bl-mono" v-model="form.ds_code" :disabled="isEdit" maxlength="64"
                       :placeholder="isEdit ? '' : '英文下划线格式, 创建后不可修改'" /></label>
              <label class="eds-fld"><span class="eds-lbl">所属领域</span>
                <BlSelect v-model="form.category_code" :options="domainOptions" clearable placeholder="未分类" /></label>
              <label class="eds-fld"><span class="eds-lbl">数据源类型 <i>*</i></span>
                <BlSelect v-model="form.ds_type" :options="DS_TYPE_OPTS" /></label>
              <label class="eds-fld"><span class="eds-lbl">读写属性</span>
                <BlSelect v-model="form.read_write_type" :options="RW_OPTS" /></label>
              <label class="eds-fld"><span class="eds-lbl">状态</span>
                <BlSelect v-model="form.status" :options="STATUS_OPTS" /></label>
            </div>
          </div>

          <!-- 2 公用连接配置 -->
          <div class="eds-card"><div class="eds-card-hd">公用连接配置 <span class="bl-muted eds-hint">所有下属接口默认继承,单接口可单独覆盖</span></div>
            <label class="eds-fld eds-fld-full"><span class="eds-lbl">基础地址 Base URL <i>*</i></span>
              <input class="bl-input" v-model="form.base_url" placeholder="https://api.example.com/v1" /></label>
            <div class="eds-grid" style="margin-top:12px">
              <label class="eds-fld"><span class="eds-lbl">默认请求方法 <i>*</i></span>
                <BlSelect v-model="form.default_method" :options="METHOD_OPTS" /></label>
              <label class="eds-fld"><span class="eds-lbl">默认数据格式</span>
                <BlSelect v-model="form.content_type" :options="CONTENT_TYPE_OPTS" /></label>
              <label class="eds-fld"><span class="eds-lbl">连接超时 (ms) <i>*</i></span>
                <input class="bl-input" type="number" v-model.number="form.connect_timeout" /></label>
              <label class="eds-fld"><span class="eds-lbl">读取超时 (ms) <i>*</i></span>
                <input class="bl-input" type="number" v-model.number="form.read_timeout" /></label>
              <label class="eds-fld"><span class="eds-lbl">重试次数 <i>*</i></span>
                <input class="bl-input" type="number" min="0" max="3" v-model.number="form.retry_count" /></label>
              <label class="eds-fld"><span class="eds-lbl">重试间隔 (ms) <i>*</i></span>
                <input class="bl-input" type="number" v-model.number="form.retry_interval" /></label>
            </div>
            <div class="eds-switches">
              <label class="eds-ck"><input type="checkbox" v-model="form.ssl_verify" :true-value="1" :false-value="0" /> 启用 SSL 证书校验</label>
              <label class="eds-ck"><input type="checkbox" v-model="form.log_enable" :true-value="1" :false-value="0" /> 开启请求日志记录</label>
              <label class="eds-ck"><input type="checkbox" v-model="form.header_enable" :true-value="1" :false-value="0" /> 启用全局请求头</label>
            </div>
            <!-- 全局请求头键值对 -->
            <div v-if="form.header_enable" class="eds-headers">
              <div v-for="(h, i) in headers" :key="i" class="eds-header-row">
                <input class="bl-input bl-input-sm bl-mono" v-model="h.k" placeholder="Header 名, 如 Content-Type" />
                <input class="bl-input bl-input-sm" v-model="h.v" placeholder="值" />
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除" @click="headers.splice(i,1)" v-html="BL.icon('x', 11)"></button>
              </div>
              <button class="eds-add" @click="headers.push({ k:'', v:'' })"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">添加请求头</span></button>
            </div>
          </div>

          <!-- 3 鉴权配置 -->
          <div class="eds-card"><div class="eds-card-hd">鉴权配置 <span class="bl-muted eds-hint">数据源级全局配置,下属接口可继承或单独配置</span></div>
            <AuthConfigForm v-model:auth-type="form.auth_type" v-model:config="authConfig" />
          </div>

          <!-- 4 扩展信息 -->
          <div class="eds-card"><div class="eds-card-hd">扩展信息</div>
            <textarea class="bl-textarea" v-model="form.remark" rows="3" maxlength="512"
                      placeholder="数据源业务用途、环境说明、注意事项等"></textarea>
          </div>
        </div>

        <div class="eds-ft">
          <span v-if="err" class="eds-err">{{ err }}</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-sm" @click="close">取消</button>
          <button class="bl-btn bl-btn-sm bl-btn-primary" :disabled="saving" @click="onSave">保存配置</button>
        </div>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import { extDatasourceApi } from '@/api'
import AuthConfigForm from './AuthConfigForm.vue'
import { AUTH_META, defaultAuthConfig, validateAuthConfig } from './authModel.js'

const props = defineProps({
  open: Boolean,
  record: { type: Object, default: null },       // 编辑时传入, 新建为 null
  domainOptions: { type: Array, default: () => [] },
  defaultCategory: { type: String, default: '' },
})
const emit = defineEmits(['update:open', 'saved'])

const DS_TYPE_OPTS = [
  { value: 'http_rest', label: 'REST API' }, { value: 'webhook', label: 'Webhook' }, { value: 'graphql', label: 'GraphQL' },
]
const DS_TYPE_LABEL = Object.fromEntries(DS_TYPE_OPTS.map(o => [o.value, o.label]))
const RW_OPTS = [{ value: 1, label: '只读' }, { value: 2, label: '读写' }]
const STATUS_OPTS = [{ value: 1, label: '启用' }, { value: 0, label: '禁用' }]
const METHOD_OPTS = ['GET', 'POST', 'PUT', 'DELETE'].map(v => ({ value: v, label: v }))
const CONTENT_TYPE_OPTS = ['application/json', 'application/x-www-form-urlencoded', 'multipart/form-data'].map(v => ({ value: v, label: v }))

const isEdit = ref(false)
const saving = ref(false)
const err = ref('')
const form = reactive({})
const authConfig = ref({})
const headers = ref([])

function blank() {
  return { ds_name:'', ds_code:'', category_code: props.defaultCategory || '', ds_type:'http_rest', read_write_type:1,
    base_url:'', default_method:'POST', content_type:'application/json',
    connect_timeout:5000, read_timeout:10000, retry_count:1, retry_interval:1000,
    ssl_verify:1, log_enable:1, header_enable:0, auth_type:'none', status:1, remark:'' }
}

watch(() => props.open, v => {
  if (!v) return
  err.value = ''
  isEdit.value = !!props.record?.id
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, blank(), props.record || {})
  authConfig.value = parseJson(form.auth_config) || defaultAuthConfig(form.auth_type)
  headers.value = Object.entries(parseJson(form.global_header) || {}).map(([k, v]) => ({ k, v }))
}, { immediate: true })

function parseJson(s) {
  if (!s) return null
  if (typeof s === 'object') return s
  try { return JSON.parse(s) } catch { return null }
}

async function onSave() {
  err.value = ''
  if (!String(form.ds_name || '').trim()) return err.value = '请填写名称'
  if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(form.ds_code || '')) return err.value = 'Source API Name 需为英文下划线格式,且以字母开头'
  if (!/^https?:\/\//i.test(form.base_url || '')) return err.value = '基础地址必须以 http:// 或 https:// 开头'
  if (!inRange(form.connect_timeout, 100, 300000)) return err.value = '连接超时取值范围 100 ~ 300000 毫秒'
  if (!inRange(form.read_timeout, 100, 600000)) return err.value = '读取超时取值范围 100 ~ 600000 毫秒'
  if (!inRange(form.retry_count, 0, 3)) return err.value = '重试次数最大支持 3 次'
  if (!inRange(form.retry_interval, 100, 60000)) return err.value = '重试间隔取值范围 100 ~ 60000 毫秒'
  const authErr = validateAuthConfig(form.auth_type, authConfig.value)
  if (authErr) return err.value = authErr

  const body = { ...form,
    auth_config: JSON.stringify(authConfig.value || {}),
    global_header: form.header_enable ? JSON.stringify(Object.fromEntries(headers.value.filter(h => h.k).map(h => [h.k, h.v]))) : null,
  }
  saving.value = true
  try {
    if (isEdit.value) await extDatasourceApi.update(form.id, body)
    else await extDatasourceApi.create(body)
    BL.success('已保存')
    emit('saved')
    close()
  } catch (e) { err.value = e?.msg || '保存失败' } finally { saving.value = false }
}

function inRange(v, min, max) { const n = Number(v); return Number.isFinite(n) && n >= min && n <= max }

async function onTest() {
  try {
    const r = await extDatasourceApi.test(form.id)
    r?.ok ? BL.success(r.message) : BL.warning(r?.message || '测试未通过')
  } catch (e) { BL.error(e?.msg || '测试失败') }
}
function close() { emit('update:open', false) }
</script>

<style scoped>
.eds-drawer { position: fixed; top: 0; right: 0; bottom: 0; width: 760px; max-width: 96vw; z-index: 1010;
  background: var(--bl-bg-2); border-left: 1px solid var(--bl-border); box-shadow: -8px 0 24px rgba(0,0,0,.12);
  display: flex; flex-direction: column; }
.eds-hd { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-divider); }
.eds-ic { width: 36px; height: 36px; border-radius: 9px; background: #00B42A; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.eds-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; min-width: 0; }
.eds-sub { font-size: 11.5px; color: var(--bl-text-3); margin-top: 2px; }
.eds-body { flex: 1; overflow-y: auto; padding: 14px 16px; }
.eds-card { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px; padding: 16px 18px; margin-bottom: 14px; }
.eds-card-hd { font-size: 13px; font-weight: 600; color: var(--bl-text-1); padding-left: 8px; border-left: 3px solid var(--bl-primary); margin-bottom: 14px; line-height: 1.2; }
.eds-hint { font-size: 11px; font-weight: 400; margin-left: 8px; }
.eds-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 20px; }
.eds-fld { display: flex; align-items: center; gap: 10px; min-width: 0; }
.eds-fld-full { display: flex; }
.eds-lbl { flex: 0 0 132px; text-align: right; font-size: 12.5px; color: var(--bl-text-2); }
.eds-lbl i { color: #f53f3f; font-style: normal; margin-left: 2px; }
.eds-fld .bl-input, .eds-fld > .bs { flex: 1; min-width: 0; }
.eds-switches { display: flex; flex-wrap: wrap; gap: 20px; margin-top: 14px; padding-left: 142px; }
.eds-ck { display: inline-flex; align-items: center; gap: 6px; font-size: 12.5px; color: var(--bl-text-2); cursor: pointer; }
.eds-headers { margin-top: 12px; padding-left: 142px; }
.eds-header-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.eds-header-row .bl-input:first-child { flex: 0 0 220px; }
.eds-header-row .bl-input:nth-child(2) { flex: 1; min-width: 0; }
.eds-add { display: inline-flex; align-items: center; padding: 6px 12px; background: transparent; border: 1px dashed var(--bl-border-strong);
  border-radius: 6px; color: var(--bl-text-2); font-size: 12.5px; cursor: pointer; }
.eds-add:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.eds-card .bl-textarea { width: 100%; box-sizing: border-box; }
.eds-ft { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: var(--bl-bg-1); border-top: 1px solid var(--bl-divider); }
.eds-err { font-size: 12.5px; color: #f53f3f; }
.eds-drawer-enter-active, .eds-drawer-leave-active { transition: transform .22s ease; }
.eds-drawer-enter-from, .eds-drawer-leave-to { transform: translateX(100%); }
</style>
