<template>
  <span
    v-if="legendConfig.show"
    class="legend-cotent"
    :style="`--x: ${legendConfig.x};--y: ${legendConfig.y};${style}`"
  >
    <EfChartLegend
      class="legend-viewer"
      :headers="headers"
      :colors="colors"
      :data="data"
      :has-headers="hasHeaders"
      :classes="classes"
    />

  </span>
</template>

<script>
import { getCollapseRule, getDataTypeFormat } from '../../../configs/common-func'
import { utils } from 'efficient-suite'
export default {
  name: 'LegendContent',

  props: {
    relList: {
      type: [Array, Object],
      default: () => []
    }
  },
  data () {
    return {
      position: '',
      legendConfig: {
        show: false,
        unit: '',
        unitPosition: 'onHeader',
        position: 'topLeft',
        x: 0,
        y: 0
      },
      hasHeaders: true,
      headers: ['', '', ''],
      classes: ['t-l-r', 'bold-text t-l-r', 't-l-r'],
      colors: ['#008FFF', '#3ED848', '#FFC72F', '#FF9B58'],
      data: [

      ]
    }
  },
  computed: {
    style () {
      let string = ''
      const alignList = {
        topLeft: 'left:10px;top:10px;',
        topCenter: 'left: 50%; transform: translateX(-50%);top:10px;',
        topRight: 'right:10px;top:10px;',
        bottomLeft: 'left:10px;bottom:10px;',
        bottomCenter: 'left: 50%; transform: translateX(-50%);bottom:10px;',
        bottomRight: 'right:10px;bottom:10px;'
      }

      if (this.legendConfig?.position) {
        string = alignList[this.legendConfig.position]
      }
      return string
    }
  },
  methods: {
    buildLegend (config) {
      if (!config?.configOption || (!Array.isArray(this.relList))) {
        this.legendConfig.show = false
        return
      }
      Object.assign(this.$data, this.$options.data())
      if (config.configOption?.legendConfig) {
        this.legendConfig = { ...config.configOption.legendConfig }
      }
      if (this.legendConfig?.header?.length) {
        this.headers[0] = this.legendConfig?.header[0]
        this.headers[1] = this.legendConfig?.header[1]
        if (this.legendConfig.showPerc) {
          this.headers[2] = this.legendConfig?.header[2]
        }
      }
      const option = utils.deepClone(config.configOption)
      let colorList = []
      if (option?.color) {
        colorList = option.color
      }
      // 去重
      const { newArr, kind } = this.classify(this.relList, 'name')
      let list = []
      for (let i = 0; i < newArr.length; i++) {
        let num = 0
        for (let j = 0; j < newArr[i].length; j++) {
          if (newArr[i][j].value) {
            let value = newArr[i][j].value
            if (isNaN(value)) {
              try {
                value = Number(value.replace(/,/g, ''))
              } catch (error) {

              }
            }
            num += value
          }
          list.push({ name: kind[i], value: num })
        }
      }
      const total = list.reduce(function (pre, cur) {
        return Number(pre) + Number(cur.value)
      }, 0)
      if (config.branchType === 'multiplePieChart') {
        // 排序
        const orderList = []
        for (let i = 0; i < option.series.length; i++) {
          let yData = this.relList.filter(item => item.colorField === option.series[i].dataId)
          yData = yData.map(item => { return { name: item.name, value: item.value } })
          if (option.series[i]?.hasChildren) {
            // 子系列逻辑
            const ruleList = getCollapseRule(option.series[i]?.collapseRule || '')
            const childrenData = yData.filter((ele, index) => {
              index = index + 1
              // eslint-disable-next-line no-eval
              return eval(ruleList)
            })
            const fatherData = yData.filter((ele, index) => {
              index = index + 1
              // eslint-disable-next-line no-eval
              return !eval(ruleList)
            })
            fatherData.forEach(ele => {
              if (orderList.findIndex(el => { return el.name === ele.name }) === -1) {
                orderList.push(ele)
              }
            })
            childrenData.forEach(ele => {
              if (orderList.findIndex(el => { return el.name === ele.name }) === -1) {
                orderList.push(ele)
              }
            })
          } else {
            yData.forEach(ele => {
              if (orderList.findIndex(el => { return el.name === ele.name }) === -1) {
                orderList.push(ele)
              }
            })
          }
        }
        // 数据数组和排序数组合并
        list = orderList.map((ele, index) => {
          const item = list.find(el => { return el.name === ele.name })
          return { ...ele, value: item.value }
        })
      }
      const format = getDataTypeFormat(this.legendConfig.dataType)
      const percNumber = this.legendConfig?.percNumber !== undefined ? this.legendConfig?.percNumber : 2
      this.data = list.map((ele, index) => {
        if (index >= colorList.length) {
          colorList.push(colorList[colorList.length % index])
        }
        if (this.legendConfig.unitPosition === 'onData') {
          if (this.legendConfig.showPerc) {
            return [ele.name, format(ele.value) + this.legendConfig?.unit || '', ((ele.value / total) * 100).toFixed(percNumber) + '%']
          } else {
            return [ele.name, format(ele.value) + this.legendConfig?.unit || '']
          }
        } else {
          if (this.legendConfig.showPerc) {
            return [ele.name, format(ele.value), ((ele.value / total) * 100).toFixed(percNumber) + '%']
          } else {
            return [ele.name, format(ele.value)]
          }
        }
      })
      this.colors = colorList

      if (this.legendConfig.unitPosition === 'onHeader') {
        this.headers[1] = this.headers[1] + this.legendConfig?.unit
      }
    },
    classify (arr, key) {
      const kind = []
      const newArr = [] // 返回的数据
      arr.forEach((item) => {
        // 判断key是否存在，不存在则添加
        if (!kind.includes(item[key])) {
          kind.push(item[key]) // kind添加新标识
          newArr.push([]) // 添加数组
        }
        const index = kind.indexOf(item[key]) // 返回带有标识在kind内的下标，判断加入哪个数组
        newArr[index].push(item) // 将对象存入数组
      })
      return { newArr, kind }
    }
  }
}
</script>

<style lang="scss" scoped>
.legend-cotent {
  position: absolute;

  :deep() {

    .ef-legend-wrapper {
      position: relative;
      top: calc(var(--y) * 1px);
      left: calc(var(--x) * 1px);
    }

    .t-l-r {
      text-align: right;
    }
  }
}

</style>
