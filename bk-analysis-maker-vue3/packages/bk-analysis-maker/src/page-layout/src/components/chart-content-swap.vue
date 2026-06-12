<template>
  <EfFlexBoxCell
    v-bind="configs"
    @click="clickChart"
  >
    <el-container
      class="chart-content"
      :class="[setMode ? 'set-chart-content':'',focusFlag ? 'foucs-chart-content':'']"
    >
      <el-header class="chart-title">
        {{ chartCfg.title || '标题' }}
      </el-header>
      <el-main>
        <draggable
          v-if="setMode"
          :list="dragList"
          class="list-group"
          style="height: 100%;"
          :sort="false"
          :group="{name:'bk-chart' }"
          :handle="setMode?'':'.handle'"
          item-key="chartId"
          @change="change"
          @start="handleEvent('start')"
          @end="handleEvent('end')"
          @move="handleEvent('move')"
        >
          <component
            :is="cfg.type"
            v-for="cfg in chartList"
            :key="cfg.id"
            :configs="configs"
            :set-mode="setMode"
          />
        </draggable>
        <component
          :is="chartCfg.type"
          v-else
          :configs="configs"
          :set-mode="setMode"
        />
      </el-main>
    </el-container>
  </EfFlexBoxCell>
</template>

<script>
import draggable from 'vuedraggable'
import { utils } from 'efficient-suite'
import emitter from '../../../configs/emitter'
export default {
  name: 'ChartContent',
  components: {
    draggable
  },
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
      dragList: [],
      chartList: [],
      chartCfg: {},
      dragState: false
    }
  },
  created () {
    emitter.on('chartClick', this.handleChartClick)
    emitter.on('changeChart', this.changeChart)
    if (!this.setMode) {
      this.chartCfg = this.configs.chartCfg
    }
  },
  beforeUnmount () {
    emitter.off('chartClick', this.handleChartClick)
    emitter.off('changeChart', this.changeChart)
  },
  methods: {
    handleChartClick (configs) {
      this.focusFlag = this.configs.id === configs.id
    },
    clickChart () {
      if (this.setMode && !this.focusFlag) {
        emitter.emit('chartClick', this.configs, this.chartList.length > 0)
      }
    },
    change ({ added, removed }) {
      if (added) {
        const beforeData = utils.deepClone(this.chartList)
        this.chartList = [{ ...utils.deepClone(added.element) }]
        this.dragList = utils.deepClone(this.chartList)
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
        this.chartList = data
        this.dragList = utils.deepClone(this.chartList)
        this.dragState = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
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
    display: flex;
    align-items: center;
    height: 40px !important;
    border-bottom: 1px solid #e8eaec;
  }
</style>
