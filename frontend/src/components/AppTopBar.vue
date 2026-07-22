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
      <div class="search" v-click-outside="closeSearch">
        <div class="search-trigger">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input ref="gInput" v-model="gquery" class="bl-input search-input"
                 placeholder="全局搜索（对象、关系、动作、数据源…）"
                 @focus="openSearch" @input="openSearch" @keydown.esc="closeSearch" />
          <button v-if="gquery" class="search-clear" title="清空" @click="clearSearch" v-html="BL.icon('x', 13)"></button>
          <span class="search-kbd">⌘ K</span>
        </div>
        <!-- 全局搜索:搜索框下方燕尾下拉(非弹框),用上方输入框的关键词 -->
        <GlobalSearchModal v-model:open="searchOpen" inline :external-query="gquery" />
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
import GlobalSearchModal from '@/components/GlobalSearchModal.vue'

const app = useAppStore()

const fullscreen = ref(false)
const searchOpen = ref(false)
const gquery = ref('')
const gInput = ref(null)
function openSearch() { searchOpen.value = true }
function closeSearch() { searchOpen.value = false }
function clearSearch() { gquery.value = ''; gInput.value?.focus() }

/* click-outside 局部指令 */
const vClickOutside = {
  mounted(el, binding) { el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }; setTimeout(() => document.addEventListener('mousedown', el.__h), 0) },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}
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
  /* 头部彩色渐变条:跟随主题色(accent)自动变化, 白字 —— 参考 UI demo 头部色系 */
  --hdr-text: #ffffff;
  --hdr-text-dim: rgba(255,255,255,.72);
  --hdr-search-bg: rgba(255,255,255,.16);
  --hdr-search-bg-hover: rgba(255,255,255,.24);
  --hdr-search-border: rgba(255,255,255,.24);
  --hdr-btn-hover: rgba(255,255,255,.16);
  /* 对标 demo:沉稳皇家蓝渐变(左略深→中主色→右略浅), 不提亮不发光, 避免刺眼 */
  background:
    linear-gradient(100deg,
      color-mix(in srgb, var(--bl-primary) 88%, #071b52) 0%,
      var(--bl-primary) 52%,
      color-mix(in srgb, var(--bl-primary) 86%, #ffffff) 100%);
  border-bottom: 1px solid rgba(255,255,255,0);
  box-shadow: 0 1px 4px rgba(20,36,64,.14);
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 var(--bl-sp-4);
  position: sticky; top: 0; z-index: 50;
  flex-shrink: 0;
  gap: var(--bl-sp-4);
  color: var(--hdr-text);
}
/* 深色下: 头部转为深 navy(带一丝主题色), 与主内容拉开层差 */
:root[data-theme="dark"] .topbar {
  --hdr-search-bg: rgba(255,255,255,.05);
  --hdr-search-bg-hover: rgba(255,255,255,.10);
  --hdr-search-border: rgba(255,255,255,.10);
  --hdr-btn-hover: rgba(255,255,255,.08);
  /* 对标 demo:很深的 navy(近黑,仅一丝蓝), 只掺极少 accent 保留色系跟随 */
  background:
    linear-gradient(100deg,
      color-mix(in srgb, var(--bl-primary) 6%, #0a1526) 0%,
      color-mix(in srgb, var(--bl-primary) 10%, #0e1f38) 100%);
  border-bottom-color: rgba(255,255,255,.06);
  box-shadow: 0 2px 12px rgba(0,0,0,0.5);
}
.topbar-l { display: flex; align-items: center; }
.topbar-c { display: flex; justify-content: center; }
.topbar-r { display: flex; align-items: center; justify-content: flex-end; gap: 4px; }

.brand { display: inline-flex; align-items: center; gap: 4px; text-decoration: none; }
/* logo 含蓝色元素, 在彩色头部上会糊 —— 转为纯白剪影, 与 demo 白色字标一致 */
.brand-img { height: 36px; width: auto; display: block; 
  /* filter: brightness(0) invert(1);  */
}
.brand-sep { color: var(--hdr-text-dim); font-size: var(--bl-fs-24); line-height: 1; }
.brand-suffix {
  font-size: var(--bl-fs-20);
  font-weight: 600;
  color: var(--hdr-text);
  letter-spacing: .5px;
  white-space: nowrap;
}

.search { position: relative; width: 480px; max-width: 100%; }
.search-trigger { position: relative; width: 100%; }
.search-trigger .search-input { width: 100%; box-sizing: border-box; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--hdr-text-dim); pointer-events: none; }
.search-input {
  padding-left: 32px; padding-right: 84px;
  /* 半透明白搜索框, 悬浮于彩色头部之上 */
  background: var(--hdr-search-bg);
  border: 1px solid var(--hdr-search-border);
  color: var(--hdr-text);
  transition: background-color .15s, border-color .15s;
}
.search-input::placeholder { color: var(--hdr-text-dim); }
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
  background: var(--hdr-search-bg-hover);
  border-color: rgba(255,255,255,.34);
}
.search-input:focus {
  background: var(--hdr-search-bg-hover);
  border-color: rgba(255,255,255,.5);
  box-shadow: 0 0 0 2px rgba(255,255,255,.14);
}
.search-clear {
  position: absolute; right: 44px; top: 50%; transform: translateY(-50%);
  width: 18px; height: 18px; border: 0; padding: 0; border-radius: 50%;
  background: rgba(255,255,255,.24); color: #fff;
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.search-clear:hover { background: rgba(255,255,255,.4); color: #fff; }
.search-kbd {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: var(--bl-fs-11); color: var(--hdr-text-dim);
  background: rgba(255,255,255,.14);
  padding: 2px 6px; border-radius: 4px;
  border: 1px solid rgba(255,255,255,.2);
}
/* 右侧图标按钮: 白色, hover 半透明白底 */
.topbar-r :deep(.bl-btn) { color: var(--hdr-text); }
.topbar-r :deep(.bl-btn:hover) { background: var(--hdr-btn-hover); color: #fff; }

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
