<template>
  <div :class="['erx-card', d.type === 'object' ? 'is-obj' : (d.relType === 1 ? 'is-main' : 'is-supp')]">
    <div class="erx-hd">
      <span class="erx-hd-title bl-truncate">{{ d.title }}</span>
      <span class="erx-hd-cnt">{{ d.countText }}</span>
    </div>
    <div class="erx-list">
      <div v-for="(r, i) in d.rows" :key="r.key"
           class="erx-row"
           :class="{ 'is-mapped': r.mapped, 'is-sortable': sortable, 'is-dragging': sortable && dragIdx === i }"
           :draggable="sortable"
           @dragstart="onDragStart(i, $event)"
           @dragover="onDragOver(i, $event)"
           @drop="onDrop(i)"
           @dragend="onDragEnd"
           @mousedown="sortable && $event.stopPropagation()">
        <div v-if="sortable && overIdx === i && dragIdx !== -1 && dragIdx !== i" class="erx-drop-line"></div>
        <template v-if="d.type === 'object'">
          <span class="erx-no">{{ r.no }}</span>
          <span class="erx-ic" :style="{ background: r.color }" v-html="r.iconSvg"></span>
          <span class="erx-dt">{{ r.dt }}</span>
          <span class="erx-text bl-truncate">{{ r.label }}<span class="erx-api"> ({{ r.api }})</span></span>
        </template>
        <template v-else>
          <span class="erx-text bl-mono bl-truncate">{{ r.name }}</span>
          <span class="erx-dt">{{ r.dt }}</span>
        </template>
      </div>
      <div v-if="!d.rows.length" class="erx-empty">无字段</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
/* x6-vue-shape 把 node / graph 作为 props 传入 */
const props = defineProps({ node: { type: Object, required: true }, graph: Object })
const d = ref(props.node.getData() || {})
function sync() { d.value = props.node.getData() || {} }
onMounted(() => props.node.on('change:data', sync))
onBeforeUnmount(() => props.node.off('change:data', sync))

/* 排序模式 (仅对象卡片): 拖属性行调顺序 */
const sortable = computed(() => d.value.type === 'object' && d.value.mode === 'sort')
const dragIdx = ref(-1)
const overIdx = ref(-1)
function onDragStart(i, ev) {
  if (!sortable.value) { ev.preventDefault(); return }
  dragIdx.value = i
  ev.dataTransfer.effectAllowed = 'move'
}
function onDragOver(i, ev) {
  if (!sortable.value || dragIdx.value < 0) return
  ev.preventDefault()
  overIdx.value = i
}
function onDrop(i) {
  const from = dragIdx.value
  reset()
  if (from < 0 || from === i) return
  const rows = [...(d.value.rows || [])]
  const [m] = rows.splice(from, 1)
  rows.splice(i, 0, m)
  const ids = rows.map(r => r.key)
  if (typeof d.value.onReorder === 'function') d.value.onReorder(ids)
}
function onDragEnd() { reset() }
function reset() { dragIdx.value = -1; overIdx.value = -1 }
</script>

<style scoped>
/* 卡片高度由 X6 节点尺寸决定; 这里行高/表头高必须与编排组件的 HH/ROWH 常量一致, 否则端口与行错位 */
.erx-card {
  width: 100%; height: 100%; box-sizing: border-box;
  background: #fff; border: 2px solid transparent; border-radius: 8px;
  overflow: hidden; display: flex; flex-direction: column;
  font-size: 12.5px; box-shadow: 0 2px 8px rgba(0,0,0,.06);
}
.erx-card.is-obj  { border-color: #722ED1; }
.erx-card.is-main { border-color: #00B42A; }
.erx-card.is-supp { border-color: #FF7D00; }

.erx-hd {
  height: 38px; flex-shrink: 0; box-sizing: border-box;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px; color: #fff; font-weight: 600; font-size: 13px;
}
.is-obj  .erx-hd { background: #722ED1; }
.is-main .erx-hd { background: #00B42A; }
.is-supp .erx-hd { background: #FF7D00; }
.erx-hd-cnt {
  font-size: 11px; opacity: .9; flex-shrink: 0;
  background: rgba(255,255,255,.18); padding: 2px 8px; border-radius: 10px;
}

.erx-list { flex: 1; overflow: hidden; }
.erx-row {
  position: relative;
  height: 36px; box-sizing: border-box;
  display: flex; align-items: center; gap: 8px;
  padding: 0 14px; border-top: 1px solid #f0f0f0;
}
.erx-row:first-child { border-top: 0; }
.erx-row.is-mapped { background: #f0fff4; }
.erx-row.is-sortable { cursor: grab; }
.erx-row.is-sortable:hover { background: #f5f7fa; }
.erx-row.is-dragging { opacity: .4; }
.erx-drop-line { position: absolute; left: 6px; right: 6px; top: -1px; height: 2px; background: #1677ff; border-radius: 1px; }
.erx-no {
  width: 20px; height: 20px; border-radius: 50%; flex-shrink: 0;
  background: #1677ff; color: #fff; font-size: 11px; font-weight: 600;
  display: inline-flex; align-items: center; justify-content: center;
}
.erx-ic {
  width: 16px; height: 16px; border-radius: 3px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.erx-dt {
  font-size: 10px; color: #86909c; flex-shrink: 0;
  background: #f2f3f5; padding: 1px 6px; border-radius: 3px;
  font-family: ui-monospace, Consolas, monospace;
}
.erx-text { flex: 1; min-width: 0; color: #1d2129; }
.erx-api { color: #86909c; font-family: ui-monospace, Consolas, monospace; }
.erx-empty { padding: 16px; color: #86909c; font-size: 12px; text-align: center; }
</style>
