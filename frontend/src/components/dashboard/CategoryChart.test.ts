import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CategoryChart from './CategoryChart.vue'
import type { CategoryBreakdown } from '@/types'

// Mock the Unovis components to avoid rendering issues in tests
vi.mock('@unovis/vue', () => ({
  VisSingleContainer: { template: '<div><slot /></div>' },
  VisDonut: { template: '<div />' },
  VisTooltip: { template: '<div />' },
}))

vi.mock('@unovis/ts', () => ({
  Donut: { selectors: { segment: 'segment' } },
  omit: vi.fn((obj, keys) => {
    const result = { ...obj }
    keys.forEach((key: string) => delete result[key])
    return result
  }),
}))

describe('CategoryChart', () => {
  const createMockData = (): CategoryBreakdown[] => [
    { category: 'Entertainment', total: 45.99, percentage: 46, count: 3 },
    { category: 'Music', total: 29.99, percentage: 30, count: 2 },
    { category: 'Fitness', total: 24.01, percentage: 24, count: 1 },
  ]

  describe('rendering', () => {
    it('should render without crashing', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })
      expect(wrapper.exists()).toBe(true)
    })

    it('should render category legend items', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const legendItems = wrapper.findAll('.space-y-2 > div')
      expect(legendItems.length).toBe(3)
    })

    it('should display category names in legend', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      expect(wrapper.text()).toContain('Entertainment')
      expect(wrapper.text()).toContain('Music')
      expect(wrapper.text()).toContain('Fitness')
    })

    it('should display formatted amounts in legend', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      expect(wrapper.text()).toContain('45.99 USD')
      expect(wrapper.text()).toContain('29.99 USD')
      expect(wrapper.text()).toContain('24.01 USD')
    })

    it('should display percentages in legend', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      expect(wrapper.text()).toContain('(46.0%)')
      expect(wrapper.text()).toContain('(30.0%)')
      expect(wrapper.text()).toContain('(24.0%)')
    })
  })

  describe('data transformation', () => {
    it('should transform data correctly for chart', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { category: string; total: number }[] }
      expect(vm.chartData).toEqual([
        { category: 'Entertainment', total: 45.99 },
        { category: 'Music', total: 29.99 },
        { category: 'Fitness', total: 24.01 },
      ])
    })

    it('should handle empty category as Uncategorized', () => {
      const dataWithEmptyCategory: CategoryBreakdown[] = [
        { category: '', total: 50.00, percentage: 50, count: 2 },
        { category: 'Music', total: 50.00, percentage: 50, count: 1 },
      ]

      const wrapper = mount(CategoryChart, {
        props: {
          data: dataWithEmptyCategory,
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { category: string; total: number }[] }
      expect(vm.chartData[0].category).toBe('Uncategorized')
      expect(wrapper.text()).toContain('Uncategorized')
    })

    it('should handle single category', () => {
      const singleCategory: CategoryBreakdown[] = [
        { category: 'Entertainment', total: 100.00, percentage: 100, count: 5 },
      ]

      const wrapper = mount(CategoryChart, {
        props: {
          data: singleCategory,
          currency: 'EUR',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { category: string; total: number }[] }
      expect(vm.chartData).toHaveLength(1)
      expect(vm.chartData[0].category).toBe('Entertainment')
    })

    it('should handle empty data array', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: [],
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartData: { category: string; total: number }[] }
      expect(vm.chartData).toEqual([])
    })
  })

  describe('colors', () => {
    it('should generate correct number of colors', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartColors: string[] }
      expect(vm.chartColors).toHaveLength(3)
    })

    it('should handle more categories than predefined colors', () => {
      const manyCategories: CategoryBreakdown[] = Array.from({ length: 10 }, (_, i) => ({
        category: `Category ${i + 1}`,
        total: 10.00,
        percentage: 10,
        count: 1,
      }))

      const wrapper = mount(CategoryChart, {
        props: {
          data: manyCategories,
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { chartColors: string[] }
      // Should have colors up to the predefined limit (8 colors)
      expect(vm.chartColors.length).toBeLessThanOrEqual(10)
    })

    it('should apply colors to legend items', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const colorDots = wrapper.findAll('.rounded-full.w-3.h-3')
      expect(colorDots.length).toBe(3)

      // Each color dot should have a background color style
      colorDots.forEach(dot => {
        expect(dot.attributes('style')).toContain('background-color')
      })
    })
  })

  describe('value formatter', () => {
    it('should format values with currency', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const vm = wrapper.vm as unknown as { valueFormatter: (value: number) => string }
      expect(vm.valueFormatter(45.99)).toBe('45.99 USD')
      expect(vm.valueFormatter(100)).toBe('100.00 USD')
      expect(vm.valueFormatter(0)).toBe('0.00 USD')
    })

    it('should work with different currencies', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'EUR',
        },
      })

      const vm = wrapper.vm as unknown as { valueFormatter: (value: number) => string }
      expect(vm.valueFormatter(45.99)).toBe('45.99 EUR')
    })
  })

  describe('props handling', () => {
    it('should accept different currencies', () => {
      const currencies = ['USD', 'EUR', 'GBP', 'JPY']

      currencies.forEach(currency => {
        const wrapper = mount(CategoryChart, {
          props: {
            data: createMockData(),
            currency,
          },
        })
        expect(wrapper.exists()).toBe(true)
        expect(wrapper.text()).toContain(currency)
      })
    })

    it('should handle data with zero values', () => {
      const dataWithZero: CategoryBreakdown[] = [
        { category: 'Empty', total: 0, percentage: 0, count: 0 },
        { category: 'Music', total: 100, percentage: 100, count: 1 },
      ]

      const wrapper = mount(CategoryChart, {
        props: {
          data: dataWithZero,
          currency: 'USD',
        },
      })

      expect(wrapper.text()).toContain('0.00 USD')
    })

    it('should handle decimal percentages', () => {
      const dataWithDecimalPercentage: CategoryBreakdown[] = [
        { category: 'Test', total: 33.33, percentage: 33.333, count: 1 },
      ]

      const wrapper = mount(CategoryChart, {
        props: {
          data: dataWithDecimalPercentage,
          currency: 'USD',
        },
      })

      expect(wrapper.text()).toContain('(33.3%)')
    })
  })

  describe('responsive layout', () => {
    it('should have flex layout', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const container = wrapper.find('.flex')
      expect(container.exists()).toBe(true)
    })

    it('should have chart and legend sections', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      // Chart section
      expect(wrapper.find('.h-64').exists()).toBe(true)
      // Legend section with space-y-2
      expect(wrapper.find('.space-y-2').exists()).toBe(true)
    })
  })
})
