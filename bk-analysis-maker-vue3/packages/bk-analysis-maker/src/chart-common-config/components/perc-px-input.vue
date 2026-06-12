<template>
  <div class="d-flex full-width">
    <el-input-number
      v-show="type==='px'"
      v-model="number"
      controls-position="right"
      size="small"
      :disabled="!saveAble"
      class="input-number-box-px"
      @change="numberChange"
    />
    <el-input-number
      v-show="type==='%'"
      v-model="number"
      controls-position="right"
      size="small"
      :disabled="!saveAble"
      class="input-number-box-perc"
      @change="numberChange"
    />
    <el-radio-group
      v-if="!defaultType"
      v-model="type"
      style="margin-left: 10px;"
      size="small"
      @change="numberChange"
    >
      <el-radio-button value="%">
        %
      </el-radio-button>
      <el-radio-button value="px">
        px
      </el-radio-button>
    </el-radio-group>
  </div>
</template>

<script>
export default {
  name: 'PercPxInput',
  inject: ['getSaveAble'],
  props: {
    defaultType: {
      default: '',
      type: String
    },
    editAble: {
      type: Boolean,
      default: true
    },
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['change', 'update:modelValue'],
  data () {
    return {
      number: 0,
      type: '%'
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    defaultType: {
      handler (nv, ov) {
        if (this.defaultType) {
          this.type = this.defaultType
        }
      },
      immediate: true
    },
    modelValue: {
      handler (nv, ov) {
        if (typeof this.modelValue === 'string') {
          if (this.modelValue.endsWith('%')) {
            const str = this.modelValue.replace('%', '')
            this.number = Number(str)
            this.type = '%'
          } else if (this.modelValue.endsWith('px')) {
            const str = this.modelValue.replace('px', '')
            this.number = Number(str)
            this.type = 'px'
          } else {
            this.number = undefined
          }
        }
      },
      immediate: true
    }
  },
  methods: {
    numberChange (e) {
      this.$nextTick(() => {
        if (this.number) {
          this.$emit('update:modelValue', this.number + this.type)
          this.$emit('change', this.number + this.type)
        } else {
          this.$emit('update:modelValue', '')
          this.$emit('change', '')
        }
      })
    }
  }
}
</script>
<style lang="scss" scoped>
:deep(.el-select) {

  .el-input__prefix {
    top: 2px;
    left: 2px;
    box-sizing: border-box;
    display: inline-block;
    width: calc(100% - 3px);
    height: calc(100% - 3px);
    padding: 0 15px;
    color: #606266;
    cursor: pointer;
    background-color: #fff;
    background-image: none;
    border-radius: 4px;
    outline: 0;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
    appearance: none;
  }
}

</style>
