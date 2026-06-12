<template>
  <EfModal
    v-model="modalVisible"
    width="800px"
    height="600px"
    :title="'过滤器设置-' + (item.label || item.field)"
    @close="$emit('close')"
  >
    <div class="filter-panel">
      <div class="filter-content">
        <div class="code-bg mtk1">
          <span class="mtk8">function</span> filter(data) {
        </div>
        <div class="code-content">
          <BKCodeCom
            ref="editor"
            v-model="filterScript"
            language="javascript"
            :readonly="!setMode"
            method-name="filter(data)"
          />
        </div>
        <div class="code-bg mtk1 code-close">
          }
        </div>
      </div>
      <div class="analysis-modal-footer">
        <el-button
          type="primary"
          size="small"
          :disabled="!setMode"
          @click="save"
        >
          保存
        </el-button>
        <el-button
          size="small"
          class="analysis-modal-close-button"
          @click="$emit('close')"
        >
          关闭
        </el-button>
      </div>
    </div>
  </EfModal>
</template>
<script>
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: true
    },
    item: {
      type: Object,
      default: () => {}
    },
    mappingData: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['close', 'save'],
  data () {
    return {
      filterScript: '',
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs-dark', overviewRulerBorder: false
      },
      modalVisible: false
    }
  },
  watch: {
    visible:
    {
      handler () {
        this.modalVisible = this.visible
      },
      immediate: true
    }
  },
  created () {
    this.filterScript = this.mappingData[`${this.item.field}_filter`] || 'return data;'
  },
  mounted () {

  },
  methods: {
    save () {
      this.$emit('save', this.filterScript, this.item)
      this.$emit('close')
    }
  }
}

</script>

<style lang="scss" scoped>
.filter-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow: hidden;

  .filter-content {
    position: relative;
    display: flex;
    flex: 1;
    flex-direction: column;
    padding: 10px 20px 0 0;
  }

  .code-bg {
    padding-left: 20px;
    background-color: #1e1e1e;
  }

  .code-content {
    flex: 1;
  }

  .code-close {
    position: relative;
    top: -4px;
  }
}
</style>
