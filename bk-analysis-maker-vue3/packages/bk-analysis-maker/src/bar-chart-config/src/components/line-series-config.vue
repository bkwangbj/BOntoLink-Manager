<template>
  <el-form
    :model="form"
    label-width="80px"
    size="small"
    :disabled="!saveAble"
  >
    <el-form-item label="折线">
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.lineStyle.width"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 粗细</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-radio-group
              v-model="form.smooth"
              size="small"
              class="radio-group-box"
              @change="smoothChange"
            >
              <el-radio-button :value="true">
                平滑
              </el-radio-button>
              <el-radio-button :value="false">
                折线
              </el-radio-button>
            </el-radio-group>
            <span class="extra-bottom-text"> 曲线类型</span>
          </div>
        </el-col>
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
        style="width: 193px;"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-select
              v-model="form.lineStyle.type"
              placeholder="请选择"
              size="small"
              @change="formChange"
            >
              <el-option
                v-for="item in lineTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text"> 类型</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.lineStyle.color"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 颜色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>

    <div class="d-flex el-form-item">
      <div style="width: 80px;">
        <el-switch
          v-model="form.showSymbol"
          class="am-switch active-switch"
          size="small"
          @click.stop.prevent
          @change="formChange"
        />
        <span class="chart-config-title">
          圆点  </span>
      </div>
      <div style="flex: 1;">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.symbolSize"
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
                v-model="form.itemStyle.color"
                @change="formChange"
              />
              <span class="extra-bottom-text"> 颜色</span>
            </div>
          </el-col>
        </el-row>
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-select
                v-model="form.symbol"
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
      </div>
    </div>
    <div class="d-flex">
      <div style="width: 80px;">
        <el-switch
          v-model="isArea"
          class="am-switch active-switch"
          size="small"
          @click.stop.prevent
          @change="isAreaChange"
        />
        <span class="chart-config-title">
          区域  </span>
      </div>
      <el-form
        size="small"
        label-width="80px"
        :disabled="!isArea"
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
                v-model="form.areaColor"
                @change="areaColorChange"
              />
              <span class="extra-bottom-text"> 颜色</span>
            </div>
          </el-col>
        </el-row>
      </el-form>
    </div>
    <CollapseItem
      v-model="form.label.show"
      name="值标签"
      @change="formChange"
    >
      <el-form-item label="文本">
        <text-style
          ref="labelTextRef"
          @text-style-change="textStyleChange"
        />
      </el-form-item>
    </CollapseItem>
  </el-form>
</template>

<script>
import TextStyle from '../../../chart-common-config/components/style-config/text-style.vue'
import { utils } from 'efficient-suite'
export default {
  components: {
    TextStyle
  },
  inject: ['getSaveAble'],
  props: {
    series: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['lineSeriesChange'],
  data () {
    return {
      type: 'text',
      form: {
        symbolSize: 4,
        symbol: 'emptyCircle',
        showSymbol: false,
        smooth: false,
        areaColor: '#000',
        itemStyle: {
          color: '#000'
        },
        label: { show: false },
        lineStyle: {
          color: '#000',
          width: 1,
          type: 'solid'
        }
      },
      isArea: false,
      areaColor: '#000',
      symbolOptions: Object.freeze([{ label: '默认', value: '' }, { label: '空', value: 'none' }, { label: '空心圆环', value: 'emptyCircle' }, { label: '圆点', value: 'circle' }, { label: '方形', value: 'rect' }, { label: '圆角方形', value: 'roundRect' }, { label: '三角形', value: 'triangle' }, { label: '菱形', value: 'diamond' }, { label: '箭头', value: 'arrow' }]),
      lineTypeOptions: Object.freeze([{ label: '实线', value: 'solid' }, { label: '虚线', value: 'dashed' }, { label: '点线', value: 'dotted' }])
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    series: {
      handler (v) {
        if (JSON.stringify(this.series) !== '{}') {
          this.$nextTick(() => {
            this.setFormData(this.series)
          })
        }
      },
      immediate: true
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())

      this.form = utils.deepClone(this.$options.data().form)
      this.form = Object.assign(this.form, form)
      this.isArea = !!this.form?.areaStyle

      if (form?.label) {
        this.$refs.labelTextRef.setFormData(form?.label || {})
      }
    },
    formChange () {
      this.$emit('lineSeriesChange', this.form)
    },
    textStyleChange (textStyle) {
      this.form.label = { ...this.form.label, ...textStyle }
      this.$emit('lineSeriesChange', this.form)
    },
    areaColorChange (e) {
      this.form.areaStyle = this.isArea
        ? {
            color: this.getGradientColor(e, '#ffffff00')
          }
        : null
      this.$emit('lineSeriesChange', this.form)
    },
    smoothChange (e) {
      this.$emit('lineSeriesChange', this.form)
    },
    isAreaChange (e) {
      this.form.areaStyle = this.isArea
        ? {
            color: this.getGradientColor(this.form.areaColor, '#ffffff00')
          }
        : null
      this.$emit('lineSeriesChange', this.form)
    },
    getGradientColor (startColor, endColor) {
      return {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: startColor },
          { offset: 1, color: endColor }
        ]
      }
    }
  }
}
</script>
<style  lang="scss">
.text-style-form > .el-form-item__content {
  margin-left: 0 !important;
}

.d-flex {
  display: flex;
}

.d-flex-c {
  display: flex;
  flex-direction: column;
}

.ai-c {
  align-items: center;
}

.radio-group-box {
  flex-wrap: nowrap;

  .el-radio-button__inner {
    padding: 4px 6px !important;
  }

}
</style>
