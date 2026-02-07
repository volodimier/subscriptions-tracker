<script setup lang="ts">
import { computed, defineComponent, h } from 'vue'
import { AreaChart } from '@/components/ui/chart-area'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { formatCurrency } from '@/utils/formatters'

const props = defineProps<{
  data: { month: string; estimated: number }[]
  currency: string
}>()

interface ChartDataPoint {
  month: string
  spending: number
  [key: string]: string | number
}

const chartData = computed<ChartDataPoint[]>(() => {
  return props.data.map(d => {
    const [year, month] = d.month.split('-')
    const monthLabel = new Date(parseInt(year), parseInt(month) - 1).toLocaleDateString('en-US', { month: 'short' })
    return {
      month: monthLabel,
      spending: d.estimated,
    }
  })
})

function yFormatter(value: number | Date): string {
  const numValue = typeof value === 'number' ? value : value.getTime()
  // Format without currency code for cleaner Y-axis display
  return numValue.toFixed(0)
}

// Custom tooltip component that formats values with currency
const CustomTooltip = defineComponent({
  props: {
    title: { type: String, default: '' },
    data: { type: Array, default: () => [] },
  },
  setup(tooltipProps) {
    return () => h(Card, { class: 'text-sm' }, () => [
      tooltipProps.title && h(CardHeader, { class: 'p-3 border-b' }, () =>
        h(CardTitle, null, () => tooltipProps.title)
      ),
      h(CardContent, { class: 'p-3 min-w-[140px] flex flex-col gap-1' }, () =>
        (tooltipProps.data as Array<{ name: string; color: string; value: number }>).map((item) =>
          h('div', { key: item.name, class: 'flex justify-between items-center' }, [
            h('div', { class: 'flex items-center' }, [
              h('span', {
                class: 'w-2.5 h-2.5 rounded-full mr-2',
                style: { backgroundColor: item.color }
              }),
              h('span', null, 'Spending'),
            ]),
            h('span', { class: 'font-semibold ml-4' }, formatCurrency(item.value, props.currency)),
          ])
        )
      ),
    ])
  }
})
</script>

<template>
  <div class="h-64">
    <AreaChart
      :data="chartData"
      index="month"
      :categories="['spending']"
      :colors="['hsl(var(--primary))']"
      :y-formatter="yFormatter"
      :show-legend="false"
      :show-gradient="true"
      :custom-tooltip="CustomTooltip"
      class="h-full"
    />
  </div>
</template>
