<template>
  <EfModal
    v-model="modalVisible"
    width="1000px"
    height="600px"
    :before-hide-method="beforeHideMethod"
    title="全局参数设置"
    class-name="no-title-bg-modal"
    @close="close"
  >
    <div class="var-content">
      <div class="var-panel">
        <div style="position: relative;">
          <EfTabs
            v-model="currentTab"
            :data="tabs"
            type="border-card"
            class="tabs_box"
          />
          <div
            v-if="setMode"
            class="var-tools"
          >
            <el-button
              type="primary"
              size="small"
              text
              @click="addRow"
            >
              <el-icon class="am-btn-icon">
                <CirclePlus />
              </el-icon>新增
            </el-button>
            <el-button
              type="primary"
              size="small"
              text
              @click="removeRows"
            >
              <el-icon class="am-btn-icon">
                <CircleClose />
              </el-icon> 删除
            </el-button>
          </div>
        </div>
        <div class="var-table">
          <EfTable
            ref="grid"
            :columns="columns"
            :data="tableData"
            :has-pager="false"
            :has-seq="true"
            :has-checkbox="setMode"
            :show-overflow="false"
            :stripe="false"
            size="small"
            height="auto"
            align="center"
            :edit-config="{
              trigger: 'click',
              mode: 'cell',
              beforeEditMethod: beforeEditMethod
            }"
          />
        </div>
      </div>
      <div class="analysis-modal-footer">
        <el-button
          type="primary"
          :disabled="!setMode"
          @click="saveVar"
        >
          保存
        </el-button>
        <el-button
          class="analysis-modal-close-button"
          @click="close"
        >
          关闭
        </el-button>
      </div>
    </div>
    <QueryConfig
      v-if="queryConfigVisible"
      :visible="queryConfigVisible"
      :set-mode="setMode"
      :config-row="queryConfigRow"
      @close="queryConfigVisible=false"
      @save="saveQueryConfig"
    />
  </EfModal>
</template>
<script lang="jsx">
import { v4 as uuidv4 } from 'uuid'
import emitter from '../../../configs/emitter'
import QueryConfig from './query-config.vue'
import { utils } from 'efficient-suite'
import { ElMessage, ElMessageBox } from 'element-plus'
const changeTypeData = [{ value: 'refreshData', label: '刷新数据' }, { value: 'openModal', label: '打开弹窗' }]
const defaultData = [{ value: '0', label: '否' }, { value: '1', label: '是' }]
export default {
  name: 'VarConfig',

  components: { QueryConfig },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: true
    },
    configs: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['close', 'save'],
  data () {
    return {
      tabs: [{ name: 'external', label: '外部变量' }, { name: 'internal', label: '内部变量' }],
      currentTab: 'external',
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
        field: 'initValue',
        title: '初始值',
        width: 100,
        editRender: {},
        slots: {
          edit: ({ row }) => {
            return [
              <div>
                <el-input
                  vModel={row.initValue}
                  size="small"
                />
              </div>
            ]
          }
        }
      },
      {
        field: 'changeType',
        title: '响应类型',
        width: 120,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.changeType, changeTypeData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={changeTypeData}
                  vModel={row.changeType}
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
        field: 'isDefault',
        title: '默认监听',
        width: 100,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.isDefault, defaultData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={defaultData}
                  vModel={row.isDefault}
                  onChange={() => this.defaultChange(row)}
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
        field: 'isRequired',
        title: '是否必填',
        width: 100,
        slots: {
          default: ({ row }) => {
            return [
              <div>
                {this.getTypeText(row.isRequired, defaultData)}
              </div>
            ]
          },
          edit: ({ row }) => {
            return [
              <div>
                <EfSelect
                  clearable={false}
                  mock={defaultData}
                  vModel={row.isRequired}
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
        title: '打开页面',
        width: 150,
        editRender: {},
        slots: {
          default: ({ row }) => {
            if (row.changeType === 'openModal') {
              return [
                <div>
                  {
                    this.setMode
                      ? <el-button
                        size="small"
                        onClick={() => { this.openPageConfig(row) }}
                      >配置界面</el-button>
                      : null
                  }
                  {row.openPage ? <div>{row.openPage.name}</div> : null}
                </div>
              ]
            }
          },
          edit: ({ row }) => {
            if (row.changeType === 'openModal') {
              return [
                <div style="padding:3px 0;">
                  {
                    this.setMode
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
                </div>
              ]
            }
          }
        }
      },
      {
        field: 'isQuery',
        title: '查询配置',
        width: 120,
        slots: {
          default: ({ row }) => {
            if (row.changeType === 'refreshData') {
              return [
                <div>
                  <vxe-switch vModel={row.isQuery} disabled={!this.setMode} size="small" open-value="1" close-value="0" open-label="开启查询" close-label="关闭查询"></vxe-switch>
                  {
                    row.isQuery === '1'
                      ? <el-button
                        size="small"
                        onClick={() => { this.openQueryConfig(row) }}
                      >显示配置</el-button>
                      : null
                  }
                </div>
              ]
            }
          }
        }
      },
      {
        field: 'remark',
        title: '描述',
        minWidth: 100,
        editRender: {},
        slots: {
          edit: ({ row }) => {
            return [
              <div>
                <el-input
                  vModel={row.remark}
                  size="small"
                />
              </div>
            ]
          }
        }
      }],
      varData: [],
      queryConfigRow: {},
      queryConfigVisible: false,
      modalVisible: false
    }
  },
  computed: {
    tableData () {
      return this.varData.filter(c => c.type === this.currentTab)
    }
  },
  watch: {
    currentTab () {
      // const field = this.$refs.grid.getColumnByField('isQuery')
      // if (field) {
      //   field.visible = this.currentTab === 'external'
      //   this.$refs.grid.refreshColumn()
      // }
    },
    visible:
    {
      handler () {
        this.modalVisible = this.visible
      },
      immediate: true
    }
  },
  created () {
    this.varData = this.configs.varConfig ? utils.deepClone(this.configs.varConfig.map(item => { return { ...item, beforeDefault: item.isDefault } })) : []
    emitter.on('saveOpenPageConfig', this.saveOpenPageConfig)
    const data = utils.deepClone(this.varData)
    for (let i = 0; i < data.length; i++) {
      delete data[i]._XID
      delete data[i]._X_ROW_KEY
    }
    this.beforeData = JSON.stringify(data)
  },
  beforeUnmount () {
    emitter.off('saveOpenPageConfig', this.saveOpenPageConfig)
  },
  methods: {
    openQueryConfig (row) {
      this.queryConfigRow = row
      this.queryConfigVisible = true
    },
    saveQueryConfig (config) {
      this.queryConfigRow.queryConfig = config
    },
    openPageConfig (row) {
      emitter.emit('openPageConfig', row)
    },
    saveOpenPageConfig (configs) {
      const data = this.varData.find(c => c.id === configs.config.id)
      if (data) {
        data.openPage = { name: configs.page.name, id: configs.page.id }
        this.$forceUpdate()
      }
    },
    saveVar () {
      for (let i = 0; i < this.varData.length; i++) {
        if (!this.varData[i].name) {
          return ElMessage.error('名称不能为空')
        }
        if (this.varData.find(c => c.id !== this.varData[i].id && c.name === this.varData[i].name && c.type === this.varData[i].type)) {
          return ElMessage.error('名称不能重复')
        }
      }
      const data = utils.deepClone(this.varData)
      for (let i = 0; i < data.length; i++) {
        delete data[i]._XID
        delete data[i]._X_ROW_KEY
      }
      this.$emit('save', data)
      for (let i = 0; i < this.varData.length; i++) {
        this.varData[i].isAdd = false
        this.varData[i].isRemove = false
      }
      this.$emit('close')
    },
    beforeHideMethod () {
      const data = utils.deepClone(this.varData)
      for (let i = 0; i < data.length; i++) {
        delete data[i]._XID
        delete data[i]._X_ROW_KEY
      }
      if (this.beforeData !== JSON.stringify(data)) {
        ElMessageBox.confirm('参数已修改未保存，确认关闭？').then(() => {
          this.$emit('close')
        }).catch(() => {})
        return new Error()
      } else {
        this.$emit('close')
      }
    },
    close () {
      const data = utils.deepClone(this.varData)
      for (let i = 0; i < data.length; i++) {
        delete data[i]._XID
        delete data[i]._X_ROW_KEY
      }
      if (this.beforeData !== JSON.stringify(data)) {
        ElMessageBox.confirm('参数已修改未保存，确认关闭？').then(() => {
          this.$emit('close')
        }).catch(() => {})
      } else {
        this.$emit('close')
      }
    },
    defaultChange (row) {
      if (row.isDefault === '0' && row.beforeDefault === '1') {
        row.isRemove = true
      } else {
        row.isRemove = false
      }
      if (row.isDefault === '1' && row.beforeDefault === '0') {
        row.isAdd = true
      } else {
        row.isAdd = false
      }
      row.beforeDefault = row.isDefault
    },
    addRow () {
      this.varData.push({ id: uuidv4(), changeType: 'refreshData', isRequired: '0', isDefault: '0', isQuery: '0', isAdd: true, beforeDefault: '0', type: this.currentTab })
    },
    removeRows () {
      const data = this.$refs.grid.vxeGridRef.getCheckboxRecords()
      if (data.length === 0) {
        return ElMessage.error('请勾选需要移除的数据')
      }
      ElMessageBox.confirm('确认移除勾选的数据？').then(async () => {
        for (let i = 0; i < data.length; i++) {
          const index = this.varData.indexOf(data[i])
          this.varData.splice(index, 1)
        }
      }).catch(() => {})
    },
    getTypeText (value, list) {
      return list.find(c => c.value === value).label
    },
    beforeEditMethod () {
      return this.setMode
    }
  }
}
</script>

<style lang="scss" scoped>

:deep() {

  .el-button--primary.is-text {
    color: #1f6aff;
  }
}

.tabs_box {
  border-top: none;
  border-right: none;
  border-bottom: none;
  border-left: none;
  box-shadow: 0 0 0 0;

  :deep() {

    .el-tabs__nav-wrap::after {
      height: 1px;
    }

    .el-tabs__header {
      margin: 0;
      background-color: #f7f7fb;
    }

    .el-tabs__nav-wrap {
      padding-top: 5px;
      margin-left: 10px;

      // .el-tabs__nav {
      //   border-left: 1px solid #e4e7ed;
      // }
      .el-tabs__item {
        position: relative;
        height: 34px;
        padding: 0 15px;
        line-height: 30px;
        color: #303133;
        text-align: center;

        &:first-child {
          margin-left: 0;
        }

        &.is-active {
          color: #1777f5;
          border-top-left-radius: 4px;
          border-top-right-radius: 4px;

          &::after {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 2px;
            content: "";
            background: #1777f5;
          }
        }
      }
    }

    .el-tabs__content {
      display: none;
    }
  }
}

.var-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  .var-panel {
    display: flex;
    flex: 1;
    flex-direction: column;
    margin: 10px;
    overflow: hidden;
    background: #f7f7fb;
    border: 1px solid #e6e6e6;
    border-radius: 4px 4px 0 0;
  }

  .var-tools {
    position: absolute;
    top: 5px;
    right: 10px;
    display: flex;
    justify-content: flex-end;
  }

  .var-table {
    display: flex;
    flex: 1;
    flex-direction: column;
    overflow: hidden;

    :deep() {

      .ef-table-inner-wrapper .vxe-grid {
        padding: 8px;
      }
    }
  }
}
</style>
