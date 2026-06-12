import PageLayout from './src/main.vue'
import { libPrefix } from '../configs'

PageLayout.install = function (Vue) {
  Vue.component(libPrefix + PageLayout.name, PageLayout)
}

export default PageLayout
