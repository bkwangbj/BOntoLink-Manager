<template>
  <el-collapse
    v-model="activeItem"
    class="event-collapse"
  >
    <el-collapse-item
      v-for="(event,i) in eventData"
      :key="i"
      :disabled="!event.isActive"
      :name="i.toString()"
    >
      <template #title>
        <div style="display: flex;align-items: center;">
          <span @click.stop="">
            <el-switch
              v-model="event.isActive"
              :disabled="!saveAble"
              class="am-switch active-switch"
              size="small"
              @change="eventChange(i.toString(), $event)"
            />
          </span>
          {{ event.title }}
          <i-ri-add-circle-fill
            v-if="event.isActive && saveAble && activeItem.includes(i.toString())"
            style="margin-left: 7px;color: #1f6aff;cursor: pointer;"
            @click.stop="addItem(event)"
          />
        </div>
      </template>
      <div
        v-if="event.isActive"
        class="event-form"
      >
        <div class="event-form-item">
          <div
            v-if="configs && (configs.type === 'BKTableChart' || configs.isTable)"
            class="event-form-item-header"
            style="width: 120px;"
          >
            列字段名称/第几列
          </div>
          <div
            class="event-form-item-header"
            style="width: 120px;"
          >
            字段名称
          </div>
          <div class="event-form-item-header">
            更新至变量
          </div>
        </div>
        <div
          v-for="(item,j) in getEventItems(event.items, undefined)"
          :key="j"
          class="event-form-item"
        >
          <div class="event-form-item-field">
            {{ item.field }}
          </div>
          <div
            class="event-form-item-select"
            style="flex: 1;"
          >
            <EfSelect
              v-model="item.varField"
              size="small"
              :mock="varData"
              :disabled="!saveAble"
              style="width: 100%;"
            />
          </div>
        </div>
        <div
          v-for="(item,j) in getEventItems(event.items, true)"
          :key="'add-'+j"
          class="event-form-item"
        >
          <div
            v-if="configs && (configs.type === 'BKTableChart' || configs.isTable)"
            class="event-form-item-field"
            style="padding-right: 13px;"
          >
            <EfInput
              v-model="item.columnField"
              :disabled="!saveAble"
              placeholder="列字段名称/第几列"
              size="small"
            />
          </div>
          <div
            class="event-form-item-field"
            style="padding-right: 13px;"
          >
            <EfInput
              v-model="item.field"
              :disabled="!saveAble"
              size="small"
              placeholder="字段名称"
            />
          </div>
          <div
            class="event-form-item-select"
            style="flex: 1;"
          >
            <div
              class="event-form-item-group"
            >
              <EfSelect
                v-model="item.varField"
                style="width: 100%;"
                size="small"
                :mock="varData"
                :disabled="!saveAble"
              />
              <i-ri-delete-bin-4-fill
                v-if="saveAble"
                class="item-remove-btn"
                style="color: #7a7a7a;"
                @click="removeItem(event, j)"
              />
            </div>
          </div>
        </div>
      </div>
    </el-collapse-item>
  </el-collapse>
</template>
<script>
import { getEvent } from '../../configs/common-func'
import emitter from '../../configs/emitter'
import { utils } from 'efficient-suite'
export default {
  name: 'EventConfig',
  inheritAttrs: false,
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    pageConfig: {
      type: Object,
      default: () => {}
    },
    saveAble: {
      type: Boolean,
      default: true
    }
  },
  data () {
    return {
      varData: [],
      eventData: [],
      activeItem: []
    }
  },
  watch: {
    pageConfig: {
      handler () {
        this.varData = this.formatData(this.pageConfig.varConfig || [])
      },
      immediate: true
    },
    'configs.chartId': {
      handler () {
        this.eventData = this.configs.eventConfig ? utils.deepClone(this.configs.eventConfig) : []
      },
      immediate: true
    }
  },
  created () {
    emitter.on('varConfigChange', this.varConfigChange)
  },
  mounted () {
  },
  beforeUnmount () {
    emitter.off('varConfigChange', this.varConfigChange)
  },
  methods: {
    getEventItems (items, isAdd) {
      return items ? items.filter(c => c.isAdd === isAdd) : []
    },
    addItem (event) {
      const data = { isAdd: true, field: '', varField: '' }
      if (this.configs.type === 'BKTableChart') {
        data.columnField = ''
      }
      event.items.push(data)
    },
    removeItem (event, i) {
      const items = this.getEventItems(event.items, true)
      const item = items[i]
      event.items.splice(event.items.indexOf(item), 1)
    },
    varConfigChange (cfg) {
      this.varData = this.formatData(cfg)
      this.eventData = getEvent(this.eventData, cfg)
    },
    formatData (data) {
      return data.filter(c => c.type === 'internal').map(c => { return { ...c, value: c.id, label: `${c.name}${c.remark ? '(' + c.remark + ')' : ''}` } })
    },
    eventChange (key, value) {
      if (value) {
        this.activeItem.push(key)
      } else {
        const i = this.activeItem.findIndex(item => item === key)
        this.activeItem.splice(i, 1)
      }
    },
    getConfig () {
      return this.eventData
    }
  }
}
</script>

<style lang="scss" scoped>
  :deep() {

    .event-form {
      display: flex;
      flex-direction: column;
      padding: 12px 18px;
      padding-right: 31px;
      margin-bottom: 12px;
      background: #f7f7f7;
      border-radius: 2px;

      .event-form-item {
        display: flex;
        align-items: center;
        margin-right: 0;

        &:not(:last-child) {
          margin-bottom: 14px;
        }

        .event-form-item-header {
          color: #a3a3a3;
        }
      }

      .event-form-item-group {
        position: relative;
        display: flex;
        align-items: center;
      }

      .event-form-item-field {
        flex: 0 0 120px;
      }

      .item-remove-btn {
        position: absolute;
        // top: 3px;
        right: -20px;
        font-size: 14px;
        cursor: pointer;
      }
    }

    .active-switch {
      margin-right: 5px;
    }
  }

  .event-collapse {
    border: none !important;

    :deep(.el-collapse-item__header) {
      border: none !important;
    }

    :deep(.el-collapse-item__wrap) {
      border: none !important;
    }
  }
</style>>
