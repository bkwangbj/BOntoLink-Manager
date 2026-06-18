<template>
  <div class="toolbar-wrapper">
    <div class="left-container">
      <!-- <div
        :class="['menu-item', activeMenu === m.key ? 'active' : '']"
        v-for="m in menu"
        :key="m.key"
        @click="clickMenuItem(m)"
      >
        <i :class="['icon', m.icon]" />
        <span>{{ m.name }}</span>
      </div> -->
    </div>
    <div
      v-if="position !== 'left'"
      class="right-container"
    >
      <!-- <div
        class="bk-button-group"
        style="display: flex; align-items: center;"
      >
        <el-button
          size="small"
          @click="$emit('toggleEvent', { key: 'showVarConfigVisible' })"
        >
          全局参数
        </el-button>
        <el-button
          size="small"
          @click="$emit('toggleEvent', { key: 'openPageSetting' })"
        >
          页面设置
        </el-button>
      </div> -->
      <div
        style="display: flex; align-items: center;"
        class="text-button"
        @click="clickMenuItem()"
      >
        <i-ri-pencil-ruler-2-fill />
        工具面板
      </div>

      <div
        v-if="!hideGrid"
        class="grid-icon text-button"
        @click="openGridSetting"
      >
        <i-ri-table-fill />
        <!-- <img src="./images/grid.png"> -->
        栅格
      </div>
      <div
        style="display: flex; align-items: center;"
        class="text-button"
        @click="$emit('clearConfig')"
      >
        <i-ri-delete-bin-2-fill />
        清空
      </div>
      <!--  <div
        class="bk-button-group"
        style="display: flex; align-items: center;"
      >
        <div class="slider">
          <i
            class="el-icon-minus"
            @click="minusScale"
          />
          <el-slider
            v-model="scaleValNum"
            :min="50"
            :max="200"
            style="width: 180px;"
          />
          <i
            class="el-icon-plus"
            @click="plusScale"
          />
        </div>
        <el-select
          v-model="scaleVal"
          placeholder="选择比例"
          style="width: 85px;margin-right: 5px;"
          size="small"
          :transfer="1 == 1"
          class="scale-select"
        >
          <el-option
            v-for="num in scale"
            :key="num"
            :value="num"
            :label="num"
          />
        </el-select>
        <el-tooltip :content="!isShowfull ? '全屏' : '还原'">
          <el-button
            type="text"

            size="medium"
            @click="toChangeFull"
            style="color: #051a56;"
          >
            <i :class="[!isShowfull ? 'ri-fullscreen-line' : 'ri-fullscreen-exit-line']" />
          </el-button>
        </el-tooltip>
      </div> -->
      <!-- <el-button
        size="small"
        @click="$emit('toggleEvent', { key: 'previewPage' })"
      >
        预览
      </el-button>
      <el-button
        size="small"
        @click="$emit('toggleEvent', { key: 'savePageConfig' })"
        type="primary"
        v-if="!disabled"
      >
        保存
      </el-button> -->
    </div>
    <div
      v-if="position === 'left'"
      class="pannel-container"
    >
      <ToolbarPannel
        v-bind="{...$props,...$attrs}"
        ref="pannel"
        :disabled="disabled"
        @click-item="toggleEvent"
      />
    </div>
    <div
      v-show="showGridSetting"
      class="grid-setting-bar"
    >
      <span style="font-size: 14px;">页面栅格设置</span>
      <div style="display: flex;align-items: center;font-size: 12px;white-space: nowrap;">
        <span style="margin-right: 6px;">栅格列数</span>
        <EfSelect
          v-model="columnNum"
          :mock="columnNumsOptions"
          size="small"
          style="width: 70px; margin-right: 40px;"
          :disabled="disabled ? disabled : (disabled || isApp)"
          :clearable="false"
          @change="changeColNum"
        />
        <span
          v-show="showRowHeight"
          style="margin-right: 6px;"
        >每行高度</span>
        <el-input-number
          v-show="showRowHeight"
          v-model="rowHeight"
          :disabled="disabled"
          controls-position="right"
          :min="30"
          :max="200"
          size="small"
          @change="changeRowHeight"
        />
      </div>
      <el-button
        size="small"
        @click="saveGridSetting"
      >
        完成
      </el-button>
    </div>
  </div>
</template>
<script>
import ToolbarPannel from './pannel.vue'
import emitter from '../../../../configs/emitter'
export default {
  name: 'Toolbar',
  components: {
    ToolbarPannel
  },
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    layoutConfig: {
      type: Object,
      default: null
    },
    chartMenuList: {
      type: Array,
      default: () => []
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    position: {
      type: String,
      default: 'left'
    },
    isApp: {
      type: Boolean,
      default: false
    },
    layoutTools: {
      type: Array,
      default: () => []
    },
    hideGrid: {
      type: Boolean,
      default: false
    }
  },
  emits: ['clearConfig', 'toggleEvent', 'showLeftPanel', 'showGridSetting', 'updateGridSetting'],
  data () {
    return {
      menu: [
        { name: '布局', icon: 'ri-layout-3-fill', key: 'layout', isBasic: true },
        { name: '模板', icon: 'am-iconfont icon-moban', key: 'template' },
        { name: '图表', icon: 'am-iconfont icon-zhuzhuangtu', key: 'chart' },
        { name: '地图', icon: 'am-iconfont icon-ditu', key: 'map' },
        { name: '组件', icon: 'am-iconfont icon-zujian', key: 'component' },
        { name: '其他', icon: 'am-iconfont icon-qita', key: 'other' }
      ],
      activeMenu: '',
      scaleValNum: 100,
      scaleVal: '100%',
      scale: ['50%', '60%', '70%', '80%', '90%', '100%', '130%', '150%', '200%'],
      isShowfull: false,
      showGridSetting: false,
      columnNums: [4, 6, 8, 10, 12, 14, 16, 18, 20, 21, 24],
      columnNum: 12,
      rowHeight: 30
    }
  },
  computed: {
    columnNumsOptions () {
      return this.columnNums.map(item => ({ label: item, value: item }))
    },
    showRowHeight () {
      return this.layoutConfig?.themeConfigs?.pageLayout?.pageHeightMode === 'auto'
    }
  },
  watch: {
    scaleValNum (v) {
      this.scaleVal = v + '%'
    },
    scaleVal (v) {
      this.scaleValNum = parseInt(v)
      this.$emit('toggleEvent', { key: 'updateScale', payload: v })
    }
  },
  created () {
    emitter.on('updateActiveMenu', this.updateActiveMenu)
    if (this.isBasicMode) {
      this.menu = this.menu.filter(c => c.isBasic)
    }
    if (this.chartMenuList) {
      this.menu = [...this.menu, ...this.chartMenuList.map(item => { return { name: item.name, key: item.key, icon: item.icon } })]
    }
  },
  beforeUnmount () {
    emitter.off('updateActiveMenu', this.updateActiveMenu)
  },
  methods: {
    updateActiveMenu (value) {
      this.activeMenu = value
    },
    clickMenuItem (item) {
      if (!item) {
        this.$emit('showLeftPanel')
        return
      }
      this.activeMenu = item.key === this.activeMenu ? '' : item.key
      if (this.activeMenu) {
        this.$refs.pannel.toMenu(item.key)
      } else {
        this.$refs.pannel.close()
      }
    },
    toggleEvent (event) {
      this.$emit('toggleEvent', event)
    },
    toChangeFull () {
      this.isShowfull = !this.isShowfull
      this.$emit('toggleEvent', { key: 'toChangeFull' })
    },
    minusScale () {
      this.scaleValNum = this.scaleValNum === 50 ? this.scaleValNum : this.scaleValNum - 1
    },
    plusScale () {
      this.scaleValNum = this.scaleValNum === 200 ? this.scaleValNum : this.scaleValNum + 1
    },
    openGridSetting () {
      if (this.position !== 'left') {
        this.$emit('showGridSetting')
        return
      }
      this.showGridSetting = true
      this.columnNum = this.layoutConfig.colNum
      this.rowHeight = this.layoutConfig.rowHeight
      emitter.emit('toggleGridLine', true)
    },
    saveGridSetting () {
      this.$emit('updateGridSetting', { colNum: this.columnNum, rowHeight: this.rowHeight })
      this.showGridSetting = false
      emitter.emit('toggleGridLine', false)
    },
    changeColNum () {
      this.$emit('updateGridSetting', { colNum: this.columnNum, rowHeight: this.rowHeight })
    },
    changeRowHeight () {
      this.$emit('updateGridSetting', { colNum: this.columnNum, rowHeight: this.rowHeight })
    }
  }
}

</script>

<style lang="scss" scoped>
  .toolbar-wrapper {
    display: flex;
    // justify-content: space-between;
    // width: 100%;
    // height: 40px;
    .left-container {
      display: flex;
      gap: 7px;
      align-items: center;
      padding-left: 10px;

      .menu-item {
        display: flex;
        align-items: center;
        padding: 5px 12px;
        cursor: pointer;

        > .icon {
          margin-right: 3px;
          font-size: 14px;
        }

        > span {
          font-size: 14px;
          color: #1a1a1a;
        }

        &.active,
        &:hover {
          background: #f0f5ff;
          border-radius: 2px;

          > span,
          .icon {
            color: #1f6aff;
          }
        }
      }
    }

    .right-container {
      display: flex;
      // padding-right: 10px;
      gap: 8px;
      align-items: center;

      .bk-button-group {
        height: 30px;
        // border-right: 1px solid #cbd0e4;
        vertical-align: middle;
      }

      .slider {
        display: flex;
        align-items: center;

        > svg {
          margin: 0 10px;
          color: #d8d8d8;
          cursor: pointer;
        }

        :deep(.el-slider__runway) {
          height: 2px;
          background: #c4c4c4;
          border-radius: 2px;

          .el-slider__bar {
            height: 2px;
            background-color: #191919;
            border-top-left-radius: 2px;
            border-bottom-left-radius: 2px;
          }

          .el-slider__button-wrapper {
            top: -17px;
            z-index: 99;
          }

          .el-slider__button {
            width: 10px;
            height: 10px;
            background: #1a1a1a;
            border: none;
            border-radius: 2px;
          }
        }
      }

      .scale-select {

        :deep(.el-input__inner) {
          border: none;
        }
      }

      .text-button {
        position: relative;
        padding: 5px;
        font-size: 14px;
        color: #1a1a1a;
        cursor: pointer;
        border-radius: 4px;

        &::after {
          position: absolute;
          top: 50%;
          right: -4px;
          width: 1px;
          height: 14px;
          margin-top: -7px;
          content: "";
          background: #c4c4c4;
        }

        &:hover {
          background: rgb(0 0 0 / 10%);
        }

        > svg {
          width: 16px;
          height: 16px;
          margin-right: 3px;
        }
      }

      .grid-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        // width: 70px;
        height: 28px;
        color: #1a1a1a;
        cursor: pointer;
        border-radius: 4px;
        // padding-right: 10px;
        > img {
          width: 22px;
          height: 22px;
          margin-right: 3px;
        }
      }
    }

    .pannel-container {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 999;
      height: 100%;
    }
  }

  .grid-setting-bar {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 9999;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 40px;
    padding: 0 16px;
    color: #fff;
    background: #2e74ff;

    // :deep(.el-input) {

    //   &.is-disabled {

    //     .el-input__inner {
    //       color: #dfdfdf;
    //       background: #699eab47;
    //       border-color: #b7b7b7;
    //     }
    //   }
    // }
    :deep(.el-select__wrapper) {
      background: transparent;

    }

    :deep(.el-select__wrapper:not(.is-disabled)) {

      .el-select__selected-item {
        color: #fff;
      }
    }

    :deep(.el-input__wrapper) {
      background: transparent;

      .el-input__inner {
        color: #fff;

      }
    }

    :deep(.el-input-number__decrease),
    :deep(.el-input-number__increase) {
      color: #fff;
      background: transparent;
    }
  }
</style>
