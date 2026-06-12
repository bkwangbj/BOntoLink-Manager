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
// import originList from '../../map-chart-config/src/config/default-data'
import { mixins } from '../../configs/commom-chart'
import { utils } from 'efficient-suite'
export default {
  name: 'RadarChart',
  components: {

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
    itemClick ({ data }) {
      this.handleEvent(data, 'click')
    },
    customResetChart (config) {
      this.$nextTick(() => {
        if (config.configOption) {
          const form = utils.deepClone(config.configOption)
          let xData = this.relList.map(item => item.t).filter(ele => { return ele })
          xData = Array.from(new Set(xData))
          if (form.radar) {
            if (form.radar.radius) {
              form.radar.radius = form.radar.radius + '%'
            } if (form.radar.center) {
              form.radar.center[0] = form.radar.center[0] + '%'
              form.radar.center[1] = form.radar.center[1] + '%'
            }
            form.radar.indicator = xData.map(ele => { return { text: ele } })
          }
          if (form.legend.alignPosition) {
            const alignList = {
              topLeft: { left: 'left', top: 10, bottom: undefined },
              topCenter: { left: 'center', top: 10, bottom: undefined },
              topRight: { left: 'right', top: 10, bottom: undefined },
              bottomLeft: { left: 'left', bottom: 5, top: undefined },
              bottomCenter: { left: 'center', bottom: 5, top: undefined },
              bottomRight: { left: 'right', bottom: 5, top: undefined }
            }
            if (form.legend.left) {
              form.legend.left = form.legend.left + '%'
              form.legend.top = form.legend.top + '%'
            }
            if (form.legend.top) {
              form.legend.top = form.legend.top + '%'
            }
            form.legend = { ...form.legend, ...form.legend.alignPosition ? alignList[form.legend.alignPosition] : null }
          }
          if (form.series) {
            form.series.forEach((serie, i) => {
              if (!serie.areaStyle?.show) {
                serie.areaStyle = null
              }
              let yData = this.relList.filter(item => item.colorField === form.series[i].dataId)
              yData = xData.map(item => {
                const dataItem = yData.find(ele => ele.t === item)
                let value = dataItem?.r
                if (isNaN(value)) {
                  try {
                    value = Number(dataItem?.y.replace(/,/g, ''))
                  } catch (error) {

                  }
                }
                return value || null
              })
              serie.data = [
                {
                  value: yData,
                  name: serie.name || ''
                }
              ]
            })
          }
          this.setCustomOption(form, config)
          this.option = form
        }
      })
    }
  }
}
</script>
