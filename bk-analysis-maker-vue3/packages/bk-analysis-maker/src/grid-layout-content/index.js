import GridLayoutContent from './src/main.vue'
import { libPrefix } from '../configs'

GridLayoutContent.install = function (Vue) {
  Vue.component(libPrefix + GridLayoutContent.name, GridLayoutContent)
}

export default GridLayoutContent
