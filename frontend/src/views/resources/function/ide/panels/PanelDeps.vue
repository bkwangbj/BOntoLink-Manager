<template>
  <div class="idep-deps">
    <div class="idep-bar">
      <div class="idep-search">
        <span class="idep-search-ic" v-html="BL.icon('search', 11)"></span>
        <input class="idep-input" v-model="q" placeholder="搜索依赖包" @keydown.enter="searchRegistry" />
      </div>
      <button v-if="q" class="idep-icon-btn" title="清空" @click="clearSearch" v-html="BL.icon('x', 12)"></button>
    </div>

    <div class="idep-body">
      <div v-if="loading" class="idep-empty">加载中…</div>

      <!-- 仓库没有 package.json -->
      <template v-else-if="!pkg">
        <div class="idep-empty">
          仓库根目录没有 package.json,无法管理依赖。
        </div>
        <div style="padding:0 8px">
          <button class="idep-btn is-primary" :disabled="creating" @click="createPkg">
            <span v-html="BL.icon('plus', 11)"></span>{{ creating ? '创建中…' : '创建 package.json' }}
          </button>
        </div>
      </template>

      <template v-else>
        <!-- npm 搜索结果 -->
        <template v-if="searchResults.length || searching || searchError">
          <div class="idep-group" style="cursor:default">
            搜索结果<span class="idep-group-n">{{ searchResults.length }}</span>
          </div>
          <div v-if="searching" class="idep-empty">搜索中…</div>
          <div v-else-if="searchError" class="idep-empty">{{ searchError }}</div>
          <div v-for="r in searchResults" :key="r.name" class="idep-row" :title="r.description">
            <span class="idep-row-ic" style="background:#cb3837" v-html="BL.icon('package', 10, '#fff')"></span>
            <span class="idep-row-name">{{ r.name }}</span>
            <span class="idep-row-sub">{{ r.version }}</span>
            <button class="idep-row-act idep-add" title="加入依赖" @click.stop="addDep(r.name, '^' + r.version)"
                    v-html="BL.icon('plus', 11)"></button>
          </div>
        </template>

        <!-- 已声明依赖 -->
        <template v-for="g in groups" :key="g.k">
          <div class="idep-group" @click="toggleGroup(g.k)">
            <span class="idep-group-chev" :class="opened.has(g.k) && 'is-open'" v-html="BL.icon('chevronRight', 10)"></span>
            {{ g.label }}<span class="idep-group-n">{{ g.items.length }}</span>
          </div>
          <template v-if="opened.has(g.k)">
            <div v-for="d in g.items" :key="g.k + d.name" class="idep-row" :title="`${d.name}@${d.version}`">
              <span class="idep-row-ic" style="background:#cb3837" v-html="BL.icon('package', 10, '#fff')"></span>
              <span class="idep-row-name">{{ d.name }}</span>
              <span class="idep-row-sub">{{ d.version }}</span>
              <button class="idep-row-act" title="移除" @click.stop="removeDep(g.k, d.name)" v-html="BL.icon('trash', 11)"></button>
            </div>
            <div v-if="!g.items.length" class="idep-empty" style="padding:6px 12px">无</div>
          </template>
        </template>

        <!-- 手动添加 -->
        <div class="idep-manual">
          <input class="idep-input is-plain" v-model="manualName" placeholder="包名" />
          <input class="idep-input is-plain idep-ver" v-model="manualVer" placeholder="版本" />
          <button class="idep-btn" :disabled="!manualName.trim() || busy" @click="addDep(manualName.trim(), manualVer.trim() || 'latest')">添加</button>
        </div>
        <div class="idep-hint">
          改动会写回仓库的 package.json 并生成一次本地提交;真正的 npm 安装(下载 node_modules)在 P7 的终端能力落地后才能做。
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
/**
 * 依赖库管理 (文档「依赖」)
 *
 * 读写代码仓根目录的 package.json:列出 dependencies / devDependencies、增删条目。
 * npm 搜索走公共 registry,网络不通时给出明确提示而不是静默空列表 —— 内网环境本来就可能访问不到。
 *
 * 注意:这里只维护"依赖声明",不执行 npm install。真正的安装需要服务端终端/构建能力(P7)。
 */
import { ref, computed, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { fnRepoApi } from '@/api'

const PKG_PATH = 'package.json'

const loading = ref(false)
const busy = ref(false)
const creating = ref(false)
const pkg = ref(null)
const q = ref('')
const searching = ref(false)
const searchError = ref('')
const searchResults = ref([])
const opened = ref(new Set(['dependencies', 'devDependencies']))
const manualName = ref('')
const manualVer = ref('')

async function load() {
  loading.value = true
  try {
    const r = await fnRepoApi.read(PKG_PATH).catch(() => null)
    pkg.value = r ? safeParse(r.content) : null
  } finally {
    loading.value = false
  }
}
onMounted(load)

function safeParse(text) {
  try { return JSON.parse(text) } catch { BL.warning('package.json 不是合法 JSON,已按空对象处理'); return {} }
}

const groups = computed(() => ['dependencies', 'devDependencies'].map(k => ({
  k,
  label: k === 'dependencies' ? '生产依赖' : '开发依赖',
  items: Object.entries((pkg.value && pkg.value[k]) || {}).map(([name, version]) => ({ name, version })),
})))

function toggleGroup(k) {
  const s = new Set(opened.value)
  s.has(k) ? s.delete(k) : s.add(k)
  opened.value = s
}

async function savePkg(message) {
  busy.value = true
  try {
    await fnRepoApi.write(PKG_PATH, JSON.stringify(pkg.value, null, 2) + '\n', message)
    BL.success('已写回 package.json(本地提交,未推送)')
  } catch (e) {
    BL.error(`保存失败:${e?.message || e}`)
  } finally {
    busy.value = false
  }
}

async function createPkg() {
  creating.value = true
  try {
    pkg.value = { name: 'bontolink-functions', version: '0.0.1', type: 'module', dependencies: {}, devDependencies: {} }
    await savePkg('chore: 初始化 package.json (BOntoLink IDE)')
  } finally {
    creating.value = false
  }
}

async function addDep(name, version) {
  if (!pkg.value) return
  if (!pkg.value.dependencies) pkg.value.dependencies = {}
  pkg.value.dependencies[name] = version
  manualName.value = ''
  manualVer.value = ''
  await savePkg(`chore: 新增依赖 ${name}@${version}`)
}

async function removeDep(group, name) {
  const ok = await BL.confirm({ title: '移除依赖', content: `确定从 ${group} 中移除 ${name}?`, danger: true, okText: '移除' })
  if (!ok) return
  if (pkg.value?.[group]) delete pkg.value[group][name]
  await savePkg(`chore: 移除依赖 ${name}`)
}

function clearSearch() { q.value = ''; searchResults.value = []; searchError.value = '' }

/** npm 公共 registry 搜索;内网/离线时给明确提示 */
async function searchRegistry() {
  const k = q.value.trim()
  if (!k) { clearSearch(); return }
  searching.value = true
  searchError.value = ''
  searchResults.value = []
  try {
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 6000)
    const resp = await fetch(`https://registry.npmjs.org/-/v1/search?text=${encodeURIComponent(k)}&size=15`, { signal: ctrl.signal })
    clearTimeout(timer)
    if (!resp.ok) throw new Error('HTTP ' + resp.status)
    const data = await resp.json()
    searchResults.value = (data.objects || []).map(o => ({
      name: o.package.name, version: o.package.version, description: o.package.description || '',
    }))
    if (!searchResults.value.length) searchError.value = '无匹配的依赖包'
  } catch (e) {
    searchError.value = `无法访问 npm registry(${e.name === 'AbortError' ? '超时' : e.message}),可在下方手动填写包名与版本`
  } finally {
    searching.value = false
  }
}
</script>

<style scoped>
.idep-deps { display: flex; flex-direction: column; min-height: 0; }
.idep-body { flex: 1; min-height: 0; overflow: auto; }
.idep-add:hover { color: var(--ide-blue) !important; }
.idep-manual { display: flex; gap: 4px; padding: 8px; border-top: 1px solid var(--ide-border); margin-top: 6px; }
.idep-ver { max-width: 80px; }
</style>
