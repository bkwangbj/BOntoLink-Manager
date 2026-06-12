<template>
  <div class="operator-content">
    <div class="operator-table">
      <EfTable
        ref="grid"
        :columns="columns"
        :data="operatorData"
        :has-pager="false"
        :has-seq="true"
        has-checkbox
        :show-overflow="false"
        size="small"
        height="auto"
        align="center"
        :edit-config="{
          trigger: 'click',
          mode: 'cell',
          beforeEditMethod: beforeEditMethod
        }"
        :menu-config="menuConfig"
        @menu-click="contextMenuClickEvent"
      />
    </div>
    <div
      v-if="saveAble"
      class="operator-tools"
    >
      <el-button
        text
        style="padding: 0;color: #1f6aff;"
        size="small"
        @click="addRow"
      >
        <i-ri-add-circle-fill class="am-btn-icon" />新增
      </el-button>
      <el-button
        text
        style="padding: 0;color: #1f6aff;"
        size="small"
        @click="removeRows"
      >
        <i-ri-close-circle-fill class="am-btn-icon" />删除
      </el-button>
    </div>
    <ScriptConfig
      v-if="scriptConfigVisible"
      :visible="scriptConfigVisible"
      :set-mode="saveAble"
      :operator-data="currentRow"
      @close="scriptConfigVisible=false"
      @save="saveScript"
    />
  </div>
</template>
<script lang="jsx">
import emitter from '../../configs/emitter'
import { v4 as uuidv4 } from 'uuid'
import ScriptConfig from './components/script-config.vue'
import { utils } from 'efficient-suite'
import { ElMessage, ElMessageBox } from 'element-plus'
const operatorTypeData = [{ value: 'download', label: '下载' }, { value: 'detail', label: '查看详情' }, { value: 'sort', label: '排序' }, { value: 'restore', label: '还原' }, { value: 'custom', label: '自定义' }]
const defaultData = [{ value: '0', label: '否' }, { value: '1', label: '是' }]
export default {
  name: 'OperatorConfig',
  components: { ScriptConfig },
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
  emits: ['save'],
  data () {
    return {
      columns: [{
        field: 'name',
        title: '名称',
        width: 150,
        editRender: {},
        slots: {
          edit: ({ row }) => {
            return [
              <div>
                <el-input
                  vModel={row.name}
                  size="small"
                />
              </div>
            ]
          }
        }
      },
      {
        field: 'operatorType',
        title: '类型',
        width: 120,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.operatorType, operatorTypeData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={operatorTypeData}
                  vModel={row.operatorType}
                  onChange={ () => this.typeChange(row)}
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
        field: 'isShow',
        title: '是否显示',
        width: 100,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.isShow, defaultData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={defaultData}
                  vModel={row.isShow}
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
        field: 'modal',
        title: '配置',
        width: 150,
        editRender: {},
        slots: {
          default: ({ row }) => {
            if (row.operatorType === 'detail') {
              return [<div>
                {
                  this.saveAble
                    ? <el-button
                      size="small"
                      onClick={() => { this.openPageConfig(row) }}
                    >配置界面</el-button>
                    : null
                }
                {row.openPage ? <div>{row.openPage.name}</div> : null}
              </div>]
            } else if (row.operatorType === 'custom') {
              return [
                <div>
                  <el-button
                    size="small"
                    onClick={() => this.openScript(row)}
                  >配置脚本</el-button>
                </div>
              ]
            }
          },
          edit: ({ row }) => {
            if (row.operatorType === 'detail') {
              return [<div style="padding:3px 0;">
                {
                  this.saveAble
                    ? <el-button
                      size="small"
                      onClick={() => { this.openPageConfig(row) }}
                    >配置界面</el-button>
                    : null
                }
                {row.openPage
                  ? <div style="padding:3px 0;"> <el-input
                  vModel={row.openPage.name}
                  size="small"
                /></div>
                  : null}
              </div>]
            } else if (row.operatorType === 'custom') {
              return [
                <div>
                  <el-button
                    size="small"
                    onClick={() => this.openScript(row)}
                  >配置脚本</el-button>
                </div>
              ]
            }
          }
        }
      },
      {
        field: 'icon',
        title: '图标',
        minWidth: 100,
        editRender: {},
        slots: {
          edit: ({ row }) => {
            return [
              <div>
                <el-input
                  vModel={row.icon}
                  size="small"
                />
              </div>
            ]
          }
        }
      },
      {
        field: 'isChangePart',
        title: '是否联动',
        width: 100,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.isChangePart, defaultData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={defaultData}
                  vModel={row.isChangePart}
                  size="small"
                  teleported={false}
                />
              </div>
            ]
          }
        },
        editRender: {}
      }],
      operatorData: [],
      currentRow: {},
      scriptConfigVisible: false,
      menuConfig: {
        body: {
          options: [
            [
              { code: 'delete', name: '删除' }
            ]
          ]
        }
      }
    }
  },
  watch: {
    'configs.chartId': {
      handler () {
        this.operatorData = this.configs.operatorConfig ? utils.deepClone(this.configs.operatorConfig) : []
      },
      immediate: true
    }
  },
  created () {
    emitter.on('saveOpenPageConfig', this.saveOpenPageConfig)
  },
  beforeUnmount () {
    emitter.off('saveOpenPageConfig', this.saveOpenPageConfig)
  },
  methods: {
    typeChange (row) {
      if (row.operatorType === 'download') {
        row.icon = 'Download'
        row.name = '下载'
      } else if (row.operatorType === 'detail') {
        row.icon = 'View'
        row.name = '查看详情'
      } else if (row.operatorType === 'sort') {
        row.icon = 'Histogram'
        row.name = '排序'
      } else if (row.operatorType === 'restore') {
        row.icon = 'Refresh'
        row.name = '还原'
      } else {
        row.icon = ''
      }
    },
    openScript (row) {
      this.currentRow = row
      this.scriptConfigVisible = true
    },
    saveScript (scriptContent) {
      this.currentRow.scriptContent = scriptContent
    },
    openPageConfig (row) {
      emitter.emit('openPageConfig', row)
    },
    saveOpenPageConfig (configs) {
      const data = this.operatorData.find(c => c.id === configs.config.id)
      if (data) {
        data.openPage = { name: configs.page.name, id: configs.page.id }
        this.$refs.grid.refresh()
      }
    },
    saveVar () {
      for (let i = 0; i < this.operatorData.length; i++) {
        if (!this.operatorData[i].name) {
          return ElMessage.error('名称不能为空')
        }
        if (!this.operatorData[i].operatorType) {
          return ElMessage.error('类型不能为空')
        }
        if (this.operatorData.find(c => c.id !== this.operatorData[i].id && c.name === this.operatorData[i].name)) {
          return ElMessage.error('名称不能重复')
        }
      }
      this.$emit('save', utils.deepClone(this.operatorData))
    },
    getConfig () {
      // for (let i = 0; i < this.operatorData.length; i++) {
      //   if (!this.operatorData[i].name) {
      //     return ElMessage.error('操作名称不能为空')
      //   }
      //   if (!this.operatorData[i].operatorType) {
      //     return ElMessage.error('操作类型不能为空')
      //   }
      //   if (this.operatorData.find(c => c.id !== this.operatorData[i].id && c.name === this.operatorData[i].name)) {
      //     return ElMessage.error('操作名称不能重复')
      //   }
      // }
      return utils.deepClone(this.operatorData)
    },
    addRow () {
      this.operatorData.push({ id: uuidv4(), operatorType: 'detail', isShow: '1', name: '查看详情', icon: 'View' })
      this.$refs.grid.refresh()
    },
    removeRows () {
      const data = this.$refs.grid.vxeGridRef.getCheckboxRecords()
      if (data.length === 0) {
        return ElMessage.error('请勾选需要移除的数据')
      }
      ElMessageBox.confirm('确认移除勾选的数据？').then(async () => {
        for (let i = 0; i < data.length; i++) {
          const index = this.operatorData.indexOf(data[i])
          this.operatorData.splice(index, 1)
          this.$refs.grid.refresh()
        }
      }).catch(() => {})
    },
    getTypeText (value, list) {
      const data = list.find(c => c.value === value)
      return data?.label
    },
    beforeEditMethod () {
      return this.saveAble
    },
    contextMenuClickEvent ({ menu, rowIndex }) {
      if (menu.code === 'delete') {
        this.operatorData.splice(rowIndex, 1)
        this.$refs.grid.refresh()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.operator-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 12px;
  padding-bottom: 0;
  margin-bottom: 12px;
  background: #f7f7f7;
  border-radius: 4px;

  .operator-tools {
    // padding: 5px;
    // border-top: 1px solid #e8eaec;
    display: flex;
    margin-top: 8px;
    margin-bottom: 12px;
    // justify-content: flex-end;
  }

  .operator-table {
    height: 250px;
  }
}
</style>
