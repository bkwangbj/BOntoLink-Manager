<template>
  <v-charts
    ref="echart"
    class="bk-maker-chart"
    v-bind="$attrs"
    autoresize
  />
</template>

<script>
/* analysis-maker 的实际图表渲染组件(宿主提供,注册为全局 BKChart) */
import VCharts from 'vue-echarts'
import { use, registerMap, graphic } from 'echarts/core'
import 'echarts-gl'
import { CanvasRenderer } from 'echarts/renderers'
import {
  BarChart, LineChart, ScatterChart, PieChart, MapChart, RadarChart, GaugeChart, HeatmapChart,
  FunnelChart, SankeyChart, GraphChart, TreeChart, TreemapChart, SunburstChart, BoxplotChart, ThemeRiverChart
} from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent, TitleComponent, VisualMapComponent, PolarComponent, CalendarComponent,
  SingleAxisComponent, DataZoomComponent, GraphicComponent, MarkLineComponent, MarkPointComponent, ToolboxComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  LineChart, BarChart, PieChart, MapChart, RadarChart, GaugeChart, ScatterChart, HeatmapChart,
  FunnelChart, SankeyChart, GraphChart, TreeChart, TreemapChart, SunburstChart, BoxplotChart, ThemeRiverChart,
  GridComponent, TooltipComponent, LegendComponent, TitleComponent, VisualMapComponent, PolarComponent, CalendarComponent,
  SingleAxisComponent, DataZoomComponent, GraphicComponent, MarkLineComponent, MarkPointComponent, ToolboxComponent
])

export default {
  name: 'Chart',
  components: { VCharts },
  inheritAttrs: false,
  mounted () {
    const proxyMethods = [
      'setOption', 'getWidth', 'getHeight', 'getDom', 'getOption', 'resize',
      'dispatchAction', 'convertToPixel', 'convertFromPixel', 'containPixel',
      'showLoading', 'hideLoading', 'getDataURL', 'getConnectedDataURL', 'clear', 'dispose'
    ]
    this.$nextTick(() => {
      for (const m of proxyMethods) this[m] = this.$refs.echart[m]
    })
  },
  created () {
    this.registerMap = (name, data) => { registerMap(name, data) }
    this.graphic = graphic
  }
}
</script>

<style scoped>
.bk-maker-chart { width: 100%; height: 100%; }
</style>
