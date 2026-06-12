/* stylelint-disable custom-property-pattern */

<template>
  <div
    ref="rootRef"
    class="full-box"
    style="position: relative;"
  >
    <slot name="content">
      <component
        :is="contentConfig.component"
        v-if="contentConfig && contentConfig.component"
        v-bind="contentConfig.props || {}"
      />
    </slot>
    <TimeSilder
      :style="styles"
      :marks="relList"
      :options="props"
      @item-click="itemClick"
    />
  </div>
</template>
<script>
import { mixins } from '../../configs/commom-chart'
import TimeSilder from '../time-silder.vue'
import { utils } from 'efficient-suite'
// import { colorList } from '../../configs/chart-cfg'
export default {
  name: 'TimeChart',
  components: {
    TimeSilder
  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {

  },

  data () {
    return {
      option: {},
      defaultData: [],
      finalList: [],
      props: {
        type: 'yellow'
      },
      styles: {
      }
    }
  },
  watch: {
    relList () {
      this.debouncedCustomResetChart(this.configs)
    }
  },
  created () {
  },
  methods: {
    itemClick (data) {
      this.handleEvent(data, 'click')
    },
    customResetChart (config) {
      if (config.configOption) {
        const { spotConfig, upTextConfig, downTextConfig, sliderConfig, basicConfig, isVertical } = utils.deepClone(config.configOption)
        const styles = { upText: upTextConfig, downText: downTextConfig, slider: sliderConfig, spot: spotConfig.styles }
        const form = {}
        this.flatStyles(styles, '', form)
        this.styles = form
        this.props = { ...spotConfig.config, ...basicConfig, sliderLength: sliderConfig.length, downTextShow: downTextConfig.select.show, upTextShow: upTextConfig.select.show, isVertical }
      }
    },
    flatStyles (styles, name = '', form) {
      for (const i in styles) {
        if (typeof styles[i] === 'object' && !(styles[i] instanceof Array)) {
          this.flatStyles(styles[i], name + i, form)
        } else {
          form['--' + name + i] = styles[i]
        }
      }
    }
  }
}
</script>
<style lang="scss" scoped>
/* stylelint-disable custom-property-pattern */

</style>
