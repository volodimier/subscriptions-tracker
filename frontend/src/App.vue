<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/common/AppHeader.vue'

const authStore = useAuthStore()

onMounted(async () => {
  await authStore.checkAuth()
})
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <AppHeader v-if="authStore.isAuthenticated" />
    <main class="flex-1">
      <router-view />
    </main>
  </div>
</template>
