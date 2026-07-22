<template>
  <div
    ref="rootRef"
    class="full-box"
    style="position: relative;"
  >
    <slot name="content">
      <component
        :is="contentConfig.component"
        v-if="contentConfig && contentConfig.component"
        v-bind="contentConfig.props || {}"
      />
    </slot>
    <div
      v-show="isDeep&&orginMapItem.level!==nowMapItem.level"
      class="map-back"
      @click="mapBack"
    >
      <el-icon class="back-icon">
        <Back />
      </el-icon>上一级
    </div>
    <div
      v-show="hasLabelContent"
      class="map-check"
    >
      <el-checkbox-group
        v-model="checkList"
        @change="checkChange"
      >
        <el-checkbox value="value">
          数值
        </el-checkbox>
        <el-checkbox value="name">
          名称
        </el-checkbox>
      </el-checkbox-group>
    </div>
    <BKChart
      ref="chart"
      :option="option"
      @click="itemClick"
    />
    <a
      ref="downLoadPic"
      href="#"
      style="display: none;"
    />
  </div>
</template>
<script>
// import originList from '../../map-chart-config/src/config/default-data'
import { mixins } from '../../configs/commom-chart'
// import './config/china'
import { labelFormmat } from './config/getFormmat'
import { utils } from 'efficient-suite'
import areaData from '../../configs/json/area.json'
const deepLevelMap = new Map([['country', '1'], ['province', '2'], ['city', '3']])
export default {
  name: 'MapChart',
  components: {

  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {
    mapSource: {
      type: Array,
      default: () => []
    },
    mapPath: {
      type: String,
      default: '/map/'
    }
  },
  data () {
    return {
      option: {},
      areaList: [],
      checkList: [],
      defaultData: [],
      nowMapItem: {},
      isFreshData: false,
      hasLabelContent: false,
      deepLevel: '',
      orginMapItem: {},
      isDeep: true
    }
  },
  watch: {
    relList () {
      this.debouncedCustomResetChart(this.configs)
    }
  },
  async created () {
    const data = utils.deepClone(areaData)
    this.areaList = [...data, ...this.mapSource]
  },
  methods: {

    async itemClick ({ data, name }) {
      if (this.isDeep) {
        const item = this.areaList.find(ele => { return ele.name === name })
        const level = deepLevelMap.get(this.nowMapItem.level)
        let isLevel = false
        if (this.deepLevel) {
          isLevel = Number(this.deepLevel) <= Number(level)
        }

        if (item && item.level !== 'district' && ((this.deepLevel && !isLevel) || !this.deepLevel)) {
          const mapName = item.adcode.toString()
          this.mapInit(mapName)
          this.nowMapItem = item
          this.isFreshData = true
        }
      }
      if (data) {
        this.handleEvent(data.originData, 'click')
      }
    },
    async mapInit (name) {
      let mapJson
      if (name.startsWith('ms-')) {
        mapJson = JSON.parse(this.areaList.find(c => c.adcode === name).MAP_BJ)
      } else {
        mapJson = await fetch(`${this.mapPath}${name}_geojson_full.json`)
        mapJson = await mapJson.json()
      }
      this.$refs.chart.registerMap(this.isRoot(name) ? 'china' : name, mapJson)
      this.$refs.chart.resize && this.$refs.chart.resize()
      const mapName = (this.isRoot(name) ? 'china' : name) || 'china'
      if (this.configs.branchType === 'solidtMap') {
        if (this.option.showScatter) {
          this.option.geo3D.map = mapName
        } else {
          this.option.geo3D.map = mapName
          this.option.series[0].map = mapName
        }
      } else {
        this.option.geo.map = mapName
      }

      this.$refs.chart.dispatchAction({
        type: 'hideTip'
      })
      if (!this.$refs.chart) {
        return
      }
      this.$refs.chart.setOption && this.$refs.chart.setOption(this.option, true)
    },
    async resetMap () {
      if (this.isDeep) {
        this.nowMapItem = { ...this.orginMapItem }
        this.mapInit(this.orginMapItem.adcode.toString())
      }
    },
    checkChange (e) {
      const option = utils.deepClone(this.option)
      if (this.configs.branchType === 'solidtMap') {
        if (option.showScatter) {
          if (e.includes('name') && e.includes('value')) {
            option.geo3D.label.formatter = labelFormmat(this.areaList, this.relList, '1')
          } else if (e.includes('name')) {
            option.geo3D.label.formatter = labelFormmat(this.areaList, this.relList, '2')
          } else if (e.includes('value')) {
            option.geo3D.label.formatter = labelFormmat(this.areaList, this.relList, '3')
          } else {
            option.geo3D.label.formatter = null
          }
        } else {
          if (e.includes('name') && e.includes('value')) {
            option.series[0].label.formatter = labelFormmat(this.areaList, this.relList, '1')
          } else if (e.includes('name')) {
            option.series[0].label.formatter = labelFormmat(this.areaList, this.relList, '2')
          } else if (e.includes('value')) {
            option.series[0].label.formatter = labelFormmat(this.areaList, this.relList, '3')
          } else {
            option.series[0].label.formatter = null
          }
        }
      } else {
        if (e.includes('name') && e.includes('value')) {
          option.geo.label.formatter = labelFormmat(this.areaList, this.relList, '1')
        } else if (e.includes('name')) {
          option.geo.label.formatter = labelFormmat(this.areaList, this.relList, '2')
        } else if (e.includes('value')) {
          option.geo.label.formatter = labelFormmat(this.areaList, this.relList, '3')
        } else {
          option.geo.label.formatter = null
        }
      }

      this.option = option
      this.$refs.chart.setOption && this.$refs.chart.setOption(option, true)
    },
    mapBack () {
      let name = this.nowMapItem.parent
      if (name && name !== null) {
        name = name.toString()
        const item = this.areaList.find(ele => { return ele.adcode.toString() === name })
        this.mapInit(name)
        this.nowMapItem = item
        this.handleEvent({ yid: this.isRoot(item.adcode.toString()) ? '0' : item.adcode.toString(), value: '', name: item.name }, 'click')
        if (name.toString() === this.orginMapItem.adcode.toString()) {
          this.clearParams()
        }
      }
    },
    async customResetChart (config) {
      /*   const data = { seriesName: config.seriesName || '', mapName: config.mapName || 'china', data: this.relList }
      const finalData = this.$utils.getMapChartOption(data)
      // finalData.series[0].name = config.seriesName
      const seriesConfig = config.seriesConfig || originList
      if (seriesConfig.length > 0) {
        finalData.visualMap.splitList = seriesConfig
        finalData.visualMap.color = seriesConfig.map(item => { return item.color })
      }
      this.option = finalData
      // if (Object.keys(this.option).length === 0) {
      // } else {
      //   this.$refs.chart.setOption(finalData, true)
      // } */
      await this.$nextTick()
      if (!this.$refs.chart) {
        return
      }
      let option = {}
      let mapName = ''
      if (this.isFreshData && this.nowMapItem?.adcode && (this.orginMapItem?.adcode.toString() === config?.mapName)) {
        mapName = this.nowMapItem.adcode
      } else {
        this.nowMapItem = this.areaList.find(ele => { return ele.adcode.toString() === config?.mapName })
        this.orginMapItem = { ...this.nowMapItem }
        mapName = config?.mapName
      }
      mapName = mapName.toString()
      this.isDeep = config.isDeep
      this.deepLevel = config.deepLevel
      let data
      if (mapName.startsWith('ms-')) {
        data = JSON.parse(this.areaList.find(c => c.adcode === mapName).MAP_BJ)
      } else {
        data = await fetch(`${this.mapPath}${mapName}_geojson_full.json`)
        data = await data.json()
      }
      this.$refs.chart.registerMap(this.isRoot(mapName) ? 'china' : mapName, data)
      this.$refs.chart.resize && this.$refs.chart.resize()
      if (config.configOption) {
        option = utils.deepClone(config.configOption)
        if (Array.isArray(option.geo)) {
          this.hasLabelContent = !!option.geo[0].labelContent
          option.geo.forEach(ele => {
            ele.map = (this.isRoot(mapName) ? 'china' : mapName) || 'china'
          })
        } else {
          this.hasLabelContent = !!option.geo.labelContent

          option.geo.map = (this.isRoot(mapName) ? 'china' : mapName) || 'china'
        }

        const visiList = this.relList.map(ele => {
          let value = ele.value
          if (isNaN(value)) {
            try {
              value = Number(ele.value.replace(/,/g, ''))
            } catch (error) {

            }
          }
          if (ele.code) {
            const form = this.areaList.find(item => { return item.adcode.toString() === ele.code })
            return { name: form?.name || ele.name, value, originData: ele }
          } else {
            return { name: ele.name, value, originData: ele }
          }
        })
        option.series[0].data = visiList

        const scatterList = this.relList.map(ele => {
          let value = ele.value
          if (isNaN(value)) {
            try {
              value = Number(ele.value.replace(/,/g, ''))
            } catch (error) {

            }
          }
          // 数据自带经纬度时,散点直接落在该经纬度(用于逐点实例散点,如测站坐标)
          if (ele.lng != null && ele.lat != null && !isNaN(Number(ele.lng)) && !isNaN(Number(ele.lat))) {
            return { name: ele.name, value: [Number(ele.lng), Number(ele.lat)].concat(value), originData: ele }
          }
          if (ele.code) {
            const form = this.areaList.find(item => { return item.adcode.toString() === ele.code })
            return { name: form?.name || ele.name, value: [form?.lng, form?.lat].concat(value), originData: ele }
          } else {
            const form = this.areaList.find(item => { return item.name === ele.name })
            return { name: ele.name, value: [form?.lng, form?.lat].concat(value), originData: ele }
          }
        })
        if (option.scatterSeries?.length) {
          option.scatterSeries[0].data = scatterList
          // 数值有差异时,散点大小随数值缩放(6~26);全相同则保持预设固定大小
          const vals = scatterList.map(p => Number(p.value && p.value[2])).filter(v => !isNaN(v))
          const mn = Math.min(...vals)
          const mx = Math.max(...vals)
          if (vals.length && mx > mn) {
            option.scatterSeries[0].symbolSize = (val) => {
              const v = Number(Array.isArray(val) ? val[2] : val)
              if (isNaN(v)) return 8
              return 6 + ((v - mn) / (mx - mn)) * 20
            }
            // 颜色随数值:浅蓝 → 深蓝(与大小一起双编码)
            const lerp = (a, b, t) => {
              const pa = [parseInt(a.slice(1, 3), 16), parseInt(a.slice(3, 5), 16), parseInt(a.slice(5, 7), 16)]
              const pb = [parseInt(b.slice(1, 3), 16), parseInt(b.slice(3, 5), 16), parseInt(b.slice(5, 7), 16)]
              const c = pa.map((x, i) => Math.round(x + (pb[i] - x) * t))
              return `rgb(${c[0]},${c[1]},${c[2]})`
            }
            option.scatterSeries[0].itemStyle = { ...(option.scatterSeries[0].itemStyle || {}) }
            option.scatterSeries[0].itemStyle.color = (params) => {
              const v = Number(params.value && params.value[2])
              if (isNaN(v)) return '#5b9bff'
              return lerp('#a8c8ff', '#0b4bd6', (v - mn) / (mx - mn))
            }
          }
          option.scatterSeries.forEach(ele => {
            ele.emphasis.disabled = !ele.emphasis.show
            ele.select.disabled = !ele.select.show
          })
        }

        option.series[0].name = config?.seriesName || ''
      }

      if (!option.showVisualMap) {
        delete option.visualMap
      }
      if (option.showScatter) {
        option.series = option.series.concat(option.scatterSeries)
        option.tooltip.formatter = function (params) {
          if (typeof (params.value)[2] === 'undefined') {
            return params.name + ' : ' + params.value
          } else {
            return params.name + ' : ' + params.value[2]
          }
        }
      }
      if (this.configs.branchType === 'solidtMap') {
        option.geo.itemStyle.color = option.geo.itemStyle.areaColor
        option.geo.emphasis.itemStyle.color = option.geo.emphasis.itemStyle.areaColor
        option.series.forEach((ele, index) => {
          if (index === 0) {
            ele.type = 'map3D'
            ele.map = option.geo.map
            ele.label = utils.deepClone(option.geo.label)
            ele.emphasis = utils.deepClone(option.geo.emphasis)
            ele.itemStyle = utils.deepClone(option.geo.itemStyle)
            ele.select = utils.deepClone(option.geo.select)
            delete ele.geoIndex
          }
          if (index === 1) {
            ele.type = 'scatter3D'
            ele.geo3DIndex = 0
          }
          ele.coordinateSystem = 'geo3D'
        })
        option.geo3D = utils.deepClone(option.geo)
        if (!option.showVisualMap && option.showScatter) {
          option.series.shift()
        }
        delete option.geo
      }
      this.setCustomOption(option, config)
      this.option = option

      this.isFreshData = false
      if (!this.$refs.chart) {
        return
      }
      this.$refs.chart.setOption && this.$refs.chart.setOption(option, true)
      this.$refs.chart.resize && this.$refs.chart.resize()
      if (this.hasLabelContent) {
        this.checkList = ['name', 'value']
        this.checkChange(this.checkList)
      } else {
        this.checkList = []
      }
    },
    isRoot (code) {
      return code === '100000' || code === 'ms-100000'
    }
  }
}
</script>
<style lang="scss" scoped>
.map-back {
  position: absolute;
  top: 0;
  right: 50px;
  z-index: 38;
  display: flex;
  align-items: center;
  height: 28px;
  padding-right: 5px;
  padding-left: 25px;
  font-size: 12px;
  line-height: 28px;
  color: #999;
  cursor: pointer;

  .back-icon {
    font-size: 14px;
  }
}

.map-check {
  position: absolute;
  top: 50%;
  left: 5px;
  z-index: 99;
  transform: translateY(-50%);

  :deep(.el-checkbox-group) {
    display: flex;
    flex-direction: column;

    .el-checkbox {
      display: flex;
      flex-direction: column;
      margin-bottom: 10px;

      &:last-of-type {
        margin-bottom: 0;
      }

      .el-checkbox__label {
        padding-left: 0;
        font-size: 14px;
        line-height: 14px;
        color: #999;
        text-align: left;
        writing-mode: vertical-lr;
      }
    }
  }
}
</style>
