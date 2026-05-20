<template>
  <div class="mg">
    <div v-if="loading" class="bl-empty" style="padding:48px">加载图谱…</div>
    <div v-else-if="!nodes.length" class="bl-empty" style="padding:48px">
      <div class="bl-empty-icon" v-html="BL.icon('network', 28)"></div>
      该分组下暂无对象类型
    </div>
    <svg v-else :viewBox="`0 0 ${size} ${size}`" preserveAspectRatio="xMidYMid meet" class="mg-svg">
      <!-- 关系标记 -->
      <defs>
        <marker id="mg-arrow" markerWidth="8" markerHeight="8" refX="6" refY="4" orient="auto">
          <path d="M0,0 L6,4 L0,8 z" :fill="edgeColor" />
        </marker>
      </defs>
      <!-- 边 -->
      <g class="edges">
        <line v-for="(e, i) in edges" :key="'e'+i"
              :x1="pos[e.source_class_id]?.x" :y1="pos[e.source_class_id]?.y"
              :x2="pos[e.target_class_id]?.x" :y2="pos[e.target_class_id]?.y"
              :stroke="edgeColor" stroke-width="1.2"
              marker-end="url(#mg-arrow)" />
        <text v-for="(e, i) in edges" :key="'el'+i"
              :x="(pos[e.source_class_id]?.x + pos[e.target_class_id]?.x) / 2"
              :y="(pos[e.source_class_id]?.y + pos[e.target_class_id]?.y) / 2 - 4"
              text-anchor="middle"
              :fill="textMuted" font-size="10">
          {{ e.display_name || e.api_name }}
        </text>
      </g>
      <!-- 节点 -->
      <g class="nodes">
        <g v-for="n in nodes" :key="n.id" :transform="`translate(${pos[n.id].x}, ${pos[n.id].y})`">
          <circle r="26" :fill="n.color || '#165DFF'" stroke="#fff" stroke-width="2" />
          <svg x="-12" y="-12" width="24" height="24" viewBox="0 0 24 24"
               fill="none" stroke="#fff" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" overflow="visible">
            <path :d="BL.iconPath(n.icon || 'cube')" />
          </svg>
          <text text-anchor="middle" dy="42" :fill="textColor" font-size="12" font-weight="500">
            {{ n.display_name || n.api_name }}
          </text>
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { categoryApi } from '@/api'

const props = defineProps({ nodeId: String })

const loading = ref(false)
const nodes = ref([])
const edges = ref([])
const size = 420

const pos = computed(() => {
  const cx = size / 2, cy = size / 2
  const R = nodes.value.length <= 1 ? 0 : size / 2 - 70
  const m = {}
  nodes.value.forEach((n, i) => {
    if (nodes.value.length === 1) { m[n.id] = { x: cx, y: cy }; return }
    const ang = (i * 2 * Math.PI) / nodes.value.length - Math.PI / 2
    m[n.id] = { x: cx + R * Math.cos(ang), y: cy + R * Math.sin(ang) }
  })
  return m
})

const edgeColor = '#C9CDD4'
const textColor = '#1D2129'
const textMuted = '#86909C'

async function load() {
  if (!props.nodeId) return
  loading.value = true
  try {
    const data = await categoryApi.graph(props.nodeId)
    nodes.value = data?.nodes || []
    edges.value = data?.edges || []
  } finally { loading.value = false }
}
watch(() => props.nodeId, load, { immediate: true })
</script>

<style scoped>
.mg {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  padding: 12px;
  min-height: 420px;
  display: flex; align-items: center; justify-content: center;
  flex: 1; width: 100%;
}
.mg-svg { width: 100%; height: 100%; max-height: 70vh; }
</style>
