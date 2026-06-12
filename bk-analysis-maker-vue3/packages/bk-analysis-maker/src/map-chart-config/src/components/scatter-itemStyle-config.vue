<template>
  <el-form
    :model="form"
    label-width="80px"
    size="small"
    :disabled="!saveAble"
  >
    <el-form-item label="圆点颜色">
      <CommonColorPicker
        v-model="form.color"
        @change="formChange"
      />
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
              v-model="form.borderWidth"
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
              v-model="form.borderColor"
              @change="formChange"
            />
            <span class="extra-bottom-text">边框颜色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
  </el-form>
</template>

<script>
import { utils } from 'efficient-suite'
export default {
  components: {
  },
  inject: ['getSaveAble'],
  props: {
    series: {
      type: [Object, Array],
      default: () => {}
    }
  },
  emits: ['scatterChange'],
  data () {
    return {
      form: {
        color: '',
        borderColor: '',
        borderWidth: 0

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
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())

      this.form = utils.deepClone(this.$options.data().form)
      this.form = Object.assign(this.form, form)
      this.$nextTick(() => {
        this.$refs.shadowRef.setFormData(form?.itemStyle || {})
      })
    },
    formChange () {
      this.$emit('scatterChange', this.form)
    },
    shadowChange (shadowStyle) {
      this.form = { ...this.form, ...shadowStyle }
      this.$emit('scatterChange', this.form)
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
