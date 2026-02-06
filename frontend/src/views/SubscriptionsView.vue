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
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Label } from '@/components/ui/label'
import { DatePicker } from '@/components/ui/date-picker'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { CircleDollarSign } from 'lucide-vue-next'

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
      <Button as-child>
        <router-link to="/subscriptions/add">+ Add Subscription</router-link>
      </Button>
    </div>

    <div class="mb-6">
      <SubscriptionFiltersComponent v-model="filters" />
    </div>

    <Alert v-if="subscriptionsStore.error" variant="destructive" class="mb-6">
      <AlertDescription>{{ subscriptionsStore.error }}</AlertDescription>
    </Alert>

    <div v-if="subscriptionsStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <div v-else-if="subscriptionsStore.subscriptions.length === 0" class="text-center py-12">
      <div class="text-gray-500">
        <CircleDollarSign class="mx-auto h-12 w-12" />
        <h3 class="mt-2 text-sm font-medium text-gray-900">No subscriptions</h3>
        <p class="mt-1 text-sm text-gray-500">Get started by adding a new subscription.</p>
        <Button as-child class="mt-4">
          <router-link to="/subscriptions/add">+ Add Subscription</router-link>
        </Button>
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
    <Dialog v-model:open="showReactivateDialog">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Reactivate Subscription</DialogTitle>
          <DialogDescription>
            Reactivate your {{ selectedSubscription?.service.name }} subscription. Select the next billing date:
          </DialogDescription>
        </DialogHeader>
        <div class="py-4">
          <Label>Next Billing Date</Label>
          <DatePicker
            v-model="reactivateDate"
            placeholder="Select billing date"
            class="mt-2"
          />
        </div>
        <DialogFooter>
          <Button variant="outline" @click="cancelDialog">Cancel</Button>
          <Button variant="outline" class="text-green-600 border-green-600 hover:bg-green-50" @click="confirmReactivate">
            Reactivate
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
