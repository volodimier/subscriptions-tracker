import type { InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'

export function createRequestInterceptor(getToken: () => string | null) {
  return (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  }
}

export function createRequestErrorInterceptor() {
  return (error: unknown): Promise<never> => {
    return Promise.reject(error)
  }
}

export function createResponseInterceptor() {
  return (response: AxiosResponse): AxiosResponse => {
    return response
  }
}

export function createResponseErrorInterceptor(
  clearAuth: () => void,
  redirectToLogin: () => void
) {
  return (error: AxiosError): Promise<never> => {
    if (error.response?.status === 401) {
      clearAuth()
      redirectToLogin()
    }
    return Promise.reject(error)
  }
}
