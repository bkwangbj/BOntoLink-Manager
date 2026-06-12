import TimeChart from './src/main.vue'
import { libPrefix } from '../configs'

TimeChart.install = function (Vue) {
  Vue.component(libPrefix + TimeChart.name, TimeChart)
}

export default TimeChart
