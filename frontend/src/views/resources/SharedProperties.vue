<template>
  <div class="page">
    <PageHeader title="共享属性" subtitle="跨对象类型复用属性,一次修改全局生效">
      <template #actions>
        <!-- 概览统计 -->
        <div class="ov">
          <span class="ov-item"><span class="ov-lbl">总数</span><b>{{ rows.length }}</b></span>
          <span class="ov-item ov-ok"><span class="ov-lbl">启用</span><b>{{ enabledCount }}</b></span>
          <span class="ov-item ov-risk"><span class="ov-lbl">已引用</span><b>{{ usedCount }}</b></span>
        </div>

        <!-- 列表 / 分组 视图切换 -->
        <div class="sp-view-toggle">
          <button :class="['vt-btn', !groupByDomain && 'is-on']" @click="groupByDomain = false" title="列表视图">
            <span v-html="BL.icon('list', 13)"></span><span>列表</span>
          </button>
          <button :class="['vt-btn', groupByDomain && 'is-on']" @click="groupByDomain = true" title="按领域分组">
            <span v-html="BL.icon('layers', 13)"></span><span>分组</span>
          </button>
        </div>

        <select class="bl-input hd-filter" v-model="filterDomain" title="所属领域">
          <option value="">全部领域</option>
          <option v-for="d in domainOptions" :key="d.code" :value="d.code">{{ d.label }}</option>
        </select>
        <select class="bl-input hd-filter" v-model="filterStatus" title="状态">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>

        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索共享属性 (名称 / 编码)" v-model="q" />
        </div>

        <button class="bl-btn bl-btn-primary" @click="openWizard">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建共享属性</span>
        </button>
      </template>
    </PageHeader>

    <div class="sp-main">
      <!-- 左侧行业分类树 (统一组件) -->
      <CategoryTreeFilter :rows="rows"
                          title="行业分类"
                          total-label="全部共享属性"
                          store-key="sharedProps"
                          @change="onCategoryChange" />

      <!-- 右侧: 列表 + 分页 -->
      <section class="pane pane-list">
        <!-- 子页签: 普通属性 / 结构属性 -->
        <div class="sp-subtabs">
          <button :class="['sp-stab', activeMainTab === 'normal' && 'is-on']" @click="activeMainTab = 'normal'">
            <span v-html="BL.icon('list', 13)"></span>
            <span style="margin-left:4px">普通属性</span>
            <span v-if="rows.length > 0" class="sp-stab-cnt">{{ rows.length }}</span>
          </button>
          <button :class="['sp-stab', activeMainTab === 'struct' && 'is-on']" @click="activeMainTab = 'struct'">
            <span v-html="BL.icon('layers', 13)"></span>
            <span style="margin-left:4px">结构属性</span>
            <span v-if="structCount > 0" class="sp-stab-cnt">{{ structCount }}</span>
          </button>
        </div>

        <!-- 结构属性视图 (左右分栏 + 拖拽宽度) -->
        <StructTypesView v-if="activeMainTab === 'struct'"
                         :selected-codes="selectedCategoryCodes"
                         @counts="onStructCount" />

        <!-- 普通属性: 沿用既有列表 -->
        <div v-show="activeMainTab === 'normal'" class="sp-list-scroll">
          <table class="bl-table sp-table">
            <thead>
              <tr>
                <th style="width:36px" class="t-center"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th><span class="th-sort" @click="toggleSort('rdfs_label')">属性<span class="th-arrow">{{ sortArrow('rdfs_label') }}</span></span></th>
                <th style="width:160px"><span class="th-sort" @click="toggleSort('category_code')">所属领域<span class="th-arrow">{{ sortArrow('category_code') }}</span></span></th>
                <th style="width:120px" class="t-center"><span class="th-sort" @click="toggleSort('data_type')">数据类型<span class="th-arrow">{{ sortArrow('data_type') }}</span></span></th>
                <th style="width:160px" class="t-left">值类型</th>
                <th style="width:130px" class="t-center" title="必填 / 多值 / 值域约束 / 格式化">标记</th>
                <th style="width:90px" class="t-center"><span class="th-sort" @click="toggleSort('ref_count')">引用数<span class="th-arrow">{{ sortArrow('ref_count') }}</span></span></th>
                <th style="width:90px" class="t-center"><span class="th-sort" @click="toggleSort('status')">状态<span class="th-arrow">{{ sortArrow('status') }}</span></span></th>
                <th style="width:96px"></th>
              </tr>
            </thead>
            <tbody>
              <template v-for="row in displayRows" :key="row.key">
                <!-- 分组标题行 (分组视图时) -->
                <tr v-if="row.type === 'group'" class="sp-group-row" @click="toggleDomainCollapse(row.key)">
                  <td colspan="9">
                    <span class="sp-group-chev" v-html="BL.icon(row.collapsed ? 'chevronRight' : 'chevronDown', 12)"></span>
                    <span class="sp-group-label">{{ row.label }}</span>
                    <span class="sp-group-count">{{ row.count }}</span>
                  </td>
                </tr>
                <!-- 属性行 -->
                <tr v-else :class="['sp-row', groupByDomain && 'is-grouped', selectedId === row.data.id && 'is-active']"
                    @click="onRowClick(row.data)">
                  <td class="t-center" @click.stop>
                    <input type="checkbox" :checked="checked.has(row.data.id)" @change="toggleCheck(row.data.id)" />
                  </td>
                  <!-- 属性 (两行: 名称 / 编码·类型) -->
                  <td>
                    <div class="sp-name-cell">
                      <span class="sp-ic" :style="{ background: propTypeColor(row.data) }"
                            v-html="BL.icon(propTypeIcon(row.data), 12, '#fff')"></span>
                      <div class="sp-name-text">
                        <div class="sp-name bl-truncate" :title="row.data.rdfs_label || row.data.prop_code">{{ row.data.rdfs_label || row.data.prop_code }}</div>
                        <div class="sp-code bl-mono bl-muted">{{ row.data.prop_code }} · {{ row.data.prop_type }}</div>
                      </div>
                    </div>
                  </td>
                  <td><span class="bl-truncate" :title="domainLabel(row.data.category_code)">{{ domainLabel(row.data.category_code) || '通用' }}</span></td>
                  <td class="t-center"><span class="bl-tag">{{ row.data.data_type || '—' }}</span></td>
                  <td class="t-left">
                    <span class="bl-truncate" :title="row.data.value_type_label || row.data.value_type">{{ row.data.value_type_label || row.data.value_type || '—' }}</span>
                  </td>
                  <!-- 标记: 必填 / 多值 / 约束 / 格式化 -->
                  <td class="t-center">
                    <span class="sp-flags">
                      <span v-if="row.data.is_required"          class="sp-flag is-req" title="必填">必</span>
                      <span v-if="row.data.is_multi_valued_prop" class="sp-flag is-mul" title="多值">多</span>
                      <span v-if="row.data.is_range_constraint_prop" class="sp-flag is-con" title="值域约束">约</span>
                      <span v-if="row.data.format_enabled"       class="sp-flag is-fmt" title="已配置格式化">式</span>
                      <span v-if="!hasAnyFlag(row.data)" class="bl-muted" style="font-size:11px">—</span>
                    </span>
                  </td>
                  <td class="t-center">
                    <span :class="['bl-tag', (row.data.ref_count || 0) > 0 && 'bl-tag-primary']">{{ row.data.ref_count || 0 }}</span>
                  </td>
                  <td class="t-center">
                    <span :class="['bl-tag', row.data.status === 1 ? 'bl-tag-success' : 'bl-tag-muted']">{{ row.data.status === 1 ? '启用' : '禁用' }}</span>
                  </td>
                  <td class="t-center" @click.stop>
                    <div class="bl-row" style="gap:0;justify-content:center">
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="查看 / 编辑" @click="onRowClick(row.data)" v-html="BL.icon('edit', 12)"></button>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除"
                              :disabled="(row.data.ref_count || 0) > 0"
                              @click="onSingleDelete(row.data)" v-html="BL.icon('trash', 12)"></button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
          <div v-if="!flatList.length" class="bl-empty" style="padding:48px">暂无匹配的共享属性,请调整筛选条件或点击「新建共享属性」</div>
        </div>

        <!-- 分页钉底 (普通属性 tab 专属) -->
        <div v-show="activeMainTab === 'normal'" class="sp-pager">
          <div class="sp-pager-l">
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm sp-del-btn" style="margin-left:8px" @click="onBatchDelete">
                <span v-html="BL.icon('trash', 12)"></span>
                <span style="margin-left:4px">批量删除</span>
              </button>
              <button class="bl-btn bl-btn-sm sp-ena-btn" style="margin-left:6px" @click="onBatchStatus(1)" title="批量启用所选属性">
                <span v-html="BL.icon('check', 12)"></span>
                <span style="margin-left:4px">启用</span>
              </button>
              <button class="bl-btn bl-btn-sm sp-dis-btn" style="margin-left:6px" @click="onBatchStatus(0)" title="批量禁用所选属性">
                <span v-html="BL.icon('power', 12)"></span>
                <span style="margin-left:4px">禁用</span>
              </button>
              <button class="bl-btn bl-btn-sm bl-btn-text sp-clear-btn" style="margin-left:6px" @click="clearSelection" title="清空选择">
                取消选择
              </button>
            </template>
            <template v-else>
              共 {{ flatList.length }} 项
              <span v-if="groupByDomain" class="bl-muted" style="margin-left:8px">· {{ groupCount }} 个领域</span>
            </template>
          </div>
          <div class="sp-pager-r" v-if="!groupByDomain">
            <span class="bl-muted" style="font-size:12px;margin-right:6px">每页</span>
            <select class="bl-input sp-page-size" v-model.number="pageSize">
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page <= 1" @click="page--">‹</button>
            <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page >= totalPages" @click="page++">›</button>
          </div>
          <div class="sp-pager-r" v-else>
            <button class="bl-btn bl-btn-sm bl-btn-text" @click="expandAllGroups">展开全部</button>
            <button class="bl-btn bl-btn-sm bl-btn-text" @click="collapseAllGroups">折叠全部</button>
          </div>
        </div>
      </section>
    </div>

    <!-- 右侧详情抽屉 -->
    <SharedPropertyDetailDrawer v-model:open="drawerOpen"
                                :property="drawerProperty"
                                @saved="onSaved"
                                @deleted="onDeleted" />

    <!-- 新建向导 -->
    <NewSharedPropertyWizard v-model:open="wizardOpen"
                             :init-category="selectedCategoryCode"
                             @created="onCreated" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { sharedPropertyApi, categoryApi, structTypeApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import CategoryTreeFilter from '@/components/CategoryTreeFilter.vue'
import SharedPropertyDetailDrawer from './sharedproperty/SharedPropertyDetailDrawer.vue'
import NewSharedPropertyWizard from './sharedproperty/NewSharedPropertyWizard.vue'
import StructTypesView from './sharedproperty/StructTypesView.vue'

const rows = ref([])
const structCount = ref(0)  // 结构属性数量(由 StructTypesView 通过 @counts 回传)
function onStructCount(n) {
  structCount.value = typeof n === 'number' ? n : (Array.isArray(n) ? n.length : 0)
}
const activeMainTab = ref(localStorage.getItem('bontolink.sp.mainTab') || 'normal')
watch(activeMainTab, v => localStorage.setItem('bontolink.sp.mainTab', v))
const checked = ref(new Set())
const selectedId = ref(null)
const q = ref('')
const filterStatus = ref('')
const filterDomain = ref('')
const groupByDomain = ref(false)
const sortKey = ref('')
const sortDir = ref('')

const selectedCategoryCodes = ref(null)  // null=全部, Set<string>=当前分类子树的 category_code 集合

const drawerOpen = ref(false)
const drawerProperty = ref(null)
const wizardOpen = ref(false)

const page = ref(1)
const pageSize = ref(20)

/* —— 领域映射 (category_code → label) —— */
const categoryMap = ref({})
const domainOptions = computed(() => {
  const set = new Set()
  rows.value.forEach(r => { if (r.category_code) set.add(r.category_code) })
  return [...set].map(code => ({ code, label: categoryMap.value[code] || code }))
})
function domainLabel(code) { return code ? (categoryMap.value[code] || code) : '' }

/* —— 加载 —— */
async function load() {
  rows.value = await sharedPropertyApi.list().catch(() => [])
}
async function loadCategoryMap() {
  const tree = await categoryApi.tree().catch(() => [])
  const m = {}
  const walk = (ns) => ns.forEach(n => { if (n.categoryCode) m[n.categoryCode] = n.label; if (n.children) walk(n.children) })
  walk(tree)
  categoryMap.value = m
}
async function loadStructCount() {
  const list = await structTypeApi.list().catch(() => [])
  structCount.value = Array.isArray(list) ? list.length : 0
}
const route = useRoute()
const router = useRouter()
// URL 带 ?openId=<id> 时打开详情;消费后清 query,避免刷新自动弹、并支持同页再次点击
function applyOpenId(id) {
  if (!id) return
  const row = rows.value.find(r => r.id === id)
  if (row) { onRowClick(row); router.replace({ query: {} }) }
}
onMounted(async () => {
  await load(); loadCategoryMap(); loadStructCount()
  applyOpenId(route.query.openId)
})
watch(() => route.query.openId, applyOpenId)

/* —— 行业分类筛选 (左树) —— */
const selectedCategoryCode = ref('')   // 当前选中的领域 code (null/全部 → '')
function onCategoryChange({ codes, categoryCode }) {
  selectedCategoryCodes.value = codes || null
  selectedCategoryCode.value = categoryCode || ''
}

/* —— 排序 —— */
function toggleSort(key) {
  if (sortKey.value !== key) { sortKey.value = key; sortDir.value = 'asc' }
  else if (sortDir.value === 'asc') sortDir.value = 'desc'
  else { sortKey.value = ''; sortDir.value = '' }
}
function sortArrow(key) {
  if (sortKey.value !== key) return ' ⇅'
  return sortDir.value === 'asc' ? ' ↑' : ' ↓'
}

/* —— 过滤后扁平列表 (用于统计 + 分页) —— */
const flatList = computed(() => {
  let list = rows.value
  if (selectedCategoryCodes.value) list = list.filter(r => selectedCategoryCodes.value.has(r.category_code))
  if (filterDomain.value) list = list.filter(r => (r.category_code || '') === filterDomain.value)
  if (filterStatus.value !== '') list = list.filter(r => String(r.status) === filterStatus.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.rdfs_label, r.prop_code, r.rdfs_comment]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  if (sortKey.value && sortDir.value) {
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      const va = a[sortKey.value]; const vb = b[sortKey.value]
      if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
      return String(va || '').localeCompare(String(vb || '')) * dir
    })
  }
  return list
})

/* —— 列表视图: 分页;分组视图: 全量按领域折行 —— */
const totalPages = computed(() => Math.max(1, Math.ceil(flatList.value.length / pageSize.value)))
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return flatList.value.slice(start, start + pageSize.value)
})

const collapsedDomains = ref(new Set())
function toggleDomainCollapse(code) {
  const s = new Set(collapsedDomains.value)
  s.has(code) ? s.delete(code) : s.add(code)
  collapsedDomains.value = s
}
function expandAllGroups() { collapsedDomains.value = new Set() }
function collapseAllGroups() {
  const codes = new Set()
  flatList.value.forEach(r => codes.add(r.category_code || '__none__'))
  collapsedDomains.value = codes
}

/* 渲染行集: 列表模式 = 分页后的扁平行;分组模式 = 分组标题 + 该组属性行 */
const displayRows = computed(() => {
  if (!groupByDomain.value) {
    return pagedList.value.map(d => ({ type: 'item', key: d.id, data: d }))
  }
  // 分组
  const buckets = new Map()
  flatList.value.forEach(d => {
    const code = d.category_code || '__none__'
    if (!buckets.has(code)) buckets.set(code, [])
    buckets.get(code).push(d)
  })
  const out = []
  for (const [code, items] of buckets) {
    const collapsed = collapsedDomains.value.has(code)
    const label = code === '__none__' ? '通用 (未分配领域)' : (categoryMap.value[code] || code)
    out.push({ type: 'group', key: code, label, count: items.length, collapsed })
    if (!collapsed) items.forEach(d => out.push({ type: 'item', key: d.id, data: d }))
  }
  return out
})
const groupCount = computed(() => {
  if (!groupByDomain.value) return 0
  return new Set(flatList.value.map(r => r.category_code || '__none__')).size
})

/* 统计 */
const enabledCount = computed(() => rows.value.filter(r => r.status === 1).length)
const usedCount    = computed(() => rows.value.filter(r => (r.ref_count || 0) > 0).length)

/* 全选 / 选择 */
const allChecked = computed(() => {
  const visible = displayRows.value.filter(r => r.type === 'item').map(r => r.data.id)
  return visible.length > 0 && visible.every(id => checked.value.has(id))
})
function toggleCheck(id) {
  const s = new Set(checked.value)
  s.has(id) ? s.delete(id) : s.add(id); checked.value = s
}
function toggleAll() {
  const s = new Set(checked.value)
  const visible = displayRows.value.filter(r => r.type === 'item').map(r => r.data.id)
  if (allChecked.value) visible.forEach(id => s.delete(id))
  else visible.forEach(id => s.add(id))
  checked.value = s
}

/* 行点击 → 抽屉 */
function onRowClick(p) {
  selectedId.value = p.id
  drawerProperty.value = { ...p }
  drawerOpen.value = true
}

/* 标记 */
function hasAnyFlag(p) {
  return !!(p.is_required || p.is_multi_valued_prop || p.is_range_constraint_prop || p.format_enabled)
}

/* 单条 / 批量 删除 */
async function onSingleDelete(p) {
  if ((p.ref_count || 0) > 0) { BL.warning(`「${p.rdfs_label || p.prop_code}」被引用 ${p.ref_count} 次,无法删除`); return }
  const ok = await BL.confirm({ title: '删除共享属性', content: `确定删除「${p.rdfs_label || p.prop_code}」?`, danger: true, okText: '删除' })
  if (!ok) return
  try { await sharedPropertyApi.remove(p.id); BL.success('已删除'); await load() } catch (e) { BL.error(e?.msg || '删除失败') }
}
async function onBatchDelete() {
  const ids = [...checked.value]
  if (!ids.length) return
  const blocked = rows.value.filter(r => ids.includes(r.id) && (r.ref_count || 0) > 0)
  let msg = `确定删除选中的 ${ids.length} 个共享属性?`
  if (blocked.length) msg += `\n其中 ${blocked.length} 个被引用,将自动跳过`
  const ok = await BL.confirm({ title: '批量删除', content: msg, danger: true, okText: '删除' })
  if (!ok) return
  const res = await sharedPropertyApi.batchRemove(ids).catch(() => null)
  if (res && res.blocked) BL.warning(`已删除 ${res.deleted} 个,${res.blocked} 个被引用未删除`)
  else BL.success(`已删除 ${res?.deleted || ids.length} 个`)
  checked.value = new Set()
  await load()
}

/* —— 批量启用 / 禁用 —— */
async function onBatchStatus(targetStatus) {
  const ids = [...checked.value]
  if (!ids.length) return
  const targets = rows.value.filter(r => ids.includes(r.id))
  const toChange = targets.filter(r => r.status !== targetStatus)
  if (!toChange.length) {
    BL.warning(`所选 ${ids.length} 项已经全部是「${targetStatus === 1 ? '启用' : '禁用'}」状态`)
    return
  }
  const label = targetStatus === 1 ? '启用' : '禁用'
  const ok = await BL.confirm({
    title: `批量${label}`,
    content: `确定将选中的 ${toChange.length} 个共享属性${label}?`,
    okText: label
  })
  if (!ok) return
  let okCount = 0, failCount = 0
  for (const r of toChange) {
    try {
      await sharedPropertyApi.update(r.id, { ...r, status: targetStatus })
      okCount++
    } catch { failCount++ }
  }
  if (failCount) BL.warning(`成功 ${okCount} 个,失败 ${failCount} 个`)
  else BL.success(`已${label} ${okCount} 个`)
  await load()
}

/* 取消选择 */
function clearSelection() { checked.value = new Set() }

/* 抽屉回调 */
async function onSaved() { await load() }
async function onDeleted() { drawerOpen.value = false; await load() }

/* 向导 */
function openWizard() { wizardOpen.value = true }
async function onCreated() { await load() }

/* 类型图标 / 配色 */
function propTypeIcon(p) {
  if (p.prop_type === 'object') return 'link'
  if (p.prop_type === 'annotation') return 'chat'
  return 'database'
}
function propTypeColor(p) {
  if (p.prop_type === 'object') return '#FF7D00'
  if (p.prop_type === 'annotation') return '#00B42A'
  return '#1677ff'
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }

/* 概览统计 (与 Datasources 一致) */
.ov { display: inline-flex; gap: 14px; padding: 4px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); }
.ov-item { font-size: var(--bl-fs-13); color: var(--bl-text-2); }
.ov-item .ov-lbl { color: var(--bl-text-3); margin-right: 6px; }
.ov-item b { font-weight: 600; color: var(--bl-text-1); }
.ov-ok b { color: var(--bl-success); }
.ov-risk b { color: var(--bl-warning); }

/* 列表 / 分组 视图切换 */
.sp-view-toggle {
  display: inline-flex; align-items: center;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  padding: 2px;
}
.vt-btn {
  display: inline-flex; align-items: center; gap: 4px;
  height: 26px; padding: 0 10px;
  border: 0; background: transparent; cursor: pointer;
  font-size: var(--bl-fs-12); color: var(--bl-text-2);
  border-radius: var(--bl-radius-1, 4px);
}
.vt-btn:hover { color: var(--bl-text-1); }
.vt-btn.is-on {
  background: var(--bl-bg-1); color: var(--bl-primary);
  font-weight: 500; box-shadow: var(--bl-shadow-1, 0 1px 2px rgba(0,0,0,.08));
}

.hd-filter { width: 130px; }

.search-wrap { position: relative; width: 260px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

/* 主体: 左树 + 列表 */
.sp-main {
  flex: 1;
  display: flex;
  gap: 12px; padding: 12px;
  overflow: hidden;
}
.pane {
  flex: 1;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  overflow: hidden;
  display: flex; flex-direction: column;
}
.pane-list { display: flex; flex-direction: column; overflow: hidden; }

/* 主页签切换: 普通属性 / 结构属性 */
.sp-subtabs {
  flex-shrink: 0;
  display: flex; gap: 2px;
  padding: 0 12px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-1);
}
.sp-stab {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 0 14px; height: 38px;
  border: 0; background: transparent; cursor: pointer;
  font-size: 13px; color: var(--bl-text-2);
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.sp-stab:hover { color: var(--bl-text-1); }
.sp-stab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
.sp-stab-cnt {
  display: inline-block;
  margin-left: 6px; padding: 0 7px;
  min-width: 20px; height: 18px;
  background: var(--bl-bg-2); color: var(--bl-text-3);
  border-radius: 9px;
  font-size: 11px; font-weight: 500; line-height: 18px;
  text-align: center;
  font-feature-settings: "tnum";
  /* 0 / 空内容 也不会塌成纯色圆点: 至少有数字占位 */
}
.sp-stab.is-on .sp-stab-cnt { background: var(--bl-primary); color: #fff; }

/* 表格滚动区 + 分页钉底 */
.sp-list-scroll { flex: 1; min-height: 0; overflow: auto; }
.sp-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  font-size: var(--bl-fs-12);
}
.sp-pager-l { display: inline-flex; align-items: center; }
.sp-pager-r { display: inline-flex; align-items: center; gap: 4px; }
.sp-page-size { width: 64px; height: 26px; }

/* 表格 */
.sp-table { width: 100%; }
.sp-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  box-shadow: inset 0 -1px 0 var(--bl-divider);
}
.t-center { text-align: center; }
.t-left { text-align: left; }
.sp-row { cursor: pointer; }
.sp-row.is-active { background: var(--bl-primary-soft); }
.sp-row.is-active td { color: var(--bl-primary); }

/* 名称单元格 (两行: 名称 + 编码·类型) */
.sp-name-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.sp-name-text { min-width: 0; }
.sp-name { font-weight: 500; color: var(--bl-text-1); }
.sp-code { font-size: var(--bl-fs-11); }
.sp-ic {
  width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

/* 表头排序 */
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; white-space: nowrap; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }

/* 标记小徽章 (必/多/约/式) */
.sp-flags { display: inline-flex; gap: 3px; }
.sp-flag {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px;
  border-radius: 4px;
  font-size: 11px; font-weight: 600;
  color: #fff;
}
.sp-flag.is-req { background: #f53f3f; }
.sp-flag.is-mul { background: #722ED1; }
.sp-flag.is-con { background: #FF7D00; }
.sp-flag.is-fmt { background: var(--bl-primary); }

/* 批量操作按钮 (删除红 / 启用绿 / 禁用灰 / 取消选择浅) */
.sp-del-btn { background: var(--bl-bg-1); border: 1px solid #f53f3f; color: #f53f3f; }
.sp-del-btn:hover { background: color-mix(in srgb, #f53f3f 8%, var(--bl-bg-1)); }
.sp-ena-btn { background: var(--bl-bg-1); border: 1px solid #00b42a; color: #00b42a; }
.sp-ena-btn:hover { background: color-mix(in srgb, #00b42a 8%, var(--bl-bg-1)); }
.sp-dis-btn { background: var(--bl-bg-1); border: 1px solid #86909c; color: var(--bl-text-2); }
.sp-dis-btn:hover { background: var(--bl-bg-hover); }
.sp-clear-btn { color: var(--bl-text-3); }
.sp-clear-btn:hover { color: var(--bl-primary); }

/* 分组标题行 */
.sp-group-row { cursor: pointer; background: var(--bl-bg-2); }
.sp-group-row:hover { background: var(--bl-bg-hover); }
.sp-group-row td {
  padding: 7px 12px !important;
  border-bottom: 1px solid var(--bl-border);
}
.sp-group-chev {
  display: inline-flex; vertical-align: middle;
  color: var(--bl-text-3); margin-right: 6px;
}
.sp-group-label {
  font-weight: 600; font-size: var(--bl-fs-13); color: var(--bl-text-1);
  vertical-align: middle;
}
.sp-group-count {
  margin-left: 8px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 9px; padding: 0 8px; line-height: 16px;
  vertical-align: middle; font-feature-settings: "tnum";
}
.sp-row.is-grouped td:first-child { padding-left: 24px; }

/* 通用 tag-muted (灰色禁用 tag) */
:deep(.bl-tag-muted) { background: var(--bl-bg-2); color: var(--bl-text-3); }
:deep(.bl-tag-primary) { background: var(--bl-primary-soft); color: var(--bl-primary); }
</style>
