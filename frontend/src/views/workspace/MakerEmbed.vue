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
import { instanceApi } from '@/api'
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
let mountSeq = 0

function destroy () { if (childApp) { childApp.unmount(); childApp = null } }

// 取链接对象类型及其列(供看板自动分析关联对象的属性/枚举)
async function loadLinkGroups (classId) {
  if (!classId) return []
  try {
    const links = (await instanceApi.links(classId)) || []
    const top = links.slice(0, 3)
    const cols = await Promise.all(top.map(l => instanceApi.columns(l.targetClassId).catch(() => [])))
    return top.map((l, i) => ({ classId: l.targetClassId, name: l.targetClassName || l.targetClassId, columns: cols[i] || [] }))
  } catch { return [] }
}

async function mount () {
  destroy()
  if (!host.value) return
  const seq = ++mountSeq
  const linkGroups = await loadLinkGroups(props.classId)
  if (seq !== mountSeq || !host.value) return   // 竞态:期间又触发了重建
  let pageConfig = null
  try { pageConfig = props.classId ? buildPageConfig(props.classId, props.columns, props.filterParams, linkGroups) : null }
  catch (e) { console.error('[MakerEmbed] buildPageConfig failed', e); pageConfig = null }
  // 跟随主项目亮/暗:暗色 → 看板用「默认2」(blue 深色),亮色 → 「默认」(default 浅色)
  const themeKey = document.documentElement.getAttribute('data-theme') === 'dark' ? 'blue' : 'default'
  const Root = {
    render () {
      const Maker = resolveComponent('AnalysisMaker')
      return h(Maker, {
        chartMenuList: chartList,
        pageConfig,
        operPermission: ['*.*'],
        layoutTools: ['addItem', 'addTabLayout'],
        isBasicMode: false,
        defaultThemeKey: themeKey
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

// 监听主项目亮/暗切换(data-theme),变化时重建看板以套用对应预设主题
let themeObserver = null
let lastTheme = ''
onMounted(() => {
  nextTick(mount)
  lastTheme = document.documentElement.getAttribute('data-theme') || ''
  themeObserver = new MutationObserver(() => {
    const t = document.documentElement.getAttribute('data-theme') || ''
    if (t !== lastTheme) { lastTheme = t; nextTick(mount) }
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})
// 对象类型变化 → 重建(列随之变);筛选变化不重建,避免覆盖用户编辑
watch(() => props.classId, (v, old) => { if (v !== old) nextTick(mount) })

onBeforeUnmount(() => { destroy(); if (themeObserver) themeObserver.disconnect() })
</script>

<style scoped>
.maker-embed { width: 100%; height: 100%; min-height: 0; overflow: hidden; }
.maker-host { width: 100%; height: 100%; }
</style>

<!-- 主项目深色模式下:统一 maker 设计器外壳(面板/工具栏/文字/背景板)为深色,配合深色画布 -->
<style>
:root[data-theme="dark"] .maker-host .analysis-maker-wrapper { background: #061230 !important; }
:root[data-theme="dark"] .maker-host .top-header,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper { background: #0A1A3C !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .group { border-color: #1E3A6E !important; }
/* 文字 / 图标 → 浅色 */
:root[data-theme="dark"] .maker-host .top-header .left span,
:root[data-theme="dark"] .maker-host .top-header .page-icon,
:root[data-theme="dark"] .maker-host .top-header .right .text-button,
:root[data-theme="dark"] .maker-host .top-header .right .text-button span,
:root[data-theme="dark"] .maker-host .text-button,
:root[data-theme="dark"] .maker-host .text-button span,
:root[data-theme="dark"] .maker-host .text-button svg,
:root[data-theme="dark"] .maker-host .grid-icon,
:root[data-theme="dark"] .maker-host .grid-icon span,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .title,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .group-name,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .menu-card-name { color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .text-button:hover { background: rgba(255,255,255,.08) !important; }
/* 搜索框 / 卡片 */
/* 面板顶部标题区 / 内容区 → 透明(露出面板深蓝底) */
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .title,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .content { background: transparent !important; }
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .title svg { color: #C9D6EC !important; }
/* 搜索框深色 */
:root[data-theme="dark"] .maker-host .search-input,
:root[data-theme="dark"] .maker-host .search-input input,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .el-input__wrapper { background: #0E224C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; box-shadow: 0 0 0 1px #1E3A6E inset !important; }
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .el-input__inner { color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .menu-item { background: transparent !important; }
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .menu-item:hover { border-color: #2E74FF !important; }
/* 图表项 hover 预览 tooltip(渲染到 body,带 tip_popper) */
:root[data-theme="dark"] .tip_popper.el-popper,
:root[data-theme="dark"] .tip_popper { background: #0A1A3C !important; border: 1px solid #1E3A6E !important; color: #C9D6EC !important; }
:root[data-theme="dark"] .tip_popper .menu-item-tip,
:root[data-theme="dark"] .tip_popper .menu-card-name { color: #C9D6EC !important; }
:root[data-theme="dark"] .tip_popper .el-popper__arrow::before { background: #0A1A3C !important; border-color: #1E3A6E !important; }
/* 右侧配置面板(页面设置 / 图表配置)深色化 */
:root[data-theme="dark"] .maker-host .right-setting-container,
:root[data-theme="dark"] .maker-host .page-setting-wrapper { background: #0A1A3C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .title,
:root[data-theme="dark"] .maker-host .right-setting-container label,
:root[data-theme="dark"] .maker-host .right-setting-container span { color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-input__wrapper,
:root[data-theme="dark"] .maker-host .right-setting-container .el-textarea__inner,
:root[data-theme="dark"] .maker-host .right-setting-container .el-select__wrapper,
:root[data-theme="dark"] .maker-host .right-setting-container .el-input-number { background: #0E224C !important; box-shadow: 0 0 0 1px #1E3A6E inset !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-input__inner,
:root[data-theme="dark"] .maker-host .right-setting-container .el-textarea__inner { color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-radio-button__inner,
:root[data-theme="dark"] .maker-host .right-setting-container .el-button:not(.el-button--primary) { background: #0E224C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-radio-button.is-active .el-radio-button__inner { background: #2E74FF !important; color: #fff !important; border-color: #2E74FF !important; }
/* 折叠分组条(卡片样式/页签样式/页面布局…)+ 顶部标题条 → 深色 */
:root[data-theme="dark"] .maker-host .right-setting-container .title { background: transparent !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-collapse,
:root[data-theme="dark"] .maker-host .right-setting-container .el-collapse-item__header,
:root[data-theme="dark"] .maker-host .right-setting-container .el-collapse-item__wrap,
:root[data-theme="dark"] .maker-host .right-setting-container .el-collapse-item__content { background: #0A1A3C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-collapse-item__header { border-bottom: 1px solid #1E3A6E !important; }
/* 分段控制器(填充方式/圆角/卡片间距 等) */
:root[data-theme="dark"] .maker-host .right-setting-container .el-segmented { background: #0E224C !important; --el-segmented-bg-color: #0E224C !important; --el-segmented-item-selected-bg-color: #2E74FF !important; --el-segmented-color: #C9D6EC !important; --el-segmented-item-selected-color: #fff !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-segmented__item { color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-segmented__item.is-selected { color: #fff !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-segmented__item-selected { background: #2E74FF !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-segmented__group { background: transparent !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-segmented__item { background: transparent !important; }
/* 颜色选择器(背景颜色/边框颜色 色块) */
:root[data-theme="dark"] .maker-host .right-setting-container .el-color-picker__trigger { background: #0E224C !important; border-color: #1E3A6E !important; }
/* 画布工作区底色(左侧浅色工作台 → 深色) */
:root[data-theme="dark"] .maker-host .working-container { background: #061230 !important; }
:root[data-theme="dark"] .maker-host .working-container.no-background { background: transparent !important; }
/* 左侧面板分组/搜索分隔线 → 深色 */
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .el-collapse,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .el-collapse-item,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .el-collapse-item__header,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .el-collapse-item__wrap,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .group,
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .title { border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .toolbar-pannel-wrapper .el-collapse-item__header { border-bottom-color: #1E3A6E !important; }
/* 兜底:深色模式下 maker 容器内所有布局分隔边框统一深蓝(顶部横线/左侧竖线等) */
:root[data-theme="dark"] .maker-host * { border-color: #1E3A6E !important; }
/* 画布工作区 / 中间容器边框也压深 */
:root[data-theme="dark"] .maker-host .working-container,
:root[data-theme="dark"] .maker-host .top-header { border-color: #1E3A6E !important; }
/* 右侧面板内残留白块(标题下条 / 底部 footer)→ 透明露深蓝 */
:root[data-theme="dark"] .maker-host .page-setting-wrapper,
:root[data-theme="dark"] .maker-host .right-setting-container { background: #0A1A3C !important; }
:root[data-theme="dark"] .maker-host .page-setting-wrapper > div,
:root[data-theme="dark"] .maker-host .right-setting-container > div,
:root[data-theme="dark"] .maker-host .page-setting-wrapper .footer,
:root[data-theme="dark"] .maker-host .right-setting-container .footer { background-color: transparent !important; }
/* 按钮组(主题模式 默认/深蓝 等 el-radio-button):未选深底,选中蓝 */
:root[data-theme="dark"] .maker-host .right-setting-container .el-radio-group { background: transparent !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-radio-button__inner { background: transparent !important; color: #C9D6EC !important; border-color: #1E3A6E !important; box-shadow: -1px 0 0 0 #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-radio-button:first-child .el-radio-button__inner { box-shadow: none !important; }
:root[data-theme="dark"] .maker-host .right-setting-container .el-radio-button.is-active .el-radio-button__inner { background: #2E74FF !important; color: #fff !important; border-color: #2E74FF !important; }
/* 分隔线 */
:root[data-theme="dark"] .maker-host .right-setting-container .el-divider { border-color: #1E3A6E !important; background: #1E3A6E !important; }
/* 兜底:面板内各类分段/标签/单选按钮组的容器与未选项 → 深色融入 */
:root[data-theme="dark"] .maker-host .right-setting-container [class*="segment"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="tabs"]:not(.el-collapse),
:root[data-theme="dark"] .maker-host .right-setting-container [class*="tab-group"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="btn-group"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="toggle"] { background: transparent !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container [class*="segment"] [class*="item"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="tab"] [class*="item"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="toggle"] [class*="item"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="btn-group"] > * { background: transparent !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container [class*="item"][class*="active"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="item"][class*="selected"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="active"][class*="item"] { background: #2E74FF !important; color: #fff !important; }
/* 高特异性覆盖:page-setting 面板内被 maker scoped !important 钉死的 ef-radio-button(平铺/拉伸、无/小/大、下划线/分割线… 等) */
:root[data-theme="dark"] .maker-host .page-setting-wrapper .page-setting-container .el-radio-button .el-radio-button__inner { background-color: transparent !important; color: #C9D6EC !important; border-color: #1E3A6E !important; box-shadow: -1px 0 0 0 #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .page-setting-wrapper .page-setting-container .el-radio-button:first-child .el-radio-button__inner { box-shadow: none !important; }
:root[data-theme="dark"] .maker-host .page-setting-wrapper .page-setting-container .el-radio-button[class*="active"] .el-radio-button__inner,
:root[data-theme="dark"] .maker-host .page-setting-wrapper .page-setting-container .el-radio-button.is-active .el-radio-button__inner { background-color: #2E74FF !important; color: #fff !important; border-color: #2E74FF !important; }
/* page-setting 顶部标题区/分隔线 → 深色(高特异性盖 scoped) */
:root[data-theme="dark"] .maker-host .page-setting-wrapper .page-setting-container .title,
:root[data-theme="dark"] .maker-host .page-setting-wrapper .title { background-color: transparent !important; border-color: #1E3A6E !important; border-bottom-color: #1E3A6E !important; box-shadow: 0 1px 0 0 #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .page-setting-wrapper .page-setting-container > div,
:root[data-theme="dark"] .maker-host .page-setting-wrapper .page-setting-container > div:first-child { background-color: transparent !important; border-color: #1E3A6E !important; }
/* maker 弹框(主题配置等,teleport 到 body)深色 */
:root[data-theme="dark"] .el-overlay .el-dialog { background: #0A1A3C !important; border: 1px solid #1E3A6E !important; }
:root[data-theme="dark"] .el-overlay .el-dialog__header,
:root[data-theme="dark"] .el-overlay .el-dialog__footer { border-color: #1E3A6E !important; }
:root[data-theme="dark"] .el-overlay .el-dialog__title,
:root[data-theme="dark"] .el-overlay .el-dialog__headerbtn .el-dialog__close,
:root[data-theme="dark"] .el-overlay .el-dialog__body { color: #C9D6EC !important; }
:root[data-theme="dark"] .el-overlay .el-dialog__body,
:root[data-theme="dark"] .el-overlay .el-dialog__body textarea,
:root[data-theme="dark"] .el-overlay .el-dialog__body pre,
:root[data-theme="dark"] .el-overlay .el-dialog__body .el-textarea__inner { background: #0E224C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
</style>
