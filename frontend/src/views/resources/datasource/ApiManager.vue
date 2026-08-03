<template>
  <div class="apm">
    <!-- 左: 目录导航 -->
    <div class="apm-left" :style="{ width: leftW + 'px' }">
      <ApiTree :ds="ds" :groups="groups" :apis="apis" :active-id="activeTab?.api?.id || ''"
               @open="openApi" @add="onAdd" @remove-group="removeGroup" @remove-api="removeApi" />
    </div>
    <div class="apm-vdrag" :class="{ 'is-on': dragging === 'x' }" @mousedown="startDrag('x', $event)"></div>

    <!-- 右: 标签 + 地址栏 + 上下分栏 -->
    <div class="apm-right">
      <div class="apm-tabbar">
        <div class="apm-tabs">
          <div v-for="t in tabs" :key="t.key" :class="['apm-tab', t.key === activeKey && 'is-on']" @click="activeKey = t.key">
            <span class="apm-tab-m" :style="{ color: METHOD_COLOR[t.api.method] || '#9b9b9b' }">{{ t.api.method }}</span>
            <span class="bl-truncate apm-tab-n">{{ t.api.api_name }}</span>
            <span v-if="t.dirty" class="apm-dot" title="有未保存修改"></span>
            <button class="apm-tab-x" @click.stop="closeTab(t.key)" v-html="BL.icon('x', 10)"></button>
          </div>
          <button class="apm-tab-add" title="新建接口" @click="onAdd('api')" v-html="BL.icon('plus', 12)"></button>
        </div>
        <button class="apm-newwin" title="在新窗口中打开" @click="openNewWindow" v-html="BL.icon('externalLink', 13)"></button>
      </div>

      <template v-if="activeTab">
        <!-- 地址栏: 方法 + 分段 URL + 发送/保存 -->
        <div class="apm-urlbar">
          <select class="apm-method" v-model="activeTab.api.method"
                  :style="{ color: METHOD_COLOR[activeTab.api.method] }">
            <option v-for="m in METHODS" :key="m" :value="m">{{ m }}</option>
          </select>
          <div class="apm-url">
            <span class="apm-url-base" :title="ds.base_url">{{ (ds.base_url || '').replace(/\/+$/, '') }}</span>
            <input class="apm-url-path" v-model="activeTab.api.api_path" placeholder="/path/to/api" spellcheck="false" />
          </div>
          <button class="apm-send" :disabled="sending" @click="send">{{ sending ? '发送中…' : '发送' }}</button>
          <button class="apm-save" :disabled="saving" @click="save">保存</button>
        </div>

        <div class="apm-req" :style="{ height: topH + '%' }">
          <ApiRequestPanel :api="activeTab.api" :params="activeTab.params" :ds="ds" />
        </div>
        <div class="apm-hdrag" :class="{ 'is-on': dragging === 'y' }" @mousedown="startDrag('y', $event)"></div>
        <div class="apm-resp">
          <ApiResponsePanel :resp="activeTab.resp" :loading="sending" :sample="activeTab.sample" />
        </div>
      </template>

      <div v-else class="apm-blank">
        <div class="apm-blank-t">未打开任何接口</div>
        <div class="apm-blank-d">从左侧目录选择一个接口，或点击「+」新建。</div>
        <button class="apm-blank-btn" @click="onAdd('api')"><span v-html="BL.icon('plus', 12)"></span>新建接口</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { extDatasourceApi } from '@/api'
import ApiTree from './ApiTree.vue'
import ApiRequestPanel from './ApiRequestPanel.vue'
import ApiResponsePanel from './ApiResponsePanel.vue'
import { METHODS, METHOD_COLOR, newApi, parseRequestParams, serializeParams,
         buildSendPayload, validateSendable } from './apiModel.js'

const route = useRoute()
const dsId = route.params.dsId

const ds = ref({})
const groups = ref([])
const apis = ref([])
const tabs = ref([])
const activeKey = ref('')
const sending = ref(false)
const saving = ref(false)

const activeTab = computed(() => tabs.value.find(t => t.key === activeKey.value) || null)

/* —— 布局拖拽: 左右宽度 200~400, 上下最小高度靠百分比约束 —— */
const leftW = ref(260)
const topH = ref(65)
const dragging = ref('')
let startPos = 0, startVal = 0
function startDrag(axis, e) {
  dragging.value = axis
  startPos = axis === 'x' ? e.clientX : e.clientY
  startVal = axis === 'x' ? leftW.value : topH.value
  document.body.style.cursor = axis === 'x' ? 'col-resize' : 'row-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', endDrag)
}
function onDrag(e) {
  if (dragging.value === 'x') {
    leftW.value = Math.min(400, Math.max(200, startVal + (e.clientX - startPos)))
  } else {
    const host = document.querySelector('.apm-right')
    const h = host ? host.clientHeight : window.innerHeight
    const delta = ((e.clientY - startPos) / h) * 100
    /* 上下都留至少 120px, 换算成百分比约束 */
    const minPct = (120 / h) * 100
    topH.value = Math.min(100 - minPct - 8, Math.max(minPct, startVal + delta))
  }
}
function endDrag() {
  dragging.value = ''
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', endDrag)
}
onBeforeUnmount(endDrag)

async function loadAll() {
  ds.value = await extDatasourceApi.get(dsId).catch(() => ({}))
  groups.value = await extDatasourceApi.groups(dsId).catch(() => [])
  apis.value = await extDatasourceApi.interfaces(dsId).catch(() => [])
}
onMounted(loadAll)

/* 同一接口只开一个标签, 重复点击切过去 */
function openApi(a) {
  const hit = tabs.value.find(t => t.api.id === a.id)
  if (hit) { activeKey.value = hit.key; return }
  tabs.value.push(makeTab(a))
  activeKey.value = a.id
}
function makeTab(a) {
  return {
    key: a.id, api: { ...a }, params: parseRequestParams(a),
    sample: parseSample(a), resp: null, dirty: false,
  }
}
function parseSample(a) {
  let s = {}
  try { s = typeof a?.response_params === 'string' ? JSON.parse(a.response_params) : (a?.response_params || {}) } catch { s = {} }
  return { json: s.json || '', fields: Array.isArray(s.fields) ? s.fields : [] }
}
function closeTab(key) {
  const i = tabs.value.findIndex(t => t.key === key)
  if (i < 0) return
  tabs.value.splice(i, 1)
  if (activeKey.value === key) activeKey.value = tabs.value[Math.max(0, i - 1)]?.key || ''
}

async function onAdd(kind) {
  if (kind === 'group') {
    const name = await BL.prompt({ title: '新增分组', label: '分组名称' })
    if (!name) return
    await extDatasourceApi.createGroup(dsId, { group_name: name })
    groups.value = await extDatasourceApi.groups(dsId).catch(() => [])
    return
  }
  const code = await BL.prompt({
    title: '新增接口', label: '接口编码 (数据源内唯一)',
    validate: v => /^[a-zA-Z][a-zA-Z0-9_]*$/.test(v || '') ? true : '需为字母开头的英文/数字/下划线',
  })
  if (!code) return
  try {
    const created = await extDatasourceApi.createApi(dsId, { ...newApi(), api_code: code, api_name: code })
    apis.value = await extDatasourceApi.interfaces(dsId).catch(() => [])
    openApi(created)
    BL.success('接口已创建')
  } catch (e) { BL.error(e?.msg || '创建失败') }
}

async function removeGroup(g) {
  const ok = await BL.confirm({ title: '删除分组', content: `确定删除「${g.group_name}」？组内接口会移到未分组，不会被删除。`, danger: true, okText: '删除' })
  if (!ok) return
  await extDatasourceApi.removeGroup(g.id)
  await loadAll()
}
async function removeApi(a) {
  const ok = await BL.confirm({ title: '删除接口', content: `确定删除「${a.api_name}」？`, danger: true, okText: '删除' })
  if (!ok) return
  await extDatasourceApi.removeApi(a.id)
  closeTab(a.id)
  apis.value = await extDatasourceApi.interfaces(dsId).catch(() => [])
}

async function save() {
  const t = activeTab.value
  if (!t) return
  saving.value = true
  try {
    const body = { ...t.api,
      request_params: serializeParams(t.params),
      response_params: JSON.stringify(t.sample),
    }
    const saved = await extDatasourceApi.updateApi(t.api.id, body)
    t.api = { ...saved }
    /* 保存不落文件内容, 内存里的已选文件继续保留, 不重新 parse 覆盖 */
    t.dirty = false
    apis.value = await extDatasourceApi.interfaces(dsId).catch(() => [])
    BL.success('已保存')
  } catch (e) { BL.error(e?.msg || '保存失败') } finally { saving.value = false }
}

async function send() {
  const t = activeTab.value
  if (!t) return
  const bad = validateSendable(t.params)
  if (bad) { BL.warning(bad); return }
  sending.value = true
  t.resp = null
  try {
    t.resp = await extDatasourceApi.sendApi(t.api.id, buildSendPayload(t.api, t.params))
  } catch (e) {
    t.resp = { ok: false, httpStatus: 0, costTime: 0, responseSize: 0, body: '', errorMsg: e?.msg || '请求失败' }
  } finally { sending.value = false }
}

function openNewWindow() { window.open(window.location.href, '_blank') }

/* 任一编辑动作都标脏, 提示未保存 */
watch(() => activeTab.value && [activeTab.value.api, activeTab.value.params, activeTab.value.sample],
  () => { if (activeTab.value) activeTab.value.dirty = true }, { deep: true })
</script>

<style scoped>
.apm { position: fixed; inset: 0; display: flex; background: #1e1e1e; color: #ccc;
  font-family: system-ui, -apple-system, 'Segoe UI', sans-serif; z-index: 5; }
.apm-left { flex-shrink: 0; min-width: 0; }
.apm-vdrag { width: 4px; cursor: col-resize; background: #333; flex-shrink: 0; transition: background .12s; }
.apm-vdrag:hover, .apm-vdrag.is-on { background: #3b82f6; }
.apm-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }

.apm-tabbar { display: flex; align-items: stretch; background: #252526; border-bottom: 1px solid #333; flex-shrink: 0; }
.apm-tabs { flex: 1; min-width: 0; display: flex; align-items: stretch; overflow-x: auto; }
.apm-tab { display: flex; align-items: center; gap: 6px; padding: 0 10px; height: 34px; max-width: 220px;
  border-right: 1px solid #333; color: #9b9b9b; font-size: 12px; cursor: pointer; flex-shrink: 0; }
.apm-tab:hover { color: #ddd; }
.apm-tab.is-on { background: #1e1e1e; color: #fff; border-top: 2px solid #3b82f6; }
.apm-tab-m { font-size: 10px; font-weight: 700; flex-shrink: 0; }
.apm-tab-n { max-width: 130px; }
.apm-dot { width: 6px; height: 6px; border-radius: 50%; background: #3b82f6; flex-shrink: 0; }
.apm-tab-x { border: 0; background: transparent; color: #7a7a7a; cursor: pointer; display: inline-flex; padding: 2px; border-radius: 3px; }
.apm-tab-x:hover { background: #4a4a4a; color: #fff; }
.apm-tab-add, .apm-newwin { border: 0; background: transparent; color: #9b9b9b; cursor: pointer; padding: 0 12px; display: inline-flex; align-items: center; }
.apm-tab-add:hover, .apm-newwin:hover { color: #fff; background: #333; }

.apm-urlbar { display: flex; align-items: center; gap: 8px; padding: 9px 12px; background: #252526; border-bottom: 1px solid #333; flex-shrink: 0; }
.apm-method { background: #2d2d2d; border: 1px solid #3d3d3d; border-radius: 5px; height: 30px; padding: 0 8px;
  font-size: 12px; font-weight: 700; outline: none; cursor: pointer; }
.apm-url { flex: 1; min-width: 0; display: flex; align-items: center; background: #2d2d2d; border: 1px solid #3d3d3d;
  border-radius: 5px; height: 30px; padding: 0 10px; }
.apm-url:focus-within { border-color: #3b82f6; }
.apm-url-base { color: #6b6b6b; font-size: 12px; font-family: var(--bl-mono, Consolas, monospace); white-space: nowrap; }
.apm-url-path { flex: 1; min-width: 0; background: transparent; border: 0; outline: none; color: #ddd;
  font-size: 12px; font-family: var(--bl-mono, Consolas, monospace); }
.apm-send { height: 30px; padding: 0 18px; border: 0; border-radius: 5px; background: #3b82f6; color: #fff; font-size: 12.5px; cursor: pointer; }
.apm-send:hover:not(:disabled) { background: #2f6fd6; }
.apm-send:disabled { opacity: .6; cursor: not-allowed; }
.apm-save { height: 30px; padding: 0 14px; border: 1px solid #3d3d3d; border-radius: 5px; background: transparent;
  color: #ddd; font-size: 12.5px; cursor: pointer; }
.apm-save:hover:not(:disabled) { border-color: #3b82f6; color: #3b82f6; }

.apm-req { min-height: 120px; overflow: hidden; }
.apm-hdrag { height: 4px; cursor: row-resize; background: #333; flex-shrink: 0; transition: background .12s; }
.apm-hdrag:hover, .apm-hdrag.is-on { background: #3b82f6; }
.apm-resp { flex: 1; min-height: 120px; overflow: hidden; }

.apm-blank { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; }
.apm-blank-t { font-size: 14px; color: #9b9b9b; }
.apm-blank-d { font-size: 12px; color: #6b6b6b; }
.apm-blank-btn { display: inline-flex; align-items: center; gap: 5px; margin-top: 6px; padding: 7px 16px;
  background: #3b82f6; border: 0; border-radius: 5px; color: #fff; font-size: 12.5px; cursor: pointer; }
</style>
