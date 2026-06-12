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
        :span="24"
      >
        <el-form-item
          label=""
          style="width: 100%;"
          class="radio-item"
        >
          <div class="d-flex-c">
            <EfRadio
              v-model="form.mergeChildChart"
              is-radio-button
              :mock="mergeChildChartList"
              size="small"
              style="width: 100%;"
              @change="formChange"
            />            <span class="extra-bottom-text">展现形式</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="24"
      >
        <el-form-item
          label=""
          style="width: 100%;"
          class="radio-item"
        >
          <div class="d-flex-c">
            <EfRadio
              v-model="form.cardMode"
              is-radio-button
              :mock="cardModeList"
              size="small"
              style="width: 100%;"
              @change="formChange"
            />            <span class="extra-bottom-text">布局</span>
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
              v-model="form.colNum"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />           <span class="extra-bottom-text"> 栅格列数</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item label="">
          <div
            v-if="form.cardMode == 'adaptive'"
            class="d-flex-c"
          >
            <el-input-number
              v-model="form.maxRows"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />            <span class="extra-bottom-text"> 最大行数</span>
          </div>
          <div
            v-else
            class="d-flex-c"
          >
            <el-input-number
              v-model="form.rowHeight"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />            <span class="extra-bottom-text"> 每行高度</span>
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
              v-model="form.rowMargin"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />           <span class="extra-bottom-text"> 行间距</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item label="">
          <div class="d-flex-c">
            <el-input-number
              v-model="form.colMargin"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />            <span class="extra-bottom-text"> 列间距</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
import { mergeChildChartList, cardModeList } from '../../../configs/chart-cfg'
import { utils } from 'efficient-suite'
export default {
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['childStyleChange'],
  data () {
    return {
      type: 'line',
      form: {
        mergeChildChart: 'merge', // 子页面组件合并
        cardMode: 'auto', // 子页面布局
        colNum: 12,
        rowHeight: 1,
        rowMargin: 0,
        colMargin: 5,
        maxRows: 40
      },
      mergeChildChartList,
      cardModeList
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
          if (i === 'margin') {
            this.form.rowMargin = form.margin[0]
            this.form.colMargin = form.margin[1]
            continue
          }
          if (form[i] !== undefined) {
            this.form[i] = form[i]
          }
        }
      }
    },
    formChange () {
      const form = utils.deepClone(this.form)
      form.margin = [form.rowMargin, form.colMargin]
      delete form.rowMargin
      delete form.colMargin
      this.$emit('childStyleChange', form)
    },
    getFormData () {
      const form = utils.deepClone(this.form)
      form.margin = [form.rowMargin, form.colMargin]
      delete form.rowMargin
      delete form.colMargin
      return form
    }
  }
}
</script>
