<template>
  <div
    ref="rootRef"
    class="full-box overflow-hidden"
    style="position: relative;"
  >
    <slot name="content">
      <component
        :is="contentConfig.component"
        v-if="contentConfig && contentConfig.component"
        v-bind="contentConfig.props || {}"
      />
    </slot>
    <EfTable
      v-if="reChange"
      ref="table"
      v-bind="props"
      :columns="finalColums"
      :data="configs.branchType==='tjbTable'?relData:relList"
      :has-pager="props.hasPager"
      :page-size="props.pageSize"
      :has-seq="false"
      :seq-config="{seqMethod:seqMethod}"
      :total="props.isStatic ? undefined : total"
      :style="{'--tablecellheight':48,'--headercellheight':48,...styles}"
      @page-change="pageChange"
    />
  </div>
</template>
<script lang="jsx">
import { mixins } from '../../configs/commom-chart'
import emitter from '../../configs/emitter'
import { getDataTypeFormat } from '../../configs/common-func'
import { utils } from 'efficient-suite'
export default {
  name: 'TableChart',
  components: {

  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {

  },
  data () {
    return {
      columns: [],
      relData: [],
      defaultData: [],
      reChange: true,
      isTjb: false,
      styles:
       {
         '--borderColor': '#e8eaec',
         '--stripeColor': '#fafafa',
         '--bodyBg': '#fff',
         '--headerBg': '#f8f8f9',
         '--bodycolor': '#606266',
         '--rowCurrentColor': '#e6f7ff',
         '--bodyfontSize': 12,
         '--bodyfontFamily': 'Microsoft YaHei',
         '--bodyfontWeight': 'normal',
         '--headercolor': '#606266',
         '--headerfontSize': 12,
         '--headerfontFamily': 'Microsoft YaHei',
         '--headerfontWeight': 'normal'
       },
      props: {
        hasPager: false,
        isStatic: false
      },
      pageParam: {
        pageNum: 1,
        pageSize: 20
      },
      isDesc: false,
      total: 0
    }
  },
  computed: {
    finalColums () {
      if (this.configs.eventConfig) {
        const event = this.configs.eventConfig.find(c => c.event === 'cellClick' && c.isActive)
        if (event && event.items && event.items.length > 0) {
          return this.columns.map((col, index) => {
            const item = event.items.find(c => c.columnField === col.field || c.columnField === (index + 1 + '') || c.columnField === this.configs?.configOption?.columns[index].field)

            let format = function (e) {
              return e
            }
            if (col.sourceType === 'time' || col.sourceType === 'value') {
              if (col.dataType) {
                format = getDataTypeFormat(col.dataType, col.sourceType)
              }
            }

            if (item) {
              col.slots.default = ({ row }) => {
                let data = row
                if (this.configs.branchType === 'tjbTable') {
                  data = this.relList.find(c => c.ylabel === row.ylabel && c.xlabel === row[col.field + 'Field'])
                  if (!data) {
                    data = this.relList.find(c => c.ylabel === row.ylabel)
                  }
                }

                return [
                    <el-link type="primary" onClick={() => { this.handleEvent(data, 'cellClick', item.columnField) }}>{format(row[col.field])}</el-link>
                ]
              }
            }
            return col
          })
        }
      }
      return this.columns.map((col) => {
        if (!col.slots) {
          col.slots = {}
        }
        if (col.sourceType === 'download') {
          col.slots.default = ({ row }) => {
            if (row[col.field]) {
              return [
                      <el-link type="primary" underline={false} onClick={() => { window.open(row[col.field]) }}><el-icon><Document /></el-icon></el-link>
              ]
            }
          }
        }
        return col
      })
    }
  },
  watch: {
    relList () {
      this.debouncedCustomResetChart(this.configs)
    },
    'props.hasPager': {
      handler () {
        this.reChange = false
        this.$nextTick(() => {
          this.reChange = true
        })
      }
    },
    'props.isStatic': {
      handler () {
        this.reChange = false
        this.$nextTick(() => {
          this.reChange = true
        })
      }
    }
  },
  created () {
    emitter.on('syncColumn', this.syncColumn)
  },
  beforeUnmount () {
    emitter.off('syncColumn', this.syncColumn)
  },
  methods: {

    async customResetChart (config) {
      if (config.configOption) {
        const option = utils.deepClone(config.configOption)
        const { columns, props, styles, filterDataEmpty } = option
        columns.forEach(item => {
          if (item.sourceType === 'time' || item.sourceType === 'value') {
            if (item.dataType) {
              const format = getDataTypeFormat(item.dataType, item.sourceType)
              item.formatter = ({ cellValue }) => {
                return format(cellValue)
              }
            }
          }
          if (item.columnsConfig) {
            this.executeColumnScript(item)
          }
        })
        if (config.branchType === 'tjbTable') {
          const fieldMap = new Map()
          columns.forEach((ele, index) => {
            fieldMap.set('a' + index, ele.field)
          })
          let list = []
          // 去重
          this.relList.forEach(ele => {
            if (!list.includes(ele.ylabel)) {
              list.push(ele.ylabel)
            }
          })
          // 放入数据
          list = list.map(ele => {
            const obj = { ylabel: ele }
            columns.forEach((column, index) => {
              if (column.field !== 'ylabel') {
                column.field = 'a' + index
                const item = this.relList.find(data => { return obj.ylabel === data.ylabel && fieldMap.get('a' + index) === data.xlabel })
                obj['a' + index] = item?.value || ''
                obj['a' + index + 'Field'] = fieldMap.get('a' + index)
              }
            })
            return obj
          })
          this.relData = list
        }
        if (filterDataEmpty) {
          const fieldList = []
          if (config.branchType === 'tjbTable') {
            this.relData.forEach(data => {
              for (const key in data) {
                if ((Number(data[key]) && data[key]) || (isNaN(data[key] && data[key]))) {
                  fieldList.push(key)
                }
              }
            })
          } else {
            this.relList.forEach(data => {
              for (const key in data) {
                if ((Number(data[key]) && data[key]) || (isNaN(data[key] && data[key]))) {
                  fieldList.push(key)
                }
              }
            })
          }
          columns.forEach(column => {
            if (!fieldList.includes(column.field)) {
              column.visible = false
            }
          })
        }
        if (props.hasSeq) {
          columns.unshift({ type: 'seq', title: '序号', width: props?.seqWidth || 60, fixed: 'left' })
        }
        this.columns = columns || []
        this.props = props
        this.styles = styles
        if (this.props.hasPager && !this.props.isStatic) {
          this.$nextTick(() => {
            if (this.$refs.table) {
              this.$refs.table.$refs.packingTarget.pagerConfig.total = this.total
              this.$refs.table.vxeGridRef.loadData(this.configs.branchType === 'tjbTable' ? this.relData : this.relList) // 需修改
              this.$refs.table.vxeGridRef.loadData(this.configs.branchType === 'tjbTable' ? this.relData : this.relList) // 需修改
            }
          })
        } else {
          if (props?.isStatic) {
            await this.$nextTick()
            if (!this.$refs.table) {
              return
            }
            this.$refs.table.$refs.packingTarget.pagerConfig.currentPage = 1
          } else {
            this.pageParam.pageNum = 1
          }
        }
        if (!this.isChangePage) {
          this.pageParam.pageSize = props.pageSize
        }
      }
    },
    pageChange ({ currentPage, pageSize }) {
      if (this.props.isStatic) {
        return
      }
      if (this.pageParam.pageSize !== pageSize) {
        this.pageParam.pageNum = 1
      } else {
        this.pageParam.pageNum = currentPage
      }
      this.isChangePage = true
      this.pageParam.pageSize = pageSize
      this.debouncedRefreshData(true)
    },
    seqMethod ({ row, rowIndex, column, columnIndex }) {
      if (this.props.hasPager) {
        if (this.props.isStatic) {
          return (this.$refs.table.$refs.packingTarget.pagerConfig.currentPage - 1) * this.$refs.table.$refs.packingTarget.pagerConfig.pageSize + rowIndex + 1
        } else {
          return (this.pageParam.pageNum - 1) * this.pageParam.pageSize + rowIndex + 1
        }
      } else {
        return rowIndex + 1
      }
    },
    sortChart () {
      const tableData = this.$refs.table.$refs.packingTarget.getData()
      tableData.sort((a, b) => {
        const aSum = this.getNum(a)
        const bSum = this.getNum(b)
        if (!this.isDesc) {
          return bSum - aSum
        } else {
          return aSum - bSum
        }
      })
      this.$refs.table.$refs.packingTarget.loadData(tableData)
      this.isDesc = !this.isDesc
    },
    restoreChart (configs) {
      this.customResetChart(configs)
    },
    getNum (obj) {
      let num = 0
      for (const key in obj) {
        if (!isNaN(obj[key])) {
          num += Number(obj[key])
        }
      }
      return num
    },
    syncColumn (id) {
      if (this.configs.chartId !== id) {
        return
      }
      const list = this.$refs.table.$refs.packingTarget.getColumns()
      const column = list.map(ele => {
        return { width: ele?.resizeWidth || ele?.width || 0, field: ele.property }
      })
      emitter.emit('resizeChange', column)
    },
    buildSeriesConfig () {
      const columns = []
      let fieldList = []
      for (let i = 0; i < this.relList.length; i++) {
        const keys = Object.keys(this.relList[i])
        fieldList = [...fieldList, ...keys]
      }
      fieldList = Array.from(new Set(fieldList))
      for (let i = 0; i < fieldList.length; i++) {
        columns.push({ field: fieldList[i], title: '列' + (i + 1), width: 100, align: 'center' })
      }
      return columns
    },
    executeColumnScript (column) {
      const func = new Function('column', column.columnsConfig)
      const value = func(column)
      return value
    }
  }
}
</script>
<style lang="scss" scoped>
/* stylelint-disable custom-property-pattern */
.ef-table-outer-wrapper {
  background-color: transparent !important;

  :deep() {

    .ef-table-inner-wrapper {
      padding: 0 10px 10px;

      .vxe-grid {
        padding: 0;
      }

      .vxe-table--render-default .vxe-header--column {
        line-height: 18px;
      }

      .vxe-table--render-default .vxe-header--column:not(.col--ellipsis),
      .vxe-table--render-default .vxe-body--column:not(.col--ellipsis),
      .vxe-table--render-default .vxe-footer--column:not(.col--ellipsis) {
        padding: 0;
      }
    }

    .vxe-table--render-default {

      .vxe-table--header-wrapper {
        font-family: var(--headerfontFamily);
        font-size: calc(var(--headerfontSize) * 1px);
        font-style: var(--headerfontStyle);
        font-weight: var(--headerfontWeight);
        color: var(--headercolor);
        text-decoration: var(--headertextDecoration);
        background-color: var(--headerBg);

        .vxe-header--column.col--ellipsis {
          height: calc(var(--headercellheight) * 1px);
        }
      }

      .vxe-body--column.col--ellipsis {
        height: calc(var(--tablecellheight) * 1px);
      }

      .vxe-body--row.row--stripe {
        background-color: var(--stripeColor);
      }

      .vxe-header--column {
        font-weight: var(--headerfontWeight);
      }

      .vxe-table--body-wrapper {
        font-family: var(--bodyfontFamily);
        font-size: calc(var(--bodyfontSize) * 1px);
        font-style: var(--bodyfontStyle);
        font-weight: var(--bodyfontWeight);
        color: var(--bodycolor);
        text-decoration: var(--bodytextDecoration);
        background-color: var(--bodyBg);
      }

      .vxe-table--body {
        background-color: var(--bodyBg);
      }

      .vxe-body--row.row--current {
        background-color: var(--rowCurrentColor);
      }

      /* 边框线 */
      .vxe-table--border-line {
        border: 1px solid var(--borderColor);
      }

      .vxe-table--header-border-line {
        border-bottom: 1px solid var(--borderColor);
      }

      /* 边框 */
      .vxe-table--footer-wrapper {
        border-top: 1px solid var(--borderColor);
      }

      &.border--default,
      &.border--full,
      &.border--none,
      &.border--inner,
      &.border--outer {

        .vxe-table--header-wrapper {
          background-color: var(--headerBg);
        }
      }

      &.border--default,
      &.border--inner {

        .vxe-header--column,
        .vxe-body--column,
        .vxe-footer--column {
          background-image: linear-gradient(var(--borderColor), var(--borderColor));
        }
      }

      &.border--full {

        .vxe-header--column,
        .vxe-body--column,
        .vxe-footer--column {
          background-image: linear-gradient(var(--borderColor), var(--borderColor)), linear-gradient(var(--borderColor), var(--borderColor));
        }

        .vxe-table--fixed-left-wrapper {
          // border-right: 1px solid var(--borderColor);
          .vxe-body--column {
            border-right-color: var(--borderColor);
          }
        }
      }

      &.border--default,
      &.border--full,
      &.border--outer,
      &.border--inner {

        .vxe-table--header-wrapper {

          .vxe-header--row {

            &:last-child {

              .vxe-header--gutter {
                background-image: linear-gradient(var(--borderColor), var(--borderColor));
              }
            }
          }
        }
      }

      &.border--inner,
      &.border--none {

        .vxe-table--fixed-left-wrapper {
          border-right: 0;
        }
      }

      &.border--inner {

        .vxe-table--border-line {
          border-width: 0 0 1px;
        }
      }

      &.border--none {

        .vxe-table--border-line {
          display: none;
        }

        .vxe-table--header-border-line {
          display: none;
        }
      }
    }
  }
}
</style>
