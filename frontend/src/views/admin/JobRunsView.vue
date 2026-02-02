<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { adminService, type AdminError } from '@/services/adminService'
import type { JobRun, PaginatedResponse } from '@/types'
import { formatDateTime } from '@/utils/formatters'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Pagination from '@/components/common/Pagination.vue'

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

function getStatusClass(status: string): string {
  return status === 'SUCCESS'
    ? 'bg-green-100 text-green-800'
    : 'bg-red-100 text-red-800'
}

function getTriggerTypeClass(triggerType: string): string {
  return triggerType === 'SCHEDULED'
    ? 'bg-blue-100 text-blue-800'
    : 'bg-purple-100 text-purple-800'
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
      <p class="mt-1 text-sm text-gray-500">View history of scheduled and manual job executions</p>
    </div>

    <!-- Error Message -->
    <div
      v-if="error"
      class="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg"
    >
      {{ error }}
    </div>

    <!-- Loading State -->
    <div v-if="loading && !hasJobRuns" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <!-- Empty State -->
    <div
      v-else-if="!loading && !hasJobRuns && !error"
      class="text-center py-12"
    >
      <svg
        class="mx-auto h-12 w-12 text-gray-400"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
        />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">No job runs</h3>
      <p class="mt-1 text-sm text-gray-500">No scheduled or manual jobs have been executed yet.</p>
    </div>

    <!-- Job Runs Table -->
    <div v-else class="card overflow-hidden">
      <!-- Desktop Table -->
      <div class="hidden md:block overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Job Name
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Trigger
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Start Time
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Duration
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Error
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="jobRun in jobRuns" :key="jobRun.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ jobRun.jobName }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span
                  :class="[
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    getStatusClass(jobRun.status)
                  ]"
                >
                  {{ jobRun.status }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span
                  :class="[
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    getTriggerTypeClass(jobRun.triggerType)
                  ]"
                >
                  {{ jobRun.triggerType }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ formatDateTime(jobRun.startTime) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
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
            <span
              :class="[
                'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                getStatusClass(jobRun.status)
              ]"
            >
              {{ jobRun.status }}
            </span>
          </div>
          <div class="flex items-center justify-between text-sm">
            <span class="text-gray-500">Trigger:</span>
            <span
              :class="[
                'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                getTriggerTypeClass(jobRun.triggerType)
              ]"
            >
              {{ jobRun.triggerType }}
            </span>
          </div>
          <div class="flex items-center justify-between text-sm">
            <span class="text-gray-500">Start Time:</span>
            <span class="text-gray-900">{{ formatDateTime(jobRun.startTime) }}</span>
          </div>
          <div class="flex items-center justify-between text-sm">
            <span class="text-gray-500">Duration:</span>
            <span class="text-gray-900">{{ formatDuration(jobRun.startTime, jobRun.finishTime) }}</span>
          </div>
          <div v-if="jobRun.errorMessage" class="text-sm">
            <span class="text-gray-500">Error:</span>
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
    </div>
  </div>
</template>
