import DecorateChart from './src/main.vue'
import { libPrefix } from '../configs'

DecorateChart.install = function (Vue) {
  Vue.component(libPrefix + DecorateChart.name, DecorateChart)
}

export default DecorateChart
