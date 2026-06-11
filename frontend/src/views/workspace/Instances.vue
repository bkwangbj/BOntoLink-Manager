<template>
  <div class="ix-root" :class="{ 'is-max': maximized }" ref="rootEl">
    <!-- 顶部浏览器风格页签栏 -->
    <div class="ix-tabs">
      <div class="ix-tabs-list">
        <div v-for="t in tabs" :key="t.id" :class="['ix-tab', t.id===activeId && 'is-active']" @click="activeId=t.id">
          <span class="ix-tab-ic" v-html="BL.icon(t.icon, 13, t.id===activeId ? (t.color||'var(--bl-primary)') : 'var(--bl-text-3)')"></span>
          <span class="ix-tab-title bl-truncate">{{ t.title }}</span>
          <button v-if="tabs.length>1" class="ix-tab-x" @click.stop="closeTab(t.id)" v-html="BL.icon('x', 14)"></button>
        </div>
        <button class="ix-tab-add" title="新建探索" @click="addTab()" v-html="BL.icon('plus', 14)"></button>
      </div>
      <div class="ix-tabs-actions">
        <button class="ix-gbtn" :title="maximized ? '退出全屏' : '全屏'" @click="toggleMax"
                v-html="BL.icon(maximized ? 'minimize' : 'maximize', 15)"></button>
      </div>
    </div>

    <!-- 探索页签:独立全页(自带头部,无二级标签) -->
    <InstanceExplore v-if="activeTab.kind==='explore'" :key="activeTab.id" :types="types"
                     :initial-class-id="activeTab.classId" :fixed-title="activeTab.fixedTitle" @open-instance="onOpenInstance" />

    <template v-else>
    <!-- 全局搜索行(独占一行，固定不变，不随二级标签切换) -->
    <div class="ix-toolbar-row">
      <div class="ix-searchbar">
        <!-- 范围选择：双栏燕尾角抽屉 -->
      <div class="ix-scope" @click.stop="scopeOpen=!scopeOpen" v-click-outside="()=>scopeOpen=false">
        <span class="bl-truncate">{{ scopeText }}</span>
        <span v-html="BL.icon('chevronDown', 12)"></span>
        <ScopeSelectorDrawer v-if="scopeOpen" :tree="scopeTree" :model-value="[...selDomains]"
                             @update:model-value="onScopeChange" />
      </div>

      <!-- 全局搜索 -->
      <div class="ix-search" v-click-outside="()=>searchOpen=false">
        <span class="ix-search-ic" v-html="BL.icon('search', 14, 'var(--bl-text-3)')"></span>
        <input class="ix-search-input" v-model="globalQ" placeholder="搜索对象类型和属性…"
               @input="onSearchInput" @focus="searchOpen=true" @keyup.enter="openSearchResults(globalQ)" />
        <span class="ix-search-help" v-html="BL.icon('help', 14)" @click.stop="helpOpen=!helpOpen" title="搜索语法"></span>
        <!-- 语法帮助 -->
        <div v-if="helpOpen" class="ix-help-pop" v-click-outside="()=>helpOpen=false" @click.stop>
          <span class="ix-beak" style="right:24px;left:auto"></span>
          <div class="ix-help-title">搜索语法</div>
          <div class="ix-help-sec">默认分词（任一命中）</div>
          <ul class="ix-help-list">
            <li><code>大坝 防渗</code>：匹配含「大坝」<b>或</b>「防渗」任一词的数据</li>
          </ul>
          <div class="ix-help-sec">引号精确短语</div>
          <ul class="ix-help-list">
            <li><code>"大坝 防渗"</code>：仅匹配连续完整包含该短语的数据</li>
          </ul>
          <div class="ix-help-sec">逻辑运算 AND / OR（可加括号）</div>
          <ul class="ix-help-list">
            <li><code>黄河 AND 堤防</code>：同时包含</li>
            <li><code>水库 OR 水闸</code>：包含任一</li>
            <li><code>("大坝 防渗" OR "渠道 衬砌") AND 河南</code></li>
          </ul>
          <div class="ix-help-sec">通配符</div>
          <ul class="ix-help-list">
            <li><code>闸?门</code>：<b>?</b> 匹配单个任意字符（闸门 / 闸X门）</li>
            <li><code>灌*</code>：<b>*</b> 匹配任意长度（灌区 / 灌溉渠 / 灌排管网）</li>
          </ul>
        </div>
        <!-- 搜索建议下拉:聚焦即弹,默认列出对象类型,随输入逐步过滤 -->
        <div v-if="searchOpen" class="ix-search-pop">
          <div v-if="globalQ.trim()" class="ix-search-row ix-search-go" @click="openSearchResults(globalQ)"
               v-html="iconText('search', `搜索「${globalQ.trim()}」的全部结果 →`)"></div>
          <template v-if="popTypes.length">
            <div class="ix-search-group">对象类型 · {{ popTypes.length }}</div>
            <div v-for="t in popTypes.slice(0, globalQ.trim() ? 6 : 8)" :key="t.id" class="ix-search-item" @click="openExplore(t)">
              <span class="ix-search-item-ic" :style="{ background:(t.color||'#165DFF')+'1f', color:t.color||'#165DFF' }"
                    v-html="BL.icon(t.icon||'cube', 12, t.color||'#165DFF')"></span>
              <span class="bl-grow bl-truncate">{{ t.display_name }}</span>
              <span class="bl-muted" style="font-size:11px">{{ t.instanceCount }} 条</span>
            </div>
          </template>
          <template v-if="globalQ.trim() && searchResult.instances?.length">
            <div class="ix-search-group">实例 · {{ searchResult.counts?.instances || 0 }}</div>
            <div v-for="(r,i) in searchResult.instances.slice(0,8)" :key="r.id+i" class="ix-search-item" @click="openInstance(r)">
              <span class="ix-search-item-ic" :style="{ background:(r.color||'#86909c')+'1f', color:r.color||'#86909c' }"
                    v-html="BL.icon(r.icon||'cube', 12, r.color||'#86909c')"></span>
              <span class="bl-grow bl-truncate">{{ r.title }}</span>
              <span class="bl-muted" style="font-size:11px">{{ r.className }}</span>
            </div>
          </template>
          <div v-if="!popTypes.length && !(globalQ.trim() && searchResult.instances?.length)" class="ix-search-empty">无匹配结果</div>
        </div>
      </div>
      </div>
    </div>

    <!-- 搜索结果页面(1.1.1.2.4) -->
    <SearchResults v-if="searchView" :query="searchView.q"
                   @explore="openExplore" @open-instance="onOpenInstance" @close="searchView=null" />

    <template v-else>
      <!-- 二级标签页导航 + 右侧各页签工具条(实例/对象类型 teleport 到此) -->
      <div class="ix-subtabs">
        <button v-for="s in SUBTABS" :key="s.key" :class="['ix-subtab', activeTab.view===s.key && 'is-active']"
                @click="setView(s.key)">{{ s.label }}</button>
        <div class="ix-toolbar-slot" id="ix-toolbar-slot"></div>
      </div>

      <!-- 内容展示区 -->
      <div class="ix-content">
        <InstanceOverview v-if="activeTab.view==='overview'" :types="scopedTypes"
                          @explore="openExplore" @preview="previewType" @graph="goGraph" />
        <InstanceObjectTypes v-else-if="activeTab.view==='objectTypes'" :types="scopedTypes"
                          @explore="openExplore" @preview="previewType" />
        <InstanceExplore v-else :key="activeTab.id" :types="types" :initial-class-id="activeTab.classId"
                          @open-instance="onOpenInstance" />
      </div>
    </template>
    </template>

    <!-- 详情抽屉 -->
    <InstanceDetailDrawer v-if="detail" :mode="detail.mode" :type="detail.type" :row="detail.row"
                          @close="detail=null" @explore-type="onExploreType" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { instanceApi, categoryApi } from '@/api'
import InstanceOverview from './instance/InstanceOverview.vue'
import InstanceObjectTypes from './instance/InstanceObjectTypes.vue'
import InstanceExplore from './instance/InstanceExplore.vue'
import InstanceDetailDrawer from './instance/InstanceDetailDrawer.vue'
import ScopeSelectorDrawer from './instance/ScopeSelectorDrawer.vue'
import SearchResults from './instance/SearchResults.vue'

const router = useRouter()
function iconText(ic, t) { return `${BL.icon(ic, 13)}<span style="margin-left:6px">${t}</span>` }

const SUBTABS = [
  { key: 'overview', label: '概览' },
  { key: 'instances', label: '实例' },
  { key: 'objectTypes', label: '对象类型' }
]

/* —— 数据 —— */
const types = ref([])
const catTreeRaw = ref([])        // 原始分类树(含 categoryType / color)
const selDomains = ref(new Set()) // 已选范围 codes(可含行业/领域/分组任意层级)
const rootEl = ref(null)
const maximized = ref(false)   // 浏览器全屏(对探索工作区元素 requestFullscreen)
function toggleMax() {
  if (!document.fullscreenElement) rootEl.value?.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.()
}
function onFsChange() { maximized.value = document.fullscreenElement === rootEl.value }

/* —— 页签会话 —— */
let seq = 1
const tabs = ref([{ id: 't1', title: '新建探索', icon: 'search', color: '', view: 'overview', classId: '' }])
const activeId = ref('t1')
const activeTab = computed(() => tabs.value.find(t => t.id === activeId.value) || tabs.value[0])

function addTab(init) {
  const id = 't' + (++seq)
  tabs.value.push({ id, title: '新建探索', icon: 'search', color: '', view: 'overview', classId: '', ...init })
  activeId.value = id
}
function closeTab(id) {
  const i = tabs.value.findIndex(t => t.id === id)
  if (i < 0 || tabs.value.length <= 1) return
  tabs.value.splice(i, 1)
  if (activeId.value === id) activeId.value = tabs.value[Math.max(0, i - 1)].id
}
function setView(v) { activeTab.value.view = v }

/* —— 范围选择 —— */
const scopeOpen = ref(false)
const scopeText = computed(() => {
  const n = selDomains.value.size
  return n ? `${n > 99 ? '99+' : n} 项` : '全部'
})
function onScopeChange(arr) { selDomains.value = new Set(arr) }

/* 带对象类型计数的范围树(行业→领域→分组) */
const scopeTree = computed(() => {
  const cnt = {}
  for (const t of types.value) { const c = t.category_code; if (c) cnt[c] = (cnt[c] || 0) + 1 }
  const typeName = (ct) => ct === 1 ? '行业' : ct === 2 ? '领域' : '分组'
  const build = (node) => {
    const code = node.categoryCode || node.category_code || node.id
    const children = (node.children || []).map(build)
    const count = (cnt[code] || 0) + children.reduce((s, c) => s + c.count, 0)
    return { code, label: node.rdfsLabel || node.cn || node.label || code, type: typeName(node.categoryType), color: node.color, count, children }
  }
  return (catTreeRaw.value || []).map(build)
})
const scopeByCode = computed(() => {
  const m = {}; const walk = (ns) => ns.forEach(n => { m[n.code] = n; if (n.children) walk(n.children) })
  walk(scopeTree.value); return m
})

const scopedTypes = computed(() => {
  if (!selDomains.value.size) return types.value
  // 把已选(任意层级)展开为其子树下全部 codes，再按 category_code 过滤
  const codes = new Set()
  const collect = (n) => { codes.add(n.code); (n.children || []).forEach(collect) }
  for (const c of selDomains.value) { const n = scopeByCode.value[c]; n ? collect(n) : codes.add(c) }
  return types.value.filter(t => codes.has(t.category_code))
})

/* —— 全局搜索 —— */
const globalQ = ref('')
const searchOpen = ref(false)
const searchView = ref(null)   // 搜索结果页面 { q }
function openSearchResults(q) {
  const query = (q || '').trim()
  if (!query) return
  searchView.value = { q: query }
  searchOpen.value = false
}
const helpOpen = ref(false)
const searchResult = ref({})
/* 下拉里的对象类型:本地即时过滤(默认列全部),输入字母/汉字逐步收窄 */
const popTypes = computed(() => {
  const q = globalQ.value.trim().toLowerCase()
  const list = scopedTypes.value
  if (!q) return list
  return list.filter(t =>
    (t.display_name || '').toLowerCase().includes(q) ||
    (t.api_name || '').toLowerCase().includes(q))
})
let searchTimer = null
function onSearchInput() {
  searchOpen.value = true
  clearTimeout(searchTimer)
  const q = globalQ.value.trim()
  if (!q) { searchResult.value = {}; return }
  searchTimer = setTimeout(async () => {
    try { searchResult.value = await instanceApi.search(q) } catch { searchResult.value = {} }
  }, 220)
}

/* —— 打开探索 / 详情 —— */
function openExplore(type, fixedTitle) {
  searchOpen.value = false
  searchView.value = null
  const ft = fixedTitle || type.fixedTitle || ''   // payload 里也可带 fixedTitle(单条实例探索)
  addTab({ kind: 'explore', title: ft || type.display_name || type.api_name, icon: type.icon || 'cube', color: type.color, view: 'instances', classId: type.id, fixedTitle: ft })
}
function onExploreType(payload) {
  const p = typeof payload === 'string' ? { classId: payload } : (payload || {})
  const t = types.value.find(x => x.id === p.classId)
  detail.value = null
  if (t) openExplore(t, p.title)
}
function previewType(type) { detail.value = { mode: 'type', type, row: {} } }
function onOpenInstance({ classId, row }) {
  const type = types.value.find(t => t.id === classId) || { id: classId }
  detail.value = { mode: 'instance', type, row }
}
function openInstance(r) {
  searchOpen.value = false
  const type = types.value.find(t => t.id === r.classId) || { id: r.classId, display_name: r.className, icon: r.icon, color: r.color }
  detail.value = { mode: 'instance', type, row: r }
}
const detail = ref(null)

/* —— 其它 —— */
function goGraph() { router.push('/workspace/graph') }

/* —— click-outside —— */
const vClickOutside = {
  mounted(el, binding) { el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }; setTimeout(() => document.addEventListener('mousedown', el.__h), 0) },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}

onMounted(async () => {
  document.addEventListener('fullscreenchange', onFsChange)
  try { types.value = await instanceApi.objectTypes() } catch { types.value = [] }
  // 分类树原始结构(范围选择燕尾抽屉用)
  try {
    const tree = await categoryApi.tree()
    catTreeRaw.value = Array.isArray(tree) ? tree : []
  } catch { catTreeRaw.value = [] }
})
onBeforeUnmount(() => document.removeEventListener('fullscreenchange', onFsChange))
</script>

<style scoped>
.ix-root { display: flex; flex-direction: column; height: 100%; background: var(--bl-bg-2); overflow: hidden; }
/* 工作区最大化：铺满整屏，盖住 App 侧栏与顶栏 */
.ix-root.is-max { position: fixed; inset: 0; z-index: 200; height: 100vh; }

.ix-tabs-actions { display: flex; align-items: center; gap: 2px; padding-left: 8px; flex-shrink: 0; margin-left: auto; }
.ix-gbtn { width: 28px; height: 28px; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 6px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.ix-gbtn:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }

/* 顶部页签栏 */
.ix-tabs {
  display: flex; align-items: center; gap: 8px; height: 40px; flex-shrink: 0;
  background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); padding: 0 10px;
}
.ix-tabs-list { flex: 1; display: flex; align-items: center; gap: 2px; overflow-x: auto; scrollbar-width: none; }
.ix-tabs-list::-webkit-scrollbar { display: none; }
.ix-tab {
  display: flex; align-items: center; gap: 6px; height: 30px; padding: 0 8px 0 10px;
  background: var(--bl-bg-2); border: 1px solid transparent; border-radius: 7px; cursor: pointer; font-size: 12.5px;
  color: var(--bl-text-3); max-width: 180px; flex-shrink: 0;
}
.ix-tab:hover { background: var(--bl-bg-hover); }
.ix-tab.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; border-color: var(--bl-primary-border); }
.ix-tab-ic { display: inline-flex; flex-shrink: 0; }
.ix-tab-title { min-width: 0; }
/* 关闭按钮:不占位,hover 当前页签才显示 */
.ix-tab-x { width: 18px; height: 18px; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 4px; cursor: pointer; display: none; align-items: center; justify-content: center; flex-shrink: 0; }
.ix-tab:hover .ix-tab-x, .ix-tab.is-active .ix-tab-x { display: inline-flex; }
.ix-tab-x:hover { background: var(--bl-bg-3,#e5e6eb); color: var(--bl-text-1); }
.ix-tab-add { width: 24px; height: 24px; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 5px; cursor: pointer; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ix-tab-add:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }

/* 搜索栏(内嵌二级标签行右侧，靠右对齐、垂直居中) */
/* 通栏搜索条：范围选择 + 全宽搜索合并为一条，占满整行 */
.ix-searchbar { flex: 1; min-width: 0; align-self: center; display: flex; align-items: center; height: 32px; box-sizing: border-box; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); }
.ix-searchbar:focus-within { border-color: var(--bl-primary); }
.ix-scope {
  position: relative; display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  height: 100%; box-sizing: border-box; padding: 0 12px; border-right: 1px solid var(--bl-border); border-radius: 6px 0 0 6px; cursor: pointer;
  font-size: 13px; min-width: 72px; color: var(--bl-text-2);
}
.ix-scope:hover { background: var(--bl-bg-hover); }
.ix-beak { position: absolute; top: -7px; left: 24px; width: 12px; height: 12px; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border); border-top: 1px solid var(--bl-border); transform: rotate(45deg); }

.ix-search { position: relative; flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; height: 100%; box-sizing: border-box; padding: 0 12px; background: transparent; }
.ix-search-ic { display: inline-flex; flex-shrink: 0; }
.ix-search-input { flex: 1; border: 0; outline: none; background: transparent; font-size: 13px; }
.ix-search-help { color: var(--bl-text-3); cursor: pointer; display: inline-flex; flex-shrink: 0; }
.ix-search-help:hover { color: var(--bl-primary); }
.ix-help-pop { position: absolute; top: calc(100% + 8px); right: 0; width: 380px; max-height: 70vh; overflow: auto; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 10px 32px rgba(0,0,0,.14); z-index: 60; padding: 12px 14px; }
.ix-help-title { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
.ix-help-sec { font-size: 11px; color: var(--bl-text-3); margin-top: 10px; padding-left: 2px; border-left: 2px solid var(--bl-primary); padding-left: 6px; }
.ix-help-list { margin: 4px 0 0; padding-left: 16px; font-size: 12px; color: var(--bl-text-2); line-height: 1.8; }
.ix-help-list code { background: var(--bl-bg-2); padding: 0 4px; border-radius: 3px; font-size: 11px; color: var(--bl-text-1); }

.ix-search-pop { position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 10px 32px rgba(0,0,0,.16); z-index: 55; padding: 6px; max-height: 420px; overflow: auto; }
.ix-search-row { padding: 8px 10px; font-size: 12px; color: var(--bl-text-3); display: flex; align-items: center; }
.ix-search-go { cursor: pointer; border-radius: 6px; color: var(--bl-primary); font-weight: 500; }
.ix-search-go:hover { background: var(--bl-primary-soft); }
.ix-search-group { font-size: 11px; color: var(--bl-text-3); padding: 8px 10px 4px; }
.ix-search-item { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.ix-search-item:hover { background: var(--bl-bg-hover); }
.ix-search-item-ic { width: 22px; height: 22px; border-radius: 5px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ix-search-empty { padding: 20px; text-align: center; color: var(--bl-text-3); font-size: 13px; }

/* 搜索/工具栏行(独占一行) */
.ix-toolbar-row { display: flex; align-items: center; height: 44px; padding: 0 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); flex-shrink: 0; box-sizing: border-box; }
/* 实例/对象类型页签：工具条传送槽位，占据二级标签行右侧剩余宽度 */
.ix-toolbar-slot { flex: 1; min-width: 0; display: flex; align-items: center; margin-left: 16px; }

/* 二级标签(纯标签行) */
.ix-subtabs { display: flex; align-items: stretch; gap: 4px; height: 42px; padding: 0 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); flex-shrink: 0; box-sizing: border-box; }
.ix-subtab { display: inline-flex; align-items: center; padding: 0 14px; border: 0; background: transparent; cursor: pointer; font-size: 13px; color: var(--bl-text-2); border-bottom: 2px solid transparent; margin-bottom: -1px; }
.ix-subtab:hover { color: var(--bl-text-1); }
.ix-subtab.is-active { color: var(--bl-primary); border-bottom-color: var(--bl-primary); font-weight: 500; }

/* 内容区 */
.ix-content { flex: 1; display: flex; min-height: 0; overflow: hidden; }
</style>
