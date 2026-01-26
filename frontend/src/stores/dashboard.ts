import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardService } from '@/services/dashboardService'
import type { DashboardSummary, Projection } from '@/types'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummary | null>(null)
  const projection = ref<Projection | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSummary(startDate?: string, endDate?: string) {
    loading.value = true
    error.value = null
    try {
      summary.value = await dashboardService.getSummary(startDate, endDate)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch summary'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchProjection() {
    loading.value = true
    error.value = null
    try {
      projection.value = await dashboardService.getProjection()
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch projection'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    summary,
    projection,
    loading,
    error,
    fetchSummary,
    fetchProjection,
  }
})
