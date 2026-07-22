import { cardRadiusList } from './configs'
import { getStyleText, styleProps } from '../../configs/pre-page-setting'
import { utils } from 'efficient-suite'
import { ElMessageBox } from 'element-plus'
import emitter from '../../configs/emitter'
// import XEUtils from 'xe-utils'
export default {
  methods: {
    publicChangeMethod (key, value) {
      const path = key.split('.')
      let changeData = this.layoutConfig.themeConfigs
      for (let i = 0; i < path.length; i++) {
        if (i === path.length - 1) {
          changeData[path[i]] = value
          break
        }
        changeData = changeData[[path[i]]]
      }
      if (styleProps.indexOf(path[path.length - 1]) !== -1) {
        this.getStyleText(path)
      }
    },
    customScriptBlur () {
      this.createScript()
    },
    customStyleBlur () {
      this.createStyle()
    },
    getStyleText (path) {
      this.styleText = getStyleText(this.layoutConfig.themeConfigs[path[0]])
    },
    setThemeMode (theme) {
      const modeConfig = this.getModeConfig(this.layoutConfig.themeConfigs.preTheme, theme, true)
      const chartStyle = modeConfig.chartStyle
      delete modeConfig.chartStyle
      this.layoutConfig = {
        ...this.layoutConfig,
        themeConfigs:
        { ...utils.deepClone(this.layoutConfig.themeConfigs), ...modeConfig },
        margin: modeConfig.globalCss.margin
      }
      this.layoutConfig.themeConfigs.globalCss.themeMode = theme
      this.layoutConfig.themeConfigs.chartStyle = chartStyle
      if (theme === 'default') {
        emitter.emit('setEditorTheme', 'vs')
      } else {
        emitter.emit('setEditorTheme', 'vs-dark')
      }
      const list = []
      this.$refs.layoutWrapper.classList.forEach(c => {
        if (c.startsWith('am-theme-')) {
          list.push(c)
        }
      })
      for (let i = 0; i < list.length; i++) {
        if (this.$refs.layoutWrapper.classList.contains(list[i])) {
          this.$refs.layoutWrapper.classList.remove(list[i])
        }
      }
      this.$refs.layoutWrapper.classList.add(`am-theme-${theme}`)
      emitter.emit('chartThemeChange')
    },
    changeCardRadius (value) {
      this.layoutConfig.themeConfigs.globalCss.cardRadius = value
      for (let i = 0; i < cardRadiusList.length; i++) {
        if (this.$refs.layoutWrapper.classList.contains(cardRadiusList[i].class)) {
          this.$refs.layoutWrapper.classList.remove(cardRadiusList[i].class)
        }
        if (cardRadiusList[i].value === value) {
          this.$refs.layoutWrapper.classList.add(cardRadiusList[i].class)
        }
      }
    },
    changeSpaceMode (value) {
      let margin = [10, 10]
      switch (value) {
        case 'compact':
          margin = [8, 8]
          break
        case 'normal':
          margin = [16, 16]
          break
        case 'custom':
          {
            const temp = utils.deepClone(this.layoutConfig)
            temp.themeConfigs.globalCss.margin = this.layoutConfig.themeConfigs.globalCss.margin ?? margin
            this.layoutConfig = temp
          }
          return
      }
      this.changeSpace({ margin, type: value })
    },
    changeSpace ({ margin, type }) {
      if (type === 'custom') {
        this.layoutConfig.themeConfigs.globalCss.margin = margin
      }
      const globalCss = { ...this.layoutConfig.themeConfigs.globalCss, spaceMode: type }
      this.layoutConfig = { ...this.layoutConfig, themeConfigs: { ...this.layoutConfig.themeConfigs, globalCss }, margin }
      this.listenerContainerHeightCallBack()
    },
    changePageWidth ({ value, type }) {
      this.layoutConfig.themeConfigs.pageLayout.pageWidthMode = type
      this.layoutConfig.themeConfigs.pageLayout.pageWidth = value
    },
    changePageMinHeight (value) {
      this.layoutConfig.themeConfigs.pageLayout.pageMinHeight = value
    },
    changePageMinWidth (value) {
      this.layoutConfig.themeConfigs.pageLayout.pageMinWidth = value
    },
    changePagePadding (e) {
      const pageLayout = { ...this.layoutConfig.themeConfigs.pageLayout, pagePadding: e }
      this.layoutConfig = { ...this.layoutConfig, themeConfigs: { ...this.layoutConfig.themeConfigs, pageLayout } }
      this.listenerContainerHeightCallBack()
    },
    changeCardPadding (e) {
      const globalCss = { ...this.layoutConfig.themeConfigs.globalCss, cardPadding: e }
      this.layoutConfig = { ...this.layoutConfig, themeConfigs: { ...this.layoutConfig.themeConfigs, globalCss } }
      // emitter.emit('setCardPadding', e)
    },
    innerPaddingRefChange (e) {
      this.layoutConfig = { ...this.layoutConfig, themeConfigs: { ...this.layoutConfig.themeConfigs, innerPaddingConfig: e } }
    },
    changePreTheme (preTheme) {
      const chartStyle = utils.deepClone(preTheme.chartStyle)
      delete preTheme.chartStyle
      // 切换主题时保留用户当前选定的图表配色模版(不重置为新主题内置色系)
      const prevColorList = this.layoutConfig?.themeConfigs?.chartStyle?.themeStyle?.colorList
      if (prevColorList && prevColorList.length && chartStyle?.themeStyle) {
        chartStyle.themeStyle.colorList = utils.deepClone(prevColorList)
      }
      this.layoutConfig = {
        ...this.layoutConfig,
        themeConfigs: { ...utils.deepClone(preTheme), chartStyle },
        margin: preTheme.globalCss.margin
      }
      emitter.emit('chartThemeChange')
    },
    changePageHeightMode (value) {
      const pageLayout = { ...this.layoutConfig.themeConfigs.pageLayout, pageHeightMode: value }
      this.layoutConfig = { ...this.layoutConfig, themeConfigs: { ...this.layoutConfig.themeConfigs, pageLayout } }
      if (value === 'adaptive') {
        this.$nextTick(() => {
          this.calculateLayoutHeight(() => {
            const pageLayout = { ...this.layoutConfig.themeConfigs.pageLayout, pageHeightMode: 'auto' }
            this.layoutConfig = { ...this.layoutConfig, themeConfigs: { ...this.layoutConfig.themeConfigs, pageLayout } }
            this.$refs.pageSetting.init(this.layoutConfig.themeConfigs)
          })
        })
      } else {
        this.layoutConfig = { ...this.layoutConfig, maxRows: Infinity }
        this.decorateConfig.decorateMaxRows = Infinity
      }
    },
    chartStyleChange ({ path, form, field }) {
      const data = utils.deepClone(this.layoutConfig)
      if (path) {
        if (!data.themeConfigs?.chartStyle?.[path]) {
          data.themeConfigs.chartStyle[path] = {}
        }
        data.themeConfigs.chartStyle[path][field] = form
      } else {
        data.themeConfigs.chartStyle[field] = form
      }
      this.layoutConfig = data

      emitter.emit('chartThemeChange')
    },
    calculateLayoutHeight (callback) {
      const maxRows = this.calculateMaxRows()
      const decorateMaxRows = this.calculateDecorateMaxRows()
      const temp = utils.deepClone(this.layoutConfig)
      const decorateLayout = temp.decorateLayout.filter(item => (item.y + item.h) <= decorateMaxRows)
      temp.decorateLayout = decorateLayout
      this.decorateConfig.decorateMaxRows = decorateMaxRows
      if (temp.layout.filter(item => (item.y + item.h) > maxRows).length) {
        ElMessageBox.confirm('本次操作将删除超出页面容器的区域', '提示', { type: 'warning' }).then(() => {
          const layout = temp.layout.filter(item => (item.y + item.h) <= maxRows)
          temp.layout = layout
          temp.maxRows = maxRows
          this.layoutConfig = temp
          this.listenerContainerHeightCallBack()
        }).catch(callback)
      } else {
        const layout = temp.layout.filter(item => (item.y + item.h) <= maxRows)
        temp.layout = layout
        temp.maxRows = maxRows
        this.layoutConfig = temp
        this.listenerContainerHeightCallBack()
      }
    },
    calculateMaxRows (isChange = true) {
      if (!this.$refs.layoutWrapper) {
        return Infinity
      }
      const layoutWrapperHeight = this.$refs.layoutWrapper.clientHeight
      let rowHeight = this.layoutConfig.rowHeight
      if (!isChange) {
        rowHeight = this.layoutConfig.autoRowHeight
      }
      const paddingBottom = this.layoutConfig?.themeConfigs?.pageLayout?.pagePadding?.paddingBottom || 0
      const paddingTop = this.layoutConfig?.themeConfigs?.pageLayout?.pagePadding?.paddingTop || 0
      const maxRows = Math.floor((layoutWrapperHeight - paddingBottom - paddingTop) / (rowHeight + this.layoutConfig.margin[1]))
      return maxRows
    },
    calculateDecorateMaxRows (isChange = true) {
      if (!this.$refs.layoutWrapper) {
        return Infinity
      }
      const layoutWrapperHeight = this.$refs.layoutWrapper.clientHeight
      if (layoutWrapperHeight === 0) {
        return Infinity
      }
      let rowHeight = this.decorateConfig.decorateRowHeight
      if (!isChange) {
        rowHeight = this.decorateConfig.decorateAutoRowHeight
      }
      const paddingBottom = this.layoutConfig?.themeConfigs?.pageLayout?.pagePadding?.paddingBottom || 0
      const paddingTop = this.layoutConfig?.themeConfigs?.pageLayout?.pagePadding?.paddingTop || 0
      const maxRows = Math.floor((layoutWrapperHeight - paddingBottom - paddingTop) / (rowHeight + this.decorateConfig.decorateMargin[1]))
      return maxRows
    }
  }
}
