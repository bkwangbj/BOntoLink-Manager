/**
 * 由实例探索上下文(对象类型 + 列 + 筛选)自动构造 analysis-maker 的初始 pageConfig。
 * 关键结构:layout 每项是「网格卡片」,图表配置放在该卡片的 tabList[0] 里。
 * 按数据特征自动出图:每个分类维度一个柱图,首维再加一个饼图;数据走 /instance/chart-data。
 */
import { chartList } from './maker-presets'

const BAR_TPL = chartList?.[0]?.children?.[1]   // BKBarChart 模板(菜单态,扁平)
const PIE_TPL = chartList?.[0]?.children?.[0]   // BKPieChart 模板

function uid () { return (crypto.randomUUID ? crypto.randomUUID() : 'id-' + Math.random().toString(36).slice(2) + Date.now().toString(36)) }
function clone (o) { return JSON.parse(JSON.stringify(o)) }

function dataSource (classId, field, baseQuery) {
  let qs = `classId=${encodeURIComponent(classId)}&groupBy=${encodeURIComponent(field)}&agg=count&limit=12`
  if (baseQuery && baseQuery.filter) qs += `&filter=${encodeURIComponent(baseQuery.filter)}`
  if (baseQuery && baseQuery.q) qs += `&q=${encodeURIComponent(baseQuery.q)}`
  return {
    type: 'get', interfacePath: `/instance/chart-data?${qs}`, paramsType: 'json',
    interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {}
  }
}

// 构造图表「tab」配置(扁平,保留模板的 chartComId/type/configOption/chartTheme)
function makeTab (tpl, classId, dim, baseQuery, kind, titlePrefix) {
  const c = clone(tpl)
  const id = uid()
  c.id = id
  c.chartId = uid()
  const base = titlePrefix ? `${titlePrefix} · ${dim.label}` : dim.label
  c.title = kind === 'pie' ? `${base} 占比` : `${base} 分布`
  c.isShowTitle = '1'
  c.varListener = []
  c.eventConfig = []
  c.tabord = 0
  c.initChartId = true
  c.dataSourceConfig = dataSource(classId, dim.field, baseQuery)
  if (kind === 'pie') {
    c.items = [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }]
    if (c.configOption && Array.isArray(c.configOption.series) && c.configOption.series.length) {
      const s = c.configOption.series[0]
      s.center = [50, 50]; s.radius = [0, 65]; s.dataId = ''; s.name = '占比'
      c.configOption.series = [s]
    }
    c.branchType = 'pieChart'
  } else {
    c.items = [
      { label: '名称', field: 'name', value: 'x' },
      { label: '数量', field: 'value', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
    if (c.configOption) c.configOption.autoSeries = true
  }
  return c
}

// 把图表 tab 包成网格卡片
function gridItem (tab, x, y, w, h) {
  return { isChart: true, x, y, w: w || 6, h: h || 9, i: tab.id, id: tab.id, moved: false, tabList: [tab] }
}

const DIM_RE = /编码|名称|code|name|编号/i
// 可作分类维度的列(枚举/字符串/布尔,排除编码名称)
function pickDims (columns) {
  return (columns || []).filter(c =>
    ['string', 'enum', 'boolean'].includes(c.dataType) && !DIM_RE.test((c.label || '') + ' ' + (c.field || '')))
}
// 按数据特征选图表类型:枚举/布尔 → 饼图(看占比);其它分类 → 柱图(看分布)
function kindFor (col) {
  return (col.dataType === 'boolean' || col.dataType === 'enum') ? 'pie' : 'bar'
}

/**
 * 按列特征自动生成多图表的初始大屏布局。
 * 同时分析:主对象类型 + 链接对象类型 的属性/枚举,自动选图表类型。
 * @param {Array} linkGroups [{ classId, name, columns }] 链接对象类型及其列
 */
export function buildPageConfig (classId, columns, baseQuery = {}, linkGroups = []) {
  if (!classId || !BAR_TPL) return null
  const mainDims = pickDims(columns)
  const useMain = mainDims.length ? mainDims.slice(0, 4) : (columns || []).slice(0, 1)
  if (!useMain.length || !useMain[0].field) return null

  // 收集图表规格(specs):主对象 + 链接对象
  const specs = []
  // 主对象首维:柱图 + 饼图 并排(便于同时看分布与占比)
  specs.push({ classId, dim: useMain[0], kind: 'bar', main: true })
  if (PIE_TPL) specs.push({ classId, dim: useMain[0], kind: 'pie', main: true })
  // 主对象其余维度:按类型选图
  for (let i = 1; i < useMain.length; i++) specs.push({ classId, dim: useMain[i], kind: kindFor(useMain[i]), main: true })
  // 链接对象:每个关联类型取前 2 个可视维度,标题带关联对象名
  for (const g of (linkGroups || []).slice(0, 3)) {
    if (!g || !g.classId) continue
    const dims = pickDims(g.columns).slice(0, 2)
    for (const d of dims) specs.push({ classId: g.classId, dim: d, kind: kindFor(d), prefix: g.name })
  }

  // 两列网格依次排布(每图 6 列宽 × 9 行高)
  const layout = specs.map((s, idx) => {
    const x = idx % 2 === 0 ? 0 : 6
    const y = Math.floor(idx / 2) * 9
    const tpl = (s.kind === 'pie' && PIE_TPL) ? PIE_TPL : BAR_TPL
    // 链接对象的聚合用其自身实例(mock 无联表),不套主对象筛选
    const q = s.main ? baseQuery : {}
    return gridItem(makeTab(tpl, s.classId, s.dim, q, s.kind, s.prefix), x, y)
  })
  return { layout, decorateLayout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, maxRows: Infinity }
}
