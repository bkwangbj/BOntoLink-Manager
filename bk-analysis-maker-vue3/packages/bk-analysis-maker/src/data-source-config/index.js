import DataSourceConfig from './src/main.vue'
import { libPrefix } from '../configs'

DataSourceConfig.install = function (Vue) {
  Vue.component(libPrefix + DataSourceConfig.name, DataSourceConfig)
}

export default DataSourceConfig
