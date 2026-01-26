import axios, { type AxiosInstance } from 'axios'
import router from '@/router'
import {
  createRequestInterceptor,
  createRequestErrorInterceptor,
  createResponseInterceptor,
  createResponseErrorInterceptor,
} from './apiInterceptors'

const api: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor - add JWT token
api.interceptors.request.use(
  createRequestInterceptor(() => localStorage.getItem('token')),
  createRequestErrorInterceptor()
)

// Response interceptor - handle 401 errors
api.interceptors.response.use(
  createResponseInterceptor(),
  createResponseErrorInterceptor(
    () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    () => router.push('/login')
  )
)

export default api
