<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSubscriptionsStore } from '@/stores/subscriptions'
import type { UpdateSubscriptionRequest } from '@/types'
import SubscriptionForm from '@/components/subscription/SubscriptionForm.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const route = useRoute()
const subscriptionsStore = useSubscriptionsStore()
const formError = ref('')

const subscriptionId = parseInt(route.params.id as string)

onMounted(async () => {
  await subscriptionsStore.fetchSubscription(subscriptionId)
})

async function handleSave(data: UpdateSubscriptionRequest) {
  formError.value = ''
  try {
    await subscriptionsStore.updateSubscription(subscriptionId, data)
    router.push(`/subscriptions/${subscriptionId}`)
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    formError.value = axiosError.response?.data?.message || 'Failed to update subscription'
  }
}

function handleCancel() {
  router.back()
}
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="mb-6">
      <router-link :to="`/subscriptions/${subscriptionId}`" class="text-primary-600 hover:text-primary-800 text-sm">
        &larr; Back to Subscription
      </router-link>
    </div>

    <div v-if="subscriptionsStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <div v-else-if="subscriptionsStore.currentSubscription" class="card">
      <h1 class="text-2xl font-bold text-gray-900 mb-6">
        Edit {{ subscriptionsStore.currentSubscription.service.name }} Subscription
      </h1>
      <SubscriptionForm
        :subscription="subscriptionsStore.currentSubscription"
        :error="formError"
        @save="handleSave"
        @cancel="handleCancel"
      />
    </div>

    <div v-else class="text-center py-12">
      <p class="text-gray-500">Subscription not found</p>
    </div>
  </div>
</template>
