<template>
  <div class="page">
    <PageHeader title="导入导出" subtitle="Import & Export · 跨环境迁移与备份" />
    <div class="body bl-row" style="align-items:stretch; gap:12px">
      <div class="bl-card bl-card-pad bl-col" style="flex:1; gap:12px">
        <h3 style="margin:0">导入</h3>
        <p class="bl-muted" style="margin:0">支持 OWL / RDF / Turtle / JSON 业务快照。</p>
        <button class="bl-btn bl-btn-primary">选择文件并上传</button>
      </div>
      <div class="bl-card bl-card-pad bl-col" style="flex:1; gap:12px">
        <h3 style="margin:0">导出</h3>
        <p class="bl-muted" style="margin:0">将当前本体版本导出为 OWL / 业务快照 / JSON。</p>
        <div class="bl-row" style="gap:8px">
          <button class="bl-btn">OWL/RDF</button>
          <button class="bl-btn">业务快照</button>
          <button class="bl-btn">JSON</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue'
</script>
<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { padding: 12px; flex: 1; overflow: auto; }
</style>
