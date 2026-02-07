<script setup lang="ts">
import type { DashboardSummary, Projection } from '@/types'
import { formatCurrency } from '@/utils/formatters'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Coins, TrendingUp, CreditCard, XCircle } from 'lucide-vue-next'

defineProps<{
  summary: DashboardSummary['summary']
  projection?: Projection['projection']
}>()
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
    <Card>
      <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground">
          Monthly Burn Rate
        </CardTitle>
        <Coins class="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">
          {{ formatCurrency(summary.monthlyAverage, summary.currency) }}
        </div>
      </CardContent>
    </Card>

    <Card>
      <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground">
          Total Spent
        </CardTitle>
        <TrendingUp class="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">
          {{ formatCurrency(summary.totalSpent, summary.currency) }}
        </div>
        <p v-if="projection" class="text-xs text-muted-foreground">
          Projected: {{ formatCurrency(projection.estimatedTotal, summary.currency) }}
        </p>
      </CardContent>
    </Card>

    <Card>
      <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground">
          Active Subscriptions
        </CardTitle>
        <CreditCard class="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold text-green-600">
          {{ summary.activeSubscriptions }}
        </div>
      </CardContent>
    </Card>

    <Card>
      <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground">
          Cancelled
        </CardTitle>
        <XCircle class="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold text-muted-foreground">
          {{ summary.cancelledSubscriptions }}
        </div>
      </CardContent>
    </Card>
  </div>
</template>
