import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSubscriptionsStore } from './subscriptions'
import { subscriptionService } from '@/services/subscriptionService'
import type { Subscription, SubscriptionDetail } from '@/types'

vi.mock('@/services/subscriptionService', () => ({
  subscriptionService: {
    getSubscriptions: vi.fn(),
    getSubscription: vi.fn(),
    getCategories: vi.fn(),
    createSubscription: vi.fn(),
    updateSubscription: vi.fn(),
    cancelSubscription: vi.fn(),
    reactivateSubscription: vi.fn(),
    deleteSubscription: vi.fn(),
  },
}))

const createMockSubscription = (overrides: Partial<Subscription> = {}): Subscription => ({
  id: 1,
  service: { id: 1, name: 'Netflix', category: 'Entertainment', websiteUrl: null },
  amount: 15.99,
  currencyCode: 'USD',
  billingCycle: 'monthly',
  billingCycleDays: null,
  paymentMethod: 'Credit Card',
  startDate: '2025-01-01',
  nextBillingDate: '2025-02-01',
  cancelledAt: null,
  notes: null,
  status: 'active',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
  ...overrides,
})

describe('Subscriptions Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have empty subscriptions array', () => {
      const store = useSubscriptionsStore()
      expect(store.subscriptions).toEqual([])
    })

    it('should have null currentSubscription', () => {
      const store = useSubscriptionsStore()
      expect(store.currentSubscription).toBeNull()
    })

    it('should have empty categories array', () => {
      const store = useSubscriptionsStore()
      expect(store.categories).toEqual([])
    })

    it('should have default filters', () => {
      const store = useSubscriptionsStore()
      expect(store.filters).toEqual({
        status: 'active',
        sort: 'nextBillingDate',
        order: 'asc',
      })
    })

    it('should have default pagination', () => {
      const store = useSubscriptionsStore()
      expect(store.pagination).toEqual({
        page: 1,
        limit: 20,
        total: 0,
        totalPages: 0,
      })
    })
  })

  describe('computed properties', () => {
    describe('activeSubscriptions', () => {
      it('should return only active subscriptions', () => {
        const store = useSubscriptionsStore()
        store.subscriptions = [
          createMockSubscription({ id: 1, status: 'active' }),
          createMockSubscription({ id: 2, status: 'cancelled' }),
          createMockSubscription({ id: 3, status: 'active' }),
        ]

        expect(store.activeSubscriptions).toHaveLength(2)
        expect(store.activeSubscriptions.map(s => s.id)).toEqual([1, 3])
      })

      it('should return empty array when no active subscriptions', () => {
        const store = useSubscriptionsStore()
        store.subscriptions = [
          createMockSubscription({ id: 1, status: 'cancelled' }),
          createMockSubscription({ id: 2, status: 'cancelled' }),
        ]

        expect(store.activeSubscriptions).toHaveLength(0)
      })

      it('should return empty array when subscriptions is empty', () => {
        const store = useSubscriptionsStore()
        expect(store.activeSubscriptions).toEqual([])
      })
    })

    describe('cancelledSubscriptions', () => {
      it('should return only cancelled subscriptions', () => {
        const store = useSubscriptionsStore()
        store.subscriptions = [
          createMockSubscription({ id: 1, status: 'active' }),
          createMockSubscription({ id: 2, status: 'cancelled' }),
          createMockSubscription({ id: 3, status: 'cancelled' }),
        ]

        expect(store.cancelledSubscriptions).toHaveLength(2)
        expect(store.cancelledSubscriptions.map(s => s.id)).toEqual([2, 3])
      })

      it('should return empty array when no cancelled subscriptions', () => {
        const store = useSubscriptionsStore()
        store.subscriptions = [
          createMockSubscription({ id: 1, status: 'active' }),
        ]

        expect(store.cancelledSubscriptions).toHaveLength(0)
      })
    })
  })

  describe('fetchSubscriptions', () => {
    const mockResponse = {
      data: [createMockSubscription()],
      pagination: { page: 1, limit: 20, total: 1, totalPages: 1 },
    }

    it('should fetch subscriptions and update state', async () => {
      vi.mocked(subscriptionService.getSubscriptions).mockResolvedValue(mockResponse)

      const store = useSubscriptionsStore()
      await store.fetchSubscriptions()

      expect(store.subscriptions).toEqual(mockResponse.data)
      expect(store.pagination).toEqual(mockResponse.pagination)
    })

    it('should set loading during fetch', async () => {
      let loadingDuringCall = false
      vi.mocked(subscriptionService.getSubscriptions).mockImplementation(async () => {
        const store = useSubscriptionsStore()
        loadingDuringCall = store.loading
        return mockResponse
      })

      const store = useSubscriptionsStore()
      await store.fetchSubscriptions()

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should pass page and limit parameters', async () => {
      vi.mocked(subscriptionService.getSubscriptions).mockResolvedValue(mockResponse)

      const store = useSubscriptionsStore()
      await store.fetchSubscriptions(2, 10)

      expect(subscriptionService.getSubscriptions).toHaveBeenCalledWith(
        store.filters,
        2,
        10
      )
    })

    it('should use current filters', async () => {
      vi.mocked(subscriptionService.getSubscriptions).mockResolvedValue(mockResponse)

      const store = useSubscriptionsStore()
      store.filters = { status: 'cancelled', sort: 'amount', order: 'desc' }
      await store.fetchSubscriptions()

      expect(subscriptionService.getSubscriptions).toHaveBeenCalledWith(
        { status: 'cancelled', sort: 'amount', order: 'desc' },
        1,
        20
      )
    })

    it('should set error on failure', async () => {
      vi.mocked(subscriptionService.getSubscriptions).mockRejectedValue({
        response: { data: { message: 'Server error' } },
      })

      const store = useSubscriptionsStore()

      await expect(store.fetchSubscriptions()).rejects.toBeDefined()

      expect(store.error).toBe('Server error')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(subscriptionService.getSubscriptions).mockRejectedValue({
        response: { data: {} },
      })

      const store = useSubscriptionsStore()

      await expect(store.fetchSubscriptions()).rejects.toBeDefined()

      expect(store.error).toBe('Failed to fetch subscriptions')
    })
  })

  describe('fetchSubscription', () => {
    const mockSubscriptionDetail: SubscriptionDetail = {
      ...createMockSubscription(),
      totalPaid: 47.97,
      paymentCount: 3,
    }

    it('should fetch subscription detail and update currentSubscription', async () => {
      vi.mocked(subscriptionService.getSubscription).mockResolvedValue(mockSubscriptionDetail)

      const store = useSubscriptionsStore()
      await store.fetchSubscription(1)

      expect(store.currentSubscription).toEqual(mockSubscriptionDetail)
    })

    it('should set error on failure', async () => {
      vi.mocked(subscriptionService.getSubscription).mockRejectedValue({
        response: { data: { message: 'Not found' } },
      })

      const store = useSubscriptionsStore()

      await expect(store.fetchSubscription(999)).rejects.toBeDefined()

      expect(store.error).toBe('Not found')
    })
  })

  describe('fetchCategories', () => {
    it('should fetch and update categories', async () => {
      const mockCategories = ['Entertainment', 'Music', 'Fitness']
      vi.mocked(subscriptionService.getCategories).mockResolvedValue(mockCategories)

      const store = useSubscriptionsStore()
      await store.fetchCategories()

      expect(store.categories).toEqual(mockCategories)
    })

    it('should silently fail without setting error', async () => {
      vi.mocked(subscriptionService.getCategories).mockRejectedValue(new Error('Failed'))

      const store = useSubscriptionsStore()
      await store.fetchCategories()

      expect(store.error).toBeNull()
      expect(store.categories).toEqual([])
    })
  })

  describe('createSubscription', () => {
    const mockRequest = {
      serviceId: 1,
      amount: 15.99,
      currencyCode: 'USD',
      billingCycle: 'monthly' as const,
      nextBillingDate: '2025-02-01',
    }

    it('should create subscription and refetch list', async () => {
      const mockSubscription = createMockSubscription()
      vi.mocked(subscriptionService.createSubscription).mockResolvedValue(mockSubscription)
      vi.mocked(subscriptionService.getSubscriptions).mockResolvedValue({
        data: [mockSubscription],
        pagination: { page: 1, limit: 20, total: 1, totalPages: 1 },
      })

      const store = useSubscriptionsStore()
      const result = await store.createSubscription(mockRequest)

      expect(result).toEqual(mockSubscription)
      expect(subscriptionService.getSubscriptions).toHaveBeenCalled()
      expect(store.createErrorDetails).toBeNull()
    })

    it('should set error on failure', async () => {
      vi.mocked(subscriptionService.createSubscription).mockRejectedValue({
        response: { data: { message: 'Validation error' } },
      })

      const store = useSubscriptionsStore()

      await expect(store.createSubscription(mockRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Validation error')
      expect(store.createErrorDetails).toBeNull()
    })

    it('should preserve recurrence details on create failure', async () => {
      vi.mocked(subscriptionService.createSubscription).mockRejectedValue({
        response: {
          data: {
            message: 'Either FirstBillDate or NextBillDate is required.',
            details: {
              ruleId: 'VAL_REC_001',
              code: 'RECURRENCE_DATE_REQUIRED',
              field: 'firstBillDate,nextBillDate',
            },
          },
        },
      })

      const store = useSubscriptionsStore()

      await expect(store.createSubscription(mockRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Either FirstBillDate or NextBillDate is required.')
      expect(store.createErrorDetails).toEqual({
        ruleId: 'VAL_REC_001',
        code: 'RECURRENCE_DATE_REQUIRED',
        field: 'firstBillDate,nextBillDate',
      })
    })

    it('should clear stale recurrence details before create', async () => {
      const mockSubscription = createMockSubscription()
      vi.mocked(subscriptionService.createSubscription).mockResolvedValue(mockSubscription)
      vi.mocked(subscriptionService.getSubscriptions).mockResolvedValue({
        data: [mockSubscription],
        pagination: { page: 1, limit: 20, total: 1, totalPages: 1 },
      })

      const store = useSubscriptionsStore()
      store.createErrorDetails = {
        ruleId: 'VAL_REC_001',
        code: 'RECURRENCE_DATE_REQUIRED',
        field: 'firstBillDate,nextBillDate',
      }

      await store.createSubscription(mockRequest)

      expect(store.createErrorDetails).toBeNull()
    })
  })

  describe('updateSubscription', () => {
    it('should update subscription in list', async () => {
      const originalSubscription = createMockSubscription({ id: 1, amount: 15.99 })
      const updatedSubscription = createMockSubscription({ id: 1, amount: 19.99 })

      vi.mocked(subscriptionService.updateSubscription).mockResolvedValue(updatedSubscription)

      const store = useSubscriptionsStore()
      store.subscriptions = [originalSubscription]

      const result = await store.updateSubscription(1, { amount: 19.99 })

      expect(result).toEqual(updatedSubscription)
      expect(store.subscriptions[0].amount).toBe(19.99)
    })

    it('should refetch currentSubscription if it matches updated id', async () => {
      const updatedSubscription = createMockSubscription({ id: 1 })
      const mockDetail: SubscriptionDetail = { ...updatedSubscription, totalPaid: 0, paymentCount: 0 }

      vi.mocked(subscriptionService.updateSubscription).mockResolvedValue(updatedSubscription)
      vi.mocked(subscriptionService.getSubscription).mockResolvedValue(mockDetail)

      const store = useSubscriptionsStore()
      store.currentSubscription = { ...updatedSubscription, totalPaid: 0, paymentCount: 0 }

      await store.updateSubscription(1, { amount: 19.99 })

      expect(subscriptionService.getSubscription).toHaveBeenCalledWith(1)
    })

    it('should not refetch if currentSubscription has different id', async () => {
      const updatedSubscription = createMockSubscription({ id: 1 })
      const differentSubscription = createMockSubscription({ id: 2 })

      vi.mocked(subscriptionService.updateSubscription).mockResolvedValue(updatedSubscription)

      const store = useSubscriptionsStore()
      store.currentSubscription = { ...differentSubscription, totalPaid: 0, paymentCount: 0 }

      await store.updateSubscription(1, { amount: 19.99 })

      expect(subscriptionService.getSubscription).not.toHaveBeenCalled()
    })
  })

  describe('cancelSubscription', () => {
    it('should update subscription status to cancelled', async () => {
      const cancelledSubscription = createMockSubscription({
        id: 1,
        status: 'cancelled',
        cancelledAt: '2025-01-15',
      })

      vi.mocked(subscriptionService.cancelSubscription).mockResolvedValue(cancelledSubscription)

      const store = useSubscriptionsStore()
      store.subscriptions = [createMockSubscription({ id: 1 })]

      const result = await store.cancelSubscription(1, { cancelledAt: '2025-01-15' })

      expect(result.status).toBe('cancelled')
      expect(store.subscriptions[0].status).toBe('cancelled')
    })
  })

  describe('reactivateSubscription', () => {
    it('should update subscription status to active', async () => {
      const reactivatedSubscription = createMockSubscription({
        id: 1,
        status: 'active',
        cancelledAt: null,
      })

      vi.mocked(subscriptionService.reactivateSubscription).mockResolvedValue(reactivatedSubscription)

      const store = useSubscriptionsStore()
      store.subscriptions = [createMockSubscription({ id: 1, status: 'cancelled' })]

      const result = await store.reactivateSubscription(1, { nextBillingDate: '2025-02-01' })

      expect(result.status).toBe('active')
      expect(store.subscriptions[0].status).toBe('active')
    })
  })

  describe('deleteSubscription', () => {
    it('should remove subscription from list', async () => {
      vi.mocked(subscriptionService.deleteSubscription).mockResolvedValue(undefined)

      const store = useSubscriptionsStore()
      store.subscriptions = [
        createMockSubscription({ id: 1 }),
        createMockSubscription({ id: 2 }),
        createMockSubscription({ id: 3 }),
      ]

      await store.deleteSubscription(2)

      expect(store.subscriptions).toHaveLength(2)
      expect(store.subscriptions.map(s => s.id)).toEqual([1, 3])
    })

    it('should set error on failure', async () => {
      vi.mocked(subscriptionService.deleteSubscription).mockRejectedValue({
        response: { data: { message: 'Cannot delete' } },
      })

      const store = useSubscriptionsStore()

      await expect(store.deleteSubscription(1)).rejects.toBeDefined()

      expect(store.error).toBe('Cannot delete')
    })
  })

  describe('setFilters', () => {
    it('should update filters with new values', () => {
      const store = useSubscriptionsStore()

      store.setFilters({ status: 'cancelled' })

      expect(store.filters.status).toBe('cancelled')
      expect(store.filters.sort).toBe('nextBillingDate')
      expect(store.filters.order).toBe('asc')
    })

    it('should merge multiple filter updates', () => {
      const store = useSubscriptionsStore()

      store.setFilters({ status: 'cancelled', sort: 'amount' })

      expect(store.filters).toEqual({
        status: 'cancelled',
        sort: 'amount',
        order: 'asc',
      })
    })

    it('should allow setting search and category', () => {
      const store = useSubscriptionsStore()

      store.setFilters({ search: 'netflix', category: 'Entertainment' })

      expect(store.filters.search).toBe('netflix')
      expect(store.filters.category).toBe('Entertainment')
    })
  })

  describe('clearCurrentSubscription', () => {
    it('should set currentSubscription to null', () => {
      const store = useSubscriptionsStore()
      store.currentSubscription = {
        ...createMockSubscription(),
        totalPaid: 0,
        paymentCount: 0,
      }

      store.clearCurrentSubscription()

      expect(store.currentSubscription).toBeNull()
    })
  })
})
