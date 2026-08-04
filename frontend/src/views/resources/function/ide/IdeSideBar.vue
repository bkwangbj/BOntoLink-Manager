<template>
  <div class="idesb">
    <!-- ===== 活动栏 (8 个功能) ===== -->
    <div class="idesb-act">
      <button v-for="a in ACTIVITIES" :key="a.k"
              :class="['idesb-act-btn', active === a.k && 'is-on']"
              :title="a.label"
              @click="pickActivity(a.k)">
        <span v-if="a.text" class="idesb-act-text">{{ a.text }}</span>
        <span v-else v-html="BL.icon(a.icon, 17)"></span>
        <span v-if="a.k === 'scm' && aheadBadge" class="idesb-badge">{{ aheadBadge }}</span>
      </button>
      <span class="bl-grow"></span>
      <button class="idesb-act-btn" title="收起侧边栏" @click="$emit('collapse')" v-html="BL.icon('chevronLeft', 16)"></button>
    </div>

    <!-- ===== 侧边面板 ===== -->
    <div class="idesb-panel" :style="{ width: width + 'px' }">
      <div class="idesb-hd">{{ activeLabel }}</div>

      <!-- 项目文件树 -->
      <template v-if="active === 'files'">
        <div class="idesb-bar">
          <div class="idesb-search">
            <span class="idesb-search-ic" v-html="BL.icon('search', 11)"></span>
            <input class="idesb-input" v-model="fileFilter" placeholder="筛选文件..." />
          </div>
          <button class="idesb-icon-btn" title="刷新" @click="$emit('reload-tree')" v-html="BL.icon('refresh', 12)"></button>
        </div>
        <div class="idesb-body">
          <IdeTreeNode v-for="n in filteredTree" :key="n.path" :node="n" :depth="0"
                       :active-path="activePath" :expanded="expanded"
                       @open="p => $emit('open-file', p)" @toggle="toggleDir" />
          <div v-if="!filteredTree.length" class="idesb-empty">
            {{ fileFilter ? '无匹配文件' : (repo.ready ? '仓库为空' : '仓库不可用') }}
          </div>
        </div>
      </template>

      <!-- 全局搜索 -->
      <template v-else-if="active === 'search'">
        <div class="idesb-bar">
          <div class="idesb-search">
            <span class="idesb-search-ic" v-html="BL.icon('search', 11)"></span>
            <input class="idesb-input" v-model="keyword" placeholder="在已打开文件中搜索" @keydown.enter="doSearch" />
          </div>
        </div>
        <div class="idesb-body">
          <div v-for="g in searchResults" :key="g.path" class="idesb-sr">
            <div class="idesb-sr-file" @click="$emit('open-file', g.path)">
              <span v-html="BL.icon('fileCode', 11)"></span>{{ g.path }}
              <span class="idesb-sr-n">{{ g.hits.length }}</span>
            </div>
            <div v-for="h in g.hits" :key="h.line" class="idesb-sr-line" @click="$emit('goto', { path: g.path, line: h.line })">
              <span class="idesb-sr-ln">{{ h.line }}</span><span class="bl-truncate">{{ h.text }}</span>
            </div>
          </div>
          <div v-if="keyword && !searchResults.length" class="idesb-empty">无匹配结果</div>
          <div v-if="!keyword" class="idesb-empty">
            输入关键词回车搜索。<br />当前只搜已打开的文件,跨全仓检索在 P6 落地。
          </div>
        </div>
      </template>

      <!-- 版本变更 -->
      <template v-else-if="active === 'scm'">
        <div class="idesb-body">
          <div class="idesb-scm-row"><span>分支</span><b class="bl-mono">{{ repo.current_branch || repo.branch || '—' }}</b></div>
          <div class="idesb-scm-row"><span>HEAD</span><b class="bl-mono">{{ (repo.head || '').slice(0, 8) || '—' }}</b></div>
          <div class="idesb-scm-row"><span>工作区</span><b>{{ repo.dirty ? '有未提交改动' : '干净' }}</b></div>
          <div class="idesb-scm-row"><span>待推送</span><b :class="aheadBadge && 'is-warn'">{{ aheadText }}</b></div>
          <div class="idesb-scm-row"><span>远程</span><b class="bl-mono idesb-remote" :title="repo.remote">{{ repo.remote || '(未配置)' }}</b></div>

          <button class="idesb-push" :disabled="pushing || !repo.ready" @click="doPush">
            <span v-html="BL.icon(pushing ? 'refresh' : 'upload', 12)"></span>
            {{ pushing ? '推送中…' : (aheadBadge ? `推送 ${aheadBadge} 个提交` : '推送(已是最新)') }}
          </button>
          <div class="idesb-tip">保存只在服务端本地提交,推送由这里显式触发。</div>

          <div class="idesb-hd2">提交历史</div>
          <div v-for="c in history" :key="c.commit" class="idesb-commit" :title="c.message">
            <div class="idesb-commit-msg bl-truncate">{{ c.message }}</div>
            <div class="idesb-commit-meta">
              <span class="bl-mono">{{ c.short }}</span> · {{ c.author }} · {{ c.time }}
            </div>
          </div>
          <div v-if="!history.length" class="idesb-empty">暂无提交</div>
        </div>
      </template>

      <!-- 其余面板:P6 落地 -->
      <template v-else>
        <div class="idesb-body">
          <div class="idesb-todo">
            <span v-html="BL.icon('info', 16)"></span>
            <div>「{{ activeLabel }}」面板在 <b>P6</b> 落地</div>
            <div class="idesb-todo-sub">{{ todoHint }}</div>
          </div>
        </div>
      </template>

      <div class="idesb-drag" :class="resizing && 'is-resizing'" @mousedown="onDragStart"></div>
    </div>
  </div>
</template>

<script setup>
/**
 * IDE 活动栏 + 侧边面板 (文档「左侧活动栏」)
 *
 * 8 个功能对应文档的图标表;P5 实做 项目文件树 / 全局搜索 / 版本变更 三个,
 * 其余(文件结构大纲、资源导入、接口|数据源、依赖、分支管理)按文档留位并标注 P6。
 */
import { ref, computed, h, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { fnRepoApi } from '@/api'

const props = defineProps({
  tree: { type: Array, default: () => [] },
  repo: { type: Object, default: () => ({}) },
  activePath: { type: String, default: '' },
  /** 已打开文件 [{ path, content }] —— 全局搜索当前只覆盖这些 */
  openFiles: { type: Array, default: () => [] }
})
const emit = defineEmits(['open-file', 'goto', 'reload-tree', 'collapse', 'pushed'])

const ACTIVITIES = [
  { k: 'files', icon: 'folder', label: '项目文件树' },
  { k: 'search', icon: 'search', label: '全局搜索' },
  { k: 'scm', icon: 'refresh', label: '版本变更面板' },
  { k: 'outline', text: 'ƒ', label: '文件结构大纲' },
  { k: 'imports', icon: 'download', label: '资源导入' },
  { k: 'api', icon: 'globe', label: '接口 | 数据源' },
  { k: 'deps', icon: 'package', label: '依赖' },
  { k: 'branch', icon: 'branch', label: '分支管理' },
]
const TODO_HINT = {
  outline: '解析当前文件语法节点,结构化展示接口 / 函数 / 变量并支持跳转',
  imports: '本体 / 查询函数 / 模型 / 数据源 四类资源导入,导入即生成 TypeScript 类型定义',
  api: '接口与数据源选择器,查看源元数据与 Webhook 接口',
  deps: 'NPM 依赖搜索、安装、移除,同步 package.json',
  branch: '分支新建 / 切换,带数字角标提醒',
}

const active = ref('files')
const activeLabel = computed(() => (ACTIVITIES.find(a => a.k === active.value) || {}).label || '')
const todoHint = computed(() => TODO_HINT[active.value] || '')
function pickActivity(k) {
  active.value = k
  if (k === 'scm') loadHistory()
}

/* —— 待推送角标 —— */
const aheadBadge = computed(() => {
  const n = Number(props.repo.ahead)
  return Number.isFinite(n) && n > 0 ? n : 0
})
const aheadText = computed(() => {
  const n = Number(props.repo.ahead)
  if (n === -1) return '远程尚无该分支'
  return n > 0 ? `${n} 个提交` : '无'
})

/* —— 文件树 —— */
const fileFilter = ref('')
const expanded = ref(new Set())
function toggleDir(path) {
  const s = new Set(expanded.value)
  s.has(path) ? s.delete(path) : s.add(path)
  expanded.value = s
}
/** 关键词过滤:保留命中文件及其祖先目录 */
function filterNodes(nodes, k) {
  const out = []
  for (const n of nodes) {
    if (n.dir) {
      const kids = filterNodes(n.children || [], k)
      if (kids.length || n.name.toLowerCase().includes(k)) out.push({ ...n, children: kids })
    } else if (n.name.toLowerCase().includes(k) || n.path.toLowerCase().includes(k)) {
      out.push(n)
    }
  }
  return out
}
const filteredTree = computed(() => {
  const k = fileFilter.value.trim().toLowerCase()
  return k ? filterNodes(props.tree, k) : props.tree
})
/* 有筛选词时自动展开全部目录, 否则默认展开一级 */
watch([filteredTree, fileFilter], () => {
  const s = new Set(expanded.value)
  const walk = (ns, depth) => ns.forEach(n => {
    if (!n.dir) return
    if (fileFilter.value || depth === 0) s.add(n.path)
    walk(n.children || [], depth + 1)
  })
  walk(filteredTree.value, 0)
  expanded.value = s
}, { immediate: true })

/* —— 全局搜索(当前覆盖已打开文件) —— */
const keyword = ref('')
const searchResults = ref([])
function doSearch() {
  const k = keyword.value.trim().toLowerCase()
  if (!k) { searchResults.value = []; return }
  const out = []
  for (const f of props.openFiles) {
    const hits = []
    String(f.content || '').split('\n').forEach((text, i) => {
      if (text.toLowerCase().includes(k)) hits.push({ line: i + 1, text: text.trim().slice(0, 120) })
    })
    if (hits.length) out.push({ path: f.path, hits: hits.slice(0, 50) })
  }
  searchResults.value = out
}

/* —— 版本变更 —— */
const history = ref([])
const pushing = ref(false)
async function loadHistory() {
  history.value = await fnRepoApi.history(null, 20).catch(() => [])
}
async function doPush() {
  pushing.value = true
  try {
    const r = await fnRepoApi.push()
    if (r?.ok) BL.success(`推送成功:${(r.details || []).join('; ')}`)
    else BL.error(`推送失败:${r?.message || (r?.details || []).join('; ')}`)
    emit('pushed')
    await loadHistory()
  } catch (e) {
    BL.error(`推送失败:${e?.message || e}`)
  } finally {
    pushing.value = false
  }
}
watch(() => props.repo.head, () => { if (active.value === 'scm') loadHistory() })

/* —— 面板宽度拖拽 (文档:最小 220 最大 420) —— */
const MIN_W = 220, MAX_W = 420
const width = ref(Number(localStorage.getItem('bl.ide.sidebar.w')) || 260)
const resizing = ref(false)
let startX = 0, startW = 0
function onDragStart(e) {
  resizing.value = true; startX = e.clientX; startW = width.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  width.value = Math.max(MIN_W, Math.min(MAX_W, startW + (e.clientX - startX)))
}
function onDragEnd() {
  resizing.value = false
  localStorage.setItem('bl.ide.sidebar.w', String(width.value))
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}

defineExpose({ reloadHistory: loadHistory, setActivity: (k) => pickActivity(k) })

/* —— 递归树节点 —— */
const IdeTreeNode = {
  name: 'IdeTreeNode',
  props: ['node', 'depth', 'activePath', 'expanded'],
  emits: ['open', 'toggle'],
  setup(p, { emit: e }) {
    return () => {
      const n = p.node
      const open = p.expanded.has(n.path)
      const row = h('div', {
        class: ['idesb-node', !n.dir && p.activePath === n.path && 'is-on'],
        style: { paddingLeft: (6 + p.depth * 12) + 'px' },
        title: n.path,
        onClick: () => (n.dir ? e('toggle', n.path) : e('open', n.path))
      }, [
        n.dir
          ? h('span', { class: ['idesb-node-chev', open && 'is-open'], innerHTML: BL.icon('chevronRight', 10) })
          : h('span', { class: 'idesb-node-chev' }),
        h('span', { class: 'idesb-node-ic', innerHTML: BL.icon(n.dir ? (open ? 'folderOpen' : 'folder') : 'fileCode', 12) }),
        h('span', { class: 'idesb-node-name' }, n.name)
      ])
      if (!n.dir || !open) return row
      return h('div', [row, ...(n.children || []).map(c => h(IdeTreeNode, {
        key: c.path, node: c, depth: p.depth + 1, activePath: p.activePath, expanded: p.expanded,
        onOpen: (x) => e('open', x), onToggle: (x) => e('toggle', x)
      }))])
    }
  }
}
</script>

<style scoped>
.idesb { display: flex; align-items: stretch; min-height: 0; }
.bl-grow { flex: 1; }

/* 活动栏 */
.idesb-act {
  width: 44px; flex-shrink: 0;
  background: var(--ide-bg-3); border-right: 1px solid var(--ide-border);
  display: flex; flex-direction: column; align-items: center; padding: 4px 0;
}
.idesb-act-btn {
  position: relative; width: 44px; height: 42px;
  border: 0; background: transparent; cursor: pointer;
  color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center;
}
.idesb-act-btn:hover { color: var(--ide-text-strong); }
.idesb-act-btn.is-on { color: var(--ide-text-strong); box-shadow: inset 2px 0 0 var(--ide-blue); }
.idesb-act-text { font-family: Consolas, Monaco, monospace; font-size: 17px; font-style: italic; }
.idesb-badge {
  position: absolute; right: 6px; bottom: 6px; min-width: 15px; height: 15px;
  border-radius: 8px; background: var(--ide-blue); color: #fff;
  font-size: 10px; line-height: 15px; text-align: center; padding: 0 3px;
}

/* 侧边面板 */
.idesb-panel {
  position: relative; flex-shrink: 0;
  background: var(--ide-bg-2); border-right: 1px solid var(--ide-border);
  display: flex; flex-direction: column; min-height: 0;
}
.idesb-hd {
  flex-shrink: 0; padding: 8px 10px 6px;
  font-size: 11px; letter-spacing: .08em; text-transform: uppercase;
  color: var(--ide-text-dim); border-bottom: 1px solid var(--ide-border);
}
.idesb-hd2 {
  margin-top: 10px; padding: 6px 10px 4px;
  font-size: 11px; letter-spacing: .06em; color: var(--ide-text-dim);
  border-top: 1px solid var(--ide-border);
}
.idesb-bar { display: flex; align-items: center; gap: 4px; padding: 6px; }
.idesb-search { position: relative; flex: 1; min-width: 0; }
.idesb-search-ic { position: absolute; left: 6px; top: 50%; transform: translateY(-50%); color: var(--ide-text-dim); }
.idesb-input {
  width: 100%; height: 24px; padding: 0 6px 0 22px;
  background: var(--ide-bg); border: 1px solid var(--ide-border);
  color: var(--ide-text); font-size: 12px; border-radius: 3px; outline: none;
}
.idesb-input:focus { border-color: var(--ide-blue); }
.idesb-icon-btn {
  width: 24px; height: 24px; flex-shrink: 0;
  border: 0; background: transparent; color: var(--ide-text-dim); cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; border-radius: 3px;
}
.idesb-icon-btn:hover { background: var(--ide-hover); color: var(--ide-text-strong); }
.idesb-body { flex: 1; min-height: 0; overflow: auto; padding-bottom: 8px; }
.idesb-empty { padding: 20px 12px; font-size: 11.5px; color: var(--ide-text-dim); line-height: 18px; }

/* 文件树节点(h() 渲染, 样式放非 scoped 块) */
.idesb-todo {
  padding: 24px 14px; color: var(--ide-text-dim); font-size: 12px; text-align: center; line-height: 20px;
}
.idesb-todo b { color: var(--ide-blue); }
.idesb-todo-sub { margin-top: 6px; font-size: 11px; }

/* 搜索结果 */
.idesb-sr { margin-bottom: 6px; }
.idesb-sr-file {
  display: flex; align-items: center; gap: 5px; padding: 4px 10px;
  font-size: 12px; color: var(--ide-text); cursor: pointer;
}
.idesb-sr-file:hover { background: var(--ide-hover); }
.idesb-sr-n { margin-left: auto; font-size: 10.5px; color: var(--ide-text-dim); }
.idesb-sr-line {
  display: flex; gap: 8px; padding: 2px 10px 2px 26px;
  font-family: Consolas, Monaco, monospace; font-size: 11px; color: var(--ide-text-dim); cursor: pointer;
}
.idesb-sr-line:hover { background: var(--ide-hover); color: var(--ide-text); }
.idesb-sr-ln { flex-shrink: 0; min-width: 26px; text-align: right; }

/* 版本变更 */
.idesb-scm-row {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 5px 10px; font-size: 11.5px; color: var(--ide-text-dim);
}
.idesb-scm-row b { color: var(--ide-text); font-weight: 500; min-width: 0; }
.idesb-scm-row b.is-warn { color: #e2c08d; }
.idesb-remote { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; }
.idesb-push {
  display: flex; align-items: center; justify-content: center; gap: 5px;
  margin: 8px 10px 4px; width: calc(100% - 20px); height: 28px;
  border: 0; border-radius: 3px; cursor: pointer;
  background: var(--ide-blue); color: #fff; font-size: 12px;
}
.idesb-push:disabled { opacity: .5; cursor: default; }
.idesb-tip { padding: 0 10px; font-size: 11px; color: var(--ide-text-dim); line-height: 16px; }
.idesb-commit { padding: 5px 10px; border-bottom: 1px solid var(--ide-border); }
.idesb-commit-msg { font-size: 12px; color: var(--ide-text); }
.idesb-commit-meta { font-size: 10.5px; color: var(--ide-text-dim); margin-top: 2px; }

/* 拖拽手柄 */
.idesb-drag {
  position: absolute; top: 0; bottom: 0; right: -2px; width: 5px;
  cursor: col-resize; z-index: 5; transition: background-color .15s;
}
.idesb-drag:hover, .idesb-drag.is-resizing { background: var(--ide-blue); }
</style>

<style>
/* 非 scoped:文件树节点由 h() 渲染 */
.idesb-node {
  display: flex; align-items: center; gap: 4px;
  height: 22px; padding-right: 8px;
  font-size: 12.5px; color: var(--ide-text); cursor: pointer; user-select: none;
}
.idesb-node:hover { background: var(--ide-hover); }
.idesb-node.is-on { background: var(--ide-active); color: var(--ide-text-strong); }
.idesb-node-chev {
  width: 12px; flex-shrink: 0; color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center; transition: transform .12s;
}
.idesb-node-chev.is-open { transform: rotate(90deg); }
.idesb-node-ic { flex-shrink: 0; color: var(--ide-text-dim); display: inline-flex; }
.idesb-node-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
