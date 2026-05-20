<template>
  <div class="page">
    <PageHeader title="总览" subtitle="Discover · 本体系统整体情况" />
    <div class="page-body">
      <div class="tabs">
        <div :class="['tab', tab==='industry' && 'is-active']" @click="tab='industry'">行业 / 领域情况</div>
        <div :class="['tab', tab==='concept' && 'is-active']" @click="tab='concept'">概念情况</div>
      </div>

      <div v-if="tab==='industry'" class="card">
        <div class="bl-row" style="gap:24px; flex-wrap:wrap">
          <div v-for="ind in industries" :key="ind.id" class="ind-card">
            <div class="bl-icon-block" :style="{ background: ind.color || '#165DFF' }" v-html="BL.icon(ind.icon || 'industry', 18, '#fff')"></div>
            <div>
              <div class="ind-title">{{ ind.label }}</div>
              <div class="bl-muted" style="font-size:12px">{{ ind.children?.length || 0 }} 个领域 · {{ ind.nsCode }}</div>
            </div>
          </div>
          <div v-if="!industries.length" class="bl-empty">暂无行业数据</div>
        </div>
      </div>

      <div v-else class="bl-col" style="gap:16px">
        <div class="stats">
          <div class="bl-stat"><div class="lbl">行业数</div><div class="val">{{ stats.industry || 0 }}</div></div>
          <div class="bl-stat"><div class="lbl">领域数</div><div class="val">{{ stats.domain || 0 }}</div></div>
          <div class="bl-stat"><div class="lbl">对象数</div><div class="val">{{ stats.ontClass || 0 }}</div></div>
          <div class="bl-stat"><div class="lbl">接口数</div><div class="val">{{ stats.ontInterface || 0 }}</div></div>
          <div class="bl-stat"><div class="lbl">关系数</div><div class="val">{{ stats.ontLink || 0 }}</div></div>
          <div class="bl-stat"><div class="lbl">动作数</div><div class="val">{{ stats.ontAction || 0 }}</div></div>
          <div class="bl-stat"><div class="lbl">属性数</div><div class="val">{{ stats.ontProperty || 0 }}</div></div>
          <div class="bl-stat"><div class="lbl">命名空间</div><div class="val">{{ stats.namespace || 0 }}</div></div>
        </div>

        <div class="bl-card">
          <div class="bl-card-hd"><span>最近更新动态</span></div>
          <div class="bl-card-pad bl-col" style="gap:8px">
            <div class="timeline-row" v-for="t in timeline" :key="t.id">
              <span class="dot" :style="{ background: t.color }"></span>
              <span class="bl-grow">{{ t.text }}</span>
              <span class="bl-muted" style="font-size:11px">{{ t.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { BL } from '@/lib/bl.js'
import { resourceApi, categoryApi } from '@/api'

const tab = ref('concept')
const stats = ref({})
const industries = ref([])
const timeline = ref([
  { id: 1, color: '#165DFF', text: '水土保持初版发布（v1.0）', time: '7 分钟前' },
  { id: 2, color: '#00B42A', text: '新增对象类型 HydrologyStation', time: '1 小时前' },
  { id: 3, color: '#FF7D00', text: '关系 locatedInRiver 上线', time: '今天 09:30' },
  { id: 4, color: '#722ED1', text: '命名空间 w_wtr_hyd 元数据更新', time: '昨天 16:42' }
])

onMounted(async () => {
  stats.value = await resourceApi.stats().catch(() => ({}))
  const tree = await categoryApi.tree().catch(() => [])
  industries.value = tree
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.page-body { padding: 16px; overflow: auto; flex: 1; }
.tabs { display: flex; gap: 4px; margin-bottom: 12px; }
.tab {
  padding: 8px 14px; font-size: var(--bl-fs-13);
  color: var(--bl-text-2); cursor: pointer; border-radius: var(--bl-radius-2);
}
.tab:hover { background: var(--bl-bg-hover); }
.tab.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.card { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3); padding: 16px; }
.ind-card {
  display: flex; align-items: center; gap: 12px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3);
  padding: 14px 18px; min-width: 220px;
}
.ind-title { font-weight: 600; }
.stats { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); }
.timeline-row {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 0; font-size: var(--bl-fs-13);
}
.timeline-row .dot { width: 8px; height: 8px; border-radius: 50%; }
</style>
