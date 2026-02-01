<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSubscriptionsStore } from '@/stores/subscriptions'
import type { Subscription, SubscriptionFilters } from '@/types'
import SubscriptionCard from '@/components/subscription/SubscriptionCard.vue'
import SubscriptionFiltersComponent from '@/components/subscription/SubscriptionFilters.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Pagination from '@/components/common/Pagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { formatDateISO } from '@/utils/formatters'

const router = useRouter()
const subscriptionsStore = useSubscriptionsStore()

const filters = ref<SubscriptionFilters>({
  status: 'active',
  sort: 'nextBillingDate',
  order: 'asc',
})

const showCancelDialog = ref(false)
const showReactivateDialog = ref(false)
const selectedSubscription = ref<Subscription | null>(null)
const reactivateDate = ref(formatDateISO(new Date()))

onMounted(async () => {
  subscriptionsStore.setFilters(filters.value)
  await subscriptionsStore.fetchSubscriptions()
})

watch(filters, async () => {
  subscriptionsStore.setFilters(filters.value)
  await subscriptionsStore.fetchSubscriptions(1)
}, { deep: true })

async function handlePageChange(page: number) {
  await subscriptionsStore.fetchSubscriptions(page)
}

function handleView(subscription: Subscription) {
  router.push(`/subscriptions/${subscription.id}`)
}

function handleEdit(subscription: Subscription) {
  router.push(`/subscriptions/${subscription.id}/edit`)
}

function handleCancelClick(subscription: Subscription) {
  selectedSubscription.value = subscription
  showCancelDialog.value = true
}

function handleReactivateClick(subscription: Subscription) {
  selectedSubscription.value = subscription
  reactivateDate.value = formatDateISO(new Date())
  showReactivateDialog.value = true
}

async function confirmCancel() {
  if (selectedSubscription.value) {
    try {
      await subscriptionsStore.cancelSubscription(selectedSubscription.value.id, {
        cancelledAt: formatDateISO(new Date()),
      })
    } catch {
      // Error handled in store
    }
  }
  showCancelDialog.value = false
  selectedSubscription.value = null
}

async function confirmReactivate() {
  if (selectedSubscription.value) {
    try {
      await subscriptionsStore.reactivateSubscription(selectedSubscription.value.id, {
        nextBillingDate: reactivateDate.value,
      })
    } catch {
      // Error handled in store
    }
  }
  showReactivateDialog.value = false
  selectedSubscription.value = null
}

function cancelDialog() {
  showCancelDialog.value = false
  showReactivateDialog.value = false
  selectedSubscription.value = null
}
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Subscriptions</h1>
        <p class="text-gray-600">Manage all your subscriptions</p>
      </div>
      <router-link to="/subscriptions/add" class="btn-primary">
        + Add Subscription
      </router-link>
    </div>

    <div class="mb-6">
      <SubscriptionFiltersComponent v-model="filters" />
    </div>

    <div v-if="subscriptionsStore.error" class="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
      {{ subscriptionsStore.error }}
    </div>

    <div v-if="subscriptionsStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <div v-else-if="subscriptionsStore.subscriptions.length === 0" class="text-center py-12">
      <div class="text-gray-500">
        <svg class="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">No subscriptions</h3>
        <p class="mt-1 text-sm text-gray-500">Get started by adding a new subscription.</p>
        <router-link to="/subscriptions/add" class="mt-4 inline-block btn-primary">
          + Add Subscription
        </router-link>
      </div>
    </div>

    <div v-else class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      <SubscriptionCard
        v-for="subscription in subscriptionsStore.subscriptions"
        :key="subscription.id"
        :subscription="subscription"
        @view="handleView"
        @edit="handleEdit"
        @cancel="handleCancelClick"
        @reactivate="handleReactivateClick"
      />
    </div>

    <div v-if="subscriptionsStore.pagination.totalPages > 1" class="mt-6">
      <Pagination
        :current-page="subscriptionsStore.pagination.page"
        :total-pages="subscriptionsStore.pagination.totalPages"
        :total-items="subscriptionsStore.pagination.total"
        @page-change="handlePageChange"
      />
    </div>

    <!-- Cancel Dialog -->
    <ConfirmDialog
      v-if="showCancelDialog"
      title="Cancel Subscription"
      :message="`Are you sure you want to cancel your ${selectedSubscription?.service.name} subscription?`"
      confirm-text="Cancel Subscription"
      type="danger"
      @confirm="confirmCancel"
      @cancel="cancelDialog"
    />

    <!-- Reactivate Dialog -->
    <div v-if="showReactivateDialog" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="cancelDialog" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h2 class="text-xl font-semibold mb-4">Reactivate Subscription</h2>
          <p class="text-gray-600 mb-4">
            Reactivate your {{ selectedSubscription?.service.name }} subscription. Select the next billing date:
          </p>
          <div class="mb-4">
            <label for="reactivateDate" class="label">Next Billing Date</label>
            <input
              id="reactivateDate"
              v-model="reactivateDate"
              type="date"
              class="input"
            />
          </div>
          <div class="flex justify-end space-x-3">
            <button class="btn-secondary" @click="cancelDialog">Cancel</button>
            <button class="btn-success" @click="confirmReactivate">Reactivate</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
