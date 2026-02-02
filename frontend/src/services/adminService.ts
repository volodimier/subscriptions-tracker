import api from './api'
import type { JobRun, PaginatedResponse } from '@/types'

export interface AdminError {
  status: number
  message: string
}

function isAdminError(error: unknown): error is { response?: { status?: number; data?: { message?: string } } } {
  return typeof error === 'object' && error !== null && 'response' in error
}

function handleAdminError(error: unknown): never {
  if (isAdminError(error)) {
    const status = error.response?.status || 500
    const message = error.response?.data?.message || 'An error occurred'

    if (status === 403) {
      throw { status: 403, message: 'Access denied. Admin privileges required.' } as AdminError
    }

    throw { status, message } as AdminError
  }

  throw { status: 500, message: 'An unexpected error occurred' } as AdminError
}

export const adminService = {
  async getJobRuns(page = 1, size = 20): Promise<PaginatedResponse<JobRun>> {
    try {
      const response = await api.get<PaginatedResponse<JobRun>>(
        `/admin/job-runs?page=${page}&limit=${size}`
      )
      return response.data
    } catch (error) {
      handleAdminError(error)
    }
  },

  async getJobRunById(id: number): Promise<JobRun> {
    try {
      const response = await api.get<JobRun>(`/admin/job-runs/${id}`)
      return response.data
    } catch (error) {
      handleAdminError(error)
    }
  },

  async refreshFxRates(): Promise<{ message: string; updatedAt: string; rates: Record<string, number> }> {
    try {
      const response = await api.post<{ message: string; updatedAt: string; rates: Record<string, number> }>(
        '/fx-rates/refresh'
      )
      return response.data
    } catch (error) {
      handleAdminError(error)
    }
  },
}
