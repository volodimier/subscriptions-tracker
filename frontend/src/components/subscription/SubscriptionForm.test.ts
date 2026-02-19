import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import SubscriptionForm from './SubscriptionForm.vue'
import type { Subscription } from '@/types'

vi.mock('@/components/service/ServiceSelector.vue', () => ({
  default: defineComponent({
    name: 'MockServiceSelector',
    props: { modelValue: { type: [String, Number], default: undefined } },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
      return () => h('select', {
        'data-testid': 'service-selector',
        value: props.modelValue ?? '',
        onChange: (e: Event) => {
          const value = (e.target as HTMLSelectElement).value
          emit('update:modelValue', value ? Number(value) : null)
        },
      }, [
        h('option', { value: '' }, 'Select service'),
        h('option', { value: '1' }, 'Netflix'),
      ])
    },
  }),
}))

vi.mock('@/components/ui/button', () => ({
  Button: defineComponent({
    name: 'MockButton',
    props: {
      variant: { type: String, default: undefined },
      type: { type: String, default: 'button' },
      disabled: { type: Boolean, default: false },
    },
    emits: ['click'],
    setup(props, { slots, emit }) {
      return () => h('button', {
        type: props.type,
        disabled: props.disabled,
        onClick: () => emit('click'),
      }, slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/input', () => ({
  Input: defineComponent({
    name: 'MockInput',
    props: {
      modelValue: { type: [String, Number], default: undefined },
      id: { type: String, default: undefined },
      type: { type: String, default: 'text' },
      disabled: { type: Boolean, default: false },
      placeholder: { type: String, default: undefined },
    },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
      return () => h('input', {
        id: props.id,
        value: props.modelValue,
        type: props.type,
        disabled: props.disabled,
        placeholder: props.placeholder,
        onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
      })
    },
  }),
}))

vi.mock('@/components/ui/label', () => ({
  Label: defineComponent({
    name: 'MockLabel',
    setup(_, { slots }) {
      return () => h('label', slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/alert', () => ({
  Alert: defineComponent({
    name: 'MockAlert',
    setup(_, { slots }) {
      return () => h('div', { class: 'alert' }, slots.default?.())
    },
  }),
  AlertDescription: defineComponent({
    name: 'MockAlertDescription',
    setup(_, { slots }) {
      return () => h('div', slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/date-picker', () => ({
  DatePicker: defineComponent({
    name: 'MockDatePicker',
    props: {
      modelValue: { type: String, default: '' },
      placeholder: { type: String, default: undefined },
      disabled: { type: Boolean, default: false },
    },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
      return () => h('input', {
        type: 'date',
        value: props.modelValue,
        disabled: props.disabled,
        'data-testid': 'date-picker',
        'data-placeholder': props.placeholder,
        onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
      })
    },
  }),
}))

vi.mock('@/stores/settings', () => ({
  useSettingsStore: () => ({
    settings: {
      baseCurrency: 'USD',
      userTimeZone: 'UTC',
    },
    fetchSettings: vi.fn(),
  }),
}))

const createMockSubscription = (overrides: Partial<Subscription> = {}): Subscription => ({
  id: 1,
  service: { id: 1, name: 'Netflix', category: 'Entertainment', websiteUrl: undefined },
  amount: 15.99,
  currencyCode: 'USD',
  billingCycle: 'monthly',
  billingCycleDays: undefined,
  paymentMethod: 'Credit Card',
  startDate: '2025-01-01',
  nextBillingDate: '2025-02-01',
  status: 'active',
  cancelledAt: undefined,
  notes: undefined,
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
  ...overrides,
})

async function fillBaseRequiredFields(wrapper: ReturnType<typeof mount>) {
  await wrapper.get('[data-testid="service-selector"]').setValue('1')
  await wrapper.get('#amount').setValue('15.99')
}

function getCreateDateInput(wrapper: ReturnType<typeof mount>, placeholder: string) {
  return wrapper.get(`input[data-placeholder="${placeholder}"]`)
}

describe('SubscriptionForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-02-10T00:10:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('edit-mode behavior', () => {
    it('disables billing cycle radio buttons when editing', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: createMockSubscription(),
        },
      })
      await flushPromises()

      const radioInputs = wrapper.findAll('input[type="radio"]')
      expect(radioInputs.length).toBeGreaterThan(0)
      radioInputs.forEach((input) => {
        expect(input.attributes('disabled')).toBeDefined()
      })
    })

    it('shows billing-cycle restriction hint while editing', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: createMockSubscription(),
        },
      })
      await flushPromises()

      expect(wrapper.text()).toContain('Billing cycle cannot be changed')
    })
  })

  describe('recurrence validation', () => {
    it('blocks submit when both recurrence dates are missing', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await fillBaseRequiredFields(wrapper)
      await getCreateDateInput(wrapper, 'Select first billing date').setValue('')
      await getCreateDateInput(wrapper, 'Select next billing date').setValue('')

      await wrapper.get('form').trigger('submit.prevent')

      expect(wrapper.emitted('save')).toBeUndefined()
      expect(wrapper.text()).toContain('Either first billing date or next billing date is required.')
    })

    it('blocks submit when first billing date is after local cutoff', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await fillBaseRequiredFields(wrapper)
      await getCreateDateInput(wrapper, 'Select first billing date').setValue('2026-02-11')
      await getCreateDateInput(wrapper, 'Select next billing date').setValue('')

      await wrapper.get('form').trigger('submit.prevent')

      expect(wrapper.emitted('save')).toBeUndefined()
      expect(wrapper.text()).toContain('First billing date cannot be after your local cutoff date.')
    })

    it('blocks submit for strict both-dates mismatch', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await fillBaseRequiredFields(wrapper)
      await getCreateDateInput(wrapper, 'Select first billing date').setValue('2025-11-30')
      await getCreateDateInput(wrapper, 'Select next billing date').setValue('2026-02-27')

      await wrapper.get('form').trigger('submit.prevent')

      expect(wrapper.emitted('save')).toBeUndefined()
      expect(wrapper.text()).toContain('Next billing date does not match a standard recurrence schedule.')
    })

    it('shows monthly ambiguity prompt for Feb 28 and hides for non-ambiguous date', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await getCreateDateInput(wrapper, 'Select first billing date').setValue('')
      await getCreateDateInput(wrapper, 'Select next billing date').setValue('2026-02-28')

      const anchorSelect = wrapper.get('#anchorDay')
      expect(anchorSelect.exists()).toBe(true)
      const optionValues = anchorSelect.findAll('option').map(option => option.text())
      expect(optionValues).toEqual(expect.arrayContaining(['28', '29', '30', '31']))

      await getCreateDateInput(wrapper, 'Select next billing date').setValue('2026-03-12')
      expect(wrapper.find('#anchorDay').exists()).toBe(false)
    })

    it('shows yearly Feb 28 prompt and no prompt for Feb 29', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await wrapper.get('input[type="radio"][value="yearly"]').setValue(true)
      await getCreateDateInput(wrapper, 'Select first billing date').setValue('')
      await getCreateDateInput(wrapper, 'Select next billing date').setValue('2026-02-28')

      const yearlyAnchorSelect = wrapper.get('#anchorMonthDay')
      expect(yearlyAnchorSelect.exists()).toBe(true)
      expect(yearlyAnchorSelect.findAll('option').map(option => option.text())).toEqual(
        expect.arrayContaining(['02-28', '02-29'])
      )

      await getCreateDateInput(wrapper, 'Select next billing date').setValue('2024-02-29')
      expect(wrapper.find('#anchorMonthDay').exists()).toBe(false)
    })

    it('includes anchor only when ambiguity requires it', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await fillBaseRequiredFields(wrapper)
      await getCreateDateInput(wrapper, 'Select first billing date').setValue('')
      await getCreateDateInput(wrapper, 'Select next billing date').setValue('2026-02-28')
      await wrapper.get('#anchorDay').setValue('31')

      await wrapper.get('form').trigger('submit.prevent')

      const firstPayload = wrapper.emitted('save')?.[0]?.[0] as Record<string, unknown>
      expect(firstPayload.anchorDay).toBe(31)
      expect(firstPayload.anchorMonthDay).toBeUndefined()

      const wrapperNoAmbiguity = mount(SubscriptionForm)
      await flushPromises()
      await fillBaseRequiredFields(wrapperNoAmbiguity)
      await getCreateDateInput(wrapperNoAmbiguity, 'Select first billing date').setValue('')
      await getCreateDateInput(wrapperNoAmbiguity, 'Select next billing date').setValue('2026-03-12')

      await wrapperNoAmbiguity.get('form').trigger('submit.prevent')

      const secondPayload = wrapperNoAmbiguity.emitted('save')?.[0]?.[0] as Record<string, unknown>
      expect(secondPayload.anchorDay).toBeUndefined()
      expect(secondPayload.anchorMonthDay).toBeUndefined()
    })

    it('renders next-3 preview based on selected anchor', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await getCreateDateInput(wrapper, 'Select first billing date').setValue('')
      await getCreateDateInput(wrapper, 'Select next billing date').setValue('2026-02-28')
      await wrapper.get('#anchorDay').setValue('31')

      expect(wrapper.text()).toContain('Next 3 occurrences')
      expect(wrapper.text()).toContain('2026-02-28')
      expect(wrapper.text()).toContain('2026-03-31')
      expect(wrapper.text()).toContain('2026-04-30')
    })

    it('maps backend recurrence rule/code errors to inline field messages', async () => {
      const wrapper = mount(SubscriptionForm)
      await flushPromises()

      await wrapper.setProps({
        recurrenceError: {
          ruleId: 'VAL_REC_001',
          code: 'RECURRENCE_DATE_REQUIRED',
          field: 'firstBillDate,nextBillDate',
        },
      })

      expect(wrapper.text()).toContain('Either first billing date or next billing date is required.')
    })
  })
})
