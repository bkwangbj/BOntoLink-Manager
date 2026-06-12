<template>
  <el-container
    v-if="showCfg"
    class="chart-cfg-wrap"
  >
    <el-header
      style="height: 30px !important;"
      class="chart-cfg-header"
    >
      <b>图表配置</b>
      <i
        class="ri-close-fill"
        style="cursor: pointer;"
        @click="$emit('setExpand')"
      />
    </el-header>
    <el-main class="chart-cfg-main">
      <EfForm
        ref="form"
        size="small"
        label-width="80px"
        :rules="rules"
        :items="items"
        :has-reset="false"
        :has-submit="false"
        :disabled="!saveAble"
      />
    </el-main>
    <el-footer class="chart-cfg-footer">
      <el-button
        type="primary"
        size="small"
        :disabled="!saveAble"
        @click="save"
      >
        应用
      </el-button>
    </el-footer>
  </el-container>
  <div
    v-else
    style="overflow: hidden;"
  >
    选择图表进行具体配置
  </div>
</template>

<script lang="jsx">
import { colorList, stateDic } from '../../../configs/chart-cfg'
import { utils } from 'efficient-suite'
export default {
  name: 'Right',
  components: {
  },
  inheritAttrs: false,
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    saveAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['setExpand', 'saveChartCfg'],
  data () {
    return {
      items: [
        { id: 0, type: 'input', label: '标题', field: 'title', group: '基础配置', groupType: 'card' },
        { id: 2, type: 'radio', label: '显示标题', field: 'isShowTitle', group: '基础配置', value: '1', props: { mock: stateDic } },
        { id: 3, type: 'radio', label: '是否反查', field: 'isFC', group: '基础配置', value: '1', props: { mock: stateDic } },
        {
          id: 4,
          label: '',
          field: 'customItem',
          group: '自定义配置',
          groupType: 'card',
          itemClass: 'custom-group',
          itemSlots: {
            default: () => {
              return [
                <template>
                  {this.seriesList.forEach((series, index) => {
                    <div
            key={index}
          >
            <div class="series-header">
              <span>系列{ index + 1 }</span>
              <el-button
                v-if="index === 0"
                size="small"
                text
                class="add-seris-btn"
                onClick={this.addSeris}
              ><el-icon><Plus /></el-icon></el-button>
              <el-button
                v-else
                size="small"
                plain
                text
                class="del-seris-btn"
                onClick={() => { this.delSeris(index) }}
              ><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-form
              model={series}
              label-width="80px"
            >
              <el-form-item label="名称">
                <el-input
                  v-model="series.name"
                  size="small"
                />
              </el-form-item>
              <el-form-item label="类型">
                <el-radio-group v-model="series.type">
                  <el-radio label="bar">
                    柱状图
                  </el-radio>
                  <el-radio label="line">
                    线图
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="单位名称">
                <el-input
                  v-model="series.measureName"
                  size="small"
                />
              </el-form-item>
              <el-form-item
                label="图例颜色"
                style="margin-top: 5px;"
              >
                <el-color-picker
                  v-model="series.color"
                  size="small"
                />
              </el-form-item>
            </el-form>
          </div>
                  })}
                </template>

              ]
            }
          }
        }
      ],
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
      },
      seriesList: []
    }
  },
  computed: {
    showCfg () {
      return (Object.keys(this.configs).length > 0 && !!this.configs.type)
    }
  },
  watch: {
    configs () {
      if (this.configs && this.configs.type) {
        this.$nextTick(() => {
          this.$refs.form && this.$refs.form.setFormData(this.configs)
          if (!this.configs.seriesConfig) {
            this.seriesList = [{ type: 'bar', color: colorList[0] }]
          } else {
            this.seriesList = utils.deepClone(this.configs.seriesConfig)
          }
        })
      }
    }
  },
  created () {
  },
  methods: {
    addSeris () {
      let index = this.seriesList.length
      if (index >= colorList.length) {
        index = ((index + 1) % colorList.length) - 1
      }
      this.seriesList.push({ type: 'bar', color: colorList[index] })
    },
    delSeris (index) {
      this.seriesList.splice(index, 1)
    },
    async save () {
      const valid = await this.$refs.form.validate()
      if (valid) {
        const data = { ...this.$refs.form.getFormData(), seriesConfig: this.seriesList }
        delete data.tabList
        this.$emit('saveChartCfg', data)
        // this.$emit('saveChartCfg', { ...this.$refs.form.getFormData(), seriesConfig: this.seriesList })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.chart-cfg-wrap {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: #f8f9fa;
  border: 1px solid #e0e5ee;
  border-top: none;
  border-bottom: none;
}

.chart-cfg-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px !important;
  border-bottom: 1px solid #e8eaec;
}

.chart-cfg-footer {
  height: 40px !important;
  padding-top: 5px;
  text-align: center;
  border-top: 1px solid #e8eaec;
}

.chart-cfg-main {
  flex: 1;
  height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
}

:deep() {

  .custom-group > .el-form-item__content {
    margin-left: 0 !important;
  }

  .explain-item > .el-form-item__content {
    margin-left: 0 !important;
  }

  .series-header {
    display: flex;
    justify-content: space-between;
  }
}
</style>
