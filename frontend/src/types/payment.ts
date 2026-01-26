export interface PaymentRecord {
  id: number
  subscriptionId: number
  amount: number
  currencyCode: string
  paymentDate: string
  fxRateToBase: number
  amountInBaseCurrency: number
  createdAt?: string
  updatedAt?: string
}

export interface CreatePaymentRequest {
  subscriptionId: number
  amount: number
  currencyCode: string
  paymentDate: string
  fxRateToBase?: number
}

export interface UpdatePaymentRequest {
  amount?: number
  currencyCode?: string
  paymentDate?: string
  fxRateToBase?: number
}
