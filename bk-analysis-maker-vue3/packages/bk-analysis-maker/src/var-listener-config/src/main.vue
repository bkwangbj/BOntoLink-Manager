<template>
  <EfTable
    v-if="varData.length > 0"
    ref="grid"
    :columns="columns"
    :data="varData"
    :has-pager="false"
    :has-seq="true"
    :has-checkbox="true"
    size="small"
    height="auto"
    align="center"
    :edit-config="{
      trigger: 'click',
      mode: 'cell',
      beforeEditMethod: beforeEditMethod
    }"
  />
  <!-- <EfCheckbox
    v-if="varData.length > 0"
    :has-check-all="false"
    :mock="varData"
    v-model="varListener"
    :disabled="!saveAble"
    @change="$emit('change',varListener)"
  /> -->
  <div v-else>
    暂无全局变量
  </div>
</template>
<script lang="jsx">
import emitter from '../../configs/emitter'
import { getVar } from '../../configs/common-func'
import { utils } from 'efficient-suite'
const operatorData = [
  { value: 'eq', label: '等于' },
  { value: 'in', label: '包含' },
  { value: 'notin', label: '不包含' },
  { value: 'gt', label: '大于' },
  { value: 'ge', label: '大于等于' },
  { value: 'lt', label: '小于' },
  { value: 'le', label: '小于等于' }
]
const defaultData = [{ value: '0', label: '否' }, { value: '1', label: '是' }]
export default {
  name: 'VarListenerConfig',
  inheritAttrs: false,
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    pageConfig: {
      type: Object,
      default: () => {}
    },
    saveAble: {
      type: Boolean,
      default: true
    }
  },
  data () {
    return {
      varListener: [],
      varData: [],
      columns: [{
        field: 'name',
        title: '名称',
        width: 150
      },
      {
        field: 'alias',
        title: '别名',
        width: 100,
        editRender: {},
        slots: {
          edit: ({ row }) => {
            return [
              <div>
                <el-input
                  vModel={row.alias}
                  size="small"
                />
              </div>
            ]
          }
        }
      },
      {
        field: 'operator',
        title: '运算符',
        width: 120,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.operator, operatorData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={operatorData}
                  vModel={row.operator}
                  size="small"
                  teleported={false}
                />
              </div>
            ]
          }
        },
        editRender: {}
      },
      {
        field: 'isShowQuery',
        title: '是否显示查询',
        width: 120,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.isShowQuery, defaultData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={defaultData}
                  vModel={row.isShowQuery}
                  size="small"
                  teleported={false}
                />
              </div>
            ]
          }
        },
        editRender: {}
      },
      {
        field: 'ord',
        title: '顺序',
        width: 100,
        editRender: {},
        slots: {
          edit: ({ row }) => {
            return [
              <div>
                <el-input
                  vModel={row.ord}
                  size="small"
                />
              </div>
            ]
          }
        }
      },
      {
        field: 'remark',
        title: '描述',
        minWidth: 100
      }]
    }
  },
  watch: {
    pageConfig: {
      handler () {
        this.varData = this.formatData(this.pageConfig.varConfig || [])
        this.buildData()
      },
      immediate: true
    },
    varData: {
      handler () {
        // setTimeout(() => {
        //   const defalutVar = this.varData.filter(c => c.isDefault === '1' && c.isAdd)
        //   for (let i = 0; i < defalutVar.length; i++) {
        //     if (this.varListener.indexOf(defalutVar[i].id) === -1) {
        //       this.varListener.push(defalutVar[i].id)
        //     }
        //   }
        //   const relVal = []
        //   for (let i = 0; i < this.varListener.length; i++) {
        //     if (this.varData.find(c => c.id === this.varListener[i])) {
        //       relVal.push(this.varListener[i])
        //     }
        //   }
        //   this.varListener = relVal
        //   this.$emit('change', this.varListener)
        // }, 100)
        if (!this.saveAble) {
          setTimeout(() => {
            this.$nextTick(() => {
              const checkList = this.$refs.grid.$el.querySelectorAll('.vxe-cell--checkbox')
              for (let i = 0; i < checkList.length; i++) {
                if (!checkList[i].classList.contains('am-disabled-checkbox')) {
                  checkList[i].classList.add('am-disabled-checkbox')
                }
                if (!checkList[i].classList.contains('is--disabled')) {
                  checkList[i].classList.add('is--disabled')
                }
              }
            })
          }, 1000)
        }
      },
      immediate: true
    },
    'configs.chartId': {
      handler () {
        this.varListener = utils.deepClone(this.configs.varListener) || []
        this.buildData()
      },
      immediate: true
    }
  },
  created () {
    emitter.on('varConfigChange', this.varConfigChange)
  },
  mounted () {
    this.buildData()
  },
  beforeUnmount () {
    emitter.off('varConfigChange', this.varConfigChange)
  },
  methods: {
    varConfigChange (cfg) {
      this.varListener = getVar(this.varListener, cfg)
      this.varData = this.formatData(cfg)
      this.buildData()
    },
    buildData () {
      this.$nextTick(() => {
        if (this.varData.length > 0 || this.varListener.length > 0) {
          const relData = this.varData.map(item => {
            let listen = this.varListener.find(c => c.id === item.id)
            if (!listen) {
              listen = { alias: item.name, operator: 'eq', isShowQuery: '0', ord: '' }
            }
            return { ...item, alias: listen.alias, operator: listen.operator, isShowQuery: listen.isShowQuery, ord: listen.ord || '' }
          })
          this.varData = relData
          const ids = this.varListener.map(item => item.id)
          const list = this.varData.filter(c => ids.indexOf(c.id) !== -1)
          setTimeout(() => {
            this.$refs.grid.vxeGridRef.setCheckboxRow(list, true)
          }, 200)
        } else {
          setTimeout(() => {
            this.$refs.grid && this.$refs.grid.vxeGridRef.clearCheckboxRow()
          }, 200)
        }
      })
    },
    formatData (cfg) {
      const data = cfg.filter(c => c.changeType === 'refreshData')
      return data
    },
    getTypeText (value, list) {
      const data = list.find(c => c.value === value)
      return data ? data.label : ''
    },
    beforeEditMethod () {
      return this.saveAble
    },
    getConfig () {
      if (this.varData.length > 0 && this.$refs.grid) {
        return this.$refs.grid.vxeGridRef.getCheckboxRecords()
      }
      return []
    }
  }
}
</script>
<style lang="scss" scoped>
:deep() {

  .am-disabled-checkbox {
    pointer-events: none;

    &::before {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 99;
      display: inline-block;
      width: 16px;
      height: 17px;
      cursor: not-allowed;
      content: "  ";
      opacity: 0;
    }
  }
}
</style>
