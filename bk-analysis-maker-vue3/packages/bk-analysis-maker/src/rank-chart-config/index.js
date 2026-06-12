import RankChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

RankChartConfig.install = function (Vue) {
  Vue.component(libPrefix + RankChartConfig.name, RankChartConfig)
}

export default RankChartConfig
