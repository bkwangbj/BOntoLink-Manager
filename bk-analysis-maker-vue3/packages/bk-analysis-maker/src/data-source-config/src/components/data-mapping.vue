<template>
  <div class="data-mapping-wrapper">
    <div class="data-source-title">
      <span>{{ showSelectData ? '③':'②' }} 数据映射</span>
      <div
        class="icon"
        @click="$emit('close')"
      >
        <i-ri-menu-unfold-line />
      </div>
    </div>
    <div class="mapping-panel">
      <div
        class="mapping-tools"
      >
        <span>图表配置</span>

        <el-tooltip
          class="item"
          effect="dark"
          content="新增"
          placement="bottom"
        >
          <el-button
            v-if="setMode"
            size="small"
            plain
            @click="addItem"
          >
            <el-icon><CirclePlus /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
      <div class="mapping-form">
        <div>
          <EfForm
            ref="form"
            size="small"
            label-width="120px"
            :rules="rules"
            :items="finalItems"
            :has-reset="false"
            :has-submit="false"
            :disabled="!setMode"
          />
        </div>
        <div
          v-if="addItems.length"
          class="add-form"
        >
          <div
            v-for="(item,i) in addItems"
            :key="i"
            class="add-form-item"
          >
            <div style="margin-bottom: 5px;">
              <el-input
                v-model="item.field"
                :disabled="!setMode"
                placeholder="字段名称"
                size="small"
                style="flex: 1;"
              />
              <i
                v-if="setMode"
                class="am-iconfont icon-yangshi"
                style="margin-left: 5px;font-size: 12px;color: #7a7a7a;cursor: pointer;"
                title="过滤器"
                @click="filterSet(item)"
              />
            </div>
            <div>
              <el-input
                v-model="item.value"
                :disabled="!setMode"
                placeholder="映射字段名称"
                size="small"
                style="flex: 1;"
              />
              <i-ri-close-circle-line
                v-if="setMode"
                style="margin-left: 5px;font-size: 12px;color: #7a7a7a;cursor: pointer;"
                title="过滤器"
                @click="removeItem(i)"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="analysis-modal-footer">
        <el-button
          type="primary"
          size="small"
          :disabled="!setMode"
          @click="save"
        >
          保存
        </el-button>
        <!-- <el-button
          @click="$emit('close')"
          size="small"
          class="analysis-modal-close-button"
        >
          关闭
        </el-button> -->
      </div>
    </div>
    <FilterConfig
      v-if="filterConfigVisible"
      :visible="filterConfigVisible"
      :item="filterItem"
      :set-mode="setMode"
      :mapping-data="filterMappingData"
      @close="filterConfigVisible=false"
      @save="saveFilter"
    />
  </div>
</template>
<script lang="jsx">
import FilterConfig from './filter-config.vue'
export default {
  components: { FilterConfig },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    setMode: {
      type: Boolean,
      default: true
    },
    items: {
      type: Array,
      default: () => []
    },
    rules: {
      type: Object,
      default: () => {}
    },
    mappingData: {
      type: Object,
      default: () => {}
    },
    dataSourceType: {
      type: String,
      default: ''
    }
  },
  emits: ['close', 'save'],
  data () {
    return {
      filterConfigVisible: false,
      filterItem: '',
      filterMappingData: {},
      addItems: []
    }
  },
  computed: {
    finalItems () {
      return this.items.filter(item => !item.isAdd).map(item => {
        return {
          ...item,
          label: `${item.label}(${item.field})`,
          itemClass: 'item-btn',
          itemSlots: {
            label: () => {
              return [
              <div style="display:flex;justify-content: space-between;align-items: center;">
                <span>{`${item.label}(${item.field})`}</span>
                <i class="am-iconfont icon-yangshi" style="cursor:pointer;color:#7A7A7A;font-size: 12px;" title="过滤器" onClick={() => this.filterSet(item)}></i>
              </div>
              ]
            }
          }
        }
      })
    },
    showSelectData () {
      return this.dataSourceType === 'tjb' || this.dataSourceType === 'watf'
    }
  },
  created () {
    this.addItems = this.items.filter(c => c.isAdd)
  },
  mounted () {
    this.$nextTick(() => {
      const data = {}
      for (let i = 0; i < this.finalItems.length; i++) {
        data[this.finalItems[i].field] = this.mappingData[this.finalItems[i].field]
        data[`${this.finalItems[i].field}_filter`] = this.mappingData[`${this.finalItems[i].field}_filter`]
      }
      this.$refs.form.setFormData(data)
    })
  },
  methods: {
    addItem () {
      this.addItems.push({ isAdd: true, field: '', value: '' })
    },
    removeItem (index) {
      this.addItems.splice(index, 1)
    },
    filterSet (item) {
      event.stopPropagation()
      this.filterItem = item
      if (item.isAdd) {
        this.filterMappingData = item
      } else {
        this.filterMappingData = this.$refs.form.getFormData()
      }
      this.filterConfigVisible = true
    },
    saveFilter (filter, item) {
      if (item.isAdd) {
        this.filterMappingData[`${item.field}_filter`] = filter
      } else {
        this.$refs.form.setFormData({ [`${item.field}_filter`]: filter })
      }
    },
    async save () {
      const valid = await this.$refs.form.validate()
      if (valid) {
        const data = this.$refs.form.getFormData()
        for (let i = 0; i < this.addItems.length; i++) {
          if (this.addItems[i].field) {
            data[this.addItems[i].field] = this.addItems[i].value
            data[`${this.addItems[i].field}_filter`] = this.addItems[i][`${this.addItems[i].field}_filter`]
          }
        }
        this.$emit('save', data, this.addItems)
      }
    }
  }
}

</script>

<style lang="scss" scoped>
:deep() {

  .item-btn .el-form-item__content {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .filter-btn {
      padding-left: 5px;
    }
  }
}

.mapping-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: calc(100% - 32px);
  border-left: 1px solid #e6e6e6;

  .mapping-tools {
    display: flex;
    align-items: center;
    padding: 14px 16px;
    // justify-content: space-between;
    border-bottom: 1px solid #e8eaec;

    > span {
      color: #1a1a1a;
    }

    .el-button {
      padding: 0;
      margin-left: 2px;
      font-size: 14px;
      // border-color: #1f6aff;
      color: #1f6aff;
      border-color: transparent;
      border-radius: 4px;
    }
  }

  .mapping-form {
    display: flex;
    flex: 1;
    flex-direction: column;
    padding: 10px 12px;
    overflow: auto;

    :deep(.ef-form-wrapper) {
      padding: 14px;
      background: #f7f7f7;
      border-radius: 4px;

      > .el-form {
        overflow: hidden;
      }

      .el-form-item {
        display: flex;
        flex-direction: column;
        margin-bottom: 14px;

        &:last-child {
          margin-bottom: 0;
        }

        .el-form-item__label {
          width: 100% !important;
          padding: 0 5px 0 0 !important;
        }

        .el-form-item__content {
          margin: 0 !important;
        }
      }
    }
  }

  .add-form {
    display: flex;
    flex-direction: column;
    padding: 14px;
    margin-top: 12px;
    background: #f7f7f7;
    border-radius: 4px;
  }

  .add-form-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    &:not(:last-child) {
      margin-bottom: 10px;
    }

    > div {
      display: flex;
      align-items: center;
    }
  }
}

.data-mapping-wrapper {
  width: 100%;
  height: 100%;

  .data-source-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 32px;
    padding: 0 12px;
    font-size: 12px;
    color: #0a0a0a;
    background: #f5f5f5;
    border: 1px solid #e6e6e6;
    border-right: none;

    .icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 25px;
      height: 25px;
      cursor: pointer;
      background: #ededed;
      border-radius: 4px;

      &:hover {
        background: #e1e1e1;
      }
    }
  }
}
</style>
