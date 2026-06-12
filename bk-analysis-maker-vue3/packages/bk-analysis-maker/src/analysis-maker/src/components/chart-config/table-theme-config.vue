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
              v-model="form.hasSeq"
              class="am-switch active-switch"
              size="small"
              @change="$emit('chartChange')"
              @click.stop.prevent
            />
            <span class="extra-bottom-text">
              显示序号列</span>
          </div>
        </el-col> <el-col :span="12">
          <div class="d-flex-c">
            <el-input-number
              v-model="form.seqWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 序号列宽度</span>
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
            <el-switch
              v-model="form.highlightCurrentRow"
              class="am-switch active-switch"
              size="small"
              @change="$emit('chartChange')"
              @click.stop.prevent
            />
            <span class="extra-bottom-text"> 开启高亮</span>
          </div>
        </el-col>
        <el-col
          :span="12"
          style="padding-right: 10px;"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="styles['--rowCurrentColor']"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 高亮颜色</span>
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
              v-model="form.border"
              size="small"
              placeholder="请选择"
              @change="$emit('chartChange')"
            >
              <el-option
                v-for="item in borderOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text"> 边框类型</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="styles['--borderColor']"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 边框颜色</span>
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
            <el-input-number
              v-model="styles['--tablecellheight']"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 单元格高度</span>
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
              v-model="form.stripe"
              class="am-switch active-switch"
              size="small"
              @change="$emit('chartChange')"
              @click.stop.prevent
            />
            <span class="extra-bottom-text"> 开启斑马纹</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="styles['--stripeColor']"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 斑马纹颜色</span>
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
            <CommonColorPicker
              v-model="styles['--bodyBg']"
              :edit-able="editAble"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 背景颜色</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-select
              v-model="form.align"
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
            <span class="extra-bottom-text"> 对齐方式</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item label="">
      <text-style
        ref="bodyStyle"
        :config-type="'html'"
        :edit-able="editAble"
        @text-style-change="textStyleChange($event,'body')"
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
        '--borderColor': '#e8eaec',
        '--stripeColor': '#fafafa',
        '--bodyBg': '#fff',
        '--tablecellheight': 48,
        '--rowCurrentColor': '#e6f7ff'
      },
      form: {
        seqWidth: 60,
        stripe: false,
        highlightCurrentRow: false,
        hasSeq: true,
        border: 'outer',
        align: 'center'
      },
      borderOptions: Object.freeze([{ label: '完整边框', value: 'full' }, { label: '外边框', value: 'outer' }, { label: '内边框', value: 'inner' }, { label: '无边框', value: 'none' }]),
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
      this.$refs.bodyStyle.setFormData({
        color: styles['--bodycolor'],
        fontSize: styles['--bodyfontSize'],
        fontFamily: styles['--bodyfontFamily'],
        fontWeight: styles['--bodyfontWeight'],
        textDecoration: styles['--bodytextDecoration'],
        fontStyle: styles['--bodyfontStyle']
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
    },
    alignChange (ele) {
      this.form.label.position = ele.value
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-row) {
  width: 100%;
}

</style>
