<script setup lang="ts">
import { useSidebar } from '@/composables/useSidebar'
import { Button } from '@/components/ui/button'
import { Menu } from 'lucide-vue-next'

const { isMobile, toggle } = useSidebar()
</script>

<template>
  <header class="sticky top-0 z-30 flex h-14 items-center gap-4 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 px-4 sm:px-6">
    <!-- Mobile menu button -->
    <Button
      v-if="isMobile"
      variant="ghost"
      size="icon-sm"
      @click="toggle"
    >
      <Menu class="h-5 w-5" />
      <span class="sr-only">Toggle sidebar</span>
    </Button>

    <!-- Spacer -->
    <div class="flex-1" />
  </header>
</template>
