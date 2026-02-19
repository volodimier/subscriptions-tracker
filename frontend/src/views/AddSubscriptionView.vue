<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSubscriptionsStore } from '@/stores/subscriptions'
import type { CreateSubscriptionRequest } from '@/types'
import SubscriptionForm from '@/components/subscription/SubscriptionForm.vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

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
    formError.value = axiosError.response?.data?.message
      || subscriptionsStore.error
      || 'Failed to create subscription'
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

    <Card>
      <CardHeader>
        <CardTitle class="text-2xl">Add New Subscription</CardTitle>
      </CardHeader>
      <CardContent>
        <SubscriptionForm
          :error="formError"
          :recurrence-error="subscriptionsStore.createErrorDetails"
          @save="handleSave"
          @cancel="handleCancel"
        />
      </CardContent>
    </Card>
  </div>
</template>
