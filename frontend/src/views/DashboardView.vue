<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useSubscriptionsStore } from '@/stores/subscriptions'
import { formatCurrency, formatDate, daysUntil } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import SummaryCards from '@/components/dashboard/SummaryCards.vue'
import CategoryChart from '@/components/dashboard/CategoryChart.vue'

const router = useRouter()
const dashboardStore = useDashboardStore()
const subscriptionsStore = useSubscriptionsStore()

onMounted(async () => {
  await Promise.all([
    dashboardStore.fetchSummary(),
    dashboardStore.fetchProjection(),
    subscriptionsStore.fetchSubscriptions(1, 5),
  ])
})

const upcomingSubscriptions = computed(() => {
  return subscriptionsStore.subscriptions
    .filter(s => s.status === 'active')
    .sort((a, b) => new Date(a.nextBillingDate).getTime() - new Date(b.nextBillingDate).getTime())
    .slice(0, 5)
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p class="text-gray-600">Overview of your subscriptions</p>
      </div>
      <div class="flex space-x-3">
        <router-link to="/subscriptions/add" class="btn-primary">
          + Add Subscription
        </router-link>
        <router-link to="/statistics" class="btn-secondary">
          View Reports
        </router-link>
      </div>
    </div>

    <div v-if="dashboardStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <template v-else>
      <!-- Summary Cards -->
      <SummaryCards
        v-if="dashboardStore.summary"
        :summary="dashboardStore.summary.summary"
        :projection="dashboardStore.projection?.projection"
      />

      <div class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Upcoming Payments -->
        <div class="card">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-lg font-semibold text-gray-900">Upcoming Payments</h2>
            <router-link to="/subscriptions" class="text-primary-600 hover:text-primary-800 text-sm">
              View all
            </router-link>
          </div>

          <div v-if="upcomingSubscriptions.length === 0" class="text-center py-8 text-gray-500">
            No upcoming payments
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="subscription in upcomingSubscriptions"
              :key="subscription.id"
              class="flex items-center justify-between p-3 bg-gray-50 rounded-lg cursor-pointer hover:bg-gray-100"
              @click="router.push(`/subscriptions/${subscription.id}`)"
            >
              <div class="flex items-center space-x-3">
                <div v-if="subscription.service.faviconUrl" class="w-10 h-10">
                  <img
                    :src="subscription.service.faviconUrl"
                    :alt="subscription.service.name"
                    class="w-full h-full object-contain rounded"
                  />
                </div>
                <div v-else class="w-10 h-10 bg-primary-100 rounded flex items-center justify-center">
                  <span class="text-primary-600 font-semibold">{{ subscription.service.name.charAt(0) }}</span>
                </div>
                <div>
                  <p class="font-medium text-gray-900">{{ subscription.service.name }}</p>
                  <p class="text-sm text-gray-500">{{ formatDate(subscription.nextBillingDate) }}</p>
                </div>
              </div>
              <div class="text-right">
                <p class="font-semibold">{{ formatCurrency(subscription.amount, subscription.currencyCode) }}</p>
                <p
                  :class="[
                    'text-sm',
                    daysUntil(subscription.nextBillingDate) <= 3 ? 'text-orange-600' : 'text-gray-500'
                  ]"
                >
                  {{ daysUntil(subscription.nextBillingDate) === 0 ? 'Today' : `in ${daysUntil(subscription.nextBillingDate)} days` }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Category Breakdown -->
        <div class="card">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Spending by Category</h2>
          <CategoryChart
            v-if="dashboardStore.summary && dashboardStore.summary.byCategory.length > 0"
            :data="dashboardStore.summary.byCategory"
            :currency="dashboardStore.summary.summary.currency"
          />
          <div v-else class="text-center py-8 text-gray-500">
            No spending data available
          </div>
        </div>
      </div>

      <!-- Projection -->
      <div v-if="dashboardStore.projection" class="mt-6 card">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">{{ dashboardStore.projection.year }} Projection</h2>
        <p class="text-gray-600 mb-4">
          Based on your {{ dashboardStore.projection.projection.activeSubscriptions }} active subscription(s),
          your estimated total for {{ dashboardStore.projection.year }} is:
        </p>
        <p class="text-3xl font-bold text-primary-600">
          {{ formatCurrency(dashboardStore.projection.projection.estimatedTotal, dashboardStore.projection.baseCurrency) }}
        </p>
        <p class="text-sm text-gray-500 mt-2">
          {{ dashboardStore.projection.projection.assumptions }}
        </p>
      </div>
    </template>
  </div>
</template>
