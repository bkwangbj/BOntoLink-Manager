<template>
  <el-form
    :model="form"
    label-width="80px"
    size="small"
    :disabled="!saveAble"
  >
    <el-form-item label="开启散点">
      <el-switch

        v-model="scatterShow"
        :disabled="!saveAble"
        class="am-switch active-switch"
        size="small"
        @change="showChange"
      />
    </el-form-item>
    <el-form-item label="圆点">
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
          :span="24"
        >
          <div class="d-flex-c">
            <el-select
              v-model="form.symbol"
              allow-create
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
    <el-form-item label="阴影">
      <ShadowStyle
        ref="shadowRef"
        @shadow-style-change="shadowChange"
      />
    </el-form-item>
    <el-form-item
      label="边框样式"
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
              v-model="form.itemStyle.borderWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text">边框粗细</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.itemStyle.borderColor"
              @change="formChange"
            />
            <span class="extra-bottom-text">边框颜色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item label="坐标偏移">
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.symbolOffset[0]"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> x</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.symbolOffset[1]"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> y</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <CollapseItem
      v-model="form.emphasis.show"
      name="高亮样式"
      @change="formChange"
    >
      <ScatterItemStyleConfig
        :series="form.emphasis.itemStyle"
        @scatter-change="emphasisChange"
      />
    </CollapseItem>
    <CollapseItem
      v-model="form.select.show"
      name="选中样式"
      @change="formChange"
    >
      <ScatterItemStyleConfig
        :series="form.select.itemStyle"
        @scatter-change="selectChange"
      />
    </CollapseItem>
  </el-form>
</template>

<script>
import ScatterItemStyleConfig from './scatter-itemStyle-config.vue'
import { utils } from 'efficient-suite'
export default {
  components: {
    ScatterItemStyleConfig
  },
  inject: ['getSaveAble'],
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    series: {
      type: [Object, Array],
      default: () => {}
    }
  },
  emits: ['scatterChange', 'showChange', 'update:modelValue'],
  data () {
    return {
      scatterShow: false,
      form: {
        symbolSize: 4,
        symbol: 'circle',
        symbolKeepAspect: true,
        symbolOffset: [0, 0],
        itemStyle: {
          color: '#000'
        },
        label: { show: false },
        emphasis: {
          show: false,
          scale: true
        },
        select: {
          show: false

        }
      },
      symbolOptions: [{ label: '默认', value: '' }, { label: '空', value: 'none' }, { label: '空心圆环', value: 'emptyCircle' }, { label: '圆点', value: 'circle' }, { label: '方形', value: 'rect' }, { label: '圆角方形', value: 'roundRect' }, { label: '三角形', value: 'triangle' }, { label: '菱形', value: 'diamond' }, { label: '箭头', value: 'arrow' }]
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
            if (Array.isArray(this.series)) {
              this.setFormData(this.series[0])
            } else {
              this.setFormData(this.series)
            }
          })
        }
      },
      immediate: true
    },
    modelValue: {
      handler () {
        this.scatterShow = this.modelValue
      },
      immediate: true
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())
      this.scatterShow = this.modelValue
      this.form = utils.deepClone(this.$options.data().form)
      this.form = Object.assign(this.form, form)

      this.$nextTick(() => {
        this.$refs.shadowRef.setFormData(form?.itemStyle || {})
      })
    },
    showChange (v) {
      this.$emit('update:modelValue', v)
      this.$emit('showChange', v)
    },
    formChange () {
      this.$emit('scatterChange')
    },
    emphasisChange (form) {
      this.form.emphasis.itemStyle = form
      this.$emit('scatterChange')
    },
    selectChange (form) {
      this.form.select.itemStyle = form
      this.$emit('scatterChange')
    },
    shadowChange (shadowStyle) {
      this.form.itemStyle = { ...this.form.itemStyle, ...shadowStyle }
    },
    saveFormData () {
      return utils.deepClone(this.form)
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
</style>
