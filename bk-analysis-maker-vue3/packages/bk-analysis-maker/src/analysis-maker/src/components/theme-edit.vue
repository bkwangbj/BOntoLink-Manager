<template>
  <EfModal
    v-model="modalVisible"
    width="600px"
    height="450px"
    title="主题另存为"
    @close="$emit('close')"
  >
    <div class="query-content">
      <div class="query-panel">
        <EfForm
          ref="form"
          size="small"
          label-width="120px"
          :rules="rules"
          :items="items"
          :has-reset="false"
          :has-submit="false"
        />
      </div>
      <div class="analysis-modal-footer">
        <el-button
          type="primary"
          size="small"
          :loading="loading"
          @click="save"
        >
          保存
        </el-button>
        <el-button
          size="small"
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
import { v4 as uuidv4 } from 'uuid'
import { hasPermission } from '../../../configs/common-func'
import { utils } from 'efficient-suite'
import { ElMessage, ElMessageBox } from 'element-plus'
import { componentConfigs } from '../../../configs'
const defaultData = [{ value: '0', label: '否' }, { value: '1', label: '是' }]
export default {
  inject: ['getPreThemeList'],
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: true
    },
    preTheme: {
      type: Object,
      default: () => {}
    },
    operPermission: {
      type: Array,
      default: () => []
    }
  },
  emits: ['close', 'save', 'refresh'],
  data () {
    return {
      loading: false,
      items: [
        {
          field: 'key',
          label: '主题',
          itemSlots: {
            default: () => {
              return [
              <el-select
              vModel={this.keyValue}
              onChange={(val) => { this.keyChange(val, '1') }}
            >
             {
              this.keyList.map((item) => (
                <el-option
                key={item.value}
                label={item.label}
                value={item.value}
                class="d-flex flex-between"
              >
                <span>{ item.label }</span>
                {
                  this.hasThemeDeletePermission && item.hasDel
                    ? <el-button
                  size="small"
                  text
                  class="c-red"
                  onClick={this.deleteData(item, 'theme')}
                > <el-icon> <Close /> </el-icon></el-button>
                    : null
                }
              </el-option>
              ))
             }

            </el-select>
              ]
            }
          },
          props: { mock: [], clearable: false, onChange: (val) => { this.keyChange(val, '1') } }
        },
        { field: 'name', label: '名称', type: 'input', props: { disabled: false } },
        { field: 'isDefault', label: '是否默认', type: 'radio', props: { mock: defaultData }, value: '0' },
        {
          field: 'mode',
          label: '主题模式',
          itemSlots: {
            default: () => {
              return [
              <el-select
              vModel={this.modeValue}
              onChange={(val) => { this.modeChange(val, '1') }}
            >
             {
              this.modeOptionList.map(item => (
                <el-option
                key={item.value}
                label={item.label}
                value={item.value}
                class="d-flex flex-between"
              >
                <span>{ item.label }</span>
                {this.hasThemeDeletePermission && item.hasDel && this.modeOptionList.length > 2
                  ? <el-button
                  size="small"
                  text
                  class="c-red"
                  onClick={this.deleteData(item, 'mode')}
                ><el-icon>
                <Close />
              </el-icon></el-button>
                  : null}
              </el-option>
              ))
             }

            </el-select>
              ]
            }
          },
          props: { mock: [], clearable: false },
          onChange: (val) => { this.modeChange(val, '1') }
        },
        { field: 'modeName', label: '模式名称', type: 'input', props: { disabled: false } },
        { field: 'isDefaultMode', label: '是否默认模式', type: 'radio', props: { mock: defaultData }, value: '0' },
        {
          field: 'img',
          label: '示例图片',
          itemSlots: {
            default: () => {
              return [
                 <div class="img-item">
              <div class="d-flex ai-c">
                <el-input
                  vModel={this.imgPath}
                  size="small"
                />
                <EfUpload
                  custom={false}
                  auto-upload={true}
                  multiple={false}
                  accept=".jpg,.jpeg,.png,.gif"
                  id-prop="filePath"
                  data={ { branchPath: 'am-image', isRename: '1' } }
                  show-file-list={false}
                  http-request={(e) => { this.handleUpload(e) }}
                >
                  <div style="margin-left: 10px;">
                    <el-button
                      size="small"
                      plain
                    >
                      <el-icon>
                        <Upload class="am-btn-icon" />
                      </el-icon>上传
                    </el-button>
                  </div>
                </EfUpload>
              </div>
            </div>
              ]
            }
          }
        }
      ],
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        modeName: [{ required: true, message: '请输入模式名称', trigger: 'blur' }],
        mode: [{ required: true, message: '请选择模式', trigger: 'change' }],
        key: [{ required: true, message: '请选择主题', trigger: 'change' }],
        isDefault: [{ required: true, message: '请选择是否默认', trigger: 'change' }],
        isDefaultMode: [{ required: true, message: '请选择是否默认模式', trigger: 'change' }]
      },
      imgPath: '',
      preThemeList: [],
      keyList: [],
      modeOptionList: [],
      modeList: [],
      preThemeData: {},
      modeValue: '',
      keyValue: '',
      modalVisible: false
    }
  },
  computed: {
    hasThemeDeletePermission () {
      return hasPermission(this.operPermission, 'theme.delete')
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
      this.preThemeList = this.getPreThemeList()
      const keyList = this.preThemeList.map(item => { return { value: item.key, label: item.name, flag: item.flag, hasDel: true } })
      const newTheme = { value: uuidv4(), label: '新主题' }
      const newMode = { value: uuidv4(), label: '新主题模式' }
      keyList.push(newTheme)
      const preThemeData = this.preThemeList.find(c => c.key === this.preTheme.preTheme)
      this.preThemeData = preThemeData
      const modeList = preThemeData.config.themeList.map(c => { return { value: c.value, label: c.label, hasDel: true } })
      // this.items.find(c => c.field === 'key').props.mock = keyList.filter(c => c.flag !== '0')
      this.keyList = keyList.filter(c => c.flag !== '0')
      const formData = { isDefault: '0', isDefaultMode: '0' }
      if (preThemeData.flag === '0') { // 内置主题
      // this.items.find(c => c.field === 'mode').props.mock = [newMode]
        this.modeOptionList = [newMode]
        formData.key = newTheme.value
        this.keyValue = newTheme.value
        formData.mode = newMode.value
        this.modeValue = newMode.value
      } else {
        this.modeList = utils.deepClone(modeList)
        modeList.push(newMode)
        // this.items.find(c => c.field === 'mode').props.mock = modeList
        this.modeOptionList = modeList
        formData.key = preThemeData.key
        this.keyValue = preThemeData.key
        formData.name = preThemeData.name
        formData.isDefault = preThemeData.isDefault
        formData.mode = this.preTheme.globalCss.themeMode
        this.modeValue = this.preTheme.globalCss.themeMode
        formData.modeName = modeList.find(c => c.value === this.modeValue).label
        formData.isDefaultMode = this.modeValue === preThemeData.config.themeMode ? '1' : '0'
        this.imgPath = preThemeData.config.imgPath || ''
      }
      this.$nextTick(() => {
        this.$refs.form.setFormData(formData)
      })
    },
    keyChange (value) {
      const preThemeData = this.preThemeList.find(c => c.key === value)
      const newMode = { value: uuidv4(), label: '新主题模式' }
      if (preThemeData && preThemeData.flag !== '0') {
        const modeList = preThemeData.config.themeList.map(c => { return { value: c.value, label: c.label, hasDel: true } })
        this.modeList = utils.deepClone(modeList)
        modeList.push(newMode)
        this.modeOptionList = modeList
        // this.items.find(c => c.field === 'mode').props.mock = modeList
      } else {
        this.modeList = []
        // this.items.find(c => c.field === 'mode').props.mock = [newMode]
        this.modeOptionList = [newMode]
      }
      const mode = newMode.value
      const theme = this.preThemeList.find(c => c.key === value)
      let name = ''
      if (theme) {
        name = theme.name
      }
      this.modeValue = mode
      this.$refs.form.setFormData({ name, mode })
      this.modeChange(mode)
    },
    modeChange (value) {
      const mode = this.modeList.find(c => c.value === value)
      let name = ''
      if (mode) {
        name = mode.label
      }
      this.$refs.form.setFormData({ modeName: name })
    },
    handleUpload (e) {
      return componentConfigs.request.upload('/file/uploads', { file: e.files, branchPath: 'am-image', isRename: '1' }, true).then(async (res) => {
        this.imgPath = res[res.length - 1].filePath
        return res
      })
    },
    deleteData (item, type) {
      event.stopPropagation()
      ElMessageBox.confirm('确认删除？').then(() => {
        let themeData = {}
        if (type === 'theme') {
          themeData = { ...item, isDelete: true }
        } else {
          themeData = utils.deepClone(this.preThemeData)
          const index = themeData.config.themeList.findIndex(c => c.value === item.value)
          themeData.config.themeList.splice(index, 1)
          if (themeData.config.themeMode === item.value) {
            themeData.config.themeMode = themeData.config.themeList[0].value
          }
        }
        this.$emit('save', themeData, () => {
          this.loading = false
          ElMessage.success('删除成功')
          this.$emit('refresh')
          this.$emit('close')
        })
      }).catch(() => {})
    },
    async save () {
      const valid = await this.$refs.form.validate()
      if (valid) {
        const formData = this.$refs.form.getFormData()
        const themeData = utils.deepClone(this.preThemeData)
        const isNewTheme = this.preThemeList.find(c => c.key === this.keyValue) === undefined
        themeData.key = this.keyValue
        themeData.name = formData.name
        themeData.isDefault = formData.isDefault
        themeData.flag = '1'
        themeData.config.preTheme = this.keyValue
        const mode = this.modeList.find(c => c.value === this.modeValue)
        const newConfig = {
          globalCss: utils.deepClone(this.preTheme.globalCss),
          pageLayout: utils.deepClone(this.preTheme.pageLayout),
          chartStyle: utils.deepClone(this.preTheme.chartStyle),
          globalCustomStyle: this.preTheme.globalCustomStyle || '',
          customScript: this.preTheme.customScript || ''
        }
        if (mode) {
          const oldMode = themeData.config.themeList.find(c => c.value === this.modeValue)
          oldMode.label = formData.modeName
          oldMode.config = { ...oldMode.config, ...newConfig }
        } else { // 新模式
          newConfig.globalCss.themeMode = this.modeValue
          // const beforeMode = themeData.config.themeList.find(c => c.value === this.preTheme.globalCss.themeMode)
          // newConfig.chartStyle = beforeMode.config.chartStyle
          const newMode = { value: this.modeValue, label: formData.modeName, config: newConfig }
          if (isNewTheme) { // 新主题
            themeData.config.themeList = [newMode]
            themeData.config.themeMode = this.modeValue
          } else {
            themeData.config.themeList.push(newMode)
          }
        }
        if (formData.isDefaultMode === '1') {
          themeData.config.themeMode = this.modeValue
        }
        themeData.config.imgPath = this.imgPath
        this.loading = true
        this.$emit('save', themeData, () => {
          this.loading = false
          ElMessage.success('保存成功')
          this.$emit('refresh')
          this.$emit('close')
        })
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

  .c-red {
    color: #ff2121 !important;
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
