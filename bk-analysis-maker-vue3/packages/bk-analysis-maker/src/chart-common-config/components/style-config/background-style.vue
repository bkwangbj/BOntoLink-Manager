<template>
  <el-form
    :ref="'form-line'"
    :model="form"
    size="small"
    label-width="0px"
    :disabled="!saveAble || !editAble"
    inline
    class="inline-col-form"
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
                v-model="form.backgroundImage"
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
    >
      <el-col
        :span="24"
      >
        <el-form-item
          label=""
          style="width: 100%;"
          class="radio-item"
        >
          <div class="d-flex-c">
            <EfRadio
              v-model="form.backgroundRepeat"
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
  </el-form>
</template>

<script>
import { backgroundRepeatList } from '../../../configs/chart-cfg'
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
  emits: ['backgroundStyleChange'],
  data () {
    return {
      Upload: shallowRef(Upload),
      form: {
        backgroundRepeat: 'repeat',
        backgroundImage: ''
      },
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
        this.form.backgroundImage = res[res.length - 1].filePath
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
      const form = utils.deepClone(this.form)
      this.$emit('backgroundStyleChange', form)
    },
    getFormData () {
      const form = utils.deepClone(this.form)
      return form
    }
  }
}
</script>
