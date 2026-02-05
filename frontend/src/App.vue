<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { AppLayout } from '@/components/layout'

const authStore = useAuthStore()
const route = useRoute()

// Auth routes that should not have the sidebar layout
const authRoutes = ['login', 'register']

const shouldUseSidebarLayout = computed(() => {
  return authStore.isAuthenticated && !authRoutes.includes(route.name as string)
})

onMounted(async () => {
  await authStore.checkAuth()
})
</script>

<template>
  <AppLayout v-if="shouldUseSidebarLayout">
    <router-view />
  </AppLayout>
  <router-view v-else />
</template>
