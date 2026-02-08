import api from './api'
import type { EmailVerificationStatus, User } from '@/types'

export const emailVerificationService = {
  async getStatus(): Promise<EmailVerificationStatus> {
    const response = await api.get<EmailVerificationStatus>('/auth/email/status')
    return response.data
  },

  async resendVerificationEmail(): Promise<void> {
    await api.post('/auth/email/resend')
  },

  async verifyEmail(token: string): Promise<User> {
    const response = await api.get<User>('/auth/email/verify', {
      params: { token },
    })
    return response.data
  },
}
