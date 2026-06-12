<template>
  <div
    ref="codeContainer"
    class="bk-code-editor"
  >
    <MonacoEditor
      v-show="!tableVisible"
      ref="editor"
      v-bind="$props"
      v-model:value="currentCode"
      class="content"
      theme="vs-dark"
    />
    <div
      v-if="tableVisible"
      class="content table-content"
      :style="{width: isModal ? '100%': '430px'}"
    >
      <div class="operator-table flex-1">
        <EfTable
          ref="grid"
          :show-header="false"
          :columns="columns"
          :stripe="false"
          :data="tableData"
          :has-pager="false"
          :has-seq="false"
          :show-overflow="false"
          :has-checkbox="!readonly"
          size="small"
          height="auto"
          align="center"
          :edit-config="{
            trigger: 'click',
            mode: 'cell',
            beforeEditMethod: beforeEditMethod
          }"
          :menu-config="readonly ? undefined : menuConfig"
          :row-class-name="rowClassName"
          @menu-click="contextMenuClickEvent"
          @checkbox-change="selectChangeEvent"
        />
      </div>
      <div
        v-if="!readonly"
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
    </div>
    <div
      class="buttons"
      :class="tableVisible ? 'table-buttons' : ''"
    >
      <i-ri-code-line
        v-if="hasTable && tableVisible"
        title="代码模式"
        @click="changeMode"
      />
      <i-ri-grid-fill
        v-if="hasTable && !tableVisible"
        title="列表模式"
        @click="changeMode"
      />
      <el-popover
        v-if="tableVisible && !readonly"
        v-model="excelVisible"
        placement="top"
        width="300"
      >
        <el-input
          v-model="excelContent"
          type="textarea"
          :rows="5"
        />
        <div style=" margin: 0;text-align: right;">
          <el-button
            size="small"
            text
            @click="excelVisible = false"
          >
            取消
          </el-button>
          <el-button
            text
            size="small"
            @click="pasteData"
          >
            确定
          </el-button>
        </div>
        <template #reference>
          <i-ri-file-excel-2-fill
            class="excel-paste-btn"
            title="粘贴Excel数据"
          />
        </template>
      </el-popover>
      <!-- <i
        class="ri-file-excel-2-fill"
        @click="pasteData"
        title="粘贴Excel数据"
        v-if="tableVisible && !readonly"
      /> -->
      <i-ri-file-copy-fill
        title="复制"
        @click="copyCode"
      />
      <i-ri-code-box-line
        title="格式化"
        @click="format"
      />
      <i-ri-fullscreen-fill
        v-if="!isModal"
        title="全屏"
        @click="fullScreenByEditor"
      />
    </div>
    <EfModal
      v-model="visible"
      title="全屏编辑器"
      width="70%"
      height="70%"
      transfer
      @close="closeModal"
    >
      <div class="modal-code-panel">
        <div
          v-if="methodName"
          class="code-bg mtk1"
        >
          <span class="mtk8">function</span> {{ methodName }} {
        </div>
        <CodeCom
          ref="modalEditor"
          v-bind="$props"
          is-modal
          :class="methodName ? 'has-end':''"
          class="modal-code-content"
          :has-table="hasTable"
        />
        <div
          v-if="methodName"
          class="code-bg mtk1 code-close"
        >
          }
        </div>
      </div>
    </EfModal>
  </div>
</template>

<script lang="jsx">
import { v4 as uuidv4 } from 'uuid'
import { utils } from 'efficient-suite'
import { ElMessage, ElMessageBox } from 'element-plus'
import { componentConfigs } from '../../configs'
import MonacoEditor from 'monaco-editor-vue3'
import emitter from '../../configs/emitter'
export default {
  name: 'CodeCom',
  components: {
    MonacoEditor
  },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    language: {
      type: String,
      default: 'xml'
    },
    readonly: {
      type: Boolean,
      default: false
    },
    isModal: {
      type: Boolean,
      default: false
    },
    methodName: {
      type: String,
      default: ''
    },
    hasTable: {
      type: Boolean,
      default: false
    }
  },
  emits: ['blur', 'update:modelValue', 'input'],
  data () {
    return {
      currentCode: this.modelValue,
      visible: false,
      configs: null,
      modalCode: '',
      currentTheme: 'vs',
      tableVisible: false,
      excelVisible: false,
      excelContent: '',
      columns: [],
      tableData: [],
      menuConfig: {
        body: {
          options: [
            [
              { code: 'addTopRow', name: '上面添加一行', prefixIcon: 'ri-skip-up-line' },
              { code: 'addBottomRow', name: '下面添加一行', prefixIcon: 'ri-skip-down-line' },
              { code: 'addLeftCol', name: '左边添加一列', prefixIcon: 'ri-skip-left-line' },
              { code: 'addRightCol', name: '右边添加一列', prefixIcon: 'ri-skip-right-line' },
              { code: 'deleteRow', name: '删除该行', prefixIcon: 'ri-delete-bin-line' },
              { code: 'deleteCol', name: '删除该列', prefixIcon: 'ri-delete-bin-line' }
            ]
          ]
        },
        visibleMethod: this.visibleMethod
      }
    }
  },
  watch: {
    excelVisible () {
      this.excelContent = ''
    },
    modelValue () {
      this.currentCode = this.modelValue
      this.format()
    },
    currentCode (v) {
      this.$emit('update:modelValue', v)
      this.$emit('input', v)
    },
    tableData: {
      handler () {
        this.currentCode = this.buildCodeData()
        this.setValue(this.currentCode)
      },
      deep: true
    },
    columns: {
      handler () {
        this.currentCode = this.buildCodeData()
        this.setValue(this.currentCode)
      },
      deep: true
    }
  },
  created () {
    emitter.on('setEditorTheme', this.setEditorTheme)
  },
  mounted () {
    this.initResizeObserver()
    this.$refs.editor.editor.onDidBlurEditorWidget(() => {
      this.$emit('blur')
    })
  },
  beforeUnmount () {
    this.resizeObserver && this.resizeObserver.unobserve(this.$refs.codeContainer)
    emitter.off('setEditorTheme', this.setEditorTheme)
    if (this.$refs.editor) {
      this.$refs.editor.editor.dispose()
    }
  },
  methods: {
    init (value, configs = { theme: 'vs-dark' }) {
      window.editorCurrentTheme = window.editorCurrentTheme ? window.editorCurrentTheme : 'vs'
      configs.theme = window.editorCurrentTheme
      this.configs = configs
      if (!this.$refs.editor) {
        // loader.init().then(monaco => {
        //   const editor = monaco.editor.create(this.$refs.editor, {
        //     ...configs,
        //     value: value !== undefined ? value : this.currentCode,
        //     language: this.language,
        //     scrollBeyondLastLine: false,
        //     automaticLayout: true,
        //     readOnly: this.readonly,
        //     wordWrap: 'on'
        //   })
        //   this.editor = editor
        //   editor.onDidChangeModelContent(() => {
        //     this.currentCode = editor.getValue()
        //   })
        //   editor.onDidChangeCursorPosition((e) => {
        //     this.currentPosition = e.position
        //   })
        //   editor.onDidBlurEditorWidget(() => {
        //     this.$emit('blur')
        //   })
        // })
      } else {
        // this.$refs.editor.setValue(value || '')
      }
      this.$nextTick(() => {
        if (this.$refs.editor.editor.getValue()) {
          this.format()
        }
      })
      return this.$refs.editor.editor
    },
    setEditorTheme (theme) {
      window.editorCurrentTheme = theme
      // this.$refs.editor.editor.setTheme(theme)
    },
    setValue (value) {
      this.$refs.editor.editor.setValue(value || '')
      if (value) {
        this.format()
      }
    },
    format () {
      this.$refs.editor.editor.trigger('editor', 'editor.action.formatDocument')
    },
    // insertText (text) {
    //   const line = this.currentPosition || { lineNumber: 1, column: 1 }
    //   const range = new monaco.Range(line.lineNumber, line.column, line.lineNumber, line.column)
    //   const id = { major: 1, minor: 1 }
    //   const op = { identifier: id, range, text, forceMoveMarkers: true }
    //   this.editor.executeEdits('my-source', [op])
    // },
    getCurrentCode () {
      if (!this.$refs.editor) return ''
      if (this.$refs.editor.editor.getSelection().isEmpty()) {
        return this.$refs.editor.editor.editorgetValue()
      }
      return this.$refs.editor.editor.getModel().getValueInRange(this.$refs.editor.editor.getSelection())
    },
    copyCode () {
      let code = ''
      if (this.tableVisible) {
        code = this.buildCodeData()
      } else {
        code = this.$refs.editor.editor.getValue()
      }
      componentConfigs.request.copyTextToClipboard(code)
      // navigator.clipboard.writeText(code).then(() => {
      //   ElMessage.success('复制到剪贴板成功！')
      // })
    },
    initResizeObserver () {
      this.resizeObserver = new ResizeObserver(this.listenerContainerHeightCallBack)
      this.resizeObserver.observe(this.$refs.codeContainer)
    },
    listenerContainerHeightCallBack (list) {
      let height = null
      let width = null
      if (list && list[0]) {
        height = list[0].contentRect.height
        width = list[0].contentRect.width
      } else {
        height = this.$refs.codeContainer.clientHeight
        width = this.$refs.codeContainer.width
      }
      this.resize({ width, height })
    },
    resize (size) {
      this.$refs.editor.editor.layout(size)
    },
    fullScreenByEditor () {
      this.visible = true
      this.$nextTick(() => {
        this.modalCode = this.$refs.editor.editor.getValue()
        this.$refs.modalEditor.$refs.editor.editor.setValue(this.modalCode)
        if (this.tableVisible) {
          this.$refs.modalEditor.changeMode()
        }
      })
    },
    changeMode () {
      this.tableVisible = !this.tableVisible
      if (this.tableVisible) {
        this.buildTableData()
      } else {
        this.$nextTick(() => {
          if (this.$refs.editor.editor.getValue()) {
            this.format()
          }
        })
      }
    },
    pasteData () {
      try {
        const rows = this.excelContent.split('\n')
        const reg = /\r/g
        const parsedData = rows.map(row => row.split('\t'))
        const data = []
        for (let i = 0; i < parsedData.length; i++) {
          if (parsedData[i].length === 1 && parsedData[i][0] === '') {
            continue
          }
          const obj = {}
          for (let j = 0; j < parsedData[i].length; j++) {
            if (this.columns.length < j + 1) {
              break
            }
            let value = parsedData[i][j]
            value = value.replace(reg, '')
            value = value.trim().replaceAll(',', '')
            obj[this.columns[j].field] = value
          }
          if (Object.keys(obj).length > 0) {
            data.push(obj)
          }
        }
        if (data.length > 0) {
          this.tableData = [this.tableData[0], ...data]
        }
      } catch (error) {
        ElMessage.info('请粘贴正确的excel数据')
      }
      this.excelVisible = false
    },
    buildCodeData () {
      let code = ''
      if (this.tableData.length > 0) {
        const colData = utils.deepClone(this.tableData[0])
        delete colData._XID
        delete colData._X_ROW_KEY
        const data = this.tableData.slice(1)
        let list = []
        for (let j = 0; j < data.length; j++) {
          const object = {}
          for (const key in colData) {
            if (!this.columns.find(c => c.field === key)) {
              continue
            }
            let value = data[j][key]
            try {
              value = JSON.parse(value)
            } catch (error) {
              value = data[j][key]
            }
            object[colData[key]] = value
          }
          list.push(object)
        }
        if (list.length <= 1 && !this.isArray) {
          list = list[0] || {}
        }
        code = JSON.stringify(list)
      }
      return code
    },
    buildTableData () {
      this.tableData = []
      this.columns = []
      try {
        this.isArray = true
        let data = JSON.parse(this.$refs.editor.editor.getValue())
        if (!Array.isArray(data)) {
          data = [data]
          this.isArray = false
        }
        const keys = this.getUniqueValues(data)
        const colRow = {}
        this.columns = keys.map(key => {
          const field = uuidv4()
          colRow[field] = key
          for (let i = 0; i < data.length; i++) {
            data[i][field] = typeof (data[i][key]) === 'object' ? JSON.stringify(data[i][key]) : data[i][key]
          }
          return {
            title: key,
            field,
            width: 150,
            editRender: {},
            slots: {
              edit: ({ row }) => {
                return [
                  <div>
                    <el-input
                      vModel={row[field]}
                      size="small"
                    />
                  </div>
                ]
              }
            }
          }
        })
        this.tableData = [colRow, ...data]
      } catch (error) {

      }
    },
    getUniqueValues (array) {
      return Array.from(
        array.reduce((keys, obj) => keys.union(new Set(Object.keys(obj))), new Set())
      )
    },
    visibleMethod ({ options, rowIndex, columnIndex }) {
      const op = options[0].filter(c => c.code === 'addTopRow' || c.code === 'addBottomRow' || c.code === 'deleteRow')
      for (let i = 0; i < op.length; i++) {
        op[i].visible = rowIndex !== 0
      }
      options[0].find(c => c.code === 'deleteCol').visible = this.columns.length > 1
      return !this.readonly
    },
    closeModal () {
      this.currentCode = this.$refs.modalEditor.$refs.editor.editor.getValue()
      this.$refs.editor.editor.setValue(this.currentCode || '')
      if (this.tableVisible) {
        this.buildTableData()
      }
    },
    beforeEditMethod () {
      return !this.readonly
    },
    contextMenuClickEvent ({ menu, rowIndex, columnIndex }) {
      if (menu.code === 'addTopRow') {
        const data = this.getNewRow()
        this.tableData.splice(rowIndex, 0, data)
      } else if (menu.code === 'addBottomRow') {
        const data = this.getNewRow()
        this.tableData.splice(rowIndex + 1, 0, data)
      } else if (menu.code === 'addLeftCol') {
        const data = this.getNewCol()
        this.columns.splice(columnIndex - 1, 0, data)
      } else if (menu.code === 'addRightCol') {
        const data = this.getNewCol()
        this.columns.splice(columnIndex, 0, data)
      } else if (menu.code === 'deleteRow') {
        if (rowIndex !== 0) {
          this.tableData.splice(rowIndex, 1)
        }
      } else if (menu.code === 'deleteCol') {
        if (this.columns.length > 1) {
          this.columns.splice(columnIndex - 1, 1)
        }
      }
    },
    rowClassName ({ rowIndex }) {
      if (rowIndex === 0) {
        return 'col-color'
      }
      return null
    },
    selectChangeEvent ({ checked, rowIndex }) {
      if (rowIndex === 0) {
        if (checked) {
          this.$refs.grid.vxeGridRef.setAllCheckboxRow(true)
        } else {
          this.$refs.grid.vxeGridRef.clearCheckboxRow()
        }
      }
    },
    addRow () {
      this.tableData.push(this.getNewRow())
      this.$refs.grid.vxeGridRef.reloadData(this.tableData)
    },
    getNewRow () {
      const obj = {}
      if (this.columns.length > 0) {
        for (let i = 0; i < this.columns.length; i++) {
          obj[this.columns[i].field] = ''
        }
      }
      return obj
    },
    getNewCol () {
      const field = uuidv4()
      return {
        title: uuidv4(),
        field,
        width: 150,
        editRender: {},
        slots: {
          edit: ({ row }) => {
            return [
              <div>
                <el-input
                  vModel={row[field]}
                  size="small"
                />
              </div>
            ]
          }
        }
      }
    },
    removeRows () {
      const data = this.$refs.grid.vxeGridRef.getCheckboxRecords()
      if (data.length === 0) {
        return ElMessage.error('请勾选需要移除的数据')
      }
      ElMessageBox.confirm('确认移除勾选的数据？').then(async () => {
        for (let i = 0; i < data.length; i++) {
          const index = this.tableData.indexOf(data[i])
          if (index === 0) {
            continue
          }
          this.tableData.splice(index, 1)
        }
        this.$refs.grid.vxeGridRef.reloadData(this.tableData)
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.bk-code-editor {
  position: relative;
  width: 100%;
  height: 100%;

  > .content {
    width: 100%;
    height: 100%;
  }

  .table-content {
    display: flex;
    flex-direction: column;
    // padding: 12px;
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

  .table-buttons {
    bottom: 40px !important;
  }

  .buttons {
    position: absolute;
    right: 16px;
    bottom: 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;

    > svg {
      width: 16px;
      height: 16px;
      font-size: 16px;
      color: #bfbfbf;
      cursor: pointer;

      &:hover {
        color: #1f6aff;
      }
    }

    .excel-paste-btn {
      font-size: 16px;
      color: #bfbfbf;
      cursor: pointer;

      &:hover {
        color: #1f6aff;
      }
    }
  }
}

:deep() {

  .col-color {
    background: #f8f8f9;
  }

  .table-content .vxe-grid {
    padding-left: 0;
  }
}

:deep(.vxe-modal--content) {
  overflow: hidden !important;

}

.modal-code-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  .modal-code-content {
    flex: 1;
  }

  .code-bg {
    padding-left: 20px;
    background-color: #1e1e1e;
  }

  .has-end {

    .buttons {
      bottom: -10px;
      z-index: 99;
    }
  }
}
</style>
