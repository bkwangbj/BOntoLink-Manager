<template>
  <component
    :is="componentName"
    :ref="'chart' + configs.id"
    :configs="configs"
    :set-mode="setMode"
  />
</template>
<script>
import ChartContent from './components/chart-content.vue'
import SubContent from './components/sub-content.vue'
export default {
  name: 'PageLayout',
  components: {
    SubContent,
    ChartContent
  },
  inheritAttrs: false,
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    setMode: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      componentName: 'ChartContent'

    }
  },
  computed: {

  },
  created () {
    this.componentName = (this.configs.children && this.configs.children.length) ? 'SubContent' : 'ChartContent'
  },
  methods: {

  }
}
</script>
<style lang="scss" scoped>
  :deep() {

    .ef-flex-box-cell-container {
      flex-shrink: 0;
    }

    .ef-flex-box-container {
      overflow: auto;
    }
  }
</style>
