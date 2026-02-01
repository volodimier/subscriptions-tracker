<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useServicesStore } from '@/stores/services'
import type { Service, CreateServiceRequest } from '@/types'
import ServiceForm from './ServiceForm.vue'

const props = defineProps<{
  modelValue?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const servicesStore = useServicesStore()
const showDropdown = ref(false)
const showCreateForm = ref(false)
const searchQuery = ref('')
const formError = ref('')

onMounted(async () => {
  await servicesStore.fetchAllServices()
})

const filteredServices = computed(() => {
  if (!searchQuery.value) return servicesStore.allServices
  const search = searchQuery.value.toLowerCase()
  return servicesStore.allServices.filter(s => s.name.toLowerCase().includes(search))
})

const selectedService = computed(() => {
  if (!props.modelValue) return null
  return servicesStore.allServices.find(s => s.id === props.modelValue)
})

function selectService(service: Service) {
  emit('update:modelValue', service.id)
  showDropdown.value = false
  searchQuery.value = ''
}

function openCreateForm() {
  showDropdown.value = false
  showCreateForm.value = true
  formError.value = ''
}

async function handleCreateService(data: CreateServiceRequest) {
  formError.value = ''
  try {
    const service = await servicesStore.createService(data)
    emit('update:modelValue', service.id)
    showCreateForm.value = false
  } catch (err: unknown) {
    const axiosError = err as { response?: { data?: { message?: string } } }
    formError.value = axiosError.response?.data?.message || 'Failed to create service'
  }
}

function closeDropdown() {
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}
</script>

<template>
  <div class="relative">
    <button
      type="button"
      class="input text-left flex justify-between items-center"
      @click="showDropdown = !showDropdown"
    >
      <span :class="selectedService ? 'text-gray-900' : 'text-gray-500'">
        {{ selectedService?.name || 'Select a service...' }}
      </span>
      <svg class="w-5 h-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <div
      v-if="showDropdown"
      class="absolute z-20 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg"
    >
      <div class="p-2">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search services..."
          class="input text-sm"
          @blur="closeDropdown"
        />
      </div>

      <div class="max-h-48 overflow-auto">
        <button
          v-for="service in filteredServices"
          :key="service.id"
          type="button"
          class="w-full text-left px-4 py-2 hover:bg-gray-100 flex items-center space-x-2"
          @click="selectService(service)"
        >
          <div v-if="service.faviconUrl" class="w-6 h-6 flex-shrink-0">
            <img :src="service.faviconUrl" :alt="service.name" class="w-full h-full object-contain rounded" />
          </div>
          <div v-else class="w-6 h-6 bg-primary-100 rounded flex items-center justify-center">
            <span class="text-primary-600 text-xs font-semibold">{{ service.name.charAt(0) }}</span>
          </div>
          <span class="text-sm">{{ service.name }}</span>
        </button>
      </div>

      <div class="border-t border-gray-200 p-2">
        <button
          type="button"
          class="w-full text-left px-4 py-2 text-primary-600 hover:bg-primary-50 rounded text-sm font-medium"
          @click="openCreateForm"
        >
          + Create New Service
        </button>
      </div>
    </div>

    <!-- Create Service Modal -->
    <div v-if="showCreateForm" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCreateForm = false" />
        <div class="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
          <h2 class="text-xl font-semibold mb-4">Create New Service</h2>
          <ServiceForm
            :error="formError"
            @save="handleCreateService"
            @cancel="showCreateForm = false"
          />
        </div>
      </div>
    </div>
  </div>
</template>
