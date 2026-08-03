<template>
  <div class="fpk" :class="{ 'is-compact': compact }">
    <template v-if="file">
      <span class="fpk-name bl-truncate" :title="file.name">{{ file.name }}</span>
      <span v-if="file.data" class="fpk-size">{{ formatSize(file.size) }}</span>
      <span v-else class="fpk-stale" title="保存时不保留文件内容，需要重新选择">需重选</span>
      <button class="fpk-btn" @click="pick">更换</button>
      <button class="fpk-x" title="移除" @click="$emit('clear')" v-html="BL.icon('x', 11)"></button>
    </template>
    <button v-else class="fpk-btn fpk-main" @click="pick">
      <span v-html="BL.icon('upload', 12)"></span>选择文件
    </button>
    <input ref="inp" type="file" class="fpk-inp" @change="onChange" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { BL } from '@/lib/bl.js'
import { readFile, formatSize } from './apiModel.js'

defineProps({
  file: { type: Object, default: null },
  compact: { type: Boolean, default: false },
})
const emit = defineEmits(['pick', 'clear'])

const inp = ref(null)
function pick() { inp.value?.click() }
async function onChange(e) {
  const f = e.target.files?.[0]
  e.target.value = ''            // 同名文件重选也要触发 change
  if (!f) return
  try { emit('pick', await readFile(f)) } catch (err) { BL.error(err.message) }
}
</script>

<style scoped>
.fpk { display: flex; align-items: center; gap: 6px; min-width: 0; }
.fpk-inp { display: none; }
.fpk-name { font-size: 12px; color: #ddd; max-width: 190px; }
.fpk-size { font-size: 11px; color: #7a7a7a; white-space: nowrap; }
.fpk-stale { font-size: 10.5px; color: #f0a020; background: rgba(240,160,32,.14); border-radius: 3px; padding: 1px 5px; white-space: nowrap; }
.fpk-btn { display: inline-flex; align-items: center; gap: 5px; height: 24px; padding: 0 9px; background: transparent;
  border: 1px solid #3d3d3d; border-radius: 4px; color: #bbb; font-size: 11.5px; cursor: pointer; white-space: nowrap; }
.fpk-btn:hover { border-color: #3b82f6; color: #3b82f6; }
.fpk-main { border-style: dashed; }
.fpk:not(.is-compact) .fpk-main { height: 34px; padding: 0 16px; font-size: 12.5px; }
.fpk-x { border: 0; background: transparent; color: #7a7a7a; cursor: pointer; display: inline-flex; padding: 3px; border-radius: 3px; }
.fpk-x:hover { color: #f87171; background: #3a3a3a; }
</style>
