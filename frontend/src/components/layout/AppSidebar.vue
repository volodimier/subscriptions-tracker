<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useSidebar } from '@/composables/useSidebar'
import { useAuthStore } from '@/stores/auth'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  LayoutDashboard,
  CreditCard,
  Layers,
  BarChart3,
  Settings,
  Shield,
  X,
  ChevronLeft,
  ChevronRight,
} from 'lucide-vue-next'

const route = useRoute()
const authStore = useAuthStore()
const { isCollapsed, isMobile, isMobileOpen, toggle, closeMobile } = useSidebar()

interface NavItem {
  title: string
  href: string
  icon: typeof LayoutDashboard
  adminOnly?: boolean
}

const navItems: NavItem[] = [
  { title: 'Dashboard', href: '/', icon: LayoutDashboard },
  { title: 'Subscriptions', href: '/subscriptions', icon: CreditCard },
  { title: 'Services', href: '/services', icon: Layers },
  { title: 'Statistics', href: '/statistics', icon: BarChart3 },
  { title: 'Settings', href: '/settings', icon: Settings },
]

const adminNavItems: NavItem[] = [
  { title: 'Job Runs', href: '/admin/job-runs', icon: Shield, adminOnly: true },
]

const filteredNavItems = computed(() => navItems)
const filteredAdminItems = computed(() =>
  authStore.isAdmin ? adminNavItems : []
)

function isActive(href: string): boolean {
  if (href === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(href)
}

function handleNavClick() {
  if (isMobile.value) {
    closeMobile()
  }
}
</script>

<template>
  <!-- Mobile overlay -->
  <Transition
    enter-active-class="transition-opacity duration-300"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition-opacity duration-300"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div
      v-if="isMobile && isMobileOpen"
      class="fixed inset-0 z-40 bg-black/50"
      @click="closeMobile"
    />
  </Transition>

  <!-- Sidebar -->
  <aside
    :class="cn(
      'flex flex-col border-r bg-card transition-all duration-300 ease-in-out',
      isMobile
        ? 'fixed inset-y-0 left-0 z-50 w-64 transform'
        : 'relative',
      isMobile && !isMobileOpen && '-translate-x-full',
      isMobile && isMobileOpen && 'translate-x-0',
      !isMobile && isCollapsed && 'w-16',
      !isMobile && !isCollapsed && 'w-64'
    )"
  >
    <!-- Sidebar header -->
    <div class="flex h-14 items-center border-b px-4">
      <router-link
        to="/"
        :class="cn(
          'flex items-center gap-2 font-semibold text-foreground transition-opacity',
          isCollapsed && !isMobile && 'opacity-0 w-0 overflow-hidden'
        )"
        @click="handleNavClick"
      >
        <CreditCard class="h-6 w-6 shrink-0 text-primary" />
        <span v-if="!isCollapsed || isMobile" class="truncate">SubscriptionTracker</span>
      </router-link>

      <!-- Mobile close button -->
      <Button
        v-if="isMobile"
        variant="ghost"
        size="icon-sm"
        class="ml-auto"
        @click="closeMobile"
      >
        <X class="h-5 w-5" />
        <span class="sr-only">Close sidebar</span>
      </Button>

      <!-- Desktop collapse button -->
      <Button
        v-if="!isMobile"
        variant="ghost"
        size="icon-sm"
        :class="cn(
          'transition-all',
          isCollapsed ? 'mx-auto' : 'ml-auto'
        )"
        @click="toggle"
      >
        <ChevronLeft v-if="!isCollapsed" class="h-5 w-5" />
        <ChevronRight v-else class="h-5 w-5" />
        <span class="sr-only">{{ isCollapsed ? 'Expand' : 'Collapse' }} sidebar</span>
      </Button>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 space-y-1 p-2">
      <router-link
        v-for="item in filteredNavItems"
        :key="item.href"
        :to="item.href"
        :class="cn(
          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
          isActive(item.href)
            ? 'bg-primary text-primary-foreground'
            : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
          isCollapsed && !isMobile && 'justify-center px-2'
        )"
        @click="handleNavClick"
      >
        <component :is="item.icon" class="h-5 w-5 shrink-0" />
        <span
          v-if="!isCollapsed || isMobile"
          class="truncate"
        >
          {{ item.title }}
        </span>
      </router-link>

      <!-- Admin section -->
      <template v-if="filteredAdminItems.length > 0">
        <div
          :class="cn(
            'mt-4 border-t pt-4',
            isCollapsed && !isMobile && 'border-t-0 pt-2'
          )"
        >
          <p
            v-if="!isCollapsed || isMobile"
            class="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground"
          >
            Admin
          </p>
          <router-link
            v-for="item in filteredAdminItems"
            :key="item.href"
            :to="item.href"
            :class="cn(
              'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
              isActive(item.href)
                ? 'bg-amber-100 text-amber-900'
                : 'text-amber-700 hover:bg-amber-50 hover:text-amber-900',
              isCollapsed && !isMobile && 'justify-center px-2'
            )"
            @click="handleNavClick"
          >
            <component :is="item.icon" class="h-5 w-5 shrink-0" />
            <span
              v-if="!isCollapsed || isMobile"
              class="truncate"
            >
              {{ item.title }}
            </span>
          </router-link>
        </div>
      </template>
    </nav>

    <!-- User section at bottom -->
    <div class="border-t p-2">
      <div
        :class="cn(
          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm',
          isCollapsed && !isMobile && 'justify-center px-2'
        )"
      >
        <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
          <span class="text-sm font-medium">
            {{ authStore.user?.email?.charAt(0).toUpperCase() || 'U' }}
          </span>
        </div>
        <div v-if="!isCollapsed || isMobile" class="flex-1 truncate">
          <p class="truncate text-sm font-medium text-foreground">
            {{ authStore.user?.email || 'User' }}
          </p>
          <p class="truncate text-xs text-muted-foreground">
            {{ authStore.user?.role || 'Member' }}
          </p>
        </div>
      </div>
    </div>
  </aside>
</template>
