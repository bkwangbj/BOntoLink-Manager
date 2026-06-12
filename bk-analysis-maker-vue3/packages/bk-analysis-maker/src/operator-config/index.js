import OperatorConfig from './src/main.vue'
import { libPrefix } from '../configs'

OperatorConfig.install = function (Vue) {
  Vue.component(libPrefix + OperatorConfig.name, OperatorConfig)
}

export default OperatorConfig
