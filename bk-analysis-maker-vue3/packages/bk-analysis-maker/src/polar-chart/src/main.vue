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
  name: 'PolarChart',
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
      if (option.polar?.radius) {
        option.polar.radius[0] = option.polar.radius[0] + '%'
        option.polar.radius[1] = option.polar.radius[1] + '%'
      } if (option.polar.center) {
        option.polar.center[0] = option.polar.center[0] + '%'
        option.polar.center[1] = option.polar.center[1] + '%'
      }
      if (option.angleAxis.axisLabel.dataType) {
        if (option.angleAxis.type === 'value' || option.angleAxis.type === 'time') {
          option.angleAxis.axisLabel.formatter = getDataTypeFormat(option.angleAxis.axisLabel.dataType, option.angleAxis.type)
        }
      }
      if (option.color) {
        option.color = option.color.map(ele => {
          return convertCssColorToEChartsColor(ele)
        })
      }
      option.angleAxis.type = option.angleAxis.type === 'time' ? 'category' : option.angleAxis.type

      if (option.radiusAxis.axisLabel.dataType) {
        if (option.radiusAxis.type === 'value' || option.radiusAxis.type === 'time') {
          option.radiusAxis.axisLabel.formatter = getDataTypeFormat(option.radiusAxis.axisLabel.dataType, option.radiusAxis.type)
        }
      }

      let xData = this.relList.map(item => item.x).filter(ele => { return ele })
      xData = Array.from(new Set(xData))
      option.angleAxis.data = xData
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
        option.series[i].stack = 'A'
        option.series[i].data = yData
        if (option.series[i]?.itemStyle?.color) {
          const color = convertCssColorToEChartsColor(option.series[i].itemStyle.color)
          option.series[i].itemStyle.color = color
        }
        // series.push({ ...utils.deepClone(seriesConfig[i]), data: yData })
      }

      //  const yAxisNames = seriesConfig.filter(c => c.measureName).map(item => { return item.measureName })
      // let x = 'x'
      // if (config.dataSourceConfig.dataMapping && config.dataSourceConfig.dataMapping.x) {
      //   x = config.dataSourceConfig.dataMapping.x
      // }

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
        seriesData.push({ type: 'bar', color: colorList[index], name: series[i], id: series[i], coordinateSystem: 'polar' })
      }
      return seriesData
    }
  }
}
</script>
