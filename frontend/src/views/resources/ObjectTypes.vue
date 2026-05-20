<template>
  <div class="page">
    <PageHeader title="对象类型" subtitle="Object types · 本体业务实体的元数据定义">
      <template #actions>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索对象类型 / api_name" v-model="q" />
        </div>
        <button class="bl-btn">筛选</button>
        <button class="bl-btn bl-btn-primary">新建对象类型</button>
      </template>
    </PageHeader>

    <div class="body">
      <div class="bl-card">
        <table class="bl-table">
          <thead>
            <tr>
              <th></th>
              <th>显示名</th>
              <th>API name</th>
              <th>命名空间</th>
              <th>分类编码</th>
              <th>状态</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in filtered" :key="c.id" @contextmenu.prevent="onCtx($event, c)">
              <td style="width:48px">
                <div class="bl-icon-block bl-icon-block-sm" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></div>
              </td>
              <td>{{ c.display_name || c.rdfs_label }}</td>
              <td class="bl-mono">{{ c.api_name }}</td>
              <td><span class="bl-tag">{{ c.ns_code }}</span></td>
              <td class="bl-mono">{{ c.category_code }}</td>
              <td>
                <span :class="['bl-tag', c.status===1 ? 'bl-tag-success' : 'bl-tag-warning']">
                  {{ c.status===1 ? 'Active' : 'Draft' }}
                </span>
              </td>
              <td><button class="bl-btn bl-btn-sm bl-btn-text" v-html="BL.icon('more', 14)"></button></td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filtered.length" class="bl-empty">
          <div class="bl-empty-icon" v-html="BL.icon('cube', 32)"></div>
          暂无对象类型，<a>创建第一个 →</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'

const rows = ref([])
const q = ref('')
const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return rows.value
  return rows.value.filter(r => [r.api_name, r.display_name, r.rdfs_label, r.ns_code].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})
function onCtx(e, c) { BL.info(`右键 · ${c.api_name}`) }
onMounted(async () => { rows.value = await resourceApi.classes().catch(() => []) })
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { padding: 12px; flex: 1; overflow: auto; }
.search-wrap { position: relative; width: 280px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }
</style>
