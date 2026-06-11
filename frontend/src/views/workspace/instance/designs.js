import { ref } from 'vue'

/* 实例探索 — 已保存的"探索设计/模版"(localStorage 持久化, 全局单例响应式)
 * 每个设计:{ id, name, classId, className, icon, color, kw, pills, sort, order, viewMode, charts, savedAt } */
const KEY = 'bl-instance-designs'

function load() {
  try { return JSON.parse(localStorage.getItem(KEY) || '[]') }
  catch { return [] }
}

const designs = ref(load())

function persist() {
  localStorage.setItem(KEY, JSON.stringify(designs.value))
}

let seq = 0
function newId() { return 'design-' + Date.now().toString(36) + '-' + (++seq) }

export function useDesigns() {
  /* 某对象类型下的设计列表 */
  const listFor = (classId) => designs.value.filter(d => d.classId === classId)

  /* 新增设计;返回带 id 的对象 */
  const save = (design) => {
    const d = { ...design, id: newId(), savedAt: new Date().toISOString() }
    designs.value = [...designs.value, d]
    persist()
    return d
  }

  /* 覆盖更新已有设计 */
  const update = (id, patch) => {
    designs.value = designs.value.map(d => d.id === id ? { ...d, ...patch, savedAt: new Date().toISOString() } : d)
    persist()
  }

  const remove = (id) => {
    designs.value = designs.value.filter(d => d.id !== id)
    persist()
  }

  const get = (id) => designs.value.find(d => d.id === id) || null

  return { designs, listFor, save, update, remove, get }
}
