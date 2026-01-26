import api from './api'
import type { PaymentRecord, CreatePaymentRequest, UpdatePaymentRequest, PaginatedResponse } from '@/types'

export const paymentService = {
  async getPaymentsForSubscription(
    subscriptionId: number,
    page = 1,
    limit = 20
  ): Promise<PaginatedResponse<PaymentRecord>> {
    const response = await api.get<PaginatedResponse<PaymentRecord>>(
      `/subscriptions/${subscriptionId}/payments?page=${page}&limit=${limit}`
    )
    return response.data
  },

  async createPayment(data: CreatePaymentRequest): Promise<PaymentRecord> {
    const response = await api.post<PaymentRecord>('/payments', data)
    return response.data
  },

  async updatePayment(id: number, data: UpdatePaymentRequest): Promise<PaymentRecord> {
    const response = await api.put<PaymentRecord>(`/payments/${id}`, data)
    return response.data
  },

  async deletePayment(id: number): Promise<void> {
    await api.delete(`/payments/${id}`)
  },

  async refreshFxRates(): Promise<{ message: string; updatedAt: string; rates: Record<string, number> }> {
    const response = await api.post<{ message: string; updatedAt: string; rates: Record<string, number> }>(
      '/fx-rates/refresh'
    )
    return response.data
  },
}
