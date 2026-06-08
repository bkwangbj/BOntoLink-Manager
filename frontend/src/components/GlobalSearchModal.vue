<!--
  全局搜索弹框 — 头部 ⌘K / 点击搜索框触发.
  设计参考截图风格: 顶部 input + tab 切换 + 分组卡片结果 + 键盘导航.
-->
<template>
  <Teleport to="body">
    <div v-if="open" class="gs-mask" @click.self="close">
      <div class="gs-modal" @click.stop>
        <!-- 搜索输入 -->
        <header class="gs-hd">
          <div class="gs-search-box">
            <span class="gs-hd-ic" v-html="BL.icon('search', 16)"></span>
            <input ref="inputRef" v-model="query" class="gs-input"
                   placeholder="请输入关键字按回车进行搜索"
                   @keydown="onKeyDown" />
            <button v-if="query" class="gs-clear-text" @click="query=''">清除</button>
          </div>
          <button class="gs-close" @click="close" v-html="BL.icon('x', 18)" title="关闭 (Esc)"></button>
        </header>

        <!-- tab 切换 -->
        <nav class="gs-tabs">
          <button v-for="t in TABS" :key="t.k"
                  class="gs-tab" :class="{ 'is-on': tab === t.k }"
                  @click="tab = t.k">
            {{ t.label }}
            <span v-if="tabCount(t.k)" class="gs-tab-cnt">{{ tabCount(t.k) }}</span>
          </button>
        </nav>

        <!-- 结果区 -->
        <main class="gs-body" ref="bodyRef">
          <!-- loading -->
          <div v-if="loading" class="gs-loading">搜索中...</div>

          <!-- 空查询 -->
          <div v-else-if="!query.trim()" class="gs-empty">
            <div class="gs-empty-ic" v-html="BL.icon('search', 32, 'var(--bl-text-4)')"></div>
            <div class="gs-empty-title">输入关键词开始搜索</div>
            <div class="gs-empty-tip">支持模糊匹配:名称、编码、API、描述</div>
            <div class="gs-empty-scope">
              <span v-for="t in TABS.slice(1)" :key="t.k" class="gs-empty-tag">{{ t.label }}</span>
            </div>
          </div>

          <!-- 无结果 -->
          <div v-else-if="!totalCount" class="gs-empty">
            <div class="gs-empty-ic" v-html="BL.icon('search', 32, 'var(--bl-text-4)')"></div>
            <div class="gs-empty-title">未找到「{{ query }}」相关结果</div>
            <div class="gs-empty-tip">试试切换分类或更换关键词</div>
          </div>

          <!-- 分组结果 -->
          <template v-else>
            <section v-for="g in visibleGroups" :key="g.kind" class="gs-group">
              <div class="gs-group-hd">
                <span class="gs-group-lbl">{{ g.label }}</span>
                <span class="gs-group-cnt">{{ g.list.length }}</span>
              </div>
              <ul class="gs-list">
                <li v-for="(item, i) in g.list" :key="g.kind + '-' + item.id"
                    :class="['gs-item', activeKey === itemKey(g.kind, i) && 'is-active']"
                    @mouseenter="activeKey = itemKey(g.kind, i)"
                    @click="goTo(item)">
                  <span class="gs-item-ic"
                        :style="{ background: kindColor(item.kind) + '1F', color: kindColor(item.kind) }"
                        v-html="BL.icon(kindIcon(item.kind), 18)"></span>
                  <div class="gs-item-body">
                    <div class="gs-item-r1">
                      <span class="gs-item-title">{{ item.title || '—' }}</span>
                      <span v-if="item.subtitle && item.subtitle !== item.title" class="gs-item-subtitle bl-mono">{{ item.subtitle }}</span>
                    </div>
                    <div v-if="item.description" class="gs-item-desc">{{ item.description }}</div>
                    <div class="gs-item-meta">
                      <span class="gs-item-kind">{{ kindLabel(item.kind) }}</span>
                      <span v-if="item.extra" class="gs-item-meta-tag">{{ item.extra }}</span>
                      <span v-if="item.rid" class="gs-item-meta-tag bl-mono">{{ item.rid }}</span>
                    </div>
                  </div>
                  <span class="gs-item-arrow" v-html="BL.icon('chevronRight', 14)"></span>
                </li>
              </ul>
            </section>
          </template>
        </main>

        <!-- 底部快捷键 -->
        <footer class="gs-ft">
          <span class="gs-ft-kbd"><kbd>↑</kbd><kbd>↓</kbd> 选择</span>
          <span class="gs-ft-kbd"><kbd>Enter</kbd> 打开</span>
          <span class="gs-ft-kbd"><kbd>Esc</kbd> 关闭</span>
          <span class="gs-ft-spacer"></span>
          <span v-if="totalCount" class="gs-ft-total">共 {{ totalCount }} 项</span>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { searchApi } from '@/api'
import { BL } from '@/lib/bl.js'

const props = defineProps({ open: Boolean })
const emit = defineEmits(['update:open'])
const router = useRouter()

const inputRef = ref(null)
const bodyRef = ref(null)
const query = ref('')
const loading = ref(false)
const tab = ref('all')
const result = ref({})
const activeKey = ref('')

const TABS = [
  { k: 'all',    label: '综合' },
  { k: 'object', label: '对象' },
  { k: 'link',   label: '关系' },
  { k: 'prop',   label: '属性' },
  { k: 'ds',     label: '数据源' },
  { k: 'other',  label: '其他' }
]

/* 每种结果类型的视觉元数据: hex 色 (用 hex 才方便做透明叠层 + 文字色), 图标, 中文标签. */
const KIND_META = {
  object:     { label: '对象类型', color: '#165DFF', icon: 'cube' },
  link:       { label: '链接类型', color: '#FF7D00', icon: 'link' },
  sharedProp: { label: '共享属性', color: '#00B42A', icon: 'menu' },
  valueType:  { label: '值类型',   color: '#722ED1', icon: 'box' },
  enumType:   { label: '枚举类型', color: '#EB2F96', icon: 'list' },
  interface:  { label: '接口',     color: '#13C2C2', icon: 'share' },
  datasource: { label: '数据源',   color: '#FF7D00', icon: 'database' },
  typeClass:  { label: '类型类',   color: '#722ED1', icon: 'tag' },
  category:   { label: '行业分类', color: '#86909C', icon: 'folder' }
}
function kindLabel(k) { return KIND_META[k]?.label || k }
function kindColor(k) { return KIND_META[k]?.color || 'var(--bl-text-3)' }
function kindIcon(k)  { return KIND_META[k]?.icon  || 'box' }

/* 按 tab → 后端结果 key 列表 (综合: 所有) */
const TAB_KEYS = {
  all:    ['objects','links','sharedProps','valueTypes','enumTypes','datasources','interfaces','typeClasses','categories'],
  object: ['objects'],
  link:   ['links'],
  prop:   ['sharedProps','valueTypes','enumTypes'],
  ds:     ['datasources'],
  other:  ['interfaces','typeClasses','categories']
}
const KEY_TO_KIND = {
  objects: 'object', links: 'link', sharedProps: 'sharedProp', valueTypes: 'valueType',
  enumTypes: 'enumType', interfaces: 'interface', datasources: 'datasource',
  typeClasses: 'typeClass', categories: 'category'
}

/* 当前 tab 下展示的分组 (含空过滤) */
const visibleGroups = computed(() => {
  const keys = TAB_KEYS[tab.value] || []
  const groups = []
  for (const k of keys) {
    const list = result.value?.[k] || []
    if (list.length === 0) continue
    const kind = KEY_TO_KIND[k]
    groups.push({ kind, label: kindLabel(kind), list })
  }
  return groups
})

/* 总数 (用于底栏 + 空态判断) */
const totalCount = computed(() => visibleGroups.value.reduce((s, g) => s + g.list.length, 0))

/* 单个 tab 的数量 (用于 tab 上的小标记) */
function tabCount(k) {
  if (!result.value) return 0
  const keys = TAB_KEYS[k] || []
  return keys.reduce((s, key) => s + (result.value[key]?.length || 0), 0)
}

/* 平铺 list 用于键盘导航 (按 visibleGroups 顺序) */
const flatItems = computed(() => {
  const list = []
  for (const g of visibleGroups.value) {
    g.list.forEach((item, i) => list.push({ kind: g.kind, item, key: itemKey(g.kind, i) }))
  }
  return list
})
function itemKey(kind, i) { return kind + '-' + i }

/* —— debounced 搜索 —— */
let debounceTimer = null
watch([query, tab], () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!query.value.trim()) {
    result.value = {}
    activeKey.value = ''
    return
  }
  debounceTimer = setTimeout(doSearch, 250)
}, { flush: 'post' })

async function doSearch() {
  loading.value = true
  try {
    const data = await searchApi.global(query.value.trim(), tab.value)
    result.value = data || {}
    // 默认选中第一条
    nextTick(() => {
      const first = flatItems.value[0]
      activeKey.value = first ? first.key : ''
      bodyRef.value && (bodyRef.value.scrollTop = 0)
    })
  } catch (e) {
    BL.error('搜索失败')
    result.value = {}
  } finally {
    loading.value = false
  }
}

/* —— 键盘导航 —— */
function onKeyDown(e) {
  if (e.key === 'Escape') { close(); return }
  if (e.key === 'Enter') {
    const cur = flatItems.value.find(x => x.key === activeKey.value)
    if (cur) { goTo(cur.item); e.preventDefault() }
    return
  }
  if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return
  e.preventDefault()
  const list = flatItems.value
  if (!list.length) return
  let idx = list.findIndex(x => x.key === activeKey.value)
  if (idx < 0) idx = 0
  else idx = (e.key === 'ArrowDown' ? idx + 1 : idx - 1 + list.length) % list.length
  activeKey.value = list[idx].key
  scrollActiveIntoView()
}
function scrollActiveIntoView() {
  nextTick(() => {
    const el = bodyRef.value?.querySelector('.gs-item.is-active')
    if (el && el.scrollIntoView) el.scrollIntoView({ block: 'nearest' })
  })
}

/* —— 跳转 —— */
function goTo(item) {
  if (!item || !item.route) return
  router.push(item.route)
  close()
}

function close() {
  emit('update:open', false)
}

/* 打开时聚焦 input + 重置状态 */
watch(() => props.open, (v) => {
  if (v) {
    nextTick(() => inputRef.value?.focus())
  } else {
    // 关闭时清空,下次打开干净状态 (体验上更接近 cmd+k)
    query.value = ''
    tab.value = 'all'
    result.value = {}
    activeKey.value = ''
  }
})

onUnmounted(() => { if (debounceTimer) clearTimeout(debounceTimer) })
</script>

<style scoped>
.gs-mask {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  z-index: 1400;
  display: flex; align-items: flex-start; justify-content: center;
  padding-top: 10vh;
}
.gs-modal {
  width: 720px; max-width: 92vw;
  height: 78vh;            /* 固定高度: 打开时直接到最大, 列表少时也保持框体一致 */
  max-height: 78vh;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 12px;
  box-shadow:
    0 24px 60px rgba(0,0,0,0.4),
    0 4px 12px rgba(0,0,0,0.2);
  display: flex; flex-direction: column;
  overflow: hidden;
}
:root[data-theme="dark"] .gs-modal {
  border-color: var(--bl-border-strong);
  box-shadow:
    0 24px 60px rgba(0,0,0,0.75),
    0 6px 16px rgba(0,0,0,0.5),
    inset 0 1px 0 rgba(255,255,255,0.06);
}

/* ----- 输入框 (胶囊形 + "清除"文字按钮) + 外侧 × 关闭弹框 ----- */
.gs-hd {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 14px; flex-shrink: 0;
  border-bottom: 1px solid var(--bl-divider);
}
.gs-search-box {
  flex: 1; min-width: 0;
  display: flex; align-items: center; gap: 8px;
  height: 38px; padding: 0 12px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 999px;
  transition: border-color .15s, box-shadow .15s;
}
.gs-search-box:focus-within {
  border-color: var(--bl-primary);
  box-shadow: 0 0 0 3px var(--bl-primary-soft);
}
:root[data-theme="dark"] .gs-search-box {
  background: var(--bl-bg-2);
  border-color: var(--bl-border-strong);
}
.gs-hd-ic { color: var(--bl-text-3); display: inline-flex; flex-shrink: 0; }
.gs-input {
  flex: 1; min-width: 0;
  border: 0; outline: 0; background: transparent;
  font-size: 14px; color: var(--bl-text-1);
  font-family: inherit;
}
.gs-input::placeholder { color: var(--bl-text-3); }
/* "清除"文字按钮 — 框内右侧 */
.gs-clear-text {
  flex-shrink: 0;
  border: 0; background: transparent; cursor: pointer;
  padding: 2px 8px;
  font-size: 12px; color: var(--bl-text-3);
  border-radius: 4px;
  transition: background-color .12s, color .12s;
}
.gs-clear-text:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }
/* 弹框关闭叉号 — 搜索框右侧外部 */
.gs-close {
  flex-shrink: 0;
  width: 32px; height: 32px;
  border: 0; background: transparent; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-3); border-radius: 6px;
  transition: background-color .12s, color .12s;
}
.gs-close:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }

/* ----- tabs ----- */
.gs-tabs {
  display: flex; gap: 4px;
  padding: 6px 10px; flex-shrink: 0;
  border-bottom: 1px solid var(--bl-divider);
}
.gs-tab {
  display: inline-flex; align-items: center; gap: 6px;
  height: 28px; padding: 0 12px;
  border: 0; background: transparent;
  font-size: 13px; color: var(--bl-text-2);
  border-radius: 6px;
  cursor: pointer;
  transition: background-color .12s, color .12s;
}
.gs-tab:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.gs-tab.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.gs-tab-cnt {
  font-size: 10px; padding: 0 5px; min-width: 16px; text-align: center;
  border-radius: 8px; background: var(--bl-bg-2); color: var(--bl-text-3);
  line-height: 14px; font-weight: 500;
}
.gs-tab.is-on .gs-tab-cnt { background: var(--bl-primary); color: #fff; }

/* ----- 结果区 ----- */
.gs-body {
  flex: 1; min-height: 0; overflow-y: auto;
  padding: 8px 10px 12px;
}

/* loading / empty */
.gs-loading {
  padding: 32px; text-align: center; color: var(--bl-text-3); font-size: 13px;
}
.gs-empty {
  padding: 56px 24px 40px; text-align: center;
  color: var(--bl-text-3); font-size: 13px;
}
.gs-empty-ic { margin-bottom: 12px; display: inline-flex; }
.gs-empty-title { font-size: 14px; font-weight: 500; color: var(--bl-text-2); margin-bottom: 4px; }
.gs-empty-tip { font-size: 12px; }
.gs-empty-scope {
  margin-top: 18px;
  display: flex; flex-wrap: wrap; gap: 6px; justify-content: center;
}
.gs-empty-tag {
  padding: 2px 8px; border-radius: 10px;
  background: var(--bl-bg-2); color: var(--bl-text-2);
  font-size: 11px;
}

/* 分组 */
.gs-group + .gs-group { margin-top: 4px; }
.gs-group-hd {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 8px 4px;
  font-size: 11px; font-weight: 600;
  color: var(--bl-text-3); text-transform: uppercase;
  letter-spacing: 0.5px;
}
.gs-group-cnt {
  font-size: 10px; padding: 0 5px; min-width: 16px; text-align: center;
  border-radius: 8px; background: var(--bl-bg-2); color: var(--bl-text-3);
  letter-spacing: 0;
}
.gs-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; }

/* 卡片 */
.gs-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color .1s;
  border: 1px solid transparent;
}
.gs-item.is-active {
  background: var(--bl-primary-soft);
  border-color: var(--bl-primary-border);
}
.gs-item-ic {
  width: 38px; height: 38px; flex-shrink: 0;
  border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 18px;
}
.gs-item-body { flex: 1; min-width: 0; }
.gs-item-r1 { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.gs-item-title {
  font-size: 13.5px; font-weight: 600; color: var(--bl-text-1);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.gs-item-subtitle {
  font-size: 11.5px; color: var(--bl-text-3);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  flex-shrink: 0; max-width: 50%;
}
.gs-item-desc {
  font-size: 12px; color: var(--bl-text-2);
  margin-top: 2px;
  display: -webkit-box;
  -webkit-line-clamp: 1; -webkit-box-orient: vertical;
  overflow: hidden;
}
.gs-item-meta {
  margin-top: 4px;
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
}
.gs-item-kind {
  font-size: 10px; padding: 1px 6px; border-radius: 3px;
  background: var(--bl-bg-2); color: var(--bl-text-2);
  font-weight: 500;
}
.gs-item-meta-tag {
  font-size: 10.5px; color: var(--bl-text-3);
  padding: 1px 6px; border-radius: 3px;
  background: var(--bl-bg-hover);
  max-width: 240px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.gs-item-arrow {
  color: var(--bl-text-4);
  display: inline-flex;
  opacity: 0; transition: opacity .12s, color .12s;
  flex-shrink: 0;
}
.gs-item:hover .gs-item-arrow,
.gs-item.is-active .gs-item-arrow { opacity: 1; color: var(--bl-primary); }

/* ----- 底栏 ----- */
.gs-ft {
  display: flex; align-items: center; gap: 14px;
  padding: 8px 14px; flex-shrink: 0;
  border-top: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
  font-size: 11px; color: var(--bl-text-3);
}
.gs-ft-kbd { display: inline-flex; align-items: center; gap: 4px; }
.gs-ft-kbd kbd {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 4px;
  font-size: 10.5px; font-family: var(--bl-font-mono);
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 3px;
  color: var(--bl-text-2);
}
.gs-ft-spacer { flex: 1; }
.gs-ft-total { color: var(--bl-text-2); }
</style>
