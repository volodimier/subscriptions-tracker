<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const showUserMenu = ref(false)

async function handleLogout() {
  await authStore.logout()
}

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function closeUserMenu() {
  showUserMenu.value = false
}
</script>

<template>
  <header class="bg-white/80 backdrop-blur-md shadow-soft border-b border-gray-200/60 sticky top-0 z-40">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between h-16">
        <div class="flex items-center">
          <router-link to="/" class="flex items-center">
            <span class="text-xl font-bold text-primary-600">SubscriptionTracker</span>
          </router-link>

          <nav class="hidden md:ml-8 md:flex md:space-x-1">
            <router-link
              to="/"
              class="px-3.5 py-2 rounded-lg text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-primary-50/50 transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Dashboard
            </router-link>
            <router-link
              to="/subscriptions"
              class="px-3.5 py-2 rounded-lg text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-primary-50/50 transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Subscriptions
            </router-link>
            <router-link
              to="/services"
              class="px-3.5 py-2 rounded-lg text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-primary-50/50 transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Services
            </router-link>
            <router-link
              to="/statistics"
              class="px-3.5 py-2 rounded-lg text-sm font-medium text-gray-600 hover:text-primary-600 hover:bg-primary-50/50 transition-colors"
              active-class="text-primary-600 bg-primary-50"
            >
              Statistics
            </router-link>
          </nav>
        </div>

        <div class="flex items-center">
          <div class="relative">
            <button
              class="flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:text-primary-600 hover:bg-gray-50"
              @click="toggleUserMenu"
            >
              <span>{{ authStore.user?.email }}</span>
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <div
              v-if="showUserMenu"
              class="absolute right-0 mt-2 w-48 bg-white/95 backdrop-blur-sm rounded-xl shadow-soft-lg py-1.5 z-50 border border-gray-200/60"
              @click="closeUserMenu"
            >
              <router-link
                to="/settings"
                class="block px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Settings
              </router-link>
              <button
                class="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                @click="handleLogout"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>
