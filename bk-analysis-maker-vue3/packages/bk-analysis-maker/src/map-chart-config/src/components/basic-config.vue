<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      size="small"
      :disabled="!saveAble"
      label-width="80px"
    >
      <el-form-item
        label="开启缩放"
        size="small"
      >
        <el-switch
          v-model="form.roam"
          class="am-switch active-switch"
          size="small"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-collapse v-model="normalShow">
        <el-collapse-item name="normal">
          <template #title>
            <div class="collapse-title-common ">
              通常样式
            </div>
          </template>
          <div class="plane-inner-bg">
            <div style="width: 80px;margin-bottom: 10px;">
              <el-switch
                v-model="form.labelContent"
                class="am-switch active-switch"
                size="small"
                @click.stop.prevent
                @change="$emit('chartChange')"
              />
              <span class="chart-config-title">
                标签内容</span>
            </div>
            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.label.show"
                  class="am-switch active-switch"
                  size="small"
                  @click.stop.prevent
                  @change="$emit('chartChange')"
                />
                <span class="chart-config-title">
                  标签</span>
              </div>

              <text-style
                ref="normalTextRef"
                :disabled="!form.label.show"
                @text-style-change="textStyleChange($event,'normal')"
              />
            </div>
            <el-form-item label="阴影">
              <ShadowStyle
                ref="normalShadowRef"
                @shadow-style-change="shadowStyleChange($event,'normal')"
              />
            </el-form-item>
            <el-form-item label="地图样式">
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
                      v-model="form.itemStyle.areaColor"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">地图颜色</span>
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
                      v-model="form.itemStyle.borderWidth"
                      controls-position="right"
                      size="small"
                      class="input-number-box-px"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">描边宽度</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <CommonColorPicker
                      v-model="form.itemStyle.borderColor"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">描边颜色</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
          </div>
        </el-collapse-item>
        <el-collapse-item>
          <template #title>
            <div class="collapse-title-common ">
              选中样式
            </div>
          </template>
          <div class="plane-inner-bg">
            <div class="d-flex">
              <div style="width: 80px;">
                <el-switch
                  v-model="form.select.label.show"
                  class="am-switch active-switch"
                  size="small"
                  @change="$emit('chartChange')"
                  @click.stop.prevent
                />
                <span class="chart-config-title">
                  标签</span>
              </div>
              <el-form
                ref="标签"
                :disabled="!form.select.label.show"
                label-width="0px"
              >
                <el-form-item label="">
                  <text-style

                    ref="selectTextRef"
                    @text-style-change="textStyleChange($event,'select')"
                  />
                </el-form-item>
              </el-form>
            </div>
            <el-form-item label="阴影">
              <ShadowStyle
                ref="selectShadowRef"
                @shadow-style-change="shadowStyleChange($event,'select')"
              />
            </el-form-item>
            <el-form-item label="地图样式">
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
                      v-model="form.select.itemStyle.areaColor"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">地图颜色</span>
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
                      v-model="form.select.itemStyle.borderWidth"
                      controls-position="right"
                      size="small"
                      class="input-number-box-px"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">描边宽度</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <CommonColorPicker
                      v-model="form.select.itemStyle.borderColor"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">描边颜色</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
          </div>
        </el-collapse-item>
      </el-collapse>
      <CollapseItem
        v-model="form.emphasis.show"
        name="高亮样式"
        @change="$emit('chartChange')"
      >
        <div class="d-flex">
          <div style="width: 80px;">
            <el-switch
              v-model="form.emphasis.label.show"
              class="am-switch active-switch"
              size="small"
              @change="$emit('chartChange')"
              @click.stop.prevent
            />
            <span class="chart-config-title">
              标签</span>
          </div>
          <el-form
            ref="标签"
            :disabled="!form.emphasis.label.show"
            label-width="0px"
          >
            <el-form-item label="">
              <text-style
                ref="emphasisTextRef"
                @text-style-change="textStyleChange($event,'emphasis')"
              />
            </el-form-item>
          </el-form>
        </div>
        <el-form-item label="阴影">
          <ShadowStyle
            ref="emphasisShadowRef"
            @shadow-style-change="shadowStyleChange($event,'emphasis')"
          />
        </el-form-item>
        <el-form-item label="地图样式">
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
                  v-model="form.emphasis.itemStyle.areaColor"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">地图颜色</span>
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
                  v-model="form.emphasis.itemStyle.borderWidth"
                  controls-position="right"
                  size="small"
                  class="input-number-box-px"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">描边宽度</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <CommonColorPicker
                  v-model="form.emphasis.itemStyle.borderColor"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text">描边颜色</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
      </CollapseItem>
    </el-form>
  </div>
</template>

<script>
import { utils } from 'efficient-suite'
export default {
  inject: ['getSaveAble'],
  props: {
    formData: {

      type: [Array, Object],
      default: () => {}
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      form: {
        roam: true,
        labelContent: false,

        label: {
          show: false
        },

        itemStyle: {
          areaColor: '#fff',
          borderColor: '#000',
          borderWidth: 0
        },

        emphasis: {
          disabled: false,
          show: false,
          label: {
            show: false
          },
          itemStyle: {
            areaColor: '#fff',
            borderColor: '#000',
            borderWidth: 0
          }
        },
        select: {
          disabled: false,
          show: false,
          label: {
            show: false
          },
          itemStyle: {
            areaColor: '#fff',
            borderColor: '#000',
            borderWidth: 0
          }
        }
      },
      form2: {
        select: { itemStyle: {} },
        emphasis: { itemStyle: {} },
        itemStyle: {}
      },
      normalShow: ['normal'],
      borderOptions: [{ label: '完整边框', value: ' full' }, { label: '外边框', value: 'outer' }, { label: '内边框', value: 'inner' }, { label: '无边框', value: 'none' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    formData: {
      immediate: true,
      handler () {
        this.$nextTick(() => {
          if (this.formData) {
            if (JSON.stringify(this.formData !== '{}')) {
              this.setFormData(utils.deepClone(this.formData))
            }
          }
        })
      }
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())
      if (Array.isArray(form)) {
        const geo1 = form[0]
        const geo2 = form[1]
        Object.keys(geo1).forEach(key => {
          if (['label', 'itemStyle'].includes(key)) {
            this.form[key] = Object.assign(this.form[key], geo1[key])
          } else if (['emphasis'].includes(key)) {
            this.form[key].label = Object.assign(this.form[key].label, geo1[key]?.label || {})
            this.form[key].itemStyle = Object.assign(this.form[key].itemStyle, geo1[key].itemStyle || {})
            this.form[key].show = !geo1[key].disabled
          } else {
            this.form[key] = geo1[key]
          }
        })
        this.$refs.normalShadowRef.setFormData(geo2?.itemStyle || {})
        this.$refs.normalTextRef.setFormData(geo1.label)
        this.$refs.emphasisShadowRef.setFormData(geo2?.emphasis?.itemStyle || {})
        this.$refs.emphasisTextRef.setFormData(geo1.emphasis?.label || {})
        this.$refs.selectTextRef.setFormData(geo1.select?.label || {})
        this.$refs.selectShadowRef.setFormData(geo2?.select?.itemStyle || {})
      } else {
        Object.keys(form).forEach(key => {
          if (['label', 'itemStyle'].includes(key)) {
            this.form[key] = Object.assign(this.form[key], form[key])
          } else if (['emphasis'].includes(key)) {
            this.form[key].label = Object.assign(this.form[key].label, form[key]?.label || {})
            this.form[key].itemStyle = Object.assign(this.form[key].itemStyle, form[key].itemStyle || {})
            this.form[key].show = !form[key].disabled
          } else {
            this.form[key] = form[key]
          }
        })
        this.$refs.normalShadowRef.setFormData(form?.itemStyle || {})
        this.$refs.normalTextRef.setFormData(form.label)
        this.$refs.emphasisShadowRef.setFormData(form?.emphasis?.itemStyle || {})
        this.$refs.emphasisTextRef.setFormData(form.emphasis?.label || {})
        this.$refs.selectTextRef.setFormData(form.select?.label || {})
        this.$refs.selectShadowRef.setFormData(form?.select?.itemStyle || {})
      }
    },
    textStyleChange (textStyle, type) {
      if (type === 'normal') {
        this.form.label = { ...this.form.label, ...textStyle }
      } else {
        this.form[type].label = { ...this.form[type].label, ...textStyle }
      }

      this.$emit('chartChange')
    },
    shadowStyleChange (shadowStyle, type) {
      if (type === 'normal') {
        this.form.itemStyle = { ...this.form.itemStyle, ...shadowStyle }
      } else {
        this.form[type].itemStyle = { ...this.form[type].itemStyle, ...shadowStyle }
      }

      this.$emit('chartChange')
    },
    saveFormData () {
      if (Array.isArray(this.formData)) {
        this.form.emphasis.disabled = !this.form.emphasis.show
        const form = utils.deepClone({ geo: [this.form, { ...this.formData[1], ...this.form2, roam: this.form.roam }] })
        return form
      } else {
        this.form.emphasis.disabled = !this.form.emphasis.show
        const form = utils.deepClone({ geo: this.form })
        return form
      }
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
