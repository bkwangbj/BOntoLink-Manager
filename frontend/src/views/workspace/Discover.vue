<template>
  <div class="ov-root">
    <!-- 头部标题区 (高 60px, 图标 + 主标题 + 英文副标题) -->
    <header class="ov-hd">
      <span class="ov-hd-ic" v-html="BL.icon('chart', 22, '#3b82f6')"></span>
      <span class="ov-hd-title">总览</span>
      <span class="ov-hd-en">(Overview)</span>
    </header>

    <!-- 筛选导航区: 行业 / 领域 双行 + 右上角清空 -->
    <section class="ov-filter">
      <!-- 行业选择 -->
      <div class="ov-filter-row ov-filter-industry">
        <button :class="['ov-chip', 'ov-chip-all', industriesSel.length === 0 && 'is-on']"
                @click="setAllIndustries">全部行业</button>
        <div class="ov-chips-wrap">
          <button v-for="ind in industries" :key="ind.category_code"
                  :class="['ov-chip', industriesSel.includes(ind.category_code) && 'is-on']"
                  @click="toggleIndustry(ind.category_code)">{{ ind.cn || ind.category_code }}</button>
        </div>
      </div>
      <!-- 领域选择 + 清空按钮 (右上) -->
      <div class="ov-filter-row ov-filter-domain">
        <button :class="['ov-chip', 'ov-chip-all', domainsSel.length === 0 && 'is-on']"
                @click="setAllDomains">全部领域</button>
        <div class="ov-chips-wrap">
          <button v-for="dom in availableDomains" :key="dom.category_code"
                  :class="['ov-chip', domainsSel.includes(dom.category_code) && 'is-on']"
                  @click="toggleDomain(dom.category_code)">{{ dom.cn || dom.category_code }}</button>
        </div>
        <button class="ov-clear-btn" title="清空筛选条件" @click="clearFilter">
          <span v-html="BL.icon('x', 18)"></span>
        </button>
      </div>
    </section>

    <!-- 面包屑导航区: 当前统计范围 -->
    <nav class="ov-breadcrumb">
      <span v-html="BL.icon('map', 13, '#6b7280')"></span>
      <span class="ov-bc-text">{{ breadcrumb }}</span>
    </nav>

    <!-- 统计卡片矩阵区 (三行: 4 / 6 / 5) -->
    <section class="ov-stats">
      <div v-for="(group, gi) in STAT_GROUPS" :key="'g-'+gi"
           class="ov-stats-row" :style="{ '--cols': group.cols }">
        <div v-for="s in group.items" :key="s.key"
             class="ov-card" @click="onCardClick(s)">
          <div class="ov-card-name">{{ s.cn }}</div>
          <div class="ov-card-en">{{ s.en }}</div>
          <div class="ov-card-num" :class="numScale(stats[s.key])">
            <span class="ov-card-active">{{ stats[s.key]?.active ?? 0 }}</span>
            <span class="ov-card-sep">|</span>
            <span class="ov-card-total">{{ stats[s.key]?.total ?? 0 }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { resourceApi, categoryApi } from '@/api'

const router = useRouter()

/* —— 统计项配置 (3 行布局, 严格对照总览6.5.pdf 表格顺序) —— */
const STAT_GROUPS = [
  // 第一行: 4 列 (大屏)
  { cols: 4, items: [
    { key: 'industry',        cn: '行业数',     en: 'Industry',        target: '/config/category' },
    { key: 'domain',          cn: '领域数',     en: 'Domain',          target: '/config/category' },
    { key: 'group',           cn: '分组数',     en: 'Group',           target: '/config/category' },
    { key: 'datasource',      cn: '数据源数',   en: 'Datasources',     target: '/resources/datasources' }
  ]},
  // 第二行: 6 列
  { cols: 6, items: [
    { key: 'objectType',      cn: '对象类型',   en: 'Object types',    target: '/resources/object-types' },
    { key: 'linkType',        cn: '关系类型',   en: 'Link types',      target: '/resources/link-types' },
    { key: 'actionType',      cn: '动作类型',   en: 'Action types',    target: '/resources/actions' },
    { key: 'function',        cn: '函数',       en: 'Functions',       target: '/resources/functions' },
    { key: 'typeClass',       cn: '类型类',     en: 'Type classes',    target: '/config/type-classes' },
    { key: 'interface',       cn: '接口',       en: 'Interfaces',      target: '/resources/interfaces' }
  ]},
  // 第三行: 5 列
  { cols: 5, items: [
    { key: 'property',        cn: '属性数',     en: 'Properties',      target: '/resources/properties' },
    { key: 'enumType',        cn: '枚举类型',   en: 'Enum types',      target: '/resources/enum-types' },
    { key: 'valueType',       cn: '值类型',     en: 'Value types',     target: '/resources/value-types' },
    { key: 'structProperty',  cn: '结构属性',   en: 'Structural properties', target: '/resources/shared-props' },
    { key: 'sharedProperty',  cn: '共享属性',   en: 'Shared properties',     target: '/resources/shared-props' }
  ]}
]

/* —— 数据 —— */
const industries = ref([])          // [{ category_code, cn, children: [...] }, ...]
const industryByCode = computed(() => {
  const m = {}
  industries.value.forEach(i => { m[i.category_code] = i })
  return m
})
const industriesSel = ref([])       // 选中的行业 codes (空 = 全部)
const domainsSel = ref([])          // 选中的领域 codes (空 = 全部)
const stats = reactive({})          // { industry: {active, total}, ... }

/* 当前可选领域 = 选中行业下的所有领域+子领域 (不分层级平铺) */
const availableDomains = computed(() => {
  const out = []
  const seen = new Set()
  // 若未选行业, 展开所有行业下的所有领域
  const scope = industriesSel.value.length
    ? industriesSel.value.map(c => industryByCode.value[c]).filter(Boolean)
    : industries.value
  scope.forEach(ind => collectLeaves(ind, out, seen))
  return out
})

function collectLeaves(node, sink, seen) {
  if (!node?.children) return
  node.children.forEach(c => {
    if (c.category_code && !seen.has(c.category_code)) {
      seen.add(c.category_code)
      sink.push({ category_code: c.category_code, cn: c.cn || c.label || c.category_code })
    }
    collectLeaves(c, sink, seen)
  })
}

/* —— 加载行业树 —— */
async function loadIndustries() {
  const tree = await categoryApi.tree().catch(() => [])
  // 标准化字段: tree 节点可能用 label / cn / name 等; 统一处理
  const norm = (n) => ({
    category_code: n.categoryCode || n.category_code || n.code || n.id,
    cn: n.cn || n.label || n.name || n.categoryCode,
    children: (n.children || []).map(norm)
  })
  industries.value = (Array.isArray(tree) ? tree : []).map(norm)
}

/* —— 加载统计 —— */
async function loadStats() {
  const params = {}
  if (industriesSel.value.length) params.industries = industriesSel.value.join(',')
  if (domainsSel.value.length)    params.domains    = domainsSel.value.join(',')
  const res = await resourceApi.overview(params).catch(() => null)
  if (!res?.rows) { Object.keys(stats).forEach(k => delete stats[k]); return }
  Object.keys(res.rows).forEach(k => { stats[k] = res.rows[k] })
}

/* —— 互斥切换: 行业 —— */
function setAllIndustries() {
  industriesSel.value = []
  domainsSel.value = []     // 切换行业自动重置领域
}
function toggleIndustry(code) {
  const i = industriesSel.value.indexOf(code)
  if (i >= 0) industriesSel.value.splice(i, 1)
  else industriesSel.value.push(code)
  domainsSel.value = []     // 切换行业自动重置领域
}

/* —— 互斥切换: 领域 —— */
function setAllDomains() { domainsSel.value = [] }
function toggleDomain(code) {
  const i = domainsSel.value.indexOf(code)
  if (i >= 0) domainsSel.value.splice(i, 1)
  else domainsSel.value.push(code)
}

/* —— 清空筛选 —— */
function clearFilter() {
  industriesSel.value = []
  domainsSel.value = []
}

/* —— 面包屑文本 —— */
const breadcrumb = computed(() => {
  const indPart = industriesSel.value.length === 0
    ? '全部行业'
    : namesOf(industriesSel.value, industryByCode.value)
  const domPart = domainsSel.value.length === 0
    ? '全部领域'
    : namesOf(domainsSel.value, availableDomainByCode.value)
  return `当前统计范围: ${indPart} | ${domPart}`
})
const availableDomainByCode = computed(() => {
  const m = {}
  availableDomains.value.forEach(d => { m[d.category_code] = d })
  return m
})
function namesOf(codes, dict) {
  const names = codes.map(c => dict[c]?.cn || c)
  if (names.length <= 3) return names.join('、')
  return names.slice(0, 3).join('、') + `...等 ${names.length} 个`
}

/* —— 卡片跳转 (带筛选 query) —— */
function onCardClick(s) {
  if (!s.target) { BL.info(`${s.cn} 模块跳转待联调`); return }
  const query = {}
  if (industriesSel.value.length) query.industries = industriesSel.value.join(',')
  if (domainsSel.value.length)    query.domains    = domainsSel.value.join(',')
  router.push({ path: s.target, query })
}

/* —— 数字超长自适应缩字号 (>6 位 缩) —— */
function numScale(v) {
  if (!v) return ''
  const totalStr = String((v.active ?? 0)) + String((v.total ?? 0))
  if (totalStr.length > 6) return 'is-small'
  return ''
}

/* —— 监听筛选变化, 自动刷新统计 —— */
watch([industriesSel, domainsSel], loadStats, { deep: true })

onMounted(async () => {
  await loadIndustries()
  await loadStats()
})
</script>

<style scoped>
.ov-root {
  display: flex; flex-direction: column;
  min-height: 100%;
  padding: 0 24px 24px;
  background: #f9fafb;
  font-family: "Microsoft YaHei", "微软雅黑", sans-serif;
  overflow: auto;
}

/* —— 头部标题区 (60px 高) —— */
.ov-hd {
  display: flex; align-items: center; gap: 10px;
  height: 60px; flex-shrink: 0;
}
.ov-hd-ic { display: inline-flex; }
.ov-hd-title { font-size: 24px; font-weight: 700; color: #1f2937; }
.ov-hd-en { font-size: 16px; font-weight: 400; color: #6b7280; }

/* —— 筛选导航区 —— */
.ov-filter {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  overflow: hidden;
}
.ov-filter-row {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 20px;
  position: relative;
}
.ov-filter-industry { border-bottom: 1px solid #e5e7eb; }

.ov-chips-wrap {
  display: flex; flex-wrap: wrap; gap: 12px;
  flex: 1; min-width: 0;
}
.ov-chip {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: linear-gradient(180deg, #f9fafb 0%, #f3f4f6 100%);
  color: #374151;
  font-size: 14px;
  font-weight: 400;
  cursor: pointer;
  transition: background .15s, color .15s, border-color .15s;
  white-space: nowrap;
}
.ov-chip:hover {
  background: linear-gradient(180deg, #f3f4f6 0%, #e5e7eb 100%);
}
.ov-chip.is-on {
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
  border-color: rgba(59, 130, 246, 0.3);
  color: #3b82f6;
}
/* "全部" chip 默认稍深蓝渐变, 固定在左侧不参与 wrap */
.ov-chip-all {
  flex-shrink: 0;
  background: linear-gradient(180deg, #dbeafe 0%, #bfdbfe 100%);
  color: #3b82f6;
  font-weight: 500;
}
.ov-chip-all.is-on {
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  border-color: transparent;
}

/* 清空筛选按钮 —— 右上角 */
.ov-clear-btn {
  position: absolute; top: 12px; right: 12px;
  width: 32px; height: 32px;
  border: 0; background: transparent;
  border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  color: #9ca3af;
  cursor: pointer;
  transition: background .15s, color .15s;
}
.ov-clear-btn:hover {
  background: #f3f4f6;
  color: #ef4444;
}

/* —— 面包屑导航区 (32px 高) —— */
.ov-breadcrumb {
  display: flex; align-items: center; gap: 6px;
  height: 32px; margin-top: 16px;
  color: #6b7280; font-size: 14px;
}

/* —— 统计卡片矩阵区 —— */
.ov-stats {
  display: flex; flex-direction: column; gap: 24px;
  margin-top: 8px;
}
.ov-stats-row {
  display: grid;
  grid-template-columns: repeat(var(--cols), minmax(0, 1fr));
  gap: 24px;
}
.ov-card {
  height: 120px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 16px 20px;
  cursor: pointer;
  display: flex; flex-direction: column;
  justify-content: space-between;
  transition: transform .18s, box-shadow .18s;
}
.ov-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.ov-card-name {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}
.ov-card-en {
  font-size: 12px;
  font-weight: 400;
  color: #6b7280;
  margin-top: -4px;
}
.ov-card-num {
  font-size: 30px;
  font-weight: 700;
  display: flex; align-items: baseline; gap: 8px;
  font-family: "DIN", "Helvetica Neue", Helvetica, Arial, sans-serif;
}
.ov-card-num.is-small { font-size: 22px; }      /* 数字过长自适应缩字 */
.ov-card-active { color: #10b981; }
.ov-card-sep { color: #9ca3af; font-weight: 400; }
.ov-card-total { color: #1f2937; }

/* —— 响应式断点 —— */
@media (max-width: 1280px) {
  .ov-stats-row {
    /* 中屏: 第一行 2 列, 第二行 3 列, 第三行 3 列 */
    grid-template-columns: repeat(min(var(--cols), 3), minmax(0, 1fr));
  }
}
@media (max-width: 960px) {
  .ov-stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .ov-stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
