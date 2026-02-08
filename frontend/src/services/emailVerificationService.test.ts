import { describe, it, expect, vi, beforeEach } from 'vitest'
import { emailVerificationService } from './emailVerificationService'
import api from './api'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('emailVerificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getStatus', () => {
    it('should fetch email verification status for verified user', async () => {
      const mockStatus = {
        verified: true,
        verifiedAt: '2024-01-01T00:00:00Z',
        withinGracePeriod: false,
        canResend: false,
      }
      vi.mocked(api.get).mockResolvedValue({ data: mockStatus })

      const result = await emailVerificationService.getStatus()

      expect(api.get).toHaveBeenCalledWith('/auth/email/status')
      expect(result).toEqual(mockStatus)
    })

    it('should fetch email verification status for unverified user within grace period', async () => {
      const mockStatus = {
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: true,
        resendAvailableInSeconds: 0,
      }
      vi.mocked(api.get).mockResolvedValue({ data: mockStatus })

      const result = await emailVerificationService.getStatus()

      expect(api.get).toHaveBeenCalledWith('/auth/email/status')
      expect(result).toEqual(mockStatus)
    })

    it('should return rate limit info when resend is not available', async () => {
      const mockStatus = {
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: false,
        resendAvailableInSeconds: 90,
      }
      vi.mocked(api.get).mockResolvedValue({ data: mockStatus })

      const result = await emailVerificationService.getStatus()

      expect(result.canResend).toBe(false)
      expect(result.resendAvailableInSeconds).toBe(90)
    })

    it('should handle errors', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Network error'))

      await expect(emailVerificationService.getStatus()).rejects.toThrow('Network error')
    })
  })

  describe('resendVerificationEmail', () => {
    it('should call API to resend verification email', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: undefined })

      await emailVerificationService.resendVerificationEmail()

      expect(api.post).toHaveBeenCalledWith('/auth/email/resend')
    })

    it('should handle rate limit error', async () => {
      vi.mocked(api.post).mockRejectedValue({
        response: {
          status: 429,
          data: { message: 'Rate limit exceeded' },
        },
      })

      await expect(emailVerificationService.resendVerificationEmail()).rejects.toEqual({
        response: {
          status: 429,
          data: { message: 'Rate limit exceeded' },
        },
      })
    })

    it('should handle already verified error', async () => {
      vi.mocked(api.post).mockRejectedValue({
        response: {
          status: 400,
          data: { message: 'Email already verified' },
        },
      })

      await expect(emailVerificationService.resendVerificationEmail()).rejects.toEqual({
        response: {
          status: 400,
          data: { message: 'Email already verified' },
        },
      })
    })
  })

  describe('verifyEmail', () => {
    it('should verify email with valid token', async () => {
      const mockUser = {
        id: 1,
        email: 'test@example.com',
        role: 'USER',
        emailVerified: true,
      }
      vi.mocked(api.get).mockResolvedValue({ data: mockUser })

      const result = await emailVerificationService.verifyEmail('valid-token')

      expect(api.get).toHaveBeenCalledWith('/auth/email/verify', {
        params: { token: 'valid-token' },
      })
      expect(result).toEqual(mockUser)
    })

    it('should handle invalid token error', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: {
          status: 400,
          data: { message: 'Invalid token', code: 'TOKEN_INVALID' },
        },
      })

      await expect(emailVerificationService.verifyEmail('invalid-token')).rejects.toEqual({
        response: {
          status: 400,
          data: { message: 'Invalid token', code: 'TOKEN_INVALID' },
        },
      })
    })

    it('should handle expired token error', async () => {
      vi.mocked(api.get).mockRejectedValue({
        response: {
          status: 400,
          data: { message: 'Token expired', code: 'TOKEN_EXPIRED' },
        },
      })

      await expect(emailVerificationService.verifyEmail('expired-token')).rejects.toEqual({
        response: {
          status: 400,
          data: { message: 'Token expired', code: 'TOKEN_EXPIRED' },
        },
      })
    })
  })
})
