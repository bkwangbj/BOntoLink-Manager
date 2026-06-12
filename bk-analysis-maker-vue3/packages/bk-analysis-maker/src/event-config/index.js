import EventConfig from './src/main.vue'
import { libPrefix } from '../configs'

EventConfig.install = function (Vue) {
  Vue.component(libPrefix + EventConfig.name, EventConfig)
}

export default EventConfig
