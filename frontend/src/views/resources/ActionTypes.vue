<template>
  <div class="page">
    <PageHeader title="动作类型" subtitle="Action types · 对象上的操作 / 流程">
      <template #actions>
        <input class="bl-input" placeholder="搜索 api_name" v-model="q" style="width:240px" />
        <button class="bl-btn bl-btn-primary">新建动作</button>
      </template>
    </PageHeader>
    <div class="body">
      <div class="bl-card">
        <table class="bl-table">
          <thead>
            <tr><th>显示名</th><th>API name</th><th>所属对象</th><th>动作种类</th><th>状态</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in filtered" :key="r.id">
              <td>{{ r.display_name }}</td>
              <td class="bl-mono">{{ r.api_name }}</td>
              <td><span class="bl-tag">{{ r.class_id }}</span></td>
              <td><span class="bl-tag bl-tag-warning">{{ r.action_kind }}</span></td>
              <td><span class="bl-tag bl-tag-success">Active</span></td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filtered.length" class="bl-empty">暂无动作定义</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { resourceApi } from '@/api'
const rows = ref([])
const q = ref('')
const filtered = computed(() => rows.value.filter(r => !q.value || (r.api_name || '').toLowerCase().includes(q.value.toLowerCase())))
onMounted(async () => { rows.value = await resourceApi.actions().catch(() => []) })
</script>
<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { padding: 12px; flex: 1; overflow: auto; }
</style>
