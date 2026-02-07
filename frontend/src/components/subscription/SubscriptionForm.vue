<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { Subscription, CreateSubscriptionRequest, BillingCycle } from '@/types'
import { CURRENCIES, BILLING_CYCLES, SELECTABLE_BILLING_CYCLES } from '@/utils/constants'
import { formatDateISO } from '@/utils/formatters'
import { calculateStartDate } from '@/utils/billingCalculations'
import ServiceSelector from '@/components/service/ServiceSelector.vue'
import { useSettingsStore } from '@/stores/settings'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { DatePicker } from '@/components/ui/date-picker'

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

// Compute available billing cycles: use selectable cycles, but include the current
// subscription's cycle if it's a hidden one (bi_annual or custom) to allow viewing/keeping it
const availableBillingCycles = computed(() => {
  const hiddenCycles = ['bi_annual', 'custom']
  const currentCycle = props.subscription?.billingCycle

  // If editing a subscription with a hidden cycle, include that cycle in the options
  if (currentCycle && hiddenCycles.includes(currentCycle)) {
    const currentCycleOption = BILLING_CYCLES.find(c => c.value === currentCycle)
    if (currentCycleOption) {
      return [...SELECTABLE_BILLING_CYCLES, currentCycleOption]
    }
  }

  return SELECTABLE_BILLING_CYCLES
})

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
    <Alert v-if="error" variant="destructive">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

    <div>
      <Label>Service *</Label>
      <ServiceSelector v-model="serviceId" class="mt-2" />
    </div>

    <div class="grid grid-cols-2 gap-4">
      <div>
        <Label for="amount">Amount *</Label>
        <Input
          id="amount"
          v-model="amount"
          type="number"
          step="0.01"
          min="0.01"
          required
          placeholder="0.00"
          class="mt-2"
        />
      </div>
      <div>
        <Label for="currency">Currency *</Label>
        <select id="currency" v-model="currencyCode" class="mt-2 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm">
          <option v-for="currency in CURRENCIES" :key="currency" :value="currency">
            {{ currency }}
          </option>
        </select>
      </div>
    </div>

    <div>
      <Label>Billing Cycle *</Label>
      <div class="mt-2 grid grid-cols-2 sm:grid-cols-4 gap-2">
        <label
          v-for="cycle in availableBillingCycles"
          :key="cycle.value"
          :class="[
            'flex items-center justify-center p-3 border rounded-lg cursor-pointer transition-colors',
            billingCycle === cycle.value
              ? 'border-primary bg-primary/5 text-primary'
              : 'border-input hover:border-gray-400'
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
      <Label for="billingCycleDays">Every how many days? *</Label>
      <Input
        id="billingCycleDays"
        v-model="billingCycleDays"
        type="number"
        min="1"
        required
        placeholder="30"
        class="mt-2"
      />
    </div>

    <div>
      <Label for="paymentMethod">Payment Method</Label>
      <Input
        id="paymentMethod"
        v-model="paymentMethod"
        type="text"
        placeholder="e.g., Revolut Visa"
        class="mt-2"
      />
    </div>

    <div class="grid grid-cols-2 gap-4">
      <div>
        <Label>Next Billing Date *</Label>
        <DatePicker
          v-model="nextBillingDate"
          placeholder="Select billing date"
          class="mt-2"
        />
      </div>
      <div>
        <Label>Start Date</Label>
        <DatePicker
          v-model="startDate"
          disabled
          placeholder="Auto-calculated"
          class="mt-2"
        />
        <p class="text-xs text-muted-foreground mt-1">Auto-calculated based on billing cycle</p>
      </div>
    </div>

    <div>
      <Label for="notes">Notes</Label>
      <textarea
        id="notes"
        v-model="notes"
        rows="3"
        class="mt-2 flex min-h-[60px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-base shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm"
        placeholder="Optional notes..."
      />
    </div>

    <div class="flex justify-end space-x-3 pt-4">
      <Button type="button" variant="outline" @click="emit('cancel')">
        Cancel
      </Button>
      <Button type="submit" :disabled="!isValid">
        {{ subscription ? 'Save Changes' : 'Create Subscription' }}
      </Button>
    </div>
  </form>
</template>
