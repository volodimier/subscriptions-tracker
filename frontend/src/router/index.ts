import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const isAuthenticated = !!token

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (!to.meta.requiresAuth && isAuthenticated && (to.name === 'login' || to.name === 'register')) {
    next('/')
  } else {
    next()
  }
})

export default router
