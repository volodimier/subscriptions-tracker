import api from './api'
import type { UserSettings, ChangePasswordRequest, DeleteAccountRequest } from '@/types'

export interface UpdateSettingsRequest {
  baseCurrency?: string
  userTimeZone?: string
}

export const settingsService = {
  async getSettings(): Promise<UserSettings> {
    const response = await api.get<UserSettings>('/user/settings')
    return response.data
  },

  async updateSettings(data: UpdateSettingsRequest): Promise<UserSettings> {
    const response = await api.put<UserSettings>('/user/settings', data)
    return response.data
  },

  async changePassword(data: ChangePasswordRequest): Promise<{ message: string }> {
    const response = await api.post<{ message: string }>('/user/change-password', data)
    return response.data
  },

  async deleteAccount(data: DeleteAccountRequest): Promise<void> {
    await api.delete('/user/account', { data })
  },
}
