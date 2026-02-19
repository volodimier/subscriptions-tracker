import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { subscriptionService } from '@/services/subscriptionService'
import type {
  ApiError,
  RecurrenceErrorDetails,
  Subscription,
  SubscriptionDetail,
  CreateSubscriptionRequest,
  UpdateSubscriptionRequest,
  CancelSubscriptionRequest,
  ReactivateSubscriptionRequest,
  SubscriptionFilters,
} from '@/types'

type SubscriptionAxiosError = { response?: { data?: ApiError } }

const RECURRENCE_CODE_MESSAGES: Record<string, string> = {
  RECURRENCE_DATE_REQUIRED: 'Either first billing date or next billing date is required.',
  RECURRENCE_FIRST_DATE_AFTER_CUTOFF: 'First billing date cannot be after your local cutoff date.',
  RECURRENCE_FIRST_AFTER_NEXT: 'First billing date must be on or before next billing date.',
  RECURRENCE_NEXT_DATE_MISMATCH: 'Next billing date does not match a standard recurrence schedule.',
  RECURRENCE_CADENCE_NOT_SUPPORTED: 'Only monthly and yearly billing cycles are supported.',
  RECURRENCE_USER_TIMEZONE_INVALID: 'Set a valid timezone in Settings before creating this subscription.',
  RECURRENCE_ANCHOR_OVERRIDE_NOT_ALLOWED: 'Anchor override is not allowed when first billing date is provided.',
  RECURRENCE_ANCHOR_REQUIRED: 'Select an anchor value for this billing date.',
  RECURRENCE_ANCHOR_NOT_ALLOWED: 'Anchor input is not allowed for this billing date.',
  RECURRENCE_MONTHLY_ANCHOR_REQUIRED: 'Select a monthly anchor day for this billing date.',
  RECURRENCE_MONTHLY_ANCHOR_INVALID: 'Selected monthly anchor day is not valid for this billing date.',
  RECURRENCE_MONTHLY_ANCHOR_OUT_OF_RANGE: 'Monthly anchor day must be between 1 and 31.',
  RECURRENCE_YEARLY_ANCHOR_REQUIRED: 'Select a yearly anchor month/day for this billing date.',
  RECURRENCE_YEARLY_ANCHOR_INVALID: 'Selected yearly anchor month/day is not valid for this billing date.',
  RECURRENCE_YEARLY_ANCHOR_FORMAT_INVALID: 'Yearly anchor format must be MM-DD and represent a valid date.',
}

function normalizeFieldName(field: string): string {
  switch (field.trim()) {
    case 'firstBillDate':
      return 'firstBillingDate'
    case 'nextBillDate':
      return 'nextBillingDate'
    default:
      return field.trim()
  }
}

function buildRecurrenceMessage(details: RecurrenceErrorDetails): string {
  if (details.code && RECURRENCE_CODE_MESSAGES[details.code]) {
    if (details.allowedValues && details.code.endsWith('_REQUIRED')) {
      return `${RECURRENCE_CODE_MESSAGES[details.code]} Allowed values: ${details.allowedValues}.`
    }
    return RECURRENCE_CODE_MESSAGES[details.code]
  }
  return 'Invalid recurrence input.'
}

export function extractRecurrenceErrorDetails(details?: ApiError['details']): RecurrenceErrorDetails | null {
  if (!details) {
    return null
  }
  if (details.ruleId || details.code || details.field || details.allowedValues) {
    return details
  }
  return null
}

export function mapRecurrenceErrorToFieldErrors(details: RecurrenceErrorDetails | null): Record<string, string> {
  if (!details) {
    return {}
  }

  const message = buildRecurrenceMessage(details)
  const rawFields = details.field?.split(',').map(normalizeFieldName).filter(Boolean) ?? []

  if (rawFields.length === 0) {
    return { form: message }
  }

  return rawFields.reduce<Record<string, string>>((acc, field) => {
    acc[field] = message
    return acc
  }, {})
}

export const useSubscriptionsStore = defineStore('subscriptions', () => {
  const subscriptions = ref<Subscription[]>([])
  const currentSubscription = ref<SubscriptionDetail | null>(null)
  const categories = ref<string[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const createErrorDetails = ref<RecurrenceErrorDetails | null>(null)
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
    createErrorDetails.value = null
    try {
      const subscription = await subscriptionService.createSubscription(data)
      await fetchSubscriptions()
      return subscription
    } catch (err: unknown) {
      const axiosError = err as SubscriptionAxiosError
      error.value = axiosError.response?.data?.message || 'Failed to create subscription'
      createErrorDetails.value = extractRecurrenceErrorDetails(axiosError.response?.data?.details)
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
    createErrorDetails,
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
