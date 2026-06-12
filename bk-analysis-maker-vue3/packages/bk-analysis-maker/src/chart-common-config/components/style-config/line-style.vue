<template>
  <el-form
    :ref="'form-line'"
    :model="form"
    size="small"
    label-width="0px"
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
        <el-form-item label="">
          <div class="d-flex-c">
            <el-input-number
              v-model="form.width"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />            <span class="extra-bottom-text"> 宽度</span>
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
            />            <span class="extra-bottom-text"> 颜色</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>   <el-row
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <el-select
              v-model="form.type"
              placeholder="请选择"
              size="small"
              @change="formChange"
            >
              <el-option
                v-for="item in lineTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>            <span class="extra-bottom-text"> 类型</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
import { lineTypeOptions } from '../../../configs/chart-cfg'
import { utils } from 'efficient-suite'
export default {
  name: 'LineStyle',
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    },
    option: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['lineStyleChange'],
  data () {
    return {
      type: 'line',
      lineTypeOptions,
      form: {
        width: 1,
        color: '#000',
        type: 'solid'
      }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    option: {
      handler () {
        this.setFormData(this.option)
      },
      immediate: true
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
      this.$emit('lineStyleChange', this.form)
    },
    getFormData () {
      const form = utils.deepClone(this.form)
      return form
    }
  }
}
</script>
