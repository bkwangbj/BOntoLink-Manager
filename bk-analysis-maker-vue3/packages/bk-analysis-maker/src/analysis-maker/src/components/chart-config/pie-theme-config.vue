<template>
  <el-form
    ref="form"
    size="small"
    :disabled="!saveAble || !editAble"
    label-width="0px"
  >
    <el-form-item>
      <el-row
        :gutter="10"
        type="flex"
        style="width: 100%;"
      >
        <el-col
          :span="12"
        >
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
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <div class="d-flex">
              <div
                v-for="ele in alignOption"
                :key="ele.value"
                class="chart-align-item"
                :class="{'active':form.label.position===ele.value,'is-disabled':!saveAble||!editAble}"
                @click="alignChange(ele)"
              >
                <i :class="['icon am-iconfont align-icon', ele.icon]" />
              </div>
            </div>
            <span class="extra-bottom-text"> 标签位置</span>
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
            <el-switch
              v-model="form.labelLine.show"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="formChange"
            />
            <span class="extra-bottom-text"> 显示引线</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.labelLine.lineStyle.width"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 引线粗细</span>
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
              v-model="form.labelLine.length"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 第一段长度</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.labelLine.length2"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 第二段长度</span>
          </div>
        </el-col>
      </el-row>

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
      alignOption: Object.freeze([{ label: '外侧', value: 'outside', icon: 'icon-biaoqian-waibu' }, { label: '内部', value: 'inner', icon: 'icon-biaoqian-neibu' }]),
      form: {
        label: { show: false, position: 'outside' },
        labelLine: { show: false, length: 5, length2: 10, lineStyle: { width: 1 } }
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
    },
    alignChange (ele) {
      this.form.label.position = ele.value
      this.$emit('chartChange')
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
