import GaugeChart from './src/main.vue'
import { libPrefix } from '../configs'

GaugeChart.install = function (Vue) {
  Vue.component(libPrefix + GaugeChart.name, GaugeChart)
}

export default GaugeChart
