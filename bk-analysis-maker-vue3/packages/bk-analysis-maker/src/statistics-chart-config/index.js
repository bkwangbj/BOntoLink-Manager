import StatisticsChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

StatisticsChartConfig.install = function (Vue) {
  Vue.component(libPrefix + StatisticsChartConfig.name, StatisticsChartConfig)
}

export default StatisticsChartConfig
