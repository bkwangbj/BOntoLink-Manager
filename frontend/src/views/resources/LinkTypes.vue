<template>
  <div class="page">
    <PageHeader title="链接" subtitle="Links · 对象之间的连接定义">
      <template #actions>
        <input class="bl-input" placeholder="搜索 api_name" v-model="q" style="width:240px" />
        <button class="bl-btn bl-btn-primary">新建链接</button>
      </template>
    </PageHeader>
    <div class="body">
      <div class="bl-card">
        <table class="bl-table">
          <thead>
            <tr><th>链接</th><th>API name</th><th>源</th><th>目标</th><th>基数</th><th>状态</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in filtered" :key="r.id">
              <td>{{ r.display_name }}</td>
              <td class="bl-mono">{{ r.api_name }}</td>
              <td><span class="bl-tag">{{ classMap[r.source_class_id] || r.source_class_id }}</span></td>
              <td><span class="bl-tag">{{ classMap[r.target_class_id] || r.target_class_id }}</span></td>
              <td><span class="bl-tag bl-tag-primary">{{ r.cardinality }}</span></td>
              <td><span class="bl-tag bl-tag-success">Active</span></td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filtered.length" class="bl-empty">暂无链接定义</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { resourceApi } from '@/api'

const rows = ref([])
const classMap = ref({})
const q = ref('')

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return rows.value
  return rows.value.filter(r => (r.api_name || '').toLowerCase().includes(k))
})

onMounted(async () => {
  rows.value = await resourceApi.links().catch(() => [])
  const classes = await resourceApi.classes().catch(() => [])
  const m = {}
  classes.forEach(c => m[c.id] = c.display_name || c.api_name)
  classMap.value = m
})
</script>
<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { padding: 12px; flex: 1; overflow: auto; }
</style>
