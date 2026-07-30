<template>
  <div class="fpm-subhd">{{ required ? '必选入参' : '可选入参' }}
    <span v-if="required" class="bl-muted">(函数定义决定,不可删除)</span></div>
  <table class="bl-table fpm-table">
    <thead><tr><th class="t-left">参数名</th><th class="t-left">类型</th><th class="t-center">必填</th><th class="t-left">值来源类型</th><th class="t-left">取值配置</th><th></th></tr></thead>
    <tbody>
      <tr v-for="(fp, fi) in rows" :key="fi" :class="{ 'is-mismatch': mismatch(fp) }">
        <td><input class="bl-input bl-input-xs bl-mono" v-model="fp.name" placeholder="param_name" /></td>
        <td><BlSelect v-model="fp.param_type" :options="FUNC_PTYPE_OPTS" size="sm" /></td>
        <td class="t-center"><span class="bl-tag" :class="required && 'bl-tag-danger'">{{ required ? '必填' : '可选' }}</span></td>
        <td><BlSelect v-model="fp.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
        <td>
          <BlSelect v-if="Number(fp.value_source) === 1" v-model="fp.value_content" :options="formOptions" size="sm" clearable placeholder="选表单参数" />
          <BlSelect v-else-if="Number(fp.value_source) === 5" v-model="fp.value_content" :options="objectOptions" size="sm" clearable placeholder="选对象引用参数" />
          <input v-else-if="Number(fp.value_source) === 2" class="bl-input bl-input-xs" v-model="fp.value_content" placeholder="静态值" />
          <span v-else class="bl-muted fpm-auto">自动</span>
        </td>
        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeRow(fp)" v-html="BL.icon('x', 11)"></button></td>
      </tr>
      <tr v-if="!rows.length"><td colspan="6" class="fpm-empty">暂无{{ required ? '必选' : '可选' }}入参</td></tr>
    </tbody>
  </table>
  <button class="bl-btn bl-btn-text bl-btn-sm fpm-add" @click="addRow"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加{{ required ? '必选' : '可选' }}入参</span></button>
</template>

<script setup>
import { computed } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import { VALUE_SOURCE_OPTS, FUNC_PTYPE_OPTS, newFuncParam } from './funcParamModel.js'

const props = defineProps({
  params: { type: Array, default: () => [] },        // 规则的 func_params 原数组, 直接就地增删
  required: { type: Number, default: 1 },            // 1=必选入参表, 0=可选入参表
  formOptions: { type: Array, default: () => [] },
  objectOptions: { type: Array, default: () => [] },
  mismatch: { type: Function, default: () => false },
})

const rows = computed(() => props.params.filter(p => (p.required ? 1 : 0) === props.required))
function addRow() { props.params.push(newFuncParam(props.required)) }
function removeRow(fp) { const i = props.params.indexOf(fp); if (i >= 0) props.params.splice(i, 1) }
</script>

<style scoped>
.fpm-subhd { font-size: 12.5px; font-weight: 600; color: var(--bl-text-2); margin-bottom: 6px; }
.fpm-subhd .bl-muted { font-weight: 400; }
.fpm-table { width: 100%; font-size: 12px; background: var(--bl-bg-1); }
.fpm-table thead th { background: var(--bl-bg-2); font-weight: 600; height: 30px; padding: 0 6px; white-space: nowrap; color: var(--bl-text-2); }
.fpm-table thead th.t-left { text-align: left; }
.fpm-table td { padding: 3px 5px; border-top: 1px solid var(--bl-divider); }
.fpm-table td.t-center { text-align: center; }
.fpm-table .bl-input-xs { height: 28px; padding: 0 6px; font-size: 12px; }
.fpm-table tr.is-mismatch td { background: color-mix(in srgb, #f53f3f 8%, transparent); }
.fpm-table tr.is-mismatch .bl-input { border-color: #f53f3f; }
.fpm-empty { text-align: center; padding: 8px; font-size: 12px; color: var(--bl-text-3); }
.fpm-auto { font-size: 12px; padding-left: 4px; }
.fpm-add { margin-top: 4px; }
</style>
