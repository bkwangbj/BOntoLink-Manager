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
    @config-option-init="configOptionInit"
    @set-expand="$emit('setExpand')"
    @save-chart-config="saveChartCfg"
    @build-chart-series-data="buildChartData"
  >
    <div style="display: flex;flex-direction: column;height: 100%;">
      <SidebarTabs
        v-model="activeConfig"
        :menu="chartMenu"
      />
      <div class="pie-basic-config ">
        <div
          v-show="activeConfig==='basic'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"
            @click="copyConfig('pieBasic')"
          >
            复制基础配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('pieBasic')"
          >
            粘贴基础配置
          </el-button>
        </div>
        <ChartTypeChange
          v-show="activeConfig==='basic'"
          style="margin-bottom: 10px;"
          :type="configs.branchType"
          @change-type="changeChartType"
        />
        <component
          :is="name+'-config'"
          v-for="(name,index) in componentsList"
          v-show="activeConfig===configType[name]"
          :key="index"
          :ref="name+'form'"
          :save-able="saveAble"
          :is-expand="name==='legend'"
          @legend-show-change="legendShowChange"
          @chart-change="getChartBasicConfigData"
        />
        <div
          v-if="configs.branchType!='progressChart'"
          v-show="activeConfig==='basic'"
        >
          <ColorsPicker
            v-model="color"
            name="饼图颜色"
            :color-list="colorsList"
            @color-change="getChartBasicConfigData"
          />
        </div>
        <div
          v-show="activeConfig==='series'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"
            @click="copyConfig('pieSeries')"
          >
            复制系列配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('pieSeries')"
          >
            粘贴系列配置
          </el-button>
        </div>
        <div
          v-if="configs.branchType==='ringChart2' "
          v-show="activeConfig==='basic'"
          style="padding-top: 10px;"
        >
          <el-form label-width="80px">
            <el-form-item label="饼图半径">
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.radius[0]"
                      size="small"
                      controls-position="right"
                      class="input-number-box-perc"
                      :min="0"
                      :max="100"
                      @change="getChartBasicConfigData"
                    />
                    <span class="extra-bottom-text"> 内径</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.radius[1]"
                      size="small"
                      controls-position="right"
                      class="input-number-box-perc"
                      :min="0"
                      :max="100"
                      @change="getChartBasicConfigData"
                    />
                    <span class="extra-bottom-text"> 外径</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
            <el-form-item label="中心位置">
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.center[0]"
                      size="small"
                      class="input-number-box-perc"
                      controls-position="right"
                      :min="0"
                      :max="100"
                      @change="getChartBasicConfigData"
                    />
                    <span class="extra-bottom-text"> 水平</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.center[1]"
                      size="small"
                      class="input-number-box-perc"
                      controls-position="right"
                      :min="0"
                      :max="100"
                      @change="getChartBasicConfigData"
                    />
                    <span class="extra-bottom-text"> 垂直</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
          </el-form>
        </div>
        <SeriesConfig
          v-if="configs.branchType==='multiplePieChart'"
          v-show="activeConfig==='series'"
          ref="seriesConfigRef"
          style="padding-top: 10px;"
          @chart-change="getChartBasicConfigData"
        />

        <ProgressSeriesConfig
          v-if="configs.branchType==='progressChart'"
          v-show="activeConfig==='basic'"
          ref="progressSeries"
          style="padding-top: 10px;"
          @pie-series-change="getChartBasicConfigData"
        />
        <PieSeriesConfig
          v-if="configs.branchType==='ringChart'||configs.branchType==='pieChart'"
          v-show="activeConfig==='basic'"
          ref="singleSeries"
          style="padding-top: 10px;"
          @pie-series-change="getChartBasicConfigData"
        />
        <Pie3dConfig
          v-if="configs.branchType==='3dPieChart'"
          v-show="activeConfig==='basic'"
          ref="pie3dRef"
          style="margin-top: 10px;"
          @chart-change="getChartBasicConfigData"
        />
        <ExLegendConfig
          v-show="activeConfig==='basic'&&configs.branchType!='progressChart'"
          ref="exLegendConfig"
          @ex-legend-show-change="exLegendShowChange"
          @chart-change="getChartBasicConfigData"
        />
      </div>
    </div>
  </BKBasicChartConfig>
</template>

<script>
// import { colorList } from '../../configs/chart-cfg'
import ChartTypeChange from '../../chart-common-config/components/chart-type-change.vue'
import SidebarTabs from '../../chart-common-config/components/sidebar-tabs.vue'
import SeriesConfig from '../src/components/series-config.vue'
import { commonItems, commonRules } from '../../configs/common-item'
import { baseChartComponents } from '../../chart-common-config/chart-config/index'
import PieSeriesConfig from './components/pie-series-config.vue'
import { itemsConfig } from './config/config-items'
import ExLegendConfig from '../../chart-common-config/components/legend-config.vue'
import ProgressSeriesConfig from './components/progress-series-config.vue'
import Pie3dConfig from './components/pie3d-config.vue'
import Copy from '../../configs/copy-chart-mixins'
import XEUtils from 'xe-utils'
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
export default {
  name: 'PieChartConfig',
  components: {
    ...baseChartComponents,
    SidebarTabs,
    ProgressSeriesConfig,
    PieSeriesConfig,
    SeriesConfig,
    ChartTypeChange,
    ExLegendConfig,
    Pie3dConfig
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
      seriesList: [],
      componentsList: [],
      form: {
        radius: [90, 100],
        center: [50, 50]
      },
      color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#E6A23C'],
      activeConfig: 'basic',
      pieColorList: [],
      configType: { grid: 'basic', legend: 'legend', tooltip: 'other' }
    }
  },

  computed: {
    chartMenu () {
      if (this.configs.branchType === 'multiplePieChart') {
        return [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '系列', key: 'series', icon: 'icon-a-shujuyuan2' }, { name: '图例', key: 'legend', icon: 'ri-list-unordered' }, { name: '其他', key: 'other', icon: 'icon-qita' }]
      } else if (this.configs.branchType === 'progressChart') {
        return [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '其他', key: 'other', icon: 'icon-qita' }]
      } else {
        return [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '图例', key: 'legend', icon: 'ri-list-unordered' }, { name: '其他', key: 'other', icon: 'icon-qita' }]
      }
    },
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
      return itemsConfig.basicConfig[ele] && this.chartMenu.find(el => { return el.key === this.configType[ele] })
    })
  },
  methods: {
    buildChartData (type, data) {
      if (!this.configs.dataSourceConfig) {
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
        newList.push({ name: series[i], dataId: series[i], type: 'pie' })
      }
      newList.forEach((ele, index) => {
        if (this.seriesList[index]) {
          this.seriesList[index].name = newList[index].name
          this.seriesList[index].dataId = newList[index].dataId
        } else {
          this.seriesList.push(ele)
        }
      })
      if (this.configs.branchType === 'multiplePieChart') {
        this.seriesList = this.seriesList.slice(0, newList.length)
        this.$refs.seriesConfigRef.setSeries(this.seriesList)
        ElMessage.success('已生成系列')
      }
      this.getChartBasicConfigData()
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
    getChartBasicConfigData (e) {
      this.$nextTick(() => {
        this.$refs.basicChartConfig.saveConfig()
      })
    },
    addSeris () {
      this.seriesList.push({ type: 'pie', radius: 100 })
    },
    async saveChartCfg (data) {
      let form = { series: [] }
      this.$nextTick(() => {
        this.componentsList.forEach(name => {
          const formData = this.$refs[name + 'form'][0].saveFormData()
          if (name === 'axis') {
            form = { ...form, ...formData }
          } else if (name === 'tooltip') {
            form[name] = formData.form
            form.customConfig = formData.customConfig
          } else {
            form[name] = formData
          }
        })
        // 色系
        form.color = this.color
        // 额外图例
        form.legendConfig = this.$refs.exLegendConfig.saveFormData()
        const config = utils.deepClone(data)
        if (this.configs.branchType === 'multiplePieChart') {
          form.series = this.$refs.seriesConfigRef.saveFormData()
          config.configOption = { ...config.configOption, ...form }
        } else if (this.configs.branchType === 'progressChart') {
          const { option, series } = this.$refs.progressSeries.saveFormData()
          form = { ...form, ...option }
          form.series = [series]
          config.configOption = { ...config.configOption, ...form }
        } else if (this.configs.branchType === '3dPieChart') {
          const { internalDiameterRatio, viewControl } = this.$refs.pie3dRef.saveFormData()
          form.internalDiameterRatio = internalDiameterRatio
          form.grid3D = { viewControl }
          config.configOption = XEUtils.merge(config.configOption, form)
        } else if (this.configs.branchType === 'ringChart2') {
          form.position = utils.deepClone(this.form)
          config.configOption = XEUtils.merge(config.configOption, form)
        } else {
          form.series = [{ ...this.$refs.singleSeries.saveFormData(), type: 'pie' }]
          config.configOption = { ...config.configOption, ...form }
        }
        this.seriesList = utils.deepClone(form.series)
        this.$emit('saveChartCfg', config)
      })
    },
    legendShowChange (e) {
      if (e) {
        this.$refs.exLegendConfig.setFormData({ show: !e })
        this.getChartBasicConfigData()
      }
    },
    exLegendShowChange (e) {
      if (e) {
        this.$refs.legendform[0].setFormData({ show: !e })
        this.getChartBasicConfigData()
      }
    },
    configOptionInit () {
      this.$nextTick(() => {
        if (!this.$refs.basicChartConfig) {
          return
        }
        const option = utils.deepClone(this.configs.configOption)
        if (option) {
          this.componentsList.forEach(name => {
            if (name === 'axis') {
              this.$refs[name + 'form'][0].setFormData({ yAxis: option?.yAxis || {}, xAxis: option?.xAxis || {} })
            } else if (name === 'tooltip') {
              this.$refs[name + 'form'][0].setFormData(option[name] || {}, option.customConfig || {})
            } else {
              this.$refs[name + 'form'][0].setFormData(option[name] || {})
            }
          })
          this.color = option?.color
        }
        this.seriesList = utils.deepClone(option?.series || [])
        if (this.configs.branchType === 'multiplePieChart') {
          this.$refs.seriesConfigRef.setSeries(this.seriesList)
        } else if (this.configs.branchType === 'progressChart') {
          this.$refs.progressSeries.setFormData({ option: { title: option.title, polar: option.polar }, series: option.series[0] })
        } else if (this.configs.branchType === '3dPieChart') {
          this.$refs.pie3dRef.setFormData({ internalDiameterRatio: option?.internalDiameterRatio || 0.5, viewControl: option?.grid3D.viewControl || {} })
        } else if (this.configs.branchType === 'ringChart2') {
          if (option?.position) { this.form = Object.assign(this.form, option?.position) }
        } else {
          this.$refs.singleSeries.setFormData(this.seriesList[0])
        }
        // 额外图例
        this.$refs.exLegendConfig.setFormData(option?.legendConfig || {})
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

.pie-basic-config {
  width: 100%;
  padding: 10px 16px 10px 24px;
  // padding: 0 4px 12px 12px;
  overflow: auto;

  :deep(.el-collapse-item__arrow) {
    margin-right: 0;
  }
}

:deep() {

  .pie-color .el-form-item__content {
    display: flex;
    flex-wrap: wrap;
    margin-left: 0;
  }

  .color-group {
    position: relative;
    display: flex;
  }

  .color-group:hover {

    .color-del-btn {
      display: block;
    }
  }

  .color-del-btn {
    position: absolute;
    top: -15px;
    right: -1px;
    display: none;
    color: #f56c6c;
  }
}
</style>
