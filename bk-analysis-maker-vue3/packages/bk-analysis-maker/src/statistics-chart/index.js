import StatisticsChart from './src/main.vue'
import { libPrefix } from '../configs'

StatisticsChart.install = function (Vue) {
  Vue.component(libPrefix + StatisticsChart.name, StatisticsChart)
}

export default StatisticsChart
