<template>
  <div
    class="chart-content-panel"
    @click="clickChart"
  >
    <el-container
      class="chart-content"
      :class="[!isModal ? 'set-chart-content':'',focusFlag ? 'foucs-chart-content':'']"
      :style="{padding: cardPadding}"
    >
      <!-- <el-header
        class="chart-title"
        v-if="setMode && (tabList.length != 1 || (tabList.length ==1 && tabList[0].isShowTitle != '0') || focusFlag)"
      > -->
      <el-header
        v-if="!(isModal && tabList.length === 1 && tabList[0].isShowTitle == '0')"
        class="chart-title"
        :class="tabStyleCss"
      >
        <draggable
          :list="tabList"
          class="tab-content"
          :group="{name:'bk-chart-tab',put:['bk-chart-com','bk-chart-tab'] }"
          :disabled="!setMode"
          item-key="chartId"
          @change="tabChange"
        >
          <template #item="{element:tab}">
            <div
              class="tab-btn"
              :class="currentTab == tab.chartId ? 'tab-btn-active':''"
              :label="tab.chartId"
              @click="tabClick(tab)"
            >
              {{ tab.title }}<el-button
                v-if="setMode"
                size="small"
                text
                class="tab-del oper-btn"
                @click.stop="delTab(tab)"
              >
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </template>
        </draggable>
      </el-header>
      <el-main
        class="chart-content-main"
        :class="(isModal && tabList.length === 1 && tabList[0].isShowTitle == '0' && !tabList[0].initChartId) ? 'chart-tab-hidden-title':''"
      >
        <keep-alive>
          <template v-for="tab in tabList">
            <QueryArea
              v-if="tab.chartId === currentTab"
              ref="queryArea"
              :key="'q'+tab.chartId"
              :configs="tab"
              :query-items="getQueryItems(tab)"
              :is-chart="true"
              :set-mode="setMode"
              :page-config="pageConfig"
              @query="querySetParams"
            />
          </template>
        </keep-alive>
        <draggable
          v-if="showDrag"
          :list="dragList"
          class="list-group"
          style="position: relative;height: 100%;overflow: hidden;"
          :sort="false"
          :group="{ name: 'bk-chart', put: ['bk-chart-com', 'bk-chart'] }"
          :disabled="!setMode || (dragList.length > 0 && !!dragList[0].parentItemDisabled)"
          item-key="chartId"
          @change="change"
          @start="handleEvent('start')"
          @end="handleEvent('end')"
          @move="handleEvent('move',$event)"
        >
          <template #item="{element:tab}">
            <div
              style="width: 100%;height: 100%;"
            >
              <div
                v-if="!setMode || dragList.length > 0"
                class="chart-content-box"
                :style="{padding: chartBoxPadding}"
              >
                <template v-if="setMode && dragList.length > 0">
                  <component
                    :is="chartList[0].type"
                    v-bind="{...$props, ...chartList[0].customProps}"
                    :ref="'chart' + chartList[0].chartId"
                    :configs="chartList[0]"
                    :set-mode="(chartList[0].initChartId && isBasicMode) ? false : setMode"
                    :is-modal="(chartList[0].initChartId && isBasicMode) ? true : isModal"
                    :class="chartList[0].initChartId && isModal ? 'am-exteneral-item' : ''"
                    :is-preview="isPreview"
                    :page-config="pageConfig"
                    @rel-list-change="relListChange"
                    @show-clear="setShowClear"
                  />
                </template>
                <keep-alive v-else>
                  <template v-for="stab in tabList">
                    <component
                      :is="stab.type"
                      v-bind="{...$props, ...stab.customProps}"
                      v-if="stab.chartId === currentTab"
                      :key="stab.chartId"
                      :ref="'chart' + stab.chartId"
                      :configs="stab"
                      :set-mode="(stab.initChartId && isBasicMode) ? false : setMode"
                      :is-modal="(stab.initChartId && isBasicMode) ? true : isModal"
                      :is-preview="isPreview"
                      :page-config="pageConfig"
                      :class="stab.initChartId && isModal ? 'am-exteneral-item' : ''"
                      @rel-list-change="relListChange"
                      @show-clear="setShowClear"
                    />
                  </template>
                </keep-alive>

                <el-tooltip
                  v-if="!isModal &&!isBasicMode && chartList.length > 0 && chartList[0].type === 'BKCodeChart'"
                  effect="dark"
                  :content="isPreview? '编辑' : '预览'"
                  placement="top"
                  class="code-preview"
                  :style="{top:btnTop }"
                >
                  <el-button
                    v-if="!isModal && !isBasicMode && chartList.length > 0 && chartList[0].type === 'BKCodeChart'"
                    size="small"
                    text
                    class="oper-btn code-preview"
                    @click="isPreview=!isPreview"
                  >
                    <el-icon><Edit v-if="isPreview" /><View v-else /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>

              <OperatorContent
                v-bind="$props"
                :key="'o'+tab.chartId"
                :configs="tab"
                :show-clear="chartParamsFlag[tab.chartId]"
                @sort="sortChart"
                @restore="restoreChart"
                @download="downloadChart"
                @clear-parmas="clearParmas"
              />
              <ExplainContent
                v-if="tab.explainConfig && tab.explainConfig.showArea !== 'card'"
                :key="'e'+tab.chartId"
                :configs="tab"
                :rel-list="relList"
                :show-clear="chartParamsFlag[tab.chartId]"
                @clear-parmas="clearParmas"
              />
            </div>
          </template>
          <template #footer>
            <div
              v-show="tabList.length === 0 && setMode"
              class="empty-container"
            >
              <div class="empty-opts">
                <!-- 方式一:从左侧拖拽 -->
                <div class="empty-opt empty-opt-drag">
                  <div class="empty-opt-ic">
                    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#4080ff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 3v18M3 12h18M12 3 9 6M12 3l3 3M12 21l-3-3M12 21l3-3M3 12l3-3M3 12l3 3M21 12l-3-3M21 12l-3 3" />
                    </svg>
                  </div>
                  <div class="empty-opt-tt">拖拽组件</div>
                  <div class="empty-opt-sub">从左侧面板拖到此处</div>
                </div>
                <div class="empty-or">或</div>
                <!-- 方式二:点击从图表库添加 -->
                <div class="empty-opt empty-opt-click" @click.stop="openPicker">
                  <div class="empty-opt-ic">
                    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#4080ff" stroke-width="1.8" stroke-linecap="round">
                      <path d="M12 5v14M5 12h14" />
                    </svg>
                  </div>
                  <div class="empty-opt-tt">添加图表</div>
                  <div class="empty-opt-sub">从图表库中选择</div>
                </div>
              </div>
            </div>
          </template>
        </draggable>
      </el-main>
    </el-container>
    <ExplainContent
      v-if="currentTabData && currentTabData.explainConfig && currentTabData.explainConfig.showArea == 'card'"
      :configs="currentTabData"
      :rel-list="relList"
      :show-clear="chartParamsFlag[currentTabData.chartId]"
      @clear-parmas="clearParmas"
    />
    <!-- 就近添加图表:图表选择弹层(Teleport 到 body、固定居中,避免被小区域裁剪) -->
    <teleport to="body">
      <div
        v-if="pickerOpen"
        class="bk-chart-picker-mask"
        @click.self="pickerOpen=false"
      >
        <div class="bk-chart-picker">
          <div class="bcp-head">
            <span>选择图表</span>
            <button class="bcp-x" @click="pickerOpen=false">✕</button>
          </div>
          <div class="bcp-main">
            <!-- 左侧分组锚点列:点击滚动定位,右侧滚动时高亮当前分组 -->
            <div class="bcp-nav">
              <div
                v-for="g in pickerGroups"
                :key="g.key"
                :class="['bcp-nav-item', pickerActive === g.key && 'is-active']"
                @click="scrollToPickerGroup(g.key)"
              >
                <span class="bcp-nav-txt">{{ g.name }}</span>
                <span class="bcp-nav-count">{{ g.items.length }}</span>
              </div>
            </div>
            <!-- 右侧内容 -->
            <div ref="bcpBody" class="bcp-body" @scroll="onPickerScroll">
              <div v-for="g in pickerGroups" :key="g.key" class="bcp-group" :data-key="g.key">
                <div class="bcp-title">{{ g.name }}</div>
                <div class="bcp-grid">
                  <div
                    v-for="it in g.items"
                    :key="it.chartComId"
                    class="bcp-card"
                    :title="it.title"
                    @click="pickChart(it)"
                  >
                    <img
                      v-if="chartImg(it)"
                      :src="chartImg(it)"
                      class="bcp-img"
                    >
                    <span class="bcp-name">{{ it.title }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import { v4 as uuidv4 } from 'uuid'
import emitter from '../../../configs/emitter'
import { getVarAndEvent, getDefaultConfig } from '../../../configs/common-func'
import { chartComponents, ringComponents, mapComponents, tableComponents, customComponents, imgObject } from '../../../configs/chart-com'
import OperatorContent from './operator-content.vue'
import ExplainContent from './explain-content.vue'
import QueryArea from './query-area.vue'
import { utils } from 'efficient-suite'
import { ElMessageBox } from 'element-plus'
export default {
  name: 'ChartContent',

  components: {
    draggable, OperatorContent, ExplainContent, QueryArea
  },
  inject: ['chartConfig', 'getThemeChartStyle', 'setParams'],
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    setMode: {
      type: Boolean,
      default: false
    },
    pageConfig: {
      type: Object,
      default: () => {}
    },
    btnTop: {
      type: String,
      default: ''
    },
    configNode: {
      type: Object,
      default: () => { return { label: '' } }
    },
    isChild: {
      type: Boolean,
      default: false
    },
    mapSource: {
      type: Array,
      default: () => []
    },
    mapPath: {
      type: String,
      default: '/map/'
    },
    isModal: {
      type: Boolean,
      default: false
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    tjbURL: {
      type: String,
      default: ''
    }
  },
  emits: ['updateCurrentConfigId', 'sortPartChart', 'restorePartChart'],
  data () {
    return {
      pickerOpen: false,
      pickerActive: '',            // 选择图表弹框:左侧锚点列当前激活分组
      pickerClickLock: false,      // 点击锚点触发的平滑滚动期间,忽略滚动高亮回写
      focusFlag: false,
      currentTab: '',
      tabList: [],
      dragList: [],
      showDrag: true,
      chartList: [],
      chartCfg: {},
      dragState: false,
      processSetp: 0,
      relList: [],
      isPreview: true,
      chartParamsFlag: {},
      currentConfigId: ''
    }
  },
  computed: {
    // 就近添加图表:图表选择弹层的分组数据(与左侧图表资源同源)
    pickerGroups () {
      const LINE = ['lineChart', 'smoothLineChart', 'markLineChart', 'areaChart', 'stackAreaChart', 'stepLineChart', 'rainfallEvap', 'stackLineChart', 'markerLineChart', 'sectionsLineChart', 'gradientStackAreaChart', 'rainfallFlowChart', 'timeAxisLineChart', 'rainfallRunoffChart']
      const ADV = ['bubbleChart', 'calendarHeatmap', 'polarChart', 'scatterChart', 'funnelChart', 'heatmapChart', 'sankeyChart', 'treemapChart', 'sunburstChart', 'graphChart', 'themeRiverChart', 'boxplotChart', 'gradeGaugeChart', 'parallelChart', 'pictorialBarChart', 'candlestickChart', 'treeChart', 'roseChart', 'waterfallChart', 'bumpChart', 'nestPieChart', 'effectScatterChart', 'gradientAreaChart', 'thresholdAreaChart', 'confidenceBandChart', 'speedGaugeChart', 'stageGaugeChart', 'tempGaugeChart', 'ringGaugeChart', 'barometerGaugeChart', 'multiGaugeChart']
      // 仪表盘(从高级图表拆出 + 基础仪表盘 BKGaugeChart)
      const GAUGES = ['gradeGaugeChart', 'speedGaugeChart', 'stageGaugeChart', 'tempGaugeChart', 'ringGaugeChart', 'barometerGaugeChart', 'multiGaugeChart']
      const isGauge = c => c.type === 'BKGaugeChart' || GAUGES.includes(c.branchType)
      const bars = chartComponents.filter(c => (c.type === 'BKBarChart' || c.type === 'BKPolarChart') && !LINE.includes(c.branchType) && !ADV.includes(c.branchType))
      const lines = chartComponents.filter(c => LINE.includes(c.branchType))
      const adv = chartComponents.filter(c => ADV.includes(c.branchType) && !isGauge(c))
      const gauges = chartComponents.filter(isGauge)
      return [
        { key: 'bar', name: '柱状图', items: bars },
        { key: 'line', name: '折线图', items: lines },
        { key: 'pie', name: '饼形图', items: ringComponents || [] },
        { key: 'gauge', name: '仪表盘', items: gauges },
        { key: 'adv', name: '高级图表', items: adv },
        { key: 'table', name: '表格', items: tableComponents || [] },
        { key: 'map', name: '地图', items: mapComponents || [] },
        { key: 'com', name: '其他', items: customComponents || [] }
      ].filter(g => g.items && g.items.length)
    },
    mergeChild () {
      return this.pageConfig?.themeConfigs?.globalCss?.childConfig?.mergeChildChart === 'merge'
    },
    isShowTitle () {
      return this.isChild ? (this.mergeChild ? '0' : '1') : '1'
    },
    queryItems () {
      if (this.isModal) {
        const tab = this.tabList.find(c => c.chartId === this.currentTab)
        if (tab && tab.varListener) {
          return tab.varListener.filter(c => c.type === 'internal' && c.isShowQuery === '1' && c.isQuery === '1' && c.queryConfig)
        }
      }
      if (this.chartList && this.chartList.length > 0 && this.chartList[0].varListener) {
        return this.chartList[0].varListener.filter(c => c.type === 'internal' && c.isShowQuery === '1' && c.isQuery === '1' && c.queryConfig)
      }
      return []
    },
    currentTabData () {
      return this.chartList.find(c => c.chartId === this.currentTab)
    },
    tabStyleCss () {
      if (this.pageConfig?.themeConfigs?.globalCss?.tabStyle) {
        return `am-${this.pageConfig.themeConfigs.globalCss.tabStyle.type}-tab`
      }
      return ''
    },
    cardPadding () {
      if (this.pageConfig?.themeConfigs?.globalCss?.cardPadding) {
        const { paddingTop, paddingBottom, paddingLeft, paddingRight } = this.pageConfig.themeConfigs.globalCss.cardPadding
        return `${paddingTop}px ${paddingRight}px ${paddingBottom}px ${paddingLeft}px`
      } else {
        return '0'
      }
    },
    chartBoxPadding () {
      let cfg = {}
      if (this.setMode) {
        cfg = this.chartList?.length ? this.chartList[0] : {}
      } else {
        cfg = this.tabList.find(ele => { return ele.chartId === this.currentTab })
      }
      if (cfg?.paddingConfig && cfg.paddingConfig?.show) {
        const { top, bottom, left, right } = cfg?.paddingConfig
        return `${top || 0}px ${right || 0}px ${bottom || 0}px ${left || 0}px`
      } else if (this.pageConfig?.themeConfigs?.innerPaddingConfig && this.pageConfig?.themeConfigs?.innerPaddingConfig.show) {
        const { top, bottom, left, right } = this.pageConfig?.themeConfigs?.innerPaddingConfig
        return `${top || 0}px ${right || 0}px ${bottom || 0}px ${left || 0}px`
      } else {
        return '0'
      }
    },
    chartTheme () {
      return (this.pageConfig?.themeConfigs?.preTheme || '') + ',' + (this.pageConfig?.themeConfigs?.globalCss?.themeMode || '')
    }
  },
  watch: {
    currentTab: {
      handler () {
        // if (!this.isModal) {
        if (this.currentTab) {
          const data = this.tabList.find(c => c.chartId === this.currentTab)
          if (data) {
            this.chartList = [data]
            this.dragList = utils.deepClone(this.chartList)
          } else {
            this.chartList = []
            this.dragList = []
          }
        } else {
          this.chartList = []
          this.dragList = []
        }
        if (this.processSetp !== 1 && !this.isModal) {
          emitter.emit('chartClick', { configs: this.chartList.length > 0 ? { ...utils.deepClone(this.configs), ...utils.deepClone(this.chartList[0]) } : {}, expand: this.chartList.length > 0 }
          )
        }
        this.processSetp = 2
        // }
      }
    },
    currentConfigId (v) {
      this.$emit('updateCurrentConfigId', v)
    },
    // 设计/预览模式切换时重置选中态,避免上个模式残留的 focusFlag 让切回后点不开配置面板
    setMode () {
      this.focusFlag = false
      this.currentConfigId = ''
    },
    focusFlag () {
      if (!this.isModal && !this.isBasicMode && this.chartList.length > 0 && this.chartList[0].type === 'BKCodeChart') {
        this.isPreview = !this.focusFlag
      }
    },
    dragList () {
      if (this.setMode) {
        this.showDrag = false
        setTimeout(() => {
          this.showDrag = true
        }, 10)
      }
    }
  },
  created () {
    emitter.on('chartClick', this.handleChartClick)
    emitter.on('changeChart', this.changeChart)
    emitter.on('saveChartCfg', this.saveChartCfg)
    emitter.on('varConfigChange', this.varConfigChange)
    if (this.configs.tabList) {
      this.tabList = utils.deepClone(this.configs.tabList)
      this.processSetp = 1
    }
    emitter.on('savePage', this.savePage)
    emitter.on('resetChartFocus', this.resetChartFocus)
    if (this.tabList.length > 0) {
      this.currentTab = this.tabList[0].chartId
    }
  },
  beforeUnmount () {
    emitter.off('chartClick', this.handleChartClick)
    emitter.off('changeChart', this.changeChart)
    emitter.off('saveChartCfg', this.saveChartCfg)
    emitter.off('savePage', this.savePage)
    emitter.off('varConfigChange', this.varConfigChange)
    emitter.off('resetChartFocus', this.resetChartFocus)
  },
  methods: {
    getChartObject (obj) {
      return this.chartList.find(c => c.chartId === obj.chartId)
    },
    getQueryItems (tab) {
      if (tab.varListener) {
        return tab.varListener.filter(c => c.type === 'internal' && c.isShowQuery === '1' && c.isQuery === '1' && c.queryConfig)
      }
      return []
    },
    getRefObject (chartId) {
      const ref = this.$refs['chart' + chartId]
      if (Array.isArray(ref)) {
        if (ref.length > 0) {
          return ref[0]
        } else {
          return null
        }
      } else {
        return ref
      }
    },
    querySetParams (params) {
      this.setParams(params)
      // emitter.emit('setParams', params)
    },
    setShowClear (show, chartId) {
      this.chartParamsFlag[chartId] = show
      this.$forceUpdate()
    },
    clearParmas (configs) {
      try {
        this.getRefObject(configs.chartId).clearParams()
      } catch (error) {
        console.log(configs.type + '组件未实现clearParams方法')
      }
    },
    downloadChart (configs) {
      try {
        this.getRefObject(configs.chartId).downloadChart()
      } catch (error) {
        console.log(configs.type + '组件未实现downloadChart方法')
      }
    },
    relListChange (list) {
      this.relList = list
    },
    sortChart (configs, oper) {
      if (oper && oper.isChangePart === '1') {
        this.$emit('sortPartChart', configs.chartId)
      }
      if (this.getRefObject(configs.chartId)) {
        try {
          this.getRefObject(configs.chartId).sortChart()
        } catch (error) {
          console.log(configs.type + '组件未实现sortChart方法', error)
        }
      }
    },
    restoreChart (configs, oper) {
      if (oper && oper.isChangePart === '1') {
        this.$emit('restorePartChart', configs.chartId)
      }
      if (this.getRefObject(configs.chartId)) {
        try {
          this.getRefObject(configs.chartId).restoreChart(configs)
        } catch (error) {
          console.log(configs.type + '组件未实现restoreChart方法')
        }
      }
    },
    handleChartClick ({ configs }) {
      if (!this.isModal) {
        this.currentConfigId = configs.id
        this.focusFlag = this.configs.id === configs.id
      }
    },
    // 右侧配置面板收起时,重置选中态,否则再次单击同一图表(focusFlag 仍为 true)不会重新弹出面板
    resetChartFocus () {
      if (!this.isModal) {
        this.focusFlag = false
        this.currentConfigId = ''
      }
    },
    clickChart () {
      // 仅设计模式(setMode)可点击弹出配置面板;预览模式点击不触发,避免污染 focusFlag 导致切回设计后点不开
      if (!this.isModal && !this.focusFlag && this.setMode) {
        emitter.emit('chartClick', { configs: this.chartList.length > 0 ? { ...utils.deepClone(this.configs), ...utils.deepClone(this.chartList[0]), id: this.configs.id } : this.configs, expand: this.chartList.length > 0 })
      }
    },
    tabClick (tab) {
      this.currentTab = tab.chartId
    },
    delTab (tab) {
      ElMessageBox.confirm('确认删除该页签吗？').then(() => {
        this.handleDelTab(tab)
      }).catch(() => {})
    },
    handleDelTab (tab) {
      this.tabList.splice(this.tabList.indexOf(tab), 1)
      if (this.tabList.length > 0) {
        this.currentTab = this.tabList[0].chartId
      } else {
        this.currentTab = ''
      }
    },
    getDefaultVarListener () {
      let varListener = []
      if (this.pageConfig.varConfig) {
        const defalutVar = this.pageConfig.varConfig.filter(c => c.changeType === 'refreshData' && c.isDefault === '1')
        varListener = defalutVar.map(item => { return { ...item, alias: item.name, operator: 'eq', isShowQuery: '0' } })
      }
      return varListener
    },
    async tabChange ({ added, removed }) {
      if (added) {
        const addData = added.element.payload || added.element
        const index = this.tabList.indexOf(added.element)
        const tabAddData = this.tabList[index].payload || this.tabList[index]
        if (!addData.chartId) {
          const chartStyle = utils.deepClone(this.pageConfig?.themeConfigs?.chartStyle || {})
          const defaultConfig = await getDefaultConfig(tabAddData, chartStyle)
          this.tabList[index] = {
            ...utils.deepClone(tabAddData),
            chartId: uuidv4(),
            varListener: this.getDefaultVarListener(),
            isShowTitle: this.isShowTitle,
            ...defaultConfig
          }
        }
        if (tabAddData.initChartId === true) {
          this.tabList[index] = {
            ...utils.deepClone(tabAddData),
            chartId: uuidv4(),
            initChartId: 'init'
          }
          // delete this.tabList[index].initChartId
        }
        this.tabList[index].id = this.configs.id
        this.currentTab = this.tabList[index].chartId
      }
      if (removed && removed.element) {
        if (this.tabList.length > 0) {
          this.currentTab = this.tabList[0].chartId
        } else {
          this.currentTab = ''
        }
      }
    },
    change ({ added, removed }) {
      if (added) {
        const beforeData = utils.deepClone(this.chartList)
        const chart = utils.deepClone(added.element.payload || added.element)
        if (chart.initChartId) {
          chart.chartId = uuidv4()
          chart.initChartId = 'init'
          // delete chart.initChartId
        }
        this.updateTabContent(chart)
        this.dragState = false
        emitter.emit('changeChart', beforeData)
      }
      if (removed && removed.element) {
        this.dragState = true
      }
      // this.clickChart()
    },
    handleEvent (type) {
      if (type === 'start') {
        this.dragState = true
      } else if (type === 'end') {
        this.dragState = false
      }
      return true
    },
    // —— 就近添加图表 ——
    chartImg (it) { return it && it.img ? imgObject[it.img] : '' },
    openPicker () {
      this.pickerOpen = true
      this.pickerActive = this.pickerGroups[0]?.key || ''
      this.$nextTick(() => { if (this.$refs.bcpBody) this.$refs.bcpBody.scrollTop = 0 })
    },
    // 左侧锚点点击:平滑滚动到对应分组
    scrollToPickerGroup (key) {
      this.pickerActive = key
      const body = this.$refs.bcpBody
      if (!body) return
      const el = body.querySelector(`[data-key="${key}"]`)
      if (!el) return
      this.pickerClickLock = true
      body.scrollTo({ top: el.offsetTop, behavior: 'smooth' })
      clearTimeout(this._bcpLockTimer)
      this._bcpLockTimer = setTimeout(() => { this.pickerClickLock = false }, 400)
    },
    // 右侧滚动:高亮当前分组锚点
    onPickerScroll () {
      if (this.pickerClickLock) return
      const body = this.$refs.bcpBody
      if (!body) return
      const groups = body.querySelectorAll('.bcp-group')
      const top = body.scrollTop
      let active = this.pickerActive
      for (const g of groups) {
        if (g.offsetTop - 8 <= top) active = g.dataset.key
        else break
      }
      if (top + body.clientHeight >= body.scrollHeight - 2 && groups.length) {
        active = groups[groups.length - 1].dataset.key
      }
      this.pickerActive = active
    },
    // 从弹层选中图表 → 加入本区域(等效拖入),并打开其配置
    pickChart (item) {
      this.pickerOpen = false
      const beforeData = utils.deepClone(this.chartList)
      const chart = utils.deepClone(item.payload || item)
      if (chart.initChartId) {
        chart.chartId = uuidv4()
        chart.initChartId = 'init'
      }
      this.updateTabContent(chart)
      this.dragState = false
      emitter.emit('changeChart', beforeData)
    },
    changeChart (data) {
      if (this.dragState) {
        if (data.length > 0) {
          this.updateTabContent(data[0])
        } else {
          this.handleDelTab(this.tabList.find(c => c.chartId === this.currentTab))
        }
        // this.chartList = data
        // this.dragList = utils.deepClone(this.chartList)
        this.dragState = false
      }
    },
    async  updateTabContent (newTab) {
      if (this.currentTab) {
        const index = this.tabList.findIndex(c => c.chartId === this.currentTab)
        let tab = utils.deepClone(newTab)

        if (!tab.chartId) {
          tab.chartId = uuidv4()
          tab.varListener = this.getDefaultVarListener()
          const chartStyle = utils.deepClone(this.pageConfig?.themeConfigs?.chartStyle || {})
          const defaultConfig = await getDefaultConfig(tab, chartStyle)
          tab = { ...tab, ...defaultConfig, isShowTitle: this.isShowTitle }
        }
        tab.id = this.configs.id
        this.tabList[index] = tab
        this.currentTab = tab.chartId
      } else {
        this.tabList = [{ ...utils.deepClone(newTab), id: this.configs.id }]
        if (!this.tabList[0].chartId) {
          this.tabList[0].chartId = uuidv4()
          this.tabList[0].isShowTitle = this.isShowTitle
          this.tabList[0].varListener = this.getDefaultVarListener()
          const chartStyle = utils.deepClone(this.pageConfig?.themeConfigs?.chartStyle || {})
          const defaultConfig = await getDefaultConfig(this.tabList[0], chartStyle)
          this.tabList[0] = { ...this.tabList[0], ...defaultConfig }
        }
        this.currentTab = this.tabList[0].chartId
      }
    },
    saveChartCfg (configs) {
      if (this.currentTab === configs.chartId) {
        const index = this.tabList.findIndex(c => c.chartId === this.currentTab)
        if (configs.type === 'BKCodeChart') {
          configs.code = this.tabList[index].code
        }
        this.tabList[index] = { ...utils.deepClone(this.tabList[index]), ...utils.deepClone(configs) }
        this.chartList = [this.tabList[index]]
        this.dragList = utils.deepClone(this.chartList)
        this.$nextTick(() => {
          setTimeout(() => {
            try {
              this.getRefObject(configs.chartId).resetChart(this.tabList[index])
            } catch (error) {
              console.log(configs.type + '组件未实现resetChart方法')
            }
          }, 20)
        })
      }
    },
    savePage () {
      // for (let i = 0; i < this.tabList.length; i++) {
      //   if (this.tabList[i].type === 'BKCodeChart') {
      //     if (this.$refs['chart' + this.tabList[i].chartId].length > 0) {
      //       this.tabList[i].code = this.$refs['chart' + this.tabList[i].chartId][0].code
      //     }
      //   }
      // }
      this.chartConfig.setConfig(this.configs.id, this.tabList)
    },
    varConfigChange (varConfig) {
      if (this.tabList.length > 0) {
        for (let i = 0; i < this.tabList.length; i++) {
          const { varListener, eventConfig } = getVarAndEvent(this.tabList[i], varConfig)
          this.tabList[i].varListener = varListener
          this.tabList[i].eventConfig = eventConfig
          if (this.getRefObject(this.tabList[i].chartId)) {
            try {
              this.getRefObject(this.tabList[i].chartId).buildParams(true)
            } catch (error) {
              console.log(this.tabList[i].type + '组件未实现buildParams方法')
            }
          }
        }
        const index = this.tabList.findIndex(c => c.chartId === this.currentTab)
        if (index !== -1) {
          this.chartList = [this.tabList[index]]
          this.dragList = utils.deepClone(this.chartList)
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>
:deep() {

  .el-tabs__content {
    height: calc(100% - 43px);
  }

  .am-exteneral-item {

    .vgl-item {
      background: none !important;
      border: none !important;
      box-shadow: none !important;
    }
  }

  .preview-tab {
    padding-top: 5px;

    .el-tabs__header {
      margin: 0 0 10px;

      .el-tabs__nav-wrap {
        padding-left: 10px;
      }

      .el-tabs__active-bar {
        display: none;
      }

      .el-tabs__item {
        height: 33px;
        padding: 0 20px 0 15px;
        line-height: 33px;
      }
    }
  }

  .chart-tab-hidden-title {
    margin-top: 10px;
  }

  .production-tab-hidden-title {

    .el-tabs__header {
      display: none;
    }

    .el-tabs__content {
      height: 100%;
    }
  }

  .modal-content {
    position: relative;
    height: 100%;
    overflow: hidden;
  }
}

.code-preview {
  position: absolute;
  top: 5px;
  right: 30px;
}

.chart-content-panel {
  width: 100%;
  height: 100%;

  .full-box {
    width: 100%;
    height: 100%;
  }

  .empty-container {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: transparent;

    .empty-opts {
      display: flex;
      align-items: stretch;
      gap: 12px;
    }

    .empty-opt {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 5px;
      width: 128px;
      padding: 16px 12px;
      border: 1px dashed #d5dce6;
      border-radius: 10px;
      background: #fbfcfe;
      transition: border-color .15s, background .15s, box-shadow .15s;
    }

    .empty-opt-click {
      cursor: pointer;
      border-style: solid;
      border-color: #c9dbff;
    }

    .empty-opt-click:hover {
      border-color: #1f6aff;
      background: #f2f7ff;
      box-shadow: 0 4px 14px rgba(31, 106, 255, .12);
    }

    .empty-opt-ic { line-height: 0; margin-bottom: 2px; }
    .empty-opt-tt { font-size: 13px; font-weight: 600; color: #1d2129; }
    .empty-opt-sub { font-size: 11px; color: #86909c; text-align: center; line-height: 1.4; }

    .empty-or {
      display: flex;
      align-items: center;
      font-size: 12px;
      color: #a9afba;
    }
  }
}

/* 就近添加图表:图表选择弹层(teleport 到 body,顶层规则以匹配传送后的元素) */
.bk-chart-picker-mask {
  position: fixed;
  inset: 0;
  z-index: 2400;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, .35);
}
.bk-chart-picker {
  width: 620px;
  max-height: 72vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, .18);
  overflow: hidden;
}
/* 主体:左锚点列 + 右内容 */
.bcp-main {
  flex: 1;
  min-height: 0;
  display: flex;
}
/* 左侧分组锚点列 */
.bcp-nav {
  flex: 0 0 100px;
  width: 100px;
  overflow-y: auto;
  padding: 8px 0;
  border-right: 1px solid #eef0f3;
  background: #fafafa;
}
.bcp-nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  height: 34px;
  padding: 0 12px 0 16px;
  font-size: 13px;
  color: #4e5969;
  cursor: pointer;
  border-left: 2px solid transparent;
  white-space: nowrap;
  transition: background .15s, color .15s;
}
.bcp-nav-item:hover { background: #f0f2f5; color: #1a1a1a; }
.bcp-nav-item.is-active {
  color: #1f6aff;
  font-weight: 600;
  background: #fff;
  border-left-color: #1f6aff;
}
.bcp-nav-txt { overflow: hidden; text-overflow: ellipsis; }
.bcp-nav-count { flex-shrink: 0; font-size: 11px; font-weight: normal; color: #a8adb7; }
.bcp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  border-bottom: 1px solid #f0f2f5;
}
.bcp-x { border: 0; background: transparent; color: #86909c; font-size: 14px; cursor: pointer; }
.bcp-x:hover { color: #1f6aff; }
.bcp-body { position: relative; flex: 1; min-width: 0; padding: 8px 16px 16px; overflow-y: auto; }
.bcp-title { margin: 12px 0 8px; font-size: 12px; color: #8a8f99; }
.bcp-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.bcp-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px 4px;
  border: 1px solid #eceef1;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color .15s, box-shadow .15s;
}
.bcp-card:hover { border-color: #1f6aff; box-shadow: 0 2px 8px rgba(31, 106, 255, .15); }
.bcp-img { width: 100%; height: 46px; object-fit: contain; }
.bcp-name { font-size: 12px; color: #4e5969; text-align: center; }

.chart-content {
  flex-direction: column;
  width: 100%;
  // border: 1px solid #e8eaec;
  height: 100%;

  .chart-content-box {
    width: 100%;
    height: 100%;
    overflow: auto;
  }
}

.chart-title {
  position: relative;
  // border-bottom: 1px solid #e8eaec;
  display: flex;
  align-items: center;
  margin: 0 0 10px !important;
  overflow: visible;
}

.tab-content {
  width: 100%;
}

</style>
