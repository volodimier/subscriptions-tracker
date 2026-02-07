<script setup lang="ts">
import { computed } from 'vue'
import { DonutChart } from '@/components/ui/chart-donut'
import type { CategoryBreakdown } from '@/types'
import { formatCurrency } from '@/utils/formatters'

const props = defineProps<{
  data: CategoryBreakdown[]
  currency: string
}>()

const colors = [
  'hsl(var(--primary))',
  'hsl(142.1 76.2% 36.3%)', // green
  'hsl(38.3 92% 50.2%)', // yellow/amber
  'hsl(0 84.2% 60.2%)', // red
  'hsl(262.1 83.3% 57.8%)', // purple
  'hsl(330.4 81.2% 60.4%)', // pink
  'hsl(173.4 80.4% 40%)', // teal
  'hsl(20.5 90.2% 48.2%)', // orange
]

interface ChartDataPoint {
  category: string
  total: number
  [key: string]: string | number
}

const chartData = computed<ChartDataPoint[]>(() => {
  return props.data.map(d => ({
    category: d.category || 'Uncategorized',
    total: d.total,
  }))
})

const chartColors = computed(() => colors.slice(0, props.data.length))

function valueFormatter(value: number): string {
  return formatCurrency(value, props.currency)
}
</script>

<template>
  <div class="flex flex-col md:flex-row gap-4">
    <div class="w-full md:w-1/2 h-64">
      <DonutChart
        :data="chartData"
        index="category"
        category="total"
        :colors="chartColors"
        :value-formatter="valueFormatter"
        :show-legend="false"
        type="pie"
        class="h-full"
      />
    </div>
    <div class="w-full md:w-1/2">
      <div class="space-y-2">
        <div
          v-for="(item, index) in data"
          :key="item.category || 'uncategorized'"
          class="flex justify-between items-center text-sm"
        >
          <div class="flex items-center">
            <span
              class="w-3 h-3 rounded-full mr-2 shrink-0"
              :style="{ backgroundColor: chartColors[index] }"
            />
            <span class="text-foreground truncate">{{ item.category || 'Uncategorized' }}</span>
          </div>
          <div class="text-right shrink-0 ml-2">
            <span class="font-medium">{{ formatCurrency(item.total, currency) }}</span>
            <span class="text-muted-foreground ml-2">({{ item.percentage.toFixed(1) }}%)</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
