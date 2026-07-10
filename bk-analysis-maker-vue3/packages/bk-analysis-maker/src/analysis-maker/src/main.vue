<template>
  <div
    style="height: 100%;padding: 0;"
    :class="[!isShowfull ? '' : 'has_full', 'analysis-maker-wrapper', showGridLine ? 'point-disable' : '']"
  >
    <Teleport :to="embedToolbarTarget || 'body'" :disabled="!embedToolbarTarget">
    <div
      v-if="!isModal"
      class="top-header"
      :class="[isModal ? 'query-header':'', embedToolbarTarget ? 'is-embed-tools' : '']"
    >
      <div
        v-if="!isModal"
        class="left"
        style="display: flex;align-items: center;"
      >
        <!-- <img
          src="./images/left-icon.png"
          style="width: 24px;height: 28px;margin-right: 12px;"
        > -->
        <div class="page-icon">
          <i-ri-file-chart-fill />
        </div>
        <span>{{ configNode.label }}</span>
      </div>
      <div
        v-if="!isModal"
        class="right"
      >
        <!-- 设计模式:工具按钮组(顺序严格按设计稿) -->
        <template v-if="designMode">
          <!-- 添加区域(split:区域/布局/页签/查询) -->
          <el-dropdown
            ref="addAreaDropdown"
            split-button
            size="small"
            trigger="click"
            class="add-area-dropdown"
            popper-class="add-area-dd"
            @click="addItem(true)"
          >
            <i-ri-add-line /><span style="margin-left:3px">添加区域</span>
            <template #dropdown>
              <div class="aac-panel">
                <div class="aac-group-name">布局</div>
                <div class="aac-grid">
                  <div
                    v-for="it in addAreaCards.layout"
                    :key="it.key"
                    class="aac-card"
                    @click="onAddCardClick(it)"
                  >
                    <img
                      :src="it.img"
                      :title="it.name"
                      class="aac-img"
                    >
                    <span class="aac-name">{{ it.name }}</span>
                  </div>
                </div>
                <div
                  v-if="addAreaCards.tpl.length"
                  class="aac-group-name"
                >模板</div>
                <div
                  v-if="addAreaCards.tpl.length"
                  class="aac-grid"
                >
                  <div
                    v-for="(it, i) in addAreaCards.tpl"
                    :key="'tpl' + i"
                    class="aac-card"
                    @click="onAddCardClick(it)"
                  >
                    <img
                      :src="it.img"
                      :title="it.name"
                      class="aac-img"
                    >
                    <span class="aac-name">{{ it.name }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-dropdown>
          <div class="text-button" @click="clearConfig">
            <i-ri-delete-bin-2-fill /><span>清空</span>
          </div>
          <div :class="['text-button', showBodyPannel && 'is-on']" @click="showLeftPanel">
            <i-ri-bar-chart-2-fill /><span>图表</span>
          </div>
          <!-- 图表配置按钮暂时隐藏(单击图表标题即可打开配置面板) -->
          <div
            v-if="false"
            :class="['fold-icon', (expand && !pageSettingVisible && !cardSettingVisible) ? 'active' : '']"
            title="图表配置"
            @click="toggleChartConfig"
          >
            <i-ri-pencil-ruler-2-fill /><span style="margin-left:4px">图表配置</span>
          </div>
          <div v-if="!isBasicMode" :class="['text-button', pageSettingVisible && 'is-on']" @click="openPageSetting">
            <i-ri-settings-5-fill /><span>页面设置</span>
          </div>
          <el-dropdown
            v-if="!isBasicMode"
            trigger="click"
            class="more-dropdown"
            @command="onMoreCommand"
          >
            <div class="text-button">
              <i-ri-more-2-fill /><span>更多</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="grid">
                  <i-ri-table-fill style="margin-right:6px" />栅格
                </el-dropdown-item>
                <el-dropdown-item command="vars">
                  <i-ri-file-settings-fill style="margin-right:6px" />全局参数
                </el-dropdown-item>
                <el-dropdown-item v-if="hasThemeConfigPermission" command="theme">
                  <i-ri-equalizer-fill style="margin-right:6px" />主题设置
                </el-dropdown-item>
                <el-dropdown-item v-if="hasCardConfigPermission" command="card">
                  <i-ri-equalizer-fill style="margin-right:6px" />卡片设置
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="tb-divider"></span>
          <!-- 画布缩放(仅设计模式):−/百分比/+/适应 -->
          <div class="canvas-zoom-ctrl">
            <span class="cz-btn" title="缩小" @click="zoomOut">−</span>
            <span class="cz-val" title="重置 100%" @click="zoomReset">{{ designScale }}%</span>
            <span class="cz-btn" title="放大" @click="zoomIn">+</span>
            <span class="cz-fit" title="适应窗口" @click="zoomFit">适应</span>
          </div>
          <el-switch
            v-if="setMode"
            v-model="autoSave"
            active-text="自动保存"
            class="am-switch"
            size="small"
          />
          <!-- 保存(split:主按钮=保存;下拉=保存 / 另存为,均带图标) -->
          <el-dropdown
            v-if="setMode"
            split-button
            type="primary"
            size="small"
            trigger="click"
            placement="bottom-end"
            class="save-dropdown"
            popper-class="save-dd-menu"
            @click="savePageConfig(false)"
            @command="onSaveCommand"
          >
            <i-ri-save-fill /><span style="margin-left:3px">保存</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="save"><i-ri-save-fill /><span>保存</span></el-dropdown-item>
                <el-dropdown-item command="saveAs"><i-ri-file-copy-2-fill /><span>另存为</span></el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="tb-divider"></span>
        </template>
        <!-- 模式切换(单按钮):显示「要切换过去」的目标模式,点击直接切换 -->
        <button
          v-if="setMode"
          type="button"
          class="mode-toggle"
          :title="designMode ? '切换到预览' : '切换到设计'"
          @click="onModeCommand(designMode ? 'preview' : 'design')"
        >
          <i-ri-eye-line v-if="designMode" />
          <i-ri-pencil-ruler-2-line v-else />
          <span>{{ designMode ? '预览' : '设计' }}</span>
        </button>
      </div>
    </div>
    </Teleport>
    <div
      style=" display: flex;flex: 1; overflow: hidden; position: relative;"
    >
      <div
        style="position: relative;display: flex;flex: 1; flex-direction: column;height: 100%;overflow: hidden;"
        direction="vertical"
      >
        <div
          v-if="!isModal"
          style="padding: 0;"
        >
          <Toolbar
            v-bind="$props"
            ref="leftToolBar"
            :custom-chart="customChart"
            :disabled="!setMode"
            :is-app="isApp"
            :layout-config="layoutConfig"
            @toggle-event="toggleEvent"
            @update-grid-setting="updateGridSetting"
            @clear-config="clearConfig"
          />
        </div>
        <div style="display: flex;flex: 1; overflow: hidden; position: relative;">
          <ToolbarPannel
            v-if="showBodyPannel && !isModal && designMode"
            ref="pannel"
            v-bind="{...$props}"
            :custom-chart="customChart"
            :disabled="!setMode"
            append-to-body
            :is-app="isApp"
            @click-item="toggleEvent"
            @hidden="showBodyPannel = false"
          />
          <div
            ref="workingContainer"
            :class="['working-container', (isModal || !designMode) ? 'no-background' : '']"
            :style="`padding: ${(isModal || !designMode) ? '0' : '30px 40px'}; height:  ${!layoutConfig.layout.length ? '100%' : 'auto'};${styleText}`"
            @scroll="handleScroll"
          >
            <!-- <div
              class="empty-area"
              v-if="!layoutConfig.layout.length"
            >
              <img
                src="./images/empty-area.png"
                alt=""
              >
              <span>添加布局或模板</span>
            </div> -->
            <div
              v-show="showLayout"
              ref="layoutWrapper"
              class="layout-wrapper"
              :style="{backgroundColor : 'var(--backgroundColor)',backgroundImage: 'var(--pageBackgroundbackgroundImage)',backgroundRepeat: 'var(--pageBackgroundbackgroundRepeat)',backgroundSize: 'var(--pageBackgroundbackgroundSize)', margin: '0 auto', width: layoutWrapperWidth, height: layoutWrapperHeight,
                       minWidth: layoutWrapperMinWidth, minHeight: layoutWrapperMinHeight, border: 'var(--bgBorderwidth) var(--bgBordertype) var(--bgBordercolor)' }"
            >
              <div
                class="layout-wrapper-content"
                :style="[{ padding: layoutWrapperPadding, color: 'var(--textStylecolor)', fontSize: 'var(--textStylefontSize)'
                          , fontFamily: 'var(--textStylefontFamily)', fontWeight: 'var(--textStylefontWeight)'}, canvasZoomStyle]"
              >
                <BKGridLayoutContent
                  v-if="!isThemeChange && showContent"
                  v-bind="$props"
                  ref="gridLayout"
                  :configs="layoutConfig"
                  :set-mode="editable"
                  :edit-mode="designMode"
                  :page-config="layoutConfig"
                  :custom-card-style-props="customCardStyleProps"
                  :selected-card-item="cardSettingVisible ? cardItem : null"
                  @remove-item="removeItem"
                  @cancel-set-card="cancelSetCard"
                />
              </div>
              <div
                class="decorate-container"
              >
                <BKGridLayoutDecorate
                  v-if="!isThemeChange && showContent"
                  v-bind="$props"
                  ref="gridLayoutDecorate"
                  :configs="layoutConfig"
                  :set-mode="editable"
                  :page-config="layoutConfig"
                  :decorate-config="decorateConfig"
                />
              </div>
              <div
                v-show="showGridLine"
                ref="gridLineContainer"
                class="grid-line-container"
              >
                <div
                  v-for="num in layoutConfig.colNum"
                  :key="num"
                />
              </div>
            </div>
            <!-- <SketchRule
              v-if="!isModal"
              ref="ruler"
              :width="rulerWidth"
              :height="rulerHeight"
              :thick="thick"
              :scale="1"
              :corner-active="true"
              :shadow="{}"
              class="am-ruler"
              :hor-line-arr="[]"
              :ver-line-arr="[]"
              :start-x="startX"
              :start-y="startY"
              :style="{height: rulerHeight + 'px',width: rulerWidth + 'px',top: rulerScrollTop + 'px',left: rulerScrollLeft + 'px'}"
            /> -->
          </div>
        </div>
      </div>
      <div
        v-if="!isModal && !isBasicMode"
        :class="['right-setting-container', panelPinned && 'is-pinned']"
        :style="{width: expand ? (dataMappingVisible && rightTab === 'data' ? '745px' : '500px') : '0' }"
      >
        <!-- <div class="expand-icon">
          <i
            :class="[expand ? 'el-icon-s-unfold' : 'el-icon-s-fold']"
            @click="setRightExpand()"
          />
        </div> -->
        <!-- 固定/浮动 切换:浮动=悬浮画布上,固定=右侧占位挤压画布 -->
        <div
          v-if="expand"
          :class="['cfg-pin-btn', panelPinned && 'is-on']"
          :title="panelPinned ? '取消固定(浮动)' : '固定到右侧'"
          @click="panelPinned = !panelPinned"
        >
          <i-ri-pushpin-2-fill v-if="panelPinned" />
          <i-ri-pushpin-2-line v-else />
        </div>
        <!-- 右侧面板关闭按钮:图表配置 / 页面设置 / 卡片设置 / 空态 通用,只要展开就显示,点击清空并收起 -->
        <div
          v-if="expand"
          class="cfg-close-btn"
          title="关闭"
          @click="closeRightPanel"
        >
          <i-ri-close-line />
        </div>
        <div
          v-if="!pageSettingVisible && !cardSettingVisible"
          v-show="expand"
          style="display: flex;height: 100%;overflow: auto;"
        >
          <component
            v-bind="$props"
            :is="getConfigName(configs)"
            v-if="configs && configs.type"
            :key="configs.chartId"
            :configs="configs"
            :page-config="layoutConfig"
            :save-able="setMode"
            :init-tab="rightTab"
            style="overflow: hidden;"
            @save-chart-cfg="saveChartCfg"
            @set-expand="setRightExpand(false)"
            @change-tab="rightTab = $event"
            @change-data-mapping-visible="dataMappingVisible = $event"
          />
          <div
            v-else
            style="display: flex;flex-direction: column;align-items: center;align-self: center;justify-content: center;width: 100%;overflow: hidden;"
          >
            <img
              src="./images/empty-config.png"
              alt=""
            >
            <span style="margin-top: 10px;">选择图表进行具体配置</span>
          </div>
        </div>
        <PageSetting
          v-else-if="pageSettingVisible"
          v-show="expand"
          ref="pageSetting"
          :oper-permission="operPermission"
          :theme="layoutConfig.themeConfigs"
          :disabled="!setMode"
          :is-app="isApp"
          @set-theme="setThemeMode"
          @change-card-radius="changeCardRadius"
          @change-space-mode="changeSpaceMode"
          @change-space="changeSpace"
          @change-page-width="changePageWidth"
          @change-page-padding="changePagePadding"
          @change-card-padding="changeCardPadding"
          @inner-padding-ref-change="innerPaddingRefChange"
          @change-pre-theme="changePreTheme"
          @change-page-height-mode="changePageHeightMode"
          @change-page-min-height="changePageMinHeight"
          @change-page-min-width="changePageMinWidth"
          @chart-style-change="chartStyleChange"
          @public-change-method="publicChangeMethod"
          @custom-script-blur="customScriptBlur"
          @custom-style-blur="customStyleBlur"
          @open-save-as="themeEditVisible=true"
        />
        <CardSetting
          v-else-if="cardSettingVisible"
          v-show="expand"
          ref="cardSetting"
          :theme="layoutConfig.themeConfigs"
          :disabled="!setMode"
          :card-item="cardItem"
          @change="changeCardSetting"
        />
      </div>
    </div>
    <VarConfig
      v-if="varConfigVisible"
      :visible="varConfigVisible"
      :set-mode="setMode"
      :configs="layoutConfig"
      @close="varConfigVisible=false"
      @save="saveVarConfig"
    />
    <OpenPageModal
      v-if="openPageModalVisible"
      v-bind="{...$props,...$attrs}"
      key="1"
      :visible="openPageModalVisible"
      :configs="openModalConfig"
      :map-source="mapSource"
      :map-path="mapPath"
      :is-basic-mode="isBasicMode"
      :tjb-u-r-l="tjbURL"
      @close="handleModalClose"
    />
    <PreviewPageModal
      v-if="previewVisible"
      v-bind="{...$props,...$attrs}"
      key="2"
      :visible="previewVisible"
      :configs="previewModalConfig"
      :map-source="mapSource"
      :tjb-u-r-l="tjbURL"
      :map-path="mapPath"
      :is-basic-mode="isBasicMode"
      :is-app="isApp"
      @preview-close="previewVisible=false"
    />
    <CardConfig
      v-if="cardConfigVisible"
      :visible="cardConfigVisible"
      @close="cardConfigVisible=false"
      @save="saveCardConfig"
      @refresh="refreshCardConfig"
    />
    <ThemeConfig
      v-if="themeConfigVisible"
      :visible="themeConfigVisible"
      @close="themeConfigVisible=false"
      @save="saveThemeConfig"
      @refresh="refreshThemeConfig"
    />
    <ThemeEdit
      v-if="themeEditVisible"
      :visible="themeEditVisible"
      :pre-theme="layoutConfig.themeConfigs"
      :oper-permission="operPermission"
      @close="themeEditVisible=false"
      @save="saveThemeConfig"
      @refresh="refreshThemeConfig"
    />
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import emitter from '../../configs/emitter'
import { defaultThemeList } from '../../configs/pre-page-setting'
import { chartDefaultConfig } from '../../configs/chart-default-config'
import { v4 as uuidv4 } from 'uuid'
import { allComponents, imgObject } from '../../configs/chart-com'
import { menuList as sidebarMenuList } from './configs'
import gridLayoutTemplates from '../../configs/grid-layout-cfg'
import VarConfig from './components/var-config.vue'
import OpenPageModal from './components/open-page-modal.vue'
import PreviewPageModal from './components/preview-page-modal.vue'
import PageSetting from './components/page-setting.vue'
import CardSetting from './components/card-setting.vue'
import ThemeEvents from './theme-events'
import Toolbar from './components/toolbar/index.vue'
import { getConfigTheme, getInitValue, hasPermission } from '../../configs/common-func'
import ToolbarPannel from './components/toolbar/pannel.vue'
import ThemeConfig from './components/theme-config.vue'
import ThemeEdit from './components/theme-edit.vue'
import CardConfig from './components/card-config.vue'
import { debounce } from 'throttle-debounce'
import SketchRule from 'vue3-sketch-ruler'
import { GridLayout, GridItem } from 'grid-layout-plus'
import { utils } from 'efficient-suite'
import { ElMessageBox } from 'element-plus'
export default {
  name: 'AnalysisMaker',
  components: {
    draggable,
    VarConfig,
    OpenPageModal,
    PageSetting,
    Toolbar,
    ToolbarPannel,
    PreviewPageModal,
    ThemeConfig,
    ThemeEdit,
    SketchRule,
    GridLayout,
    GridItem,
    CardSetting,
    CardConfig
  },
  mixins: [ThemeEvents],
  provide () {
    return {
      chartConfig: { setConfig: this.setConfig },
      getSaveAble: () => this.setMode,
      getPreThemeList: () => { return this.getThemeList() },
      getThemeConfig: (theme, mode, isAll) => { return this.getThemeConfig(theme, mode, isAll) },
      getThemeChartStyle: (theme, mode) => { return this.getThemeChartStyle(theme, mode) },
      setParams: (params, sync) => { return this.setParams(params, sync) },
      setQueryAreaParams: () => { this.setQueryAreaParams() }
    }
  },
  inheritAttrs: false,
  props: {
    setMode: {
      type: Boolean,
      default: true
    },
    // 宿主提供的工具栏插槽选择器(如 '#ixe-maker-tools');设置后顶栏 Teleport 到该处
    embedToolbarTarget: {
      type: String,
      default: ''
    },
    // 宿主"另存为探索布局"回调(保存下拉);未提供则回退 maker 自身另存
    embedOnSaveAs: {
      type: Function,
      default: null
    },
    pageConfig: {
      type: Object,
      default: () => null
    },
    tjbURL: {
      type: String,
      default: ''
    },
    customChart: {
      type: Array,
      default: () => []
    },
    isModal: {
      type: Boolean,
      default: false
    },
    configNode: {
      type: Object,
      default: () => { return { label: '' } }
    },
    autoSaveInterval: {
      type: Number,
      default: 1
    },
    chartMenuList: {
      type: Array,
      default: () => []
    },
    mapSource: {
      type: Array,
      default: () => []
    },
    mapPath: {
      type: String,
      default: '/map/'
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    preThemeList: {
      type: Array,
      default: () => []
    },
    operPermission: {
      type: Array,
      default: () => []
    },
    layoutTools: {
      type: Array,
      default: () => ['addItem']
    },
    defaultThemeKey: {
      type: String,
      default: ''
    },
    defaultParams: {
      type: Array,
      default: () => []
    },
    // 初始是否为设计模式(默认预览);宿主「新建看板」时置 true 直接进设计模式
    defaultDesignMode: {
      type: Boolean,
      default: false
    },
    // 嵌入时画布宽度自适应(铺满容器):默认主题强制 pageWidthMode=adaptive(不改 colNum)
    embedFluidWidth: {
      type: Boolean,
      default: false
    }
  },
  emits: ['invokeAMakerMethod'],
  data () {
    return {
      layoutConfig: {
        layout: [],
        decorateLayout: [],
        colNum: 12,
        rowHeight: 30,
        autoRowHeight: 30,
        draggable: true,
        resizable: true,
        maxRows: Infinity
      },
      decorateConfig: {
        decorateColNum: 36,
        decorateRowHeight: 1,
        decorateAutoRowHeight: 1,
        decorateMaxRows: Infinity,
        decorateMargin: [5, 5]
      },
      pageTitle: '页面标题',
      isThemeChange: false,
      editTitleVisible: false,
      configs: {},
      finalConfig: {},
      isShowfull: false,
      widthScale: '100%',
      heightScale: '100%',
      scaleVal: 100,
      scale: [70, 80, 90, 100, 130, 150, 200, 300],
      varConfigVisible: false,
      currentOpenPageConfig: null,
      openModalConfig: null,
      previewVisible: false,
      previewModalConfig: null,
      openPageModalVisible: false,
      pageSettingVisible: false,
      cardSettingVisible: false,
      cardItem: null,
      showBodyPannel: false,
      expand: false,
      showGridLine: false,
      designMode: this.defaultDesignMode, // 设计/预览模式,默认预览(可由 defaultDesignMode 初始为设计)
      rightTab: 'base',
      dataMappingVisible: false,
      autoSave: false,
      saveTime: '',
      showContent: true,
      openModalParams: [],
      styleText: '',
      cardConfigVisible: false,
      themeConfigVisible: false,
      themeEditVisible: false,
      rulerHeight: 0,
      rulerWidth: 0,
      startX: 0,
      startY: 0,
      thick: 16,
      rulerScrollLeft: 0,
      rulerScrollTop: 0,
      customCardStyleProps: {},
      // 看板设计模式画布无级缩放(独立于 maker 原生 scaleVal/isApp,预览恒 1:1)
      designScale: 100,
      // 右侧配置面板:false=浮层(悬浮画布上),true=固定(右侧占位,挤压画布)
      panelPinned: false
    }
  },
  computed: {
    // 「添加区域」下拉卡片:布局(区域/布局/页签/查询)+ 模板(左右/上下),形式同左侧工具面板
    addAreaCards () {
      const layoutGroup = (sidebarMenuList.find(m => m.key === 'layout')?.children) || []
      const layout = layoutGroup.map(c => ({ name: c.name, key: c.key, payload: c.payload, img: c.img }))
      const tpl = (gridLayoutTemplates || []).map(item => ({
        name: item.title, key: 'setLayout', payload: item.configs, img: item.img ? imgObject[item.img] : ''
      }))
      return { layout, tpl }
    },
    layoutWrapperWidth () {
      if (this.layoutConfig?.themeConfigs?.pageLayout?.pageWidthMode === 'custom' && this.layoutConfig?.themeConfigs?.pageLayout?.pageWidth) {
        if (this.widthScale !== '100%') {
          return (this.layoutConfig?.themeConfigs?.pageLayout?.pageWidth * (this.scaleVal / 100)) + 'px'
        } else {
          return this.layoutConfig?.themeConfigs?.pageLayout?.pageWidth + 'px'
        }
      } else {
        if (this.widthScale !== '100%') {
          return this.widthScale
        }
        return '100%'
      }
    },
    layoutWrapperMinWidth () {
      if (this.isApp) {
        return '0'
      }
      if (this.layoutConfig?.themeConfigs?.pageLayout?.pageWidthMode === 'adaptive' && this.layoutConfig?.themeConfigs?.pageLayout?.pageMinWidth) {
        return this.layoutConfig?.themeConfigs?.pageLayout?.pageMinWidth + 'px'
      } else {
        return '0'
      }
    },
    layoutWrapperHeight () {
      if (this.layoutConfig?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive') {
        if (this.heightScale !== '100%') {
          return this.heightScale
        }
        return '100%'
      } else {
        return 'auto'
      }
    },
    layoutWrapperMinHeight () {
      if (this.layoutConfig?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive' && this.layoutConfig?.themeConfigs?.pageLayout?.pageMinHeight) {
        return this.layoutConfig?.themeConfigs?.pageLayout?.pageMinHeight + 'px'
      } else {
        return '0'
      }
    },
    layoutWrapperPadding () {
      if (this.layoutConfig?.themeConfigs?.pageLayout?.pagePadding) {
        const { paddingTop, paddingBottom, paddingLeft, paddingRight } = this.layoutConfig.themeConfigs.pageLayout.pagePadding
        return `${paddingTop}px ${paddingRight}px ${paddingBottom}px ${paddingLeft}px`
      } else {
        return '0'
      }
    },
    showLayout () {
      return this.layoutConfig.layout.length || this.layoutConfig?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive'
    },
    chartTheme () {
      return (this.layoutConfig?.themeConfigs?.preTheme || '') + ',' + (this.layoutConfig?.themeConfigs?.globalCss?.themeMode || '')
    },
    activeText () {
      return '自动保存' + ((this.autoSave && this.saveTime) ? '(已实时保存' + this.saveTime + ')' : '')
    },
    hasThemeConfigPermission () {
      return hasPermission(this.operPermission, 'theme.edit')
    },
    hasCardConfigPermission () {
      return hasPermission(this.operPermission, 'card.config')
    },
    isApp () {
      if (this.configNode) {
        return this.configNode.CLIENT_TP === '1'
      }
      return false
    },
    // 画布是否可编辑:仅「设计模式」可编辑;「预览模式」只读(无拖拽/关闭/hover 编辑/网格背景)
    editable () {
      return this.setMode && this.designMode
    },
    // 看板画布缩放比例:仅设计模式生效,预览恒 1:1(不影响预览界面)
    canvasScale () {
      return this.designMode ? (this.designScale / 100) : 1
    },
    // 用 CSS zoom 缩放画布:zoom 参与真实布局,滚动条自动正确;Chrome 下拖拽命中一致。预览恒不缩放
    canvasZoomStyle () {
      if (this.canvasScale === 1) return {}
      return { zoom: this.canvasScale }
    }
  },
  watch: {
    scaleVal () {
      this.widthScale = this.scaleVal + '%'
      this.heightScale = this.scaleVal + '%'
    },
    // 进入预览模式:强制关闭右侧图表配置/页面设置面板(预览纯只读,不出配置面板)
    designMode (v) {
      if (!v) {
        this.pageSettingVisible = false
        this.cardSettingVisible = false
        this.configs = {}
        this.setRightExpand(false)
      }
    },
    pageConfig: {
      handler () {
        this.debouncedInit ? this.debouncedInit(false, true) : this.initPage(false, true)
      },
      immediate: true
    },
    pageSettingVisible () {
      if (this.pageSettingVisible) {
        this.cardSettingVisible = false
      }
    },
    cardSettingVisible () {
      if (this.cardSettingVisible) {
        this.pageSettingVisible = false
      }
    },
    configNode () {
      this.debouncedInit()
    },
    layoutConfig: {
      immediate: true,
      handler (v) {
        this.$nextTick(() => {
          if (this.layoutConfig.themeConfigs) {
            window.editorCurrentTheme = this.layoutConfig.themeConfigs.globalCss.themeMode === 'default-theme' ? 'vs' : 'vs-dark'
            this.setTheme(this.layoutConfig.themeConfigs.globalCss.themeMode)
            // 设置卡片圆角
            this.changeCardRadius(this.layoutConfig.themeConfigs.globalCss.cardRadius)
          }
        })
      }
    },
    'layoutConfig.colNum' () {
      const queryItem = this.layoutConfig.layout.find(c => c.isQuery)
      if (queryItem) {
        queryItem.w = this.layoutConfig.colNum
      }
    },
    'layoutConfig.rowHeight' () {
      const queryItem = this.layoutConfig.layout.find(c => c.isQuery)
      if (queryItem) {
        queryItem.h = 50 / this.layoutConfig.rowHeight
      }
    },
    autoSave () {
      this.handleAutoSave()
    }
  },
  created () {
    this.debouncedInit = debounce(50, (clear, init) => {
      this.initPage(clear, init)
    })

    emitter.on('setParams', this.setParams)
    emitter.on('openItemStyle', this.openItemStyle)
    emitter.on('openPageModal', this.openPageModal)
    emitter.on('openPageConfig', this.openPageConfig)
    emitter.on('openTjbConfig', this.openTjbConfig)
    emitter.on('chartClick', this.handleChartClick)
    emitter.on('pegToolbarPannel', this.pegToolbarPannel)

    emitter.on('chartThemeChange', this.chartThemeChange)
    emitter.on('toggleGridLine', this.toggleGridLine)
    document.addEventListener('keydown', this.handleKeydown)
  },
  mounted () {
    this.initResizeObserver()
    if (this.isBasicMode) {
      this.showBodyPannel = true
    }
  },
  beforeUnmount () {
    if (this.timer) {
      clearInterval(this.timer)
    }
    if (this.scriptNode) {
      document.body.removeChild(this.scriptNode)
      this.scriptNode = null
    }
    if (this.styleNode) {
      document.body.removeChild(this.styleNode)
      this.styleNode = null
    }
    emitter.off('chartClick', this.handleChartClick)
    emitter.off('openPageConfig', this.openPageConfig)
    emitter.off('openTjbConfig', this.openTjbConfig)
    emitter.off('setParams', this.setParams)
    emitter.off('openItemStyle', this.openItemStyle)
    emitter.off('openPageModal', this.openPageModal)
    emitter.off('pegToolbarPannel', this.pegToolbarPannel)
    emitter.off('toggleGridLine', this.toggleGridLine)
    emitter.off('chartThemeChange', this.chartThemeChange)
    this.resizeObserver && this.resizeObserver.unobserve(this.$refs.layoutWrapper)
    document.removeEventListener('keydown', this.handleKeydown)
  },
  methods: {
    initPage (isClear = false, isInit = false) {
      window['$amObject' + this.configNode?.id] = this
      this.customCardStyleProps = {}
      this.showContent = false
      this.saveTime = ''
      this.showGridLine = false
      this.pageSettingVisible = false
      this.cardSettingVisible = false
      emitter.emit('chartClick', { configs: {}, expand: true })
      this.scaleVal = 100
      this.$refs.leftToolBar && (this.$refs.leftToolBar.showGridSetting = false)
      if (this.pageConfig && !isClear) {
        const newConfig = this.checkConfig(this.pageConfig)
        this.layoutConfig = {
          layout: [],
          decorateLayout: [],
          margin: this.getDefaultTheme().config.globalCss.margin,
          ...newConfig,
          draggable: this.setMode,
          resizable: this.setMode,
          themeConfigs: this.initThemeConfigs(this.pageConfig.themeConfigs)
        }
      } else if (this.pageConfig && isClear) {
        const newConfig = this.checkConfig(this.pageConfig)
        this.layoutConfig = {
          ...newConfig,
          layout: [],
          decorateLayout: [],
          draggable: this.setMode,
          resizable: this.setMode
        }
      } else {
        this.layoutConfig = {
          layout: [],
          decorateLayout: [],
          colNum: this.isApp ? 1 : 12,
          rowHeight: 30,
          autoRowHeight: 30,
          margin: this.getDefaultTheme().config.globalCss.margin,
          draggable: this.setMode,
          resizable: this.setMode,
          maxRows: Infinity,
          themeConfigs: this.initThemeConfigs()
        }
      }
      this.configs = {}
      this.setRightExpand(false)
      this.handleAutoSave()
      this.createScript()
      this.createStyle()
      setTimeout(() => {
        if (this.layoutConfig?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive') {
          if (this.layoutConfig.maxRows === Infinity) {
            this.layoutConfig.maxRows = this.calculateMaxRows()
          }
          if (this.decorateConfig.decorateMaxRows === Infinity) {
            this.decorateConfig.decorateMaxRows = this.calculateDecorateMaxRows()
          }
          this.listenerContainerHeightCallBack()
        }
        this.showContent = true
        this.$nextTick(() => {
          if (!this.isModal) {
            this.$refs.workingContainer.scrollTop = 0
            this.$refs.workingContainer.scrollLeft = 0
          }
        })
      }, 10)
      this.finalConfig = {}
      if (isInit) {
        this.originConfig = JSON.stringify(this.layoutConfig)
      }
      this.$refs.pannel && this.$refs.pannel.initMenu()
      this.$nextTick(() => {
        if (this.defaultParams && this.defaultParams.length > 0) {
          this.setParams(this.defaultParams, true)
        }
      })
    },
    saveThemeConfig (config, callBack) {
      this.$emit('invokeAMakerMethod', 'saveThemeConfig', config, () => {
        callBack()
      })
    },
    refreshThemeConfig () {
      this.debouncedInit()
    },
    saveCardConfig (config, callBack) {
      this.$emit('invokeAMakerMethod', 'saveCardConfig', config, () => {
        callBack()
      })
    },
    refreshCardConfig () {
      this.$emit('invokeAMakerMethod', 'refreshPage')
    },

    getThemeList () {
      if (this.preThemeList && this.preThemeList.length > 0) {
        return this.preThemeList
      }
      return defaultThemeList
    },
    getDefaultTheme (isAll = false) {
      const list = this.getThemeList()
      let defaultTheme = (this.defaultThemeKey && list.find(c => c.key === this.defaultThemeKey)) || list.find(c => c.isDefault === '1')
      if (!defaultTheme) {
        defaultTheme = list[0]
      }
      const theme = utils.deepClone(defaultTheme)
      const mode = theme.config.themeList.find(c => c.value === theme.config.themeMode).config
      for (let i = 0; i < theme.config.themeList.length; i++) {
        delete theme.config.themeList[i].config
      }
      if (!isAll) {
        delete mode.chartStyle
      }
      theme.config = { ...theme.config, ...mode }
      return theme
    },
    createScript () {
      if (this.scriptNode) {
        document.body.removeChild(this.scriptNode)
        this.scriptNode = null
      }
      if (this.layoutConfig?.themeConfigs?.customScript) {
        const script = document.createElement('script')
        script.type = 'text/javascript'
        script.text = this.layoutConfig.themeConfigs.customScript
        this.scriptNode = script
        document.body.appendChild(script)
      }
    },
    createStyle () {
      if (this.styleNode) {
        document.body.removeChild(this.styleNode)
        this.styleNode = null
      }
      if (this.layoutConfig?.themeConfigs?.globalCustomStyle) {
        const style = document.createElement('style')
        // style.type = 'text/css'
        const mode = this.layoutConfig.themeConfigs.globalCss.themeMode
        // const className = this.layoutConfig.themeConfigs.themeList.find(c => c.value === mode)
        const cssName = `am-theme-${mode}`
        let text = this.layoutConfig.themeConfigs.globalCustomStyle
        if (cssName) {
          text = `.${cssName} {${text}}`
        }
        style.innerHTML = text
        this.styleNode = style
        document.body.appendChild(style)
      }
    },
    handleKeydown () {
      if (event.ctrlKey && event.key === 's') {
        if (this.setMode && !this.isModal) {
          this.savePageConfig(false)
        }
        event.preventDefault()
      }
    },
    handleAutoSave () {
      if (this.timer) {
        clearInterval(this.timer)
      }
      if (this.autoSave && !this.isModal && this.setMode) {
        const timer = setInterval(() => {
          if (this.autoSave && !this.isModal && this.setMode) {
            this.savePageConfig(true)
          }
        }, this.autoSaveInterval * 60 * 1000)
        this.timer = timer
        // this.$once('hook:beforeDestroy', () => {
        //   clearInterval(timer)
        // })
      }
    },
    getConfigName (cfg) {
      return cfg.isBasic ? 'BKBasicChartConfig' : (cfg.type + 'Config')
    },
    openItemStyle (item) {
      emitter.emit('chartClick', { configs: {}, expand: true })
      if (!item.cardStyle) {
        item.cardStyle = { isCustom: false }
      }
      this.cardItem = item
      this.cardSettingVisible = true
    },
    cancelSetCard () {
      this.cardItem = null
      this.cardSettingVisible = false
      this.setRightExpand(false)
    },
    setQueryAreaParams () {
      if (this.defaultParams && this.defaultParams.length > 0) {
        if (this.layoutConfig.varConfig) {
          const { varParamList } = this.getParamsList(this.defaultParams, false)
          if (varParamList.length > 0) {
            emitter.emit('paramsChangeForQuery', { params: varParamList, pageConfig: this.pageConfig })
          }
        }
      }
    },
    setParams (params, sync = false) {
      if (this.layoutConfig.varConfig) {
        if (!Array.isArray(params)) {
          params = [params]
        }
        const { varParamList, openModal } = this.getParamsList(params, true)
        if (varParamList.length > 0) {
          emitter.emit('paramsChange', { params: varParamList, pageConfig: this.pageConfig })
          if (sync) {
            emitter.emit('paramsChangeForQuery', { params: varParamList, pageConfig: this.pageConfig })
          }
        }
        if (openModal) {
          this.openPageModal(openModal)
        }
      }
    },
    getParamsList (params, isAll) {
      if (isAll) {
        this.openModalParams = []
      }
      let openModal = null
      const varParamList = []
      for (let i = 0; i < params.length; i++) {
        const param = params[i]
        const varParam = this.layoutConfig.varConfig?.find(c => c.id === param.id || c.name === param.name)
        if (varParam) {
          varParam.value = param.value
          if (varParam.changeType === 'openModal') {
            if (isAll) {
              this.openModalParams.push(varParam)
            }
            if (varParam.openPage) {
              openModal = varParam.openPage
            }
          } else if (varParam.changeType === 'refreshData') {
            varParamList.push(varParam)
          }
        }
      }
      return { varParamList, openModal }
    },
    openPageModal (openPage) {
      if (this.openPageModalVisible) {
        return
      }
      this.$emit('invokeAMakerMethod', 'getOpenPageConfig', openPage, (config) => {
        if (config) {
          this.openModalConfig = { config, openPage, varConfig: this.layoutConfig.varConfig || [] }
          this.openPageModalVisible = true
        } else {
          console.log('配置界面不完整')
        }
      })
    },
    handleModalClose () {
      this.openPageModalVisible = false
      if (this.openModalParams.length > 0) {
        for (let i = 0; i < this.openModalParams.length; i++) {
          const varCfg = this.layoutConfig.varConfig?.find(c => c.id === this.openModalParams[i].id)
          if (varCfg) {
            varCfg.value = getInitValue(varCfg.initValue)
          }
        }
        this.openModalParams = []
      }
    },
    setLayout (cfg) {
      const config = utils.deepClone(cfg)
      if (config.layout) {
        this.initItem(config.layout)
      }
      // for (let i = 0; i < config.layout.length; i++) {
      //   config.layout[i].i = uuidv4()
      //   config.layout[i].id = config.layout[i].i
      // }
      if (this.layoutConfig.layout.length > 0) {
        ElMessageBox.confirm('确认替换当前布局吗？').then(() => {
          this.layoutConfig = { ...this.layoutConfig, ...config }
          this.handleChartClick({ configs: {}, expand: false })
        }).catch(() => {})
      } else {
        this.layoutConfig = { ...this.layoutConfig, ...config }
      }
    },
    initItem (data) {
      for (let i = 0; i < data.length; i++) {
        data[i].i = uuidv4()
        data[i].id = data[i].i
        if (data[i].layout) {
          this.initItem(data[i].layout)
        }
        if (data[i].tabList) {
          this.initItem(data[i].tabList)
        }
      }
    },
    setTheme (theme) {
      if (!this.$refs.layoutWrapper) return
      const list = []
      this.$refs.layoutWrapper.classList.forEach(c => {
        if (c.startsWith('am-theme-')) {
          list.push(c)
        }
      })
      for (let i = 0; i < list.length; i++) {
        if (this.$refs.layoutWrapper.classList.contains(list[i])) {
          this.$refs.layoutWrapper.classList.remove(list[i])
        }
      }
      this.$refs.layoutWrapper.classList.add(`am-theme-${theme}`)
      this.getStyleText(['globalCss'])
      this.createScript()
      this.createStyle()
    },
    initResizeObserver () {
      this.resizeObserver = new ResizeObserver(this.listenerContainerHeightCallBack)
      this.resizeObserver.observe(this.$refs.layoutWrapper)
    },
    listenerContainerHeightCallBack (list) {
      let height = null
      if (list && list[0]) {
        height = list[0].contentRect.height
      } else {
        // if (!this.$refs.layoutWrapper) {
        //   return
        // }
        height = this.$refs.layoutWrapper.clientHeight
      }
      if (this.layoutConfig?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive') {
        const paddingBottom = this.layoutConfig?.themeConfigs?.pageLayout?.pagePadding?.paddingBottom || 0
        const paddingTop = this.layoutConfig?.themeConfigs?.pageLayout?.pagePadding?.paddingTop || 0
        if (this.layoutConfig.maxRows) {
          this.layoutConfig = Object.assign({}, this.layoutConfig, {
            autoRowHeight: ((height - paddingBottom - paddingTop) / this.layoutConfig.maxRows) -
          this.layoutConfig.margin[1]
          })
        }
        if (this.decorateConfig.decorateMaxRows) {
          this.decorateConfig.decorateAutoRowHeight = ((height - paddingBottom - paddingTop) / this.decorateConfig.decorateMaxRows) -
          this.decorateConfig.decorateMargin[1]
        }
      } else {
        if (this.setMode && !this.isModal) {
          const decorateMaxRows = this.calculateDecorateMaxRows()
          const decorateLayout = this.layoutConfig.decorateLayout.filter(item => (item.y + item.h) <= decorateMaxRows)
          this.layoutConfig.decorateLayout = decorateLayout
          this.decorateConfig.decorateMaxRows = decorateMaxRows
        }
      }
      const cHeight = this.$refs.workingContainer.clientHeight
      const cWidth = this.$refs.workingContainer.clientWidth
      this.rulerHeight = cHeight
      this.rulerWidth = cWidth
    },
    handleScroll () {
      this.rulerScrollTop = this.$refs.workingContainer.scrollTop
      this.rulerScrollLeft = this.$refs.workingContainer.scrollLeft
      // 标尺开始的刻度
      const startX = (this.rulerScrollLeft + this.thick)
      const startY = (this.rulerScrollTop + this.thick)
      this.startX = startX
      this.startY = startY
    },
    saveVarConfig (cfg) {
      this.layoutConfig.varConfig = cfg
      if (cfg.find(c => c.type === 'external' && c.isQuery === '1' && c.queryConfig)) {
        if (!this.layoutConfig.layout.find(c => c.isQuery)) {
          ElMessageBox.confirm('是否添加查询区域？').then(() => {
            this.addQuery()
          }).catch(() => {})
        }
      } else {
        const item = this.layoutConfig.layout.find(c => c.isQuery)
        if (item) {
          ElMessageBox.confirm('是否删除查询区域？').then(() => {
            this.$refs.gridLayout && this.$refs.gridLayout.removeItem(item.i, true)
          }).catch(() => {})
        }
      }
      emitter.emit('varConfigChange', cfg)
    },
    addItem (isChart) {
      this.$refs.gridLayout && this.$refs.gridLayout.addItem(isChart)
    },
    addQuery () {
      this.$refs.gridLayout && this.$refs.gridLayout.addQuery()
    },
    addLayout (isChart) {
      this.addItem(isChart)
    },
    addTabLayout () {
      this.$refs.gridLayout && this.$refs.gridLayout.addTabLayout()
    },
    setConfig (id, data) {
      this.finalConfig[id] = data
    },
    removeItem (item) {
      if (this.configs.id === item.id || (item.layout && item.layout.find(c => c.id === this.configs.id))) {
        this.handleChartClick({ configs: {}, expand: false })
      }
    },
    handleChartClick ({ configs, expand }) {
      if (this.isModal || !this.designMode) {
        return
      }
      // 面板已打开且已选中图表 → 切换图表时保持当前 tab(数据源/高级 不跳回基础);否则(首次/重新打开)回到基础
      const panelWasOpen = this.expand && this.configs && this.configs.type
      this.pageSettingVisible = false
      this.cardSettingVisible = false
      if (!panelWasOpen) this.rightTab = 'base'
      this.dataMappingVisible = false
      this.setRightExpand(expand)
      this.configs = configs
      configs.hookId && console.log('hookId', configs.hookId)
    },
    saveChartCfg (configs) {
      this.configs = configs
      this.pageSettingVisible = false
      this.cardSettingVisible = false
      emitter.emit('saveChartCfg', configs)
    },
    async isChange () {
      if (!this.pageConfig) {
        return false
      }
      emitter.emit('savePage')
      await this.$nextTick()
      const data = utils.deepClone(this.layoutConfig)
      if (data.varConfig) {
        for (let i = 0; i < data.varConfig.length; i++) {
          const varc = data.varConfig[i]
          varc.value = ''
        }
      }
      const originData = JSON.parse(this.originConfig)
      if (originData?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive') {
        delete originData.autoRowHeight
        delete originData.maxRows
      }
      if (data?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive') {
        delete data.autoRowHeight
        delete data.maxRows
      }
      return JSON.stringify(originData) !== JSON.stringify(data)
    },
    savePageConfig (isAuto) {
      emitter.emit('savePage')
      this.$nextTick(() => {
        const data = utils.deepClone(this.layoutConfig)
        if (data.varConfig) {
          for (let i = 0; i < data.varConfig.length; i++) {
            const varc = data.varConfig[i]
            varc.value = ''
          }
        }
        this.originConfig = JSON.stringify(data)
        this.setCfg(data)
        console.log(data, this.finalConfig)
        const oriDara = utils.deepClone(data)
        this.setOriginCfg(oriDara, this.finalConfig)
        console.log(oriDara)
        this.$emit('invokeAMakerMethod', 'savePageConfig', data, this.finalConfig, isAuto, () => {
          if (isAuto) {
            this.saveTime = utils.createDate(new Date()).format('YYYY-MM-DD HH:mm:ss')
          }
        })
      })
    },
    setCfg (data, list) {
      if (data.tabList && !data.isTabLayout) {
        delete data.tabList
      }
      if (list) {
        const cfg = list[data.id]
        if (cfg && cfg.length > 0) {
          data.tabList = utils.deepClone(cfg)
          data.tabList.sort((a, b) => {
            return a.tabord - b.tabord
          })
        }
      }
      if (data.layout) {
        for (let i = 0; i < data.layout.length; i++) {
          this.setCfg(data.layout[i], list)
        }
      }
      if (data.tabList && data.isTabLayout) {
        for (let i = 0; i < data.tabList.length; i++) {
          data.tabList[i].isEdit = false
          this.setCfg(data.tabList[i], list)
        }
      }
    },
    setOriginCfg (data, configList) {
      const cfg = configList[data.id]
      if (cfg && cfg.length > 0) {
        data.tabList = cfg
      }
      if (data.layout) {
        for (let i = 0; i < data.layout.length; i++) {
          this.setOriginCfg(data.layout[i], configList)
        }
      }
      if (data.tabList && data.isTabLayout) {
        for (let i = 0; i < data.tabList.length; i++) {
          this.setOriginCfg(data.tabList[i], configList)
        }
      }
    },
    openPageConfig (config) {
      this.currentOpenPageConfig = config
      this.$emit('invokeAMakerMethod', 'handleOpenPageConfig', config)
    },
    saveOpenPageConfig (page) {
      emitter.emit('saveOpenPageConfig', { config: this.currentOpenPageConfig, page })
    },
    openTjbConfig (config) {
      this.$emit('invokeAMakerMethod', 'openTjbConfig', config)
    },
    saveTjbConfig (config) {
      emitter.emit('saveTjbConfig', config)
    },
    toChangeFull () {
      this.isShowfull = !this.isShowfull
    },
    toggleDesignMode () {
      this.designMode = !this.designMode
    },
    onMoreCommand (cmd) {
      if (cmd === 'grid') this.showGridSetting()
      else if (cmd === 'vars') this.showVarConfigVisible()
      else if (cmd === 'theme') this.themeConfigVisible = true
      else if (cmd === 'card') this.cardConfigVisible = true
    },
    onAddCommand (cmd) {
      if (cmd === 'addItem') this.addItem(true)
      else if (cmd === 'addLayout') this.addLayout(false)
      else if (cmd === 'addTabLayout') this.addTabLayout()
      else if (cmd === 'addQuery') this.addQuery()
    },
    // 「添加区域」下拉卡片点击:复用左侧工具面板的 toggleEvent(key → 对应方法),然后关闭下拉
    onAddCardClick (it) {
      this.toggleEvent({ key: it.key, payload: it.payload })
      this.$refs.addAreaDropdown && this.$refs.addAreaDropdown.handleClose && this.$refs.addAreaDropdown.handleClose()
    },
    onSaveCommand (cmd) {
      if (cmd === 'save') {
        this.savePageConfig(false)
      } else if (cmd === 'saveAs') {
        if (this.embedOnSaveAs) this.embedOnSaveAs()
        else this.savePageConfig(true)
      }
    },
    onModeCommand (cmd) {
      this.designMode = (cmd === 'design')
    },
    openPageSetting () {
      // 再次点击关闭:收起整个右侧面板,不要回落到空的「图表配置」占位
      if (this.pageSettingVisible) { this.pageSettingVisible = false; this.setRightExpand(false); return }
      emitter.emit('chartClick', { configs: {}, expand: true })
      this.pageSettingVisible = true
    },
    toggleEvent ({ key, payload }) {
      this[key] && this[key](payload)
    },
    addDecorate (config) {
      if (this.layoutConfig.layout.length === 0) {
        return
      }
      this.$refs.gridLayoutDecorate && this.$refs.gridLayoutDecorate.addItem(config)
    },
    showVarConfigVisible () {
      this.varConfigVisible = true
    },
    updateScale (v) {
      this.scaleVal = parseInt(v)
    },
    pegToolbarPannel () {
      this.showBodyPannel = true
    },
    previewPage () {
      emitter.emit('savePage')
      this.$nextTick(() => {
        const data = utils.deepClone(this.layoutConfig)
        this.setCfg(data, this.finalConfig)
        this.previewModalConfig = { config: data, openPage: { name: '预览' }, varConfig: this.layoutConfig.varConfig }
        this.previewVisible = true
      })
    },
    setRightExpand (value) {
      if (typeof value !== 'boolean') {
        this.expand = !this.expand
      } else {
        this.expand = value
      }
      // 面板收起时通知图表卡片重置选中态,否则再次单击同一图表打不开配置面板
      if (!this.expand) {
        emitter.emit('resetChartFocus')
      }
    },
    // —— 画布缩放(仅设计模式)——
    clampScale (v) { return Math.max(25, Math.min(200, Math.round(v))) },
    zoomIn () { this.designScale = this.clampScale(this.designScale + 10) },
    zoomOut () { this.designScale = this.clampScale(this.designScale - 10) },
    zoomReset () { this.designScale = 100 },
    // 适应窗口:按内容宽/高相对可视区取较小比例,使整页尽量塞进 working-container
    zoomFit () {
      const wc = this.$refs.workingContainer
      const lw = this.$refs.layoutWrapper
      if (!wc || !lw) { this.designScale = 100; return }
      const cur = this.canvasScale || 1
      // lw 当前已被缩放,先还原成原始尺寸再算
      const naturalW = lw.offsetWidth / cur
      const naturalH = lw.offsetHeight / cur
      if (!naturalW || !naturalH) { this.designScale = 100; return }
      const availW = wc.clientWidth - 32
      const availH = wc.clientHeight - 32
      const ratio = Math.min(availW / naturalW, availH / naturalH) * 100
      this.designScale = this.clampScale(ratio)
    },
    // 关闭右侧面板(图表配置/页面设置/卡片设置通用):清空并收起
    closeRightPanel () {
      this.pageSettingVisible = false
      this.cardSettingVisible = false
      this.configs = {}
      this.setRightExpand(false)
    },
    // 「图表配置」按钮:只控制右侧图表配置面板,不影响「页面设置」面板
    // 已在图表配置态 → 收起;否则 → 关掉页面设置/卡片设置并打开图表配置
    toggleChartConfig () {
      if (this.expand && !this.pageSettingVisible && !this.cardSettingVisible) {
        this.setRightExpand(false)
      } else {
        this.pageSettingVisible = false
        this.cardSettingVisible = false
        this.setRightExpand(true)
      }
    },
    toggleGridLine (value) {
      this.showGridLine = value
      this.$refs.gridLineContainer.style.gap = `${this.layoutConfig.margin ? this.layoutConfig.margin[1] : 10}px`
    },
    updateGridSetting (event) {
      Object.assign(this.layoutConfig, event)
    },
    showLeftPanel () {
      this.showBodyPannel = !this.showBodyPannel
    },
    showGridSetting () {
      this.$refs.leftToolBar.openGridSetting()
    },
    clearConfig () {
      ElMessageBox.confirm('确认清空所有配置？').then(() => {
        this.initPage(true)
      }).catch(() => {})
    },
    checkConfig (config) {
      const newConfig = utils.deepClone(config)
      if (newConfig.layout) {
        this.syncConfig(newConfig.layout)
      }
      return newConfig
    },
    syncConfig (layout) {
      const customCardStyleProps = {}
      for (let i = 0; i < layout.length; i++) {
        const tabList = layout[i].tabList
        if (tabList) {
          for (let j = 0; j < tabList.length; j++) {
            const tab = tabList[j]
            this.syncEventConfig(tab) // 同步事件
            this.syncOptionConfig(tab) // 同步配置
          }
        }
        customCardStyleProps[layout[i].id] = this.customCardStyle(layout[i])
        if (layout[i].layout) {
          this.syncConfig(layout[i].layout)
        }
        if (layout[i].isTabLayout && layout[i].tabList) {
          this.syncConfig(layout[i].tabList)
        }
      }
      this.customCardStyleProps = customCardStyleProps
    },
    syncOptionConfig (tab) {
      const defaultOption = chartDefaultConfig.get(tab?.branchType || tab.type)
      if (defaultOption) {
        if (tab?.configOption) {
          const option = this.deepTraversal(tab?.configOption, defaultOption)
          tab.configOption = option
        } else {
          tab.configOption = defaultOption
        }
      }
    },
    customCardStyle (item) {
      const style = {}
      if (item.cardStyle && item.cardStyle.isCustom && item.cardStyle.config) {
        for (const key in item.cardStyle.config) {
          const data = item.cardStyle.config[key]
          if (data) {
            if (key === 'cardBackground') {
              if (data.backgroundImage) {
                style.backgroundImage = 'url(' + data.backgroundImage + ')'
                style.backgroundRepeat = data.backgroundRepeat
                if (data.backgroundRepeat === 'no-repeat') {
                  style.backgroundSize = '100% 100%'
                }
              }
            } else if (key === 'cardStyle') {
              style.border = data.width + 'px ' + data.type + ' ' + data.color
            } else if (key === 'cardShadow') {
              style.boxShadow = data.shadowOffsetX + 'px ' + data.shadowOffsetY + 'px ' + data.shadowBlur + 'px ' + ' ' + data.shadowColor
            } else if (key === 'cardBackgroundColor') {
              style.backgroundColor = data
            } else if (key === 'cardRadius') {
              let radius = '0'
              if (data === 'small') {
                radius = '8px'
              } else if (data === 'big') {
                radius = '20px'
              }
              style.borderRadius = radius
            }
          }
        }
      }
      return style
    },
    changeCardSetting () {
      if (this.cardItem) {
        this.customCardStyleProps = { ...this.customCardStyleProps, [this.cardItem.id]: this.customCardStyle(this.cardItem) }
        // this.customCardStyleProps[this.cardItem.id] = this.customCardStyle(this.cardItem)
      }
    },
    getModeConfig (theme, mode, isAll = false) {
      const list = this.getThemeList()
      let themeData = list.find(c => c.key === theme)
      if (themeData) {
        themeData = utils.deepClone(themeData)
        let modeData = themeData.config.themeList.find(c => c.value === mode)
        if (!modeData) {
          return null
        }
        modeData = modeData.config
        if (!isAll) {
          delete modeData.chartStyle
        }
        return modeData
      }
      return null
    },
    getThemeChartStyle (theme, mode) {
      const modeData = this.getModeConfig(theme, mode, true)
      return modeData.chartStyle
    },
    getThemeConfig (theme, mode, isAll = false) {
      const list = this.getThemeList()
      let themeData = list.find(c => c.key === theme)
      if (themeData) {
        themeData = utils.deepClone(themeData)
        let modeData = themeData.config.themeList.find(c => c.value === mode)
        if (!modeData) {
          return null
        }
        modeData = modeData.config
        for (let i = 0; i < themeData.config.themeList.length; i++) {
          delete themeData.config.themeList[i].config
        }
        if (!isAll) {
          delete modeData.chartStyle
        }
        themeData.config = { ...themeData.config, ...modeData }
        return themeData
      }
      return null
    },
    syncEventConfig (tab) {
      let chartCom = allComponents.find(c => c.branchType === tab.branchType && c.type === tab.type)
      if (!chartCom) {
        chartCom = this.customChart.find(c => c.type === tab.type)
      }
      if (chartCom) {
        for (const key in chartCom) {
          if (tab[key] === undefined) {
            tab[key] = chartCom[key]
          }
        }
        if (chartCom.eventConfig) {
          if (!tab.eventConfig) {
            tab.eventConfig = []
          }
          const delEvent = []
          for (let n = 0; n < tab.eventConfig.length; n++) {
            const event = tab.eventConfig[n]
            if (!chartCom.eventConfig.find(c => c.event === event.event)) {
              delEvent.push(event)
            }
          }
          for (let n = 0; n < delEvent.length; n++) {
            tab.eventConfig.splice(tab.eventConfig.indexOf(delEvent[n]), 1)
          }
          for (let n = 0; n < chartCom.eventConfig.length; n++) {
            const event = chartCom.eventConfig[n]
            if (!tab.eventConfig.find(c => c.event === event.event)) {
              tab.eventConfig.push(utils.deepClone(event))
            }
          }
        }
      }
    },
    initThemeConfigs (config) {
      if (config) {
        const preThemeConfig = this.getThemeConfig(config.preTheme, config?.globalCss?.themeMode || '', true)
        if (preThemeConfig) {
          // 如果是已经有的预设主题
          this.recurseInitTheme(config, preThemeConfig.config)
          config.themeList = preThemeConfig.config.themeList
          if (!config.chartStyle) {
            return { ...config }
          } else {
            return config
          }
        } else {
          // 如果是新的预设主题
          // const deConfig = this.getDefaultTheme(true)
          // const chartStyle = deConfig.config.chartStyle
          // delete deConfig.config.chartStyle
          // this.recurseInitTheme(config, deConfig.config)
          // if (!config.chartConfig) {
          //   const chartConfig = getChartStyleInit(chartStyle)
          //   return { ...config, chartConfig }
          // } else {
          //   return config
          // }
        }
      }
      const deConfig = this.getDefaultTheme(true)
      const configData = utils.deepClone(deConfig.config)
      if (this.isApp || this.embedFluidWidth) {
        configData.pageLayout.pageWidthMode = 'adaptive'
        configData.pageLayout.pageMinWidth = ''
      }
      return { ...configData }
    },
    recurseInitTheme (config1, config2) {
      for (const key in config2) {
        // 循环预设主题
        if (Object.hasOwnProperty.call(config2, key)) {
          const element = config2[key]
          if (config1[key] === undefined) {
            config1[key] = typeof element === 'object' ? utils.deepClone(element) : element
          } else {
            if (typeof config1[key] === 'object' && !Array.isArray(config1[key])) {
              this.recurseInitTheme(config1[key], element)
            } else if (Array.isArray(config1[key]) && Array.isArray(element) && config1[key].length !== element.length) {
              config1[key] = utils.deepClone(element)
            }
          }
        }
      }
    },
    chartThemeChange () {
      this.isThemeChange = true
      emitter.emit('savePage')
      this.$nextTick(() => {
        const data = utils.deepClone(this.layoutConfig)
        const chartStyle = data.themeConfigs.chartStyle
        this.setCfg(data)
        this.mergeCfg(data, this.finalConfig)
        this.themeChange(data.layout, chartStyle)
        this.layoutConfig = data
        this.isThemeChange = false
      })
    },
    themeChange (layout, chartStyle) {
      for (let i = 0; i < layout.length; i++) {
        let tabList = layout[i].tabList
        if (tabList) {
          for (let j = 0; j < tabList.length; j++) {
            let tab = tabList[j]

            const option = getConfigTheme(tab?.configOption || {}, chartStyle, tab.type, tab.branchType)
            tab = Object.assign(tab, option)

            if (tab.explainConfig) {
              if (!tab.explainConfig.textStyle) {
                tab.explainConfig.textStyle = {}
              }
              tab.explainConfig.textStyle.color = option.chartTheme.textColor
              tab.explainConfig.textStyle.fontSize = option.chartTheme.fontSize
            }
          }
          tabList = null
        }
        if (layout[i].layout) {
          this.themeChange(layout[i].layout, chartStyle)
        }
        if (layout[i].isTabLayout && layout[i].tabList) {
          this.themeChange(layout[i].tabList, chartStyle)
        }
      }
      return layout
    },

    mergeCfg (data, list) {
      let cfg = list[data.id]
      if (cfg?.length > 0) {
        data.tabList = utils.deepClone(cfg)
        data.tabList.sort((a, b) => {
          return a.tabord - b.tabord
        })
        cfg = null
      }
      if (data.layout) {
        for (let i = 0; i < data.layout.length; i++) {
          this.mergeCfg(data.layout[i], list)
        }
      }
      if (data.isTabLayout && data.tabList) {
        for (let i = 0; i < data.tabList.length; i++) {
          this.mergeCfg(data.tabList[i], list)
        }
      }
    },
    deepTraversal (obj, defaultObj) {
      let result
      const objType = Object.prototype.toString.call(obj).slice(8, -1)
      const defaultObjType = Object.prototype.toString.call(defaultObj).slice(8, -1)
      if (objType !== defaultObjType && objType !== 'Null' && defaultObjType !== 'Null' && objType !== 'Undefined' && defaultObjType !== 'Undefined') {
        return obj
      }
      if (typeof defaultObj !== 'object' || defaultObj === null) {
        if (obj !== undefined) {
          return obj
        } else {
          return defaultObj
        }
      } else {
        if (defaultObj instanceof Array) {
          result = []
          for (let i = 0; i < defaultObj.length; i++) {
            if (obj[i]) {
              result[i] = this.deepTraversal(obj[i], defaultObj[i])
            }
          }

          return obj
        } else {
          result = {}
          for (const key in defaultObj) {
            if (obj[key]) {
              result[key] = this.deepTraversal(obj[key], defaultObj[key])
            } else {
              if (typeof defaultObj[key] === 'object' && defaultObj[key] !== null) {
                result[key] = utils.deepClone(defaultObj[key])
              } else {
                result[key] = defaultObj[key]
              }
            }
          }
        }

        for (const key in obj) {
          result[key] = this.deepTraversal(obj[key], defaultObj[key])
        }
      }

      return result
    }
  }
}
</script>

<style lang="scss" scoped>
// @import "../../styles/index.scss";
/* 模式切换下拉:预览 / 设计(触发器,与列表「预览/对比」一致) */
.mode-dropdown {
  margin-left: 8px;
}
.mode-dd-trigger {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  width: 96px;
  box-sizing: border-box;
  height: 28px;
  padding: 0 8px 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  font-size: 13px;
  color: #4e5969;
  cursor: pointer;
  outline: none;
  user-select: none;
}
.mode-dd-trigger:hover { border-color: #1f6aff; color: #1f6aff; }
.mode-dd-label { flex: 1; }
.mode-dd-caret { margin-left: auto; font-size: 13px; color: #86909c; }
.mode-dd-trigger svg { flex-shrink: 0; }
/* 画布缩放控件(仅设计模式) */
.canvas-zoom-ctrl {
  display: inline-flex;
  align-items: center;
  height: 26px;
  margin-left: 4px;
}
.canvas-zoom-ctrl .cz-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; font-size: 16px; line-height: 1;
  color: #4e5969; cursor: pointer; border-radius: 4px; user-select: none;
}
.canvas-zoom-ctrl .cz-btn:hover { background: #f2f3f5; color: #1d2129; }
.canvas-zoom-ctrl .cz-val {
  min-width: 42px; text-align: center; font-size: 12px; color: #4e5969;
  cursor: pointer; user-select: none; font-variant-numeric: tabular-nums;
}
.canvas-zoom-ctrl .cz-val:hover { color: #1f6aff; }
.canvas-zoom-ctrl .cz-fit {
  margin-left: 4px; padding: 0 7px; height: 22px; line-height: 22px;
  font-size: 12px; color: #4e5969; cursor: pointer; border-radius: 4px; user-select: none;
}
.canvas-zoom-ctrl .cz-fit:hover { background: #f2f3f5; color: #1d2129; }
/* 工具面板 / 页面设置 等开关型按钮激活态 */
.text-button.is-on {
  color: #1f6aff;
  background: rgba(31, 106, 255, 0.1);
}
.page-cfg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px !important;
  padding: 10px !important;
  border-bottom: 1px solid #e8eaec;
}

.page-title {
  display: flex;
  justify-content: center;
  height: 40px !important;

  .title-text {
    display: flex;
    align-items: center;
    font-size: 20px;
    font-weight: bold;
  }

  .edit-btn {
    position: relative;
    left: 10px;
  }
}

.layout-wrapper {
  position: relative;
  width: 100%;
  overflow: auto;
}
/* 预览模式:全幅、无页面边框/背景,贴合预览弹框的纯净展现。
   注意:挂在祖先 .working-container.no-background 上,不能在 layoutWrapper 自身用响应式 class,
   否则 Vue 重写 class 会冲掉 changeCardRadius/setTheme 手动加的圆角/主题类(am-card-radius / am-theme),导致切模式后圆角丢失。 */
.working-container.no-background .layout-wrapper {
  width: 100% !important;
  min-width: 0 !important;
  border: none !important;
  background: none !important;
}
.layout-wrapper {

  .layout-wrapper-content {
    width: 100%;
    height: 100%;
    overflow: hidden;
  }

  .decorate-container {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    overflow: hidden;
    pointer-events: none;
  }

  .grid-line-container {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 99;
    display: flex;

    > div {
      flex: 1;
      background-color: rgb(46 116 255 / 2%);
      border: 1px solid rgb(46 116 255 / 15%);
    }
  }
}

.bk-button-group {
  height: 30px;
  padding-right: 5px;
  margin-right: 5px;
  vertical-align: middle;
  border-right: 1px solid #cbd0e4;
}

.has_full {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 88;
  background: #fff;
}

.analysis-maker-wrapper {
  display: flex;
  flex-direction: column;

  .top-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 50px;
    padding: 0 10px;
    border-bottom: 1px solid #ededed;

    .left {

      .page-icon {
        width: 20px;
        height: 20px;
        color: #28b4ff;
        // background-image: linear-gradient(90deg, #28b4ff, #1f6aff);
        // background-clip: text;
        & + span {
          position: relative;
          font-size: 15px;
          font-weight: bold;
          color: transparent;
          background-image: linear-gradient(90deg, #28b4ff, #1f6aff);
          background-clip: text;

          &::after {
            position: absolute;
            top: 50%;
            right: -10px;
            width: 1px;
            height: 18px;
            margin-top: -9px;
            content: "";
            background: #c4c4c4;
          }
        }
      }
    }

    .right {
      display: flex;
      gap: 10px;
      align-items: center;

      .text-button {
        position: relative;
        display: flex;
        align-items: center;
        padding: 4px;
        color: #1a1a1a;
        cursor: pointer;
        border-radius: 4px;

        &::after {
          position: absolute;
          top: 50%;
          right: -4px;
          width: 1px;
          height: 14px;
          margin-top: -7px;
          content: "";
          background: #c4c4c4;
        }

        >svg {
          width: 16px;
          height: 16px;
          margin-right: 3px;
        }

        &:hover {
          background: rgb(0 0 0 / 10%);
        }

        > img {
          width: 16px;
          height: 16px;
          margin-right: 2px;
        }

        > span {
          font-size: 14px;
          color: #1a1a1a;
        }
      }

      .fold-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        height: 26px;
        padding: 0 9px;
        font-size: 13px;
        line-height: 26px;
        color: #4e5969;
        cursor: pointer;
        background: transparent;
        border-radius: 4px;
        white-space: nowrap;

        > span { white-space: nowrap; }

        &:hover {
          background: #f2f3f5;
          color: #1d2129;

          > i, > svg {
            color: #1d2129;
          }
        }

        &.active {
          background: #e8f3ff;
          color: #1f6aff;

          > i, > svg {
            color: #1f6aff;
          }
        }
      }

      .buttons {
        display: flex;
        align-items: center;
        line-height: 1;

        .el-button {
          padding-top: 6px;
          padding-bottom: 6px;

          > :deep(span) {
            bottom: 0;
            display: flex;
            align-items: center;

            svg {
              margin-top: -2px;
              margin-right: 3px;
            }
          }
        }
      }
    }
  }

  .working-container {
    position: relative;
    flex: 1;
    overflow: auto;
    pointer-events: auto !important;
    background: url("./images/bg.png") repeat;

    &.no-background {
      background: #fff;
    }
    /* 预览模式(no-background)纯只读:隐藏缩放/拖拽手柄、卡片操作区(复制/增删)、选中边框 */
    &.no-background :deep(.vue-resizable-handle),
    &.no-background :deep(.vue-draggable-handle),
    &.no-background :deep(.grid-item-oper) {
      display: none !important;
    }
    &.no-background :deep(.vue-grid-item.active),
    &.no-background :deep(.item-set-layout.active) {
      border-color: transparent !important;
      box-shadow: none !important;
    }

    &:not(.no-background) {
      border: 1px solid #ebebeb;
    }

    .am-ruler {
      position: absolute;
      top: 0;
      left: 0;

      :deep() {

        .h-container,
        .v-container {
          // background: #e8e8e8;
          background: #f5f5f5;

          & + .corner {
            background: #f5f5f5 !important;
          }
        }
      }
    }
  }

  /* 固定模式:回到 flex 流里占位,挤压画布,无浮层阴影 */
  .right-setting-container.is-pinned {
    position: relative;
    top: auto;
    right: auto;
    bottom: auto;
    height: 100%;
    box-shadow: none;
  }
  .right-setting-container {
    /* 浮在画布之上(不再占据布局宽度,画布占满);贴视口右侧、全屏高度 */
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 2000;
    background: #fff;
    box-shadow: -6px 0 20px rgba(0, 0, 0, 0.12);
    display: flex;
    flex-direction: column;
    border-left: 1px solid #ededed;

    .cfg-close-btn, .cfg-pin-btn {
      position: absolute;
      top: 8px;
      z-index: 30;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      border-radius: 5px;
      color: #86909c;
      cursor: pointer;
      font-size: 15px;
      transition: background-color .15s, color .15s;
    }
    .cfg-close-btn { right: 10px; }
    .cfg-pin-btn { right: 38px; }
    .cfg-pin-btn.is-on { color: #1f6aff; }
    .cfg-close-btn:hover, .cfg-pin-btn:hover {
      background: #f2f3f5;
      color: #1d2129;
    }

    .expand-icon {
      position: absolute;
      right: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 40px;

      > i {
        font-size: 20px;
        color: rgb(0 0 0 / 65%);
        cursor: pointer;
      }
    }
  }
}

.analysis-maker-wrapper.point-disable {
  cursor: not-allowed;

  > div:not(.grid-setting-bar) {
    pointer-events: none;
  }

  :deep(.grid-setting-bar) {
    pointer-events: auto !important;
    cursor: auto !important;
  }
}

</style>
