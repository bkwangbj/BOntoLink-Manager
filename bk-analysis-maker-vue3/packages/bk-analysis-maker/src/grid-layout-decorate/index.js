import GridLayoutDecorate from './src/main.vue'
import { libPrefix } from '../configs'

GridLayoutDecorate.install = function (Vue) {
  Vue.component(libPrefix + GridLayoutDecorate.name, GridLayoutDecorate)
}

export default GridLayoutDecorate
