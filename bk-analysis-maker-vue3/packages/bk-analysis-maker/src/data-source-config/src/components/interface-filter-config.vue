<template>
  <div class="filter-panel">
    <div class="filter-content">
      <div
        v-if="!type"
        class="code-bg mtk1"
      >
        <span class="mtk8">function</span>{{ methodName }}{
      </div>
      <div class="code-content">
        <BKCodeCom
          ref="editor"
          v-model="filterScript"
          :language="type ? 'json' :'javascript'"
          :class="type ? '':'has-end'"
          :readonly="!setMode"
          :is-modal="type ? false : false"
          :method-name="type ? '':methodName"
          @blur="$emit('blur')"
        />
      </div>
      <div
        v-if="!type"
        class="code-bg mtk1 code-close"
      >
        }
      </div>
    </div>
    <!-- <div
      class="analysis-modal-footer"
      style="border-top: none;"
    >
      <el-button
        type="primary"
        @click="save"
        size="small"
        :disabled="!setMode"
      >
        保存
      </el-button>
      <el-button
        @click="$emit('close')"
        size="small"
        class="analysis-modal-close-button"
      >
        关闭
      </el-button>
    </div>
  </div> -->
  </div>
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
    interfaceFilter: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: ''
    },
    methodName: {
      type: String,
      default: ' filter(data, params) '
    },
    returnText: {
      type: String,
      default: 'return data;'
    }
  },
  emits: ['close', 'save', 'blur'],
  data () {
    return {
      filterScript: '',
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs', overviewRulerBorder: false
      }
    }
  },
  watch: {
    filterScript () {
      this.$emit('save', this.filterScript)
    },
    interfaceFilter () {
      if (this.interfaceFilter !== this.filterScript) {
        this.filterScript = this.interfaceFilter || (this.type ? '{}' : this.returnText)
      }
    }
  },
  created () {
    this.filterScript = this.interfaceFilter || (this.type ? '{}' : this.returnText)
  },
  mounted () {

  },
  methods: {
    save () {
      this.$emit('save', this.filterScript)
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
  height: 300px;

  .filter-content {
    position: relative;
    display: flex;
    flex: 1;
    flex-direction: column;
    border: 1px solid #ededed;
    border-radius: 4px;

    :deep(.has-end) {

      .buttons {
        bottom: -10px;
        z-index: 99;
      }
    }
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
