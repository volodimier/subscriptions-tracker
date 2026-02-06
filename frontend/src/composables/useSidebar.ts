import { ref, computed, provide, inject } from 'vue'
import type { InjectionKey, Ref, ComputedRef } from 'vue'
import { useMediaQuery } from '@vueuse/core'

interface SidebarContext {
  isCollapsed: Ref<boolean>
  isMobile: ComputedRef<boolean>
  isMobileOpen: Ref<boolean>
  toggle: () => void
  setCollapsed: (collapsed: boolean) => void
  openMobile: () => void
  closeMobile: () => void
}

const SIDEBAR_KEY = Symbol('sidebar') as InjectionKey<SidebarContext>

const STORAGE_KEY = 'sidebar-collapsed'

export function provideSidebar() {
  // Check localStorage for persisted state
  const storedCollapsed = localStorage.getItem(STORAGE_KEY)
  const isCollapsed = ref(storedCollapsed === 'true')

  const isMobile = useMediaQuery('(max-width: 768px)')
  const isMobileOpen = ref(false)

  function toggle() {
    if (isMobile.value) {
      isMobileOpen.value = !isMobileOpen.value
    } else {
      isCollapsed.value = !isCollapsed.value
      localStorage.setItem(STORAGE_KEY, String(isCollapsed.value))
    }
  }

  function setCollapsed(collapsed: boolean) {
    isCollapsed.value = collapsed
    localStorage.setItem(STORAGE_KEY, String(collapsed))
  }

  function openMobile() {
    isMobileOpen.value = true
  }

  function closeMobile() {
    isMobileOpen.value = false
  }

  const context: SidebarContext = {
    isCollapsed,
    isMobile: computed(() => isMobile.value),
    isMobileOpen,
    toggle,
    setCollapsed,
    openMobile,
    closeMobile,
  }

  provide(SIDEBAR_KEY, context)

  return context
}

export function useSidebar() {
  const context = inject(SIDEBAR_KEY)
  if (!context) {
    throw new Error('useSidebar must be used within a component that calls provideSidebar')
  }
  return context
}
