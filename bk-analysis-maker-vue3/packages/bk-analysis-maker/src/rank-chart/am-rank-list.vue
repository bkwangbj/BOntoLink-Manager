<template>
  <div
    v-if="data.length"
    class="rank-list-container"
    :class="`${type}-rank`"
  >
    <div
      v-for="(item, i) in finalData"
      :key="i"
      class="rank-item"
      :class="i===0?'first-bg':i===1?'second-bg':i===2?'third-bg':'other-bg'"
    >
      <div style="display: flex;justify-content: space-between;">
        <div
          style="  display: flex;align-items: center;"
          @click="itemClick(item)"
        >
          <span class="rank-image">{{ i + 1 }}</span>
          <span class="f-14 title">{{ item.name }}</span>
        </div>
        <div class="f-16 f-w ff-wf num">
          {{ item.number }}
        </div>
      </div>
      <el-slider
        v-model="item.number"
        :show-tooltip="false"
        disabled
        :max="max"
      />
    </div>
  </div>
  <div
    v-else
    style="display: flex;align-items: center;justify-content: center; width: 100%;height: 100%;"
  >
    <el-empty description="暂无数据" />
  </div>
</template>

<script>
import XEUtils from 'xe-utils'
export default {
  name: 'RankList',
  props: {
    type: {
      type: String,
      default: 'blue',
      validator (v) {
        return ['blue', 'yellow'].includes(v)
      }
    },
    data: {
      type: Array,
      default: () => []
    },
    max: {
      type: Number,
      default: 100
    }
  },
  emits: ['itemClick'],
  computed: {
    finalData () {
      const data = XEUtils.clone(this.data)
      data.sort((a, b) => { return Number(b.number) - Number(a.number) })
      return data
    }
  },
  methods: {
    itemClick (item) {
      this.$emit('itemClick', item)
    }
  }
}
</script>

<style lang="scss" scoped>
.rank-list-container {
  height: 100%;
  padding: 12px 18px;
  overflow: auto;

  .rank-item {

    .ff-wf {
      font-family: "D DIN", sans-serif;
    }

    &:not(:last-child) {
      margin-bottom: 10px;
    }

    &.first-bg {

      .rank-image {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAYAAABB7B6eAAAAAXNSR0IArs4c6QAAANFJREFUOE/tlDEKwlAQRGeKeARFEcU7JIXxAjbmAtqKrYX3sBHsvEBArXKDJCDfSyiIegFBMCtGEIlIAsnvsvXsPHb2/6VY3T6EKwB15KsTwDGV733bUEz7DKCWz/vTfaAK2kmAFGQe21AFLAEpicocKpwRiOLIxLQL3UFMF25QiYYMw5sewHvGHe4Y6AS8IJ5GAH0YkaMJIGsYGGnagSzQaU7pug8tr6j8yal38Tciy75AUE3tzCY4UgWtxDXtOYAsATSyefxREVdEMuE+3H4rni9mYDVXyMRpAAAAAElFTkSuQmCC");
      }
    }

    &.second-bg {

      .rank-image {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAYAAABB7B6eAAAAAXNSR0IArs4c6QAAALpJREFUOE/tlbEOAVEQRc9V0ypI6H2Jhqg0tNQi+ud9gYbSL9hO/IDGZ7CNRK/yntiILJGsZPd1pp65J5mZOyNvaFNiDdTIFzGOkSy7tIy8JQbq+bRf1UcZmp8AX5B4IiOD/oCsji5wzGRxScu8pdAZPOkRFQaacg0FeHAOeDohASC2IQF7HN0wALGhzDDMDDxLWkzU5xZki/5OznLx12N3BqqZlb8lnGRovF/TOT3EqoCHcwHGMkRpwB1Z0kpu+g5q0AAAAABJRU5ErkJggg==");
      }
    }

    &.third-bg {

      .rank-image {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAYAAABB7B6eAAAAAXNSR0IArs4c6QAAAKJJREFUOE9j/L+ZIYCBkWEaAwODJANl4A3Df4Y0Rl+G9cjGMP7fzPCSgZFBjDKz4bqfMPowyKJasIXhP5UMBxvD6MPAOGoBoRDtZzjNUMLYyPAPHGT/qRwHUNvXM3xjiGYMY/hOKwtA9pxiYGXwpaUFIEu20dKCowwsDH60smAdwzeGGFrFwRSGbwwFjGEMf2mSikZzMqFcjKWwo3lxTeMKBwDOa0wSIuAxIAAAAABJRU5ErkJggg==");
      }
    }

    .rank-image {
      width: 24px;
      height: 18px;
      padding-right: 3px;
      margin-right: 8px;
      font-size: 12px;
      line-height: 18px;
      color: #fff;
      text-align: center;
    }

    :deep() {

      .el-slider__runway {
        margin: 6px 0;
        background: #f8f8f9;

        .el-slider__button-wrapper {
          cursor: default !important;

          .el-slider__button {
            position: relative;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 18px;
            height: 18px;
            cursor: default !important;
            border: none;

            &::before {
              position: absolute;
              z-index: 1;
              width: 12px;
              height: 12px;
              content: "";
              border-radius: 50%;
            }
          }
        }
      }
    }
  }

  &.blue-rank {

    .other-bg {

      .rank-image {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAYAAABB7B6eAAAAAXNSR0IArs4c6QAAANRJREFUOE/tlb0OwWAUht83hi5dkRi4Ez+bRccuLK3BanADvQKLxFQuQIJJjJ9BJO4CieAGhIQjFUMlohL9tp75nPfJ+WfFF+tO9Ank8IcJcCLRUg6nYRmWBnIkkPlDOxy6Uy7zb4DyQCQm8aeMcskE8LWiBLrFDTqex3vgyLh78KJPLibqK5tnXQAIsTZSqGkDBJkIMNMHECwNgaUFIMD4aqKhqwe9tIn2yOZNyxQlmxx5Fz+V6AAgGxn5g4MA24XLwvs1HUpVBH4MD2dPoqkczsOAB7H8Y5U7BYR9AAAAAElFTkSuQmCC");
      }
    }

    :deep() {

      .el-slider__bar {
        background: #409eff !important;
      }

      .el-slider__button {
        background: rgba($color: #a5e8ff, $alpha: 20%) !important;

        &::before {
          background: #dbeefd !important;
        }
      }
    }
  }

  &.yellow-rank {

    .other-bg {

      .rank-image {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAYAAABB7B6eAAAAAXNSR0IArs4c6QAAANRJREFUOE/tlTEKwkAQRf9fPIOChWLsgmDvCWxMaaOdha2F97ARrLyCWokXsLEOCkK2UBD1ABECZiRWCYgKyXaZeuY9mNmdobg1R5SaAVJGmiDuFA5pe6s4huHBukFQTMOO1Z6VrStJwd6SjOBvjLI1c8HXjkqgJ6qJMYkwSmSY8QwQ6Ii7pI8eW3iYEkSSHYmOSQFArE0KtizAMSVY0EffyAwk0FN1xIhdPI28ovwn/9yLn1p0BVD6WflfwknZuprYpuLW26IwT31wwAtDDNjwNnHBC6+5a2I3HRs3AAAAAElFTkSuQmCC");
      }
    }

    :deep() {

      .el-slider__bar {
        background: #fed000 !important;
      }

      .el-slider__button {
        background: rgba($color: #fcf5d6, $alpha: 20%) !important;

        &::before {
          background: #fcf5d6 !important;
        }
      }
    }
  }
}
</style>
