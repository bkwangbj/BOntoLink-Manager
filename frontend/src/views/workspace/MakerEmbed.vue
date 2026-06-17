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
  // 跟随主项目亮/暗:暗色 → 看板用「默认2」(blue 深色)+ 图表浅色文字,亮色 → 「默认」(default 浅色)
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
  const themeKey = isDark ? 'blue' : 'default'
  let pageConfig = null
  try { pageConfig = props.classId ? buildPageConfig(props.classId, props.columns, props.filterParams, linkGroups, isDark) : null }
  catch (e) { console.error('[MakerEmbed] buildPageConfig failed', e); pageConfig = null }
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
  // 静音良性警告:maker plugin 被重复 use(不影响功能),避免刷屏
  childApp.config.warnHandler = (msg, inst, trace) => { if (typeof msg === 'string' && msg.includes('Plugin has already been applied')) return; console.warn(msg) }
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
/* 工具栏按钮之间的分隔竖线 → 淡一些 */
:root[data-theme="dark"] .maker-host .text-button::after,
:root[data-theme="dark"] .maker-host .top-header .text-button::after { background: rgba(255,255,255,.12) !important; }
/* tdesign 颜色选择器(背景颜色/边框颜色 t-color-picker / t-input)深色 */
:root[data-theme="dark"] .maker-host .common-color-input .t-input,
:root[data-theme="dark"] .maker-host .t-color-picker__trigger .t-input,
:root[data-theme="dark"] .maker-host .t-input.t-is-disabled,
:root[data-theme="dark"] .maker-host .t-input { background-color: #0E224C !important; border-color: #1E3A6E !important; color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .t-input__inner,
:root[data-theme="dark"] .maker-host .t-input.t-is-disabled .t-input__inner { color: #C9D6EC !important; -webkit-text-fill-color: #C9D6EC !important; }
/* 页边距可视化设置框(padding-box-wrapper)深色 */
:root[data-theme="dark"] .maker-host .padding-box-wrapper,
:root[data-theme="dark"] .maker-host .padding-box-wrapper .inner-box { background-color: #0E224C !important; border-color: #1E3A6E !important; }
/* 图表配置面板 tab(基础/数据源/高级 等 el-tabs)文字 */
:root[data-theme="dark"] .maker-host .el-tabs__item { color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .config-tab .el-tabs__header .el-tabs__item.is-active,
:root[data-theme="dark"] .maker-host .el-tabs--card .el-tabs__header .el-tabs__item.is-active,
:root[data-theme="dark"] .maker-host .el-tabs__item.is-active { color: #fff !important; font-weight: 700 !important; background-color: #033599 !important;  border-radius: 4px 4px 0 0 !important; }
:root[data-theme="dark"] .maker-host .el-tabs__nav-wrap::after { background-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .el-tabs__active-bar { background-color: #4D8BFF !important; height: 3px !important; }
/* 图表配置面板左侧竖排 tab(chart-sidebar,#f7f7f7 白)→ 深色 */
:root[data-theme="dark"] .maker-host .chart-sidebar { background-color: #041b55 !important; border-right:1px solid #073997 !important; }
/* tabs 栏背景比内容区更深一档,形成区分 */
:root[data-theme="dark"] .maker-host .el-tabs__header,
:root[data-theme="dark"] .maker-host .el-tabs__nav-wrap { background-color: #060F26 !important; }
:root[data-theme="dark"] .maker-host .chart-sidebar,
:root[data-theme="dark"] .maker-host .chart-sidebar * { color: #C9D6EC !important; }
/* 竖排 tab 项:未选透明融入,选中蓝底白字 */
:root[data-theme="dark"] .maker-host .chart-sidebar > *,
:root[data-theme="dark"] .maker-host .chart-sidebar li,
:root[data-theme="dark"] .maker-host .chart-sidebar [class*="item"],
:root[data-theme="dark"] .maker-host .chart-sidebar [class*="tab"] { background-color: transparent !important; }
:root[data-theme="dark"] .maker-host .chart-sidebar [class*="active"],
:root[data-theme="dark"] .maker-host .chart-sidebar [class*="selected"],
:root[data-theme="dark"] .maker-host .chart-sidebar .is-active { background-color: #033599 !important; }
:root[data-theme="dark"] .maker-host .chart-sidebar [class*="active"],
:root[data-theme="dark"] .maker-host .chart-sidebar [class*="active"] *,
:root[data-theme="dark"] .maker-host .chart-sidebar [class*="selected"],
:root[data-theme="dark"] .maker-host .chart-sidebar .is-active * { color: #fff !important; }
/* tab 内容区(数据源/基础 等)文字标签 → 浅色(排除选中态) */
:root[data-theme="dark"] .maker-host .el-tabs__content label,
:root[data-theme="dark"] .maker-host .el-tabs__content [class*="label"],
:root[data-theme="dark"] .maker-host .el-tabs__content [class*="title"]:not([class*="active"]),
:root[data-theme="dark"] .maker-host .el-tabs__content [class*="name"],
:root[data-theme="dark"] .maker-host .el-tabs__content span:not([class*="active"]):not([class*="selected"]) { color: #C9D6EC !important; }
/* 开关(el-switch / t-switch):关闭态深底蓝边,开启态蓝 */
:root[data-theme="dark"] .maker-host .el-switch { --el-switch-off-color: #0E224C !important; --el-switch-border-color: #1E3A6E !important; --el-switch-on-color: #2E74FF !important; }
:root[data-theme="dark"] .maker-host .el-switch__core { border: 1px solid #0c41a1 !important; }
:root[data-theme="dark"] .maker-host .t-switch:not(.t-is-checked) { background-color: #0E224C !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .t-switch.t-is-checked { background-color: #2E74FF !important; }
/* 数字输入框上下调节按钮(el-input-number spinner)深色 + hover */
:root[data-theme="dark"] .maker-host .el-input-number__increase,
:root[data-theme="dark"] .maker-host .el-input-number__decrease { background-color: #0E224C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .el-input-number__increase:hover,
:root[data-theme="dark"] .maker-host .el-input-number__decrease:hover { background-color: #143063 !important; color: #fff !important; }
/* 工具栏「配置面板」切换按钮(fold-icon)深色:常态透明描边,激活蓝 */
:root[data-theme="dark"] .maker-host .top-header .fold-icon,
:root[data-theme="dark"] .maker-host .fold-icon { background-color: transparent !important; color: #C9D6EC !important; border: 1px solid #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .fold-icon.active,
:root[data-theme="dark"] .maker-host .fold-icon.is-active,
:root[data-theme="dark"] .maker-host .fold-icon[class*="active"] { background-color: rgba(46,116,255,.22) !important; color: #4D8BFF !important; border-color: #2E74FF !important; }
/* 弹框标题栏/底部栏背景 → 深色(之前只改了 body 和边框,标题/底栏仍白) */
:root[data-theme="dark"] .el-overlay .el-dialog__header,
:root[data-theme="dark"] .el-overlay .el-dialog__footer { background-color: #0A1A3C !important; }
:root[data-theme="dark"] .el-overlay .el-dialog__title { color: #C9D6EC !important; }
:root[data-theme="dark"] .el-overlay .el-dialog__headerbtn .el-dialog__close { color: #C9D6EC !important; }
/* maker 弹框其实是 vxe-modal(teleport 到 body):用 vxe 变量 + class 深色 */
:root[data-theme="dark"] .vxe-modal--wrapper {
  --vxe-modal-header-background-color: #0A1A3C !important;
  --vxe-modal-body-background-color: #0E224C !important;
  --vxe-modal-border-color: #1E3A6E !important;
  --vxe-ui-modal-header-background-color: #0A1A3C !important;
  --vxe-ui-modal-body-background-color: #0E224C !important;
  --vxe-ui-modal-border-color: #1E3A6E !important;
}
:root[data-theme="dark"] .vxe-modal--box { background-color: #0A1A3C !important; }
:root[data-theme="dark"] .vxe-modal--header { background-color: #0A1A3C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .vxe-modal--body { background-color: #0E224C !important; color: #C9D6EC !important; }
:root[data-theme="dark"] .vxe-modal--footer { background-color: #0A1A3C !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .vxe-modal--header .vxe-modal--close-btn,
:root[data-theme="dark"] .vxe-modal--header .vxe-modal--zoom-btn,
:root[data-theme="dark"] .vxe-modal--header .vxe-modal--title { color: #C9D6EC !important; }
/* 弹框内表格(vxe-modal 在 body,不在 .maker-host)套深色 */
:root[data-theme="dark"] .vxe-modal--wrapper .el-table {
  --el-table-bg-color: #0E224C !important; --el-table-tr-bg-color: #0E224C !important; --el-table-header-bg-color: #0A1733 !important;
  --el-table-border-color: #20395E !important; --el-table-text-color: #C9D6EC !important; --el-table-header-text-color: #C9D6EC !important;
}
:root[data-theme="dark"] .vxe-modal--wrapper .vxe-table { --vxe-table-border-color: #20395E !important; --vxe-ui-table-border-color: #20395E !important; }
:root[data-theme="dark"] .vxe-modal--wrapper .el-tabs__nav-wrap::after { background-color: #1E3A6E !important; height: 1px !important; }
:root[data-theme="dark"] .vxe-modal--wrapper .el-table { --el-table-border-color: #20395E !important; }
:root[data-theme="dark"] .vxe-modal--wrapper .vxe-header--column,
:root[data-theme="dark"] .vxe-modal--wrapper .vxe-table .vxe-header--column { background-color: #0A1733 !important; color: #C9D6EC !important; }
:root[data-theme="dark"] .vxe-modal--wrapper .vxe-body--column,
:root[data-theme="dark"] .vxe-modal--wrapper .vxe-table .vxe-body--column { background-color: #0E224C !important; color: #C9D6EC !important; }
:root[data-theme="dark"] .vxe-modal--wrapper .el-tabs__item { color: #C9D6EC !important; }
:root[data-theme="dark"] .vxe-modal--wrapper .el-tabs__item.is-active { color: #4D8BFF !important; }
/* 全局参数弹框内容面板(var-content/var-panel #f7f7fb 白 / footer)→ 深 */
:root[data-theme="dark"] .vxe-modal--wrapper .var-content,
:root[data-theme="dark"] .vxe-modal--wrapper .var-panel { background-color: #0E224C !important; border: none !important; color: #C9D6EC !important; }
:root[data-theme="dark"] .vxe-modal--wrapper .analysis-modal-footer { background-color: #0E224C !important; border-color: #1E3A6E !important; color: #C9D6EC !important; }
/* tabs 头部背景差异化(比内容区亮一档,盖过早期 [class*="tabs"] 兜底的 transparent) */
:root[data-theme="dark"] .maker-host .right-setting-container .config-tab .el-tabs__header,
:root[data-theme="dark"] .maker-host .right-setting-container .config-tab .el-tabs__nav,
:root[data-theme="dark"] .maker-host .config-tab .el-tabs__header,
:root[data-theme="dark"] .maker-host .config-tab .el-tabs__nav,
:root[data-theme="dark"] .maker-host .right-setting-container .el-tabs__header,
:root[data-theme="dark"] .maker-host .right-setting-container .el-tabs__nav { background-color: #102A55 !important; }
/* 通用:所有 el-radio-button 按钮组(图表配置 风玫瑰图 半径/角度 等)未选透明/选中蓝 — 高特异性盖 maker scoped */
:root[data-theme="dark"] .maker-host .el-radio-group .el-radio-button .el-radio-button__inner { background-color: transparent !important; color: #C9D6EC !important; border-color: #1E3A6E !important; box-shadow: -1px 0 0 0 #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .el-radio-group .el-radio-button:first-child .el-radio-button__inner { box-shadow: none !important; }
:root[data-theme="dark"] .maker-host .el-radio-group .el-radio-button.is-active .el-radio-button__inner,
:root[data-theme="dark"] .maker-host .el-radio-group .el-radio-button[class*="active"] .el-radio-button__inner { background-color: #2E74FF !important; color: #fff !important; border-color: #2E74FF !important; }
/* 下拉浮层(配色方案/选择项 等,teleport 到 body) */
:root[data-theme="dark"] .el-select-dropdown,
:root[data-theme="dark"] .el-select__popper.el-popper { background: #0A1A3C !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .el-select-dropdown__item { color: #C9D6EC !important; }
:root[data-theme="dark"] .el-select-dropdown__item.is-hovering,
:root[data-theme="dark"] .el-select-dropdown__item:hover { background-color: #0E224C !important; }
:root[data-theme="dark"] .el-select__popper.el-popper .el-popper__arrow::before { background: #0A1A3C !important; border-color: #1E3A6E !important; }
/* —— 共性批量:配置面板容器统一深底 —— */
:root[data-theme="dark"] .maker-host .right-setting-container,
:root[data-theme="dark"] .maker-host .chart-cfg-wrap,
:root[data-theme="dark"] .maker-host .page-setting-wrapper { background-color: #0A1A3C !important; color: #C9D6EC !important; }
/* —— 共性批量:面板内「非控件」布局块背景一律透明露深底(一次盖掉各种 #f7f7f7/#ededed/#fff 白底);控件因类名带 input/select/radio/picker… 被排除,保留各自深色 —— */
:root[data-theme="dark"] .maker-host .right-setting-container div:not([class*="input"]):not([class*="select"]):not([class*="radio"]):not([class*="button"]):not([class*="picker"]):not([class*="switch"]):not([class*="segment"]):not([class*="checkbox"]):not([class*="slider"]):not([class*="color"]):not([class*="active"]):not([class*="selected"]):not([class*="inner-bg"]):not([class*="padding-box"]):not([class*="el-tabs__header"]):not([class*="el-tabs__nav"]):not([class*="sidebar"]),
:root[data-theme="dark"] .maker-host .chart-cfg-wrap div:not([class*="input"]):not([class*="select"]):not([class*="radio"]):not([class*="button"]):not([class*="picker"]):not([class*="switch"]):not([class*="segment"]):not([class*="checkbox"]):not([class*="slider"]):not([class*="color"]):not([class*="active"]):not([class*="selected"]):not([class*="inner-bg"]):not([class*="padding-box"]):not([class*="el-tabs__header"]):not([class*="el-tabs__nav"]):not([class*="sidebar"]) { background-color: transparent !important; }
/* 功能区块(边距框/内边距框等)→ 比页面底稍亮一档,形成层次差异 */
:root[data-theme="dark"] .maker-host .padding-box-wrapper,
:root[data-theme="dark"] .maker-host .plane-inner-bg,
:root[data-theme="dark"] .maker-host [class*="inner-bg"],
:root[data-theme="dark"] .maker-host [class*="padding-box"] { background-color: #102A55 !important; border: 1px solid #20395E !important; border-radius: 4px !important; }
/* 面板内文字统一浅色(选中态除外) */
:root[data-theme="dark"] .maker-host .right-setting-container label,
:root[data-theme="dark"] .maker-host .chart-cfg-wrap label,
:root[data-theme="dark"] .maker-host .chart-cfg-wrap span:not([class*="active"]) { color: #C9D6EC !important; }
/* 共性补充:普通按钮(非主按钮/文字按钮)统一深底 */
:root[data-theme="dark"] .maker-host .el-button:not(.el-button--primary):not(.el-button--text):not(.is-text) { background-color: #0E224C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .el-button:not(.el-button--primary):not(.el-button--text):hover { background-color: #143063 !important; border-color: #2E74FF !important; color: #fff !important; }
/* 共性补充:颜色输入容器统一深底 */
:root[data-theme="dark"] .maker-host .common-color-input,
:root[data-theme="dark"] .maker-host .common-color-input .t-input,
:root[data-theme="dark"] .maker-host [class*="color-input"] { background-color: #0E224C !important; border-color: #1E3A6E !important; }
/* 共性:复制/粘贴基础配置(.copy-button-box 内白底 outline 按钮,虽是 primary)→ 深底 */
:root[data-theme="dark"] .maker-host .copy-button-box .el-button { background-color: #0E224C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .copy-button-box .el-button:hover { background-color: #143063 !important; border-color: #2E74FF !important; color: #fff !important; }
/* 共性:el-select 前缀区(配色方案预览 #ededed)→ 深底 */
:root[data-theme="dark"] .maker-host .el-select__prefix { background-color: #0E224C !important; }
/* 含 "color" 的「输入框」(仅这些容器深底,不碰颜色预览色块!色块靠 inline 背景显示颜色) */
:root[data-theme="dark"] .maker-host .colors-number,
:root[data-theme="dark"] .maker-host .common-color-input,
:root[data-theme="dark"] .maker-host [class*="color-input"] { background-color: #0E224C !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .colors-number,
:root[data-theme="dark"] .maker-host .colors-number input { color: #C9D6EC !important; -webkit-text-fill-color: #C9D6EC !important; }
/* 共性:efficient-suite 边框变量 → 深蓝(一次盖掉 mc-dragger 等用 --ef-border-color 的浅色线) */
:root[data-theme="dark"] .maker-host { --ef-border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .mc-dragger::before,
:root[data-theme="dark"] .maker-host .mc-dragger::after { background-color: #1E3A6E !important; }
/* 共性:element-plus 表格(操作配置 等)靠 CSS 变量整表深色 */
:root[data-theme="dark"] .maker-host .el-table {
  --el-table-bg-color: #0E224C !important;
  --el-table-tr-bg-color: #0E224C !important;
  --el-table-header-bg-color: #0A1733 !important;
  --el-table-border-color: #1E3A6E !important;
  --el-table-border: 1px solid #1E3A6E !important;
  --el-table-text-color: #C9D6EC !important;
  --el-table-header-text-color: #C9D6EC !important;
  --el-table-row-hover-bg-color: #143063 !important;
  background-color: transparent !important;
}
:root[data-theme="dark"] .maker-host .el-table th.el-table__cell,
:root[data-theme="dark"] .maker-host .el-table td.el-table__cell { background-color: var(--el-table-tr-bg-color) !important; }
/* vxe 表格兜底 */
:root[data-theme="dark"] .maker-host .vxe-table,
:root[data-theme="dark"] .maker-host .vxe-table--main-wrapper { background-color: #0E224C !important; color: #C9D6EC !important; }
:root[data-theme="dark"] .maker-host .vxe-table .vxe-header--column,
:root[data-theme="dark"] .maker-host .vxe-table .vxe-body--column,
:root[data-theme="dark"] .maker-host .vxe-table .vxe-cell { background-color: #0E224C !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .vxe-table .vxe-header--column { background-color: #0A1733 !important; }
/* 表格网格线 → 更深更淡(融入深底),并盖过 * border 兜底 */
:root[data-theme="dark"] .maker-host .el-table { --el-table-border-color: #33538C !important; --el-table-border: 1px solid #33538C !important; }
:root[data-theme="dark"] .maker-host .el-table .el-table__cell,
:root[data-theme="dark"] .maker-host .el-table td.el-table__cell,
:root[data-theme="dark"] .maker-host .el-table th.el-table__cell,
:root[data-theme="dark"] .maker-host .el-table--border .el-table__cell,
:root[data-theme="dark"] .maker-host .el-table--border::after,
:root[data-theme="dark"] .maker-host .el-table--border::before,
:root[data-theme="dark"] .maker-host .el-table::before,
:root[data-theme="dark"] .maker-host .el-table__border-left-patch,
:root[data-theme="dark"] .maker-host .vxe-table .vxe-header--column,
:root[data-theme="dark"] .maker-host .vxe-table .vxe-body--column,
:root[data-theme="dark"] .maker-host .vxe-table--border-line { border-color: #33538C !important; }
/* vxe 表格网格线是 background-image linear-gradient + --vxe-table-border-color 画的 → 改变量 */
:root[data-theme="dark"] .maker-host .vxe-table,
:root[data-theme="dark"] .maker-host .vxe-table--render-default {
  --vxe-table-border-color: #33538C !important;
  --vxe-ui-table-border-color: #33538C !important;
}
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
:root[data-theme="dark"] .maker-host .working-container {background: url("./images/dark-bg.png") repeat !important; }
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
:root[data-theme="dark"] .maker-host .top-header { border-color: #000 !important; }
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

:root[data-theme="dark"] .maker-host .right-setting-container [class*="btn-group"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="toggle"] { background: #0a1a3c !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container [class*="segment"] [class*="item"]:not([class*="color"]):not([class*="view"]):not([class*="active"]),
:root[data-theme="dark"] .maker-host .right-setting-container [class*="tab"] [class*="item"]:not([class*="color"]):not([class*="view"]):not([class*="active"]),
:root[data-theme="dark"] .maker-host .right-setting-container [class*="toggle"] [class*="item"]:not([class*="color"]):not([class*="view"]):not([class*="active"]),
:root[data-theme="dark"] .maker-host .right-setting-container [class*="btn-group"] > *:not([class*="color"]):not([class*="view"]):not([class*="active"]) { background: transparent !important; color: #C9D6EC !important; border-color: #1E3A6E !important; }
:root[data-theme="dark"] .maker-host .right-setting-container [class*="item"][class*="active"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="item"][class*="selected"],
:root[data-theme="dark"] .maker-host .right-setting-container [class*="active"][class*="item"] { 
  /* background: #2E74FF !important; */
   color: #fff !important; }
   :root[data-theme="dark"] .maker-host .right-setting-container [class*="tabs"]:not(.el-collapse),
:root[data-theme="dark"] .maker-host .right-setting-container [class*="tab-group"]{ background: #031e58 !important; border-bottom: 0px solid #023ead !important; }
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
:root[data-theme="dark"] .maker-host .right-setting-container.el-tabs__nav.is-top{
background: #0E224C !important;
}
:root[data-theme="dark"] .vxe-modal--wrapper.is--visible.is--mask:before {
    background-color: rgb(81 84 87 / 50%);
}
</style>
