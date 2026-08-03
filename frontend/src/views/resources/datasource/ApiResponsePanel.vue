<template>
  <div class="arsp">
    <div class="arsp-tabs">
      <button v-for="t in RESP_TABS" :key="t.k" :class="['arsp-tab', tab === t.k && 'is-on']" @click="tab = t.k">{{ t.label }}</button>
      <span style="flex:1"></span>
      <!-- 状态摘要: 有响应才显示 -->
      <div v-if="resp" class="arsp-meta">
        <span :class="['arsp-status', resp.ok ? 'is-ok' : 'is-err']">
          {{ resp.httpStatus || '—' }} {{ statusText }}
        </span>
        <span class="arsp-metric" :class="costClass">{{ resp.costTime }} ms</span>
        <span class="arsp-metric">{{ formatSize(resp.responseSize) }}</span>
        <span v-if="resp.authApplied && resp.authApplied !== 'none'" class="arsp-auth" title="本次调用应用的鉴权方式">
          {{ resp.authApplied }}
        </span>
      </div>
    </div>

    <div class="arsp-body">
      <!-- 鉴权提示是附加信息, 不参与下面的分支互斥 -->
      <div v-if="!loading && resp?.authNote" class="arsp-warn">{{ resp.authNote }}</div>
      <div v-if="loading" class="arsp-hint">请求发送中…</div>

      <template v-else-if="tab === 'live'">
        <div v-if="!resp" class="arsp-hint">点击右上角「发送」执行接口,响应结果会显示在这里。</div>
        <template v-else>
          <div v-if="resp.errorMsg" class="arsp-err">{{ resp.errorMsg }}</div>
          <pre v-else class="arsp-code" :class="{ 'is-err': !resp.ok }">{{ prettyJson(resp.body) || '(空响应体)' }}</pre>
        </template>
      </template>

      <template v-else-if="tab === 'reqh'">
        <div v-if="!resp" class="arsp-hint">发送后展示本次调用实际携带的请求头。</div>
        <table v-else class="arsp-kv"><tbody>
          <tr v-for="(v, k) in (resp.reqHeaders || {})" :key="k"><td class="arsp-k">{{ k }}</td><td>{{ v }}</td></tr>
        </tbody></table>
      </template>

      <template v-else-if="tab === 'resph'">
        <div v-if="!resp" class="arsp-hint">发送后展示服务端返回的响应头。</div>
        <table v-else class="arsp-kv"><tbody>
          <tr v-for="(v, k) in (resp.respHeaders || {})" :key="k"><td class="arsp-k">{{ k }}</td><td>{{ v }}</td></tr>
        </tbody></table>
      </template>

      <!-- 成功响应示例: 左 JSON, 右 字段说明表 -->
      <template v-else>
        <div class="arsp-sample">
          <div class="arsp-sample-l">
            <div class="arsp-sample-hd">响应示例 (JSON)
              <button v-if="resp?.body" class="arsp-mini" title="用本次响应填充示例" @click="fillFromResp">从响应填充</button>
            </div>
            <textarea class="arsp-code arsp-edit" v-model="sample.json" rows="12" spellcheck="false"
                      placeholder='{ "code": 200, "msg": "ok", "data": {} }'></textarea>
          </div>
          <div class="arsp-sample-r">
            <div class="arsp-sample-hd">参数说明
              <button class="arsp-mini" @click="sample.fields.push(newField())">添加</button>
            </div>
            <table class="arsp-table">
              <thead><tr><th style="width:40px">启用</th><th>参数名</th><th>示例值</th><th style="width:44px">必填</th><th style="width:74px">类型</th><th>描述</th><th style="width:32px"></th></tr></thead>
              <tbody>
                <tr v-for="(f, i) in sample.fields" :key="i">
                  <td class="t-center"><input type="checkbox" v-model="f.enabled" :true-value="1" :false-value="0" /></td>
                  <td><input class="arsp-cell" v-model="f.name" placeholder="字段名" /></td>
                  <td><input class="arsp-cell" v-model="f.value" placeholder="示例" /></td>
                  <td class="t-center"><input type="checkbox" v-model="f.required" :true-value="1" :false-value="0" /></td>
                  <td>
                    <select class="arsp-cell" v-model="f.type">
                      <option v-for="t in FIELD_TYPES" :key="t" :value="t">{{ t }}</option>
                    </select>
                  </td>
                  <td><input class="arsp-cell" v-model="f.desc" placeholder="说明" /></td>
                  <td class="t-center"><button class="arsp-x" @click="sample.fields.splice(i,1)" v-html="BL.icon('x', 11)"></button></td>
                </tr>
                <tr v-if="!sample.fields.length"><td colspan="7" class="arsp-empty">暂无字段说明</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import { RESP_TABS, prettyJson, formatSize } from './apiModel.js'

const props = defineProps({
  resp: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  sample: { type: Object, required: true },   // { json, fields } 就地修改
})

const FIELD_TYPES = ['string', 'number', 'boolean', 'object', 'array']
const tab = ref('live')

const STATUS_TEXT = { 200:'OK', 201:'Created', 204:'No Content', 400:'Bad Request', 401:'Unauthorized',
  403:'Forbidden', 404:'Not Found', 408:'Request Timeout', 500:'Internal Server Error', 502:'Bad Gateway', 503:'Service Unavailable' }
const statusText = computed(() => STATUS_TEXT[props.resp?.httpStatus] || '')
/* 文档: 超 3000ms 预警, 超 10000ms 告警 */
const costClass = computed(() => {
  const c = Number(props.resp?.costTime) || 0
  return c > 10000 ? 'is-danger' : c > 3000 ? 'is-warn' : ''
})

function newField() { return { enabled: 1, name: '', value: '', required: 0, type: 'string', desc: '' } }
function fillFromResp() { props.sample.json = prettyJson(props.resp?.body || '') }
</script>

<style scoped>
.arsp { display: flex; flex-direction: column; height: 100%; min-height: 0; background: #1e1e1e; }
.arsp-tabs { display: flex; align-items: center; gap: 2px; padding: 0 12px; border-bottom: 1px solid #333; flex-shrink: 0; }
.arsp-tab { padding: 8px 13px; border: 0; background: transparent; color: #9b9b9b; font-size: 12.5px; cursor: pointer; }
.arsp-tab:hover { color: #ddd; }
.arsp-tab.is-on { color: #fff; box-shadow: inset 0 -2px 0 #3b82f6; }
.arsp-meta { display: flex; align-items: center; gap: 12px; font-size: 11.5px; }
.arsp-status { font-weight: 600; }
.arsp-status.is-ok { color: #4ade80; }
.arsp-status.is-err { color: #f87171; }
.arsp-metric { color: #9b9b9b; }
.arsp-metric.is-warn { color: #fb923c; }
.arsp-metric.is-danger { color: #f87171; }
.arsp-body { flex: 1; overflow: auto; padding: 12px 16px; }
.arsp-hint { font-size: 12px; color: #6b6b6b; padding: 20px 0; text-align: center; }
.arsp-err { font-size: 12.5px; color: #f87171; background: #2a1f1f; border: 1px solid #4a2a2a; border-radius: 5px; padding: 10px 14px; line-height: 1.7; }
.arsp-auth { color: #3b82f6; background: rgba(59,130,246,.14); border-radius: 3px; padding: 1px 6px; }
.arsp-warn { font-size: 12px; color: #f0a020; background: rgba(240,160,32,.1); border: 1px solid rgba(240,160,32,.3);
  border-radius: 5px; padding: 8px 12px; line-height: 1.6; margin-bottom: 10px; }
.arsp-code { font-family: var(--bl-mono, Consolas, monospace); font-size: 12px; line-height: 1.65; color: #4ade80;
  white-space: pre-wrap; word-break: break-all; margin: 0; }
.arsp-code.is-err { color: #f87171; }
.arsp-edit { width: 100%; box-sizing: border-box; background: #252526; border: 1px solid #3d3d3d; border-radius: 5px;
  padding: 8px 10px; color: #ddd; outline: none; resize: vertical; }
.arsp-edit:focus { border-color: #3b82f6; }
.arsp-kv { width: 100%; border-collapse: collapse; font-size: 12px; color: #ccc; }
.arsp-kv td { padding: 5px 8px; border-bottom: 1px solid #2a2a2a; word-break: break-all; }
.arsp-k { color: #9b9b9b; width: 220px; white-space: nowrap; }
.arsp-sample { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.arsp-sample-hd { display: flex; align-items: center; gap: 8px; font-size: 11.5px; color: #9b9b9b; margin-bottom: 6px; }
.arsp-mini { margin-left: auto; border: 1px solid #3d3d3d; background: transparent; color: #9b9b9b; font-size: 11px;
  padding: 2px 8px; border-radius: 4px; cursor: pointer; }
.arsp-mini:hover { border-color: #3b82f6; color: #3b82f6; }
.arsp-table { width: 100%; border-collapse: collapse; font-size: 11.5px; }
.arsp-table th { text-align: left; font-weight: 600; color: #9b9b9b; padding: 5px 6px; border-bottom: 1px solid #333; white-space: nowrap; }
.arsp-table td { padding: 2px 4px; border-bottom: 1px solid #2a2a2a; }
.arsp-table td.t-center, .arsp-table th.t-center { text-align: center; }
.arsp-cell { width: 100%; box-sizing: border-box; background: transparent; border: 1px solid transparent; border-radius: 4px;
  color: #ddd; font-size: 11.5px; height: 24px; padding: 0 5px; outline: none; }
.arsp-cell:hover { border-color: #3d3d3d; }
.arsp-cell:focus { border-color: #3b82f6; background: #2d2d2d; }
.arsp-x { border: 0; background: transparent; color: #7a7a7a; cursor: pointer; display: inline-flex; padding: 2px; border-radius: 3px; }
.arsp-x:hover { color: #f87171; background: #3a3a3a; }
.arsp-empty { text-align: center; color: #6b6b6b; padding: 12px; }
</style>
