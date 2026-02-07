import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import TwoFaSetupDialog from './TwoFaSetupDialog.vue'
import { twoFaService } from '@/services/twoFaService'

// Mock the twoFaService
vi.mock('@/services/twoFaService', () => ({
  twoFaService: {
    initiateSetup: vi.fn(),
    completeSetup: vi.fn(),
  },
}))

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn().mockResolvedValue(undefined),
  },
})

// Mock URL.createObjectURL and URL.revokeObjectURL
global.URL.createObjectURL = vi.fn(() => 'blob:mock-url')
global.URL.revokeObjectURL = vi.fn()

const globalStubs = {
  Dialog: {
    template: '<div v-if="open"><slot /></div>',
    props: ['open'],
  },
  DialogScrollContent: { template: '<div><slot /></div>' },
  DialogHeader: { template: '<div><slot /></div>' },
  DialogTitle: { template: '<div><slot /></div>' },
  DialogFooter: { template: '<div><slot /></div>' },
  Button: {
    template: '<button @click="$emit(\'click\')"><slot /></button>',
    emits: ['click'],
  },
  Input: {
    template: '<input :value="value" @input="$emit(\'input\', $event)" />',
    props: ['value'],
    emits: ['input'],
  },
  Label: { template: '<label><slot /></label>' },
  Alert: { template: '<div class="alert"><slot /></div>' },
  AlertDescription: { template: '<span><slot /></span>' },
}

describe('TwoFaSetupDialog', () => {
  const mockSetupResponse = {
    secret: 'JBSWY3DPEHPK3PXP',
    qrCodeDataUri: 'data:image/png;base64,mockQrCode',
  }

  const mockCompleteResponse = {
    twoFactorEnabled: true,
    recoveryCodes: [
      'CODE1-AAAA-BBBB',
      'CODE2-CCCC-DDDD',
      'CODE3-EEEE-FFFF',
      'CODE4-GGGG-HHHH',
      'CODE5-IIII-JJJJ',
      'CODE6-KKKK-LLLL',
      'CODE7-MMMM-NNNN',
      'CODE8-OOOO-PPPP',
      'CODE9-QQQQ-RRRR',
      'CODE10-SSSS-TTTT',
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(twoFaService.initiateSetup).mockResolvedValue(mockSetupResponse)
    vi.mocked(twoFaService.completeSetup).mockResolvedValue(mockCompleteResponse)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('dialog opening', () => {
    it('should call initiateSetup when dialog opens via prop change', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: false,
        },
        global: {
          stubs: globalStubs,
        },
      })

      // Initially closed, should not call initiateSetup
      expect(twoFaService.initiateSetup).not.toHaveBeenCalled()

      // Open the dialog
      await wrapper.setProps({ open: true })
      await nextTick()

      // Should call initiateSetup
      expect(twoFaService.initiateSetup).toHaveBeenCalledTimes(1)
    })

    it('should call initiateSetup immediately when mounted with open=true', async () => {
      mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await nextTick()

      // Should call initiateSetup immediately due to immediate: true on watch
      expect(twoFaService.initiateSetup).toHaveBeenCalledTimes(1)
    })

    it('should not call initiateSetup again when closing and reopening if setupData exists', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      // Should have called once on initial open
      expect(twoFaService.initiateSetup).toHaveBeenCalledTimes(1)

      // Close the dialog - note: this won't clear setupData unless we call handleClose
      await wrapper.setProps({ open: false })
      await nextTick()

      // Reopen - should not call again because setupData is already set
      await wrapper.setProps({ open: true })
      await nextTick()

      // Should still be 1 because setupData was already set
      expect(twoFaService.initiateSetup).toHaveBeenCalledTimes(1)
    })
  })

  describe('setup step', () => {
    it('should show loading spinner while initiating', async () => {
      // Make initiateSetup hang
      vi.mocked(twoFaService.initiateSetup).mockImplementation(
        () => new Promise(() => {}) // Never resolves
      )

      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await nextTick()

      expect(wrapper.text()).toContain('Setting up two-factor authentication')
    })

    it('should show error when initiateSetup fails', async () => {
      vi.mocked(twoFaService.initiateSetup).mockRejectedValue({
        response: { data: { message: 'Setup failed' } },
      })

      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('Setup failed')
    })
  })

  describe('verify step', () => {
    it('should show QR code and secret after successful setup', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()
      await nextTick()

      expect(wrapper.text()).toContain('JBSWY3DPEHPK3PXP')
      expect(wrapper.text()).toContain('Scan this QR code')
    })

    it('should transition to verify step after setup completes', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as unknown as { step: string }
      expect(vm.step).toBe('verify')
    })
  })

  describe('recovery codes step', () => {
    it('should show recovery codes after successful verification', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      // Call verifySetup via component internals
      const vm = wrapper.vm as unknown as {
        verificationCode: string
        verifySetup: () => Promise<void>
      }
      vm.verificationCode = '123456'
      await vm.verifySetup()
      await flushPromises()

      // Should show recovery codes
      expect(wrapper.text()).toContain('Save Your Recovery Codes')
      expect(wrapper.text()).toContain('CODE1-AAAA-BBBB')
    })

    it('should disable Done button until codes are saved', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        verificationCode: string
        verifySetup: () => Promise<void>
        savedCodesConfirmed: boolean
        canConfirmSaved: boolean
      }

      vm.verificationCode = '123456'
      await vm.verifySetup()
      await flushPromises()

      // Checkbox should be disabled until codes are copied or downloaded
      expect(vm.canConfirmSaved).toBe(false)
    })

    it('should enable confirmation after copying codes', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        verificationCode: string
        verifySetup: () => Promise<void>
        copiedCodes: boolean
        canConfirmSaved: boolean
      }

      vm.verificationCode = '123456'
      await vm.verifySetup()
      await flushPromises()

      vm.copiedCodes = true
      await nextTick()

      expect(vm.canConfirmSaved).toBe(true)
    })
  })

  describe('emits', () => {
    it('should emit success when done is clicked', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      // Progress through the steps
      const vm = wrapper.vm as unknown as {
        verificationCode: string
        verifySetup: () => Promise<void>
        copiedCodes: boolean
        savedCodesConfirmed: boolean
        handleDone: () => void
      }

      vm.verificationCode = '123456'
      await vm.verifySetup()
      await flushPromises()

      // Simulate copying codes and confirming
      vm.copiedCodes = true
      vm.savedCodesConfirmed = true
      await nextTick()

      vm.handleDone()

      expect(wrapper.emitted('success')).toBeTruthy()
      expect(wrapper.emitted('update:open')).toBeTruthy()
    })

    it('should emit update:open when dialog closes from setup step', async () => {
      // Make initiateSetup hang so we stay in setup step
      vi.mocked(twoFaService.initiateSetup).mockImplementation(
        () => new Promise(() => {}) // Never resolves
      )

      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await nextTick()

      // handleClose should work from setup step
      const vm = wrapper.vm as unknown as { step: string; handleClose: () => void }
      expect(vm.step).toBe('setup')
      vm.handleClose()

      expect(wrapper.emitted('update:open')).toBeTruthy()
      expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
    })
  })

  describe('completeSetup', () => {
    it('should call twoFaService.completeSetup with verification code', async () => {
      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        verificationCode: string
        verifySetup: () => Promise<void>
      }

      vm.verificationCode = '123456'
      await vm.verifySetup()

      expect(twoFaService.completeSetup).toHaveBeenCalledWith('123456')
    })

    it('should show error when completeSetup fails', async () => {
      vi.mocked(twoFaService.completeSetup).mockRejectedValue({
        response: { data: { message: 'Invalid code' } },
      })

      const wrapper = mount(TwoFaSetupDialog, {
        props: {
          open: true,
        },
        global: {
          stubs: globalStubs,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        verificationCode: string
        verifySetup: () => Promise<void>
      }

      vm.verificationCode = '123456'
      await vm.verifySetup()
      await flushPromises()

      expect(wrapper.text()).toContain('Invalid code')
    })
  })
})
