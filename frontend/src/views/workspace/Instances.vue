<template>
  <div class="page">
    <PageHeader title="实例" subtitle="Object instances · 业务数据浏览与查询">
      <template #actions>
        <button class="bl-btn bl-btn-primary" v-html="iconText('plus','新建实例')"></button>
      </template>
    </PageHeader>

    <div class="body">
      <aside class="left">
        <div class="bl-card bl-card-pad bl-col" style="gap:8px">
          <div class="bl-muted" style="font-size:12px">选择对象类型</div>
          <div v-for="c in classes" :key="c.id"
               :class="['bl-list-item', cur===c.id && 'is-active']"
               @click="cur = c.id">
            <span class="bl-icon-block bl-icon-block-sm" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></span>
            <span class="bl-grow bl-truncate">{{ c.display_name || c.api_name }}</span>
            <span class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }}</span>
          </div>
        </div>
      </aside>

      <section class="right">
        <div class="bl-card">
          <div class="bl-card-hd">
            <span>实例列表（{{ curObj?.display_name || '请选择对象类型' }}）</span>
            <div class="bl-row" style="gap:8px">
              <input class="bl-input bl-input-sm" placeholder="搜索…" style="width:200px" />
              <button class="bl-btn bl-btn-sm">筛选</button>
            </div>
          </div>
          <table class="bl-table" v-if="cur">
            <thead><tr><th>实例编码</th><th>名称</th><th>命名空间</th><th>状态</th><th></th></tr></thead>
            <tbody>
              <tr v-for="i in mockInstances" :key="i.code">
                <td class="bl-mono">{{ i.code }}</td>
                <td>{{ i.name }}</td>
                <td><span class="bl-tag">{{ i.ns }}</span></td>
                <td><span class="bl-tag bl-tag-success">已同步</span></td>
                <td><button class="bl-btn bl-btn-sm bl-btn-text">查看</button></td>
              </tr>
            </tbody>
          </table>
          <div v-else class="bl-empty" style="padding:48px">请选择左侧对象类型</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

const classes = ref([])
const cur = ref(null)
const curObj = computed(() => classes.value.find(c => c.id === cur.value))
const mockInstances = ref([
  { code: 'HS-001', name: '黄河上游测站',  ns: 'w_wtr_hyd' },
  { code: 'HS-002', name: '长江三峡测站',  ns: 'w_wtr_hyd' },
  { code: 'HS-003', name: '汉江某测站',    ns: 'w_wtr_hyd' }
])

onMounted(async () => {
  classes.value = await resourceApi.classes().catch(() => [])
  if (classes.value.length) cur.value = classes.value[0].id
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { flex: 1; display: grid; grid-template-columns: 260px 1fr; gap: 12px; padding: 12px; overflow: hidden; }
.left, .right { overflow: auto; }
</style>
