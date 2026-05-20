<template>
  <header class="ph">
    <div class="ph-main">
      <h1 class="ph-title">{{ title }}</h1>
      <div v-if="subtitle" class="ph-sub">{{ subtitle }}</div>
    </div>
    <div class="ph-r">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({ title: String, subtitle: String })
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
.ph-main { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.ph-title { margin: 0; font-size: 18px; font-weight: 600; line-height: 1.2; }
.ph-sub { font-size: var(--bl-fs-12); color: var(--bl-text-3); }
.ph-r { display: flex; align-items: center; gap: 12px; }
</style>
