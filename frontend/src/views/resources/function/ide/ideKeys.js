/**
 * 快捷键体系 (P9 · 文档 模块9-2)
 *
 * 默认绑定对齐 VS Code;用户自定义存 localStorage,支持冲突检测。
 * 这里只管「按键 ↔ 命令 id」的映射与匹配,命令本身在 ideCommands.js 里注册。
 */

const STORAGE_KEY = 'bl.ide.keybindings'

/** 把键盘事件规范成 "ctrl+shift+p" 这种可比较的字符串 */
export function keyOfEvent(e) {
  const parts = []
  if (e.ctrlKey || e.metaKey) parts.push('ctrl')
  if (e.shiftKey) parts.push('shift')
  if (e.altKey) parts.push('alt')
  let k = e.key
  if (k === ' ') k = 'space'
  else if (k.startsWith('Arrow')) k = k.slice(5).toLowerCase()
  else if (/^F\d{1,2}$/.test(k)) k = k.toLowerCase()
  else if (k.length === 1) k = k.toLowerCase()
  else k = k.toLowerCase()
  // 修饰键本身不构成绑定
  if (['control', 'shift', 'alt', 'meta'].includes(k)) return ''
  parts.push(k)
  return parts.join('+')
}

/** "ctrl+shift+p" → ["Ctrl","Shift","P"],用于界面展示 */
export function keyLabel(binding) {
  if (!binding) return ''
  return binding.split('+').map(p => {
    if (p === 'ctrl') return 'Ctrl'
    if (p === 'shift') return 'Shift'
    if (p === 'alt') return 'Alt'
    if (/^f\d{1,2}$/.test(p)) return p.toUpperCase()
    if (p === 'space') return 'Space'
    return p.length === 1 ? p.toUpperCase() : p.charAt(0).toUpperCase() + p.slice(1)
  })
}

export function loadOverrides() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export function saveOverrides(map) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(map || {})) } catch { /* 隐私模式忽略 */ }
}

/** 命令列表 + 用户覆盖 → 最终生效的 { commandId: binding } */
export function resolveBindings(commands, overrides) {
  const out = {}
  commands.forEach(c => {
    const ov = overrides[c.id]
    const b = ov === undefined ? (c.key || '') : ov      // 允许覆盖为 '' 表示解绑
    if (b) out[c.id] = b
  })
  return out
}

/**
 * 冲突检测:同一组合键绑到多个命令。
 * 返回 { binding: [commandId, ...] },只含真正冲突的项。
 */
export function findConflicts(bindings) {
  const byKey = {}
  Object.entries(bindings).forEach(([id, b]) => {
    if (!b) return
    ;(byKey[b] = byKey[b] || []).push(id)
  })
  const conflicts = {}
  Object.entries(byKey).forEach(([b, ids]) => { if (ids.length > 1) conflicts[b] = ids })
  return conflicts
}

/** 事件 → 命令 id(找不到返回 null) */
export function matchCommand(e, bindings) {
  const k = keyOfEvent(e)
  if (!k) return null
  for (const [id, b] of Object.entries(bindings)) {
    if (b === k) return id
  }
  return null
}
