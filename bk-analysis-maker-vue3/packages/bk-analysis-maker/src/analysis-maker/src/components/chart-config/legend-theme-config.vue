<template>
  <el-form
    ref="form"
    size="small"
    :disabled="!saveAble || !editAble"
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
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.itemGap"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text">图例间隔</span>
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
              v-model="form.itemWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text">图形宽度</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.itemHeight"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 图形高度</span>
          </div>
        </el-col>
      </el-row>
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
  name: 'LegendConfig',
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
        itemGap: 10,
        itemHeight: 15,
        itemWidth: 25,
        textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' }
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
.d-flex {
  display: flex;
}

</style>
