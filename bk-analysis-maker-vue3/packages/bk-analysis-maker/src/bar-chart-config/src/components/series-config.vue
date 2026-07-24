<template>
  <div class="full-box">
    <div class="ser-switch-row">
      <span class="chart-config-title">无数据列过滤</span>
      <div class="ser-switch-op">
        <el-switch
          v-model="filterDataEmpty"
          :disabled="!saveAble"
          class="am-switch active-switch"
          @click.stop.prevent
          @change="$emit('chartChange')"
        />
        <span
          class="ser-enable"
          :class="{ 'is-on': filterDataEmpty }"
          @click.stop.prevent="saveAble && (filterDataEmpty = !filterDataEmpty, $emit('chartChange'))"
        >启用</span>
      </div>
    </div>
    <div class="ser-switch-row">
      <span class="chart-config-title">自生成系列</span>
      <div class="ser-switch-op">
        <el-switch
          v-model="autoSeries"
          :disabled="!saveAble"
          class="am-switch active-switch"
          size="small"
          @click.stop.prevent
          @change="$emit('chartChange')"
        />
        <span
          class="ser-enable"
          :class="{ 'is-on': autoSeries }"
          @click.stop.prevent="saveAble && (autoSeries = !autoSeries, $emit('chartChange'))"
        >启用</span>
      </div>
    </div>
    <div
      v-if="isLineChart"
      class="ser-switch-row"
    >
      <span class="chart-config-title">平滑曲线</span>
      <div class="ser-switch-op">
        <el-switch
          v-model="smoothAll"
          :disabled="!saveAble"
          class="am-switch active-switch"
          size="small"
          @click.stop.prevent
          @change="toggleSmoothAll"
        />
        <span
          class="ser-enable"
          :class="{ 'is-on': smoothAll }"
          @click.stop.prevent="saveAble && (smoothAll = !smoothAll, toggleSmoothAll(smoothAll))"
        >启用</span>
      </div>
    </div>
    <SeriesPlane
      name="数据系列"
      :series-list="seriesList"
      @del-series="delSeries"
      @copy-series="copySeries"
      @series-change="$emit('chartChange')"
      @add-series="addSeries"
    >
      <template #title="{index}">
        <span>系列{{ index + 1 }}</span>
      </template>
      <template #content="{series,index}">
        <el-form
          :ref="'form'+index"
          size="small"
          :model="series"
          label-width="80px"
          :disabled="!saveAble"
        >
          <el-form-item label="显示系列">
            <el-input
              v-model="series.basic.name"
              size="small"
              @change="formChange"
            />
          </el-form-item>

          <el-form-item label="数据系列">
            <el-input
              v-model="series.basic.dataId"
              size="small"
              @change="formChange"
            />
          </el-form-item>
          <el-form-item
            v-show="seriesLength>1"
            label="所在y轴"
          >
            <el-input-number
              v-model="series.basic.yAxisIndex"
              size="small"
              controls-position="right"
              class="input-number-box"
              :precision="0"
              :min="0"
              :max="1"
              :step="1"
              @change="formChange"
            />
          </el-form-item>
          <el-form-item
            v-if="series.basic.type!=='line'"

            label="颜色"
          >
            <CommonColorPicker
              v-model="series.barSeries.itemStyle.color"
              size="small"
              :color-modes="['monochrome', 'linear-gradient']"
              @change="$emit('chartChange')"
            />
          </el-form-item>

          <el-form-item
            v-if="branchType==='stackedChart'"

            label="堆叠"
          >
            <el-input
              v-model="series.barSeries.stack"
              size="small"
              @change="$emit('chartChange')"
            />
          </el-form-item>
          <el-form-item
            v-if="branchType==='mixChart'"

            label="类型"
          >
            <el-radio-group
              v-model="series.basic.type"
              size="small"
              @change="mixChange($event,index)"
            >
              <el-radio-button value="bar">
                柱体
              </el-radio-button>
              <el-radio-button value="line">
                折线
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <div
            class="d-flex"
            style="margin-bottom: 10px;"
          >
            <div style="width: 80px;">
              <el-switch
                v-model="series.basic.legendConfig.show"
                size="small"
                :disabled="!saveAble"
                class="am-switch active-switch"
                @change="$emit('chartChange')"
              />
              <span class="chart-config-title">
                图例  </span>
            </div>
            <el-form
              size="small"
              label-width="80px"
              :disabled="!series.basic.legendConfig.show||!saveAble"
              inline
            >
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-select
                      v-model="series.basic.legendConfig.icon"
                      clearable
                      placeholder="请选择"
                      size="small"
                      @change="$emit('chartChange')"
                    >
                      <el-option
                        v-for="item in symbolOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                    <span class="extra-bottom-text"> 图形</span>
                  </div>
                </el-col>
              </el-row>
            </el-form>
          </div>
          <el-form-item
            v-if="series.basic.type!=='line'"

            label="柱子宽度"
          >
            <el-input-number
              v-model="series.barSeries.barMaxWidth"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
          </el-form-item>
          <div
            v-if="series.basic.type!=='line'"
            class="d-flex"
          >
            <div style="width: 80px;">
              <el-switch
                v-model="series.barSeries.showBackground"
                class="am-switch active-switch"
                size="small"
                @click.stop.prevent
                @change="$emit('chartChange')"
              />
              <span class="chart-config-title">
                背景色 </span>
            </div>
            <el-form
              size="small"
              label-width="80px"
              :disabled="!series.barSeries.showBackground"
              inline
            >
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <CommonColorPicker
                      v-model="series.barSeries.backgroundStyle.color "

                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 颜色</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="series.barSeries.backgroundStyle.opacity"
                      size="small"
                      :min="0"
                      :max="1"
                      controls-position="right"
                      :precision="2"
                      class="input-number-box"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 透明度</span>
                  </div>
                </el-col>
              </el-row>
            </el-form>
          </div>
          <lineSeriesConfig
            v-show="branchType==='lineChart'||series.basic.type==='line'"
            :ref="index+'seriesRef'"
            :series="series.lineSeries"
            @line-series-change="lineSeriesChange($event,index)"
          />
        </el-form>
      </template>
    </seriesplane>
  </div>
</template>

<script>
import lineSeriesConfig from './line-series-config.vue'
import SeriesPlane from '../../../chart-common-config/components/series-plane.vue'
import { utils } from 'efficient-suite'
export default {
  components: {
    lineSeriesConfig,
    SeriesPlane
  },
  inject: ['getSaveAble'],
  props: {
    branchType: {
      type: String,
      default: ''
    },
    colorList: {
      type: Array,
      default: () => []
    },
    seriesLength: {
      type: Number,
      default: 0
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      show: [],
      seriesList: [],
      autoSeries: false,
      smoothAll: false,
      filterDataEmpty: false,
      symbolOptions: [{ label: '默认', value: '' }, { label: '空', value: 'none' }, { label: '空心圆环', value: 'emptyCircle' }, { label: '圆点', value: 'circle' }, { label: '方形', value: 'rect' }, { label: '圆角方形', value: 'roundRect' }, { label: '三角形', value: 'triangle' }, { label: '菱形', value: 'diamond' }, { label: '箭头', value: 'arrow' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    },
    // 是否折线类图表(用于显示「平滑曲线」总开关):已知折线 branchType 或存在 line 系列
    isLineChart () {
      const LINE = ['lineChart', 'smoothLineChart', 'markLineChart', 'areaChart', 'stackAreaChart', 'stepLineChart', 'rainfallEvap', 'mixChart']
      if (LINE.includes(this.branchType)) return true
      return this.seriesList.some(s => s?.basic?.type === 'line')
    }
  },
  watch: {
    seriesList: {
      handler (nv, ov) {
        this.$emit('chartChange')
      },
      deep: false

    }
  },
  methods: {
    setSeries (data) {
      const formData = utils.deepClone(data)
      this.seriesList = formData.map(ele => {
        const form = { basic: { name: ele.name, type: ele.type, yAxisIndex: ele.yAxisIndex || 0, dataId: ele.dataId, legendConfig: ele?.legendConfig || { show: false, icon: '' } }, lineSeries: { }, barSeries: {} }
        form.barSeries = { ...ele }
        form.lineSeries = { ...ele }
        return form
      })
      // 「平滑曲线」总开关初值:任一 line 系列已开平滑则勾选
      this.smoothAll = this.seriesList.some(s => s.basic.type === 'line' && s.lineSeries && s.lineSeries.smooth)
    },
    // 「平滑曲线」总开关:对所有系列的折线配置统一设置 smooth(自生成系列取 series[0] 作模板,同样生效)
    toggleSmoothAll (val) {
      this.seriesList = this.seriesList.map(s => ({ ...s, lineSeries: { ...s.lineSeries, smooth: val } }))
      this.$emit('chartChange')
    },
    setFilterDataEmpty (e) {
      this.filterDataEmpty = e
    },
    setAutoSeries (e) {
      this.autoSeries = e
    },
    mixChange (e, index) {
      const arr = utils.deepClone(this.seriesList)
      arr[index].basic.type = e
      if (e === 'line') {
        arr[index].lineSeries = { smooth: false, lineStyle: { width: 2, color: this.seriesList[index].barSeries.itemStyle?.color || null }, itemStyle: { color: this.seriesList[index].barSeries.itemStyle?.color || null } }
      } else {
        if (!arr[index].barSeries || JSON.stringify(arr[index].barSeries) === '{}') {
          arr[index].barSeries = { itemStyle: { color: this.seriesList[index].lineSeries?.itemStyle?.color || null }, backgroundStyle: {}, stack: this.branchType === 'stackedChart' ? '1' : null }
        } else {
          if (this.seriesList[index].lineSeries?.itemStyle?.color) {
            arr[index].barSeries.itemStyle.color = this.seriesList[index].lineSeries.itemStyle.color
          }
        }
      }
      this.seriesList = arr
      this.$emit('chartChange')
    },
    saveFormData () {
      const data = this.seriesList.map(ele => {
        return { ...ele[ele.basic.type + 'Series'], ...ele.basic }
      })
      return { series: data, filterDataEmpty: this.filterDataEmpty, autoSeries: this.autoSeries }
    },
    lineSeriesChange (form, index) {
      this.seriesList[index].lineSeries = { ...form }
      this.$emit('chartChange')
    },
    addSeries () {
      let index = this.seriesList.length
      if (index >= this.colorList.length) {
        index = index % this.colorList.length
        if (index < 0) {
          index = 0
        }
      }
      if (this.branchType === 'lineChart') {
        const form = {
          basic: { type: 'line', name: '', dataId: this.seriesList.length + 1, yAxisIndex: 0, legendConfig: { show: false, icon: '' } },
          lineSeries: {

            lineStyle: {
              width: 2

            },
            showSymbol: true,
            smooth: false,
            itemStyle: { },
            backgroundStyle: {}
          }
        }
        this.seriesList.push(form)
      } else {
        this.seriesList.push({ basic: { type: 'bar', name: '', yAxisIndex: 0, dataId: this.seriesList.length + 1, legendConfig: { show: false, icon: '' } }, barSeries: { itemStyle: { }, stack: this.branchType === 'stackedChart' ? '1' : null, backgroundStyle: {} } })
      }
      this.$emit('chartChange')
    },
    copySeries (series) {
      const defaultSeries = utils.deepClone(series)
      this.seriesList.push(defaultSeries)
      this.$emit('chartChange')
    },
    delSeries (index) {
      this.seriesList.splice(index, 1)
      this.$emit('chartChange')
    },
    formChange () {
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
/* stylelint-disable-next-line scss/at-import-partial-extension */
@import "../../../styles/index.scss";

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
/* 系列开关行:标签左对齐(与数据系列/轴配置一致), 开关+「启用」文字靠右, 与上方轴配置视觉统一 */
.ser-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  white-space: nowrap;
  margin-bottom: 10px;
  padding-right: 18px;
  border-bottom: 1px #f2f3f5 solid;
}
.ser-switch-op {
  display: flex;
  align-items: center;
}
.ser-enable {
  font-size: 12px;
  line-height: 1;
  color: #c0c4cc;
  cursor: pointer;
  user-select: none;
  transition: color .15s;
}
.ser-enable.is-on {
  color: #4e5969;
}
</style>
