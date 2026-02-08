import api from './api'
import type {
  Subscription,
  SubscriptionDetail,
  CreateSubscriptionRequest,
  UpdateSubscriptionRequest,
  CancelSubscriptionRequest,
  ReactivateSubscriptionRequest,
  SubscriptionFilters,
  SubscriptionHistoryEntry,
  PaginatedResponse,
} from '@/types'

export const subscriptionService = {
  async getSubscriptions(
    filters: SubscriptionFilters = {},
    page = 1,
    limit = 20
  ): Promise<PaginatedResponse<Subscription>> {
    const params = new URLSearchParams()
    if (filters.status && filters.status !== 'all') params.append('status', filters.status)
    if (filters.category) params.append('category', filters.category)
    if (filters.search) params.append('search', filters.search)
    if (filters.sort) params.append('sort', filters.sort)
    if (filters.order) params.append('order', filters.order)
    params.append('page', page.toString())
    params.append('limit', limit.toString())

    const response = await api.get<PaginatedResponse<Subscription>>(`/subscriptions?${params}`)
    return response.data
  },

  async getSubscription(id: number): Promise<SubscriptionDetail> {
    const response = await api.get<SubscriptionDetail>(`/subscriptions/${id}`)
    return response.data
  },

  async createSubscription(data: CreateSubscriptionRequest): Promise<Subscription> {
    const response = await api.post<Subscription>('/subscriptions', data)
    return response.data
  },

  async updateSubscription(id: number, data: UpdateSubscriptionRequest): Promise<Subscription> {
    const response = await api.put<Subscription>(`/subscriptions/${id}`, data)
    return response.data
  },

  async cancelSubscription(id: number, data: CancelSubscriptionRequest): Promise<Subscription> {
    const response = await api.patch<Subscription>(`/subscriptions/${id}/cancel`, data)
    return response.data
  },

  async reactivateSubscription(id: number, data: ReactivateSubscriptionRequest): Promise<Subscription> {
    const response = await api.patch<Subscription>(`/subscriptions/${id}/reactivate`, data)
    return response.data
  },

  async deleteSubscription(id: number): Promise<void> {
    await api.delete(`/subscriptions/${id}`)
  },

  async getCategories(): Promise<string[]> {
    const response = await api.get<string[]>('/subscriptions/categories')
    return response.data
  },

  async getSubscriptionHistory(id: number): Promise<SubscriptionHistoryEntry[]> {
    const response = await api.get<SubscriptionHistoryEntry[]>(`/subscriptions/${id}/history`)
    return response.data
  },
}
