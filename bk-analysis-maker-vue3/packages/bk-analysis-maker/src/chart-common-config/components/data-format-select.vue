<template>
  <div style="width: 100%;margin-bottom: 10px;">
    <el-form
      ref="form"
      size="small"
      :disabled="!saveAble"
      label-width="80px"
    >
      <el-form-item
        :label="name"
      >
        <template #label>
          <el-popover
            :append-to-body="false"
            placement="bottom-start"
            title=""
            popper-class="tooltip-text"
            width="200"
            trigger="click"
          >
            <div v-show="type==='time'">
              参考文档：<a
                target="_blank"
                href="https://dayjs.fenxianglu.cn/category/display.html#%E6%A0%BC%E5%BC%8F%E5%8C%96"
              >https://dayjs.fenxianglu.cn/category/display.html#格式化</a>
            </div>
            <div v-show="type==='value'">
              显示格式采用一种格式规范的语法，例如 .1f 代表保留一位小数。更多使用方法参考：<a
                target="_blank"
                href="https://github.com/d3/d3-format/blob/v3.1.0/README.md#format"
              >https://github.com/d3/d3-format/blob/v3.1.0/README.md#format</a>
            </div>
            <template #reference>
              <span

                class="label-title"
                size="small"
              >{{ name }}</span>
            </template>
          </el-popover>
        </template>
        <el-select
          v-model="dataType"
          placeholder="请选择"
          filterable
          :filter-method="customFilter"
          allow-create
          :clearable="false"
          size="small"
          @blur="listInitialize"
          @change="changeType"
        >
          <el-option
            v-for="item in options"
            :key="item.value"
            filterable
            allow-create
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'DataFormatSelect',
  inject: ['getSaveAble'],
  props: {
    type: {
      default: 'value',
      type: String
    },
    modelValue: {
      default: '',
      type: String
    },
    name: {
      default: '显示格式',
      type: String
    }
  },
  emits: ['dataTypeChange', 'update:modelValue'],
  data () {
    return {
      dataType: '',
      timeTypeList: [
        {
          value: '',
          label: '默认'
        }, {
          value: 'YYYY年',
          label: '2016年'
        },
        {
          value: 'YYYY',
          label: '2016(年份)'
        },
        {
          value: 'MM月DD日',
          label: '01月01日'
        },
        {
          value: 'MM/DD',
          label: '01/01(月/日)'
        },
        {
          value: 'HH:mm:ss',
          label: '02:30:00'
        },
        {
          value: 'YYYY/MM/DD HH:mm',
          label: '2016/01/01 02:30'
        }
      ],
      dataTypeList: [{
        value: '',
        label: '默认'
      },
      {
        value: '.0f',
        label: '11(整数)'
      },
      {
        value: '.1f',
        label: '11.1(浮点数)'
      },
      {
        value: '.2f',
        label: '11.11(浮点数)'
      },
      {
        value: '.0%',
        label: '11%(百分比)'
      },
      {
        value: '.1%',
        label: '11.1%(百分比)'
      },
      {
        value: ',.0f',
        label: '1,111(千分位)'
      },
      {
        value: ',.1f',
        label: '1,111.1(千分位)'
      }],
      options: []
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }

  },
  watch: {
    type: {
      immediate: true,
      handler () {
        this.listInitialize()
      }
    },
    modelValue: {
      handler () {
        this.dataType = this.modelValue
      },
      immediate: true
    }
  },
  created () {

  },
  methods: {
    async changeType () {
      this.$nextTick(() => {
        this.$emit('update:modelValue', this.dataType)
        this.$emit('dataTypeChange', this.dataType)
      })
    },
    listInitialize () {
      this.options = this.type === 'value' ? this.dataTypeList : this.timeTypeList
    },
    customFilter (query) {
      if (query) {
        let list = this.type === 'value' ? this.dataTypeList : this.timeTypeList
        list = list.filter(item => {
          return item.value.toLowerCase().includes(query.toLowerCase()) || item.label.toLowerCase().includes(query.toLowerCase())
        })
        this.options = list
      } else {
        this.listInitialize()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.d-flex {
  display: flex;
}

.label-title {
  z-index: 2;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-decoration: underline dotted;
  text-overflow: ellipsis;
  cursor: help;
}
</style>
