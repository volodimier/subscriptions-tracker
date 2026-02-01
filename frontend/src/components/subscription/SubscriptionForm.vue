<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { Subscription, CreateSubscriptionRequest, BillingCycle } from '@/types'
import { CURRENCIES, BILLING_CYCLES } from '@/utils/constants'
import { formatDateISO } from '@/utils/formatters'
import { calculateStartDate } from '@/utils/billingCalculations'
import ServiceSelector from '@/components/service/ServiceSelector.vue'
import { useSettingsStore } from '@/stores/settings'

const props = defineProps<{
  subscription?: Subscription | null
  error?: string
}>()

const emit = defineEmits<{
  save: [data: CreateSubscriptionRequest]
  cancel: []
}>()

const settingsStore = useSettingsStore()

const serviceId = ref<number | null>(null)
const amount = ref('')
const currencyCode = ref(settingsStore.settings?.baseCurrency || 'USD')
const billingCycle = ref<BillingCycle>('monthly')
const billingCycleDays = ref('')
const paymentMethod = ref('')
const startDate = ref('')
const nextBillingDate = ref('')
const notes = ref('')

onMounted(async () => {
  if (props.subscription) {
    serviceId.value = props.subscription.service.id
    amount.value = props.subscription.amount.toString()
    currencyCode.value = props.subscription.currencyCode
    billingCycle.value = props.subscription.billingCycle
    billingCycleDays.value = props.subscription.billingCycleDays?.toString() || ''
    paymentMethod.value = props.subscription.paymentMethod || ''
    nextBillingDate.value = props.subscription.nextBillingDate
    notes.value = props.subscription.notes || ''
    // Calculate start date from existing next billing date
    updateStartDate()
  } else {
    // Fetch settings if not already loaded to get user's default currency
    if (!settingsStore.settings) {
      await settingsStore.fetchSettings()
    }
    if (settingsStore.settings?.baseCurrency) {
      currencyCode.value = settingsStore.settings.baseCurrency
    }
    // Set next billing date to today, then calculate start date
    nextBillingDate.value = formatDateISO(new Date())
    updateStartDate()
  }
})

// Watch for changes to nextBillingDate or billing cycle to recalculate startDate
watch([nextBillingDate, billingCycle, billingCycleDays], () => {
  updateStartDate()
})

function updateStartDate() {
  if (!nextBillingDate.value) return

  const customDays = billingCycle.value === 'custom' ? parseInt(billingCycleDays.value) || 30 : undefined
  startDate.value = calculateStartDate(nextBillingDate.value, billingCycle.value, customDays)
}

const isValid = computed(() => {
  return serviceId.value !== null &&
         amount.value !== '' &&
         parseFloat(amount.value) > 0 &&
         startDate.value !== '' &&
         nextBillingDate.value !== '' &&
         (billingCycle.value !== 'custom' || (billingCycleDays.value !== '' && parseInt(billingCycleDays.value) > 0))
})

function handleSubmit() {
  if (!isValid.value || serviceId.value === null) return

  const data: CreateSubscriptionRequest = {
    serviceId: serviceId.value,
    amount: parseFloat(amount.value),
    currencyCode: currencyCode.value,
    billingCycle: billingCycle.value,
    billingCycleDays: billingCycle.value === 'custom' ? parseInt(billingCycleDays.value) : undefined,
    paymentMethod: paymentMethod.value || undefined,
    nextBillingDate: nextBillingDate.value,
    notes: notes.value || undefined,
  }

  emit('save', data)
}
</script>

<template>
  <form class="space-y-6" @submit.prevent="handleSubmit">
    <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
      {{ error }}
    </div>

    <div>
      <label class="label">Service *</label>
      <ServiceSelector v-model="serviceId" />
    </div>

    <div class="grid grid-cols-2 gap-4">
      <div>
        <label for="amount" class="label">Amount *</label>
        <input
          id="amount"
          v-model="amount"
          type="number"
          step="0.01"
          min="0.01"
          required
          class="input"
          placeholder="0.00"
        />
      </div>
      <div>
        <label for="currency" class="label">Currency *</label>
        <select id="currency" v-model="currencyCode" class="input">
          <option v-for="currency in CURRENCIES" :key="currency" :value="currency">
            {{ currency }}
          </option>
        </select>
      </div>
    </div>

    <div>
      <label class="label">Billing Cycle *</label>
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-2">
        <label
          v-for="cycle in BILLING_CYCLES"
          :key="cycle.value"
          :class="[
            'flex items-center justify-center p-3 border rounded-lg cursor-pointer transition-colors',
            billingCycle === cycle.value
              ? 'border-primary-500 bg-primary-50 text-primary-700'
              : 'border-gray-300 hover:border-gray-400'
          ]"
        >
          <input
            v-model="billingCycle"
            type="radio"
            :value="cycle.value"
            class="sr-only"
          />
          <span class="text-sm font-medium">{{ cycle.label }}</span>
        </label>
      </div>
    </div>

    <div v-if="billingCycle === 'custom'">
      <label for="billingCycleDays" class="label">Every how many days? *</label>
      <input
        id="billingCycleDays"
        v-model="billingCycleDays"
        type="number"
        min="1"
        required
        class="input"
        placeholder="30"
      />
    </div>

    <div>
      <label for="paymentMethod" class="label">Payment Method</label>
      <input
        id="paymentMethod"
        v-model="paymentMethod"
        type="text"
        class="input"
        placeholder="e.g., Revolut Visa"
      />
    </div>

    <div class="grid grid-cols-2 gap-4">
      <div>
        <label for="nextBillingDate" class="label">Next Billing Date *</label>
        <input
          id="nextBillingDate"
          v-model="nextBillingDate"
          type="date"
          required
          class="input"
        />
      </div>
      <div>
        <label for="startDate" class="label">Start Date</label>
        <input
          id="startDate"
          v-model="startDate"
          type="date"
          readonly
          class="input bg-gray-50 cursor-not-allowed"
        />
        <p class="text-xs text-gray-500 mt-1">Auto-calculated based on billing cycle</p>
      </div>
    </div>

    <div>
      <label for="notes" class="label">Notes</label>
      <textarea
        id="notes"
        v-model="notes"
        rows="3"
        class="input"
        placeholder="Optional notes..."
      />
    </div>

    <div class="flex justify-end space-x-3 pt-4">
      <button type="button" class="btn-secondary" @click="emit('cancel')">
        Cancel
      </button>
      <button type="submit" :disabled="!isValid" class="btn-primary">
        {{ subscription ? 'Save Changes' : 'Create Subscription' }}
      </button>
    </div>
  </form>
</template>
