import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SpendingChart from './SpendingChart.vue'

// Mock the Unovis components to avoid rendering issues in tests
vi.mock('@unovis/vue', () => ({
  VisXYContainer: { template: '<div><slot /></div>' },
  VisArea: { template: '<div />' },
  VisLine: { template: '<div />' },
  VisAxis: { template: '<div />' },
  VisCrosshair: { template: '<div />' },
  VisTooltip: { template: '<div />' },
}))

vi.mock('@unovis/ts', () => ({
  Area: { selectors: { area: 'area' } },
  Axis: { selectors: { grid: 'grid' } },
  Line: { selectors: { line: 'line' } },
  CurveType: { MonotoneX: 'monotoneX' },
  omit: vi.fn((obj, keys) => {
    const result = { ...obj }
    keys.forEach((key: string) => delete result[key])
    return result
  }),
}))

describe('SpendingChart', () => {
  const createMockData = () => [
    { month: '2025-01', estimated: 150.00 },
    { month: '2025-02', estimated: 175.50 },
    { month: '2025-03', estimated: 200.00 },
    { month: '2025-04', estimated: 125.75 },
  ]

  describe('rendering', () => {
    it('should render without crashing', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })
      expect(wrapper.exists()).toBe(true)
    })

    it('should have correct container height class', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })
      expect(wrapper.find('.h-64').exists()).toBe(true)
    })
  })

  describe('data transformation', () => {
    it('should transform month format correctly', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      // Access the component's internal chartData computed property
      const vm = wrapper.vm as unknown as { chartData: { month: string; spending: number }[] }
      expect(vm.chartData).toEqual([
        { month: 'Jan', spending: 150.00 },
        { month: 'Feb', spending: 175.50 },
        { month: 'Mar', spending: 200.00 },
        { month: 'Apr', spending: 125.75 },
      ])
    })

    it('should handle single month data', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: [{ month: '2025-12', estimated: 99.99 }],
          currency: 'EUR',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { month: string; spending: number }[] }
      expect(vm.chartData).toHaveLength(1)
      expect(vm.chartData[0].month).toBe('Dec')
      expect(vm.chartData[0].spending).toBe(99.99)
    })

    it('should handle empty data array', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: [],
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { month: string; spending: number }[] }
      expect(vm.chartData).toEqual([])
    })

    it('should convert month correctly for all months', () => {
      const allMonthsData = [
        { month: '2025-01', estimated: 100 },
        { month: '2025-02', estimated: 100 },
        { month: '2025-03', estimated: 100 },
        { month: '2025-04', estimated: 100 },
        { month: '2025-05', estimated: 100 },
        { month: '2025-06', estimated: 100 },
        { month: '2025-07', estimated: 100 },
        { month: '2025-08', estimated: 100 },
        { month: '2025-09', estimated: 100 },
        { month: '2025-10', estimated: 100 },
        { month: '2025-11', estimated: 100 },
        { month: '2025-12', estimated: 100 },
      ]

      const wrapper = mount(SpendingChart, {
        props: {
          data: allMonthsData,
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { month: string; spending: number }[] }
      const expectedMonths = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

      vm.chartData.forEach((dataPoint, index) => {
        expect(dataPoint.month).toBe(expectedMonths[index])
      })
    })
  })

  describe('y-axis formatter', () => {
    it('should format y-axis values without currency code', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { yFormatter: (value: number | Date) => string }
      expect(vm.yFormatter(150)).toBe('150')
      expect(vm.yFormatter(99.99)).toBe('100')
      expect(vm.yFormatter(0)).toBe('0')
    })
  })

  describe('props handling', () => {
    it('should accept different currencies', () => {
      const currencies = ['USD', 'EUR', 'GBP', 'JPY']

      currencies.forEach(currency => {
        const wrapper = mount(SpendingChart, {
          props: {
            data: createMockData(),
            currency,
          },
        })
        expect(wrapper.exists()).toBe(true)
      })
    })

    it('should handle data with zero values', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: [
            { month: '2025-01', estimated: 0 },
            { month: '2025-02', estimated: 100 },
          ],
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { month: string; spending: number }[] }
      expect(vm.chartData[0].spending).toBe(0)
      expect(vm.chartData[1].spending).toBe(100)
    })

    it('should handle data with decimal values', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: [
            { month: '2025-01', estimated: 99.99 },
            { month: '2025-02', estimated: 123.456 },
          ],
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { month: string; spending: number }[] }
      expect(vm.chartData[0].spending).toBe(99.99)
      expect(vm.chartData[1].spending).toBe(123.456)
    })
  })
})
