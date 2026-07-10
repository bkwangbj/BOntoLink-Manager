<template>
  <el-container
    class="chart-cfg-wrap"
  >
    <!-- <el-header
      style="height: 40px !important;"
      class="title"
    >
      图表配置
      <b>图表配置</b>
      <i
        class="ri-close-fill"
        style="cursor: pointer;"
        @click="$emit('setExpand')"
      />
    </el-header> -->
    <el-main class="chart-cfg-main">
      <el-tabs
        v-model="currentTab"
        type="card"
        class="config-tab"

        @tab-click="handleTabClick"
      >
        <el-tab-pane name="base">
          <template #label>
            <span><i
              class="am-iconfont icon-yangshi"
              style="margin-right: 4px;font-size: 12px;"
            />基础</span>
          </template>

          <div class="base-flow">
            <EfForm
              ref="baseForm"
              class="base-form"
              size="small"
              label-width="80px"
              :rules="rules"
              :items="finalFormItems"
              :has-reset="false"
              :has-submit="false"
              :disabled="!saveAble"
            />
            <div
              class="custom-group inline-col-form"
              style="width: 100%;"
            >
              <slot />
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane
          name="data"
          lazy
        >
          <template #label>
            <div

              class="am-dt-content"
            >
              <i
                class="am-iconfont icon-a-shujuyuan2"
                style="margin-right: 4px;font-size: 12px;"
              />数据源
              <el-button
                v-if="currentTab === 'data'"
                text
                size="small"
                type="primary"
                class="am-dt-btn am-dt-copy"
                title="复制数据源"
                @click="copyDataSourceConfig"
              >
                <el-icon><CopyDocument /></el-icon>
              </el-button>
              <el-button
                v-if="currentTab === 'data' && saveAble"
                text
                size="small"
                type="primary"
                class="am-dt-btn am-dt-paste"
                title="粘贴数据源"
                @click="pasteDataSourceConfig"
              >
                <el-icon><FolderChecked /></el-icon>
              </el-button>
            </div>
          </template>
          <BKDataSourceConfig
            ref="dataSourceCfg"
            :save-able="saveAble"
            :configs="finalConfig"
            :page-config="pageConfig"
            :tjb-u-r-l="tjbURL"
            v-bind="$attrs"
            @build-chart-data="buildChartData"
            @save-data-mapping="saveDataMapping"
          />
        </el-tab-pane>
        <el-tab-pane
          name="advanced"
          lazy
        >
          <template #label>
            <span><i
              class="am-iconfont icon-gaoji"
              style="margin-right: 4px;font-size: 12px;"
            />高级</span>
          </template>
          <el-collapse class="advanced-container">
            <el-collapse-item>
              <template #title>
                变量监听配置
              </template>
              <div
                class="collapse-item var-table"
                style="height: 276px;"
              >
                <BKVarListenerConfig
                  ref="vlCfg"
                  :save-able="saveAble"
                  :configs="finalConfig"
                  :page-config="pageConfig"
                  @change="varListenerChange"
                />
              </div>
            </el-collapse-item>
            <el-collapse-item v-if="finalConfig.eventConfig && finalConfig.eventConfig.length">
              <template #title>
                事件配置
              </template>
              <div class="collapse-item">
                <BKEventConfig
                  ref="eventCfg"
                  :save-able="saveAble"
                  :configs="finalConfig"
                  :page-config="pageConfig"
                  @event-change="eventChange"
                />
              </div>
            </el-collapse-item>
            <el-collapse-item>
              <template #title>
                操作配置
              </template>
              <div class="collapse-item">
                <BKOperatorConfig
                  ref="operatorCfg"
                  :save-able="saveAble"
                  :configs="finalConfig"
                  :page-config="pageConfig"
                />
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
      </el-tabs>
    </el-main>
    <el-footer class="chart-cfg-footer">
      <el-button
        type="primary"
        size="small"
        :disabled="!saveAble"
        @click="save"
      >
        应用
      </el-button>
      <el-dropdown class="am-copy-btn">
        <span class="el-dropdown-link">
          复制<el-icon class="el-icon--right">
            <arrow-down />
          </el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="copyConfig">
              复制配置
            </el-dropdown-item>
            <el-dropdown-item @click="copyConfig('1')">
              复制为外置组件配置
            </el-dropdown-item>
            <el-dropdown-item @click="copyConfig('2')">
              更新外置组件配置
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <!-- <el-button
        @click="copyConfig"
        size="small"
        class="am-copy-btn"
      >
        复制
      </el-button> -->
    </el-footer>
  </el-container>
</template>

<script lang="jsx">
import { v4 as uuidv4 } from 'uuid'
import { commonItems, commonRules } from '../../configs/common-item'
import { getTjbMapping, getChartSeries, getConfigTheme, getColumns, getDefaultData } from '../../configs/common-func'
import { chartDefaultConfig } from '../../configs/chart-default-config'
import { debounce } from 'throttle-debounce'
import ExplainConfig from '../../chart-common-config/components/explain-config.vue'
import HookConfig from '../../chart-common-config/components/hook-config.vue'
import CardPaddingConfig from '../../chart-common-config/components/card-padding-config.vue'
import QueryPositionConfig from '../../chart-common-config/components/query-position-config.vue'
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
import { componentConfigs } from '../../configs'
export default {
  name: 'BasicChartConfig',
  components: {
    // eslint-disable-next-line
    ExplainConfig,
    // eslint-disable-next-line
    CardPaddingConfig,
    // eslint-disable-next-line
    QueryPositionConfig,
    // eslint-disable-next-line
    HookConfig
  },
  inject: ['getThemeChartStyle'],
  provide () {
    return {
      getThemeFont: () => this.configs?.chartTheme?.textColor || '#000'
    }
  },
  inheritAttrs: false,
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    pageConfig: {
      type: Object,
      default: () => {}
    },
    saveAble: {
      type: Boolean,
      default: true
    },
    items: {
      type: Array,
      default: () => commonItems.filter(c => c.isBasic)
    },
    rules: {
      type: Object,
      default: () => commonRules
    },
    tjbURL: {
      type: String,
      default: ''
    },
    // 初始激活的配置 tab(base/data/...);切换左侧图表时由宿主透传当前 tab,保持不跳回基础
    initTab: {
      type: String,
      default: 'base'
    }
  },
  emits: ['configOptionInit', 'buildChartSeriesData', 'changeBranchType', 'saveChartCfg', 'saveChartConfig', 'changeTab'],
  data () {
    return {
      // dataSourceConfig: { data: [], type: '' },
      // varListener: [],
      // eventConfig: [],
      finalConfig: {},
      // addItems: [],
      currentTab: this.initTab || 'base',
      finalFormItems: [],
      isFresh: true,
      explainConfigRef: null,
      hookConfigRef: null,
      cardPaddingRef: null,
      queryConfigRef: null
    }
  },
  computed: {
    chartTheme () {
      return (this.pageConfig?.themeConfigs?.preTheme || '') + ',' + (this.pageConfig?.themeConfigs?.globalCss?.themeMode || '')
    }
  },
  watch: {
    'configs.chartId': {
      handler () {
        this.$nextTick(() => {
          this.$emit('configOptionInit')
          if (this.explainConfigRef) {
            this.explainConfigRef.setFormData(this.configs?.explainConfig || {})
          }
          if (this.hookConfigRef) {
            this.hookConfigRef.setFormData(this.configs?.hookConfig || {})
          }
          if (this.cardPaddingRef) {
            this.cardPaddingRef.setFormData(this.configs?.paddingConfig || {})
          }
          if (this.queryConfigRef) {
            this.queryConfigRef.setFormData(this.configs?.queryConfig || {})
          }
        })
      },
      immediate: true
    },
    items: {
      handler () {
        if (this.items?.length) {
          const items = utils.deepClone(this.items)
          items.forEach(ele => {
            if (ele?.eventsType && ele?.eventsType === 'basic') {
              if (!ele.props) {
                ele.props = {}
              }
              ele.props.onChange = () => {
                this.formChange()
              }
            }
            if (ele.slot === 'explain') {
              delete ele.slot
              ele.itemSlots = {
                default: () => {
                  return [
                    <ExplainConfig
                      ref={el => { this.explainConfigRef = el }}
                      configs={this.configs}
                      onChartChange={this.formChange}
                    />
                  ]
                }
              }
            } else if (ele.slot === 'hook') {
              delete ele.slot
              ele.itemSlots = {
                default: () => {
                  return [
                    <HookConfig
                      ref={el => { this.hookConfigRef = el }}
                      configs={this.configs}
                      onChartChange={this.formChange}
                    />
                  ]
                }
              }
            } else if (ele.slot === 'paddingBox') {
              delete ele.slot
              ele.itemSlots = {
                default: () => {
                  return [
                    <CardPaddingConfig
                      ref={el => { this.cardPaddingRef = el }}
                      onChartChange={this.formChange}
                    />
                  ]
                }
              }
            } else if (ele.slot === 'queryBox') {
              delete ele.slot
              ele.itemSlots = {
                default: () => {
                  return [
                    <QueryPositionConfig
                      ref={el => { this.queryConfigRef = el }}
                      style="overflow: hidden;"
                      onChartChange={this.formChange}
                    />
                  ]
                }
              }
            }
          })
          this.finalFormItems = items
          this.$nextTick(() => {
            this.initConfig()
          })
        }
      },
      immediate: true
    },
    configs: {
      immediate: true,
      handler () {
        if (this.configs && this.configs.type) {
          this.initConfig()
          // if (!this.configs.dataSourceConfig) {
          //   this.dataSourceConfig = { }
          // } else {
          //   this.dataSourceConfig = utils.deepClone(this.configs.dataSourceConfig)
          // }

          // this.varListener = this.configs.varListener ? utils.deepClone(this.configs.varListener) : []
          // this.eventConfig = this.configs.eventConfig ? utils.deepClone(this.configs.eventConfig) : []
          // this.addItems = this.configs.items ? utils.deepClone(this.configs.items) : []
        }
        this.finalConfig = utils.deepClone(this.configs)
      }
    }
  },
  created () {
    this.debounceSave = debounce(200, () => {
      this.save()
    })
  },
  methods: {
    async buildChartData (type, data) {
      // this.dataSourceConfig.type = type
      // if (type === 'static') {
      //   this.dataSourceConfig.data = data
      // }
      // await this.getConfigData()
      this.$emit('buildChartSeriesData', type, data)
    },
    saveDataMapping (mapping, addItems) {
      // this.dataSourceConfig.dataMapping = mapping
      // this.addItems = addItems
      this.getConfigData()
    },
    varListenerChange (value) {
      this.varListener = value
      this.getConfigData()
    },
    formChange () {
      this.$nextTick(() => {
        this.save()
      })
    },
    eventChange (eventConfig) {
      this.eventConfig = eventConfig
      this.getConfigData()
    },
    async getConfigData () {
      let data = {
        ...this.$refs.baseForm.getFormData()
      }
      if (this.$refs.dataSourceCfg) {
        const config = this.$refs.dataSourceCfg.getConfig()
        if (config.items) {
          data.items = config.items
          delete config.items
        }
        data.dataSourceConfig = { ...this.configs.dataSourceConfig, ...config }
      }
      if (this.$refs.vlCfg) {
        data.varListener = this.$refs.vlCfg.getConfig()
      }
      if (this.$refs.eventCfg) {
        data.eventConfig = this.$refs.eventCfg.getConfig()
      }
      if (this.$refs.operatorCfg) {
        data.operatorConfig = this.$refs.operatorCfg.getConfig()
      }
      if (this.explainConfigRef) {
        data.explainConfig = this.explainConfigRef.saveFormData()
      }
      if (this.hookConfigRef) {
        data.hookConfig = this.hookConfigRef.saveFormData()
      }
      if (this.cardPaddingRef) {
        data.paddingConfig = this.cardPaddingRef.saveFormData()
      }
      if (this.queryConfigRef) {
        data.queryConfig = this.queryConfigRef.saveFormData()
      }
      data = {
        ...this.configs,
        ...data
      }
      delete data.tabList
      this.finalConfig = data
      await this.$nextTick()
      return data
    },
    getDataSourceConfig () {
      if (this.$refs.dataSourceCfg) {
        const config = this.$refs.dataSourceCfg.getConfig()
        if (config.items) {
          delete config.items
        }
        return config
      }
      return this.finalConfig.dataSourceConfig
    },
    initConfig () {
      this.$nextTick(() => {
        this.$refs.baseForm && this.$refs.baseForm.setFormData(this.configs)
      })
    },
    async changeChartType (config) {
      const valid = await this.$refs.baseForm.validate()
      if (valid) {
        // 获取最新配置和数据源
        let newConfig = await this.getConfigData()
        if (newConfig.dataSourceConfig.type === 'tjb' && config.type === 'BKPieChart' && !newConfig.dataSourceConfig.dataMapping) {
          newConfig.dataSourceConfig.dataMapping = getTjbMapping(newConfig.dataSourceConfig.dataMapping, config.items)
        }
        // 最新数据
        let data = await this.getChartData()
        // 切换后默认静态数据
        const defaultData = getDefaultData(config)
        let dataSourceConfig = newConfig.dataSourceConfig
        if (Object.prototype.toString.call(data) !== Object.prototype.toString.call(defaultData)) {
          dataSourceConfig = { type: 'static', data: defaultData }
          data = defaultData
        }
        const option = utils.deepClone(chartDefaultConfig.get(config?.branchType || config.type))
        // 读取系列

        if (config.type === 'BKTableChart') {
          let key = 'xlabel'
          if (dataSourceConfig.dataMapping && dataSourceConfig.dataMapping.xlabel) {
            key = dataSourceConfig.dataMapping.xlabel
          }
          option.columns = await getColumns(data, config, key, dataSourceConfig)
        } else {
          let key = 'colorField'
          if (dataSourceConfig.dataMapping && dataSourceConfig.dataMapping.colorField) {
            key = dataSourceConfig.dataMapping.colorField
          }
          getChartSeries(config, data, option.series, key)
        }
        const chartStyle = utils.deepClone(this.pageConfig.themeConfigs.chartStyle)
        // 读取主题
        const { configOption, chartTheme } = getConfigTheme(option, chartStyle, config.type, config.branchType)

        newConfig = { ...newConfig, configOption, chartTheme, branchType: config.branchType, items: config.items, dataSourceConfig }
        if (this.$refs.dataSourceCfg) {
          this.$refs.dataSourceCfg.refreshConfig(newConfig)
          this.$refs.dataSourceCfg.dataMappingVisible = false
        }
        this.$emit('changeBranchType', newConfig)
      }
    },
    async getChartData () {
      let data
      const dataSourceConfig = this.getDataSourceConfig()
      if (dataSourceConfig.type === 'static') {
        try {
          data = dataSourceConfig.data
        } catch (error) {
          // return ElMessage.error('请输入正确的格式')
        }
      } else if (dataSourceConfig.type === 'get' || dataSourceConfig.type === 'post') {
        if (dataSourceConfig.interfacePath) {
          let params
          if (dataSourceConfig.interfaceTempParamsVisible) {
            try {
              params = JSON.parse(dataSourceConfig.interfaceTempParams)
            } catch (error) {
            // return ElMessage.error('请输入正确的格式')
            }
          }
          if (dataSourceConfig.paramHandlerVisible && dataSourceConfig.paramHandler) {
            params = this.executeScript(dataSourceConfig.paramHandler, params, true)
          }
          let res
          if (dataSourceConfig.interfacePath && dataSourceConfig.interfacePath.startsWith('http')) {
            res = await componentConfigs.request[dataSourceConfig.type === 'post' ? 'fetchPost' : 'fetchGet'](dataSourceConfig.interfacePath, params, dataSourceConfig.paramsType !== 'form')
          } else {
            res = await componentConfigs.request[dataSourceConfig.type](dataSourceConfig.interfacePath, params, dataSourceConfig.paramsType !== 'form')
          }

          if (dataSourceConfig.interfaceFilterVisible && dataSourceConfig.interfaceFilter) {
            res = this.executeScript(dataSourceConfig.interfaceFilter, res)
          }
          data = res
        }
      } else if (dataSourceConfig.type === 'fetch-get' || dataSourceConfig.type === 'fetch-post' || dataSourceConfig.type === 'fetch-post-form') {
        if (dataSourceConfig.interfacePath) {
          let params
          if (dataSourceConfig.interfaceTempParamsVisible) {
            try {
              params = JSON.parse(dataSourceConfig.interfaceTempParams)
            } catch (error) {
            // return ElMessage.error('请输入正确的格式')
            }
          }
          if (dataSourceConfig.paramHandlerVisible && dataSourceConfig.paramHandler) {
            params = this.executeScript(dataSourceConfig.paramHandler, params, true)
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
          if (dataSourceConfig.interfaceFilterVisible && dataSourceConfig.interfaceFilter) {
            res = this.executeScript(dataSourceConfig.interfaceFilter, res)
          }
          data = res
        }
      } else if (dataSourceConfig.type === 'tjb') {
        if ((dataSourceConfig.interfacePath || this.tjbURL) && dataSourceConfig.tjbConfig && dataSourceConfig.tjbConfig.table) {
          let params
          if (dataSourceConfig.interfaceTempParamsVisible) {
            try {
              params = JSON.parse(dataSourceConfig.interfaceTempParams)
            } catch (error) {
            }
          }
          params = {
            ...params,
            tableid: dataSourceConfig.tjbConfig.table.id,
            rowindexs: dataSourceConfig.tjbConfig.config.rowindexs,
            colindexs: dataSourceConfig.tjbConfig.config.colindexs
          }
          if (dataSourceConfig.paramHandlerVisible && dataSourceConfig.paramHandler) {
            params = this.executeScript(dataSourceConfig.paramHandler, params, true)
          }
          let res = await componentConfigs.request.post(dataSourceConfig.interfacePath || this.tjbURL, params)
          res = res.data || data
          if (res && res.needcal) {
            return ElMessage.info('正在计算数据，请稍后再试')
          }
          if (dataSourceConfig.interfaceFilterVisible && dataSourceConfig.interfaceFilter) {
            res = this.executeScript(dataSourceConfig.interfaceFilter, res)
          }
          data = res
        } else {
          ElMessage.error('请配置完整数据源')
        }
      }
      return data
    },
    executeScript (code, data, isParams = false) {
      if (isParams) {
        const func = new Function('params', code)
        const value = func(data)
        return value
      }
      const func = new Function('data', code)
      const value = func(data)
      return value
    },
    async save () {
      if (!this.$refs.baseForm) {
        return
      }
      const valid = await this.$refs.baseForm.validate()
      if (!valid) {
        return
      }
      const data = await this.saveData()
      if (data.isBasic) {
        this.$emit('saveChartCfg', data)
      } else {
        this.$emit('saveChartConfig', data)
      }
    },
    async  saveData () {
      if (!this.$refs.baseForm) {
        return
      }
      const valid = await this.$refs.baseForm.validate()
      if (valid) {
        let data = await this.getConfigData()
        if (data.dataSourceConfig.type === 'tjb' && this.$refs.dataSourceCfg && this.$refs.dataSourceCfg.changeTjb) {
          this.$refs.dataSourceCfg.setTjbMapping()
          data = await this.getConfigData()
        }
        return data
      }
    },
    copyConfig (type) {
      const config = utils.deepClone(this.configs)
      delete config.tabList
      if (type === '1') {
        config.chartComId = uuidv4()
      }
      if (type === '1' || type === '2') {
        config.initChartId = true
      }
      if (config.varListener) {
        for (let i = 0; i < config.varListener.length; i++) {
          config.varListener[i].value = ''
        }
      }
      componentConfigs.request.copyTextToClipboard(JSON.stringify(config))
    },
    copyDataSourceConfig () {
      const data = { type: 'dataSource', data: this.configs }
      localStorage.setItem('amCopyData', JSON.stringify(data))
      ElMessage.success('复制成功')
      // componentConfigs.request.copyTextToClipboard(JSON.stringify(data))
    },
    pasteDataSourceConfig () {
      try {
        const data = JSON.parse(localStorage.getItem('amCopyData'))
        if (data && data.type === 'dataSource') {
          if (this.$refs.dataSourceCfg) {
            const dataSourceConfig = data.data.dataSourceConfig
            const newItems = data.data.items || []
            const beforeItems = this.$refs.dataSourceCfg.finalItems
            let items = beforeItems.filter(c => !c.isAdd)
            for (let i = 0; i < items.length; i++) {
              const newItem = newItems.find(c => c.field === items[i].field && !c.isAdd)
              if (newItem) {
                items[i] = newItem
              }
            }
            if (newItems) {
              items = [...items, ...newItems.filter(c => c.isAdd)]
            }
            const newMapping = {}
            if (dataSourceConfig && dataSourceConfig.dataMapping) {
              for (const key in dataSourceConfig.dataMapping) {
                if (items.find(c => c.field === key)) {
                  newMapping[key] = dataSourceConfig.dataMapping[key]
                  newMapping[key + '_filter'] = dataSourceConfig.dataMapping[key + '_filter']
                }
              }
            }
            this.$refs.dataSourceCfg.refreshConfig({ items, dataSourceConfig: { ...dataSourceConfig, dataMapping: newMapping } })
            this.$refs.dataSourceCfg.dataMappingVisible = false
            ElMessage.success('粘贴完成')
          }
        } else {
          ElMessage.info('请粘贴正确的数据源数据')
        }
      } catch (error) {
        ElMessage.info('请粘贴正确的数据源数据')
      }
    },
    handleTabClick (pane) {
      this.$emit('changeTab', pane.paneName)
    },
    saveConfig () {
      this.debounceSave()
    }
  }
}
</script>

<style lang="scss" scoped>
// @import "../../styles/index.css";
.chart-cfg-wrap {

  .el-header.title {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #1a1a1a;
    background: #fcfcfc;
    box-shadow: 0 1px 0 0 #ededed;
  }
}

.am-copy-btn {
  position: absolute;
  right: 10px;
}

:deep(.config-tab) {
  // display: flex;
  // flex-direction: column;
  height: 100%;

  >.el-tabs__header {
    margin-bottom: 0;
    background: #f7f7f7;
    border-bottom: none;
    /* 右侧留位,避免最右页签与上层「固定/关闭」图标按钮重合 */
    padding-right: 72px;

    .el-tabs__nav {
      display: flex;
      width: auto;
      border: none;
      // border-top: 1px solid #e4e7ed;
      .el-tabs__item {
        flex: 0 0 auto;
        padding: 0 16px;
        font-size: 12px;
        color: #a3a3a3;
        text-align: center;
        border: none;
      }

      .el-tabs__item.is-active {
        color: #1a1a1a;
        background: #fff;
      }
    }
  }

  .el-tabs__content {
    flex: 1;
    overflow: auto;
    // padding: 0 24px;
    background: #fff;

    .el-tab-pane {
      height: 100%;

      .base-form.ef-form-wrapper {
        box-sizing: border-box;
        height: 100%;
        padding-top: 5px;
        padding-right: 14px;

        & > .el-form > .el-row {
          // display: flex;
          // flex-direction: column;
          height: 100%;

          .el-collapse-item__header {
            height: 32px;
            line-height: 32px;
          }

          .el-col {

            &:first-child {

              .el-form-item {
                // padding-right: 12px;
                margin-top: 10px;
              }
            }

            &:nth-child(2) {

              .el-form-item {
                margin-top: 5px;
                margin-bottom: 5px;
              }
            }
          }

          > .el-col:last-child {
            // flex: 1;
            // overflow: hidden;
            >.el-form-item {
              height: 100%;

              >.el-form-item__content {
                height: 100%;
                overflow: auto;
              }
            }
          }
        }

        .el-input__wrapper {
          font-size: 12px;
        }

        .explain-config {

          .el-form > .el-row {

            .el-col {

              &:first-child {

                .el-form-item {
                  padding-right: 0;
                }
              }
            }
          }
        }
      }

      // 单容器顺排:表单 + 插槽按内容高度堆叠,由 .base-flow 统一滚动(去掉原分割容器的 height:100% 链)
      .base-flow {
        height: 100%;
        overflow-y: auto;
        overflow-x: hidden;
        box-sizing: border-box;

        .base-form.ef-form-wrapper {
          height: auto;

          & > .el-form > .el-row {
            height: auto;

            > .el-col:last-child > .el-form-item {
              height: auto;

              > .el-form-item__content {
                height: auto;
                overflow: visible;
              }
            }
          }
        }

        .custom-group {
          height: auto;
        }
      }
    }
  }

  .base-form >.el-form > .el-row>.el-col > .el-form-item {
    width: auto !important;

    .el-collapse {
      width: 100%;
    }
  }

  .am-dt-copy {
    right: 20px;
  }

  .am-dt-paste {
    right: 0;
  }

  .am-dt-btn {
    position: absolute;
    top: -2px;
    display: none;
    font-size: 12px;
  }

  .am-dt-content {
    position: relative;
    width: 100%;

    &:hover {

      .am-dt-btn {
        display: inline-block;
      }
    }
  }
}

.advanced-container {

  :deep(.el-collapse-item__header) {
    // flex-direction: row-reverse;
    justify-content: space-between;
    height: 40px;
    padding: 0 12px;
    line-height: 40px;

    .el-collapse-item__arrow {
      // margin: 0 10px;
      margin: 0 0 0 10px;
    }
  }

  :deep(.el-collapse-item__content) {
    padding: 0 12px;

    .vxe-grid {
      padding: 0;
    }
  }

  .collapse-item.var-table {
    padding: 11px;
    margin-bottom: 10px;
    background: #f7f7f7;
    border-radius: 4px;

    :deep(.el-form) {
      padding: 0;
    }
  }
}
</style>
