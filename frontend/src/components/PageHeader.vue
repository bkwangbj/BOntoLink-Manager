<template>
  <header class="ph">
    <div class="ph-main" :title="tip">
      <h1 class="ph-title">{{ title }}</h1>
      <div v-if="subtitle" class="ph-sub">{{ subtitle }}</div>
    </div>
    <div class="ph-r">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ title: String, subtitle: String })
const tip = computed(() => props.subtitle ? `${props.title} · ${props.subtitle}` : props.title)
</script>

<style scoped>
.ph {
  padding: 10px 20px 12px;
  display: flex; align-items: center; justify-content: space-between;
  gap: 16px;
  background: var(--bl-bg-1);
  border-bottom: 1px solid var(--bl-border);
  flex-shrink: 0;
}
/* 标题 + 副标题在同一行；空间不够时副标题先省略 */
.ph-main {
  display: flex; align-items: baseline; gap: 12px;
  min-width: 0; flex: 1;
  overflow: hidden;
}
.ph-title {
  margin: 0; font-size: 18px; font-weight: 600; line-height: 1.2;
  flex-shrink: 0;
  white-space: nowrap;
}
.ph-sub {
  font-size: var(--bl-fs-12);
  color: var(--bl-text-3);
  min-width: 0; flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ph-r { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
</style>
