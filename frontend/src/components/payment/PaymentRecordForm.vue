<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { PaymentRecord, CreatePaymentRequest } from '@/types'
import { CURRENCIES } from '@/utils/constants'
import { formatDateISO } from '@/utils/formatters'

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
</script>

<template>
  <form @submit.prevent="handleSubmit" class="space-y-4">
    <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
      {{ error }}
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
      <label for="paymentDate" class="label">Payment Date *</label>
      <input
        id="paymentDate"
        v-model="paymentDate"
        type="date"
        required
        class="input"
      />
    </div>

    <div>
      <label for="fxRateToBase" class="label">FX Rate to Base Currency</label>
      <input
        id="fxRateToBase"
        v-model="fxRateToBase"
        type="number"
        step="0.000001"
        min="0.000001"
        class="input"
        placeholder="Leave empty to auto-calculate"
      />
      <p class="text-xs text-gray-500 mt-1">
        Leave empty to use the current exchange rate
      </p>
    </div>

    <div class="flex justify-end space-x-3 pt-4">
      <button type="button" @click="emit('cancel')" class="btn-secondary">
        Cancel
      </button>
      <button type="submit" :disabled="!isValid" class="btn-primary">
        {{ payment ? 'Save Changes' : 'Add Payment' }}
      </button>
    </div>
  </form>
</template>
