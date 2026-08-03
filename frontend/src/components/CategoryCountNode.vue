<template>
  <div class="cct-wrap">
    <div :class="['cct-tn', isOn && 'is-active']" @click="$emit('pick', node)">
      <span v-if="kids.length" class="cct-tn-toggle" @click.stop="open = !open"
            v-html="BL.icon(open ? 'chevronDown' : 'chevronRight', 12)"></span>
      <span v-else class="cct-tn-toggle cct-tn-toggle-empty"></span>
      <span class="cct-tn-ico" :style="{ background: prof.color }" v-html="BL.icon(prof.icon, 13, '#fff')"></span>
      <span class="cct-tn-label bl-truncate" :title="label">{{ label }}</span>
      <span class="cct-tn-count" :title="`${total} 项`">{{ total }}</span>
    </div>
    <div v-if="open && kids.length" class="cct-tn-children">
      <CategoryCountNode v-for="c in kids" :key="c.id" :node="c" :active="active" :counts="counts"
                         @pick="n => $emit('pick', n)" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import { nodeProfile } from '@/lib/domain.js'

const props = defineProps({
  node: { type: Object, required: true },
  active: { type: Object, default: null },
  counts: { type: Object, default: () => ({}) },
})
defineEmits(['pick'])

const open = ref(true)
const kids = computed(() => (props.node.children || []).filter(c => c.categoryType === 1 || c.categoryType === 2))
const isOn = computed(() => !!props.active && props.active.id === props.node.id)
const label = computed(() => props.node.rdfsLabel || props.node.label || props.node.categoryCode)
const prof = computed(() => nodeProfile(props.node))
/* 自身 + 全部后代的数量之和, 折叠时也能看出这一支下有多少 */
const total = computed(() => {
  let n = 0
  const walk = x => { if (x.categoryCode) n += props.counts[x.categoryCode] || 0; (x.children || []).forEach(walk) }
  walk(props.node)
  return n
})
</script>
