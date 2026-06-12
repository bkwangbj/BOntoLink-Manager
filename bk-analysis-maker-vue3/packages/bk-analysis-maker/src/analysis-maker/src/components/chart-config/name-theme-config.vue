<template>
  <el-form
    ref="form"
    size="small"
    :disabled="!saveAble || !editAble"
    label-width="0px"
  >
    <el-form-item label="">
      <div class="d-flex-c">
        <el-input-number
          v-model="form.nameGap"
          size="small"
          class="input-number-box-px"
          controls-position="right"
          @change="$emit('chartChange')"
        />
        <span class="extra-bottom-text"> 偏移量</span>
      </div>
    </el-form-item>
    <el-form-item label="">
      <div class="d-flex-c">
        <EfRadio
          v-model="form.nameLocation"
          is-radio-button
          :mock="nameLocationList"
          size="small"
          @change="$emit('chartChange')"
        />
        <span class="extra-bottom-text"> 位置</span>
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
        nameGap: 15,
        nameLocation: 'end',
        nameTextStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'bold' }
      },
      nameLocationList: [{ label: '起始', value: 'start' }, { label: '中部', value: 'middle' }, { label: '末尾', value: 'end' }]
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
      form.nameTextStyle = this.$refs.textStyleFrom.getFormData()
      return form
    },
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
      this.$refs.textStyleFrom.setFormData(form?.nameTextStyle || {})
    },
    textStyleChange (textStyle) {
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
.d-flex {
  display: flex;
}

</style>
