<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSubscriptionsStore } from '@/stores/subscriptions'
import { formatCurrency, formatDate, formatDateTime, getBillingCycleLabel, daysUntil } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { formatDateISO } from '@/utils/formatters'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
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

const router = useRouter()
const route = useRoute()
const subscriptionsStore = useSubscriptionsStore()

const showCancelDialog = ref(false)
const showDeleteDialog = ref(false)
const showReactivateDialog = ref(false)
const reactivateDate = ref(formatDateISO(new Date()))

const subscriptionId = parseInt(route.params.id as string)

onMounted(async () => {
  await subscriptionsStore.fetchSubscription(subscriptionId)
})

async function handleCancel() {
  try {
    await subscriptionsStore.cancelSubscription(subscriptionId, {
      cancelledAt: formatDateISO(new Date()),
    })
    await subscriptionsStore.fetchSubscription(subscriptionId)
  } catch {
    // Error handled in store
  }
  showCancelDialog.value = false
}

async function handleReactivate() {
  try {
    await subscriptionsStore.reactivateSubscription(subscriptionId, {
      nextBillingDate: reactivateDate.value,
    })
    await subscriptionsStore.fetchSubscription(subscriptionId)
  } catch {
    // Error handled in store
  }
  showReactivateDialog.value = false
}

async function handleDelete() {
  try {
    await subscriptionsStore.deleteSubscription(subscriptionId)
    router.push('/subscriptions')
  } catch {
    // Error handled in store
  }
  showDeleteDialog.value = false
}
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="mb-6">
      <router-link to="/subscriptions" class="text-primary-600 hover:text-primary-800 text-sm">
        &larr; Back to Subscriptions
      </router-link>
    </div>

    <div v-if="subscriptionsStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <div v-else-if="subscriptionsStore.currentSubscription" class="space-y-6">
      <Card>
        <CardContent class="p-6">
          <div class="flex items-start justify-between">
            <div class="flex items-center space-x-4">
              <div v-if="subscriptionsStore.currentSubscription.service.faviconUrl" class="w-16 h-16 flex-shrink-0">
                <img
                  :src="subscriptionsStore.currentSubscription.service.faviconUrl"
                  :alt="subscriptionsStore.currentSubscription.service.name"
                  class="w-full h-full object-contain rounded"
                />
              </div>
              <div v-else class="w-16 h-16 bg-primary-100 rounded flex items-center justify-center">
                <span class="text-primary-600 font-bold text-2xl">
                  {{ subscriptionsStore.currentSubscription.service.name.charAt(0).toUpperCase() }}
                </span>
              </div>
              <div>
                <h1 class="text-2xl font-bold text-gray-900">
                  {{ subscriptionsStore.currentSubscription.service.name }}
                </h1>
                <Badge
                  :variant="subscriptionsStore.currentSubscription.status === 'active' ? 'secondary' : 'outline'"
                  :class="subscriptionsStore.currentSubscription.status === 'active' ? 'bg-green-100 text-green-800 border-green-200 mt-1' : 'mt-1'"
                >
                  {{ subscriptionsStore.currentSubscription.status === 'active' ? 'Active' : 'Cancelled' }}
                </Badge>
              </div>
            </div>
            <div class="flex space-x-2">
              <Button variant="outline" as-child>
                <router-link :to="`/subscriptions/${subscriptionId}/edit`">
                  Edit
                </router-link>
              </Button>
              <Button variant="destructive" @click="showDeleteDialog = true">
                Delete
              </Button>
            </div>
          </div>

          <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="space-y-4">
              <h3 class="font-semibold text-gray-900">Subscription Details</h3>
              <dl class="space-y-3">
                <div class="flex justify-between">
                  <dt class="text-gray-500">Amount</dt>
                  <dd class="font-medium">
                    {{ formatCurrency(subscriptionsStore.currentSubscription.amount, subscriptionsStore.currentSubscription.currencyCode) }}
                  </dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">Billing Cycle</dt>
                  <dd class="font-medium">
                    {{ getBillingCycleLabel(subscriptionsStore.currentSubscription.billingCycle) }}
                    <span v-if="subscriptionsStore.currentSubscription.billingCycleDays">
                      ({{ subscriptionsStore.currentSubscription.billingCycleDays }} days)
                    </span>
                  </dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">Category</dt>
                  <dd class="font-medium">{{ subscriptionsStore.currentSubscription.service.category || '-' }}</dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">Payment Method</dt>
                  <dd class="font-medium">{{ subscriptionsStore.currentSubscription.paymentMethod || '-' }}</dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">Start Date</dt>
                  <dd class="font-medium">{{ formatDate(subscriptionsStore.currentSubscription.startDate) }}</dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">
                    {{ subscriptionsStore.currentSubscription.status === 'active' ? 'Next Billing' : 'Cancelled' }}
                  </dt>
                  <dd class="font-medium">
                    <template v-if="subscriptionsStore.currentSubscription.status === 'active'">
                      {{ formatDate(subscriptionsStore.currentSubscription.nextBillingDate) }}
                      <span
                        v-if="daysUntil(subscriptionsStore.currentSubscription.nextBillingDate) <= 7"
                        class="text-orange-600 text-sm"
                      >
                        ({{ daysUntil(subscriptionsStore.currentSubscription.nextBillingDate) === 0 ? 'Today' : `in ${daysUntil(subscriptionsStore.currentSubscription.nextBillingDate)} days` }})
                      </span>
                    </template>
                    <template v-else>
                      {{ subscriptionsStore.currentSubscription.cancelledAt ? formatDateTime(subscriptionsStore.currentSubscription.cancelledAt) : '-' }}
                    </template>
                  </dd>
                </div>
                <div v-if="subscriptionsStore.currentSubscription.notes">
                  <dt class="text-gray-500">Notes</dt>
                  <dd class="font-medium mt-1">{{ subscriptionsStore.currentSubscription.notes }}</dd>
                </div>
              </dl>
            </div>

            <div v-if="subscriptionsStore.currentSubscription.stats" class="space-y-4">
              <h3 class="font-semibold text-gray-900">Quick Stats</h3>
              <dl class="space-y-3">
                <div class="flex justify-between">
                  <dt class="text-gray-500">Total Paid</dt>
                  <dd class="font-medium">{{ formatCurrency(subscriptionsStore.currentSubscription.stats.totalPaid, 'USD') }}</dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">Total Payments</dt>
                  <dd class="font-medium">{{ subscriptionsStore.currentSubscription.stats.totalPayments }}</dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">Avg per Month</dt>
                  <dd class="font-medium">{{ formatCurrency(subscriptionsStore.currentSubscription.stats.averagePerMonth, 'USD') }}</dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">Active Since</dt>
                  <dd class="font-medium">{{ formatDate(subscriptionsStore.currentSubscription.stats.activeSince) }}</dd>
                </div>
              </dl>
            </div>
          </div>

          <div class="mt-6 pt-6 border-t border-gray-200">
            <h3 class="font-semibold text-gray-900 mb-3">Actions</h3>
            <div class="flex space-x-3">
              <template v-if="subscriptionsStore.currentSubscription.status === 'active'">
                <Button variant="destructive" @click="showCancelDialog = true">
                  Cancel Subscription
                </Button>
              </template>
              <template v-else>
                <Button variant="outline" class="text-green-600 border-green-600 hover:bg-green-50" @click="showReactivateDialog = true">
                  Reactivate Subscription
                </Button>
              </template>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <div v-else class="text-center py-12">
      <p class="text-gray-500">Subscription not found</p>
    </div>

    <!-- Cancel Dialog -->
    <ConfirmDialog
      v-if="showCancelDialog"
      title="Cancel Subscription"
      :message="`Are you sure you want to cancel your ${subscriptionsStore.currentSubscription?.service.name} subscription?`"
      confirm-text="Cancel Subscription"
      type="danger"
      @confirm="handleCancel"
      @cancel="showCancelDialog = false"
    />

    <!-- Delete Dialog -->
    <ConfirmDialog
      v-if="showDeleteDialog"
      title="Delete Subscription"
      :message="`Are you sure you want to permanently delete this subscription? This will also delete all payment history. This action cannot be undone.`"
      confirm-text="Delete"
      type="danger"
      @confirm="handleDelete"
      @cancel="showDeleteDialog = false"
    />

    <!-- Reactivate Dialog -->
    <Dialog v-model:open="showReactivateDialog">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Reactivate Subscription</DialogTitle>
          <DialogDescription>
            Select the next billing date for this subscription:
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
          <Button variant="outline" @click="showReactivateDialog = false">Cancel</Button>
          <Button variant="outline" class="text-green-600 border-green-600 hover:bg-green-50" @click="handleReactivate">
            Reactivate
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
