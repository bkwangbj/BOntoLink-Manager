import { ref, onBeforeUnmount } from 'vue'

/**
 * 草稿与自动保存 (P9 · 文档 模块9-5)
 *
 * 定时把「有未保存改动」的文件缓存到 localStorage,刷新页面 / 异常关闭后能恢复。
 * 注意:这里存的是**本地草稿**,不等于保存 —— 真正落盘仍要走 Ctrl+S(服务端提交)。
 * 恢复时不会自动覆盖磁盘内容,而是把草稿灌回编辑器并标脏,由用户决定要不要保存。
 *
 * 按 分支 + 文件路径 分桶,切分支不会串草稿。
 */
const AUTOSAVE_MS = 5000
const MAX_BYTES = 2 * 1024 * 1024      // 单分支草稿总量上限,超了就不再写,避免撑爆 localStorage

export function useIdeDrafts() {
  const lastSavedAt = ref(0)
  const restoredCount = ref(0)
  let timer = null
  let branch = 'master'

  function key() { return `bl.ide.drafts.${branch}` }
  function setBranch(b) { branch = b || 'master' }

  function readAll() {
    try {
      const raw = localStorage.getItem(key())
      return raw ? JSON.parse(raw) : {}
    } catch {
      return {}
    }
  }

  function writeAll(map) {
    try {
      const s = JSON.stringify(map)
      if (s.length > MAX_BYTES) {
        console.warn('[ide] 草稿超出容量上限,本次不写入')
        return false
      }
      localStorage.setItem(key(), s)
      lastSavedAt.value = Date.now()
      return true
    } catch (e) {
      console.warn('[ide] 草稿写入失败', e)
      return false
    }
  }

  /**
   * 启动定时自动保存。
   * @param {Function} collect 返回 [{ path, content, dirty }]
   */
  function start(collect) {
    stop()
    timer = setInterval(() => {
      const files = collect() || []
      const dirty = files.filter(f => f.dirty)
      const map = {}
      dirty.forEach(f => { map[f.path] = { content: f.content, ts: Date.now() } })
      // 全部保存干净时清空草稿桶,避免下次误恢复旧内容
      if (!dirty.length) {
        const cur = readAll()
        if (Object.keys(cur).length) localStorage.removeItem(key())
        return
      }
      writeAll(map)
    }, AUTOSAVE_MS)
  }

  function stop() { clearInterval(timer); timer = null }

  /** 取出某分支的全部草稿 [{ path, content, ts }] */
  function list() {
    const map = readAll()
    return Object.entries(map).map(([path, v]) => ({ path, content: v.content, ts: v.ts }))
  }

  function drop(path) {
    const map = readAll()
    if (map[path]) { delete map[path]; writeAll(map) }
  }

  function clear() {
    try { localStorage.removeItem(key()) } catch { /* ignore */ }
  }

  onBeforeUnmount(stop)

  return { start, stop, list, drop, clear, setBranch, lastSavedAt, restoredCount }
}
