<template>
  <grid-layout
    v-model:layout="configs.layout"
    :col-num="isChild ? pageConfig.themeConfigs.globalCss.childConfig.colNum :pageConfig.colNum"
    :row-height="isChild ? childRowHeight : rowHeight"
    :is-draggable="pageConfig.draggable && editMode"
    :is-resizable="pageConfig.resizable && editMode"
    :vertical-compact="true"
    :use-css-transforms="true"
    :margin="isChild ?pageConfig.themeConfigs.globalCss.childConfig.margin :configs.margin"
    :max-rows="isChild ? childMaxRows : maxRows"
    :prevent-collision="isChild ? childPreventCollision : preventCollision"
  >
    <grid-item
      v-for="item in configs.layout"
      :id="item.i"
      :key="item.i"
      :static="item.static"
      :x="item.x"
      :y="item.y"
      :w="item.w"
      :h="item.h"
      :i="item.i"
      drag-allow-from=".vue-draggable-handle"
      drag-ignore-from=".chart-content-root .grid-item-remove .grid-item-add"
      :class="[(item.isTabLayout && mergeChild)? 'item-layout':'',item.isQuery ? 'item-query':'',
               item.isQuery && isModal ? 'item-modal-query':'',
               (item.isTabLayout || (!item.isChart && !item.isQuery)) && !mergeChild ? 'item-layout-out':'',
               isModal ? 'item-modal-layout':'item-set-layout',
               (editMode && item.id === currentConfigId) ? 'active' : '']"
      :style="customCardStyleProps[item.id]"
    >
      <ChartContent
        v-if="item.isChart"
        v-bind="$props"
        :key="item.i"
        :ref="'chart-' + item.i"
        :configs="item"
        :page-config="pageConfig"
        :set-mode="setMode"
        :btn-top="btnTop"
        class="chart-content-root"
        :is-child="isChild"
        @update-current-config-id="currentConfigId = $event"
        @sort-part-chart="sortPartChart"
        @restore-part-chart="restorePartChart"
      />
      <QueryArea
        v-else-if="item.isQuery"
        ref="queryArea"
        :query-items="queryItems"
        :page-config="pageConfig"
        :set-mode="setMode"
        class=""
        @query="querySetParams"
      />
      <BKTabLayout
        v-else-if="item.isTabLayout"
        v-bind="$props"
        :ref="'tabLayout' + item.i"
        :configs="item"
        :set-mode="setMode"
        :page-config="pageConfig"
        :parent-page-configs="configs"
        @dropdown-visible-change="dropdownVisibleChange"
        @remove-item="$emit('removeItem', $event)"
        @cancel-set-card="$emit('cancelSetCard')"
      />
      <div
        v-else
        class="child-wrapper"
        :class="mergeChild && isModal ? 'merge-child-wrapper':''"
      >
        <BKGridLayoutContent
          v-bind="$props"
          :ref="'gridLayout' + item.i"
          :configs="item"
          :set-mode="setMode"
          :page-config="pageConfig"
          :custom-card-style-props="customCardStyleProps"
          is-child
          :parent-config="item"
          :parent-page-configs="configs"
          @remove-item="emitRemoveItem"
          @cancel-set-card="$emit('cancelSetCard')"
        />
      </div>
      <i-ri-pushpin-fill
        v-if="selectedCardItem && item.id === selectedCardItem.id"
        class="card-style-active"
        title="取消设置样式"
        @click="$emit('cancelSetCard')"
      />
      <i
        v-if="setMode && !item.isQuery"
        class="vue-draggable-handle icon-tuozhuai icon am-iconfont"
        style="font-size: 20px;color: #a3a3a3;"
        :style="{top:dragBtnTop }"
      />
      <div
        class="grid-item-oper"
        :style="{top:btnTop}"
      >
        <div
          :ref="'oper'+ item.i"
          class="item-oper-list"
        >
          <el-dropdown
            v-if="setMode && !item.isChart && !item.isQuery && !item.isTabLayout"
            class="grid-item-remove grid-item-add oper-btn"
            style="margin-right: 10px;margin-bottom: 2px;font-size: 12px;"
            @visible-change="(val) => {dropdownVisibleChange(val, item)}"
          >
            <span class="el-dropdown-link">
              <el-icon class="el-icon--right"><Plus /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  @click="addSelfItem(true, item)"
                >
                  添加区域
                </el-dropdown-item>
                <el-dropdown-item
                  @click="addSelfItem(false, item)"
                >
                  添加布局
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- <el-button
            size="small"
            @click="addSelfItem(true, item)"
            icon="el-icon-plus"
            type="text"
            v-if="setMode && !item.isChart && !item.isQuery && !item.isTabLayout"
            class="grid-item-remove grid-item-add oper-btn"
            title="添加区域"
          />
          <el-button
            size="small"
            @click="addSelfItem(false, item)"
            icon="el-icon-circle-plus"
            type="text"
            v-if="setMode && !item.isChart && !item.isQuery && !item.isTabLayout"
            class="grid-item-remove grid-item-add oper-btn"
            title="添加布局"
          /> -->
          <el-dropdown
            v-if="!isModal"
            class="grid-item-remove oper-btn"
            style="margin-right: 10px;margin-bottom: 2px;font-size: 12px;"
            @visible-change="(val) => {dropdownVisibleChange(val, item)}"
          >
            <span class="el-dropdown-link">
              <el-icon class="el-icon--right"><CopyDocument /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-if="item.isChart && !isBasicMode"
                  @click="copyItemMethod(item, 3)"
                >
                  复制当前图表配置
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="setMode"
                  @click="copyItemMethod(item, 1)"
                >
                  复制区域
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="setMode && item.isChart && !isBasicMode"
                  @click="copyItemMethod(item, 2)"
                >
                  复制区域和当前图表
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="setMode && item.isChart && !isBasicMode"
                  @click="copyItemMethod(item, 4)"
                >
                  粘贴图表
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="setMode && item.isChart && !isBasicMode"
                  @click="copyItemMethod(item, 5)"
                >
                  粘贴为新图表
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="!isBasicMode"
                  @click="openItemStyle(item)"
                >
                  卡片样式
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- <el-button
            type="text"
            icon="el-icon-copy-document"
            size="small"
            class="grid-item-remove oper-btn"
            @click="copyItemMethod(item)"
            v-if="setMode"
            title="复制区域"
          /> -->
          <el-button
            v-if="setMode"
            text
            size="small"
            class="grid-item-remove oper-btn"
            title="删除区域"
            @click="removeItem(item.i)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <div
        v-if="setMode && !item.isChart && !item.isQuery && !item.isTabLayout && !item.layout.length"
        class="empty-area"
        @click="addSelfItem(true, item)"
      >
        <img
          src="./images/empty-area.png"
          alt=""
        >
        <span>点击添加区域</span>
      </div>
    </grid-item>
  </grid-layout>
</template>

<script>
import { GridLayout, GridItem } from 'grid-layout-plus'
import ChartContent from './components/chart-content.vue'
import QueryArea from './components/query-area.vue'
import { v4 as uuidv4 } from 'uuid'
import emitter from '../../configs/emitter'
import { utils } from 'efficient-suite'
import { ElMessage, ElMessageBox } from 'element-plus'
export default {
  name: 'GridLayoutContent',
  components: {
    GridLayout,
    GridItem,
    ChartContent,
    QueryArea
  },
  inject: ['setParams'],
  inheritAttrs: false,

  props: {
    parentPageConfigs: {
      type: Object,
      default: () => { return { layout: [], margin: [10, 10] } }
    },
    configs: {
      type: Object,
      default: () => { return { layout: [], margin: [10, 10] } }
    },
    parentConfig: {
      type: Object,
      default: () => { return { } }
    },
    setMode: {
      type: Boolean,
      default: true
    },
    // 编辑模式(设计=true / 预览=false):显式控制可拖拽/缩放/选中,响应式,首屏即生效
    editMode: {
      type: Boolean,
      default: true
    },
    pageConfig: {
      type: Object,
      default: () => { return { layout: [], margin: [10, 10] } }
    },
    isChild: {
      type: Boolean,
      default: false
    },
    configNode: {
      type: Object,
      default: () => { return { label: '' } }
    },
    mapSource: {
      type: Array,
      default: () => []
    },
    mapPath: {
      type: String,
      default: '/map/'
    },
    isModal: {
      type: Boolean,
      default: false
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    tjbURL: {
      type: String,
      default: ''
    },
    customCardStyleProps: {
      type: Object,
      default: () => { return {} }
    },
    selectedCardItem: {
      type: Object,
      default: null
    }
  },
  emits: ['removeItem', 'cancelSetCard'],
  data () {
    return {
      currentConfigId: ''
    }
  },
  computed: {
    queryItems () {
      if (this.configs && this.configs.varConfig) {
        return this.configs.varConfig.filter(c => c.type === 'external' && c.isQuery === '1' && c.queryConfig)
      }
      return []
    },
    maxRows () {
      if (this.configs?.maxRows && this.preventCollision) {
        return this.configs?.maxRows
      } else {
        return Infinity
      }
    },
    childMaxRows () {
      if (this.childPreventCollision) {
        // const rowHeight = this.parentPageConfigs?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive' ? this.pageConfig?.autoRowHeight : this.pageConfig?.rowHeight
        // const height = rowHeight * this.parentConfig?.h +
        // ((this.parentConfig?.h - 1) * this.parentPageConfigs?.margin[1]) -
        // this.pageConfig?.themeConfigs?.globalCss?.tabCommonStyle?.height - 10
        // const maxRows = Math.floor((height) / (this.pageConfig?.themeConfigs?.globalCss?.childConfig?.rowHeight + this.pageConfig?.themeConfigs?.globalCss?.childConfig?.margin[1]))
        return this.pageConfig?.themeConfigs?.globalCss?.childConfig?.maxRows || 40
      } else {
        return Infinity
      }
    },
    preventCollision () {
      return this.configs?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive'
    },
    childPreventCollision () {
      return this.pageConfig?.themeConfigs?.globalCss?.childConfig?.cardMode === 'adaptive'
    },
    rowHeight () {
      return this.preventCollision ? this.pageConfig.autoRowHeight : this.pageConfig.rowHeight
    },
    childRowHeight () {
      if (this.childPreventCollision) {
        const rowHeight = this.parentPageConfigs?.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive' ? this.pageConfig?.autoRowHeight : this.pageConfig?.rowHeight
        const height = rowHeight * this.parentConfig?.h +
        ((this.parentConfig?.h - 1) * this.parentPageConfigs?.margin[1]) -
        this.pageConfig?.themeConfigs?.globalCss?.tabCommonStyle?.height - 10
        return (height / this.childMaxRows) -
           this.pageConfig?.themeConfigs?.globalCss?.childConfig?.margin[1]
      }
      return this.pageConfig?.themeConfigs?.globalCss?.childConfig?.rowHeight
    },
    btnTop () {
      if (this.pageConfig?.themeConfigs?.globalCss?.cardPadding) {
        const { paddingTop } = this.pageConfig.themeConfigs.globalCss.cardPadding
        return (paddingTop + 5) + 'px'
      } else {
        return '0px'
      }
    },
    dragBtnTop () {
      if (this.pageConfig?.themeConfigs?.globalCss?.cardPadding) {
        const { paddingTop } = this.pageConfig.themeConfigs.globalCss.cardPadding
        return (paddingTop - 14) + 'px'
      } else {
        return '-14px'
      }
    },
    mergeChild () {
      return this.pageConfig?.themeConfigs?.globalCss?.childConfig?.mergeChildChart === 'merge'
    }
  },
  methods: {
    emitRemoveItem (data) {
      this.$emit('removeItem', data)
    },
    sortPartChart (id) {
      for (let i = 0; i < this.configs.layout.length; i++) {
        const item = this.configs.layout[i]
        if (item.isChart && item.tabList) {
          const refs = this.$refs['chart-' + item.i]
          if (refs && refs.length > 0) {
            for (let j = 0; j < item.tabList.length; j++) {
              if (id === item.tabList[j].chartId) {
                continue
              }
              refs[0].sortChart(item.tabList[j])
            }
          }
        }
      }
    },
    restorePartChart (id) {
      for (let i = 0; i < this.configs.layout.length; i++) {
        const item = this.configs.layout[i]
        if (item.isChart && item.tabList) {
          const refs = this.$refs['chart-' + item.i]
          if (refs && refs.length > 0) {
            for (let j = 0; j < item.tabList.length; j++) {
              if (id === item.tabList[j].chartId) {
                continue
              }
              refs[0].restoreChart(item.tabList[j])
            }
          }
        }
      }
    },
    isRectangleOverlap (rectangle1, rectangle2) {
      if (
        rectangle1[0] >= rectangle2[2] || // rectangle1 的左边界在 rectangle2 的右边界的右侧  x
        rectangle1[1] >= rectangle2[3] || // rectangle1 的上边界在 rectangle2 的下边界的下侧  y
        rectangle1[2] <= rectangle2[0] || // rectangle1 的右边界在 rectangle2 的左边界的左侧  x + w
        rectangle1[3] <= rectangle2[1] // rectangle1 的下边界在 rectangle2 的上边界的上侧  y + h
      ) {
        return false
      }
      return true
    },
    calculateContainer (value, copyItem) {
      const w = copyItem ? copyItem.w : (value ? 6 : 4)
      // 新增区域默认高度 9(= 自动图表块高度,350px),与现有块保持一致
      const h = copyItem ? copyItem.h : (value ? 16 : 9)
      const maxYLayout = Math.max(...this.configs.layout.map(item => (item.y + item.h)))
      const maxY = (!this.configs.maxRows || this.configs.maxRows === Infinity) ? maxYLayout : (this.configs.maxRows - h)
      const maxX = this.configs.colNum - w
      for (let i = 0; i <= maxY; i++) {
        for (let j = 0; j <= maxX; j++) {
          const coordinates = [j, i, j + w, i + h]
          let count = 0
          for (const item of this.configs.layout) {
            if (this.isRectangleOverlap(coordinates, [item.x, item.y, item.x + item.w, item.y + item.h])) break
            count++
          }

          if (count === this.configs.layout.length) {
            return { x: j, y: i, w, h }
          }
        }
      }
      return null
    },
    addItem (isChart, isChild, isTabLayout, copyItem) {
      const coordinates = this.calculateContainer(!isChart || isChild || isTabLayout, copyItem)
      let item = {
        isChart,
        isTabLayout,
        x: (this.configs.layout.length * 2) % (this.configs.colNum || 12),
        y: this.configs.layout.length + (this.configs.colNum || 12),
        w: copyItem ? copyItem.w : (this.configs.w || 4),
        h: copyItem ? copyItem.h : (isChild ? 16 : (this.configs.h || 9)),
        i: uuidv4()
      }
      item = coordinates ? Object.assign(item, coordinates) : item
      if (this.configs.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive') {
        if (((item.y + item.h) > this.maxRows) || !coordinates) {
          ElMessage.warning('已没有剩余空间添加区域')
          return
        }
      }
      item.id = item.i
      if (!isChart) {
        item = { ...item, layout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, draggable: this.setMode, resizable: this.setMode }
        if (isTabLayout) {
          const tab = { chartId: uuidv4(), title: '页签1', isEdit: false, id: item.id, layout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, draggable: this.setMode, resizable: this.setMode }
          item.tabList = [tab]
        }
      }
      // else {
      //   if (this.mergeChild) {
      //     item.isShowTitle = '0'
      //   }
      // }
      this.configs.layout.push(item)
      this.$nextTick(() => {
        const div = document.getElementById(item.i)
        if (div) {
          div.scrollIntoView()
        }
      })
      return item
    },
    addSelfItem (isChart, item) {
      this.$refs['gridLayout' + item.i][0].addItem(isChart, true)
    },
    addQuery () {
      if (this.configs.layout.find(c => c.isQuery)) {
        return ElMessage.error('已存在查询区域，不能重复添加')
      }
      if (this.configs.themeConfigs?.pageLayout?.pageHeightMode === 'adaptive') {
        if (this.configs.layout.find(item => ((item.y + item.h + 40 / this.configs.rowHeight) > this.maxRows))) {
          ElMessage.warning('已没有剩余空间添加查询区域')
          return
        }
      }
      const item = {
        isQuery: true,
        x: 0,
        y: 0,
        w: this.configs.colNum,
        h: 40 / this.configs.rowHeight,
        i: uuidv4(),
        static: true
      }
      item.id = item.i
      this.configs.layout.push(item)
      this.$nextTick(() => {
        const div = document.getElementById(item.i)
        if (div) {
          div.scrollIntoView()
        }
      })
    },
    addTabLayout () {
      this.addItem(false, true, true)
    },
    querySetParams (params) {
      this.setParams(params)
      // emitter.emit('setParams', params)
    },
    openItemStyle (item) {
      emitter.emit('openItemStyle', item)
    },
    copyItemMethod (item, type) {
      if (type === 1) {
        this.addItem(true, false, false, item)
      } else if (type === 2) {
        const newItem = this.addItem(true, false, false, item)
        const refs = this.$refs['chart-' + item.i]
        if (refs && refs.length > 0) {
          let chart = refs[0].chartList
          if (chart.length > 0) {
            chart = utils.deepClone(chart[0])
            chart.chartId = uuidv4()
            chart.hookId = chart.type.replace('BK', '') + utils.createDate().format('YYYYMMDDHHmmss')
            chart = { ...chart, ...newItem }
            this.$nextTick(() => {
              const newRefs = this.$refs['chart-' + newItem.i]
              if (newRefs && newRefs.length > 0) {
                newRefs[0].tabList = [chart]
                newRefs[0].currentTab = chart.chartId
              }
            })
          }
        }
      } else if (type === 3) {
        const refs = this.$refs['chart-' + item.i]
        if (refs && refs.length > 0) {
          let chart = refs[0].chartList
          if (chart.length > 0) {
            chart = utils.deepClone(chart[0])
            const props = ['x', 'y', 'w', 'h', 'i', 'id', 'chartId', 'hookId']
            for (let i = 0; i < props.length; i++) {
              delete chart[props[i]]
            }
            localStorage.setItem('amCopyData', JSON.stringify({ type: 'chartCopy', data: chart }))
            ElMessage.success('复制成功')
            // componentConfigs.request.copyTextToClipboard(JSON.stringify({ type: 'chartCopy', data: chart }))
          } else {
            ElMessage.info('该区域没有图表')
          }
        }
      } else if (type === 4 || type === 5) {
        try {
          const data = JSON.parse(localStorage.getItem('amCopyData'))
          if (data && data.type === 'chartCopy') {
            const chartConfig = data.data
            chartConfig.chartId = uuidv4()
            chartConfig.hookId = chartConfig.type.replace('BK', '') + utils.createDate().format('YYYYMMDDHHmmss')
            const refs = this.$refs['chart-' + item.i]
            if (refs && refs.length > 0) {
              if (type === 4) {
                refs[0].change({ added: { element: chartConfig } })
              } else if (type === 5) {
                chartConfig.id = item.id
                refs[0].tabList.push(chartConfig)
                refs[0].currentTab = chartConfig.chartId
              }
            }
            ElMessage.success('粘贴完成')
          } else {
            ElMessage.info('请粘贴正确的图表数据')
          }
        } catch (error) {
          ElMessage.info('请粘贴正确的图表数据')
        }
      }
    },
    removeItem (val, auto) {
      if (auto) {
        const index = this.configs.layout.map(item => item.i).indexOf(val)
        const data = this.configs.layout.splice(index, 1)
        if (data.length > 0) {
          this.$emit('removeItem', data[0])
          this.$nextTick(() => {
            const layoutItem = this.configs.layout.filter(c => c.y < 0)
            for (let i = 0; i < layoutItem.length; i++) {
              layoutItem[i].y = 0
            }
          })
        }
      } else {
        ElMessageBox.confirm('确认删除该区域吗？').then(() => {
          const index = this.configs.layout.map(item => item.i).indexOf(val)
          const data = this.configs.layout.splice(index, 1)
          if (data.length > 0) {
            this.$emit('removeItem', data[0])
            this.$nextTick(() => {
              const layoutItem = this.configs.layout.filter(c => c.y < 0)
              for (let i = 0; i < layoutItem.length; i++) {
                layoutItem[i].y = 0
              }
            })
          }
        }).catch(() => {})
      }
    },
    dropdownVisibleChange (visible, item) {
      if (visible) {
        this.$refs['oper' + item.id][0].classList.add('show-oper')
      } else {
        this.$refs['oper' + item.id][0].classList.remove('show-oper')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  .empty-area {
    position: absolute;
    top: 50%;
    left: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 130px;
    height: 130px;
    cursor: pointer;
    background: #fff;
    border-radius: 8px;
    transform: translate(-50%, -50%);

    > img {
      width: 44px;
      height: 45px;
    }

    > span {
      font-size: 12px;
      color: var(--textStylecolor) !important;
    }
  }

  .grid-item-remove {
    // position: absolute;
    // right: 3px;
    // top: 5px;
    display: none;
  }

  .grid-item-oper {
    position: absolute;
    top: 0;
    right: 3px;
    z-index: 9999;

    .item-oper-list {
      display: flex;
      align-items: center;
    }
  }

  .child-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
    overflow: auto;
  }

  .merge-child-wrapper {

    :deep() {

      .vgl-item {
        background: none !important;
        border: none !important;
        box-shadow: none !important;
      }
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

    .card-style-active {
      position: absolute;
      top: 0;
      right: 0;
      cursor: pointer;
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
      }

      .show-oper .grid-item-remove {
        display: inline-block;
      }

      & > .vgl-item__resizer {
        display: inline-block;
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
