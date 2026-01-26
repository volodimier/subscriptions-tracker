import { computed, type Ref } from 'vue'

export function calculatePages(currentPage: number, totalPages: number): (number | string)[] {
  const result: (number | string)[] = []

  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) {
      result.push(i)
    }
  } else {
    result.push(1)

    if (currentPage > 3) {
      result.push('...')
    }

    const start = Math.max(2, currentPage - 1)
    const end = Math.min(totalPages - 1, currentPage + 1)

    for (let i = start; i <= end; i++) {
      result.push(i)
    }

    if (currentPage < totalPages - 2) {
      result.push('...')
    }

    result.push(totalPages)
  }

  return result
}

export function usePagination(
  currentPage: Ref<number>,
  totalPages: Ref<number>
) {
  const pages = computed(() => calculatePages(currentPage.value, totalPages.value))

  function canGoToPage(page: number): boolean {
    return page >= 1 && page <= totalPages.value && page !== currentPage.value
  }

  return {
    pages,
    canGoToPage,
  }
}
