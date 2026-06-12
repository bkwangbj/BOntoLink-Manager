import RadarChart from './src/main.vue'
import { libPrefix } from '../configs'

RadarChart.install = function (Vue) {
  Vue.component(libPrefix + RadarChart.name, RadarChart)
}

export default RadarChart
