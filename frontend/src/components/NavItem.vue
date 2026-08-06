<template>
  <router-link :to="item.path" v-slot="{ isActive, navigate }">
    <div :class="['nav-item', isActive && 'is-active']" @click="navigate" :title="collapsed ? `${item.label} · ${item.en}` : item.en">
      <span class="ic" v-html="BL.icon(item.icon, 16)"></span>
      <span class="lbl" v-show="!collapsed">{{ item.label }}</span>
      <span class="en" v-show="!collapsed">{{ item.en }}</span>
    </div>
  </router-link>
</template>

<script setup>
import { BL } from '@/lib/bl.js'
defineProps({ item: Object, collapsed: Boolean })
</script>

<style scoped>
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px;
  margin: 2px 0;
  border-radius: var(--bl-radius-2);
  color: var(--bl-nav-text);
  cursor: pointer; position: relative;
  font-size: var(--bl-fs-14);
  justify-content: center;
}
.nav-item:hover { background: var(--bl-nav-hover-bg); color: var(--bl-nav-active-text); }
.nav-item:hover .en { opacity: 1; }
.nav-item.is-active { background: var(--bl-nav-active-bg); color: var(--bl-nav-active-text); font-weight: 500; }
/* 深色外框里选中态靠"主色染底 + 左侧色条"表达, 换强调色立刻可见 */
.nav-item.is-active::before {
  content: ''; position: absolute; left: 0; top: 6px; bottom: 6px; width: 3px;
  background: var(--bl-nav-active-bar); border-radius: 0 2px 2px 0;
}
.ic { width: 16px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.lbl { flex: 1; min-width: 0; }
.en { font-size: var(--bl-fs-11); color: var(--bl-text-3); opacity: 0; transition: opacity .15s; display: none; }
</style>
