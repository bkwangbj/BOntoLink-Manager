<template>
  <div class="maker-studio">
    <div ref="host" class="maker-host"></div>
  </div>
</template>

<script setup>
/**
 * 数据可视化设计器(analysis-maker)隔离挂载点。
 * 方案 A:在独立子 Vue app 里安装 element-plus / vxe-table / maker 等重依赖,
 *        挂到本组件的容器 div,离开路由即 unmount —— 不污染 BOntoLink02 主 app。
 * 这些 import 只在访问本路由时才作为懒加载 chunk 下载。
 */
import { createApp, h, ref, onMounted, onBeforeUnmount, resolveComponent } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory, useRoute } from 'vue-router'
import { instanceApi } from '@/api'
import { buildPageConfig } from './maker-instance'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElIcons from '@element-plus/icons-vue'
import VXETable from 'vxe-table'
import 'vxe-table/lib/style.css'
import ContextMenu from '@imengyu/vue3-context-menu'
import '@imengyu/vue3-context-menu/lib/vue3-context-menu.css'
import Suite from 'efficient-suite'
import 'efficient-suite/dist/style.css'
import { ColorPicker as TColorPicker } from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import AnalysisMaker from 'analysis-maker-vue3'
import 'analysis-maker-vue3/dist/style.css'
import 'uno.css'   // UnoCSS 原子类(只为 maker dist 生成)
import makerRequest from '@/lib/maker-request'
import { chartList } from './maker-presets'
import MakerChart from './maker-chart.vue'   // 实际图表渲染组件,注册为全局 BKChart

// monaco 编辑器 worker(代码图表用)
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import CssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import HtmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

if (!self.MonacoEnvironment) {
  self.MonacoEnvironment = {
    getWorker (_, label) {
      if (label === 'json') return new JsonWorker()
      if (label === 'css' || label === 'scss' || label === 'less') return new CssWorker()
      if (label === 'html' || label === 'handlebars' || label === 'razor') return new HtmlWorker()
      if (label === 'typescript' || label === 'javascript') return new TsWorker()
      return new EditorWorker()
    }
  }
}

const host = ref(null)
let childApp = null
const route = useRoute()

onMounted(async () => {
  // 从实例探索进入时,带 classId → 取列 → 构造初始 pageConfig(图表绑定实例数据)
  let pageConfig = null
  const classId = route.query.classId
  if (classId) {
    try {
      const columns = await instanceApi.columns(classId)
      pageConfig = buildPageConfig(classId, columns, { q: route.query.q, filter: route.query.filter })
    } catch (e) { pageConfig = null }
  }

  const Root = {
    render () {
      // AnalysisMaker 由插件全局注册,按注册名解析组件(默认导入的是插件 {install},不可直接渲染)
      const Maker = resolveComponent('AnalysisMaker')
      return h(Maker, {
        chartMenuList: chartList,
        pageConfig,
        operPermission: ['*.*'],
        layoutTools: ['addItem', 'addTabLayout'],
        isBasicMode: false
      })
    }
  }
  childApp = createApp(Root)
  // 子 app 独立的 pinia + router(memory history,避免与主路由抢 URL hash)
  childApp.use(createPinia())
  childApp.use(createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { render: () => null } }] }))
  for (const [k, c] of Object.entries(ElIcons)) childApp.component(k, c)
  childApp.component('BKChart', MakerChart)   // maker 图表渲染委托给宿主提供的 BKChart
  childApp.use(ElementPlus, { locale: zhCn })
  childApp.use(VXETable)
  childApp.use(ContextMenu)
  childApp.use(TColorPicker)
  childApp.use(Suite, { request: makerRequest })
  childApp.use(AnalysisMaker, { request: makerRequest })
  childApp.mount(host.value)
})

onBeforeUnmount(() => {
  if (childApp) { childApp.unmount(); childApp = null }
})
</script>

<style scoped>
.maker-studio { width: 100%; height: 100%; min-height: 0; overflow: hidden; }
.maker-host { width: 100%; height: 100%; }
</style>
