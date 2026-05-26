<template>
  <div :class="['fr', inline && 'fr-inline']">
    <div class="fr-label">
      <span>{{ label }}</span>
      <span v-if="hint" class="fr-hint" :title="hint" v-html="BL.icon('info', 12)"></span>
    </div>
    <div class="fr-value"><slot /></div>
  </div>
</template>

<script setup>
import { BL } from '@/lib/bl.js'
defineProps({ label: String, hint: String, inline: Boolean })
</script>

<style scoped>
.fr {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0 10px;
  border-bottom: 1px dashed var(--bl-divider);
  font-size: var(--bl-fs-13);
}
.fr:last-child { border-bottom: 0; }
.fr-label {
  color: var(--bl-text-3);
  font-size: var(--bl-fs-12);
  align-self: flex-start;
  display: inline-flex; align-items: center; gap: 4px;
}
.fr-hint {
  display: inline-flex; align-items: center;
  color: var(--bl-text-4);
  cursor: help;
}
.fr-hint:hover { color: var(--bl-primary); }
.fr-value { color: var(--bl-text-1); min-width: 0; word-break: break-all; }

/* 行内：label 在左，value 在右，同一行 */
.fr.fr-inline {
  flex-direction: row;
  align-items: center;
  gap: 12px;
}
.fr.fr-inline .fr-label {
  width: 52px;
  flex-shrink: 0;
  align-self: center;   /* 抵消基础 .fr-label 的 flex-start，让 label 与 value 行内垂直居中 */
}
.fr.fr-inline .fr-value { flex: 1; }
</style>
