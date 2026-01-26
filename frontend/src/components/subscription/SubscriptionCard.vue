<script setup lang="ts">
import type { Subscription } from '@/types'
import { formatCurrency, formatDate, daysUntil, getBillingCycleLabel } from '@/utils/formatters'

const props = defineProps<{
  subscription: Subscription
}>()

const emit = defineEmits<{
  edit: [subscription: Subscription]
  cancel: [subscription: Subscription]
  reactivate: [subscription: Subscription]
  view: [subscription: Subscription]
}>()

const daysUntilBilling = daysUntil(props.subscription.nextBillingDate)
const isActive = props.subscription.status === 'active'
</script>

<template>
  <div
    :class="[
      'card-hover cursor-pointer',
      !isActive && 'opacity-60'
    ]"
    @click="emit('view', subscription)"
  >
    <div class="flex items-start justify-between">
      <div class="flex items-center space-x-3">
        <div v-if="subscription.service.faviconUrl" class="w-12 h-12 flex-shrink-0 rounded-xl bg-gray-50 p-1.5">
          <img
            :src="subscription.service.faviconUrl"
            :alt="subscription.service.name"
            class="w-full h-full object-contain"
          />
        </div>
        <div v-else class="w-12 h-12 bg-gradient-to-br from-primary-50 to-primary-100 rounded-xl flex items-center justify-center">
          <span class="text-primary-600 font-semibold text-xl">
            {{ subscription.service.name.charAt(0).toUpperCase() }}
          </span>
        </div>
        <div>
          <h3 class="font-semibold text-gray-900">{{ subscription.service.name }}</h3>
          <p v-if="subscription.service.category" class="text-sm text-gray-500">
            {{ subscription.service.category }}
          </p>
        </div>
      </div>
      <span
        :class="[
          'px-2.5 py-1 text-xs font-medium rounded-full',
          isActive ? 'bg-green-50 text-green-700 ring-1 ring-green-600/20' : 'bg-gray-100 text-gray-600'
        ]"
      >
        {{ isActive ? 'Active' : 'Cancelled' }}
      </span>
    </div>

    <div class="mt-4 grid grid-cols-2 gap-4 text-sm">
      <div>
        <span class="text-gray-500">Amount</span>
        <p class="font-semibold">
          {{ formatCurrency(subscription.amount, subscription.currencyCode) }}/{{ getBillingCycleLabel(subscription.billingCycle).toLowerCase() }}
        </p>
      </div>
      <div>
        <span class="text-gray-500">{{ isActive ? 'Next Billing' : 'Cancelled' }}</span>
        <p class="font-semibold">
          <template v-if="isActive">
            {{ formatDate(subscription.nextBillingDate) }}
            <span v-if="daysUntilBilling <= 7 && daysUntilBilling >= 0" class="text-orange-600 text-xs">
              ({{ daysUntilBilling === 0 ? 'Today' : `${daysUntilBilling}d` }})
            </span>
          </template>
          <template v-else>
            {{ subscription.cancelledAt ? formatDate(subscription.cancelledAt) : '-' }}
          </template>
        </p>
      </div>
    </div>

    <div v-if="subscription.paymentMethod" class="mt-3 text-sm text-gray-500">
      Payment: {{ subscription.paymentMethod }}
    </div>

    <div class="mt-4 flex justify-end space-x-3" @click.stop>
      <button
        @click="emit('edit', subscription)"
        class="text-primary-600 hover:text-primary-700 text-sm font-medium transition-colors"
      >
        Edit
      </button>
      <template v-if="isActive">
        <button
          @click="emit('cancel', subscription)"
          class="text-red-600 hover:text-red-700 text-sm font-medium transition-colors"
        >
          Cancel
        </button>
      </template>
      <template v-else>
        <button
          @click="emit('reactivate', subscription)"
          class="text-green-600 hover:text-green-700 text-sm font-medium transition-colors"
        >
          Reactivate
        </button>
      </template>
    </div>
  </div>
</template>
