/**
 * 行业分类树的分组节点过滤逻辑 composable
 *
 * 用于处理 CategoryTreeFilter 中 categoryType=3 (分组节点) 的特殊逻辑：
 * - 分组节点应该按 group_id 过滤而不是 category_code
 * - 需要为分组节点提供自定义计数
 *
 * 使用示例:
 * const {
 *   categoryTree, selectedNode, customCategoryCounts,
 *   onCategoryChange, loadCategoryTree, filterByCategory
 * } = useCategoryGroupFilter({ items, groups })
 */

import { ref, computed } from 'vue'
import { categoryApi } from '@/api'

export function useCategoryGroupFilter({ items, groups }) {
  const categoryTree = ref([])
  const selectedCategoryCodes = ref(null)
  const selectedCategoryCode = ref('')
  const selectedNode = ref(null)

  /**
   * 加载分类树
   */
  async function loadCategoryTree() {
    categoryTree.value = await categoryApi.tree().catch(() => [])
  }

  /**
   * CategoryTreeFilter change 事件处理
   */
  function onCategoryChange({ codes, categoryCode, node }) {
    selectedCategoryCodes.value = codes || null
    selectedCategoryCode.value = categoryCode || ''
    selectedNode.value = node || null
  }

  /**
   * 为分组节点 (categoryType=3) 提供自定义计数：按 group_id 统计
   * CategoryTreeFilter 的 customCounts 格式: { [node.id]: count }
   */
  const customCategoryCounts = computed(() => {
    const counts = {}
    // 递归遍历分类树，为 categoryType=3 的节点计算项目数量
    const walk = (nodes) => {
      nodes.forEach(n => {
        if (n.categoryType === 3 && n.categoryCode) {
          // 分组节点：找到对应的 ont_biz_group，统计其下的项目数
          const groupsForNode = groups.value.filter(g => {
            const catCode = g.category_code || g.categoryCode
            return catCode === n.categoryCode
          })
          let count = 0
          groupsForNode.forEach(g => {
            count += items.value.filter(item => item.group_id === g.id).length
          })
          counts[n.id] = count
        }
        if (n.children) walk(n.children)
      })
    }
    walk(categoryTree.value)
    return counts
  })

  /**
   * 根据当前选中的分类节点过滤列表
   * @param {Array} list - 要过滤的列表
   * @returns {Array} 过滤后的列表
   */
  function filterByCategory(list) {
    if (!selectedCategoryCodes.value) return list

    // 特殊处理：如果选中的是分组节点 (categoryType=3)，按 group_id 过滤而不是 category_code
    if (selectedNode.value && selectedNode.value.categoryType === 3) {
      // 分组节点：按 group_id 过滤（找到对应的 group，通过 categoryCode 匹配）
      const groupsForNode = groups.value.filter(g => {
        const catCode = g.category_code || g.categoryCode
        return catCode === selectedNode.value.categoryCode
      })
      if (groupsForNode.length) {
        const groupIds = new Set(groupsForNode.map(g => g.id))
        // 递归收集子分组
        groupsForNode.forEach(g => {
          const collectChildren = (pid) => {
            groups.value.filter(g2 => {
              const parentId = g2.parent_id || g2.parentId
              return parentId === pid
            }).forEach(g2 => {
              groupIds.add(g2.id)
              collectChildren(g2.id)
            })
          }
          collectChildren(g.id)
        })
        return list.filter(item => item.group_id && groupIds.has(item.group_id))
      }
    } else {
      // 行业/领域节点：按 category_code 过滤
      return list.filter(item => {
        const catCode = item.category_code || item.categoryCode
        return selectedCategoryCodes.value.has(catCode)
      })
    }
    return list
  }

  return {
    categoryTree,
    selectedCategoryCodes,
    selectedCategoryCode,
    selectedNode,
    customCategoryCounts,
    onCategoryChange,
    loadCategoryTree,
    filterByCategory
  }
}
