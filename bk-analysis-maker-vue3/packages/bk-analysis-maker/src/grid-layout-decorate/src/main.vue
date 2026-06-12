<template>
  <grid-layout
    v-model:layout="configs.decorateLayout"
    :col-num="decorateConfig.decorateColNum"
    :row-height="rowHeight"
    :is-draggable="pageConfig.draggable"
    :is-resizable="pageConfig.resizable"
    :vertical-compact="false"
    :use-css-transforms="true"
    :max-rows="maxRows"
    :margin="decorateConfig.decorateMargin"
  >
    <grid-item
      v-for="item in configs.decorateLayout"
      :id="item.i"
      :key="item.i"
      :x="item.x"
      :y="item.y"
      :w="item.w"
      :h="item.h"
      :i="item.i"
      drag-ignore-from=".grid-item-remove .grid-item-add"
      class="item-decorate"
      :class="[!isModal ? 'set-chart-content':'']"
      style="pointer-events: auto;"
    >
      <div class="full-box d-flex ai-c jc-c decorate-content">
        <BKDecorateChart :content="item.content" />
      </div>
      <!-- <i
        class="vue-draggable-handle icon-tuozhuai icon am-iconfont"
        style="font-size: 20px;color: #a3a3a3;"
        v-if="setMode"
      /> -->
      <div
        class="grid-item-oper"
      >
        <div
          :ref="'oper'+ item.i"
          class="item-oper-list"
        >
          <el-button
            v-if="setMode"
            text
            size="small"
            class="grid-item-remove oper-btn"
            title="复制"
            @click="copyItemMethod(item, 1)"
          >
            <el-icon><CopyDocument /></el-icon>
          </el-button>
          <el-button
            v-if="setMode"
            text
            size="small"
            class="grid-item-remove oper-btn"
            title="删除"
            @click="removeItem(item.i)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
    </grid-item>
  </grid-layout>
</template>

<script>
import { GridLayout, GridItem } from 'grid-layout-plus'
import { v4 as uuidv4 } from 'uuid'
import { ElMessageBox } from 'element-plus'
export default {
  name: 'GridLayoutDecorate',
  components: {
    GridLayout,
    GridItem
  },
  inheritAttrs: false,

  props: {
    configs: {
      type: Object,
      default: () => { return { decorateLayout: [], margin: [10, 10] } }
    },
    setMode: {
      type: Boolean,
      default: true
    },
    pageConfig: {
      type: Object,
      default: () => { return { decorateLayout: [], margin: [10, 10] } }
    },
    isModal: {
      type: Boolean,
      default: false
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    decorateConfig: {
      type: Object,
      default: () => {
        return {
          decorateColNum: 36,
          decorateRowHeight: 1,
          decorateAutoRowHeight: 1,
          decorateMaxRows: Infinity,
          decorateMargin: [5, 5]
        }
      }
    }
  },
  emits: ['removeDecorateItem'],
  data () {
    return {
      currentConfigId: ''
    }
  },
  computed: {
    maxRows () {
      if (this.decorateConfig?.decorateMaxRows) {
        return this.decorateConfig?.decorateMaxRows
      } else {
        return Infinity
      }
    },
    preventCollision () {
      return this.configs?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive'
    },
    rowHeight () {
      return this.preventCollision ? this.decorateConfig.decorateAutoRowHeight : this.decorateConfig.decorateRowHeight
    }
  },
  methods: {

    addItem (copyItem) {
      const item = {
        w: copyItem && copyItem.w ? copyItem.w : 4,
        h: copyItem && copyItem.h ? copyItem.h : 4,
        ...copyItem,
        i: uuidv4(),
        x: 0,
        y: 0
      }
      item.id = item.i
      this.configs.decorateLayout.push(item)
      this.$nextTick(() => {
        const div = document.getElementById(item.i)
        if (div) {
          div.scrollIntoView()
        }
      })
      return item
    },
    copyItemMethod (item, type) {
      if (type === 1) {
        this.addItem(item)
      }
    },
    removeItem (val, auto) {
      if (auto) {
        const index = this.configs.decorateLayout.map(item => item.i).indexOf(val)
        const data = this.configs.decorateLayout.splice(index, 1)
        if (data.length > 0) {
          this.$emit('removeDecorateItem', data[0])
          this.$nextTick(() => {
            const layoutItem = this.configs.decorateLayout.filter(c => c.y < 0)
            for (let i = 0; i < layoutItem.length; i++) {
              layoutItem[i].y = 0
            }
          })
        }
      } else {
        ElMessageBox.confirm('确认删除该素材吗？').then(() => {
          const index = this.configs.decorateLayout.map(item => item.i).indexOf(val)
          const data = this.configs.decorateLayout.splice(index, 1)
          if (data.length > 0) {
            this.$emit('removeDecorateItem', data[0])
            this.$nextTick(() => {
              const layoutItem = this.configs.decorateLayout.filter(c => c.y < 0)
              for (let i = 0; i < layoutItem.length; i++) {
                layoutItem[i].y = 0
              }
            })
          }
        }).catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .grid-item-remove,
  .vue-draggable-handle {
    // position: absolute;
    // right: 3px;
    // top: 5px;
    display: none;
  }

  .grid-item-oper {
    position: absolute;
    top: -25px;
    right: 3px;
    z-index: 9999;

    .item-oper-list {
      display: flex;
      align-items: center;
    }
  }

  .vue-draggable-handle {
    position: absolute;
    top: -10px !important;
    left: 50%;
    display: none;
    transform: translateX(-50%);
  }

  :deep() {

    .decorate-content {
      font-family: var(--decorateStylefontFamily);
      font-size: var(--decorateStylefontSize);
      font-weight: var(--decorateStylefontWeight);
      color: var(--decorateStylecolor);
    }

    .vgl-item__resizer {
      right: 4px;
      bottom: 5px;
      // display: none;
      background: none;

      &::before {
        border: none;
      }
    }

    .vgl-item {

      &:hover {
        // border: 1px solid #e8eaec;
        & > .vue-draggable-handle,
        & > .grid-item-oper > div > .grid-item-remove {
          display: inline-block;
        }

        & > .vgl-item__resizer {
          display: inline-block !important;
        }
      }

      .show-oper .grid-item-remove {
        display: inline-block;
      }

      & > .vgl-item__resizer {
        display: none !important;
        font-family: iconfont !important;
        font-size: 12px;
        font-style: normal;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
      }
    }

    // .item-set-layout {
    // overflow: auto;

    // .vue-grid-layout {
    //   padding: 20px;
    // }
    // }
  }
</style>
