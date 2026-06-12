<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      size="small"
      :disabled="!saveAble"
      label-width="80px"
    >
      <el-form-item label="数据类型">
        <el-radio-group
          v-model="form.type"
          size="small"
          @change="typeChange"
        >
          <el-radio-button value="category">
            类目轴
          </el-radio-button>
          <el-radio-button value="value">
            数值轴
          </el-radio-button>
          <el-radio-button value="time">
            时间轴
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <DataFormatSelect
        v-show="form.type==='value'||form.type==='time'"
        v-model="form.dataType"
        :type="form.type"
        @data-type-change="$emit('chartChange')"
      />
      <el-form-item label="节点个数">
        <el-input-number
          v-model="form.spotNumber"
          controls-position="right"
          size="small"
          :min="2"
          :precision="0"
          class="input-number-box"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-collapse>
        <el-collapse-item>
          <template #title>
            <div class="collapse-title-common ">
              通常样式
            </div>
          </template>
          <div class="plane-inner-bg">
            <el-form-item label="节点大小">
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="styles.spot.width"
                      controls-position="right"
                      size="small"
                      class="input-number-box-px"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 宽度</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="styles.spot.height"
                      controls-position="right"
                      size="small"
                      class="input-number-box-px"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 高度</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
            <el-form-item label="填充颜色">
              <CommonColorPicker
                v-model="styles.spot.bgColor"
                @change="$emit('chartChange')"
              />
            </el-form-item>
            <el-form-item label="边框颜色">
              <CommonColorPicker
                v-model="styles.spot.borderColor"
                @change="$emit('chartChange')"
              />
            </el-form-item>
            <el-form-item label="边框粗细">
              <el-input-number
                v-model="styles.spot.borderWidth"
                controls-position="right"
                size="small"
                class="input-number-box-px"
                @change="$emit('chartChange')"
              />
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
            <el-form-item label="节点大小">
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="styles.selectspot.width"
                      controls-position="right"
                      size="small"
                      class="input-number-box-px"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 宽度</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="styles.selectspot.height"
                      controls-position="right"
                      size="small"
                      class="input-number-box-px"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 高度</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
            <el-form-item label="填充颜色">
              <CommonColorPicker
                v-model="styles.selectspot.bgColor"
                @change="$emit('chartChange')"
              />
            </el-form-item>
            <el-form-item label="边框颜色">
              <CommonColorPicker
                v-model="styles.selectspot.borderColor"
                @change="$emit('chartChange')"
              />
            </el-form-item>
            <el-form-item label="边框粗细">
              <el-input-number
                v-model="styles.selectspot.borderWidth"
                controls-position="right"
                size="small"
                class="input-number-box-px"
                @change="$emit('chartChange')"
              />
            </el-form-item>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-form>
  </div>
</template>

<script>
import { utils } from 'efficient-suite'
export default {
  components: {
  },
  inject: ['getSaveAble'],
  props: {
    formData: {

      type: Object,
      default: () => {}
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      form: {
        type: 'category',
        dataType: '',
        spotNumber: 3

      },
      styles: {
        spot: {
          borderColor: '',
          borderWidth: 0,
          bgColor: '#00b8fc',
          width: 6,
          height: 6
        },
        selectspot: {
          borderColor: '',
          borderWidth: 0,
          bgColor: '#ff7a75',
          width: 6,
          height: 6
        }
      }
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
      const { config, styles } = form
      if (config) {
        this.form = config || {}
      } if (styles) {
        this.styles = styles || {}
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
    saveFormData () {
      const form = utils.deepClone({ config: this.form, styles: this.styles })
      return form
    },
    typeChange () {
      this.form.dataType = ''
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
