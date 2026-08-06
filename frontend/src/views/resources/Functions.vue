<template>
  <div class="page fn-page">
    <PageHeader title="函数管理" subtitle="业务逻辑、计算规则、动作编排">
      <template #actions>
        <div class="ov">
          <span class="ov-item"><span class="ov-lbl">总数</span><b>{{ rows.length }}</b></span>
          <span class="ov-item ov-ok"><span class="ov-lbl">已发布</span><b>{{ countBy(2) }}</b></span>
          <span class="ov-item"><span class="ov-lbl">草稿</span><b style="color:#FF7D00">{{ countBy(1) }}</b></span>
          <span class="ov-item ov-risk"><span class="ov-lbl">已停用</span><b>{{ countBy(3) }}</b></span>
        </div>

        <!-- 关键词: 仅匹配函数名称, 回车触发查询 -->
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索函数名称" v-model="q" @keydown.enter="onQuery" />
        </div>

        <!-- 函数类型 (多选) -->
        <div class="fn-msel" ref="typeMsel">
          <button type="button" class="bl-input fn-msel-btn" @click="typeOpen = !typeOpen" title="函数类型 (多选)">
            <span :class="{ 'is-ph': filterTypes.size === 0 || filterTypes.size === TYPE_OPTS.length }">{{ typeFilterLabel }}</span>
            <span class="fn-msel-arrow" :class="{ 'is-open': typeOpen }" v-html="BL.icon('chevronDown', 12)"></span>
          </button>
          <div v-if="typeOpen" class="fn-msel-pop">
            <label v-for="t in TYPE_OPTS" :key="t.v" class="fn-msel-opt">
              <input type="checkbox" :checked="filterTypes.has(t.v)" @change="toggleType(t.v)" />
              <span class="fn-dot" :style="{ background: t.color }"></span>{{ t.label }}
            </label>
          </div>
        </div>

        <!-- 开发语言 (单选) -->
        <BlSelect v-model="filterLang" :options="LANG_OPTS" placeholder="全部语言" clearable style="width:118px" />

        <!-- 状态 (多选) -->
        <div class="fn-msel" ref="statusMsel">
          <button type="button" class="bl-input fn-msel-btn" @click="statusOpen = !statusOpen" title="状态 (多选)">
            <span :class="{ 'is-ph': filterStatuses.size === 0 || filterStatuses.size === STATUS_OPTS.length }">{{ statusFilterLabel }}</span>
            <span class="fn-msel-arrow" :class="{ 'is-open': statusOpen }" v-html="BL.icon('chevronDown', 12)"></span>
          </button>
          <div v-if="statusOpen" class="fn-msel-pop">
            <label v-for="s in STATUS_OPTS" :key="s.v" class="fn-msel-opt">
              <input type="checkbox" :checked="filterStatuses.has(s.v)" @change="toggleStatus(s.v)" />
              <span class="fn-dot" :style="{ background: s.color }"></span>{{ s.label }}
            </label>
          </div>
        </div>

        <!-- 可见性 (单选) -->
        <BlSelect v-model="filterVisibility" :options="VISIBILITY_OPTS" placeholder="全部可见性" clearable style="width:126px" />

        <button class="bl-btn bl-btn-primary" @click="onQuery">查询</button>
        <button class="bl-btn bl-btn-primary fn-add-btn" title="新增函数" @click="openCreate">
          <span v-html="BL.icon('plus', 13, '#fff')"></span>
        </button>
      </template>
    </PageHeader>

    <div class="fn-main">
      <FunctionDirTree ref="dirTree" @change="onDirChange" />

      <section class="pane pane-list">
        <div class="fn-list-scroll">
          <table class="bl-table fn-table">
            <colgroup>
              <col style="width:40px" /><col style="width:224px" /><col style="width:96px" />
              <col style="width:160px" /><col style="width:176px" /><col style="width:180px" />
              <col style="width:80px" /><col style="width:80px" /><col style="width:80px" />
              <col style="width:80px" /><col style="width:112px" /><col style="width:112px" />
              <col style="width:250px" />
            </colgroup>
            <thead>
              <tr>
                <th class="t-center fn-frz1"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th class="t-left fn-frz2" @click="sortBy('api_name')">函数名称<SortIc :dir="sortDirOf('api_name')" /></th>
                <th class="t-center" @click="sortBy('function_type')">类型<SortIc :dir="sortDirOf('function_type')" /></th>
                <th class="t-left" @click="sortBy('subject_type')">作用对象<SortIc :dir="sortDirOf('subject_type')" /></th>
                <th class="t-left" @click="sortBy('code_file_path')">所在目录及文件<SortIc :dir="sortDirOf('code_file_path')" /></th>
                <th class="t-left">入参 / 出参</th>
                <th class="t-center" @click="sortBy('language')">语言<SortIc :dir="sortDirOf('language')" /></th>
                <th class="t-center" @click="sortBy('version_no')">版本<SortIc :dir="sortDirOf('version_no')" /></th>
                <th class="t-center" @click="sortBy('visibility')">可见性<SortIc :dir="sortDirOf('visibility')" /></th>
                <th class="t-center" @click="sortBy('status')">状态<SortIc :dir="sortDirOf('status')" /></th>
                <th class="t-right" @click="sortBy('calls_7d')">近 7 天 / 总调用<SortIc :dir="sortDirOf('calls_7d')" /></th>
                <th class="t-center" @click="sortBy('update_time')">最近更新<SortIc :dir="sortDirOf('update_time')" /></th>
                <th class="t-left" @click="sortBy('rdfs_comment')">函数说明<SortIc :dir="sortDirOf('rdfs_comment')" /></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in paged" :key="r.id"
                  :class="['fn-row', selectedId === r.id && 'is-active']"
                  @click="openDetail(r)">
                <!-- 多选框: 点击不触发详情抽屉 -->
                <td class="t-center fn-frz1" @click.stop>
                  <input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" />
                </td>
                <!-- 函数名称: 上行英文名(代码字体/蓝/加粗) 下行中文显示名 -->
                <td class="t-left fn-frz2">
                  <div class="fn-name-cell">
                    <span class="fn-name-ic"><span v-html="BL.icon('code', 12, '#fff')"></span></span>
                    <div class="fn-name-txt">
                      <div class="bl-mono bl-truncate fn-api" :title="r.api_name">{{ r.api_name }}</div>
                      <div class="bl-muted bl-truncate fn-sub" :title="r.function_label">{{ r.function_label || '—' }}</div>
                    </div>
                  </div>
                </td>
                <!-- 类型: 彩色圆角标签 -->
                <td class="t-center">
                  <span class="bl-tag fn-tag" :style="tagStyle(typeMeta(r.function_type).color)">{{ typeMeta(r.function_type).label }}</span>
                </td>
                <!-- 作用对象: 上行 行业·领域, 下行 对象标识 -->
                <td class="t-left">
                  <template v-if="r.subject_type">
                    <div class="bl-muted bl-truncate fn-sub">{{ r.industry_dir }}·{{ shortDomain(r.category_dir) }}</div>
                    <div class="bl-mono bl-truncate fn-obj" :title="r.subject_type">{{ r.subject_type }}</div>
                  </template>
                  <span v-else class="bl-muted">—</span>
                </td>
                <!-- 所在目录及文件 -->
                <td class="t-left">
                  <div class="bl-muted bl-truncate fn-sub" :title="`${r.industry_dir} / ${r.category_dir}`">{{ r.industry_dir }} / {{ r.category_dir }}</div>
                  <div class="bl-mono bl-truncate fn-file" :title="r.code_file_path">{{ r.code_file_path }}</div>
                </td>
                <!-- 入参 / 出参: 仅展示类型, 超 3 个显示 …, tooltip 给全量 -->
                <td class="t-left">
                  <div class="bl-truncate fn-io" :title="ioTitle('入', r.in_types)">
                    <span class="fn-io-lbl">入:</span>
                    <span v-if="(r.in_types || []).length" class="bl-mono">{{ ioBrief(r.in_types) }}</span>
                    <span v-else class="bl-muted">无</span>
                  </div>
                  <div class="bl-truncate fn-io" :title="ioTitle('出', r.out_types)">
                    <span class="fn-io-lbl">出:</span>
                    <span v-if="(r.out_types || []).length" class="bl-mono">{{ ioBrief(r.out_types) }}</span>
                    <span v-else class="bl-muted">无</span>
                  </div>
                </td>
                <td class="t-center">{{ langLabel(r.language) }}</td>
                <td class="t-center"><span class="bl-mono bl-muted">{{ r.version_no }}</span></td>
                <!-- 可见性: 图标 + 文字 -->
                <td class="t-center">
                  <span class="fn-vis" :title="visMeta(r.visibility).label">
                    <span v-html="BL.icon(visMeta(r.visibility).icon, 12)"></span>{{ visMeta(r.visibility).short }}
                  </span>
                </td>
                <!-- 状态: 色点 + 文字 -->
                <td class="t-center">
                  <span class="fn-st">
                    <span class="fn-dot" :style="{ background: statusMeta(r.status).color }"></span>{{ statusMeta(r.status).label }}
                  </span>
                </td>
                <!-- 近 7 天 / 总调用 -->
                <td class="t-right">
                  <div class="fn-calls">{{ fmtNum(r.calls_7d) }}</div>
                  <div class="bl-muted fn-sub">{{ fmtNum(r.calls_total) }}</div>
                </td>
                <td class="t-center"><span class="fn-time">{{ fmtTime(r.update_time) }}</span></td>
                <td class="t-left"><div class="fn-desc" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</div></td>
              </tr>
            </tbody>
          </table>
          <div v-if="!filtered.length" class="bl-empty" style="padding:48px">暂无匹配的函数,请调整筛选条件或点击右上角「+」新增函数</div>
        </div>

        <!-- 底部: 统计 / 批量操作 + 分页 -->
        <div class="fn-pager">
          <div class="fn-pager-l">
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm fn-del-btn" style="margin-left:8px" @click="onBatchDelete">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除</span>
              </button>
              <button class="bl-btn bl-btn-sm fn-pub-btn" style="margin-left:6px" @click="onBatchStatus(2)">
                <span v-html="BL.icon('check', 12)"></span><span style="margin-left:4px">发布</span>
              </button>
              <button class="bl-btn bl-btn-sm fn-off-btn" style="margin-left:6px" @click="onBatchStatus(3)">
                <span v-html="BL.icon('power', 12)"></span><span style="margin-left:4px">停用</span>
              </button>
              <button class="bl-btn bl-btn-sm bl-btn-text" style="margin-left:6px" @click="checked = new Set()">取消选择</button>
            </template>
            <template v-else>共 {{ filtered.length }} 个函数</template>
          </div>
          <Pager v-model:page="page" v-model:page-size="pageSize" :total-pages="totalPages" :sizes="[10, 20, 50]" />
        </div>
      </section>
    </div>

    <!-- 新增函数向导 (3 步) -->
    <FunctionCreateWizard v-model:open="wizardOpen"
                          :init-industry="dir.industry || ''"
                          :init-category="dir.category || ''"
                          @created="onCreated" />

    <!-- 函数详情抽屉工作台 -->
    <FunctionDetailWorkspace v-model:open="detailOpen" :function-id="detailId" @saved="refresh" />
  </div>
</template>

<script setup>
/**
 * 函数管理列表页 (P1)
 *
 * 对齐《本体管理系统-函数Functions.pdf》5.1 管理列表页:
 * 顶部筛选栏 + 左侧行业领域分组树 + 右侧 12 列数据表格 + 底部分页。
 * 列表默认每个访问路径只显示最新版本 (后端 latestPerPath), 历史版本走详情页版本选择器。
 *
 * 待接:点击行打开的函数详情抽屉 (P3)、右上角「+」的新增函数向导 (P2)。
 */
import { ref, computed, onMounted, onUnmounted, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { functionApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import BlSelect from '@/components/BlSelect.vue'
import Pager from '@/components/Pager.vue'
import FunctionDirTree from './function/FunctionDirTree.vue'
import FunctionCreateWizard from './function/FunctionCreateWizard.vue'
import FunctionDetailWorkspace from './function/FunctionDetailWorkspace.vue'
import { usePagination } from '@/lib/usePagination'

/* —— 枚举 (与 enum_function_type / language / status / visibility 对齐) —— */
const TYPE_OPTS = [
  { v: 1, label: '常规函数', color: '#165DFF' },
  { v: 2, label: '动作函数', color: '#722ED1' },
  { v: 3, label: '聚合函数', color: '#00B42A' },
  { v: 4, label: '衍生函数', color: '#FF7D00' },
  { v: 5, label: '时序函数', color: '#0FC6C2' },
]
const STATUS_OPTS = [
  { v: 2, label: '已发布', color: '#00B42A' },
  { v: 1, label: '草稿',   color: '#FF7D00' },
  { v: 3, label: '已停用', color: '#86909C' },
  { v: 4, label: '已废弃', color: '#F53F3F' },
]
const LANG_OPTS = [
  { value: '2', label: 'TypeScript' },
  { value: '1', label: 'Python' },
]
const VISIBILITY_OPTS = [
  { value: '1', label: '全平台可见' },
  { value: '2', label: '仅本部门' },
  { value: '3', label: '指定角色' },
  { value: '4', label: '私有' },
]
const VIS_META = {
  1: { label: '全平台可见', short: '可见', icon: 'eye' },
  2: { label: '仅本部门可见', short: '部门', icon: 'users' },
  3: { label: '指定角色可见', short: '角色', icon: 'userCheck' },
  4: { label: '私有', short: '私有', icon: 'lock' },
}

function typeMeta(v) { return TYPE_OPTS.find(t => t.v === Number(v)) || { label: '—', color: '#86909C' } }
function statusMeta(v) { return STATUS_OPTS.find(s => s.v === Number(v)) || { label: '—', color: '#86909C' } }
function visMeta(v) { return VIS_META[Number(v)] || VIS_META[1] }
function langLabel(v) { return Number(v) === 1 ? 'Python' : 'TypeScript' }

/* —— 数据 —— */
const rows = ref([])
const checked = ref(new Set())
const selectedId = ref(null)

async function load() {
  rows.value = await functionApi.list().catch(() => [])
}

/**
 * 深链 ?openId=<函数id>:供其他模块(如对象类别详情页的「关联函数」)跳进来直接开详情。
 * 消费后清掉 query, 避免刷新自动弹、并支持同页再次点击同一条。
 */
const route = useRoute()
const router = useRouter()
function applyOpenId(id) {
  if (!id) return
  const row = rows.value.find(r => r.id === id)
  if (!row) return
  openDetail(row)
  router.replace({ query: {} })
}
watch(() => route.query.openId, applyOpenId)

onMounted(async () => {
  await load()
  applyOpenId(route.query.openId)
  window.addEventListener('click', onDocClick)
})
onUnmounted(() => window.removeEventListener('click', onDocClick))

function countBy(status) { return rows.value.filter(r => Number(r.status) === status).length }

/* —— 筛选 —— */
const q = ref('')
const filterLang = ref('')
const filterVisibility = ref('')
/* 默认: 类型全选; 状态默认选中 已发布 / 草稿 / 已停用 (文档 4.2) */
const filterTypes = ref(new Set(TYPE_OPTS.map(t => t.v)))
const filterStatuses = ref(new Set([1, 2, 3]))
const dir = ref({ industry: null, category: null })

const typeOpen = ref(false), statusOpen = ref(false)
const typeMsel = ref(null), statusMsel = ref(null)
function onDocClick(e) {
  if (typeMsel.value && !typeMsel.value.contains(e.target)) typeOpen.value = false
  if (statusMsel.value && !statusMsel.value.contains(e.target)) statusOpen.value = false
}
/* ref 在模板表达式里不能重新指向 (踩坑表), 一律走具名函数改 .value */
function toggleSet(setRef, v) {
  const s = new Set(setRef.value)
  s.has(v) ? s.delete(v) : s.add(v)
  setRef.value = s
}
function toggleType(v) { toggleSet(filterTypes, v) }
function toggleStatus(v) { toggleSet(filterStatuses, v) }
const typeFilterLabel = computed(() => {
  const n = filterTypes.value.size
  if (n === 0 || n === TYPE_OPTS.length) return '全部类型'
  return n === 1 ? typeMeta([...filterTypes.value][0]).label : `类型 (${n})`
})
const statusFilterLabel = computed(() => {
  const n = filterStatuses.value.size
  if (n === 0 || n === STATUS_OPTS.length) return '全部状态'
  return n === 1 ? statusMeta([...filterStatuses.value][0]).label : `状态 (${n})`
})

function onDirChange(p) { dir.value = { industry: p.industry, category: p.category } }
function onQuery() { load() }

const filtered = computed(() => {
  let list = rows.value
  if (dir.value.industry) {
    list = list.filter(r => r.industry_dir === dir.value.industry
      && (!dir.value.category || r.category_dir === dir.value.category))
  }
  const ts = filterTypes.value
  if (ts.size > 0 && ts.size < TYPE_OPTS.length) list = list.filter(r => ts.has(Number(r.function_type)))
  const ss = filterStatuses.value
  if (ss.size > 0 && ss.size < STATUS_OPTS.length) list = list.filter(r => ss.has(Number(r.status)))
  if (filterLang.value) list = list.filter(r => String(r.language) === filterLang.value)
  if (filterVisibility.value) list = list.filter(r => String(r.visibility) === filterVisibility.value)
  const k = q.value.trim().toLowerCase()
  // 文档 4.2: 关键词仅匹配函数名称 (英文 API 名 + 中文显示名)
  if (k) list = list.filter(r => [r.api_name, r.function_label].filter(Boolean)
    .some(s => String(s).toLowerCase().includes(k)))
  return sortList(list)
})

/* —— 排序: 默认按最近更新倒序 —— */
const sortKey = ref('update_time')
const sortDir = ref('desc')
function sortBy(key) {
  if (sortKey.value === key) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortKey.value = key; sortDir.value = key === 'update_time' ? 'desc' : 'asc' }
}
function sortDirOf(key) { return sortKey.value === key ? sortDir.value : '' }
const NUMERIC_KEYS = ['function_type', 'language', 'visibility', 'status', 'calls_7d']
function sortList(list) {
  const k = sortKey.value, sign = sortDir.value === 'asc' ? 1 : -1
  return [...list].sort((a, b) => {
    if (NUMERIC_KEYS.includes(k)) return (Number(a[k] || 0) - Number(b[k] || 0)) * sign
    return String(a[k] ?? '').localeCompare(String(b[k] ?? ''), 'zh') * sign
  })
}

const { page, pageSize, totalPages, paged } = usePagination(filtered, 10)

/* —— 选择 / 批量 —— */
const allChecked = computed(() => paged.value.length > 0 && paged.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) { toggleSet(checked, id) }
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) paged.value.forEach(r => s.delete(r.id))
  else paged.value.forEach(r => s.add(r.id))
  checked.value = s
}
async function onBatchDelete() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中的 ${ids.length} 个函数?函数的参数、运行配置、环境变量与调用统计会一并清除。`, danger: true, okText: '删除' })
  if (!ok) return
  await functionApi.batchRemove(ids).catch(() => null)
  BL.success(`已删除 ${ids.length} 个`)
  checked.value = new Set()
  await refresh()
}
async function onBatchStatus(status) {
  const ids = [...checked.value]
  if (!ids.length) return
  // 发布走独立接口:除状态外还要落发布时间, 并把当前代码仓分支/commit 登记到版本库
  if (status === 2) {
    const ok = await BL.confirm({
      title: '发布函数',
      content: `发布选中的 ${ids.length} 个函数?发布会记录发布时间,并把当前代码仓的分支与提交登记进版本库。`,
      okText: '发布',
    })
    if (!ok) return
    await functionApi.batchPublish(ids).catch(e => BL.error(e?.message || '发布失败'))
    BL.success('已发布')
  } else {
    await Promise.all(ids.map(id => functionApi.setStatus(id, status).catch(() => null)))
    BL.success('已停用')
  }
  checked.value = new Set()
  await refresh()
}

const dirTree = ref(null)
async function refresh() {
  await load()
  dirTree.value?.reload()
}

/* —— 行交互: 打开详情抽屉 (点复选框区域不触发, 见模板 @click.stop) —— */
const detailOpen = ref(false)
const detailId = ref('')
function openDetail(r) {
  selectedId.value = r.id
  detailId.value = r.id
  detailOpen.value = true
}
const wizardOpen = ref(false)
function openCreate() { wizardOpen.value = true }
/** 向导创建成功: 刷新列表与目录树后直接打开新函数详情 (文档 5.2 六、6.1) */
async function onCreated(id) {
  await refresh()
  if (!id) return
  selectedId.value = id
  detailId.value = id
  detailOpen.value = true
}

/* —— 展示工具 —— */
function shortDomain(category) { return String(category || '').replace(/函数集$/, '') }
function ioBrief(types) {
  const list = types || []
  return list.slice(0, 3).join('、') + (list.length > 3 ? '、…' : '')
}
function ioTitle(prefix, types) {
  const list = types || []
  return `${prefix}: ${list.length ? list.join('、') : '无'}`
}
function fmtNum(n) { return Number(n || 0).toLocaleString('en-US') }
function fmtTime(t) { return String(t || '').slice(0, 16) || '—' }
function tagStyle(c) {
  return {
    background: `color-mix(in srgb, ${c} 12%, transparent)`,
    color: c,
    border: `1px solid color-mix(in srgb, ${c} 30%, transparent)`
  }
}

/* 表头排序小箭头 (无排序时淡显) */
const SortIc = (props) => h('span', {
  class: ['fn-sort', props.dir && 'is-on'],
  innerHTML: BL.icon(props.dir === 'asc' ? 'chevronUp' : 'chevronDown', 10)
})
SortIc.props = ['dir']
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }

/* 顶部统计 */
.ov { display: inline-flex; gap: 14px; padding: 4px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); }
.ov-item { font-size: var(--bl-fs-13); color: var(--bl-text-2); }
.ov-item .ov-lbl { color: var(--bl-text-3); margin-right: 6px; }
.ov-item b { font-weight: 600; color: var(--bl-text-1); }
.ov-ok b { color: var(--bl-success); }
.ov-risk b { color: #86909c; }

.search-wrap { position: relative; width: 220px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

/* 多选筛选 (类型 / 状态) */
.fn-msel { position: relative; }
.fn-msel-btn { display: inline-flex; align-items: center; justify-content: space-between; gap: 6px; width: 120px; cursor: pointer; }
.fn-msel-btn .is-ph { color: var(--bl-text-3); }
.fn-msel-arrow { color: var(--bl-text-3); display: inline-flex; transition: transform .15s; }
.fn-msel-arrow.is-open { transform: rotate(180deg); }
.fn-msel-pop {
  position: absolute; top: calc(100% + 4px); left: 0; z-index: 100; min-width: 148px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  box-shadow: 0 6px 18px rgba(0,0,0,.14); padding: 4px;
}
.fn-msel-opt { display: flex; align-items: center; gap: 6px; padding: 6px 8px; border-radius: 4px; font-size: 13px; cursor: pointer; color: var(--bl-text-1); }
.fn-msel-opt:hover { background: var(--bl-bg-hover); }
.fn-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; display: inline-block; }

.fn-add-btn { width: 32px; padding: 0; display: inline-flex; align-items: center; justify-content: center; }

/* 主体 */
.fn-main { flex: 1; display: flex; gap: var(--bl-panel-gap); padding: var(--bl-content-pad); overflow: hidden; }
.pane {
  flex: 1; background: var(--bl-bg-1);
  border: 1px solid var(--bl-panel-border); border-radius: var(--bl-panel-radius);
  overflow: hidden; display: flex; flex-direction: column;
}
.pane-list { min-width: 0; }
.fn-list-scroll { flex: 1; min-height: 0; overflow: auto; }

/* 表格: 固定列宽, 前两列横向冻结 */
.fn-table { width: 100%; min-width: 1670px; table-layout: fixed; }
.fn-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-thead-bg);
  font-weight: 600; font-size: 12px; height: 40px; padding: 0 8px;
  color: var(--bl-text-1); white-space: nowrap; cursor: pointer; user-select: none;
}
.fn-table thead th.t-left { text-align: left; }
.fn-table thead th.t-right { text-align: right; }
.fn-table tbody tr { background: var(--bl-bg-1); cursor: pointer; }
.fn-table tbody tr:hover { background: var(--bl-bg-hover); }
.fn-table tbody tr.is-active { background: var(--bl-primary-soft); }
.fn-table td { padding: 0 8px; font-size: 12px; height: 52px; vertical-align: middle; }
.fn-table td.t-center { text-align: center; }
.fn-table td.t-left { text-align: left; }
.fn-table td.t-right { text-align: right; }

/* 冻结列: 表体继承行底色, 表头角落 z-index 更高 */
.fn-frz1, .fn-frz2 { position: sticky; z-index: 3; background: inherit; }
.fn-frz1 { left: 0; }
.fn-frz2 { left: 40px; box-shadow: 6px 0 8px -6px rgba(0,0,0,.18); }
.fn-table thead th.fn-frz1, .fn-table thead th.fn-frz2 { z-index: 4; background: var(--bl-thead-bg); }
.fn-table tbody tr td.fn-frz1, .fn-table tbody tr td.fn-frz2 { background: var(--bl-bg-1); }
.fn-table tbody tr:hover td.fn-frz1, .fn-table tbody tr:hover td.fn-frz2 { background: var(--bl-bg-hover); }
.fn-table tbody tr.is-active td.fn-frz1, .fn-table tbody tr.is-active td.fn-frz2 { background: var(--bl-primary-soft); }

.fn-sort { display: inline-flex; margin-left: 4px; color: var(--bl-text-3); opacity: .35; vertical-align: middle; }
.fn-sort.is-on { opacity: 1; color: var(--bl-primary); }

/* 单元格内容 */
.fn-name-cell { display: inline-flex; align-items: center; gap: 8px; min-width: 0; max-width: 100%; }
.fn-name-ic {
  width: 26px; height: 26px; border-radius: 6px; flex-shrink: 0; background: #165DFF;
  display: inline-flex; align-items: center; justify-content: center;
}
.fn-name-txt { min-width: 0; }
.fn-api { font-weight: 600; color: var(--bl-primary); font-size: 12.5px; }
.fn-sub { font-size: 11px; }
.fn-obj, .fn-file { font-size: 11.5px; color: var(--bl-text-2); }
.fn-tag { padding: 0 8px; white-space: nowrap; }
.fn-io { font-size: 11.5px; line-height: 18px; }
.fn-io-lbl { color: var(--bl-text-3); margin-right: 3px; }
.fn-vis { display: inline-flex; align-items: center; gap: 4px; color: var(--bl-text-2); }
.fn-st { display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; }
.fn-calls { font-weight: 600; color: var(--bl-text-1); font-variant-numeric: tabular-nums; }
.fn-time { white-space: nowrap; color: var(--bl-text-2); }
.fn-desc {
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; color: var(--bl-text-2); line-height: 16px;
}

/* 底部 */
.fn-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  font-size: 12px; display: flex; justify-content: space-between; align-items: center;
}
.fn-pager-l { display: inline-flex; align-items: center; }
.fn-del-btn { background: var(--bl-bg-1); border: 1px solid #f53f3f; color: #f53f3f; }
.fn-del-btn:hover { background: color-mix(in srgb, #f53f3f 8%, var(--bl-bg-1)); }
.fn-pub-btn { background: var(--bl-bg-1); border: 1px solid #00b42a; color: #00b42a; }
.fn-pub-btn:hover { background: color-mix(in srgb, #00b42a 8%, var(--bl-bg-1)); }
.fn-off-btn { background: var(--bl-bg-1); border: 1px solid #86909c; color: #86909c; }
.fn-off-btn:hover { background: color-mix(in srgb, #86909c 8%, var(--bl-bg-1)); }
</style>
