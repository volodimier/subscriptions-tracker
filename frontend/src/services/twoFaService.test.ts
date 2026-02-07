import { describe, it, expect, vi, beforeEach } from 'vitest'
import { twoFaService } from './twoFaService'
import api from './api'
import axios from 'axios'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      post: vi.fn(),
    })),
  },
}))

describe('twoFaService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getStatus', () => {
    it('should fetch 2FA status', async () => {
      const mockStatus = {
        enabled: true,
        enabledAt: '2024-01-01T00:00:00Z',
        remainingRecoveryCodes: 8,
      }
      vi.mocked(api.get).mockResolvedValue({ data: mockStatus })

      const result = await twoFaService.getStatus()

      expect(api.get).toHaveBeenCalledWith('/auth/totp/status')
      expect(result).toEqual(mockStatus)
    })
  })

  describe('initiateSetup', () => {
    it('should initiate 2FA setup', async () => {
      const mockSetupData = {
        secret: 'JBSWY3DPEHPK3PXP',
        qrCodeDataUri: 'data:image/png;base64,...',
      }
      vi.mocked(api.post).mockResolvedValue({ data: mockSetupData })

      const result = await twoFaService.initiateSetup()

      expect(api.post).toHaveBeenCalledWith('/auth/totp/setup')
      expect(result).toEqual(mockSetupData)
    })
  })

  describe('completeSetup', () => {
    it('should complete 2FA setup with verification code', async () => {
      const mockResponse = {
        twoFactorEnabled: true,
        recoveryCodes: ['code1', 'code2', 'code3'],
      }
      vi.mocked(api.post).mockResolvedValue({ data: mockResponse })

      const result = await twoFaService.completeSetup('123456')

      expect(api.post).toHaveBeenCalledWith('/auth/totp/setup/verify', { code: '123456' })
      expect(result).toEqual(mockResponse)
    })
  })

  describe('verifyCode', () => {
    it('should verify TOTP code with partial token', async () => {
      const mockAuthResponse = {
        user: { id: 1, email: 'test@example.com', role: 'USER' },
        token: 'jwt-token',
        refreshToken: 'refresh-token',
      }
      const mockPartialApi = {
        post: vi.fn().mockResolvedValue({ data: mockAuthResponse }),
      }
      vi.mocked(axios.create).mockReturnValue(mockPartialApi as never)

      const result = await twoFaService.verifyCode('123456', 'partial-token')

      expect(axios.create).toHaveBeenCalledWith({
        baseURL: '/api/v1',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer partial-token',
        },
      })
      expect(mockPartialApi.post).toHaveBeenCalledWith('/auth/totp/verify', { code: '123456' })
      expect(result).toEqual(mockAuthResponse)
    })
  })

  describe('verifyRecoveryCode', () => {
    it('should verify recovery code with partial token', async () => {
      const mockAuthResponse = {
        user: { id: 1, email: 'test@example.com', role: 'USER' },
        token: 'jwt-token',
        refreshToken: 'refresh-token',
      }
      const mockPartialApi = {
        post: vi.fn().mockResolvedValue({ data: mockAuthResponse }),
      }
      vi.mocked(axios.create).mockReturnValue(mockPartialApi as never)

      const result = await twoFaService.verifyRecoveryCode('ABCD-1234', 'partial-token')

      expect(axios.create).toHaveBeenCalledWith({
        baseURL: '/api/v1',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer partial-token',
        },
      })
      expect(mockPartialApi.post).toHaveBeenCalledWith('/auth/totp/recovery', { recoveryCode: 'ABCD-1234' })
      expect(result).toEqual(mockAuthResponse)
    })
  })

  describe('disable', () => {
    it('should disable 2FA with password and code', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: undefined })

      await twoFaService.disable('password123', '123456')

      expect(api.post).toHaveBeenCalledWith('/auth/totp/disable', {
        password: 'password123',
        code: '123456',
      })
    })
  })

  describe('regenerateRecoveryCodes', () => {
    it('should regenerate recovery codes', async () => {
      const mockResponse = {
        twoFactorEnabled: true,
        recoveryCodes: ['newcode1', 'newcode2', 'newcode3'],
      }
      vi.mocked(api.post).mockResolvedValue({ data: mockResponse })

      const result = await twoFaService.regenerateRecoveryCodes('123456')

      expect(api.post).toHaveBeenCalledWith('/auth/totp/recovery/regenerate', { code: '123456' })
      expect(result).toEqual(mockResponse)
    })
  })
})
