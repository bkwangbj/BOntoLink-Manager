<template>
  <div class="grid-config">
    <div style="width: 80px;">
      <el-switch
        v-model="form.show"
        :disabled="!saveAble"
        class="am-switch active-switch"
        size="small"
        @click.stop.prevent
        @change="$emit('chartChange')"
      />
      <span class="chart-config-title">
        图表边距  </span>
    </div>
    <el-form
      label-width="80px"
      size="small"
      inline
    >
      <el-row>
        <el-col :span="24">
          <PaddingBox
            ref="paddingBoxRef"
            :disabled="!form.show||!saveAble"
            @click.stop
            @change="girdPositionChange"
          />
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script>
import PaddingBox from '../../analysis-maker/src/components/padding-box.vue'

export default {
  name: 'GridConfig',
  components: {
    PaddingBox

  },
  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      form: {
        show: false
      },
      position: {}
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    form: {
      handler (nv, ov) {
      },
      deep: false
    }
  },
  methods: {
    girdPositionChange (e) {
      this.position = { top: e.paddingTop, bottom: e.paddingBottom, left: e.paddingLeft, right: e.paddingRight }
      this.$emit('chartChange')
    },
    setFormData (e) {
      Object.assign(this.$data, this.$options.data())

      if (Object.keys(e).length !== 0) {
        this.form = { ...e, show: true }
        this.$refs.paddingBoxRef.setPadding({ paddingTop: e.top, paddingBottom: e.bottom, paddingLeft: e.left, paddingRight: e.right })
      }
    },
    saveFormData () {
      if (this.form.show) {
        return { ...this.form, ...this.position, show: false }
      } else {
        return {}
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.grid-config {
  display: flex;
  margin-bottom: 10px;
}
</style>
