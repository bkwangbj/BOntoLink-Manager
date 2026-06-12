<template>
  <el-form
    :ref="'form-text'"
    :model="form"
    size="small"
    label-width="10px"
    :disabled="!saveAble || !editAble"
    inline
    class="inline-col-form"
  >
    <el-row
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-select
              v-model="form.fontWeight"
              placeholder="请选择"
              size="small"
              @change="formChange"
            >
              <el-option
                v-for="item in fontWeightOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text"> 文字粗细</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-select
              v-model="form.fontFamily"
              placeholder="请选择"
              size="small"
              @change="formChange"
            >
              <el-option
                v-for="item in fontFamilyOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text"> 字体</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-input-number
              v-model="form.fontSize"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 字号</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.color"
              :edit-able="editAble"
              @change="formChange"
            />
            <span
              style="display: block;"
              class="extra-bottom-text"
            > 颜色</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row
      v-show="configType==='html'"
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-select
              v-model="extra.fontStyle"
              placeholder="请选择"
              size="small"
              @change="formChange"
            >
              <el-option
                v-for="item in fontStyleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text"> 文本风格</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-select
              v-model="extra.textDecoration"
              placeholder="请选择"
              size="small"
              @change="formChange"
            >
              <el-option
                v-for="item in textDecorationOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text"> 文本装饰</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
import { fontWeightOptions } from '../../../configs/chart-cfg'
export default {
  name: 'TextStyle',
  inject: ['getSaveAble', 'getThemeFont'],
  props: {
    configType: {
      type: String,
      default: 'chart'
    },
    modelValue: {
      type: Object,
      default: () => {}
    },
    editAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['textStyleChange', 'update:modelValue'],
  data () {
    return {
      type: 'text',
      textDecorationOptions: Object.freeze([{ value: 'none', label: '默认' }, { value: 'underline', label: '下划线' }, { value: 'overline', label: '上划线' }, { value: 'line-through', label: '删除线' }]),
      fontStyleOptions: Object.freeze([{ value: 'normal', label: '默认' }, { value: 'italic', label: '斜体样式' }, { value: 'oblique', label: '倾斜样式' }]),
      fontWeightOptions: Object.freeze(fontWeightOptions),
      fontFamilyOptions: Object.freeze([{ label: 'Microsoft YaHei', value: 'Microsoft YaHei' }, { label: 'sans-serif', value: 'sans-serif' }, { label: 'Arial', value: 'Arial' }]),
      form: {
        color: '#000',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'normal'
      },
      extra: {
        textDecoration: 'none',
        fontStyle: 'normal'
      },
      ischange: false
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    },
    defaultColor () {
      if (!this.getThemeFont) {
        return
      }
      return this.getThemeFont()
    }
  },
  watch: {
    defaultColor: {
      handler () {
        if (this.defaultColor && !this.ischange) {
          this.form.color = this.defaultColor || '#000'
        }
      },
      immediate: true
    },
    modelValue: {
      handler () {
        this.setFormData(this.modelValue)
      },
      immediate: true
    }
  },
  methods: {
    setFormData (form) {
      this.ischange = true
      if (this.defaultColor) {
        this.form.color = this.defaultColor
      }

      Object.assign(this.$data, this.$options.data())
      if (form) {
        for (const i in this.form) {
          if (form[i] !== undefined) {
            this.form[i] = form[i]
          }
        }
        if (this.configType === 'html') {
          for (const i in this.extra) {
            if (form[i] !== undefined) {
              this.extra[i] = form[i]
            }
          }
        }
      }
    },
    formChange () {
      let form = {}
      if (this.configType === 'html') {
        form = { ...this.form, ...this.extra }
      } else {
        form = { ...this.form }
      }
      this.$emit('update:modelValue', form)
      this.$emit('textStyleChange', form)
    },
    getFormData () {
      let form = {}
      if (this.configType === 'html') {
        form = { ...this.form, ...this.extra }
      } else {
        form = { ...this.form }
      }
      return form
    }

  }
}
</script>
// <style lang="scss" scoped>
// :deep() {

// }
// </style>
