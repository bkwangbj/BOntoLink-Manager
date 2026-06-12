<template>
  <EfModal
    v-model="modalVisible"
    width="auto"
    height="auto"
    :title="title"
    @close="$emit('close')"
  >
    <AnalysisMaker
      ref="analysisMaker"
      v-bind="$attrs"
      :set-mode="false"
      :page-config="configs.config"
      class="analysis-page"
      :is-modal="true"
      :custom-chart="customChart"
      :map-source="mapSource"
      :tjb-u-r-l="tjbURL"
      :map-path="mapPath"
      :is-basic-mode="isBasicMode"
      :pre-theme-list="preThemeList"
      :default-params="configs.varConfig || []"
    />
  </EfModal>
</template>
<script>
import { getInitValue } from '../../../configs/common-func'
export default {
  name: 'OpenPageModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    configs: {
      type: Object,
      default: () => { return { config: {}, openPage: {}, varConfig: [] } }
    },
    customChart: {
      type: Array,
      default: () => []
    },
    mapSource: {
      type: Array,
      default: () => []
    },
    mapPath: {
      type: String,
      default: '/map/'
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    tjbURL: {
      type: String,
      default: ''
    },
    preThemeList: {
      type: Array,
      default: () => []
    }
  },
  emits: ['close'],
  data () {
    return {
      modalVisible: false
    }
  },
  computed: {
    title () {
      if (this.configs.openPage.name) {
        // const regex = /\$([^$]+)\$/
        return this.configs.openPage.name.replace(/\${(.*?)}/g, (match, p1) => { // ${参数名称}
          const params = this.configs.varConfig.find(c => c.name === p1)
          let str = match
          if (params) {
            str = params.value || getInitValue(params.initValue)
          }
          return str || ''
        })
      }
      return ''
    }
  },
  watch: {
    visible:
    {
      handler () {
        this.$nextTick(() => {
          this.modalVisible = this.visible
          if (this.modalVisible) {
            this.$nextTick(() => {
              this.init()
            })
          }
        })
      },
      immediate: true
    }
  },
  mounted () {

  },
  methods: {
    init () {
      if (this.configs.varConfig) {
        // this.$refs.analysisMaker.setParams(this.configs.varConfig)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.analysis-page {
  width: 100%;
  height: 100%;
}
</style>
