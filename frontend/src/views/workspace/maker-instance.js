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
function makeTab (tpl, classId, dim, baseQuery, kind) {
  const c = clone(tpl)
  const id = uid()
  c.id = id
  c.chartId = uid()
  c.title = kind === 'pie' ? `${dim.label} 占比` : `${dim.label} 分布`
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

/** 按列特征自动生成多图表的初始大屏布局 */
export function buildPageConfig (classId, columns, baseQuery = {}) {
  if (!classId || !BAR_TPL) return null
  const dims = (columns || []).filter(c =>
    ['string', 'enum', 'boolean'].includes(c.dataType) &&
    !/编码|名称|code|name|编号/i.test((c.label || '') + ' ' + (c.field || ''))
  )
  const useDims = dims.length ? dims.slice(0, 4) : (columns || []).slice(0, 1)
  if (!useDims.length || !useDims[0].field) return null

  const layout = []
  // 首维:柱图 + 饼图 并排
  layout.push(gridItem(makeTab(BAR_TPL, classId, useDims[0], baseQuery, 'bar'), 0, 0))
  if (PIE_TPL) layout.push(gridItem(makeTab(PIE_TPL, classId, useDims[0], baseQuery, 'pie'), 6, 0))
  // 其余维度:每个一个柱图,两列网格往下排
  for (let i = 1; i < useDims.length; i++) {
    const col = (i - 1) % 2 === 0 ? 0 : 6
    const row = 9 + Math.floor((i - 1) / 2) * 9
    layout.push(gridItem(makeTab(BAR_TPL, classId, useDims[i], baseQuery, 'bar'), col, row))
  }
  return { layout, decorateLayout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, maxRows: Infinity }
}
