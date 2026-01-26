import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePaymentsStore } from './payments'
import { paymentService } from '@/services/paymentService'
import type { PaymentRecord } from '@/types'

vi.mock('@/services/paymentService', () => ({
  paymentService: {
    getPaymentsForSubscription: vi.fn(),
    createPayment: vi.fn(),
    updatePayment: vi.fn(),
    deletePayment: vi.fn(),
  },
}))

const createMockPayment = (overrides: Partial<PaymentRecord> = {}): PaymentRecord => ({
  id: 1,
  subscriptionId: 1,
  amount: 15.99,
  currencyCode: 'USD',
  paymentDate: '2025-01-15',
  fxRateToBase: 1.0,
  amountInBaseCurrency: 15.99,
  createdAt: '2025-01-15T00:00:00Z',
  updatedAt: '2025-01-15T00:00:00Z',
  ...overrides,
})

describe('Payments Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have empty payments array', () => {
      const store = usePaymentsStore()
      expect(store.payments).toEqual([])
    })

    it('should have loading set to false', () => {
      const store = usePaymentsStore()
      expect(store.loading).toBe(false)
    })

    it('should have error set to null', () => {
      const store = usePaymentsStore()
      expect(store.error).toBeNull()
    })

    it('should have default pagination', () => {
      const store = usePaymentsStore()
      expect(store.pagination).toEqual({
        page: 1,
        limit: 20,
        total: 0,
        totalPages: 0,
      })
    })
  })

  describe('fetchPayments', () => {
    const mockResponse = {
      data: [createMockPayment({ id: 1 }), createMockPayment({ id: 2 })],
      pagination: { page: 1, limit: 20, total: 2, totalPages: 1 },
    }

    it('should fetch payments and update state', async () => {
      vi.mocked(paymentService.getPaymentsForSubscription).mockResolvedValue(mockResponse)

      const store = usePaymentsStore()
      await store.fetchPayments(1)

      expect(store.payments).toEqual(mockResponse.data)
      expect(store.pagination).toEqual(mockResponse.pagination)
    })

    it('should pass subscriptionId, page and limit to service', async () => {
      vi.mocked(paymentService.getPaymentsForSubscription).mockResolvedValue(mockResponse)

      const store = usePaymentsStore()
      await store.fetchPayments(5, 2, 10)

      expect(paymentService.getPaymentsForSubscription).toHaveBeenCalledWith(5, 2, 10)
    })

    it('should use default page and limit', async () => {
      vi.mocked(paymentService.getPaymentsForSubscription).mockResolvedValue(mockResponse)

      const store = usePaymentsStore()
      await store.fetchPayments(1)

      expect(paymentService.getPaymentsForSubscription).toHaveBeenCalledWith(1, 1, 20)
    })

    it('should set loading during fetch', async () => {
      let loadingDuringCall = false
      vi.mocked(paymentService.getPaymentsForSubscription).mockImplementation(async () => {
        const store = usePaymentsStore()
        loadingDuringCall = store.loading
        return mockResponse
      })

      const store = usePaymentsStore()
      await store.fetchPayments(1)

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(paymentService.getPaymentsForSubscription).mockRejectedValue({
        response: { data: { message: 'Subscription not found' } },
      })

      const store = usePaymentsStore()

      await expect(store.fetchPayments(999)).rejects.toBeDefined()

      expect(store.error).toBe('Subscription not found')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(paymentService.getPaymentsForSubscription).mockRejectedValue({
        response: { data: {} },
      })

      const store = usePaymentsStore()

      await expect(store.fetchPayments(1)).rejects.toBeDefined()

      expect(store.error).toBe('Failed to fetch payments')
    })

    it('should clear error before fetching', async () => {
      vi.mocked(paymentService.getPaymentsForSubscription).mockResolvedValue(mockResponse)

      const store = usePaymentsStore()
      store.error = 'Previous error'

      await store.fetchPayments(1)

      expect(store.error).toBeNull()
    })
  })

  describe('createPayment', () => {
    const mockRequest = {
      subscriptionId: 1,
      amount: 15.99,
      currencyCode: 'USD',
      paymentDate: '2025-01-15',
    }

    it('should create payment and add to beginning of list', async () => {
      const newPayment = createMockPayment({ id: 3 })
      vi.mocked(paymentService.createPayment).mockResolvedValue(newPayment)

      const store = usePaymentsStore()
      store.payments = [createMockPayment({ id: 1 }), createMockPayment({ id: 2 })]

      const result = await store.createPayment(mockRequest)

      expect(result).toEqual(newPayment)
      expect(store.payments).toHaveLength(3)
      expect(store.payments[0].id).toBe(3)
    })

    it('should set loading during creation', async () => {
      let loadingDuringCall = false
      vi.mocked(paymentService.createPayment).mockImplementation(async () => {
        const store = usePaymentsStore()
        loadingDuringCall = store.loading
        return createMockPayment()
      })

      const store = usePaymentsStore()
      await store.createPayment(mockRequest)

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(paymentService.createPayment).mockRejectedValue({
        response: { data: { message: 'Invalid payment data' } },
      })

      const store = usePaymentsStore()

      await expect(store.createPayment(mockRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Invalid payment data')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(paymentService.createPayment).mockRejectedValue({
        response: { data: {} },
      })

      const store = usePaymentsStore()

      await expect(store.createPayment(mockRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Failed to create payment')
    })
  })

  describe('updatePayment', () => {
    const mockRequest = {
      amount: 19.99,
      currencyCode: 'USD',
      paymentDate: '2025-01-20',
    }

    it('should update payment in list', async () => {
      const updatedPayment = createMockPayment({ id: 2, amount: 19.99 })
      vi.mocked(paymentService.updatePayment).mockResolvedValue(updatedPayment)

      const store = usePaymentsStore()
      store.payments = [
        createMockPayment({ id: 1 }),
        createMockPayment({ id: 2, amount: 15.99 }),
        createMockPayment({ id: 3 }),
      ]

      const result = await store.updatePayment(2, mockRequest)

      expect(result).toEqual(updatedPayment)
      expect(store.payments[1].amount).toBe(19.99)
    })

    it('should not modify list if payment id not found', async () => {
      const updatedPayment = createMockPayment({ id: 99, amount: 19.99 })
      vi.mocked(paymentService.updatePayment).mockResolvedValue(updatedPayment)

      const store = usePaymentsStore()
      store.payments = [createMockPayment({ id: 1 }), createMockPayment({ id: 2 })]

      await store.updatePayment(99, mockRequest)

      expect(store.payments).toHaveLength(2)
      expect(store.payments.every(p => p.id !== 99)).toBe(true)
    })

    it('should set error on failure', async () => {
      vi.mocked(paymentService.updatePayment).mockRejectedValue({
        response: { data: { message: 'Payment not found' } },
      })

      const store = usePaymentsStore()

      await expect(store.updatePayment(999, mockRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Payment not found')
    })
  })

  describe('deletePayment', () => {
    it('should remove payment from list', async () => {
      vi.mocked(paymentService.deletePayment).mockResolvedValue(undefined)

      const store = usePaymentsStore()
      store.payments = [
        createMockPayment({ id: 1 }),
        createMockPayment({ id: 2 }),
        createMockPayment({ id: 3 }),
      ]

      await store.deletePayment(2)

      expect(store.payments).toHaveLength(2)
      expect(store.payments.map(p => p.id)).toEqual([1, 3])
    })

    it('should set error on failure', async () => {
      vi.mocked(paymentService.deletePayment).mockRejectedValue({
        response: { data: { message: 'Cannot delete payment' } },
      })

      const store = usePaymentsStore()

      await expect(store.deletePayment(1)).rejects.toBeDefined()

      expect(store.error).toBe('Cannot delete payment')
    })

    it('should set loading during deletion', async () => {
      let loadingDuringCall = false
      vi.mocked(paymentService.deletePayment).mockImplementation(async () => {
        const store = usePaymentsStore()
        loadingDuringCall = store.loading
      })

      const store = usePaymentsStore()
      store.payments = [createMockPayment({ id: 1 })]

      await store.deletePayment(1)

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })
  })

  describe('clearPayments', () => {
    it('should clear payments array', () => {
      const store = usePaymentsStore()
      store.payments = [createMockPayment({ id: 1 }), createMockPayment({ id: 2 })]

      store.clearPayments()

      expect(store.payments).toEqual([])
    })

    it('should reset pagination', () => {
      const store = usePaymentsStore()
      store.pagination = { page: 3, limit: 10, total: 50, totalPages: 5 }

      store.clearPayments()

      expect(store.pagination).toEqual({
        page: 1,
        limit: 20,
        total: 0,
        totalPages: 0,
      })
    })
  })
})
