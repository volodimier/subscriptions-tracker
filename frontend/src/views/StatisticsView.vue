<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { formatCurrency, formatDateISO } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import SummaryCards from '@/components/dashboard/SummaryCards.vue'
import CategoryChart from '@/components/dashboard/CategoryChart.vue'
import SpendingChart from '@/components/dashboard/SpendingChart.vue'

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
    case 'last3Months':
      const threeMonthsAgo = new Date(today)
      threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3)
      startDate.value = formatDateISO(threeMonthsAgo)
      endDate.value = formatDateISO(today)
      break
    case 'thisYear':
      startDate.value = formatDateISO(new Date(thisYear, 0, 1))
      endDate.value = formatDateISO(today)
      break
    case 'allTime':
      startDate.value = formatDateISO(new Date(2020, 0, 1))
      endDate.value = formatDateISO(today)
      break
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
    <div class="card mb-6">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center space-x-2">
          <label class="text-sm text-gray-600">From:</label>
          <input v-model="startDate" type="date" class="input w-auto" />
        </div>
        <div class="flex items-center space-x-2">
          <label class="text-sm text-gray-600">To:</label>
          <input v-model="endDate" type="date" class="input w-auto" />
        </div>
        <div class="flex space-x-2">
          <button @click="setPreset('thisMonth')" class="btn-secondary text-sm">This Month</button>
          <button @click="setPreset('last3Months')" class="btn-secondary text-sm">Last 3 Months</button>
          <button @click="setPreset('thisYear')" class="btn-secondary text-sm">This Year</button>
          <button @click="setPreset('allTime')" class="btn-secondary text-sm">All Time</button>
        </div>
      </div>
    </div>

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
        <div class="card">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Spending by Category</h2>
          <CategoryChart
            v-if="dashboardStore.summary.byCategory.length > 0"
            :data="dashboardStore.summary.byCategory"
            :currency="dashboardStore.summary.summary.currency"
          />
          <div v-else class="text-center py-8 text-gray-500">
            No spending data available for this period
          </div>
        </div>

        <!-- Spending Over Time -->
        <div class="card">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Monthly Spending</h2>
          <SpendingChart
            v-if="dashboardStore.projection"
            :data="dashboardStore.projection.monthlyBreakdown"
            :currency="dashboardStore.projection.baseCurrency"
          />
          <div v-else class="text-center py-8 text-gray-500">
            No projection data available
          </div>
        </div>
      </div>

      <!-- Projection Card -->
      <div v-if="dashboardStore.projection" class="mt-6 card">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">
          {{ dashboardStore.projection.year }} Projection
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <p class="text-sm text-gray-500">Estimated Total</p>
            <p class="text-2xl font-bold text-primary-600">
              {{ formatCurrency(dashboardStore.projection.projection.estimatedTotal, dashboardStore.projection.baseCurrency) }}
            </p>
          </div>
          <div>
            <p class="text-sm text-gray-500">Active Subscriptions</p>
            <p class="text-2xl font-bold text-gray-900">
              {{ dashboardStore.projection.projection.activeSubscriptions }}
            </p>
          </div>
          <div>
            <p class="text-sm text-gray-500">Monthly Average</p>
            <p class="text-2xl font-bold text-gray-900">
              {{ formatCurrency(dashboardStore.projection.projection.estimatedTotal / 12, dashboardStore.projection.baseCurrency) }}
            </p>
          </div>
        </div>
        <p class="text-sm text-gray-500 mt-4">
          {{ dashboardStore.projection.projection.assumptions }}
        </p>
      </div>
    </template>
  </div>
</template>
