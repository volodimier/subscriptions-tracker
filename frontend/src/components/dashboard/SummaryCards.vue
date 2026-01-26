<script setup lang="ts">
import type { DashboardSummary, Projection } from '@/types'
import { formatCurrency } from '@/utils/formatters'

defineProps<{
  summary: DashboardSummary['summary']
  projection?: Projection['projection']
}>()
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
    <div class="card">
      <p class="text-sm font-medium text-gray-500 tracking-wide">Monthly Burn Rate</p>
      <p class="mt-2 text-3xl font-semibold text-gray-900 tracking-tight">
        {{ formatCurrency(summary.monthlyAverage, summary.currency) }}
      </p>
      <p class="mt-1 text-sm text-gray-400">in {{ summary.currency }}</p>
    </div>

    <div class="card">
      <p class="text-sm font-medium text-gray-500 tracking-wide">Total Spent</p>
      <p class="mt-2 text-3xl font-semibold text-gray-900 tracking-tight">
        {{ formatCurrency(summary.totalSpent, summary.currency) }}
      </p>
      <p v-if="projection" class="mt-1 text-sm text-gray-400">
        Projected: {{ formatCurrency(projection.estimatedTotal, summary.currency) }}
      </p>
    </div>

    <div class="card">
      <p class="text-sm font-medium text-gray-500 tracking-wide">Active Subscriptions</p>
      <p class="mt-2 text-3xl font-semibold text-green-600 tracking-tight">
        {{ summary.activeSubscriptions }}
      </p>
    </div>

    <div class="card">
      <p class="text-sm font-medium text-gray-500 tracking-wide">Cancelled</p>
      <p class="mt-2 text-3xl font-semibold text-gray-400 tracking-tight">
        {{ summary.cancelledSubscriptions }}
      </p>
    </div>
  </div>
</template>
