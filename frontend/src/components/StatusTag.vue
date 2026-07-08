<template>
  <span v-if="status == null || status === ''" class="bl-tag bl-tag-muted">—</span>
  <span v-else :class="['bl-tag', status == 1 || status === 'active' || status === 'online' ? 'bl-tag-success' : status === 0 || status === 'inactive' || status === 'offline' ? 'bl-tag-danger' : 'bl-tag-warning']">
    {{ label }}
  </span>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useDict } from '@/composables/useDict'

const props = defineProps({
  status: { type: [String, Number], default: null },
  dictCode: { type: String, default: 'sys_act_status' }
})

const label = ref('—')
const { getValue } = useDict()

watch(() => props.status, (s) => {
  if (s == null || s === '') { label.value = '—'; return }
  const v = getValue(props.dictCode, String(s))
  label.value = v || String(s)
}, { immediate: true })
</script>
