<template>
  <div class="tn-wrap">
    <div :class="['tn', selected?.id===node.id && 'is-active']"
         @click="$emit('select', node)"
         @contextmenu.prevent="onContext($event)">
      <span class="tn-toggle" @click.stop="$emit('toggle', node)" v-if="hasChildren">
        <span v-html="BL.icon(isOpen ? 'chevronDown' : 'chevronRight', 12)"></span>
      </span>
      <span v-else class="tn-toggle tn-toggle-empty"></span>
      <span class="tn-ico" :style="{ background: prof.color }" v-html="BL.icon(prof.icon, 13, '#fff')"></span>
      <span class="tn-label bl-truncate" :title="node.label">
        <span v-html="highlight(node.label, search)"></span>
      </span>
      <span class="tn-count" :title="`${objCount} 个对象`">{{ objCount }}</span>
    </div>
    <div v-if="hasChildren && isOpen" class="tn-children">
      <TreeNode
        v-for="c in node.children" :key="c.id"
        :node="c"
        :selected="selected"
        :search="search"
        :expanded="expanded"
        :stats-map="statsMap"
        @select="$emit('select', $event)"
        @toggle="$emit('toggle', $event)"
        @ctx="(n,a)=>$emit('ctx', n, a)" />
    </div>

    <!-- 右键菜单 -->
    <transition name="fade">
      <div v-if="ctxOpen" class="bl-popover" :style="ctxStyle" @click.stop>
        <div class="bl-menu-item" @click="emit('ctx', node, 'addChild'); ctxOpen=false">
          <span v-html="BL.icon('plus', 12)"></span> 新建下级
        </div>
        <div class="bl-menu-item" @click="emit('ctx', node, 'edit'); ctxOpen=false">
          <span v-html="BL.icon('edit', 12)"></span> 编辑
        </div>
        <div class="bl-menu-divider"></div>
        <div class="bl-menu-item is-danger" @click="emit('ctx', node, 'delete'); ctxOpen=false">
          <span v-html="BL.icon('trash', 12)"></span> 删除
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { BL } from '@/lib/bl.js'
import { nodeProfile } from '@/lib/domain.js'

const props = defineProps({
  node: Object, selected: Object,
  search: String,
  expanded: Set,
  statsMap: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['select', 'toggle', 'ctx'])

const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)
const isOpen = computed(() => props.expanded?.has(props.node.id))
const prof = computed(() => nodeProfile(props.node))
const objCount = computed(() => props.statsMap?.[props.node.id]?.classCount ?? 0)

function typeText(t) { return t === 1 ? '行业' : t === 2 ? '领域' : t === 3 ? '分组' : '' }

const ctxOpen = ref(false)
const ctxStyle = ref({})
function onContext(e) {
  emit('select', props.node)
  ctxStyle.value = { top: e.clientY + 'px', left: e.clientX + 'px', position: 'fixed' }
  ctxOpen.value = true
  document.addEventListener('click', () => ctxOpen.value = false, { once: true })
}

function highlight(text, q) {
  if (!q) return escape(text || '')
  const t = escape(text || '')
  const r = new RegExp(`(${escape(q)})`, 'ig')
  return t.replace(r, '<mark style="background:#fff7c2;color:inherit;padding:0 2px;border-radius:2px">$1</mark>')
}
function escape(s) { return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])) }
</script>

<style scoped>
.tn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 6px 6px 8px;
  margin: 1px 0;
  border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-13);
  cursor: pointer;
  user-select: none;
  position: relative;
  z-index: 1;
}
.tn:hover { background: var(--bl-bg-hover); }
.tn.is-active {
  background: var(--bl-primary-soft);
  color: var(--bl-primary);
  font-weight: 500;
}
.tn-toggle {
  width: 16px; height: 16px;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-1); flex-shrink: 0;
  position: relative;
  z-index: 2;
}
.tn-toggle:hover { color: var(--bl-primary); }
/* 有箭头的 toggle 加上节点行背景色，并通过 outline 向外扩展 3px，让竖虚线在 toggle 前完全被遮住 */
.tn-toggle:not(.tn-toggle-empty) {
  background: var(--bl-bg-1);
  border-radius: 3px;
  outline: 3px solid var(--bl-bg-1);
}
.tn:hover .tn-toggle:not(.tn-toggle-empty) {
  background: var(--bl-bg-hover);
  outline-color: var(--bl-bg-hover);
}
.tn.is-active .tn-toggle:not(.tn-toggle-empty) {
  background: var(--bl-primary-soft-ss);
  outline-color: var(--bl-primary-soft-ss);
}
.tn-toggle-empty { width: 16px; height: 16px; background: transparent; }
.tn-ico {
  width: 20px; height: 20px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  margin-right: 4px;
}
.tn-type {
  font-size: var(--bl-fs-11);
  color: var(--bl-text-3);
  margin-left: auto;
  padding-left: 8px;
  flex-shrink: 0;
}
.tn-label { flex: 1; min-width: 0; display: flex; align-items: center; gap: 6px; }
.tn-count {
  flex-shrink: 0; margin-left: auto;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2);
  border-radius: 9px; padding: 0 7px; min-width: 20px;
  height: 17px; line-height: 17px; text-align: center;
  font-feature-settings: "tnum";
}
.tn.is-active .tn-count { background: var(--bl-primary-soft-ss, var(--bl-bg-1)); color: var(--bl-primary); }
.tn-tag {
  font-family: var(--bl-font-mono);
  font-size: var(--bl-fs-11);
  color: var(--bl-text-3);
  background: var(--bl-bg-2);
  padding: 1px 6px; border-radius: 3px;
}
.tn-wrap { position: relative; }
/*
  树缩进与引线对齐：
  父 .tn 的 chevron 中心 = .tn padding-left(8) + toggle width/2(8) = 16px from .tn 左
  所以 .tn-children 的竖虚线起点必须落在 .tn-wrap 左 16px 处（margin-left:16）
  水平虚线再把子节点连进来（padding-left:12 给水平线留 12px 空间）
*/
/*
  树引线对齐：
  父 .tn 的 图标方块左缘 = padding-left(8) + toggle(16) + gap(6) = 30px from .tn 左
  把竖虚线对齐到父图标方块左缘 → margin-left: 30
  子 .tn 内同样布局，图标块左缘在 子.tn 左 30
  水平虚线从竖虚线（子.tn 左 0）延伸到子图标块左缘前 4px = width: 26
*/
.tn-children {
  margin-left: 20px;
  padding-left: 0;
  position: relative;
}
.tn-children::before {
  content: '';
  position: absolute;
  left: 14px; top: -2px; bottom: 18px;
  border-left: 1px dashed var(--bl-border-strong);
}
.tn-children > .tn-wrap > .tn::before {
  content: '';
  position: absolute;
  left: 14px; top: 50%;
  width: 14px;
  border-top: 1px dashed var(--bl-border-strong);
}

.fade-enter-active, .fade-leave-active { transition: opacity .12s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
