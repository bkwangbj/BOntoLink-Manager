<template>
  <el-form
    ref="form"
    size="small"
    :disabled="!saveAble || !editAble"
    inline
    label-width="0px"
  >
    <el-form-item
      label=""
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
              v-model="form.borderRadius[0]"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 柱体圆角左上</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.borderRadius[1]"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 柱体圆角右上</span>
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
              v-model="form.borderRadius[2]"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 柱体圆角右下</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.borderRadius[3]"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 柱体圆角左下</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>

    <el-form-item
      label=""
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
              v-model="form.barWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 柱子宽度</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
  </el-form>
</template>

<script>
import { utils } from 'efficient-suite'
export default {
  name: 'BarThemeConfig',
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      form: {
        borderRadius: [0, 0, 0, 0],
        barWidth: 0
      }
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
      return form
    },
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-form-item) {
  margin-right: 0;
}

</style>
