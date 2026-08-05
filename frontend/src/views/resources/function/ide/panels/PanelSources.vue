<template>
  <div class="idep-sources">
    <div class="idep-bar">
      <div class="idep-search">
        <span class="idep-search-ic" v-html="BL.icon('search', 11)"></span>
        <input class="idep-input" v-model="q" placeholder="搜索数据源 / 接口" />
      </div>
      <button class="idep-icon-btn" title="刷新" @click="load" v-html="BL.icon('refresh', 12)"></button>
    </div>

    <div class="idep-body">
      <div v-if="loading" class="idep-empty">加载中…</div>
      <template v-else>
        <template v-for="g in groups" :key="g.k">
          <div class="idep-group" @click="toggleGroup(g.k)">
            <span class="idep-group-chev" :class="opened.has(g.k) && 'is-open'" v-html="BL.icon('chevronRight', 10)"></span>
            {{ g.label }}<span class="idep-group-n">{{ g.items.length }}</span>
          </div>
          <template v-if="opened.has(g.k)">
            <div v-for="r in g.items" :key="g.k + r.id"
                 :class="['idep-row', selected === g.k + r.id && 'is-on']"
                 :title="r.sub" @click="select(g.k, r)">
              <span class="idep-row-ic" :style="{ background: g.color }" v-html="BL.icon(g.icon, 10, '#fff')"></span>
              <span class="idep-row-name">{{ r.label }}</span>
              <span class="idep-row-sub">{{ r.sub }}</span>
            </div>
            <div v-if="!g.items.length" class="idep-empty" style="padding:6px 12px">无</div>
          </template>
        </template>

        <!-- 选中项详情卡片 -->
        <div v-if="detail" class="idep-card">
          <div class="idep-kv"><span>名称</span><b>{{ detail.label }}</b></div>
          <div class="idep-kv"><span>类型</span><b>{{ detail.sub }}</b></div>
          <div v-if="detail.rid" class="idep-kv"><span>RID</span><b class="idep-mono">{{ detail.rid }}</b></div>
          <div v-if="detail.url" class="idep-kv"><span>地址</span><b class="idep-mono">{{ detail.url }}</b></div>
          <div v-if="detail.status !== undefined" class="idep-kv"><span>状态</span><b>{{ Number(detail.status) === 1 ? '启用' : '停用' }}</b></div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
/**
 * 接口 | 数据源面板 (文档「接口|数据源」)
 * 浏览平台已登记的数据库数据源、外部接口数据源与接口定义,点选查看源元数据。
 * 只读浏览:真正的"选择并注入调用代码"跟随资源导入面板的类型注入能力做。
 */
import { ref, computed, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { datasourceApi, extDatasourceApi, interfaceApi } from '@/api'

const q = ref('')
const loading = ref(false)
const sysDs = ref([])
const extDs = ref([])
const ifaces = ref([])
const opened = ref(new Set(['db', 'ext']))
const selected = ref('')
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const [a, b, c] = await Promise.all([
      datasourceApi.list().catch(() => []),
      extDatasourceApi.list().catch(() => []),
      interfaceApi.list().catch(() => []),
    ])
    sysDs.value = Array.isArray(a) ? a : []
    extDs.value = Array.isArray(b) ? b : []
    ifaces.value = Array.isArray(c) ? c : []
  } finally {
    loading.value = false
  }
}
onMounted(load)

/** 各模块字段命名不统一(Jackson 大小写差异), 统一在这里适配 */
function nameOf(d) { return d.ds_name || d.dsName || d.name || d.rdfs_label || d.id }
function typeOf(d) { return d.ds_type || d.dsType || d.type || '' }

const groups = computed(() => {
  const k = q.value.trim().toLowerCase()
  const hit = (label, sub) => !k || String(label).toLowerCase().includes(k) || String(sub).toLowerCase().includes(k)
  return [
    {
      k: 'db', label: '数据库数据源', icon: 'database', color: '#0FC6C2',
      items: sysDs.value.map(d => ({ id: d.id, label: nameOf(d), sub: typeOf(d) || 'DB', raw: d })).filter(r => hit(r.label, r.sub)),
    },
    {
      k: 'ext', label: '外部接口数据源', icon: 'globe', color: '#722ED1',
      items: extDs.value.map(d => ({ id: d.id, label: nameOf(d), sub: 'REST API', raw: d })).filter(r => hit(r.label, r.sub)),
    },
    {
      k: 'if', label: '接口', icon: 'plug', color: '#165DFF',
      items: ifaces.value.map(d => ({ id: d.id, label: d.rdfs_label || d.api_name || d.id, sub: d.api_name || '', raw: d })).filter(r => hit(r.label, r.sub)),
    },
  ]
})

function toggleGroup(k) {
  const s = new Set(opened.value)
  s.has(k) ? s.delete(k) : s.add(k)
  opened.value = s
}
function select(gk, r) {
  selected.value = gk + r.id
  const raw = r.raw || {}
  detail.value = {
    label: r.label,
    sub: r.sub,
    rid: raw.rid || '',
    url: raw.jdbc_url || raw.jdbcUrl || raw.base_url || raw.baseUrl || '',
    status: raw.status,
  }
}
</script>

<style scoped>
.idep-sources { display: flex; flex-direction: column; min-height: 0; }
.idep-body { flex: 1; min-height: 0; overflow: auto; }
.idep-mono { font-family: Consolas, Monaco, monospace; font-size: 11px; }
</style>
