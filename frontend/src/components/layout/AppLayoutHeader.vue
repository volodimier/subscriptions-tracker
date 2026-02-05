<script setup lang="ts">
import { ref } from 'vue'
import { useSidebar } from '@/composables/useSidebar'
import { useAuthStore } from '@/stores/auth'
import { Button } from '@/components/ui/button'
import { Menu, LogOut, User, Settings } from 'lucide-vue-next'

const authStore = useAuthStore()
const { isMobile, toggle } = useSidebar()

const showUserMenu = ref(false)

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function closeUserMenu() {
  showUserMenu.value = false
}

async function handleLogout() {
  closeUserMenu()
  await authStore.logout()
}
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

    <!-- User menu -->
    <div class="relative">
      <Button
        variant="ghost"
        size="sm"
        class="flex items-center gap-2"
        @click="toggleUserMenu"
      >
        <div class="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-primary">
          <User class="h-4 w-4" />
        </div>
        <span class="hidden sm:inline-block text-sm font-medium">
          {{ authStore.user?.email }}
        </span>
      </Button>

      <!-- Dropdown menu -->
      <Transition
        enter-active-class="transition duration-100 ease-out"
        enter-from-class="transform scale-95 opacity-0"
        enter-to-class="transform scale-100 opacity-100"
        leave-active-class="transition duration-75 ease-in"
        leave-from-class="transform scale-100 opacity-100"
        leave-to-class="transform scale-95 opacity-0"
      >
        <div
          v-if="showUserMenu"
          class="absolute right-0 mt-2 w-48 origin-top-right rounded-lg border bg-card py-1 shadow-lg ring-1 ring-black/5"
        >
          <div class="border-b px-4 py-2">
            <p class="truncate text-sm font-medium text-foreground">
              {{ authStore.user?.email }}
            </p>
            <p class="truncate text-xs text-muted-foreground">
              {{ authStore.user?.role }}
            </p>
          </div>
          <router-link
            to="/settings"
            class="flex items-center gap-2 px-4 py-2 text-sm text-foreground hover:bg-accent transition-colors"
            @click="closeUserMenu"
          >
            <Settings class="h-4 w-4" />
            Settings
          </router-link>
          <button
            class="flex w-full items-center gap-2 px-4 py-2 text-sm text-destructive hover:bg-accent transition-colors"
            @click="handleLogout"
          >
            <LogOut class="h-4 w-4" />
            Logout
          </button>
        </div>
      </Transition>

      <!-- Click outside to close -->
      <div
        v-if="showUserMenu"
        class="fixed inset-0 z-[-1]"
        @click="closeUserMenu"
      />
    </div>
  </header>
</template>
