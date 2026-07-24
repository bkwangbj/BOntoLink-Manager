// 日历热力图演示数据:2026 全年每 2~3 天一个点、值 5~95(确定性生成,色块明显)
const CAL_DEMO = (() => {
  const out = []
  const pad = (n) => String(n).padStart(2, '0')
  const days = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  let seed = 7
  const rnd = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280 }
  for (let m = 1; m <= 12; m++) {
    for (let d = 1; d <= days[m - 1]; d += 2 + Math.floor(rnd() * 2)) {
      out.push({ date: `2026-${pad(m)}-${pad(d)}`, value: 5 + Math.floor(rnd() * 90) })
    }
  }
  return out
})()

const chartComponents = [
  {
    title: '柱状图',
    type: 'BKBarChart',
    branchType: 'barChart',
    img: 'bar.png',
    chartComId: '2',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'x', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'y', varField: '' }]
      }],
    items: [
      { label: 'x', field: 'x', value: 'x' },
      { label: 'y', field: 'y', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
  }, {
    title: '正负柱图',
    type: 'BKBarChart',
    branchType: 'posNegBarChart',
    img: 'bar-posneg.svg',
    chartComId: '39',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '气泡图',
    type: 'BKBarChart',
    branchType: 'bubbleChart',
    img: 'bubble.svg',
    chartComId: '40',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x轴', field: 'x', value: 'x' }, { label: 'y轴', field: 'y', value: 'y' }, { label: '气泡大小', field: 'size', value: 'size' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }],
    dataSourceConfig: { type: 'static', data: [{ x: 5, y: 20, size: 30, colorField: 'A' }, { x: 12, y: 35, size: 60, colorField: 'A' }, { x: 20, y: 15, size: 45, colorField: 'B' }, { x: 28, y: 42, size: 80, colorField: 'B' }, { x: 35, y: 25, size: 20, colorField: 'C' }, { x: 42, y: 38, size: 55, colorField: 'C' }], value: '[{"x":5,"y":20,"size":30},{"x":12,"y":35,"size":60},{"x":20,"y":15,"size":45},{"x":28,"y":42,"size":80},{"x":35,"y":25,"size":20},{"x":42,"y":38,"size":55}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '日历热力图',
    type: 'BKBarChart',
    branchType: 'calendarHeatmap',
    img: 'calendar-heatmap.svg',
    chartComId: '41',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'date', varField: '' }, { field: 'value', varField: '' }] }],
    items: [{ label: '日期', field: 'date', value: 'date' }, { label: '数值', field: 'value', value: 'value' }],
    dataSourceConfig: { type: 'static', data: CAL_DEMO, value: JSON.stringify(CAL_DEMO.slice(0, 3)), paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '散点图',
    type: 'BKBarChart',
    branchType: 'scatterChart',
    img: 'scatter.svg',
    chartComId: '44',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'X轴', field: 'x', value: 'x' }, { label: 'Y轴', field: 'y', value: 'y' }, { label: '分组', field: 'colorField', value: 'colorField' }],
    dataSourceConfig: { type: 'static', data: [{ x: 10, y: 20 }, { x: 25, y: 35 }, { x: 40, y: 15 }, { x: 55, y: 48 }, { x: 70, y: 30 }, { x: 85, y: 55 }], value: '[{"x":10,"y":20},{"x":25,"y":35},{"x":40,"y":15}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '漏斗图',
    type: 'BKBarChart',
    branchType: 'funnelChart',
    img: 'funnel.svg',
    chartComId: '45',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: '名称', field: 'x', value: 'x' }, { label: '数值', field: 'y', value: 'y' }],
    dataSourceConfig: { type: 'static', data: [{ x: '展现', y: 100 }, { x: '点击', y: 80 }, { x: '访问', y: 60 }, { x: '咨询', y: 40 }, { x: '订单', y: 20 }], value: '[{"x":"展现","y":100},{"x":"点击","y":80}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '热力图',
    type: 'BKBarChart',
    branchType: 'heatmapChart',
    img: 'heatmap.svg',
    chartComId: '46',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'X轴', field: 'x', value: 'x' }, { label: 'Y轴', field: 'y', value: 'y' }, { label: '数值', field: 'value', value: 'value' }],
    dataSourceConfig: { type: 'static', data: [{ x: '周一', y: 'A区', value: 12 }, { x: '周二', y: 'A区', value: 30 }, { x: '周三', y: 'A区', value: 55 }, { x: '周四', y: 'A区', value: 8 }, { x: '周五', y: 'A区', value: 40 }, { x: '周一', y: 'B区', value: 60 }, { x: '周二', y: 'B区', value: 22 }, { x: '周三', y: 'B区', value: 75 }, { x: '周四', y: 'B区', value: 48 }, { x: '周五', y: 'B区', value: 18 }, { x: '周一', y: 'C区', value: 33 }, { x: '周二', y: 'C区', value: 90 }, { x: '周三', y: 'C区', value: 15 }, { x: '周四', y: 'C区', value: 66 }, { x: '周五', y: 'C区', value: 28 }], value: '[{"x":"周一","y":"A区","value":12}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '桑基图',
    type: 'BKBarChart',
    branchType: 'sankeyChart',
    img: 'sankey.svg',
    chartComId: '47',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '矩形树图',
    type: 'BKBarChart',
    branchType: 'treemapChart',
    img: 'treemap.svg',
    chartComId: '48',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '旭日图',
    type: 'BKBarChart',
    branchType: 'sunburstChart',
    img: 'sunburst.svg',
    chartComId: '49',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '关系图',
    type: 'BKBarChart',
    branchType: 'graphChart',
    img: 'graph.svg',
    chartComId: '50',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '主题河流',
    type: 'BKBarChart',
    branchType: 'themeRiverChart',
    img: 'themeriver.svg',
    chartComId: '51',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '箱线图',
    type: 'BKBarChart',
    branchType: 'boxplotChart',
    img: 'boxplot.svg',
    chartComId: '52',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '分段仪表盘',
    type: 'BKBarChart',
    branchType: 'gradeGaugeChart',
    img: 'grade-gauge.svg',
    chartComId: '53',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '平行坐标',
    type: 'BKBarChart',
    branchType: 'parallelChart',
    img: 'parallel.svg',
    chartComId: '54',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '象形柱图',
    type: 'BKBarChart',
    branchType: 'pictorialBarChart',
    img: 'pictorialbar.svg',
    chartComId: '55',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: 'K线图',
    type: 'BKBarChart',
    branchType: 'candlestickChart',
    img: 'candlestick.svg',
    chartComId: '56',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '树图',
    type: 'BKBarChart',
    branchType: 'treeChart',
    img: 'tree.svg',
    chartComId: '57',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '玫瑰图',
    type: 'BKBarChart',
    branchType: 'roseChart',
    img: 'rose.svg',
    chartComId: '58',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: '名称', field: 'x', value: 'x' }, { label: '数值', field: 'y', value: 'y' }],
    dataSourceConfig: { type: 'static', data: [{ x: '测站A', y: 45 }, { x: '测站B', y: 62 }, { x: '测站C', y: 38 }, { x: '测站D', y: 70 }, { x: '测站E', y: 55 }], value: '[{"x":"测站A","y":45}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '瀑布图',
    type: 'BKBarChart',
    branchType: 'waterfallChart',
    img: 'waterfall.svg',
    chartComId: '59',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: '类目', field: 'x', value: 'x' }, { label: '增量', field: 'y', value: 'y' }],
    dataSourceConfig: { type: 'static', data: [{ x: '期初', y: 100 }, { x: '1月', y: 20 }, { x: '2月', y: -15 }, { x: '3月', y: 30 }, { x: '4月', y: -10 }, { x: '5月', y: 25 }], value: '[{"x":"期初","y":100}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '凹凸图',
    type: 'BKBarChart',
    branchType: 'bumpChart',
    img: 'bump.svg',
    chartComId: '60',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '嵌套环形图',
    type: 'BKBarChart',
    branchType: 'nestPieChart',
    img: 'nestpie.svg',
    chartComId: '61',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '涟漪散点',
    type: 'BKBarChart',
    branchType: 'effectScatterChart',
    img: 'effectscatter.svg',
    chartComId: '62',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: '类目', field: 'x', value: 'x' }, { label: '数值', field: 'y', value: 'y' }],
    dataSourceConfig: { type: 'static', data: [{ x: '测站A', y: 30 }, { x: '测站B', y: 68 }, { x: '测站C', y: 45 }, { x: '测站D', y: 90 }, { x: '测站E', y: 52 }], value: '[{"x":"测站A","y":30}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '渐变面积',
    type: 'BKBarChart',
    branchType: 'gradientAreaChart',
    img: 'gradientarea.svg',
    chartComId: '63',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: '类目', field: 'x', value: 'x' }, { label: '数值', field: 'y', value: 'y' }],
    dataSourceConfig: { type: 'static', data: [{ x: '1月', y: 42 }, { x: '2月', y: 55 }, { x: '3月', y: 48 }, { x: '4月', y: 70 }, { x: '5月', y: 62 }, { x: '6月', y: 85 }], value: '[{"x":"1月","y":42}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '阈值分段折线',
    type: 'BKBarChart',
    branchType: 'thresholdAreaChart',
    img: 'threshold.svg',
    chartComId: '64',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: '类目', field: 'x', value: 'x' }, { label: '数值', field: 'y', value: 'y' }],
    dataSourceConfig: { type: 'static', data: [{ x: '周一', y: 50 }, { x: '周二', y: 65 }, { x: '周三', y: 78 }, { x: '周四', y: 88 }, { x: '周五', y: 95 }, { x: '周六', y: 82 }, { x: '周日', y: 70 }], value: '[{"x":"周一","y":50}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '置信带',
    type: 'BKBarChart',
    branchType: 'confidenceBandChart',
    img: 'confidence.svg',
    chartComId: '65',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }] }],
    items: [],
    dataSourceConfig: { type: 'static', data: [], value: '[]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '折线图',
    type: 'BKBarChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'line.png',
    branchType: 'lineChart',
    chartComId: '7',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'x', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'y', varField: '' }]
      }],
    items: [
      { label: 'x', field: 'x', value: 'x' },
      { label: 'y', field: 'y', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
  }, {
    title: '平滑折线图',
    type: 'BKBarChart',
    branchType: 'smoothLineChart',
    img: 'smooth-line.png',
    chartComId: '31',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '标点折线',
    type: 'BKBarChart',
    branchType: 'markLineChart',
    img: 'mark-line.svg',
    chartComId: '25',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '面积图',
    type: 'BKBarChart',
    branchType: 'areaChart',
    img: 'area.png',
    chartComId: '32',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '堆叠面积图',
    type: 'BKBarChart',
    branchType: 'stackAreaChart',
    img: 'stack-area.png',
    chartComId: '33',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '阶梯折线图',
    type: 'BKBarChart',
    branchType: 'stepLineChart',
    img: 'step-line.png',
    chartComId: '34',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '横向条形图',
    type: 'BKBarChart',
    branchType: 'barHorizontalChart',
    img: 'bar-horizontal.png',
    chartComId: '35',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '雨量蒸发量关系图',
    type: 'BKBarChart',
    branchType: 'rainfallEvap',
    img: 'yuliang.png',
    chartComId: '36',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '圆角柱状图',
    type: 'BKBarChart',
    branchType: 'barRadiusChart',
    img: 'bar-radius.svg',
    chartComId: '22',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'x', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'y', varField: '' }]
      }],
    items: [
      { label: 'x', field: 'x', value: 'x' },
      { label: 'y', field: 'y', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
  }, {
    title: '渐变柱状图',
    type: 'BKBarChart',
    branchType: 'barLineargradientChart',
    img: 'bar-gradient.svg',
    chartComId: '23',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'x', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'y', varField: '' }]
      }],
    items: [
      { label: 'x', field: 'x', value: 'x' },
      { label: 'y', field: 'y', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
  }, {
    title: '折柱混合图',
    type: 'BKBarChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    branchType: 'mixChart',
    img: 'line-bar.png',
    chartComId: '8',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'x', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'y', varField: '' }]
      }],
    items: [
      { label: 'x', field: 'x', value: 'x' },
      { label: 'y', field: 'y', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
  }, {
    title: '堆叠柱状图',
    type: 'BKBarChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    branchType: 'stackedChart',
    img: 'bar-stacked.svg',
    chartComId: '9',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'x', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'y', varField: '' }]
      }],
    items: [
      { label: 'x', field: 'x', value: 'x' },
      { label: 'y', field: 'y', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
  }, {
    title: '风玫瑰图',
    type: 'BKPolarChart',
    branchType: 'polarChart',
    img: 'wind-rose.svg',
    chartComId: '24',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'x', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'y', varField: '' }]
      }],
    items: [
      { label: 'x', field: 'x', value: 'x' },
      { label: 'y', field: 'y', value: 'y' },
      { label: '颜色映射', field: 'colorField', value: 'colorField' }
    ]
  }
]
const ringComponents = [{
  title: '饼状图',
  type: 'BKPieChart',
  img: 'pie.png',
  explainConfig: {
    show: false,
    text: '',
    position: 'topLeft',
    x: 0,
    y: 0
  },
  chartComId: '3',
  branchType: 'pieChart',
  eventConfig: [
    {
      title: '当数据项被点击时',
      event: 'click',
      isActive: false,
      items: [
        { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
        { field: 'value', varField: '' }]
    }],
  items: [
    { label: 'name', field: 'name', value: 'name' },
    { label: 'value', field: 'value', value: 'value' }
  ]
}, {
  title: '圆角饼图',
  type: 'BKPieChart',
  img: 'round-pie.svg',
  explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
  chartComId: '37',
  branchType: 'roundPieChart',
  eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }, { field: 'value', varField: '' }] }],
  items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }]
}, {
  title: '圆角环图',
  type: 'BKPieChart',
  img: 'round-ring.svg',
  explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
  chartComId: '38',
  branchType: 'roundRingChart',
  eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }, { field: 'value', varField: '' }] }],
  items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }]
}, {
  title: '圆环图',
  type: 'BKPieChart',
  img: 'ring-pie.png',
  explainConfig: {
    show: false,
    text: '',
    position: 'topLeft',
    x: 0,
    y: 0
  },
  chartComId: '10',
  branchType: 'ringChart',
  eventConfig: [
    {
      title: '当数据项被点击时',
      event: 'click',
      isActive: false,
      items: [
        { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
        { field: 'value', varField: '' }]
    }],
  items: [
    { label: 'name', field: 'name', value: 'name' },
    { label: 'value', field: 'value', value: 'value' }
  ]
}, {
  title: '辉光圆环图',
  type: 'BKPieChart',
  img: 'light-pie.png',
  explainConfig: {
    show: false,
    text: '',
    position: 'topLeft',
    x: 0,
    y: 0
  },
  chartComId: '18',
  branchType: 'ringChart2',
  eventConfig: [
    {
      title: '当数据项被点击时',
      event: 'click',
      isActive: false,
      items: [
        { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
        { field: 'value', varField: '' }]
    }],
  items: [
    { label: 'name', field: 'name', value: 'name' },
    { label: 'value', field: 'value', value: 'value' }
  ]
}, {
  title: '进度圆环图',
  type: 'BKPieChart',
  img: 'progress.png',
  explainConfig: {
    show: false,
    text: '',
    position: 'topLeft',
    x: 0,
    y: 0
  },
  chartComId: '13',
  branchType: 'progressChart',
  items: [
    { label: 'name', field: 'name', value: 'name' },
    { label: 'value', field: 'value', value: 'value' }
  ],
  eventConfig: [
    {
      title: '当数据项被点击时',
      event: 'click',
      isActive: false,
      items: [
        { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
        { field: 'value', varField: '' }]
    }]
}, {
  title: '多个饼图',
  type: 'BKPieChart',
  explainConfig: {
    show: false,
    text: '',
    position: 'topLeft',
    x: 0,
    y: 0
  },
  img: 'multiple-pie.png',
  chartComId: '11',
  branchType: 'multiplePieChart',
  eventConfig: [
    {
      title: '当数据项被点击时',
      event: 'click',
      isActive: false,
      items: [
        { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
        { field: 'value', varField: '' }]
    }],
  items: [
    { label: 'name', field: 'name', value: 'name' },
    { label: 'value', field: 'value', value: 'value' },
    { label: '颜色映射', field: 'colorField', value: 'colorField' }
  ]
}, {
  title: '3D饼图',
  type: 'BKPieChart',
  explainConfig: {
    show: false,
    text: '',
    position: 'topLeft',
    x: 0,
    y: 0
  },
  img: '3d-pie.png',
  chartComId: '17',
  branchType: '3dPieChart',
  eventConfig: [
    {
      title: '当数据项被点击时',
      event: 'click',
      isActive: false,
      items: [
        { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
        { field: 'value', varField: '' }]
    }],
  items: [
    { label: 'name', field: 'name', value: 'name' },
    { label: 'value', field: 'value', value: 'value' }
  ]
}, {
  title: '轮播饼图',
  type: 'BKPieChart',
  img: 'carousel-pie.svg',
  explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
  chartComId: '42',
  branchType: 'carouselPieChart',
  eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }, { field: 'value', varField: '' }] }],
  items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }]
}, {
  title: '花瓣饼图',
  type: 'BKPieChart',
  img: 'petal-pie.svg',
  explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
  chartComId: '43',
  branchType: 'petalPieChart',
  eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }, { field: 'value', varField: '' }] }],
  items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }]
}]
const mapComponents = [
  {
    title: '地图',
    type: 'BKMapChart',
    branchType: 'heatMap',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'map.png',
    chartComId: '4',
    mapName: '100000',
    isDeep: false,
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'value', varField: '' }]
      }],
    items: [
      { label: 'name', field: 'name', value: 'name' },
      { label: 'value', field: 'value', value: 'value' },
      { label: 'code', field: 'code', value: 'code' }
    ]
  }, {
    title: '散点地图',
    type: 'BKMapChart',
    branchType: 'scatterMap',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'scatter-map.png',
    chartComId: '19',
    mapName: '100000',
    isDeep: false,
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'value', varField: '' }]
      }],
    items: [
      { label: 'name', field: 'name', value: 'name' },
      { label: 'value', field: 'value', value: 'value' },
      { label: 'code', field: 'code', value: 'code' }
    ]
  }, {
    title: '立体地图',
    type: 'BKMapChart',
    branchType: 'solidtMap',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: '3d-map.png',
    chartComId: '20',
    mapName: '100000',
    isDeep: false,
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'value', varField: '' }]
      }],
    items: [
      { label: 'name', field: 'name', value: 'name' },
      { label: 'value', field: 'value', value: 'value' },
      { label: 'code', field: 'code', value: 'code' }
    ]
  }
]

const tableComponents = [
  {
    title: '表格',
    type: 'BKTableChart',
    branchType: 'commonTable',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'table.png',
    chartComId: '1',
    eventConfig: [
      {
        title: '当单元格被点击时',
        event: 'cellClick',
        isActive: false,
        items: []
      }
    ]
  }, {
    title: '统计表格',
    type: 'BKTableChart',
    branchType: 'tjbTable',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    items: [
      { label: 'xlabel', field: 'xlabel', value: 'xlabel' },
      { label: 'ylabel', field: 'ylabel', value: 'ylabel' },
      { label: 'value', field: 'value', value: 'value' }
    ],
    img: 'statis-table.png',
    chartComId: '12',
    eventConfig: [
      {
        title: '当单元格被点击时',
        event: 'cellClick',
        isActive: false,
        items: []
      }
    ]
  }
]
const customComponents = [
  {
    title: '统计信息',
    type: 'BKStatisticsChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'statistics.png',
    chartComId: '6',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'text', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'number', varField: '' }]
      }],
    items: [
      { label: '数字', field: 'number', value: 'number' },
      { label: '文本', field: 'text', value: 'text' },
      { label: '单位', field: 'unit', value: 'unit' },
      { label: '图片', field: 'url', value: 'url' }
    ]
  }, {
    title: '时间轴',
    type: 'BKTimeChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'time.png',
    chartComId: '16',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'description', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'value', varField: '' }]
      }],
    items: [
      { label: '描述', field: 'description', value: 'description' },
      { label: '值', field: 'value', value: 'value' }

    ]
  },
  {
    title: '排行榜',
    type: 'BKRankChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'rank-list.png',
    chartComId: '14',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'number', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'name', varField: '' }]
      }],
    items: [
      { label: '数字', field: 'number', value: 'number' },
      { label: '文本', field: 'name', value: 'name' }
    ]
  }, {
    title: '雷达图',
    type: 'BKRadarChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'radar.png',
    chartComId: '21',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'r', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 't', varField: '' }]
      }],
    items: [
      { label: 'r', field: 'r', value: 'r' },
      { label: 't', field: 't', value: 't' },
      { label: 'colorField', field: 'colorField', value: 'colorField' }
    ]
  }, {
    title: '仪表盘',
    type: 'BKGaugeChart',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    img: 'guage.png',
    chartComId: '15',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: [
          { field: 'name', varField: '' }, //  field 值字段   varField 更新值变量
          { field: 'value', varField: '' }]
      }],
    items: [
      { label: 'name', field: 'name', value: 'name' },
      { label: 'value', field: 'value', value: 'value' }

    ]
  },
  {
    title: 'HTML',
    img: 'html.png',
    explainConfig: {
      show: false,
      text: '',
      position: 'topLeft',
      x: 0,
      y: 0
    },
    type: 'BKCodeChart',
    eventConfig: [
      {
        title: '当数据项被点击时',
        event: 'click',
        isActive: false,
        items: []
      }],
    chartComId: '5',
    isBasic: true
  } // isBasic 是否是基础配置
]

const decorateComponents = [
  {
    title: '装饰1',
    type: 'decorate',
    configs: { content: () => { return <el-icon><StarFilled /></el-icon> } }
  }
]

const allComponents = chartComponents.concat(mapComponents, customComponents, ringComponents, tableComponents)
const req = import.meta.glob('../analysis-maker/src/components/toolbar/images/*.{png,svg}', { eager: true })
// const req = require.context('../analysis-maker/src/components/toolbar/images', false, /\.png$/)
const path = Object.keys(req).map(key => req[key])
const imgObject = {}
const keys = Object.keys(req)
for (let i = 0; i < keys.length; i++) {
  imgObject[keys[i].split('/').pop()] = path[i].default
}
export { chartComponents, mapComponents, ringComponents, customComponents, allComponents, tableComponents, imgObject, decorateComponents }
