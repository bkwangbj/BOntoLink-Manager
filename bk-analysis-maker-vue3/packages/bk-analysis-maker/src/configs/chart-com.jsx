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
    img: 'bar.png',
    chartComId: '39',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }]
  }, {
    title: '气泡图',
    type: 'BKBarChart',
    branchType: 'bubbleChart',
    img: 'bar.png',
    chartComId: '40',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
    items: [{ label: 'x轴', field: 'x', value: 'x' }, { label: 'y轴', field: 'y', value: 'y' }, { label: '气泡大小', field: 'size', value: 'size' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }],
    dataSourceConfig: { type: 'static', data: [{ x: 5, y: 20, size: 30, colorField: 'A' }, { x: 12, y: 35, size: 60, colorField: 'A' }, { x: 20, y: 15, size: 45, colorField: 'B' }, { x: 28, y: 42, size: 80, colorField: 'B' }, { x: 35, y: 25, size: 20, colorField: 'C' }, { x: 42, y: 38, size: 55, colorField: 'C' }], value: '[{"x":5,"y":20,"size":30},{"x":12,"y":35,"size":60},{"x":20,"y":15,"size":45},{"x":28,"y":42,"size":80},{"x":35,"y":25,"size":20},{"x":42,"y":38,"size":55}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
  }, {
    title: '日历热力图',
    type: 'BKBarChart',
    branchType: 'calendarHeatmap',
    img: 'bar.png',
    chartComId: '41',
    explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
    eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'date', varField: '' }, { field: 'value', varField: '' }] }],
    items: [{ label: '日期', field: 'date', value: 'date' }, { label: '数值', field: 'value', value: 'value' }],
    dataSourceConfig: { type: 'static', data: [{ date: '2026-01-05', value: 12 }, { date: '2026-01-18', value: 30 }, { date: '2026-02-03', value: 8 }, { date: '2026-02-20', value: 45 }, { date: '2026-03-11', value: 22 }, { date: '2026-04-06', value: 60 }, { date: '2026-05-15', value: 18 }, { date: '2026-06-09', value: 37 }, { date: '2026-07-01', value: 52 }], value: '[{"date":"2026-01-05","value":12},{"date":"2026-02-20","value":45},{"date":"2026-04-06","value":60}]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} }
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
    img: 'radius-bar.png',
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
    img: 'bar.png',
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
    img: 'stack-bar.png',
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
    img: 'bar.png',
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
  img: 'pie.png',
  explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0 },
  chartComId: '37',
  branchType: 'roundPieChart',
  eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }, { field: 'value', varField: '' }] }],
  items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }]
}, {
  title: '圆角环图',
  type: 'BKPieChart',
  img: 'ring-pie.png',
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
const req = import.meta.glob('../analysis-maker/src/components/toolbar/images/*.png', { eager: true })
// const req = require.context('../analysis-maker/src/components/toolbar/images', false, /\.png$/)
const path = Object.keys(req).map(key => req[key])
const imgObject = {}
const keys = Object.keys(req)
for (let i = 0; i < keys.length; i++) {
  imgObject[keys[i].split('/').pop()] = path[i].default
}
export { chartComponents, mapComponents, ringComponents, customComponents, allComponents, tableComponents, imgObject, decorateComponents }
