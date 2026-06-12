<template>
  <t-color-picker
    v-bind="$attrs"
    v-model="value"
    size="small"
    enable-alpha
    format="RGBA"
    :color-modes="colorModes"
    :recent-colors="null"
    :swatch-colors="null"
    :disabled="!saveAble || !editAble"
    class="common-color-input"
    @change=" colorChange"
  />
</template>

<script>
export default {
  name: 'CommonColorPicker',
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    },
    // 'monochrome', 'linear-gradient'
    colorModes: {
      type: Array,
      default: () => ['monochrome']
    },
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['change', 'update:modelValue'],
  data () {
    return {
      value: ''
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    modelValue: {
      handler (nv, ov) {
        this.value = this.modelValue
      },
      immediate: true
    }
  },
  methods: {
    colorChange (e, w) {
      this.$nextTick(() => {
        this.$emit('update:modelValue', this.value ? this.value : null)
        this.$emit('change', this.value ? this.value : null)
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.common-color-input {
  max-width: 100%;
  overflow: hidden;
  line-height: 28px;

  :deep(.t-input__wrap) {
    width: calc(100% - 18px) !important;
    height: 28px;
    line-height: 28px;
    text-align: left;
    background: transparent;
    border: none;

    &:focus {
      background: #fff;
    }
  }

  :deep(.t-color-picker__trigger--default) {
    display: flex;

    .t-input--auto-width {
      width: 100%;

      .t-input {
        height: 28px;
        line-height: 28px;
        background: #ededed;
        border-color: transparent;
        border-radius: 4px;

        &.t-is-disabled {
          background: #f5f7fa;
        }

        &.t-is-focused {
          background: #fff;
          border: 1px solid #409efe;
          border-radius: 4px;
          box-shadow: none;

          .t-input__prefix {
            background: transparent;
          }
        }

        .t-input__prefix {
          background: transparent;
        }
      }
    }
  }
}

:deep(.el-select) {

  .el-input__prefix {
    top: 2px;
    left: 2px;
    box-sizing: border-box;
    display: inline-block;
    width: calc(100% - 3px);
    height: calc(100% - 3px);
    padding: 0 15px;
    color: #606266;
    cursor: pointer;
    background-color: #fff;
    background-image: none;
    border-radius: 4px;
    outline: 0;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
    appearance: none;
  }
}

.color-select__control {
  position: relative;
  box-sizing: border-box;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 26px;
  margin: 0;
  line-height: 20px;
  cursor: pointer;
  background: rgb(247 247 247);
  border: none;
  border-radius: 4px;
  outline: 0 !important;
  transition: all 0.2s ease 0s;
  -webkit-box-align: center;
  -webkit-box-pack: justify;

  &:focus-within {
    background: rgb(246 246 246);
    border: 1px solid #1f6aff;
  }

  .color-contaniner {
    position: relative;
    box-sizing: border-box;
    display: grid;
    flex: 1 1 0%;
    flex-wrap: wrap;
    align-items: center;
    height: 26px;
    padding: 0;
    margin-left: 8px;
    overflow: hidden;
    -webkit-box-align: center;

    .color-view {
      box-sizing: border-box;
      display: flex;
      height: 100%;
      padding: 6px 0;

      .color-item {
        width: 12px;
        height: 100%;
      }
    }
  }
}

.color-view-dropdown {
  box-sizing: border-box;
  display: flex;
  padding: 6px;

  .color-item {
    width: 12px;
    height: 12px;
  }
}

.color-view {
  box-sizing: border-box;
  display: flex;
  height: 100%;
  padding: 6px 0;

  .color-item {
    width: 12px;
    height: 100%;
  }
}
</style>
