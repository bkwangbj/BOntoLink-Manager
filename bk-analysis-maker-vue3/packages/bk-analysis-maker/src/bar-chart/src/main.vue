<template>
  <div
    ref="rootRef"
    class="full-box"
    style="position: relative;"
  >
    <slot name="content">
      <component
        :is="contentConfig.component"
        v-if="contentConfig && contentConfig.component"
        v-bind="contentConfig.props || {}"
      />
    </slot>
    <BKChart
      ref="chart"
      :option="option"
      @click="itemClick"
    />
    <a
      ref="downLoadPic"
      href="#"
      style="display: none;"
    />
  </div>
</template>
<script>
import { mixins } from '../../configs/commom-chart'
import { colorList } from '../../configs/chart-cfg'
import { getDataTypeFormat, convertCssColorToEChartsColor } from '../../configs/common-func'
import { utils } from 'efficient-suite'
export default {
  name: 'BarChart',
  components: {

  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {

  },
  data () {
    return {
      option: {},
      defaultData: [],
      isDesc: false
    }
  },
  watch: {
    relList () {
      this.debouncedCustomResetChart(this.configs)
    }
  },
  created () {
  },
  mounted () {
  },
  methods: {
    itemClick (event) {
      const series = this.option.series.find(c => c.name === event.seriesName)
      if (series) {
        const data = this.relList.filter(c => c.colorField === series.dataId)
        this.handleEvent(data[event.dataIndex], 'click')
      }
    },
    sortChart () {
      const option = utils.deepClone(this.option)
      let list = []
      if (this.option?.xAxis?.data) {
        list = this.option?.xAxis?.data.map(ele => { return { colorField: ele, data: [] } })
        this.option.series.forEach((ele) => {
          if (ele.data.length) {
            for (let j = 0; j < ele.data.length; j++) {
              list[j].data.push(ele.data[j])
            }
          }
        })
      }
      const xData = list.sort((a, b) => {
        const aSum = a.data.reduce(function (pre, cur) {
          return Number(pre) + Number(cur)
        })
        const bSum = b.data.reduce(function (pre, cur) {
          return Number(pre) + Number(cur)
        })
        if (!this.isDesc) {
          return bSum - aSum
        } else {
          return aSum - bSum
        }
      }).map(ele => { return ele.colorField })
      for (let i = 0; i < option.series.length; i++) {
        let yData = this.relList.filter(item => item.colorField === option.series[i].dataId)
        yData = xData.map(item => {
          const dataItem = yData.find(ele => ele.x === item)
          let value = dataItem?.y
          if (isNaN(value)) {
            try {
              value = Number(dataItem?.y.replace(/,/g, ''))
            } catch (error) {

            }
          }
          return value || null
        })
        option.series[i].data = yData

        // series.push({ ...utils.deepClone(seriesConfig[i]), data: yData })
      }
      this.isDesc = !this.isDesc
      option.xAxis.data = xData
      this.option = option
      if (!this.$refs.chart) {
        return
      }
      this.$refs.chart.setOption && this.$refs.chart.setOption(option, true)
    },
    restoreChart (configs) {
      this.customResetChart(configs)
    },
    // setChartData (data) {
    //   this.option.series[0].data = data
    // },
    async customResetChart (config) {
      this.isDesc = false
      //  let seriesConfig = []
      //    const series = []
      let option = { series: [] }
      if (config.configOption) {
        option = utils.deepClone(config.configOption)
      } else {
        return
      }
      // 原始 echarts option 直通(双坐标/多 grid 等特殊结构):跳过单坐标系的 series/轴重建
      if (option.rawEChart) {
        // 非直角系图(桑基/树图/旭日/关系图等)剥离 themeInit 残留的直角坐标, 避免叠出空网格
        if (option.noGrid) {
          option.xAxis = undefined; option.yAxis = undefined; option.grid = undefined
          option.polar = undefined; option.angleAxis = undefined; option.radiusAxis = undefined
        }
        // 节点标签渲染时统一补显(老看板保存的 configOption 标签可能不可见/缺色, 每次渲染强制补上):
        // 树图/旭日标签压在色块上→白字;桑基/关系图标签在白底→主题文字色
        {
          const onFill = config.branchType === 'treemapChart' || config.branchType === 'sunburstChart'
          const tc = (config.chartTheme && config.chartTheme.textColor) || '#333'
          if (Array.isArray(option.series)) {
            option.series.forEach(s => {
              if (s && s.label && s.label.show !== false) s.label = { ...s.label, show: true, color: onFill ? '#fff' : tc }
            })
          }
        }
        // 图例位置:rawEChart 不走下方标准的 alignPosition 转换, 这里补上, 保证「图例配置」的顶部/底部×左中右生效且往返一致
        if (option.legend && option.legend.alignPosition) {
          const alignList = {
            topLeft: { left: 'left', top: 10, bottom: undefined }, topCenter: { left: 'center', top: 10, bottom: undefined }, topRight: { left: 'right', top: 10, bottom: undefined },
            bottomLeft: { left: 'left', bottom: 5, top: undefined }, bottomCenter: { left: 'center', bottom: 5, top: undefined }, bottomRight: { left: 'right', bottom: 5, top: undefined }
          }
          option.legend = { ...option.legend, ...(alignList[option.legend.alignPosition] || {}) }
        }
        // 箱线图:盒体色随「色系配置」首色(仪表盘保留语义红黄绿分段,不跟随)
        if (config.branchType === 'boxplotChart' && option.series && option.series[0] && option.color && option.color[0]) {
          const c = option.color[0]
          option.series[0].itemStyle = { ...(option.series[0].itemStyle || {}), color: c + '33', borderColor: c }
        }
        // 象形柱图:symbol 色随「色系配置」首色 + 符号尺寸自适应(圆角柱=扁平层叠, 其它图标=方形堆叠计数)
        if (config.branchType === 'pictorialBarChart' && option.series && option.series[0]) {
          const s0 = option.series[0]
          if (option.color && option.color[0]) s0.itemStyle = { ...(s0.itemStyle || {}), color: option.color[0] }
          if (s0.symbol && s0.symbol !== 'roundRect') { s0.symbolSize = [22, 22]; s0.symbolMargin = 4 } else { s0.symbolSize = ['70%', 6]; s0.symbolMargin = 3 }
        }
        // 分段仪表盘:默认红黄绿, 但 3 段刻度色跟随「色系配置」前 3 色(可自定义)
        if (config.branchType === 'gradeGaugeChart' && option.series && option.series[0] && Array.isArray(option.color) && option.color.length >= 3) {
          const [c0, c1, c2] = option.color
          const s0 = option.series[0]
          s0.axisLine = { ...(s0.axisLine || {}), lineStyle: { ...((s0.axisLine && s0.axisLine.lineStyle) || {}), width: 16, color: [[0.3, c0], [0.7, c1], [1, c2]] } }
        }
        this.option = option
        await this.$nextTick()
        if (this.$refs.chart && this.$refs.chart.setOption) this.$refs.chart.setOption(option, true)
        return
      }
      // 气泡图:直角坐标 + 散点,x/y 为数值轴,气泡大小按 size 字段映射(10~50)
      if (config.branchType === 'bubbleChart') {
        await this.renderBubble(option)
        return
      }
      // 日历热力图:日历坐标系 + heatmap,按 date 落格、value 定色深
      if (config.branchType === 'calendarHeatmap') {
        await this.renderCalendar(option)
        return
      }
      // 散点图:直角数值轴 + scatter,固定点大小(区别于气泡图)
      if (config.branchType === 'scatterChart') {
        await this.renderScatter(option)
        return
      }
      // 漏斗图:name/value → funnel
      if (config.branchType === 'funnelChart') {
        await this.renderFunnel(option)
        return
      }
      // 直角热力图:x/y 为类目轴,value 定色深
      if (config.branchType === 'heatmapChart') {
        await this.renderHeatmap(option)
        return
      }
      // 配置特殊处理
      if (option.legend.alignPosition) {
        const alignList = {
          topLeft: { left: 'left', top: 10, bottom: undefined },
          topCenter: { left: 'center', top: 10, bottom: undefined },
          topRight: { left: 'right', top: 10, bottom: undefined },
          bottomLeft: { left: 'left', bottom: 5, top: undefined },
          bottomCenter: { left: 'center', bottom: 5, top: undefined },
          bottomRight: { left: 'right', bottom: 5, top: undefined }
        }
        if (option.legend.left) {
          option.legend.left = option.legend.left + '%'
          option.legend.top = option.legend.top + '%'
        }
        if (option.legend.top) {
          option.legend.top = option.legend.top + '%'
        }
        option.legend = { ...option.legend, ...option.legend.alignPosition ? alignList[option.legend.alignPosition] : null }
      }
      if (option.xAxis.axisLabel.dataType) {
        if (option.xAxis.type === 'value' || option.xAxis.type === 'time') {
          option.xAxis.axisLabel.formatter = getDataTypeFormat(option.xAxis.axisLabel.dataType, option.xAxis.type)
        }
      }
      if (option.color) {
        option.color = option.color.map(ele => {
          return convertCssColorToEChartsColor(ele)
        })
      }
      option.xAxis.type = option.xAxis.type === 'time' ? 'category' : option.xAxis.type
      if (!Array.isArray(option.yAxis)) {
        if (option.yAxis.axisLabel.dataType) {
          if (option.yAxis.type === 'value' || option.yAxis.type === 'time') {
            option.yAxis.axisLabel.formatter = getDataTypeFormat(option.yAxis.axisLabel.dataType, option.yAxis.type)
          }
        }
      } else {
        option.yAxis.forEach(yAxis => {
          if (yAxis.axisLabel.dataType) {
            if (yAxis.type === 'value' || yAxis.type === 'time') {
              yAxis.axisLabel.formatter = getDataTypeFormat(yAxis.axisLabel.dataType, yAxis.type)
            }
          }
        })
      }

      // Y轴单位随聚合类型:计数→「次」,求和/平均等→留空;仅当用户未自定义轴名(默认"次"/空)时生效,不覆盖自定义
      const _agg = config.dataSourceConfig?.metrics?.[0]?.aggs?.[0]
      if (_agg) {
        const _unit = _agg === 'count' ? '次' : ''
        const _applyUnit = (ax) => { if (ax && (ax.name === '次' || ax.name === '' || ax.name == null)) ax.name = _unit }
        if (Array.isArray(option.yAxis)) { _applyUnit(option.yAxis[0]) } else { _applyUnit(option.yAxis) }
      }

      let xData = this.relList.map(item => item.x).filter(ele => { return ele })
      xData = Array.from(new Set(xData))
      option.xAxis.data = xData
      const legendData = []
      if (option.autoSeries) {
        option.series = this.addSeris(option)
      }
      // 正负柱图:渲染时对每个数据点按正负着色,颜色取色系配置前两色(正=color[0],负=color[1]),可自定义
      const isPosNeg = config.branchType === 'posNegBarChart'
      const posColor = (isPosNeg && option.color && option.color[0]) || '#3ED848'
      const negColor = (isPosNeg && option.color && option.color[1]) || '#F56C6C'
      for (let i = 0; i < option.series.length; i++) {
        let yData = this.relList.filter(item => item.colorField === option.series[i].dataId)
        yData = xData.map(item => {
          const dataItem = yData.find(ele => ele.x === item)
          let value = dataItem?.y
          if (isNaN(value)) {
            try {
              value = Number(dataItem?.y.replace(/,/g, ''))
              if (isNaN(value)) {
                value = dataItem?.y
              }
            } catch (error) {

            }
          }
          const v = value || null
          if (isPosNeg && v != null && !isNaN(Number(v))) {
            return { value: Number(v), itemStyle: { color: Number(v) < 0 ? negColor : posColor } }
          }
          return v
        })
        if (option.series[i]?.legendConfig && option.series[i]?.legendConfig.show) {
          legendData.push({
            name: option.series[i].name,
            icon: option.series[i]?.legendConfig.icon
          })
        } else {
          legendData.push({ name: option.series[i].name })
        }
        if (option.series.length <= 1) {
          option.series[i].yAxisIndex = null
        }
        option.series[i].data = yData
        if (option.series[i]?.itemStyle?.color) {
          const color = convertCssColorToEChartsColor(option.series[i].itemStyle.color)
          option.series[i].itemStyle.color = color
        }
        if (option?.borderRadius && option?.showBorderRadius) {
          option.series[i].itemStyle.borderRadius = option.borderRadius
        }
        // series.push({ ...utils.deepClone(seriesConfig[i]), data: yData })
      }
      option.legend.data = legendData
      // 正负柱图:图例改成「正值(绿)/负值(红)」两项,与柱子按符号着色对应(加两个空系列承载图例)
      if (isPosNeg) {
        option.series.push(
          { type: 'bar', name: '正值', data: [], itemStyle: { color: posColor }, tooltip: { show: false }, silent: true },
          { type: 'bar', name: '负值', data: [], itemStyle: { color: negColor }, tooltip: { show: false }, silent: true }
        )
        option.legend.data = [{ name: '正值' }, { name: '负值' }]
        option.legend.show = true
      }
      //  const yAxisNames = seriesConfig.filter(c => c.measureName).map(item => { return item.measureName })
      // let x = 'x'
      // if (config.dataSourceConfig.dataMapping && config.dataSourceConfig.dataMapping.x) {
      //   x = config.dataSourceConfig.dataMapping.x
      // }

      if (option?.isTransposition) {
        const positionMap = new Map([['left', 'bottom'], ['right', 'top'], ['bottom', 'left'], ['top', 'right']])
        const xAxis = utils.deepClone(option.xAxis)
        const yAxis = utils.deepClone(option.yAxis)
        if (!Array.isArray(yAxis)) {
          xAxis.position = positionMap.get(xAxis.position)
          yAxis.position = positionMap.get(yAxis.position)
          option.xAxis = yAxis
          option.yAxis = xAxis
        } else {
          yAxis.forEach(ele => {
            ele.position = positionMap.get(ele.position)
          })

          option.xAxis = yAxis
          option.yAxis = xAxis
          option.series.forEach(ele => {
            ele.xAxisIndex = ele.yAxisIndex
            ele.yAxisIndex = null
          })
        }
      }
      // const data = { series, xAxis: xData }
      //  if (yAxisNames.length > 0) {
      //    data.yAxisNames = yAxisNames
      //    for (let i = 0; i < series.length; i++) {
      //     const index = yAxisNames.indexOf(series[i].measureName)
      //     if (index !== -1) {
      //      series[i].yAxisIndex = index
      //    }
      //   }
      //   }
      if (option.filterDataEmpty) {
        option.series = option.series.filter(ele => {
          return ele.data.some(dataItem => {
            return (Number(dataItem) && dataItem) || (isNaN(dataItem && dataItem))
          }
          )
        })
      }
      this.setCustomOption(option, config)
      this.option = option
      await this.$nextTick()
      if (!this.$refs.chart) {
        return
      }
      this.$refs.chart.setOption && this.$refs.chart.setOption(option, true)
      // if (Object.keys(this.option).length === 0) {
      // } else {
      //   this.$refs.chart.setOption(option, true)
      // }
    },
    // 气泡图渲染:relList 每项 { x, y, size, colorField } → 散点 [x, y, size],气泡大小按 size 线性映射
    async renderBubble (option) {
      const pts = (this.relList || []).map(it => {
        const x = Number(it.x); const y = Number(it.y)
        const size = (it.size === '' || it.size == null) ? null : Number(it.size)
        return { value: [x, y, (size == null || isNaN(size)) ? null : size], name: it.colorField || it.name || '' }
      }).filter(p => !isNaN(p.value[0]) && !isNaN(p.value[1]))
      const ss = pts.map(p => p.value[2]).filter(v => v != null && !isNaN(v))
      const minS = ss.length ? Math.min(...ss) : 0
      const maxS = ss.length ? Math.max(...ss) : 1
      const symbolSize = (val) => {
        const s = Array.isArray(val) ? val[2] : null
        if (s == null || isNaN(s) || maxS === minS) return 22
        return 12 + ((s - minS) / (maxS - minS)) * 42 // 12~54
      }
      const color = (option.color && option.color[0]) || '#00E4BF'
      option.series = [{ type: 'scatter', symbolSize, data: pts, itemStyle: { color, opacity: 0.75, borderColor: '#fff', borderWidth: 1 } }]
      const toValueAxis = (ax) => { if (ax) { ax.type = 'value'; ax.data = undefined; ax.scale = true } }
      toValueAxis(option.xAxis)
      if (Array.isArray(option.yAxis)) toValueAxis(option.yAxis[0]); else toValueAxis(option.yAxis)
      option.tooltip = { trigger: 'item', formatter: (p) => `${p.name ? p.name + '<br/>' : ''}X: ${p.value[0]}<br/>Y: ${p.value[1]}` + (p.value[2] != null ? `<br/>大小: ${p.value[2]}` : '') }
      this.option = option
      await this.$nextTick()
      if (this.$refs.chart && this.$refs.chart.setOption) this.$refs.chart.setOption(option, true)
    },
    async renderCalendar (option) {
      // relList: { date: 'YYYY-MM-DD', value }。落到日历坐标系,颜色深浅按 value
      const pts = (this.relList || []).map(it => {
        const d = String(it.date || '').trim()
        const v = (it.value === '' || it.value == null) ? null : Number(it.value)
        return [d, (v == null || isNaN(v)) ? 0 : v]
      }).filter(p => /^\d{4}-\d{1,2}-\d{1,2}$/.test(p[0]))
      const vals = pts.map(p => p[1])
      const maxV = vals.length ? Math.max(...vals) : 100
      // range 自动取数据所在年份(取最早一条),无数据回退默认
      const year = pts.length ? pts[0][0].slice(0, 4) : (option.calendar && option.calendar.range) || '2026'
      if (!option.calendar) option.calendar = { top: 45, bottom: 55, left: 55, right: 25, cellSize: 'auto', itemStyle: { borderWidth: 1, borderColor: '#fff', color: '#F2F3F5' }, yearLabel: { show: false }, dayLabel: { firstDay: 1, margin: 10, nameMap: ['日', '一', '二', '三', '四', '五', '六'] }, monthLabel: { show: true, nameMap: 'cn' } }
      option.calendar.range = year
      if (!option.visualMap) option.visualMap = { show: true, orient: 'horizontal', left: 'center', bottom: 0, inRange: { color: ['#E8F3FF', '#4080FF', '#0E42D2'] } }
      option.visualMap.min = 0
      option.visualMap.max = maxV || 100
      // 日历坐标系不用直角轴/图例
      option.xAxis = undefined; option.yAxis = undefined; option.grid = undefined
      option.series = [{ type: 'heatmap', coordinateSystem: 'calendar', data: pts }]
      option.tooltip = { trigger: 'item', formatter: (p) => `${p.value[0]}<br/>数值: ${p.value[1]}` }
      this.option = option
      await this.$nextTick()
      if (this.$refs.chart && this.$refs.chart.setOption) this.$refs.chart.setOption(option, true)
    },
    // 散点图:relList { x, y[, colorField] } → 直角散点,点大小固定
    async renderScatter (option) {
      const pts = (this.relList || []).map(it => {
        const x = Number(it.x); const y = Number(it.y)
        return { value: [x, y], name: it.colorField || it.name || '' }
      }).filter(p => !isNaN(p.value[0]) && !isNaN(p.value[1]))
      const color = (option.color && option.color[0]) || '#00E4BF'
      const size = (option.series && option.series[0] && option.series[0].symbolSize) || 14
      option.series = [{ type: 'scatter', symbolSize: size, data: pts, itemStyle: { color, opacity: 0.8, borderColor: '#fff', borderWidth: 1 } }]
      const toValueAxis = (ax) => { if (ax) { ax.type = 'value'; ax.data = undefined; ax.scale = true } }
      toValueAxis(option.xAxis)
      if (Array.isArray(option.yAxis)) toValueAxis(option.yAxis[0]); else toValueAxis(option.yAxis)
      option.tooltip = { trigger: 'item', formatter: (p) => `${p.name ? p.name + '<br/>' : ''}X: ${p.value[0]}<br/>Y: ${p.value[1]}` }
      this.option = option
      await this.$nextTick()
      if (this.$refs.chart && this.$refs.chart.setOption) this.$refs.chart.setOption(option, true)
    },
    // 漏斗图:relList { x=名称, y=数值 } → funnel data
    async renderFunnel (option) {
      const data = (this.relList || []).map(it => ({
        name: String(it.x ?? it.name ?? ''), value: Number(it.y ?? it.value)
      })).filter(d => d.name && !isNaN(d.value)).sort((a, b) => b.value - a.value)
      const s = (option.series && option.series[0]) || { type: 'funnel' }
      s.type = 'funnel'; s.data = data
      option.series = [s]
      option.xAxis = undefined; option.yAxis = undefined; option.grid = undefined
      this.option = option
      await this.$nextTick()
      if (this.$refs.chart && this.$refs.chart.setOption) this.$refs.chart.setOption(option, true)
    },
    // 直角热力图:relList { x, y, value } → [[xIdx, yIdx, value]],x/y 类目轴
    async renderHeatmap (option) {
      const rows = this.relList || []
      const xs = Array.from(new Set(rows.map(it => String(it.x ?? '')).filter(v => v !== '')))
      const ys = Array.from(new Set(rows.map(it => String(it.y ?? '')).filter(v => v !== '')))
      const data = rows.map(it => {
        const xi = xs.indexOf(String(it.x ?? '')); const yi = ys.indexOf(String(it.y ?? ''))
        const v = Number(it.value)
        return [xi, yi, isNaN(v) ? 0 : v]
      }).filter(d => d[0] >= 0 && d[1] >= 0)
      const vals = data.map(d => d[2]); const maxV = vals.length ? Math.max(...vals) : 100
      if (option.xAxis) { option.xAxis.type = 'category'; option.xAxis.data = xs }
      if (option.yAxis) { const yA = Array.isArray(option.yAxis) ? option.yAxis[0] : option.yAxis; yA.type = 'category'; yA.data = ys }
      // 热力图用 visualMap 上色,不吃 series 图例 → 隐藏图例;色深随「色系配置」首色构建 浅→深 单色渐变
      option.legend = { show: false }
      const lighten = (hex, amt) => {
        const h = String(hex || '').replace('#', '')
        if (h.length !== 6) return '#E8F3FF'
        const n = parseInt(h, 16); const r = (n >> 16) & 255; const g = (n >> 8) & 255; const b = n & 255
        const m = (v) => Math.round(v + (255 - v) * amt)
        return '#' + ((1 << 24) + (m(r) << 16) + (m(g) << 8) + m(b)).toString(16).slice(1)
      }
      if (option.visualMap) {
        option.visualMap.min = 0; option.visualMap.max = maxV || 100
        const c = (option.color && option.color[0]) || '#4080FF'
        option.visualMap.inRange = { color: [lighten(c, 0.86), c] }
      }
      const s = (option.series && option.series[0]) || { type: 'heatmap' }
      s.type = 'heatmap'; s.data = data
      option.series = [s]
      option.tooltip = { trigger: 'item', formatter: (p) => `${xs[p.value[0]]} / ${ys[p.value[1]]}<br/>数值: ${p.value[2]}` }
      this.option = option
      await this.$nextTick()
      if (this.$refs.chart && this.$refs.chart.setOption) this.$refs.chart.setOption(option, true)
    },
    addSeris (option) {
      const colorList = option?.color || this.pageConfig?.themeConfigs?.chartConfig?.chart?.colorList

      const newList = []
      let series = this.relList.map(item => {
        const key = 'colorField'

        return item[key]
      })

      series = Array.from(new Set(series))
      for (let i = 0; i < series.length; i++) {
        let index = i
        if (index >= colorList.length) {
          index = index % colorList.length
          if (index < 0) {
            index = 0
          }
        }
        const origin = utils.deepClone(option.series[0]) || { type: 'bar' }
        const form = { ...origin, name: series[i], dataId: series[i], data: [] }
        if (origin.type === 'bar') {
          form.itemStyle.color = colorList[index]
        } else if (origin.type === 'line') {
          form.itemStyle.color = colorList[index]
          form.lineStyle.color = colorList[index]
          form.itemStyle.borderColor = colorList[index]
          form.color = colorList[index]
        }
        form.data = []
        newList.push(form)
      }
      return newList
    },
    buildSeriesConfig () {
      let series = this.relList.map(item => {
        return item.colorField
      })
      series = Array.from(new Set(series))
      const seriesData = []
      for (let i = 0; i < series.length; i++) {
        let index = seriesData.length
        if (index >= colorList.length) {
          index = index % colorList.length
          if (index < 0) {
            index = 0
          }
        }
        seriesData.push({ type: 'bar', color: colorList[index], name: series[i], id: series[i] })
      }
      return seriesData
    }
  }
}
</script>
