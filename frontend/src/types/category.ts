export interface Category {
  id: number
  name: string
  system: boolean
}

export interface CreateCategoryRequest {
  name: string
}

export interface UpdateCategoryRequest {
  name: string
}
