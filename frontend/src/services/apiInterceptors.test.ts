import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { InternalAxiosRequestConfig, AxiosResponse, AxiosError, AxiosHeaders } from 'axios'
import {
  createRequestInterceptor,
  createRequestErrorInterceptor,
  createResponseInterceptor,
  createResponseErrorInterceptor,
} from './apiInterceptors'

const createMockConfig = (overrides: Partial<InternalAxiosRequestConfig> = {}): InternalAxiosRequestConfig => ({
  headers: {
    set: vi.fn(),
    get: vi.fn(),
    has: vi.fn(),
    delete: vi.fn(),
    clear: vi.fn(),
    normalize: vi.fn(),
    concat: vi.fn(),
    toJSON: vi.fn(),
    getContentType: vi.fn(),
    setContentType: vi.fn(),
    getContentLength: vi.fn(),
    setContentLength: vi.fn(),
    getAccept: vi.fn(),
    setAccept: vi.fn(),
    getUserAgent: vi.fn(),
    setUserAgent: vi.fn(),
    getAuthorization: vi.fn(),
    setAuthorization: vi.fn(),
  } as unknown as AxiosHeaders,
  ...overrides,
})

const createMockResponse = (overrides: Partial<AxiosResponse> = {}): AxiosResponse => ({
  data: {},
  status: 200,
  statusText: 'OK',
  headers: {},
  config: createMockConfig(),
  ...overrides,
})

const createMockError = (status?: number): AxiosError => ({
  isAxiosError: true,
  name: 'AxiosError',
  message: 'Request failed',
  toJSON: () => ({}),
  response: status ? {
    status,
    statusText: status === 401 ? 'Unauthorized' : 'Error',
    data: {},
    headers: {},
    config: createMockConfig(),
  } : undefined,
  config: createMockConfig(),
})

describe('API Interceptors', () => {
  describe('createRequestInterceptor', () => {
    it('should add Authorization header when token exists', () => {
      const getToken = vi.fn().mockReturnValue('my-jwt-token')
      const interceptor = createRequestInterceptor(getToken)

      const config = createMockConfig()
      const result = interceptor(config)

      expect(result.headers.Authorization).toBe('Bearer my-jwt-token')
    })

    it('should not add Authorization header when token is null', () => {
      const getToken = vi.fn().mockReturnValue(null)
      const interceptor = createRequestInterceptor(getToken)

      const config = createMockConfig()
      const result = interceptor(config)

      expect(result.headers.Authorization).toBeUndefined()
    })

    it('should not add Authorization header when token is empty string', () => {
      const getToken = vi.fn().mockReturnValue('')
      const interceptor = createRequestInterceptor(getToken)

      const config = createMockConfig()
      const result = interceptor(config)

      expect(result.headers.Authorization).toBeUndefined()
    })

    it('should call getToken function', () => {
      const getToken = vi.fn().mockReturnValue('token')
      const interceptor = createRequestInterceptor(getToken)

      interceptor(createMockConfig())

      expect(getToken).toHaveBeenCalled()
    })

    it('should return the config object', () => {
      const getToken = vi.fn().mockReturnValue('token')
      const interceptor = createRequestInterceptor(getToken)

      const config = createMockConfig()
      const result = interceptor(config)

      expect(result).toBe(config)
    })

    it('should handle config without headers gracefully', () => {
      const getToken = vi.fn().mockReturnValue('token')
      const interceptor = createRequestInterceptor(getToken)

      const config = { headers: undefined } as unknown as InternalAxiosRequestConfig
      const result = interceptor(config)

      expect(result).toBe(config)
    })
  })

  describe('createRequestErrorInterceptor', () => {
    it('should reject with the error', async () => {
      const interceptor = createRequestErrorInterceptor()
      const error = new Error('Request setup failed')

      await expect(interceptor(error)).rejects.toBe(error)
    })

    it('should preserve error details', async () => {
      const interceptor = createRequestErrorInterceptor()
      const error = { message: 'Network error', code: 'ERR_NETWORK' }

      await expect(interceptor(error)).rejects.toEqual(error)
    })
  })

  describe('createResponseInterceptor', () => {
    it('should return the response unchanged', () => {
      const interceptor = createResponseInterceptor()

      const response = createMockResponse({ data: { id: 1, name: 'Test' } })
      const result = interceptor(response)

      expect(result).toBe(response)
      expect(result.data).toEqual({ id: 1, name: 'Test' })
    })

    it('should pass through all response properties', () => {
      const interceptor = createResponseInterceptor()

      const response = createMockResponse({
        status: 201,
        statusText: 'Created',
        data: { created: true },
      })
      const result = interceptor(response)

      expect(result.status).toBe(201)
      expect(result.statusText).toBe('Created')
      expect(result.data).toEqual({ created: true })
    })
  })

  describe('createResponseErrorInterceptor', () => {
    let clearAuth: ReturnType<typeof vi.fn>
    let redirectToLogin: ReturnType<typeof vi.fn>

    beforeEach(() => {
      clearAuth = vi.fn()
      redirectToLogin = vi.fn()
    })

    it('should call clearAuth and redirectToLogin on 401 error', async () => {
      const interceptor = createResponseErrorInterceptor(clearAuth, redirectToLogin)
      const error = createMockError(401)

      await expect(interceptor(error)).rejects.toBeDefined()

      expect(clearAuth).toHaveBeenCalled()
      expect(redirectToLogin).toHaveBeenCalled()
    })

    it('should not call clearAuth or redirectToLogin on non-401 errors', async () => {
      const interceptor = createResponseErrorInterceptor(clearAuth, redirectToLogin)

      const error400 = createMockError(400)
      await expect(interceptor(error400)).rejects.toBeDefined()

      const error403 = createMockError(403)
      await expect(interceptor(error403)).rejects.toBeDefined()

      const error500 = createMockError(500)
      await expect(interceptor(error500)).rejects.toBeDefined()

      expect(clearAuth).not.toHaveBeenCalled()
      expect(redirectToLogin).not.toHaveBeenCalled()
    })

    it('should not call clearAuth or redirectToLogin when no response', async () => {
      const interceptor = createResponseErrorInterceptor(clearAuth, redirectToLogin)
      const error = createMockError()

      await expect(interceptor(error)).rejects.toBeDefined()

      expect(clearAuth).not.toHaveBeenCalled()
      expect(redirectToLogin).not.toHaveBeenCalled()
    })

    it('should reject with the original error', async () => {
      const interceptor = createResponseErrorInterceptor(clearAuth, redirectToLogin)
      const error = createMockError(401)

      await expect(interceptor(error)).rejects.toBe(error)
    })

    it('should call clearAuth before redirectToLogin', async () => {
      const callOrder: string[] = []
      clearAuth = vi.fn(() => callOrder.push('clearAuth'))
      redirectToLogin = vi.fn(() => callOrder.push('redirectToLogin'))

      const interceptor = createResponseErrorInterceptor(clearAuth, redirectToLogin)
      const error = createMockError(401)

      await expect(interceptor(error)).rejects.toBeDefined()

      expect(callOrder).toEqual(['clearAuth', 'redirectToLogin'])
    })

    it('should handle 404 errors without auth actions', async () => {
      const interceptor = createResponseErrorInterceptor(clearAuth, redirectToLogin)
      const error = createMockError(404)

      await expect(interceptor(error)).rejects.toBe(error)

      expect(clearAuth).not.toHaveBeenCalled()
      expect(redirectToLogin).not.toHaveBeenCalled()
    })

    it('should handle network errors without response', async () => {
      const interceptor = createResponseErrorInterceptor(clearAuth, redirectToLogin)
      const error: AxiosError = {
        isAxiosError: true,
        name: 'AxiosError',
        message: 'Network Error',
        toJSON: () => ({}),
        response: undefined,
        config: createMockConfig(),
      }

      await expect(interceptor(error)).rejects.toBe(error)

      expect(clearAuth).not.toHaveBeenCalled()
      expect(redirectToLogin).not.toHaveBeenCalled()
    })
  })
})
