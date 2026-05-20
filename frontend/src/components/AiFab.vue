<template>
  <button class="bl-ai-fab" title="AI 助手" @click="open">
    <span v-html="BL.icon('ai', 22, '#fff')"></span>
  </button>
</template>

<script setup>
import { BL } from '@/lib/bl.js'
import { useRouter } from 'vue-router'
const router = useRouter()
function open() { router.push('/tools/ai') }
</script>
