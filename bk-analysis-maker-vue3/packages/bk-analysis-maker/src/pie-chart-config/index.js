import PieChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

PieChartConfig.install = function (Vue) {
  Vue.component(libPrefix + PieChartConfig.name, PieChartConfig)
}

export default PieChartConfig
