<template>
  <Teleport to="body">
    <transition name="idekb-fade">
      <div v-if="open" class="fnide idekb-mask" :data-ide-theme="theme" @click.self="close">
        <div class="idekb">
          <div class="idekb-hd">
            <span class="idekb-title">键盘快捷键</span>
            <span class="bl-grow"></span>
            <div class="idekb-search">
              <span class="idekb-search-ic" v-html="BL.icon('search', 11)"></span>
              <input class="idekb-input" v-model="q" placeholder="搜索命令或按键" />
            </div>
            <button class="idekb-x" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <div v-if="conflictCount" class="idekb-warn">
            <span v-html="BL.icon('warning', 12)"></span>
            检测到 {{ conflictCount }} 组快捷键冲突,冲突项已标红 —— 同一组合键绑定到多个命令时,只有第一个会生效
          </div>

          <div class="idekb-body">
            <table class="idekb-table">
              <colgroup><col style="width:200px" /><col style="width:90px" /><col style="width:170px" /><col /></colgroup>
              <thead>
                <tr><th>命令</th><th>分类</th><th>快捷键</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="c in filtered" :key="c.id" :class="isConflict(c.id) && 'is-conflict'">
                  <td>{{ c.title }}</td>
                  <td class="idekb-cat">{{ c.category }}</td>
                  <td>
                    <span v-if="recordingId === c.id" class="idekb-recording">按下组合键…(Esc 取消)</span>
                    <span v-else-if="bindings[c.id]" class="ide-keys">
                      <template v-for="(k, ki) in keyLabel(bindings[c.id])" :key="ki">
                        <span v-if="ki" class="ide-key-plus">+</span><span class="ide-key">{{ k }}</span>
                      </template>
                    </span>
                    <span v-else class="idekb-none">未绑定</span>
                  </td>
                  <td>
                    <button class="idekb-btn" @click="startRecord(c.id)">
                      {{ recordingId === c.id ? '录制中' : '修改' }}
                    </button>
                    <button v-if="bindings[c.id]" class="idekb-btn" @click="unbind(c.id)">解绑</button>
                    <button v-if="overrides[c.id] !== undefined" class="idekb-btn" @click="reset(c.id)">还原默认</button>
                  </td>
                </tr>
                <tr v-if="!filtered.length"><td colspan="4" class="idekb-empty">无匹配命令</td></tr>
              </tbody>
            </table>
          </div>

          <div class="idekb-ft">
            <button class="idekb-btn" @click="resetAll">全部还原默认</button>
            <span class="bl-grow"></span>
            <span class="idekb-tip">改动即时生效并保存到本地</span>
            <button class="idekb-btn is-primary" @click="close">完成</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 快捷键自定义 (P9 · 文档 模块9-2「支持自定义快捷键配置,冲突检测」)
 * 录制新组合键 → 即时保存到 localStorage → 冲突项标红提示。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { keyLabel, keyOfEvent, findConflicts } from './ideKeys.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  commands: { type: Array, default: () => [] },
  bindings: { type: Object, default: () => ({}) },
  overrides: { type: Object, default: () => ({}) },
  theme: { type: String, default: 'dark' },
})
const emit = defineEmits(['update:open', 'set-binding', 'reset-all'])

const q = ref('')
const recordingId = ref('')

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return props.commands
  return props.commands.filter(c =>
    c.title.toLowerCase().includes(k) ||
    c.category.toLowerCase().includes(k) ||
    String(props.bindings[c.id] || '').includes(k))
})

const conflicts = computed(() => findConflicts(props.bindings))
const conflictCount = computed(() => Object.keys(conflicts.value).length)
function isConflict(id) {
  return Object.values(conflicts.value).some(ids => ids.includes(id))
}

function startRecord(id) { recordingId.value = recordingId.value === id ? '' : id }
function unbind(id) { emit('set-binding', { id, binding: '' }) }
function reset(id) { emit('set-binding', { id, binding: undefined }) }
function resetAll() { emit('reset-all') }
function close() { recordingId.value = ''; emit('update:open', false) }

/** 录制期间吞掉所有按键,避免触发 IDE 自身的快捷键 */
function onKeyDown(e) {
  if (!props.open || !recordingId.value) return
  e.preventDefault()
  e.stopPropagation()
  if (e.key === 'Escape') { recordingId.value = ''; return }
  const k = keyOfEvent(e)
  if (!k) return
  emit('set-binding', { id: recordingId.value, binding: k })
  recordingId.value = ''
}
onMounted(() => window.addEventListener('keydown', onKeyDown, true))
onUnmounted(() => window.removeEventListener('keydown', onKeyDown, true))
</script>

<style scoped>
.idekb-mask {
  position: fixed; inset: 0; z-index: 1500;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
}
.idekb {
  width: 860px; max-width: calc(100vw - 40px); height: 74vh;
  background: var(--ide-bg-2); color: var(--ide-text);
  border: 1px solid var(--ide-border); border-radius: 6px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, .5);
  display: flex; flex-direction: column; overflow: hidden;
}
.idekb-hd {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-bottom: 1px solid var(--ide-border);
}
.idekb-title { font-size: 14px; font-weight: 600; color: var(--ide-text-strong); }
.idekb-search { position: relative; width: 220px; }
.idekb-search-ic { position: absolute; left: 6px; top: 50%; transform: translateY(-50%); color: var(--ide-text-dim); }
.idekb-input {
  width: 100%; height: 26px; padding: 0 6px 0 22px; border-radius: 3px;
  background: var(--ide-bg); border: 1px solid var(--ide-border);
  color: var(--ide-text); font-size: 12px; outline: none;
}
.idekb-x {
  width: 24px; height: 24px; border: 0; border-radius: 3px; cursor: pointer;
  background: transparent; color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center;
}
.idekb-x:hover { background: var(--ide-hover); color: var(--ide-text-strong); }

.idekb-warn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; font-size: 12px;
  background: rgba(226, 192, 141, .12); color: #e2c08d;
  border-bottom: 1px solid var(--ide-border);
}
.idekb-body { flex: 1; min-height: 0; overflow: auto; }
.idekb-table { width: 100%; border-collapse: collapse; }
.idekb-table thead th {
  position: sticky; top: 0; z-index: 1;
  background: var(--ide-bg-3); text-align: left;
  padding: 7px 12px; font-size: 11.5px; font-weight: 600; color: var(--ide-text-dim);
}
.idekb-table td { padding: 5px 12px; font-size: 12.5px; border-bottom: 1px solid rgba(128,128,128,.12); }
.idekb-table tr.is-conflict td { background: rgba(241, 76, 76, .10); }
.idekb-cat { color: var(--ide-text-dim); font-size: 11.5px; }
.idekb-none { color: var(--ide-text-dim); font-size: 11.5px; }
.idekb-recording { color: var(--ide-blue); font-size: 11.5px; }
.idekb-empty { text-align: center; padding: 24px; color: var(--ide-text-dim); }
.idekb-btn {
  height: 22px; padding: 0 8px; margin-right: 4px; border-radius: 3px; cursor: pointer;
  background: transparent; border: 1px solid var(--ide-border); color: var(--ide-text); font-size: 11px;
}
.idekb-btn:hover { background: var(--ide-hover); }
.idekb-btn.is-primary { background: var(--ide-blue); border-color: var(--ide-blue); color: #fff; }
.idekb-ft {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-top: 1px solid var(--ide-border);
}
.idekb-tip { font-size: 11.5px; color: var(--ide-text-dim); }
.bl-grow { flex: 1; }
.idekb-fade-enter-active, .idekb-fade-leave-active { transition: opacity .15s; }
.idekb-fade-enter-from, .idekb-fade-leave-to { opacity: 0; }
</style>
