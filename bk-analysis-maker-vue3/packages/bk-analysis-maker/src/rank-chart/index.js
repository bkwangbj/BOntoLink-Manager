import RankChart from './src/main.vue'
import { libPrefix } from '../configs'

RankChart.install = function (Vue) {
  Vue.component(libPrefix + RankChart.name, RankChart)
}

export default RankChart
