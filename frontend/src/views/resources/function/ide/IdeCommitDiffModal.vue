<template>
  <Teleport to="body">
    <transition name="idecd-fade">
      <div v-if="open" class="fnide idecd-mask" :data-ide-theme="theme" @click.self="close">
        <div class="idecd">
          <div class="idecd-hd">
            <span class="idecd-title">提交对比</span>
            <span class="bl-mono idecd-sha">{{ detail.short || '' }}</span>
            <span class="idecd-msg bl-truncate" :title="detail.message">{{ firstLine }}</span>
            <span class="bl-grow"></span>
            <span class="idecd-meta">{{ detail.author }} · {{ detail.time }}</span>
            <button class="idecd-x" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="idecd-body">
            <aside class="idecd-files">
              <div class="idecd-files-hd">
                变更文件<span class="idecd-n">{{ files.length }}</span>
                <span class="bl-grow"></span>
                <span class="idecd-stat">
                  <b class="is-add">+{{ totalAdd }}</b><b class="is-del">-{{ totalDel }}</b>
                </span>
              </div>
              <div class="idecd-files-body">
                <div v-for="f in files" :key="f.path"
                     :class="['idecd-file', current === f.path && 'is-on']"
                     :title="f.path" @click="pick(f)">
                  <span class="idecd-badge" :class="'is-' + f.change.toLowerCase()">{{ badge(f.change) }}</span>
                  <span class="idecd-file-name bl-truncate">{{ f.path }}</span>
                  <span class="idecd-file-stat">
                    <b v-if="f.additions" class="is-add">+{{ f.additions }}</b>
                    <b v-if="f.deletions" class="is-del">-{{ f.deletions }}</b>
                  </span>
                </div>
                <div v-if="!files.length && !loading" class="idecd-empty">此提交没有文件变更</div>
              </div>
            </aside>

            <section class="idecd-diff">
              <div class="idecd-diff-hd" v-if="current">
                <span v-html="BL.icon('fileCode', 12)"></span>
                <span class="bl-truncate">{{ current }}</span>
                <span class="bl-grow"></span>
                <label class="idecd-toggle">
                  <input type="checkbox" v-model="inline" /> 内联视图
                </label>
              </div>
              <div class="idecd-diff-body">
                <div v-show="!notice" ref="diffEl" class="idecd-editor"></div>
                <div v-if="notice" class="idecd-empty">{{ notice }}</div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 提交对比弹窗
 *
 * 左边是这次提交的变更文件清单(增删行数由服务端按 JGit 的 EditList 算好),
 * 右边用 Monaco 的 diff 编辑器并排显示提交前后的正文。
 *
 * 注意:diff 编辑器实例和它的两个 model 都不能进响应式 —— Monaco 的对象图带循环引用,
 * Vue 深代理会把主线程跑死(见 CLAUDE.md 踩坑表)。这里一律 shallowRef + markRaw + 普通变量。
 */
import { ref, shallowRef, markRaw, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import * as monaco from 'monaco-editor'
import { BL } from '@/lib/bl.js'
import { fnRepoApi } from '@/api'
import { langOfPath } from './ideSettings.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  commit: { type: String, default: '' },
  theme: { type: String, default: 'dark' },
})
const emit = defineEmits(['update:open'])

const detail = ref({})
const files = ref([])
const current = ref('')
const loading = ref(false)
const notice = ref('')
const inline = ref(false)

const diffEl = ref(null)
const diffEditor = shallowRef(null)
let originalModel = null
let modifiedModel = null

const firstLine = computed(() => String(detail.value.message || '').split('\n')[0])
const totalAdd = computed(() => files.value.reduce((s, f) => s + (f.additions || 0), 0))
const totalDel = computed(() => files.value.reduce((s, f) => s + (f.deletions || 0), 0))

function badge(change) {
  return { ADD: '新', MODIFY: '改', DELETE: '删', RENAME: '移', COPY: '拷' }[change] || '改'
}

function monacoTheme() {
  return props.theme === 'light' ? 'vs' : 'vs-dark'
}

function ensureEditor() {
  if (diffEditor.value || !diffEl.value) return
  diffEditor.value = markRaw(monaco.editor.createDiffEditor(diffEl.value, {
    theme: monacoTheme(),
    readOnly: true,
    renderSideBySide: !inline.value,
    automaticLayout: true,
    fontSize: 13,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    renderOverviewRuler: true,
  }))
}

function disposeEditor() {
  diffEditor.value?.dispose()
  diffEditor.value = null
  originalModel?.dispose(); originalModel = null
  modifiedModel?.dispose(); modifiedModel = null
}

async function load() {
  detail.value = {}
  files.value = []
  current.value = ''
  notice.value = ''
  loading.value = true
  try {
    const d = await fnRepoApi.commit(props.commit)
    detail.value = d || {}
    files.value = d?.files || []
    if (files.value.length) {
      await nextTick()
      ensureEditor()
      await pick(files.value[0])
    } else {
      notice.value = '此提交没有文件变更'
    }
  } catch (e) {
    notice.value = `读取提交失败:${e?.message || e}`
  } finally {
    loading.value = false
  }
}

async function pick(f) {
  current.value = f.path
  notice.value = ''
  let r
  try {
    r = await fnRepoApi.commitFile(props.commit, f.path)
  } catch (e) {
    notice.value = `读取对比内容失败:${e?.message || e}`
    return
  }
  if (r.binary) { notice.value = '二进制文件,不支持文本对比'; return }
  if (r.too_large) { notice.value = '文件过大(超过 512 KB),不做在线对比'; return }

  await nextTick()
  ensureEditor()
  const lang = langOfPath(f.path)
  originalModel?.dispose()
  modifiedModel?.dispose()
  originalModel = markRaw(monaco.editor.createModel(r.old_content || '', lang))
  modifiedModel = markRaw(monaco.editor.createModel(r.new_content || '', lang))
  diffEditor.value?.setModel({ original: originalModel, modified: modifiedModel })
}

function close() { emit('update:open', false) }

watch(() => props.open, (v) => {
  if (v && props.commit) load()
  if (!v) disposeEditor()
})
watch(inline, (v) => diffEditor.value?.updateOptions({ renderSideBySide: !v }))
watch(() => props.theme, () => diffEditor.value?.updateOptions({ theme: monacoTheme() }))

onBeforeUnmount(disposeEditor)
</script>

<style scoped>
.idecd-mask {
  position: fixed; inset: 0; z-index: 1500;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
  /* 增删色单列一份:主题 token 里没有红色, 且浅色主题下需要更深的对比 */
  --idecd-add: #3fb950;
  --idecd-del: #f85149;
}
.idecd-mask[data-ide-theme="light"] { --idecd-add: #1a7f37; --idecd-del: #cf222e; }
.idecd {
  width: 1180px; max-width: calc(100vw - 40px); height: 80vh;
  background: var(--ide-bg-2); color: var(--ide-text);
  border: 1px solid var(--ide-border); border-radius: 6px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, .5);
  display: flex; flex-direction: column; overflow: hidden;
}
.idecd-hd {
  display: flex; align-items: center; gap: 10px; flex-shrink: 0;
  padding: 10px 14px; border-bottom: 1px solid var(--ide-border);
}
.idecd-title { font-size: 14px; font-weight: 600; color: var(--ide-text-strong); }
.idecd-sha {
  font-size: 11.5px; color: var(--ide-text-dim);
  padding: 1px 6px; border: 1px solid var(--ide-border); border-radius: 3px;
}
.idecd-msg { font-size: 12.5px; color: var(--ide-text); max-width: 460px; }
.idecd-meta { font-size: 11.5px; color: var(--ide-text-dim); }
.idecd-x {
  width: 24px; height: 24px; border: 0; border-radius: 3px; cursor: pointer;
  background: transparent; color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center;
}
.idecd-x:hover { background: var(--ide-hover); color: var(--ide-text-strong); }

.idecd-body { flex: 1; min-height: 0; display: flex; }
.idecd-files {
  width: 280px; flex-shrink: 0; display: flex; flex-direction: column;
  border-right: 1px solid var(--ide-border);
}
.idecd-files-hd {
  display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  padding: 7px 10px; font-size: 11.5px; color: var(--ide-text-dim);
  border-bottom: 1px solid var(--ide-border); text-transform: none;
}
.idecd-n {
  margin-left: 4px; padding: 0 5px; border-radius: 8px;
  background: var(--ide-hover); color: var(--ide-text); font-size: 10.5px;
}
.idecd-stat b, .idecd-file-stat b { font-size: 11px; font-weight: 600; margin-left: 5px; }
.idecd-stat b.is-add, .idecd-file-stat b.is-add { color: var(--idecd-add); }
.idecd-stat b.is-del, .idecd-file-stat b.is-del { color: var(--idecd-del); }
.idecd-files-body { flex: 1; min-height: 0; overflow: auto; }
.idecd-file {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 10px; font-size: 12px; cursor: pointer; color: var(--ide-text);
  border-left: 2px solid transparent;
}
.idecd-file:hover { background: var(--ide-hover); }
.idecd-file.is-on { background: var(--ide-active); border-left-color: var(--ide-blue); color: var(--ide-text-strong); }
.idecd-file-name { flex: 1; min-width: 0; direction: rtl; text-align: left; }
.idecd-badge {
  flex-shrink: 0; width: 16px; height: 16px; border-radius: 3px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 10px; color: #fff;
}
.idecd-badge.is-add { background: #2ea043; color: #fff; }
.idecd-badge.is-modify { background: #1f6feb; }
.idecd-badge.is-delete { background: #cf222e; color: #fff; }
.idecd-badge.is-rename, .idecd-badge.is-copy { background: #8250df; }

.idecd-diff { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.idecd-diff-hd {
  display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  padding: 7px 12px; font-size: 12px; color: var(--ide-text);
  border-bottom: 1px solid var(--ide-border);
}
.idecd-toggle {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11.5px; color: var(--ide-text-dim); cursor: pointer;
}
.idecd-diff-body { flex: 1; min-height: 0; position: relative; }
.idecd-editor { position: absolute; inset: 0; }
.idecd-empty {
  padding: 20px 14px; font-size: 12px; color: var(--ide-text-dim); text-align: center;
}
.bl-grow { flex: 1; }
.bl-mono { font-family: Consolas, Monaco, monospace; }
.bl-truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.idecd-fade-enter-active, .idecd-fade-leave-active { transition: opacity .15s; }
.idecd-fade-enter-from, .idecd-fade-leave-to { opacity: 0; }
</style>
