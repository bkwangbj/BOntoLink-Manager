import TableChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

TableChartConfig.install = function (Vue) {
  Vue.component(libPrefix + TableChartConfig.name, TableChartConfig)
}

export default TableChartConfig
