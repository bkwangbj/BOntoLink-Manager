<template>
  <el-collapse>
    <el-collapse-item
      name="1"
    >
      <template #title>
        <div>
          <span
            class="collapse-title-common "
            style="padding-left: 16px;"
          >
            查询 </span>
        </div>
      </template>
      <el-form
        class="inline-col-form query-p-form"
        :disabled="!saveAble"
      >
        <el-form-item
          label="宽度"
        >
          <el-input-number
            v-model="form['--width']"
            size="small"
            controls-position="right"
            class="input-number-box-px"
            @change="$emit('chartChange')"
          />
        </el-form-item>
        <el-form-item
          label="位置"
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
                  v-model="form['--right']"
                  size="small"
                  controls-position="right"
                  class="input-number-box-px"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 右偏移</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form['--top']"
                  size="small"
                  controls-position="right"
                  class="input-number-box-px"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 上偏移</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </el-collapse-item>
  </el-collapse>
</template>

<script>
export default {

  name: 'QueryPositionConfig',

  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {

      form: { '--right': 0, '--top': 3, '--width': 120 }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    setFormData (e) {
      Object.assign(this.$data, this.$options.data())
      if (Object.keys(e).length !== 0) {
        this.form = { ...e }
      }
    },
    saveFormData () {
      return { ...this.form }
    }
  }
}
</script>
<style lang="scss" scoped>
.padding-config {
  display: flex;
  margin-bottom: 10px;
}

.query-p-form {

  :deep(.el-form-item) {
    margin-bottom: 10px !important;
  }
}

:deep(.el-collapse-item__wrap) {
  padding: 10px;
  // margin-right: 10px;
  margin-right: 0;
  margin-bottom: 10px;
  background: #f7f7f7;
}
</style>
