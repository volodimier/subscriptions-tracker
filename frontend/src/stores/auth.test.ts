import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from './auth'
import { authService } from '@/services/authService'
import router from '@/router'

vi.mock('@/services/authService', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
  },
}))

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
  },
}))

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should have null user and token', () => {
      const store = useAuthStore()
      expect(store.user).toBeNull()
      expect(store.token).toBeNull()
    })

    it('should not be authenticated', () => {
      const store = useAuthStore()
      expect(store.isAuthenticated).toBe(false)
    })

    it('should have loading set to false', () => {
      const store = useAuthStore()
      expect(store.loading).toBe(false)
    })

    it('should have error set to null', () => {
      const store = useAuthStore()
      expect(store.error).toBeNull()
    })
  })

  describe('isAuthenticated computed', () => {
    it('should return false when token is null', () => {
      const store = useAuthStore()
      store.token = null
      store.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }
      expect(store.isAuthenticated).toBe(false)
    })

    it('should return false when user is null', () => {
      const store = useAuthStore()
      store.token = 'some-token'
      store.user = null
      expect(store.isAuthenticated).toBe(false)
    })

    it('should return true when both token and user are set', () => {
      const store = useAuthStore()
      store.token = 'some-token'
      store.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }
      expect(store.isAuthenticated).toBe(true)
    })
  })

  describe('isAdmin computed', () => {
    it('should return false when user is null', () => {
      const store = useAuthStore()
      store.user = null
      expect(store.isAdmin).toBe(false)
    })

    it('should return false when user role is USER', () => {
      const store = useAuthStore()
      store.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }
      expect(store.isAdmin).toBe(false)
    })

    it('should return true when user role is ADMIN', () => {
      const store = useAuthStore()
      store.user = { id: 1, email: 'admin@example.com', baseCurrencyCode: 'USD', role: 'ADMIN' }
      expect(store.isAdmin).toBe(true)
    })
  })

  describe('login', () => {
    const mockUser = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' as const }
    const mockToken = 'mock-jwt-token'
    const mockRefreshToken = 'mock-refresh-token'
    const mockResponse = { user: mockUser, token: mockToken, refreshToken: mockRefreshToken }

    it('should set user and token on successful login', async () => {
      vi.mocked(authService.login).mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.login({ email: 'test@example.com', password: 'password123' })

      expect(store.user).toEqual(mockUser)
      expect(store.token).toBe(mockToken)
      expect(store.isAuthenticated).toBe(true)
    })

    it('should store token, refreshToken, and user in localStorage', async () => {
      vi.mocked(authService.login).mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.login({ email: 'test@example.com', password: 'password123' })

      expect(localStorage.getItem('token')).toBe(mockToken)
      expect(localStorage.getItem('refreshToken')).toBe(mockRefreshToken)
      expect(localStorage.getItem('user')).toBe(JSON.stringify(mockUser))
    })

    it('should navigate to home on successful login', async () => {
      vi.mocked(authService.login).mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.login({ email: 'test@example.com', password: 'password123' })

      expect(router.push).toHaveBeenCalledWith('/')
    })

    it('should set loading to true during login', async () => {
      let loadingDuringCall = false
      vi.mocked(authService.login).mockImplementation(async () => {
        const store = useAuthStore()
        loadingDuringCall = store.loading
        return mockResponse
      })

      const store = useAuthStore()
      await store.login({ email: 'test@example.com', password: 'password123' })

      expect(loadingDuringCall).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('should set error on failed login', async () => {
      const errorMessage = 'Invalid credentials'
      vi.mocked(authService.login).mockRejectedValue({
        response: { data: { message: errorMessage } },
      })

      const store = useAuthStore()

      await expect(store.login({ email: 'test@example.com', password: 'wrong' })).rejects.toBeDefined()

      expect(store.error).toBe(errorMessage)
      expect(store.user).toBeNull()
      expect(store.token).toBeNull()
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(authService.login).mockRejectedValue({
        response: { data: {} },
      })

      const store = useAuthStore()

      await expect(store.login({ email: 'test@example.com', password: 'wrong' })).rejects.toBeDefined()

      expect(store.error).toBe('Login failed')
    })

    it('should clear previous error on new login attempt', async () => {
      const store = useAuthStore()
      store.error = 'Previous error'

      vi.mocked(authService.login).mockResolvedValue(mockResponse)
      await store.login({ email: 'test@example.com', password: 'password123' })

      expect(store.error).toBeNull()
    })
  })

  describe('register', () => {
    const mockUser = { id: 1, email: 'new@example.com', baseCurrencyCode: 'USD', role: 'USER' as const }
    const mockToken = 'mock-jwt-token'
    const mockRefreshToken = 'mock-refresh-token'
    const mockResponse = { user: mockUser, token: mockToken, refreshToken: mockRefreshToken }

    it('should set user and token on successful registration', async () => {
      vi.mocked(authService.register).mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.register({ email: 'new@example.com', password: 'password123' })

      expect(store.user).toEqual(mockUser)
      expect(store.token).toBe(mockToken)
      expect(store.isAuthenticated).toBe(true)
    })

    it('should store token, refreshToken, and user in localStorage', async () => {
      vi.mocked(authService.register).mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.register({ email: 'new@example.com', password: 'password123' })

      expect(localStorage.getItem('token')).toBe(mockToken)
      expect(localStorage.getItem('refreshToken')).toBe(mockRefreshToken)
      expect(localStorage.getItem('user')).toBe(JSON.stringify(mockUser))
    })

    it('should navigate to home on successful registration', async () => {
      vi.mocked(authService.register).mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.register({ email: 'new@example.com', password: 'password123' })

      expect(router.push).toHaveBeenCalledWith('/')
    })

    it('should set error on failed registration', async () => {
      const errorMessage = 'Email already exists'
      vi.mocked(authService.register).mockRejectedValue({
        response: { data: { message: errorMessage } },
      })

      const store = useAuthStore()

      await expect(store.register({ email: 'existing@example.com', password: 'pass' })).rejects.toBeDefined()

      expect(store.error).toBe(errorMessage)
    })

    it('should use default error message when no message in response', async () => {
      vi.mocked(authService.register).mockRejectedValue({
        response: { data: {} },
      })

      const store = useAuthStore()

      await expect(store.register({ email: 'test@example.com', password: 'pass' })).rejects.toBeDefined()

      expect(store.error).toBe('Registration failed')
    })
  })

  describe('logout', () => {
    it('should clear user and token', async () => {
      const store = useAuthStore()
      store.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }
      store.token = 'some-token'
      localStorage.setItem('refreshToken', 'some-refresh-token')

      vi.mocked(authService.logout).mockResolvedValue(undefined)
      await store.logout()

      expect(store.user).toBeNull()
      expect(store.token).toBeNull()
      expect(store.isAuthenticated).toBe(false)
    })

    it('should send refreshToken to logout API', async () => {
      const store = useAuthStore()
      localStorage.setItem('refreshToken', 'some-refresh-token')

      vi.mocked(authService.logout).mockResolvedValue(undefined)
      await store.logout()

      expect(authService.logout).toHaveBeenCalledWith('some-refresh-token')
    })

    it('should remove token, refreshToken, and user from localStorage', async () => {
      localStorage.setItem('token', 'some-token')
      localStorage.setItem('refreshToken', 'some-refresh-token')
      localStorage.setItem('user', JSON.stringify({ id: 1 }))

      const store = useAuthStore()
      vi.mocked(authService.logout).mockResolvedValue(undefined)
      await store.logout()

      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
      expect(localStorage.getItem('user')).toBeNull()
    })

    it('should navigate to login page', async () => {
      const store = useAuthStore()
      localStorage.setItem('refreshToken', 'some-refresh-token')
      vi.mocked(authService.logout).mockResolvedValue(undefined)
      await store.logout()

      expect(router.push).toHaveBeenCalledWith('/login')
    })

    it('should clear state even if logout API fails', async () => {
      const store = useAuthStore()
      store.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }
      store.token = 'some-token'
      localStorage.setItem('token', 'some-token')
      localStorage.setItem('refreshToken', 'some-refresh-token')

      vi.mocked(authService.logout).mockRejectedValue(new Error('Network error'))
      await store.logout()

      expect(store.user).toBeNull()
      expect(store.token).toBeNull()
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
      expect(router.push).toHaveBeenCalledWith('/login')
    })

    it('should not call logout API if no refreshToken is available', async () => {
      const store = useAuthStore()
      store.user = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' }
      store.token = 'some-token'
      // No refreshToken in localStorage or store

      await store.logout()

      expect(authService.logout).not.toHaveBeenCalled()
      expect(store.user).toBeNull()
      expect(store.token).toBeNull()
      expect(router.push).toHaveBeenCalledWith('/login')
    })
  })

  describe('checkAuth', () => {
    it('should restore auth state from localStorage', async () => {
      const mockUser = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD', role: 'USER' as const }
      const mockToken = 'stored-token'
      const mockRefreshToken = 'stored-refresh-token'

      localStorage.setItem('token', mockToken)
      localStorage.setItem('refreshToken', mockRefreshToken)
      localStorage.setItem('user', JSON.stringify(mockUser))

      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser)

      const store = useAuthStore()
      await store.checkAuth()

      expect(store.token).toBe(mockToken)
      expect(store.user).toEqual(mockUser)
    })

    it('should update user from API response', async () => {
      const storedUser = { id: 1, email: 'test@example.com', baseCurrencyCode: 'USD' }
      const updatedUser = { id: 1, email: 'test@example.com', baseCurrencyCode: 'EUR' }

      localStorage.setItem('token', 'some-token')
      localStorage.setItem('user', JSON.stringify(storedUser))

      vi.mocked(authService.getCurrentUser).mockResolvedValue(updatedUser)

      const store = useAuthStore()
      await store.checkAuth()

      expect(store.user).toEqual(updatedUser)
      expect(localStorage.getItem('user')).toBe(JSON.stringify(updatedUser))
    })

    it('should do nothing if no stored token', async () => {
      const store = useAuthStore()
      await store.checkAuth()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
      expect(authService.getCurrentUser).not.toHaveBeenCalled()
    })

    it('should logout if getCurrentUser fails', async () => {
      localStorage.setItem('token', 'invalid-token')
      localStorage.setItem('refreshToken', 'invalid-refresh-token')
      localStorage.setItem('user', JSON.stringify({ id: 1 }))

      vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('Unauthorized'))
      vi.mocked(authService.logout).mockResolvedValue(undefined)

      const store = useAuthStore()
      await store.checkAuth()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
    })
  })

  describe('updateUser', () => {
    it('should update user in store', () => {
      const store = useAuthStore()
      const updatedUser = { id: 1, email: 'updated@example.com', baseCurrencyCode: 'EUR', role: 'USER' as const }

      store.updateUser(updatedUser)

      expect(store.user).toEqual(updatedUser)
    })

    it('should update user in localStorage', () => {
      const store = useAuthStore()
      const updatedUser = { id: 1, email: 'updated@example.com', baseCurrencyCode: 'EUR', role: 'USER' as const }

      store.updateUser(updatedUser)

      expect(localStorage.getItem('user')).toBe(JSON.stringify(updatedUser))
    })
  })
})
