<template>
  <el-form
    :ref="'form-position'"
    :model="form"
    size="small"
    label-width="0px"
    :disabled="!saveAble || !editAble"
  >
    <el-row
      v-show="type!=='absolute'"
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="24"
      >
        <el-form-item label="">
          <div class="d-flex-c">
            <el-radio-group
              v-model="form.x"
              size="small"
              @change="positionChange('x')"
            >
              <el-radio-button value="left">
                左
              </el-radio-button>
              <el-radio-button value="center">
                中
              </el-radio-button>
              <el-radio-button value="right">
                右
              </el-radio-button>
            </el-radio-group>
            <span class="extra-bottom-text"> 水平</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row
      v-show="type!=='absolute'"
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="24"
      >
        <el-form-item label="">
          <div class="d-flex-c">
            <el-radio-group
              v-model="form.y"
              size="small"
              @change="positionChange('y')"
            >
              <el-radio-button value="top">
                上
              </el-radio-button>
              <el-radio-button value="center">
                中
              </el-radio-button>
              <el-radio-button value="bottom">
                下
              </el-radio-button>
            </el-radio-group>
            <span class="extra-bottom-text"> 垂直</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row
      :gutter="10"
      type="flex"
    >
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <PercPxInput
              v-model="form.left"
              default-type="px"
              @change=" deviationChange "
            />
            <span class="extra-bottom-text"> 水平位移</span>
          </div>
        </el-form-item>
      </el-col>
      <el-col
        :span="12"
      >
        <el-form-item>
          <div class="d-flex-c">
            <PercPxInput
              v-model="form.top"
              default-type="px"
              @change=" deviationChange "
            />
            <span class="extra-bottom-text"> 垂直位移</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
import { utils } from 'efficient-suite'
export default {
  name: 'PositionStyle',
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    },
    type: {
      default: '',
      type: String
    },
    modelValue: {
      type: Object,
      default: () => {}

    }
  },
  emits: ['positionStyleChange', 'update:modelValue'],
  data () {
    return {
      form: {
        x: 'center',
        y: 'center',
        left: '',
        top: ''
      }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    modelValue: {
      handler (nv, ov) {
        this.setFormData(this.modelValue)
      }
    }
  },
  methods: {
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
    positionChange (type) {
      if (type === 'x') {
        this.form.left = ''
      } else {
        this.form.top = ''
      }
      this.$emit('update:modelValue', this.form)
      this.$emit('positionStyleChange', this.form)
    },
    deviationChange () {
      this.$emit('update:modelValue', this.form)
      this.$emit('positionStyleChange', this.form)
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

  .el-input__inner {
    text-align: left;
    background: #ededed;
    border: none;
    border-radius: 4px !important;
  }

  .el-input-number {
    position: relative;
    width: 100%;

    .el-input-number__decrease,
    .el-input-number__increase {
      display: none;
    }

    &:hover {

      .el-input-number__decrease,
      .el-input-number__increase {
        display: inline-block;
      }
    }

    &.is-controls-right .el-input__inner {
      padding-right: 10px;
    }
  }

  .el-form-item {
    margin-bottom: 0 !important;
  }
}
</style>
