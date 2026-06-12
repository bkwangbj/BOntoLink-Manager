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
            <el-input-number
              v-model="form.shadowBlur"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 模糊大小</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.shadowColor"
              show-alpha
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
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-input-number
              v-model="form.shadowOffsetX"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 水平偏移</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-input-number
              v-model="form.shadowOffsetY"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 垂直偏移</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
export default {
  name: 'ShadowStyle',
  inject: ['getSaveAble'],
  props: {
    configType: {
      type: String,
      default: 'chart'
    },
    editAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['shadowStyleChange'],
  data () {
    return {
      type: 'text',
      form: {
        shadowBlur: 0,
        shadowColor: 'rgba(0, 0, 0, 0)',
        shadowOffsetX: 0,
        shadowOffsetY: 0
      },
      extra: {
        textDecoration: 'none',
        fontStyle: 'normal'
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
      if (form) {
        for (const i in this.form) {
          if (form[i] !== undefined) {
            this.form[i] = form[i]
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
      this.$emit('shadowStyleChange', form)
    },
    getFormData () {
      let form = {}

      form = { ...this.form }

      return form
    }

  }
}
</script>
