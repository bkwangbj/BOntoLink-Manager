<template>
  <div
    style="width: 100%;"
    class="d-flex-c"
  >
    <div style="display: flex;justify-content: center;width: 100%;">
      <div
        v-show="name"
        style="box-sizing: border-box;width: 80px; padding-right: 8px;padding-left: 20px;"
        class="chart-config-title"
      >
        {{ name }}
      </div>
      <div
        class="d-flex"
        style="flex: 1;align-items: center;overflow: hidden;"
      >
        <el-select
          v-model="value"
          size="small"
          :disabled="!saveAble||!editAble"
          placeholder="请选择"
          @change="colorChange"
        >
          <template #prefix>
            <div class="color-view">
              <div
                v-for="(color,index) in value.split('@')"
                :key="index"
                class="color-item"
                :style="`background:${color}`"
              />
            </div>
          </template>
          <el-option
            v-for="(colors,index) in colorsList"
            :key="index"

            :value="colors"
          >
            <div class="color-view-dropdown">
              <div
                v-for="(color,key) in colors.split('@')"
                :key="key"
                class="color-item"
                :style="`background:${color}`"
              />
            </div>
          </el-option>
        </el-select>
        <el-button
          type="info"
          round
          style="padding: 2px 6px;margin-left: 5px;font-size: 16px;border-radius: 5px;"
          size="small"
          @click="downColorActive=!downColorActive"
        >
          <i-ri-palette-line />
        </el-button>
      </div>
    </div>

    <div
      v-show="downColorActive"
      class="d-flex-c plane-inner-bg "
      :style=" `margin: 10px 10px 0 ${name?40:0}px;padding: 0;width:auto;`"
    >
      <div
        style="  position: relative;width: 100%;padding: 30px 10px 10px 20px; border-bottom: 1px solid #e8eaec;"
      >
        <el-button
          circle
          text
          class="pin-icon"
          style="position: absolute;top: 3px;right: 8px;"
          size="small"
          :class="{'pin-active':allColorsPin}"
          @click="changeAllPin"
        >
          <i-ri-pushpin-fill />
        </el-button>
        <div
          v-for="(ele,index) in conversionColorList"
          :key="index"
          style="position: relative;width: 100%;"
          class="d-flex down-color-picker"
        >
          <div
            class="color-example"
            :style="`background:${conversionColorList[index].color};`"
          />
          <div
            class="down-color-item"
            style="margin: 2px 0;"
          >
            <CommonColorPicker
              v-model="conversionColorList[index].color"
              :color-modes="conversionColorList[index].color.includes('linear-gradient')?['linear-gradient']:colorModes"
              @change="conversionColorListChange"
            />
            <el-button
              text
              circle
              class="pin-icon"
              size="small"
              :class="{'pin-active':conversionColorList[index].isPin}"
              @click="conversionColorList[index].isPin=!conversionColorList[index].isPin"
            >
              <i-ri-pushpin-fill />
            </el-button>
          </div>
        </div>
      </div>
      <div style="display: flex;justify-content: center;width: 100%;padding: 10px;font-size: 14px;">
        <div style="display: flex; margin-right: 30px;">
          数量
          <div
            controls-position="right"
            class="colors-number"
            style=" position: relative;width: 60px;margin-left: 5px;"
            :value="conversionColorList.length"
            @change="size=&quot;mini&quot;"
          >
            {{ conversionColorList.length }}
            <div class="d-flex-c colors-number-suffix">
              <el-button
                type="info"
                class="colors-number-button"
                size="small"
                :disabled="!saveAble"
                @click="changeColorList('plus')"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
              <el-button
                type="info"
                class="colors-number-button"
                size="small"
                :disabled="!saveAble"
                @click="changeColorList('minus')"
              >
                <el-icon><Minus /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        <div
          style="display: flex;"
          :class="{'reverse-active':reverseActive}"
          @click="reverseList"
        >
          反序
          <el-button

            circle
            class="reverse-icon"
            size="small"
          >
            <el-icon><Sort /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CommonColorPicker from './common-color-picker.vue'
import { utils } from 'efficient-suite'
export default {
  name: 'ColorsPicker',
  components: {
    CommonColorPicker
  },
  inject: ['getSaveAble'],
  props: {
    colorList: {
      type: Array,
      default: () => []

    },
    colorModes: {
      type: Array,
      default: () => ['monochrome']
    },
    name: {
      type: String,
      default: ''
    },
    editAble: {
      type: Boolean,
      default: true
    },
    modelValue: {
      type: Array,
      default: () => []
    }
  },
  emits: ['colorChange', 'update:modelValue'],
  data () {
    return {
      value: '',
      colorsList: [],
      conversionColorList: [],
      downColorActive: false,
      reverseActive: false
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    },
    allColorsPin () {
      return !this.conversionColorList.find(item => !item.isPin)
    }
  },
  watch: {
    modelValue: {
      handler (nv, ov) {
        this.value = this.modelValue.join('@')
        this.filtersColor(this.modelValue)
      },
      immediate: true
    },
    colorList: {
      handler (nv, ov) {
        if (nv.length) {
          this.colorsList = this.colorList.map(ele => {
            if (Array.isArray(ele)) {
              return ele.join('@')
            } else {
              if (!ele) {
                return ele
              } else {
                return ele.split(',').join('@')
              }
            }
          })
        }
      },
      immediate: true
    }
  },

  methods: {
    colorChange (e) {
      let data = []
      if (e) {
        data = utils.deepClone(e).split('@')
      }
      this.filtersColor(data)
      this.$emit('update:modelValue', this.conversionColorList.map(item => item.color))
      this.$emit('colorChange', this.conversionColorList.map(item => item.color))
    },
    filtersColor (colors) {
      if (!this.conversionColorList.length) {
        this.conversionColorList = colors.map(color => { return { color, isPin: false } })
      } else {
        const length = colors.length > this.conversionColorList.length ? colors.length : this.conversionColorList.length
        const list = utils.deepClone(this.conversionColorList)
        for (let i = 0; i < length; i++) {
          if (this.conversionColorList[i] && colors[i] && !this.conversionColorList[i].isPin) {
            list[i].color = colors[i]
          } else if (!this.conversionColorList[i]) {
            list.push({ color: colors[i], isPin: false })
          } else if (!colors[i] && !this.conversionColorList[i].isPin) {
            list[i].isDelet = true
          }
        }
        this.conversionColorList = list.filter(item => { return !item.isDelet })
      }
    },
    conversionColorListChange () {
      this.$emit('update:modelValue', this.conversionColorList.map(item => item.color))
      this.$emit('colorChange', this.conversionColorList.map(item => item.color))
    },
    reverseList () {
      if (!this.saveAble) {
        return
      }
      this.reverseActive = !this.reverseActive
      this.conversionColorList.reverse()
      this.$emit('update:modelValue', this.conversionColorList.map(item => item.color))
      this.$emit('colorChange', this.conversionColorList.map(item => item.color))
    },
    changeColorList (flag) {
      if (flag === 'minus') {
        this.conversionColorList.splice(this.conversionColorList.length - 1)
        this.$emit('update:modelValue', this.conversionColorList.map(item => item.color))
        this.$emit('colorChange', this.conversionColorList.map(item => item.color))
      } else {
        this.conversionColorList.push({ color: '', isPin: false })
      }
      // this.$emit('update:modelValue', this.conversionColorList.map(item => item.color))
      // this.$emit('colorChange', this.conversionColorList.map(item => item.color))
    },
    changeAllPin () {
      const active = !this.allColorsPin
      this.conversionColorList.forEach(ele => { ele.isPin = active })
    }
  }
}
</script>
<style lang="scss" scoped>
:deep(.el-select) {

  .el-select__prefix {
    top: 2px;
    left: 2px;
    box-sizing: border-box;
    display: inline-block;
    width: calc(100% - 3px);
    height: calc(100% - 3px);
    padding: 0 15px 0 8px;
    color: #606266;
    cursor: pointer;
    background: #ededed !important;
    background-image: none;
    border-radius: 4px;
    outline: 0;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
    appearance: none;
  }

  .el-select__suffix {
    position: absolute;
    right: 10px;
  }
}

.el-button.pin-icon {
  position: absolute;
  top: 0;
  right: -2px;
  font-size: 14px;
  color: #606266;
  background: transparent;
  border-color: transparent;

  &.pin-active {
    color: #1f6aff;
  }
}

.down-color-picker {

  .color-example {
    width: 20px;
    margin-right: 40px;
  }

  .down-color-item {
    position: relative;
    flex: 1;
    overflow: hidden;
  }

  &::before {
    position: absolute;
    bottom: 5px;
    left: 24px;
    display: inline-block;
    font-size: 18px;
    color: #d5d5d5;
    content: "----";
  }
}

.colors-number {
  height: 28px;
  padding-right: 30px;
  padding-left: 10px;
  font-size: 12px;
  color: #424e61;
  text-align: left;
  background: #ededed;
  border: none;
  border-radius: 4px;

  .colors-number-suffix {
    position: absolute;
    top: 1px;
    right: 2px;
    width: auto;
  }

  .colors-number-button {
    width: 20px;
    height: 13px;
    padding: 0 2px;
    margin-left: 5px;
    font-size: 12px;
    border-radius: 0;
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

.reverse-icon {
  font-size: 14px;
  color: #606266;
  background: transparent;
  border-color: transparent;
}

.reverse-active {
  color: #1f6aff;

  .reverse-icon {
    color: #1f6aff;
  }
}

.color-view-dropdown {
  box-sizing: border-box;
  display: flex;
  padding: 12px 0;

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
    height: 12px;
  }
}
</style>
