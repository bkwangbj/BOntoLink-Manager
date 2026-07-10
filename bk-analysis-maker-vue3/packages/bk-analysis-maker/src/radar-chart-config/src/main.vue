<template>
  <BKBasicChartConfig
    ref="basicChartConfig"
    :configs="configs"
    :page-config="pageConfig"
    :save-able="saveAble"
    :rules="rules"
    :tjb-u-r-l="tjbURL"
    v-bind="$attrs"
    @build-chart-series-data="buildChartData"
    @set-expand="$emit('setExpand')"
    @config-option-init="configOptionInit"
    @save-chart-config="saveChartCfg"
  >
    <div
      class="d-flex"
      style="flex-direction: column;height: 100%;overflow: auto;"
    >
      <SidebarTabs
        v-model="activeConfig"
        :menu="chartMenu"
      />
      <div class="radar-basic-config ">
        <div
          v-show="activeConfig==='basic'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"
            @click="copyConfig('radarBasic')"
          >
            复制基础配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('radarBasic')"
          >
            粘贴基础配置
          </el-button>
        </div>

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
        <SeriesConfig

          v-show="activeConfig==='series'"

          ref="seriesConfigRef"
          :series-length="seriesList.length||0"
          :branch-type="configs.branchType"
          @chart-change="getChartBasicConfigData"
        />
        <AxisConfig

          v-show="activeConfig==='axis'"
          ref="axisConfigRef"
          :series-length="seriesList.length||0"
          :branch-type="configs.branchType"
          @chart-change="getChartBasicConfigData"
        />
      </div>
    </div>
  </BKBasicChartConfig>
</template>

<script>
import SidebarTabs from '../../chart-common-config/components/sidebar-tabs.vue'
import { commonItems, commonRules } from '../../configs/common-item'
import { itemsConfig } from './config/config-items'
import { baseChartComponents } from '../../chart-common-config/chart-config/index'
import SeriesConfig from './components/series-config.vue'
import AxisConfig from './components/axis-config.vue'
import Copy from '../../configs/copy-chart-mixins'
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
export default {
  name: 'RadarChartConfig',
  components: {
    ...baseChartComponents,
    SidebarTabs,
    SeriesConfig,
    AxisConfig

  },
  mixins: [Copy],
  provide () {
    return {
      getThemeFont: () => this.configs?.chartTheme?.textColor || '#000'
    }
  },
  inject: ['getSaveAble'],
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
      colorList: [],
      seriesList: [],
      activeConfig: 'basic',
      configType: { grid: 'basic', legend: 'basic', axis: 'axis', tooltip: 'other' },
      chartMenu: [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '坐标轴', key: 'axis', icon: 'icon-zuobiao' }, { name: '系列', key: 'series', icon: 'icon-a-shujuyuan2' }, { name: '其他', key: 'other', icon: 'icon-qita' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
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
        newList.push({ name: series[i], dataId: series[i], type: 'radar' })
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
    configOptionInit () {
      this.$nextTick(() => {
        if (this.configs.configOption) {
          if (!this.$refs.seriesConfigRef) {
            return
          }
          this.componentsList.forEach(name => {
            if (name === 'axis') {
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
        this.$refs.axisConfigRef.setFormData(this.configs.configOption?.radar || [])
        this.$refs.seriesConfigRef.setSeries(this.seriesList)
      })
    },
    getChartBasicConfigData () {
      if (this.$refs.basicChartConfig) {
        this.$refs.basicChartConfig.saveConfig()
      }
    },
    colorsChange () {
      this.$nextTick(() => {
        this.$refs.basicChartConfig.saveConfig()
      })
    },
    saveChartCfg (data) {
      let form = {}
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
      const radar = this.$refs.axisConfigRef.saveFormData()
      form.radar = radar
      const series = this.$refs.seriesConfigRef.saveFormData()
      form.series = series
      form.color = this.colorList
      this.seriesList = utils.deepClone(form.series)
      const config = utils.deepClone(data)
      config.configOption = Object.assign(config.configOption, form)
      this.$emit('saveChartCfg', config)
    },
    addSeris (formData) {
      const form = {
        type: 'radar',
        name: formData.name,
        dataId: formData.dataId,
        symbol: 'circle',

        symbolSize: 4,
        areaStyle: {
          show: false,
          color: null,
          opacity: 0.7
        },
        itemStyle: {
          color: null
        },
        lineStyle: {
          width: 1,
          color: null,
          type: 'solid'
        }

      }

      this.seriesList.push(form)
    }
  },
  delSeris (index) {
    this.seriesList.splice(index, 1)
    if (this.seriesList.length === 1) {
      this.seriesList[0].basic.yAxisIndex = 0
    }
  }
}
</script>

<style lang="scss" scoped>
// @import "../../styles/index.css";
.radar-basic-config {
  width: 100%;
  padding: 10px 16px 10px 24px;
  overflow: auto;
}

</style>
