<template>
  <div class="sr-root">
    <!-- 标签：全部 / 实例对象 / 对象类型 -->
    <div class="sr-tabs">
      <button :class="['sr-tab', tab==='all' && 'is-on']" @click="tab='all'">全部 <i>{{ counts.all || 0 }}</i></button>
      <button :class="['sr-tab', tab==='instances' && 'is-on']" @click="tab='instances'">实例对象 <i>{{ counts.instances || 0 }}</i></button>
      <button :class="['sr-tab', tab==='types' && 'is-on']" @click="tab='types'">对象类型 <i>{{ counts.objectTypes || 0 }}</i></button>
      <span class="bl-grow"></span>
      <button class="bl-btn bl-btn-sm bl-btn-text" @click="$emit('close')" v-html="iconText('x','退出搜索')"></button>
    </div>

    <div class="sr-body">
      <!-- 左侧导航 -->
      <aside class="sr-nav">
        <div :class="['sr-nav-item', nav==='all' && 'is-on']" @click="setNav('all')">
          <span class="bl-grow">全部结果</span><span class="sr-nav-n">{{ counts.all || 0 }}</span>
        </div>

        <div class="sr-nav-sep"></div>
        <div class="sr-nav-title"><span class="bl-grow">对象类型</span><span class="sr-nav-col">实例数</span></div>
        <div v-for="g in data.instanceGroups" :key="'n'+g.classId"
             :class="['sr-nav-item', nav==='type:'+g.classId && 'is-on']" @click="setNav('type:'+g.classId)">
          <span class="sr-nav-ic" v-html="BL.icon('filter', 11, 'var(--bl-text-3)')"></span>
          <span class="bl-grow bl-truncate">{{ g.className }}</span>
          <span class="sr-nav-n">{{ g.hitCount }}</span>
        </div>
        <div class="sr-nav-more" @click="tab='instances'; nav='all'">查看</div>

        <div v-if="data.groups?.length" class="sr-nav-sep"></div>
        <template v-if="data.groups?.length">
          <div class="sr-nav-title"><span class="bl-grow">分组</span><span class="sr-nav-col">对象类型数</span></div>
          <div v-for="gp in data.groups" :key="'g'+gp.key"
               :class="['sr-nav-item', nav==='group:'+gp.key && 'is-on']" @click="setNav('group:'+gp.key)">
            <span class="sr-nav-ic" v-html="BL.icon('folder', 11, 'var(--bl-text-3)')"></span>
            <span class="bl-grow bl-truncate">{{ gp.key }}</span>
            <span class="sr-nav-n">{{ gp.count }}</span>
          </div>
          <div class="sr-nav-more" @click="tab='types'; nav='all'">查看</div>
        </template>
      </aside>

      <!-- 右侧结果 -->
      <section class="sr-content">
        <div v-if="loading" class="bl-empty" style="padding:60px">搜索中…</div>
        <div v-else-if="empty" class="bl-empty" style="padding:60px">未找到与「{{ data.query }}」匹配的结果</div>

        <template v-else>
          <!-- 实例对象分组 -->
          <template v-if="tab !== 'types'">
            <div v-for="g in visibleGroups" :key="g.classId" class="sr-group">
              <div class="sr-group-hd">
                <span class="sr-group-ic" :style="{ background:(g.color||'#165DFF')+'1f', color:g.color||'#165DFF' }"
                      v-html="BL.icon(g.icon||'cube', 14, g.color||'#165DFF')"></span>
                <span class="sr-group-name">{{ g.className }}</span>
                <span class="sr-group-cnt">{{ g.hitCount }}</span>
              </div>
              <div v-for="r in g.samples" :key="r.id" class="sr-item" @click="$emit('open-instance', { classId: g.classId, row: r })">
                <span class="sr-item-ic" :style="{ background:(r.color||'#86909c')+'1f', color:r.color||'#86909c' }"
                      v-html="BL.icon(r.icon||'cube', 13, r.color||'#86909c')"></span>
                <div class="sr-item-body">
                  <div class="sr-item-title bl-truncate">{{ r.title }} <span class="sr-item-code">{{ r.code }}</span></div>
                  <div class="sr-item-desc bl-truncate">{{ r.desc || '—' }}</div>
                </div>
                <div class="sr-item-actions">
                  <button class="sr-act" title="探索此实例" @click.stop="exploreInstance(g, r)" v-html="BL.icon('externalLink', 14)"></button>
                  <button class="sr-act sr-star" :class="isFav(g.classId)&&'is-fav'" :title="isFav(g.classId)?'取消收藏':'收藏'"
                          @click.stop="toggleFav(g.classId)" v-html="BL.icon('star', 14)"></button>
                </div>
              </div>
              <div v-if="g.hitCount > g.samples.length" class="sr-more" @click="exploreClass(g)">
                查看全部 {{ g.hitCount }} 条 →
              </div>
            </div>
          </template>

          <!-- 对象类型卡 -->
          <template v-if="tab !== 'instances' && data.objectTypes?.length">
            <div class="sr-types-hd" v-if="tab==='all'">匹配的对象类型</div>
            <div class="sr-types">
              <div v-for="t in data.objectTypes" :key="t.id" class="sr-type-card" @click="$emit('explore', t)">
                <span class="sr-type-ic" :style="{ background:(t.color||'#165DFF')+'1f', color:t.color||'#165DFF' }"
                      v-html="BL.icon(t.icon||'cube', 16, t.color||'#165DFF')"></span>
                <div class="bl-grow" style="min-width:0">
                  <div class="bl-truncate" style="font-weight:600;font-size:13px">{{ t.display_name }}</div>
                  <div class="bl-muted" style="font-size:11px">{{ t.instanceCount }} 条实例 · {{ t.domainLabel }}</div>
                </div>
                <button class="sr-act" title="探索" @click.stop="$emit('explore', t)" v-html="BL.icon('externalLink', 14)"></button>
              </div>
            </div>
          </template>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { instanceApi } from '@/api'
import { useFavorites } from './favorites.js'

const props = defineProps({ query: { type: String, default: '' } })
const emit = defineEmits(['explore', 'open-instance', 'close'])

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }
const { isFav, toggle } = useFavorites()
function toggleFav(id) { const now = toggle(id); BL.success(now ? '已收藏' : '已取消收藏') }

const data = ref({ objectTypes: [], instanceGroups: [], groups: [], counts: {}, query: '' })
const counts = computed(() => data.value.counts || {})
const loading = ref(false)
const tab = ref('all')          // all | instances | types
const nav = ref('all')          // all | type:<id> | group:<key>

const empty = computed(() => !data.value.instanceGroups?.length && !data.value.objectTypes?.length)

const visibleGroups = computed(() => {
  let gs = data.value.instanceGroups || []
  if (nav.value.startsWith('type:')) gs = gs.filter(g => g.classId === nav.value.slice(5))
  else if (nav.value.startsWith('group:')) gs = gs.filter(g => g.domainLabel === nav.value.slice(6))
  return gs
})

function setNav(v) { nav.value = v }
function exploreClass(g) {
  emit('explore', { id: g.classId, display_name: g.className, icon: g.icon, color: g.color })
}
function exploreInstance(g, r) {
  // 单条实例:页签标题用实例名(与实例详情进入一致)
  emit('explore', { id: g.classId, display_name: g.className, icon: r.icon || g.icon, color: r.color || g.color, fixedTitle: r.title })
}

async function load() {
  if (!props.query?.trim()) { data.value = { objectTypes: [], instanceGroups: [], groups: [], counts: {}, query: props.query }; return }
  loading.value = true
  tab.value = 'all'; nav.value = 'all'
  try { data.value = await instanceApi.searchResults(props.query) }
  catch { data.value = { objectTypes: [], instanceGroups: [], groups: [], counts: {}, query: props.query } }
  loading.value = false
}
watch(() => props.query, load, { immediate: true })
</script>

<style scoped>
.sr-root { flex: 1; display: flex; flex-direction: column; min-height: 0; background: var(--bl-bg-2); }
.sr-tabs { display: flex; align-items: center; gap: 4px; padding: 6px 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); flex-shrink: 0; }
.sr-tab { padding: 6px 12px; border: 0; background: transparent; cursor: pointer; font-size: 13px; color: var(--bl-text-2); border-radius: 6px; display: inline-flex; align-items: center; gap: 6px; }
.sr-tab i { font-style: normal; font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 9px; padding: 0 6px; }
.sr-tab:hover { background: var(--bl-bg-hover); }
.sr-tab.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.sr-tab.is-on i { color: var(--bl-primary); background: #fff; }

.sr-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }
.sr-nav { width: 200px; flex-shrink: 0; border-right: 1px solid var(--bl-border); overflow: auto; padding: 8px; background: var(--bl-bg-1); }
.sr-nav-item { display: flex; align-items: center; gap: 6px; padding: 7px 8px; border-radius: 6px; cursor: pointer; font-size: 12.5px; color: var(--bl-text-2); }
.sr-nav-item:hover { background: var(--bl-bg-hover); }
.sr-nav-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.sr-nav-ic { display: inline-flex; flex-shrink: 0; }
.sr-nav-n { font-size: 11px; color: var(--bl-text-3); }
.sr-nav-item.is-on .sr-nav-n { color: var(--bl-primary); }
.sr-nav-title { display: flex; align-items: center; font-size: 11px; color: var(--bl-text-3); padding: 12px 8px 4px; letter-spacing: .3px; }
.sr-nav-col { flex-shrink: 0; color: var(--bl-text-3); font-weight: 500; }
.sr-nav-more { font-size: 12px; color: var(--bl-primary); padding: 4px 8px; cursor: pointer; text-align: right; }
.sr-nav-more:hover { text-decoration: underline; }
.sr-nav-sep { height: 1px; background: var(--bl-divider); margin: 8px 8px 0; }

.sr-content { flex: 1; overflow: auto; padding: 14px 18px; }
.sr-group { margin-bottom: 16px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; overflow: hidden; }
.sr-group-hd { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-bottom: 1px solid var(--bl-divider); }
.sr-group-ic { width: 26px; height: 26px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.sr-group-name { font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.sr-group-cnt { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 9px; padding: 1px 7px; }
.sr-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; cursor: pointer; border-bottom: 1px solid var(--bl-divider); }
.sr-item:last-child { border-bottom: 0; }
.sr-item:hover { background: var(--bl-bg-hover); }
.sr-item-ic { width: 28px; height: 28px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.sr-item-body { flex: 1; min-width: 0; }
.sr-item-title { font-size: 13px; font-weight: 500; color: var(--bl-text-1); }
.sr-item-code { font-size: 11px; color: var(--bl-text-3); font-family: var(--bl-font-mono, monospace); margin-left: 4px; }
.sr-item-desc { font-size: 11.5px; color: var(--bl-text-3); margin-top: 2px; }
.sr-item-actions { display: flex; gap: 2px; flex-shrink: 0; opacity: 0; transition: opacity .15s; }
.sr-item:hover .sr-item-actions { opacity: 1; }
.sr-act { width: 28px; height: 28px; border: 0; background: transparent; color: var(--bl-text-3); border-radius: 6px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.sr-act:hover { background: var(--bl-bg-2); color: var(--bl-primary); }
.sr-star.is-fav { color: #fbbf24; opacity: 1; }
.sr-item-actions:has(.is-fav) { opacity: 1; }
.sr-more { padding: 8px 14px; font-size: 12px; color: var(--bl-primary); cursor: pointer; background: var(--bl-bg-2); }
.sr-more:hover { text-decoration: underline; }

.sr-types-hd { font-size: 12px; color: var(--bl-text-3); margin: 6px 0 8px; }
.sr-types { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 10px; }
.sr-type-card { display: flex; align-items: center; gap: 10px; padding: 12px 14px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; cursor: pointer; }
.sr-type-card:hover { border-color: var(--bl-primary-border); box-shadow: 0 4px 14px rgba(0,0,0,.06); }
.sr-type-ic { width: 34px; height: 34px; border-radius: 8px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
</style>
