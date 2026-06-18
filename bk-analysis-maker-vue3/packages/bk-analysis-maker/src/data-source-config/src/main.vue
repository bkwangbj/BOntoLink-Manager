<template>
  <div class="data-source-wrapper">
    <div
      class="left-container"
      :style="{flex: dataMappingVisible ? '0 0 500px' : '1'}"
    >
      <MetricSetting
        v-if="!dataMappingVisible"
        :metrics="metrics"
        :field-options="metricFieldOptions"
        :set-mode="saveAble"
        @update:metrics="onMetricsChange"
      />
      <GroupFilter
        v-if="!dataMappingVisible"
        :grouping="grouping"
        :dimension-options="metricFieldOptions"
        :group-values="groupValues"
        :set-mode="saveAble"
        @update:grouping="onGroupingChange"
      />
      <SortSetting
        v-if="!dataMappingVisible"
        :sorts="sorts"
        :field-options="metricFieldOptions"
        :set-mode="saveAble"
        @update:sorts="onSortsChange"
      />
      <template v-if="showRawSource">
      <div class="data-source-title">
        ① 设置数据源
      </div>
      <div
        v-show="dataSourceType && !dataMappingVisible && showSelectData"
        class="data-map"
        @click="openNext(dataSourceType === 'tjb'?'1' : '2')"
      >
        ②选取数据
      </div>
      <div
        v-show="dataSourceType && !dataMappingVisible"
        class="data-map"
        :class="showSelectData ? 'data-map-bottom':''"
        @click="openNext('3')"
      >
        <div
          class="icon"
        >
          <i-ri-menu-fold-line />
        </div>
        {{ showSelectData ? '③数据映射' : '②数据映射' }}
      </div>
      <div class="data-source-main">
        <div class="main-label">
          数据源类型
        </div>
        <EfSelect
          v-model="dataSourceType"
          size="small"
          :clearable="false"
          :mock="dataSourceList"
          :style="{margin: '0 5px 12px 0', width: dataSourceType || dataMappingVisible ? 'calc(100% - 10px)' : '100%'}"
          :disabled="!saveAble"
          group-by="group"
          @change="changeDataSourceType"
        />
        <BKCodeCom
          v-if="dataSourceType === 'static'"
          ref="editor"
          key="1"
          v-model="staticValue"
          class="code-content"
          language="json"
          :readonly="!saveAble"
          :has-table="true"
        />
        <EfTable
          v-if="dataSourceType === 'tjb'"
          :has-pager="false"
          :has-seq="false"
          :show-overflow="false"
          :stripe="false"
          :columns="tjbColumns"
          :data="tjbData"
          size="small"
          :style="{height: tjbData.length > 0 ?'200px' : '50px',width: '420px'}"
          class="vxe_table"
        />
        <!-- <el-input
          type="
          textarea"
          class="code-content tjb-config"
          v-if="dataSourceType === 'tjb'"
          ref="tjbEditor"
          v-model="tjbValue"
          readonly
          :rows="8"
          resize="none"
        /> -->
        <div
          v-if="dataSourceType != 'static'"
        >
          <div
            v-if="['post', 'get', 'tjb', 'fetch-get', 'fetch-post', 'fetch-post-form'].includes(dataSourceType)"
            class="main-label"
          >
            URL
          </div>
          <div
            class="d-flex"
            :style="{marginRight: '5px',marginBottom: '12px',width: dataSourceType && dataSourceType !== 'tjb' || dataMappingVisible ? 'calc(100% - 30px)' : '100%'}"
          >
            <el-input
              v-if="['post', 'get', 'tjb', 'fetch-get', 'fetch-post', 'fetch-post-form'].includes(dataSourceType)"
              v-model="interfacePath"
              size="small"
              :disabled="!saveAble"
              style="margin-right: 5px;"
            />
            <EfRadio
              v-if="dataSourceType == 'post'"
              v-model="paramsType"
              is-radio-button
              size="small"
              :mock="paramsTypeList"
              :disabled="!saveAble"
              class="post-type-radio"
            />
          </div>
          <div class="main-label">
            <el-checkbox
              v-model="interfaceFilterVisible"
              style="margin-right: 5px;"
              :disabled="!saveAble"
            />过滤器
          </div>
          <InterfaceFilterConfig
            v-if="interfaceFilterVisible"
            :visible="interfaceFilterVisible"
            :interface-filter="interfaceFilter"
            :set-mode="saveAble"
            @close="interfaceFilterVisible=false"
            @save="saveInterfaceFilter"
          />
          <div class="main-label">
            <el-checkbox
              v-model="interfaceTempParamsVisible"
              :disabled="!saveAble || dataSourceType == 'watf'"
              style="margin-right: 5px;"
              @change="interfaceTempParamsVisibleChange"
            />{{ dataSourceType == 'watf' ? '固定参数' : '临时参数' }}
          </div>
          <InterfaceFilterConfig
            v-if="interfaceTempParamsVisible"
            ref="paramsEditor"
            :visible="interfaceTempParamsVisible"
            :interface-filter="interfaceTempParams"
            type="params"
            :set-mode="saveAble"
            @close="interfaceTempParamsVisible=false"
            @save="saveInterfaceTempParams"
          />
        </div>
        <div class="main-label">
          <el-checkbox
            v-model="paramHandlerVisible"
            style="margin-right: 5px;"
            :disabled="!saveAble"
          />参数处理器
        </div>
        <InterfaceFilterConfig
          v-if="paramHandlerVisible"
          :visible="paramHandlerVisible"
          :interface-filter="paramHandler"
          :set-mode="saveAble"
          method-name=" paramHandler(params) "
          return-text="return params;"
          @close="paramHandlerVisible=false"
          @save="saveParamHandler"
        />
        <el-button
          v-show="dataSourceType"
          text
          style="padding: 0;margin-top: 12px;color: #1f6aff;"
          size="small"
          :disabled="!saveAble"
          @click="getData(true)"
        >
          <i-ri-eye-fill class="am-btn-icon" />提取数据
        </el-button>
      </div>
      </template>
    </div>
    <div
      v-if="dataMappingVisible && showRawSource"
      class="right-container"
      :style="{flex: '0 0 245px', width: '245px'}"
    >
      <DataMapping
        :visible="dataMappingVisible"
        :set-mode="saveAble"
        :items="mappingItems"
        :mapping-data="dataMapping"
        :data-source-type="dataSourceType"
        @close="dataMappingVisible=false"
        @save="saveDataMapping"
      />
    </div>
    <WatfConfig
      v-if="watfConfigVisible"
      :visible="watfConfigVisible"
      :set-mode="saveAble"
      :watf-config="interfaceTempParams"
      @close="watfConfigVisible=false"
      @save="saveWatfConfig"
    />
  </div>
</template>
<script>
import DataMapping from './components/data-mapping.vue'
import InterfaceFilterConfig from './components/interface-filter-config.vue'
import WatfConfig from './components/watf-config.vue'
import MetricSetting from './components/metric-setting.vue'
import GroupFilter from './components/group-filter.vue'
import SortSetting from './components/sort-setting.vue'
import emitter from '../../configs/emitter'
import { getTjbMapping, getInitValue } from '../../configs/common-func'
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
import { componentConfigs } from '../../configs'
export default {
  name: 'DataSourceConfig',
  components: {
    DataMapping,
    InterfaceFilterConfig,
    WatfConfig,
    MetricSetting,
    GroupFilter,
    SortSetting
  },
  inheritAttrs: false,

  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    saveAble: {
      type: Boolean,
      default: true
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
  emits: ['changeDataMappingVisible', 'buildChartData'],
  data () {
    return {
      dataSourceType: '',
      staticValue: '',
      interfacePath: '',
      interfaceFilter: '',
      beforeInterfaceFilter: '',
      interfaceTempParams: '',
      paramsType: 'json',
      paramsTypeList: [{ value: 'json', label: 'JSON' }, { value: 'form', label: 'FORM' }],
      beforeInterfaceTempParams: '',
      tjbConfig: null,
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs', overviewRulerBorder: false
      },
      dataMappingVisible: false,
      mappingItems: [],
      finalItems: [],
      interfaceFilterVisible: false,
      interfaceTempParamsVisible: false,
      tjbColumns: [],
      tjbData: [],
      dataMapping: {},
      changeTjb: false,
      watfConfigVisible: false,
      paramHandler: '',
      paramHandlerVisible: false,
      metrics: [],
      grouping: { field: '', mode: 'include', includeOther: true, selected: [] },
      groupValues: [], // 分组取值列表(P5 由数据源返回填充)
      sorts: [],
      // 看板场景隐藏 maker 原生「① 设置数据源 / 数据映射」配置(数据由预置 chart-data 提供)
      showRawSource: false
    }
  },
  computed: {
    // dataMapping () {
    //   if (!this.configs.dataSourceConfig) {
    //     return {}
    //   }
    //   if (!this.configs.dataSourceConfig.dataMapping) {
    //     return {}
    //   }
    //   return this.configs.dataSourceConfig.dataMapping
    // }
    dataSourceList () {
      if (this.tjbURL) {
        return [{ value: 'static', label: '静态数据', group: '常用' }, { value: 'post', label: 'POST接口', group: '常用' }, { value: 'get', label: 'GET接口', group: '常用' }, { value: 'tjb', label: '多维分析表', group: '特殊' }, { value: 'watf', label: 'WATF平台', group: '特殊' }]
      } else {
        return [{ value: 'static', label: '静态数据', group: '常用' }, { value: 'post', label: 'POST接口', group: '常用' }, { value: 'get', label: 'GET接口', group: '常用' }, { value: 'watf', label: 'WATF平台', group: '特殊' }]
      }
    },
    showSelectData () {
      return this.dataSourceType === 'tjb' || this.dataSourceType === 'watf'
    },
    // 指标/分组/排序可选字段:优先用对象类型属性(宿主注入 dataSourceConfig.fieldOptions),回退图表数据字段
    metricFieldOptions () {
      const injected = this.configs && this.configs.dataSourceConfig && this.configs.dataSourceConfig.fieldOptions
      if (Array.isArray(injected) && injected.length) return injected
      const seen = new Set()
      const opts = []
      ;(this.finalItems || []).forEach(c => {
        const v = c.field || c.value
        if (v && !seen.has(v)) { seen.add(v); opts.push({ label: c.label || v, value: v }) }
      })
      return opts
    }
  },
  watch: {
    'configs.chartId': {
      immediate: true,
      handler () {
        this.refreshConfig(this.configs)
      }
    },
    dataMappingVisible (v) {
      this.$emit('changeDataMappingVisible', v)
    },
    tjbConfig: {
      immediate: true,
      handler () {
        if (this.tjbConfig && this.tjbConfig.table) {
          this.tjbColumns = []
          this.tjbData = []
          if (!this.tjbConfig.config.xData) {
            this.getTjbTitle()
          } else {
            this.buildTjbConfig()
          }
        } else {
          this.tjbColumns = []
          this.tjbData = []
        }
      }
    }
  },
  created () {
    emitter.on('saveTjbConfig', this.saveTjbConfig)
  },
  mounted () {
  },
  beforeUnmount () {
    emitter.off('saveTjbConfig', this.saveTjbConfig)
  },
  methods: {
    async getTjbTitle () {
      let params = this.getDefaultParams()
      if (this.interfaceTempParamsVisible) {
        try {
          params = JSON.parse(this.interfaceTempParams)
        } catch (error) {
        }
      }
      params = {
        ...params,
        tableid: this.tjbConfig.table.id,
        rowindexs: this.tjbConfig.config.rowindexs,
        colindexs: this.tjbConfig.config.colindexs
      }
      if (this.paramHandlerVisible && this.paramHandler) {
        params = this.executeScript(this.paramHandler, params, true)
      }
      let res = await componentConfigs.request.post('/ywtt/jcfx/zhzs/getTableLabel', params)
      res = res.data || res
      // let yData = res.map(c => c.ylabel)
      // let xData = res.map(c => c.xlabel)
      // yData = Array.from(new Set(yData))
      // xData = Array.from(new Set(xData))
      this.tjbConfig.config.yData = res.rowlabels
      this.tjbConfig.config.xData = res.collabels
      this.buildTjbConfig()
    },
    buildTjbConfig () {
      const cols = this.tjbConfig.config.xData.map((item, i) => { return { title: item, field: `a-${i}`, width: 100 } })
      this.tjbColumns = [{ title: '', field: 'name', width: 100 }, ...cols]
      this.tjbData = this.tjbConfig.config.yData.map(item => { return { name: item } })
    },
    interfaceTempParamsVisibleChange () {
      if (this.saveAble && this.dataSourceType !== 'static' && this.dataSourceType !== 'watf' &&
      this.interfaceTempParamsVisible && !this.interfaceTempParams) {
        this.interfaceTempParams = JSON.stringify(this.getDefaultParams())
      }
    },
    getDefaultParams () {
      const params = {}
      if (this.configs && this.configs.varListener) {
        const varList = this.configs.varListener.filter(c => c.changeType === 'refreshData')
        for (let i = 0; i < varList.length; i++) {
          params[varList[i].alias] = varList[i].value || getInitValue(varList[i].initValue)
        }
      }
      return params
    },
    async openDataMapping () {
      try {
        let list = await this.getData(false)
        if (!list) {
          list = []
        }
        let fieldList = []
        if (Array.isArray(list)) {
          for (let i = 0; i < list.length; i++) {
            const keys = Object.keys(list[i])
            fieldList = [...fieldList, ...keys]
          }
        } else {
          fieldList = Object.keys(list)
        }
        fieldList = Array.from(new Set(fieldList))
        const mock = fieldList.map(item => { return { value: item, label: item } })
        this.mappingItems = this.finalItems
          ? this.finalItems.map(c => {
            return {
              ...c,
              type: c.isAdd && mock.length === 0 ? 'EfInput' : 'EfSelect',
              props: { clearable: false, mock }
            }
          })
          : []
        // if (this.configs.type === 'BKStatisticsChart') {
        // } else if (this.configs.type === 'BKBarChart') {
        //   this.mappingItems = [
        //     { type: 'EfSelect', label: 'x', field: 'x', props: { clearable: false, mock: mock } },
        //     { type: 'EfSelect', label: 'y', field: 'y', props: { clearable: false, mock: mock } },
        //     { type: 'EfSelect', label: '颜色映射', field: 'colorField', props: { clearable: false, mock: mock } }]
        // }
        this.dataMappingVisible = true
      } catch (error) {
        this.mappingItems = this.finalItems
          ? this.finalItems.map(c => {
            return {
              ...c,
              type: 'EfInput'
            }
          })
          : []
        this.dataMappingVisible = true
        // return ElMessage.error('请输入正确的格式')
      }
    },
    saveDataMapping (data, addItems) {
      this.dataMappingVisible = false
      let items = this.finalItems ? this.finalItems.filter(c => !c.isAdd) : []
      items = [...items, ...addItems]
      this.finalItems = items
      this.dataMapping = data
    },
    saveInterfaceFilter (filter) {
      this.interfaceFilter = filter
    },
    saveInterfaceTempParams (filter) {
      this.interfaceTempParams = filter
    },
    saveParamHandler (filter) {
      this.paramHandler = filter
    },
    async openTjb () {
      emitter.emit('openTjbConfig', this.tjbConfig)
    },
    openWatf () {
      this.watfConfigVisible = true
    },
    saveWatfConfig (config) {
      this.interfaceTempParams = JSON.stringify(config)
      this.$refs.paramsEditor.setValue(this.interfaceTempParams)
    },
    clearData () {
      // if (this.configs && this.configs.type) {
      //   this.dataSourceType = 'static'
      //   this.staticValue = JSON.stringify(getDefaultData(this.configs.type))
      //   this.$nextTick(() => {
      //     this.getData()
      //   })
      // } else {
      // }
      this.dataSourceType = ''
      this.staticValue = ''
      this.interfacePath = ''
      this.interfaceFilter = ''
      this.interfaceTempParams = ''
      this.paramsType = 'json'
      this.paramHandler = ''
      this.tjbConfig = null
      this.finalItems = []
      this.dataMapping = {}
      this.beforeInterfaceFilter = this.interfaceFilter
      this.beforeInterfaceTempParams = this.interfaceTempParams
    },
    saveTjbConfig (config) {
      this.tjbConfig = config
    },
    async getData (show) {
      let data
      if (this.dataSourceType === 'static') {
        try {
          data = JSON.parse(this.staticValue)
          if (show === true) {
            this.$emit('buildChartData', this.dataSourceType, data)
          }
        } catch (error) {
          // return ElMessage.error('请输入正确的格式')
        }
      } else if (this.dataSourceType === 'get' || this.dataSourceType === 'post') {
        if (this.interfacePath) {
          let params = this.getDefaultParams()
          if (this.interfaceTempParamsVisible) {
            try {
              params = JSON.parse(this.interfaceTempParams)
            } catch (error) {
            // return ElMessage.error('请输入正确的格式')
            }
          }
          if (this.paramHandlerVisible && this.paramHandler) {
            params = this.executeScript(this.paramHandler, params, true)
          }
          let res
          if (this.interfacePath && this.interfacePath.startsWith('http')) {
            res = await componentConfigs.request[this.type === 'post' ? 'fetchPost' : 'fetchGet'](this.interfacePath, params, this.paramsType !== 'form')
          } else {
            res = await componentConfigs.request[this.dataSourceType](this.interfacePath, params, this.paramsType !== 'form')
          }
          if (this.interfaceFilterVisible && this.interfaceFilter) {
            res = this.executeScript(this.interfaceFilter, res)
          }
          data = res
          if (show === true) {
            this.$emit('buildChartData', this.dataSourceType, data)
          }
        }
      } else if (this.dataSourceType === 'fetch-get' || this.dataSourceType === 'fetch-post' || this.dataSourceType === 'fetch-post-form') {
        if (this.interfacePath) {
          let params = this.getDefaultParams()
          if (this.interfaceTempParamsVisible) {
            try {
              params = JSON.parse(this.interfaceTempParams)
            } catch (error) {
            // return ElMessage.error('请输入正确的格式')
            }
          }
          if (this.paramHandlerVisible && this.paramHandler) {
            params = this.executeScript(this.paramHandler, params, true)
          }
          const fetchParam = { method: this.dataSourceType === 'fetch-get' ? 'GET' : 'POST' }
          if (this.dataSourceType !== 'fetch-get') {
            const useJson = this.dataSourceType === 'fetch-post-form'
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
                'Content-Type': this.dataSourceType === 'fetch-post' ? 'application/json' : 'application/x-www-form-urlencoded'
              }
            }
          }
          const queryString = new URLSearchParams(params).toString()
          let res = await fetch(`${this.interfacePath}${this.dataSourceType === 'fetch-get' ? '?' + queryString : ''}`, fetchParam)
          res = await res.json()
          if (this.interfaceFilterVisible && this.interfaceFilter) {
            res = this.executeScript(this.interfaceFilter, res)
          }
          data = res
          if (show === true) {
            this.$emit('buildChartData', this.dataSourceType, data)
          }
        }
      } else if (this.dataSourceType === 'watf') {
        let params = this.getDefaultParams()
        if (this.interfaceTempParamsVisible) {
          try {
            params = JSON.parse(this.interfaceTempParams)
          } catch (error) {
          // return ElMessage.error('请输入正确的格式')
          }
        }
        if (this.paramHandlerVisible && this.paramHandler) {
          params = this.executeScript(this.paramHandler, params, true)
        }
        let res = []
        try {
          res = await componentConfigs.request.singleTableOperation(params)
        } catch (error) {
          ElMessage.error('接口报错，请检查参数')
        }
        if (this.interfaceFilterVisible && this.interfaceFilter) {
          res = this.executeScript(this.interfaceFilter, res)
        }
        data = res
        if (show === true) {
          this.$emit('buildChartData', this.dataSourceType, data)
        }
      } else if (this.dataSourceType === 'tjb') {
        if ((this.interfacePath || this.tjbURL) && this.tjbConfig && this.tjbConfig.table) {
          if (show === true) {
            if (this.changeTjb) {
              this.dataMapping = getTjbMapping(this.dataMapping, this.finalItems)
              this.changeTjb = false
            }
            let params = this.getDefaultParams()
            if (this.interfaceTempParamsVisible) {
              try {
                params = JSON.parse(this.interfaceTempParams)
              } catch (error) {
              }
            }
            params = {
              ...params,
              tableid: this.tjbConfig.table.id,
              rowindexs: this.tjbConfig.config.rowindexs,
              colindexs: this.tjbConfig.config.colindexs
            }
            if (this.paramHandlerVisible && this.paramHandler) {
              params = this.executeScript(this.paramHandler, params, true)
            }
            let res = await componentConfigs.request.post(this.interfacePath || this.tjbURL, params)
            res = res.data || data
            if (res && res.needcal) {
              return ElMessage.info('正在计算数据，请稍后再试')
            }
            if (this.interfaceFilterVisible && this.interfaceFilter) {
              res = this.executeScript(this.interfaceFilter, res)
            }
            data = res
            this.$emit('buildChartData', this.dataSourceType, data)
          } else {
            if (this.changeTjb) {
              this.dataMapping = getTjbMapping(this.dataMapping, this.finalItems)
              this.changeTjb = false
            }
            let params = this.getDefaultParams()
            if (this.interfaceTempParamsVisible) {
              try {
                params = JSON.parse(this.interfaceTempParams)
              } catch (error) {
              }
            }
            params = {
              ...params,
              tableid: this.tjbConfig.table.id,
              rowindexs: this.tjbConfig.config.rowindexs,
              colindexs: this.tjbConfig.config.colindexs
            }
            if (this.paramHandlerVisible && this.paramHandler) {
              params = this.executeScript(this.paramHandler, params, true)
            }
            let res = await componentConfigs.request.post(this.interfacePath || this.tjbURL, params)
            res = res.data || data
            if (this.interfaceFilterVisible && this.interfaceFilter) {
              res = this.executeScript(this.interfaceFilter, res)
            }
            data = res
          }
        } else {
          ElMessage.error('请配置完整数据源')
        }
      }
      return data
    },
    setTjbMapping () {
      this.dataMapping = getTjbMapping(this.dataMapping, this.finalItems)
      this.changeTjb = false
    },
    executeScript (code, data, isParams = false) {
      if (isParams) {
        const func = new Function('params', code)
        const value = func(data)
        return value
      } else {
        let params = this.getDefaultParams()
        if (this.interfaceTempParamsVisible) {
          try {
            params = JSON.parse(this.interfaceTempParams)
          } catch (error) {
          }
        }
        const func = new Function('data', 'params', code)
        const value = func(data, params)
        return value
      }
    },
    changeDataSourceType () {
      this.$nextTick(() => {
        this.changeTjb = this.dataSourceType === 'tjb'
        this.dataMappingVisible = false
        if (this.dataSourceType === 'tjb') {
          this.interfacePath = this.tjbURL
        } else if (this.dataSourceType === 'watf') {
          this.beforeInterfaceFilter = this.interfaceFilter
          this.beforeInterfaceTempParams = this.interfaceTempParams
          this.interfaceTempParamsVisible = true
          this.interfaceFilterVisible = true
          this.interfaceFilter = 'return data.rows'
          this.interfaceTempParams = JSON.stringify({
            url: '/grid/getTableData',
            appId: 'CXFX',
            bzCode: 'CXFX',
            metaSet: '',
            module: '',
            methodCode: 'select',
            viewCode: '',
            methodParams: {
              queryParam: {}
            }
          })
        } else {
          this.interfaceFilter = this.beforeInterfaceFilter
          this.interfaceTempParams = this.beforeInterfaceTempParams
        }
      })
    },
    getConfig () {
      // let res = { type: this.dataSourceType, items: this.finalItems }
      let data
      try {
        data = JSON.parse(this.staticValue)
      } catch (error) {

      }
      const res = {
        type: this.dataSourceType,
        items: this.finalItems,
        value: this.staticValue,
        data,
        interfacePath: this.interfacePath,
        interfaceFilter: this.interfaceFilter,
        interfaceTempParams: this.interfaceTempParams,
        paramsType: this.paramsType,
        interfaceFilterVisible: this.interfaceFilterVisible,
        interfaceTempParamsVisible: this.interfaceTempParamsVisible,
        paramHandler: this.paramHandler,
        paramHandlerVisible: this.paramHandlerVisible,
        tjbConfig: this.tjbConfig,
        // 指标/分组筛选/排序(本面板新增):随「应用/保存」一并写回,避免被旧配置覆盖
        metrics: utils.deepClone(this.metrics),
        grouping: utils.deepClone(this.grouping),
        sorts: utils.deepClone(this.sorts)
      }
      if (this.dataMapping) {
        res.dataMapping = this.dataMapping
      }
      // if (this.dataSourceType === 'static') {
      //   let data
      //   try {
      //     data = JSON.parse(this.staticValue)
      //   } catch (error) {

      //   }
      //   res = { ...res, value: this.staticValue, data: data }
      // } else if (this.dataSourceType === 'post' || this.dataSourceType === 'get') {
      //   res = { ...res, interfacePath: this.interfacePath, interfaceFilter: this.interfaceFilter, interfaceFilterVisible: this.interfaceFilterVisible }
      // } else if (this.dataSourceType === 'tjb') {
      //   res = { ...res, tjbConfig: this.tjbConfig }
      // }
      return res
    },
    refreshConfig (configs) {
      if (configs) {
        if (configs.dataSourceConfig) {
          this.dataSourceType = configs.dataSourceConfig.type || this.dataSourceType
          this.staticValue = configs.dataSourceConfig.data ? JSON.stringify(configs.dataSourceConfig.data) : ''
          this.tjbConfig = configs.dataSourceConfig.tjbConfig
          this.interfacePath = configs.dataSourceConfig.interfacePath
          this.interfaceFilter = configs.dataSourceConfig.interfaceFilter
          this.paramHandler = configs.dataSourceConfig.paramHandler
          this.paramsType = configs.dataSourceConfig.paramsType || 'json'
          this.interfaceTempParams = configs.dataSourceConfig.interfaceTempParams
          this.interfaceFilterVisible = configs.dataSourceConfig.interfaceFilterVisible || false
          this.paramHandlerVisible = configs.dataSourceConfig.paramHandlerVisible || false
          this.interfaceTempParamsVisible = configs.dataSourceConfig.interfaceTempParamsVisible || false
          this.dataMapping = configs.dataSourceConfig.dataMapping || {}
          this.beforeInterfaceFilter = this.interfaceFilter
          this.beforeInterfaceTempParams = this.interfaceTempParams
          this.metrics = configs.dataSourceConfig.metrics ? utils.deepClone(configs.dataSourceConfig.metrics) : []
          this.grouping = configs.dataSourceConfig.grouping ? utils.deepClone(configs.dataSourceConfig.grouping) : { field: '', mode: 'include', includeOther: true, selected: [] }
          this.sorts = configs.dataSourceConfig.sorts ? utils.deepClone(configs.dataSourceConfig.sorts) : []
          this.$nextTick(() => this.loadGroupValues())
        } else {
          this.clearData()
          this.metrics = []
          this.grouping = { field: '', mode: 'include', includeOther: true, selected: [] }
          this.sorts = []
          this.groupValues = []
        }
        this.finalItems = (configs && configs.items) ? utils.deepClone(configs.items) : []
      } else {
        this.clearData()
        this.metrics = []
        this.grouping = { field: '', mode: 'include', includeOther: true, selected: [] }
        this.sorts = []
      }
    },
    // 把当前 指标/分组/排序 广播给对应图表(按 chartId),图表同步后重新取数(实时生效)
    applyToChart () {
      emitter.emit('am-datasource-apply', {
        chartId: this.configs && this.configs.chartId,
        metrics: utils.deepClone(this.metrics),
        grouping: utils.deepClone(this.grouping),
        sorts: utils.deepClone(this.sorts)
      })
    },
    // 指标变化:写回图表配置 configs.dataSourceConfig.metrics(随页面保存持久化)
    onMetricsChange (list) {
      this.metrics = list
      if (!this.configs.dataSourceConfig) this.configs.dataSourceConfig = {}
      this.configs.dataSourceConfig.metrics = utils.deepClone(list)
      this.applyToChart()
    },
    // 分组与筛选变化:写回 configs.dataSourceConfig.grouping;维度变化则重新拉取分组取值
    onGroupingChange (g) {
      const fieldChanged = g.field !== (this.grouping && this.grouping.field)
      this.grouping = g
      if (!this.configs.dataSourceConfig) this.configs.dataSourceConfig = {}
      this.configs.dataSourceConfig.grouping = utils.deepClone(g)
      if (fieldChanged) this.loadGroupValues()
      this.applyToChart()
    },
    // 用图表接口按当前分组维度拉取取值列表 [{name,value}] → 填充分组值列表(勾选/数值/进度条)
    loadGroupValues () {
      const ds = this.configs && this.configs.dataSourceConfig
      const field = this.grouping && this.grouping.field
      const reqGet = componentConfigs.request && componentConfigs.request.get
      if (!ds || !ds.interfacePath || !field || !reqGet) { this.groupValues = []; return }
      Promise.resolve(reqGet(ds.interfacePath, { grouping: JSON.stringify({ field }), limit: 50 }, true))
        .then(res => {
          const data = (res && (res.data || res)) || []
          this.groupValues = Array.isArray(data)
            ? data.map(d => ({ name: d.name != null ? d.name : (d.x != null ? d.x : d.key), value: d.value != null ? d.value : (d.y != null ? d.y : 0) }))
            : []
        })
        .catch(() => { this.groupValues = [] })
    },
    // 排序条件变化:写回 configs.dataSourceConfig.sorts
    onSortsChange (list) {
      this.sorts = list
      if (!this.configs.dataSourceConfig) this.configs.dataSourceConfig = {}
      this.configs.dataSourceConfig.sorts = utils.deepClone(list)
      this.applyToChart()
    },
    openNext (type) {
      if (type === '1') {
        this.openTjb()
      } else if (type === '2') {
        this.openWatf()
      } else {
        this.openDataMapping()
      }
    }

  }
}
</script>

<style lang="scss" scoped>
:deep() {

  .code-item {

    .el-form-item__content {
      height: 200px;
    }
  }

  .data-source-item {
    display: flex;
    justify-content: space-between;
  }

  .el-radio-group {
    display: flex;
    flex-wrap: nowrap;
    width: 100%;
    padding: 0 3px;
    background: #ededed;
    border-radius: 4px;

    .el-radio-button--small {
      display: flex;
      flex: 1;
      align-items: center;
      padding: 3px 0;

      .el-radio-button__inner {
        width: 100%;
        height: 100%;
        padding: 4px 10px;
        color: #a3a3a3 !important;
        background-color: #ededed !important;
        border-color: #ededed !important;
        box-shadow: none !important;
      }
    }

    .el-radio-button--small.is-active {

      .el-radio-button__inner {
        font-size: 12px;
        line-height: 1em;
        color: #0a0a0a !important;
        background-color: #fff !important;
        border-color: #fff !important;
        border-radius: 4px;
      }
    }
  }
}

.data-source-wrapper {
  display: flex;
  height: 100%;
  overflow-x: hidden;

  .data-source-title {
    height: 32px;
    padding-left: 12px;
    font-size: 12px;
    line-height: 32px;
    color: #0a0a0a;
    background: #f5f5f5;
    border: 1px solid #e6e6e6;
    border-right: none;
  }

  .left-container {
    position: relative;
    display: flex;
    flex-direction: column;
    padding: 12px 14px;
    box-sizing: border-box;

    /* 指标/分组/排序里的下拉:白底 + 边框(覆盖 maker 灰底/无边框) */
    :deep(.el-select__wrapper),
    :deep(.el-input__wrapper) {
      background-color: #fff !important;
      box-shadow: 0 0 0 1px #dcdfe6 inset !important;
      border-radius: 4px;
    }
    :deep(.el-select__wrapper:hover),
    :deep(.el-input__wrapper:hover) {
      box-shadow: 0 0 0 1px #c0c4cc inset !important;
    }
    :deep(.el-select__wrapper.is-focused),
    :deep(.el-input.is-focus .el-input__wrapper) {
      box-shadow: 0 0 0 1px #1f6aff inset !important;
    }
    :deep(.el-select__wrapper.is-disabled),
    :deep(.el-input.is-disabled .el-input__wrapper) {
      background-color: #f5f7fa !important;
    }

    .data-map {
      position: absolute;
      top: 0;
      right: 0;
      z-index: 99;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 39px;
      height: 166px;
      font-size: 12px;
      color: #1a1a1a;
      letter-spacing: 10px;
      cursor: pointer;
      background: #f5f5f5;
      border: 1px solid #e6e6e6;
      border-radius: 0 0 0 4px;
      writing-mode: vertical-lr;

      &:hover {
        background: #e9e9e9;
      }

      .icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 25px;
        height: 25px;
        margin-bottom: 10px;
        background: #ededed;
        border-radius: 4px;

        > i {
          position: relative;
          top: 6px;
        }
      }
    }

    .data-map-bottom {
      top: 166px;
    }

    .data-source-main {
      padding: 12px;
      margin: 10px 12px;
      background: #f7f7f7;
      border-radius: 4px;

      .main-label {
        display: flex;
        align-items: center;
        margin-bottom: 8px;
        font-size: 12px;
        color: #7a7a7a;
      }

      .code-content {
        height: 500px;
        margin-bottom: 12px;
        overflow: hidden;
        border: 1px solid #ededed;
        border-radius: 4px;
      }

      .tjb-config {
        height: 182px;
      }
    }
  }
}

.vxe_table {

  :deep(.vxe-grid) {
    padding-left: 0;
  }
}
</style>
