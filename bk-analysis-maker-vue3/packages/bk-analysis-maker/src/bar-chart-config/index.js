import BarChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

BarChartConfig.install = function (Vue) {
  Vue.component(libPrefix + BarChartConfig.name, BarChartConfig)
}

export default BarChartConfig
