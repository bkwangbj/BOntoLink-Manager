import { chartDefaultConfig, getParametricEquation } from './chart-default-config'
import { getChartTheme, chartThemeInit } from './pre-page-setting'
import XEUtils from 'xe-utils'
// import dayjs from './utils/dayjs.min'
import { format } from 'd3-format'
import { componentConfigs } from '../configs'
import { utils } from 'efficient-suite'
const typeChartData = {
  BKBarChart: [{
    colorField: '第一周',
    x: '活动1',
    y: 1
  },
  {
    colorField: '第二周',
    x: '活动1',
    y: 4
  },
  {
    colorField: '第一周',
    x: '活动2',
    y: 2
  },
  {
    colorField: '第二周',
    x: '活动2',
    y: 3
  },
  {
    colorField: '第一周',
    x: '活动3',
    y: 3
  },
  {
    colorField: '第二周',
    x: '活动3',
    y: 2
  },
  {
    colorField: '第一周',
    x: '活动4',
    y: 4
  },
  {
    colorField: '第二周',
    x: '活动4',
    y: 4
  }],
  BKPolarChart: [{
    colorField: '第一周',
    x: '活动1',
    y: 1
  },
  {
    colorField: '第二周',
    x: '活动1',
    y: 4
  },
  {
    colorField: '第三周',
    x: '活动1',
    y: 3
  },
  {
    colorField: '第一周',
    x: '活动2',
    y: 2
  },
  {
    colorField: '第二周',
    x: '活动2',
    y: 3
  },
  {
    colorField: '第三周',
    x: '活动2',
    y: 1
  },
  {
    colorField: '第一周',
    x: '活动3',
    y: 3
  },
  {
    colorField: '第二周',
    x: '活动3',
    y: 2
  },
  {
    colorField: '第三周',
    x: '活动3',
    y: 4
  },
  {
    colorField: '第一周',
    x: '活动4',
    y: 4
  },
  {
    colorField: '第二周',
    x: '活动4',
    y: 4
  }, {
    colorField: '第三周',
    x: '活动4',
    y: 4
  }],
  BKPieChart: [
    { name: '男', value: 11 },
    { name: '女', value: 5 }],
  multiplePieChart: [
    { name: '男', value: 11, colorField: '1' },
    { name: '女', value: 5, colorField: '1' },
    { name: '工人', value: 3, colorField: '2' },
    { name: '儿童', value: 7, colorField: '2' }],
  BKMapChart: [{ name: '北京市', value: Math.round(Math.random() * 500), code: 110000 },
    { name: '天津市', value: Math.round(Math.random() * 500), code: 120000 },
    { name: '上海市', value: Math.round(Math.random() * 500) },
    { name: '重庆市', value: Math.round(Math.random() * 500) },
    { name: '河北', value: Math.round(Math.random() * 500) },
    { name: '河南', value: Math.round(Math.random() * 500) },
    { name: '云南', value: Math.round(Math.random() * 500) },
    { name: '辽宁', value: Math.round(Math.random() * 500) },
    { name: '黑龙江', value: Math.round(Math.random() * 500) },
    { name: '湖南', value: Math.round(Math.random() * 500) },
    { name: '安徽', value: Math.round(Math.random() * 500) },
    { name: '山东', value: Math.round(Math.random() * 500) },
    { name: '新疆', value: Math.round(Math.random() * 500) },
    { name: '江苏', value: Math.round(Math.random() * 500) },
    { name: '浙江', value: Math.round(Math.random() * 500) },
    { name: '江西', value: Math.round(Math.random() * 500) },
    { name: '湖北', value: Math.round(Math.random() * 500) },
    { name: '广西', value: Math.round(Math.random() * 500) },
    { name: '甘肃', value: Math.round(Math.random() * 500) },
    { name: '山西', value: Math.round(Math.random() * 500) },
    { name: '内蒙古', value: Math.round(Math.random() * 500) },
    { name: '陕西', value: Math.round(Math.random() * 500) },
    { name: '吉林', value: Math.round(Math.random() * 500) },
    { name: '福建', value: Math.round(Math.random() * 500) },
    { name: '贵州', value: Math.round(Math.random() * 500) },
    { name: '广东', value: Math.round(Math.random() * 500) },
    { name: '青海', value: Math.round(Math.random() * 500) },
    { name: '西藏', value: Math.round(Math.random() * 500) },
    { name: '四川', value: Math.round(Math.random() * 500) },
    { name: '宁夏', value: Math.round(Math.random() * 500) },
    { name: '海南', value: Math.round(Math.random() * 500) },
    { name: '台湾', value: Math.round(Math.random() * 500) },
    { name: '香港', value: Math.round(Math.random() * 500) },
    { name: '澳门', value: Math.round(Math.random() * 500) }],
  commonTable: [{ name: '姓名', value: 11 },
    { name: '姓名', value: 5 }],
  tjbTable: [{ ylabel: '水利部', xlabel: '新开工项目个数（个）', value: '' },
    { ylabel: '  长委', xlabel: '新开工项目个数（个）', value: '2.00' },
    { ylabel: '  黄委', xlabel: '新开工项目个数（个）', value: '' },
    { ylabel: '  淮委', xlabel: '新开工项目个数（个）', value: '16.00' },
    { ylabel: '  海委', xlabel: '新开工项目个数（个）', value: '' },
    { ylabel: '  珠委', xlabel: '新开工项目个数（个）', value: '1.00' },
    { ylabel: '  松辽委', xlabel: '新开工项目个数（个）', value: '7.00' },
    { ylabel: '  太湖局', xlabel: '新开工项目个数（个）', value: '6.00' }],
  BKStatisticsChart:
  [
    { id: 1, text: '未复审', number: 5, unit: '(个)', url: 'https://picx.zhimg.com/70/v2-76ba10f470b1af65abe28b99f09b5fa0_1440w.avis?source=172ae18b&biz_tag=Post' },
    { id: 2, text: '复审通过', number: 2, unit: '(个)', url: 'https://picx.zhimg.com/70/v2-76ba10f470b1af65abe28b99f09b5fa0_1440w.avis?source=172ae18b&biz_tag=Post' },
    { id: 3, text: '复审不通过', number: 3, unit: '(个)', url: 'https://picx.zhimg.com/70/v2-76ba10f470b1af65abe28b99f09b5fa0_1440w.avis?source=172ae18b&biz_tag=Post' },
    { id: 4, text: '递补', number: 1, unit: '(个)', url: 'https://picx.zhimg.com/70/v2-76ba10f470b1af65abe28b99f09b5fa0_1440w.avis?source=172ae18b&biz_tag=Post' }],
  BKCodeChart: {
    name: '测试',
    num: 3
  },
  BKRankChart: [{ name: '项目1', number: 14947.7 }, { name: '项目2', number: 3330.9 },
    { name: '项目3', number: 3330 }, { name: '项目4', number: 3290.5 }, { name: '项目5', number: 1542.4 },
    { name: '项目6', number: 1276 }, { name: '项目7', number: 1228 }, { name: '项目8', number: 859.5 }],
  progressChart: {
    name: '任务完成度', value: '70'
  },
  BKGaugeChart: {
    name: '任务完成度', value: '70'
  },
  BKTimeChart: [{ value: '2016-12-12', description: '第一年' }, { value: '2017-6-12', description: '第二年' }, { value: '2018-12-12', description: '第三年' }],
  BKRadarChart: [{
    t: '维度A',
    r: 320,
    colorField: '角色1'
  },
  {
    t: '维度B',
    r: 200,
    colorField: '角色1'
  },
  {
    t: '维度C',
    r: 150,
    colorField: '角色1'
  },
  {
    t: '维度D',
    r: 300,
    colorField: '角色1'
  },
  {
    t: '维度E',
    r: 280,
    colorField: '角色1'
  },
  {
    t: '维度F',
    r: 360,
    colorField: '角色1'
  },
  {
    t: '维度A',
    r: 200,
    colorField: '角色2'
  },
  {
    t: '维度B',
    r: 300,
    colorField: '角色2'
  },
  {
    t: '维度C',
    r: 420,
    colorField: '角色2'
  },
  {
    t: '维度D',
    r: 400,
    colorField: '角色2'
  },
  {
    t: '维度E',
    r: 200,
    colorField: '角色2'
  },
  {
    t: '维度F',
    r: 100,
    colorField: '角色2'
  },
  {
    t: '维度A',
    r: 520,
    colorField: '角色3'
  },
  {
    t: '维度B',
    r: 200,
    colorField: '角色3'
  },
  {
    t: '维度C',
    r: 100,
    colorField: '角色3'
  },
  {
    t: '维度D',
    r: 500,
    colorField: '角色3'
  },
  {
    t: '维度E',
    r: 500,
    colorField: '角色3'
  },
  {
    t: '维度F',
    r: 150,
    colorField: '角色3'
  }
  ]
}

function getInitValue (value) {
  if (value && value.startsWith('$store')) {
    const res = componentConfigs.getStore(value)
    // const props = value.split('.')
    // let res = window.$store
    // for (let i = 0; i < props.length; i++) {
    //   if (props[i] === '$store') {
    //     continue
    //   }
    //   res = res[props[i]]
    // }
    return res
  } else {
    try {
      const str = JSON.parse(value)
      if (Array.isArray(str)) {
        const list = []
        for (const i of str) {
          if (typeof i === 'string' && i.includes('dayFormat')) {
            const result = executeScript('return ' + i)

            list.push(result)
          } else {
            list.push(i)
          }
        }
        return list
      }
    } catch {
      if (value && value.includes('dayFormat')) {
        const result = executeScript('return ' + value)
        return result
      }
    }
  }
  return value || ''
}

function executeScript (code) {
  const func = new Function('dayFormat', code)
  const value = func(utils, code)
  return value
}

const timerObj = {}
function getCalState (tableId, calId, configId) {
  return new Promise((resolve) => {
    componentConfigs.request.post('/ywtt/jcfx/dwfx/getcalstate', { tableid: tableId, calid: calId }).then((res) => {
      if (res.state === 'end') {
        clearTimer(configId)
        resolve(true)
      } else {
        const timer = setTimeout(() => {
          if (!timerObj[configId]) {
            resolve(false)
          } else {
            getCalState(tableId, calId, configId).then((res) => {
              resolve(res)
            })
          }
        }, 1000)
        timerObj[configId] = timer
      }
    })
  })
}

function clearTimer (configId) {
  if (timerObj[configId]) {
    clearTimeout(timerObj[configId])
    delete timerObj[configId]
  }
}

function getVarAndEvent (config, varConfig) {
  return { varListener: getVar(config.varListener || [], varConfig), eventConfig: getEvent(config.eventConfig || [], varConfig) }
}

function getVar (configVarListener, varConfig) {
  const data = varConfig.filter(c => c.changeType === 'refreshData')
  const defalutVar = data.filter(c => c.isDefault === '1' && c.isAdd)
  const varListener = utils.deepClone(configVarListener)
  for (let i = 0; i < defalutVar.length; i++) {
    const index = varListener.findIndex(c => c.id === defalutVar[i].id)
    if (index === -1) {
      varListener.push({ ...defalutVar[i], alias: defalutVar[i].name, operator: 'eq', isShowQuery: '0' })
    } else {
      varListener[index] = { ...defalutVar[i], ...varListener[index] }
    }
  }
  const changeVar = data.filter(c => c.isDefault === '0' && c.isRemove && !c.isAdd)
  for (let i = 0; i < changeVar.length; i++) {
    const index = varListener.findIndex(c => c.id === changeVar[i].id)
    if (index !== -1) {
      varListener.splice(index, 1)
    }
  }
  const relVal = []
  for (let i = 0; i < varListener.length; i++) {
    const configV = data.find(c => c.id === varListener[i].id)
    if (configV) {
      relVal.push({ ...varListener[i], ...configV, alias: varListener[i].name === varListener[i].alias ? configV.name : varListener[i].alias })
    }
  }
  return relVal
}
const chartItemList = ['BKBarChart', 'BKPieChart', 'BKRadarChart', 'BKPolarChart']

async function getDefaultOption (tab, data, theme) {
  const option = XEUtils.clone(chartDefaultConfig.get(tab?.branchType || tab.type))
  const explainConfig = tab.explainConfig
  // rawEChart 原始 option 图(桑基/树图/旭日/关系图等):整套 option 已内置,跳过 series 重建与 themeInit 合并
  // (themeInit 会往 series 展开 lineStyle,可能覆盖 type;这里只套用看板配色)
  if (option.rawEChart) {
    const chartTheme = getChartTheme(theme, tab.type, tab.branchType)
    const boardColors0 = theme?.themeStyle?.colorList
    // keepColor 图(分段仪表盘)保留自带默认配色(红黄绿语义段),不套用看板通用色;其余 rawEChart 图跟随看板
    if (!option.keepColor && Array.isArray(boardColors0) && boardColors0.length) option.color = XEUtils.clone(boardColors0, true)
    // 坐标轴跟随主题(基础配置):箱线图直角轴 / 主题河流 singleAxis 的文字/轴线/分隔线色随深浅主题, 避免深色模式下轴不自适应
    const tc = chartTheme.textColor; const alc = chartTheme.axisLineColor; const slc = chartTheme.splitLineColor; const fs = chartTheme.fontSize
    const applyAxis = (ax) => {
      if (!ax || typeof ax !== 'object') return
      if (tc) { ax.axisLabel = { ...(ax.axisLabel || {}), color: tc, fontSize: fs }; ax.nameTextStyle = { ...(ax.nameTextStyle || {}), color: tc, fontSize: fs } }
      if (alc) ax.axisLine = { ...(ax.axisLine || {}), lineStyle: { ...((ax.axisLine && ax.axisLine.lineStyle) || {}), color: alc } }
      if (slc && ax.splitLine && ax.splitLine.show !== false) ax.splitLine = { ...ax.splitLine, lineStyle: { ...((ax.splitLine && ax.splitLine.lineStyle) || {}), color: slc } }
    }
    ;[].concat(option.xAxis || [], option.yAxis || [], option.singleAxis || []).forEach(applyAxis)
    // 图例文字跟随主题
    if (option.legend && tc) option.legend = { ...option.legend, textStyle: { ...(option.legend.textStyle || {}), color: tc } }
    // series 节点标签显式套色, 防止被全局 textStyle 顶成不可见色(桑基/关系图白底→主题文字色;树图/旭日压色块→白字)
    const onFillLabel = tab.branchType === 'treemapChart' || tab.branchType === 'sunburstChart'
    if (Array.isArray(option.series)) {
      option.series.forEach(s => {
        if (s && s.label && s.label.show !== false) s.label = { ...s.label, color: onFillLabel ? '#fff' : (tc || s.label.color) }
      })
    }
    explainConfig.textStyle = { color: chartTheme.textColor, fontSize: chartTheme.fontSize }
    return { configOption: option, chartTheme, explainConfig }
  }
  // 读取系列
  if (tab.branchType === '3dPieChart') {
    const { series, format } = get3dPieChartSeries(data, option?.internalDiameterRatio || 0.5)
    option.tooltip.formatter = format
    option.series = series
  } else if (chartItemList.includes(tab?.type)) {
    getChartSeries(tab, data, option.series)
  } else if (tab.type === 'BKTableChart') {
    option.columns = await getColumns(data, tab)
  } else if (tab.type === 'BKMapChart') {
    const { max, min } = getInterval(data)
    option.visualMap.max = max
    option.visualMap.min = min
  }
  // 读取主题
  const { configOption, chartTheme } = getConfigTheme(option, theme, tab.type, tab.branchType)
  // 新增图表:默认整体取用看板当前选定的图表配色模版(themeStyle.colorList),与看板保持一致。
  // 用整体替换而非按索引合并(getConfigTheme 内是 merge,默认色比 palette 长时会残留旧色)。
  // 仅作用于以 color 数组取色的图表类型;用户后续可在图表配置里单独改色。
  const boardColors = theme?.themeStyle?.colorList
  if (Array.isArray(boardColors) && boardColors.length && chartItemList.includes(tab.type)) {
    configOption.color = XEUtils.clone(boardColors, true)
  }
  explainConfig.textStyle = { color: chartTheme.textColor, fontSize: chartTheme.fontSize }

  return { configOption, chartTheme, explainConfig }
}
function getInterval (data) {
  const list = data.map(ele => {
    let value = ele.value
    if (isNaN(value)) {
      try {
        value = Number(value.replace(/,/g, ''))
      } catch (error) {

      }
    }
    return value
  })
  const max = formatInt(Math.max(...list))
  const min = formatInt(Math.min(...list), false)
  return { max, min }
}
function formatInt (num, ceil = true) {
  // num：数值；prec：向上取整多少位，默认为2位；ceil：true-向上，false-向下；
  let prec = 1
  if (Math.abs(num) > 100) {
    prec = String(Math.ceil(num)).length - 1
  } else if (Math.abs(num) < 100) {
    prec = 1
  } else if (Math.abs(num) < 1) {
    return num
  }
  const len = String(num).length
  if (len <= prec) { return num }
  const mult = Math.pow(10, prec)
  return ceil ? Math.ceil(num / mult) * mult : Math.floor(num / mult) * mult
}

// 读取主题
function getConfigTheme (option = {}, theme, type, branchType) {
  const defaultStyle = getChartTheme(theme, type, branchType)
  let form = {}
  if (type === 'BKRankChart') {
    form = { styles: defaultStyle }
  } else if (chartItemList.includes(type) || ['BKMapChart', 'BKTableChart', 'BKGaugeChart', 'BKTimeChart', 'BKStatisticsChart'].includes(type)) {
    const initTheme = chartThemeInit.get(branchType) || chartThemeInit.get(type)
    form = initTheme(option, defaultStyle, branchType)
  }

  option = XEUtils.merge(option, form)
  return { configOption: option, chartTheme: defaultStyle }
}

function getEvent (eventConfigs, varConfig) {
  let eventConfig = []
  if (eventConfigs) {
    eventConfig = utils.deepClone(eventConfigs)
    for (let j = 0; j < eventConfig.length; j++) {
      const items = eventConfig[j].items || []
      for (let n = 0; n < items.length; n++) {
        if (items[n].varField) {
          if (!varConfig.find(c => c.id === items[n].varField && c.type === 'internal')) {
            items[n].varField = ''
          }
        }
      }
    }
  }
  return eventConfig
}
// 生成图表系列
function getChartSeries (tab, data, seriesList, key = 'colorField') {
  const newList = []
  if (tab.branchType === 'multiplePieChart' || tab.type !== 'BKPieChart') {
    let series = data.map(item => {
      return item[key]
    })
    series = Array.from(new Set(series))
    for (let i = 0; i < series.length; i++) {
      newList.push({ name: series[i], dataId: series[i] })
    }

    newList.forEach((ele, index) => {
      if (seriesList[index]) {
        seriesList[index].name = newList[index].name
        seriesList[index].dataId = newList[index].dataId
      } else {
        const item = {}

        if (tab.type === 'BKPieChart') {
          item.type = 'pie'
        } else if (tab.type === 'BKBarChart') {
          if (tab.type === 'BKBarChart' && tab.branchType === 'lineChart') {
            item.type = 'line'
          } else {
            item.type = 'bar'
          }
        } else if (tab.type === 'BKRadarChart') {
          item.type = 'radar'
        } else if (tab.type === 'BKPolarChart') {
          item.type = 'bar'
          item.coordinateSystem = 'polar'
        }
        seriesList.push({ ...ele, ...item })
      }
    })
  }
}
// 生成表格列
async function getColumns (data, tab, key = 'xlabel', dataSourceConfig) {
  let fieldList = []
  const type = dataSourceConfig?.type || 'static'
  let columnsList = []

  if (tab?.branchType === 'tjbTable') {
    const columns = []
    const kind = []
    data.forEach((item) => {
      if (!kind.includes(item[key])) {
        kind.push(item[key])
      }
    })
    for (let i = 0; i < kind.length; i++) {
      columns.push({ field: kind[i], title: kind[i], width: null, align: 'center' })
    }
    columns.unshift({ field: 'ylabel', title: '', width: null, align: 'center' })
    columnsList = columns
  } else {
    if (type === 'watf') {
      const params = JSON.parse(dataSourceConfig.interfaceTempParams)
      if (params.viewCode) {
        const { columns } = await componentConfigs.request.post('/view/getColumnsData', {
          appId: params.appId,
          bzCode: params.bzCode,
          viewCode: params.viewCode
        })
        columnsList = columns
      } else {
        const { columns } = await componentConfigs.request.post('/grid/getColumnsData', {
          appId: params.appId,
          bzCode: params.bzCode,
          metaSet: params.metaSet
        })
        columnsList = columns
      }
    } else {
      const columns = []
      for (let i = 0; i < data.length; i++) {
        const keys = Object.keys(data[i])
        fieldList = [...fieldList, ...keys]
      }
      fieldList = Array.from(new Set(fieldList))
      for (let i = 0; i < fieldList.length; i++) {
        columns.push({ field: fieldList[i], title: '列' + (i + 1), width: null, align: 'center' })
      }
      columnsList = columns
    }
  }
  return columnsList
}
// 生成3d饼图系列
function get3dPieChartSeries (data, internalDiameterRatio) {
  const total = data.map(item => item.value).reduce((a, b) => a + b, 0)
  const pieData = data.map((r, i) => ({
    name: r.name,
    value: r.value * 3 / total,
    raw: r.value,
    percentage: Math.round(r.value * 100 / total),
    color: ''
  }))
  const series = []
  let sumValue = 0
  let startValue = 0
  let endValue = 0
  const legendData = []
  const k = typeof internalDiameterRatio !== 'undefined' ? (1 - internalDiameterRatio) / (1 + internalDiameterRatio) : 1 / 3

  // 为每一个饼图数据，生成一个 series-surface 配置
  for (let i = 0; i < pieData.length; i++) {
    sumValue += pieData[i].value
    const seriesItem = {
      name: typeof pieData[i].name === 'undefined' ? `series${i}` : pieData[i].name,
      type: 'surface',
      parametric: true,
      wireframe: {
        show: false
      },
      pieData: pieData[i],
      pieStatus: {
        selected: false,
        hovered: false,
        k
      }
    }
    seriesItem.itemStyle = {
      opacity: 0.8,
      color: pieData[i].color
    }
    series.push(seriesItem)
  }
  // 使用上一次遍历时，计算出的数据和 sumValue，调用 getParametricEquation 函数，
  // 向每个 series-surface 传入不同的参数方程 series-surface.parametricEquation，也就是实现每一个扇形。
  for (let i = 0; i < series.length; i++) {
    endValue = startValue + series[i].pieData.value
    series[i].pieData.startRatio = startValue / sumValue
    series[i].pieData.endRatio = endValue / sumValue
    series[i].parametricEquation = getParametricEquation(series[i].pieData.startRatio, series[i].pieData.endRatio, false, false, k, series[i].pieData.value)

    startValue = endValue

    legendData.push(series[i].name)
  }
  const format = function (params) {
    if (params.seriesName !== 'mouseoutSeries') {
      const pieData = series[params.seriesIndex].pieData
      return `${params.seriesName}<br/><span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${params.color};"></span>${pieData.raw} 占比 ${pieData.percentage}%`
    }
  }
  return { series, format }
}
function getDefaultData (tab) {
  return typeChartData[tab.branchType] || typeChartData[tab.type] || []
}
// 初始化组件配置
async function getDefaultConfig (tab, theme) {
  const data = getDefaultData(tab)
  const { configOption, chartTheme, explainConfig } = await getDefaultOption(tab, data, theme)
  const base = { dataSourceConfig: { type: 'static', data }, configOption, chartTheme, explainConfig, hookId: tab.type.replace('BK', '') + utils.createDate().format('YYYYMMDDHHmmss') }
  // 宿主注入:新增图表默认绑定真实数据源(替代静态默认),(chartConfig) => { dataSourceConfig, items, autoSeries } | null
  try {
    const factory = componentConfigs.embedDefaultDataSource
    if (factory) {
      const patch = factory(tab)
      if (patch && patch.dataSourceConfig) {
        base.dataSourceConfig = patch.dataSourceConfig
        if (patch.items) base.items = patch.items
        if (patch.autoSeries && base.configOption) base.configOption.autoSeries = true
      }
    }
  } catch (e) { console.warn('[maker] embedDefaultDataSource failed', e) }
  return base
}
// 位置组件转换函数
function getPositionStyle (position) {
  const form = {}
  if (position.x) {
    switch (position.x) {
      case 'left': {
        form.left = '0'
        break
      }
      case 'center': {
        form.left = '50%'
        form.transform = 'translateX(-50%)'
        break
      }
      case 'right': {
        form.right = '0'
        break
      }
    }
  }

  if (position.y) {
    switch (position.y) {
      case 'top': {
        form.top = '0'
        break
      }
      case 'center': {
        form.top = '50%'
        form.transform = 'translateY(-50%)'
        break
      }
      case 'bottom': {
        form.bottom = '0'
        break
      }
    }
  }
  if (position.x === 'center' && position.y === 'center') {
    form.transform = 'translate(-50%,-50%)'
  }
  if (position.left !== undefined) {
    form['--x'] = position.left
  }
  if (position.top !== undefined) {
    form['--y'] = position.top
  }
  return form
}
function getTextStyle (style) {
  const text = {}
  for (const i in style) {
    if (i === 'fontSize') {
      text[i] = style[i] + 'px'
    } else {
      text[i] = style[i]
    }
  }
  return text
}
function getTjbMapping (mapping, items) {
  if (!mapping) {
    mapping = {}
  }
  for (let i = 0; i < items.length; i++) {
    if (items[i].field === 'colorField') {
      mapping[items[i].field] = 'ylabel'
    } else if (items[i].field === 'x' || items[i].field === 'name') {
      mapping[items[i].field] = 'xlabel'
    } else if (items[i].field === 'y' || items[i].field === 'value') {
      mapping[items[i].field] = 'value'
    } else if (items[i].field === 'code') {
      mapping[items[i].field] = 'yid'
    }
  }
  return mapping
}
// 统计表饼图分组规则
function getCollapseRule (rule) {
  let ruleList = rule.split(',')
  ruleList = ruleList.map(ele => {
    if (ele.startsWith('-')) {
      const rule = /^-?\d+$/
      if (rule.test(ele)) {
        return ele.replace('-', 'index<=')
      }
    } else if (ele.endsWith('-')) {
      const rule = /^\d+-?$/
      if (rule.test(ele)) {
        return ele.replace('-', '<=index')
      }
    } else if (ele.includes('-')) {
      const rule = /^\d+-?\d+$/
      if (rule.test(ele)) {
        return ele.replace('-', '<=index||') + '>=index'
      }
    } else {
      const rule = /^\d+$/
      if (rule.test(ele)) {
        return 'index===' + ele
      }
    }
    return ''
  }).filter(ele => ele).join('||')
  return ruleList
}
// 数据格式化
function getDataTypeFormat (type, sourceType = 'value') {
  if (sourceType === 'time') {
    if (type) {
      return function (params) {
        return utils.createDate(params).format(type)
      }
    }
  } else if (sourceType === 'value') {
    if (type) {
      return function (params) {
        params = Number(params)
        return format(type)(params)
      }
    }
  }
  return function (params) {
    return params
  }
}
// css转echarts渐变
function convertCssColorToEChartsColor (cssColor) {
  // 已是 echarts 渐变对象(非字符串)则原样返回,避免 .match 报错
  if (typeof cssColor !== 'string') return cssColor
  const matches = cssColor.match(/linear-gradient\((.*)\)/)
  // 不是渐变色直接返回
  if (!matches) return cssColor

  const parts = matches[1].split('deg,')

  // 将角度转换成坐标
  const angle = +parts[0]
  const radian = (angle * Math.PI) / 180
  const x = Math.cos(radian)
  const y = Math.sin(radian)
  const x2 = Math.cos(radian + Math.PI)
  const y2 = Math.sin(radian + Math.PI)

  // 颜色转换处理
  const colors = parts[1].split('%,')
  const colorStops = []
  for (let i = 0; i < colors.length; i++) {
    const splitIndex = colors[i].lastIndexOf(' ')

    const color = colors[i].slice(0, splitIndex)
    let percent = colors[i].slice(splitIndex + 1)
    if (percent.endsWith('%')) percent = colors[i].slice(splitIndex + 1, -1)

    colorStops.push({
      offset: +percent / 100,
      color
    })
  }

  return {
    type: 'linear',
    x,
    y,
    x2,
    y2,
    colorStops
  }
}

function hasPermission (permissions, value) {
  // 超级管理员
  if (permissions && permissions.includes('*.*')) return true
  // 无权限码控制默认显示
  if (!value) return true
  // 接受字符串或者数组形式的权限码配置
  const permissionCodeList = Array.isArray(value) ? value : [value]
  if (permissionCodeList.length) {
    return permissions && permissions.some(r => permissionCodeList.includes(r))
  } else {
    // 配置为空数组时，任何人都有权访问
    return true
  }
}

export {
  chartItemList, convertCssColorToEChartsColor, getVarAndEvent, getVar, getEvent, getDefaultData, getDefaultConfig, getConfigTheme, getChartSeries, getTjbMapping, getCollapseRule, getColumns,
  getInterval, getDataTypeFormat, get3dPieChartSeries, getInitValue, getCalState, clearTimer, getPositionStyle, getTextStyle, hasPermission
}
