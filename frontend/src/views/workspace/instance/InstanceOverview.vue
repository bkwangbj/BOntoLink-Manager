<template>
  <div class="ixo-root">
    <!-- 左侧筛选栏 -->
    <aside class="ixo-side">
      <div class="ixo-side-scroll">
        <div :class="['ixo-filter', curFilter==='all' && 'is-on']" @click="curFilter='all'">
          <span class="ixo-filter-ic" v-html="BL.icon('grid', 13)"></span>
          <span class="bl-grow">全部</span>
          <span class="ixo-filter-n">{{ types.length }}</span>
        </div>
        <div :class="['ixo-filter', curFilter==='explore' && 'is-on']" @click="curFilter='explore'">
          <span class="ixo-filter-ic" v-html="BL.icon('bookmark', 13)"></span>
          <span class="bl-grow">我的探索与列表</span>
          <span class="ixo-filter-n">{{ myExplorations.length }}</span>
        </div>
        <div :class="['ixo-filter', curFilter==='fav' && 'is-on']" @click="curFilter='fav'">
          <span class="ixo-filter-ic" v-html="BL.icon('star', 13)"></span>
          <span class="bl-grow">我的收藏</span>
          <span class="ixo-filter-n">{{ favCount }}</span>
        </div>

        <div class="ixo-side-sep"></div>
        <div class="ixo-side-title">对象类型分组</div>
        <div v-for="g in groups" :key="g.key"
             :class="['ixo-filter', curFilter===g.key && 'is-on']" @click="curFilter=g.key">
          <span class="ixo-filter-ic" v-html="BL.icon('folder', 13)"></span>
          <span class="bl-grow bl-truncate">{{ g.key }}</span>
          <span class="ixo-filter-n">{{ g.items.length }}</span>
        </div>
      </div>
      <!-- 底部分页占位(对齐文档) -->
      <div class="ixo-side-ft">
        <button class="bl-btn bl-btn-sm bl-btn-text" disabled>上一页</button>
        <button class="bl-btn bl-btn-sm bl-btn-text" disabled>下一页</button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <section class="ixo-main">
      <!-- 我的探索与列表 -->
      <div v-if="showExplorations" class="ixo-group">
        <div class="ixo-group-hd">
          <span class="ixo-group-name">我的探索与列表</span>
          <span class="ixo-group-cnt">{{ myExplorations.length }}</span>
        </div>
        <div v-if="myExplorations.length" class="ixo-grid">
          <div v-for="e in myExplorations" :key="e.id" class="ixo-card" @click="openExploration(e)">
            <span class="ixo-card-ic" :style="{ background:(e.color||'#165DFF')+'1f', color:e.color||'#165DFF' }"
                  v-html="BL.icon(e.icon||'search', 18, e.color||'#165DFF')"></span>
            <div class="ixo-card-body">
              <div class="ixo-card-title bl-truncate">{{ e.name }}</div>
              <div class="ixo-card-sub bl-truncate">{{ e.desc }}</div>
            </div>
            <span class="ixo-exp-cnt" v-if="e.count != null">{{ e.count }}</span>
          </div>
        </div>
        <div v-else class="bl-empty" style="padding:32px">暂无已保存的探索与列表（开始探索后可保存）</div>
      </div>

      <div v-if="!visibleGroups.length && curFilter!=='explore'" class="bl-empty" style="padding:60px">暂无对象类型</div>
      <div v-for="g in visibleGroups" :key="g.key" class="ixo-group">
        <div class="ixo-group-hd">
          <span class="ixo-group-name">{{ g.key }}</span>
          <span class="ixo-group-cnt">{{ g.items.length }}</span>
          <span class="bl-grow"></span>
          <div class="ixo-viewtog">
            <button class="is-on" title="列表" v-html="BL.icon('list', 13)"></button>
            <button title="图谱" @click="$emit('graph', g)" v-html="BL.icon('network', 13)"></button>
          </div>
        </div>
        <div class="ixo-grid">
          <div v-for="t in g.items" :key="t.id" class="ixo-card" @click="$emit('preview', t)">
            <span class="ixo-card-ic" :style="{ background: (t.color||'#165DFF')+'1f', color: t.color||'#165DFF' }"
                  v-html="BL.icon(t.icon||'cube', 18, t.color||'#165DFF')"></span>
            <div class="ixo-card-body">
              <div class="ixo-card-title bl-truncate">{{ t.display_name || t.api_name }}</div>
              <div class="ixo-card-sub bl-truncate">{{ t.instanceCount }} 条实例 · {{ t.propCount }} 属性</div>
            </div>
            <!-- hover 操作: 探索 + 收藏 -->
            <div class="ixo-card-actions">
              <button class="ixo-card-act" title="开启探索" @click.stop="$emit('explore', t)"
                      v-html="BL.icon('externalLink', 14)"></button>
              <button class="ixo-card-act ixo-star" :class="isFav(t.id) && 'is-fav'"
                      :title="isFav(t.id)?'取消收藏':'收藏'" @click.stop="onStar(t)"
                      v-html="BL.icon('star', 14)"></button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import { useFavorites } from './favorites.js'

const props = defineProps({
  types: { type: Array, default: () => [] }
})
defineEmits(['explore', 'preview', 'graph'])

const { isFav, toggle, favSet } = useFavorites()
const curFilter = ref('all')

const favCount = computed(() => props.types.filter(t => favSet.value.has(t.id)).length)

/* 我的探索与列表(演示数据；保存探索功能后续接入) */
const myExplorations = ref([
  { id: 'exp-1', name: '高水位测站探索', icon: 'station', color: '#165DFF', desc: '水文测站 · 水位 > 100', count: 26 },
  { id: 'exp-2', name: '重点水库清单', icon: 'dam', color: '#FF7D00', desc: '水库 · 已保存列表', count: 18 }
])
const showExplorations = computed(() =>
  curFilter.value === 'explore' || (curFilter.value === 'all' && myExplorations.value.length))
function openExploration(e) { BL.info(`演示：恢复已保存探索「${e.name}」（恢复功能规划中）`) }

/* 按 groupKey(领域) 分组 */
const groups = computed(() => {
  const map = new Map()
  for (const t of props.types) {
    const k = t.groupKey || '未分组'
    if (!map.has(k)) map.set(k, { key: k, items: [] })
    map.get(k).items.push(t)
  }
  return [...map.values()]
})

const visibleGroups = computed(() => {
  if (curFilter.value === 'explore') return []
  if (curFilter.value === 'all') return groups.value
  if (curFilter.value === 'fav') {
    const items = props.types.filter(t => favSet.value.has(t.id))
    return items.length ? [{ key: '我的收藏', items }] : []
  }
  return groups.value.filter(g => g.key === curFilter.value)
})

function onStar(t) {
  const now = toggle(t.id)
  BL.success(now ? `已收藏 ${t.display_name || t.api_name}` : '已取消收藏')
}
</script>

<style scoped>
.ixo-root { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* 左侧筛选栏 200px */
.ixo-side {
  width: 200px; flex-shrink: 0;
  border-right: 1px solid var(--bl-border);
  display: flex; flex-direction: column;
  background: var(--bl-bg-1);
}
.ixo-side-scroll { flex: 1; overflow: auto; padding: 8px; }
.ixo-side-sep { height: 1px; background: var(--bl-divider); margin: 8px 8px 0; }
.ixo-side-title {
  font-size: 11px; color: var(--bl-text-3); padding: 12px 8px 4px;
  letter-spacing: .5px;
}
.ixo-filter {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 8px; border-radius: 6px; cursor: pointer;
  font-size: 13px; color: var(--bl-text-2);
}
.ixo-filter:hover { background: var(--bl-bg-hover); }
.ixo-filter.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.ixo-filter-ic { display: inline-flex; color: inherit; }
.ixo-filter-n { font-size: 11px; color: var(--bl-text-3); }
.ixo-filter.is-on .ixo-filter-n { color: var(--bl-primary); }
.ixo-side-ft { display: flex; gap: 4px; padding: 8px; border-top: 1px solid var(--bl-divider); }

/* 右侧内容区 */
.ixo-main { flex: 1; overflow: auto; padding: 14px 18px; background: var(--bl-bg-2); }
.ixo-group { margin-bottom: 18px; }
.ixo-group-hd { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.ixo-group-name { font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.ixo-group-cnt {
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-3, #f0f2f5); border-radius: 10px; padding: 1px 8px;
}
.ixo-viewtog { display: inline-flex; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; }
.ixo-viewtog button {
  width: 28px; height: 24px; border: 0; background: var(--bl-bg-1);
  color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.ixo-viewtog button.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixo-viewtog button:hover:not(.is-on) { background: var(--bl-bg-hover); }

.ixo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}
.ixo-card {
  position: relative;
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px;
  cursor: pointer; transition: box-shadow .15s, border-color .15s;
}
.ixo-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.08); border-color: var(--bl-primary-border); }
.ixo-card-ic { width: 36px; height: 36px; border-radius: 8px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ixo-card-body { flex: 1; min-width: 0; }
.ixo-card-title { font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.ixo-card-sub { font-size: 11.5px; color: var(--bl-text-3); margin-top: 2px; }
.ixo-exp-cnt { font-size: 11px; color: var(--bl-text-3); flex-shrink: 0; align-self: flex-start; }
.ixo-card-actions {
  display: flex; gap: 2px; flex-shrink: 0;
  opacity: 0; transition: opacity .15s;
}
.ixo-card:hover .ixo-card-actions { opacity: 1; }
.ixo-card-act {
  width: 28px; height: 28px; border: 0; background: transparent; border-radius: 6px;
  color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.ixo-card-act:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }
.ixo-star.is-fav { color: #fbbf24; opacity: 1; }
/* 已收藏的星标即使未 hover 也显示 */
.ixo-card-actions:has(.is-fav) { opacity: 1; }
</style>
