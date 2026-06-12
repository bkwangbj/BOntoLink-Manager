import XEUtils from 'xe-utils'
import { utils } from 'efficient-suite'
import { ElMessage, ElMessageBox } from 'element-plus'
const config = {
  tableBasic: {
    exist: {
      props: null,
      styles: null,
      filterDataEmpty: null
    },
    ignore: {}

  },
  tableColumns: {
    exist: { columns: null },
    ignore: {}

  },
  polarBasic: {
    exist: { },
    ignore: { series: null }
  },
  polarSeries: { exist: { series: null }, ignore: { } },
  barBasic: {
    exist: { },
    ignore: { series: null, autoSeries: null }
  },
  barSeries: {
    exist: { series: null, autoSeries: null },
    ignore: { }
  },
  pieBasic: {
    exist: { },
    ignore: { series: null }
  },
  pieSeries: {
    exist: { series: null },
    ignore: { }
  },
  timeBasic: {
    exist: { },
    ignore: { }
  },
  rankBasic: {
    exist: { },
    ignore: { }
  },
  gaugeBasic: {
    exist: { },
    ignore: { }
  },
  radareBasic: {
    exist: { },
    ignore: { }
  },
  mapBasic: {
    exist: { },
    ignore: { }
  }
}
export default {
  methods: {
    checkConfig (type) {

    },

    pasteConfig  (type) {
      ElMessageBox.confirm('是否粘贴配置?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // const text = await navigator.clipboard.readText()
          const config = JSON.parse(localStorage.getItem('amCopyData'))
          if (config?.type !== type) {
            ElMessage.error('配置类型不匹配')
            return
          }
          const data = await this.$refs.basicChartConfig.saveData()
          let option = this.configs?.configOption || {}
          option = utils.deepClone(option)
          const finalObj = XEUtils.merge(option, config?.option || {})
          data.configOption = finalObj
          this.$emit('saveChartCfg', data)
          this.$nextTick(() => {
            this.configOptionInit()
          })
        } catch (err) {
          ElMessage.error('配置类型错误')
        }
      }).catch(() => {

      })
    },

    async copyConfig (type) {
      const chartConfig = config[type]
      let option = this.configs?.configOption || {}
      const obj = {}
      option = utils.deepClone(option)
      for (const i in option) {
        if (Object.keys(chartConfig?.exist || {})?.length) {
          if (Object.keys(chartConfig?.exist || {}).includes(i) && (!Object.keys(chartConfig?.ignore || {}).includes(i))) {
            obj[i] = option[i]
          }
        } else {
          if ((!Object.keys(chartConfig?.ignore || {}).includes(i))) {
            obj[i] = option[i]
          }
        }
      }
      const str = JSON.stringify({ type, option: obj })
      try {
        localStorage.setItem('amCopyData', str)
        ElMessage.success('复制成功')
        // await navigator.clipboard.writeText(str)
      } catch (err) {
        console.log(err)
      }
    }
  }

}
