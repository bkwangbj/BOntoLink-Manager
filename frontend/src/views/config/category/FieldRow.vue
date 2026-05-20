<template>
  <div :class="['fr', inline && 'fr-inline']">
    <div class="fr-label" :title="hint || ''" :class="hint && 'has-hint'">{{ label }}</div>
    <div class="fr-value"><slot /></div>
  </div>
</template>

<script setup>
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
}
.fr-label.has-hint {
  cursor: help;
  border-bottom: 1px dotted var(--bl-text-4);
}
.fr-value { color: var(--bl-text-1); min-width: 0; word-break: break-all; }

/* 行内：label 在左，value 在右，同一行 */
.fr.fr-inline {
  flex-direction: row;
  align-items: center;
  gap: 12px;
}
.fr.fr-inline .fr-label {
  width: 60px;
  flex-shrink: 0;
}
.fr.fr-inline .fr-value { flex: 1; }
</style>
