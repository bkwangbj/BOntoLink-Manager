<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      :disabled="!saveAble"
      size="small"
      label-width="80px"
    >
      <el-form-item label="半径">
        <div class="d-flex-c">
          <el-input-number
            v-model="form.radius"
            controls-position="right"
            size="small"
            class="input-number-box-perc"
            @change="$emit('chartChange')"
          />
        </div>
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
                @change="$emit('chartChange')"
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
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text"> 垂直</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="起止角度">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.startAngle"
                size="small"
                class="input-number-box-angle"
                controls-position="right"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text"> 起始角度</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.endAngle"
                size="small"
                class="input-number-box-angle"
                controls-position="right"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text"> 结束角度</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="刻度配置">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.splitNumber"
                controls-position="right"
                size="small"
                class="input-number-box"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text">分割段数</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-radio-group
                v-model="form.clockwise"
                size="small"
                class="nowrap-radio"
                @change="$emit('chartChange')"
              >
                <el-radio-button :value="1">
                  顺时针
                </el-radio-button>
                <el-radio-button :value="0">
                  逆时针
                </el-radio-button>
              </el-radio-group>
              <span class="extra-bottom-text">刻度方向</span>
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
                v-model="form.min"
                size="small"
                class="input-number-box"
                controls-position="right"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text"> 最小值</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.max"
                size="small"
                class="input-number-box"
                controls-position="right"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text">最大值</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <CollapseItem
        v-model="form.title.show"
        name=" 标题"
        @change="$emit('chartChange')"
      >
        <!-- <el-form-item label="内容">
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="24"
            >
              <div class="d-flex-c">
                <el-input
                  size="small"
                  @change="$emit('chartChange')"
                  v-model="form.name"
                />
              </div>
            </el-col>
          </el-row>
        </el-form-item>-->
        <el-form-item label="中心偏移">
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form.title.offsetCenter[0]"
                  size="small"
                  class="input-number-box-px"
                  controls-position="right"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">x</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form.title.offsetCenter[1]"
                  size="small"
                  class="input-number-box-px"
                  controls-position="right"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">y</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="文本样式">
          <TextStyle
            ref="titleRef"
            :disabled="!form.title.show"
            @text-style-change="textStyleChange($event,'title')"
          />
        </el-form-item>
      </CollapseItem>
      <CollapseItem
        v-model="form.detail.show"
        name=" 显示数据"
        @change="$emit('chartChange')"
      >
        <DataFormatSelect
          v-model="form.detail.dataType"
          type="value"
          @data-type-change="$emit('chartChange')"
        />
        <el-form-item label="内容">
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input
                  v-model="form.detail.prefix"
                  size="small"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">前缀</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input
                  v-model="form.detail.suffix"
                  size="small"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">后缀</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="中心偏移">
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form.detail.offsetCenter[0]"
                  size="small"
                  class="input-number-box-px"
                  controls-position="right"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">x</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form.detail.offsetCenter[1]"
                  size="small"
                  class="input-number-box-px"
                  controls-position="right"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">-y</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="文本样式">
          <TextStyle
            ref="detailRef"
            :disabled="!form.detail.show"
            @text-style-change="textStyleChange($event,'detail')"
          />
        </el-form-item>
      </CollapseItem>
      <el-collapse>
        <el-collapse-item>
          <template #title>
            <div class="collapse-title-common ">
              刻度线配置
            </div>
          </template>
          <div class="plane-inner-bg">
            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.axisLabel.show"
                  class="am-switch active-switch"
                  size="small"
                  @click.stop.prevent
                  @change="$emit('chartChange')"
                />
                <span class="chart-config-title">
                  标签</span>
              </div>
              <el-form
                :disabled="!form.axisLabel.show"
                label-width="0px"

                size="small"
              >
                <el-form-item label="">
                  <el-row
                    :gutter="10"
                    type="flex"
                  >
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.axisLabel.distance"
                          size="small"
                          class="input-number-box-px"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">偏移量</span>
                      </div>
                    </el-col>
                  </el-row>
                </el-form-item>
                <el-form-item label="">
                  <TextStyle
                    ref="axisLabelRef"
                    :disabled="!form.axisLabel.show"
                    @text-style-change="textStyleChange($event,'axisLabel')"
                  />
                </el-form-item>
              </el-form>
            </div>

            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.splitLine.show"
                  class="am-switch active-switch"
                  size="small"
                  @click.stop.prevent
                  @change="$emit('chartChange')"
                />
                <span class="chart-config-title">
                  分隔线</span>
              </div>
              <el-form
                size="small"
                :disabled="!form.splitLine.show"
                label-width="0px"
              >
                <el-form-item label="">
                  <el-row
                    :gutter="10"
                    type="flex"
                  >
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.splitLine.length"
                          size="small"
                          class="input-number-box-px"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">长度</span>
                      </div>
                    </el-col>
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.splitLine.distance"
                          size="small"
                          class="input-number-box-px"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">偏移量</span>
                      </div>
                    </el-col>
                  </el-row>
                </el-form-item>
                <el-form-item label="">
                  <LineStyle
                    ref="splitLineRef"
                    :disabled="!form.splitLine.show"
                    @line-style-change="lineStyleChange($event,'splitLine')"
                  />
                </el-form-item>
              </el-form>
            </div>
            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.axisTick.show"
                  class="am-switch active-switch"
                  size="small"
                  @click.stop.prevent
                  @change="$emit('chartChange')"
                />
                <span class="chart-config-title">
                  刻度</span>
              </div>
              <el-form
                :disabled="!form.axisTick.show"
                label-width="0px"
              >
                <el-form-item label="">
                  <el-row
                    :gutter="10"
                    type="flex"
                  >
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.axisTick.length"
                          size="small"
                          class="input-number-box-px"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">长度</span>
                      </div>
                    </el-col>
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.axisTick.distance"
                          size="small"
                          class="input-number-box-px"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">偏移量</span>
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
                          v-model="form.axisTick.splitNumber"
                          size="small"
                          class="input-number-box"
                          controls-position="right"
                          :min="0"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">分隔线之间的刻度数</span>
                      </div>
                    </el-col>
                  </el-row>
                </el-form-item>
                <el-form-item label="">
                  <LineStyle
                    ref="axisTickRef"
                    :disabled="!form.axisTick.show"
                    @line-style-change="lineStyleChange($event,'axisTick')"
                  />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-collapse-item>
        <el-collapse-item>
          <template #title>
            <div class="collapse-title-common ">
              进度条配置
            </div>
          </template>
          <div class="plane-inner-bg">
            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.axisLine.show"
                  class="am-switch active-switch"
                  size="small"
                  @click.stop.prevent
                  @change="$emit('chartChange')"
                />
                <span class="chart-config-title">
                  轴线</span>
              </div>
              <el-form
                :disabled="!form.axisLine.show||!saveAble"
                label-width="0px"
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
                        v-model="form.axisLine.lineStyle.width"
                        size="small"
                        class="input-number-box-px"
                        controls-position="right"
                        @change="$emit('chartChange')"
                      />
                      <span class="extra-bottom-text">宽度</span>
                    </div>
                  </el-col>
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <CommonColorPicker
                        v-model="form.axisLine.lineStyle.color"
                        @change="$emit('chartChange')"
                      />
                      <span class="extra-bottom-text"> 颜色</span>
                    </div>
                  </el-col>
                </el-row>
              </el-form>
            </div>

            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.progress.show"
                  class="am-switch active-switch"
                  size="small"
                  @click.stop.prevent
                  @change="$emit('chartChange')"
                />
                <span class="chart-config-title">
                  进度条</span>
              </div>
              <el-form
                :disabled="!form.progress.show||!saveAble"
                label-width="0px"
              >
                <el-form-item label="">
                  <el-row
                    :gutter="10"
                    type="flex"
                  >
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.progress.width"
                          size="small"
                          class="input-number-box-px"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">宽度</span>
                      </div>
                    </el-col>
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <CommonColorPicker
                          v-model="form.progress.itemStyle.color"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text"> 颜色</span>
                      </div>
                    </el-col>
                  </el-row>
                </el-form-item>
              </el-form>
            </div>
            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.pointer.show"
                  class="am-switch active-switch"
                  size="small"
                  @click.stop.prevent
                  @change="$emit('chartChange')"
                />
                <span class="chart-config-title">
                  指针</span>
              </div>
              <el-form
                :disabled="!form.pointer.show||!saveAble"
                label-width="0px"
              >
                <el-form-item label="">
                  <el-row
                    :gutter="10"
                    type="flex"
                  >
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.pointer.width"
                          size="small"
                          class="input-number-box-px"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">宽度</span>
                      </div>
                    </el-col>
                    <el-col
                      :span="12"
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          v-model="form.pointer.length"
                          size="small"
                          class="input-number-box-perc"
                          controls-position="right"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text">长度</span>
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
                        <CommonColorPicker
                          v-model="form.pointer.itemStyle.color"
                          @change="$emit('chartChange')"
                        />
                        <span class="extra-bottom-text"> 颜色</span>
                      </div>
                    </el-col>
                  </el-row>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
      <CollapseItem
        v-model="form.customConfig.show"
        name=" 个性化配置"
        @change="$emit('chartChange')"
      >
        <InterfaceFilterConfig
          :interface-filter="form.customConfig.config"
          :set-mode="saveAble"
          method-name=" setCustomConfig(option, config, data, params) "
          return-text="return option;"
          @save="saveCustomConfig"
          @blur="configBlur"
        />
      </CollapseItem>
    </el-form>
  </div>
</template>

<script>
import TextStyle from '../../../chart-common-config/components/style-config/text-style.vue'
import LineStyle from '../../../chart-common-config/components/style-config/line-style.vue'
import InterfaceFilterConfig from '../../../data-source-config/src/components/interface-filter-config.vue'
import XEUtils from 'xe-utils'
import { utils } from 'efficient-suite'
export default {
  components: {
    TextStyle,
    LineStyle,
    InterfaceFilterConfig
  },
  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      form: {
        customConfig: {
          show: false,
          config: 'return option'
        },
        center: [50, 50],
        min: 0,
        max: 100,
        startAngle: 225,
        endAngle: -45,
        clockwise: 1,
        splitNumber: 10,
        radius: 50,
        axisLabel: {
          show: true,
          distance: 25
        },
        detail: {
          show: true,
          suffix: '%',
          prefix: '',
          offsetCenter: [0, 82]
        },
        title: { // 标题
          show: true,
          offsetCenter: [0, 46] // x, y，单位px

        },
        splitLine: {
          show: true,
          distance: -70,
          length: 20,
          lineStyle: {
            color: '#468EFD',
            width: 1
          }
        },
        axisTick: {
          show: true,
          splitNumber: 5,
          distance: -60,
          lineStyle: {
            color: '#468EFD',
            width: 1
          },
          length: 8
        },
        progress: {
          show: true,
          width: 8,
          itemStyle: {
            color: '#468EFD'
          }
        },
        axisLine: {
          lineStyle: {
            color: '#111F42',
            width: 8
          }

        },
        pointer: {
          show: true,
          length: 75,
          width: 10,
          itemStyle: {
            color: '#468EFD'
          }
        }
      }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())

      this.form = XEUtils.merge(this.form, form)
      this.$refs.axisLabelRef.setFormData(form.axisLabel)
      this.$refs.titleRef.setFormData(form.title)
      this.$refs.detailRef.setFormData(form.detail)
      this.$refs.splitLineRef.setFormData(form.splitLine.lineStyle || {})
      this.$refs.axisTickRef.setFormData(form.axisTick.lineStyle || {})
    },
    textStyleChange (textStyle, type) {
      this.form[type] = { ...this.form[type], ...textStyle }

      this.$emit('chartChange')
    },
    lineStyleChange (lineStyle, type) {
      this.form[type].lineStyle = lineStyle

      this.$emit('chartChange')
    },
    saveFormData () {
      const form = utils.deepClone(this.form)
      return form
    },
    alignChange (ele) {
      this.form.label.position = ele.value
      this.$emit('chartChange')
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
    },
    saveCustomConfig (script) {
      this.form.customConfig.config = script
    },
    configBlur () {
      this.$emit('chartChange')
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

:deep(.nowrap-radio.el-radio-group) {
  flex-wrap: nowrap;
}

</style>
