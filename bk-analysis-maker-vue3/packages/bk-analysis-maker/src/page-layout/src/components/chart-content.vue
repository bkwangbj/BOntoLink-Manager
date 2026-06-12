<template>
  <EfFlexBoxCell
    v-bind="configs"
    @click="clickChart"
  >
    <el-container
      class="chart-content"
      :class="[setMode ? 'set-chart-content':'',focusFlag ? 'foucs-chart-content':'']"
    >
      <el-header
        v-if="setMode"
        class="chart-title"
      >
        <draggable
          :list="tabList"
          class="tab-content"
          style="height: 100%;"
          :group="{name:'bk-chart-tab',put:['bk-chart-com','bk-chart-tab'] }"
          item-key="chartId"
          @change="tabChange"
        >
          <div
            v-for="(tab,index) in tabList"
            :key="index"
            class="tab-btn"
            :class="currentTab == tab.chartId ? 'tab-btn-active':''"
            :label="tab.chartId"
            @click="tabClick(tab)"
          >
            {{ tab.title }}<el-button
              size="small"
              text
              class="tab-del"
              @click="delTab(tab)"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </draggable>
      </el-header>
      <el-main>
        <draggable
          v-if="setMode"
          :list="dragList"
          class="list-group"
          style="height: 100%;overflow: hidden;"
          :sort="false"
          :group="{name:'bk-chart',put:['bk-chart-com','bk-chart'] }"
          @change="change"
          @start="handleEvent('start')"
          @end="handleEvent('end')"
          @move="handleEvent('move',$event)"
        >
          <component
            :is="cfg.type"
            v-for="cfg in chartList"
            :key="cfg.chartId"
            :ref="'chart' + cfg.chartId"
            :configs="cfg"
            :set-mode="setMode"
          />
        </draggable>
        <el-tabs
          v-else
          v-model="currentTab"
          style="height: 100%;padding: 10px;"
        >
          <el-tab-pane
            v-for="chart in tabList"
            :key="chart.chartId"
            :label="chart.title"
            :name="chart.chartId"
            style="height: 100%;"
          >
            <component
              :is="chart.type"
              :configs="chart"
              :set-mode="setMode"
            />
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>
  </EfFlexBoxCell>
</template>

<script>
import draggable from 'vuedraggable'
import { v4 as uuidv4 } from 'uuid'
import emitter from '../../../configs/emitter'
import { utils } from 'efficient-suite'
import { ElMessageBox } from 'element-plus'
export default {
  name: 'ChartContent',

  components: {
    draggable
  },
  inject: ['chartConfig'],
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    setMode: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      focusFlag: false,
      currentTab: '',
      tabList: [],
      dragList: [],
      chartList: [],
      chartCfg: {},
      dragState: false,
      processSetp: 0
    }
  },
  watch: {
    currentTab () {
      if (this.setMode) {
        if (this.currentTab) {
          const data = this.tabList.find(c => c.chartId === this.currentTab)
          if (data) {
            this.chartList = [data]
            this.dragList = utils.deepClone(this.chartList)
          } else {
            this.chartList = []
            this.dragList = []
          }
        } else {
          this.chartList = []
          this.dragList = []
        }
        if (this.processSetp !== 1 && this.setMode) {
          emitter.emit('chartClick', this.chartList.length > 0 ? { ...utils.deepClone(this.configs), ...utils.deepClone(this.chartList[0]) } : {}, this.chartList.length > 0)
        }
        this.processSetp = 2
      }
    }
  },
  created () {
    emitter.on('chartClick', this.handleChartClick)
    emitter.on('changeChart', this.changeChart)
    emitter.on('saveChartCfg', this.saveChartCfg)
    if (this.configs.tabList) {
      this.tabList = utils.deepClone(this.configs.tabList)
      this.processSetp = 1
    }
    emitter.on('savePage', this.savePage)
    if (this.tabList.length > 0) {
      this.currentTab = this.tabList[0].chartId
    }
  },
  beforeUnmount () {
    emitter.off('chartClick', this.handleChartClick)
    emitter.off('changeChart', this.changeChart)
    emitter.off('saveChartCfg', this.saveChartCfg)
    emitter.off('savePage', this.savePage)
  },
  methods: {
    handleChartClick (configs) {
      this.focusFlag = this.configs.id === configs.id
    },
    clickChart () {
      if (this.setMode && !this.focusFlag) {
        emitter.emit('chartClick', this.chartList.length > 0 ? { ...utils.deepClone(this.configs), ...utils.deepClone(this.chartList[0]), id: this.configs.id } : this.configs, this.chartList.length > 0)
      }
    },
    tabClick (tab) {
      this.currentTab = tab.chartId
    },
    delTab (tab) {
      ElMessageBox.confirm('确认删除该页签吗？').then(() => {
        this.handleDelTab(tab)
      }).catch(() => {})
    },
    handleDelTab (tab) {
      this.tabList.splice(this.tabList.indexOf(tab), 1)
      if (this.tabList.length > 0) {
        this.currentTab = this.tabList[0].chartId
      } else {
        this.currentTab = ''
      }
    },
    tabChange ({ added, removed }) {
      if (added) {
        const index = this.tabList.indexOf(added.element)
        if (!added.element.chartId) {
          this.tabList[index] = { ...utils.deepClone(this.tabList[index]), chartId: uuidv4() }
        }
        this.tabList[index].id = this.configs.id
        this.currentTab = this.tabList[index].chartId
      }
      if (removed && removed.element) {
        if (this.tabList.length > 0) {
          this.currentTab = this.tabList[0].chartId
        } else {
          this.currentTab = ''
        }
      }
    },
    change ({ added, removed }) {
      if (added) {
        const beforeData = utils.deepClone(this.chartList)
        this.updateTabContent(added.element)
        this.dragState = false
        emitter.emit('changeChart', beforeData)
      }
      if (removed && removed.element) {
        this.dragState = true
      }
      // this.clickChart()
    },
    handleEvent (type) {
      if (type === 'start') {
        this.dragState = true
      } else if (type === 'end') {
        this.dragState = false
      }
    },
    changeChart (data) {
      if (this.dragState) {
        if (data.length > 0) {
          this.updateTabContent(data[0])
        } else {
          this.handleDelTab(this.tabList.find(c => c.chartId === this.currentTab))
        }
        // this.chartList = data
        // this.dragList = utils.deepClone(this.chartList)
        this.dragState = false
      }
    },
    updateTabContent (newTab) {
      if (this.currentTab) {
        const index = this.tabList.findIndex(c => c.chartId === this.currentTab)
        const tab = utils.deepClone(newTab)
        if (!tab.chartId) {
          tab.chartId = uuidv4()
        }
        tab.id = this.configs.id
        this.tabList[index] = tab
        this.currentTab = tab.chartId
      } else {
        this.tabList = [{ ...utils.deepClone(newTab), id: this.configs.id }]
        if (!this.tabList[0].chartId) {
          this.tabList[0].chartId = uuidv4()
        }
        this.currentTab = this.tabList[0].chartId
      }
    },
    saveChartCfg (configs) {
      if (this.currentTab === configs.chartId) {
        const index = this.tabList.findIndex(c => c.chartId === this.currentTab)
        this.tabList[index] = { ...utils.deepClone(this.tabList[index]), ...utils.deepClone(configs) }
        this.chartList = [this.tabList[index]]
        this.dragList = utils.deepClone(this.chartList)
        this.$nextTick(() => {
          try {
            this.$refs['chart' + configs.chartId][0].resetChart(this.tabList[index])
          } catch (error) {
            console.log(configs.type + '组件未实现resetChart方法')
          }
        })
      }
    },
    savePage () {
      this.chartConfig.setConfig(this.configs.id, this.tabList)
    }
  }
}
</script>

<style lang="scss" scoped>
:deep() {

  .el-tabs__content {
    height: calc(100% - 50px);
  }
}

.chart-content {
  width: 100%;
  height: 100%;
  border: 1px solid #e8eaec;
}

.set-chart-content {

  &:hover {
    cursor: pointer;
    background-color: #f8f8f8;
  }
}

.foucs-chart-content {
  border: 1px solid #1777f5;
}

.chart-title {
  position: relative;
  display: flex;
  align-items: center;
  height: 35px !important;
  margin: 5px 0 10px !important;
  border-bottom: 1px solid #e8eaec;
}

.tab-content {
  // border: 1px solid #e8eaec;
  // border-bottom: none;
  position: relative;
  bottom: -1px;
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  padding-left: 10px;

  .tab-btn {
    position: relative;
    padding: 3px 10px 0;
    cursor: pointer;
    border-bottom: 1px solid transparent;

    &:hover {
      color: #1777f5;

      .tab-del {
        display: inline-block;
      }
    }
  }

  .tab-btn-active {
    position: relative;
    // border-bottom: 1px solid #1777f5;
    color: #1777f5;
    border-top: 1px solid #e8eaec;
    border-right: 1px solid #e8eaec;
    border-bottom-color: #fff;
    border-left: 1px solid #e8eaec;
    border-top-left-radius: 4px;
    border-top-right-radius: 4px;

    .tab-del {
      display: inline-block;
    }
  }

  .tab-del {
    position: relative;
    bottom: 2px;
    left: 3px;
    display: none;
  }
}
</style>
