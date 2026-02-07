import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService } from '@/services/authService'
import { twoFaService } from '@/services/twoFaService'
import type { User, LoginRequest, RegisterRequest, AuthResponse } from '@/types'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const twoFactorRequired = ref<boolean>(false)
  const partialToken = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(data: LoginRequest): Promise<AuthResponse> {
    loading.value = true
    error.value = null
    try {
      const response = await authService.login(data)

      // Check if 2FA is required
      if (response.twoFactorRequired && response.partialToken) {
        setPartialAuth(response.partialToken)
        return response
      }

      // Complete login normally
      completeLogin(response)
      return response
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Login failed'
      throw err
    } finally {
      loading.value = false
    }
  }

  function setPartialAuth(token: string) {
    partialToken.value = token
    twoFactorRequired.value = true
  }

  function clearPartialAuth() {
    partialToken.value = null
    twoFactorRequired.value = false
  }

  function completeLogin(response: AuthResponse) {
    token.value = response.token
    refreshToken.value = response.refreshToken
    user.value = response.user
    localStorage.setItem('token', response.token)
    localStorage.setItem('refreshToken', response.refreshToken)
    localStorage.setItem('user', JSON.stringify(response.user))
    clearPartialAuth()
    router.push('/')
  }

  async function verifyTotp(code: string): Promise<void> {
    if (!partialToken.value) {
      throw new Error('No partial token available')
    }

    loading.value = true
    error.value = null
    try {
      const response = await twoFaService.verifyCode(code, partialToken.value)
      completeLogin(response)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Invalid code'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function verifyRecoveryCode(code: string): Promise<void> {
    if (!partialToken.value) {
      throw new Error('No partial token available')
    }

    loading.value = true
    error.value = null
    try {
      const response = await twoFaService.verifyRecoveryCode(code, partialToken.value)
      completeLogin(response)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Invalid recovery code'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function register(data: RegisterRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authService.register(data)
      token.value = response.token
      refreshToken.value = response.refreshToken
      user.value = response.user
      localStorage.setItem('token', response.token)
      localStorage.setItem('refreshToken', response.refreshToken)
      localStorage.setItem('user', JSON.stringify(response.user))
      router.push('/')
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } }
      error.value = axiosError.response?.data?.message || 'Registration failed'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    const storedRefreshToken = refreshToken.value || localStorage.getItem('refreshToken')
    try {
      if (storedRefreshToken) {
        await authService.logout(storedRefreshToken)
      }
    } catch {
      // Ignore logout errors - still clear local state
    } finally {
      token.value = null
      refreshToken.value = null
      user.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      router.push('/login')
    }
  }

  async function checkAuth() {
    const storedToken = localStorage.getItem('token')
    const storedRefreshToken = localStorage.getItem('refreshToken')
    const storedUser = localStorage.getItem('user')

    if (storedToken && storedUser) {
      token.value = storedToken
      refreshToken.value = storedRefreshToken
      user.value = JSON.parse(storedUser)

      try {
        const currentUser = await authService.getCurrentUser()
        user.value = currentUser
        localStorage.setItem('user', JSON.stringify(currentUser))
      } catch {
        logout()
      }
    }
  }

  function updateUser(updatedUser: User) {
    user.value = updatedUser
    localStorage.setItem('user', JSON.stringify(updatedUser))
  }

  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    isAdmin,
    twoFactorRequired,
    partialToken,
    login,
    register,
    logout,
    checkAuth,
    updateUser,
    setPartialAuth,
    clearPartialAuth,
    verifyTotp,
    verifyRecoveryCode,
  }
})
