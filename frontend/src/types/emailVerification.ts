export interface EmailVerificationStatus {
  verified: boolean
  verifiedAt?: string
  withinGracePeriod: boolean
  gracePeriodEndsAt?: string
  canResend: boolean
  resendAvailableInSeconds?: number
}
