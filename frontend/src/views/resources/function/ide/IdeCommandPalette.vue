<template>
  <Teleport to="body">
    <transition name="idecp-fade">
      <div v-if="open" class="fnide idecp-mask" :data-ide-theme="theme" @click.self="close">
        <div class="idecp">
          <div class="idecp-input-wrap">
            <span v-if="mode === 'file'" class="idecp-prefix">文件</span>
            <input ref="inputEl" class="idecp-input" v-model="q"
                   :placeholder="mode === 'file' ? '按文件名跳转…' : '输入命令名称,或 > 切换到命令模式'"
                   @keydown.down.prevent="move(1)" @keydown.up.prevent="move(-1)"
                   @keydown.enter.prevent="runActive" @keydown.esc="close" />
          </div>

          <div class="idecp-list" ref="listEl">
            <div v-for="(it, i) in results" :key="it.key"
                 :class="['idecp-item', i === index && 'is-on']"
                 @mouseenter="index = i" @click="runAt(i)">
              <span class="idecp-item-ic" v-html="BL.icon(it.icon, 12)"></span>
              <span class="idecp-item-title">{{ it.title }}</span>
              <span v-if="it.sub" class="idecp-item-sub">{{ it.sub }}</span>
              <span v-if="it.keys?.length" class="ide-keys">
                <template v-for="(k, ki) in it.keys" :key="ki">
                  <span v-if="ki" class="ide-key-plus">+</span><span class="ide-key">{{ k }}</span>
                </template>
              </span>
            </div>
            <div v-if="!results.length" class="idecp-empty">
              {{ mode === 'file' ? '没有匹配的文件' : '没有匹配的命令' }}
            </div>
          </div>

          <div class="idecp-foot">
            <span>↑↓ 选择 · Enter 执行 · Esc 关闭</span>
            <span class="bl-grow"></span>
            <span>{{ results.length }} 项</span>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 命令面板 (P9 · 文档 模块9-1)
 * Ctrl+Shift+P 打开命令模式,Ctrl+P 打开文件跳转模式;输入 `>` 可在面板内切回命令模式。
 * 模糊匹配走 ideCommands.fuzzyScore(连续命中优先,其次字符子序列)。
 */
import { ref, computed, watch, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { fuzzyScore } from './ideCommands.js'
import { keyLabel } from './ideKeys.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  /** 'command' | 'file' */
  mode: { type: String, default: 'command' },
  commands: { type: Array, default: () => [] },
  bindings: { type: Object, default: () => ({}) },
  /** 扁平化的文件路径列表 */
  files: { type: Array, default: () => [] },
  theme: { type: String, default: 'dark' },
})
const emit = defineEmits(['update:open', 'run', 'open-file', 'update:mode'])

const q = ref('')
const index = ref(0)
const inputEl = ref(null)
const listEl = ref(null)

watch(() => props.open, (v) => {
  if (!v) return
  q.value = ''
  index.value = 0
  nextTick(() => inputEl.value?.focus())
})
/* 面板内输入 ">" 直接切到命令模式(与 VS Code 习惯一致) */
watch(q, (v) => {
  if (props.mode === 'file' && v.startsWith('>')) {
    emit('update:mode', 'command')
    q.value = v.slice(1)
  }
  index.value = 0
})

const CATEGORY_ICON = {
  文件: 'file', 编辑: 'edit', 运行: 'play', 调试: 'zap',
  代码仓: 'refresh', 视图: 'layout', 帮助: 'help',
}

const results = computed(() => {
  const key = q.value.trim()
  if (props.mode === 'file') {
    return props.files
      .map(p => ({ p, score: fuzzyScore(p, key) }))
      .filter(x => x.score >= 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, 50)
      .map(x => ({ key: 'f:' + x.p, title: baseName(x.p), sub: x.p, icon: 'fileCode', path: x.p }))
  }
  return props.commands
    .map(c => {
      const s = Math.max(fuzzyScore(c.title, key), fuzzyScore(c.category + ' ' + c.title, key))
      return { c, score: s }
    })
    .filter(x => x.score >= 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 60)
    .map(x => ({
      key: 'c:' + x.c.id, title: x.c.title, sub: x.c.category,
      icon: CATEGORY_ICON[x.c.category] || 'terminal',
      keys: keyLabel(props.bindings[x.c.id]), cmd: x.c,
    }))
})

function move(step) {
  if (!results.value.length) return
  index.value = (index.value + step + results.value.length) % results.value.length
  nextTick(() => {
    const el = listEl.value?.querySelectorAll('.idecp-item')[index.value]
    el?.scrollIntoView({ block: 'nearest' })
  })
}
function runActive() { runAt(index.value) }
function runAt(i) {
  const it = results.value[i]
  if (!it) return
  close()
  if (it.path) emit('open-file', it.path)
  else emit('run', it.cmd)
}
function close() { emit('update:open', false) }
function baseName(p) { return String(p || '').split('/').pop() }
</script>

<style scoped>
.idecp-mask {
  position: fixed; inset: 0; z-index: 1500;
  background: rgba(0, 0, 0, .35);
  display: flex; justify-content: center; align-items: flex-start; padding-top: 12vh;
}
.idecp {
  width: 620px; max-width: calc(100vw - 40px);
  background: var(--ide-menu-bg); color: var(--ide-text);
  border: 1px solid var(--ide-border); border-radius: 6px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, .5);
  display: flex; flex-direction: column; overflow: hidden;
}
.idecp-input-wrap { display: flex; align-items: center; gap: 6px; padding: 8px 10px; border-bottom: 1px solid var(--ide-border); }
.idecp-prefix {
  flex-shrink: 0; padding: 1px 6px; border-radius: 3px;
  background: var(--ide-active); color: var(--ide-text-strong); font-size: 11px;
}
.idecp-input {
  flex: 1; min-width: 0; height: 26px; border: 0; outline: none;
  background: transparent; color: var(--ide-text); font-size: 13px;
}
.idecp-list { max-height: 46vh; overflow: auto; padding: 4px 0; }
.idecp-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 12px; font-size: 12.5px; cursor: pointer; color: var(--ide-text);
}
.idecp-item.is-on { background: var(--ide-active); color: var(--ide-text-strong); }
.idecp-item-ic { flex-shrink: 0; color: var(--ide-text-dim); display: inline-flex; }
.idecp-item.is-on .idecp-item-ic { color: var(--ide-text-strong); }
.idecp-item-title { flex-shrink: 0; }
.idecp-item-sub {
  flex: 1; min-width: 0; color: var(--ide-text-dim); font-size: 11px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.idecp-empty { padding: 20px 12px; color: var(--ide-text-dim); font-size: 12px; text-align: center; }
.idecp-foot {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 12px; border-top: 1px solid var(--ide-border);
  font-size: 11px; color: var(--ide-text-dim);
}
.bl-grow { flex: 1; }
.idecp-fade-enter-active, .idecp-fade-leave-active { transition: opacity .12s; }
.idecp-fade-enter-from, .idecp-fade-leave-to { opacity: 0; }
</style>
