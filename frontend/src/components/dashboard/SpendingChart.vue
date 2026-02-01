<script setup lang="ts">
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js'
import { formatCurrency } from '@/utils/formatters'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

const props = defineProps<{
  data: { month: string; estimated: number }[]
  currency: string
}>()

const chartData = computed(() => {
  return {
    labels: props.data.map(d => {
      const [year, month] = d.month.split('-')
      return new Date(parseInt(year), parseInt(month) - 1).toLocaleDateString('en-US', { month: 'short' })
    }),
    datasets: [
      {
        label: 'Estimated Spending',
        data: props.data.map(d => d.estimated),
        borderColor: '#3B82F6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        fill: true,
        tension: 0.4,
      },
    ],
  }
})

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      callbacks: {
        label: (context: { raw: unknown }) => {
          return formatCurrency(context.raw as number, props.currency)
        },
      },
    },
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: (value: string | number) => formatCurrency(Number(value), props.currency),
      },
    },
  },
}))
</script>

<template>
  <div class="h-64">
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>
