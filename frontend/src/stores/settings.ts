import { defineStore } from 'pinia'
import { ref } from 'vue'
import { settingsService } from '@/services/settingsService'
import { paymentService } from '@/services/paymentService'
import type { UserSettings, ChangePasswordRequest, DeleteAccountRequest } from '@/types'
import { useAuthStore } from './auth'

export const useSettingsStore = defineStore('settings', () => {
  const settings = ref<UserSettings | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSettings() {
    loading.value = true
    error.value = null
    try {
      settings.value = await settingsService.getSettings()
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch settings'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateBaseCurrency(currency: string) {
    loading.value = true
    error.value = null
    try {
      settings.value = await settingsService.updateSettings(currency)
      const authStore = useAuthStore()
      if (authStore.user) {
        authStore.updateUser({ ...authStore.user, baseCurrencyCode: currency })
      }
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to update settings'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changePassword(data: ChangePasswordRequest): Promise<{ message: string }> {
    loading.value = true
    error.value = null
    try {
      return await settingsService.changePassword(data)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to change password'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteAccount(data: DeleteAccountRequest) {
    loading.value = true
    error.value = null
    try {
      await settingsService.deleteAccount(data)
      const authStore = useAuthStore()
      await authStore.logout()
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to delete account'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function refreshFxRates() {
    loading.value = true
    error.value = null
    try {
      await paymentService.refreshFxRates()
      await fetchSettings()
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to refresh FX rates'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    settings,
    loading,
    error,
    fetchSettings,
    updateBaseCurrency,
    changePassword,
    deleteAccount,
    refreshFxRates,
  }
})
