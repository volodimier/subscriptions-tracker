import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { calculatePages, usePagination } from './usePagination'

describe('usePagination', () => {
  describe('calculatePages', () => {
    describe('when totalPages <= 7', () => {
      it('should return single page for totalPages = 1', () => {
        expect(calculatePages(1, 1)).toEqual([1])
      })

      it('should return all pages for totalPages = 2', () => {
        expect(calculatePages(1, 2)).toEqual([1, 2])
        expect(calculatePages(2, 2)).toEqual([1, 2])
      })

      it('should return all pages for totalPages = 5', () => {
        expect(calculatePages(1, 5)).toEqual([1, 2, 3, 4, 5])
        expect(calculatePages(3, 5)).toEqual([1, 2, 3, 4, 5])
        expect(calculatePages(5, 5)).toEqual([1, 2, 3, 4, 5])
      })

      it('should return all pages for totalPages = 7', () => {
        expect(calculatePages(1, 7)).toEqual([1, 2, 3, 4, 5, 6, 7])
        expect(calculatePages(4, 7)).toEqual([1, 2, 3, 4, 5, 6, 7])
        expect(calculatePages(7, 7)).toEqual([1, 2, 3, 4, 5, 6, 7])
      })

      it('should return empty array for totalPages = 0', () => {
        expect(calculatePages(1, 0)).toEqual([])
      })
    })

    describe('when totalPages > 7', () => {
      it('should show ellipsis only after first page when currentPage <= 3', () => {
        expect(calculatePages(1, 10)).toEqual([1, 2, '...', 10])
        expect(calculatePages(2, 10)).toEqual([1, 2, 3, '...', 10])
        expect(calculatePages(3, 10)).toEqual([1, 2, 3, 4, '...', 10])
      })

      it('should show both ellipses when currentPage is in the middle', () => {
        expect(calculatePages(5, 10)).toEqual([1, '...', 4, 5, 6, '...', 10])
        expect(calculatePages(6, 10)).toEqual([1, '...', 5, 6, 7, '...', 10])
      })

      it('should show ellipsis only before last page when currentPage >= totalPages - 2', () => {
        expect(calculatePages(8, 10)).toEqual([1, '...', 7, 8, 9, 10])
        expect(calculatePages(9, 10)).toEqual([1, '...', 8, 9, 10])
        expect(calculatePages(10, 10)).toEqual([1, '...', 9, 10])
      })

      it('should handle transition point at page 4', () => {
        expect(calculatePages(4, 10)).toEqual([1, '...', 3, 4, 5, '...', 10])
      })

      it('should handle transition point at page totalPages - 3', () => {
        expect(calculatePages(7, 10)).toEqual([1, '...', 6, 7, 8, '...', 10])
      })

      it('should work with large page counts', () => {
        expect(calculatePages(1, 100)).toEqual([1, 2, '...', 100])
        expect(calculatePages(50, 100)).toEqual([1, '...', 49, 50, 51, '...', 100])
        expect(calculatePages(100, 100)).toEqual([1, '...', 99, 100])
      })

      it('should work with 8 pages (boundary case)', () => {
        expect(calculatePages(1, 8)).toEqual([1, 2, '...', 8])
        expect(calculatePages(4, 8)).toEqual([1, '...', 3, 4, 5, '...', 8])
        expect(calculatePages(5, 8)).toEqual([1, '...', 4, 5, 6, '...', 8])
        expect(calculatePages(8, 8)).toEqual([1, '...', 7, 8])
      })
    })

    describe('edge cases', () => {
      it('should always include first and last page when totalPages > 7', () => {
        for (let current = 1; current <= 20; current++) {
          const pages = calculatePages(current, 20)
          expect(pages[0]).toBe(1)
          expect(pages[pages.length - 1]).toBe(20)
        }
      })

      it('should always include current page', () => {
        for (let current = 1; current <= 15; current++) {
          const pages = calculatePages(current, 15)
          expect(pages).toContain(current)
        }
      })

      it('should never have adjacent ellipses', () => {
        for (let current = 1; current <= 20; current++) {
          const pages = calculatePages(current, 20)
          for (let i = 0; i < pages.length - 1; i++) {
            if (pages[i] === '...') {
              expect(pages[i + 1]).not.toBe('...')
            }
          }
        }
      })
    })
  })

  describe('usePagination composable', () => {
    it('should return computed pages', () => {
      const currentPage = ref(1)
      const totalPages = ref(5)

      const { pages } = usePagination(currentPage, totalPages)

      expect(pages.value).toEqual([1, 2, 3, 4, 5])
    })

    it('should update when refs change', () => {
      const currentPage = ref(1)
      const totalPages = ref(10)

      const { pages } = usePagination(currentPage, totalPages)

      expect(pages.value).toEqual([1, 2, '...', 10])

      currentPage.value = 5
      expect(pages.value).toEqual([1, '...', 4, 5, 6, '...', 10])
    })

    describe('canGoToPage', () => {
      it('should return true for valid page', () => {
        const currentPage = ref(1)
        const totalPages = ref(5)

        const { canGoToPage } = usePagination(currentPage, totalPages)

        expect(canGoToPage(2)).toBe(true)
        expect(canGoToPage(3)).toBe(true)
        expect(canGoToPage(5)).toBe(true)
      })

      it('should return false for current page', () => {
        const currentPage = ref(3)
        const totalPages = ref(5)

        const { canGoToPage } = usePagination(currentPage, totalPages)

        expect(canGoToPage(3)).toBe(false)
      })

      it('should return false for page < 1', () => {
        const currentPage = ref(1)
        const totalPages = ref(5)

        const { canGoToPage } = usePagination(currentPage, totalPages)

        expect(canGoToPage(0)).toBe(false)
        expect(canGoToPage(-1)).toBe(false)
      })

      it('should return false for page > totalPages', () => {
        const currentPage = ref(1)
        const totalPages = ref(5)

        const { canGoToPage } = usePagination(currentPage, totalPages)

        expect(canGoToPage(6)).toBe(false)
        expect(canGoToPage(100)).toBe(false)
      })
    })
  })
})
