import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, type PropType, type Component } from 'vue'
import type { CategoryBreakdown } from '@/types'

// Mock the shadcn-vue DonutChart component to avoid Unovis rendering issues in jsdom
vi.mock('@/components/ui/chart-donut', () => ({
  DonutChart: defineComponent({
    name: 'MockDonutChart',
    props: {
      data: { type: Array as PropType<Record<string, unknown>[]>, required: true },
      index: { type: String, required: true },
      category: { type: String, required: true },
      colors: { type: Array as PropType<string[]>, default: () => [] },
      margin: { type: Object, default: () => ({}) },
      filterOpacity: { type: Number, default: 0.2 },
      showTooltip: { type: Boolean, default: true },
      showLegend: { type: Boolean, default: true },
      type: { type: String as PropType<'donut' | 'pie'>, default: 'donut' },
      sortFunction: { type: Function, default: () => undefined },
      valueFormatter: { type: Function, default: undefined },
      customTooltip: { type: Object as PropType<Component>, default: undefined },
    },
    setup(props) {
      // Store props for test assertions - use global variable
      ;(globalThis as Record<string, unknown>).__mockDonutChartProps = { ...props }

      return () => h('div', {
        'data-testid': 'mock-donut-chart',
        'data-index': props.index,
        'data-category': props.category,
        'data-type': props.type,
        'data-data-length': props.data.length,
      }, [
        h('span', { class: 'mock-chart-label' }, 'DonutChart Mock'),
      ])
    },
  }),
}))

// Import after mocking
import CategoryChart from './CategoryChart.vue'

// Helper to get stored mock props
function getMockDonutChartProps(): Record<string, unknown> | null {
  return (globalThis as Record<string, unknown>).__mockDonutChartProps as Record<string, unknown> | null
}

function resetMockDonutChartProps(): void {
  ;(globalThis as Record<string, unknown>).__mockDonutChartProps = null
}

describe('CategoryChart', () => {
  const createMockData = (): CategoryBreakdown[] => [
    { category: 'Entertainment', total: 45.99, percentage: 46, count: 3 },
    { category: 'Music', total: 29.99, percentage: 30, count: 2 },
    { category: 'Fitness', total: 24.01, percentage: 24, count: 1 },
  ]

  beforeEach(() => {
    resetMockDonutChartProps()
  })

  afterEach(() => {
    resetMockDonutChartProps()
  })

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

    it('should render the mocked DonutChart component', () => {
      const wrapper = mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })
      expect(wrapper.find('[data-testid="mock-donut-chart"]').exists()).toBe(true)
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

  describe('chart props verification', () => {
    it('should pass correct data to DonutChart', () => {
      mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockDonutChartProps()
      expect(mockProps).not.toBeNull()
      expect(mockProps?.data).toEqual([
        { category: 'Entertainment', total: 45.99 },
        { category: 'Music', total: 29.99 },
        { category: 'Fitness', total: 24.01 },
      ])
    })

    it('should pass correct index prop to DonutChart', () => {
      mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockDonutChartProps()
      expect(mockProps?.index).toBe('category')
    })

    it('should pass correct category prop to DonutChart', () => {
      mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockDonutChartProps()
      expect(mockProps?.category).toBe('total')
    })

    it('should pass type as pie to DonutChart', () => {
      mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockDonutChartProps()
      expect(mockProps?.type).toBe('pie')
    })

    it('should pass showLegend as false to DonutChart', () => {
      mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockDonutChartProps()
      expect(mockProps?.showLegend).toBe(false)
    })

    it('should pass correct colors to DonutChart', () => {
      mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockDonutChartProps()
      // Should have 3 colors for 3 categories
      expect((mockProps?.colors as string[])?.length).toBe(3)
      expect((mockProps?.colors as string[])?.[0]).toBe('hsl(var(--primary))')
    })

    it('should pass valueFormatter function to DonutChart', () => {
      mount(CategoryChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockDonutChartProps()
      expect(mockProps?.valueFormatter).toBeDefined()
      expect(typeof mockProps?.valueFormatter).toBe('function')
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
