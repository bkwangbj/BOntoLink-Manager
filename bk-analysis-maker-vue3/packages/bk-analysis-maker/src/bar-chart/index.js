import BarChart from './src/main.vue'
import { libPrefix } from '../configs'

BarChart.install = function (Vue) {
  Vue.component(libPrefix + BarChart.name, BarChart)
}

export default BarChart
