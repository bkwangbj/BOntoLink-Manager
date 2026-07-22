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
    <LegendContent
      ref="legendContent"
      :rel-list="relList"
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
import { getRingOption } from '../ring'
import { getCollapseRule, getDataTypeFormat, get3dPieChartSeries } from '../../configs/common-func'
import LegendContent from '../../grid-layout-content/src/components/legend-content.vue'
import { utils } from 'efficient-suite'
// import { colorList } from '../../configs/chart-cfg'
export default {
  name: 'PieChart',
  components: {
    LegendContent
  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {

  },
  data () {
    return {
      option: {},
      defaultData: []
    }
  },
  watch: {
    relList () {
      this.debouncedCustomResetChart(this.configs)
    }
  },
  created () {
  },
  methods: {
    itemClick (params) {
      if (this.configs.branchType === '3dPieChart') {
        const series = this.option.series.find(c => c.name === params.seriesName)
        if (series) {
          const data = this.relList.filter(c => c.name === series.dataId)
          this.handleEvent(data, 'click')
        }
        // const item = this.relList.find(ele => { return ele.name === params.seriesName })
        // this.handleEvent(item, 'click')
      } else {
        if (this.configs.branchType === 'ringChart2') {
          if (params.dataIndex % 2 === 1) {
            return
          } else {
            this.handleEvent(params.data?.originData || {}, 'click')
            return
          }
        }

        this.handleEvent(params.data, 'click')
      }
    },
    customResetChart (config) {
      this.clearCarousel()
      if (!this.relList) {
        return
      }
      this.$nextTick(() => {
        if (!this.$refs.chart) {
          return
        }
        let option = {}
        /*
      let seriesConfig = []
      if (config.seriesConfig) {
        seriesConfig = config.seriesConfig
      } else {
        seriesConfig = this.buildSeriesConfig()
      }

      for (let i = 0; i < seriesConfig.length; i++) {
        let radius = seriesConfig[i].radius + ''
        if (radius.indexOf(',') !== -1) {
          radius = radius.split(',')
        }
        const tempOption = this.$utils.getPieChartOption({ ...utils.deepClone(seriesConfig[i]), radius: radius, data: this.relList })
        tempOption.series[0].left = seriesConfig[i].left
        if (i === 0) {
          option = tempOption
        } else {
          option.series.push(tempOption.series[0])
        }
      }
      option.color = config.colorConfig || colorList
*/

        if (config.configOption) {
          if (config.configOption.legendConfig && this.$refs?.legendContent) {
            this.$refs.legendContent.buildLegend(config)
          } else {
            this.$refs.legendContent.buildLegend({})
          }
          // 处理数据
          let formData = {}
          if (Array.isArray(this.relList)) {
            formData = this.relList.map(ele => {
              let value = ele.value
              if (isNaN(value)) {
                try {
                  value = Number(ele.value.replace(/,/g, ''))
                } catch (error) {

                }
              }
              return { ...ele, value }
            })
          } else {
            let value = this.relList.value
            if (isNaN(value)) {
              try {
                value = Number(this.relList.value.replace(/,/g, ''))
              } catch (error) {
              }
            }
            formData = { ...this.relList, value }
          }

          option = utils.deepClone(config.configOption)
          if (option.legend) {
            const alignList = {
              topLeft: { left: 'left', top: 5, bottom: undefined },
              topCenter: { left: 'center', top: 5, bottom: undefined },
              topRight: { left: 'right', top: 5, bottom: undefined },
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

          if (config.branchType === 'multiplePieChart') {
            const seriesList = []
            for (let i = 0; i < option.series.length; i++) {
              let yData = formData.filter(item => item.colorField === option.series[i].dataId)
              yData = yData.map(item => { return { name: item.name, value: item.value, ...item } })
              if (option.series[i]?.hasChildren) {
              // 子系列逻辑
                const ruleList = getCollapseRule(option.series[i]?.collapseRule || '')
                const childrenData = yData.filter((ele, index) => {
                  index = index + 1
                  // eslint-disable-next-line no-eval
                  return eval(ruleList)
                })
                const fatherData = yData.filter((ele, index) => {
                  index = index + 1
                  // eslint-disable-next-line no-eval
                  return !eval(ruleList)
                })

                option.series[i].data = fatherData
                seriesList.push(option.series[i])
                const item = { ...option.series[i].childSeries, data: childrenData, name: option.series[i].name, type: 'pie', isChildren: true }
                seriesList.push(item)
              } else {
                if (!option.series[i].isChildren) {
                  option.series[i].data = yData
                  seriesList.push(option.series[i])
                }
              }
            }
            option.series = seriesList
          } else if (config.branchType === 'progressChart') {
            if (option.polar?.radius) {
              option.polar.radius[0] = option.polar.radius[0] + '%'
              option.polar.radius[1] = option.polar.radius[1] + '%'
            } if (option.polar.center) {
              option.polar.center[0] = option.polar.center[0] + '%'
              option.polar.center[1] = option.polar.center[1] + '%'
            }
            let data = formData?.value
            if (option.title?.dataType) {
              const format = getDataTypeFormat(option.title?.dataType)
              data = format(data)
            }
            option.series[0].data = [formData]
            option.series[0].name = ''
            option.title.text = (data || 0) + '%'
          } else if (config.branchType === '3dPieChart') {
            const { series, format } = get3dPieChartSeries(formData, option?.internalDiameterRatio || 0.5)
            option.series = series
            option.tooltip.formatter = format
          } else if (config.branchType === 'ringChart2') {
            const color = option.color
            const ring = getRingOption({
              position: option?.position,
              // 颜色
              color,
              // 数据 [{name: '名称', value: 1 }]
              data: formData
            })
            option = { ...option, ...ring }
            option.tooltip.position = function (point, params, dom, rect, size) {
              return point
            }
            option.tooltip.formatter = function ({ data, color }) {
              return '<div style="display:flex;align-items:center;"> <div style="height:10px;width:10px;border-radius:5px;margin-right:10px;background:' + color + ';"></div>' + data.originData.name + '<div style="width:10px;"></div>' + data.originData.value + ' </div>'
            }
          } else if (config.branchType === 'carouselPieChart') {
            option.series[0].data = formData
            this._carouselLen = Array.isArray(formData) ? formData.length : 0
          } else {
            option.series[0].data = formData
            // 圆角饼图/圆角环图:强制给饼图 series 上圆角(默认配置已带,这里兜底,避免配置面板剥离)
            if (config.branchType === 'roundPieChart' || config.branchType === 'roundRingChart') {
              option.series.forEach(s => { if (s && s.type === 'pie') s.itemStyle = { ...(s.itemStyle || {}), borderRadius: 8, borderColor: '#fff', borderWidth: 2 } })
            }
            if (option.title) {
              let sum = 0
              for (let i = 0; i < formData.length; i++) {
                sum += formData[i]?.value || 0
              }

              option.title = {
                text: '{name|' + '总量' + '}\n{val|' + sum + '}{unit|' + '个' + '}',
                top: 'center',
                left: 'center',
                textStyle: {
                  rich: {
                    name: {
                      fontSize: 14,
                      fontWeight: 'normal',
                      color: '#000',
                      padding: [10, 0]
                    },
                    val: {
                      fontSize: 24,
                      fontWeight: 'bold',
                      color: '#000'
                    },
                    unit: {
                      fontSize: 14,
                      fontWeight: 'normal',
                      color: '#000'
                    }
                  }
                }
              }
            }
          }
          option.series.forEach(ele => {
            if (ele.radius) {
              ele.radius[0] = ele.radius[0] + '%'
              ele.radius[1] = ele.radius[1] + '%'
            } if (ele.center) {
              ele.center[0] = ele.center[0] + '%'
              ele.center[1] = ele.center[1] + '%'
            }
            if (ele.label?.formatter === '{c}') {
              ele.label.formatter = (ele.label.prefix || '') + '{c}' + (ele.label.suffix || '')
              if (ele.label.dataType) {
                const format = getDataTypeFormat(ele.label.dataType)
                ele.label.formatter = function ({ data }) {
                  return (ele.label.prefix || '') + format(data.value) + (ele.label.suffix || '')
                }
              }
            }
          })
          this.setCustomOption(option, config)
          this.option = option

          this.$refs.chart.setOption && this.$refs.chart.setOption(option, true)
          if (config.branchType === 'carouselPieChart' && this._carouselLen > 0) {
            this.startCarousel()
          }
        }
      })
      // if (Object.keys(this.option).length === 0) {
      // } else {
      //   this.$refs.chart.setOption(option, true)
      // }
    },
    buildSeriesConfig () {
      return [{ type: 'pie', radius: 100 }]
    },
    // 轮播饼图:定时高亮各扇区,emphasis 中心标签显示当前项占比+名称
    startCarousel () {
      this.clearCarousel()
      const len = this._carouselLen
      if (!len || !this.$refs.chart) {
        return
      }
      let idx = 0
      const tick = () => {
        const chart = this.$refs.chart
        if (!chart || !chart.dispatchAction) {
          this.clearCarousel()
          return
        }
        chart.dispatchAction({ type: 'downplay', seriesIndex: 0 })
        chart.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: idx })
        chart.dispatchAction({ type: 'showTip', seriesIndex: 0, dataIndex: idx })
        idx = (idx + 1) % len
      }
      tick()
      this._carouselTimer = setInterval(tick, 2500)
    },
    clearCarousel () {
      if (this._carouselTimer) {
        clearInterval(this._carouselTimer)
        this._carouselTimer = null
      }
    }
  },
  beforeUnmount () {
    this.clearCarousel()
  }
}
</script>
