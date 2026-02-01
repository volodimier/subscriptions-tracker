<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useServicesStore } from '@/stores/services'
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '@/types'
import ServiceCard from '@/components/service/ServiceCard.vue'
import ServiceForm from '@/components/service/ServiceForm.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Pagination from '@/components/common/Pagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const servicesStore = useServicesStore()

const searchQuery = ref('')
const showForm = ref(false)
const editingService = ref<Service | null>(null)
const showDeleteConfirm = ref(false)
const serviceToDelete = ref<Service | null>(null)
const formError = ref('')

onMounted(async () => {
  await servicesStore.fetchServices()
})

watch(searchQuery, async (value) => {
  await servicesStore.fetchServices(value, 1)
})

async function handlePageChange(page: number) {
  await servicesStore.fetchServices(searchQuery.value, page)
}

function openCreateForm() {
  editingService.value = null
  formError.value = ''
  showForm.value = true
}

function openEditForm(service: Service) {
  editingService.value = service
  formError.value = ''
  showForm.value = true
}

async function handleSave(data: CreateServiceRequest | UpdateServiceRequest) {
  formError.value = ''
  try {
    if (editingService.value) {
      await servicesStore.updateService(editingService.value.id, data)
    } else {
      await servicesStore.createService(data as CreateServiceRequest)
    }
    showForm.value = false
    editingService.value = null
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    formError.value = axiosError.response?.data?.message || 'Failed to save service'
  }
}

function handleCancel() {
  showForm.value = false
  editingService.value = null
  formError.value = ''
}

function confirmDelete(service: Service) {
  serviceToDelete.value = service
  showDeleteConfirm.value = true
}

async function handleDelete() {
  if (serviceToDelete.value) {
    try {
      await servicesStore.deleteService(serviceToDelete.value.id)
    } catch {
      // Error is handled in store
    }
  }
  showDeleteConfirm.value = false
  serviceToDelete.value = null
}

function cancelDelete() {
  showDeleteConfirm.value = false
  serviceToDelete.value = null
}
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">My Services</h1>
        <p class="text-gray-600">Manage your subscription services catalog</p>
      </div>
      <button class="btn-primary" @click="openCreateForm">
        + Add Service
      </button>
    </div>

    <div class="mb-6">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search services..."
        class="input max-w-md"
      />
    </div>

    <div v-if="servicesStore.error" class="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
      {{ servicesStore.error }}
    </div>

    <div v-if="servicesStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <div v-else-if="servicesStore.services.length === 0" class="text-center py-12">
      <div class="text-gray-500">
        <svg class="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">No services</h3>
        <p class="mt-1 text-sm text-gray-500">Get started by creating a new service.</p>
        <button class="mt-4 btn-primary" @click="openCreateForm">
          + Add Service
        </button>
      </div>
    </div>

    <div v-else class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      <ServiceCard
        v-for="service in servicesStore.services"
        :key="service.id"
        :service="service"
        @edit="openEditForm"
        @delete="confirmDelete"
      />
    </div>

    <div v-if="servicesStore.pagination.totalPages > 1" class="mt-6">
      <Pagination
        :current-page="servicesStore.pagination.page"
        :total-pages="servicesStore.pagination.totalPages"
        :total-items="servicesStore.pagination.total"
        @page-change="handlePageChange"
      />
    </div>

    <!-- Service Form Modal -->
    <div v-if="showForm" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="handleCancel" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
          <h2 class="text-xl font-semibold mb-4">
            {{ editingService ? 'Edit Service' : 'Add New Service' }}
          </h2>
          <ServiceForm
            :service="editingService"
            :error="formError"
            @save="handleSave"
            @cancel="handleCancel"
          />
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Dialog -->
    <ConfirmDialog
      v-if="showDeleteConfirm"
      title="Delete Service"
      :message="`Are you sure you want to delete '${serviceToDelete?.name}'? This action cannot be undone.`"
      confirm-text="Delete"
      type="danger"
      @confirm="handleDelete"
      @cancel="cancelDelete"
    />
  </div>
</template>
