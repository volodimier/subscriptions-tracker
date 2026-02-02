import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { adminService, type AdminError } from './adminService'
import api from './api'
import type { JobRun, PaginatedResponse } from '@/types'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
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
})
