import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import SubscriptionHistory from './SubscriptionHistory.vue'
import type { SubscriptionHistoryEntry } from '@/types'

// Mock shadcn UI components
vi.mock('@/components/ui/card', () => ({
  Card: defineComponent({
    name: 'MockCard',
    setup(_, { slots }) {
      return () => h('div', { class: 'card' }, slots.default?.())
    },
  }),
  CardContent: defineComponent({
    name: 'MockCardContent',
    setup(_, { slots }) {
      return () => h('div', { class: 'card-content' }, slots.default?.())
    },
  }),
}))

vi.mock('@/components/common/LoadingSpinner.vue', () => ({
  default: defineComponent({
    name: 'MockLoadingSpinner',
    props: ['size'],
    setup() {
      return () => h('div', { 'data-testid': 'loading-spinner' }, 'Loading...')
    },
  }),
}))

// Mock the subscription service
vi.mock('@/services/subscriptionService', () => ({
  subscriptionService: {
    getSubscriptionHistory: vi.fn(),
  },
}))

import { subscriptionService } from '@/services/subscriptionService'

const createEntry = (overrides: Partial<SubscriptionHistoryEntry> = {}): SubscriptionHistoryEntry => ({
  id: 1,
  subscriptionId: 42,
  changedAt: '2026-01-01T00:00:00Z',
  amount: 9.99,
  currencyCode: 'USD',
  paymentMethod: 'Credit Card',
  notes: undefined,
  ...overrides,
})

describe('SubscriptionHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should show loading spinner while fetching and hide it after', async () => {
    let resolvePromise: (value: SubscriptionHistoryEntry[]) => void
    vi.mocked(subscriptionService.getSubscriptionHistory).mockImplementation(
      () => new Promise((resolve) => { resolvePromise = resolve })
    )

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    // Wait for onMounted to start the async call (microtask)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="loading-spinner"]').exists()).toBe(true)

    resolvePromise!([])
    await flushPromises()

    expect(wrapper.find('[data-testid="loading-spinner"]').exists()).toBe(false)
  })

  it('should show empty state when no history entries', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockResolvedValue([])

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No change history available yet.')
  })

  it('should show error message when API call fails', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockRejectedValue(
      new Error('Network error')
    )

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load change history')
  })

  it('should display "Subscription created" for a single entry', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockResolvedValue([
      createEntry({ changedAt: '2026-01-01T00:00:00Z' }),
    ])

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Subscription created')
  })

  it('should display human-readable changes for multiple entries', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockResolvedValue([
      createEntry({ id: 2, changedAt: '2026-02-08T12:00:00Z', amount: 14.99 }),
      createEntry({ id: 1, changedAt: '2026-01-01T00:00:00Z', amount: 9.99 }),
    ])

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Price changed from 9.99 USD to 14.99 USD')
    expect(wrapper.text()).toContain('Subscription created')
  })

  it('should call API with the correct subscription ID', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockResolvedValue([])

    mount(SubscriptionHistory, {
      props: { subscriptionId: 123 },
    })
    await flushPromises()

    expect(subscriptionService.getSubscriptionHistory).toHaveBeenCalledWith(123)
  })

  it('should display the Change History heading', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockResolvedValue([])

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    await flushPromises()

    expect(wrapper.find('h3').text()).toBe('Change History')
  })

  it('should render timeline list with correct ARIA label', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockResolvedValue([
      createEntry({ changedAt: '2026-01-01T00:00:00Z' }),
    ])

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    await flushPromises()

    const list = wrapper.find('ul[role="list"]')
    expect(list.exists()).toBe(true)
    expect(list.attributes('aria-label')).toBe('Subscription change history')
  })

  it('should display multiple change types in a single entry', async () => {
    vi.mocked(subscriptionService.getSubscriptionHistory).mockResolvedValue([
      createEntry({
        id: 2,
        changedAt: '2026-02-08T12:00:00Z',
        amount: 19.99,
        paymentMethod: 'PayPal',
      }),
      createEntry({
        id: 1,
        changedAt: '2026-01-01T00:00:00Z',
        amount: 9.99,
        paymentMethod: 'Visa',
      }),
    ])

    const wrapper = mount(SubscriptionHistory, {
      props: { subscriptionId: 42 },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Price changed from 9.99 USD to 19.99 USD')
    expect(wrapper.text()).toContain('Payment method changed from Visa to PayPal')
  })
})
