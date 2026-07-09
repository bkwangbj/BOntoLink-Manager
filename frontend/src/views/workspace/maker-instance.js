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
    interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {},
    // 预置数据源面板,与图表一致:按该字段分组、统计数量、按数量降序(用户可增删改)
    metrics: [{ field, aggs: ['count'] }],                                       // 指标 = 该字段的计数(数量)
    grouping: { field, mode: 'include', includeOther: false, selected: [] },     // 分组维度 = 该字段
    sorts: [{ field, agg: 'count', desc: true }]                                 // 排序 = 按该字段计数降序
  }
}

// 深色看板:把图表文字(图例/坐标轴/标签)改为浅色,坐标轴/网格线压深
function applyDarkText (c) {
  const COL = '#C9D6EC'
  if (c.chartTheme) { c.chartTheme.textColor = COL; if ('axisLineColor' in c.chartTheme) c.chartTheme.axisLineColor = '#2A3A5E'; if ('splitLineColor' in c.chartTheme) c.chartTheme.splitLineColor = '#22304F' }
  const o = c.configOption
  if (!o) return
  if (o.legend) o.legend.textStyle = { ...(o.legend.textStyle || {}), color: COL }
  const axes = []
  if (o.xAxis) axes.push(...(Array.isArray(o.xAxis) ? o.xAxis : [o.xAxis]))
  if (o.yAxis) axes.push(...(Array.isArray(o.yAxis) ? o.yAxis : [o.yAxis]))
  axes.forEach(a => {
    if (!a) return
    a.axisLabel = { ...(a.axisLabel || {}), color: COL }
    a.nameTextStyle = { ...(a.nameTextStyle || {}), color: COL }
    if (a.splitLine && a.splitLine.lineStyle) a.splitLine.lineStyle.color = '#22304F'
    if (a.axisLine && a.axisLine.lineStyle) a.axisLine.lineStyle.color = '#2A3A5E'
  })
  if (Array.isArray(o.series)) o.series.forEach(s => { if (s && s.label) s.label = { ...s.label, color: COL } })
}

// 构造图表「tab」配置(扁平,保留模板的 chartComId/type/configOption/chartTheme)
function makeTab (tpl, classId, dim, baseQuery, kind, titlePrefix, isDark, cols) {
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
  // 数据源面板下拉选项 = 对象类型属性列表(指标/分组维度/排序字段可选)
  c.dataSourceConfig.fieldOptions = (cols || []).map(o => ({ label: o.label || o.field, value: o.field, dataType: o.dataType })).filter(o => o.value)
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
  if (isDark) applyDarkText(c)
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
export function buildPageConfig (classId, columns, baseQuery = {}, linkGroups = [], isDark = false) {
  if (!classId || !BAR_TPL) return null
  const mainDims = pickDims(columns)
  const useMain = mainDims.length ? mainDims.slice(0, 4) : (columns || []).slice(0, 1)
  if (!useMain.length || !useMain[0].field) return null

  // 收集图表规格(specs):主对象 + 链接对象
  const specs = []
  // 主对象首维:柱图 + 饼图 并排(便于同时看分布与占比)
  specs.push({ classId, dim: useMain[0], kind: 'bar', main: true, cols: columns })
  if (PIE_TPL) specs.push({ classId, dim: useMain[0], kind: 'pie', main: true, cols: columns })
  // 主对象其余维度:按类型选图
  for (let i = 1; i < useMain.length; i++) specs.push({ classId, dim: useMain[i], kind: kindFor(useMain[i]), main: true, cols: columns })
  // 链接对象:每个关联类型取前 2 个可视维度,标题带关联对象名
  for (const g of (linkGroups || []).slice(0, 3)) {
    if (!g || !g.classId) continue
    const dims = pickDims(g.columns).slice(0, 2)
    for (const d of dims) specs.push({ classId: g.classId, dim: d, kind: kindFor(d), prefix: g.name, cols: g.columns })
  }

  // 两列网格依次排布(每图 6 列宽 × 9 行高)
  // 一行三列:每图 4 列宽(12/3),按 3 个一行排布
  const layout = specs.map((s, idx) => {
    const x = (idx % 3) * 4
    const y = Math.floor(idx / 3) * 9
    const tpl = (s.kind === 'pie' && PIE_TPL) ? PIE_TPL : BAR_TPL
    // 链接对象的聚合用其自身实例(mock 无联表),不套主对象筛选
    const q = s.main ? baseQuery : {}
    return gridItem(makeTab(tpl, s.classId, s.dim, q, s.kind, s.prefix, isDark, s.cols), x, y, 4)
  })
  return { layout, decorateLayout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, maxRows: Infinity, varConfig: [], margin: [10, 10] }
}

/** 空白看板配置:layout 为空,用于「新建看板」打开全空设计画布(区别于按数据自动出图) */
export function buildBlankPageConfig () {
  return { layout: [], decorateLayout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, maxRows: Infinity, varConfig: [], margin: [10, 10] }
}

// 可绑实例数据的图表类型(数据形态 = 分组 + 计数,{name,value});其它(地图/仪表盘/排名/代码…)跳过
const EMBED_BINDABLE = ['BKBarChart', 'BKPieChart']

/**
 * 生成「新增图表默认数据源」工厂,供 maker 在新增图表时回调(embedDefaultDataSource 钩子)。
 * 默认按第一个可用分类维度 groupBy+count 绑定实例 chart-data;用户可在数据源面板改维度。
 * 返回的工厂:(chartConfig) => { dataSourceConfig, items, autoSeries } | null(类型不支持时 null)
 */
export function buildEmbedDefaultDataSource (classId, columns, filterParams = {}, isDark = false) {
  const dims = pickDims(columns)
  const dim = dims.length ? dims[0] : (columns || [])[0]
  if (!classId || !dim || !dim.field) return null
  const fieldOptions = (columns || []).map(o => ({ label: o.label || o.field, value: o.field, dataType: o.dataType })).filter(o => o.value)
  return (chartConfig) => {
    if (!chartConfig || !EMBED_BINDABLE.includes(chartConfig.type)) return null
    // 气泡图需要 x/y/size 三维,实例聚合(name/value)喂不了 → 直接给一份静态演示数据源
    if (chartConfig.branchType === 'bubbleChart') {
      const demo = [
        { x: 73, y: 20, size: 30, colorField: '华北' }, { x: 90, y: 35, size: 60, colorField: '华东' },
        { x: 105, y: 28, size: 45, colorField: '华中' }, { x: 118, y: 42, size: 80, colorField: '东北' },
        { x: 100, y: 25, size: 20, colorField: '西南' }, { x: 112, y: 38, size: 55, colorField: '华南' }
      ]
      return {
        dataSourceConfig: { type: 'static', data: demo, value: JSON.stringify(demo), paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {}, fieldOptions },
        items: [{ label: 'x轴', field: 'x', value: 'x' }, { label: 'y轴', field: 'y', value: 'y' }, { label: '气泡大小', field: 'size', value: 'size' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
      }
    }
    // 日历热力图需要 date/value:优先绑实例里的日期字段(分组+计数),否则给一份静态演示
    if (chartConfig.branchType === 'calendarHeatmap') {
      const dateCol = (columns || []).find(c => c.dataType === 'date' || /日期|时间|date|time/i.test((c.label || '') + ' ' + (c.field || '')))
      if (dateCol && dateCol.field) {
        const ds = dataSource(classId, dateCol.field, filterParams)
        ds.fieldOptions = fieldOptions
        ds.sorts = [{ field: dateCol.field, agg: 'count', desc: false }]
        return { dataSourceConfig: ds, items: [{ label: '日期', field: 'name', value: 'date' }, { label: '数值', field: 'value', value: 'value' }] }
      }
      const demo = [
        { date: '2026-01-05', value: 12 }, { date: '2026-01-18', value: 30 }, { date: '2026-02-03', value: 8 },
        { date: '2026-02-20', value: 45 }, { date: '2026-03-11', value: 22 }, { date: '2026-04-06', value: 60 },
        { date: '2026-05-15', value: 18 }, { date: '2026-06-09', value: 37 }, { date: '2026-07-01', value: 52 }
      ]
      return {
        dataSourceConfig: { type: 'static', data: demo, value: JSON.stringify(demo), paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {}, fieldOptions },
        items: [{ label: '日期', field: 'date', value: 'date' }, { label: '数值', field: 'value', value: 'value' }]
      }
    }
    const ds = dataSource(classId, dim.field, filterParams)
    ds.fieldOptions = fieldOptions
    if (chartConfig.type === 'BKPieChart') {
      return { dataSourceConfig: ds, items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }] }
    }
    // 柱/条/线:数据 name/value 映射到 x/y,开启 autoSeries(与自动图表一致)
    return {
      dataSourceConfig: ds,
      items: [
        { label: '名称', field: 'name', value: 'x' },
        { label: '数量', field: 'value', value: 'y' },
        { label: '颜色映射', field: 'colorField', value: 'colorField' }
      ],
      autoSeries: true
    }
  }
}
