import MapChartConfig from './src/main.vue'
import { libPrefix } from '../configs'

MapChartConfig.install = function (Vue) {
  Vue.component(libPrefix + MapChartConfig.name, MapChartConfig)
}

export default MapChartConfig
