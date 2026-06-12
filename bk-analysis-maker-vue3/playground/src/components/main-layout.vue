<template>
  <ElContainer class="layout-container">
    <el-header>
      <div
        class="top-area"
      >
        水库定制化
      </div>
    </el-header>
    <ElContainer style="overflow: hidden;">
      <el-aside
        v-if="false"
        width="280px"
      >
        <div class="nav-wrapper">
          <div
            v-for="c in components"
            :key="c"
            class="component-item"
            :class="{active:current===c}"
            @click="navigateTo(c)"
          >
            {{ c }}
          </div>
        </div>
      </el-aside>
      <el-main><router-view /></el-main>
    </ElContainer>
  </ElContainer>
</template>

<script setup>
import { useComponentsStore } from '@/store'
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const { components } = useComponentsStore()

defineExpose({
  test
})

const current = ref(route.name)

watch(route, (v) => {
  current.value = v.name
})

function navigateTo (name) {
  if (route.name === name) return
  router.push({ name })
}
const instance = getCurrentInstance()
function test (id) {
  const com = instance.proxy.$stage.get(id)
  return com
}
</script>

<style lang="scss" scoped>
.layout-container {
  width: 100%;
  height: 100%;
  padding: 0 20px;

  .top-area {
    font-size: 24px;
    line-height: 60px;
    color: #006bde;
    text-align: center;
    border-bottom: 1px solid #dcdfe6;
  }

  .nav-wrapper {
    box-sizing: border-box;
    height: 100%;
    padding: 20px;
    overflow: auto;
    border-right: 1px solid #dcdfe6;

    .component-item {
      height: 40px;
      font-size: 14px;
      line-height: 40px;
      color: #444;
      cursor: pointer;
      transition: color 0.2s;

      &:hover,
      &.active {
        color: #409eff;
      }
    }
  }
}
</style>
