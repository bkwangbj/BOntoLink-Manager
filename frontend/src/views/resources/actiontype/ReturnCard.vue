<template>
  <div class="rc" :class="[`is-${scheme}`, compact && 'is-compact']">
    <span v-if="scheme !== 'inline'" class="rc-ic" v-html="BL.icon('box', compact ? 11 : 13, '#fff')"></span>
    <div class="rc-txt">
      <div class="rc-title">{{ title || '(未选属性)' }}</div>
      <div v-if="sub" class="rc-sub">{{ sub }}</div>
    </div>
    <span v-for="t in tags" :key="t" class="rc-tag">{{ t }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  items: { type: Array, default: () => [] },      // [{ code, name }] 顺序即卡片槽位
  scheme: { type: String, default: 'title_sub' }, // inline | title_sub | title_tags | title_sub_tags
  compact: { type: Boolean, default: false },
})
/* 没有真实实例数据, 一律用 {属性名} 占位, 一眼看出每块取哪个属性 */
const ph = it => `{${it.name || it.code}}`
const title = computed(() => {
  const a = props.items
  if (!a.length) return ''
  return props.scheme === 'inline' ? a.map(ph).join(' · ') : ph(a[0])
})
const sub = computed(() => {
  const a = props.items
  if (props.scheme === 'title_sub') return a.slice(1).map(ph).join(' · ')
  if (props.scheme === 'title_sub_tags') return a[1] ? ph(a[1]) : ''
  return ''
})
const tags = computed(() => {
  const a = props.items
  if (props.scheme === 'title_tags') return a.slice(1).map(ph)
  if (props.scheme === 'title_sub_tags') return a.slice(2).map(ph)
  return []
})
</script>

<style scoped>
.rc { display: flex; align-items: center; gap: 8px; padding: 8px 10px; min-width: 0;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; }
/* 缩略预览宽度有限, 标签换行而不是把标题挤没 */
.rc.is-compact { padding: 7px 8px; gap: 6px; flex-wrap: wrap; }
.rc.is-compact .rc-txt { flex: 1 1 60%; }
.rc-ic { width: 24px; height: 24px; border-radius: 6px; background: var(--bl-primary); flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center; }
.rc.is-compact .rc-ic { width: 20px; height: 20px; border-radius: 5px; }
.rc-txt { flex: 1; min-width: 0; }
.rc-title { font-size: 13px; color: var(--bl-text-1); line-height: 1.35;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rc.is-compact .rc-title { font-size: 12px; }
.rc-sub { font-size: 11.5px; color: var(--bl-text-3); line-height: 1.3;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rc-tag { flex-shrink: 0; font-size: 10.5px; padding: 1px 6px; border-radius: 4px;
  background: var(--bl-primary-soft); color: var(--bl-primary); }
</style>
