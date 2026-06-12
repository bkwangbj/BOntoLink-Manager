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
      <div class="d-flex-c">
        <el-switch
          v-model="form.show"
          :disabled="!saveAble"
          class="am-switch active-switch"
          size="small"
          @click.stop.prevent
          @change="$emit('chartChange')"
        />
        <span class="extra-bottom-text"> 是否显示</span>
      </div>
    </el-form-item>
    <el-form-item>
      <TextStyle
        ref="textStyleFrom"
        :edit-able="editAble"
        @text-style-change="textStyleChange"
      />
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
            <CommonColorPicker
              v-model="form.backgroundColor"
              show-alpha
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 背景色</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.padding"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 内边距</span>
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
              v-model="form.borderWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 边框粗细</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.borderColor"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 边框颜色</span>
          </div>
        </el-col>
      </el-row>
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
        show: true,
        backgroundColor: '#fff',
        padding: 5,
        borderWidth: 0,
        borderColor: '#000',
        textStyle: {
          color: '#7A7A7A',
          fontSize: 12,
          fontFamily: 'Microsoft YaHei',
          fontWeight: 'normal'
        }
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
      form.textStyle = this.$refs.textStyleFrom.getFormData()
      return form
    },
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
      this.$refs.textStyleFrom.setFormData(form?.textStyle)
    },
    textStyleChange (textStyle) {
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
