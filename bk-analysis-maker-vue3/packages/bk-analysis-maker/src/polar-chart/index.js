import PolarChart from './src/main.vue'
import { libPrefix } from '../configs'

PolarChart.install = function (Vue) {
  Vue.component(libPrefix + PolarChart.name, PolarChart)
}

export default PolarChart
