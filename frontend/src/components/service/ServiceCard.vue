<script setup lang="ts">
import type { Service } from '@/types'

const props = defineProps<{
  service: Service
}>()

const emit = defineEmits<{
  edit: [service: Service]
  delete: [service: Service]
}>()

const canDelete = (props.service.subscriptionCount ?? 0) === 0
</script>

<template>
  <div class="card-hover">
    <div class="flex items-start justify-between">
      <div class="flex items-center space-x-3">
        <div v-if="service.faviconUrl" class="w-10 h-10 flex-shrink-0 rounded-lg bg-gray-50 p-1">
          <img :src="service.faviconUrl" :alt="service.name" class="w-full h-full object-contain" />
        </div>
        <div v-else class="w-10 h-10 bg-gradient-to-br from-primary-50 to-primary-100 rounded-lg flex items-center justify-center">
          <span class="text-primary-600 font-semibold text-lg">{{ service.name.charAt(0).toUpperCase() }}</span>
        </div>
        <div>
          <h3 class="font-semibold text-gray-900">{{ service.name }}</h3>
          <p v-if="service.category" class="text-sm text-gray-500">{{ service.category }}</p>
        </div>
      </div>
    </div>

    <div class="mt-4 flex items-center justify-between text-sm">
      <span class="text-gray-500">
        Used in {{ service.subscriptionCount || 0 }} subscription(s)
      </span>
      <div class="flex space-x-3">
        <button
          @click="emit('edit', service)"
          class="text-primary-600 hover:text-primary-700 font-medium transition-colors"
        >
          Edit
        </button>
        <button
          @click="emit('delete', service)"
          :disabled="!canDelete"
          :class="[
            'font-medium transition-colors',
            canDelete
              ? 'text-red-600 hover:text-red-700'
              : 'text-gray-400 cursor-not-allowed'
          ]"
          :title="!canDelete ? 'Cannot delete - service is in use' : ''"
        >
          Delete
        </button>
      </div>
    </div>

    <a
      v-if="service.websiteUrl"
      :href="service.websiteUrl"
      target="_blank"
      rel="noopener noreferrer"
      class="mt-3 text-sm text-primary-600 hover:text-primary-700 inline-flex items-center transition-colors"
    >
      Visit website
      <svg class="w-4 h-4 ml-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
      </svg>
    </a>
  </div>
</template>
