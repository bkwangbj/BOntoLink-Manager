import CodeChart from './src/main.vue'
import { libPrefix } from '../configs'

CodeChart.install = function (Vue) {
  Vue.component(libPrefix + CodeChart.name, CodeChart)
}

export default CodeChart
