import PolarChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

PolarChartConfig.install = function (Vue) {
  Vue.component(libPrefix + PolarChartConfig.name, PolarChartConfig)
}

export default PolarChartConfig
