import VarListenerConfig from './src/main.vue'
import { libPrefix } from '../configs'

VarListenerConfig.install = function (Vue) {
  Vue.component(libPrefix + VarListenerConfig.name, VarListenerConfig)
}

export default VarListenerConfig
