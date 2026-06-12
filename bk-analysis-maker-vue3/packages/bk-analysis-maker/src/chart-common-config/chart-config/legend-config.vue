<template>
  <div class="legend-config">
    <CollapseItem
      v-model="form.show"
      :is-expand="isExpand"
      name=" 图例配置"
      @change="legendShowChange"
    >
      <EfForm
        ref="form"
        label-width="80px"
        :items="items"
        :has-reset="false"
        :has-submit="false"
      />
    </CollapseItem>
  </div>
</template>

<script lang="jsx">
import TextStyle from '../components/style-config/text-style.vue'
import { utils } from 'efficient-suite'
export default {
  name: 'LegendConfig',
  components: {
    // eslint-disable-next-line
    TextStyle
  },
  inject: ['getSaveAble'],
  props: {
    isExpand: {
      default: false,
      type: Boolean
    }
  },
  emits: ['chartChange', 'legendShowChange'],
  data () {
    return {
      items: [
        {
          label: '类型',
          field: 'legendType',
          itemSlots: {
            default: () => {
              return [
                <el-radio-group
                  vModel={this.form.type}
                  style="margin-bottom: 10px;"
                  size="small"
                  onInput={() => { this.$emit('chartChange') }}
                >
                  <el-radio-button value="plain">
                    普通
                  </el-radio-button>
                  <el-radio-button value="scroll">
                    翻页
                  </el-radio-button>
                </el-radio-group>
              ]
            }
          }
        },
        {
          label: '文本',
          field: 'textStyle',
          itemClass: 'text-style-form',
          itemSlots: {
            default: () => {
              return [
                <TextStyle
                  ref={el => { this.textStyleFrom = el }}
                  onTextStyleChange={this.textStyleChange}
                />
              ]
            }
          }
        }, {
          label: '图形',
          field: 'item',
          itemSlots: {
            default: () => {
              return [
                <div>
                  <el-row
            gutter={10}
            type="flex"
          >
            <el-col
              span={12}
            >
              <div class="d-flex-c">
                <EfSelect
                  vModel={this.form.icon}
                  clearable
                  placeholder="请选择"
                  size="small"
                  mock={this.symbolOptions}
                  onChange={() => { this.$emit('chartChange') }}
                />
                <span class="extra-bottom-text"> 图形</span>
              </div>
            </el-col>
          </el-row>
          <el-row
            gutter={10}
            type="flex"
          >
            <el-col
              span={12}
            >
              <div class="d-flex-c">
                <el-input-number
                  vModel={this.form.itemWidth}
                  size="small"
                  controls-position="right"
                  class="input-number-box-px"
                  onChange={() => { this.$emit('chartChange') }}
                />
                <span class="extra-bottom-text"> 宽度</span>
              </div>
            </el-col>
            <el-col
              span={12}
            >
              <div class="d-flex-c">
                <el-input-number
                  vModel={this.form.itemHeight}
                  size="small"
                  controls-position="right"
                  class="input-number-box-px"
                 onChange={() => { this.$emit('chartChange') }}
                />
                <span class="extra-bottom-text"> 高度</span>
              </div>
            </el-col>
          </el-row>
                </div>
              ]
            }
          }
        }, {
          label: '',
          field: 'layoutStyle',
          itemSlots: {
            default: () => {
              return [
                <el-collapse style="width: 100%;">
            <el-collapse-item v-slots={{
              title: () => {
                return [
                    <div>
                      <span
                        class="collapse-title-common "
                      >
                        布局 </span>
                    </div>
                ]
              }
            }}>
              <el-form
                size="small"
                label-width="80px"
                disabled={!this.saveAble}
              >
                <el-form-item
                  label="间隔"
                >
                  <el-input-number
                    vModel={this.form.itemGap}
                    size="small"
                    class="input-number-box-px"
                    controls-position="right"
                    onChange={() => { this.$emit('chartChange') }}
                  />
                </el-form-item>
                <el-form-item
                  label="朝向"
                >
                  <el-radio-group
                    vModel={this.form.orient}
                    size="small"
                    onChange={() => { this.$emit('chartChange') }}
                  >
                    <el-radio-button value="horizontal">
                      水平
                    </el-radio-button>
                    <el-radio-button value="vertical">
                      垂直
                    </el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <el-form-item
                  label="宽度"
                >
                  <el-input-number
                    vModel={this.form.width}
                    size="small"
                    class="input-number-box-px"
                    controls-position="right"
                    onChange={() => { this.$emit('chartChange') }}
                  />
                </el-form-item>
                <el-form-item
                  label="高度"
                >
                  <el-input-number
                    vModel={this.form.height}
                    size="small"
                    class="input-number-box-px"
                    controls-position="right"
                   onChange={() => { this.$emit('chartChange') }}
                  />
                </el-form-item>
                <el-form-item
                  label="位置"
                >
                  <div class="d-flex">
                    {
                      this.alignOption.forEach(ele => {
                         <div
                      key={ele.value}
                      class="chart-align-item"
                      class={{ active: this.form.alignPosition === ele.value, 'is-disabled': !this.saveAble }}
                      onClick={() => { this.alignChange(ele) }}
                    >
                      <i class={['icon am-iconfont align-icon', ele.icon]} />
                    </div>
                      })
                    }

                  </div>
                  <el-row
                    gutter={10}
                    type="flex"
                    style="margin-top: 10px;"
                  >
                    <el-col
                      span={12}
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          vModel={this.form.left}
                          size="small"
                          controls-position="right"
                          class="input-number-box-perc"
                          onChange={this.chartChange}
                        />
                        <span class="extra-bottom-text"> 水平偏移</span>
                      </div>
                    </el-col>
                    <el-col
                      span={12}
                    >
                      <div class="d-flex-c">
                        <el-input-number
                          vModel={this.form.top}
                          size="small"
                          controls-position="right"
                          class="input-number-box-perc"
                          onChange={this.chartChange}
                        />
                        <span class="extra-bottom-text"> 垂直偏移</span>
                      </div>
                    </el-col>
                  </el-row>
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
              ]
            }
          },
          labelWidth: '0px'
        }
      ],
      symbolOptions: [{ label: '默认', value: '' }, { label: '空', value: 'none' }, { label: '空心圆环', value: 'emptyCircle' }, { label: '圆点', value: 'circle' }, { label: '方形', value: 'rect' }, { label: '圆角方形', value: 'roundRect' }, { label: '三角形', value: 'triangle' }, { label: '菱形', value: 'diamond' }, { label: '箭头', value: 'arrow' }],
      form: {
        show: false,
        itemGap: 10,
        icon: '',
        type: 'plain',
        itemHeight: 14,
        itemWidth: 25,
        width: null,
        height: null,
        top: 0,
        left: 0,
        orient: 'horizontal',
        alignPosition: 'topLeft'
      },
      alignOption: [{ label: '顶部居左', value: 'topLeft', icon: 'icon-tuli-dingbujuzuo' }, { label: '顶部居中', value: 'topCenter', icon: 'icon-tuli-dingbujuzhong' }, { label: '顶部居右', value: 'topRight', icon: 'icon-tuli-dingbujuyou' },
        { label: '底部居左', value: 'bottomLeft', icon: 'icon-tuli-dibujuzuo' }, { label: '底部居中', value: 'bottomCenter', icon: 'icon-a-tuli-dibujuzhong-' }, { label: '底部居右', value: 'bottomRight', icon: 'icon-tuli-dibujuyou' }],
      textStyleFrom: null
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },

  methods: {
    saveFormData () {
      const form = utils.deepClone(this.form)
      form.height = form.height ? form.height : undefined
      form.width = form.width ? form.width : undefined
      form.textStyle = this.textStyleFrom.getFormData()
      return form
    },
    setFormData (form) {
      this.form = { ...this.form, ...form, type: form?.type !== 'scroll' ? 'plain' : 'scroll' }
      this.textStyleFrom.setFormData(form?.textStyle)
    },
    textStyleChange (textStyle) {
      this.$emit('chartChange')
    },
    legendShowChange () {
      this.$emit('legendShowChange', this.form.show)
      this.$emit('chartChange')
    },
    chartChange () {
      this.form.alignPosition = ''
      this.$emit('chartChange')
    },
    alignChange (ele) {
      this.form.alignPosition = ele.value
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
.d-flex {
  display: flex;
}

</style>
