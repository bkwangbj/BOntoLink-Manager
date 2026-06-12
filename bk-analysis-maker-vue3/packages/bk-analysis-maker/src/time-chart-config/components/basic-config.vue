<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      size="small"
      :disabled="!saveAble"
      label-width="80px"
    >
      <el-form-item label="轮播">
        <el-switch
          v-model="form.carousel"
          class="am-switch active-switch"
          size="small"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-form-item label="间隔时间">
        <el-input-number
          v-model="form.interval"
          controls-position="right"
          size="small"
          class="input-number-box-s"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-form-item label="停留时间">
        <el-input-number
          v-model="form.retention"
          controls-position="right"
          size="small"
          class="input-number-box-s"
          @change="$emit('chartChange')"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { utils } from 'efficient-suite'
export default {
  inject: ['getSaveAble'],
  props: {
    formData: {

      type: Object,
      default: () => {}
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      form: {
        retention: 3,
        interval: 3,
        carousel: false
      }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    formData: {
      immediate: true,
      handler () {
        this.$nextTick(() => {
          if (this.formData) {
            if (JSON.stringify(this.formData !== '{}')) {
              this.setFormData(utils.deepClone(this.formData))
            }
          }
        })
      }
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())
      Object.keys(form).forEach(key => {
        this.form[key] = form[key]
      })
    },

    saveFormData () {
      const form = utils.deepClone(this.form)
      return form
    }
  }
}
</script>
<style  lang="scss" scoped>
.text-style-form > .el-form-item__content {
  margin-left: 0 !important;
}

.d-flex-c {
  display: flex;
  flex-direction: column;
}

.ai-c {
  align-items: center;
}

.d-flex {
  display: flex;
}

</style>
