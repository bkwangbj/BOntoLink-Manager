import TableChart from './src/main.vue'
import { libPrefix } from '../configs'

TableChart.install = function (Vue) {
  Vue.component(libPrefix + TableChart.name, TableChart)
}

export default TableChart
