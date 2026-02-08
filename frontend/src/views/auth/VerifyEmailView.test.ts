import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import VerifyEmailView from './VerifyEmailView.vue'
import { emailVerificationService } from '@/services/emailVerificationService'

// Mock the emailVerificationService
vi.mock('@/services/emailVerificationService', () => ({
  emailVerificationService: {
    verifyEmail: vi.fn(),
  },
}))

// Mock vue-router
const mockPush = vi.fn()
const mockRoute = {
  query: {
    token: 'valid-token',
  },
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({
    push: mockPush,
  }),
}))

const globalStubs = {
  Card: { template: '<div><slot /></div>' },
  CardHeader: { template: '<div><slot /></div>' },
  CardTitle: { template: '<div><slot /></div>' },
  CardDescription: { template: '<div><slot /></div>' },
  CardContent: { template: '<div><slot /></div>' },
  Button: {
    template: '<button @click="$emit(\'click\')"><slot /></button>',
    emits: ['click'],
  },
  Alert: { template: '<div class="alert" :class="variant"><slot /></div>', props: ['variant'] },
  AlertDescription: { template: '<span><slot /></span>' },
}

describe('VerifyEmailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockRoute.query.token = 'valid-token'
  })

  describe('loading state', () => {
    it('should show loading state while verifying', async () => {
      // Make verifyEmail hang
      vi.mocked(emailVerificationService.verifyEmail).mockImplementation(
        () => new Promise(() => {}) // Never resolves
      )

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await nextTick()

      expect(wrapper.text()).toContain('Email Verification')
      expect(wrapper.text()).toContain('Verifying your email address')
      expect(wrapper.text()).toContain('Please wait while we verify your email')
    })
  })

  describe('success state', () => {
    it('should show success on valid token', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockResolvedValue({
        id: 1,
        email: 'test@example.com',
        role: 'USER',
        emailVerified: true,
      })

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(emailVerificationService.verifyEmail).toHaveBeenCalledWith('valid-token')
      expect(wrapper.text()).toContain('Email Verified')
      expect(wrapper.text()).toContain('Your email has been successfully verified')
      expect(wrapper.text()).toContain('Your email address has been verified')
      expect(wrapper.text()).toContain('Continue to Login')
    })

    it('should navigate to login after clicking continue button', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockResolvedValue({
        id: 1,
        email: 'test@example.com',
        role: 'USER',
        emailVerified: true,
      })

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const button = wrapper.find('button')
      await button.trigger('click')

      expect(mockPush).toHaveBeenCalledWith('/login')
    })
  })

  describe('error state', () => {
    it('should show error on invalid token', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockRejectedValue({
        response: {
          data: { message: 'Invalid token', code: 'TOKEN_INVALID' },
        },
      })

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Verification Failed')
      expect(wrapper.text()).toContain('This verification link is invalid')
    })

    it('should show error on expired token', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockRejectedValue({
        response: {
          data: { message: 'Token expired', code: 'TOKEN_EXPIRED' },
        },
      })

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Verification Failed')
      expect(wrapper.text()).toContain('This verification link has expired')
    })

    it('should show already verified message', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockRejectedValue({
        response: {
          data: { message: 'Already verified', code: 'ALREADY_VERIFIED' },
        },
      })

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Verification Failed')
      expect(wrapper.text()).toContain('Your email has already been verified')
    })

    it('should show generic error for unknown errors', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockRejectedValue({
        response: {
          data: { message: 'Something went wrong' },
        },
      })

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Verification Failed')
      expect(wrapper.text()).toContain('Something went wrong')
    })

    it('should show fallback error message when no message provided', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockRejectedValue(new Error('Network error'))

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Verification Failed')
      expect(wrapper.text()).toContain('Failed to verify email. Please try again.')
    })

    it('should show login navigation option on error', async () => {
      vi.mocked(emailVerificationService.verifyEmail).mockRejectedValue({
        response: {
          data: { message: 'Token expired', code: 'TOKEN_EXPIRED' },
        },
      })

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Go to Login')

      const button = wrapper.find('button')
      await button.trigger('click')

      expect(mockPush).toHaveBeenCalledWith('/login')
    })
  })

  describe('missing token', () => {
    it('should show error when no token is provided', async () => {
      mockRoute.query.token = ''

      const wrapper = mount(VerifyEmailView, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(emailVerificationService.verifyEmail).not.toHaveBeenCalled()
      expect(wrapper.text()).toContain('Verification Failed')
      expect(wrapper.text()).toContain('No verification token provided')
    })
  })
})
