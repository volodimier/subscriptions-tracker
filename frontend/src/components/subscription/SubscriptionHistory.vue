<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { subscriptionService } from '@/services/subscriptionService'
import { computeTimeline } from '@/utils/historyDiff'
import { formatDateTime } from '@/utils/formatters'
import type { SubscriptionHistoryEntry } from '@/types'
import type { TimelineEntry } from '@/utils/historyDiff'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import { Card, CardContent } from '@/components/ui/card'

const props = defineProps<{
  subscriptionId: number
}>()

const loading = ref(false)
const error = ref<string | null>(null)
const timeline = ref<TimelineEntry[]>([])

onMounted(async () => {
  await fetchHistory()
})

async function fetchHistory() {
  loading.value = true
  error.value = null
  try {
    const entries: SubscriptionHistoryEntry[] = await subscriptionService.getSubscriptionHistory(props.subscriptionId)
    timeline.value = computeTimeline(entries)
  } catch {
    error.value = 'Failed to load change history'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Card>
    <CardContent class="p-6">
      <h3 class="font-semibold text-gray-900 mb-4">Change History</h3>

      <div v-if="loading" class="flex justify-center py-8">
        <LoadingSpinner size="md" />
      </div>

      <div v-else-if="error" class="text-center py-8">
        <p class="text-red-500 text-sm">{{ error }}</p>
      </div>

      <div v-else-if="timeline.length === 0" class="text-center py-8">
        <p class="text-gray-500 text-sm">No change history available yet.</p>
      </div>

      <div v-else class="relative">
        <!-- Timeline line -->
        <div class="absolute left-3 top-2 bottom-2 w-px bg-gray-200" aria-hidden="true" />

        <ul class="space-y-4" role="list" aria-label="Subscription change history">
          <li
            v-for="(entry, index) in timeline"
            :key="index"
            class="relative pl-8"
          >
            <!-- Timeline dot -->
            <div
              :class="[
                'absolute left-1.5 top-1.5 w-3 h-3 rounded-full border-2 border-white',
                index === timeline.length - 1 ? 'bg-green-500' : 'bg-primary'
              ]"
              aria-hidden="true"
            />

            <div class="text-sm">
              <time class="text-gray-500 font-medium" :datetime="entry.date">
                {{ formatDateTime(entry.date) }}
              </time>
              <ul class="mt-1 space-y-0.5 list-none p-0">
                <li
                  v-for="(change, changeIndex) in entry.changes"
                  :key="changeIndex"
                  class="text-gray-700"
                >
                  {{ change }}
                </li>
              </ul>
            </div>
          </li>
        </ul>
      </div>
    </CardContent>
  </Card>
</template>
