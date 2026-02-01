<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { usePaymentsStore } from '@/stores/payments'
import type { PaymentRecord, CreatePaymentRequest } from '@/types'
import { formatCurrency, formatDate } from '@/utils/formatters'
import PaymentRecordForm from './PaymentRecordForm.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Pagination from '@/components/common/Pagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const props = defineProps<{
  subscriptionId: number
  currencyCode: string
}>()

const paymentsStore = usePaymentsStore()

const showForm = ref(false)
const editingPayment = ref<PaymentRecord | null>(null)
const showDeleteConfirm = ref(false)
const paymentToDelete = ref<PaymentRecord | null>(null)
const formError = ref('')

onMounted(async () => {
  await paymentsStore.fetchPayments(props.subscriptionId)
})

watch(() => props.subscriptionId, async () => {
  await paymentsStore.fetchPayments(props.subscriptionId)
})

async function handlePageChange(page: number) {
  await paymentsStore.fetchPayments(props.subscriptionId, page)
}

function openCreateForm() {
  editingPayment.value = null
  formError.value = ''
  showForm.value = true
}

function openEditForm(payment: PaymentRecord) {
  editingPayment.value = payment
  formError.value = ''
  showForm.value = true
}

async function handleSave(data: CreatePaymentRequest) {
  formError.value = ''
  try {
    if (editingPayment.value) {
      await paymentsStore.updatePayment(editingPayment.value.id, {
        amount: data.amount,
        currencyCode: data.currencyCode,
        paymentDate: data.paymentDate,
        fxRateToBase: data.fxRateToBase,
      })
    } else {
      await paymentsStore.createPayment({
        ...data,
        subscriptionId: props.subscriptionId,
      })
    }
    showForm.value = false
    editingPayment.value = null
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    formError.value = axiosError.response?.data?.message || 'Failed to save payment'
  }
}

function handleCancel() {
  showForm.value = false
  editingPayment.value = null
  formError.value = ''
}

function confirmDelete(payment: PaymentRecord) {
  paymentToDelete.value = payment
  showDeleteConfirm.value = true
}

async function handleDelete() {
  if (paymentToDelete.value) {
    try {
      await paymentsStore.deletePayment(paymentToDelete.value.id)
    } catch {
      // Error handled in store
    }
  }
  showDeleteConfirm.value = false
  paymentToDelete.value = null
}

function cancelDelete() {
  showDeleteConfirm.value = false
  paymentToDelete.value = null
}
</script>

<template>
  <div class="space-y-4">
    <div class="flex justify-between items-center">
      <h3 class="text-lg font-semibold text-gray-900">Payment History</h3>
      <button class="btn-primary text-sm" @click="openCreateForm">
        + Add Payment
      </button>
    </div>

    <div v-if="paymentsStore.loading" class="flex justify-center py-8">
      <LoadingSpinner />
    </div>

    <div v-else-if="paymentsStore.payments.length === 0" class="text-center py-8 text-gray-500">
      No payment records found.
    </div>

    <div v-else class="overflow-x-auto">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Currency</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">FX Rate</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Base Amount</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="payment in paymentsStore.payments" :key="payment.id">
            <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
              {{ formatDate(payment.paymentDate) }}
            </td>
            <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
              {{ formatCurrency(payment.amount, payment.currencyCode) }}
            </td>
            <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
              {{ payment.currencyCode }}
            </td>
            <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
              {{ payment.fxRateToBase.toFixed(4) }}
            </td>
            <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
              {{ formatCurrency(payment.amountInBaseCurrency, 'USD') }}
            </td>
            <td class="px-4 py-3 whitespace-nowrap text-right text-sm">
              <button
                class="text-primary-600 hover:text-primary-800 mr-3"
                @click="openEditForm(payment)"
              >
                Edit
              </button>
              <button
                class="text-red-600 hover:text-red-800"
                @click="confirmDelete(payment)"
              >
                Delete
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="paymentsStore.pagination.totalPages > 1">
      <Pagination
        :current-page="paymentsStore.pagination.page"
        :total-pages="paymentsStore.pagination.totalPages"
        :total-items="paymentsStore.pagination.total"
        @page-change="handlePageChange"
      />
    </div>

    <!-- Payment Form Modal -->
    <div v-if="showForm" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="handleCancel" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
          <h2 class="text-xl font-semibold mb-4">
            {{ editingPayment ? 'Edit Payment' : 'Add Manual Payment' }}
          </h2>
          <PaymentRecordForm
            :payment="editingPayment"
            :subscription-id="subscriptionId"
            :default-currency="currencyCode"
            :error="formError"
            @save="handleSave"
            @cancel="handleCancel"
          />
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Dialog -->
    <ConfirmDialog
      v-if="showDeleteConfirm"
      title="Delete Payment"
      :message="`Are you sure you want to delete this payment record from ${paymentToDelete ? formatDate(paymentToDelete.paymentDate) : ''}?`"
      confirm-text="Delete"
      type="danger"
      @confirm="handleDelete"
      @cancel="cancelDelete"
    />
  </div>
</template>
