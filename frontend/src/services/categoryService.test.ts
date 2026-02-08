import { describe, it, expect, beforeEach, vi } from 'vitest'
import { categoryService } from './categoryService'
import api from './api'
import type { Category } from '@/types'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
  },
}))

const createMockCategory = (overrides: Partial<Category> = {}): Category => ({
  id: 1,
  name: 'Entertainment',
  system: true,
  ...overrides,
})

describe('Category Service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getCategories', () => {
    it('should fetch all categories', async () => {
      const mockCategories = [
        createMockCategory({ id: 1, name: 'Entertainment', system: true }),
        createMockCategory({ id: 2, name: 'Music', system: true }),
        createMockCategory({ id: 3, name: 'Custom', system: false }),
      ]
      vi.mocked(api.get).mockResolvedValue({ data: mockCategories })

      const result = await categoryService.getCategories()

      expect(api.get).toHaveBeenCalledWith('/categories')
      expect(result).toEqual(mockCategories)
      expect(result).toHaveLength(3)
    })

    it('should return empty array when no categories exist', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [] })

      const result = await categoryService.getCategories()

      expect(api.get).toHaveBeenCalledWith('/categories')
      expect(result).toEqual([])
    })

    it('should propagate API errors', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network error'))

      await expect(categoryService.getCategories()).rejects.toThrow('Network error')
    })
  })
})
