import GaugeChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

GaugeChartConfig.install = function (Vue) {
  Vue.component(libPrefix + GaugeChartConfig.name, GaugeChartConfig)
}

export default GaugeChartConfig
