<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { formatCurrency, formatDateISO } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import SummaryCards from '@/components/dashboard/SummaryCards.vue'
import CategoryChart from '@/components/dashboard/CategoryChart.vue'
import SpendingChart from '@/components/dashboard/SpendingChart.vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { DatePicker } from '@/components/ui/date-picker'

const dashboardStore = useDashboardStore()

const startDate = ref(formatDateISO(new Date(new Date().getFullYear(), 0, 1)))
const endDate = ref(formatDateISO(new Date()))

onMounted(async () => {
  await fetchData()
})

watch([startDate, endDate], async () => {
  await fetchData()
})

async function fetchData() {
  await Promise.all([
    dashboardStore.fetchSummary(startDate.value, endDate.value),
    dashboardStore.fetchProjection(),
  ])
}

function setPreset(preset: string) {
  const today = new Date()
  const thisYear = today.getFullYear()

  switch (preset) {
    case 'thisMonth':
      startDate.value = formatDateISO(new Date(thisYear, today.getMonth(), 1))
      endDate.value = formatDateISO(today)
      break
    case 'last3Months': {
      const threeMonthsAgo = new Date(today)
      threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3)
      startDate.value = formatDateISO(threeMonthsAgo)
      endDate.value = formatDateISO(today)
      break
    }
    case 'thisYear':
      startDate.value = formatDateISO(new Date(thisYear, 0, 1))
      endDate.value = formatDateISO(today)
      break
    case 'allTime': {
      const earliest = dashboardStore.summary?.earliestSubscriptionDate
      if (earliest) {
        startDate.value = earliest
      } else {
        // Fallback to a far-back date if no earliest date is available
        startDate.value = formatDateISO(new Date(2000, 0, 1))
      }
      endDate.value = formatDateISO(today)
      break
    }
  }
}
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Statistics & Reports</h1>
        <p class="text-gray-600">Analyze your subscription spending</p>
      </div>
    </div>

    <!-- Date Range Selector -->
    <Card class="mb-6">
      <CardContent class="pt-6">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center space-x-2">
            <Label class="text-sm text-muted-foreground">From:</Label>
            <DatePicker v-model="startDate" placeholder="Start date" class="w-auto" />
          </div>
          <div class="flex items-center space-x-2">
            <Label class="text-sm text-muted-foreground">To:</Label>
            <DatePicker v-model="endDate" placeholder="End date" class="w-auto" />
          </div>
          <div class="flex space-x-2">
            <Button variant="outline" size="sm" @click="setPreset('thisMonth')">This Month</Button>
            <Button variant="outline" size="sm" @click="setPreset('last3Months')">Last 3 Months</Button>
            <Button variant="outline" size="sm" @click="setPreset('thisYear')">This Year</Button>
            <Button variant="outline" size="sm" @click="setPreset('allTime')">All Time</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <div v-if="dashboardStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <template v-else-if="dashboardStore.summary">
      <!-- Summary Cards -->
      <SummaryCards
        :summary="dashboardStore.summary.summary"
        :projection="dashboardStore.projection?.projection"
      />

      <div class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Category Breakdown -->
        <Card>
          <CardHeader>
            <CardTitle>Spending by Category</CardTitle>
          </CardHeader>
          <CardContent>
            <CategoryChart
              v-if="dashboardStore.summary.byCategory.length > 0"
              :data="dashboardStore.summary.byCategory"
              :currency="dashboardStore.summary.summary.currency"
            />
            <div v-else class="text-center py-8 text-muted-foreground">
              No spending data available for this period
            </div>
          </CardContent>
        </Card>

        <!-- Spending Over Time -->
        <Card>
          <CardHeader>
            <CardTitle>Monthly Spending</CardTitle>
          </CardHeader>
          <CardContent>
            <SpendingChart
              v-if="dashboardStore.projection"
              :data="dashboardStore.projection.monthlyBreakdown"
              :currency="dashboardStore.projection.baseCurrency"
            />
            <div v-else class="text-center py-8 text-muted-foreground">
              No projection data available
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- Projection Card -->
      <Card v-if="dashboardStore.projection" class="mt-6">
        <CardHeader>
          <CardTitle>{{ dashboardStore.projection.year }} Projection</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <p class="text-sm text-muted-foreground">Estimated Total</p>
              <p class="text-2xl font-bold text-primary">
                {{ formatCurrency(dashboardStore.projection.projection.estimatedTotal, dashboardStore.projection.baseCurrency) }}
              </p>
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Active Subscriptions</p>
              <p class="text-2xl font-bold text-gray-900">
                {{ dashboardStore.projection.projection.activeSubscriptions }}
              </p>
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Monthly Average</p>
              <p class="text-2xl font-bold text-gray-900">
                {{ formatCurrency(dashboardStore.projection.projection.estimatedTotal / 12, dashboardStore.projection.baseCurrency) }}
              </p>
            </div>
          </div>
          <p class="text-sm text-muted-foreground mt-4">
            {{ dashboardStore.projection.projection.assumptions }}
          </p>
        </CardContent>
      </Card>
    </template>
  </div>
</template>
