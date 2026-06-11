import { ref } from 'vue'

/* 实例探索 — 对象类型收藏夹 (localStorage 持久化, 全局单例响应式) */
const KEY = 'bl-instance-favorites'

function load() {
  try { return new Set(JSON.parse(localStorage.getItem(KEY) || '[]')) }
  catch { return new Set() }
}

const favSet = ref(load())

function persist() {
  localStorage.setItem(KEY, JSON.stringify([...favSet.value]))
}

export function useFavorites() {
  const isFav = (id) => favSet.value.has(id)
  const toggle = (id) => {
    const s = new Set(favSet.value)
    s.has(id) ? s.delete(id) : s.add(id)
    favSet.value = s
    persist()
    return s.has(id)
  }
  return { favSet, isFav, toggle }
}
