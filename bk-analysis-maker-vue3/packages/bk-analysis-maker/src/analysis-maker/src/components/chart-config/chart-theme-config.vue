<template>
  <el-collapse>
    <el-collapse-item>
      <template #title>
        图类样式
      </template>
      <EfForm
        ref="chartConfigForm"
        :items="chartConfigItems"
        :has-submit="false"
        :disabled="disabled"
        :has-reset="false"
        size="small"
        label-width="90px"
        class="inline-col-form chart-col-form"
      />
    </el-collapse-item>
    <el-collapse-item class="chart-collapse-item">
      <template #title>
        坐标轴样式
      </template>
      <el-collapse>
        <el-collapse-item>
          <template #title>
            x轴
          </template>
          <EfForm
            ref="aAxisConfigForm"
            :items="xAxisConfigItems"
            :has-submit="false"
            :disabled="disabled"
            :has-reset="false"
            size="small"
            label-width="90px"
            class="inline-col-form chart-col-form"
          />
        </el-collapse-item>
        <el-collapse-item>
          <template #title>
            y轴
          </template>
          <EfForm
            ref="yAxisConfigForm"
            :items="yAxisConfigItems"
            :has-submit="false"
            :disabled="disabled"
            :has-reset="false"
            size="small"
            label-width="90px"
            class="inline-col-form chart-col-form"
          />
        </el-collapse-item>
      </el-collapse>
    </el-collapse-item>
    <el-collapse-item>
      <template #title>
        表类样式
      </template>
      <EfForm
        ref="tableConfigForm"
        :items="tableConfigItems"
        :has-submit="false"
        :disabled="disabled"
        :has-reset="false"
        size="small"
        label-width="90px"
        class="inline-col-form chart-col-form"
      />
    </el-collapse-item>
    <el-collapse-item>
      <template #title>
        地图样式
      </template>
      <EfForm
        ref="mapConfigForm"
        :items="mapConfigItems"
        :has-submit="false"
        :disabled="disabled"
        :has-reset="false"
        size="small"
        label-width="90px"
        class="inline-col-form chart-col-form"
      />
    </el-collapse-item>
  </el-collapse>
</template>

<script lang="jsx">
import ColorsPicker from '../../../../chart-common-config/components/colors-picker.vue'
import TooltipConfig from './tooltip-config.vue'
import BarThemeConfig from './bar-theme-config.vue'
import LineThemeConfig from './line-theme-config.vue'
import AxisTickThemeConfig from './axisTick-theme-config.vue'
import AxisLabelThemeConfig from './axisLabel-theme-config.vue'
import AxisLineThemeConfig from './axisLine-theme-config.vue'
import NameThemeConfig from './name-theme-config.vue'
import LegendThemeConfig from './legend-theme-config.vue'
import TableThemeConfig from './table-theme-config.vue'
import TableHeaderConfig from './table-header-config.vue'
import MapThemeConfig from './map-theme-config.vue'
import PieThemeConfig from './pie-theme-config.vue'
import { utils } from 'efficient-suite'
export default {
  inject: ['getPreThemeList', 'getThemeConfig'],
  props: {
    theme: {
      type: Object,
      default: () => ({})
    },
    preThemeData: {
      type: Object,
      default: () => ({})
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['chartStyleChange'],
  data () {
    return {
      chartConfigItems: [],
      xAxisConfigItems: [],
      yAxisConfigItems: [],
      tableConfigItems: [],
      mapConfigItems: [],
      switchMap: {},
      colorList: [],
      colorsList: [],
      colorListRef: null,
      tooltipRef: null,
      legendRef: null,
      barStyleRef: null,
      lineStyleRef: null,
      pieStyleRef: null,
      tablebodyRef: null,
      tableheaderRef: null,
      mapnormalRef: null,
      mapselectRef: null,
      mapemphasisRef: null,
      xAxisnameStyleRef: null,
      xAxisaxisLabelRef: null,
      xAxisaxisLineRef: null,
      xAxissplitLineRef: null,
      xAxisaxisTickRef: null,
      yAxisnameStyleRef: null,
      yAxisaxisLabelRef: null,
      yAxisaxisLineRef: null,
      yAxissplitLineRef: null,
      yAxisaxisTickRef: null
    }
  },
  methods: {
    async init () {
      this.chartStyle = utils.deepClone(this.theme.chartStyle)
      this.colorList = utils.deepClone(this.chartStyle.themeStyle.colorList)
      const list = [this.colorList]

      this.colorsList = list.concat(this.chartStyle?.defaultColors)
      this.initItems()
      await this.$nextTick()
      this.tooltipRef.setFormData(this.chartStyle?.tooltip || {})
      this.barStyleRef.setFormData(this.chartStyle?.barStyle || {})
      this.lineStyleRef.setFormData(this.chartStyle?.lineStyle || {})
      this.legendRef.setFormData(this.chartStyle?.legend || {})
      this.pieStyleRef.setFormData(this.chartStyle?.pieStyle || {})
      for (const item of this.xAxisConfigItems) {
        this['xAxis' + item.field + 'Ref'].setFormData(this.chartStyle?.xAxis?.[item.field] || {})
      }
      for (const item of this.yAxisConfigItems) {
        this['yAxis' + item.field + 'Ref'].setFormData(this.chartStyle?.yAxis?.[item.field] || {})
      }
      for (const item of this.tableConfigItems) {
        this['table' + item.field + 'Ref'].setFormData(this.chartStyle?.table?.[item.field] || { styles: {}, props: {} })
      }
      for (const item of this.mapConfigItems) {
        this['map' + item.field + 'Ref'].setFormData(this.chartStyle?.map?.[item.field] || { })
      }
    },
    initItems () {
      const items1 = [
        {
          label: '色系配置',
          field: 'colorList',
          itemSlots: {
            default: () => {
              return [
              <ColorsPicker style="width: 100%;" ref={el => { this.colorListRef = el }} editAble={this.switchMap.colorList} v-model={this.colorList}
                color-list={this.colorsList} onColorChange={(event) => this.chartChange({ path: '', field: 'colorList' })}></ColorsPicker>
              ]
            }
          }
        },
        {
          label: '提示框',
          field: 'tooltip',
          itemSlots: {
            default: () => {
              return [
              <TooltipConfig style="width: 100%;" ref={el => { this.tooltipRef = el }} editAble={this.switchMap.tooltip}
                onChartChange={(event) => this.chartChange({ path: '', field: 'tooltip' })}></TooltipConfig>
              ]
            }
          }
        },
        {
          label: '图例',
          field: 'legend',
          itemSlots: {
            default: () => {
              return [
              <LegendThemeConfig style="width: 100%;" ref={el => { this.legendRef = el }} editAble={this.switchMap.legend}
                onChartChange={(event) => this.chartChange({ path: '', field: 'legend' })}></LegendThemeConfig>
              ]
            }
          }
        },
        {
          label: '柱图样式',
          field: 'barStyle',
          itemSlots: {
            default: () => {
              return [
              <BarThemeConfig style="width: 100%;" ref={el => { this.barStyleRef = el }} editAble={this.switchMap.barStyle}
                onChartChange={(event) => this.chartChange({ path: '', field: 'barStyle' })}></BarThemeConfig>
              ]
            }
          }
        }, {
          label: '折线样式',
          field: 'lineStyle',
          itemSlots: {
            default: () => {
              return [
              <LineThemeConfig style="width: 100%;" ref={el => { this.lineStyleRef = el }} editAble={this.switchMap.lineStyle}
                onChartChange={(event) => this.chartChange({ path: '', field: 'lineStyle' })}></LineThemeConfig>
              ]
            }
          }
        },
        {
          label: '饼图样式',
          field: 'pieStyle',
          itemSlots: {
            default: () => {
              return [
              <PieThemeConfig style="width: 100%;" ref={el => { this.pieStyleRef = el }} editAble={this.switchMap.pieStyle}
                onChartChange={(event) => this.chartChange({ path: '', field: 'pieStyle' })}></PieThemeConfig>
              ]
            }
          }
        }
      ]
      const itemAxis = [
        {
          label: '轴单位样式',
          field: 'nameStyle'
        },
        {
          label: '轴标签样式',
          field: 'axisLabel'

        },
        {
          label: '轴线样式',
          field: 'axisLine'
        },
        {
          label: '网格线样式',
          field: 'splitLine'
        },
        {
          label: '刻度线样式',
          field: 'axisTick'
        }

      ]
      const items2 = [
        {
          label: '表体配置',
          field: 'body',
          itemSlots: {
            default: () => {
              return [
              <TableThemeConfig style="width: 100%;" ref={el => { this.tablebodyRef = el }} editAble={this.switchMap.tablebody}
                onChartChange={(event) => this.chartChange({ path: 'table', field: 'body' })}></TableThemeConfig>
              ]
            }
          }
        }, {
          label: '表头配置',
          field: 'header',
          itemSlots: {
            default: () => {
              return [
              <TableHeaderConfig style="width: 100%;" ref={el => { this.tableheaderRef = el }} editAble={this.switchMap.tableheader}
                onChartChange={(event) => this.chartChange({ path: 'table', field: 'header' })}></TableHeaderConfig>
              ]
            }
          }
        }]
      const items3 = [
        {
          label: '通常样式',
          field: 'normal',
          itemSlots: {
            default: () => {
              return [<MapThemeConfig style="width: 100%;" ref={el => { this.mapnormalRef = el }} editAble={this.switchMap.mapnormal}
              onChartChange={(event) => this.chartChange({ path: 'map', field: 'normal' })} ></MapThemeConfig>]
            }
          }
        }, {
          label: '选中样式',
          field: 'select',
          itemSlots: {
            default: () => {
              return [
                [<MapThemeConfig style="width: 100%;" ref={el => { this.mapselectRef = el }} editAble={this.switchMap.mapselect}
                onChartChange={(event) => this.chartChange({ path: 'map', field: 'select' })} ></MapThemeConfig>]
              ]
            }
          }
        }, {
          label: '高亮样式',
          field: 'emphasis',
          itemSlots: {
            default: () => {
              return [<MapThemeConfig style="width: 100%;" ref={el => { this.mapemphasisRef = el }} editAble={this.switchMap.mapemphasis}
              onChartChange={(event) => this.chartChange({ path: 'map', field: 'emphasis' })} ></MapThemeConfig>]
            }
          }
        }
      ]
      const map = {}
      items1.forEach(item => {
        if (item.field !== 'colorList') {
          map[item.field] = !this.compareEqual(this.theme.chartStyle[item.field], this.preThemeData.chartStyle[item.field])
        } else {
          map[item.field] = !this.compareEqual(this.theme.chartStyle.themeStyle.colorList, this.preThemeData.chartStyle.themeStyle.colorList)
        }
        // map[item.field] = this.theme.globalCss[item.field] !== this.preThemeData.globalCss[item.field]
        if (item.props) {
          item.props.disabled = !map[item.field]
        }
      })
      itemAxis.forEach(item => {
        map['xAxis' + item.field] = !this.compareEqual(this.theme.chartStyle?.xAxis?.[item.field], this.preThemeData.chartStyle.xAxis?.[item.field])
      })

      itemAxis.forEach(item => {
        map['yAxis' + item.field] = !this.compareEqual(this.theme.chartStyle?.yAxis?.[item.field], this.preThemeData.chartStyle.yAxis?.[item.field])
      })
      items2.forEach(item => {
        map['table' + item.field] = !this.compareEqual(this.theme.chartStyle.table?.[item.field], this.preThemeData.chartStyle.table?.[item.field])
      })
      items3.forEach(item => {
        map['map' + item.field] = !this.compareEqual(this.theme.chartStyle.map?.[item.field], this.preThemeData.chartStyle.map?.[item.field])
      })
      this.switchMap = map
      this.getAxisItems('xAxis')
      this.getAxisItems('yAxis')
      this.tableConfigItems = this.formmaterLabelItems(items2, 'table')
      this.chartConfigItems = this.formmaterLabelItems(items1, '')
      this.mapConfigItems = this.formmaterLabelItems(items3, 'map')
    },
    chartChange ({ path = '', field }) {
      let form = { }
      if (path) {
        form = this[path + field + 'Ref'].saveFormData()
      } else {
        if (field === 'colorList') {
          form = utils.deepClone(this.colorList)
          path = 'themeStyle'
        } else if (field !== 'colorList') {
          form = this[field + 'Ref'].saveFormData()
        }
      }
      this.$emit('chartStyleChange', { form, path, field })
    },
    async changeSwitchValue (item, value, path) {
      if (!value) {
        let form = {}
        if (path) {
          form = utils.deepClone(this.preThemeData?.chartStyle?.[path]?.[item.field] || {})
          this[path + item.field + 'Ref'].setFormData(this.preThemeData?.chartStyle?.[path]?.[item.field] || {})
        } else {
          if (item.field !== 'colorList') {
            form = utils.deepClone(this.preThemeData?.chartStyle[item.field] || {})
            this[item.field + 'Ref'].setFormData(this.preThemeData?.chartStyle[item.field] || {})
          } else {
            path = 'themeStyle'
            form = utils.deepClone(this.preThemeData?.chartStyle?.themeStyle.colorList)
            this.colorList = this.preThemeData?.chartStyle?.themeStyle.colorList
          }
        }

        await this.$nextTick()

        this.$emit('chartStyleChange', { form, field: item.field, path })
      }
    },
    getAxisItems (path) {
      const item = [
        {
          label: '轴单位样式',
          field: 'nameStyle',
          itemSlots: {
            default: () => {
              return [<NameThemeConfig
              ref={el => { this[`${path}nameStyleRef`] = el }}
              editAble={this.switchMap[path + 'nameStyle']}
              onChartChange={(event) => this.chartChange(({ path, field: 'nameStyle' }))}
            />]
            }
          }
        },
        {
          label: '轴标签样式',
          field: 'axisLabel',
          itemSlots: {
            default: () => {
              return [
              <AxisLabelThemeConfig
                ref={el => { this[`${path}axisLabelRef`] = el }}
                editAble={this.switchMap[path + 'axisLabel']}
                onChartChange={(event) => this.chartChange({ path, field: 'axisLabel' })}
              />
              ]
            }
          }
        },
        {
          label: '轴线样式',
          field: 'axisLine',
          itemSlots: {
            default: () => {
              return [
              <AxisLineThemeConfig
                ref={el => { this[`${path}axisLineRef`] = el }}
                editAble={this.switchMap[path + 'axisLine']}
                onChartChange={(event) => this.chartChange({ path, field: 'axisLine' })}
              />
              ]
            }
          }
        },
        {
          label: '网格线样式',
          field: 'splitLine',
          itemSlots: {
            default: () => {
              return [
              <AxisLineThemeConfig
                ref={el => { this[`${path}splitLineRef`] = el }}
                editAble={this.switchMap[path + 'splitLine']}
                onChartChange={(event) => this.chartChange({ path, field: 'splitLine' })}
              />
              ]
            }
          }
        },
        {
          label: '刻度线样式',
          field: 'axisTick',
          itemSlots: {
            default: () => {
              return [
              <AxisTickThemeConfig
                ref={el => { this[`${path}axisTickRef`] = el }}
                editAble={this.switchMap[path + 'axisTick']}
                onChartChange={(event) => this.chartChange({ path, field: 'axisTick' })}
              />
              ]
            }
          }
        }

      ]
      this[path + 'ConfigItems'] = this.formmaterLabelItems(item, path)
      return item
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
        const str = path ? (path + item.field) : item.field
        if (!item.itemSlots) {
          item.itemSlots = {}
        }
        item.itemSlots.label = () => {
          return [
            <div class="form-label">
              <el-switch class="am-switch" size="small" vModel={this.switchMap[str]} onChange={(value) => this.changeSwitchValue(item, value, path)}></el-switch>
              <span>{item.label}</span>
            </div>
          ]
        }
      }
      return items
    }
  }
}
</script>
<style lang="scss" scoped>
  :deep(.chart-col-form) {

    >.el-form>.el-row>.el-col>.el-form-item {
      margin-bottom: 10px !important;
    }
  }
</style>
