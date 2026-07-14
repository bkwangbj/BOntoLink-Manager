<template>
  <div class="app-shell">
    <!-- 全屏页面(meta.fullscreen,如实例探索):隐藏全局头部与侧栏,主视区最大化 -->
    <AppTopBar v-if="!chromeless" />
    <div class="app-body">
      <AppSidebar v-if="!chromeless" />
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
.app-shell {
  display: flex; flex-direction: column;
  width: 100%; height: 100vh; overflow: hidden;
}
.app-body {
  flex: 1; display: flex; overflow: hidden;
}
.app-main {
  flex: 1; overflow: auto;
  background: var(--bl-bg-0);
}
</style>
