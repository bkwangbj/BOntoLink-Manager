<template>
  <BKBasicChartConfig
    ref="basicChartConfig"
    :configs="configs"
    :page-config="pageConfig"
    :save-able="saveAble"
    :rules="rules"
    :tjb-u-r-l="tjbURL"
    v-bind="$attrs"
    @set-expand="$emit('setExpand')"
    @config-option-init="configOptionInit"
    @save-chart-config="saveChartCfg"
  >
    <div style="height: 100%;padding: 10px;overflow: auto;">
      <div
        class="copy-button-box"
      >
        <el-button
          type="primary"
          size="small"
          @click="copyConfig('gaugeBasic')"
        >
          复制基础配置
        </el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="!saveAble"
          @click="pasteConfig('gaugeBasic')"
        >
          粘贴基础配置
        </el-button>
      </div>
      <basicConfig
        ref="basicConfigRef"
        @chart-change="getChartBasicConfigData"
      />
    </div>
  </BKBasicChartConfig>
</template>

<script>
import { commonItems, commonRules } from '../../configs/common-item'
import basicConfig from './components/basic-config.vue'
import Copy from '../../configs/copy-chart-mixins'
export default {
  name: 'GaugeChartConfig',
  components: {
    basicConfig
  },
  mixins: [Copy],
  provide () {
    return {
      getThemeFont: () => this.configs?.chartTheme?.textColor || '#000'
    }
  },
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
      rules: commonRules
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  created () {
  },
  methods: {
    configOptionInit () {
      this.$nextTick(() => {
        if (this.$refs.basicConfigRef) {
          this.$refs.basicConfigRef.setFormData(this.configs.configOption)
        }
      })
    },
    getChartBasicConfigData () {
      if (this.$refs.basicChartConfig) {
        this.$refs.basicChartConfig.saveConfig()
      }
    },
    saveChartCfg (data) {
      const formData = this.$refs.basicConfigRef.saveFormData()
      data.configOption = formData
      this.$emit('saveChartCfg', data)
    }
  }
}
</script>

<style lang="scss" scoped>
  // @import "../../styles/index.css";
</style>
