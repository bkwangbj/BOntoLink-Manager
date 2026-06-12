<template>
  <div class="oper-list">
    <el-button
      v-if="showClear"
      title="清除事件参数"
      size="small"
      text
      class="oper-btn"
      @click.stop="handleClick({operatorType: 'clear'})"
    >
      <el-icon><RefreshRight /></el-icon>
    </el-button>
    <el-button
      v-for="(oper, i) in operatorList"
      :key="i"
      :title="oper.name"
      size="small"
      text
      class="oper-btn"
      @click.stop="handleClick(oper)"
    >
      <el-icon><component :is="oper.icon" /></el-icon>{{ oper.icon ? '' :oper.name }}
    </el-button>
  </div>
</template>

<script>
import emitter from '../../../configs/emitter'
import { utils } from 'efficient-suite'
export default {

  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    showClear: {
      type: Boolean,
      default: false
    },
    configNode: {
      type: Object,
      default: () => { return { label: '' } }
    }
  },
  emits: ['download', 'sort', 'restore', 'clearParmas'],
  data () {
    return {
      operatorList: []
    }
  },
  watch: {
    configs: {
      handler () {
        this.operatorList = this.configs.operatorConfig
          ? utils.deepClone(this.configs.operatorConfig.filter(c => c.isShow === '1'))
          : []
      },
      immediate: true
    }
  },
  methods: {
    handleClick (oper) {
      if (oper.operatorType === 'custom' && oper.scriptContent) {
        this.executeScript(oper.scriptContent)
      } else if (oper.operatorType === 'download') {
        this.$emit('download', this.configs)
      } else if (oper.operatorType === 'sort') {
        this.$emit('sort', this.configs, oper)
      } else if (oper.operatorType === 'restore') {
        this.$emit('restore', this.configs, oper)
      } else if (oper.operatorType === 'clear') {
        this.$emit('clearParmas', this.configs)
      } else if (oper.operatorType === 'detail' && oper.openPage) {
        emitter.emit('openPageModal', oper.openPage)
      }
    },
    executeScript (code) {
      const func = new Function('config', '$this', code)
      func(this.configs, window['$amObject' + this.configNode?.id])
    }
  }
}
</script>

<style lang="scss" scoped>
.oper-list {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-right: 10px;
}

</style>
