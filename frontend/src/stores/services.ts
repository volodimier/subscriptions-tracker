import { defineStore } from 'pinia'
import { ref } from 'vue'
import { serviceService } from '@/services/serviceService'
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '@/types'

export const useServicesStore = defineStore('services', () => {
  const services = ref<Service[]>([])
  const allServices = ref<Service[]>([])
  const categories = ref<string[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    page: 1,
    limit: 20,
    total: 0,
    totalPages: 0,
  })

  async function fetchServices(search?: string, page = 1, limit = 20) {
    loading.value = true
    error.value = null
    try {
      const response = await serviceService.getServices(search, page, limit)
      services.value = response.data
      pagination.value = response.pagination
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch services'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAllServices() {
    try {
      allServices.value = await serviceService.getAllServices()
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to fetch services'
      throw err
    }
  }

  async function fetchCategories() {
    try {
      categories.value = await serviceService.getCategories()
    } catch {
      // Silently fail for categories
    }
  }

  async function createService(data: CreateServiceRequest): Promise<Service> {
    loading.value = true
    error.value = null
    try {
      const service = await serviceService.createService(data)
      await fetchServices()
      await fetchAllServices()
      return service
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to create service'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateService(id: number, data: UpdateServiceRequest): Promise<Service> {
    loading.value = true
    error.value = null
    try {
      const service = await serviceService.updateService(id, data)
      const index = services.value.findIndex(s => s.id === id)
      if (index !== -1) {
        services.value[index] = service
      }
      await fetchAllServices()
      return service
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to update service'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteService(id: number) {
    loading.value = true
    error.value = null
    try {
      await serviceService.deleteService(id)
      services.value = services.value.filter(s => s.id !== id)
      allServices.value = allServices.value.filter(s => s.id !== id)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Failed to delete service'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    services,
    allServices,
    categories,
    loading,
    error,
    pagination,
    fetchServices,
    fetchAllServices,
    fetchCategories,
    createService,
    updateService,
    deleteService,
  }
})
