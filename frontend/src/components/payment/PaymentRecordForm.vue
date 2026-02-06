<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { PaymentRecord, CreatePaymentRequest } from '@/types'
import { CURRENCIES } from '@/utils/constants'
import { formatDateISO } from '@/utils/formatters'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { DatePicker } from '@/components/ui/date-picker'

const props = defineProps<{
  payment?: PaymentRecord | null
  subscriptionId: number
  defaultCurrency?: string
  error?: string
}>()

const emit = defineEmits<{
  save: [data: CreatePaymentRequest]
  cancel: []
}>()

const amount = ref('')
const currencyCode = ref(props.defaultCurrency || 'USD')
const paymentDate = ref(formatDateISO(new Date()))
const fxRateToBase = ref('')

onMounted(() => {
  if (props.payment) {
    amount.value = props.payment.amount.toString()
    currencyCode.value = props.payment.currencyCode
    paymentDate.value = props.payment.paymentDate
    fxRateToBase.value = props.payment.fxRateToBase.toString()
  }
})

const isValid = computed(() => {
  return amount.value !== '' &&
         parseFloat(amount.value) > 0 &&
         paymentDate.value !== ''
})

function handleSubmit() {
  if (!isValid.value) return

  const data: CreatePaymentRequest = {
    subscriptionId: props.subscriptionId,
    amount: parseFloat(amount.value),
    currencyCode: currencyCode.value,
    paymentDate: paymentDate.value,
    fxRateToBase: fxRateToBase.value ? parseFloat(fxRateToBase.value) : undefined,
  }

  emit('save', data)
}

const selectClass = "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm"
</script>

<template>
  <form class="space-y-4" @submit.prevent="handleSubmit">
    <Alert v-if="error" variant="destructive">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

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
        <select id="currency" v-model="currencyCode" :class="['mt-2', selectClass]">
          <option v-for="currency in CURRENCIES" :key="currency" :value="currency">
            {{ currency }}
          </option>
        </select>
      </div>
    </div>

    <div>
      <Label>Payment Date *</Label>
      <DatePicker
        v-model="paymentDate"
        placeholder="Select payment date"
        class="mt-2"
      />
    </div>

    <div>
      <Label for="fxRateToBase">FX Rate to Base Currency</Label>
      <Input
        id="fxRateToBase"
        v-model="fxRateToBase"
        type="number"
        step="0.000001"
        min="0.000001"
        placeholder="Leave empty to auto-calculate"
        class="mt-2"
      />
      <p class="text-xs text-muted-foreground mt-1">
        Leave empty to use the current exchange rate
      </p>
    </div>

    <div class="flex justify-end space-x-3 pt-4">
      <Button type="button" variant="outline" @click="emit('cancel')">
        Cancel
      </Button>
      <Button type="submit" :disabled="!isValid">
        {{ payment ? 'Save Changes' : 'Add Payment' }}
      </Button>
    </div>
  </form>
</template>
