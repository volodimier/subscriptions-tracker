import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import EmailVerificationSection from './EmailVerificationSection.vue'
import { emailVerificationService } from '@/services/emailVerificationService'

// Mock the emailVerificationService
vi.mock('@/services/emailVerificationService', () => ({
  emailVerificationService: {
    getStatus: vi.fn(),
    resendVerificationEmail: vi.fn(),
  },
}))

// Mock formatters
vi.mock('@/utils/formatters', () => ({
  formatDateTime: vi.fn((date: string) => `Formatted: ${date}`),
}))

const globalStubs = {
  Button: {
    template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
    props: ['disabled'],
    emits: ['click'],
  },
  Alert: { template: '<div class="alert" :class="variant"><slot /></div>', props: ['variant'] },
  AlertDescription: { template: '<span><slot /></span>' },
  Badge: { template: '<span class="badge"><slot /></span>', props: ['variant'] },
}

describe('EmailVerificationSection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('loading state', () => {
    it('should show loading spinner while fetching status', async () => {
      // Make getStatus hang
      vi.mocked(emailVerificationService.getStatus).mockImplementation(
        () => new Promise(() => {}) // Never resolves
      )

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await nextTick()

      expect(wrapper.text()).toContain('Loading email verification status')
    })
  })

  describe('verified status', () => {
    it('should display verified status correctly', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: true,
        verifiedAt: '2024-01-01T00:00:00Z',
        withinGracePeriod: false,
        canResend: false,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Email verified')
      expect(wrapper.text()).toContain('Verified')
      expect(wrapper.text()).toContain('Formatted: 2024-01-01T00:00:00Z')
    })

    it('should not show resend button for verified users', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: true,
        verifiedAt: '2024-01-01T00:00:00Z',
        withinGracePeriod: false,
        canResend: false,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).not.toContain('Resend Verification Email')
    })
  })

  describe('unverified status', () => {
    it('should display unverified status with grace period info', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: true,
        resendAvailableInSeconds: 0,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Email not verified')
      expect(wrapper.text()).toContain('Unverified')
    })

    it('should display deletion warning with correct date for unverified accounts', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: true,
        resendAvailableInSeconds: 0,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Warning')
      expect(wrapper.text()).toContain('Your account will be deleted on')
      expect(wrapper.text()).toContain('Formatted: 2024-01-15T00:00:00Z')
    })

    it('should not show deletion warning when not within grace period', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: false,
        canResend: true,
        resendAvailableInSeconds: 0,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).not.toContain('Your account will be deleted on')
    })
  })

  describe('resend button', () => {
    it('should enable resend button when canResend is true', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: true,
        resendAvailableInSeconds: 0,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const button = wrapper.find('button')
      expect(button.attributes('disabled')).toBeUndefined()
      expect(wrapper.text()).toContain('Resend Verification Email')
    })

    it('should disable resend button with countdown when rate limited', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: false,
        resendAvailableInSeconds: 90,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const button = wrapper.find('button')
      expect(button.attributes('disabled')).toBeDefined()
      expect(wrapper.text()).toContain('Resend available in 90s')
    })

    it('should countdown and re-enable button', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: false,
        resendAvailableInSeconds: 2,
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Resend available in 2s')

      // Advance timer by 1 second
      vi.advanceTimersByTime(1000)
      await nextTick()

      expect(wrapper.text()).toContain('Resend available in 1s')

      // Mock the next status call to return canResend: true
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: true,
        resendAvailableInSeconds: 0,
      })

      // Advance timer to complete countdown
      vi.advanceTimersByTime(1000)
      await nextTick()
      await flushPromises()

      expect(wrapper.text()).toContain('Resend Verification Email')
    })
  })

  describe('resend functionality', () => {
    it('should show success message after resend', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: true,
        resendAvailableInSeconds: 0,
      })
      vi.mocked(emailVerificationService.resendVerificationEmail).mockResolvedValue()

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const button = wrapper.find('button')
      await button.trigger('click')
      await flushPromises()

      expect(emailVerificationService.resendVerificationEmail).toHaveBeenCalled()
      expect(wrapper.text()).toContain('Verification email sent')
    })

    it('should show error message on resend failure', async () => {
      vi.mocked(emailVerificationService.getStatus).mockResolvedValue({
        verified: false,
        withinGracePeriod: true,
        gracePeriodEndsAt: '2024-01-15T00:00:00Z',
        canResend: true,
        resendAvailableInSeconds: 0,
      })
      vi.mocked(emailVerificationService.resendVerificationEmail).mockRejectedValue({
        response: {
          data: { message: 'Failed to send email' },
        },
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const button = wrapper.find('button')
      await button.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('Failed to send email')
    })
  })

  describe('error handling', () => {
    it('should display error when fetching status fails', async () => {
      vi.mocked(emailVerificationService.getStatus).mockRejectedValue({
        response: {
          data: { message: 'Server error' },
        },
      })

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Server error')
    })

    it('should display fallback error message when no message provided', async () => {
      vi.mocked(emailVerificationService.getStatus).mockRejectedValue(new Error('Network error'))

      const wrapper = mount(EmailVerificationSection, {
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('Failed to fetch email verification status')
    })
  })
})
