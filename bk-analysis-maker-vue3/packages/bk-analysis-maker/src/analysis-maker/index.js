import AnalysisMaker from './src/main.vue'

AnalysisMaker.install = function (Vue) {
  Vue.component(AnalysisMaker.name, AnalysisMaker)
}

export default AnalysisMaker
