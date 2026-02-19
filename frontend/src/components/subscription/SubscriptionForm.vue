<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type {
  Subscription,
  CreateSubscriptionRequest,
  BillingCycle,
  RecurrenceErrorDetails,
} from '@/types'
import { CURRENCIES, BILLING_CYCLES, SELECTABLE_BILLING_CYCLES } from '@/utils/constants'
import { calculateStartDate } from '@/utils/billingCalculations'
import {
  isDateOnly,
  computeUserCutoffDate,
  getMonthlyAnchorOptions,
  getYearlyAnchorOptions,
  generateNextOccurrences,
  computeExpectedNextAfterCutoff,
  type RecurrenceCadence,
} from '@/utils/recurrence'
import ServiceSelector from '@/components/service/ServiceSelector.vue'
import { mapRecurrenceErrorToFieldErrors } from '@/stores/subscriptions'
import { useSettingsStore } from '@/stores/settings'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { DatePicker } from '@/components/ui/date-picker'

const props = defineProps<{
  subscription?: Subscription | null
  error?: string
  recurrenceError?: RecurrenceErrorDetails | null
}>()

const emit = defineEmits<{
  save: [data: CreateSubscriptionRequest]
  cancel: []
}>()

const settingsStore = useSettingsStore()

const isEditing = computed(() => !!props.subscription)

const serviceId = ref<number | null>(null)
const amount = ref('')
const currencyCode = ref(settingsStore.settings?.baseCurrency || 'USD')
const billingCycle = ref<BillingCycle>('monthly')
const paymentMethod = ref('')
const startDate = ref('')
const firstBillingDate = ref('')
const nextBillingDate = ref('')
const anchorDay = ref('')
const anchorMonthDay = ref('')
const notes = ref('')
const fieldErrors = ref<Record<string, string>>({})

const recurrenceCadence = computed<RecurrenceCadence | null>(() => {
  if (billingCycle.value === 'monthly' || billingCycle.value === 'yearly') {
    return billingCycle.value
  }
  return null
})

onMounted(async () => {
  if (!settingsStore.settings) {
    await settingsStore.fetchSettings()
  }

  if (props.subscription) {
    serviceId.value = props.subscription.service.id
    amount.value = props.subscription.amount.toString()
    currencyCode.value = props.subscription.currencyCode
    billingCycle.value = props.subscription.billingCycle
    paymentMethod.value = props.subscription.paymentMethod || ''
    nextBillingDate.value = props.subscription.nextBillingDate
    notes.value = props.subscription.notes || ''
    updateStartDate()
  } else {
    if (settingsStore.settings?.baseCurrency) {
      currencyCode.value = settingsStore.settings.baseCurrency
    }
  }
})

watch(() => props.recurrenceError, (details) => {
  if (isEditing.value || !details) {
    return
  }
  fieldErrors.value = mapRecurrenceErrorToFieldErrors(details)
}, { immediate: true })

watch([nextBillingDate, billingCycle], () => {
  if (isEditing.value) {
    updateStartDate()
  }
})

watch([firstBillingDate, nextBillingDate, billingCycle, anchorDay, anchorMonthDay], () => {
  if (!isEditing.value) {
    fieldErrors.value = {}
  }
})

const monthlyAnchorOptions = computed<number[] | null>(() => {
  if (isEditing.value || firstBillingDate.value || !nextBillingDate.value || billingCycle.value !== 'monthly') {
    return null
  }
  return getMonthlyAnchorOptions(nextBillingDate.value)
})

const yearlyAnchorOptions = computed<string[] | null>(() => {
  if (isEditing.value || firstBillingDate.value || !nextBillingDate.value || billingCycle.value !== 'yearly') {
    return null
  }
  return getYearlyAnchorOptions(nextBillingDate.value)
})

watch([monthlyAnchorOptions, yearlyAnchorOptions], () => {
  if (monthlyAnchorOptions.value === null) {
    anchorDay.value = ''
  } else if (anchorDay.value) {
    const parsed = Number(anchorDay.value)
    if (!monthlyAnchorOptions.value.includes(parsed)) {
      anchorDay.value = ''
    }
  }

  if (yearlyAnchorOptions.value === null) {
    anchorMonthDay.value = ''
  } else if (anchorMonthDay.value && !yearlyAnchorOptions.value.includes(anchorMonthDay.value)) {
    anchorMonthDay.value = ''
  }
})

function updateStartDate() {
  if (!nextBillingDate.value) {
    startDate.value = ''
    return
  }
  startDate.value = calculateStartDate(nextBillingDate.value, billingCycle.value)
}

const availableBillingCycles = computed(() => {
  const hiddenCycles: BillingCycle[] = ['bi_annual', 'custom']
  const currentCycle = props.subscription?.billingCycle

  if (currentCycle && hiddenCycles.includes(currentCycle)) {
    const currentCycleOption = BILLING_CYCLES.find(c => c.value === currentCycle)
    if (currentCycleOption) {
      return [...SELECTABLE_BILLING_CYCLES, currentCycleOption]
    }
  }

  return SELECTABLE_BILLING_CYCLES
})

const canSubmit = computed(() => {
  return serviceId.value !== null
    && amount.value !== ''
    && Number(amount.value) > 0
    && !!currencyCode.value
    && (!isEditing.value || !!nextBillingDate.value)
})

function resolveCutoffDate(): { cutoffDate: string | null; errorMessage: string | null } {
  const userTimeZone = settingsStore.settings?.userTimeZone
  if (!userTimeZone) {
    return {
      cutoffDate: null,
      errorMessage: 'Configure a valid timezone in Settings before creating this subscription.',
    }
  }

  try {
    return {
      cutoffDate: computeUserCutoffDate(userTimeZone),
      errorMessage: null,
    }
  } catch {
    return {
      cutoffDate: null,
      errorMessage: 'Configure a valid IANA timezone in Settings before creating this subscription.',
    }
  }
}

function validateCreateForm(): boolean {
  const errors: Record<string, string> = {}

  if (serviceId.value === null) {
    errors.serviceId = 'Service is required.'
  }

  if (amount.value === '' || Number(amount.value) <= 0) {
    errors.amount = 'Amount must be greater than 0.'
  }

  if (!recurrenceCadence.value) {
    errors.billingCycle = 'Only monthly and yearly billing cycles are supported for new subscriptions.'
  }

  const hasFirst = !!firstBillingDate.value
  const hasNext = !!nextBillingDate.value

  if (!hasFirst && !hasNext) {
    errors.firstBillingDate = 'Either first billing date or next billing date is required.'
    errors.nextBillingDate = 'Either first billing date or next billing date is required.'
  }

  if (hasFirst && !isDateOnly(firstBillingDate.value)) {
    errors.firstBillingDate = 'First billing date must use YYYY-MM-DD format.'
  }

  if (hasNext && !isDateOnly(nextBillingDate.value)) {
    errors.nextBillingDate = 'Next billing date must use YYYY-MM-DD format.'
  }

  let cutoffDate: string | null = null
  if (hasFirst && !errors.firstBillingDate) {
    const cutoff = resolveCutoffDate()
    cutoffDate = cutoff.cutoffDate
    if (!cutoffDate && cutoff.errorMessage) {
      errors.userTimeZone = cutoff.errorMessage
    } else if (cutoffDate && firstBillingDate.value > cutoffDate) {
      errors.firstBillingDate = 'First billing date cannot be after your local cutoff date.'
    }
  }

  if (hasFirst && hasNext && !errors.firstBillingDate && !errors.nextBillingDate
      && firstBillingDate.value > nextBillingDate.value) {
    errors.firstBillingDate = 'First billing date must be on or before next billing date.'
    errors.nextBillingDate = 'First billing date must be on or before next billing date.'
  }

  if (hasFirst && (anchorDay.value || anchorMonthDay.value)) {
    errors.anchorDay = 'Anchor override is not allowed when first billing date is provided.'
    errors.anchorMonthDay = 'Anchor override is not allowed when first billing date is provided.'
  }

  if (!hasFirst && hasNext && recurrenceCadence.value === 'monthly') {
    if (anchorMonthDay.value) {
      errors.anchorMonthDay = 'Yearly anchor is not allowed for monthly cadence.'
    }

    if (monthlyAnchorOptions.value) {
      if (!anchorDay.value) {
        errors.anchorDay = 'Select a monthly anchor day for this billing date.'
      } else {
        const parsedAnchor = Number(anchorDay.value)
        if (!monthlyAnchorOptions.value.includes(parsedAnchor)) {
          errors.anchorDay = 'Selected monthly anchor day is not valid for this billing date.'
        }
      }
    } else if (anchorDay.value) {
      errors.anchorDay = 'Monthly anchor day is not allowed for this next billing date.'
    }
  }

  if (!hasFirst && hasNext && recurrenceCadence.value === 'yearly') {
    if (anchorDay.value) {
      errors.anchorDay = 'Monthly anchor day is not allowed for yearly cadence.'
    }

    if (yearlyAnchorOptions.value) {
      if (!anchorMonthDay.value) {
        errors.anchorMonthDay = 'Select a yearly anchor month/day for this billing date.'
      } else if (!yearlyAnchorOptions.value.includes(anchorMonthDay.value)) {
        errors.anchorMonthDay = 'Selected yearly anchor month/day is not valid for this billing date.'
      }
    } else if (anchorMonthDay.value) {
      errors.anchorMonthDay = 'Yearly anchor month/day is not allowed for this next billing date.'
    }
  }

  if (hasFirst && hasNext && recurrenceCadence.value && !errors.firstBillingDate && !errors.nextBillingDate) {
    const effectiveCutoff = cutoffDate ?? resolveCutoffDate().cutoffDate
    if (effectiveCutoff) {
      const derivedAnchorDay = Number(firstBillingDate.value.slice(8, 10))
      const derivedAnchorMonth = recurrenceCadence.value === 'yearly'
        ? Number(firstBillingDate.value.slice(5, 7))
        : undefined

      const expectedNext = computeExpectedNextAfterCutoff(
        firstBillingDate.value,
        effectiveCutoff,
        recurrenceCadence.value,
        derivedAnchorDay,
        derivedAnchorMonth
      )

      if (expectedNext !== nextBillingDate.value) {
        errors.nextBillingDate = 'Next billing date does not match a standard recurrence schedule.'
      }
    }
  }

  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

function buildCreatePayload(): CreateSubscriptionRequest {
  const payload: CreateSubscriptionRequest = {
    serviceId: serviceId.value as number,
    amount: Number(amount.value),
    currencyCode: currencyCode.value,
    billingCycle: recurrenceCadence.value as RecurrenceCadence,
    paymentMethod: paymentMethod.value || undefined,
    notes: notes.value || undefined,
  }

  if (firstBillingDate.value) {
    payload.firstBillingDate = firstBillingDate.value
  }

  if (nextBillingDate.value) {
    payload.nextBillingDate = nextBillingDate.value
  }

  if (!firstBillingDate.value && monthlyAnchorOptions.value && anchorDay.value) {
    payload.anchorDay = Number(anchorDay.value)
  }

  if (!firstBillingDate.value && yearlyAnchorOptions.value && anchorMonthDay.value) {
    payload.anchorMonthDay = anchorMonthDay.value
  }

  return payload
}

function buildUpdatePayload(): CreateSubscriptionRequest {
  return {
    serviceId: serviceId.value as number,
    amount: Number(amount.value),
    currencyCode: currencyCode.value,
    billingCycle: billingCycle.value,
    paymentMethod: paymentMethod.value || undefined,
    nextBillingDate: nextBillingDate.value,
    notes: notes.value || undefined,
  }
}

const nextThreeOccurrences = computed(() => {
  if (isEditing.value || !recurrenceCadence.value) {
    return []
  }

  const hasFirst = !!firstBillingDate.value && isDateOnly(firstBillingDate.value)
  const hasNext = !!nextBillingDate.value && isDateOnly(nextBillingDate.value)

  if (!hasFirst && !hasNext) {
    return []
  }

  let start = hasNext ? nextBillingDate.value : ''
  let resolvedAnchorDay: number | null = null
  let resolvedAnchorMonth: number | undefined

  if (hasFirst) {
    resolvedAnchorDay = Number(firstBillingDate.value.slice(8, 10))
    resolvedAnchorMonth = recurrenceCadence.value === 'yearly'
      ? Number(firstBillingDate.value.slice(5, 7))
      : undefined

    if (!start) {
      const cutoffDate = resolveCutoffDate().cutoffDate
      if (!cutoffDate) {
        return []
      }
      start = computeExpectedNextAfterCutoff(
        firstBillingDate.value,
        cutoffDate,
        recurrenceCadence.value,
        resolvedAnchorDay,
        resolvedAnchorMonth
      )
    }
  } else if (hasNext && recurrenceCadence.value === 'monthly') {
    if (monthlyAnchorOptions.value) {
      const parsedAnchor = Number(anchorDay.value)
      if (!anchorDay.value || !monthlyAnchorOptions.value.includes(parsedAnchor)) {
        return []
      }
      resolvedAnchorDay = parsedAnchor
    } else {
      resolvedAnchorDay = Number(nextBillingDate.value.slice(8, 10))
    }
  } else if (hasNext && recurrenceCadence.value === 'yearly') {
    if (yearlyAnchorOptions.value) {
      if (!anchorMonthDay.value || !yearlyAnchorOptions.value.includes(anchorMonthDay.value)) {
        return []
      }
      resolvedAnchorMonth = Number(anchorMonthDay.value.slice(0, 2))
      resolvedAnchorDay = Number(anchorMonthDay.value.slice(3, 5))
    } else {
      resolvedAnchorMonth = Number(nextBillingDate.value.slice(5, 7))
      resolvedAnchorDay = Number(nextBillingDate.value.slice(8, 10))
    }
  }

  if (!start || !resolvedAnchorDay) {
    return []
  }

  return generateNextOccurrences(
    start,
    recurrenceCadence.value,
    resolvedAnchorDay,
    resolvedAnchorMonth,
    3
  )
})

function handleSubmit() {
  if (!canSubmit.value || serviceId.value === null) {
    return
  }

  if (isEditing.value) {
    emit('save', buildUpdatePayload())
    return
  }

  if (!validateCreateForm()) {
    return
  }

  emit('save', buildCreatePayload())
}
</script>

<template>
  <form class="space-y-6" @submit.prevent="handleSubmit">
    <Alert v-if="error" variant="destructive">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

    <Alert v-if="fieldErrors.form" variant="destructive">
      <AlertDescription>{{ fieldErrors.form }}</AlertDescription>
    </Alert>

    <div>
      <Label>Service *</Label>
      <ServiceSelector v-model="serviceId" class="mt-2" />
      <p v-if="fieldErrors.serviceId" class="mt-1 text-xs text-red-600">{{ fieldErrors.serviceId }}</p>
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
        <p v-if="fieldErrors.amount" class="mt-1 text-xs text-red-600">{{ fieldErrors.amount }}</p>
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
            'flex items-center justify-center p-3 border rounded-lg transition-colors',
            isEditing
              ? 'cursor-not-allowed opacity-50'
              : 'cursor-pointer',
            billingCycle === cycle.value
              ? 'border-primary bg-primary/5 text-primary'
              : isEditing ? 'border-input' : 'border-input hover:border-gray-400'
          ]"
        >
          <input
            v-model="billingCycle"
            type="radio"
            :value="cycle.value"
            :disabled="isEditing"
            class="sr-only"
          />
          <span class="text-sm font-medium">{{ cycle.label }}</span>
        </label>
      </div>
      <p v-if="isEditing" class="text-xs text-muted-foreground mt-2">
        Billing cycle cannot be changed. To use a different billing cycle, cancel this subscription and create a new one.
      </p>
      <p v-if="fieldErrors.billingCycle" class="mt-1 text-xs text-red-600">{{ fieldErrors.billingCycle }}</p>
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

    <div v-if="!isEditing" class="space-y-4">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <Label>First Billing Date (optional)</Label>
          <DatePicker
            v-model="firstBillingDate"
            placeholder="Select first billing date"
            class="mt-2"
          />
          <p v-if="fieldErrors.firstBillingDate" class="mt-1 text-xs text-red-600">{{ fieldErrors.firstBillingDate }}</p>
          <p class="text-xs text-muted-foreground mt-1">Used for history import/backfill.</p>
        </div>
        <div>
          <Label>Next Billing Date (optional)</Label>
          <DatePicker
            v-model="nextBillingDate"
            placeholder="Select next billing date"
            class="mt-2"
          />
          <p v-if="fieldErrors.nextBillingDate" class="mt-1 text-xs text-red-600">{{ fieldErrors.nextBillingDate }}</p>
          <p class="text-xs text-muted-foreground mt-1">At least one of first or next billing date is required.</p>
        </div>
      </div>

      <div v-if="monthlyAnchorOptions" class="max-w-xs">
        <Label for="anchorDay">Monthly Anchor Day *</Label>
        <select
          id="anchorDay"
          v-model="anchorDay"
          class="mt-2 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm"
        >
          <option value="">Select anchor day</option>
          <option v-for="option in monthlyAnchorOptions" :key="option" :value="String(option)">
            {{ option }}
          </option>
        </select>
        <p v-if="fieldErrors.anchorDay" class="mt-1 text-xs text-red-600">{{ fieldErrors.anchorDay }}</p>
      </div>

      <div v-if="yearlyAnchorOptions" class="max-w-xs">
        <Label for="anchorMonthDay">Yearly Anchor Month/Day *</Label>
        <select
          id="anchorMonthDay"
          v-model="anchorMonthDay"
          class="mt-2 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm"
        >
          <option value="">Select anchor</option>
          <option v-for="option in yearlyAnchorOptions" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
        <p v-if="fieldErrors.anchorMonthDay" class="mt-1 text-xs text-red-600">{{ fieldErrors.anchorMonthDay }}</p>
      </div>

      <p v-if="fieldErrors.userTimeZone" class="text-xs text-red-600">{{ fieldErrors.userTimeZone }}</p>

      <div v-if="nextThreeOccurrences.length" class="rounded-md border border-input p-3">
        <p class="text-sm font-medium">Next 3 occurrences</p>
        <ul class="mt-2 space-y-1 text-sm text-muted-foreground">
          <li v-for="occurrence in nextThreeOccurrences" :key="occurrence">{{ occurrence }}</li>
        </ul>
      </div>
    </div>

    <div v-else class="grid grid-cols-2 gap-4">
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
      <Button type="submit" :disabled="!canSubmit">
        {{ subscription ? 'Save Changes' : 'Create Subscription' }}
      </Button>
    </div>
  </form>
</template>
