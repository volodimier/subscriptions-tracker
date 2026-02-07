/**
 * Chart Mock Utilities
 *
 * This file provides type definitions and utilities for mocking shadcn-vue chart components
 * in unit tests.
 *
 * IMPORTANT: Due to Vitest's mock hoisting behavior, you cannot import the mock components
 * directly and use them in vi.mock(). Instead, you must define the mock inline within the
 * vi.mock() factory function.
 *
 * Example usage in test files:
 *
 * ```typescript
 * import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
 * import { mount } from '@vue/test-utils'
 * import { defineComponent, h, type PropType } from 'vue'
 *
 * // Define mock inline in vi.mock() to avoid hoisting issues
 * vi.mock('@/components/ui/chart-area', () => ({
 *   AreaChart: defineComponent({
 *     name: 'MockAreaChart',
 *     props: {
 *       data: { type: Array, required: true },
 *       categories: { type: Array, required: true },
 *       index: { type: String, required: true },
 *       // ... other props
 *     },
 *     setup(props) {
 *       // Store props for assertions
 *       (globalThis as any).__mockAreaChartProps = { ...props }
 *       return () => h('div', { 'data-testid': 'mock-area-chart' })
 *     },
 *   }),
 * }))
 *
 * // Then import the component under test
 * import SpendingChart from './SpendingChart.vue'
 * ```
 */

import type { Component } from 'vue'

/**
 * Mock props interface for AreaChart component
 * Based on @/components/ui/chart-area/AreaChart.vue
 */
export interface MockAreaChartProps {
  data: Record<string, unknown>[]
  categories: string[]
  index: string
  colors?: string[]
  margin?: { top?: number; bottom?: number; left?: number; right?: number }
  filterOpacity?: number
  xFormatter?: (tick: number | Date, i: number, ticks: number[] | Date[]) => string
  yFormatter?: (tick: number | Date, i: number, ticks: number[] | Date[]) => string
  showXAxis?: boolean
  showYAxis?: boolean
  showTooltip?: boolean
  showLegend?: boolean
  showGridLine?: boolean
  showGradient?: boolean
  customTooltip?: Component
  curveType?: string
}

/**
 * Mock props interface for DonutChart component
 * Based on @/components/ui/chart-donut/DonutChart.vue
 */
export interface MockDonutChartProps {
  data: Record<string, unknown>[]
  index: string
  category: string
  colors?: string[]
  margin?: { top?: number; bottom?: number; left?: number; right?: number }
  filterOpacity?: number
  showTooltip?: boolean
  showLegend?: boolean
  type?: 'donut' | 'pie'
  sortFunction?: (a: Record<string, unknown>, b: Record<string, unknown>) => number | undefined
  valueFormatter?: (tick: number, i?: number, ticks?: number[]) => string
  customTooltip?: Component
}

/**
 * Mock props interface for LineChart component
 * Based on @/components/ui/chart-line/LineChart.vue
 */
export interface MockLineChartProps {
  data: Record<string, unknown>[]
  categories: string[]
  index: string
  colors?: string[]
  margin?: { top?: number; bottom?: number; left?: number; right?: number }
  filterOpacity?: number
  xFormatter?: (tick: number | Date, i: number, ticks: number[] | Date[]) => string
  yFormatter?: (tick: number | Date, i: number, ticks: number[] | Date[]) => string
  showXAxis?: boolean
  showYAxis?: boolean
  showTooltip?: boolean
  showLegend?: boolean
  showGridLine?: boolean
  customTooltip?: Component
  curveType?: string
}

/**
 * Global keys used to store mock props during tests.
 * Access via (globalThis as any).__mockAreaChartProps, etc.
 */
export const MOCK_PROP_KEYS = {
  AREA_CHART: '__mockAreaChartProps',
  DONUT_CHART: '__mockDonutChartProps',
  LINE_CHART: '__mockLineChartProps',
} as const

/**
 * Helper to get stored mock props for AreaChart
 */
export function getMockAreaChartProps(): MockAreaChartProps | null {
  return (globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.AREA_CHART] as MockAreaChartProps | null
}

/**
 * Helper to get stored mock props for DonutChart
 */
export function getMockDonutChartProps(): MockDonutChartProps | null {
  return (globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.DONUT_CHART] as MockDonutChartProps | null
}

/**
 * Helper to get stored mock props for LineChart
 */
export function getMockLineChartProps(): MockLineChartProps | null {
  return (globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.LINE_CHART] as MockLineChartProps | null
}

/**
 * Reset all stored mock props - call in beforeEach/afterEach
 */
export function resetAllMockChartProps(): void {
  ;(globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.AREA_CHART] = null
  ;(globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.DONUT_CHART] = null
  ;(globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.LINE_CHART] = null
}

/**
 * Reset mock props for AreaChart
 */
export function resetMockAreaChartProps(): void {
  ;(globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.AREA_CHART] = null
}

/**
 * Reset mock props for DonutChart
 */
export function resetMockDonutChartProps(): void {
  ;(globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.DONUT_CHART] = null
}

/**
 * Reset mock props for LineChart
 */
export function resetMockLineChartProps(): void {
  ;(globalThis as Record<string, unknown>)[MOCK_PROP_KEYS.LINE_CHART] = null
}
