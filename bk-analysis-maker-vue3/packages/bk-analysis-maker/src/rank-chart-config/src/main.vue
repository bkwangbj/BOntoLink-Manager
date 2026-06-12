<template>
  <BKBasicChartConfig
    ref="basicChartConfig"
    :configs="configs"
    :page-config="pageConfig"
    :save-able="saveAble"
    :rules="rules"
    :items="items"
    :tjb-u-r-l="tjbURL"
    v-bind="$attrs"
    @set-expand="$emit('setExpand')"
    @config-option-init="configOptionInit"
    @save-chart-config="saveChartCfg"
  >
    <el-form
      ref="customForm"
      label-width="80px"
      size="small"
      :disabled="!saveAble"
      style=" padding-right: 10px;margin-top: 10px;"
    >
      <div
        class="copy-button-box"
      >
        <el-button
          type="primary"
          size="small"
          @click="copyConfig('rankBasic')"
        >
          复制基础配置
        </el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="!saveAble"
          @click="pasteConfig('rankBasic')"
        >
          粘贴基础配置
        </el-button>
      </div>
      <el-form-item label="风格">
        <el-radio-group
          v-model="props.type"
          size="small"

          @change="getChartBasicConfigData"
        >
          <el-radio-button value="yellow">
            黄
          </el-radio-button>
          <el-radio-button value="blue">
            蓝
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="进度条">
        <div class="d-flex-c">
          <CommonColorPicker
            v-model="form['--progressBgColor']"
            @change="getChartBasicConfigData"
          />
          <span class="extra-bottom-text"> 背景色</span>
        </div>
      </el-form-item>
      <el-form-item
        label="标题样式"
        size="small"
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
                v-model="form['--titlefontSize']"
                size="small"
                class="input-number-box-px"
                controls-position="right"
                @change="getChartBasicConfigData"
              />
              <span class="extra-bottom-text"> 字号</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-select
                v-model="form['--titlefontWeight']"
                placeholder="请选择"
                size="small"
                @change="getChartBasicConfigData"
              >
                <el-option
                  v-for="item in fontWeightOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <span class="extra-bottom-text"> 文字粗细</span>
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
                v-model="form['--titlecolor']"
                @change="getChartBasicConfigData"
              />
              <span
                style="display: block;"
                class="extra-bottom-text"
              > 颜色</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item
        label="数字样式"
        size="small"
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
                v-model="form['--numfontSize']"
                size="small"
                class="input-number-box-px"
                controls-position="right"
                @change="getChartBasicConfigData"
              />
              <span class="extra-bottom-text"> 字号</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-select
                v-model="form['--numfontWeight']"
                placeholder="请选择"
                size="small"
                @change="getChartBasicConfigData"
              >
                <el-option
                  v-for="item in fontWeightOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <span class="extra-bottom-text"> 文字粗细</span>
            </div>
          </el-col>
        </el-row>
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
                v-model="form['--numcolor']"
                @change="getChartBasicConfigData"
              />
              <span
                style="display: block;"
                class="extra-bottom-text"
              > 颜色</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <DataFormatSelect
        v-model="props.dataType"
        type="value"
        @data-type-change="getChartBasicConfigData"
      />
    </el-form>
  </BKBasicChartConfig>
</template>

<script>
import dataFormatSelect from '../../chart-common-config/components/data-format-select.vue'
import { commonItems, commonRules } from '../../configs/common-item'
import Copy from '../../configs/copy-chart-mixins'
export default {
  name: 'RankChartConfig',
  components: { dataFormatSelect },
  mixins: [Copy],
  inject: ['getSaveAble'],
  inheritAttrs: false,
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    pageConfig: {
      type: Object,
      default: () => {}
    },
    tjbURL: {
      type: String,
      default: ''
    }
  },
  emits: ['setExpand', 'saveChartCfg'],
  data () {
    return {
      items: commonItems.filter(c => c.isBasic),
      rules: commonRules,
      fontWeightOptions: [{ label: 'normal', value: 'normal' }, { label: 'bold', value: 'bold' }, { label: 'bolder', value: 'bolder' }, { label: 'lighter', value: 'lighter' }],
      form: {
        '--progressBgColor': '#f8f8f9',
        '--numcolor': '#424e61',
        '--numfontSize': 12,
        '--numfontWeight': 'bold',
        '--titlecolor': '#000',
        '--titlefontSize': 12,
        '--titlefontWeight': 'normal'
      },
      props: {
        type: 'yellow',
        dataType: ''
      }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    seriesConfig: {
      handler () {
        this.$nextTick(() => {
          this.$refs.basicChartConfig.saveConfig()
        })
      },
      deep: false
    }
  },
  created () {
  },
  methods: {
    configOptionInit () {
      this.$nextTick(() => {
        Object.assign(this.$data, this.$options.data())
        if (this.configs.configOption) {
          const { props, styles } = this.configs.configOption
          this.props = Object.assign(this.props, props)
          this.form = Object.assign(this.form, styles)
        }
      })
    },
    getChartBasicConfigData () {
      this.$refs.basicChartConfig.saveConfig()
    },
    saveChartCfg (data) {
      data.configOption = { props: this.props, styles: this.form }
      this.$emit('saveChartCfg', data)
    }
  }
}
</script>

<style lang="scss" scoped>
  // @import "../../styles/index.css";
</style>
