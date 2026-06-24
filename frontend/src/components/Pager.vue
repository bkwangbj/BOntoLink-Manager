<template>
  <!-- 列表分页控件 (右侧): 每页 + 上/下页 + 页码. 配合 usePagination 使用 -->
  <div class="bl-pager-ctrl">
    <span class="bl-muted bl-pager-lbl">每页</span>
    <select class="bl-input bl-pager-size"
            :value="pageSize"
            @change="$emit('update:pageSize', Number($event.target.value))">
      <option v-for="s in sizes" :key="s" :value="s">{{ s }}</option>
    </select>
    <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page <= 1" @click="$emit('update:page', page - 1)">‹</button>
    <span class="bl-muted bl-pager-ind">{{ page }} / {{ totalPages }}</span>
    <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page >= totalPages" @click="$emit('update:page', page + 1)">›</button>
  </div>
</template>

<script setup>
defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 20 },
  totalPages: { type: Number, default: 1 },
  sizes: { type: Array, default: () => [20, 50, 100] }
})
defineEmits(['update:page', 'update:pageSize'])
</script>

<style scoped>
.bl-pager-ctrl { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; }
.bl-pager-lbl { font-size: 12px; margin-right: 6px; }
.bl-pager-size { width: 64px; height: 26px; }
.bl-pager-ind { font-size: 12px; }
</style>
