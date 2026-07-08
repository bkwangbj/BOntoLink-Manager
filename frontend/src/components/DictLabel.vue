<template>
  <span :style="color ? 'color:'+color : ''">{{ label }}</span>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useDict } from '@/composables/useDict'

const props = defineProps({
  dictCode: { type: String, required: true },
  itemCode: { type: [String, Number], default: '' },
  unknown:   { type: String, default: '—' }
})

const label = ref(props.unknown)
const { getDict } = useDict()

watch(() => props.itemCode, async (code) => {
  if (code == null || code === '') { label.value = props.unknown; return }
  const items = await getDict(props.dictCode)
  const item = items.find(d => String(d.item_code) === String(code))
  label.value = item?.item_value || props.unknown
}, { immediate: true })
</script>
