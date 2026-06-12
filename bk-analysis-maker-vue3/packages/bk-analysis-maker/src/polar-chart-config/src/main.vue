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
    <div style="display: flex;height: 100%;">
      <SidebarTabs
        v-model="activeConfig"
        :menu="chartMenu"
      />
      <div class="polar-basic-config ">
        <div
          v-show="activeConfig==='basic'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"
            class="copy_pasted_button"
            @click="copyConfig('polarBasic')"
          >
            复制基础配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('polarBasic')"
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
          :series-length="seriesList.length||0"
          @chart-change="getChartBasicConfigData"
        />

        <div
          v-show="activeConfig==='basic'"
          style="margin-top: 10px;"
        >
          <ColorsPicker
            v-model="colorList"
            name=" 色系配置"
            :color-list="colorsList"
            @color-change="colorsChange"
          />
        </div>
        <div
          v-show="activeConfig==='series'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"

            @click="copyConfig('polarSeries')"
          >
            复制系列配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('polarSeries')"
          >
            粘贴系列配置
          </el-button>
        </div>
        <barSeriesConfig
          v-show="activeConfig==='series'"
          ref="seriesConfigRef"
          :save-able="saveAble"
          :color-list="colorList"
          :series-length="seriesList.length||0"
          :branch-type="configs.branchType"
          :type="configs.type"
          @chart-change="getChartBasicConfigData"
        />
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
  name: 'PolarChartConfig',
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
      activeConfig: 'basic',
      configType: { grid: 'basic', legend: 'basic', 'polar-axis': 'axis', tooltip: 'other' },
      chartMenu: [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '坐标轴', key: 'axis', icon: 'icon-zuobiao' }, { name: '系列', key: 'series', icon: 'icon-a-shujuyuan2' }, { name: '其他', key: 'other', icon: 'icon-qita' }]
    }
  },
  computed: {
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
  methods: {
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
      const item = { type: 'bar', name: formData.name, dataId: formData.dataId, itemStyle: { }, coordinateSystem: 'polar' }

      this.seriesList.push(item)
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
        const formData = this.$refs[name + 'form'][0].saveFormData()
        if (name === 'polar-axis') {
          form = { ...form, ...formData }
        } else if (name === 'tooltip') {
          form[name] = formData.form
          form.customConfig = formData.customConfig
        } else {
          form[name] = formData
        }
      })
      const { series } = this.$refs.seriesConfigRef.saveFormData()
      form.series = series
      form.color = this.colorList
      this.seriesList = utils.deepClone(form.series)
      const config = utils.deepClone(data)
      config.configOption = Object.assign(config.configOption, form)
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
          if (!this.$refs.seriesConfigRef) {
            return
          }
          this.componentsList.forEach(name => {
            if (name === 'polar-axis') {
              this.$refs[name + 'form'][0].setFormData(this.configs.configOption)
            } else if (name === 'tooltip') {
              this.$refs[name + 'form'][0].setFormData(this.configs.configOption[name] || {}, this.configs.configOption.customConfig || {})
            } else {
              this.$refs[name + 'form'][0].setFormData(this.configs.configOption[name] || {})
            }
          })
        }
        this.colorList = this.configs.configOption?.color || this.pageConfig?.themeConfigs?.chartStyle?.themeStyle.colorList || []
        this.seriesList = utils.deepClone(this.configs.configOption?.series || [])
        this.$refs.seriesConfigRef.setSeries(this.seriesList)
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

  .polar-basic-config {
    width: calc(100% - 36px);
    padding: 10px;
    overflow: auto;
  }
</style>
