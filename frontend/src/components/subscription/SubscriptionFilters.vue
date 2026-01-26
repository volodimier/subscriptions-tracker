<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { SubscriptionFilters, SubscriptionStatus } from '@/types'
import { useSubscriptionsStore } from '@/stores/subscriptions'

const props = defineProps<{
  modelValue: SubscriptionFilters
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SubscriptionFilters]
}>()

const subscriptionsStore = useSubscriptionsStore()

const status = ref<SubscriptionStatus | 'all'>(props.modelValue.status || 'active')
const category = ref(props.modelValue.category || '')
const search = ref(props.modelValue.search || '')
const sort = ref(props.modelValue.sort || 'nextBillingDate')
const order = ref(props.modelValue.order || 'asc')

onMounted(async () => {
  await subscriptionsStore.fetchCategories()
})

watch([status, category, search, sort, order], () => {
  emit('update:modelValue', {
    status: status.value,
    category: category.value || undefined,
    search: search.value || undefined,
    sort: sort.value,
    order: order.value,
  })
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-wrap gap-4">
      <div class="flex-1 min-w-[200px]">
        <input
          v-model="search"
          type="text"
          placeholder="Search subscriptions..."
          class="input"
        />
      </div>

      <div>
        <select v-model="status" class="input">
          <option value="active">Active</option>
          <option value="cancelled">Cancelled</option>
          <option value="all">All</option>
        </select>
      </div>

      <div v-if="subscriptionsStore.categories.length > 0">
        <select v-model="category" class="input">
          <option value="">All Categories</option>
          <option v-for="cat in subscriptionsStore.categories" :key="cat" :value="cat">
            {{ cat }}
          </option>
        </select>
      </div>

      <div>
        <select v-model="sort" class="input">
          <option value="nextBillingDate">Sort by Next Billing</option>
          <option value="amount">Sort by Amount</option>
          <option value="name">Sort by Name</option>
        </select>
      </div>

      <div>
        <select v-model="order" class="input">
          <option value="asc">Ascending</option>
          <option value="desc">Descending</option>
        </select>
      </div>
    </div>
  </div>
</template>
