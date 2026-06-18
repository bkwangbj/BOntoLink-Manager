<!--
  指标设置(数据源·P2):多指标 + 5 种聚合勾选 + 拖拽排序 + 悬浮删除 + 新增行。
  v-model:metrics → [{ field, aggs: ['count'|'sum'|'avg'|'min'|'max', ...] }]
-->
<template>
  <div class="ms-block">
    <div class="ms-hd">
      <span class="ms-title">指标设置</span>
      <button
        v-if="setMode"
        class="ms-add"
        title="新增指标"
        @click="addRow"
      >
        <i-ri-add-line />
      </button>
    </div>
    <draggable
      :list="rows"
      item-key="_k"
      handle=".ms-grip"
      :disabled="!setMode"
      class="ms-list"
      @end="emitChange"
    >
      <template #item="{ element: row, index }">
        <div class="ms-row">
          <span
            v-if="setMode"
            class="ms-grip"
            title="拖拽排序"
          >
            <i-ri-draggable />
          </span>
          <el-select
            v-model="row.field"
            size="small"
            filterable
            allow-create
            default-first-option
            class="ms-field"
            placeholder="请选择"
            :disabled="!setMode"
            @change="onFieldChange(row)"
          >
            <el-option
              v-for="o in fieldOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
          <label
            v-for="a in AGGS"
            :key="a.k"
            class="ms-agg"
          >
            <el-checkbox
              :model-value="row.aggs.includes(a.k)"
              :disabled="!setMode || (a.numericOnly && !isNumericField(row.field))"
              @change="(v) => toggleAgg(row, a.k, v)"
            >
              {{ a.label }}
            </el-checkbox>
          </label>
          <button
            v-if="setMode"
            class="ms-del"
            title="删除"
            @click="removeRow(index)"
          >
            <i-ri-close-line />
          </button>
        </div>
      </template>
    </draggable>
    <div
      v-if="!rows.length"
      class="ms-empty"
    >
      点击右上角 + 添加指标
    </div>
  </div>
</template>

<script>
import draggable from 'vuedraggable'

const AGGS = [
  { k: 'count', label: '计数' },
  { k: 'sum', label: '总和' },
  { k: 'avg', label: '平均值' },
  { k: 'min', label: '最小值' },
  { k: 'max', label: '最大值' }
]
let _seq = 0

export default {
  name: 'MetricSetting',
  components: { draggable },
  props: {
    metrics: { type: Array, default: () => [] },
    fieldOptions: { type: Array, default: () => [] },
    setMode: { type: Boolean, default: true }
  },
  emits: ['update:metrics'],
  data () {
    return { AGGS, rows: [] }
  },
  watch: {
    metrics: {
      immediate: true,
      handler (v) {
        // 外部数据变化时同步到内部行(忽略 _k),避免与 emitChange 形成回环
        if (JSON.stringify(this.stripKeys(this.rows)) !== JSON.stringify(v || [])) {
          this.rows = (v || []).map(m => ({
            _k: ++_seq,
            field: m.field || '',
            aggs: Array.isArray(m.aggs) ? [...m.aggs] : []
          }))
        }
      }
    }
  },
  methods: {
    addRow () {
      this.rows.push({ _k: ++_seq, field: '', aggs: ['count'] })
      this.emitChange()
    },
    removeRow (i) {
      this.rows.splice(i, 1)
      this.emitChange()
    },
    toggleAgg (row, k, checked) {
      const i = row.aggs.indexOf(k)
      if (checked && i < 0) row.aggs.push(k)
      else if (!checked && i >= 0) row.aggs.splice(i, 1)
      this.emitChange()
    },
    // 属性是否为数值型(决定能否用 总和/平均/最值;否则只能计数)
    isNumericField (field) {
      const o = (this.fieldOptions || []).find(x => x.value === field)
      const dt = (o && o.dataType ? String(o.dataType) : '').toLowerCase()
      return /int|decimal|number|integer|long|float|double|bigint|year|年/.test(dt)
    },
    // 切换属性:非数值属性只保留「计数」,清掉总和/平均/最值
    onFieldChange (row) {
      if (!this.isNumericField(row.field)) {
        row.aggs = (row.aggs || []).filter(k => k === 'count')
        if (!row.aggs.length) row.aggs = ['count']
      }
      this.emitChange()
    },
    stripKeys (rows) {
      return (rows || []).map(r => ({ field: r.field, aggs: r.aggs }))
    },
    emitChange () {
      this.$emit('update:metrics', this.stripKeys(this.rows))
    }
  }
}
</script>

<style lang="scss" scoped>
.ms-block { margin: 0 0 12px 0; }
.ms-hd {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 5px;
}
.ms-title { font-size: 13px; font-weight: 600; color: #1d2129; }
.ms-add {
  width: 22px; height: 22px; border: 0; border-radius: 5px; cursor: pointer;
  background: #1f6aff; color: #fff;
  display: inline-flex; align-items: center; justify-content: center;
}
.ms-add:hover { background: #4080ff; }
.ms-list { display: flex; flex-direction: column; gap: 3px; }
.ms-row {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 6px; border-radius: 6px; background: #f7f8fa;
  transition: background-color .12s;
}
.ms-row:hover { background: #eef3ff; }
.ms-grip { color: #c9cdd4; cursor: grab; display: inline-flex; font-size: 14px; flex-shrink: 0; }
.ms-grip:active { cursor: grabbing; }
.ms-field { width: 116px; flex-shrink: 0; }
.ms-agg { display: inline-flex; align-items: center; }
.ms-del {
  margin-left: auto; width: 22px; height: 22px; border: 0; background: transparent;
  color: #86909c; cursor: pointer; border-radius: 5px; opacity: 0; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  transition: opacity .12s, background-color .12s, color .12s;
}
.ms-row:hover .ms-del { opacity: 1; }
.ms-del:hover { background: #ffece8; color: #f53f3f; }
.ms-empty { font-size: 12px; color: #c9cdd4; padding: 10px 0; text-align: center; }
:deep(.el-checkbox__label) { padding-left: 4px; font-size: 12px; }
:deep(.el-checkbox) { margin-right: 0; }
</style>
