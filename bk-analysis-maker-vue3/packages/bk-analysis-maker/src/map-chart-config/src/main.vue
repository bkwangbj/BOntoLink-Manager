<template>
  <BKBasicChartConfig
    v-if="showFlag"
    ref="basicChartConfig"
    :configs="configs"
    :page-config="pageConfig"
    :save-able="saveAble"
    :items="items"
    :rules="rules"
    :tjb-u-r-l="tjbURL"
    v-bind="$attrs"
    @set-expand="$emit('setExpand')"
    @save-chart-config="saveChartCfg"
    @build-chart-series-data="buildChartData"
    @config-option-init="configOptionInit"
  >
    <div class="map-config-container">
      <SidebarTabs
        v-model="activeConfig"
        :menu="chartMenu"
      />
      <div class="map-config-main">
        <div
          v-show="activeConfig==='basic'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"
            @click="copyConfig('mapBasic')"
          >
            复制基础配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('mapBasic')"
          >
            粘贴基础配置
          </el-button>
        </div>
        <component
          :is="name+'-config'"
          v-for="(name,index) in componentsList"
          v-show="activeConfig===configType[name]"
          :key="index"
          :ref="name+'form'"
          :save-able="saveAble"
          @chart-change="getChartBasicConfigData"
        />
        <VisualMap
          v-show="activeConfig==='colorField'"
          ref="visualMapRef"
          v-model="showVisualMap"
          :form-data="visualMap"
          @chart-change="getChartBasicConfigData"
          @show-change="visualMapChange"
        />
        <ScatterConfig
          v-show="activeConfig==='scatter'"
          ref="scatterRef"
          v-model="showScatter"
          :series="scatterSeries"
          @show-change="scatterChange"
          @scatter-change="getChartBasicConfigData"
        />
        <BasicConfig
          v-show="activeConfig==='basic'"
          ref="basicConfigRef"
          :form-data="geo"
          @chart-change="getChartBasicConfigData"
        />
      </div>
    </div>
  </BKBasicChartConfig>
</template>

<script>
import { commonItems, commonRules } from '../../configs/common-item'
import SidebarTabs from '../../chart-common-config/components/sidebar-tabs.vue'
import originList from './config/default-data'
import VisualMap from './components/visual-map.vue'
import { itemsConfig } from './config/config-items'
import { getInterval } from '../../configs/common-func'
import { baseChartComponents } from '../../chart-common-config/chart-config/index'
import BasicConfig from './components/basic-config.vue'
import Copy from '../../configs/copy-chart-mixins'
import ScatterConfig from './components/scatter-config.vue'
import { utils } from 'efficient-suite'
import areaData from '../../configs/json/area.json'
export default {
  name: 'MapChartConfig',
  components: {
    ...baseChartComponents,
    SidebarTabs,
    ScatterConfig,
    BasicConfig,
    VisualMap
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
    mapSource: {
      type: Array,
      default: () => []
    },
    tjbURL: {
      type: String,
      default: ''
    }
  },
  emits: ['setExpand', 'saveChartCfg'],
  data () {
    return {
      showFlag: false,
      items: [],
      visualMap: null,
      showScatter: false,
      showVisualMap: false,
      componentsList: [],
      scatterSeries: {},
      geo: null,
      rules: {
        ...commonRules,
        mapName: [{ required: true, message: '请输入地图', trigger: 'blur' }]
        // seriesName: [{ required: true, message: '请输入图例名称', trigger: 'blur' }]
      },
      chartMenu: [{ name: '图表', key: 'basic', icon: 'icon-tuxing' }, { name: '颜色映射', key: 'colorField', icon: 'icon-yangshi' }, { name: '散点配置', key: 'scatter', icon: 'icon-yangshi' }, { name: '其他', key: 'other', icon: 'icon-qita' }],
      activeConfig: 'basic',
      configType: { tooltip: 'other' }
    }
  },
  async created () {
    this.componentsList = Object.keys(itemsConfig.basicConfig).filter(ele => {
      return itemsConfig.basicConfig[ele]
    })
    let data = utils.deepClone(areaData)
    data = [...data, ...this.mapSource]
    data = data.filter(ele => { return ele.level !== 'district' }).map(ele => { return { ...ele, label: ele.name, value: ele.adcode.toString() } })
    const list = this.listToTree(JSON.parse(JSON.stringify(data)))
    const items = commonItems.filter(c => c.isBasic)
    const index = items.findIndex(c => c.field === 'isShowTitle')
    this.items = items.slice(0, index).concat([{ type: 'el-cascader', label: '地图', field: 'mapName', props: { options: list, props: { emitPath: false, checkStrictly: true } }, eventsType: 'basic' },
      { type: 'input', label: '图例名', field: 'seriesName', eventsType: 'basic' }, { type: 'el-switch', label: '开启下钻', field: 'isDeep', eventsType: 'basic' }, { type: 'select', label: '下钻层级', field: 'deepLevel', props: { mock: [{ label: '1', value: '1' }, { label: '2', value: '2' }, { label: '3', value: '3' }] }, eventsType: 'basic' }]).concat(items.slice(index))

    this.$nextTick(() => {
      this.configOptionInit()
    })
    this.showFlag = true
  },

  methods: {
    buildChartData (type, data) {
      const option = utils.deepClone(this.configs.configOption)

      const { max, min } = getInterval(data)
      option.visualMap.max = max
      option.visualMap.min = min
      this.visualMap = option.visualMap

      this.$nextTick(() => {
        this.$refs.basicChartConfig.saveConfig()
      })
    },
    visualMapChange (v) {
      if (v && this.configs.branchType === 'solidtMap') {
        this.showScatter = false
      }
      this.getChartBasicConfigData()
    },
    scatterChange (v) {
      if (v && this.configs.branchType === 'solidtMap') {
        this.showVisualMap = false
      }
      this.getChartBasicConfigData()
    },
    configOptionInit () {
      this.$nextTick(() => {
        const option = utils.deepClone(this.configs.configOption)

        this.geo = option.geo
        this.showScatter = option?.showScatter || false
        this.showVisualMap = option?.showVisualMap || false
        this.visualMap = option.visualMap

        this.scatterSeries = option?.scatterSeries || []

        this.componentsList.forEach(name => {
          if (name === 'tooltip') {
            this.$refs[name + 'form'][0].setFormData(option[name] || {}, option.customConfig || {})
          } else {
            this.$refs[name + 'form'][0].setFormData(this.configs.configOption[name] || {})
          }
        })
      })
    },
    getChartBasicConfigData () {
      this.$nextTick(() => {
        this.$refs.basicChartConfig.saveConfig()
      })
    },
    addSeris () {
      let index = this.seriesList.length
      if (index >= originList.length) {
        index = index % originList.length
        if (index < 0) {
          index = 0
        }
      }
      this.seriesList.push({ color: originList[index].color })
    },

    delSeris (index) {
      this.seriesList.splice(index, 1)
    },
    async saveChartCfg (data) {
      const { geo } = this.$refs.basicConfigRef.saveFormData()
      const form = {}
      const config = utils.deepClone(data)
      this.componentsList.forEach(name => {
        const formData = this.$refs[name + 'form'][0].saveFormData()
        if (name === 'tooltip') {
          form[name] = formData.form
          form.customConfig = formData.customConfig
        } else {
          form[name] = formData
        }
      })
      form.showVisualMap = this.showVisualMap
      form.showScatter = this.showScatter
      const { visualMap } = this.$refs.visualMapRef.saveFormData()

      config.configOption = Object.assign(config.configOption, { visualMap, geo, ...form })

      const scatterSeries = this.$refs.scatterRef.saveFormData()
      config.configOption = Object.assign(config.configOption, { scatterSeries: [scatterSeries], geo, ...form })

      this.$emit('saveChartCfg', config)
    },
    listToTree (data) {
      // * 先生成parent建立父子关系
      let obj = {}
      data.forEach((item) => {
        obj[item.adcode] = item
      })

      const parentList = []
      data.forEach((item) => {
        const parent = obj[item.parent]
        if (parent) {
          // * 当前项有父节点
          parent.children = parent.children || []
          parent.children.push(item)
        } else {
          parentList.push(item)
        }
      })
      obj = {}
      return parentList
    }
  }
}
</script>

<style lang="scss" scoped>
// @import "../../styles/index.css";
.map-config-container {
  display: flex;
  width: 100%;
  height: 100%;

  .map-config-main {
    flex: 1;
    padding: 10px;
    overflow: auto;
  }
}
</style>
