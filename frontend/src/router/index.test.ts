import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// Mock the components to avoid actual imports
const MockComponent = { template: '<div>Mock</div>' }

interface RouteMeta {
  requiresAuth?: boolean
  requiresAdmin?: boolean
}

function getUserFromStorage(): { role?: string } | null {
  const userStr = localStorage.getItem('user')
  if (!userStr) return null
  try {
    return JSON.parse(userStr)
  } catch {
    return null
  }
}

// Create test router with the same guard logic
function createTestRouter() {
  const routes: RouteRecordRaw[] = [
    { path: '/login', name: 'login', component: MockComponent, meta: { requiresAuth: false } },
    { path: '/register', name: 'register', component: MockComponent, meta: { requiresAuth: false } },
    { path: '/verify-email', name: 'verify-email', component: MockComponent, meta: { requiresAuth: false } },
    { path: '/', name: 'dashboard', component: MockComponent, meta: { requiresAuth: true } },
    { path: '/settings', name: 'settings', component: MockComponent, meta: { requiresAuth: true } },
    { path: '/admin/job-runs', name: 'admin-job-runs', component: MockComponent, meta: { requiresAuth: true, requiresAdmin: true } },
  ]

  const router = createRouter({
    history: createWebHistory(),
    routes,
  })

  router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem('token')
    const isAuthenticated = !!token
    const meta = to.meta as RouteMeta

    if (meta.requiresAuth && !isAuthenticated) {
      next('/login')
    } else if (meta.requiresAdmin) {
      const user = getUserFromStorage()
      if (user?.role !== 'ADMIN') {
        next('/')
      } else {
        next()
      }
    } else if (!meta.requiresAuth && isAuthenticated && (to.name === 'login' || to.name === 'register')) {
      next('/')
    } else {
      next()
    }
  })

  return router
}

describe('Router Guards', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('Authentication guard', () => {
    it('should redirect to login when accessing protected route without auth', async () => {
      const router = createTestRouter()
      await router.push('/')
      expect(router.currentRoute.value.path).toBe('/login')
    })

    it('should allow access to login page without auth', async () => {
      const router = createTestRouter()
      await router.push('/login')
      expect(router.currentRoute.value.path).toBe('/login')
    })

    it('should allow access to verify-email page without auth', async () => {
      const router = createTestRouter()
      await router.push('/verify-email')
      expect(router.currentRoute.value.path).toBe('/verify-email')
    })

    it('should allow access to verify-email page even with auth', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', JSON.stringify({ role: 'USER' }))

      const router = createTestRouter()
      await router.push('/verify-email')
      expect(router.currentRoute.value.path).toBe('/verify-email')
    })

    it('should allow access to protected routes with auth', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', JSON.stringify({ role: 'USER' }))

      const router = createTestRouter()
      await router.push('/')
      expect(router.currentRoute.value.path).toBe('/')
    })

    it('should redirect authenticated users from login to dashboard', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', JSON.stringify({ role: 'USER' }))

      const router = createTestRouter()
      await router.push('/login')
      expect(router.currentRoute.value.path).toBe('/')
    })

    it('should redirect authenticated users from register to dashboard', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', JSON.stringify({ role: 'USER' }))

      const router = createTestRouter()
      await router.push('/register')
      expect(router.currentRoute.value.path).toBe('/')
    })
  })

  describe('Admin guard', () => {
    it('should redirect non-admin users from admin routes to dashboard', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', JSON.stringify({ role: 'USER' }))

      const router = createTestRouter()
      await router.push('/admin/job-runs')
      expect(router.currentRoute.value.path).toBe('/')
    })

    it('should allow admin users to access admin routes', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', JSON.stringify({ role: 'ADMIN' }))

      const router = createTestRouter()
      await router.push('/admin/job-runs')
      expect(router.currentRoute.value.path).toBe('/admin/job-runs')
    })

    it('should redirect to login when accessing admin routes without auth', async () => {
      const router = createTestRouter()
      await router.push('/admin/job-runs')
      expect(router.currentRoute.value.path).toBe('/login')
    })

    it('should redirect when user object is missing role', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', JSON.stringify({ email: 'test@example.com' }))

      const router = createTestRouter()
      await router.push('/admin/job-runs')
      expect(router.currentRoute.value.path).toBe('/')
    })

    it('should redirect when user JSON is invalid', async () => {
      localStorage.setItem('token', 'test-token')
      localStorage.setItem('user', 'invalid-json')

      const router = createTestRouter()
      await router.push('/admin/job-runs')
      expect(router.currentRoute.value.path).toBe('/')
    })
  })
})
