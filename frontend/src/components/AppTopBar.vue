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

    <!-- 中：搜索，居中 — 点击即打开全局搜索弹框 -->
    <div class="topbar-c">
      <div class="search" @click="openSearch" role="button" tabindex="0" @keydown.enter="openSearch">
        <span class="search-icon" v-html="BL.icon('search', 14)"></span>
        <div class="bl-input search-input search-input-fake">
          <span class="search-placeholder">全局搜索（对象、关系、动作、数据源…）</span>
        </div>
        <span class="search-kbd">⌘ K</span>
      </div>
    </div>

    <!-- 全局搜索弹框 -->
    <GlobalSearchModal v-model:open="searchOpen" />

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
import GlobalSearchModal from '@/components/GlobalSearchModal.vue'

const app = useAppStore()

const fullscreen = ref(false)
const searchOpen = ref(false)
function openSearch() { searchOpen.value = true }
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
    searchOpen.value = true
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
  /* 主题色渐变背景 — 上→下 15%→3% 衰减, 头部与内容区分层更明显 */
  background:
    linear-gradient(180deg,
      color-mix(in srgb, var(--bl-primary) 15%, var(--bl-bg-1)) 0%,
      color-mix(in srgb, var(--bl-primary) 8%,  var(--bl-bg-1)) 60%,
      color-mix(in srgb, var(--bl-primary) 3%,  var(--bl-bg-1)) 100%);
  border-bottom: 1px solid color-mix(in srgb, var(--bl-primary) 25%, var(--bl-border));
  box-shadow: 0 2px 6px color-mix(in srgb, var(--bl-primary) 12%, transparent);
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 var(--bl-sp-4);
  position: sticky; top: 0; z-index: 50;
  flex-shrink: 0;
  gap: var(--bl-sp-4);
}
/* 深色下: 用 bg-2 为基底 + 更高的 primary 混色比, 让顶栏与主内容 (bg-0) 拉开明显层差 */
:root[data-theme="dark"] .topbar {
  background:
    linear-gradient(180deg,
      color-mix(in srgb, var(--bl-primary) 24%, var(--bl-bg-2)) 0%,
      color-mix(in srgb, var(--bl-primary) 16%, var(--bl-bg-2)) 60%,
      color-mix(in srgb, var(--bl-primary) 10%, var(--bl-bg-2)) 100%);
  border-bottom-color: color-mix(in srgb, var(--bl-primary) 45%, var(--bl-border-strong));
  box-shadow:
    0 2px 8px rgba(0,0,0,0.45),
    0 4px 16px color-mix(in srgb, var(--bl-primary) 18%, transparent);
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

.search { position: relative; width: 480px; max-width: 100%; cursor: pointer; outline: none; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); pointer-events: none; }
.search-input {
  padding-left: 32px; padding-right: 60px;
  /* 搜索框背景跟随头部主题色, 微透白让 placeholder 仍清晰 */
  background: color-mix(in srgb, var(--bl-primary) 5%, var(--bl-bg-1));
  border: 1px solid color-mix(in srgb, var(--bl-primary) 20%, var(--bl-border));
  transition: background-color .15s, border-color .15s;
}
/* 仿 input 容器: 点击打开搜索弹框, 不接受键盘输入 */
.search-input-fake {
  display: flex; align-items: center;
  cursor: pointer;
}
.search-placeholder {
  color: var(--bl-text-3);
  font-size: var(--bl-fs-13);
  user-select: none;
}
.search-input:hover {
  background: color-mix(in srgb, var(--bl-primary) 8%, var(--bl-bg-1));
  border-color: color-mix(in srgb, var(--bl-primary) 35%, var(--bl-border));
}
.search-input:focus {
  background: var(--bl-bg-1);
  border-color: var(--bl-primary);
}
/* 深色下: 搜索框与顶栏渐变中段同基底, 视觉融合, focus 时凹入感更明显 */
:root[data-theme="dark"] .search-input {
  background: color-mix(in srgb, var(--bl-primary) 10%, var(--bl-bg-2));
  border-color: color-mix(in srgb, var(--bl-primary) 30%, var(--bl-border-strong));
}
:root[data-theme="dark"] .search-input:hover {
  background: color-mix(in srgb, var(--bl-primary) 16%, var(--bl-bg-2));
  border-color: color-mix(in srgb, var(--bl-primary) 50%, var(--bl-border-strong));
}
:root[data-theme="dark"] .search-input:focus {
  background: var(--bl-bg-1);
  border-color: var(--bl-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--bl-primary) 25%, transparent);
}
.search-kbd {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: var(--bl-fs-11); color: var(--bl-text-3);
  background: color-mix(in srgb, var(--bl-primary) 3%, var(--bl-bg-1));
  padding: 2px 6px; border-radius: 4px;
  border: 1px solid color-mix(in srgb, var(--bl-primary) 15%, var(--bl-border));
}
:root[data-theme="dark"] .search-kbd {
  background: color-mix(in srgb, var(--bl-primary) 8%, var(--bl-bg-1));
  border-color: color-mix(in srgb, var(--bl-primary) 25%, var(--bl-border-strong));
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
