<template>
  <div class="arp">
    <div class="arp-tabs">
      <button v-for="t in REQ_TABS" :key="t.k" :class="['arp-tab', tab === t.k && 'is-on']" @click="tab = t.k">
        {{ t.label }}
        <span v-if="badge(t.k)" class="arp-badge">{{ badge(t.k) }}</span>
      </button>
    </div>

    <div class="arp-body">
      <!-- 设置 -->
      <template v-if="tab === 'setting'">
        <div class="arp-sec">基础信息</div>
        <div class="arp-row3">
          <label class="arp-fld"><span class="arp-lbl">名称</span>
            <input class="arp-input" v-model="api.api_name" placeholder="接口中文显示名" /></label>
          <label class="arp-fld"><span class="arp-lbl">API 名</span>
            <input class="arp-input is-ro" :value="api.api_code" disabled /></label>
          <label class="arp-fld"><span class="arp-lbl">接口状态</span>
            <select class="arp-input" v-model="api.api_status">
              <option v-for="s in API_STATUS" :key="s.value" :value="s.value">{{ s.label }}</option>
            </select></label>
        </div>

        <div class="arp-sec">请求配置 <span class="arp-sec-hint">可覆盖数据源全局设置</span></div>
        <div class="arp-row3">
          <label class="arp-fld"><span class="arp-lbl">请求数据格式</span>
            <select class="arp-input" v-model="api.content_type">
              <option :value="null">继承默认：{{ ds.content_type || 'application/json' }}</option>
              <option v-for="c in CONTENT_TYPES" :key="c" :value="c">{{ c }}</option>
            </select></label>
          <label class="arp-fld"><span class="arp-lbl">单独超时时间</span>
            <div class="arp-inline">
              <input class="arp-input" type="number" v-model.number="api.timeout" placeholder="秒" />
              <span class="arp-tip">秒,为空则继承全局</span>
            </div></label>
          <label class="arp-fld"><span class="arp-lbl">继承全局 Header</span>
            <div class="arp-inline">
              <span class="arp-sw" :class="{ 'is-on': api.header_inherit === 1 }"
                    @click="api.header_inherit = api.header_inherit === 1 ? 0 : 1"><span class="arp-sw-dot"></span></span>
              <span class="arp-tip">关闭则不携带数据源公共请求头</span>
            </div></label>
        </div>

        <div class="arp-sec">接口描述</div>
        <textarea class="arp-input arp-textarea" v-model="api.description" rows="3"
                  placeholder="接口业务说明、适用场景等备注信息"></textarea>
      </template>

      <!-- 鉴权: 默认继承数据源, 打开开关后接口独立配置 -->
      <template v-else-if="tab === 'auth'">
        <div class="arp-sec">鉴权方式</div>
        <div class="arp-inline" style="margin-bottom:12px">
          <span class="arp-sw" :class="{ 'is-on': api.override_auth === 1 }" @click="toggleOverride"><span class="arp-sw-dot"></span></span>
          <span class="arp-swtxt">为该接口单独配置鉴权</span>
          <span class="arp-tip">关闭则继承数据源全局鉴权</span>
        </div>

        <div v-if="api.override_auth !== 1" class="arp-inherit">
          <div class="arp-inherit-hd">
            当前继承数据源全局鉴权：<b>{{ dsAuthMeta.label }}</b>
          </div>
          <div class="arp-inherit-d">{{ dsAuthMeta.desc }}</div>
          <table v-if="dsAuthSummary.length" class="arp-table" style="margin-top:10px">
            <tbody>
              <tr v-for="f in dsAuthSummary" :key="f.key">
                <td style="width:150px;color:#8b8b8b">{{ f.label }}</td>
                <td class="bl-mono">{{ f.text }}</td>
              </tr>
            </tbody>
          </table>
          <div class="arp-inherit-a">要修改全局鉴权，请到数据源详情的「配置」页。</div>
        </div>

        <template v-else>
          <div class="arp-row3">
            <label class="arp-fld"><span class="arp-lbl">鉴权类型 <i>*</i></span>
              <select class="arp-input" :value="authType" @change="onAuthTypeChange($event.target.value)">
                <option v-for="t in AUTH_TYPE_OPTS" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select></label>
          </div>
          <div class="arp-authdesc">{{ AUTH_META[authType]?.desc }}</div>

          <div v-if="authType === 'none'" class="arp-note">该接口不做任何认证，直接发起调用。</div>
          <div v-else class="arp-authbox">
            <div v-for="f in authFields" :key="f.key" class="arp-authrow">
              <span class="arp-lbl arp-authlbl">{{ f.label }} <i v-if="f.required">*</i></span>
              <div class="arp-authctl">
                <select v-if="f.type === 'select'" class="arp-input" :value="av(f)" @change="asetE(f, $event)">
                  <option v-for="o in f.options" :key="o.value" :value="o.value">{{ o.label }}</option>
                </select>
                <div v-else-if="f.type === 'multi'" class="arp-chips">
                  <label v-for="o in f.options" :key="o.value" class="arp-chip" :class="{ 'is-on': (av(f) || []).includes(o.value) }">
                    <input type="checkbox" :checked="(av(f) || []).includes(o.value)" @change="toggleMulti(f, o.value)" />
                    {{ o.label }}
                  </label>
                </div>
                <label v-else-if="f.type === 'switch'" class="arp-inline">
                  <span class="arp-sw" :class="{ 'is-on': Number(av(f)) === 1 }" @click="aset(f, Number(av(f)) === 1 ? 0 : 1)"><span class="arp-sw-dot"></span></span>
                  <span class="arp-swtxt">{{ Number(av(f)) === 1 ? '开启' : '关闭' }}</span>
                </label>
                <textarea v-else-if="f.type === 'textarea'" class="arp-input arp-code" rows="3"
                          :value="av(f)" @input="asetE(f, $event)" placeholder="粘贴密钥内容"></textarea>
                <input v-else-if="f.type === 'password'" class="arp-input" type="password" autocomplete="new-password"
                       :value="av(f)" @input="asetE(f, $event)" placeholder="••••••••" />
                <input v-else-if="f.type === 'number'" class="arp-input" type="number" :value="av(f)" @input="asetE(f, $event)" />
                <input v-else class="arp-input" :value="av(f)" @input="asetE(f, $event)" />
                <div v-if="f.tip" class="arp-tip" style="margin-top:4px">{{ f.tip }}</div>
              </div>
            </div>
          </div>
          <div v-if="authLimit" class="arp-note">{{ authLimit }}</div>
        </template>
      </template>

      <!-- Header / Query 同构表格 -->
      <template v-else-if="tab === 'header' || tab === 'query'">
        <table class="arp-table">
          <thead><tr>
            <th style="width:44px">启用</th><th style="width:30%">参数名</th><th style="width:32%">参数值</th><th>参数描述</th><th style="width:40px"></th>
          </tr></thead>
          <tbody>
            <tr v-for="(r, i) in rows" :key="i">
              <td class="t-center"><input type="checkbox" v-model="r.enabled" :true-value="1" :false-value="0" /></td>
              <td><input class="arp-cell" v-model="r.name" placeholder="参数名" /></td>
              <td><input class="arp-cell" v-model="r.value" placeholder="值" /></td>
              <td><input class="arp-cell" v-model="r.desc" placeholder="说明" /></td>
              <td class="t-center"><button class="arp-x" @click="rows.splice(i,1)" v-html="BL.icon('x', 11)"></button></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="5" class="arp-empty">暂无参数,点下方添加</td></tr>
          </tbody>
        </table>
        <button class="arp-add" @click="rows.push(newParamRow())"><span v-html="BL.icon('plus', 12)"></span>添加参数</button>
        <div v-if="tab === 'header' && api.header_inherit === 1" class="arp-note">
          已开启「继承全局 Header」,数据源配置的公共请求头会在调用时自动携带,此处只列接口自身的 Header。
        </div>
      </template>

      <!-- Body -->
      <template v-else>
        <div class="arp-modes">
          <label v-for="m in BODY_MODES" :key="m.value" class="arp-mode">
            <input type="radio" :value="m.value" v-model="params.body.mode" /><span>{{ m.label }}</span>
          </label>
        </div>
        <div v-if="params.body.mode === 'none'" class="arp-note">该请求不发送 Body。</div>
        <textarea v-else-if="params.body.mode === 'raw'" class="arp-input arp-code" v-model="params.body.raw" rows="10"
                  spellcheck="false" placeholder='{ "key": "value" }'></textarea>
        <template v-else-if="params.body.mode === 'binary'">
          <div class="arp-binary">
            <FilePick :file="params.body.file" @pick="f => params.body.file = f" @clear="params.body.file = null" />
            <div class="arp-tip" style="margin-top:8px">整个请求体就是这一个文件的原始字节，Content-Type 取文件自身类型。单个文件不超过 10MB。</div>
          </div>
        </template>
        <template v-else>
          <table class="arp-table">
            <thead><tr>
              <th style="width:44px">启用</th><th style="width:26%">参数名</th>
              <th v-if="isFormData" style="width:74px">类型</th>
              <th style="width:34%">参数值</th><th>参数描述</th><th style="width:40px"></th>
            </tr></thead>
            <tbody>
              <tr v-for="(r, i) in params.body.form" :key="i">
                <td class="t-center"><input type="checkbox" v-model="r.enabled" :true-value="1" :false-value="0" /></td>
                <td><input class="arp-cell" v-model="r.name" placeholder="参数名" /></td>
                <td v-if="isFormData">
                  <select class="arp-cell arp-cellsel" v-model="r.type">
                    <option v-for="t in PART_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
                  </select>
                </td>
                <td>
                  <FilePick v-if="isFormData && r.type === 'file'" compact :file="r.file"
                            @pick="f => r.file = f" @clear="r.file = null" />
                  <input v-else class="arp-cell" v-model="r.value" placeholder="值" />
                </td>
                <td><input class="arp-cell" v-model="r.desc" placeholder="说明" /></td>
                <td class="t-center"><button class="arp-x" @click="params.body.form.splice(i,1)" v-html="BL.icon('x', 11)"></button></td>
              </tr>
              <tr v-if="!params.body.form.length"><td :colspan="isFormData ? 6 : 5" class="arp-empty">暂无参数,点下方添加</td></tr>
            </tbody>
          </table>
          <button class="arp-add" @click="params.body.form.push(newParamRow())"><span v-html="BL.icon('plus', 12)"></span>添加参数</button>
          <div v-if="isFormData && hasStaleFile" class="arp-note">
            文件内容不会随接口定义保存，只保留文件名。重新打开接口后需再次选择文件才能发送。
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import FilePick from './FilePick.vue'
import { REQ_TABS, API_STATUS, CONTENT_TYPES, BODY_MODES, PART_TYPES, newParamRow } from './apiModel.js'
import { AUTH_TYPE_OPTS, AUTH_META, AUTH_FIELDS, visibleAuthFields, defaultAuthConfig } from './authModel.js'

const props = defineProps({
  api: { type: Object, required: true },        // 就地修改
  params: { type: Object, required: true },     // { header, query, body }
  ds: { type: Object, default: () => ({}) },
})

const tab = ref('setting')
const rows = computed(() => tab.value === 'header' ? props.params.header : props.params.query)
const isFormData = computed(() => props.params.body.mode === 'form-data')
/* 文件内容不入库, 重新打开接口时只剩文件名 */
const hasStaleFile = computed(() => props.params.body.form.some(r => r.type === 'file' && r.file && !r.file.data))

function badge(k) {
  if (k === 'header') return props.params.header.filter(r => r.enabled && r.name).length
  if (k === 'query') return props.params.query.filter(r => r.enabled && r.name).length
  if (k === 'body') return props.params.body.mode !== 'none' ? 1 : 0
  if (k === 'auth') return props.api.override_auth === 1 ? '覆盖' : 0
  return 0
}

/* ===== 鉴权 ===== */

const authType = computed(() => props.api.auth_type || 'none')
const authCfg = computed(() => {
  try { return typeof props.api.auth_config === 'string' ? (JSON.parse(props.api.auth_config || '{}') || {}) : (props.api.auth_config || {}) }
  catch { return {} }
})
const authFields = computed(() => visibleAuthFields(authType.value, authCfg.value))

/* 后端在线调试只能自动完成本地可算出的鉴权, 交互式流程明确告知 */
const AUTH_LIMIT = {
  oauth2_code: '授权码模式需要用户在浏览器完成授权，在线调试无法自动取得令牌，发送时不会附带鉴权信息。',
  cas: 'CAS 单点登录需要跳转登录页换取 Ticket，在线调试无法自动完成，发送时不会附带鉴权信息。',
  digest: 'Digest 摘要认证需要先接收服务端 401 挑战再应答，在线调试暂不支持，发送时不会附带鉴权信息。',
}
const authLimit = computed(() => AUTH_LIMIT[authType.value] || '')

function toggleOverride() {
  const on = props.api.override_auth === 1
  props.api.override_auth = on ? 0 : 1
  /* 首次打开覆盖时, 以数据源当前鉴权为起点, 少填一遍 */
  if (!on && !props.api.auth_type) {
    props.api.auth_type = props.ds.auth_type || 'none'
    props.api.auth_config = props.ds.auth_config || JSON.stringify(defaultAuthConfig(props.ds.auth_type || 'none'))
  }
}
function onAuthTypeChange(t) {
  props.api.auth_type = t
  props.api.auth_config = JSON.stringify(defaultAuthConfig(t))
}
function av(f) { const v = authCfg.value[f.key]; return v === undefined || v === null ? (f.def !== undefined ? f.def : '') : v }
function aset(f, v) { props.api.auth_config = JSON.stringify({ ...authCfg.value, [f.key]: v }) }
function asetE(f, e) { aset(f, e.target.value) }
function toggleMulti(f, v) {
  const cur = Array.isArray(av(f)) ? av(f) : []
  aset(f, cur.includes(v) ? cur.filter(x => x !== v) : [...cur, v])
}

/* 继承态摘要: 展示数据源鉴权的关键字段, 密钥类一律掩码 */
const dsAuthMeta = computed(() => AUTH_META[props.ds.auth_type || 'none'] || AUTH_META.none)
const dsAuthSummary = computed(() => {
  const type = props.ds.auth_type || 'none'
  let cfg = {}
  try { cfg = typeof props.ds.auth_config === 'string' ? (JSON.parse(props.ds.auth_config || '{}') || {}) : (props.ds.auth_config || {}) } catch { cfg = {} }
  return (AUTH_FIELDS[type] || []).map(f => {
    const v = cfg[f.key] ?? f.def
    if (v === undefined || v === null || v === '') return null
    const secret = f.type === 'password' || f.secret
    return { key: f.key, label: f.label, text: secret ? '••••••••' : (Array.isArray(v) ? v.join(', ') : String(v)) }
  }).filter(Boolean)
})
</script>

<style scoped>
.arp { display: flex; flex-direction: column; height: 100%; min-height: 0; background: #1e1e1e; }
.arp-tabs { display: flex; gap: 2px; padding: 0 12px; border-bottom: 1px solid #333; flex-shrink: 0; }
.arp-tab { position: relative; padding: 9px 14px; border: 0; background: transparent; color: #9b9b9b; font-size: 12.5px; cursor: pointer; }
.arp-tab:hover { color: #ddd; }
.arp-tab.is-on { color: #fff; box-shadow: inset 0 -2px 0 #3b82f6; }
.arp-badge { margin-left: 5px; font-size: 10px; background: #3a3a3a; color: #bbb; border-radius: 8px; padding: 1px 5px; }
.arp-body { flex: 1; overflow-y: auto; padding: 14px 16px; }
.arp-sec { font-size: 12px; font-weight: 600; color: #ddd; padding-left: 7px; border-left: 3px solid #3b82f6; margin: 4px 0 12px; }
.arp-sec:not(:first-child) { margin-top: 20px; }
.arp-sec-hint { font-weight: 400; color: #7a7a7a; font-size: 11px; margin-left: 6px; }
.arp-row3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 16px; }
.arp-fld { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.arp-lbl { font-size: 11.5px; color: #9b9b9b; }
.arp-input { background: #2d2d2d; border: 1px solid #3d3d3d; border-radius: 5px; color: #ddd; font-size: 12.5px;
  height: 30px; padding: 0 9px; outline: none; width: 100%; box-sizing: border-box; }
.arp-input:focus { border-color: #3b82f6; }
.arp-input.is-ro { color: #8b8b8b; background: #262626; }
.arp-textarea, .arp-code { height: auto; padding: 8px 10px; line-height: 1.6; resize: vertical; }
.arp-code { font-family: var(--bl-mono, Consolas, monospace); font-size: 12px; }
.arp-inline { display: flex; align-items: center; gap: 8px; }
.arp-tip { font-size: 11px; color: #7a7a7a; white-space: nowrap; }
.arp-sw { display: inline-block; width: 34px; height: 18px; border-radius: 9px; background: #4a4a4a; position: relative; cursor: pointer; flex-shrink: 0; }
.arp-sw.is-on { background: #3b82f6; }
.arp-sw-dot { position: absolute; left: 2px; top: 2px; width: 14px; height: 14px; border-radius: 50%; background: #fff; transition: left .15s; }
.arp-sw.is-on .arp-sw-dot { left: 18px; }
.arp-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.arp-table th { text-align: left; font-weight: 600; color: #9b9b9b; padding: 6px 8px; border-bottom: 1px solid #333; white-space: nowrap; }
.arp-table td { padding: 3px 6px; border-bottom: 1px solid #2a2a2a; }
.arp-table td.t-center, .arp-table th.t-center { text-align: center; }
.arp-cell { width: 100%; box-sizing: border-box; background: transparent; border: 1px solid transparent; border-radius: 4px;
  color: #ddd; font-size: 12px; height: 26px; padding: 0 6px; outline: none; }
.arp-cell:hover { border-color: #3d3d3d; }
.arp-cell:focus { border-color: #3b82f6; background: #2d2d2d; }
.arp-x { border: 0; background: transparent; color: #7a7a7a; cursor: pointer; display: inline-flex; padding: 3px; border-radius: 3px; }
.arp-x:hover { color: #f87171; background: #3a3a3a; }
.arp-empty { text-align: center; color: #6b6b6b; padding: 14px; font-size: 11.5px; }
.arp-add { display: inline-flex; align-items: center; gap: 5px; margin-top: 10px; padding: 6px 12px; background: transparent;
  border: 1px dashed #4a4a4a; border-radius: 5px; color: #9b9b9b; font-size: 12px; cursor: pointer; }
.arp-add:hover { border-color: #3b82f6; color: #3b82f6; }
.arp-note { font-size: 11.5px; color: #7a7a7a; line-height: 1.7; margin-top: 12px; background: #262626;
  border: 1px solid #333; border-radius: 5px; padding: 9px 12px; }
.arp-modes { display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 14px; }
.arp-mode { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: #bbb; cursor: pointer; }
.arp-swtxt { font-size: 12px; color: #bbb; }
.arp-lbl i { color: #f87171; font-style: normal; margin-left: 2px; }
.arp-cellsel { cursor: pointer; }
.arp-binary { border: 1px dashed #4a4a4a; border-radius: 6px; padding: 18px; text-align: center; }
.arp-binary .fpk { justify-content: center; }
/* 鉴权 */
.arp-inherit { background: #262626; border: 1px solid #333; border-radius: 6px; padding: 12px 14px; }
.arp-inherit-hd { font-size: 12.5px; color: #ddd; }
.arp-inherit-hd b { color: #3b82f6; font-weight: 600; }
.arp-inherit-d { font-size: 11.5px; color: #7a7a7a; margin-top: 4px; }
.arp-inherit-a { font-size: 11px; color: #6b6b6b; margin-top: 10px; }
.arp-authdesc { font-size: 11.5px; color: #7a7a7a; margin: 6px 0 12px; }
.arp-authbox { background: #262626; border: 1px solid #333; border-radius: 8px; padding: 14px 16px;
  display: flex; flex-direction: column; gap: 12px; }
.arp-authrow { display: flex; align-items: flex-start; gap: 10px; }
.arp-authlbl { flex: 0 0 148px; text-align: right; line-height: 30px; }
.arp-authctl { flex: 1; min-width: 0; max-width: 520px; }
.arp-chips { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; min-height: 30px; }
.arp-chip { display: inline-flex; align-items: center; gap: 5px; padding: 3px 9px; border: 1px solid #3d3d3d;
  border-radius: 4px; font-size: 11.5px; color: #9b9b9b; cursor: pointer; }
.arp-chip.is-on { border-color: #3b82f6; color: #3b82f6; background: rgba(59,130,246,.12); }
</style>
