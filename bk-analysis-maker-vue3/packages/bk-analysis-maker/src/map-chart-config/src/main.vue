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
      <div class="map-config-main cfg-scroll" ref="cfgScroll">
        <div id="cfgsec-basic" class="cfg-sec">
          <div class="cfg-sec-title">图表</div>
          <div class="copy-button-box">
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
          <BasicConfig
            ref="basicConfigRef"
            :form-data="geo"
            @chart-change="getChartBasicConfigData"
          />
        </div>
        <div id="cfgsec-colorField" class="cfg-sec">
          <div class="cfg-sec-title">颜色映射</div>
          <VisualMap
            ref="visualMapRef"
            v-model="showVisualMap"
            :form-data="visualMap"
            @chart-change="getChartBasicConfigData"
            @show-change="visualMapChange"
          />
        </div>
        <div id="cfgsec-scatter" class="cfg-sec">
          <div class="cfg-sec-title">散点配置</div>
          <ScatterConfig
            ref="scatterRef"
            v-model="showScatter"
            :series="scatterSeries"
            @show-change="scatterChange"
            @scatter-change="getChartBasicConfigData"
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
            @chart-change="getChartBasicConfigData"
          />
        </div>
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

  computed: {
    otherComps () { return this.componentsList.filter(n => this.configType[n] === 'other') }
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

.map-config-main {
  flex: 1;
  min-width: 0;
  padding: 12px 16px 12px 12px;
}
</style>
