<template>
  <div class="ovc" :class="[depth === 0 ? 'is-root' : 'is-nested', 'is-' + node.logic]">
    <div class="ovc-hd">
      <span class="ovc-hd-txt">满足以下
        <select class="ovc-logic" v-model="node.logic">
          <option value="all">全部</option><option value="any">任一</option><option value="none">全部不</option>
        </select>
        条件:</span>
      <span style="flex:1"></span>
      <button v-if="depth > 0" class="ovc-x" title="删除该条件组" @click="$emit('remove')" v-html="BL.icon('x', 12)"></button>
    </div>

    <div class="ovc-body">
      <div v-for="(child, i) in node.children" :key="child._k" class="ovc-item"
           draggable="true" @dragstart="dragIdx = i" @dragover.prevent @drop="onDrop(i)" @dragend="dragIdx = null"
           :class="{ 'is-dragging': dragIdx === i }">
        <OverrideCondGroup v-if="child.type === 'group'" :node="child" :depth="depth + 1" :selected="selected"
                           @remove="node.children.splice(i,1)" @select="$emit('select', $event)" />
        <div v-else :class="['ovc-cond', selected === child && 'is-on', !condReady(child) && 'is-todo']"
             @click="$emit('select', child)">
          <span class="ovc-sub-ic" :style="{ background: child.subject === 'user' ? '#722ED1' : '#00B42A' }"
                v-html="BL.icon(child.subject === 'user' ? 'user' : 'edit', 11, '#fff')"></span>
          <span class="bl-truncate ovc-cond-txt">{{ condText(child) }}</span>
          <span v-if="!condReady(child)" class="ovc-todo" title="条件未配置完整" v-html="BL.icon('info', 12)"></span>
          <button class="ovc-x" title="删除条件" @click.stop="node.children.splice(i,1)" v-html="BL.icon('x', 12)"></button>
        </div>
      </div>
      <div v-if="!node.children.length" class="ovc-empty">暂无条件,点下方添加</div>

      <div class="ovc-add">
        <span>+ 添加</span>
        <a @click="addCond">条件</a>
        <span class="ovc-or">或</span>
        <a @click="addGroup">逻辑运算符</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { BL } from '@/lib/bl.js'
import { condText, condReady, ovUid } from './overrideModel.js'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  selected: { type: Object, default: null },
})
const emit = defineEmits(['remove', 'select'])

const dragIdx = ref(null)
function onDrop(target) {
  const from = dragIdx.value; dragIdx.value = null
  if (from === null || from === target) return
  const [it] = props.node.children.splice(from, 1)
  props.node.children.splice(target, 0, it)
}
function addCond() {
  const c = { _k: ovUid(), type: 'cond', subject: '', field: '', fieldName: '', dataType: '', operator: '', value: '' }
  props.node.children.push(c)
  emit('select', c)          // 新增后自动选中, 右侧面板进入配置流程
}
function addGroup() {
  props.node.children.push({ _k: ovUid(), type: 'group', logic: 'any', children: [] })
}
</script>

<style scoped>
.ovc { border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-1); }
.ovc.is-nested { margin: 2px 0; background: var(--bl-bg-2); }
.ovc-hd { display: flex; align-items: center; gap: 6px; padding: 7px 10px; border-bottom: 1px solid var(--bl-divider); font-size: 12.5px; color: var(--bl-text-2); }
.ovc-logic { border: 1px solid var(--bl-border); border-radius: 4px; background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 600; font-size: 12px; padding: 1px 2px; outline: none; }
.ovc-body { padding: 8px 10px; display: flex; flex-direction: column; gap: 6px; }
.ovc-item.is-dragging { opacity: .45; }
.ovc-cond { display: flex; align-items: center; gap: 7px; padding: 7px 9px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); cursor: pointer; font-size: 12.5px; }
.ovc-cond:hover { border-color: var(--bl-primary); }
.ovc-cond.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.ovc-cond.is-todo { border-style: dashed; }
.ovc-cond-txt { flex: 1; min-width: 0; }
.ovc-sub-ic { width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ovc-todo { color: #D97706; display: inline-flex; flex-shrink: 0; }
.ovc-x { border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; display: inline-flex; padding: 2px; border-radius: 4px; flex-shrink: 0; }
.ovc-x:hover { color: #f53f3f; background: var(--bl-bg-hover); }
.ovc-empty { font-size: 12px; color: var(--bl-text-3); padding: 4px 2px; }
.ovc-add { font-size: 12px; color: var(--bl-text-3); display: flex; align-items: center; gap: 6px; }
.ovc-add a { color: var(--bl-primary); cursor: pointer; }
.ovc-add a:hover { text-decoration: underline; }
.ovc-or { color: var(--bl-text-4); }
</style>
