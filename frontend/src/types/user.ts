export type UserRole = 'USER' | 'ADMIN'

export interface User {
  id: number
  email: string
  baseCurrencyCode?: string
  role: UserRole
  createdAt?: string
  updatedAt?: string
}

export interface AuthResponse {
  user: User
  token: string
  refreshToken: string
}

export interface LoginRequest {
  email: string
  password: string
  rememberMe?: boolean
}

export interface RegisterRequest {
  email: string
  password: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface DeleteAccountRequest {
  password: string
  confirmation: string
}

export interface UserSettings {
  email: string
  baseCurrency: string
  fxRatesLastUpdated?: string
  currentFxRates?: Record<string, number>
}

// Admin types
export type JobTriggerType = 'SCHEDULED' | 'MANUAL'
export type JobStatus = 'SUCCESS' | 'FAILURE'

export interface JobRun {
  id: number
  jobName: string
  triggerType: JobTriggerType
  status: JobStatus
  startTime: string
  finishTime?: string
  errorMessage?: string
  createdAt: string
}
