import { ref } from 'vue'
import { dictApi } from '@/api'

/**
 * 字典缓存 composable — 按 dict_code 加载字典条目，组件内共享缓存。
 *
 * 用法：
 * ```js
 * const { getDict, getValue, preload, refreshDict } = useDict()
 * const statusDict = await getDict('sys_act_status')
 * // statusDict = [{item_code:'1', item_value:'启用'}, ...]
 * const label = getValue('sys_act_status', row.status)  // "启用"
 * ```
 */
const cache = new Map()          // dictCode → ref(array)，全局共享
const loading = new Set()        // 正在加载中的 dictCode，防重复请求

export function useDict() {
  /** 加载单个字典，缓存命中直接返回 */
  async function getDict(code) {
    if (!code) return []
    if (cache.has(code)) return cache.get(code).value
    if (loading.has(code)) {
      // 等待正在进行的请求
      while (loading.has(code)) await new Promise(r => setTimeout(r, 50))
      return cache.get(code)?.value || []
    }
    loading.add(code)
    try {
      const data = await dictApi.getByCode(code).catch(() => [])
      const r = ref(data || [])
      cache.set(code, r)
      return r.value
    } finally {
      loading.delete(code)
    }
  }

  /** 获取显示值：getValue('sys_act_status', '1') → '启用'。缓存未加载时自动加载。 */
  function getValue(code, itemCode) {
    if (!code || itemCode == null) return String(itemCode ?? '')
    const r = cache.get(code)
    if (!r) {
      getDict(code) // 异步加载
      return String(itemCode)
    }
    const item = r.value.find(d => String(d.item_code) === String(itemCode))
    return item?.item_value || String(itemCode)
  }

  /** 预加载多个字典 */
  async function preload(codes) {
    if (!codes || !codes.length) return
    await Promise.all(codes.map(c => getDict(c)))
  }

  /** 刷新单个字典缓存 */
  async function refreshDict(code) {
    cache.delete(code)
    return getDict(code)
  }

  /** 刷新所有字典缓存 */
  function refreshAll() {
    cache.clear()
  }

  return { getDict, getValue, preload, refreshDict, refreshAll }
}
