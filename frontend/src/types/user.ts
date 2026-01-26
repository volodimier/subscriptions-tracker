export interface User {
  id: number
  email: string
  baseCurrencyCode?: string
  createdAt?: string
  updatedAt?: string
}

export interface AuthResponse {
  user: User
  token: string
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
