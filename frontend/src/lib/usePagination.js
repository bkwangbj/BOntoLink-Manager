import { ref, computed, watch } from 'vue'

/**
 * 列表分页组合式.
 * @param {import('vue').Ref<Array>|import('vue').ComputedRef<Array>} source 过滤后的全量列表 (ref / computed)
 * @param {number} defaultSize 默认每页条数
 * @returns {{ page, pageSize, totalPages, paged }}
 *
 * 用法:
 *   const { page, pageSize, totalPages, paged } = usePagination(filtered)
 *   // 列表渲染 v-for="x in paged"; 底部 <Pager v-model:page="page" v-model:page-size="pageSize" :total-pages="totalPages" />
 * 列表内容或每页条数变化导致页码越界时, 自动收敛回首页.
 */
export function usePagination(source, defaultSize = 20) {
  const page = ref(1)
  const pageSize = ref(defaultSize)
  const totalPages = computed(() => Math.max(1, Math.ceil((source.value?.length || 0) / pageSize.value)))
  const paged = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return (source.value || []).slice(start, start + pageSize.value)
  })
  watch([source, pageSize], () => { if (page.value > totalPages.value) page.value = 1 })
  return { page, pageSize, totalPages, paged }
}
