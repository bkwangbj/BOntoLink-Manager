<!--
  排序条件(数据源·P4):多排序字段 + 独立计算方式 + 升降序滑动开关 + 拖拽优先级
  + 悬浮删除 + 新增行。
  v-model:sorts → [{ field, agg: 'count'|'sum'|'avg'|'min'|'max', desc: Boolean }]
-->
<template>
  <div class="ss-block">
    <div class="ss-hd">
      <span class="ss-title">排序条件</span>
      <button
        v-if="setMode"
        class="ss-add"
        title="新增排序"
        @click="addRow"
      >
        <i-ri-add-line />
      </button>
    </div>
    <draggable
      :list="rows"
      item-key="_k"
      handle=".ss-grip"
      :disabled="!setMode"
      class="ss-list"
      @end="emitChange"
    >
      <template #item="{ element: row, index }">
        <div class="ss-row">
          <span
            v-if="setMode"
            class="ss-grip"
            title="拖拽调整优先级"
          >
            <i-ri-draggable />
          </span>
          <el-select
            v-model="row.field"
            size="small"
            filterable
            allow-create
            default-first-option
            class="ss-field"
            placeholder="字段"
            :disabled="!setMode"
            @change="emitChange"
          >
            <el-option
              v-for="o in fieldOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
          <el-select
            v-model="row.agg"
            size="small"
            class="ss-agg"
            :disabled="!setMode"
            @change="emitChange"
          >
            <el-option
              v-for="a in AGGS"
              :key="a.k"
              :label="a.label"
              :value="a.k"
            />
          </el-select>
          <el-switch
            v-model="row.desc"
            :disabled="!setMode"
            active-text="降序"
            inactive-text="升序"
            class="ss-switch"
            @change="emitChange"
          />
          <button
            v-if="setMode"
            class="ss-del"
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
      class="ss-empty"
    >
      点击右上角 + 添加排序
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
  name: 'SortSetting',
  components: { draggable },
  props: {
    sorts: { type: Array, default: () => [] },
    fieldOptions: { type: Array, default: () => [] },
    setMode: { type: Boolean, default: true }
  },
  emits: ['update:sorts'],
  data () {
    return { AGGS, rows: [] }
  },
  watch: {
    sorts: {
      immediate: true,
      handler (v) {
        if (JSON.stringify(this.stripKeys(this.rows)) !== JSON.stringify(v || [])) {
          this.rows = (v || []).map(s => ({
            _k: ++_seq,
            field: s.field || '',
            agg: s.agg || 'count',
            desc: s.desc !== false
          }))
        }
      }
    }
  },
  methods: {
    addRow () {
      this.rows.push({ _k: ++_seq, field: '', agg: 'count', desc: true })
      this.emitChange()
    },
    removeRow (i) {
      this.rows.splice(i, 1)
      this.emitChange()
    },
    stripKeys (rows) {
      return (rows || []).map(r => ({ field: r.field, agg: r.agg, desc: r.desc }))
    },
    emitChange () {
      this.$emit('update:sorts', this.stripKeys(this.rows))
    }
  }
}
</script>

<style lang="scss" scoped>
.ss-block { margin: 0 0 12px 0; }
.ss-hd {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 5px;
}
.ss-title { font-size: 13px; font-weight: 600; color: #1d2129; }
.ss-add {
  width: 22px; height: 22px; border: 0; border-radius: 5px; cursor: pointer;
  background: #1f6aff; color: #fff;
  display: inline-flex; align-items: center; justify-content: center;
}
.ss-add:hover { background: #4080ff; }
.ss-list { display: flex; flex-direction: column; gap: 3px; }
.ss-row {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 6px; border-radius: 6px; background: #f7f8fa;
  transition: background-color .12s;
}
.ss-row:hover { background: #eef3ff; }
.ss-grip { color: #c9cdd4; cursor: grab; display: inline-flex; font-size: 14px; flex-shrink: 0; }
.ss-grip:active { cursor: grabbing; }
.ss-field { flex: 1; min-width: 80px; }
.ss-agg { flex: 0 0 88px; }
.ss-switch { flex-shrink: 0; }
.ss-del {
  width: 22px; height: 22px; border: 0; background: transparent;
  color: #86909c; cursor: pointer; border-radius: 5px; opacity: 0; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  transition: opacity .12s, background-color .12s, color .12s;
}
.ss-row:hover .ss-del { opacity: 1; }
.ss-del:hover { background: #ffece8; color: #f53f3f; }
.ss-empty { font-size: 12px; color: #c9cdd4; padding: 10px 0; text-align: center; }
:deep(.el-switch__label) { font-size: 12px; }
</style>
