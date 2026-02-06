<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useSubscriptionsStore } from '@/stores/subscriptions'
import { formatCurrency, formatDate, daysUntil } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import SummaryCards from '@/components/dashboard/SummaryCards.vue'
import CategoryChart from '@/components/dashboard/CategoryChart.vue'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Plus, FileBarChart, ArrowRight } from 'lucide-vue-next'

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
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p class="text-muted-foreground">Overview of your subscriptions</p>
      </div>
      <div class="flex gap-3">
        <Button as-child>
          <router-link to="/subscriptions/add">
            <Plus class="mr-2 h-4 w-4" />
            Add Subscription
          </router-link>
        </Button>
        <Button variant="outline" as-child>
          <router-link to="/statistics">
            <FileBarChart class="mr-2 h-4 w-4" />
            View Reports
          </router-link>
        </Button>
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

      <div class="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-4">
        <!-- Upcoming Payments -->
        <Card>
          <CardHeader class="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Upcoming Payments</CardTitle>
              <CardDescription>Your next scheduled payments</CardDescription>
            </div>
            <Button variant="ghost" size="sm" as-child>
              <router-link to="/subscriptions" class="text-sm">
                View all
                <ArrowRight class="ml-1 h-4 w-4" />
              </router-link>
            </Button>
          </CardHeader>
          <CardContent>
            <div v-if="upcomingSubscriptions.length === 0" class="text-center py-8 text-muted-foreground">
              No upcoming payments
            </div>

            <div v-else class="space-y-3">
              <div
                v-for="subscription in upcomingSubscriptions"
                :key="subscription.id"
                class="flex items-center justify-between p-3 rounded-lg cursor-pointer hover:bg-accent transition-colors"
                @click="router.push(`/subscriptions/${subscription.id}`)"
              >
                <div class="flex items-center gap-3">
                  <div v-if="subscription.service.faviconUrl" class="w-10 h-10">
                    <img
                      :src="subscription.service.faviconUrl"
                      :alt="subscription.service.name"
                      class="w-full h-full object-contain rounded"
                    />
                  </div>
                  <div v-else class="w-10 h-10 bg-primary/10 rounded flex items-center justify-center">
                    <span class="text-primary font-semibold">{{ subscription.service.name.charAt(0) }}</span>
                  </div>
                  <div>
                    <p class="font-medium">{{ subscription.service.name }}</p>
                    <p class="text-sm text-muted-foreground">{{ formatDate(subscription.nextBillingDate) }}</p>
                  </div>
                </div>
                <div class="text-right">
                  <p class="font-semibold">{{ formatCurrency(subscription.amount, subscription.currencyCode) }}</p>
                  <p
                    :class="[
                      'text-sm',
                      daysUntil(subscription.nextBillingDate) <= 3 ? 'text-orange-600' : 'text-muted-foreground'
                    ]"
                  >
                    {{ daysUntil(subscription.nextBillingDate) === 0 ? 'Today' : `in ${daysUntil(subscription.nextBillingDate)} days` }}
                  </p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- Category Breakdown -->
        <Card>
          <CardHeader>
            <CardTitle>Spending by Category</CardTitle>
            <CardDescription>How your subscriptions are distributed</CardDescription>
          </CardHeader>
          <CardContent>
            <CategoryChart
              v-if="dashboardStore.summary && dashboardStore.summary.byCategory.length > 0"
              :data="dashboardStore.summary.byCategory"
              :currency="dashboardStore.summary.summary.currency"
            />
            <div v-else class="text-center py-8 text-muted-foreground">
              No spending data available
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- Projection -->
      <Card v-if="dashboardStore.projection" class="mt-4">
        <CardHeader>
          <CardTitle>{{ dashboardStore.projection.year }} Projection</CardTitle>
          <CardDescription>
            Based on your {{ dashboardStore.projection.projection.activeSubscriptions }} active subscription(s)
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p class="text-3xl font-bold text-primary">
            {{ formatCurrency(dashboardStore.projection.projection.estimatedTotal, dashboardStore.projection.baseCurrency) }}
          </p>
          <p class="text-sm text-muted-foreground mt-2">
            {{ dashboardStore.projection.projection.assumptions }}
          </p>
        </CardContent>
      </Card>
    </template>
  </div>
</template>
