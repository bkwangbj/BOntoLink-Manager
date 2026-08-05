<template>
  <div class="idep-outline">
    <div class="idep-bar">
      <div class="idep-search">
        <span class="idep-search-ic" v-html="BL.icon('search', 11)"></span>
        <input class="idep-input" v-model="q" placeholder="筛选符号..." />
      </div>
    </div>
    <div v-if="!path" class="idep-empty">未打开文件</div>
    <div v-else-if="!filtered.length" class="idep-empty">
      {{ q ? '无匹配符号' : '未解析到符号(类 / 接口 / 函数 / 常量)' }}
    </div>
    <div v-else>
      <div v-for="s in filtered" :key="s.line + s.name"
           class="idep-row" :style="{ paddingLeft: (8 + s.depth * 12) + 'px' }"
           :title="`第 ${s.line} 行`" @click="$emit('goto', s.line)">
        <span class="idep-row-ic" :style="{ background: KIND[s.kind].color }"
              v-html="BL.icon(KIND[s.kind].icon, 10, '#fff')"></span>
        <span class="idep-row-name">{{ s.name }}</span>
        <span class="idep-row-sub">{{ s.line }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 文件结构大纲 (文档「文件结构大纲 Structure」)
 * 解析当前文件的语法节点,结构化展示 类 / 接口 / 函数 / 方法 / 常量,点击跳转对应行。
 *
 * 用轻量正则而不是接语言服务:大纲只需要顶层符号,正则在 TS 与 Python 上都够用,
 * 且不依赖 worker 就绪时机;真正的语义解析留给编辑器本身的智能提示。
 */
import { computed, ref } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  path: { type: String, default: '' },
  content: { type: String, default: '' }
})
defineEmits(['goto'])

const KIND = {
  class: { icon: 'box', color: '#c586c0' },
  interface: { icon: 'layers', color: '#4ec9b0' },
  func: { icon: 'code', color: '#dcdcaa' },
  method: { icon: 'code', color: '#dcdcaa' },
  const: { icon: 'tag', color: '#9cdcfe' },
}

const q = ref('')
const isPy = computed(() => String(props.path).toLowerCase().endsWith('.py'))

const symbols = computed(() => {
  const lines = String(props.content || '').split('\n')
  const out = []
  lines.forEach((raw, i) => {
    const line = raw.replace(/\s+$/, '')
    if (!line.trim() || line.trim().startsWith('//') || line.trim().startsWith('#')) return
    const indent = line.length - line.trimStart().length
    const depth = Math.min(2, Math.floor(indent / (isPy.value ? 4 : 2)))
    const t = line.trim()
    let m
    if (isPy.value) {
      if ((m = t.match(/^class\s+([A-Za-z_]\w*)/))) out.push({ kind: 'class', name: m[1], line: i + 1, depth })
      else if ((m = t.match(/^(?:async\s+)?def\s+([A-Za-z_]\w*)/))) out.push({ kind: depth > 0 ? 'method' : 'func', name: m[1] + '()', line: i + 1, depth })
    } else {
      if ((m = t.match(/^(?:export\s+)?(?:abstract\s+)?class\s+([A-Za-z_$][\w$]*)/))) out.push({ kind: 'class', name: m[1], line: i + 1, depth })
      else if ((m = t.match(/^(?:export\s+)?interface\s+([A-Za-z_$][\w$]*)/))) out.push({ kind: 'interface', name: m[1], line: i + 1, depth })
      else if ((m = t.match(/^(?:export\s+)?type\s+([A-Za-z_$][\w$]*)/))) out.push({ kind: 'interface', name: m[1], line: i + 1, depth })
      else if ((m = t.match(/^(?:export\s+)?(?:async\s+)?function\s+([A-Za-z_$][\w$]*)/))) out.push({ kind: 'func', name: m[1] + '()', line: i + 1, depth })
      // 类成员方法:public/private/protected/static 前缀, 或直接 name(...) {
      else if ((m = t.match(/^(?:public|private|protected|static|readonly|\s)*([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*(?::[^{]+)?\{/))) {
        if (!['if', 'for', 'while', 'switch', 'catch', 'return'].includes(m[1])) {
          out.push({ kind: depth > 0 ? 'method' : 'func', name: m[1] + '()', line: i + 1, depth })
        }
      }
      else if ((m = t.match(/^(?:export\s+)?const\s+([A-Za-z_$][\w$]*)/))) out.push({ kind: 'const', name: m[1], line: i + 1, depth })
    }
  })
  return out
})

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  return k ? symbols.value.filter(s => s.name.toLowerCase().includes(k)) : symbols.value
})
</script>

<style scoped>
.idep-outline { display: flex; flex-direction: column; }
</style>
