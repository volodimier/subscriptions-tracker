<script setup lang="ts">
import { computed } from 'vue'
import { Pie } from 'vue-chartjs'
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js'
import type { CategoryBreakdown } from '@/types'
import { formatCurrency } from '@/utils/formatters'

ChartJS.register(ArcElement, Tooltip, Legend)

const props = defineProps<{
  data: CategoryBreakdown[]
  currency: string
}>()

const chartData = computed(() => {
  const colors = [
    '#3B82F6', // blue
    '#10B981', // green
    '#F59E0B', // yellow
    '#EF4444', // red
    '#8B5CF6', // purple
    '#EC4899', // pink
    '#14B8A6', // teal
    '#F97316', // orange
  ]

  return {
    labels: props.data.map(d => d.category || 'Uncategorized'),
    datasets: [
      {
        data: props.data.map(d => d.total),
        backgroundColor: colors.slice(0, props.data.length),
        borderWidth: 0,
      },
    ],
  }
})

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'right' as const,
      labels: {
        usePointStyle: true,
        padding: 15,
      },
    },
    tooltip: {
      callbacks: {
        label: (context: { label: string; raw: number }) => {
          return `${context.label}: ${formatCurrency(context.raw, props.currency)}`
        },
      },
    },
  },
}))
</script>

<template>
  <div class="flex">
    <div class="w-1/2 h-64">
      <Pie :data="chartData" :options="chartOptions" />
    </div>
    <div class="w-1/2 pl-4">
      <div class="space-y-2">
        <div
          v-for="(item, index) in data"
          :key="item.category"
          class="flex justify-between items-center text-sm"
        >
          <div class="flex items-center">
            <span
              class="w-3 h-3 rounded-full mr-2"
              :style="{ backgroundColor: ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6', '#F97316'][index] }"
            ></span>
            <span class="text-gray-700">{{ item.category || 'Uncategorized' }}</span>
          </div>
          <div class="text-right">
            <span class="font-medium">{{ formatCurrency(item.total, currency) }}</span>
            <span class="text-gray-500 ml-2">({{ item.percentage.toFixed(1) }}%)</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
