import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, RouteLocationNormalized } from 'vue-router'

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

function isAdminRoute(to: RouteLocationNormalized): boolean {
  return (to.meta as RouteMeta).requiresAdmin === true
}

function isAuthRequired(to: RouteLocationNormalized): boolean {
  return (to.meta as RouteMeta).requiresAuth === true
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/verify-email',
    name: 'verify-email',
    component: () => import('@/views/auth/VerifyEmailView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/subscriptions',
    name: 'subscriptions',
    component: () => import('@/views/SubscriptionsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/subscriptions/add',
    name: 'add-subscription',
    component: () => import('@/views/AddSubscriptionView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/subscriptions/:id',
    name: 'subscription-detail',
    component: () => import('@/views/SubscriptionDetailView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/subscriptions/:id/edit',
    name: 'edit-subscription',
    component: () => import('@/views/EditSubscriptionView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/services',
    name: 'services',
    component: () => import('@/views/ServicesView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/statistics',
    name: 'statistics',
    component: () => import('@/views/StatisticsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { requiresAuth: true },
  },
  // Admin routes
  {
    path: '/admin/job-runs',
    name: 'admin-job-runs',
    component: () => import('@/views/admin/JobRunsView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/categories',
    name: 'admin-categories',
    component: () => import('@/views/admin/CategoriesView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isAuthenticated = !!token

  if (isAuthRequired(to) && !isAuthenticated) {
    next('/login')
  } else if (isAdminRoute(to)) {
    const user = getUserFromStorage()
    if (user?.role !== 'ADMIN') {
      // Redirect non-admin users to dashboard
      next('/')
    } else {
      next()
    }
  } else if (!isAuthRequired(to) && isAuthenticated && (to.name === 'login' || to.name === 'register')) {
    next('/')
  } else {
    next()
  }
})

export default router
