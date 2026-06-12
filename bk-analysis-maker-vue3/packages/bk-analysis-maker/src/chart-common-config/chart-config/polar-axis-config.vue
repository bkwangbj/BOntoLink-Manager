<template>
  <div class="axis-config-view">
    <div class="axis-header">
      <el-tabs
        v-model="axisActive"
        class="axis-tabs"
      >
        <el-tab-pane
          label="角度轴"
          name="x"
        />
        <el-tab-pane
          label="径向轴"
          name="y"
        />
      </el-tabs>
    </div>
    <YaxisConfig
      v-show="axisActive==='y'"
      ref="yAxis"
      @axis-change="axisChange($event,'yAxis')"
    />
    <XaxisConfig
      v-show="axisActive==='x'"
      ref="xAxis"
      @axis-change="axisChange($event,'xAxis')"
    />
  </div>
</template>

<script>
import XaxisConfig from '../components/polar-axis-config/xaxis-config.vue'
import YaxisConfig from '../components/polar-axis-config/yaxis-config.vue'
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
      const radiusAxis = this.$refs.yAxis.saveFormData()
      const angleAxis = this.$refs.xAxis.saveFormData()

      return { angleAxis, radiusAxis }
    },
    setFormData (form) {
      const { angleAxis, radiusAxis } = form
      this.$refs.yAxis.setFormData(radiusAxis)
      this.$refs.xAxis.setFormData(angleAxis)
    },
    axisChange () {
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
@import "../../styles/index.css";

.axis-config-view {
  display: flex;
  flex-direction: column;

  .axis-header {
    display: flex;
    justify-content: center;
  }
}
</style>
