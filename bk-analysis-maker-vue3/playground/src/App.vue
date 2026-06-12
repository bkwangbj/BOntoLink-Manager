<template>
  <ElConfigProvider :locale="zhCn">
    <div id="app">
      <router-view />
    </div>
  </ElConfigProvider>
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
</script>

<style type="scss">
body {
  padding: 0;
  margin: 0;
}

.view-container {
  padding: 0 10px;
}

#app {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.example-section {
  margin: 20px 0;
  font-size: 22px;
  color: #1f2f3d;
}
</style>
