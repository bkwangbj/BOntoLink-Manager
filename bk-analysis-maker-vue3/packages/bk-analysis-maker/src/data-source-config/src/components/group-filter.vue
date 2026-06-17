<!--
  分组与筛选(数据源·P3):分组维度切换 + 包含/排除二选一 + 未匹配归「其他」
  + 分组值列表(勾选框 + 名称 + 数值 + 蓝色进度条 + 选中高亮)。
  v-model:grouping → { field, mode: 'include'|'exclude', includeOther, selected: [name...] }
-->
<template>
  <div class="gf-block">
    <div class="gf-title">分组与筛选</div>

    <!-- 分组维度 -->
    <el-select
      v-model="local.field"
      size="small"
      filterable
      allow-create
      default-first-option
      class="gf-dim"
      placeholder="选择分组维度"
      :disabled="!setMode"
      @change="emitChange"
    >
      <el-option
        v-for="o in dimensionOptions"
        :key="o.value"
        :label="o.label"
        :value="o.value"
      />
    </el-select>

    <!-- 筛选规则:包含/排除 二选一 + 未匹配归其他 -->
    <div class="gf-rule">
      <el-radio-group
        v-model="local.mode"
        :disabled="!setMode"
        @change="emitChange"
      >
        <el-radio value="include">包含以下</el-radio>
        <el-radio value="exclude">排除以下</el-radio>
      </el-radio-group>
      <el-checkbox
        v-model="local.includeOther"
        :disabled="!setMode"
        @change="emitChange"
      >
        未匹配归「其他」
      </el-checkbox>
    </div>

    <!-- 分组值列表 -->
    <div
      v-if="groupValues && groupValues.length"
      class="gf-list"
    >
      <label
        v-for="g in groupValues"
        :key="g.name"
        class="gf-item"
        :class="{ 'is-on': isSel(g.name) }"
      >
        <el-checkbox
          :model-value="isSel(g.name)"
          :disabled="!setMode"
          @change="(v) => toggleSel(g.name, v)"
        />
        <span
          class="gf-name"
          :title="g.name"
        >{{ g.name }}</span>
        <span class="gf-val">{{ fmt(g.value) }}</span>
        <span class="gf-bar"><span
          class="gf-bar-in"
          :style="{ width: pct(g.value) + '%' }"
        /></span>
      </label>
    </div>
    <div
      v-else
      class="gf-empty"
    >
      选择维度并保存后,由数据源返回分组取值
    </div>
  </div>
</template>

<script>
function defaultGrouping () {
  return { field: '', mode: 'include', includeOther: true, selected: [] }
}

export default {
  name: 'GroupFilter',
  props: {
    grouping: { type: Object, default: () => defaultGrouping() },
    dimensionOptions: { type: Array, default: () => [] },
    groupValues: { type: Array, default: () => [] }, // [{ name, value }]
    setMode: { type: Boolean, default: true }
  },
  emits: ['update:grouping'],
  data () {
    return { local: defaultGrouping() }
  },
  computed: {
    maxValue () {
      return (this.groupValues || []).reduce((m, g) => Math.max(m, Number(g.value) || 0), 0) || 1
    }
  },
  watch: {
    grouping: {
      immediate: true,
      deep: true,
      handler (v) {
        const next = { ...defaultGrouping(), ...(v || {}) }
        next.selected = Array.isArray(next.selected) ? [...next.selected] : []
        if (JSON.stringify(next) !== JSON.stringify(this.local)) this.local = next
      }
    }
  },
  methods: {
    isSel (name) { return this.local.selected.includes(name) },
    toggleSel (name, checked) {
      const i = this.local.selected.indexOf(name)
      if (checked && i < 0) this.local.selected.push(name)
      else if (!checked && i >= 0) this.local.selected.splice(i, 1)
      this.emitChange()
    },
    fmt (v) {
      const n = Number(v)
      return isNaN(n) ? (v ?? '') : n.toLocaleString('en-US')
    },
    pct (v) {
      return Math.max(2, Math.round((Number(v) || 0) / this.maxValue * 100))
    },
    emitChange () {
      this.$emit('update:grouping', JSON.parse(JSON.stringify(this.local)))
    }
  }
}
</script>

<style lang="scss" scoped>
.gf-block { margin: 0 5px 14px 0; }
.gf-title { font-size: 13px; font-weight: 600; color: #1d2129; margin-bottom: 8px; }
.gf-dim { width: 100%; margin-bottom: 8px; }
.gf-rule {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  margin-bottom: 8px; font-size: 12px;
}
.gf-list {
  border: 1px solid #e9e9e9; border-radius: 6px; overflow: hidden;
}
.gf-item {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 10px; cursor: pointer;
  border-bottom: 1px solid #f2f3f5;
  transition: background-color .12s;
}
.gf-item:last-child { border-bottom: 0; }
.gf-item:hover { background: #f7f8fa; }
.gf-item.is-on { background: #eef3ff; }
.gf-name { flex: 0 0 84px; font-size: 12px; color: #1d2129; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.gf-val { flex: 0 0 auto; min-width: 64px; text-align: right; font-size: 12px; color: #4e5969; font-variant-numeric: tabular-nums; }
.gf-bar { flex: 1; height: 8px; background: #f0f2f5; border-radius: 4px; overflow: hidden; }
.gf-bar-in { display: block; height: 100%; background: #1f6aff; border-radius: 4px; }
.gf-empty { font-size: 12px; color: #c9cdd4; padding: 10px 0; text-align: center; }
:deep(.el-checkbox) { margin-right: 0; }
</style>
