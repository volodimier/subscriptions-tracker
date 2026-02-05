<script setup lang="ts">
import { provideSidebar } from '@/composables/useSidebar'
import AppSidebar from './AppSidebar.vue'
import AppLayoutHeader from './AppLayoutHeader.vue'

provideSidebar()
</script>

<template>
  <div class="flex min-h-screen bg-background">
    <AppSidebar />
    <div class="flex flex-1 flex-col">
      <AppLayoutHeader />
      <main class="flex-1 overflow-auto">
        <slot />
      </main>
    </div>
  </div>
</template>
