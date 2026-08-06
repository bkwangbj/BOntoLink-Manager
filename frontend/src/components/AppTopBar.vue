<template>
  <!-- 极简顶条(24px): 只承载 通知 / 明暗 / 全屏; 品牌与全局搜索已移入侧栏 -->
  <header class="topbar">
    <button class="tb-ic" title="通知" v-html="BL.icon('bell', 18)"></button>
    <button class="tb-ic" :title="isDark ? '切换到浅色' : '切换到深色'" @click="toggleDark"
            v-html="BL.icon(isDark ? 'sun' : 'moon', 18)"></button>
    <button class="tb-ic" :title="fullscreen ? '退出全屏' : '进入全屏'" @click="toggleFullscreen"
            v-html="BL.icon(fullscreen ? 'minimize' : 'maximize', 18)"></button>
  </header>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import { useAppStore } from '@/stores/app.js'

const app = useAppStore()
const fullscreen = ref(false)

const isDark = computed(() => {
  if (app.theme === 'dark') return true
  if (app.theme === 'light') return false
  return window.matchMedia('(prefers-color-scheme: dark)').matches
})
function toggleDark() { app.setTheme(isDark.value ? 'light' : 'dark') }

function toggleFullscreen() {
  if (!document.fullscreenElement) document.documentElement.requestFullscreen?.()
  else document.exitFullscreen?.()
}
function onFsChange() { fullscreen.value = !!document.fullscreenElement }

/* ⌘K 交给侧栏的全局搜索入口, 这里只保留快捷键说明 */
function onKeydown(e) {
  if (e.key === '?' && document.activeElement?.tagName !== 'INPUT') {
    BL.info('快捷键:⌘K 搜索 · Esc 关闭弹层 · G+字母 跳模块 · Ctrl+S 保存')
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('fullscreenchange', onFsChange)
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('fullscreenchange', onFsChange)
})
</script>

<style scoped>
.topbar {
  height: var(--bl-topbar-h);
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: flex-end;
  gap: 4px;
  padding: 0 10px;
  /* 从左到右渐变, 与侧栏在左上角衔接 (浅色风格下两端同色, 即纯浅底) */
  background: linear-gradient(90deg, var(--bl-hdr-from) 0%, var(--bl-hdr-to) 100%);
  color: var(--bl-nav-text);
}
.tb-ic {
  /* 顶条高度保持 24px 不动, 只把图标与触点放大到贴边 */
  width: 26px; height: 22px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 0; border-radius: 6px;
  background: transparent;
  color: var(--bl-nav-text);
  cursor: pointer;
  transition: background-color .15s, color .15s;
}
/* hover 色走 token: 浅色风格是主题色, 深底两版是白 — 不能写死 #fff, 否则浅底上看不见 */
.tb-ic:hover { background: var(--bl-nav-hover-bg); color: var(--bl-nav-active-text); }
.tb-ic:active { background: var(--bl-nav-active-bg); }
</style>
