<template>
  <div class="maker-embed">
    <div ref="host" class="maker-host"></div>
  </div>
</template>

<script setup>
/**
 * 内嵌的数据可视化设计器(analysis-maker),由 props 驱动。
 * 用于实例探索「看板」:根据对象类型 + 列特征自动生成图表,用户可在其上增删改。
 * 在独立子 Vue app 里挂载,离开/切换即 unmount,不污染主 app。
 */
import { createApp, h, ref, watch, onMounted, onBeforeUnmount, nextTick, resolveComponent } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
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
import 'uno.css'
import makerRequest from '@/lib/maker-request'
import { chartList } from './maker-presets'
import { buildPageConfig } from './maker-instance'
import MakerChart from './maker-chart.vue'

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

const props = defineProps({
  classId: { type: String, default: '' },
  columns: { type: Array, default: () => [] },
  filterParams: { type: Object, default: () => ({}) }
})

const host = ref(null)
let childApp = null

function destroy () { if (childApp) { childApp.unmount(); childApp = null } }

function mount () {
  destroy()
  if (!host.value) return
  let pageConfig = null
  try { pageConfig = props.classId ? buildPageConfig(props.classId, props.columns, props.filterParams) : null }
  catch (e) { console.error('[MakerEmbed] buildPageConfig failed', e); pageConfig = null }
  const Root = {
    render () {
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
  childApp.config.errorHandler = (err, inst, info) => { console.error('[MakerEmbed] render error:', info, err) }
  childApp.use(createPinia())
  childApp.use(createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { render: () => null } }] }))
  for (const [k, c] of Object.entries(ElIcons)) childApp.component(k, c)
  childApp.component('BKChart', MakerChart)
  childApp.use(ElementPlus)
  childApp.use(VXETable)
  childApp.use(ContextMenu)
  childApp.use(TColorPicker)
  childApp.use(Suite, { request: makerRequest })
  childApp.use(AnalysisMaker, { request: makerRequest })
  try { childApp.mount(host.value) } catch (e) { console.error('[MakerEmbed] mount failed', e) }
}

onMounted(() => nextTick(mount))
// 对象类型变化 → 重建(列随之变);筛选变化不重建,避免覆盖用户编辑
watch(() => props.classId, (v, old) => { if (v !== old) nextTick(mount) })

onBeforeUnmount(destroy)
</script>

<style scoped>
.maker-embed { width: 100%; height: 100%; min-height: 0; overflow: hidden; }
.maker-host { width: 100%; height: 100%; }
</style>
