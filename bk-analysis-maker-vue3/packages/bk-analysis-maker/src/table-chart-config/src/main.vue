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
    <div class="cfg-anchor-wrap">
      <div class="cfg-anchor-nav">
        <div
          v-for="t in chartMenu"
          :key="t.key"
          :class="['cfg-anchor-item', activeConfig===t.key && 'is-on']"
          @click="scrollToSec(t.key)"
        >
          <i class="am-iconfont cfg-anchor-ic" :class="t.icon" />
          <span>{{ t.name }}</span>
        </div>
      </div>
      <div class="table-config-table cfg-scroll" ref="cfgScroll">
        <div id="cfgsec-basic" class="cfg-sec">
          <div class="cfg-sec-title">表格</div>
          <div class="copy-button-box">
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
            style="margin-bottom: 10px;"
            :type="configs.branchType"
            @change-type="changeChartType"
          />
          <tablePropsConfig
            ref="propsConfig"
            @paste-config="pasteConfig"
            @props-change="getBasicConfigData"
          />
        </div>
        <div id="cfgsec-columns" class="cfg-sec">
          <div class="cfg-sec-title">表格列</div>
          <div class="copy-button-box">
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
            ref="columnConfigRef"
            :save-able="saveAble"
            @sync-column="syncColumn"
            @chart-change="getBasicConfigData"
          />
        </div>
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
  mounted () {
    this.$nextTick(() => {
      this.scrollEl = this.$refs.cfgScroll?.closest('.base-flow') || this.$refs.cfgScroll
      if (this.scrollEl) this.scrollEl.addEventListener('scroll', this.onCfgScroll, { passive: true })
    })
  },
  beforeUnmount () {
    if (this.scrollEl) this.scrollEl.removeEventListener('scroll', this.onCfgScroll)
  },
  methods: {
    scrollToSec (key) {
      const sc = this.scrollEl
      const el = document.getElementById('cfgsec-' + key)
      if (sc && el) {
        this.activeConfig = key
        const top = el.getBoundingClientRect().top - sc.getBoundingClientRect().top + sc.scrollTop
        sc.scrollTo({ top: Math.max(0, top - 8), behavior: 'smooth' })
      }
    },
    onCfgScroll () {
      const sc = this.scrollEl
      if (!sc) return
      const base = sc.getBoundingClientRect().top + 24
      let cur = this.chartMenu[0].key
      for (const t of this.chartMenu) {
        const el = document.getElementById('cfgsec-' + t.key)
        if (el && el.getBoundingClientRect().top <= base) cur = t.key
      }
      this.activeConfig = cur
    },
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
/* 锚点导航布局:左侧固定锚点 + 右侧滚动配置(依次往下排) */
.cfg-anchor-wrap {
  display: flex;
  align-items: flex-start;
}
.cfg-anchor-nav {
  position: sticky;
  top: 0;
  z-index: 2;
  flex-shrink: 0;
  width: 62px;
  padding: 8px 0;
  box-sizing: border-box;
  background: #fafbfc;
  border-right: 1px solid #eef0f3;
  align-self: flex-start;
}
.cfg-anchor-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  font-size: 12px;
  color: #86909c;
  cursor: pointer;
  border-left: 2px solid transparent;
  transition: color .15s, background .15s;
}
.cfg-anchor-item:hover { color: #1f6aff; }
.cfg-anchor-item.is-on {
  color: #1f6aff;
  background: #eef4ff;
  border-left-color: #1f6aff;
}
.cfg-anchor-ic { font-size: 16px; }
.cfg-sec + .cfg-sec {
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}
.cfg-sec-title {
  margin: 6px 0 12px;
  padding-left: 8px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  color: #1a1a1a;
  border-left: 3px solid #1f6aff;
}
.table-config-table {
  flex: 1;
  min-width: 0;
  padding: 12px 16px 12px 12px;
}
</style>
