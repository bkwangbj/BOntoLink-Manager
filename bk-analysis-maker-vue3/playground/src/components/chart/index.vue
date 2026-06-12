<template>
  <v-charts
    ref="echart"
    class="my-chart-wrapper"
    v-bind="$attrs"
    autoresize
  />
</template>

<script>
import VCharts from 'vue-echarts'
import { use, registerMap, graphic } from 'echarts/core'
import 'echarts-gl'

import {
  CanvasRenderer
} from 'echarts/renderers'
import {
  BarChart,
  LineChart,
  ScatterChart,
  PieChart,
  MapChart,
  RadarChart,
  GaugeChart
} from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  VisualMapComponent,
  PolarComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  MapChart,
  RadarChart,
  GaugeChart,
  ScatterChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  VisualMapComponent,
  PolarComponent
])

export default {
  name: 'Chart',
  components: {
    VCharts
  },
  inheritAttrs: false,
  mounted () {
    const proxyMethods = [
      'setOption',
      'getWidth',
      'getHeight',
      'getDom',
      'getOption',
      'resize',
      'dispatchAction',
      'convertToPixel',
      'convertFromPixel',
      'containPixel',
      'showLoading',
      'hideLoading',
      'containPixel',
      'getDataURL',
      'getConnectedDataURL',
      'clear',
      'dispose'
    ]
    this.$nextTick(() => {
      for (const m of proxyMethods) {
        this[m] = this.$refs.echart[m]
      }
    })
  },
  created () {
    this.registerMap = (name, data) => {
      registerMap(name, data)
    }
    this.graphic = graphic
  }
}
</script>

<style lang="scss" scoped>
.my-chart-wrapper {
  width: 100%;
  height: 100%;
}
</style>
