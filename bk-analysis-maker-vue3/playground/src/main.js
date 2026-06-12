import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import VXETable from 'vxe-table'
import 'vxe-table/lib/style.css'
import { router } from './router'
import Suite from 'efficient-suite'
import 'efficient-suite/dist/style.css'
import { createPinia } from 'pinia'
import 'uno.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import '@imengyu/vue3-context-menu/lib/vue3-context-menu.css'
import ContextMenu from '@imengyu/vue3-context-menu'
import AnalysisMaker from '../../packages/bk-analysis-maker/src'
import request from './utils/request'
import Chart from './components/chart/index.vue'
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import CssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import HtmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'
import { ColorPicker as TColorPicker } from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'
function useTable (app) {
  app.use(VXETable)
}

const pinia = createPinia()
const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(pinia)
app.use(router)
app.use(ContextMenu)
app.use(ElementPlus)
app.use(useTable)
app.component('BKChart', Chart)
app.use(TColorPicker)
app.use(Suite, {
  request
})
app.use(AnalysisMaker, {
  request
})
self.MonacoEnvironment = {
  getWorker (_, label) {
    if (label === 'json') {
      return new JsonWorker()
    }
    if (label === 'css' || label === 'scss' || label === 'less') {
      return new CssWorker()
    }
    if (label === 'html' || label === 'handlebars' || label === 'razor') {
      return new HtmlWorker()
    }
    if (label === 'typescript' || label === 'javascript') {
      return new TsWorker()
    }
    return new EditorWorker()
  }
}
app.mount('#app')
