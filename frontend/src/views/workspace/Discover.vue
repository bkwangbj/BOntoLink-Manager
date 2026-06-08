<template>
  <div class="ov-root">
    <!-- 顶部头 (对齐项目其他页面 48px 紧凑式) -->
    <header class="ov-topbar">
      <div class="ov-title-wrap">
        <span class="ov-title-ic" v-html="BL.icon('chart', 14, 'var(--bl-primary)')"></span>
        <span class="ov-title">总览</span>
        <span class="ov-subtitle">Overview · 全局资源统计概览</span>
      </div>
    </header>

    <div class="ov-body">

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
      <span v-html="BL.icon('map', 12)"></span>
      <span class="ov-bc-text">{{ breadcrumb }}</span>
    </nav>

    <!-- 统计卡片矩阵区 (三行: 4 / 6 / 5) -->
    <section class="ov-stats">
      <div v-for="(group, gi) in STAT_GROUPS" :key="'g-'+gi"
           class="ov-stats-row" :style="{ '--cols': group.cols }">
        <div v-for="s in group.items" :key="s.key"
             class="ov-card" :style="{ '--sc': s.color }"
             @click="onCardClick(s)">
          <!-- 大图标水印 (右下角, 10% 透明度, 与 Category 总览同款) -->
          <span class="ov-card-wm" v-html="BL.icon(s.icon, 76, s.color)"></span>
          <!-- Row 1: 图标徽章 + 中英名称 (中文上 / 英文下) -->
          <div class="ov-card-hd">
            <span class="ov-card-ic" :style="{ color: s.color, background: s.color + '1f' }"
                  v-html="BL.icon(s.icon, 18, s.color)"></span>
            <div class="ov-card-names">
              <span class="ov-card-name">{{ s.cn }}</span>
              <span class="ov-card-en">{{ s.en }}</span>
            </div>
          </div>
          <!-- Row 2: 数字 (active | total) -->
          <div class="ov-card-num" :class="numScale(stats[s.key])">
            <span class="ov-card-active">{{ stats[s.key]?.active ?? 0 }}</span>
            <span class="ov-card-sep">|</span>
            <span class="ov-card-total">{{ stats[s.key]?.total ?? 0 }}</span>
          </div>
          <!-- Row 3: 简要文字概述 -->
          <div class="ov-card-brief" :title="s.brief">{{ s.brief }}</div>
        </div>
      </div>
    </section>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { resourceApi, categoryApi } from '@/api'

const router = useRouter()

/* —— 统计项配置 (3 行布局, 严格对照总览6.5.pdf 表格顺序)
   每项带 icon + color (用作背景渐变 / 左侧色条 / 图标徽章 / 水印) */
const STAT_GROUPS = [
  // 第一行: 4 列 — 结构资源 (行业/领域/分组/数据源)
  { cols: 4, items: [
    { key: 'industry',   cn: '行业数',   en: 'Industry',   icon: 'industry', color: '#165DFF', target: '/config/category',          brief: '系统中已创建的行业总数' },
    { key: 'domain',     cn: '领域数',   en: 'Domain',     icon: 'folder',   color: '#00B42A', target: '/config/category',          brief: '行业下细分的业务领域数量' },
    { key: 'group',      cn: '分组数',   en: 'Group',      icon: 'grid',     color: '#722ED1', target: '/config/category',          brief: '领域内按主题划分的分组' },
    { key: 'datasource', cn: '数据源数', en: 'Datasources',icon: 'database', color: '#FF7D00', target: '/resources/datasources',    brief: '系统接入的物理数据源' }
  ]},
  // 第二行: 6 列 — 本体资源
  { cols: 6, items: [
    { key: 'objectType', cn: '对象类型', en: 'Object types', icon: 'cube',         color: '#165DFF', target: '/resources/object-types', brief: '本体中定义的实体类型' },
    { key: 'linkType',   cn: '关系类型', en: 'Link types',   icon: 'link',         color: '#FF7D00', target: '/resources/link-types',   brief: '对象之间的关联关系' },
    { key: 'actionType', cn: '动作类型', en: 'Action types', icon: 'zap',          color: '#722ED1', target: '/resources/actions',      brief: '本体上可触发的操作' },
    { key: 'function',   cn: '函数',     en: 'Functions',    icon: 'fileCode',     color: '#14C9C9', target: '/resources/functions',    brief: '派生 / 计算逻辑' },
    { key: 'typeClass',  cn: '类型类',   en: 'Type classes', icon: 'tag',          color: '#EB2F96', target: '/config/type-classes',    brief: '抽象接口与规则约束类' },
    { key: 'interface',  cn: '接口',     en: 'Interfaces',   icon: 'externalLink', color: '#2F54EB', target: '/resources/interfaces',   brief: '对外暴露的 API 接口' }
  ]},
  // 第三行: 5 列 — 属性 / 类型
  { cols: 5, items: [
    { key: 'property',       cn: '属性数',   en: 'Properties',     icon: 'fileText', color: '#00B42A', target: '/resources/properties',  brief: '对象上定义的全部属性' },
    { key: 'enumType',       cn: '枚举类型', en: 'Enum types',     icon: 'list',     color: '#722ED1', target: '/resources/enum-types',  brief: '取值有限的枚举字典' },
    { key: 'valueType',      cn: '值类型',   en: 'Value types',    icon: 'hash',     color: '#14C9C9', target: '/resources/value-types', brief: '基础数据类型定义' },
    { key: 'structProperty', cn: '结构属性', en: 'Structural properties', icon: 'sliders', color: '#165DFF', target: '/resources/shared-props', brief: '复合 / 嵌套结构属性' },
    { key: 'sharedProperty', cn: '共享属性', en: 'Shared properties',     icon: 'share',   color: '#FA541C', target: '/resources/shared-props', brief: '跨对象复用的属性' }
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
  height: 100%;
  background: var(--bl-bg-2);
  overflow: hidden;
}

/* —— 顶部头 (对齐 ObjectTypes / Category 等其他页, 48px 紧凑式) —— */
.ov-topbar {
  flex-shrink: 0;
  display: flex; align-items: center;
  padding: 10px 20px 12px;
  border-bottom: 1px solid var(--bl-border);
  background: var(--bl-bg-1);
}
.ov-title-wrap {
  display: flex; align-items: baseline; gap: 8px;
  min-width: 0;
}
.ov-title-ic { display: inline-flex; align-self: center; }
.ov-title {
  font-size: 18px; font-weight: 600; line-height: 1.2;
  color: var(--bl-text-1);
  white-space: nowrap;
}
.ov-subtitle {
  font-size: var(--bl-fs-12);
  color: var(--bl-text-3);
  margin-left: 4px;
}

/* —— 主体: 可滚 + 16px padding (与 ObjectTypes / Category 一致) —— */
.ov-body {
  flex: 1; min-height: 0;
  overflow: auto;
  padding: 16px 20px;
  display: flex; flex-direction: column; gap: 12px;
}

/* —— 筛选导航区 —— */
.ov-filter {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3, 6px);
  overflow: hidden;
}
.ov-filter-row {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 14px;
  position: relative;
}
.ov-filter-industry { border-bottom: 1px solid var(--bl-divider); }

.ov-chips-wrap {
  display: flex; flex-wrap: wrap; gap: 8px;
  flex: 1; min-width: 0;
}
.ov-chip {
  padding: 5px 12px;
  border-radius: 4px;
  border: 1px solid var(--bl-border);
  background: var(--bl-bg-1);
  color: var(--bl-text-2);
  font-size: 12.5px;
  font-weight: 400;
  cursor: pointer;
  transition: background .15s, color .15s, border-color .15s;
  white-space: nowrap;
  line-height: 1.4;
}
.ov-chip:hover {
  background: var(--bl-bg-hover, #f5f7fa);
  color: var(--bl-text-1);
}
.ov-chip.is-on {
  background: var(--bl-primary-soft);
  border-color: var(--bl-primary);
  color: var(--bl-primary);
  font-weight: 500;
}
/* "全部" chip 固定左侧 + 主题色稍深 */
.ov-chip-all {
  flex-shrink: 0;
  background: var(--bl-primary-soft);
  color: var(--bl-primary);
  border-color: var(--bl-primary-soft);
  font-weight: 500;
}
.ov-chip-all.is-on {
  background: var(--bl-primary);
  color: #fff;
  border-color: var(--bl-primary);
}

/* 清空筛选按钮 (右上角) */
.ov-clear-btn {
  position: absolute; top: 8px; right: 8px;
  width: 26px; height: 26px;
  border: 0; background: transparent;
  border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-3);
  cursor: pointer;
  transition: background .15s, color .15s;
}
.ov-clear-btn:hover {
  background: #fff1f0;
  color: #f53f3f;
}

/* —— 面包屑导航区 —— */
.ov-breadcrumb {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 4px;
  color: var(--bl-text-3); font-size: 12px;
}

/* —— 统计卡片矩阵区 —— */
.ov-stats {
  display: flex; flex-direction: column; gap: 12px;
}
.ov-stats-row {
  display: grid;
  grid-template-columns: repeat(var(--cols), minmax(0, 1fr));
  gap: 12px;
}
.ov-card {
  --sc: var(--bl-primary);
  position: relative;
  /* 主题色渐变背景: 左上 12% 主题色 → 右下白底, 与 Category 总览统计卡同款 */
  background: linear-gradient(135deg,
    color-mix(in srgb, var(--sc) 12%, var(--bl-bg-1)) 0%,
    var(--bl-bg-1) 62%);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3, 6px);
  padding: 16px 18px;
  cursor: pointer;
  display: flex; flex-direction: column;
  justify-content: space-between;
  gap: 14px;
  min-height: 120px;
  overflow: hidden;
  transition: border-color .15s, box-shadow .15s, transform .15s;
}
/* 左侧 3px 色条 */
.ov-card::before {
  content: '';
  position: absolute; left: 0; top: 0; bottom: 0; width: 3px;
  background: var(--sc);
  opacity: .85;
}
.ov-card:hover {
  border-color: color-mix(in srgb, var(--sc) 45%, var(--bl-border));
  box-shadow: 0 4px 14px color-mix(in srgb, var(--sc) 18%, transparent);
  transform: translateY(-1px);
}
/* 大图标水印 (右下角倾斜, 8% 透明度) */
.ov-card-wm {
  position: absolute; right: -8px; bottom: -10px;
  opacity: .10; pointer-events: none; line-height: 0;
  transform: rotate(-8deg);
}
/* Row 1: 图标徽章 + 中英名称 (中文上 / 英文下) */
.ov-card-hd {
  display: flex; align-items: center; gap: 8px;
  position: relative; z-index: 1;
}
.ov-card-ic {
  width: 30px; height: 30px; border-radius: 6px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ov-card-names {
  display: flex; flex-direction: column; line-height: 1.2;
  min-width: 0;
}
.ov-card-name {
  font-size: 13px; font-weight: 600;
  color: var(--bl-text-1);
  white-space: nowrap;
}
.ov-card-en {
  font-size: 10.5px; font-weight: 400;
  color: var(--bl-text-3);
  margin-top: 2px;
  white-space: nowrap;
}
/* Row 2: 数字 (active | total) — 缩进 38px 与上方文字对齐 */
.ov-card-num {
  font-size: 22px;
  font-weight: 700;
  display: flex; align-items: baseline; gap: 5px;
  padding-left: 38px;
  font-family: "DIN", "Helvetica Neue", Helvetica, Arial, sans-serif;
  position: relative; z-index: 1;
  font-feature-settings: "tnum";
}
.ov-card-num.is-small { font-size: 16px; }
.ov-card-active { color: #00B42A; }
.ov-card-sep { color: var(--bl-text-3); font-weight: 400; font-size: 16px; }
.ov-card-total { color: var(--bl-text-1); font-size: 18px; font-weight: 500; }
/* Row 3: 简要文字概述 — 与数字同样缩进对齐 */
.ov-card-brief {
  font-size: 11.5px; color: var(--bl-text-3);
  padding-left: 38px;
  position: relative; z-index: 1;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* —— 响应式断点 —— */
@media (max-width: 1280px) {
  .ov-stats-row { grid-template-columns: repeat(min(var(--cols), 3), minmax(0, 1fr)); }
}
@media (max-width: 960px) {
  .ov-stats-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .ov-stats-row { grid-template-columns: 1fr; }
}
</style>
