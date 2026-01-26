<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSubscriptionsStore } from '@/stores/subscriptions'
import type { CreateSubscriptionRequest } from '@/types'
import SubscriptionForm from '@/components/subscription/SubscriptionForm.vue'

const router = useRouter()
const subscriptionsStore = useSubscriptionsStore()
const formError = ref('')

async function handleSave(data: CreateSubscriptionRequest) {
  formError.value = ''
  try {
    await subscriptionsStore.createSubscription(data)
    router.push('/subscriptions')
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    formError.value = axiosError.response?.data?.message || 'Failed to create subscription'
  }
}

function handleCancel() {
  router.back()
}
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="mb-6">
      <router-link to="/subscriptions" class="text-primary-600 hover:text-primary-800 text-sm">
        &larr; Back to Subscriptions
      </router-link>
    </div>

    <div class="card">
      <h1 class="text-2xl font-bold text-gray-900 mb-6">Add New Subscription</h1>
      <SubscriptionForm
        :error="formError"
        @save="handleSave"
        @cancel="handleCancel"
      />
    </div>
  </div>
</template>
