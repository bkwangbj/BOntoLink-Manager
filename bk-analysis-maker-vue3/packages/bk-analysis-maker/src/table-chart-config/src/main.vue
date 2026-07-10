<template>
  <BKBasicChartConfig
    ref="basicChartConfig"
    :configs="configs"
    :page-config="pageConfig"
    :save-able="saveAble"
    :items="items"
    :rules="rules"
    :tjb-u-r-l="tjbURL"
    v-bind="$attrs"
    @change-branch-type="changeBranchType"
    @set-expand="$emit('setExpand')"
    @save-chart-config="saveChartCfg"
    @config-option-init="configOptionInit"
    @build-chart-series-data="buildChartData"
  >
    <div class="table-config-content">
      <SidebarTabs
        v-model="activeConfig"
        :menu="chartMenu"
      />
      <div class="table-config-table">
        <div
          v-show="activeConfig==='basic'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"

            @click="copyConfig('tableBasic')"
          >
            复制基础配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('tableBasic')"
          >
            粘贴基础配置
          </el-button>
        </div>
        <ChartTypeChange
          v-show="activeConfig==='basic'"
          style="margin-bottom: 10px;"
          :type="configs.branchType"
          @change-type="changeChartType"
        />
        <tablePropsConfig
          v-show="activeConfig==='basic'"
          ref="propsConfig"
          @paste-config="pasteConfig"
          @props-change="getBasicConfigData"
        />
        <div
          v-show="activeConfig==='columns'"
          class="copy-button-box"
        >
          <el-button
            type="primary"
            size="small"
            @click="copyConfig('tableColumns')"
          >
            复制列配置
          </el-button>
          <el-button
            type="primary"
            size="small"
            :disabled="!saveAble"
            @click="pasteConfig('tableColumns')"
          >
            粘贴列配置
          </el-button>
        </div>
        <tableColumnsConfig
          v-show="activeConfig==='columns'"
          ref="columnConfigRef"
          :save-able="saveAble"
          @sync-column="syncColumn"
          @chart-change="getBasicConfigData"
        />
      </div>
    </div>
  </BKBasicChartConfig>
</template>

<script>
import { commonItems, commonRules } from '../../configs/common-item'
import SidebarTabs from '../../chart-common-config/components/sidebar-tabs.vue'
import tablePropsConfig from './components/table-props-config.vue'
import tableColumnsConfig from './components/table-columns-config.vue'
import ChartTypeChange from '../../chart-common-config/components/chart-type-change.vue'
import { getColumns } from '../../configs/common-func'
import Copy from '../../configs/copy-chart-mixins'
import { utils } from 'efficient-suite'
import { ElMessage } from 'element-plus'
import emitter from '../../configs/emitter'
export default {
  name: 'TableChartConfig',
  components: {
    SidebarTabs,
    tablePropsConfig,
    tableColumnsConfig,
    ChartTypeChange
  },
  mixins: [Copy],
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
    tjbURL: {
      type: String,
      default: ''
    }
  },
  emits: ['setExpand', 'saveChartCfg'],
  data () {
    return {
      items: commonItems.filter(c => c.isBasic),
      rules: commonRules,
      columns: [],
      activeConfig: 'basic',
      chartMenu: [{ name: '表格', key: 'basic', icon: 'ri-table-line' }, { name: '表格列', key: 'columns', icon: 'ri-list-ordered' }]
    }
  },
  created () {
  },
  methods: {
    getTypeText (value, list) {
      const data = list.find(c => c.value === value)
      return data ? data.label : ''
    },
    async buildChartData (type, data) {
      const dataSourceConfig = this.$refs.basicChartConfig.getDataSourceConfig()
      let key = 'xlabel'
      if (dataSourceConfig.dataMapping && dataSourceConfig.dataMapping.xlabel) {
        key = dataSourceConfig.dataMapping.xlabel
      }
      this.columns = await getColumns(data, this.configs, key, dataSourceConfig)

      this.$refs.columnConfigRef.setColumns(this.columns)
      this.getBasicConfigData()
      ElMessage.success('已生成列配置')
    },
    getBasicConfigData (field) {
      this.$nextTick(() => {
        if (field === 'align' || field === 'headerAlign') {
          const { props } = this.$refs.propsConfig.saveFormData()
          this.$refs.columnConfigRef.setAlign(field, props[field])
        }
        this.$refs.basicChartConfig.saveConfig()
      })
    },
    changeChartType (config) {
      this.$refs.basicChartConfig.changeChartType(config)
    },
    changeBranchType (config) {
      this.$emit('saveChartCfg', config)
      this.$nextTick(() => {
        this.configOptionInit()
      })
    },
    configOptionInit () {
      this.$nextTick(() => {
        if (!this.$refs.propsConfig) {
          return
        }
        this.$refs.propsConfig.setFormData({ styles: this.configs.configOption?.styles || {}, props: this.configs.configOption?.props || {} })
        this.columns = utils.deepClone(this.configs.configOption?.columns)
        this.$refs.columnConfigRef.setColumns(this.columns)
        this.$refs.columnConfigRef.setFilterDataEmpty(this.configs.configOption?.filterDataEmpty || false)
      })
    },
    syncColumn () {
      emitter.emit('syncColumn', this.configs.chartId)
    },
    beforeEditMethod () {
      return this.saveAble
    },
    async saveChartCfg (data) {
      this.$nextTick(() => {
        const props = this.$refs.propsConfig.saveFormData()
        const { columns, filterDataEmpty } = this.$refs.columnConfigRef.saveFormData()
        const config = utils.deepClone(data)
        config.configOption = { ...props, columns, filterDataEmpty }
        this.$emit('saveChartCfg', config)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
// @import "../../styles/index.css";
.table-config-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  .table-config-tools {
    // padding: 5px;
    // border-top: 1px solid #e8eaec;
    display: flex;
    justify-content: flex-end;
  }

  .table-config-table {
    flex: 1;
    padding: 10px;
    overflow: auto;
  }
}
</style>
