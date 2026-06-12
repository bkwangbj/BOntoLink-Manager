<template>
  <el-form
    :ref="'form-line'"
    :model="form"
    size="small"
    label-width="40px"
    :disabled="!saveAble || !editAble"
    inline
    class="inline-col-form"
  >
    <el-form-item label="">
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="24"
        >
          <div class="d-flex-c">
            <EfRadio
              v-model="form.type"
              is-radio-button
              :mock="typeList"
              size="small"
              class="tab-type"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 类型</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item
      v-if="form.type === 'underline'"
      label=""
    >
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.underlineStyle.lineColor"
              :edit-able="editAble"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中下划线颜色</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.underlineStyle.lineWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中下划线宽度</span>
          </div>
        </el-col>
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
        style="width: 100%;"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.underlineStyle.textColor"
              :edit-able="editAble"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中文字颜色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item
      v-if="form.type === 'splitline'"
      label=""
    >
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.splitlineStyle.lineColor"
              :edit-able="editAble"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 分割线颜色</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.splitlineStyle.lineWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 分割线宽度</span>
          </div>
        </el-col>
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
        style="width: 100%;"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.splitlineStyle.textColor"
              :edit-able="editAble"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中文字颜色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item
      v-if="form.type === 'block'"
      label=""
    >
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.blockStyle.radius"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 圆角</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.blockStyle.padding"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 间距</span>
          </div>
        </el-col>
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.blockStyle.borderWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 边框宽度</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.blockStyle.borderColor"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 边框颜色</span>
          </div>
        </el-col>
      </el-row>

      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.blockStyle.textColor"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中文字颜色</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.blockStyle.backgroundColor"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中背景颜色</span>
          </div>
        </el-col>
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-select
              v-model="form.blockStyle.borderType"
              placeholder="请选择"
              size="small"
              @change="formChange"
            >
              <el-option
                v-for="item in lineTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <span class="extra-bottom-text"> 边框类型</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.blockStyle.selectedBorderWidth"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中下边框宽度</span>
          </div>
        </el-col>
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
        style="width: 100%;"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <CommonColorPicker
              v-model="form.blockStyle.selectedBorderColor"
              :edit-able="editAble"
              @change="formChange"
            />
            <span class="extra-bottom-text"> 选中下边框颜色</span>
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item
      v-if="form.type === 'custom'"
      label=""
    >
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="24"
        >
          <el-form-item>
            <div class="d-flex-c">
              <div class="d-flex ai-c">
                <el-input
                  v-model="form.customStyle.backgroundImage"
                  size="small"
                  @change="formChange"
                />
                <EfUpload
                  :custom="true"
                  :auto-upload="true"
                  :multiple="false"
                  :http-request="handleUpload"
                  :show-file-list="false"
                  accept=".jpg,.jpeg,.png,.gif"
                  id-prop="filePath"
                  :data="{ branchPath: 'am-image', isRename: '1' }"
                >
                  <div style="margin-left: 10px;">
                    <el-button
                      size="small"
                      plain
                      :icon="Upload"
                    >
                      上传
                    </el-button>
                  </div>
                </EfUpload>
              </div>       <span class="extra-bottom-text"> 背景图片</span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row
        :gutter="10"
        type="flex"
        style="width: 100%;"
      >
        <el-col
          :span="24"
        >
          <el-form-item
            label=""
            style="width: 100%;"
          >
            <div class="d-flex-c">
              <EfRadio
                v-model="form.customStyle.backgroundRepeat"
                is-radio-button
                :mock="backgroundRepeatList"
                size="small"
                style="width: 100%;"
                @change="formChange"
              />            <span class="extra-bottom-text">填充方式</span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form-item>
  </el-form>
</template>

<script>
import { fontWeightOptions, lineTypeOptions, backgroundRepeatList } from '../../../configs/chart-cfg'
import { utils } from 'efficient-suite'
import { Upload } from '@element-plus/icons-vue'
import { componentConfigs } from '../../../configs'
export default {
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['tabStyleChange'],
  data () {
    return {
      Upload: shallowRef(Upload),
      typeList: [{ label: '下划线', value: 'underline' }, { label: '分割线', value: 'splitline' }, { label: '选中块', value: 'block' }, { label: '自定义', value: 'custom' }],
      form: {
        type: 'underline', // underline 下划线, splitline 分割线,block 选中块, custom 自定义
        underlineStyle: {
          textColor: '#1f6aff', // 选中文字颜色
          lineColor: '#1f6aff', // 选中下划线颜色
          lineWidth: 2 // 选中下划线宽度
        },
        splitlineStyle: {
          lineWidth: 1, // 分割线宽度
          lineColor: '#d2d3d9', // 分割线颜色
          textColor: '#1f6aff' // 选中文字颜色
        },
        blockStyle: {
          radius: 4, // 圆角
          padding: 0, // 间距
          textColor: '#1f6aff', // 选中文字颜色
          backgroundColor: 'rgba(31,106,255,0.1)', // 选中背景颜色
          borderWidth: 0, // 边框
          borderColor: '#d2d3d9',
          borderType: 'solid',
          selectedBorderWidth: 0,
          selectedBorderColor: '#fff'
        },
        customStyle: {
          backgroundImage: '',
          backgroundRepeat: 'no-repeat'
        }
      },
      fontWeightOptions,
      lineTypeOptions,
      backgroundRepeatList
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    handleUpload (e) {
      return componentConfigs.request.upload('/file/uploads', { file: e.files, branchPath: 'am-image', isRename: '1' }, true).then(async (res) => {
        this.form.customStyle.backgroundImage = res[res.length - 1].filePath
        this.formChange()
        return res
      })
    },
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())
      if (form) {
        for (const i in this.form) {
          if (form[i] !== undefined) {
            this.form[i] = form[i]
          }
        }
      }
    },
    formChange () {
      this.$emit('tabStyleChange', this.form)
    },
    getFormData () {
      const form = utils.deepClone(this.form)
      return form
    }
  }
}
</script>

<style lang="scss" scoped>
:deep() {

  .tab-type .el-radio-group {
    flex-wrap: nowrap;
  }

  .tab-type .el-radio-button__inner {
    padding: 4px 7.5px !important;
  }

  .el-form-item {
    margin-right: 0 !important;
  }
}
</style>
