import MapChart from './src/main.vue'
import { libPrefix } from '../configs'

MapChart.install = function (Vue) {
  Vue.component(libPrefix + MapChart.name, MapChart)
}

export default MapChart
