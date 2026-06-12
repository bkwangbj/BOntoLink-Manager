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
    <BKChart
      ref="chart"
      :option="option"
      @click="itemClick"
    />
    <a
      ref="downLoadPic"
      href="#"
      style="display: none;"
    />
  </div>
</template>
<script>
// import originList from '../../map-chart-config/src/config/default-data'
import { mixins } from '../../configs/commom-chart'
import { getDataTypeFormat } from '../../configs/common-func'
import { utils } from 'efficient-suite'
export default {
  name: 'GaugeChart',
  components: {

  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {

  },
  data () {
    return {
      option: {},
      defaultData: []
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
    itemClick ({ data }) {
      this.handleEvent(data, 'click')
    },
    customResetChart (config) {
      this.$nextTick(() => {
        const option = {}

        if (config.configOption) {
          const form = utils.deepClone(config.configOption)
          if (form.radius) {
            form.radius = form.radius + '%'
          } if (form.center) {
            form.center[0] = form.center[0] + '%'
            form.center[1] = form.center[1] + '%'
          }
          if (form.pointer) {
            form.pointer.length = form.pointer.length + '%'
          }
          if (form.axisLine) {
            form.axisLine.lineStyle.color = [[1, form.axisLine.lineStyle.color]]
          }
          if (form.detail) {
            let format = (e) => {
              return e
            }
            if (form.detail?.dataType) {
              format = getDataTypeFormat(form.detail?.dataType)
            }
            form.detail.formatter = function (value) {
              if (value !== 0) {
                return (form.detail.prefix || '') + format(value) + (form.detail.suffix ||
 '')
              } else {
                return (form.detail.prefix || '') + 0 + (form.detail.suffix ||
 '')
              }
            }
          }
          form.data = [this.relList]
          option.series = [form]
        }
        this.setCustomOption(option, config)
        this.option = option
      })
    }
  }
}
</script>
