<template>
  <el-form
    ref="form"
    size="small"
    :disabled="!saveAble || !editAble"
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
            <el-switch
              v-model="form.show"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 是否显示</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.length"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 刻度线长度</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item label="">
      <div class="d-flex-c">
        <EfRadio
          v-model="form.inside"
          is-radio-button
          :mock="insideList"
          size="small"
          @change="$emit('chartChange')"
        />
        <span class="extra-bottom-text"> 刻度线朝向</span>
      </div>
    </el-form-item>
    <el-form-item label="">
      <LineStyle
        ref="axisTickForm"
        :edit-able="editAble"
        @line-style-change="$emit('chartChange')"
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
      form: { show: true, inside: 0, length: 5, lineStyle: { width: 1, color: '#DDD' } },
      insideList: [{ value: 0, label: '朝外' }, { value: 1, label: '朝内' }]
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
      form.lineStyle = this.$refs.axisTickForm.getFormData()
      return form
    },
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
      this.$refs.axisTickForm.setFormData(form?.lineStyle)
    }
  }
}
</script>

<style lang="scss" scoped>
.d-flex {
  display: flex;
}

</style>
