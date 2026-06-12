<template>
  <EfModal
    v-model="modalVisible"
    width="800px"
    height="600px"
    title="页面卡片配置"
    @close="$emit('close')"
  >
    <div class="script-panel">
      <div class="script-content">
        <div class="code-content">
          <BKCodeCom
            ref="editor"
            v-model="scriptContent"
            language="json"
          />
        </div>
      </div>
      <div class="analysis-modal-footer">
        <el-button
          type="primary"
          size="small"
          :loading="loading"
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
import { ElMessage } from 'element-plus'
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: true
    }
  },
  emits: ['close', 'save', 'refresh'],
  data () {
    return {
      loading: false,
      scriptContent: '',
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs-dark', overviewRulerBorder: false
      },
      preThemeList: [],
      modalVisible: false
    }
  },
  watch: {
    visible:
    {
      handler () {
        this.modalVisible = this.visible
        if (this.modalVisible) {
          this.$nextTick(() => {
            this.init()
          })
        }
      },
      immediate: true
    }
  },
  created () {
  },
  mounted () {
  },
  methods: {
    init () {
      this.scriptContent = JSON.stringify({
        config: '',
        chartList: []
      })
    },
    save () {
      this.loading = true
      try {
        const theme = JSON.parse(this.scriptContent)
        this.$emit('save', theme, () => {
          this.loading = false
          ElMessage.success('保存成功')
          this.$emit('refresh')
          this.$emit('close')
        })
      } catch (error) {
        ElMessage.error('请输入正确的数据格式')
      }
    }
  }
}

</script>

<style lang="scss" scoped>
.script-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  .script-content {
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
    top: -1px;
  }
}
</style>
