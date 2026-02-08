import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import SubscriptionForm from './SubscriptionForm.vue'
import type { Subscription } from '@/types'

// Mock child components
vi.mock('@/components/service/ServiceSelector.vue', () => ({
  default: defineComponent({
    name: 'MockServiceSelector',
    props: { modelValue: { type: [String, Number, Object], default: undefined } },
    emits: ['update:modelValue'],
    setup(props) {
      return () => h('select', { 'data-testid': 'service-selector', value: props.modelValue })
    },
  }),
}))

// Mock shadcn UI components
vi.mock('@/components/ui/button', () => ({
  Button: defineComponent({
    name: 'MockButton',
    props: { variant: { type: String, default: undefined }, type: { type: String, default: undefined }, disabled: { type: Boolean, default: false } },
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
    props: { modelValue: { type: [String, Number], default: undefined }, id: { type: String, default: undefined }, type: { type: String, default: undefined }, step: { type: String, default: undefined }, min: { type: [String, Number], default: undefined }, required: { type: Boolean, default: false }, placeholder: { type: String, default: undefined }, disabled: { type: Boolean, default: false } },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
      return () => h('input', {
        id: props.id,
        value: props.modelValue,
        type: props.type,
        disabled: props.disabled,
        onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
      })
    },
  }),
}))

vi.mock('@/components/ui/label', () => ({
  Label: defineComponent({
    name: 'MockLabel',
    props: { for: { type: String, default: undefined } },
    setup(_, { slots }) {
      return () => h('label', slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/alert', () => ({
  Alert: defineComponent({
    name: 'MockAlert',
    props: { variant: { type: String, default: undefined } },
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
    props: { modelValue: { type: String, default: undefined }, placeholder: { type: String, default: undefined }, disabled: { type: Boolean, default: false } },
    emits: ['update:modelValue'],
    setup(props) {
      return () => h('input', {
        type: 'date',
        value: props.modelValue,
        disabled: props.disabled,
        'data-testid': 'date-picker',
      })
    },
  }),
}))

// Mock settings store
vi.mock('@/stores/settings', () => ({
  useSettingsStore: () => ({
    settings: { baseCurrency: 'USD' },
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

describe('SubscriptionForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('billing cycle edit restriction', () => {
    it('should disable billing cycle radio buttons when editing', async () => {
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

    it('should show hint text about billing cycle when editing', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: createMockSubscription(),
        },
      })
      await flushPromises()

      expect(wrapper.text()).toContain('Billing cycle cannot be changed')
      expect(wrapper.text()).toContain('cancel this subscription and create a new one')
    })

    it('should not disable billing cycle radio buttons when creating', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: {},
      })
      await flushPromises()

      const radioInputs = wrapper.findAll('input[type="radio"]')
      expect(radioInputs.length).toBeGreaterThan(0)
      radioInputs.forEach((input) => {
        expect(input.attributes('disabled')).toBeUndefined()
      })
    })

    it('should not show hint text about billing cycle when creating', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: {},
      })
      await flushPromises()

      expect(wrapper.text()).not.toContain('Billing cycle cannot be changed')
    })

    it('should show disabled styling on billing cycle labels when editing', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: createMockSubscription(),
        },
      })
      await flushPromises()

      const labels = wrapper.findAll('label')
      const billingCycleLabels = labels.filter((l) => {
        const text = l.text()
        return text === 'Monthly' || text === 'Yearly'
      })

      billingCycleLabels.forEach((label) => {
        expect(label.classes()).toContain('cursor-not-allowed')
        expect(label.classes()).toContain('opacity-50')
      })
    })

    it('should not show custom billing cycle days input when editing', async () => {
      const wrapper = mount(SubscriptionForm, {
        props: {
          subscription: createMockSubscription({ billingCycle: 'custom', billingCycleDays: 45 }),
        },
      })
      await flushPromises()

      expect(wrapper.text()).not.toContain('Every how many days?')
    })
  })
})
