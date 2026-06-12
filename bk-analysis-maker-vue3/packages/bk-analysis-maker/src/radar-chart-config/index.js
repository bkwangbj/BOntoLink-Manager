import RadarChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

RadarChartConfig.install = function (Vue) {
  Vue.component(libPrefix + RadarChartConfig.name, RadarChartConfig)
}

export default RadarChartConfig
