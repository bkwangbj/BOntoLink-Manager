<template>
  <div class="crm-mask" @click.self="$emit('close')">
    <div class="crm-panel">
      <!-- 头部 -->
      <div class="crm-head">
        <div class="crm-title">
          <span class="crm-title-ic" v-html="BL.icon('chart', 16, 'var(--bl-primary)')"></span>
          <b>推荐图表</b>
          <span class="crm-title-sub">{{ typeName }} · 根据数据自动推荐,勾选最多 6 个保存为默认看板</span>
        </div>
        <button class="crm-x" @click="$emit('close')" v-html="BL.icon('x', 16)"></button>
      </div>

      <!-- 候选(按分组) -->
      <div class="crm-body">
        <div v-if="!recommendations.length" class="crm-empty">该对象类型暂无可推荐的图表维度</div>
        <template v-for="g in groups" :key="g.name">
          <div class="crm-group">{{ g.name }} · {{ g.items.length }}</div>
          <div class="crm-grid">
            <div v-for="r in g.items" :key="r.key"
                 :class="['crm-card', sel.has(r.key) && 'is-on', (!sel.has(r.key) && atMax) && 'is-disabled']"
                 @click="toggle(r)">
              <span class="crm-card-ic" :style="{ background: kindColor(r.kind)+'1f', color: kindColor(r.kind) }"
                    v-html="BL.icon(kindIcon(r.kind), 15, kindColor(r.kind))"></span>
              <div class="crm-card-main">
                <div class="crm-card-label bl-truncate">{{ r.label }}</div>
                <div class="crm-card-kind">{{ kindLabel(r.kind) }}</div>
              </div>
              <span class="crm-card-check" v-if="sel.has(r.key)" v-html="BL.icon('check', 13, '#fff')"></span>
            </div>
          </div>
        </template>
      </div>

      <!-- 底部 -->
      <div class="crm-foot">
        <span class="crm-count">已选 <b :class="atMax && 'is-max'">{{ sel.size }}</b> / 6</span>
        <div class="crm-foot-btns">
          <button class="crm-btn" @click="$emit('skip')">跳过(自动出图)</button>
          <button class="crm-btn is-primary" :disabled="!sel.size" @click="confirm">保存为默认看板</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  recommendations: { type: Array, default: () => [] },
  typeName: { type: String, default: '' }
})
const emit = defineEmits(['confirm', 'skip', 'close'])

const sel = ref(new Set())
const atMax = computed(() => sel.value.size >= 6)

/* 按 group 分组,保持推荐顺序 */
const groups = computed(() => {
  const map = new Map()
  for (const r of props.recommendations) {
    if (!map.has(r.group)) map.set(r.group, [])
    map.get(r.group).push(r)
  }
  return [...map.entries()].map(([name, items]) => ({ name, items }))
})

function toggle (r) {
  const s = new Set(sel.value)
  if (s.has(r.key)) s.delete(r.key)
  else { if (s.size >= 6) return; s.add(r.key) }
  sel.value = s
}
function confirm () {
  const chosen = props.recommendations.filter(r => sel.value.has(r.key))
  emit('confirm', chosen)
}

const kindLabel = (k) => k === 'pie' ? '占比(饼图)' : k === 'line' ? '趋势(折线)' : '分布(柱图)'
const kindIcon = (k) => k === 'pie' ? 'pieChart' : k === 'line' ? 'trendingUp' : 'barChart'
const kindColor = (k) => k === 'pie' ? '#e6a23c' : k === 'line' ? '#67c23a' : '#409eff'
</script>

<style scoped>
.crm-mask { position: fixed; inset: 0; z-index: 1300; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.35); }
.crm-panel { width: 860px; max-width: 92vw; max-height: 84vh; display: flex; flex-direction: column; background: var(--bl-bg-1); border-radius: 10px; box-shadow: 0 12px 40px rgba(0,0,0,.18); overflow: hidden; }

.crm-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 16px; border-bottom: 1px solid var(--bl-border); }
.crm-title { display: flex; align-items: center; gap: 8px; min-width: 0; }
.crm-title-ic { display: inline-flex; }
.crm-title b { font-size: 15px; color: var(--bl-text-1); white-space: nowrap; }
.crm-title-sub { font-size: 12px; color: var(--bl-text-3); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.crm-x { width: 26px; height: 26px; padding: 0; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 6px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.crm-x:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }

.crm-body { flex: 1; overflow-y: auto; padding: 8px 16px 12px; }
.crm-empty { padding: 40px; text-align: center; color: var(--bl-text-3); }
.crm-group { margin: 14px 0 8px; font-size: 12px; font-weight: 600; color: var(--bl-text-2); }
.crm-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }

.crm-card { position: relative; display: flex; align-items: center; gap: 10px; padding: 9px 10px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; cursor: pointer; transition: border-color .12s, box-shadow .12s; }
.crm-card:hover { border-color: var(--bl-primary); }
.crm-card.is-on { border-color: var(--bl-primary); box-shadow: 0 0 0 1px var(--bl-primary) inset; }
.crm-card.is-disabled { opacity: .45; cursor: not-allowed; }
.crm-card.is-disabled:hover { border-color: var(--bl-border); }
.crm-card-ic { width: 28px; height: 28px; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.crm-card-main { min-width: 0; flex: 1; }
.crm-card-label { font-size: 13px; color: var(--bl-text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.crm-card-kind { font-size: 11px; color: var(--bl-text-3); margin-top: 1px; }
.crm-card-check { position: absolute; top: 6px; right: 6px; width: 16px; height: 16px; border-radius: 50%; background: var(--bl-primary); display: inline-flex; align-items: center; justify-content: center; }

.crm-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 16px; border-top: 1px solid var(--bl-border); }
.crm-count { font-size: 12px; color: var(--bl-text-3); }
.crm-count b { color: var(--bl-primary); font-size: 14px; }
.crm-count b.is-max { color: #f53f3f; }
.crm-foot-btns { display: flex; gap: 8px; }
.crm-btn { height: 32px; padding: 0 16px; border: 1px solid var(--bl-border); background: var(--bl-bg-1); color: var(--bl-text-2); border-radius: 6px; cursor: pointer; font-size: 13px; }
.crm-btn:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.crm-btn.is-primary { background: var(--bl-primary); border-color: var(--bl-primary); color: #fff; }
.crm-btn.is-primary:hover { filter: brightness(1.05); color: #fff; }
.crm-btn:disabled { opacity: .5; cursor: not-allowed; filter: none; }
</style>
