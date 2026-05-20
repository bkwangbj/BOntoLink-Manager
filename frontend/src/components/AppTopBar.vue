<template>
  <header class="topbar">
    <!-- 左：品牌 -->
    <div class="topbar-l">
      <a class="brand" href="#/" title="BOntoLink · 博智联动态本体管理系统">
        <img src="/brand-logo.svg" alt="BOntoLink 博智联" class="brand-img" />
        <!-- <span class="brand-sep">·</span>
        <span class="brand-suffix">动态本体管理系统</span> -->
      </a>
    </div>

    <!-- 中：搜索，居中 -->
    <div class="topbar-c">
      <div class="search">
        <span class="search-icon" v-html="BL.icon('search', 14)"></span>
        <input class="bl-input search-input" placeholder="全局搜索（对象、关系、动作、数据源…）" @keydown.esc="$event.target.blur()" />
        <span class="search-kbd">⌘ K</span>
      </div>
    </div>

    <!-- 右：通知 / 主题快切 / 全屏 -->
    <div class="topbar-r">
      <button class="bl-btn bl-btn-text bl-btn-icon" title="通知" v-html="BL.icon('bell', 16)"></button>
      <button class="bl-btn bl-btn-text bl-btn-icon" :title="isDark ? '切换到浅色' : '切换到深色'" @click="toggleDark" v-html="BL.icon(isDark ? 'sun' : 'moon', 16)"></button>
      <button class="bl-btn bl-btn-text bl-btn-icon" :title="fullscreen ? '退出全屏' : '进入全屏'" @click="toggleFullscreen" v-html="BL.icon(fullscreen ? 'minimize' : 'maximize', 18)"></button>
    </div>
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

function toggleDark() {
  app.setTheme(isDark.value ? 'light' : 'dark')
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

function onFsChange() { fullscreen.value = !!document.fullscreenElement }

function onKeydown(e) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    document.querySelector('.search-input')?.focus()
  } else if (e.key === '?' && document.activeElement?.tagName !== 'INPUT') {
    BL.info('快捷键：⌘K 搜索 · Esc 关闭弹层 · G+字母 跳模块 · Ctrl+S 保存')
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
  background: var(--bl-bg-1);
  border-bottom: 1px solid var(--bl-border);
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 var(--bl-sp-4);
  position: sticky; top: 0; z-index: 50;
  flex-shrink: 0;
  gap: var(--bl-sp-4);
}
.topbar-l { display: flex; align-items: center; }
.topbar-c { display: flex; justify-content: center; }
.topbar-r { display: flex; align-items: center; justify-content: flex-end; gap: 4px; }

.brand { display: inline-flex; align-items: center; gap: 4px; text-decoration: none; }
.brand-img { height: 36px; width: auto; display: block; }
.brand-sep { color: var(--bl-text-4); font-size: var(--bl-fs-24); line-height: 1; }
.brand-suffix {
  font-size: var(--bl-fs-20);
  font-weight: 600;
  color: var(--bl-text-1);
  letter-spacing: .5px;
  white-space: nowrap;
}

.search { position: relative; width: 480px; max-width: 100%; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 32px; padding-right: 60px; background: var(--bl-bg-2); border-color: transparent; }
.search-input:focus { background: var(--bl-bg-1); }
.search-kbd {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: var(--bl-fs-11); color: var(--bl-text-3);
  background: var(--bl-bg-1); padding: 2px 6px; border-radius: 4px;
  border: 1px solid var(--bl-border);
}

/* 小屏自动让搜索框缩 */
@media (max-width: 1100px) {
  .search { width: 360px; }
}
@media (max-width: 1100px) {
  .brand-suffix { display: none; }
  .brand-sep { display: none; }
}
@media (max-width: 880px) {
  .search { width: 240px; }
  .brand-img { height: 28px; }
}
</style>
