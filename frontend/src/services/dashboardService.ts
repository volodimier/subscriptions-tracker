import api from './api'
import type { DashboardSummary, Projection } from '@/types'

export const dashboardService = {
  async getSummary(startDate?: string, endDate?: string): Promise<DashboardSummary> {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)

    const response = await api.get<DashboardSummary>(`/dashboard/summary?${params}`)
    return response.data
  },

  async getProjection(): Promise<Projection> {
    const response = await api.get<Projection>('/dashboard/projection')
    return response.data
  },
}
