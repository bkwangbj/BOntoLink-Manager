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
    <div class="cfg-anchor-wrap">
      <div class="cfg-anchor-nav">
        <div
          v-for="t in chartMenu"
          :key="t.key"
          :class="['cfg-anchor-item', activeConfig===t.key && 'is-on']"
          @click="scrollToSec(t.key)"
        >
          <i class="am-iconfont cfg-anchor-ic" :class="t.icon" />
          <span>{{ t.name }}</span>
        </div>
      </div>
      <div class="radar-basic-config cfg-scroll" ref="cfgScroll">
        <div id="cfgsec-basic" class="cfg-sec">
          <div class="cfg-sec-title">图表</div>
          <div class="copy-button-box">
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
          <div style="margin-top: 10px;">
            <ColorsPicker
              v-model="colorList"
              name=" 色系配置"
              :color-list="colorsList"
              @color-change="colorsChange"
            />
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
        </div>
        <div id="cfgsec-axis" class="cfg-sec">
          <div class="cfg-sec-title">坐标轴</div>
          <AxisConfig
            ref="axisConfigRef"
            :series-length="seriesList.length||0"
            :branch-type="configs.branchType"
            @chart-change="getChartBasicConfigData"
          />
        </div>
        <div id="cfgsec-series" class="cfg-sec">
          <div class="cfg-sec-title">系列</div>
          <SeriesConfig
            ref="seriesConfigRef"
            :series-length="seriesList.length||0"
            :branch-type="configs.branchType"
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
      componentsList: [],
      activeConfig: 'basic',
      configType: { grid: 'basic', legend: 'basic', axis: 'axis', tooltip: 'other' },
      chartMenu: [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '坐标轴', key: 'axis', icon: 'icon-zuobiao' }, { name: '系列', key: 'series', icon: 'icon-a-shujuyuan2' }, { name: '其他', key: 'other', icon: 'icon-qita' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    },
    basicComps () { return this.componentsList.filter(n => this.configType[n] === 'basic') },
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
      this.scrollEl = this.$refs.cfgScroll?.closest('.base-flow') || this.$refs.cfgScroll
      if (this.scrollEl) this.scrollEl.addEventListener('scroll', this.onCfgScroll, { passive: true })
    })
  },
  beforeUnmount () {
    if (this.scrollEl) this.scrollEl.removeEventListener('scroll', this.onCfgScroll)
  },
  methods: {
    scrollToSec (key) {
      const sc = this.scrollEl
      const el = document.getElementById('cfgsec-' + key)
      if (sc && el) {
        this.activeConfig = key
        const top = el.getBoundingClientRect().top - sc.getBoundingClientRect().top + sc.scrollTop
        sc.scrollTo({ top: Math.max(0, top - 8), behavior: 'smooth' })
      }
    },
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

.radar-basic-config {
  flex: 1;
  min-width: 0;
  padding: 12px 16px 12px 12px;
}

</style>
