<template>
  <div class="ixt-root">
    <Teleport to="#ix-toolbar-slot">
      <div class="ixt-bar">
        <span class="bl-muted" style="font-size:12px">共 {{ filtered.length }} 个对象类型</span>
        <input class="bl-input bl-input-sm" style="width:240px;height:30px;box-sizing:border-box" v-model="kw" placeholder="搜索对象类型 / 编码…" />
      </div>
    </Teleport>
    <div class="ixt-table-wrap">
      <table class="bl-table ixt-table">
        <thead>
          <tr>
            <th style="width:34%" @click="sortBy('display_name')">对象类型 <SortCaret :col="'display_name'" :sort="sort" :order="order" /></th>
            <th style="width:22%">行业 / 领域</th>
            <th class="ixt-num" @click="sortBy('instanceCount')">实例数 <SortCaret :col="'instanceCount'" :sort="sort" :order="order" /></th>
            <th class="ixt-num" @click="sortBy('propCount')">属性 <SortCaret :col="'propCount'" :sort="sort" :order="order" /></th>
            <th class="ixt-num" @click="sortBy('linkCount')">关系 <SortCaret :col="'linkCount'" :sort="sort" :order="order" /></th>
            <th style="width:96px"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in sorted" :key="t.id" @click="$emit('preview', t)">
            <td>
              <div class="ixt-name">
                <span class="ixt-ic" :style="{ background:(t.color||'#165DFF')+'1f', color:t.color||'#165DFF' }"
                      v-html="BL.icon(t.icon||'cube', 14, t.color||'#165DFF')"></span>
                <div>
                  <div class="bl-truncate" style="font-weight:500">{{ t.display_name || t.api_name }}</div>
                  <div class="bl-mono bl-muted" style="font-size:11px">{{ t.api_name }}</div>
                </div>
              </div>
            </td>
            <td><span class="bl-muted" style="font-size:12px">{{ t.industryLabel || '—' }} / {{ t.domainLabel || '—' }}</span></td>
            <td class="ixt-num"><b>{{ t.instanceCount }}</b></td>
            <td class="ixt-num">{{ t.propCount }}</td>
            <td class="ixt-num">{{ t.linkCount }}</td>
            <td>
              <button class="bl-btn bl-btn-sm bl-btn-text" @click.stop="$emit('explore', t)"
                      v-html="iconText('externalLink','探索')"></button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, h } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({ types: { type: Array, default: () => [] } })
defineEmits(['explore', 'preview'])

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

const kw = ref('')
const sort = ref('instanceCount')
const order = ref('desc')

const filtered = computed(() => {
  const q = kw.value.trim().toLowerCase()
  if (!q) return props.types
  return props.types.filter(t =>
    (t.display_name || '').toLowerCase().includes(q) ||
    (t.api_name || '').toLowerCase().includes(q))
})
const sorted = computed(() => {
  const arr = [...filtered.value]
  const k = sort.value, desc = order.value === 'desc'
  arr.sort((a, b) => {
    const va = a[k], vb = b[k]
    let c = (typeof va === 'number' && typeof vb === 'number') ? va - vb : String(va ?? '').localeCompare(String(vb ?? ''))
    return desc ? -c : c
  })
  return arr
})
function sortBy(k) {
  if (sort.value === k) order.value = order.value === 'desc' ? 'asc' : 'desc'
  else { sort.value = k; order.value = 'desc' }
}

const SortCaret = {
  props: ['col', 'sort', 'order'],
  render() {
    if (this.col !== this.sort) return null
    return h('span', { style: 'font-size:10px;color:var(--bl-primary)' }, this.order === 'desc' ? ' ▼' : ' ▲')
  }
}
</script>

<style scoped>
.ixt-root { flex: 1; display: flex; flex-direction: column; min-height: 0; background: var(--bl-bg-2); }
/* 传送到二级标签行右侧槽位，内联靠右 */
.ixt-bar { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: flex-end; gap: 12px; padding: 4px 0; }
.ixt-table-wrap { flex: 1; overflow: auto; padding: 8px 16px 16px; }
.ixt-table { background: var(--bl-bg-1); }
.ixt-table th { cursor: pointer; user-select: none; white-space: nowrap; }
.ixt-table tbody tr { cursor: pointer; }
.ixt-num { text-align: right; font-variant-numeric: tabular-nums; }
.ixt-name { display: flex; align-items: center; gap: 8px; min-width: 0; }
.ixt-ic { width: 26px; height: 26px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
</style>
