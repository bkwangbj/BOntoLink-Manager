<template>
  <div class="page-setting-wrapper">
    <div class="title">
      <span style="margin-right: 10px;">页面设置</span>
      <el-button
        text
        size="small"
        class="am-ps-btn"
        type="primary"
        title="复制"
        @click="copyThemeConfig"
      >
        <el-icon><CopyDocument /></el-icon>
      </el-button>
      <el-button
        v-if="!disabled"
        text
        size="small"
        class="am-ps-btn"
        type="primary"
        title="粘贴"
        @click="pasteThemeConfig"
      >
        <el-icon><FolderChecked /></el-icon>
      </el-button>
    </div>
    <el-collapse
      v-model="collapseModel"
      class="page-setting-container"
    >
      <el-collapse-item name="preTheme">
        <template #title>
          预置主题
        </template>
        <div
          class="collapse-item theme-mode-container"
          :class="{'is-disabled':disabled}"
        >
          <div
            v-for="t in preThemeList"
            :key="t.key"
            :class="['theme-item', theme.preTheme === t.key ? 'active' : '',disabled?'is-disabled':'']"
            :style="t.config.imgPath ? {background:'url('+t.config.imgPath+') no-repeat'} : {}"
            @click="changePreTheme(t)"
          >
            <div
              v-if="!t.config.imgPath"
              class="theme-preview"
              :style="{ background: getThemePreview(t).page }"
            >
              <div
                v-for="n in 2"
                :key="n"
                class="tp-card"
                :style="{ background: getThemePreview(t).card, borderColor: getThemePreview(t).border }"
              >
                <span
                  class="tp-bar"
                  :style="{ background: getThemePreview(t).accent }"
                />
                <div class="tp-dots">
                  <i
                    v-for="(c,ci) in getThemePreview(t).palette"
                    :key="ci"
                    :style="{ background: c }"
                  />
                </div>
              </div>
            </div>
            <span>{{ t.name }}</span>
          </div>
        </div>
      </el-collapse-item>
      <el-collapse-item name="chartPalette">
        <template #title>
          图表配色模版
        </template>
        <div
          class="collapse-item chart-palette-container"
          :class="{'is-disabled':disabled}"
        >
          <div
            v-for="p in visiblePalettes"
            :key="p.key"
            :class="['palette-item', isPaletteActive(p) ? 'active' : '', disabled?'is-disabled':'']"
            @click="selectChartPalette(p)"
          >
            <div class="palette-name">
              <span>{{ p.name }}</span>
              <i
                v-if="isPaletteActive(p)"
                class="am-iconfont icon-a-yingyong palette-check"
              />
            </div>
            <div class="palette-swatches">
              <i
                v-for="(c,ci) in p.colors"
                :key="ci"
                :style="{ background: c }"
              />
            </div>
          </div>
          <!-- 默认显示前两行(6 个),更多按钮展开/收起 -->
          <div
            v-if="chartPaletteList.length > 6"
            class="palette-more"
            @click="palettesExpanded = !palettesExpanded"
          >
            <span>{{ palettesExpanded ? '收起' : `展开更多(${chartPaletteList.length - 6})` }}</span>
            <span class="palette-more-ic">{{ palettesExpanded ? '︿' : '﹀' }}</span>
          </div>
        </div>
      </el-collapse-item>
      <el-collapse-item>
        <template #title>
          全局样式
        </template>
        <div class="collapse-item">
          <EfForm
            ref="globalCssForm"
            :items="globalCssItems"
            :has-submit="false"
            :disabled="disabled"
            :has-reset="false"
            size="small"
            label-width="90px"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item>
        <template #title>
          卡片样式
        </template>
        <div class="collapse-item">
          <EfForm
            ref="cardForm"
            :items="cardItems"
            :has-submit="false"
            :disabled="disabled"
            :has-reset="false"
            size="small"
            label-width="90px"
          />
          <el-row
            v-show="showCutsomSpace"
            class="space-custom"
          >
            <span class="label">卡片间距</span>
            <div class="input-number-box">
              <el-input-number
                v-model="rowSpace"
                controls-position="right"
                :min="0"
                :max="32"
                size="small"
                :disabled="spaceDisabled||disabled"
                @change="changeSpace('custom')"
              />
              <span class="input-label">行间距</span>
            </div>
            <div class="input-number-box">
              <el-input-number
                v-model="colSpace"
                controls-position="right"
                :min="0"
                :max="32"
                size="small"
                :disabled="spaceDisabled||disabled"
                @change="changeSpace('custom')"
              />
              <span class="input-label">列间距</span>
            </div>
          </el-row>
          <el-row
            v-show="showCutsomSpace"
            class="space-custom"
          >
            <span class="label">卡片边距</span>
            <PaddingBox
              ref="cardPaddingBox"
              style="flex: 1;"
              :disabled="spaceDisabled||disabled"
              @change="changeCardPadding"
            />
          </el-row>
        </div>
      </el-collapse-item>
      <el-collapse-item>
        <template #title>
          页签样式
        </template>
        <div class="collapse-item">
          <EfForm
            ref="tabStyleForm"
            :items="tabStyleItems"
            :has-submit="false"
            :has-reset="false"
            :disabled="disabled"
            size="small"
            label-width="90px"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item>
        <template #title>
          页面布局
        </template>
        <div class="collapse-item layout-collapse-item">
          <EfForm
            ref="pageLayoutForm"
            :items="pageLayoutItems"
            :has-submit="false"
            :has-reset="false"
            :disabled="disabled"
            size="small"
            label-width="90px"
            class="inline-col-form"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item class="chart-collapse-item">
        <template #title>
          通用内容样式
        </template>
        <div class="collapse-item">
          <CardPaddingConfig
            ref="innerPaddingRef"
            class="padding-switch"
            @chart-change="innerPaddingRefChange"
          />
          <chartThemeConfig
            ref="chartThemeRef"
            :theme="theme"
            :pre-theme-data="preThemeData"
            :disabled="disabled"
            @chart-style-change="chartStyleChange"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item name="customStyle">
        <template #title>
          自定义样式
        </template>
        <div style="height: 300px;">
          <BKCodeCom
            ref="cssEditor"
            key="2"
            language="scss"
            :readonly="disabled"
            style="height: 300px;"
            @input="saveCustomStyle"
            @blur="customStyleBlur"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item name="customScript">
        <template #title>
          自定义脚本
        </template>
        <div style="height: 300px;">
          <BKCodeCom
            ref="editor"
            v-model="customScript"
            language="javascript"
            :readonly="disabled"
            style="height: 300px;"
            @input="saveCustomScript"
            @blur="customScriptBlur"
          />
        </div>
      </el-collapse-item>
    </el-collapse>
    <!-- 「另存为」= 另存自定义主题模版;当前宿主未接持久化,暂时隐藏(恢复:改回 v-if="hasSaveAsPermission") -->
    <div
      v-if="false"
      class="page-setting-footer"
    >
      <el-button
        size="small"
        type="primary"
        @click="saveAs"
      >
        另存为
      </el-button>
    </div>
  </div>
</template>

<script lang="jsx">
import { hasPermission } from '../../../configs/common-func'
import PaddingBox from './padding-box.vue'
import { cardRadiusList, spaceModeList, pageHeightList, pageWidthList } from '../configs'
import CardPaddingConfig from '../../../chart-common-config/components/card-padding-config.vue'
import LineStyle from '../../../chart-common-config/components/style-config/line-style.vue'
import TabCommonStyle from '../../../chart-common-config/components/style-config/tab-common-style.vue'
import TabStyle from '../../../chart-common-config/components/style-config/tab-style.vue'
import ChildStyle from '../../../chart-common-config/components/style-config/child-style.vue'
import BackgroundStyle from '../../../chart-common-config/components/style-config/background-style.vue'
import QueryStyle from '../../../chart-common-config/components/style-config/query-style.vue'
import chartThemeConfig from './chart-config/chart-theme-config.vue'
import { CHART_PALETTES } from '../../../configs/chart-palettes'
import { ElMessage } from 'element-plus'

export default {
  name: 'PageSetting',
  components: {
    CardPaddingConfig,
    PaddingBox,
    // eslint-disable-next-line
    LineStyle,
    // eslint-disable-next-line
    TabCommonStyle,
    // eslint-disable-next-line
    TabStyle,
    // eslint-disable-next-line
    ChildStyle,
    // eslint-disable-next-line
    BackgroundStyle,
    // eslint-disable-next-line
    QueryStyle,
    chartThemeConfig
  },
  inject: ['getPreThemeList', 'getThemeConfig'],
  provide () {
    return {
      getThemeFont: () => ''
    }
  },
  props: {
    theme: {
      type: Object,
      default: () => ({})
    },
    disabled: {
      type: Boolean,
      default: false
    },
    operPermission: {
      type: Array,
      default: () => []
    },
    isApp: {
      type: Boolean,
      default: false
    }
  },
  emits: ['changePreTheme', 'openSaveAs', 'changeCardPadding', 'changePagePadding', 'customStyleBlur', 'publicChangeMethod', 'customScriptBlur', 'chartStyleChange',
    'changePageMinWidth', 'changePageMinHeight', 'changePageHeightMode', 'changeSpace', 'changeSpaceMode', 'changeCardRadius', 'setTheme', 'changePreTheme', 'changePageWidth', 'innerPaddingRefChange'
  ],
  data () {
    return {
      preThemeList: [],
      chartPaletteList: CHART_PALETTES,
      cardRadiusList,
      spaceModeList,
      pageHeightList,
      showCutsomSpace: false,
      rowSpace: 10,
      colSpace: 10,
      pageWidthMode: 'adaptive',
      pageWidth: 800,
      pageWidthList,
      switchMap: {},
      globalCssItems: [],
      pageLayoutItems: [],
      tabStyleItems: [],
      cardItems: [],
      spaceDisabled: false,
      pageWidthDisabled: false,
      backgroundColorDisabled: false,
      cardBackgroundColorDisabled: false,
      preThemeData: {},
      pageHeightDisabled: false,
      pageHeightMode: 'adaptive',
      pageMinHeight: 800,
      pageMinWidth: 1000,
      backgroundColor: '#fff',
      cardBackgroundColor: '#fff',
      customScript: '',
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs-dark', overviewRulerBorder: false
      },
      customStyle: '',
      textStyleBox: null,
      bgBorderBox: null,
      backgroundStyleBox: null,
      childStyleBox: null,
      queryStyleBox: null,
      operateStyleBox: null,
      decorateStyleBox: null,
      paddingBox: null,
      tabCommonStyleBox: null,
      tabStyleBox: null,
      cardBackgroundStyleBox: null,
      cardBorderBox: null,
      cardShadowBox: null,
      tabSplitStyleBox: null,
      collapseModel: ['preTheme', 'chartPalette'],   // 预置主题 + 图表配色模版 默认展开
      palettesExpanded: false,                        // 图表配色模版:默认只显示前两行(6 个)
      themeModeDisabled: false,
      themeModeValue: 'default',
      spaceModeDisabled: false,
      spaceModeValue: 'normal',
      cardRadiusDisabled: false,
      cardRadiusValue: 'none'
    }
  },
  computed: {
    chartTheme () {
      return (this.theme?.preTheme || '') + ',' + (this.theme?.globalCss?.themeMode || '')
    },

    hasSaveAsPermission () {
      return hasPermission(this.operPermission, 'theme.saveAs')
    },
    // 图表配色模版:未展开只显示前两行(6 个)
    visiblePalettes () {
      return this.palettesExpanded ? this.chartPaletteList : this.chartPaletteList.slice(0, 6)
    }
  },
  watch: {
    chartTheme: {
      handler () {

      }
    },
    collapseModel (nval, bval) {
      this.$nextTick(() => {
        if (bval.indexOf('customScript') === -1 && nval.indexOf('customScript') !== -1) {
          // this.$refs.editor.resize({ height: 300, width: 300 })
        }
        if (bval.indexOf('customStyle') === -1 && nval.indexOf('customStyle') !== -1) {
          // this.$refs.cssEditor.resize({ height: 300, width: 300 })
          this.$refs.cssEditor.setValue(this.customStyle)
        }
      })
    }
  },
  created () {
    this.preThemeList = this.getPreThemeList()
  },
  mounted () {
    this.init(this.theme)
  },
  methods: {
    // 当前整页图表配色(用于高亮选中的配色模版)
    isPaletteActive (p) {
      const active = this.theme?.chartStyle?.themeStyle?.colorList || []
      return JSON.stringify(active) === JSON.stringify(p.colors)
    },
    // 选定一套配色模版 → 整页图表默认配色切换(单图表自定义配色仍优先)
    selectChartPalette (p) {
      if (this.disabled) return
      this.chartStyleChange({ form: [...p.colors], path: 'themeStyle', field: 'colorList' })
    },
    // 预置主题效果示意图配色(浅色 default / 深色 blue),自定义主题回退到浅色
    getThemePreview (t) {
      const presets = {
        default: { page: '#eef1f6', card: '#ffffff', border: '#e6e9f0', accent: '#1f6aff', palette: ['#5b9bff', '#3fd08a', '#f7b955', '#f2637b'] },
        blue: { page: '#0f1c30', card: '#1b2b45', border: '#2b3e5d', accent: '#4d8bff', palette: ['#4d8bff', '#3fd0c9', '#f7b955', '#ef6a7a'] }
      }
      return presets[t.key] || presets.default
    },
    async init (theme) {
      const preTheme = this.getThemeConfig(theme.preTheme, theme.globalCss?.themeMode || '', true)
      this.preThemeData = preTheme.config
      this.initItems()
      await this.$nextTick()
      // 全局样式
      this.rowSpace = (theme.globalCss.margin && theme.globalCss.margin[1]) || 10
      this.colSpace = (theme.globalCss.margin && theme.globalCss.margin[0]) || 10
      if (theme.globalCss.spaceMode === 'custom') {
        this.changeSpaceMode('custom')
      }
      this.$refs.chartThemeRef.init(theme)
      this.$refs.cardPaddingBox.setPadding(theme.globalCss.cardPadding)
      this.$refs.innerPaddingRef.setFormData(theme?.innerPaddingConfig || {})
      this.bgBorderBox.setFormData(theme?.globalCss?.bgBorder || {})
      this.backgroundStyleBox.setFormData(theme?.globalCss?.pageBackground || {})
      this.cardBackgroundStyleBox.setFormData(theme?.globalCss?.cardBackground || {})
      this.cardBorderBox.setFormData(theme?.globalCss?.cardStyle || {})
      this.tabSplitStyleBox.setFormData(theme?.globalCss?.tabSplitStyle || {})
      this.childStyleBox.setFormData(theme?.globalCss?.childConfig || {})
      this.cardShadowBox.setFormData(theme?.globalCss?.cardShadow || {})
      this.textStyleBox.setFormData(theme?.globalCss?.textStyle || {})
      this.decorateStyleBox.setFormData(theme?.globalCss?.decorateStyle || {})
      this.queryStyleBox.setFormData(theme?.globalCss?.queryStyle || {})
      this.operateStyleBox.setFormData(theme?.globalCss?.operateStyle || {})
      this.tabCommonStyleBox.setFormData(theme?.globalCss?.tabCommonStyle || {})
      this.tabStyleBox.setFormData(theme?.globalCss?.tabStyle || {})
      // this.$refs.globalCssForm.setFormData({
      //   themeMode: theme.globalCss.themeMode || 'default'
      // })
      this.themeModeValue = theme.globalCss.themeMode || 'default'
      // this.$refs.cardForm.setFormData({
      //   cardRadius: theme.globalCss.cardRadius || 'none',
      //   spaceMode: theme.globalCss.spaceMode || 'normal'
      // })
      this.cardRadiusValue = theme.globalCss.cardRadius || 'none'
      this.spaceModeValue = theme.globalCss.spaceMode || 'normal'
      this.backgroundColor = theme.globalCss.backgroundColor || '#fff'
      this.cardBackgroundColor = theme.globalCss.cardBackgroundColor || '#fff'
      // 页面布局
      this.pageWidthMode = theme.pageLayout.pageWidthMode
      this.pageHeightMode = theme.pageLayout.pageHeightMode
      // this.$refs.pageLayoutForm.setFormData({
      //   pageHeightMode: theme.pageLayout.pageHeightMode || 'auto'
      // })
      this.pageWidth = theme.pageLayout.pageWidth ?? 800
      this.pageMinHeight = theme.pageLayout.pageMinHeight ?? 800
      this.pageMinWidth = theme.pageLayout.pageMinWidth ?? 1000
      this.paddingBox.setPadding(theme.pageLayout.pagePadding)
      this.customScript = this.theme.customScript || ''
      this.customStyle = this.theme.globalCustomStyle || ''
      this.$nextTick(() => {
        // this.$refs.editor.resize({ height: 300, width: 300 })
        // this.$refs.cssEditor.resize({ height: 300, width: 300 })
      })
    },
    changePreTheme (theme) {
      const config = this.getThemeConfig(theme.config.preTheme, theme.config.themeMode, true)
      this.$emit('changePreTheme', config.config)
      this.$nextTick(() => {
        this.init(config.config)
      })
    },
    // 修改主题
    changeThemeMode (value) {
      if (!value) return
      this.theme.globalCss.themeMode = value
      this.$emit('setTheme', value)
      this.$nextTick(() => {
        this.init(this.theme)
      })
    },
    // 修改卡片圆角
    changeCardRadius (value) {
      if (!value) return
      this.$emit('changeCardRadius', value)
    },
    // 修改卡片间距
    changeSpaceMode (value) {
      if (!value) return
      this.showCutsomSpace = value === 'custom'
      if (value === 'custom') {
        this.changeSpace('custom')
      } else {
        this.$emit('changeSpaceMode', value)
      }
    },
    changeSpace (type) {
      this.$emit('changeSpace', { margin: [this.colSpace, this.rowSpace], type })
    },
    changePageWidthMode (value) {
      this.pageWidth = this.pageWidth ? this.pageWidth : 800
      this.$emit('changePageWidth', { value: this.pageWidth, type: value })
    },
    changePageWidth (value) {
      value = Math.floor(value)
      value = Math.max(value, 100)
      value = Math.min(value, 2560)
      this.$nextTick(() => {
        this.pageWidth = value
      })
      this.$emit('changePageWidth', { value, type: this.pageWidthMode })
    },
    innerPaddingRefChange () {
      const data = this.$refs.innerPaddingRef.saveFormData()
      this.$emit('innerPaddingRefChange', data)
    },
    initItems () {
      const items1 = [
        {
          label: '主题模式',
          field: 'themeMode',
          itemSlots: {
            default: () => {
              return [
              <EfRadio
                mock={this.theme.themeList}
                isRadioButton={true}
                size="small"
                disabled={this.themeModeDisabled}
                onChange={this.changeThemeMode}
                vModel={this.themeModeValue}
              />
              ]
            }
          }
          // props: { mock: this.theme.themeList, isRadioButton: true, size: 'small', onChange: this.changeThemeMode, disabled: false }
        },
        {
          label: '字体样式',
          field: 'textStyle',
          itemSlots: {
            default: () => {
              return [
              <TextStyle style="width: 100%;" ref={el => { this.textStyleBox = el }} editAble={this.switchMap.textStyle} onTextStyleChange={(event) => this.publicChangeMethod('globalCss.textStyle', event)}></TextStyle>
              ]
            }
          }
        },
        {
          label: '背景颜色',
          field: 'backgroundColor',
          itemSlots: {
            default: () => {
              return [
              <CommonColorPicker
                editAble={!this.backgroundColorDisabled}
                onChange={(value) => this.publicChangeMethod('globalCss.backgroundColor', value)}
                vModel={this.backgroundColor}
              />
              ]
            }
          }
        },
        {
          label: '背景边框',
          field: 'bgBorder',
          itemSlots: {
            default: () => {
              return [
              <LineStyle style="width: 100%;" ref={el => { this.bgBorderBox = el }} editAble={this.switchMap.bgBorder} onLineStyleChange={(event) => this.publicChangeMethod('globalCss.bgBorder', event)}></LineStyle>
              ]
            }
          }
        },
        {
          label: '背景图片',
          field: 'pageBackground',
          itemSlots: {
            default: () => {
              return [
              <BackgroundStyle style="width: 100%;" ref={el => { this.backgroundStyleBox = el }} editAble={this.switchMap.pageBackground} onBackgroundStyleChange={(event) => this.publicChangeMethod('globalCss.pageBackground', event)}></BackgroundStyle>
              ]
            }
          }
        },
        {
          label: '子组件',
          field: 'childConfig',
          itemSlots: {
            default: () => {
              return [
              <ChildStyle style="width: 100%;" ref={el => { this.childStyleBox = el }} editAble={this.switchMap.childConfig} onChildStyleChange={(event) => this.publicChangeMethod('globalCss.childConfig', event)}></ChildStyle>
              ]
            }
          }
        },
        {
          label: '查询样式',
          field: 'queryStyle',
          itemSlots: {
            default: () => {
              return [
              <QueryStyle style="width: 100%;" ref={el => { this.queryStyleBox = el }} editAble={this.switchMap.queryStyle} onQueryStyleChange={(event) => this.publicChangeMethod('globalCss.queryStyle', event)}></QueryStyle>
              ]
            }
          }
        },
        {
          label: '操作按钮',
          field: 'operateStyle',
          itemSlots: {
            default: () => {
              return [
              <TextStyle style="width: 100%;" ref={el => { this.operateStyleBox = el }} editAble={this.switchMap.operateStyle} onTextStyleChange={(event) => this.publicChangeMethod('globalCss.operateStyle', event)}></TextStyle>
              ]
            }
          }
        },
        {
          label: '素材样式',
          field: 'decorateStyle',
          itemSlots: {
            default: () => {
              return [
              <TextStyle style="width: 100%;" ref={el => { this.decorateStyleBox = el }} editAble={this.switchMap.decorateStyle} onTextStyleChange={(event) => this.publicChangeMethod('globalCss.decorateStyle', event)}></TextStyle>
              ]
            }
          }
        }
      ]
      const items2 = [
        {
          label: '页面宽度',
          field: 'pageWidthMode',
          type: 'radio',
          itemSlots: {
            default: () => {
              return [
              <div class="page-width-container">
                <EfRadio vModel={this.pageWidthMode} mock={this.pageWidthList} is-radio-button size="small" onChange={this.changePageWidthMode} disabled={this.pageWidthDisabled}></EfRadio>
                <EfInput v-slots={{ suffix: () => { return [<span>px</span>] } }} vModel={this.pageWidth} vShow={this.pageWidthMode !== 'adaptive'} size="small" is-number class="page-width-input" onChange={this.changePageWidth} disabled={this.pageWidthDisabled}>
                </EfInput>
                <EfInput v-slots={{ suffix: () => { return [<span>px</span>] } }} vModel={this.pageMinWidth} vShow={this.pageWidthMode === 'adaptive'} size="small" is-number class="page-width-input" onChange={this.changePageMinWidth} disabled={this.pageWidthDisabled}>
                </EfInput>
                <span class="left-label" vShow={this.pageWidthMode === 'adaptive'}>最小宽度</span>
              </div>
              ]
            }
          }
        },
        {
          label: '页面高度',
          field: 'pageHeightMode',
          type: 'radio',
          itemSlots: {
            default: () => {
              return [
              <div class="page-height-container">
                <EfRadio vModel={this.pageHeightMode} mock={this.pageHeightList} is-radio-button size="small" onChange={this.changePageHeightMode} disabled={this.pageHeightDisabled}></EfRadio>
                <EfInput v-slots={{ suffix: () => { return [<span>px</span>] } }} vModel={this.pageMinHeight} size="small" is-number class="page-width-input" vShow={this.pageHeightMode === 'adaptive'} onChange={this.changePageMinHeight} disabled={this.pageHeightDisabled}>
                </EfInput>
                <span class="left-label" vShow={this.pageHeightMode === 'adaptive'}>最小高度</span>
              </div>
              ]
            }
          }
        },
        {
          label: '页边距',
          field: 'pagePadding',
          type: 'select',
          itemSlots: {
            default: () => {
              return [
              <PaddingBox style="width: 100%;" ref={el => { this.paddingBox = el }} disabled={!this.switchMap.pagePadding} onChange={(event) => this.changePagePadding(event)}></PaddingBox>
              ]
            }
          }
        }
      ]
      const items3 = [{
        label: '基础样式',
        field: 'tabCommonStyle',
        itemSlots: {
          default: () => {
            return [
            <TabCommonStyle style="width: 100%;" ref={el => { this.tabCommonStyleBox = el }} editAble={this.switchMap.tabCommonStyle} onTextStyleChange={(event) => this.publicChangeMethod('globalCss.tabCommonStyle', event)}></TabCommonStyle>
            ]
          }
        }
      },
      {
        label: 'tab样式',
        field: 'tabStyle',
        itemSlots: {
          default: () => {
            return [
            <TabStyle style="width: 100%;" ref={el => { this.tabStyleBox = el }} editAble={this.switchMap.tabStyle} onTabStyleChange={(event) => this.publicChangeMethod('globalCss.tabStyle', event)}></TabStyle>
            ]
          }
        }
      }
      ]

      const items4 = [
        {
          label: '卡片颜色',
          field: 'cardBackgroundColor',
          itemSlots: {
            default: () => {
              return [
              <CommonColorPicker
                editAble={!this.cardBackgroundColorDisabled}
                onChange={(value) => this.publicChangeMethod('globalCss.cardBackgroundColor', value)}
                vModel={this.cardBackgroundColor}
              />
              ]
            }
          }
        },
        {
          label: '卡片图片',
          field: 'cardBackground',
          itemSlots: {
            default: () => {
              return [
              <BackgroundStyle style="width: 100%;" ref={el => { this.cardBackgroundStyleBox = el }} editAble={this.switchMap.cardBackground} onBackgroundStyleChange={(event) => this.publicChangeMethod('globalCss.cardBackground', event)}></BackgroundStyle>
              ]
            }
          }
        },
        {
          label: '卡片边框',
          field: 'cardStyle',
          itemSlots: {
            default: () => {
              return [
              <LineStyle style="width: 100%;" ref={el => { this.cardBorderBox = el }} editAble={this.switchMap.cardStyle} onLineStyleChange={(event) => this.publicChangeMethod('globalCss.cardStyle', event)}></LineStyle>
              ]
            }
          }
        },
        {
          label: '卡片阴影',
          field: 'cardShadow',
          itemSlots: {
            default: () => {
              return [
              <ShadowStyle style="width: 100%;" ref={el => { this.cardShadowBox = el }} editAble={this.switchMap.cardShadow} onShadowStyleChange={(event) => this.publicChangeMethod('globalCss.cardShadow', event)}></ShadowStyle>
              ]
            }
          }
        },
        {
          label: '卡片圆角',
          field: 'cardRadius',
          itemSlots: {
            default: () => {
              return [
              <EfRadio
                mock={this.cardRadiusList}
                isRadioButton={true}
                size="small"
                disabled={this.cardRadiusDisabled}
                onChange={this.changeCardRadius}
                vModel={this.cardRadiusValue}
              />
              ]
            }
          }
          // props: {
          //   mock: this.cardRadiusList,
          //   isRadioButton: true,
          //   size: 'small',
          //   onChange: this.changeCardRadius
          // }
        },

        {
          label: '页签分隔',
          field: 'tabSplitStyle',
          itemSlots: {
            default: () => {
              return [
              <LineStyle style="width: 100%;" ref={el => { this.tabSplitStyleBox = el }} editAble={this.switchMap.tabSplitStyle} onLineStyleChange={(event) => this.publicChangeMethod('globalCss.tabSplitStyle', event)}></LineStyle>
              ]
            }
          }
        },
        {
          label: '卡片间距',
          field: 'spaceMode',
          itemSlots: {
            default: () => {
              return [
              <EfRadio
                mock={this.spaceModeList}
                isRadioButton={true}
                size="small"
                disabled={this.spaceModeDisabled}
                onChange={this.changeSpaceMode}
                vModel={this.spaceModeValue}
              />
              ]
            }
          }
          // props: {
          //   mock: this.spaceModeList,
          //   isRadioButton: true,
          //   size: 'small',
          //   onChange: this.changeSpaceMode
          // }
        }
      ]

      const map = {}
      items1.forEach(item => {
        if (item.field === 'themeMode') {
          map[item.field] = this.theme.globalCss[item.field] !== this.preThemeData.themeMode
        } else {
          map[item.field] = !this.compareEqual(this.theme.globalCss[item.field], this.preThemeData.globalCss[item.field])
        }
        // map[item.field] = this.theme.globalCss[item.field] !== this.preThemeData.globalCss[item.field]
        if (item.props) {
          item.props.disabled = !map[item.field]
        } else {
          if (item.field === 'backgroundColor') {
            this.backgroundColorDisabled = !map[item.field]
          }
          if (item.field === 'themeMode') {
            this.themeModeDisabled = !map[item.field]
          }
        }
      })
      items2.forEach(item => {
        map[item.field] = !this.compareEqual(this.theme.pageLayout[item.field], this.preThemeData.pageLayout[item.field])
        // map[item.field] = this.theme.pageLayout[item.field] !== this.preThemeData.pageLayout[item.field]
        if (item.props) {
          item.props.disabled = !map[item.field]
        } else {
          if (item.field === 'pageWidthMode') {
            this.pageWidthDisabled = !map[item.field]
          }
          if (item.field === 'pageHeightMode') {
            this.pageHeightDisabled = !map[item.field]
          }
          if (item.field === 'pagePadding') {
            let disabled = false
            for (const key of Object.keys(this.theme.pageLayout.pagePadding)) {
              if (this.theme.pageLayout.pagePadding[key] !== this.preThemeData.pageLayout.pagePadding[key]) {
                disabled = true
                break
              }
            }
            map.pagePadding = disabled
          }
        }
      })

      items3.forEach(item => {
        map[item.field] = !this.compareEqual(this.theme.globalCss[item.field], this.preThemeData.globalCss[item.field])
        if (item.props) {
          item.props.disabled = !map[item.field]
        }
      })
      items4.forEach(item => {
        map[item.field] = !this.compareEqual(this.theme.globalCss[item.field], this.preThemeData.globalCss[item.field])
        if (item.props) {
          item.props.disabled = !map[item.field]
        } else {
          if (item.field === 'cardBackgroundColor') {
            this.cardBackgroundColorDisabled = !map[item.field]
          }
          if (item.field === 'spaceMode') {
            this.spaceModeDisabled = !map[item.field]
          }
          if (item.field === 'cardRadius') {
            this.cardRadiusDisabled = !map[item.field]
          }
        }
      })
      this.switchMap = map

      this.globalCssItems = this.formmaterLabelItems(items1, 'globalCss')
      this.pageLayoutItems = this.formmaterLabelItems(items2, 'pageLayout')
      this.tabStyleItems = this.formmaterLabelItems(items3, 'globalCss')
      this.cardItems = this.formmaterLabelItems(items4, 'globalCss')
    },
    compareEqual (obj1, obj2) {
      // 如果不是对象或类型不一致，直接返回false
      if (typeof obj1 !== 'object' || typeof obj2 !== 'object' || obj1 === null || obj2 === null) {
        return obj1 === obj2
      }
      // 获取对象的属性名
      const keys1 = Object.keys(obj1)
      const keys2 = Object.keys(obj2)
      // 如果属性数量不相等，直接返回false
      if (keys1.length !== keys2.length) {
        return false
      }
      // 遍历第一个对象的所有属性
      for (const key of keys1) {
        // 如果第二个对象没有相同的属性，或者属性值不相等，返回false
        if (!Object.prototype.hasOwnProperty.call(obj2, key) || !this.compareEqual(obj1[key], obj2[key])) {
          return false
        }
      }
      // 如果所有属性都检查过，没有发现不匹配的情况，返回true
      return true
    },
    formmaterLabelItems (items, path) {
      for (const item of items) {
        let disabled
        if (!this.disabled && this.isApp && item.field === 'pageWidthMode') {
          disabled = true
        }
        if (!item.itemSlots) {
          item.itemSlots = {}
        }
        item.itemSlots.label = () => {
          return [
            <div class="form-label">
              <el-switch class="am-switch" size="small" disabled={disabled} vModel={this.switchMap[item.field]} onChange={(value) => this.changeSwitchValue(item, value, path)}></el-switch>
              <span>{item.label}</span>
            </div>
          ]
        }
      }
      return items
    },
    publicChangeMethod (key, value) {
      this.$emit('publicChangeMethod', key, value)
    },
    changePageHeightMode (e) {
      this.$emit('changePageHeightMode', e)
    },
    changePageMinHeight (value) {
      this.$emit('changePageMinHeight', value)
    },
    changePageMinWidth (value) {
      this.$emit('changePageMinWidth', value)
    },
    changeSwitchValue (item, value, path) {
      item.props.disabled = !value
      this.spaceDisabled = this.globalCssItems.find(c => c.field === 'spaceMode') ? (this.globalCssItems.find(c => c.field === 'spaceMode').props.disabled ?? false) : false
      if (item.field === 'pageWidthMode') {
        if (this.isApp) {
          this.pageWidthDisabled = true
        } else {
          this.pageWidthDisabled = !value
        }
      }
      if (item.field === 'pageHeightMode') {
        this.pageHeightDisabled = !value
      }
      if (item.field === 'backgroundColor') {
        this.backgroundColorDisabled = !value
      }
      if (item.field === 'themeMode') {
        this.themeModeDisabled = !value
      }
      if (item.field === 'spaceMode') {
        this.spaceModeDisabled = !value
      }
      if (item.field === 'cardRadius') {
        this.cardRadiusDisabled = !value
      }
      if (item.field === 'cardBackgroundColor') {
        this.cardBackgroundColorDisabled = !value
      }
      if (!value) {
        const key = item.field.slice(0, 1).toUpperCase() + item.field.slice(1)
        if (item.field.startsWith('page')) {
          if (item.field === 'pageWidthMode') {
            this.pageWidthMode = this.preThemeData.pageLayout.pageWidthMode
            this.pageWidth = this.preThemeData.pageLayout.pageWidth ?? '100%'
            this.pageMinWidth = this.preThemeData.pageLayout.pageMinWidth ?? '1000'
          }
          if (item.field === 'pageHeightMode') {
            this.pageHeightMode = this.preThemeData.pageLayout.pageHeightMode
            this.pageMinHeight = this.preThemeData.pageLayout.pageMinHeight ?? '800'
          }
          if (item.field === 'pagePadding') {
            this.paddingBox.setPadding(this.preThemeData.pageLayout.pagePadding)
          }

          this.$refs.pageLayoutForm.setFormData({ [item.field]: this.preThemeData.pageLayout[item.field] })
          this['change' + key] && this['change' + key](this.preThemeData.pageLayout[item.field])
          if (this['change' + key]) {
            this['change' + key](this.preThemeData.pageLayout[item.field])
          } else {
            this.$emit('publicChangeMethod', path + '.' + item.field, this.preThemeData.pageLayout[item.field])
          }
        } else {
          if (item.field === 'backgroundColor') {
            this.backgroundColor = this.preThemeData.globalCss.backgroundColor
          }
          if (item.field === 'cardBackgroundColor') {
            this.cardBackgroundColor = this.preThemeData.globalCss.cardBackgroundColor
          }
          if (item.field === 'bgBorder') {
            this.bgBorderBox.setFormData(this.preThemeData.globalCss.bgBorder)
          }
          if (item.field === 'pageBackground') {
            this.backgroundStyleBox.setFormData(this.preThemeData.globalCss.pageBackground)
          }
          if (item.field === 'cardBackground') {
            this.cardBackgroundStyleBox.setFormData(this.preThemeData.globalCss.cardBackground)
          }
          if (item.field === 'cardStyle') {
            this.cardBorderBox.setFormData(this.preThemeData.globalCss.cardStyle)
          }
          if (item.field === 'tabSplitStyle') {
            this.tabSplitStyleBox.setFormData(this.preThemeData.globalCss.tabSplitStyle)
          }
          if (item.field === 'childConfig') {
            this.childStyleBox.setFormData(this.preThemeData.globalCss.childConfig)
          }
          if (item.field === 'textStyle') {
            this.textStyleBox.setFormData(this.preThemeData.globalCss.textStyle)
          }
          if (item.field === 'decorateStyle') {
            this.decorateStyleBox.setFormData(this.preThemeData.globalCss.decorateStyle)
          }
          if (item.field === 'queryStyle') {
            this.queryStyleBox.setFormData(this.preThemeData.globalCss.queryStyle)
          }
          if (item.field === 'operateStyle') {
            this.operateStyleBox.setFormData(this.preThemeData.globalCss.operateStyle)
          }
          if (item.field === 'tabCommonStyle') {
            this.tabCommonStyleBox.setFormData(this.preThemeData.globalCss.tabCommonStyle)
          }
          if (item.field === 'tabStyle') {
            this.tabStyleBox.setFormData(this.preThemeData.globalCss.tabStyle)
          }
          if (item.field === 'cardShadow') {
            this.cardShadowBox.setFormData(this.preThemeData.globalCss.cardShadow)
          }
          if (item.field === 'spaceMode' && this.preThemeData.globalCss[item.field] === 'custom') {
            // 自定义间距
            this.rowSpace = this.preThemeData.globalCss.margin[1]
            this.colSpace = this.preThemeData.globalCss.margin[0]
            this.changeSpaceMode('custom')
            this.$refs.cardPaddingBox.setPadding(this.preThemeData.globalCss.cardPadding)
          }
          let defauleValue = this.preThemeData.globalCss[item.field]
          if (item.field === 'themeMode') {
            defauleValue = this.preThemeData.themeMode
            this.themeModeValue = defauleValue
            // this.$refs.globalCssForm.setFormData({ [item.field]: defauleValue })
          } else if (item.field === 'cardRadius') {
            this.cardRadiusValue = defauleValue
            // this.$refs.cardForm.setFormData({ [item.field]: defauleValue })
          } else if (item.field === 'spaceMode') {
            this.spaceModeValue = defauleValue
            // this.$refs.cardForm.setFormData({ [item.field]: defauleValue })
          }
          if (this['change' + key]) {
            this['change' + key](defauleValue)
          } else {
            this.$emit('publicChangeMethod', path + '.' + item.field, defauleValue)
          }
        }
      }
    },
    saveCustomScript (value) {
      this.$emit('publicChangeMethod', 'customScript', value)
    },
    chartStyleChange (value) {
      this.$emit('chartStyleChange', value)
    },
    customScriptBlur () {
      this.$emit('customScriptBlur')
    },
    saveCustomStyle (value) {
      this.customStyle = value
      this.$emit('publicChangeMethod', 'globalCustomStyle', value)
    },
    customStyleBlur () {
      this.$emit('customStyleBlur')
    },
    changePagePadding (e) {
      this.$emit('changePagePadding', e)
    },
    changeCardPadding (e) {
      this.$emit('changeCardPadding', e)
    },
    saveAs () {
      this.$emit('openSaveAs')
    },
    copyThemeConfig () {
      const data = { type: 'themeConfig', data: this.theme }
      localStorage.setItem('amCopyData', JSON.stringify(data))
      ElMessage.success('复制成功')
      // componentConfigs.request.copyTextToClipboard(JSON.stringify(data))
    },
    pasteThemeConfig () {
      try {
        const data = JSON.parse(localStorage.getItem('amCopyData'))
        if (data && data.type === 'themeConfig') {
          this.$emit('changePreTheme', data.data)
          this.$nextTick(() => {
            this.init(data.data)
          })
        } else {
          ElMessage.info('请粘贴正确的页面设置数据')
        }
      } catch (error) {
        ElMessage.info('请粘贴正确的页面设置数据')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-setting-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;

  .title {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 40px;
    font-size: 12px;
    color: #1a1a1a;
    background: #fcfcfc;
    box-shadow: 0 1px 0 0 #ededed;

    &:hover {

      .am-ps-btn {
        display: inline-block;
      }
    }
  }

  .am-ps-btn {
    position: absolute;
    display: none;
    font-size: 12px;

    &:nth-child(2) {
      right: 110px;
    }

    &:nth-child(3) {
      right: 90px;
    }
  }

  .page-setting-container {
    flex: 1;
    overflow: auto;
       padding: 0 12px;

    :deep(.el-collapse-item__header) {
      // flex-direction: row-reverse;
      justify-content: space-between;
      height: 40px;
      padding: 0 12px;
      font-size: 12px;
      line-height: 40px;

      .el-collapse-item__arrow {
        margin: 0 10px;
      }
    }

    :deep(.el-collapse-item__content) {
       padding: 0 20px 20px;
    }

    :deep(.el-collapse) {

      .chart-collapse-item {
        font-size: 14px;

        >.el-collapse-item__wrap {

          >.el-collapse-item__content {
            padding: 0;

            .el-collapse {
              border: 0;

              .el-collapse-item__header {
                border: 0;
              }

              .el-collapse-item__wrap {
                border: 0;

                .el-input-number--small {
                  width: 100%;
                }

                .chart-config-title {
                  width: 100px !important;
                  padding-right: 12px;
                  padding-left: 0 !important;
                }

                .el-select__wrapper {
                  text-align: left;
                  background: #ededed;
                  border: none;
                  border-radius: 4px !important;
                  box-shadow: none;
                }

                .ef-input-wrapper:not(.text) .el-input__inner {
                  text-align: left;
                  background: #ededed;
                  border: none;
                  border-radius: 4px !important;
                }

                .el-input-number {
                  position: relative;
                  width: 100%;

                  .el-input-number__decrease,
                  .el-input-number__increase {
                    display: none;
                  }

                  &:hover {

                    .el-input-number__decrease,
                    .el-input-number__increase {
                      display: inline-block;
                    }
                  }

                  &.is-controls-right .el-input__inner {
                    padding-right: 10px;
                  }
                }

                .el-form-item {
                  margin-bottom: 18px;

                  &:last-child {
                    margin-bottom: 0;
                  }

                  &:first-child {
                    margin-bottom: 18px;
                  }
                }
              }
            }
          }
        }
      }
    }

    .chart-collapse-item {
      font-size: 14px;

      >:deep(.el-collapse-item__wrap) {

        >.el-collapse-item__content {
          padding: 0;

          .el-collapse {
            border: 0;

            .el-collapse-item__header {
              border: 0;
            }

            .el-collapse-item__wrap {
              border: 0;

              .el-input-number--small {
                width: 100%;
              }

              .chart-config-title {
                width: 100px !important;
                padding-right: 12px;
                padding-left: 0 !important;
              }

              .ef-input-wrapper:not(.text) .el-input__inner {
                text-align: left;
                background: #ededed;
                border: none;
                border-radius: 4px !important;
              }

              .el-input-number {
                position: relative;
                width: 100%;

                .el-input-number__decrease,
                .el-input-number__increase {
                  display: none;
                }

                &:hover {

                  .el-input-number__decrease,
                  .el-input-number__increase {
                    display: inline-block;
                  }
                }

                &.is-controls-right .el-input__inner {
                  padding-right: 10px;
                }
              }

              .el-form-item {
                margin-bottom: 18px;

                &:last-child {
                  margin-bottom: 0;
                }

                &:first-child {
                  margin-bottom: 18px;
                }
              }
            }
          }
        }
      }
    }

    .collapse-item {

      :deep(.el-form) {
        padding: 0;
      }

      :deep(.el-input-group__append) {
        position: absolute;
        top: 50%;
        right: 0;
        padding: 0 20px 0 0;
        background: transparent;
        border: none;
        transform: translateY(-50%);
      }

      .page-width-input {
        width: 100%;
        margin-top: 12px;

        :deep(.el-input__inner) {
          padding-right: 30px;
          background: #ededed;
          border: none;
          border-radius: 4px !important;
        }
      }

      .space-custom {
        display: flex;
        padding: 10px;
        background: #f7f7f7;

        > .label {
          display: inline-block;
          width: 80px;
          padding-right: 10px;
          text-align: right;
        }

        .input-number-box {
          display: flex;
          flex: 1;
          flex-direction: column;

          .input-label {
            margin-top: 5px;
            font-size: 12px;
            color: #a3a3a3;
          }

          &:last-child {
            margin-left: 10px;
          }

          > .el-input-number {
            position: relative;
            width: 100%;

            &::after {
              position: absolute;
              top: 0;
              right: 6px;
              display: inline;
              color: #a3a3a3;
              content: "px";
            }

            :deep() {

              .el-input__wrapper {
                padding-left: 10px !important;
                text-align: left;
                background: #ededed;
                border: none;
                box-shadow: none !important;
              }

              .el-input__inner {
                padding-right: 10px;
                text-align: left;
              }

              .el-input-number__decrease,
              .el-input-number__increase {
                display: none;
                text-align: center;
              }
            }

            &.is-focus:not(.is-disabled) {
              background: #fff;
              box-shadow: 0 0 0 1px #409efe inset !important;

              +.el-input__prefix {
                background: #fff;
              }
            }

            &:hover {

              :deep() {

                .el-input-number__decrease,
                .el-input-number__increase {
                  display: inline-block;
                }
              }
            }
          }

          .el-input-number.is-disabled {

            :deep() {

              .el-input-number__decrease,
              .el-input-number__increase {
                display: none !important;
              }
            }
          }
        }
      }

      &.is-disabled {
        cursor: not-allowed;
      }
    }

    .collapse-item.theme-mode-container {
      display: flex;
      gap: 10px;

      .theme-item {
        position: relative;
        flex: 1;
        height: 64px;
        overflow: hidden;
        cursor: pointer;
        border: 1px solid #c7c7c7;
        border-radius: 4px;

        .theme-preview {
          position: absolute;
          inset: 0 0 18px;
          display: flex;
          gap: 4px;
          align-items: stretch;
          justify-content: center;
          padding: 5px 6px;
          overflow: hidden;

          .tp-card {
            display: flex;
            flex: 1;
            flex-direction: column;
            justify-content: space-between;
            padding: 3px;
            border: 1px solid transparent;
            border-radius: 2px;
          }

          .tp-bar {
            width: 55%;
            height: 3px;
            border-radius: 2px;
          }

          .tp-dots {
            display: flex;
            gap: 2px;

            i {
              flex: 1;
              height: 7px;
              border-radius: 1px;
            }
          }
        }

        > span {
          position: absolute;
          bottom: 0;
          z-index: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 100%;
          height: 18px;
          color: #fff;
          background: rgb(0 0 0 / 40%);
        }

        &:hover,
        &.active {

          > span {
            background: rgba($color: #1f6aff, $alpha: 70%);
          }
        }

        &.is-disabled {
          pointer-events: none;
          cursor: not-allowed;
        }

        &.is-disabled.active {
          background: #79a6ff;

          > span {
            color: #e5e5e5;
            background: rgba($color: #1f6aff, $alpha: 50%);
          }
        }
      }
    }

    .collapse-item.chart-palette-container {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;

      &.is-disabled {
        pointer-events: none;
        opacity: .6;
      }

      .palette-item {
        padding: 7px 8px;
        cursor: pointer;
        background: #fff;
        border: 1px solid #e2e5ec;
        border-radius: 5px;
        transition: border-color .15s, box-shadow .15s;

        &:hover {
          border-color: #1f6aff;
        }

        &.active {
          border-color: #1f6aff;
          box-shadow: 0 0 0 1px #1f6aff inset;
        }

        &.is-disabled {
          cursor: not-allowed;
        }

        .palette-name {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 6px;
          font-size: 12px;
          color: #4a4a4a;

          .palette-check {
            font-size: 13px;
            color: #1f6aff;
          }
        }

        &.active .palette-name {
          color: #1f6aff;
        }

        .palette-swatches {
          display: flex;
          height: 14px;
          overflow: hidden;
          border-radius: 3px;

          i {
            flex: 1;
            height: 100%;
          }
        }
      }

      .palette-more {
        grid-column: 1 / -1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        height: 28px;
        margin-top: 2px;
        font-size: 12px;
        color: #1f6aff;
        cursor: pointer;
        border: 1px dashed #d2d7e0;
        border-radius: 6px;

        &:hover { background: #f2f6ff; border-color: #1f6aff; }

        .palette-more-ic { font-size: 12px; }
      }
    }

    :deep(.ef-radio-wrapper) {
      width: 100%;

      .el-radio-group {
        display: flex;
        width: 100%;
        padding: 0 3px;
        background: #ededed;
        border-radius: 4px;
      }

      .el-radio-button--small {
        display: flex;
        flex: 1;
        align-items: center;
        padding: 3px 0;

        .el-radio-button__inner {
          width: 100%;
          height: 100%;
          padding: 4px 10px;
          color: #a3a3a3 !important;
          background-color: #ededed !important;
          border-color: #ededed !important;
          box-shadow: none !important;
        }
      }

      .el-radio-button--small.is-active {

        .el-radio-button__inner {
          font-size: 12px;
          line-height: 1em;
          color: #0a0a0a !important;
          background-color: #fff !important;
          border-color: #fff !important;
          border-radius: 4px;
        }
      }
    }

    :deep(.el-form-item__label) {
      justify-content: flex-start;
      font-size: 12px;
      text-align: left;
    }

    .form-label {
      display: flex;
      align-items: center;
      font-size: 12px;
      color: #1a1a1a;
    }
  }

  .page-setting-footer {
    height: 40px !important;
    padding: 5px 20px 0;
    text-align: center;
    background: #f8f9fa;
  }
}

:deep(.page-width-container) {
  width: 100%;

  .left-label {
    position: absolute;
    bottom: 0;
    left: -20px;
    font-size: 12px;
    transform: translateX(-100%);
  }
}

:deep(.page-height-container) {
  width: 100%;

  .left-label {
    position: absolute;
    bottom: 0;
    left: -20px;
    font-size: 12px;
    transform: translateX(-100%);
  }
}

:deep(.layout-collapse-item) {

  .el-form-item {
    margin-bottom: 10px !important;
  }
}

:deep(.page-width-input) {
  width: 100%;
  margin-top: 12px;
}
</style>
