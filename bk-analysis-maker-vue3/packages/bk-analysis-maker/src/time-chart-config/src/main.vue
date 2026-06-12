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
    <el-collapse style=" height: 100%;padding: 10px 10px 0;overflow: auto;">
      <div
        class="copy-button-box"
      >
        <el-button
          type="primary"
          size="small"
          @click="copyConfig('timeBasic')"
        >
          复制基础配置
        </el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="!saveAble"
          @click="pasteConfig('timeBasic')"
        >
          粘贴基础配置
        </el-button>
      </div>
      <!-- <el-collapse-item>
        <template slot="title">
          <div class="collapse-title-common">
            基础配置
          </div>
        </template>
        <div class="d-flex-c plane-inner-bg ">
          <BasicConfig
            :form-data="option.basic"
            ref="basicRef"
            @chartChange="getChartBasicConfigData"
          />
        </div>
      </el-collapse-item>-->
      <div class="d-flex">
        <div class="collapse-title-common ">
          布局
        </div>
        <el-radio-group
          v-model="isVertical"
          size="small"
          @change="getChartBasicConfigData"
        >
          <el-radio-button :value="false">
            水平
          </el-radio-button>
          <el-radio-button :value="true">
            垂直
          </el-radio-button>
        </el-radio-group>
      </div>
      <el-collapse-item>
        <template #title>
          <div class="collapse-title-common ">
            事件节点
          </div>
        </template>
        <div class="d-flex-c plane-inner-bg ">
          <EventsSpotConfig
            ref="spotRef"
            :form-data="option.spotConfig"
            @chart-change="getChartBasicConfigData"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item>
        <template #title>
          <div class="collapse-title-common ">
            进度条配置
          </div>
        </template>
        <div class="d-flex-c plane-inner-bg ">
          <SliderConfig
            ref="sliderRef"
            :form-data="option.sliderConfig"
            @chart-change="getChartBasicConfigData"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item>
        <template #title>
          <div class="collapse-title-common ">
            节点标签-上
          </div>
        </template>
        <div class="d-flex-c plane-inner-bg ">
          <SpotTextConfig
            ref="upTextRef"
            :form-data="option.upTextConfig"
            @chart-change="getChartBasicConfigData"
          />
        </div>
      </el-collapse-item>
      <el-collapse-item>
        <template #title>
          <div class="collapse-title-common ">
            节点标签-下
          </div>
        </template>
        <div class="d-flex-c plane-inner-bg ">
          <SpotTextConfig
            ref="downTextRef"
            :form-data="option.downTextConfig"
            @chart-change="getChartBasicConfigData"
          />
        </div>
      </el-collapse-item>
    </el-collapse>
  </BKBasicChartConfig>
</template>

<script>
import { commonItems, commonRules } from '../../configs/common-item'
import EventsSpotConfig from '../components/events-spot-config.vue'
import SliderConfig from '../components/slider-config.vue'
import SpotTextConfig from '../components/spot-text-config.vue'
import Copy from '../../configs/copy-chart-mixins'
import { utils } from 'efficient-suite'
export default {
  name: 'TimeChartConfig',
  components: { EventsSpotConfig, SliderConfig, SpotTextConfig },
  mixins: [Copy],
  inject: ['getSaveAble'],
  provide () {
    return {
      getThemeFont: () => this.configs?.chartTheme?.textColor || '#000'
    }
  },
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
      option: {},
      isVertical: false,
      props: {
        type: 'category',
        dataType: ''
      }
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
      if (this.configs.configOption) {
        this.option = utils.deepClone(this.configs.configOption)
        this.isVertical = this.option.isVertical
      }
    },
    saveChartCfg (data) {
      const spotConfig = this.$refs.spotRef.saveFormData()
      const upTextConfig = this.$refs.upTextRef.saveFormData()
      const downTextConfig = this.$refs.downTextRef.saveFormData()
      const sliderConfig = this.$refs.sliderRef.saveFormData()
      //   const basicConfig = this.$refs.basicRef.saveFormData()
      data.configOption = { spotConfig, sliderConfig, upTextConfig, downTextConfig, isVertical: this.isVertical }
      this.$emit('saveChartCfg', data)
    },
    getChartBasicConfigData () {
      if (this.$refs.basicChartConfig) {
        this.$refs.basicChartConfig.saveConfig()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  // @import "../../styles/index.css";
</style>
