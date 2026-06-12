<template>
  <div style="width: 100%;">
    <el-form
      :model="form"
      size="small"
      :disabled="!saveAble"
      label-width="80px"
    >
      <el-form-item label="上下偏移">
        <el-input-number
          v-model="form.top"
          size="small"
          class="input-number-box-px"
          controls-position="right"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-form-item label="左右偏移">
        <el-input-number
          v-model="form.left"
          size="small"
          class="input-number-box-px"
          controls-position="right"
          @change="$emit('chartChange')"
        />
      </el-form-item>
      <el-collapse>
        <el-collapse-item name="normal">
          <template #title>
            <div class="collapse-title-common">
              通常样式
            </div>
          </template>

          <el-form-item label="文本样式">
            <text-style
              ref="normalTextRef"
              :config-type="'html'"
              @text-style-change="textStyleChange($event,'normal')"
            />
          </el-form-item>
        </el-collapse-item>
      </el-collapse>
      <CollapseItem
        v-model="form.select.show"
        name="选中样式"
        @change="$emit('chartChange')"
      >
        <el-form-item label="文本样式">
          <text-style
            ref="selectTextRef"
            :disabled="!form.select.show"
            :config-type="'html'"
            @text-style-change="textStyleChange($event,'select')"
          />
        </el-form-item>
      </CollapseItem>
    </el-form>
  </div>
</template>

<script>
import TextStyle from '../../chart-common-config/components/style-config/text-style.vue'
import { utils } from 'efficient-suite'
export default {
  components: {
    TextStyle
  },
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
        top: 0,
        left: 0,
        normal: {

        },
        select: {

          show: false

        }
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
        if (['normal', 'select'].includes(key)) {
          this.form[key] = Object.assign(this.form[key], form[key])
        } else {
          this.form[key] = form[key]
        }
      })
      this.$refs.normalTextRef.setFormData(form.normal)
      this.$refs.selectTextRef.setFormData(form.select || {})
    },
    textStyleChange (textStyle, type) {
      this.form[type] = { ...this.form[type], ...textStyle }

      this.$emit('chartChange')
    },
    saveFormData () {
      const form = utils.deepClone(this.form)
      return form
    },
    getGradientColor (startColor, endColor) {
      return {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: startColor },
          { offset: 1, color: endColor }
        ]
      }
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
