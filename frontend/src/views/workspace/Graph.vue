<template>
  <div class="page">
    <PageHeader title="图谱" subtitle="Graph · 行业 → 领域 → 分组 → 对象 → 关系 可视化探索">
      <template #actions>
        <button class="bl-btn bl-btn-text" @click="reset" v-html="iconText('grid','重置布局')"></button>
        <button class="bl-btn bl-btn-text" @click="zoomIn" v-html="iconText('plus','放大')"></button>
        <button class="bl-btn bl-btn-text" @click="zoomOut" v-html="iconText('minus','缩小')"></button>
      </template>
    </PageHeader>

    <div class="canvas" ref="canvasRef"
         @wheel.prevent="onWheel" @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp">
      <svg :viewBox="`${view.x} ${view.y} ${view.w} ${view.h}`" preserveAspectRatio="xMidYMid meet">
        <g>
          <line v-for="(e,i) in graph.edges" :key="'e'+i"
                :x1="pos(e.source).x" :y1="pos(e.source).y"
                :x2="pos(e.target).x" :y2="pos(e.target).y"
                :stroke="edgeColor(e)" stroke-width="1" stroke-dasharray="4 4" />
        </g>
        <g>
          <g v-for="n in graph.nodes" :key="n.id" :transform="`translate(${pos(n.id).x},${pos(n.id).y})`"
             @click.stop="select(n)" @dblclick.stop="dive(n)" style="cursor:pointer">
            <circle r="22" :fill="n.color || nodeColor(n.kind)" stroke="#fff" stroke-width="2" />
            <text text-anchor="middle" y="40" font-size="11" :fill="textColor()">{{ truncate(n.label) }}</text>
          </g>
        </g>
      </svg>

      <div v-if="selectedNode" class="info-panel bl-card bl-card-pad">
        <div class="bl-row" style="gap:8px;align-items:center">
          <span class="bl-icon-block bl-icon-block-sm" :style="{background: selectedNode.color || nodeColor(selectedNode.kind)}" v-html="BL.icon(selectedNode.icon || 'cube', 12, '#fff')"></span>
          <div class="bl-grow">
            <div style="font-weight:600">{{ selectedNode.label }}</div>
            <div class="bl-muted" style="font-size:11px">{{ selectedNode.kind }}</div>
          </div>
          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="selectedNode=null" v-html="BL.icon('x', 12)"></button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

const canvasRef = ref(null)
const graph = ref({ nodes: [], edges: [] })
const positions = reactive({})
const view = reactive({ x: 0, y: 0, w: 1000, h: 600 })
const selectedNode = ref(null)

function nodeColor(kind) {
  return ({ industry:'#165DFF', domain:'#00B42A', group:'#FF7D00', class:'#722ED1', interface:'#13C2C2' })[kind] || '#86909C'
}
function edgeColor(e) {
  return e.kind === 'hierarchy' ? '#C9CDD4' : '#722ED1'
}
function textColor() { return getComputedStyle(document.documentElement).getPropertyValue('--bl-text-2').trim() || '#4E5969' }
function truncate(s) { return (s || '').slice(0, 10) }

function pos(id) { return positions[id] || { x: 0, y: 0 } }

function layout() {
  const buckets = { industry:[], domain:[], group:[], class:[], interface:[] }
  graph.value.nodes.forEach(n => { (buckets[n.kind] || (buckets[n.kind] = [])).push(n) })
  const order = ['industry','domain','group','class','interface']
  const rowH = 130
  const cw = 1000
  order.forEach((k, ri) => {
    const arr = buckets[k] || []
    const step = cw / (arr.length + 1)
    arr.forEach((n, i) => {
      positions[n.id] = { x: step * (i + 1), y: rowH * (ri + 1) }
    })
  })
  view.w = 1000; view.h = rowH * (order.length + 1)
  view.x = 0; view.y = 0
}

async function load() {
  graph.value = await resourceApi.graph().catch(() => ({ nodes: [], edges: [] }))
  layout()
}

function select(n) { selectedNode.value = n }
function dive(n) { BL.info(`双击：${n.label}（${n.kind}）`) }
function reset() { layout() }
function zoomIn() { view.w *= 0.85; view.h *= 0.85 }
function zoomOut() { view.w /= 0.85; view.h /= 0.85 }

let dragging = false
let start = null
function onWheel(e) {
  const k = e.deltaY > 0 ? 1.1 : 0.9
  view.w *= k; view.h *= k
}
function onMouseDown(e) { dragging = true; start = { x: e.clientX, y: e.clientY, vx: view.x, vy: view.y } }
function onMouseMove(e) {
  if (!dragging || !canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  const dx = (e.clientX - start.x) / rect.width * view.w
  const dy = (e.clientY - start.y) / rect.height * view.h
  view.x = start.vx - dx
  view.y = start.vy - dy
}
function onMouseUp() { dragging = false }

onMounted(load)
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.canvas {
  flex: 1; position: relative;
  background: linear-gradient(0deg, var(--bl-bg-2), var(--bl-bg-2)) 0 0/24px 24px;
  background-image: radial-gradient(var(--bl-border) 1px, transparent 1px);
  background-size: 24px 24px;
  cursor: grab;
  overflow: hidden;
}
.canvas:active { cursor: grabbing; }
svg { width: 100%; height: 100%; display: block; }
.info-panel { position: absolute; right: 16px; top: 16px; width: 280px; }
</style>
