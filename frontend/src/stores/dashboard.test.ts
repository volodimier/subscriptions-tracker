import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from './dashboard'
import { dashboardService } from '@/services/dashboardService'
import type { DashboardSummary, Projection } from '@/types'

vi.mock('@/services/dashboardService', () => ({
  dashboardService: {
    getSummary: vi.fn(),
    getProjection: vi.fn(),
  },
}))

const createMockSummary = (overrides: Partial<DashboardSummary> = {}): DashboardSummary => ({
  totalMonthlySpending: 99.99,
  activeSubscriptionsCount: 5,
  upcomingPaymentsCount: 3,
  upcomingPaymentsAmount: 45.97,
  currencyCode: 'USD',
  categoryBreakdown: [
    { category: 'Entertainment', amount: 45.99, percentage: 46 },
    { category: 'Music', amount: 29.99, percentage: 30 },
    { category: 'Fitness', amount: 24.01, percentage: 24 },
  ],
  topServices: [
    { serviceName: 'Netflix', amount: 15.99, percentage: 16 },
    { serviceName: 'Spotify', amount: 9.99, percentage: 10 },
  ],
  ...overrides,
})

const createMockProjection = (overrides: Partial<Projection> = {}): Projection => ({
  annualTotal: 1199.88,
  currencyCode: 'USD',
  monthlyBreakdown: [
    { month: '2025-01', amount: 99.99 },
    { month: '2025-02', amount: 99.99 },
    { month: '2025-03', amount: 99.99 },
  ],
  ...overrides,
})

describe('Dashboard Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have null summary', () => {
      const store = useDashboardStore()
      expect(store.summary).toBeNull()
    })

    it('should have null projection', () => {
      const store = useDashboardStore()
      expect(store.projection).toBeNull()
    })

    it('should have loading set to false', () => {
      const store = useDashboardStore()
      expect(store.loading).toBe(false)
    })

    it('should have error set to null', () => {
      const store = useDashboardStore()
      expect(store.error).toBeNull()
    })
  })

  describe('fetchSummary', () => {
    it('should fetch summary and update state', async () => {
      const mockSummary = createMockSummary()
      vi.mocked(dashboardService.getSummary).mockResolvedValue(mockSummary)

      const store = useDashboardStore()
      await store.fetchSummary()

      expect(store.summary).toEqual(mockSummary)
    })

    it('should pass date range parameters', async () => {
      vi.mocked(dashboardService.getSummary).mockResolvedValue(createMockSummary())

      const store = useDashboardStore()
      await store.fetchSummary('2025-01-01', '2025-01-31')

      expect(dashboardService.getSummary).toHaveBeenCalledWith('2025-01-01', '2025-01-31')
    })

    it('should work without date parameters', async () => {
      vi.mocked(dashboardService.getSummary).mockResolvedValue(createMockSummary())

      const store = useDashboardStore()
      await store.fetchSummary()

      expect(dashboardService.getSummary).toHaveBeenCalledWith(undefined, undefined)
    })

    it('should set loading during fetch', async () => {
      let loadingDuringCall = false
      vi.mocked(dashboardService.getSummary).mockImplementation(async () => {
        const store = useDashboardStore()
        loadingDuringCall = store.loading
        return createMockSummary()
      })

      const store = useDashboardStore()
      await store.fetchSummary()

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(dashboardService.getSummary).mockRejectedValue({
        response: { data: { message: 'Server error' } },
      })

      const store = useDashboardStore()

      await expect(store.fetchSummary()).rejects.toBeDefined()

      expect(store.error).toBe('Server error')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(dashboardService.getSummary).mockRejectedValue({
        response: { data: {} },
      })

      const store = useDashboardStore()

      await expect(store.fetchSummary()).rejects.toBeDefined()

      expect(store.error).toBe('Failed to fetch summary')
    })

    it('should clear error before fetching', async () => {
      vi.mocked(dashboardService.getSummary).mockResolvedValue(createMockSummary())

      const store = useDashboardStore()
      store.error = 'Previous error'

      await store.fetchSummary()

      expect(store.error).toBeNull()
    })
  })

  describe('fetchProjection', () => {
    it('should fetch projection and update state', async () => {
      const mockProjection = createMockProjection()
      vi.mocked(dashboardService.getProjection).mockResolvedValue(mockProjection)

      const store = useDashboardStore()
      await store.fetchProjection()

      expect(store.projection).toEqual(mockProjection)
    })

    it('should set loading during fetch', async () => {
      let loadingDuringCall = false
      vi.mocked(dashboardService.getProjection).mockImplementation(async () => {
        const store = useDashboardStore()
        loadingDuringCall = store.loading
        return createMockProjection()
      })

      const store = useDashboardStore()
      await store.fetchProjection()

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(dashboardService.getProjection).mockRejectedValue({
        response: { data: { message: 'Failed to calculate projection' } },
      })

      const store = useDashboardStore()

      await expect(store.fetchProjection()).rejects.toBeDefined()

      expect(store.error).toBe('Failed to calculate projection')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(dashboardService.getProjection).mockRejectedValue({
        response: { data: {} },
      })

      const store = useDashboardStore()

      await expect(store.fetchProjection()).rejects.toBeDefined()

      expect(store.error).toBe('Failed to fetch projection')
    })

    it('should clear error before fetching', async () => {
      vi.mocked(dashboardService.getProjection).mockResolvedValue(createMockProjection())

      const store = useDashboardStore()
      store.error = 'Previous error'

      await store.fetchProjection()

      expect(store.error).toBeNull()
    })
  })

  describe('state updates', () => {
    it('should update summary with category breakdown', async () => {
      const mockSummary = createMockSummary({
        categoryBreakdown: [
          { category: 'Gaming', amount: 50.00, percentage: 50 },
          { category: 'Education', amount: 50.00, percentage: 50 },
        ],
      })
      vi.mocked(dashboardService.getSummary).mockResolvedValue(mockSummary)

      const store = useDashboardStore()
      await store.fetchSummary()

      expect(store.summary?.categoryBreakdown).toHaveLength(2)
      expect(store.summary?.categoryBreakdown[0].category).toBe('Gaming')
    })

    it('should update projection with monthly breakdown', async () => {
      const mockProjection = createMockProjection({
        monthlyBreakdown: [
          { month: '2025-01', amount: 100.00 },
          { month: '2025-02', amount: 150.00 },
        ],
      })
      vi.mocked(dashboardService.getProjection).mockResolvedValue(mockProjection)

      const store = useDashboardStore()
      await store.fetchProjection()

      expect(store.projection?.monthlyBreakdown).toHaveLength(2)
      expect(store.projection?.annualTotal).toBe(1199.88)
    })
  })
})
