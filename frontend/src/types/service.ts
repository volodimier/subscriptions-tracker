export interface Service {
  id: number
  name: string
  category?: string
  categoryId?: number
  faviconUrl?: string
  websiteUrl?: string
  subscriptionCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface CreateServiceRequest {
  name: string
  categoryId?: number
  websiteUrl?: string
}

export interface UpdateServiceRequest {
  name?: string
  categoryId?: number
  websiteUrl?: string
}
