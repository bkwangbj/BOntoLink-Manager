<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      size="small"
      :disabled="!saveAble"
      label-width="80px"
    >
      <el-form-item label="长度">
        <el-input-number
          v-model="form.length"
          controls-position="right"
          size="small"
          class="input-number-box-perc"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-form-item label="宽度">
        <el-input-number
          v-model="form.width"
          controls-position="right"
          size="small"
          class="input-number-box-px"
          @change="$emit('chartChange')"
        />
      </el-form-item>

      <el-form-item label="通常颜色">
        <CommonColorPicker
          v-model="form.color"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-form-item label="选中颜色">
        <CommonColorPicker
          v-model="form.selectColor"
          @change="$emit('chartChange')"
        />
      </el-form-item>
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
        width: 6,
        length: 60,
        color: '#1a4582',
        selectColor: '#2e2f36'
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
        if (this.formData) {
          if (JSON.stringify(this.formData !== '{}')) {
            this.setFormData(utils.deepClone(this.formData))
          }
        }
      }
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())
      Object.keys(form).forEach(key => {
        this.form[key] = form[key]
      }
      )
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
      const form = utils.deepClone(this.form)
      return form
    },
    typeChange () {
      this.form.type = ''
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
