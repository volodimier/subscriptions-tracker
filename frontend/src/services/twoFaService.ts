import axios from 'axios'
import api from './api'
import type {
  TotpSetupResponse,
  TotpSetupCompleteResponse,
  TotpStatusResponse,
  AuthResponse,
} from '@/types'

// Create a separate axios instance for partial token requests
const createPartialTokenApi = (partialToken: string) => {
  return axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${partialToken}`,
    },
  })
}

export const twoFaService = {
  async getStatus(): Promise<TotpStatusResponse> {
    const response = await api.get<TotpStatusResponse>('/auth/totp/status')
    return response.data
  },

  async initiateSetup(): Promise<TotpSetupResponse> {
    const response = await api.post<TotpSetupResponse>('/auth/totp/setup')
    return response.data
  },

  async completeSetup(code: string): Promise<TotpSetupCompleteResponse> {
    const response = await api.post<TotpSetupCompleteResponse>('/auth/totp/setup/verify', { code })
    return response.data
  },

  async verifyCode(code: string, partialToken: string): Promise<AuthResponse> {
    const partialApi = createPartialTokenApi(partialToken)
    const response = await partialApi.post<AuthResponse>('/auth/totp/verify', { code })
    return response.data
  },

  async verifyRecoveryCode(recoveryCode: string, partialToken: string): Promise<AuthResponse> {
    const partialApi = createPartialTokenApi(partialToken)
    const response = await partialApi.post<AuthResponse>('/auth/totp/recovery', { recoveryCode })
    return response.data
  },

  async disable(password: string, code: string): Promise<void> {
    await api.post('/auth/totp/disable', { password, code })
  },

  async regenerateRecoveryCodes(code: string): Promise<TotpSetupCompleteResponse> {
    const response = await api.post<TotpSetupCompleteResponse>('/auth/totp/recovery/regenerate', { code })
    return response.data
  },
}
