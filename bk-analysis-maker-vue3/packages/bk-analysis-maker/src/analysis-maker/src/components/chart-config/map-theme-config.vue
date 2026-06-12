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
        style="width: 100%;"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.itemStyle.areaColor"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text">地图颜色</span>
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
              v-model="form.itemStyle.borderWidth"
              controls-position="right"
              size="small"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text">描边宽度</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.itemStyle.borderColor"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text">描边颜色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item
      label=""
    >
      <ShadowStyle
        ref="shadowRef"
        :edit-able="editAble"
        @shadow-style-change="shadowStyleChange($event)"
      />
    </el-form-item>
    <el-form-item>
      <div class="d-flex-c">
        <el-switch
          v-model="form.label.show"
          class="am-switch active-switch"
          size="small"
          @click.stop.prevent
          @change="$emit('chartChange')"
        />
        <span class="extra-bottom-text"> 显示标签</span>
      </div>
    </el-form-item>
    <el-form-item>
      <TextStyle
        ref="textStyleFrom"
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
        itemStyle:
        { areaColor: '', borderColor: '', borderWidth: 0 },
        label: { show: true, color: '#333333' }
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
      this.$refs.textStyleFrom.setFormData(form.label)
      this.$refs.shadowRef.setFormData(form?.itemStyle || {})
    },
    shadowStyleChange (shadowStyle) {
      this.form.itemStyle = { ...this.form.itemStyle, ...shadowStyle }
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
:deep(.el-form-item) {
  margin-right: 0;
}

</style>
