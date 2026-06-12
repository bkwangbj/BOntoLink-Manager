import TimeChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

TimeChartConfig.install = function (Vue) {
  Vue.component(libPrefix + TimeChartConfig.name, TimeChartConfig)
}

export default TimeChartConfig
