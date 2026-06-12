<template>
  <div
    v-if="items.length > 0"
    class="query-area"
    :class="{'am-chart-query-area':isChart}"
    :style="queryConfig"
  >
    <EfForm
      v-if="showForm"
      ref="form"
      size="small"
      button-size="small"
      label-width="auto"
      :items="items"
      submit-text="查询"
      :inline="true"
      :has-submit="!isChart"
      :has-reset="!isChart"
      @submit="submit"
      @reset="reset"
    />
  </div>
  <div
    v-else-if="!isChart"
    class="query-empty"
  >
    请配置外部全局参数并开启查询
  </div>
</template>
<script>
import emitter from '../../../configs/emitter'
import { getInitValue } from '../../../configs/common-func'
import { componentConfigs } from '../../../configs'
import { utils } from 'efficient-suite'
export default {
  name: 'QueryArea',
  inject: ['setQueryAreaParams'],

  props: {
    queryItems: {
      type: Array,
      default: () => []
    },
    configs: {
      type: Object,
      default: () => {}
    },
    isChart: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: false
    },
    pageConfig: {
      type: Object,
      default: () => { }
    }
  },
  emits: ['query'],
  data () {
    return {
      inputList: ['input', 'date-picker', 'switch', 'date-range'],
      items: [],
      queryConfig: {
        '--top': 3,
        '--right': 0
      },
      showForm: true,
      initData: {}
    }
  },
  watch: {
    configs: {
      handler () {
        if (this.configs?.queryConfig) {
          this.queryConfig = { ...this.configs.queryConfig }
          if (this.setMode && this.queryConfig['--top'] === 3 && this.queryConfig['--right'] === 0) {
            this.queryConfig['--right'] = 40
          }
        }
      },
      immediate: true
    },
    queryItems: {
      handler (afterData, beforeData) {
        if (JSON.stringify(afterData) !== JSON.stringify(beforeData)) {
          this.init()
        }
      },
      immediate: true
    }
  },
  created () {
    emitter.on('paramsChangeForQuery', this.paramsChange)
  },
  beforeUnmount () {
    emitter.off('paramsChangeForQuery', this.paramsChange)
  },
  methods: {
    paramsChange ({ params, pageConfig }) {
      if (pageConfig && this.pageConfig.id !== pageConfig.id) {
        return
      }
      const data = {}
      for (let i = 0; i < params.length; i++) {
        const param = params[i]
        const p = this.items.find(c => c.field === param.name)
        if (p) {
          data[param.name] = param.value || getInitValue(param.initValue)
          this.initData[param.name] = data[param.name]
        }
      }
      const keys = Object.keys(data)
      if (keys.length > 0) {
        for (let i = 0; i < keys.length; i++) {
          const item = this.items.find(c => c.field === keys[i])
          if (item && item.type === 'date-picker' && item.props.type === 'daterange') {
            const value = data[keys[i]].split(',')
            if (value.length > 1) {
              data[keys[i]] = [value[0], value[1]]
              this.initData[keys[i]] = data[keys[i]]
            }
          }
        }
        this.$refs.form.setFormData(data)
      }
    },
    init () {
      this.items = []
      if (this.queryItems) {
        this.queryItems.map(async (item, index) => {
          const props = item.queryConfig.props ? JSON.parse(item.queryConfig.props) : {}
          let value = getInitValue(item.initValue)
          if (item.queryConfig.showType === 'date-range' && value.split(',').length > 1) {
            value = [value.split(',')[0], value.split(',')[1]]
          }
          const queryItem = {
            label: item.queryConfig.label,
            field: item.name,
            value,
            type: item.queryConfig.showType,
            props: { ...props, placeholder: item.queryConfig.placeholder },
            itemClass: `query-${item.queryConfig.showType} ${props.itemClass || ''}`,
            ord: item.ord
          }
          if (item.isRequired === '1') {
            queryItem.props.clearable = false
          }
          if (this.isChart) {
            queryItem.props.onChange = () => { this.submit() }
          }
          // if (item.queryConfig.showType === 'date-picker') {
          //   queryItem.props.valueFormat = 'yyyy-MM-dd'
          //   queryItem.props.format = 'yyyy-MM-dd'
          // }
          if (item.queryConfig.showType === 'date-range') {
            queryItem.type = 'date-picker'
            queryItem.props.type = 'daterange'
          }
          if (this.inputList.indexOf(item.queryConfig.showType) === -1) {
            if (item.queryConfig.dataSource === 'dic') {
              queryItem.props.dictionaryKey = item.queryConfig.dicKey
              this.items.push(queryItem)
              this.items = this.items.sort((a, b) => { return a.ord - b.ord })
            } else if (item.queryConfig.dataSource === 'static') {
              queryItem.props.mock = JSON.parse(item.queryConfig.staticData)
              if (item.isRequired === '1' && queryItem.props.mock && queryItem.props.mock.length > 0 && !queryItem.value) {
                queryItem.value = queryItem.props.mock[0].value
                this.$emit('query', { name: queryItem.field, value: queryItem.value })
              }
              this.items.push(queryItem)
              this.items = this.items.sort((a, b) => { return a.ord - b.ord })
            } else if (item.queryConfig.dataSource === 'interface' && item.queryConfig.interfaceFunc) {
              this.createFunctionAsync(item.queryConfig.interfaceFunc).then(res => {
                const params = {}
                for (let i = 0; i < this.pageConfig.varConfig.length; i++) {
                  params[this.pageConfig.varConfig[i].name] = this.pageConfig.varConfig[i].value
                }
                res(componentConfigs.request, params, utils).then(data => {
                  queryItem.props.mock = data
                  if (item.isRequired === '1' && queryItem.props.mock && queryItem.props.mock.length > 0 && !queryItem.value) {
                    queryItem.value = queryItem.props.mock[0].value
                    this.$emit('query', { name: queryItem.field, value: queryItem.value })
                  }
                  this.items.push(queryItem)
                  this.items = this.items.sort((a, b) => { return a.ord - b.ord })
                  this.showForm = false
                  setTimeout(() => {
                    this.showForm = true
                  }, 20)
                })
              })
            } else {
              this.items.push(queryItem)
              this.items = this.items.sort((a, b) => { return a.ord - b.ord })
            }
          } else {
            this.items.push(queryItem)
            this.items = this.items.sort((a, b) => { return a.ord - b.ord })
          }
          if (index === this.queryItems.length - 1) {
            this.$nextTick(() => {
              this.setQueryAreaParams()
            })
          }
          this.initData[queryItem.field] = queryItem.value
          return queryItem
        })
        // this.items = items
      } else {
        this.items = []
      }
    },
    createFunctionAsync (functionBody) {
      return new Promise((resolve, reject) => {
        try {
          const func = new Function('request', 'params', 'utils', functionBody)
          resolve(func)
        } catch (error) {
          reject(error)
        }
      })
    },
    submit (form) {
      this.$nextTick(() => {
        if (!form) {
          if (!this.$refs.form) {
            return
          }
          form = this.$refs.form.getFormData()
        }
        this.$refs.form.resetLoading()
        const list = []
        for (const key in form) {
          list.push({ name: key, value: form[key] })
        }
        this.$emit('query', list)
      })
    },
    reset () {
      if (!this.$refs.form) {
        return
      }
      this.$nextTick(() => {
        this.$refs.form.setFormData(this.initData)
        this.submit()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.query-area {
  right: 0;
  display: flex;
  align-items: center;
  height: 100%;
  margin-top: 6px;
  margin-left: 20px;

  :deep() {

    .el-form-item,
    .form-buttons {
      margin-bottom: 6px;
    }

    .el-form-item__label {
      font-family: var(--textStylefontFamily) !important;
      font-size: var(--textStylefontSize) !important;
      font-weight: var(--textStylefontWeight) !important;
      color: var(--textStylecolor) !important;
    }

    .ef-form-wrapper.inline-form .el-form {
      flex-wrap: nowrap;
    }

    .el-date-editor:not(.el-date-editor--daterange) {
      max-width: 120px
    }

  }
}

.query-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-family: var(--textStylefontFamily) !important;
  font-size: var(--textStylefontSize) !important;
  font-weight: var(--textStylefontWeight) !important;
  color: var(--textStylecolor) !important;
}

.am-chart-query-area {
  position: absolute;
  top: calc(var(--top) * 1px);
  right: calc(var(--right) * 1px);
  z-index: 9999;
  width: calc(var(--width) * 1px);
  height: auto;
  margin-top: 0;
}

</style>
