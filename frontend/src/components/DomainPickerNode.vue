<template>
  <div class="pn-wrap">
    <div :class="['pn', selectedId === node.id && 'is-active']" @click="$emit('select', node)">
      <span class="pn-toggle" v-if="hasChildren" @click.stop="$emit('toggle', node)" v-html="BL.icon(isOpen ? 'chevronDown' : 'chevronRight', 12)"></span>
      <span v-else class="pn-toggle pn-toggle-empty"></span>
      <span class="pn-ic" :style="{ background: prof.color }" v-html="BL.icon(prof.icon, 11, '#fff')"></span>
      <span class="pn-label bl-truncate">{{ node.label }}</span>
    </div>
    <div v-if="hasChildren && isOpen" class="pn-children">
      <DomainPickerNode
        v-for="c in node.children" :key="c.id"
        :node="c" :selected-id="selectedId" :expanded="expanded"
        @select="$emit('select', $event)" @toggle="$emit('toggle', $event)" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { BL } from '@/lib/bl.js'
import { nodeProfile } from '@/lib/domain.js'

const props = defineProps({
  node: Object, selectedId: String, expanded: Set
})
defineEmits(['select', 'toggle'])

const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)
const isOpen = computed(() => props.expanded?.has(props.node.id))
const prof = computed(() => nodeProfile(props.node))
</script>

<style scoped>
.pn {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 6px;
  border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-13);
  cursor: pointer;
}
.pn:hover { background: var(--bl-bg-hover); }
.pn.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.pn-toggle {
  width: 14px; height: 14px;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-1); flex-shrink: 0;
}
.pn-toggle-empty { width: 14px; height: 14px; }
.pn-ic {
  width: 16px; height: 16px; border-radius: 3px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.pn-label { flex: 1; min-width: 0; }
.pn-children { padding-left: 18px; }
</style>
