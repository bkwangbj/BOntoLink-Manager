<template>
  <div class="padding-box-wrapper">
    <div class="inner-box" />
    <div
      class="value-top value-container-h"
      :style="{cursor: disabled ? 'not-allowed' : 'pointer'}"
    >
      <EfInput
        v-show="showTop"
        ref="inputTop"
        v-model="paddingTop"
        class="text"
        is-number
        :disabled="disabled"
        size="small"
        @blur="blurInput('Top')"
      />
      <span
        v-show="!showTop"
        class="text"
        :style="{cursor: disabled ? 'auto' : 'pointer'}"
        @click="showInput('Top')"
      >{{ paddingTop }}</span>
    </div>
    <div
      class="value-bottom value-container-h"
      :style="{cursor: disabled ? 'auto' : 'pointer'}"
    >
      <EfInput
        v-show="showBottom"
        ref="inputBottom"
        v-model="paddingBottom"
        class="text"
        is-number
        :disabled="disabled"
        size="small"
        @blur="blurInput('Bottom')"
      />
      <span
        v-show="!showBottom"
        class="text"
        :style="{cursor: disabled ? 'auto' : 'pointer'}"
        @click="showInput('Bottom')"
      >{{ paddingBottom }}</span>
    </div>
    <div
      class="value-left value-container-v"
      :style="{cursor: disabled ? 'auto' : 'pointer'}"
    >
      <EfInput
        v-show="showLeft"
        ref="inputLeft"
        v-model="paddingLeft"
        class="text"
        is-number
        :disabled="disabled"
        size="small"
        @blur="blurInput('Left')"
      />
      <span
        v-show="!showLeft"
        class="text"
        :style="{cursor: disabled ? 'auto' : 'pointer'}"
        @click="showInput('Left')"
      >{{ paddingLeft }}</span>
    </div>
    <div
      class="value-right value-container-v"
      :style="{cursor: disabled ? 'auto' : 'pointer'}"
    >
      <EfInput
        v-show="showRight"
        ref="inputRight"
        v-model="paddingRight"
        class="text"
        is-number
        :disabled="disabled"
        size="small"
        @blur="blurInput('Right')"
      />
      <span
        v-show="!showRight"
        class="text"
        :style="{cursor: disabled ? 'auto' : 'pointer'}"
        @click="showInput('Right')"
      >{{ paddingRight }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PaddingBox',
  props: {
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['change'],
  data () {
    return {
      paddingTop: 0,
      showTop: false,
      paddingBottom: 0,
      showBottom: false,
      paddingLeft: 0,
      showLeft: false,
      paddingRight: 0,
      showRight: false
    }
  },
  methods: {
    setPadding (option) {
      const { paddingTop, paddingBottom, paddingLeft, paddingRight } = option ?? {
        paddingTop: 0,
        paddingBottom: 0,
        paddingLeft: 0,
        paddingRight: 0
      }
      this.paddingTop = paddingTop
      this.paddingBottom = paddingBottom
      this.paddingLeft = paddingLeft
      this.paddingRight = paddingRight
    },
    showInput (text) {
      if (this.disabled) return
      this['show' + text] = true
      this.$nextTick(() => {
        this.$refs['input' + text] && this.$refs['input' + text].focus()
      })
    },
    blurInput (text) {
      this['show' + text] = false
      if (!this['padding' + text] || Number(this['padding' + text]) < 0) {
        this['padding' + text] = 0
      }
      this.$emit('change', {
        paddingTop: this.paddingTop,
        paddingBottom: this.paddingBottom,
        paddingLeft: this.paddingLeft,
        paddingRight: this.paddingRight
      })
    },
    handleMouseDown (e, text) {
      this.draging = true
      this.currentText = text
      this.clientX = e.clientX
      this.clientY = e.clientY
      document.addEventListener('mousemove', this.handleMouseMove)
      document.addEventListener('mouseup', this.handleMouseUp)
    },
    handleMouseUp () {
      this.draging = false
      document.removeEventListener('mousemove', this.handleMouseMove)
      document.removeEventListener('mouseup', this.handleMouseUp)
    },
    handleMouseMove (e) {
      switch (this.currentText) {
        case 'Top':
        case 'Bottom':
          if (e.clientY < this.clientY) {
            // 往上移动鼠标
            this['padding' + this.currentText] = this['padding' + this.currentText] + 1
          } else if (e.clientY > this.clientY) {
            // 往下移动鼠标
            this['padding' + this.currentText] = this['padding' + this.currentText] - 1
          }
          break
        case 'Left':
        case 'Right':
          if (e.clientX < this.clientX) {
            // 往左移动鼠标
            this['padding' + this.currentText] = this['padding' + this.currentText] - 1
          } else if (e.clientX > this.clientX) {
            // 往右移动鼠标
            this['padding' + this.currentText] = this['padding' + this.currentText] + 1
          }
          break
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.padding-box-wrapper {
  position: relative;
  width: 190px;
  height: 129px;
  font-size: 12px;
  background: #ededed;
  border-radius: 4px;

  :deep(.el-input__wrapper) {
    background: #fff !important;
  }

  .inner-box {
    position: absolute;
    top: calc(50% - 10px);
    left: calc(50% - 20px);
    width: 40px;
    height: 20px;
    background: #ededed;
    border: 0.5px solid #c7c7c7;
    border-width: 1px;
    border-radius: 4px;
  }

  .value-container-h {
    position: absolute;
    width: 100%;
    height: 30px;
    line-height: 14px;
    user-select: none;

    .text {
      position: absolute;
      line-height: 18px;
      cursor: pointer;
    }

    .text.ef-input-wrapper {
      width: 40px;

      :deep(.el-input__inner) {
        padding: 0 5px;
      }
    }
  }

  .value-top {
    top: 0;
    // cursor: ns-resize;
    left: 0;

    .text {
      top: 10px;
      left: 50%;
      transform: translateX(-50%);
    }

    &::after {
      position: absolute;
      top: 40px;
      left: calc(50% - 1px);
      width: 2px;
      height: 6px;
      content: "";
      background: #a3a3a3;
      border-radius: 2px;
    }
  }

  .value-bottom {
    bottom: 0;
    // cursor: ns-resize;
    left: 0;

    .text {
      bottom: 6px;
      left: 50%;
      transform: translateX(-50%);
    }

    &::after {
      position: absolute;
      bottom: 38px;
      left: calc(50% - 1px);
      width: 2px;
      height: 6px;
      content: "";
      background: #a3a3a3;
      border-radius: 2px;
    }
  }

  .value-container-v {
    position: absolute;
    // cursor: ew-resize;
    width: 30%;
    height: 100%;
    line-height: 14px;
    text-align: center;
    user-select: none;

    .text.ef-input-wrapper {
      width: 40px;

      :deep(.el-input__inner) {
        padding: 0 5px;
      }
    }

    .text {
      position: absolute;
      top: 50%;
      display: inline-block;
      text-align: center;
      cursor: pointer;
    }
  }

  .value-left {
    top: 0;

    .text {
      left: 25px;
      transform: translateY(-50%) translateX(-50%);
    }

    &::after {
      position: absolute;
      top: calc(50% - 2px);
      right: -2px;
      width: 6px;
      height: 2px;
      content: "";
      background: #a3a3a3;
      border-radius: 2px;
    }
  }

  .value-right {
    top: 0;
    right: 0;

    .text {
      right: 18px;
      transform: translateY(-50%) translateX(-50%);
    }

    .text.ef-input-wrapper {
      right: -16px;
    }

    &::after {
      position: absolute;
      top: calc(50% - 2px);
      left: -2px;
      width: 6px;
      height: 2px;
      content: "";
      background: #a3a3a3;
      border-radius: 2px;
    }
  }
}
</style>
