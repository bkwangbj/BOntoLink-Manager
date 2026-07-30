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
const { getDict, getValue } = useDict()

/* 必须等字典加载完再取值: 否则首屏 getValue 命中空缓存返回码值, status 不变就永远停在 "1" */
watch(() => props.status, async (s) => {
  if (s == null || s === '') { label.value = '—'; return }
  label.value = String(s)
  await getDict(props.dictCode)
  label.value = getValue(props.dictCode, String(s)) || String(s)
}, { immediate: true })
</script>
