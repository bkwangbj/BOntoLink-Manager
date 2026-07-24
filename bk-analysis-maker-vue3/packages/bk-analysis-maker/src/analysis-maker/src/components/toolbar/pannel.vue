<template>
  <div
    v-show="appendToBody || show"
    class="toolbar-pannel-wrapper"
  >
    <div class="title">
      <span v-show="!showSearch">组件</span>
      <div
        v-show="!showSearch"
        class="icons"
      >
        <!-- <img
          src="./images/search.png"
          @click="focusSearchInput"
        > -->
        <i-ri-search-2-line
          style="color: #1a1a1a;"
          @click="focusSearchInput"
        />
        <!-- <i
          :class="[!appendToBody ? 'ri-pushpin-line' : 'ri-pushpin-fill']"
          @click="peg"
          style="cursor: pointer;  "
          :style="{color: appendToBody ? '#1F6AFF' : '#1A1A1A'}"
        /> -->
      </div>
      <el-input
        v-show="showSearch"
        ref="searchInput"
        v-model="keywords"
        size="small"
        class="search-input"
        :prefix-icon="Search"
        clearable
        @blur="hiddenSearch"
      />
    </div>
    <el-collapse
      ref="menuContent"
      v-model="activeNames"
      class="content"
    >
      <template
        v-for="grp in menuCats"
        :key="grp.cat"
      >
        <div class="menu-cat">{{ grp.cat }}</div>
        <el-collapse-item
          v-for="m in grp.items"
          :key="m.key"
          :ref="m.key"
          class="group"
          :name="m.key"
        >
        <template #title>
          <div
            class="group-name"
          >
            {{ m.name }}<span class="group-count">{{ m.children.length }}</span>
          </div>
        </template>
        <div
          v-if="!m.draggable"
          class="group-content"
        >
          <div
            v-for="(item, index) in m.children"
            :key="index"
            class="menu-item"
            @click="clickItem(item.key, item.payload)"
          >
            <el-tooltip
              placement="right"
              effect="light"
              popper-class="tip_popper"
              :class="item.isDecorate ? 'menu-item-decorate' : ''"
            >
              <template #content>
                <div

                  class="menu-item-tip"
                >
                  <div
                    v-if="item.isDecorate"
                    class="decorate-tip d-flex ai-c jc-c"
                  >
                    <BKDecorateChart :content="item.payload.content" />
                  </div>
                  <img
                    v-else
                    :src="item.img"
                    :title="item.name"
                  >
                  <span
                    v-if="!item.isDecorate"
                    class="menu-card-name"
                  >{{ item.name }}</span>
                </div>
              </template>
              <div class="menu-item-wrapper">
                <div
                  v-if="item.isDecorate"
                  class="menu-item-img d-flex ai-c jc-c"
                  style="height: 100%;"
                >
                  <BKDecorateChart :content="item.payload.content" />
                </div>
                <img
                  v-else
                  :src="item.img"
                  :title="item.name"
                  class="menu-item-img"
                >
                <span
                  v-if="!item.isDecorate"
                  class="menu-card-name"
                >{{ item.name }}</span>
              </div>
            </el-tooltip>
          </div>
        </div>
        <draggable
          v-else
          :list="m.children"
          class="group-content"
          :sort="false"
          :disabled="disabled"
          item-key="payload.chartComId"
          :group="{name:'bk-chart-com' , pull: 'clone', put: false}"
        >
          <template #item="{element:item}">
            <div
              class="menu-item"
              title="拖拽到对应区域"
            >
              <el-tooltip
                placement="right"
                effect="light"
                popper-class="tip_popper"
              >
                <template #content>
                  <div

                    class="menu-item-tip"
                  >
                    <img
                      :src="item.img"
                      :title="item.name"
                    >
                    <span class="menu-card-name">{{ item.name }}</span>
                  </div>
                </template>
                <div class="menu-item-wrapper">
                  <img
                    :src="item.img"
                    :title="item.name"
                    class="menu-item-img"
                  >
                  <span class="menu-card-name">{{ item.name }}</span>
                </div>
              </el-tooltip>
            </div>
          </template>
        </draggable>
        </el-collapse-item>
      </template>
    </el-collapse>
  </div>
</template>

<script>
import configs from '../../../../configs/grid-layout-cfg'
import { chartComponents, mapComponents, customComponents, ringComponents, tableComponents, imgObject, decorateComponents } from '../../../../configs/chart-com'
import draggable from 'vuedraggable'
import { debounce } from 'throttle-debounce'
import { menuList, getBasicMenus } from '../../configs'
import { utils } from 'efficient-suite'
import emitter from '../../../../configs/emitter'
import { Search } from '@element-plus/icons-vue'
export default {
  name: 'ToorbarPannel',
  components: {
    draggable
  },
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    customChart: {
      type: Array,
      default: () => []
    },
    appendToBody: {
      type: Boolean,
      default: false
    },
    chartMenuList: {
      type: Array,
      default: () => []
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    isApp: {
      type: Boolean,
      default: false
    },
    layoutTools: {
      type: Array,
      default: () => ['addItem']
    }
  },
  emits: ['clickItem', 'hidden'],
  data () {
    return {
      Search: shallowRef(Search),
      show: false,
      active: '',
      showSearch: false,
      pegButtonActive: false,
      cfgData: [],
      ringData: ringComponents,
      chartData: chartComponents,
      mapData: mapComponents,
      tableData: tableComponents,
      componentData: customComponents,
      decorateComponents,
      menus: [],
      keywordTargetList: [],
      keywords: '',
      searchResult: [],
      panelHeight: 0,
      topMap: {},
      activeNames: ['layout', 'line']
    }
  },
  computed: {
    finalMenus () {
      if (this.keywords) {
        return this.searchResult
      } else {
        return this.menus
      }
    },
    // 分组按两大类拆分:画布(布局/模板) + 图表(其余全部),小标题占位分隔
    menuCats () {
      const isCanvas = m => m.key === 'layout' || m.key === 'template'
      const withChildren = this.finalMenus.filter(m => m.children.length)
      const canvas = withChildren.filter(isCanvas)
      const charts = withChildren.filter(m => !isCanvas(m))
      const arr = []
      if (canvas.length) arr.push({ cat: '画布', items: canvas })
      if (charts.length) arr.push({ cat: '图表', items: charts })
      return arr
    },
    customChartList () {
      return this.customChart.filter(c => c.type !== 'decorate')
    },
    decorateChartList () {
      return this.customChart.filter(c => c.type === 'decorate')
    }
  },
  watch: {
    keywords (v) {
      if (v) {
        const target = this.findTargetByKeywords(v)
        const list = []
        const hasKeys = []
        const menus = utils.deepClone(this.menus)
        for (const t of target) {
          const item = menus.find(item => item.key === t.pKey)
          if (hasKeys.includes(item.key)) {
            const l = list.find(l => l.key === item.key)
            l.children.push(t)
          } else {
            item.children = [t]
            list.push(item)
            hasKeys.push(item.key)
          }
        }
        this.searchResult = list
      } else {
        this.searchResult = []
      }
    },
    finalMenus () {
      // 搜索态:展开所有命中分组;默认态:展开「布局 + 折线图」两组
      this.activeNames = this.keywords ? this.searchResult.map(m => m.key) : ['layout', 'line']
    }
  },
  created () {
    emitter.on('hiddenBodyPannel', this.hiddenBodyPannel)

    this.initMenu()
    this.debounceScroll = debounce(100, () => {
      this.isClick = false
    })
  },
  mounted () {
    // this.initResizeObserver()
  },
  beforeUnmount () {
    // this.resizeObserver.unobserve(this.$refs.menuContent)
    emitter.off('hiddenBodyPannel', this.hiddenBodyPannel)
  },
  methods: {
    initMenu () {
      if (this.isApp) {
        this.menus = menuList.filter(c => c.key !== 'template')
      } else {
        this.menus = utils.deepClone(menuList)
      }
      if (this.isBasicMode && this.layoutTools.length > 0) {
        this.menus = getBasicMenus(this.layoutTools)
      }
      if (this.chartMenuList) {
        this.menus = [...this.menus, ...this.chartMenuList.map(item => {
          return {
            isExChart: true,
            name: item.name,
            key: item.key,
            hasParent: item.hasParent,
            draggable: item.draggable,
            children: item.children.map(child => {
              return {
                name: child.title,
                img: child.chartImg ? child.chartImg : (child.img ? imgObject[child.img] : ''),
                payload: child,
                pKey: item.key
              }
            })
          }
        })]
      }

      this.cfgData = utils.deepClone(configs)
      // 模板
      const templateMenu = this.menus.find(c => c.key === 'template')
      if (templateMenu) {
        templateMenu.children = this.cfgData.map(item => ({
          name: item.title,
          key: 'setLayout',
          img: item.img ? imgObject[item.img] : '',
          payload: item.configs,
          pKey: templateMenu.key
        }))
      }
      // 素材
      const decorateMenu = this.menus.find(c => c.key === 'decorate')
      if (decorateMenu) {
        decorateMenu.children = [...this.decorateComponents.map(item => ({
          isDecorate: true,
          name: item.title,
          key: 'addDecorate',
          payload: item.configs,
          pKey: decorateMenu.key
        })), ...this.decorateChartList.map(item => ({
          isDecorate: true,
          name: item.title,
          key: 'addDecorate',
          payload: item.configs,
          pKey: decorateMenu.key
        }))]
      }

      // 折线图(折线类 branchType)
      const LINE_TYPES = ['lineChart', 'smoothLineChart', 'markLineChart', 'areaChart', 'stackAreaChart', 'stepLineChart', 'rainfallEvap']
      const lineMenu = this.menus.find(c => c.key === 'line')
      if (lineMenu) {
        lineMenu.children = this.chartData.filter(item => LINE_TYPES.indexOf(item.branchType) !== -1).map(item => ({
          name: item.title,
          img: item.img ? imgObject[item.img] : '',
          payload: item,
          pKey: lineMenu.key
        }))
      }
      // 高级图表(气泡图/日历热力图等,虽为 BKBarChart 但单独归组)
      const ADVANCED_TYPES = ['bubbleChart', 'calendarHeatmap', 'polarChart', 'scatterChart', 'funnelChart', 'heatmapChart', 'sankeyChart', 'treemapChart', 'sunburstChart', 'graphChart', 'themeRiverChart', 'boxplotChart', 'gradeGaugeChart', 'parallelChart', 'pictorialBarChart', 'candlestickChart', 'treeChart', 'roseChart', 'waterfallChart', 'bumpChart', 'nestPieChart', 'effectScatterChart', 'gradientAreaChart', 'thresholdAreaChart', 'confidenceBandChart']
      // 柱状图(其余 BKBarChart / 极坐标,排除折线类与高级图表)
      const barMenu = this.menus.find(c => c.key === 'chart')
      if (barMenu) {
        barMenu.children = this.chartData.filter(item => (item.type === 'BKBarChart' || item.type === 'BKPolarChart') && LINE_TYPES.indexOf(item.branchType) === -1 && ADVANCED_TYPES.indexOf(item.branchType) === -1).map(item => ({
          name: item.title,
          img: item.img ? imgObject[item.img] : '',
          payload: item,
          pKey: barMenu.key
        }))
      }
      // 高级图表
      const advancedMenu = this.menus.find(c => c.key === 'advanced')
      if (advancedMenu) {
        advancedMenu.children = this.chartData.filter(item => ADVANCED_TYPES.indexOf(item.branchType) !== -1).map(item => ({
          name: item.title,
          img: item.img ? imgObject[item.img] : '',
          payload: item,
          pKey: advancedMenu.key
        }))
      }
      // 饼形图
      const ringMenu = this.menus.find(c => c.key === 'ring')
      if (ringMenu) {
        ringMenu.children = this.ringData.map(item => ({
          name: item.title,
          img: item.img ? imgObject[item.img] : '',
          payload: item,
          pKey: ringMenu.key
        }))
      }
      // 表格
      const tableMenu = this.menus.find(c => c.key === 'table')
      if (tableMenu) {
        tableMenu.children = this.tableData.map(item => ({
          name: item.title,
          img: item.img ? imgObject[item.img] : '',
          payload: item,
          pKey: tableMenu.key
        }))
      }
      // 地图
      const mapMenu = this.menus.find(c => c.key === 'map')
      if (mapMenu) {
        mapMenu.children = this.mapData.map(item => ({
          name: item.title,
          img: item.img ? imgObject[item.img] : '',
          payload: item,
          pKey: mapMenu.key
        }))
      }
      // 仪表盘
      // 组件
      const componentMenu = this.menus.find(c => c.key === 'component')
      if (componentMenu) {
        componentMenu.children = this.componentData.map(item => ({
          name: item.title,
          img: item.img ? imgObject[item.img] : '',
          payload: item,
          pKey: componentMenu.key
        }))
      }
      // 其它
      const otherMenu = this.menus.find(c => c.key === 'other')
      if (otherMenu) {
        otherMenu.children = this.customChartList.map(item => ({
          name: item.title,
          img: item.chartImg ? item.chartImg : (item.img ? imgObject[item.img] : ''),
          payload: item,
          pKey: otherMenu.key
        }))
      }
      this.createKeywordList()
    },
    scrollContainer ({ target }) {
      if (this.isClick) {
        this.debounceScroll()
        return
      }
      const result = Object.entries(this.topMap).find((item, index, list) => target.scrollTop >= item[1] && index !== list.length - 1 && target.scrollTop < list[index + 1][1])
      if (result) {
        emitter.emit('updateActiveMenu', result[0])
      } else {
        emitter.emit('updateActiveMenu', Object.keys(this.topMap).pop())
      }
    },
    initResizeObserver () {
      this.resizeObserver = new ResizeObserver(([box]) => {
        this.panelHeight = box.target.scrollHeight
        const map = {}
        for (const item of this.finalMenus) {
          if (item.hasParent) continue
          map[item.key] = this.$refs[item.key][0].offsetTop - 36
        }
        this.topMap = map
      })
      this.resizeObserver.observe(this.$refs.menuContent)
    },
    createKeywordList () {
      const arr = []
      for (const m of this.menus) {
        for (const item of m.children) {
          arr.push({ ...item, draggable: m.draggable })
        }
      }
      this.keywordTargetList = arr
    },
    findTargetByKeywords (keyword) {
      const keywordTargetList = utils.deepClone(this.keywordTargetList)
      const target = []
      function handler (list) {
        const i = list.findIndex(item => item.name.includes(keyword))
        if (i !== -1) {
          target.push(list[i])
          list.splice(i, 1)
          handler(list)
        }
      }
      handler(keywordTargetList)
      return target
    },
    hiddenSearch () {
      if (!this.keywords) {
        this.showSearch = false
        this.searchResult = []
      }
    },
    toMenu (key) {
      this.show = true
      // this.active = key
      // this.isClick = true
      // this.$nextTick(() => {
      //   const refs = this.$refs[key]
      //   if (refs && refs.length > 0) {
      //     const el = refs[0]
      //     el.scrollIntoView({ behavior: 'smooth' })
      //   }
      // })
    },
    close () {
      this.active = ''
      this.show = false
    },
    clickItem (key, payload) {
      if (key === 'draggable') return
      if (this.disabled) return
      this.$emit('clickItem', { key, payload })
    },
    formatterData (key, isExChart) {
      if (isExChart) {
        const menu = this.chartMenuList.find(c => c.key === key)
        return menu?.children || []
      }
      if (key === 'other') {
        return this.customChartList
      }
      return this[key + 'Data']
    },
    peg () {
      if (!this.appendToBody) {
        this.show = false
        emitter.emit('pegToolbarPannel')
      } else {
        emitter.emit('hiddenBodyPannel')
        this.$emit('hidden')
      }
    },
    hiddenBodyPannel () {
      if (this.active) {
        this.toMenu(this.active)
      }
    },
    focusSearchInput () {
      this.showSearch = true
      this.$nextTick(() => {
        this.$refs.searchInput.focus()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.menu-item-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 180px;
  height: 120px;
  background: #fff;

  > img,
  .decorate-tip {
    width: 180px;
    height: 100%;
    // margin-bottom: 5px;
  }
}

.menu-item-decorate {

  .menu-item-img {
    height: 100% !important;
    margin-bottom: 0 !important;
  }

  .el-tooltip {
    margin-bottom: 0 !important;
  }
}

.menu-item .el-tooltip {
  width: 56px;
  height: 34px;
  margin-bottom: 5px;
}

.toolbar-pannel-wrapper {
  display: flex;
  flex: 0 0 178px;
  flex-direction: column;
  width: 178px;
  height: 100%;

  .title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 36px;
    padding: 0 10px;
    background: #fff;

    > span {
      font-size: 12px;
      color: #1a1a1a;
    }

    .icons {
      display: flex;
      gap: 6px;
      align-items: center;

      > img {
        width: 14px;
        height: 14px;
        cursor: pointer;
      }

      > svg {
        font-size: 18px;
      }
    }

    .search-input {

      :deep(.el-input-group__prepend) {
        padding: 0 5px;
        background-color: transparent;
      }
    }
  }

  .content {
    flex: 1;
    padding-right: 0px;
    padding-bottom: 12px;
    overflow: auto;
    background: #f7f7f7;

    :deep() {

      .el-collapse-item__header,
      .el-collapse-item__wrap {
        background-color: transparent;
      }

      .el-collapse-item__content {
        padding-bottom: 0;
      }

      .el-collapse-item.is-active .el-collapse-item__wrap {
        padding-bottom: 5px;
      }
    }

    .menu-cat {
      padding: 12px 10px 4px;
      font-size: 11px;
      color: #b0b4bd;
      letter-spacing: 1px;
    }

    .group {
      // padding: 0 12px;
      .group-name {
        // padding: 10px 0;
        // font-size: 12px;
        padding-left: 10px;
        font-weight: bold;
        color: #1a1a1a;

        & + :deep(i) {
          color: #1a1a1a;
        }

        .group-count {
          margin-left: 6px;
          padding: 0 6px;
          min-width: 16px;
          height: 16px;
          line-height: 16px;
          display: inline-block;
          text-align: center;
          font-size: 11px;
          font-weight: normal;
          color: #86909c;
          background: #f2f3f5;
          border-radius: 8px;
        }
      }

      .group-content {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
        padding-right: 10px;
        padding-left: 10px;

        .menu-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 100%;
          height: 60px;
          cursor: pointer;
          background: #fff;
          border: 1px solid transparent;

          .menu-item-wrapper {
            display: flex;
            flex-direction: column;
            align-items: center;
            width: 100%;
            height: 100%;
          }

          &:hover {
            border-color: #1f6aff;
            border-radius: 4px;
          }

          .menu-item-img {
            width: 56px;
            height: 34px;
            margin-bottom: 5px;
          }

          .menu-card-name {
            width: 100%;
            overflow: hidden;
            font-size: 12px;
            color: #2b3958;
            text-align: center;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
    }
  }
}

</style>
