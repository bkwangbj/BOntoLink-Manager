/**
 * 由实例探索上下文(对象类型 + 列 + 筛选)自动构造 analysis-maker 的初始 pageConfig。
 * 关键结构:layout 每项是「网格卡片」,图表配置放在该卡片的 tabList[0] 里。
 * 按数据特征自动出图:每个分类维度一个柱图,首维再加一个饼图;数据走 /instance/chart-data。
 */
import { chartList } from './maker-presets'

const BAR_TPL = chartList?.[0]?.children?.[1]   // BKBarChart 模板(菜单态,扁平)
const PIE_TPL = chartList?.[0]?.children?.[0]   // BKPieChart 模板
const LINE_TPL = chartList?.[0]?.children?.[2]  // BKBarChart(branchType lineChart)折线模板

// 按图表类型取模板
function tplFor (kind) {
  if (kind === 'pie') return PIE_TPL || BAR_TPL
  if (kind === 'line') return LINE_TPL || BAR_TPL
  return BAR_TPL
}

function uid () { return (crypto.randomUUID ? crypto.randomUUID() : 'id-' + Math.random().toString(36).slice(2) + Date.now().toString(36)) }
// 日历热力图演示数据:当年全年每 2~3 天一个点、值 5~95(确定性生成)
function calDemo () {
  const out = []
  const pad = (n) => String(n).padStart(2, '0')
  const y = new Date().getFullYear()
  const leap = (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  let seed = 7
  const rnd = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280 }
  for (let m = 1; m <= 12; m++) {
    for (let d = 1; d <= days[m - 1]; d += 2 + Math.floor(rnd() * 2)) {
      out.push({ date: `${y}-${pad(m)}-${pad(d)}`, value: 5 + Math.floor(rnd() * 90) })
    }
  }
  return out
}
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
  c.title = kind === 'pie' ? `${base} 占比` : kind === 'line' ? `${base} 趋势` : `${base} 分布`
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
// 数值字段(排除编码/编号类),按数值分布出柱图
function pickNums (columns) {
  return (columns || []).filter(c =>
    ['int', 'decimal'].includes(c.dataType) && !DIM_RE.test((c.label || '') + ' ' + (c.field || '')))
}
// 日期/时间字段,按时间趋势出折线
function pickDates (columns) {
  return (columns || []).filter(c => ['date', 'datetime', 'time'].includes(c.dataType))
}

/**
 * 生成「推荐图表候选列表」(供推荐弹框勾选)。覆盖:
 *  - 分类统计:枚举/布尔 → 占比(饼)+ 分布(柱);字符串 → 分布(柱)
 *  - 数值分布:数值字段 → 分布(柱)
 *  - 时间趋势:日期/时间字段 → 趋势(折线)
 *  - 关联对象:每个关联类型的前 3 个分类维度
 * 候选项:{ key, classId, field, label, kind, group, prefix?, cols, main }
 */
export function buildRecommendations (classId, columns, linkGroups = []) {
  const out = []
  for (const c of pickDims(columns)) {
    const label = c.label || c.field
    if (c.dataType === 'enum' || c.dataType === 'boolean') {
      out.push({ key: `m-${c.field}-pie`, classId, field: c.field, label, kind: 'pie', group: '分类统计', cols: columns, main: true })
      out.push({ key: `m-${c.field}-bar`, classId, field: c.field, label, kind: 'bar', group: '分类统计', cols: columns, main: true })
    } else {
      out.push({ key: `m-${c.field}-bar`, classId, field: c.field, label, kind: 'bar', group: '分类统计', cols: columns, main: true })
    }
  }
  for (const c of pickNums(columns)) {
    out.push({ key: `n-${c.field}`, classId, field: c.field, label: c.label || c.field, kind: 'bar', group: '数值分布', cols: columns, main: true })
  }
  for (const c of pickDates(columns)) {
    out.push({ key: `d-${c.field}`, classId, field: c.field, label: c.label || c.field, kind: 'line', group: '时间趋势', cols: columns, main: true })
  }
  for (const g of (linkGroups || [])) {
    if (!g || !g.classId) continue
    for (const d of pickDims(g.columns).slice(0, 3)) {
      out.push({ key: `l-${g.classId}-${d.field}`, classId: g.classId, field: d.field, label: d.label || d.field, kind: kindFor(d), group: `关联 · ${g.name}`, prefix: g.name, cols: g.columns })
    }
  }
  return out
}

/** 由「选中的推荐候选」组装看板 pageConfig(最多 6 个,3 列网格)。 */
export function buildPageConfigFrom (selected, { isDark = false, baseQuery = {} } = {}) {
  const list = (selected || []).slice(0, 6)
  const layout = list.map((s, idx) => {
    const x = (idx % 3) * 4
    const y = Math.floor(idx / 3) * 9
    const q = s.main ? baseQuery : {}
    const tab = makeTab(tplFor(s.kind), s.classId, { field: s.field, label: s.label }, q, s.kind, s.prefix, isDark, s.cols)
    return gridItem(tab, x, y, 4)
  })
  return { layout, decorateLayout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, maxRows: Infinity, varConfig: [], margin: [10, 10] }
}

/** 兜底:无用户选择时按推荐前 6 自动出图(保留旧签名与行为) */
export function buildPageConfig (classId, columns, baseQuery = {}, linkGroups = [], isDark = false) {
  if (!classId || !BAR_TPL) return null
  let recs = buildRecommendations(classId, columns, linkGroups)
  if (!recs.length) {
    const c0 = (columns || []).find(c => c.field)
    if (!c0) return null
    recs = [{ classId, field: c0.field, label: c0.label || c0.field, kind: 'bar', cols: columns, main: true }]
  }
  return buildPageConfigFrom(recs.slice(0, 6), { isDark, baseQuery })
}

/** 空白看板配置:layout 为空,用于「新建看板」打开全空设计画布(区别于按数据自动出图) */
export function buildBlankPageConfig () {
  return { layout: [], decorateLayout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, maxRows: Infinity, varConfig: [], margin: [10, 10] }
}

// 可绑实例数据的图表类型(数据形态 = 分组 + 计数,{name,value});其它(地图/仪表盘/排名/代码…)跳过
const EMBED_BINDABLE = ['BKBarChart', 'BKPieChart', 'BKMapChart']

// 逐点经纬度数据源(散点地图):/instance/geo-points 返回 [{name,lng,lat,value}]
function geoDataSource (classId, lngField, latField, nameField, valueField, baseQuery) {
  let qs = `classId=${encodeURIComponent(classId)}&lngField=${encodeURIComponent(lngField)}&latField=${encodeURIComponent(latField)}`
  if (nameField) qs += `&nameField=${encodeURIComponent(nameField)}`
  if (valueField) qs += `&valueField=${encodeURIComponent(valueField)}`
  if (baseQuery && baseQuery.filter) qs += `&filter=${encodeURIComponent(baseQuery.filter)}`
  if (baseQuery && baseQuery.q) qs += `&q=${encodeURIComponent(baseQuery.q)}`
  return {
    type: 'get', interfacePath: `/instance/geo-points?${qs}`, paramsType: 'json',
    interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {}
  }
}
const LNG_RE = /经度|lng|longitude/i
const LAT_RE = /纬度|lat|latitude/i
const NAME_RE = /名称|名字|标题|name|title/i

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
      const demo = calDemo()
      return {
        dataSourceConfig: { type: 'static', data: demo, value: JSON.stringify(demo), paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {}, fieldOptions },
        items: [{ label: '日期', field: 'date', value: 'date' }, { label: '数值', field: 'value', value: 'value' }]
      }
    }
    // 散点图:需 x/y 两个数值轴,实例单维聚合(类目+计数)喂不了 → 静态演示
    if (chartConfig.branchType === 'scatterChart') {
      const demo = [{ x: 10, y: 20 }, { x: 25, y: 35 }, { x: 40, y: 15 }, { x: 55, y: 48 }, { x: 70, y: 30 }, { x: 85, y: 55 }]
      return {
        dataSourceConfig: { type: 'static', data: demo, value: JSON.stringify(demo), paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {}, fieldOptions },
        items: [{ label: 'X轴', field: 'x', value: 'x' }, { label: 'Y轴', field: 'y', value: 'y' }]
      }
    }
    // 漏斗图 / 热力图(funnelChart/heatmapChart)不特判 → 走下方默认绑定(维度→x, 计数→y),
    // renderFunnel/renderHeatmap 直接读 x/y(/value), 与看板数据一致
    // 桑基/矩形树/旭日/关系/主题河流/箱线/分段仪表:rawEChart 直通 + 内置 demo(结构特殊,实例聚合喂不了) → 不绑定
    // 凹凸图/嵌套环:结构特殊(时间×系列 / 两级分组), 单维聚合喂不了 → 内置 demo 不绑定;玫瑰/瀑布走下方默认绑定(x/y)
    if (['rainfallEvap', 'sankeyChart', 'treemapChart', 'sunburstChart', 'graphChart', 'themeRiverChart', 'boxplotChart', 'gradeGaugeChart', 'parallelChart', 'pictorialBarChart', 'candlestickChart', 'treeChart', 'bumpChart', 'nestPieChart', 'confidenceBandChart', 'speedGaugeChart', 'stageGaugeChart', 'tempGaugeChart', 'ringGaugeChart', 'barometerGaugeChart', 'multiGaugeChart', 'gradientStackAreaChart', 'rainfallFlowChart', 'timeAxisLineChart', 'rainfallRunoffChart'].includes(chartConfig.branchType)) return null
    // 地图:仅散点地图(scatterMap)绑逐点经纬度;需对象类型有经度/纬度字段,否则保持静态演示
    if (chartConfig.type === 'BKMapChart') {
      if (chartConfig.branchType !== 'scatterMap') return null
      const lngCol = (columns || []).find(c => LNG_RE.test((c.label || '') + ' ' + (c.field || '')))
      const latCol = (columns || []).find(c => LAT_RE.test((c.label || '') + ' ' + (c.field || '')))
      if (!lngCol || !latCol) return null
      const nameCol = (columns || []).find(c => NAME_RE.test((c.label || '') + ' ' + (c.field || '')))
      // 数值字段(排除经纬度)→ 散点大小随其值变化
      const valCol = (columns || []).find(c => ['int', 'decimal'].includes(c.dataType) && c.field !== lngCol.field && c.field !== latCol.field)
      const gds = geoDataSource(classId, lngCol.field, latCol.field, nameCol ? nameCol.field : '', valCol ? valCol.field : '', filterParams)
      gds.fieldOptions = fieldOptions
      return {
        dataSourceConfig: gds,
        items: [
          { label: '名称', field: 'name', value: 'name' },
          { label: '数值', field: 'value', value: 'value' },
          { label: '经度', field: 'lng', value: 'lng' },
          { label: '纬度', field: 'lat', value: 'lat' }
        ]
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
