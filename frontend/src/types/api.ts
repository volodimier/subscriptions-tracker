export interface ApiError {
  error: string
  message: string
  details?: Record<string, string>
}

export interface PaginatedResponse<T> {
  data: T[]
  pagination: {
    page: number
    limit: number
    total: number
    totalPages: number
  }
}

export interface DashboardSummary {
  period: {
    startDate: string
    endDate: string
  }
  summary: {
    totalSpent: number
    currency: string
    activeSubscriptions: number
    cancelledSubscriptions: number
    monthlyAverage: number
  }
  byCategory: CategoryBreakdown[]
  topServices: TopService[]
}

export interface CategoryBreakdown {
  category: string
  total: number
  percentage: number
  count: number
}

export interface TopService {
  serviceId: number
  serviceName: string
  category?: string
  totalSpent: number
  paymentCount: number
}

export interface Projection {
  year: number
  baseCurrency: string
  projection: {
    estimatedTotal: number
    activeSubscriptions: number
    assumptions: string
  }
  monthlyBreakdown: {
    month: string
    estimated: number
  }[]
}
