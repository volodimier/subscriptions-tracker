import api from './api'
import type { Service, CreateServiceRequest, UpdateServiceRequest, PaginatedResponse } from '@/types'

export const serviceService = {
  async getServices(search?: string, page = 1, limit = 20): Promise<PaginatedResponse<Service>> {
    const params = new URLSearchParams()
    if (search) params.append('search', search)
    params.append('page', page.toString())
    params.append('limit', limit.toString())

    const response = await api.get<PaginatedResponse<Service>>(`/services?${params}`)
    return response.data
  },

  async getAllServices(): Promise<Service[]> {
    const response = await api.get<Service[]>('/services/all')
    return response.data
  },

  async getService(id: number): Promise<Service> {
    const response = await api.get<Service>(`/services/${id}`)
    return response.data
  },

  async createService(data: CreateServiceRequest): Promise<Service> {
    const response = await api.post<Service>('/services', data)
    return response.data
  },

  async updateService(id: number, data: UpdateServiceRequest): Promise<Service> {
    const response = await api.put<Service>(`/services/${id}`, data)
    return response.data
  },

  async deleteService(id: number): Promise<void> {
    await api.delete(`/services/${id}`)
  },

  async getCategories(): Promise<string[]> {
    const response = await api.get<string[]>('/services/categories')
    return response.data
  },
}
