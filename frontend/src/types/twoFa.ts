export interface TotpSetupResponse {
  secret: string
  qrCodeDataUri: string
}

export interface TotpSetupCompleteResponse {
  twoFactorEnabled: boolean
  recoveryCodes: string[]
}

export interface TotpStatusResponse {
  enabled: boolean
  enabledAt: string | null
  remainingRecoveryCodes: number | null
}

export interface TotpVerifyRequest {
  code: string
}

export interface TotpDisableRequest {
  password: string
  code: string
}

export interface RecoveryCodeVerifyRequest {
  recoveryCode: string
}
