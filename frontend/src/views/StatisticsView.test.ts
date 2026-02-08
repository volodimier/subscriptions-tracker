import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import StatisticsView from './StatisticsView.vue'
import { useDashboardStore } from '@/stores/dashboard'
import type { DashboardSummary, Projection } from '@/types'

// Mock child components to avoid rendering complexity
vi.mock('@/components/common/LoadingSpinner.vue', () => ({
  default: defineComponent({
    name: 'MockLoadingSpinner',
    props: { size: { type: String, default: undefined } },
    setup() {
      return () => h('div', { 'data-testid': 'loading-spinner' }, 'Loading...')
    },
  }),
}))

vi.mock('@/components/dashboard/SummaryCards.vue', () => ({
  default: defineComponent({
    name: 'MockSummaryCards',
    props: { summary: { type: Object, default: undefined }, projection: { type: Object, default: undefined } },
    setup() {
      return () => h('div', { 'data-testid': 'summary-cards' }, 'Summary Cards')
    },
  }),
}))

vi.mock('@/components/dashboard/CategoryChart.vue', () => ({
  default: defineComponent({
    name: 'MockCategoryChart',
    props: { data: { type: Array, default: undefined }, currency: { type: String, default: undefined } },
    setup() {
      return () => h('div', { 'data-testid': 'category-chart' }, 'Category Chart')
    },
  }),
}))

vi.mock('@/components/dashboard/SpendingChart.vue', () => ({
  default: defineComponent({
    name: 'MockSpendingChart',
    props: { data: { type: Array, default: undefined }, currency: { type: String, default: undefined } },
    setup() {
      return () => h('div', { 'data-testid': 'spending-chart' }, 'Spending Chart')
    },
  }),
}))

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
  CardHeader: defineComponent({
    name: 'MockCardHeader',
    setup(_, { slots }) {
      return () => h('div', { class: 'card-header' }, slots.default?.())
    },
  }),
  CardTitle: defineComponent({
    name: 'MockCardTitle',
    setup(_, { slots }) {
      return () => h('h3', { class: 'card-title' }, slots.default?.())
    },
  }),
}))

vi.mock('@/components/ui/button', () => ({
  Button: defineComponent({
    name: 'MockButton',
    props: { variant: { type: String, default: undefined }, size: { type: String, default: undefined } },
    emits: ['click'],
    setup(_, { slots, emit }) {
      return () =>
        h(
          'button',
          {
            onClick: () => emit('click'),
          },
          slots.default?.()
        )
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

vi.mock('@/components/ui/date-picker', () => ({
  DatePicker: defineComponent({
    name: 'MockDatePicker',
    props: { modelValue: { type: String, default: undefined }, placeholder: { type: String, default: undefined } },
    emits: ['update:modelValue'],
    setup(props) {
      return () =>
        h('input', {
          type: 'date',
          value: props.modelValue,
          'data-testid': 'date-picker',
        })
    },
  }),
}))

// Mock dashboard service
vi.mock('@/services/dashboardService', () => ({
  dashboardService: {
    getSummary: vi.fn(),
    getProjection: vi.fn(),
  },
}))

import { dashboardService } from '@/services/dashboardService'

const createMockSummary = (
  overrides: Partial<DashboardSummary> = {}
): DashboardSummary => ({
  period: {
    startDate: '2025-01-01',
    endDate: '2025-01-31',
  },
  summary: {
    totalSpent: 99.99,
    currency: 'USD',
    activeSubscriptions: 5,
    cancelledSubscriptions: 1,
    monthlyAverage: 99.99,
  },
  byCategory: [
    { category: 'Entertainment', total: 45.99, percentage: 46, count: 3 },
    { category: 'Music', total: 29.99, percentage: 30, count: 2 },
  ],
  topServices: [
    { serviceId: 1, serviceName: 'Netflix', totalSpent: 15.99, paymentCount: 1 },
  ],
  earliestSubscriptionDate: '2023-03-15',
  ...overrides,
})

const createMockProjection = (
  overrides: Partial<Projection> = {}
): Projection => ({
  year: 2025,
  baseCurrency: 'USD',
  projection: {
    estimatedTotal: 1199.88,
    activeSubscriptions: 5,
    assumptions: 'Based on current active subscriptions',
  },
  monthlyBreakdown: [
    { month: '2025-01', estimated: 99.99 },
    { month: '2025-02', estimated: 99.99 },
  ],
  ...overrides,
})

describe('StatisticsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2025, 5, 15, 12, 0, 0)) // June 15, 2025 at noon
    vi.clearAllMocks()

    // Setup default mock responses
    vi.mocked(dashboardService.getSummary).mockResolvedValue(createMockSummary())
    vi.mocked(dashboardService.getProjection).mockResolvedValue(createMockProjection())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const mountComponent = async () => {
    const wrapper = mount(StatisticsView, {
      global: {
        stubs: {
          RouterLink: true,
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  describe('date preset buttons', () => {
    it('should set correct date range for "This Month" button', async () => {
      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const thisMonthBtn = buttons.find((b) => b.text() === 'This Month')

      expect(thisMonthBtn).toBeDefined()
      await thisMonthBtn!.trigger('click')
      await flushPromises()

      // June 2025: starts June 1, ends June 15 (today)
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2025-06-01',
        '2025-06-15'
      )
    })

    it('should set correct date range for "Last 3 Months" button', async () => {
      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const last3MonthsBtn = buttons.find((b) => b.text() === 'Last 3 Months')

      expect(last3MonthsBtn).toBeDefined()
      await last3MonthsBtn!.trigger('click')
      await flushPromises()

      // 3 months before June 15, 2025 = March 15, 2025
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2025-03-15',
        '2025-06-15'
      )
    })

    it('should set correct date range for "This Year" button', async () => {
      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const thisYearBtn = buttons.find((b) => b.text() === 'This Year')

      expect(thisYearBtn).toBeDefined()
      await thisYearBtn!.trigger('click')
      await flushPromises()

      // Year 2025: starts Jan 1, ends June 15 (today)
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2025-01-01',
        '2025-06-15'
      )
    })

    it('should use earliestSubscriptionDate from API for "All Time" button', async () => {
      const wrapper = await mountComponent()

      // Verify initial fetch happened
      expect(dashboardService.getSummary).toHaveBeenCalled()

      const buttons = wrapper.findAll('button')
      const allTimeBtn = buttons.find((b) => b.text() === 'All Time')

      expect(allTimeBtn).toBeDefined()
      await allTimeBtn!.trigger('click')
      await flushPromises()

      // Should use earliestSubscriptionDate from the mock summary (2023-03-15)
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2023-03-15',
        '2025-06-15'
      )
    })

    it('should use fallback date when earliestSubscriptionDate is null', async () => {
      // Mock with null earliestSubscriptionDate
      vi.mocked(dashboardService.getSummary).mockResolvedValue(
        createMockSummary({ earliestSubscriptionDate: null })
      )

      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const allTimeBtn = buttons.find((b) => b.text() === 'All Time')

      expect(allTimeBtn).toBeDefined()
      await allTimeBtn!.trigger('click')
      await flushPromises()

      // Should use fallback date (2000-01-01)
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2000-01-01',
        '2025-06-15'
      )
    })

    it('should use fallback date when summary is not yet loaded', async () => {
      // Make the summary fetch very slow
      let resolveSummary: (value: DashboardSummary) => void
      vi.mocked(dashboardService.getSummary).mockImplementation(
        () =>
          new Promise((resolve) => {
            resolveSummary = resolve
          })
      )

      const wrapper = mount(StatisticsView, {
        global: {
          stubs: {
            RouterLink: true,
          },
        },
      })

      // Store hasn't received summary yet
      const store = useDashboardStore()
      expect(store.summary).toBeNull()

      // Click All Time before data loads
      const buttons = wrapper.findAll('button')
      const allTimeBtn = buttons.find((b) => b.text() === 'All Time')
      await allTimeBtn!.trigger('click')

      // Should use fallback since summary is null
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2000-01-01',
        '2025-06-15'
      )

      // Cleanup: resolve the pending promise
      resolveSummary!(createMockSummary())
      await flushPromises()
    })
  })

  describe('initial state', () => {
    it('should initialize with current year date range', async () => {
      await mountComponent()

      // Initial fetch should use current year start to today
      expect(dashboardService.getSummary).toHaveBeenCalledWith(
        '2025-01-01',
        '2025-06-15'
      )
    })

    it('should call fetchSummary and fetchProjection on mount', async () => {
      await mountComponent()

      expect(dashboardService.getSummary).toHaveBeenCalledTimes(1)
      expect(dashboardService.getProjection).toHaveBeenCalledTimes(1)
    })
  })

  describe('date handling across year boundary', () => {
    it('should handle "This Year" button correctly at year start', async () => {
      // Set date to January 5, 2025
      vi.setSystemTime(new Date(2025, 0, 5, 12, 0, 0))

      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const thisYearBtn = buttons.find((b) => b.text() === 'This Year')
      await thisYearBtn!.trigger('click')
      await flushPromises()

      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2025-01-01',
        '2025-01-05'
      )
    })

    it('should handle "Last 3 Months" button crossing year boundary', async () => {
      // Set date to February 15, 2025
      vi.setSystemTime(new Date(2025, 1, 15, 12, 0, 0))

      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const last3MonthsBtn = buttons.find((b) => b.text() === 'Last 3 Months')
      await last3MonthsBtn!.trigger('click')
      await flushPromises()

      // 3 months before Feb 15, 2025 = Nov 15, 2024
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2024-11-15',
        '2025-02-15'
      )
    })
  })

  describe('date handling at midnight and edge times', () => {
    it('should use correct local date at midnight', async () => {
      // Set date to midnight June 15, 2025
      vi.setSystemTime(new Date(2025, 5, 15, 0, 0, 0))

      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const thisMonthBtn = buttons.find((b) => b.text() === 'This Month')
      await thisMonthBtn!.trigger('click')
      await flushPromises()

      // Should still be June 15, not shifted to June 14
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2025-06-01',
        '2025-06-15'
      )
    })

    it('should use correct local date at 11:59 PM', async () => {
      // Set date to 11:59 PM June 15, 2025
      vi.setSystemTime(new Date(2025, 5, 15, 23, 59, 59))

      const wrapper = await mountComponent()

      const buttons = wrapper.findAll('button')
      const thisMonthBtn = buttons.find((b) => b.text() === 'This Month')
      await thisMonthBtn!.trigger('click')
      await flushPromises()

      // Should still be June 15, not shifted to June 16
      expect(dashboardService.getSummary).toHaveBeenLastCalledWith(
        '2025-06-01',
        '2025-06-15'
      )
    })
  })
})
