<template>
  <div class="idep-imports">
    <!-- 分类标签栏 -->
    <div class="idep-tabs">
      <button v-for="t in TABS" :key="t.k" :class="['idep-tab', tab === t.k && 'is-on']" @click="tab = t.k">
        <span v-if="t.icon" v-html="BL.icon(t.icon, 10)"></span>
        <span v-else-if="t.text" class="idep-fx">{{ t.text }}</span>
        {{ t.label }}<span v-if="t.k !== 'all'" class="idep-tab-n">{{ counts[t.k] }}</span>
      </button>
    </div>

    <!-- 筛选 + 添加 -->
    <div class="idep-bar">
      <div class="idep-search">
        <span class="idep-search-ic" v-html="BL.icon('search', 11)"></span>
        <input class="idep-input" v-model="q" placeholder="筛选..." />
      </div>
      <div class="idep-add-wrap" ref="addWrap">
        <button class="idep-btn" @click="addOpen = !addOpen">
          添加<span v-html="BL.icon('chevronDown', 10)"></span>
        </button>
        <div v-if="addOpen" class="idep-add-pop">
          <div v-for="t in KINDS" :key="t.k" class="idep-add-item" @click="startPick(t.k)">
            <span v-if="t.icon" v-html="BL.icon(t.icon, 11)"></span>
            <span v-else class="idep-fx">{{ t.text }}</span>{{ t.label }}
          </div>
        </div>
      </div>
    </div>

    <!-- 选择模式:列候选资源 -->
    <template v-if="picking">
      <div class="idep-picking">
        <span>选择要导入的{{ kindLabel(picking) }}</span>
        <button class="idep-icon-btn" title="返回" @click="picking = ''" v-html="BL.icon('x', 12)"></button>
      </div>
      <div class="idep-body">
        <div v-if="loadingCandidates" class="idep-empty">加载中…</div>
        <template v-else>
          <div v-for="c in candidates" :key="c.id" class="idep-row" @click="addResource(picking, c)">
            <span class="idep-row-ic" :style="{ background: c.color || KIND_COLOR[picking] }"
                  v-html="BL.icon(c.icon || KIND_ICON[picking], 10, '#fff')"></span>
            <span class="idep-row-name">{{ c.label }}</span>
            <span class="idep-row-sub">{{ c.sub }}</span>
          </div>
          <div v-if="!candidates.length" class="idep-empty">{{ emptyCandidateHint }}</div>
        </template>
      </div>
    </template>

    <!-- 已导入资源:按类型分组 -->
    <div v-else class="idep-body">
      <template v-for="g in visibleGroups" :key="g.k">
        <div class="idep-group" @click="toggleGroup(g.k)">
          <span class="idep-group-chev" :class="opened.has(g.k) && 'is-open'" v-html="BL.icon('chevronRight', 10)"></span>
          {{ g.label }}<span class="idep-group-n">{{ g.items.length }}</span>
        </div>
        <template v-if="opened.has(g.k)">
          <div v-for="r in g.items" :key="g.k + r.id" class="idep-row" :title="r.sub">
            <span class="idep-row-ic" :style="{ background: r.color || KIND_COLOR[g.k] }"
                  v-html="BL.icon(r.icon || KIND_ICON[g.k], 10, '#fff')"></span>
            <span class="idep-row-name">{{ r.label }}</span>
            <button class="idep-row-act" title="移除" @click.stop="remove(g.k, r.id)" v-html="BL.icon('trash', 11)"></button>
          </div>
          <div v-if="!g.items.length" class="idep-empty" style="padding:6px 12px">未导入</div>
        </template>
      </template>
      <div class="idep-hint">
        导入的本体对象会自动生成 TypeScript 类型定义并注入编辑器,代码里可直接获得补全与类型校验。
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 资源导入 (文档「资源导入 Resource Imports」)
 *
 * 四类资源:本体 / 函数 / 模型 / 数据源。导入的本体对象会拉取其属性列表,
 * 生成 .d.ts 并通过 monaco 的 addExtraLib 注入到 TypeScript 语言服务里,
 * 让编辑器对本体类型有补全与校验能力。
 *
 * 持久化:存 localStorage(按分支隔离),**不写进代码仓** ——
 * 每导入一个资源就产生一个 git 提交会把历史打得很碎;等 P8 做批量变更集时再考虑落仓。
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as monaco from 'monaco-editor'
import { BL } from '@/lib/bl.js'
import { resourceApi, functionApi, datasourceApi, extDatasourceApi } from '@/api'

const props = defineProps({
  /** 按分支隔离导入清单 */
  branch: { type: String, default: 'master' }
})

const KINDS = [
  { k: 'ontology', label: '本体', icon: 'cube' },
  { k: 'functions', label: '查询函数', text: 'ƒx' },
  { k: 'models', label: '模型', icon: 'trendingUp' },
  { k: 'sources', label: '数据源', icon: 'globe' },
]
const TABS = [{ k: 'all', label: '全部' }, ...KINDS.map(k => ({ ...k, label: k.k === 'functions' ? '函数' : k.label }))]
const KIND_ICON = { ontology: 'cube', functions: 'code', models: 'trendingUp', sources: 'globe' }
const KIND_COLOR = { ontology: '#165DFF', functions: '#722ED1', models: '#00B42A', sources: '#0FC6C2' }

const tab = ref('all')
const q = ref('')
const addOpen = ref(false)
const addWrap = ref(null)
const picking = ref('')
const loadingCandidates = ref(false)
const candidates = ref([])
const opened = ref(new Set(['ontology']))     // 默认「本体」展开(文档)
const imports = ref({ ontology: [], functions: [], models: [], sources: [] })

const storeKey = computed(() => `bl.ide.imports.${props.branch || 'master'}`)

function load() {
  try {
    const raw = localStorage.getItem(storeKey.value)
    const d = raw ? JSON.parse(raw) : {}
    imports.value = {
      ontology: d.ontology || [], functions: d.functions || [],
      models: d.models || [], sources: d.sources || [],
    }
  } catch {
    imports.value = { ontology: [], functions: [], models: [], sources: [] }
  }
}
function persist() {
  try { localStorage.setItem(storeKey.value, JSON.stringify(imports.value)) } catch { /* ignore */ }
}
onMounted(() => { load(); injectAllTypes(); window.addEventListener('click', onDocClick) })
onUnmounted(() => window.removeEventListener('click', onDocClick))
watch(storeKey, () => { load(); injectAllTypes() })

function onDocClick(e) { if (addWrap.value && !addWrap.value.contains(e.target)) addOpen.value = false }

const counts = computed(() => ({
  ontology: imports.value.ontology.length,
  functions: imports.value.functions.length,
  models: imports.value.models.length,
  sources: imports.value.sources.length,
}))

const visibleGroups = computed(() => {
  const k = q.value.trim().toLowerCase()
  return KINDS
    .filter(g => tab.value === 'all' || tab.value === g.k)
    .map(g => ({
      k: g.k,
      label: `${g.label}(${g.k === 'ontology' ? 'Ontology' : g.k === 'functions' ? 'Functions' : g.k === 'models' ? 'Models' : 'Sources'})`,
      items: (imports.value[g.k] || []).filter(r => !k || String(r.label).toLowerCase().includes(k)),
    }))
})

function toggleGroup(k) {
  const s = new Set(opened.value)
  s.has(k) ? s.delete(k) : s.add(k)
  opened.value = s
}
function kindLabel(k) { return (KINDS.find(x => x.k === k) || {}).label || '' }
const emptyCandidateHint = computed(() =>
  picking.value === 'models' ? '平台尚未接入模型资产中心,暂无可导入的模型' : '没有可导入的资源')

/* —— 候选资源 —— */
async function startPick(kind) {
  addOpen.value = false
  picking.value = kind
  loadingCandidates.value = true
  candidates.value = []
  try {
    if (kind === 'ontology') {
      const list = await resourceApi.classes().catch(() => [])
      candidates.value = (Array.isArray(list) ? list : []).map(c => ({
        id: c.id, label: c.display_name || c.rdfs_label || c.api_name,
        sub: c.api_name, api_name: c.api_name, ns: c.ns_code, icon: c.icon, color: c.color,
      }))
    } else if (kind === 'functions') {
      const list = await functionApi.list().catch(() => [])
      candidates.value = (Array.isArray(list) ? list : []).map(f => ({
        id: f.id, label: f.api_name, sub: f.function_label || '', api_name: f.api_name,
      }))
    } else if (kind === 'sources') {
      const [sys, ext] = await Promise.all([
        datasourceApi.list().catch(() => []), extDatasourceApi.list().catch(() => []),
      ])
      candidates.value = [
        ...(Array.isArray(sys) ? sys : []).map(d => ({ id: 'ds-' + d.id, label: d.ds_name || d.dsName || d.name || d.id, sub: d.ds_type || d.dsType || 'DB' })),
        ...(Array.isArray(ext) ? ext : []).map(d => ({ id: 'ext-' + d.id, label: d.ds_name || d.dsName || d.name || d.id, sub: 'REST API' })),
      ]
    } else {
      candidates.value = []           // 模型:平台暂无资产中心
    }
    // 已导入的不再出现在候选里
    const has = new Set((imports.value[kind] || []).map(r => r.id))
    candidates.value = candidates.value.filter(c => !has.has(c.id))
  } finally {
    loadingCandidates.value = false
  }
}

async function addResource(kind, c) {
  const list = imports.value[kind] || []
  if (list.some(x => x.id === c.id)) return
  list.push({ id: c.id, label: c.label, sub: c.sub, api_name: c.api_name, ns: c.ns, icon: c.icon, color: c.color })
  imports.value[kind] = list
  persist()
  opened.value = new Set([...opened.value, kind])
  picking.value = ''
  BL.success(`已导入 ${c.label}`)
  if (kind === 'ontology') await injectClassType(c)
}

function remove(kind, id) {
  imports.value[kind] = (imports.value[kind] || []).filter(r => r.id !== id)
  persist()
  if (kind === 'ontology') injectAllTypes()      // 重建类型定义
}

/* —— 本体对象 → TypeScript 类型定义 —— */
const TS_TYPE = {
  String: 'string', Text: 'string', Date: 'string', DateTime: 'string', Timestamp: 'string',
  Integer: 'number', Long: 'number', Double: 'number', Decimal: 'number', Float: 'number', Number: 'number',
  Boolean: 'boolean',
}
function tsTypeOf(p) {
  const base = TS_TYPE[p.vt_base_type] || 'any'
  return Number(p.is_multi_valued_prop) === 1 ? `${base}[]` : base
}

let extraLibDisposable = null
async function injectClassType() { await injectAllTypes() }

/** 把所有已导入本体对象合成一份 .d.ts 注入 monaco 的 TS 语言服务 */
async function injectAllTypes() {
  const list = imports.value.ontology || []
  const chunks = []
  for (const r of list) {
    const d = await resourceApi.classDetail(r.id).catch(() => null)
    if (!d) continue
    const name = d.api_name || r.api_name || r.label
    const props = d.properties || []
    const lines = [`/** ${d.display_name || d.rdfs_label || name}${d.ns_code ? ` · ${d.ns_code}` : ''} */`,
                   `declare interface ${name} {`]
    props.forEach(p => {
      const pn = p.rdfs_label || p.api_name
      if (!pn) return
      lines.push(`  /** ${p.display_name || pn}${Number(p.is_primary) === 1 ? ' (主键)' : ''} */`)
      lines.push(`  ${pn}: ${tsTypeOf(p)};`)
    })
    lines.push('}')
    chunks.push(lines.join('\n'))
  }
  const content = chunks.length
    ? `// 由 BOntoLink 资源导入自动生成 —— 请勿手改\n\n${chunks.join('\n\n')}\n`
    : ''
  try {
    extraLibDisposable?.dispose()
    extraLibDisposable = content
      ? monaco.languages.typescript.typescriptDefaults.addExtraLib(content, 'ts:bontolink/ontology.d.ts')
      : null
  } catch (e) {
    console.warn('[ide] 注入本体类型定义失败:', e)
  }
}
</script>

<style scoped>
.idep-imports { display: flex; flex-direction: column; min-height: 0; }
.idep-body { flex: 1; min-height: 0; overflow: auto; }
.idep-fx { font-family: Consolas, Monaco, monospace; font-style: italic; }
.idep-add-wrap { position: relative; flex-shrink: 0; }
.idep-add-pop {
  position: absolute; top: calc(100% + 3px); right: 0; z-index: 30; min-width: 132px;
  background: var(--ide-menu-bg); border: 1px solid var(--ide-border);
  border-radius: 4px; box-shadow: 0 6px 18px rgba(0, 0, 0, .4); padding: 3px 0;
}
.idep-add-item {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 10px; font-size: 12px; color: var(--ide-text); cursor: pointer;
}
.idep-add-item:hover { background: var(--ide-active); }
.idep-picking {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 8px; font-size: 11.5px; color: var(--ide-text-dim);
  background: var(--ide-bg); border-top: 1px solid var(--ide-border); border-bottom: 1px solid var(--ide-border);
}
</style>
