<script setup lang="ts">
import type { Service } from '@/types'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ExternalLink } from 'lucide-vue-next'

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
  <Card class="transition-shadow hover:shadow-md">
    <CardContent class="p-5">
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
        <div class="flex space-x-1">
          <Button variant="ghost" size="sm" @click="emit('edit', service)">
            Edit
          </Button>
          <Button
            variant="ghost"
            size="sm"
            :disabled="!canDelete"
            :class="canDelete ? 'text-red-600 hover:text-red-700 hover:bg-red-50' : ''"
            :title="!canDelete ? 'Cannot delete - service is in use' : ''"
            @click="emit('delete', service)"
          >
            Delete
          </Button>
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
        <ExternalLink class="w-4 h-4 ml-1" />
      </a>
    </CardContent>
  </Card>
</template>
