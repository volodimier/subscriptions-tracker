export interface Service {
  id: number
  name: string
  category?: string
  faviconUrl?: string
  websiteUrl?: string
  subscriptionCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface CreateServiceRequest {
  name: string
  category?: string
  websiteUrl?: string
}

export interface UpdateServiceRequest {
  name?: string
  category?: string
  websiteUrl?: string
}
