<template>
  <div class="cct-root">
    <div class="cct-title">行业 / 领域</div>
    <div class="cct-wrap">
      <div :class="['cct-tn', !active && 'is-active']" @click="$emit('pick', null)">
        <span class="cct-tn-toggle cct-tn-toggle-empty"></span>
        <span class="cct-tn-ico" style="background: var(--bl-primary)" v-html="BL.icon('grid', 13, '#fff')"></span>
        <span class="cct-tn-label bl-truncate">全部</span>
        <span class="cct-tn-count">{{ allCount }}</span>
      </div>
    </div>
    <CategoryCountNode v-for="n in roots" :key="n.id" :node="n" :active="active" :counts="counts"
                       @pick="n2 => $emit('pick', n2)" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { BL } from '@/lib/bl.js'
import CategoryCountNode from './CategoryCountNode.vue'

const props = defineProps({
  /** 原始分类树 (categoryApi.tree) */
  nodes: { type: Array, default: () => [] },
  /** category_code → 数量 */
  counts: { type: Object, default: () => ({}) },
  active: { type: Object, default: null },
  allCount: { type: Number, default: 0 },
})
defineEmits(['pick'])

/* 只保留「行业(1)」「领域(2)」两级, 更深的命名空间节点不参与筛选 */
const roots = computed(() => {
  const trim = list => (list || [])
    .filter(n => n.categoryType === 1 || n.categoryType === 2)
    .map(n => ({ ...n, children: trim(n.children) }))
  return trim(props.nodes)
})
</script>

<style scoped>
.cct-root { padding: 8px 6px; overflow: auto; min-height: 0; height: 100%; box-sizing: border-box;
  background: color-mix(in srgb, var(--bl-bg-2) 45%, var(--bl-bg-1)); }
.cct-title { font-size: 11px; color: var(--bl-text-3); padding: 6px 10px 8px; margin-bottom: 4px;
  border-bottom: 1px dashed var(--bl-divider); font-weight: 600; letter-spacing: .3px; }
</style>

<!-- 非 scoped: 递归子组件 CategoryCountNode 的节点也要用同一套样式, 以 .cct-root 前缀做隔离 -->
<style>
.cct-root .cct-wrap { position: relative; }
.cct-root .cct-tn {
  display: flex; align-items: center; gap: 6px; padding: 6px 8px; margin: 1px 0;
  border-radius: var(--bl-radius-2); font-size: var(--bl-fs-13); cursor: pointer; user-select: none;
  position: relative; z-index: 1; color: var(--bl-text-1);
  transition: background-color .12s ease, color .12s ease;
}
.cct-root .cct-tn:hover { background: var(--bl-bg-hover); }
.cct-root .cct-tn.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }

.cct-root .cct-tn-toggle { width: 16px; height: 16px; display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-1); flex-shrink: 0; position: relative; z-index: 2; }
.cct-root .cct-tn-toggle:hover { color: var(--bl-primary); }
/* 有箭头的 toggle 盖一层本地背景, 让竖虚线被向外延展 3px 遮住 */
.cct-root .cct-tn-toggle:not(.cct-tn-toggle-empty) { background: var(--bl-bg-2); border-radius: 3px; outline: 3px solid #f5f7fa; }
.cct-root .cct-tn:hover .cct-tn-toggle:not(.cct-tn-toggle-empty) { background: var(--bl-bg-hover); outline-color: var(--bl-bg-hover); }
.cct-root .cct-tn.is-active .cct-tn-toggle:not(.cct-tn-toggle-empty) { background: var(--bl-primary-soft); outline-color: var(--bl-primary-soft); }
.cct-root .cct-tn-toggle-empty { width: 16px; height: 16px; background: transparent; flex-shrink: 0; }

.cct-root .cct-tn-ico { width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center; }
.cct-root .cct-tn-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cct-root .cct-tn-count {
  flex-shrink: 0; margin-left: auto; font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border-radius: 9px; padding: 0 7px; min-width: 20px;
  height: 17px; line-height: 17px; text-align: center; font-feature-settings: "tnum";
  transition: background-color .12s ease, color .12s ease;
}
.cct-root .cct-tn.is-active .cct-tn-count { background: var(--bl-bg-1); color: var(--bl-primary); }

/* 树引线 (与 Category 页 TreeNode 对齐) */
.cct-root .cct-tn-children { margin-left: 20px; position: relative; }
.cct-root .cct-tn-children::before {
  content: ''; position: absolute; left: 14px; top: -2px; bottom: 18px;
  border-left: 1px dashed var(--bl-border-strong);
}
.cct-root .cct-tn-children > .cct-wrap > .cct-tn::before {
  content: ''; position: absolute; left: 14px; top: 50%; width: 14px;
  border-top: 1px dashed var(--bl-border-strong);
}
</style>
