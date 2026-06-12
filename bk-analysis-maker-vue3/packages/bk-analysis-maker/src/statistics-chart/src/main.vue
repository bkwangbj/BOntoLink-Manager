<template>
  <CustomStatistics
    ref="rootRef"
    :option="option"
    :real-data="relList"
    @item-click="itemClick"
  />
</template>
<script>
import { mixins } from '../../configs/commom-chart'
import { getPositionStyle, getTextStyle } from '../../configs/common-func'
import CustomStatistics from '../custom-statistics/index.vue'
import { utils } from 'efficient-suite'
export default {
  name: 'StatisticsChart',
  components: {
    CustomStatistics
  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {

  },
  data () {
    return {
      defaultData: [],
      option: {
        image: {},
        number: {},
        text: {},
        styles: {}
      }
    }
  },
  computed: {

  },
  watch: {
    relList () {
      this.debouncedCustomResetChart(this.configs)
    }
  },
  created () {
  },
  mounted () {

  },
  methods: {
    itemClick (itemData) {
      this.handleEvent(itemData, 'click')
    },
    customResetChart (config) {
      this.$nextTick(() => {
        if (this.configs?.configOption) {
          const form = { styles: {} }
          const option = utils.deepClone(this.configs?.configOption)
          for (const i in option) {
            if (option[i]?.position) {
              const position = getPositionStyle(option[i]?.position)
              if (i === 'unit') {
                for (const i in position) {
                  position[i + 'unit'] = position[i]
                }
              }
              if (form[i]) {
                form[i] = {
                  ...form[i],
                  ...position
                }
              } else {
                form[i] = { ...position }
              }
            }
            if (option[i]?.textStyle) {
              const text = getTextStyle(option[i]?.textStyle)
              if (form[i]) {
                form[i] = {
                  ...form[i],
                  ...text
                }
              } else {
                form[i] = { ...text }
              }
            }
            if (option[i]?.width || option[i]?.height) {
              if (form[i]) {
                form[i] = {
                  ...form[i],

                  '--width': option[i]?.width || '',
                  '--height': option[i]?.height || ''

                }
              } else {
                form[i] = {

                  '--width': option[i]?.width || '',
                  '--height': option[i]?.height || ''

                }
              }
            }
          }
          if (option.isPieces) {
            form.pieces = option.pieces
          } else {
            form.pieces = []
          }
          form.styles = option.styles
          form.imageShow = option.image.show
          form.unitShow = option.unit.show
          form.unitPosi = option.unit.posi
          for (const i in form.styles) {
            if (['borderRadius', 'borderWidth'].includes(i) && form.styles[i]) {
              form.styles[i] = form.styles[i] + 'px'
            }
          }
          if (option.widthType === 'adaptive') {
            const width = 'calc( (100% - ' + (option.styles.gap || 0) * (option.widthNum - 1) + 'px ) / ' + option.widthNum + ')'
            form.styles.width = width
          }
          if (option.heightType === 'adaptive') {
            const height = 'calc( (100% - ' + (option.styles.gap || 0) * (option.heightNum - 1) + 'px ) / ' + option.heightNum + ')'
            form.styles.height = height
          }
          this.option = form
        }
      })
    }
  }
}
</script>
<style lang="scss" scoped>
/* stylelint-disable custom-property-pattern */
:deep(.bk-statistics-wrapper) {

  .statistics-text {
    color: var(--textColor);
  }

  .statistics-number {
    color: var(--numColor);
  }
}

</style>
