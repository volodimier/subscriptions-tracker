import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { adminService, type AdminError } from './adminService'
import api from './api'
import type { JobRun, PaginatedResponse, Category } from '@/types'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const createMockJobRun = (overrides: Partial<JobRun> = {}): JobRun => ({
  id: 1,
  jobName: 'FX_RATES_UPDATE',
  triggerType: 'SCHEDULED',
  status: 'SUCCESS',
  startTime: '2025-01-15T10:00:00Z',
  finishTime: '2025-01-15T10:00:05Z',
  errorMessage: undefined,
  createdAt: '2025-01-15T10:00:00Z',
  ...overrides,
})

const createMockPaginatedResponse = (
  data: JobRun[],
  page = 1,
  totalPages = 1,
  total = data.length
): PaginatedResponse<JobRun> => ({
  data,
  pagination: {
    page,
    limit: 20,
    total,
    totalPages,
  },
})

describe('Admin Service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('getJobRuns', () => {
    it('should fetch job runs with default pagination', async () => {
      const mockResponse = createMockPaginatedResponse([createMockJobRun()])
      vi.mocked(api.get).mockResolvedValue({ data: mockResponse })

      const result = await adminService.getJobRuns()

      expect(api.get).toHaveBeenCalledWith('/admin/job-runs?page=1&limit=20')
      expect(result).toEqual(mockResponse)
    })

    it('should fetch job runs with custom pagination', async () => {
      const mockResponse = createMockPaginatedResponse([createMockJobRun()], 2, 5, 100)
      vi.mocked(api.get).mockResolvedValue({ data: mockResponse })

      const result = await adminService.getJobRuns(2, 50)

      expect(api.get).toHaveBeenCalledWith('/admin/job-runs?page=2&limit=50')
      expect(result).toEqual(mockResponse)
    })

    it('should handle 403 forbidden error', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: { status: 403, data: { message: 'Forbidden' } },
      })

      await expect(adminService.getJobRuns()).rejects.toEqual({
        status: 403,
        message: 'Access denied. Admin privileges required.',
      } as AdminError)
    })

    it('should handle generic API error', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: { status: 500, data: { message: 'Internal server error' } },
      })

      await expect(adminService.getJobRuns()).rejects.toEqual({
        status: 500,
        message: 'Internal server error',
      } as AdminError)
    })

    it('should handle error without message', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: { status: 500, data: {} },
      })

      await expect(adminService.getJobRuns()).rejects.toEqual({
        status: 500,
        message: 'An error occurred',
      } as AdminError)
    })

    it('should handle unexpected error', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network error'))

      await expect(adminService.getJobRuns()).rejects.toEqual({
        status: 500,
        message: 'An unexpected error occurred',
      } as AdminError)
    })
  })

  describe('getJobRunById', () => {
    it('should fetch a single job run by ID', async () => {
      const mockJobRun = createMockJobRun({ id: 42 })
      vi.mocked(api.get).mockResolvedValue({ data: mockJobRun })

      const result = await adminService.getJobRunById(42)

      expect(api.get).toHaveBeenCalledWith('/admin/job-runs/42')
      expect(result).toEqual(mockJobRun)
    })

    it('should handle 403 forbidden error', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: { status: 403, data: { message: 'Forbidden' } },
      })

      await expect(adminService.getJobRunById(1)).rejects.toEqual({
        status: 403,
        message: 'Access denied. Admin privileges required.',
      } as AdminError)
    })

    it('should handle 404 not found error', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: { status: 404, data: { message: 'Job run not found' } },
      })

      await expect(adminService.getJobRunById(999)).rejects.toEqual({
        status: 404,
        message: 'Job run not found',
      } as AdminError)
    })
  })

  describe('refreshFxRates', () => {
    it('should trigger FX rates refresh successfully', async () => {
      const mockResponse = {
        message: 'FX rates updated successfully',
        updatedAt: '2025-01-15T10:00:00Z',
        rates: { EUR: 0.92, GBP: 0.79 },
      }
      vi.mocked(api.post).mockResolvedValue({ data: mockResponse })

      const result = await adminService.refreshFxRates()

      expect(api.post).toHaveBeenCalledWith('/fx-rates/refresh')
      expect(result).toEqual(mockResponse)
    })

    it('should handle 403 forbidden error', async () => {
      vi.mocked(api.post).mockRejectedValue({
        response: { status: 403, data: { message: 'Forbidden' } },
      })

      await expect(adminService.refreshFxRates()).rejects.toEqual({
        status: 403,
        message: 'Access denied. Admin privileges required.',
      } as AdminError)
    })

    it('should handle FX service error', async () => {
      vi.mocked(api.post).mockRejectedValue({
        response: { status: 500, data: { message: 'Failed to fetch FX rates from external API' } },
      })

      await expect(adminService.refreshFxRates()).rejects.toEqual({
        status: 500,
        message: 'Failed to fetch FX rates from external API',
      } as AdminError)
    })
  })

  describe('getCategories', () => {
    it('should fetch all categories', async () => {
      const mockCategories: Category[] = [
        { id: 1, name: 'Entertainment', system: true },
        { id: 2, name: 'Music', system: true },
        { id: 3, name: 'Custom', system: false },
      ]
      vi.mocked(api.get).mockResolvedValue({ data: mockCategories })

      const result = await adminService.getCategories()

      expect(api.get).toHaveBeenCalledWith('/admin/categories')
      expect(result).toEqual(mockCategories)
    })

    it('should handle 403 forbidden error', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: { status: 403, data: { message: 'Forbidden' } },
      })

      await expect(adminService.getCategories()).rejects.toEqual({
        status: 403,
        message: 'Access denied. Admin privileges required.',
      } as AdminError)
    })
  })

  describe('createCategory', () => {
    it('should create a new category', async () => {
      const mockCategory: Category = { id: 4, name: 'Gaming', system: false }
      vi.mocked(api.post).mockResolvedValue({ data: mockCategory })

      const result = await adminService.createCategory({ name: 'Gaming' })

      expect(api.post).toHaveBeenCalledWith('/admin/categories', { name: 'Gaming' })
      expect(result).toEqual(mockCategory)
    })

    it('should handle 403 forbidden error', async () => {
      vi.mocked(api.post).mockRejectedValue({
        response: { status: 403, data: { message: 'Forbidden' } },
      })

      await expect(adminService.createCategory({ name: 'Test' })).rejects.toEqual({
        status: 403,
        message: 'Access denied. Admin privileges required.',
      } as AdminError)
    })

    it('should handle validation error', async () => {
      vi.mocked(api.post).mockRejectedValue({
        response: { status: 400, data: { message: 'Category name already exists' } },
      })

      await expect(adminService.createCategory({ name: 'Entertainment' })).rejects.toEqual({
        status: 400,
        message: 'Category name already exists',
      } as AdminError)
    })
  })

  describe('updateCategory', () => {
    it('should update an existing category', async () => {
      const mockCategory: Category = { id: 1, name: 'Updated Name', system: false }
      vi.mocked(api.put).mockResolvedValue({ data: mockCategory })

      const result = await adminService.updateCategory(1, { name: 'Updated Name' })

      expect(api.put).toHaveBeenCalledWith('/admin/categories/1', { name: 'Updated Name' })
      expect(result).toEqual(mockCategory)
    })

    it('should handle 403 forbidden error', async () => {
      vi.mocked(api.put).mockRejectedValue({
        response: { status: 403, data: { message: 'Forbidden' } },
      })

      await expect(adminService.updateCategory(1, { name: 'Test' })).rejects.toEqual({
        status: 403,
        message: 'Access denied. Admin privileges required.',
      } as AdminError)
    })

    it('should handle 404 not found error', async () => {
      vi.mocked(api.put).mockRejectedValue({
        response: { status: 404, data: { message: 'Category not found' } },
      })

      await expect(adminService.updateCategory(999, { name: 'Test' })).rejects.toEqual({
        status: 404,
        message: 'Category not found',
      } as AdminError)
    })
  })

  describe('deleteCategory', () => {
    it('should delete a category', async () => {
      vi.mocked(api.delete).mockResolvedValue({ status: 204 })

      await adminService.deleteCategory(1)

      expect(api.delete).toHaveBeenCalledWith('/admin/categories/1')
    })

    it('should handle 403 forbidden error', async () => {
      vi.mocked(api.delete).mockRejectedValue({
        response: { status: 403, data: { message: 'Forbidden' } },
      })

      await expect(adminService.deleteCategory(1)).rejects.toEqual({
        status: 403,
        message: 'Access denied. Admin privileges required.',
      } as AdminError)
    })

    it('should handle conflict error when category is in use', async () => {
      vi.mocked(api.delete).mockRejectedValue({
        response: { status: 409, data: { message: 'Category is in use and cannot be deleted' } },
      })

      await expect(adminService.deleteCategory(1)).rejects.toEqual({
        status: 409,
        message: 'Category is in use and cannot be deleted',
      } as AdminError)
    })
  })
})
