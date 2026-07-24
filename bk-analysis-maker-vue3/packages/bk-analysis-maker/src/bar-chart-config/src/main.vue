<template>
  <BKBasicChartConfig
    ref="basicChartConfig"
    :configs="configs"
    :page-config="pageConfig"
    :save-able="saveAble"
    :items="items"
    :rules="rules"
    :tjb-u-r-l="tjbURL"
    v-bind="$attrs"
    @change-branch-type="changeBranchType"
    @set-expand="$emit('setExpand')"
    @save-chart-config="saveChartCfg"
    @config-option-init="configOptionInit"
    @build-chart-series-data="buildChartData"
  >
    <div class="cfg-anchor-wrap">
      <div class="cfg-anchor-nav">
        <div
          v-for="t in visibleMenu"
          :key="t.key"
          :class="['cfg-anchor-item', activeConfig===t.key && 'is-on']"
          @click="scrollToSec(t.key)"
        >
          <i class="am-iconfont cfg-anchor-ic" :class="t.icon" />
          <span>{{ t.name }}</span>
        </div>
      </div>
      <div class="bar-basic-config cfg-scroll" ref="cfgScroll">
        <div id="cfgsec-basic" class="cfg-sec">
        <div class="cfg-sec-title">图表</div>
        <div
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"
            class="copy_pasted_button"
            @click="copyConfig('barBasic')"
          >
            复制基础配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('barBasic')"
          >
            粘贴基础配置
          </el-button>
        </div>
        <ChartTypeChange
          style="margin-bottom: 10px;"
          :type="configs.branchType"
          @change-type="changeChartType"
        />

        <div
          v-if="configs.branchType === 'pictorialBarChart'"
          class="pictorial-symbol-row"
          style="display:flex;align-items:center;gap:8px;margin-bottom:12px;"
        >
          <span style="font-size:12px;color:#7a7a7a;white-space:nowrap;">图标</span>
          <el-select
            v-model="pictorialSymbol"
            size="small"
            :disabled="!saveAble"
            style="flex:1;"
            @change="getChartBasicConfigData"
          >
            <el-option
              v-for="s in pictorialSymbols"
              :key="s.label"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </div>

        <component
          :is="name+'-config'"
          v-for="(name,index) in basicComps"
          :key="'b'+index"
          :ref="name+'form'"
          :save-able="saveAble"
          :series-length="seriesList.length||0"
          @chart-change="getChartBasicConfigData"
        />

        <CollapseItem
          v-if="configs.branchType!=='stackedChart' && !isRawEChart"
          v-model="showBorderRadius"
          name="柱图样式"
          @change="getChartBasicConfigData"
        >
          <div
            class="plane-inner-bg"
            style="margin-bottom: 10px;"
          >
            <el-form
              :disabled="!saveAble"
              label-width="90px"
            >
              <el-form-item label="柱体圆角">
                <el-row
                  :gutter="10"
                  type="flex"
                >
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <el-input-number
                        v-model="borderRadius[0]"
                        size="small"
                        controls-position="right"
                        class="input-number-box-px"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text"> 左上</span>
                    </div>
                  </el-col>
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <el-input-number
                        v-model="borderRadius[1]"
                        size="small"
                        controls-position="right"
                        class="input-number-box-px"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text"> 右上</span>
                    </div>
                  </el-col>
                </el-row>
                <el-row
                  :gutter="10"
                  type="flex"
                >
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <el-input-number
                        v-model="borderRadius[2]"
                        size="small"
                        controls-position="right"
                        class="input-number-box-px"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text"> 右下</span>
                    </div>
                  </el-col>
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <el-input-number
                        v-model="borderRadius[3]"
                        size="small"
                        controls-position="right"
                        class="input-number-box-px"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text"> 左下</span>
                    </div>
                  </el-col>
                </el-row>
              </el-form-item>
            </el-form>
          </div>
        </CollapseItem>
        <div
          style="margin-top: 10px;"
        >
          <ColorsPicker
            v-model="colorList"
            :color-modes=" configs.branchType==='barLineargradientChart'?['linear-gradient']:['monochrome']"
            name=" 色系配置"
            :color-list="colorsList"
            @color-change="colorsChange"
          />
        </div>
        </div>
        <div
          v-if="axisComps.length && !isRawEChart"
          id="cfgsec-axis"
          class="cfg-sec"
        >
          <div class="cfg-sec-title">坐标轴</div>
          <component
            :is="name+'-config'"
            v-for="(name,index) in axisComps"
            :key="'a'+index"
            :ref="name+'form'"
            :save-able="saveAble"
            :series-length="seriesList.length||0"
            @chart-change="getChartBasicConfigData"
          />
        </div>
        <div v-if="!isRawEChart" id="cfgsec-series" class="cfg-sec">
        <div class="cfg-sec-title">系列</div>
        <div
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"

            @click="copyConfig('barSeries')"
          >
            复制系列配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('barSeries')"
          >
            粘贴系列配置
          </el-button>
        </div>
        <barSeriesConfig
          ref="seriesConfigRef"
          :save-able="saveAble"
          :color-list="colorList"
          :series-length="seriesList.length||0"
          :branch-type="configs.branchType"
          :type="configs.type"
          @chart-change="getChartBasicConfigData"
        />
        </div>
        <div
          v-if="otherComps.length"
          id="cfgsec-other"
          class="cfg-sec"
        >
          <div class="cfg-sec-title">其他</div>
          <component
            :is="name+'-config'"
            v-for="(name,index) in otherComps"
            :key="'o'+index"
            :ref="name+'form'"
            :save-able="saveAble"
            :series-length="seriesList.length||0"
            @chart-change="getChartBasicConfigData"
          />
        </div>
      </div>
    </div>
  </BKBasicChartConfig>
</template>

<script>
import { commonItems, commonRules } from '../../configs/common-item'
import { baseChartComponents } from '../../chart-common-config/chart-config/index'
import { itemsConfig } from './config/config-items'
import barSeriesConfig from './components/series-config.vue'
import SidebarTabs from '../../chart-common-config/components/sidebar-tabs.vue'
import ChartTypeChange from '../../chart-common-config/components/chart-type-change.vue'
import Copy from '../../configs/copy-chart-mixins'
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
export default {
  name: 'BarChartConfig',
  components: {
    ...baseChartComponents,
    barSeriesConfig,
    SidebarTabs,
    ChartTypeChange
  },
  mixins: [Copy],
  provide () {
    return {
      getThemeFont: () => this.configs?.chartTheme?.textColor || '#000'
    }
  },
  inheritAttrs: false,
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    pageConfig: {
      type: Object,
      default: () => {}
    },
    saveAble: {
      type: Boolean,
      default: true
    },
    tjbURL: {
      type: String,
      default: ''
    }
  },
  emits: ['setExpand', 'saveChartCfg'],
  data () {
    return {
      items: commonItems.filter(c => c.isBasic),
      rules: commonRules,
      componentsList: [],
      seriesList: [],
      colorList: [],
      showBorderRadius: false,
      borderRadius: [0, 0, 0, 0],
      activeConfig: 'basic',
      scrollContainer: null,
      // 象形柱图内置预设符号(path:// / 内置形状均可被 itemStyle.color 染色, 跟随色系配置)
      pictorialSymbol: 'roundRect',
      pictorialSymbols: [
        { label: '圆角柱', value: 'roundRect' },
        { label: '圆点', value: 'circle' },
        { label: '三角', value: 'triangle' },
        { label: '菱形', value: 'diamond' },
        { label: '水滴', value: 'path://M512 64 C512 64 192 448 192 640 a320 320 0 1 0 640 0 C832 448 512 64 512 64 Z' },
        { label: '箭头', value: 'arrow' }
      ],
      configType: { grid: 'basic', legend: 'basic', axis: 'axis', tooltip: 'other' },
      chartMenu: [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '坐标轴', key: 'axis', icon: 'icon-zuobiao' }, { name: '系列', key: 'series', icon: 'icon-a-shujuyuan2' }, { name: '其他', key: 'other', icon: 'icon-qita' }]
    }
  },
  computed: {
    // 桑基/矩形树/旭日/关系图:rawEChart 直通,坐标轴/系列/柱样式不适用 → 配置面板隐藏这些区块
    isRawEChart () { return ['sankeyChart', 'treemapChart', 'sunburstChart', 'graphChart', 'themeRiverChart', 'boxplotChart', 'gradeGaugeChart', 'parallelChart', 'pictorialBarChart', 'candlestickChart', 'treeChart'].includes(this.configs.branchType) },
    visibleMenu () { return this.isRawEChart ? this.chartMenu.filter(t => t.key !== 'axis' && t.key !== 'series') : this.chartMenu },
    basicComps () { return this.componentsList.filter(n => this.configType[n] === 'basic') },
    axisComps () { return this.componentsList.filter(n => this.configType[n] === 'axis') },
    otherComps () { return this.componentsList.filter(n => this.configType[n] === 'other') },
    colorsList () {
      let list = []
      if (this.pageConfig?.themeConfigs?.chartStyle?.themeStyle.colorList) {
        list = [this.pageConfig?.themeConfigs?.chartStyle?.themeStyle.colorList]
      }
      return list.concat(this.pageConfig?.themeConfigs?.chartStyle?.defaultColors)
    }
  },
  created () {
    this.componentsList = Object.keys(itemsConfig.basicConfig).filter(ele => {
      return itemsConfig.basicConfig[ele]
    })
  },
  mounted () {
    this.$nextTick(() => {
      // 真正的滚动容器是外层 .base-flow(BKBasicChartConfig 的单容器)
      this.scrollEl = this.$refs.cfgScroll?.closest('.base-flow') || this.$refs.cfgScroll
      if (this.scrollEl) this.scrollEl.addEventListener('scroll', this.onCfgScroll, { passive: true })
    })
  },
  beforeUnmount () {
    if (this.scrollEl) this.scrollEl.removeEventListener('scroll', this.onCfgScroll)
  },
  methods: {
    // 点击左侧锚点:平滑滚动到对应模块
    scrollToSec (key) {
      const sc = this.scrollEl
      const el = document.getElementById('cfgsec-' + key)
      if (sc && el) {
        this.activeConfig = key
        const top = el.getBoundingClientRect().top - sc.getBoundingClientRect().top + sc.scrollTop
        sc.scrollTo({ top: Math.max(0, top - 8), behavior: 'smooth' })
      }
    },
    // 滚动时:高亮当前所在模块的锚点
    onCfgScroll () {
      const sc = this.scrollEl
      if (!sc) return
      const base = sc.getBoundingClientRect().top + 24
      let cur = this.chartMenu[0].key
      for (const t of this.chartMenu) {
        const el = document.getElementById('cfgsec-' + t.key)
        if (el && el.getBoundingClientRect().top <= base) cur = t.key
      }
      this.activeConfig = cur
    },
    buildChartData (type, data) {
      if (!this.configs.dataSourceConfig || !data) {
        return
      }
      if (!data) {
        ElMessage.warnning('暂无数据')
        return
      }
      const newList = []
      const dataSourceConfig = this.$refs.basicChartConfig.getDataSourceConfig()

      let series = data.map(item => {
        let key = 'colorField'
        if (dataSourceConfig.dataMapping && dataSourceConfig.dataMapping.colorField) {
          key = dataSourceConfig.dataMapping.colorField
        }
        return item[key]
      })
      series = Array.from(new Set(series))
      for (let i = 0; i < series.length; i++) {
        newList.push({ name: series[i], dataId: series[i], type: this.configs.branchType === 'lineChart' ? 'line' : 'bar' })
      }
      newList.forEach((ele, index) => {
        if (this.seriesList[index]) {
          this.seriesList[index].name = ele.name
          this.seriesList[index].dataId = ele.dataId
        } else {
          this.addSeris(ele)
        }
      })

      this.seriesList = this.seriesList.slice(0, newList.length)
      this.$refs.seriesConfigRef.setSeries(this.seriesList)
      ElMessage.success('已生成系列')
    },
    changeChartType (config) {
      this.$refs.basicChartConfig.changeChartType(config)
    },
    changeBranchType (config) {
      this.$emit('saveChartCfg', config)
      this.$nextTick(() => {
        this.configOptionInit()
      })
    },
    getChartBasicConfigData () {
      this.$nextTick(() => {
        this.$refs.basicChartConfig.saveConfig()
      })
    },
    addSeris (formData) {
      if (this.configs.branchType === 'lineChart') {
        const form = {
          type: 'line',
          name: formData.name,
          dataId: formData.dataId,

          showSymbol: true,
          smooth: false,
          lineStyle: {
            width: 2

          },
          itemStyle: { }

        }
        this.seriesList.push(form)
      } else {
        const item = { type: 'bar', name: formData.name, dataId: formData.dataId, itemStyle: { }, backgroundStyle: {}, stack: null }
        if (this.configs.branchType === 'stackedChart') {
          item.stack = '1'
        }
        this.seriesList.push(item)
      }
    },
    delSeris (index) {
      this.seriesList.splice(index, 1)
      if (this.seriesList.length === 1) {
        this.seriesList[0].basic.yAxisIndex = 0
      }
    },

    async saveChartCfg (data) {
      let form = {}
      this.componentsList.forEach(name => {
        // rawEChart 图隐藏了坐标轴/系列等配置组件, 对应 ref 不存在 → 跳过, 避免保存报错
        const ref = this.$refs[name + 'form']
        if (!ref || !ref[0] || typeof ref[0].saveFormData !== 'function') return
        const formData = ref[0].saveFormData()
        if (name === 'axis') {
          form = { ...form, ...formData }
        } else if (name === 'tooltip') {
          form[name] = formData.form
          form.customConfig = formData.customConfig
        } else {
          form[name] = formData
        }
      })
      // 系列配置(rawEChart 图已隐藏,ref 不存在时跳过,保留原 series 不动)
      if (this.$refs.seriesConfigRef && typeof this.$refs.seriesConfigRef.saveFormData === 'function') {
        const { series, filterDataEmpty, autoSeries } = this.$refs.seriesConfigRef.saveFormData()
        form.series = series
        form.filterDataEmpty = filterDataEmpty
        form.autoSeries = autoSeries
        this.seriesList = utils.deepClone(form.series)
      }
      form.color = this.colorList
      form.borderRadius = this.borderRadius
      form.showBorderRadius = this.showBorderRadius
      const config = utils.deepClone(data)
      config.configOption = Object.assign(config.configOption, form)
      // 象形柱图:内置符号选择 → 写入 series[0].symbol(不走系列配置组件)
      if (this.configs.branchType === 'pictorialBarChart' && config.configOption.series && config.configOption.series[0]) {
        config.configOption.series[0].symbol = this.pictorialSymbol
      }
      this.$emit('saveChartCfg', config)
    },
    colorsChange () {
      this.$nextTick(() => {
        this.$refs.basicChartConfig.saveConfig()
      })
    },
    configOptionInit () {
      this.$nextTick(() => {
        if (this.configs.configOption) {
          this.componentsList.forEach(name => {
            // rawEChart 图隐藏了坐标轴/系列等组件, 对应 ref 不存在 → 跳过(避免提前中断,漏掉色系回填)
            const ref = this.$refs[name + 'form']
            if (!ref || !ref[0] || typeof ref[0].setFormData !== 'function') return
            if (name === 'axis') {
              ref[0].setFormData(this.configs.configOption)
            } else if (name === 'tooltip') {
              ref[0].setFormData(this.configs.configOption[name] || {}, this.configs.configOption.customConfig || {})
            } else {
              ref[0].setFormData(this.configs.configOption[name] || {})
            }
          })
        }
        this.showBorderRadius = this.configs.configOption?.showBorderRadius || false
        this.borderRadius = this.configs.configOption?.borderRadius || [0, 0, 0, 0]
        // 象形柱图:回显当前符号
        if (this.configs.branchType === 'pictorialBarChart') {
          this.pictorialSymbol = this.configs.configOption?.series?.[0]?.symbol || 'roundRect'
        }
        this.colorList = this.configs.configOption?.color || this.pageConfig?.themeConfigs?.chartStyle?.themeStyle.colorList || []
        this.seriesList = utils.deepClone(this.configs.configOption?.series || [])
        // 系列配置组件(rawEChart 图已隐藏 → ref 不存在时跳过)
        if (this.$refs.seriesConfigRef) {
          this.$refs.seriesConfigRef.setSeries(this.seriesList)
          this.$refs.seriesConfigRef.setFilterDataEmpty(this.configs.configOption?.filterDataEmpty || false)
          this.$refs.seriesConfigRef.setAutoSeries(this.configs.configOption?.autoSeries || false)
        }
      })
    }

  }
}
</script>

<style lang="scss" scoped>
  // @import "../../styles/index.css";
  .d-flex {
    display: flex;
  }

  /* 锚点导航布局:左侧固定锚点 + 右侧滚动配置(依次往下排) */
  .cfg-anchor-wrap {
    display: flex;
    align-items: flex-start;
  }
  .cfg-anchor-nav {
    position: sticky;
    top: 0;
    z-index: 2;
    flex-shrink: 0;
    width: 62px;
    padding: 8px 0;
    box-sizing: border-box;
    background: #fafbfc;
    border-right: 1px solid #eef0f3;
    align-self: flex-start;
  }
  .cfg-anchor-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 10px 4px;
    font-size: 12px;
    color: #86909c;
    cursor: pointer;
    border-left: 2px solid transparent;
    transition: color .15s, background .15s;
  }
  .cfg-anchor-item:hover { color: #1f6aff; }
  .cfg-anchor-item.is-on {
    color: #1f6aff;
    background: #eef4ff;
    border-left-color: #1f6aff;
  }
  .cfg-anchor-ic { font-size: 16px; }
  .bar-basic-config {
    flex: 1;
    min-width: 0;
  }
  .cfg-sec + .cfg-sec {
    margin-top: 10px;
    padding-top: 12px;
    border-top: 1px solid #f0f2f5;
  }
  .cfg-sec-title {
    margin: 6px 0 12px;
    padding-left: 8px;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.2;
    color: #1a1a1a;
    border-left: 3px solid #1f6aff;
  }
</style>
