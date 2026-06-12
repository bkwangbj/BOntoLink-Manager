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
        <el-col :span="12">
          <div class="d-flex-c">
            <el-switch
              v-model="form.showHeader"
              class="am-switch active-switch"
              size="small"
              @change="$emit('chartChange')"
              @click.stop.prevent
            />
            <span class="extra-bottom-text">
              显示表头</span>
          </div>
        </el-col> <el-col :span="12">
          <div class="d-flex-c">
            <el-input-number
              v-model="styles['--headercellheight']"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 表头单元格高度</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item label="">
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-select
              v-model="form.headerAlign"
              size="small"
              placeholder="请选择"
              @change="$emit('chartChange')"
            >
              <el-option
                v-for="item in alignOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text">对齐方式</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="styles['--headerBg']"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 表头背景色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item label="">
      <text-style
        ref="headerStyle"
        :config-type="'html'"
        :edit-able="editAble"
        @text-style-change="textStyleChange($event,'header')"
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
      styles: {
        '--headercellheight': 48,
        '--headerBg': '#f8f8f9'
      },
      form: {
        showHeader: true,
        headerAlign: 'center'

      },
      alignOptions: Object.freeze([{ label: '居中', value: 'center' }, { label: '右对齐', value: 'right' }, { label: '左对齐', value: 'left' }])
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    setFormData ({ styles, props }) {
      Object.assign(this.$data, this.$options.data())

      this.styles = Object.assign(this.styles, styles)
      this.form = Object.assign(this.form, props)
      this.$refs.headerStyle.setFormData({
        color: styles['--headercolor'],
        fontSize: styles['--headerfontSize'],
        fontFamily: styles['--headerfontFamily'],
        fontWeight: styles['--headerfontWeight'],
        textDecoration: styles['--headertextDecoration'],
        fontStyle: styles['--headerfontStyle']
      })
    },
    textStyleChange (textStyle, type) {
      Object.keys(textStyle).forEach(ele => {
        this.styles['--' + type + ele] = textStyle[ele]
      })
      this.$emit('chartChange')
    },
    saveFormData () {
      const form = utils.deepClone({ props: this.form, styles: this.styles })
      return form
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-row) {
  width: 100%;
}

</style>
