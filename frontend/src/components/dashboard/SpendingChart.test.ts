import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, type PropType, type Component } from 'vue'

// Mock the shadcn-vue AreaChart component to avoid Unovis rendering issues in jsdom
vi.mock('@/components/ui/chart-area', () => ({
  AreaChart: defineComponent({
    name: 'MockAreaChart',
    props: {
      data: { type: Array as PropType<Record<string, unknown>[]>, required: true },
      categories: { type: Array as PropType<string[]>, required: true },
      index: { type: String, required: true },
      colors: { type: Array as PropType<string[]>, default: () => [] },
      margin: { type: Object, default: () => ({}) },
      filterOpacity: { type: Number, default: 0.2 },
      xFormatter: { type: Function, default: undefined },
      yFormatter: { type: Function, default: undefined },
      showXAxis: { type: Boolean, default: true },
      showYAxis: { type: Boolean, default: true },
      showTooltip: { type: Boolean, default: true },
      showLegend: { type: Boolean, default: true },
      showGridLine: { type: Boolean, default: true },
      showGradient: { type: Boolean, default: true },
      customTooltip: { type: Object as PropType<Component>, default: undefined },
      curveType: { type: String, default: undefined },
    },
    setup(props) {
      // Store props for test assertions - use global variable
      ;(globalThis as Record<string, unknown>).__mockAreaChartProps = { ...props }

      return () => h('div', {
        'data-testid': 'mock-area-chart',
        'data-index': props.index,
        'data-categories': JSON.stringify(props.categories),
        'data-data-length': props.data.length,
      }, [
        h('span', { class: 'mock-chart-label' }, 'AreaChart Mock'),
      ])
    },
  }),
}))

// Import after mocking
import SpendingChart from './SpendingChart.vue'

// Helper to get stored mock props
function getMockAreaChartProps(): Record<string, unknown> | null {
  return (globalThis as Record<string, unknown>).__mockAreaChartProps as Record<string, unknown> | null
}

function resetMockAreaChartProps(): void {
  ;(globalThis as Record<string, unknown>).__mockAreaChartProps = null
}

describe('SpendingChart', () => {
  const createMockData = () => [
    { month: '2025-01', estimated: 150.00 },
    { month: '2025-02', estimated: 175.50 },
    { month: '2025-03', estimated: 200.00 },
    { month: '2025-04', estimated: 125.75 },
  ]

  beforeEach(() => {
    resetMockAreaChartProps()
  })

  afterEach(() => {
    resetMockAreaChartProps()
  })

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

    it('should render the mocked AreaChart component', () => {
      const wrapper = mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })
      expect(wrapper.find('[data-testid="mock-area-chart"]').exists()).toBe(true)
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

  describe('chart props verification', () => {
    it('should pass correct data to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps).not.toBeNull()
      expect(mockProps?.data).toEqual([
        { month: 'Jan', spending: 150.00 },
        { month: 'Feb', spending: 175.50 },
        { month: 'Mar', spending: 200.00 },
        { month: 'Apr', spending: 125.75 },
      ])
    })

    it('should pass correct index prop to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps?.index).toBe('month')
    })

    it('should pass correct categories to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps?.categories).toEqual(['spending'])
    })

    it('should pass showLegend as false to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps?.showLegend).toBe(false)
    })

    it('should pass showGradient as true to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps?.showGradient).toBe(true)
    })

    it('should pass primary color to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps?.colors).toEqual(['hsl(var(--primary))'])
    })

    it('should pass custom tooltip component to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps?.customTooltip).toBeDefined()
    })

    it('should pass yFormatter function to AreaChart', () => {
      mount(SpendingChart, {
        props: {
          data: createMockData(),
          currency: 'USD',
        },
      })

      const mockProps = getMockAreaChartProps()
      expect(mockProps?.yFormatter).toBeDefined()
      expect(typeof mockProps?.yFormatter).toBe('function')
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
