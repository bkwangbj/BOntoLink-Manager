import BasicChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

BasicChartConfig.install = function (Vue) {
  Vue.component(libPrefix + BasicChartConfig.name, BasicChartConfig)
}

export default BasicChartConfig
