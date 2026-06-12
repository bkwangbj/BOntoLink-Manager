<template>
  <div
    class="chart-content-panel"
  >
    <el-container
      class="chart-content"
      :style="{padding: cardPadding}"
    >
      <el-header
        class="chart-title"
        :class="tabStyleCss"
      >
        <draggable
          :list="tabList"
          class="tab-content"
          :group="{name:'bk-tab-layout',put:['bk-tab-layout'] }"
          :disabled="!setMode"
          item-key="chartId"
        >
          <template #item="{element:tab}">
            <div
              class="tab-btn"
              :class="currentTab == tab.chartId ? 'tab-btn-active':''"
              :label="tab.chartId"
              @click="tabClick(tab)"
            >
              <el-input
                v-if="tab.isEdit && setMode"
                v-model="tab.title"
                type="text"
                size="small"
                autofocus
                @blur="tab.isEdit=false"
              />
              <span v-else> {{ tab.title }}</span>
              <el-button
                v-if="setMode"
                size="small"
                text
                class="tab-del oper-btn"
                @click.stop="tab.isEdit=true"
              >
                <el-icon><Edit /></el-icon>
              </el-button><el-button
                v-if="setMode && tabList.length > 1"
                size="small"
                text
                class="tab-del oper-btn"
                @click.stop="delTab(tab)"
              >
                <el-icon><Close /></el-icon>
              </el-button>
              <el-button
                v-if="setMode && tabList.indexOf(tab) === tabList.length -1"
                size="small"
                text
                class="tab-del oper-btn"
                style="display: block; margin-left: 20px;"
                title="添加页签"
                @click.stop="addTab()"
              >
                <el-icon><CirclePlus /></el-icon>
              </el-button>
            </div>
          </template>
        </draggable>
      </el-header>
      <el-main class="chart-content-main">
        <div
          class="list-group"
          style="position: relative;height: 100%;"
        >
          <div
            v-if="currentTabData && !currentTabData.layout.length && setMode"
            class="empty-area"
            @click="addItem(true, currentTabData, false)"
          >
            <img
              src="./images/empty-area.png"
              alt=""
            >
            <span>点击添加区域</span>
          </div>
          <div
            v-if="currentTabData"
            v-show="currentTabData.layout.length"
            class="chart-content-box"
            :style="{padding: chartBoxPadding, overflow:'auto'}"
          >
            <!-- <keep-alive> -->
            <template
              v-for="tab in tabList"
              :key="tab.chartId"
            >
              <BKGridLayoutContent
                v-show="tab.chartId === currentTabData.chartId"
                v-bind="$props"
                :ref="'tabLayout' + tab.chartId"
                :configs="tab"
                :parent-config="configs"
                :set-mode="setMode"
                :page-config="pageConfig"
                :parent-page-configs="parentPageConfigs"
                is-child
                :custom-card-style-props="customCardStyleProps"
                :selected-card-item="selectedCardItem"
                @remove-item="removeItem"
                @cancel-set-card="$emit('cancelSetCard')"
              />
            </template>
            <!-- </keep-alive> -->
          </div>
        </div>
        <!-- <el-tabs
          v-model="currentTab"
          v-else
          style="height: 100%;"
          class="preview-tab"
        >
          <el-tab-pane
            v-for="chart in tabList"
            :label="chart.title"
            :name="chart.chartId"
            :key="chart.chartId"
            style="height: 100%;"
          >
            <div
              class="chart-content-box"
              :style="{padding: chartBoxPadding, overflow:'auto'}"
            >
              <BKGridLayoutContent
                v-bind="$props"
                :configs="chart"
                :set-mode="setMode"
                :page-config="pageConfig"
                :ref="'tabLayout' + chart.chartId"
                @removeItem="removeItem"
                is-child
              />
            </div>
          </el-tab-pane>
        </el-tabs> -->
        <div
          class="tab-layout-item-oper"
          :style="{top:btnTop}"
        >
          <div
            ref="item-oper-list"
            class="item-oper-list"
          >
            <el-dropdown
              v-if="setMode && currentTabData"
              class="grid-item-add oper-btn grid-item-remove"
              style="margin-top: 5px;margin-right: 5px;font-size: 12px;"
              @visible-change="dropdownVisibleChange"
            >
              <span class="el-dropdown-link">
                <el-icon class="el-icon--right"><Plus /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    @click="addItem(true, currentTabData, false)"
                  >
                    添加区域
                  </el-dropdown-item>
                  <el-dropdown-item
                    @click="addItem(false, currentTabData, true)"
                  >
                    添加布局
                  </el-dropdown-item>
                  <el-dropdown-item
                    @click="addTabLayout(currentTabData)"
                  >
                    添加页签布局
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- <el-button
              size="small"
              icon="el-icon-plus"
              type="text"
              @click="addItem(true, currentTabData, false)"
              v-if="setMode && currentTabData"
              class="grid-item-add oper-btn"
              title="添加区域"
            /> <el-button
              size="small"
              icon="el-icon-circle-plus"
              type="text"
              @click="addItem(false, currentTabData, true)"
              v-if="setMode && currentTabData"
              class="grid-item-add oper-btn"
              title="添加布局"
            /><el-button
              size="small"
              icon="el-icon-circle-plus-outline"
              type="text"
              @click="addTabLayout(currentTabData)"
              v-if="setMode && currentTabData"
              class="grid-item-add oper-btn"
              title="添加页签布局"
            /> -->
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import { v4 as uuidv4 } from 'uuid'
import emitter from '../../configs/emitter'
import { ElMessageBox } from 'element-plus'
export default {
  name: 'TabLayout',
  components: {
    draggable
  },
  inject: ['chartConfig'],
  props: {
    parentPageConfigs: {
      type: Object,
      default: () => { return { layout: [], margin: [10, 10] } }
    },
    configs: {
      type: Object,
      default: () => {}
    },
    setMode: {
      type: Boolean,
      default: false
    },
    pageConfig: {
      type: Object,
      default: () => {}
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
  emits: ['cancelSetCard', 'removeItem', 'dropdownVisibleChange'],
  data () {
    return {
      currentTab: ''
    }
  },
  computed: {
    tabStyleCss () {
      if (this.pageConfig?.themeConfigs?.globalCss?.tabStyle) {
        return `am-${this.pageConfig.themeConfigs.globalCss.tabStyle.type}-tab`
      }
      return ''
    },
    tabList () {
      return this.configs.tabList || []
    },
    btnTop () {
      if (this.pageConfig?.themeConfigs?.globalCss?.cardPadding) {
        const { paddingTop } = this.pageConfig.themeConfigs.globalCss.cardPadding
        return (paddingTop + 5) + 'px'
      } else {
        return '5px'
      }
    },
    currentTabData () {
      return this.tabList.find(c => c.chartId === this.currentTab)
    },
    cardPadding () {
      if (this.pageConfig?.themeConfigs?.globalCss?.cardPadding) {
        const { paddingTop, paddingBottom, paddingLeft, paddingRight } = this.pageConfig.themeConfigs.globalCss.cardPadding
        return `${paddingTop}px ${paddingRight}px ${paddingBottom}px ${paddingLeft}px`
      } else {
        return '0'
      }
    },

    chartBoxPadding () {
      let cfg = {}
      // if (this.setMode) {
      //   cfg = this.chartList?.length ? this.chartList[0] : {}
      // } else {
      //   }
      cfg = this.tabList.find(ele => { return ele.chartId === this.currentTab })
      if (cfg?.paddingConfig && cfg.paddingConfig?.show) {
        const { top, bottom, left, right } = cfg?.paddingConfig
        return `${top || 0}px ${right || 0}px ${bottom || 0}px ${left || 0}px`
      } else if (this.pageConfig?.themeConfigs?.innerPaddingConfig && this.pageConfig?.themeConfigs?.innerPaddingConfig.show) {
        const { top, bottom, left, right } = this.pageConfig?.themeConfigs?.innerPaddingConfig
        return `${top || 0}px ${right || 0}px ${bottom || 0}px ${left || 0}px`
      } else {
        return '0'
      }
    }
  },
  watch: {
    currentTab: {
      handler () {

      }
    }
  },
  created () {
    emitter.on('savePage', this.savePage)
    if (this.configs.tabList && this.configs.tabList.length > 0) {
      this.currentTab = this.configs.tabList[0].chartId
    }
  },
  beforeUnmount () {
    emitter.off('savePage', this.savePage)
  },
  methods: {
    tabClick (tab) {
      this.currentTab = tab.chartId
    },
    delTab (tab) {
      ElMessageBox.confirm('确认删除该页签吗？').then(() => {
        this.handleDelTab(tab)
      }).catch(() => {})
    },
    handleDelTab (tab) {
      this.configs.tabList.splice(this.configs.tabList.indexOf(tab), 1)
      if (this.configs.tabList.length > 0) {
        this.currentTab = this.configs.tabList[0].chartId
      } else {
        this.currentTab = ''
      }
    },
    addTab () {
      for (let i = 0; i < this.configs.tabList.length; i++) {
        this.configs.tabList[i].isEdit = false
      }
      const tab = { chartId: uuidv4(), title: '页签' + (this.configs.tabList.length + 1), isEdit: true, id: this.configs.id, layout: [], colNum: 12, rowHeight: 30, autoRowHeight: 30, draggable: this.setMode, resizable: this.setMode }
      this.configs.tabList.push(tab)
      this.currentTab = tab.chartId
    },
    addTabLayout (item) {
      this.$refs['tabLayout' + item.chartId][0].addTabLayout()
    },
    addItem (isChart, item, isChild) {
      this.$refs['tabLayout' + item.chartId][0].addItem(isChart, isChild)
    },
    removeItem (data) {
      this.$emit('removeItem', data)
    },
    savePage () {
      // for (let i = 0; i < this.tabList.length; i++) {
      //   if (this.tabList[i].type === 'BKCodeChart') {
      //     if (this.$refs['chart' + this.tabList[i].chartId].length > 0) {
      //       this.tabList[i].code = this.$refs['chart' + this.tabList[i].chartId][0].code
      //     }
      //   }
      // }
      // this.chartConfig.setConfig(this.configs.id, this.tabList)
    },
    dropdownVisibleChange (visible) {
      if (visible) {
        this.$refs['item-oper-list'].classList.add('show-oper')
      } else {
        this.$refs['item-oper-list'].classList.remove('show-oper')
      }
      this.$emit('dropdownVisibleChange', visible, this.configs)
    }
  }
}
</script>

<style lang="scss" scoped>
:deep() {

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

  .tab-layout-item-oper {
    position: absolute;
    top: 0;
    right: 50px;
    z-index: 9999;

    .item-oper-list {
      display: flex;
      align-items: center;
    }
  }

  .el-tabs__content {
    height: calc(100% - 43px);
  }

  .preview-tab {
    padding-top: 5px;

    .el-tabs__header {
      margin: 0 0 10px;

      .el-tabs__nav-wrap {
        padding-left: 10px;
      }

      .el-tabs__active-bar {
        display: none;
      }

      .el-tabs__item {
        height: 33px;
        padding: 0 20px 0 15px;
        line-height: 33px;
      }
    }
  }
}

.code-preview {
  position: absolute;
  top: 5px;
  right: 30px;
}

.chart-content-panel {
  width: 100%;
  height: 100%;

  .full-box {
    width: 100%;
    height: 100%;
  }

  .empty-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: transparent;
    border-radius: 8px;

    > .empty-img {
      width: 155px;
      height: 116px;
    }

    > span {
      font-size: 12px;
      color: var(--textStylecolor) !important;
    }
  }

  &:hover {

    & > .chart-content > .chart-content-main > .tab-layout-item-oper > .item-oper-list > .grid-item-remove {
      display: inline-block;
    }
  }
}

.chart-content {
  flex-direction: column;
  width: 100%;
  // border: 1px solid #e8eaec;
  height: 100%;

  .chart-content-box {
    width: 100%;
    height: 100%;
  }
}

.chart-title {
  position: relative;
  // border-bottom: 1px solid #e8eaec;
  display: flex;
  align-items: center;
  margin: 0 0 10px !important;
}

.tab-content {
  width: 100%;

  .tab-btn {

    .el-input {
      width: 100px;
      height: 25px;
      margin-bottom: 8px;
    }
  }
}

</style>
