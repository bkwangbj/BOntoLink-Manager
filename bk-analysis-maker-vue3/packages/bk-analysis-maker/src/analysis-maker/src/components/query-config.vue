<template>
  <EfModal
    v-model="modalVisible"
    width="600px"
    height="600px"
    title="查询配置"
    class-name="no-title-bg-modal"
    @close="$emit('close')"
  >
    <div class="query-content">
      <div class="query-panel">
        <EfForm
          ref="form"
          label-width="100px"
          :rules="rules"
          :items="items"
          :has-reset="false"
          :has-submit="false"
          :disabled="!setMode"
        />
      </div>
      <div class="analysis-modal-footer">
        <el-button
          type="primary"
          :disabled="!setMode"
          @click="saveQuery"
        >
          保存
        </el-button>
        <el-button
          class="analysis-modal-close-button"
          @click="$emit('close')"
        >
          关闭
        </el-button>
      </div>
    </div>
  </EfModal>
</template>
<script lang="jsx">
import { ElMessage } from 'element-plus'
const showType = [{ label: '文本输入框', value: 'input' }, { label: '下拉选择', value: 'select' },
  { label: '复选框', value: 'checkbox' }, { label: '单选框', value: 'radio' }, { label: '开关', value: 'switch' }, { label: '日期', value: 'date-picker' }, { label: '日期范围', value: 'date-range' }]
const dataSource = [{ label: '字典', value: 'dic' }, { label: '静态数据', value: 'static' }, { label: '接口', value: 'interface' }]
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: true
    },
    configRow: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['save', 'close'],
  data () {
    return {
      editor: null,
      propsEditor: null,
      inEditor: null,
      items: [
        { field: 'label', label: '名称', type: 'input' },
        { field: 'showType', label: '显示类型', type: 'select', props: { mock: showType, clearable: false, onChange: (val) => { this.showTypeChange(val, '1') } } },
        { field: 'dataSource', label: '数据源', type: 'select', props: { mock: dataSource, clearable: false, onChange: this.dataSourceChange } },
        { field: 'dicKey', label: '字典编码', type: 'input' },
        {
          field: 'staticData',
          label: '静态数据',
          itemSlots: {
            default: () => {
              return [
               <BKCodeCom
               ref={el => { this.editor = el }}
              v-model={this.staticData}
              language="json"
              readonly={!this.setMode}
              isModal={false}
              style="height: 200px;"
            />
              ]
            }
          }
        },
        {
          field: 'interfaceFunc',
          label: '接口',
          itemSlots: {
            default: () => {
              return [
               <BKCodeCom
               ref={el => { this.inEditor = el }}
              v-model={this.interfaceFunc}
              language="json"
              readonly={!this.setMode}
              isModal={false}
              style="height: 200px;"
            />
              ]
            }
          }
        },
        { field: 'placeholder', label: '提示文字', type: 'input' },
        {
          field: 'props',
          label: '其他配置',
          itemSlots: {
            default: () => {
              return [
               <BKCodeCom
              ref={el => { this.propsEditor = el }}
              v-model={this.props}
              language="json"
              readonly={!this.setMode}
              isModal={false}
              style="height: 200px;"
            />
              ]
            }
          }
        }
      ],
      rules: {
        showType: [{ required: true, message: '请选择显示类型', trigger: 'change' }],
        dataSource: [{ required: true, message: '请选择数据源', trigger: 'change' }],
        dicKey: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
        staticData: [{ required: true, validator: this.validateStaticData, trigger: 'blur' }],
        interfaceFunc: [{ required: true, validator: this.validateInterfaceFunc, trigger: 'blur' }]
      },
      inputList: ['input', 'date-picker', 'switch', 'date-range'],
      inputInList: ['select', 'radio', 'checkbox'],
      staticData: '',
      props: '',
      interfaceFunc: '',
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs', overviewRulerBorder: false
      },
      modalVisible: false
    }
  },
  watch: {
    visible:
    {
      handler () {
        this.$nextTick(() => {
          this.modalVisible = this.visible
          if (this.modalVisible) {
            this.$nextTick(() => {
              this.init()
            })
          }
        })
      },
      immediate: true
    }
  },
  mounted () {

  },
  methods: {
    init () {
      let formData = this.configRow.queryConfig
      if (!formData) {
        formData = { showType: 'input', staticData: '', props: '', interfaceFunc: '' }
      }
      this.staticData = formData.staticData
      this.props = formData.props
      this.interfaceFunc = formData.interfaceFunc
      this.$nextTick(() => {
        // this.$refs.editor.init(this.staticData, this.editorCfg)
        // this.$refs.propEditor.init(this.props, this.editorCfg)
        // this.$refs.inEditor.init(this.interfaceFunc, this.editorCfg)
        this.$refs.form.setFormData(formData)
        this.showTypeChange(formData.showType, '0')
      })
    },
    validateStaticData (rule, value, callback) {
      const formData = this.$refs.form.getFormData()
      const staticFlag = formData.dataSource === 'static' && this.inputList.indexOf(formData.showType) === -1
      if (staticFlag && !this.staticData) {
        callback(new Error('请输入静态数据'))
      } else {
        callback()
      }
    },
    validateInterfaceFunc (rule, value, callback) {
      const formData = this.$refs.form.getFormData()
      const staticFlag = formData.dataSource === 'interface' && this.inputList.indexOf(formData.showType) === -1
      if (staticFlag && !this.interfaceFunc) {
        callback(new Error('请输入接口'))
      } else {
        callback()
      }
    },
    showTypeChange (val, type) {
      this.$nextTick(() => {
        const flag = this.inputList.indexOf(val) === -1
        this.$refs.form.setItemVisible({ dataSource: flag })
        this.rules.dataSource[0].required = flag
        const formData = this.$refs.form.getFormData()
        this.dataSourceChange(formData.dataSource)
        if (type === '0' && !formData.placeholder && ['input', 'select', 'date-picker'].indexOf(val) !== -1) {
          this.$refs.form.setFormData({ placeholder: val === 'input' ? '请输入' : '请选择' })
        }
        if (type === '1' && ['input', 'select', 'date-picker', 'switch'].indexOf(val) !== -1) {
          if (val === 'switch') {
            this.props = JSON.stringify({
              activeText: '开',
              inactiveText: '关',
              activeValue: '1',
              inactiveValue: '0'
            })
          } else {
            this.$refs.form.setFormData({ placeholder: val === 'input' ? '请输入' : '请选择' })
          }
        } else if (type === '1') {
          this.$refs.form.setFormData({ placeholder: '' })
        }
      })
    },
    dataSourceChange (val) {
      this.$nextTick(() => {
        const formData = this.$refs.form.getFormData()
        const dicFlag = val === 'dic' && this.inputList.indexOf(formData.showType) === -1
        const staticFlag = val === 'static' && this.inputList.indexOf(formData.showType) === -1
        const inFlag = val === 'interface' && this.inputList.indexOf(formData.showType) === -1
        this.$refs.form.setItemVisible({ dicKey: dicFlag, staticData: staticFlag, interfaceFunc: inFlag })
        this.rules.dicKey[0].required = dicFlag
        this.rules.staticData[0].required = staticFlag
        this.rules.interfaceFunc[0].required = inFlag
        if (staticFlag && this.staticData.length === 0) {
          this.staticData = JSON.stringify([{ label: '选项一', value: '1' }, { label: '选项二', value: '2' }])
        }
      })
    },
    async saveQuery () {
      const valid = await this.$refs.form.validate()
      if (valid) {
        const formData = this.$refs.form.getFormData()
        if (this.inputList.indexOf(formData.showType) === -1 && formData.dataSource === 'static' && !this.staticData) {
          return ElMessage.error('请输入静态数据')
        }
        if (this.inputList.indexOf(formData.showType) === -1 && formData.dataSource === 'interface' && !this.interfaceFunc) {
          return ElMessage.error('请输入接口')
        }
        formData.staticData = this.staticData
        formData.props = this.props
        formData.interfaceFunc = this.interfaceFunc
        this.$emit('save', formData)
        this.$emit('close')
      }
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

.query-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  .query-panel {
    display: flex;
    flex: 1;
    flex-direction: column;
    margin: 20px 0;
    overflow: hidden;
    // background: #f7f7fb;
    // border: 1px solid #e6e6e6;
    // border-radius: 4px 4px 0 0;
    :deep() {

      .el-form {
        padding-right: 20px;
      }
    }
  }
}
</style>
