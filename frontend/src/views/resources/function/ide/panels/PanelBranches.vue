<template>
  <div class="idep-branches">
    <div class="idep-bar">
      <div class="idep-search">
        <span class="idep-search-ic" v-html="BL.icon('search', 11)"></span>
        <input class="idep-input" v-model="q" placeholder="筛选分支" />
      </div>
      <button class="idep-icon-btn" title="刷新" @click="load" v-html="BL.icon('refresh', 12)"></button>
    </div>

    <div class="idep-body">
      <div v-if="loading" class="idep-empty">加载中…</div>
      <template v-else>
        <div v-for="b in filtered" :key="b.name"
             :class="['idep-row', b.current && 'is-on']"
             :title="b.remote_only ? '远程分支,切换时会自动建本地跟踪分支' : b.name"
             @click="!b.current && doCheckout(b)">
          <span class="idep-row-ic" :style="{ background: b.current ? '#00B42A' : '#86909C' }"
                v-html="BL.icon('branch', 10, '#fff')"></span>
          <span class="idep-row-name">{{ b.name }}</span>
          <!-- 相对远程同名分支的领先/落后 -->
          <span v-if="b.ahead" class="idep-ab is-ahead" title="领先远程的提交数">↑{{ b.ahead }}</span>
          <span v-if="b.behind" class="idep-ab is-behind" title="落后远程的提交数">↓{{ b.behind }}</span>
          <span class="idep-row-sub">
            <template v-if="b.current">当前</template>
            <template v-else-if="b.remote_only">远程</template>
            <template v-else-if="b.unmerged">+{{ b.unmerged }}</template>
            <template v-else>{{ b.head || '' }}</template>
          </span>
          <button v-if="!b.current && !b.remote_only" class="idep-row-act" title="合并到当前分支"
                  :disabled="busy" @click.stop="doMerge(b)" v-html="BL.icon('merge', 11)"></button>
          <!-- 当前分支和纯远程分支都没得删:前者 git 本身不允许, 后者本地根本没有 -->
          <button v-if="!b.current && !b.remote_only" class="idep-row-act" title="删除分支"
                  :disabled="busy" @click.stop="doDelete(b)" v-html="BL.icon('trash', 11)"></button>
        </div>
        <div v-if="!filtered.length" class="idep-empty">{{ q ? '无匹配分支' : '暂无分支' }}</div>

        <!-- 新建分支 -->
        <div class="idep-new">
          <input class="idep-input is-plain" v-model="newName" placeholder="新分支名,如 feature/xxx"
                 @keydown.enter="doCreate" />
          <button class="idep-btn is-primary" :disabled="!newName.trim() || busy" @click="doCreate">新建</button>
        </div>
        <div class="idep-hint">
          新建分支基于当前 HEAD 并立即切换。<b>工作区有未提交改动时无法切换或合并</b> ——
          请先保存(提交),或到「版本变更」面板撤销这些改动。<br />
          <span class="idep-ab is-ahead">↑</span> 领先远程 /
          <span class="idep-ab is-behind">↓</span> 落后远程 /
          <b>+N</b> 相对当前分支多出的提交。
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
/**
 * 分支管理 (文档「分支管理 Branches」)
 * 列出本地与远程分支、新建分支、切换分支。切换会改动磁盘工作区,
 * 因此完成后要通知外层重载文件树与已打开的文件。
 */
import { ref, computed, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { fnRepoApi } from '@/api'

const emit = defineEmits(['switched', 'count'])

const loading = ref(false)
const busy = ref(false)
const branches = ref([])
const q = ref('')
const newName = ref('')

async function load() {
  loading.value = true
  try {
    const list = await fnRepoApi.branches().catch(() => [])
    branches.value = Array.isArray(list) ? list : []
    emit('count', branches.value.length)
  } finally {
    loading.value = false
  }
}
onMounted(load)
defineExpose({ reload: load })

const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  return k ? branches.value.filter(b => b.name.toLowerCase().includes(k)) : branches.value
})

async function doCheckout(b) {
  const ok = await BL.confirm({
    title: '切换分支',
    content: `切换到「${b.name}」?工作区文件会随之变化,已打开的标签页将重新加载。`,
    okText: '切换'
  })
  if (!ok) return
  busy.value = true
  try {
    await fnRepoApi.checkout(b.name)
    BL.success(`已切换到 ${b.name}`)
    await load()
    emit('switched', b.name)
  } catch (e) {
    // 后端在工作区不干净时会拒绝, 这里把原因原样透出
    BL.error(e?.message || '切换失败')
  } finally {
    busy.value = false
  }
}

/**
 * 删除分支。
 * 先不带 force 试一次:后端只在「有未合并提交」时才拒,拿到这个拒绝再问一次要不要强删。
 * 这样既不会静默丢提交, 也不会对普通分支多问一遍。
 */
async function doDelete(b) {
  const ok = await BL.confirm({
    title: '删除分支',
    content: `删除本地分支「${b.name}」?${b.has_remote ? '远程同名分支不受影响。' : ''}`,
    danger: true, okText: '删除',
  })
  if (!ok) return
  busy.value = true
  try {
    await fnRepoApi.deleteBranch(b.name, false)
    BL.success(`已删除 ${b.name}`)
    await load()
  } catch (e) {
    const msg = e?.message || '删除失败'
    if (!msg.includes('未合并')) { BL.error(msg); return }
    const force = await BL.confirm({
      title: '分支有未合并提交',
      content: `「${b.name}」上有尚未合并到其他分支的提交,删除后这些提交将无法从分支名找回。确定强制删除?`,
      danger: true, okText: '强制删除',
    })
    if (!force) return
    try {
      await fnRepoApi.deleteBranch(b.name, true)
      BL.success(`已强制删除 ${b.name}`)
      await load()
    } catch (e2) {
      BL.error(e2?.message || '删除失败')
    }
  } finally {
    busy.value = false
  }
}

/**
 * 把某个分支合并进当前分支。
 * 冲突时后端已回滚到合并前状态, 这里只负责把冲突文件清单摆给用户看 ——
 * IDE 没有冲突解决界面, 半合并状态留在工作区只会更难收场。
 */
async function doMerge(b) {
  const cur = branches.value.find(x => x.current)?.name || '当前分支'
  const ok = await BL.confirm({
    title: '合并分支',
    content: `把「${b.name}」合并到「${cur}」?${b.unmerged ? `共 ${b.unmerged} 个提交。` : ''}`,
    okText: '合并',
  })
  if (!ok) return
  busy.value = true
  try {
    const r = await fnRepoApi.merge(b.name, false)
    if (r?.ok === false) {
      BL.error(r.message || '合并未成功')
      return
    }
    BL.success(r?.status === 'ALREADY_UP_TO_DATE'
      ? `${cur} 已包含 ${b.name} 的全部提交` : `已合并 ${b.name} → ${cur}`)
    await load()
    emit('switched', cur)          // 工作区文件变了, 让外层重载文件树与已打开的标签页
  } catch (e) {
    BL.error(e?.message || '合并失败')
  } finally {
    busy.value = false
  }
}

async function doCreate() {
  const name = newName.value.trim()
  if (!name) return
  busy.value = true
  try {
    await fnRepoApi.createBranch(name, true)
    BL.success(`已新建并切换到 ${name}`)
    newName.value = ''
    await load()
    emit('switched', name)
  } catch (e) {
    BL.error(e?.message || '新建分支失败')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.idep-branches { display: flex; flex-direction: column; min-height: 0; }
.idep-body { flex: 1; min-height: 0; overflow: auto; }
.idep-new { display: flex; gap: 4px; padding: 8px; border-top: 1px solid var(--ide-border); margin-top: 6px; }
.idep-ab {
  flex-shrink: 0; font-size: 10px; line-height: 14px; padding: 0 4px;
  border-radius: 7px; font-family: Consolas, Monaco, monospace;
}
.idep-ab.is-ahead { background: rgba(63, 185, 80, .16); color: #3fb950; }
.idep-ab.is-behind { background: rgba(204, 167, 0, .16); color: #cca700; }
</style>
