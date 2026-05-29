<template>
  <!-- 8 个不可见缩放热区,挂在 modal 节点最末尾。父节点需 position: relative/fixed -->
  <div class="dh-edge dh-n"  @mousedown="on('n',  $event)"></div>
  <div class="dh-edge dh-s"  @mousedown="on('s',  $event)"></div>
  <div class="dh-edge dh-e"  @mousedown="on('e',  $event)"></div>
  <div class="dh-edge dh-w"  @mousedown="on('w',  $event)"></div>
  <div class="dh-corner dh-ne" @mousedown="on('ne', $event)"></div>
  <div class="dh-corner dh-nw" @mousedown="on('nw', $event)"></div>
  <div class="dh-corner dh-se" @mousedown="on('se', $event)"></div>
  <div class="dh-corner dh-sw" @mousedown="on('sw', $event)"></div>
</template>

<script setup>
defineProps({
  /** (dir, ev) => void — 来自 useDraggableModal 的 startResize */
  on: { type: Function, required: true }
})
</script>

<style scoped>
/* 边缘热区: 宽 6px, 让光标进入即可拖拽 */
.dh-edge, .dh-corner {
  position: absolute; z-index: 5;
  background: transparent;
  user-select: none;
}
.dh-n { top: -3px; left: 14px; right: 14px; height: 6px; cursor: ns-resize; }
.dh-s { bottom: -3px; left: 14px; right: 14px; height: 6px; cursor: ns-resize; }
.dh-e { top: 14px; bottom: 14px; right: -3px; width: 6px; cursor: ew-resize; }
.dh-w { top: 14px; bottom: 14px; left: -3px; width: 6px; cursor: ew-resize; }

/* 四角: 12px 方块, 覆盖在边缘之上 */
.dh-corner { width: 16px; height: 16px; z-index: 6; }
.dh-nw { top: -4px; left: -4px; cursor: nwse-resize; }
.dh-se { bottom: -4px; right: -4px; cursor: nwse-resize; }
.dh-ne { top: -4px; right: -4px; cursor: nesw-resize; }
.dh-sw { bottom: -4px; left: -4px; cursor: nesw-resize; }
</style>
