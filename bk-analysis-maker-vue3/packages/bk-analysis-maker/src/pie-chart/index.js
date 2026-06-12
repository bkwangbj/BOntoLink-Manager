import PieChart from './src/main.vue'
import { libPrefix } from '../configs'

PieChart.install = function (Vue) {
  Vue.component(libPrefix + PieChart.name, PieChart)
}

export default PieChart
