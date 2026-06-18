<template>
  <div class="app-shell">
    <AppTopBar />
    <div class="app-body">
      <AppSidebar />
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
import AppTopBar from '@/components/AppTopBar.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import SettingsModal from '@/components/SettingsModal.vue'
import DomainSelectorModal from '@/components/DomainSelectorModal.vue'
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
