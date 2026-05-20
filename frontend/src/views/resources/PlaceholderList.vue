<template>
  <div class="page">
    <PageHeader :title="cn" :subtitle="`${en} · 暂为占位页，待后续业务建模完成上线`">
      <template #actions>
        <button class="bl-btn bl-btn-primary">新建{{ cn }}</button>
      </template>
    </PageHeader>
    <div class="body">
      <div class="bl-card bl-card-pad bl-empty" style="padding:80px">
        <div class="bl-empty-icon" v-html="BL.icon('cube', 36)"></div>
        <div>当前领域下尚未建模「{{ cn }}」</div>
        <div style="margin-top:8px">
          <button class="bl-btn bl-btn-primary">创建第一个 {{ cn }} →</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue'
import { BL } from '@/lib/bl.js'

defineProps({ kind: String, title: String, cn: String, en: String })
</script>
<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { padding: 12px; flex: 1; overflow: auto; }
</style>
