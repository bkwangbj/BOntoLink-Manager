<template>
  <div class="app-shell" :class="{ 'is-chromeless': chromeless }">
    <!-- 全屏页面(meta.fullscreen,如实例探索):隐藏全局头部与侧栏,主视区最大化 -->
    <AppSidebar v-if="!chromeless" />
    <div class="app-col">
      <AppTopBar v-if="!chromeless" />
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>
    <!-- <AiFab /> 已隐藏 -->
    <SettingsModal />
    <DomainSelectorModal />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppTopBar from '@/components/AppTopBar.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import SettingsModal from '@/components/SettingsModal.vue'
import DomainSelectorModal from '@/components/DomainSelectorModal.vue'

const route = useRoute()
const chromeless = computed(() => !!route.meta?.fullscreen)
</script>

<style scoped>
/* 外框: 侧栏贯通全高(纵向渐变) + 右侧列(顶栏横向渐变 + 圆角内容区浮在渐变上) */
.app-shell {
  display: flex;
  width: 100%; height: 100vh; overflow: hidden;
  background: var(--bl-bg-0);
}
/* 右侧列铺与顶条同向的渐变: 圆角缺口处露出的正是顶条的延续, 不会出现突兀的色块 */
.app-col {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column;
  overflow: hidden;
  background: linear-gradient(90deg, var(--bl-hdr-from) 0%, var(--bl-hdr-to) 100%);
}
.app-main {
  flex: 1; min-height: 0; overflow: auto;
  background: var(--bl-main-bg);
  /* 左/上描边勾出内容区边界(浅色下透明, 深色下是主色系细线), 顺着左上圆角走 */
  border-left: 1px solid var(--bl-main-edge);
  border-top: 1px solid var(--bl-main-edge);
  border-top-left-radius: var(--bl-main-radius);
}
/* 全屏页面没有外框, 内容铺满不留圆角与描边 */
.app-shell.is-chromeless .app-main { border: 0; border-radius: 0; }
</style>
