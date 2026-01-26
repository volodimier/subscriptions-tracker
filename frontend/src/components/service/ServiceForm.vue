<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '@/types'
import { CATEGORY_SUGGESTIONS } from '@/utils/constants'

const props = defineProps<{
  service?: Service | null
  error?: string
}>()

const emit = defineEmits<{
  save: [data: CreateServiceRequest | UpdateServiceRequest]
  cancel: []
}>()

const name = ref('')
const category = ref('')
const websiteUrl = ref('')

onMounted(() => {
  if (props.service) {
    name.value = props.service.name
    category.value = props.service.category || ''
    websiteUrl.value = props.service.websiteUrl || ''
  }
})

/**
 * Normalizes a URL by adding https:// scheme if missing.
 * Returns empty string if the input doesn't look like a valid URL.
 */
function normalizeUrl(input: string): string {
  const trimmed = input.trim()
  if (!trimmed) {
    return ''
  }

  // If already has a scheme, return as-is
  if (trimmed.toLowerCase().startsWith('http://') || trimmed.toLowerCase().startsWith('https://')) {
    return trimmed
  }

  // Reject URLs with other schemes
  if (trimmed.includes('://')) {
    return ''
  }

  // Basic validation: must contain a dot and no spaces (looks like a domain)
  if (!trimmed.includes('.') || trimmed.includes(' ')) {
    return ''
  }

  // Prepend https://
  return 'https://' + trimmed
}

const isValid = computed(() => {
  return name.value.trim() !== ''
})

function handleSubmit() {
  if (!isValid.value) return

  const normalizedUrl = normalizeUrl(websiteUrl.value)

  const data: CreateServiceRequest = {
    name: name.value.trim(),
    category: category.value || undefined,
    websiteUrl: normalizedUrl || undefined,
  }

  emit('save', data)
}
</script>

<template>
  <form @submit.prevent="handleSubmit" class="space-y-4">
    <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
      {{ error }}
    </div>

    <div>
      <label for="name" class="label">Service Name *</label>
      <input
        id="name"
        v-model="name"
        type="text"
        required
        class="input"
        placeholder="e.g., Netflix, Spotify"
      />
    </div>

    <div>
      <label for="category" class="label">Category</label>
      <select
        id="category"
        v-model="category"
        class="input"
      >
        <option value="">Select a category</option>
        <option v-for="cat in CATEGORY_SUGGESTIONS" :key="cat" :value="cat">
          {{ cat }}
        </option>
      </select>
    </div>

    <div>
      <label for="websiteUrl" class="label">Website URL</label>
      <input
        id="websiteUrl"
        v-model="websiteUrl"
        type="text"
        class="input"
        placeholder="example.com"
      />
    </div>

    <div class="flex justify-end space-x-3 pt-4">
      <button type="button" @click="emit('cancel')" class="btn-secondary">
        Cancel
      </button>
      <button type="submit" :disabled="!isValid" class="btn-primary">
        {{ service ? 'Save Changes' : 'Create Service' }}
      </button>
    </div>
  </form>
</template>
