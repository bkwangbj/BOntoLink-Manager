import TabLayout from './src/main.vue'
import { libPrefix } from '../configs'

TabLayout.install = function (Vue) {
  Vue.component(libPrefix + TabLayout.name, TabLayout)
}

export default TabLayout
