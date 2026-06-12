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
    <AmRankList
      v-bind="props"
      :style="styles"
      :data=" dataList"
      :max="max"
      @item-click="itemClick"
    />
  </div>
</template>
<script>
import { mixins } from '../../configs/commom-chart'
import AmRankList from '../am-rank-list.vue'
import { getDataTypeFormat } from '../../configs/common-func.js'
import { utils } from 'efficient-suite'
// import { colorList } from '../../configs/chart-cfg'
export default {
  name: 'RankChart',
  components: {
    AmRankList
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
      styles:
  {
    '--progressBgColor': '#f8f8f9',
    '--numcolor': '#424e61',
    '--numfontSize': 12,
    '--numfontWeight': 'bold',
    '--titlecolor': '#000',
    '--titlefontSize': 12,
    '--titlefontWeight': 'normal'
  }
    }
  },
  computed: {
    max () {
      return Math.round(Math.max(...this.relList.map(r => r.number)) * 1.1) || 100
    },
    dataList () {
      if (this.props?.dataType) {
        const format = getDataTypeFormat(this.props?.dataType)
        return this.relList.map(ele => { return { name: ele.name, number: format(ele.number) } })
      }
      return this.relList
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
        const { props, styles } = utils.deepClone(config.configOption)
        this.props = props
        this.styles = styles
      }
    }
  }
}
</script>
<style lang="scss">
/* stylelint-disable custom-property-pattern */
@font-face {
  font-family: "D DIN";
  src: url("/fonts/D-DIN.ttf");
}

@font-face {
  font-family: "D DIN Bold";
  src: url("/fonts/D-DIN-Bold.ttf");
}

.rank-list-container {

  .ff-nf {
    font-family: "D DIN Bold", sans-serif;
  }

  .ff-wf {
    font-family: "D DIN", sans-serif;
  }

  .rank-item {

    .title {
      font-size: calc(var(--titlefontSize) * 1px);
      font-weight: var(--titlefontWeight);
      color: var(--titlecolor);
    }

    .num {
      font-size: calc(var(--numfontSize) * 1px);
      font-weight: var(--numfontWeight);
      color: var(--numcolor);
    }

    .el-slider__runway {
      background: var(--progressBgColor) !important;
    }
  }
}
</style>
