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
              v-model="form.fontSize"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />            <span class="extra-bottom-text"> 字号</span>
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
            <CommonColorPicker
              v-model="form.backgroundColor"
              :edit-able="editAble"
              @change="formChange"
            />            <span class="extra-bottom-text"> 背景颜色</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item label="">
          <div class="d-flex-c">
            <el-input-number
              v-model="form.height"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />            <span class="extra-bottom-text"> 高度</span>
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
            <span class="extra-bottom-text"> 选中文字粗细</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
import { fontWeightOptions } from '../../../configs/chart-cfg'
import { utils } from 'efficient-suite'
export default {
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['textStyleChange'],
  data () {
    return {
      type: 'line',
      form: {
        color: '#747c8b',
        fontSize: 14,
        height: 35, // 高度
        backgroundColor: '#fff',
        fontWeight: 'bold'
      },
      fontWeightOptions
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
      this.$emit('textStyleChange', this.form)
    },
    getFormData () {
      const form = utils.deepClone(this.form)
      return form
    }
  }
}
</script>
