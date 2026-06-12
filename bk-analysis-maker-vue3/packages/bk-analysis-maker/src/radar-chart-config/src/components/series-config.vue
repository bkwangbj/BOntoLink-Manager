<template>
  <div class="full-box">
    <SeriesPlane
      name="系列配置"
      :series-list="seriesList"
      @del-series="delSeries"
      @copy-series="copySeries"
      @add-series="addSeries"
      @series-change="$emit('chartChange')"
    >
      <template #title="{index}">
        <span>系列{{ index + 1 }}</span>
      </template>
      <template #content="{series,index}">
        <el-form
          :ref="'form'+index"
          :model="series"
          label-width="80px"
          size="small"
          :disabled="!saveAble"
        >
          <el-form-item label="显示系列">
            <el-input
              v-model="series.name"
              size="small"
              @change="formChange"
            />
          </el-form-item>

          <el-form-item label="数据系列">
            <el-input
              v-model="series.dataId"
              size="small"
              @change="formChange"
            />
          </el-form-item>
          <el-form-item label="线条样式">
            <LineStyle
              :option="series.lineStyle||{}"
              @line-style-change="lineStyleChange($event,index)"
            />
          </el-form-item>
          <el-form-item label="圆点样式">
            <el-row
              :gutter="10"
              type="flex"
            >
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <el-input-number
                    v-model="series.symbolSize"
                    size="small"
                    controls-position="right"
                    class="input-number-box-px"
                    @change="formChange"
                  />
                  <span class="extra-bottom-text"> 大小</span>
                </div>
              </el-col>
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <CommonColorPicker
                    v-model="series.itemStyle.color"
                    @change="formChange"
                  />
                  <span class="extra-bottom-text"> 颜色</span>
                </div>
              </el-col>
            </el-row>
            <el-row
              :gutter="10"
              type="flex"
              style="width: 100%;"
            >
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <el-select
                    v-model="series.symbol"
                    placeholder="请选择"
                    size="small"
                    @change="formChange"
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
          </el-form-item>

          <div class="d-flex">
            <div style="width: 80px;">
              <el-switch
                v-model="series.areaStyle.show"
                class="am-switch active-switch"
                size="small"
                @click.stop.prevent
                @change="$emit('chartChange')"
              />
              <span class="chart-config-title">
                区域样式</span>
            </div>
            <el-form
              size="small"
              label-width="80px"
              :disabled="!series.areaStyle.show"
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
                      v-model="series.areaStyle.color"
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
                      v-model="series.areaStyle.opacity"
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
        </el-form>
      </template>
    </seriesplane>
  </div>
</template>

<script>
import SeriesPlane from '../../../chart-common-config/components/series-plane.vue'
import { utils } from 'efficient-suite'
export default {
  components: {
    SeriesPlane
  },
  inject: ['getSaveAble'],
  props: {

  },
  emits: ['chartChange'],
  data () {
    return {
      show: [],
      seriesList: [],
      filterDataEmpty: false,
      symbolOptions: Object.freeze([{ label: '默认', value: '' }, { label: '空', value: 'none' }, { label: '空心圆环', value: 'emptyCircle' }, { label: '圆点', value: 'circle' }, { label: '方形', value: 'rect' }, { label: '圆角方形', value: 'roundRect' }, { label: '三角形', value: 'triangle' }, { label: '菱形', value: 'diamond' }, { label: '箭头', value: 'arrow' }])
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
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
      this.seriesList = formData
    },
    formChange () {
      this.$emit('chartChange')
    },
    saveFormData () {
      return this.seriesList
    },
    addSeries () {
      this.seriesList.push({
        type: 'radar',
        symbol: 'circle',
        dataId: '',
        name: '',

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
      })
      this.$emit('chartChange')
    },
    lineStyleChange (e, index) {
      const series = utils.deepClone(utils.deepClone(this.seriesList))
      series[index].lineStyle = e
      this.seriesList = series
      this.$emit('chartChange')
    },
    copySeries (series) {
      this.seriesList.push({ ...utils.deepClone(series) })
      this.$emit('chartChange')
    },
    delSeries (index) {
      this.seriesList.splice(index, 1)
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
</style>
