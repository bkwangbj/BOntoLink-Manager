<template>
  <BKBasicChartConfig
    ref="basicChartConfig"
    :configs="configs"
    :page-config="pageConfig"
    :save-able="saveAble"
    :items="items"
    :rules="rules"
    :tjb-u-r-l="tjbURL"
    v-bind="$attrs"
    @set-expand="$emit('setExpand')"
    @config-option-init="configOptionInit"
    @save-chart-config="saveChartCfg"
  >
    <div style="display: flex;height: 100%;">
      <SidebarTabs
        v-model="activeConfig"
        :menu="chartMenu"
      />
      <div style=" flex: 1;height: 100%;">
        <el-form
          v-show="activeConfig==='basic'"
          ref="customForm"
          :model="form"
          label-width="80px"
          size="small"
          class="full-box"
          :disabled="!saveAble"
          style="padding: 10px; overflow: auto;"
        >
          <div
            class="copy-button-box"
          >
            <el-button
              type="primary"
              size="small"
              @click="copyConfig('gaugeBasic')"
            >
              复制基础配置
            </el-button>
            <el-button
              type="primary"
              size="small"
              :disabled="!saveAble"
              @click="pasteConfig('gaugeBasic')"
            >
              粘贴基础配置
            </el-button>
          </div>
          <el-form-item
            label="间隔"
          >
            <el-input-number
              v-model="form.styles.gap"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change=" getChartBasicConfigData"
            />
          </el-form-item>
          <el-form-item
            label="宽度"
          >
            <el-row
              :gutter="10"
              type="flex"
              style="width: 100%;margin-bottom: 10px;"
            >
              <el-col
                :span="24"
              >
                <el-radio-group
                  v-model="form.widthType"
                  size="small"
                  @change="getChartBasicConfigData"
                >
                  <el-radio-button value="adaptive">
                    自适应
                  </el-radio-button>
                  <el-radio-button value="custom">
                    自定义
                  </el-radio-button>
                </el-radio-group>
              </el-col>
            </el-row>
            <el-row
              :gutter="10"
              type="flex"
            >
              <el-col
                :span="24"
              >
                <div
                  v-show="form.widthType==='custom'"
                  class="d-flex-c"
                >
                  <PercPxInput

                    v-model="form.styles.width"
                    @change="getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">宽度</span>
                </div>
                <div
                  v-show="form.widthType==='adaptive'"
                  class="d-flex-c"
                >
                  <el-input-number
                    v-model="form.widthNum"
                    size="small"
                    class="input-number-box"
                    controls-position="right"
                    @change="getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">行个数</span>
                </div>
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item
            label="高度"
          >
            <el-row
              :gutter="10"
              type="flex"
              style="width: 100%;margin-bottom: 10px;"
            >
              <el-col
                :span="24"
              >
                <el-radio-group
                  v-model="form.heightType"
                  size="small"
                  @change="getChartBasicConfigData"
                >
                  <el-radio-button value="adaptive">
                    自适应
                  </el-radio-button>
                  <el-radio-button value="custom">
                    自定义
                  </el-radio-button>
                </el-radio-group>
              </el-col>
            </el-row>
            <el-row
              :gutter="10"
              type="flex"
              style="width: 100%;"
            >
              <el-col
                :span="24"
              >
                <div
                  v-show="form.heightType==='custom'"
                  class="d-flex-c"
                >
                  <PercPxInput

                    v-model="form.styles.height"
                    @change="getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">高度</span>
                </div>
                <div
                  v-show="form.heightType==='adaptive'"
                  class="d-flex-c"
                >
                  <el-input-number
                    v-model="form.heightNum"
                    size="small"
                    class="input-number-box"
                    controls-position="right"
                    @change="getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">列个数</span>
                </div>
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item
            label="基础样式"
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
                    v-model="form.styles.borderWidth"
                    size="small"
                    class="input-number-box-px"
                    controls-position="right"
                    @change="getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">边框粗细</span>
                </div>
              </el-col>
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <CommonColorPicker
                    v-model="form.styles.borderColor"
                    @change="getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">边框颜色</span>
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
                    v-model="form.styles.borderRadius"
                    size="small"
                    class="input-number-box-px"
                    controls-position="right"
                    @change="getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">圆角大小</span>
                </div>
              </el-col>
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <CommonColorPicker
                    v-model="form.styles.backgroundColor"
                    @change=" getChartBasicConfigData"
                  />
                  <span class="extra-bottom-text">背景色</span>
                </div>
              </el-col>
            </el-row>
          </el-form-item>
          <CollapseItem
            v-model="form.image.show"
            name=" 图片样式"
            @change="getChartBasicConfigData"
          >
            <div class="plane-inner-bg">
              <el-form-item label="尺寸">
                <el-row
                  :gutter="10"
                  type="flex"
                >
                  <el-col
                    :span="24"
                  >
                    <div class="d-flex-c">
                      <PercPxInput
                        v-model="form.image.width"
                        @change="getChartBasicConfigData"
                      />

                      <span class="extra-bottom-text">宽</span>
                    </div>
                  </el-col>
                </el-row>
                <el-row
                  :gutter="10"
                  type="flex"
                >
                  <el-col :span="24">
                    <div class="d-flex-c">
                      <PercPxInput
                        v-model="form.image.height"
                        @change="getChartBasicConfigData"
                      />

                      <span class="extra-bottom-text">高</span>
                    </div>
                  </el-col>
                </el-row>
              </el-form-item>
              <el-form-item label="位置">
                <PositionStyle
                  v-model="form.image.position"
                  @position-style-change="getChartBasicConfigData"
                />
              </el-form-item>
            </div>
          </CollapseItem>
          <el-collapse>
            <el-collapse-item>
              <template #title>
                <div class="collapse-title-common ">
                  数字样式
                </div>
              </template>
              <div class="plane-inner-bg">
                <el-form-item label="数字样式">
                  <TextStyle
                    v-model="form.number.textStyle"
                    @text-style-change="getChartBasicConfigData"
                  />
                </el-form-item>
                <el-form-item label="位置">
                  <PositionStyle
                    v-model="form.number.position"
                    @position-style-change="getChartBasicConfigData"
                  />
                </el-form-item>
              </div>
            </el-collapse-item>
            <el-collapse-item>
              <template #title>
                <div class="collapse-title-common ">
                  文本样式
                </div>
              </template>
              <div class="plane-inner-bg">
                <el-form-item label="文本样式">
                  <TextStyle
                    v-model="form.text.textStyle"
                    @text-style-change="getChartBasicConfigData"
                  />
                </el-form-item>
                <el-form-item label="位置">
                  <PositionStyle
                    v-model="form.text.position"
                    @position-style-change="getChartBasicConfigData"
                  />
                </el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
          <CollapseItem
            v-model="form.unit.show"
            name=" 单位样式"
            @change="getChartBasicConfigData"
          >
            <div class="plane-inner-bg">
              <el-form-item label="文本样式">
                <TextStyle
                  v-model="form.unit.textStyle"
                  @text-style-change="getChartBasicConfigData"
                />
              </el-form-item>
              <el-form-item label="位置">
                <el-radio-group
                  v-model="form.unit.posi"
                  size="small"
                  style="margin-bottom: 10px;"
                  @change="getChartBasicConfigData"
                >
                  <el-radio-button value="text">
                    文本
                  </el-radio-button>
                  <el-radio-button value="number">
                    数字
                  </el-radio-button>
                </el-radio-group>
                <PositionStyle
                  v-model="form.unit.position"
                  type="absolute"
                  @position-style-change="getChartBasicConfigData"
                />
              </el-form-item>
            </div>
          </CollapseItem>
        </el-form>
        <el-form
          v-show="activeConfig==='colorField'"
          :disabled="!saveAble"
          label-width="80px"
          class="full-box"
          style="padding: 10px;overflow: auto;"
        >
          <CollapseItem
            v-model="form.isPieces"
            class="pieces-collapse"
            name="区间"
            @change="getChartBasicConfigData"
          >
            <template #titleRight="{show}">
              <el-button
                v-show="show.length"
                size="small"
                text
                class="add-seris-btn"
                type="primary"
                @click.stop="addPieces"
              >
                <el-icon><CirclePlusFilled /></el-icon>
              </el-button>
            </template>
            <el-collapse>
              <el-collapse-item
                v-for="(ele,index) in pieces"
                :key="index"
              >
                <template #title>
                  <div
                    class="series-header flex-between"
                    style="width: 100%;"
                  >
                    <span style="display: inline-block;width: 90px;padding-left: 20px;text-align: left;">区间{{ index + 1 }}</span>
                    <div>
                      <el-button
                        size="small"
                        text
                        class="add-seris-btn"
                        type="primary"
                        @click.stop="copyPieces(ele)"
                      >
                        <i-ri-file-copy-2-fill />
                      </el-button>
                      <el-button
                        size="small"
                        text
                        class="del-seris-btn"
                        @click.stop="delPieces(index)"
                      >
                        <el-icon><DeleteFilled /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </template>
                <el-row
                  :gutter="10"
                  type="flex"
                >
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <el-input-number
                        v-model="ele.gt"
                        controls-position="right"
                        size="small"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text">大于</span>
                    </div>
                  </el-col>
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <el-input-number
                        v-model="ele.lte"
                        controls-position="right"
                        size="small"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text">小于</span>
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
                      <CommonColorPicker
                        v-model="ele.numberColor"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text">数字颜色</span>
                    </div>
                  </el-col>
                  <el-col
                    :span="12"
                  >
                    <div class="d-flex-c">
                      <CommonColorPicker
                        v-model="ele.textColor"
                        @change="getChartBasicConfigData"
                      />
                      <span class="extra-bottom-text">文字颜色</span>
                    </div>
                  </el-col>
                </el-row>
              </el-collapse-item>
            </el-collapse>
          </CollapseItem>
        </el-form>
      </div>
    </div>
  </BKBasicChartConfig>
</template>

<script>
import SidebarTabs from '../../chart-common-config/components/sidebar-tabs.vue'
import { commonItems, commonRules } from '../../configs/common-item'
import Copy from '../../configs/copy-chart-mixins'
import { utils } from 'efficient-suite'
const colorList = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']
export default {
  name: 'StatisticsChartConfig',
  components: {
    SidebarTabs
  },
  mixins: [Copy],
  inject: ['getSaveAble'],
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
    tjbURL: {
      type: String,
      default: ''
    }
  },
  emits: ['setExpand', 'saveChartCfg'],
  data () {
    return {
      items: commonItems.filter(c => c.isBasic),
      rules: commonRules,
      activeConfig: 'basic',
      pieces: [],
      chartMenu: [{ name: '基础样式', key: 'basic', icon: 'icon-tuxing' }, { name: '颜色映射', key: 'colorField', icon: 'icon-yangshi' }],
      form: {
        widthType: 'adaptive',
        widthNum: 4,
        isPieces: false,
        heightType: 'custom',
        heightNum: 1,
        styles: {
          gap: undefined,
          borderWidth: undefined,
          width: undefined,

          height: undefined,
          borderColor: '',
          borderRadius: undefined,
          backgroundColor: ''

        },
        unit: {
          show: false,
          posi: 'text',
          position: {
            top: '10px',
            left: '10px'
          },
          textStyle: {

          }
        },
        image: {
          show: true,
          width: undefined,
          height: undefined,
          position: {

          }
        },
        number: {
          position: {

          },
          textStyle: {

          }
        },
        text: {
          position: {

          },
          textStyle: {

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
  created () {
  },
  methods: {
    configOptionInit () {
      this.$nextTick(() => {
        this.form = Object.assign(this.form, this.configs.configOption)
        if (this.configs.configOption.pieces?.length) {
          this.pieces = utils.deepClone(this.configs.configOption.pieces)
        } else {
          this.pieces = []
        }
      })
    },
    getChartBasicConfigData () {
      this.$refs.basicChartConfig.saveConfig()
    },
    saveChartCfg (data) {
      data.configOption = this.form
      data.configOption.pieces = this.pieces
      this.$emit('saveChartCfg', data)
    },
    addPieces () {
      let index = this.pieces.length
      if (index >= colorList.length) {
        index = index % colorList.length
        if (index < 0) {
          index = 0
        }
      }
      this.pieces.push({ gt: null, lte: null, numberColor: colorList[index], textColor: colorList[index] })
      this.getChartBasicConfigData()
    },
    copyPieces (series) {
      this.pieces.push({ ...utils.deepClone(series) })
      this.getChartBasicConfigData()
    },
    delPieces (index) {
      this.pieces.splice(index, 1)
      this.getChartBasicConfigData()
    }
  }
}
</script>

<style lang="scss" scoped>
// @import "../../styles/index.css";
:deep(.pieces-collapse) {

  .el-collapse-item {

    .plane-inner-bg {
      background: #fff;

      .el-collapse {

        .el-collapse-item__header {
          background: #fff;
        }

        .el-collapse-item__content {
          background: #fff;
        }
      }
    }
  }
}
</style>
