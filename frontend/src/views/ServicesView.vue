<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useServicesStore } from '@/stores/services'
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '@/types'
import ServiceCard from '@/components/service/ServiceCard.vue'
import ServiceForm from '@/components/service/ServiceForm.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Pagination from '@/components/common/Pagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Package } from 'lucide-vue-next'

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
      <Button @click="openCreateForm">
        + Add Service
      </Button>
    </div>

    <div class="mb-6">
      <Input
        v-model="searchQuery"
        type="text"
        placeholder="Search services..."
        class="max-w-md"
      />
    </div>

    <Alert v-if="servicesStore.error" variant="destructive" class="mb-6">
      <AlertDescription>{{ servicesStore.error }}</AlertDescription>
    </Alert>

    <div v-if="servicesStore.loading" class="flex justify-center py-12">
      <LoadingSpinner size="lg" />
    </div>

    <div v-else-if="servicesStore.services.length === 0" class="text-center py-12">
      <div class="text-gray-500">
        <Package class="mx-auto h-12 w-12" />
        <h3 class="mt-2 text-sm font-medium text-gray-900">No services</h3>
        <p class="mt-1 text-sm text-gray-500">Get started by creating a new service.</p>
        <Button class="mt-4" @click="openCreateForm">
          + Add Service
        </Button>
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

    <!-- Service Form Dialog -->
    <Dialog v-model:open="showForm">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {{ editingService ? 'Edit Service' : 'Add New Service' }}
          </DialogTitle>
        </DialogHeader>
        <ServiceForm
          :service="editingService"
          :error="formError"
          @save="handleSave"
          @cancel="handleCancel"
        />
      </DialogContent>
    </Dialog>

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
