<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      label-width="80px"
      size="small"
      :disabled="!saveAble"
    >
      <el-form-item label="饼图半径">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.radius[0]"
                size="small"
                controls-position="right"
                class="input-number-box-perc"
                :min="0"
                :max="100"
                @change="formChange"
              />
              <span class="extra-bottom-text"> 内径</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.radius[1]"
                size="small"
                controls-position="right"
                class="input-number-box-perc"
                :min="0"
                :max="100"
                @change="formChange"
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
                v-model="form.center[0]"
                size="small"
                class="input-number-box-perc"
                controls-position="right"
                :min="0"
                :max="100"
                @change="formChange"
              />
              <span class="extra-bottom-text"> 水平</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.center[1]"
                size="small"
                class="input-number-box-perc"
                controls-position="right"
                :min="0"
                :max="100"
                @change="formChange"
              />
              <span class="extra-bottom-text"> 垂直</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="风玫瑰图">
        <div class="d-flex-c">
          <el-radio-group
            v-model="form.roseType"
            size="small"
            @change="formChange"
          >
            <el-radio-button :value="false">
              关
            </el-radio-button>
            <el-radio-button :value="'area'">
              半径
            </el-radio-button>
            <el-radio-button :value="'radius'">
              角度
            </el-radio-button>
          </el-radio-group>
          <span class="extra-bottom-text" />
        </div>
      </el-form-item>
      <CollapseItem
        v-model="form.label.show"
        name="饼图标签"
        @change="formChange"
      >
        <el-form
          ref="axisLabel"
          label-width="80px"
          size="small"
          :disabled="!saveAble"
          class="inline-col-form"
        >
          <el-form-item label="标签格式">
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
                    v-model="form.label.formatter"
                    size="small"
                    placeholder="请选择"
                    @change="formChange"
                  >
                    <el-option
                      v-for="item in formatterOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                  <span class="extra-bottom-text"> 格式</span>
                </div>
              </el-col>
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <div class="d-flex">
                    <div
                      v-for="ele in alignOption"
                      :key="ele.value"
                      class="chart-align-item"
                      :class="{'active':form.label.position===ele.value,'is-disabled':!saveAble}"
                      @click="alignChange(ele)"
                    >
                      <i :class="['icon am-iconfont align-icon', ele.icon]" />
                    </div>
                  </div>
                  <span class="extra-bottom-text"> 位置</span>
                </div>
              </el-col>
            </el-row>
            <el-row
              v-show="form.label.formatter==='{c}'"
              :gutter="10"
              type="flex"
            >
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <el-input
                    v-model="form.label.prefix"
                    size="small"
                    @change="formChange"
                  />
                  <span class="extra-bottom-text">前缀</span>
                </div>
              </el-col>
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <el-input
                    v-model="form.label.suffix"
                    size="small"
                    @change="formChange"
                  />
                  <span class="extra-bottom-text">后缀</span>
                </div>
              </el-col>
            </el-row>
          </el-form-item>
          <DataFormatSelect
            v-show="form.label.formatter==='{c}'"
            v-model="form.label.dataType"
            type="value"
            @data-type-change="formChange"
          />
          <div class="d-flex el-form-item">
            <div style="width: 80px;">
              <el-switch
                v-model="form.labelLine.show"
                class="am-switch active-switch"
                size="small"
                @click.stop.prevent
                @change="formChange"
              />
              <span class="chart-config-title">
                标签引线</span>
            </div>
            <el-form
              label-width="80px"
              :disabled="!saveAble"
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
                    <el-input-number
                      v-model="form.labelLine.length"
                      size="small"
                      controls-position="right"
                      class="input-number-box-px"
                      @change="formChange"
                    />
                    <span class="extra-bottom-text"> 第一段长度</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="form.labelLine.length2"
                      size="small"
                      controls-position="right"
                      class="input-number-box-px"
                      @change="formChange"
                    />
                    <span class="extra-bottom-text"> 第二段长度</span>
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
                    <el-input-number
                      v-model="form.labelLine.lineStyle.width"
                      size="small"
                      controls-position="right"
                      class="input-number-box-px"
                      @change="formChange"
                    />
                    <span class="extra-bottom-text"> 粗细</span>
                  </div>
                </el-col>
              </el-row>
            </el-form>
          </div>
          <el-form-item label="文本样式">
            <text-style
              ref="labelTextRef"
              @text-style-change="textStyleChange"
            />
          </el-form-item>
        </el-form>
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
  props: {
    series: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['pieSeriesChange'],
  data () {
    return {

      form: {
        radius: [0, 80],
        center: [50, 50],
        roseType: false,
        label: { show: false, formatter: '{d}%', position: 'outside', prefix: '', suffix: '' },
        labelLine: { show: false, length: 5, length2: 10, lineStyle: { width: 1 } }
      },
      alignOption: Object.freeze([{ label: '外侧', value: 'outside', icon: 'icon-biaoqian-waibu' }, { label: '内部', value: 'inner', icon: 'icon-biaoqian-neibu' }]),
      formatterOptions: [{ label: '数据值', value: '{c}' }, { label: '百分比', value: '{d}%' }],
      lineTypeOptions: [{ label: '实线', value: 'solid' }, { label: '虚线', value: 'dashed' }, { label: '点线', value: 'dotted' }]
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
      if (!form) {
        return
      }
      Object.assign(this.$data, this.$options.data())
      this.form = Object.assign(this.form, form)

      this.$refs.labelTextRef.setFormData(form.label)
    },
    textStyleChange (textStyle) {
      this.form.label = { ...this.form.label, ...textStyle }
      this.$emit('pieSeriesChange', utils.deepClone(this.form))
    },
    saveFormData () {
      const form = utils.deepClone(this.form)
      return form
    },
    alignChange (ele) {
      this.form.label.position = ele.value
      this.$emit('pieSeriesChange', utils.deepClone(this.form))
    },
    formChange () {
      this.$nextTick(() => {
        this.$emit('pieSeriesChange', utils.deepClone(this.form))
      })
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
