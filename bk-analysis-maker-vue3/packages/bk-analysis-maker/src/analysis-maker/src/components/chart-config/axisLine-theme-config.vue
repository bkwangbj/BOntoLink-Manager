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
      </el-row>
    </el-form-item>
    <el-form-item label="">
      <LineStyle
        ref="axisLineForm"
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
      form: { show: true, lineStyle: { width: 1, color: '#eaeff4', type: 'solid' } }
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
      form.lineStyle = this.$refs.axisLineForm.getFormData()
      return form
    },
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
      this.$refs.axisLineForm.setFormData(form?.lineStyle)
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-row) {
  width: 100%;
}

</style>
