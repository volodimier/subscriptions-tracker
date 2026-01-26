import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { subscriptionService } from '@/services/subscriptionService'
import type {
  Subscription,
  SubscriptionDetail,
  CreateSubscriptionRequest,
  UpdateSubscriptionRequest,
  CancelSubscriptionRequest,
  ReactivateSubscriptionRequest,
  SubscriptionFilters,
} from '@/types'

export const useSubscriptionsStore = defineStore('subscriptions', () => {
  const subscriptions = ref<Subscription[]>([])
  const currentSubscription = ref<SubscriptionDetail | null>(null)
  const categories = ref<string[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const filters = ref<SubscriptionFilters>({
    status: 'active',
    sort: 'nextBillingDate',
    order: 'asc',
  })
  const pagination = ref({
    page: 1,
    limit: 20,
    total: 0,
    totalPages: 0,
  })

  const activeSubscriptions = computed(() =>
    subscriptions.value.filter(s => s.status === 'active')
  )

  const cancelledSubscriptions = computed(() =>
    subscriptions.value.filter(s => s.status === 'cancelled')
  )

  async function fetchSubscriptions(page = 1, limit = 20) {
    loading.value = true
    error.value = null
    try {
      const response = await subscriptionService.getSubscriptions(filters.value, page, limit)
      subscriptions.value = response.data
      pagination.value = response.pagination
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch subscriptions'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchSubscription(id: number) {
    loading.value = true
    error.value = null
    try {
      currentSubscription.value = await subscriptionService.getSubscription(id)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCategories() {
    try {
      categories.value = await subscriptionService.getCategories()
    } catch {
      // Silently fail
    }
  }

  async function createSubscription(data: CreateSubscriptionRequest): Promise<Subscription> {
    loading.value = true
    error.value = null
    try {
      const subscription = await subscriptionService.createSubscription(data)
      await fetchSubscriptions()
      return subscription
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to create subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateSubscription(id: number, data: UpdateSubscriptionRequest): Promise<Subscription> {
    loading.value = true
    error.value = null
    try {
      const subscription = await subscriptionService.updateSubscription(id, data)
      const index = subscriptions.value.findIndex(s => s.id === id)
      if (index !== -1) {
        subscriptions.value[index] = subscription
      }
      if (currentSubscription.value?.id === id) {
        await fetchSubscription(id)
      }
      return subscription
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to update subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function cancelSubscription(id: number, data: CancelSubscriptionRequest): Promise<Subscription> {
    loading.value = true
    error.value = null
    try {
      const subscription = await subscriptionService.cancelSubscription(id, data)
      const index = subscriptions.value.findIndex(s => s.id === id)
      if (index !== -1) {
        subscriptions.value[index] = subscription
      }
      return subscription
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to cancel subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function reactivateSubscription(id: number, data: ReactivateSubscriptionRequest): Promise<Subscription> {
    loading.value = true
    error.value = null
    try {
      const subscription = await subscriptionService.reactivateSubscription(id, data)
      const index = subscriptions.value.findIndex(s => s.id === id)
      if (index !== -1) {
        subscriptions.value[index] = subscription
      }
      return subscription
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to reactivate subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteSubscription(id: number) {
    loading.value = true
    error.value = null
    try {
      await subscriptionService.deleteSubscription(id)
      subscriptions.value = subscriptions.value.filter(s => s.id !== id)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to delete subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  function setFilters(newFilters: Partial<SubscriptionFilters>) {
    filters.value = { ...filters.value, ...newFilters }
  }

  function clearCurrentSubscription() {
    currentSubscription.value = null
  }

  return {
    subscriptions,
    currentSubscription,
    categories,
    loading,
    error,
    filters,
    pagination,
    activeSubscriptions,
    cancelledSubscriptions,
    fetchSubscriptions,
    fetchSubscription,
    fetchCategories,
    createSubscription,
    updateSubscription,
    cancelSubscription,
    reactivateSubscription,
    deleteSubscription,
    setFilters,
    clearCurrentSubscription,
  }
})
