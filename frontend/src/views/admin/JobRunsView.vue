<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { adminService, type AdminError } from '@/services/adminService'
import type { JobRun, PaginatedResponse } from '@/types'
import { formatDateTime } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Pagination from '@/components/common/Pagination.vue'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { ClipboardList } from 'lucide-vue-next'

const jobRuns = ref<JobRun[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(1)
const totalPages = ref(1)
const totalItems = ref(0)
const pageSize = 20

const hasJobRuns = computed(() => jobRuns.value.length > 0)

async function fetchJobRuns(page = 1) {
  loading.value = true
  error.value = null
  try {
    const response: PaginatedResponse<JobRun> = await adminService.getJobRuns(page, pageSize)
    jobRuns.value = response.data
    currentPage.value = response.pagination.page
    totalPages.value = response.pagination.totalPages
    totalItems.value = response.pagination.total
  } catch (err) {
    const adminErr = err as AdminError
    error.value = adminErr.message || 'Failed to fetch job runs'
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  fetchJobRuns(page)
}

function formatDuration(startTime: string, finishTime?: string): string {
  if (!finishTime) return 'Running...'
  const start = new Date(startTime).getTime()
  const finish = new Date(finishTime).getTime()
  const durationMs = finish - start
  if (durationMs < 1000) return `${durationMs}ms`
  const seconds = Math.floor(durationMs / 1000)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}m ${remainingSeconds}s`
}

onMounted(() => {
  fetchJobRuns()
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Job Runs</h1>
      <p class="mt-1 text-sm text-muted-foreground">View history of scheduled and manual job executions</p>
    </div>

    <!-- Error Message -->
    <Alert v-if="error" variant="destructive" class="mb-6">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

    <!-- Loading State -->
    <div v-if="loading && !hasJobRuns" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <!-- Empty State -->
    <div
      v-else-if="!loading && !hasJobRuns && !error"
      class="text-center py-12"
    >
      <ClipboardList class="mx-auto h-12 w-12 text-gray-400" />
      <h3 class="mt-2 text-sm font-medium text-gray-900">No job runs</h3>
      <p class="mt-1 text-sm text-muted-foreground">No scheduled or manual jobs have been executed yet.</p>
    </div>

    <!-- Job Runs Table -->
    <Card v-else class="overflow-hidden">
      <CardContent class="p-0">
        <!-- Desktop Table -->
        <div class="hidden md:block overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-muted/50">
              <tr>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Job Name
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Trigger
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Start Time
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Duration
                </th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  Error
                </th>
              </tr>
            </thead>
            <tbody class="bg-background divide-y divide-gray-200">
              <tr v-for="jobRun in jobRuns" :key="jobRun.id" class="hover:bg-muted/50">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {{ jobRun.jobName }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <Badge
                    :variant="jobRun.status === 'SUCCESS' ? 'secondary' : 'destructive'"
                    :class="jobRun.status === 'SUCCESS' ? 'bg-green-100 text-green-800 border-green-200' : ''"
                  >
                    {{ jobRun.status }}
                  </Badge>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <Badge
                    variant="outline"
                    :class="jobRun.triggerType === 'SCHEDULED' ? 'bg-blue-50 text-blue-800 border-blue-200' : 'bg-purple-50 text-purple-800 border-purple-200'"
                  >
                    {{ jobRun.triggerType }}
                  </Badge>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">
                  {{ formatDateTime(jobRun.startTime) }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">
                  {{ formatDuration(jobRun.startTime, jobRun.finishTime) }}
                </td>
                <td class="px-6 py-4 text-sm text-red-600 max-w-xs truncate" :title="jobRun.errorMessage || ''">
                  {{ jobRun.errorMessage || '-' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Mobile Cards -->
        <div class="md:hidden divide-y divide-gray-200">
          <div v-for="jobRun in jobRuns" :key="jobRun.id" class="p-4 space-y-3">
            <div class="flex items-center justify-between">
              <span class="font-medium text-gray-900">{{ jobRun.jobName }}</span>
              <Badge
                :variant="jobRun.status === 'SUCCESS' ? 'secondary' : 'destructive'"
                :class="jobRun.status === 'SUCCESS' ? 'bg-green-100 text-green-800 border-green-200' : ''"
              >
                {{ jobRun.status }}
              </Badge>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-muted-foreground">Trigger:</span>
              <Badge
                variant="outline"
                :class="jobRun.triggerType === 'SCHEDULED' ? 'bg-blue-50 text-blue-800 border-blue-200' : 'bg-purple-50 text-purple-800 border-purple-200'"
              >
                {{ jobRun.triggerType }}
              </Badge>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-muted-foreground">Start Time:</span>
              <span class="text-gray-900">{{ formatDateTime(jobRun.startTime) }}</span>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-muted-foreground">Duration:</span>
              <span class="text-gray-900">{{ formatDuration(jobRun.startTime, jobRun.finishTime) }}</span>
            </div>
            <div v-if="jobRun.errorMessage" class="text-sm">
              <span class="text-muted-foreground">Error:</span>
              <p class="text-red-600 mt-1">{{ jobRun.errorMessage }}</p>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <Pagination
          v-if="totalPages > 1"
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-items="totalItems"
          @page-change="handlePageChange"
        />
      </CardContent>
    </Card>
  </div>
</template>
