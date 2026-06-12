import { debounce } from 'throttle-debounce'
import { getCalState, getDefaultData, getInitValue, clearTimer } from './common-func'
import { componentConfigs } from '../configs'
import emitter from './emitter'
import { componentRegistry } from '../utils/componentRegistry'
import { hookManager } from '../utils/hookManager'
export const mixins = {
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    setMode: {
      type: Boolean,
      default: false
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
  watch: {
    configs () {
      this.buildParams(true)
    },
    showClear () {
      this.$emit('showClear', this.showClear, this.configs.chartId)
    },
    'configs.hookConfig': {
      handler () {
        if (this.configs.hookConfig && this.configs.hookConfig.show && this.configs.hookConfig.script) {
          this.setupHooks(this.configs.hookConfig.script)
        } else {
          this.setupHooks('')
        }
      },
      deep: true,
      immediate: true
    },
    'configs.hookId': {
      handler (val, beforeVal) {
        if (val) {
          componentRegistry.register(this.configs.hookId, this.publicAPI, this.$refs.rootRef, this.setMode)
        }
        if (beforeVal) {
          componentRegistry.unregister(beforeVal)
        }
      },
      deep: true,
      immediate: true
    },
    relList: {
      handler (newData) {
        // 触发数据变化钩子
        hookManager.triggerHook('onDataChange', this, newData)
        this.$nextTick(() => {
          // 触发更新钩子
          hookManager.triggerHook('onUpdate', this)
        })
      },
      deep: true,
      immediate: true
    }
  },
  inject: ['setParams'],
  data () {
    return {
      list: [],
      params: [],
      changeParams: [],
      contentConfig: {}
    }
  },
  provide () {
    return {
      componentAPI: this.publicAPI
    }
  },
  emits: ['relListChange', 'showClear'],
  computed: {
    showClear () {
      return this.changeParams.length > 0
    },
    relList () {
      if (this.configs.dataSourceConfig && this.configs.dataSourceConfig.dataMapping) {
        const res = []
        if (Array.isArray(this.list)) {
          for (let i = 0; i < this.list.length; i++) {
            res.push(this.buildData(this.list[i]))
          }
          this.$emit('relListChange', res)
          return res
        } else {
          const data = this.buildData(this.list)
          this.$emit('relListChange', data)
          return data
        }
      }
      this.$emit('relListChange', this.list)
      return this.list
    },
    publicAPI () {
      return {
        // DOM操作
        $root: () => this.$refs.rootRef,
        show: this.show,
        hide: this.hide,
        setStyle: this.setStyle,
        addClass: this.addClass,
        setContentConfig: this.setContentConfig,
        // 数据操作
        setData: this.setData,
        getData: this.getData,

        // 事件触发
        emit: this.emitEvent
      }
    }
  },
  methods: {
    buildData (item) {
      const dataMapping = this.configs.dataSourceConfig.dataMapping
      const fields = this.configs.items || []
      const mappingObject = {}
      for (let i = 0; i < fields.length; i++) {
        const field = fields[i].field
        let value = item[dataMapping[field] || field]
        if (dataMapping[`${field}_filter`]) {
          value = this.executeScript(dataMapping[`${field}_filter`], value)
        }
        mappingObject[field] = value
      }
      return {
        ...item,
        ...mappingObject
      }
    },
    executeScript (code, data, params, isParams = false) {
      if (isParams) {
        const func = new Function('params', code)
        const value = func(data)
        return value
      }
      const func = new Function('data', 'params', code)
      const value = func(data, params)
      return value
    },
    setCustomOption (option, config) {
      if (config && config.configOption && config.configOption.customConfig && config.configOption.customConfig.show &&
        config.configOption.customConfig.config && config.configOption.customConfig.config.length > 0) {
        const func = new Function('option', 'config', 'data', 'params', config.configOption.customConfig.config)
        func(option, config, this.relList, this.params)
      }
    },
    handleEvent (itemData, eventName, columnField) {
      if (this.configs.eventConfig) {
        const clickEvent = this.configs.eventConfig.find(c => c.event === eventName && c.isActive)
        if (clickEvent) {
          let items = clickEvent.items
          if (columnField) {
            items = items.filter(c => c.columnField === columnField || c.field === columnField)
          }
          for (let i = 0; i < items.length; i++) {
            const item = items[i]
            const field = item.field
            // if (this.configs.dataSourceConfig && this.configs.dataSourceConfig.dataMapping) {
            //   const dataMapping = this.configs.dataSourceConfig.dataMapping
            //   field = dataMapping[field] || field
            // }
            if (item.varField && itemData && itemData[field] !== undefined) {
              const param = { id: item.varField, value: itemData[field] }
              this.setParams(param)
              // emitter.emit('setParams', param)
              if (this.pageConfig.varConfig) {
                const varConfig = this.pageConfig.varConfig.find(c => c.id === item.varField)
                if (varConfig && varConfig.changeType === 'refreshData' && !this.changeParams.find(c => c.id === item.varField)) {
                  this.changeParams.push(param)
                }
              }
            }
          }
        }
      }
    },
    clearParams () {
      const paramList = []
      for (let i = 0; i < this.changeParams.length; i++) {
        const param = { id: this.changeParams[i].id, value: '' }
        const varCfg = this.pageConfig.varConfig.find(c => c.id === this.changeParams[i].id)
        if (varCfg && varCfg.initValue) {
          param.value = getInitValue(varCfg.initValue)
        }
        paramList.push(param)
      }
      if (paramList.length > 0) {
        this.setParams(paramList)
        // emitter.emit('setParams', paramList)
      }
      if (this.configs.type === 'BKMapChart') {
        this.resetMap()
      }
      this.changeParams = []
    },
    paramsChange ({ params, pageConfig }) {
      if (pageConfig && this.pageConfig.id !== pageConfig.id) {
        return
      }
      let refresh = false
      for (let i = 0; i < params.length; i++) {
        const param = params[i]
        const p = this.params.find(c => c.id === param.id)
        if (p) {
          refresh = true
          p.value = param.value || getInitValue(param.initValue)
        }
      }
      if (refresh) {
        this.debouncedRefreshData()
      }
    },
    async setChartData (refresh) {
      if (!refresh && this.configs.type === 'BKTableChart' && this.configs.configOption?.props?.hasPager && !this.configs.configOption?.props?.isStatic) {
        this.pageParam = this.$options.data().pageParam
        if (this.$refs.table && this.$refs.table.$refs.packingTarget && this.$refs.table.$refs.packingTarget.pagerConfig) {
          this.$refs.table.$refs.packingTarget.pagerConfig.currentPage = 1
        }
      }
      let data = this.defaultData
      for (let i = 0; i < this.params.length; i++) {
        if (!this.params[i].value && this.params[i].isRequired === '1') {
          if (Array.isArray(data)) {
            this.list = []
          } else {
            this.list = {}
          }
          return
        }
      }
      const dataSourceConfig = this.configs.dataSourceConfig
      if (dataSourceConfig) {
        if (dataSourceConfig.type === 'static') {
          data = dataSourceConfig.data || this.defaultData
          data = this.filterStaticData(data)
        } else if (dataSourceConfig.type === 'post' || dataSourceConfig.type === 'get') {
          let params = {}
          for (let i = 0; i < this.params.length; i++) {
            params[this.params[i].alias] = this.params[i].value
          }
          params = this.otherParams(params)
          if (dataSourceConfig.paramHandlerVisible && dataSourceConfig.paramHandler) {
            params = this.executeScript(dataSourceConfig.paramHandler, params, undefined, true)
          }
          let res
          if (dataSourceConfig.interfacePath && dataSourceConfig.interfacePath.startsWith('http')) {
            res = await componentConfigs.request[dataSourceConfig.type === 'post' ? 'fetchPost' : 'fetchGet'](dataSourceConfig.interfacePath, params, dataSourceConfig.paramsType !== 'form')
          } else {
            res = await componentConfigs.request[dataSourceConfig.type](dataSourceConfig.interfacePath, params, dataSourceConfig.paramsType !== 'form')
          }
          this.handleData(res)
          if (dataSourceConfig.interfaceFilterVisible && dataSourceConfig.interfaceFilter) {
            res = this.executeScript(dataSourceConfig.interfaceFilter, res, params)
          }
          data = res
        } else if (dataSourceConfig.type === 'fetch-post' || dataSourceConfig.type === 'fetch-get' || dataSourceConfig.type === 'fetch-post-form') {
          let params = {}
          for (let i = 0; i < this.params.length; i++) {
            params[this.params[i].alias] = this.params[i].value
          }
          params = this.otherParams(params)
          if (dataSourceConfig.paramHandlerVisible && dataSourceConfig.paramHandler) {
            params = this.executeScript(dataSourceConfig.paramHandler, params, undefined, true)
          }
          const fetchParam = { method: dataSourceConfig.type === 'fetch-get' ? 'GET' : 'POST' }
          if (dataSourceConfig.type !== 'fetch-get') {
            const useJson = dataSourceConfig.type === 'fetch-post-form'
            const fd = new FormData()
            if (params && useJson) {
              for (const key in params) {
                if (Object.hasOwnProperty.call(params, key)) {
                  const value = params[key]
                  if (typeof value === 'object') {
                    fd.append(key, JSON.stringify(value))
                  } else {
                    fd.append(key, value)
                  }
                }
              }
            }
            fetchParam.body = useJson ? fd : JSON.stringify(params)
            if (!useJson) {
              fetchParam.headers = {
                'Content-Type': dataSourceConfig.type === 'fetch-post' ? 'application/json' : 'application/x-www-form-urlencoded'
              }
            }
          }
          const queryString = new URLSearchParams(params).toString()
          let res = await fetch(`${dataSourceConfig.interfacePath}${dataSourceConfig.type === 'fetch-get' ? '?' + queryString : ''}`, fetchParam)
          res = await res.json()
          this.handleData(res)
          if (dataSourceConfig.interfaceFilterVisible && dataSourceConfig.interfaceFilter) {
            res = this.executeScript(dataSourceConfig.interfaceFilter, res, params)
          }
          data = res
        } else if (dataSourceConfig.type === 'watf') {
          let params = []
          if (dataSourceConfig.interfaceTempParamsVisible) {
            for (let i = 0; i < this.params.length; i++) {
              if (this.params[i].value !== undefined) {
                params.push({ field: this.params[i].alias, value: this.params[i].value, fieldValueRel: this.params[i].operator })
              }
            }
            let watfParams = {}
            if (dataSourceConfig.interfaceTempParams) {
              watfParams = JSON.parse(dataSourceConfig.interfaceTempParams)
            }
            if (!watfParams.methodParams) {
              watfParams.methodParams = { simpleQueryParam: [] }
            }
            if (!watfParams.methodParams.simpleQueryParam) {
              watfParams.methodParams.simpleQueryParam = []
            }
            watfParams.methodParams.simpleQueryParam = [...watfParams.methodParams.simpleQueryParam, ...params]
            params = watfParams
          }
          // else {
          //   for (let i = 0; i < this.params.length; i++) {
          //     params[this.params[i].alias] = this.params[i].value
          //   }
          // }
          params = this.otherParams(params)
          if (dataSourceConfig.paramHandlerVisible && dataSourceConfig.paramHandler) {
            params = this.executeScript(dataSourceConfig.paramHandler, params, undefined, true)
          }
          let res = await componentConfigs.request.singleTableOperation(params)
          this.handleData(res)
          if (dataSourceConfig.interfaceFilterVisible && dataSourceConfig.interfaceFilter) {
            res = this.executeScript(dataSourceConfig.interfaceFilter, res, params)
          }
          data = res
        } else if (dataSourceConfig.type === 'tjb') {
          if ((dataSourceConfig.interfacePath || this.tjbURL) && this.configs.dataSourceConfig.tjbConfig && this.configs.dataSourceConfig.tjbConfig.table) {
            let params = {}
            for (let j = 0; j < this.params.length; j++) {
              params[this.params[j].alias] = this.params[j].value
            }
            params = this.otherParams(params)
            params = {
              ...params,
              tableid: this.configs.dataSourceConfig.tjbConfig.table.id,
              rowindexs: this.configs.dataSourceConfig.tjbConfig.config.rowindexs,
              colindexs: this.configs.dataSourceConfig.tjbConfig.config.colindexs
            }
            if (dataSourceConfig.paramHandlerVisible && dataSourceConfig.paramHandler) {
              params = this.executeScript(dataSourceConfig.paramHandler, params, undefined, true)
            }
            let res = await componentConfigs.request.post(dataSourceConfig.interfacePath || this.tjbURL, params)
            if (res && res.needcal) {
              const calFinished = await getCalState(this.configs.dataSourceConfig.tjbConfig.table.id, res.calid, this.configs.chartId)
              if (calFinished) {
                this.setChartData(refresh)
              }
              return
            }
            this.handleData(res)
            res = res.data || res
            if (dataSourceConfig.interfaceFilterVisible && dataSourceConfig.interfaceFilter) {
              res = this.executeScript(dataSourceConfig.interfaceFilter, res, params)
            }
            data = res
          }
        }
      } else {
        data = this.defaultData
        data = this.filterStaticData(data)
      }
      this.list = data
    },
    otherParams (params) {
      if (this.configs.type === 'BKTableChart' && this.configs.configOption?.props?.hasPager && !this.configs.configOption?.props?.isStatic) {
        params = { ...params, ...this.pageParam }
      }
      return params
    },
    handleData (res) {
      const data = res?.data || res
      if (this.configs.type === 'BKTableChart' && this.configs.configOption?.props?.hasPager && !this.configs.configOption?.props?.isStatic) {
        if (Array.isArray(data)) {
          this.total = data?.length || 0
        } else {
          this.total = data.total || 0
        }
      }
    },
    filterStaticData (data) {
      for (let i = 0; i < this.params.length; i++) {
        if (this.params[i].value) {
          if (Array.isArray(data)) {
            if (this.params[i].operator === 'eq') {
              // eslint-disable-next-line
              data = data.filter(c => c[this.params[i].alias] == this.params[i].value)
            } else if (this.params[i].operator === 'in') {
              const val = this.params[i].value.split(',')
              if (val.length === 1) {
                data = data.filter(c => c[this.params[i].alias] && c[this.params[i].alias].toString().indexOf(val[0]) !== -1)
              } else {
                data = data.filter(c => val.indexOf(c[this.params[i].alias]) !== -1)
              }
            } else if (this.params[i].operator === 'notin') {
              const val = this.params[i].value.split(',')
              if (val.length === 1) {
                data = data.filter(c => c[this.params[i].alias] && c[this.params[i].alias].toString().indexOf(val[0]) === -1)
              } else {
                data = data.filter(c => val.indexOf(c[this.params[i].alias]) === -1)
              }
            } else if (this.params[i].operator === 'gt') {
              data = data.filter(c => c[this.params[i].alias] > this.params[i].value)
            } else if (this.params[i].operator === 'ge') {
              data = data.filter(c => c[this.params[i].alias] >= this.params[i].value)
            } else if (this.params[i].operator === 'lt') {
              data = data.filter(c => c[this.params[i].alias] < this.params[i].value)
            } else if (this.params[i].operator === 'le') {
              data = data.filter(c => c[this.params[i].alias] <= this.params[i].value)
            }
          }
        }
      }
      return data
    },

    downloadChart () {
      if (this.$refs.chart) {
        const dataurl = this.$refs.chart.getDataURL({ type: 'jpeg', backgroundColor: '#ffffff' })
        this.$refs.downLoadPic.href = dataurl
        this.$refs.downLoadPic.download = `${this.configs.title || (new Date().getTime())}.jpg`
        this.$refs.downLoadPic.click()
      } else {
        try {
          this.customDownloadChart()
        } catch (error) {
          console.log(this.configs.type + '组件未实现customDownloadChart方法')
        }
      }
    },
    resetChart (config) {
      this.debouncedRefreshData()
      this.$nextTick(() => {
        this.debouncedCustomResetChart(config)
      })
    },
    buildParams (refresh) {
      this.params = []
      if (this.configs && this.configs.varListener) {
        for (let i = 0; i < this.configs.varListener.length; i++) {
          const listen = this.configs.varListener[i]
          if (this.pageConfig.varConfig) {
            const varCfg = this.pageConfig.varConfig.find(c => c.id === listen.id)
            if (varCfg) {
              this.params.push({ ...listen, value: varCfg.value || getInitValue(varCfg.initValue) })
            }
          }
        }
      }
      if (refresh) {
        this.debouncedRefreshData()
      }
    },
    show () {
      if (this.$refs.rootRef) this.$refs.rootRef.style.display = 'block'
    },
    hide () {
      if (this.$refs.rootRef) this.$refs.rootRef.style.display = 'none'
    },
    setStyle (styles) {
      if (this.$refs.rootRef) Object.assign(this.$refs.rootRef.style, styles)
    },
    addClass (className) {
      if (this.$refs.rootRef) this.$refs.rootRef.className = this.$refs.rootRef.className + ' ' + className
    },
    setContentConfig (config) {
      this.contentConfig = config
    },
    // 数据操作方法
    setData (data) {
      this.list = data
    },
    getData () {
      // 返回组件当前数据
      return this.relList
    },
    setupHooks (script) {
      hookManager.cleanupComponentHooks(this)
      if (script && typeof script === 'string') {
        script = script.replaceAll('#hookId#', this.configs.hookId)
        hookManager.executeHookScript(script, this)
      }
    },
    // 事件触发方法
    emitEvent (eventName, payload) {
      componentConfigs.emitter.emit(eventName, payload)
    }
  },
  created () {
    emitter.on('paramsChange', this.paramsChange)
    this.defaultData = getDefaultData(this.configs)
    this.debouncedCustomResetChart = debounce(200, (config) => {
      try {
        this.customResetChart(config)
      } catch (error) {
        console.log(this.configs.type + '组件未实现customResetChart方法', error)
      }
    })
    this.debouncedRefreshData = debounce(200, (refresh) => {
      this.setChartData(refresh)
    })
    this.buildParams(false)
  },
  mounted () {
    // componentRegistry.register(this.configs.hookId, this.publicAPI, this.$refs.rootRef)
    this.resetChart(this.configs)
    this.$nextTick(() => {
      hookManager.triggerHook('onMount', this)
    })
    // if (this.configs.dataSourceConfig || this.configs.seriesConfig) {
    // } else {
    //   this.debouncedRefreshData()
    // }
    // if (this.setMode) {
    // }
  },
  updated () {
    // 触发更新钩子
    hookManager.triggerHook('onUpdate', this)
  },
  beforeUnmount () {
    emitter.off('paramsChange', this.paramsChange)
    clearTimer(this.configs.chartId)
    // 触发销毁钩子
    hookManager.triggerHook('onDestroy', this)

    // 清理组件相关的钩子
    hookManager.cleanupComponentHooks(this)
    componentRegistry.unregister(this.configs.hookId)
  }
}
