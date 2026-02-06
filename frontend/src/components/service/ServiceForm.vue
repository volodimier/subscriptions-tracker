<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '@/types'
import { CATEGORY_SUGGESTIONS } from '@/utils/constants'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'

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

const selectClass = "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm"
</script>

<template>
  <form class="space-y-4" @submit.prevent="handleSubmit">
    <Alert v-if="error" variant="destructive">
      <AlertDescription>{{ error }}</AlertDescription>
    </Alert>

    <div>
      <Label for="name">Service Name *</Label>
      <Input
        id="name"
        v-model="name"
        type="text"
        required
        placeholder="e.g., Netflix, Spotify"
        class="mt-2"
      />
    </div>

    <div>
      <Label for="category">Category</Label>
      <select
        id="category"
        v-model="category"
        :class="['mt-2', selectClass]"
      >
        <option value="">Select a category</option>
        <option v-for="cat in CATEGORY_SUGGESTIONS" :key="cat" :value="cat">
          {{ cat }}
        </option>
      </select>
    </div>

    <div>
      <Label for="websiteUrl">Website URL</Label>
      <Input
        id="websiteUrl"
        v-model="websiteUrl"
        type="text"
        placeholder="example.com"
        class="mt-2"
      />
    </div>

    <div class="flex justify-end space-x-3 pt-4">
      <Button type="button" variant="outline" @click="emit('cancel')">
        Cancel
      </Button>
      <Button type="submit" :disabled="!isValid">
        {{ service ? 'Save Changes' : 'Create Service' }}
      </Button>
    </div>
  </form>
</template>
