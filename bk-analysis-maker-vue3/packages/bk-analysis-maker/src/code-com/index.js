import CodeCom from './src/main.vue'
import { libPrefix } from '../configs'

CodeCom.install = function (Vue) {
  Vue.component(libPrefix + CodeCom.name, CodeCom)
}

export default CodeCom
