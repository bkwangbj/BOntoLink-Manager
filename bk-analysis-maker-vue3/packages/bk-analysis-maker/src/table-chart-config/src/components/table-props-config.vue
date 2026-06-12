<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      size="small"
      label-width="80px"
      :disabled="!saveAble"
      class="inline-col-form"
    >
      <el-form-item label="分页">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col :span="12">
            <div class="d-flex-c">
              <el-switch
                v-model="form.hasPager"
                class="am-switch active-switch"
                size="small"
                @click.stop.prevent
                @change="$emit('propsChange')"
              />
              <span class="extra-bottom-text"> 开启分页</span>
            </div>
          </el-col>

          <el-col :span="12">
            <div class="d-flex-c">
              <el-input-number
                v-model="form.pageSize"
                size="small"
                controls-position="right"
                @change="$emit('propsChange')"
              />
              <span class="extra-bottom-text"> 每页行数</span>
            </div>
          </el-col>
        </el-row>
        <el-row
          :gutter="10"
          type="flex"
          style="width: 100%;"
        >
          <el-col :span="12">
            <div class="d-flex-c">
              <el-switch
                v-model="form.isStatic"
                class="am-switch active-switch"
                size="small"
                @click.stop.prevent
                @change="$emit('propsChange')"
              />
              <span class="extra-bottom-text"> 静态分页</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>

      <el-form-item label="序号列">
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
                @change="$emit('propsChange')"
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
                @change="$emit('propsChange')"
              />
              <span class="extra-bottom-text"> 序号列宽度</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="高亮行">
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
                @change="$emit('propsChange')"
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
                @change="$emit('propsChange')"
              />
              <span class="extra-bottom-text"> 高亮颜色</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="表格边框">
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
                @change="$emit('propsChange')"
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
                @change="$emit('propsChange')"
              />
              <span class="extra-bottom-text"> 边框颜色</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="基础样式">
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
                @change="$emit('propsChange')"
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
                @change="$emit('propsChange')"
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
                @change="$emit('propsChange')"
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
                @change="$emit('propsChange')"
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
                @change="$emit('propsChange','align')"
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
      <el-form-item label="文本样式">
        <text-style
          ref="bodyStyle"
          :config-type="'html'"
          @text-style-change="textStyleChange($event,'body')"
        />
      </el-form-item>
      <CollapseItem
        v-model="form.showHeader"
        name="表头"
        @change="$emit('propsChange')"
      >
        <el-form
          ref="axisLabel"
          :disabled="!saveAble"
          label-width="80px"
        >
          <el-form-item label="高度">
            <el-row
              :gutter="10"
              type="flex"
            >
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <el-input-number
                    v-model="styles['--headercellheight']"
                    size="small"
                    class="input-number-box-px"
                    controls-position="right"
                    @change="$emit('propsChange')"
                  />
                  <span class="extra-bottom-text"> 单元格高度</span>
                </div>
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item label="对齐方式">
            <el-select
              v-model="form.headerAlign"
              size="small"
              placeholder="请选择"
              @change="$emit('propsChange','headerAlign')"
            >
              <el-option
                v-for="item in alignOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="文本样式">
            <text-style
              ref="headerStyle"
              :config-type="'html'"
              @text-style-change="textStyleChange($event,'header')"
            />
          </el-form-item>
          <el-form-item label="背景色">
            <CommonColorPicker
              v-model="styles['--headerBg']"
              @change="$emit('propsChange')"
            />
          </el-form-item>
        </el-form>
      </CollapseItem>
    </el-form>
  </div>
</template>

<script>
import TextStyle from '../../../chart-common-config/components/style-config/text-style.vue'
import { utils } from 'efficient-suite'
export default {
  components: {

    TextStyle
  },
  inject: ['getSaveAble'],
  emits: ['propsChange'],
  data () {
    return {
      styles: {
        '--borderColor': '#e8eaec',
        '--stripeColor': '#fafafa',
        '--bodyBg': '#fff',
        '--tablecellheight': 48,
        '--headercellheight': 48,
        '--headerBg': '#f8f8f9',
        '--rowCurrentColor': '#e6f7ff'
      },
      form: {
        seqWidth: 60,
        hasPager: false,
        stripe: false,
        pageSize: 20,
        isStatic: false,
        highlightCurrentRow: false,
        showHeader: true,
        hasSeq: true,
        headerAlign: 'center',
        border: 'outer',
        align: 'center',
        showHeaderOverflow: true
      },
      borderOptions: [{ label: '完整边框', value: 'full' }, { label: '外边框', value: 'outer' }, { label: '内边框', value: 'inner' }, { label: '无边框', value: 'none' }],
      alignOptions: [{ label: '居中', value: 'center' }, { label: '右对齐', value: 'right' }, { label: '左对齐', value: 'left' }]
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
      this.$emit('propsChange')
    },
    saveFormData () {
      const form = utils.deepClone({ props: this.form, styles: this.styles })
      return form
    },
    getGradientColor (startColor, endColor) {
      return {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: startColor },
          { offset: 1, color: endColor }
        ]
      }
    }
  }
}
</script>
<style  lang="scss" scoped>
.text-style-form > .el-form-item__content {
  margin-left: 0 !important;
}

.d-flex-c {
  display: flex;
  flex-direction: column;
}

.ai-c {
  align-items: center;
}

.d-flex {
  display: flex;
}

.el-row {
  width: 100%;
}

</style>
