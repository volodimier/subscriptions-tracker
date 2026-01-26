<script setup lang="ts">
defineProps<{
  size?: 'sm' | 'md' | 'lg'
}>()
</script>

<template>
  <div class="flex justify-center items-center">
    <div
      :class="[
        'animate-spin rounded-full border-t-2 border-b-2 border-primary-600',
        {
          'h-4 w-4': size === 'sm',
          'h-8 w-8': size === 'md' || !size,
          'h-12 w-12': size === 'lg',
        },
      ]"
    ></div>
  </div>
</template>
