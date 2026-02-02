import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSettingsStore } from './settings'
import { useAuthStore } from './auth'
import { settingsService } from '@/services/settingsService'
import { paymentService } from '@/services/paymentService'
import type { UserSettings } from '@/types'

vi.mock('@/services/settingsService', () => ({
  settingsService: {
    getSettings: vi.fn(),
    updateSettings: vi.fn(),
    changePassword: vi.fn(),
    deleteAccount: vi.fn(),
  },
}))

vi.mock('@/services/paymentService', () => ({
  paymentService: {
    refreshFxRates: vi.fn(),
  },
}))

vi.mock('@/services/authService', () => ({
  authService: {
    logout: vi.fn(),
  },
}))

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
  },
}))

const createMockSettings = (overrides: Partial<UserSettings> = {}): UserSettings => ({
  baseCurrencyCode: 'USD',
  fxRates: {
    EUR: 0.92,
    GBP: 0.79,
    PLN: 4.05,
  },
  fxRatesUpdatedAt: '2025-01-15T10:00:00Z',
  ...overrides,
})

describe('Settings Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have null settings', () => {
      const store = useSettingsStore()
      expect(store.settings).toBeNull()
    })

    it('should have loading set to false', () => {
      const store = useSettingsStore()
      expect(store.loading).toBe(false)
    })

    it('should have error set to null', () => {
      const store = useSettingsStore()
      expect(store.error).toBeNull()
    })
  })

  describe('fetchSettings', () => {
    it('should fetch settings and update state', async () => {
      const mockSettings = createMockSettings()
      vi.mocked(settingsService.getSettings).mockResolvedValue(mockSettings)

      const store = useSettingsStore()
      await store.fetchSettings()

      expect(store.settings).toEqual(mockSettings)
    })

    it('should set loading during fetch', async () => {
      let loadingDuringCall = false
      vi.mocked(settingsService.getSettings).mockImplementation(async () => {
        const store = useSettingsStore()
        loadingDuringCall = store.loading
        return createMockSettings()
      })

      const store = useSettingsStore()
      await store.fetchSettings()

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(settingsService.getSettings).mockRejectedValue({
        response: { data: { message: 'Unauthorized' } },
      })

      const store = useSettingsStore()

      await expect(store.fetchSettings()).rejects.toBeDefined()

      expect(store.error).toBe('Unauthorized')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(settingsService.getSettings).mockRejectedValue({
        response: { data: {} },
      })

      const store = useSettingsStore()

      await expect(store.fetchSettings()).rejects.toBeDefined()

      expect(store.error).toBe('Failed to fetch settings')
    })

    it('should clear error before fetching', async () => {
      vi.mocked(settingsService.getSettings).mockResolvedValue(createMockSettings())

      const store = useSettingsStore()
      store.error = 'Previous error'

      await store.fetchSettings()

      expect(store.error).toBeNull()
    })
  })

  describe('updateBaseCurrency', () => {
    it('should update settings with new currency', async () => {
      const updatedSettings = createMockSettings({ baseCurrencyCode: 'EUR' })
      vi.mocked(settingsService.updateSettings).mockResolvedValue(updatedSettings)

      const store = useSettingsStore()
      await store.updateBaseCurrency('EUR')

      expect(store.settings).toEqual(updatedSettings)
      expect(settingsService.updateSettings).toHaveBeenCalledWith('EUR')
    })

    it('should update auth store user with new currency', async () => {
      const updatedSettings = createMockSettings({ baseCurrencyCode: 'EUR' })
      vi.mocked(settingsService.updateSettings).mockResolvedValue(updatedSettings)

      const authStore = useAuthStore()
      authStore.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }

      const store = useSettingsStore()
      await store.updateBaseCurrency('EUR')

      expect(authStore.user?.baseCurrencyCode).toBe('EUR')
    })

    it('should not fail if auth store user is null', async () => {
      const updatedSettings = createMockSettings({ baseCurrencyCode: 'EUR' })
      vi.mocked(settingsService.updateSettings).mockResolvedValue(updatedSettings)

      const authStore = useAuthStore()
      authStore.user = null

      const store = useSettingsStore()
      await store.updateBaseCurrency('EUR')

      expect(store.settings).toEqual(updatedSettings)
    })

    it('should set error on failure', async () => {
      vi.mocked(settingsService.updateSettings).mockRejectedValue({
        response: { data: { message: 'Invalid currency' } },
      })

      const store = useSettingsStore()

      await expect(store.updateBaseCurrency('INVALID')).rejects.toBeDefined()

      expect(store.error).toBe('Invalid currency')
    })

    it('should set loading during update', async () => {
      let loadingDuringCall = false
      vi.mocked(settingsService.updateSettings).mockImplementation(async () => {
        const store = useSettingsStore()
        loadingDuringCall = store.loading
        return createMockSettings()
      })

      const store = useSettingsStore()
      await store.updateBaseCurrency('EUR')

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })
  })

  describe('changePassword', () => {
    const passwordRequest = {
      currentPassword: 'oldPassword123',
      newPassword: 'newPassword456',
    }

    it('should change password successfully', async () => {
      const successResponse = { message: 'Password changed successfully' }
      vi.mocked(settingsService.changePassword).mockResolvedValue(successResponse)

      const store = useSettingsStore()
      const result = await store.changePassword(passwordRequest)

      expect(result).toEqual(successResponse)
      expect(settingsService.changePassword).toHaveBeenCalledWith(passwordRequest)
    })

    it('should set loading during password change', async () => {
      let loadingDuringCall = false
      vi.mocked(settingsService.changePassword).mockImplementation(async () => {
        const store = useSettingsStore()
        loadingDuringCall = store.loading
        return { message: 'Success' }
      })

      const store = useSettingsStore()
      await store.changePassword(passwordRequest)

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(settingsService.changePassword).mockRejectedValue({
        response: { data: { message: 'Current password is incorrect' } },
      })

      const store = useSettingsStore()

      await expect(store.changePassword(passwordRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Current password is incorrect')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(settingsService.changePassword).mockRejectedValue({
        response: { data: {} },
      })

      const store = useSettingsStore()

      await expect(store.changePassword(passwordRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Failed to change password')
    })
  })

  describe('deleteAccount', () => {
    const deleteRequest = {
      password: 'currentPassword123',
    }

    it('should delete account and logout', async () => {
      vi.mocked(settingsService.deleteAccount).mockResolvedValue(undefined)

      const authStore = useAuthStore()
      authStore.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }
      authStore.token = 'some-token'

      const store = useSettingsStore()
      await store.deleteAccount(deleteRequest)

      expect(settingsService.deleteAccount).toHaveBeenCalledWith(deleteRequest)
      expect(authStore.user).toBeNull()
      expect(authStore.token).toBeNull()
    })

    it('should set loading during deletion', async () => {
      let loadingDuringCall = false
      vi.mocked(settingsService.deleteAccount).mockImplementation(async () => {
        const store = useSettingsStore()
        loadingDuringCall = store.loading
      })

      const store = useSettingsStore()
      await store.deleteAccount(deleteRequest)

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(settingsService.deleteAccount).mockRejectedValue({
        response: { data: { message: 'Incorrect password' } },
      })

      const store = useSettingsStore()

      await expect(store.deleteAccount(deleteRequest)).rejects.toBeDefined()

      expect(store.error).toBe('Incorrect password')
    })
  })

  describe('refreshFxRates', () => {
    it('should refresh FX rates and refetch settings', async () => {
      const updatedSettings = createMockSettings({
        fxRates: { EUR: 0.93, GBP: 0.80, PLN: 4.10 },
        fxRatesUpdatedAt: '2025-01-16T10:00:00Z',
      })

      vi.mocked(paymentService.refreshFxRates).mockResolvedValue(undefined)
      vi.mocked(settingsService.getSettings).mockResolvedValue(updatedSettings)

      const store = useSettingsStore()
      await store.refreshFxRates()

      expect(paymentService.refreshFxRates).toHaveBeenCalled()
      expect(settingsService.getSettings).toHaveBeenCalled()
      expect(store.settings).toEqual(updatedSettings)
    })

    it('should set loading during refresh', async () => {
      let loadingDuringCall = false
      vi.mocked(paymentService.refreshFxRates).mockImplementation(async () => {
        const store = useSettingsStore()
        loadingDuringCall = store.loading
      })
      vi.mocked(settingsService.getSettings).mockResolvedValue(createMockSettings())

      const store = useSettingsStore()
      await store.refreshFxRates()

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failure', async () => {
      vi.mocked(paymentService.refreshFxRates).mockRejectedValue({
        response: { data: { message: 'Failed to fetch exchange rates' } },
      })

      const store = useSettingsStore()

      await expect(store.refreshFxRates()).rejects.toBeDefined()

      expect(store.error).toBe('Failed to fetch exchange rates')
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(paymentService.refreshFxRates).mockRejectedValue({
        response: { data: {} },
      })

      const store = useSettingsStore()

      await expect(store.refreshFxRates()).rejects.toBeDefined()

      expect(store.error).toBe('Failed to refresh FX rates')
    })
  })
})
