<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      label-width="80px"
      size="small"
      :disabled="!saveAble"
    >
      <el-form-item label="圆环样式">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <CommonColorPicker
                v-model="series.itemStyle.color"
                @change="$emit('pieSeriesChange')"
              />
              <span class="extra-bottom-text"> 圆环颜色</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <CommonColorPicker
                v-model="series.backgroundStyle.color"
                @change="$emit('pieSeriesChange')"
              />
              <span class="extra-bottom-text"> 背景颜色</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-collapse>
        <el-collapse-item>
          <template #title>
            <div class="collapse-title-common">
              极坐标轴
            </div>
          </template>
          <div class="plane-inner-bg">
            <el-form-item label="半径">
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.polar.radius[0]"
                      size="small"
                      controls-position="right"
                      class="input-number-box-perc"
                      :min="0"
                      :max="100"
                      @change="$emit('pieSeriesChange')"
                    />
                    <span class="extra-bottom-text"> 内径</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.polar.radius[1]"
                      size="small"
                      controls-position="right"
                      class="input-number-box-perc"
                      :min="0"
                      :max="100"
                      @change="$emit('pieSeriesChange')"
                    />
                    <span class="extra-bottom-text"> 外径</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
            <el-form-item label="中心位置">
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.polar.center[0]"
                      size="small"
                      class="input-number-box-perc"
                      controls-position="right"
                      :min="0"
                      :max="100"
                      @change="$emit('pieSeriesChange')"
                    />
                    <span class="extra-bottom-text"> 水平</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.polar.center[1]"
                      size="small"
                      class="input-number-box-perc"
                      controls-position="right"
                      :min="0"
                      :max="100"
                      @change="$emit('pieSeriesChange')"
                    />
                    <span class="extra-bottom-text"> 垂直</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
          </div>
        </el-collapse-item>
      </el-collapse>
      <CollapseItem
        v-model="form.title.show"
        name="标题"
        @change="$emit('pieSeriesChange')"
      >
        <DataFormatSelect
          v-model="form.title.dataType"
          type="value"
          @data-type-change="$emit('pieSeriesChange')"
        />
        <el-form-item label="标题样式">
          <text-style
            ref="textRef"
            @text-style-change="textStyleChange"
          />
        </el-form-item>
      </CollapseItem>
    </el-form>
  </div>
</template>

<script>
import TextStyle from '../../../chart-common-config/components/style-config/text-style.vue'
import { utils } from 'efficient-suite'
export default {
  components: {
    TextStyle
  },
  inject: ['getSaveAble'],
  emits: ['pieSeriesChange'],
  data () {
    return {
      form: {
        polar: {
          radius: [0, 80],
          center: [50, 50]

        },
        title: { show: true, textStyle: {} }
      },
      series: {
        backgroundStyle: { color: '' },
        itemStyle: { color: '' }
      },
      alignOption: [{ label: '外侧', value: 'outside', icon: 'icon-biaoqian-waibu' }, { label: '内部', value: 'inner', icon: 'icon-biaoqian-neibu' }],
      formatterOptions: [{ label: '数据值', value: '{c}' }, { label: '百分比', value: '{d}%' }],
      lineTypeOptions: [{ label: '实线', value: 'solid' }, { label: '虚线', value: 'dashed' }, { label: '点线', value: 'dotted' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    setFormData ({ option, series }) {
      Object.assign(this.$data, this.$options.data())
      this.form = Object.assign(this.form, option)
      this.series = Object.assign(this.series, series)
      this.$refs.textRef.setFormData(option.title?.textStyle || {})
    },
    textStyleChange (textStyle) {
      this.form.title.textStyle = { ...this.form.title?.textStyle || {}, ...textStyle }
      this.$emit('pieSeriesChange')
    },
    saveFormData () {
      const form = utils.deepClone(this.form)
      const series = utils.deepClone(this.series)
      return { option: form, series }
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
<style  lang="scss" scoped>
.text-style-form > .el-form-item__content {
  margin-left: 0 !important;
}

.d-flex-c {
  display: flex;
  flex-direction: column;
}

.ai-c {
  align-items: center;
}

.d-flex {
  display: flex;
}

</style>
