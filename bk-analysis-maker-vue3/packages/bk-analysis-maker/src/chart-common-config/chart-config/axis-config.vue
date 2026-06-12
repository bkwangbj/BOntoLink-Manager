<template>
  <div class="axis-config-view">
    <div style="width: 100px;">
      <el-switch
        v-model="isTransposition"
        :disabled="!saveAble"
        class="am-switch active-switch"
        size="small"
        @click.stop.prevent
        @change="$emit('chartChange')"
      />
      <span class="chart-config-title">
        坐标轴转置 </span>
    </div>
    <div class="axis-header">
      <el-tabs
        v-model="axisActive"
        class="axis-tabs"
      >
        <el-tab-pane
          label="x轴"
          name="x"
        />
        <el-tab-pane
          label="y轴"
          name="y"
        />
        <el-tab-pane
          v-if="seriesLength>1"
          label="y2轴"
          name="y2"
        />
      </el-tabs>
    </div>
    <YaxisConfig
      v-show="axisActive==='y'"
      ref="yAxis"
      @axis-change="axisChange($event,'yAxis')"
    />
    <YaxisConfig
      v-show="axisActive==='y2'"
      ref="yAxis2"
      @axis-change="axisChange($event,'yAxis2')"
    />
    <XaxisConfig
      v-show="axisActive==='x'"
      ref="xAxis"
      @axis-change="axisChange($event,'xAxis')"
    />
  </div>
</template>

<script>
import XaxisConfig from '../components/axis-config/xaxis-config.vue'
import YaxisConfig from '../components/axis-config/yaxis-config.vue'
/* stylelint-disable-next-line CssSyntaxError */
export default {
  name: 'AxisConfig',
  components: {
    XaxisConfig,
    YaxisConfig
  },
  inject: ['getSaveAble'],
  props: {
    seriesLength: {
      type: Number,
      default: 0
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      axisActive: 'x',
      isTransposition: false
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    saveFormData () {
      const yAxis = this.$refs.yAxis.saveFormData()
      const yAxis2 = this.$refs.yAxis2.saveFormData()
      const xAxis = this.$refs.xAxis.saveFormData()
      if (this.seriesLength > 1) {
        return { yAxis: [yAxis, yAxis2], xAxis, isTransposition: this.isTransposition }
      } else {
        return { yAxis, xAxis, isTransposition: this.isTransposition }
      }
    },
    setFormData ({ xAxis, yAxis, isTransposition }) {
      this.isTransposition = isTransposition
      if (Array.isArray(yAxis)) {
        this.$refs.yAxis.setFormData(yAxis[0])
        this.$refs.xAxis.setFormData(xAxis)
        this.$refs.yAxis2.setFormData(yAxis[1])
      } else {
        this.$refs.yAxis.setFormData(yAxis)
        this.$refs.xAxis.setFormData(xAxis)
      }
    },
    axisChange () {
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
@import "../../styles/index.css";

:deep(.axis-tabs) {

  .el-tabs__active-bar {
    width: 21px !important;
  }
}

.axis-config-view {
  display: flex;
  flex-direction: column;

  .axis-header {
    display: flex;
    justify-content: center;
  }
}
</style>
