<template>
  <div
    class="time-slider-box mini-slider"
    :class="{'is-vertical':options.isVertical}"
  >
    <div
      class="time-silder mini-slider"

      :style="`position: relative;${options.isVertical?`height:${options.sliderLength||60}%`:`width: ${options.sliderLength||60}%;`}`"
    >
      <el-slider
        v-model="nowData"
        style="position: absolute;width: 100%;pointer-events: none;"
        :min="0"
        :max="100"
        :show-input="false"
        :vertical="options.isVertical"
      />
      <div class="slider-item">
        <div
          v-for="(item, key) in markList"
          :key="key"
          :style="getStopStyle(item.position)"
          class="slider__stop el-slider__marks-stop slider__marks-text"
          :class="{'is_active':getActive(item)}"
          @click="changeValue(item)"
        >
          <div class="inner-text">
            <div
              class="slider__marks-description"
              :class="{'is_active':getActive(item)&&options.upTextShow}"
            >
              <div class="inner-content">
                {{ item.description }}
              </div>
            </div> <div
              class="slider__marks-label"
              :class="{'is_active':getActive(item)&&options.downTextShow}"
              @click="changeValue(item)"
            >
              <div class="inner-content">
                {{ item.showData }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// import dayjs from '../configs/utils/dayjs.min'
import { utils } from 'efficient-suite'
import { getDataTypeFormat } from '../configs/common-func'
export default {
  props: {
    options: {
      type: Object,
      default: () => {}
    },
    marks: {
      type: Array,
      default: () => []
    }
  },
  emits: ['itemClick'],
  data () {
    return {
      styles: {},
      nowData: 0,
      activeItem: {},
      originProps: {
        type: 'category',
        vertical: true

      }
    }
  },
  computed: {
    markList () {
      let list = []
      if (!this.marks || !this.marks?.length) {
        return []
      }
      list = this.getList(this.marks)
      const type = this.options?.type || 'category'
      const format = getDataTypeFormat(this.options.dataType || '', type)
      if (type === 'time') {
        list = list.map(ele => { return { showData: format(ele.value), data: utils.createDate(ele.value).unix(), ...ele } }).sort((a, b) => { return a.data - b.data })
      } else if (type === 'value') {
        list = list.map(ele => { return { showData: format(ele.value), data: isNaN(Number(ele.value)) ? null : Number(ele.value), ...ele } }).sort((a, b) => { return a.data - b.data })
      } else if (type === 'category') {
        list = list.map((ele, index) => { return { showData: format(ele.value), data: index, ...ele } }).sort((a, b) => { return a.data - b.data })
      }
      let range = []
      if (this.marks.length === 1) {
        list[0].position = 100
      } else {
        if (list.length) {
          range = list[list.length - 1].data - list[0].data
          list.forEach(ele => {
            ele.position = (ele.data - list[0].data) * 100 / range
          })
        }
      }
      return list
    }

  },
  watch: {
    options: {
      handler () {
        this.nowData = 0
      },
      immediate: true
    }
  },
  created () {
  },
  methods: {
    getStopStyle (position) {
      return this.options.isVertical ? { bottom: position + '%' } : { left: position + '%' }
    },
    getList (list) {
      const number = this.options.spotNumber < this.marks.length ? this.marks.length : this.options.spotNumber
      const interval = Math.floor(this.marks.length / (number - 1))
      const arr = []
      let length = 0
      list.forEach((ele, index) => {
        if (index === 0 || index === (list.length - 1)) {
          arr.push(ele)
        } else if (index % interval === 0 && length < (number - 2)) {
          arr.push(ele)
          length++
        }
      })
      return arr
    },
    changeValue (item) {
      this.activeItem = item
      this.nowData = item.position
      this.$emit('itemClick', item)
    },
    getActive (item) {
      return item.position <= this.nowData
    }
  }
}
</script>

<style lang="scss" scoped>
/* stylelint-disable custom-property-pattern */
.box {
  height: 100%;
}

.time-slider-box.is-vertical {
  justify-content: flex-center;

  .time-silder {
    margin: 0 16px;

    .el-slider {
      height: 100%;

      :deep() {

        .el-slider__runway,
        .is-vertical {
          height: 100%;
          margin: 0;
        }
      }
    }

    .slider-item {
      width: 100%;
    }

    .slider__stop {
      position: absolute;
      left: 50%;
      z-index: 66;
      width: 6px;
      height: 6px;
      background-color: #fff;
      border-radius: 100%;
      transform: translate(-50%, 50%);
    }

    .slider__marks-text {
      position: absolute;
      left: 50%;
      width: 6px;
      height: 6px;
      transform: translate(-50%, 50%);

      .inner-text {
        align-items: start;
        margin-left: 15px;

        .slider__marks-description {
          top: 20px;
          margin-top: 0;
        }

        .slider__marks-label {
          top: -5px;
        }
      }
    }
  }
}

.time-slider-box {
  display: flex;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding-top: 20px;

  .time-silder {
    margin: 16px 0;
    overflow: visible;

    .slider__stop {
      position: absolute;
      width: 6px;
      height: 6px;
      background-color: #fff;
      border-radius: 100%;
      transform: translate(-50%, 50%);
    }

    .slider__marks-text {
      position: absolute;

      .inner-text {
        position: relative;
        display: flex;
        flex-direction: column;
        align-items: center;
        width: auto;

        .slider__marks-description {
          position: absolute;
          top: -10px;
          margin-top: -15px;
          font-size: 14px;
          color: #909399;
          white-space: nowrap;
        }

        .slider__marks-label {
          position: absolute;
          top: 10px;
          width: auto;
          font-size: 14px;
          color: #909399;
          white-space: nowrap;
        }
      }
    }

    :deep() {

      .el-slider__runway {
        margin: 0;
      }

      .el-slider__button-wrapper {
        display: none;
      }
    }
  }
}

.silder-mask {
  position: relative;
}

.time-slider-box.mini-slider {

  .time-silder {
    height: calc(var(--sliderwidth) * 1px);

    .slider-item {
      position: relative;
      height: calc(var(--sliderwidth) * 1px);
    }

    .slider__stop {
      bottom: 50%;
      width: calc(var(--spotspotwidth) * 1px) !important;
      height: calc(var(--spotspotheight) * 1px) !important;
      background: var(--spotspotbgColor);
      border: calc(var(--spotspotborderWidth) * 1px) var(--spotspotborderColor)  solid;

      &.is_active {
        width: calc(var(--spotselectspotwidth) * 1px) !important;
        height: calc(var(--spotselectspotheight) * 1px) !important;
        background: var(--spotselectspotbgColor);
        border: calc(var(--spotselectspotborderWidth) * 1px) var(--spotselectspotborderColor)  solid;
      }
    }

    .slider__marks-text {

      .inner-text {

        .slider__marks-description {
          top: -10px;
          font-family: var(--upTextnormalfontFamily);
          font-size: var(--upTextnormalfontSize);
          font-style: var(--upTextnormalfontStyle);
          font-weight: var(--upTextnormalfontWeight);
          color: var(--upTextnormalcolor);
          text-decoration: var(--upTextnormaltextDecoration);

          &.is_active {
            font-family: var(--upTextselectfontFamily);
            font-size: var(--upTextselectfontSize);
            font-style: var(--upTextselectfontStyle);
            font-weight: var(--upTextselectfontWeight);
            color: var(--upTextselectcolor);
            text-decoration: var(--upTextselecttextDecoration);
          }

          .inner-content {
            position: relative;
            top: calc(var(--upTexttop) * 1px);
            left: calc(var(--upTextleft) * 1px);
          }
        }

        .slider__marks-label {
          top: 10px;
          font-family: var(--downTextnormalfontFamily);
          font-size: var(--downTextnormalfontSize);
          font-style: var(--downTextnormalfontStyle);
          font-weight: var(--downTextnormalfontWeight);
          color: var(--downTextnormalcolor);
          text-decoration: var(--downTextnormaltextDecoration);

          &.is_active {
            font-family: var(--downTextselectfontFamily);
            font-size: var(--downTextselectfontSize);
            font-style: var(--downTextselectfontStyle);
            font-weight: var(--downTextselectfontWeight);
            color: var(--downTextselectcolor);
            text-decoration: var(--downTextselecttextDecoration);
          }

          .inner-content {
            position: relative;
            top: calc(var(--downTexttop) * 1px);
            left: calc(var(--downTextleft) * 1px);
          }
        }
      }
    }

    .el-slider {

      :deep() {

        .el-slider__runway {
          width: 100%;
          height: calc(var(--sliderwidth) * 1px);
          background-color: var(--slidercolor);
          border-radius: calc(var(--sliderwidth) / 2 * 1px) !important;

          .el-slider__bar {
            height: calc(var(--sliderwidth) * 1px);
            background-color: var(--sliderselectColor);
            border-top-left-radius: calc(var(--sliderwidth) / 2 * 1px) !important;
            border-bottom-left-radius: calc(var(--sliderwidth) / 2 * 1px) !important;
          }
        }
      }
    }
  }

  &.is-vertical {

    .time-silder {
      width: calc(var(--sliderwidth) * 1px) !important;

      .slider-item {
        width: calc(var(--sliderwidth) * 1px) !important;
        height: 100%;
      }

      :deep() {

        .el-slider__runway {
          width: calc(var(--sliderwidth) * 1px) !important;
          height: 100%;
          background-color: var(--slidercolor);
          border-radius: calc(var(--sliderwidth) / 2 * 1px) !important;

          .el-slider__bar {
            width: calc(var(--sliderwidth) * 1px) !important;
            background-color: var(--sliderselectColor);
            border-top-left-radius: calc(var(--sliderwidth) / 2 * 1px) !important;
            border-bottom-left-radius: calc(var(--sliderwidth) / 2 * 1px) !important;
          }
        }
      }
    }
  }
}

</style>>
