<template>
  <el-form
    ref="form"
    size="small"
    :disabled="!saveAble || !editAble"
    class="inline-col-form"
    label-width="0px"
  >
    <el-form-item label="">
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.lineStyle.width"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 折线粗细</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-select
              v-model="form.lineStyle.type"
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
            </el-select>
            <span class="extra-bottom-text"> 折线类型</span>
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
            <EfRadio
              v-model="form.smooth"
              is-radio-button
              :mock="smoothList"
              size="small"
              style="width: 100%;"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 曲线类型</span>
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
            <el-switch
              v-model="form.showSymbol"
              :disabled="!saveAble"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="formChange"
            />
            <span class="extra-bottom-text"> 圆点显示</span>
          </div>
        </el-col>
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
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-select
              v-model="form.symbol"
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
    <el-form-item>
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col :span="12">
          <div class="d-flex-c">
            <el-switch
              v-model="form.label.show"
              name="值标签"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="formChange"
            />
            <span class="extra-bottom-text"> 显示值标签</span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="d-flex-c">
            <el-switch
              v-model="form.isArea"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="formChange"
            />
            <span class="extra-bottom-text"> 显示区域</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item>
      <text-style
        ref="labelTextRef"
        :edit-able="editAble"
        @text-style-change="textStyleChange"
      />
    </el-form-item>
  </el-form>
</template>

<script>
import { utils } from 'efficient-suite'
export default {
  name: 'TooltipConfig',
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
        showSymbol: true,
        symbolSize: 4,
        symbol: 'emptyCircle',
        lineStyle: {
          type: 'solid',
          width: 2
        },
        isArea: false,
        smooth: false,
        label: { show: false }
      },
      smoothList: [{ value: true, label: '平滑' }, { value: false, label: '折线' }],
      symbolOptions: Object.freeze([{ label: '默认', value: '' }, { label: '空', value: 'none' }, { label: '空心圆环', value: 'emptyCircle' }, { label: '圆点', value: 'circle' }, { label: '方形', value: 'rect' }, { label: '圆角方形', value: 'roundRect' }, { label: '三角形', value: 'triangle' }, { label: '菱形', value: 'diamond' }, { label: '箭头', value: 'arrow' }]),
      lineTypeOptions: Object.freeze([{ label: '实线', value: 'solid' }, { label: '虚线', value: 'dashed' }, { label: '点线', value: 'dotted' }])
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
      if (form?.label) {
        this.$refs.labelTextRef.setFormData(form?.label || {})
      }
    },
    formChange () {
      this.$emit('chartChange')
    },
    textStyleChange (textStyle) {
      this.form.label = { ...this.form.label, ...textStyle }
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-row) {
  width: 100%;
}

</style>
