import type { Service } from './service'

export type SubscriptionStatus = 'active' | 'cancelled'
export type BillingCycle = 'monthly' | 'yearly' | 'bi_annual' | 'custom'

export interface Subscription {
  id: number
  service: Service
  amount: number
  currencyCode: string
  billingCycle: BillingCycle
  billingCycleDays?: number
  paymentMethod?: string
  startDate: string
  nextBillingDate: string
  status: SubscriptionStatus
  cancelledAt?: string
  notes?: string
  createdAt?: string
  updatedAt?: string
}

export interface SubscriptionDetail extends Subscription {
  stats?: {
    totalPaid: number
    totalPayments: number
    averagePerMonth: number
    activeSince: string
  }
}

export interface CreateSubscriptionRequest {
  serviceId: number
  amount: number
  currencyCode: string
  billingCycle: BillingCycle
  billingCycleDays?: number
  paymentMethod?: string
  startDate: string
  nextBillingDate: string
  notes?: string
}

export interface UpdateSubscriptionRequest {
  serviceId?: number
  amount?: number
  currencyCode?: string
  billingCycle?: BillingCycle
  billingCycleDays?: number
  paymentMethod?: string
  nextBillingDate?: string
  notes?: string
}

export interface CancelSubscriptionRequest {
  cancelledAt: string
}

export interface ReactivateSubscriptionRequest {
  nextBillingDate: string
}

export interface SubscriptionFilters {
  status?: SubscriptionStatus | 'all'
  category?: string
  search?: string
  sort?: 'nextBillingDate' | 'amount' | 'name'
  order?: 'asc' | 'desc'
}
