import { defineStore } from 'pinia'
import { ref } from 'vue'
import { paymentService } from '@/services/paymentService'
import type { PaymentRecord, CreatePaymentRequest, UpdatePaymentRequest } from '@/types'

export const usePaymentsStore = defineStore('payments', () => {
  const payments = ref<PaymentRecord[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    page: 1,
    limit: 20,
    total: 0,
    totalPages: 0,
  })

  async function fetchPayments(subscriptionId: number, page = 1, limit = 20) {
    loading.value = true
    error.value = null
    try {
      const response = await paymentService.getPaymentsForSubscription(subscriptionId, page, limit)
      payments.value = response.data
      pagination.value = response.pagination
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch payments'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createPayment(data: CreatePaymentRequest): Promise<PaymentRecord> {
    loading.value = true
    error.value = null
    try {
      const payment = await paymentService.createPayment(data)
      payments.value.unshift(payment)
      return payment
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to create payment'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updatePayment(id: number, data: UpdatePaymentRequest): Promise<PaymentRecord> {
    loading.value = true
    error.value = null
    try {
      const payment = await paymentService.updatePayment(id, data)
      const index = payments.value.findIndex(p => p.id === id)
      if (index !== -1) {
        payments.value[index] = payment
      }
      return payment
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to update payment'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deletePayment(id: number) {
    loading.value = true
    error.value = null
    try {
      await paymentService.deletePayment(id)
      payments.value = payments.value.filter(p => p.id !== id)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to delete payment'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearPayments() {
    payments.value = []
    pagination.value = { page: 1, limit: 20, total: 0, totalPages: 0 }
  }

  return {
    payments,
    loading,
    error,
    pagination,
    fetchPayments,
    createPayment,
    updatePayment,
    deletePayment,
    clearPayments,
  }
})
