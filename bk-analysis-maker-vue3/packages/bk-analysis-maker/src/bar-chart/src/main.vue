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
      // 正负柱图:渲染时对每个数据点按正负着色(正=绿,负=红),写在 data[].itemStyle,不经序列化
      const isPosNeg = config.branchType === 'posNegBarChart'
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
            return { value: Number(v), itemStyle: { color: Number(v) < 0 ? '#F56C6C' : '#3ED848' } }
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
