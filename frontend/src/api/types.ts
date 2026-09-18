export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface ProductResponse {
  id: number
  sku: string
  name: string
  description: string | null
  category: string | null
  price: number
  stock: number
  weightKg: number | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface ProductCreateRequest {
  sku: string
  name: string
  description?: string | null
  category?: string | null
  price: number
  stock: number
  weightKg?: number | null
}

export interface ProductUpdateRequest {
  name: string
  description?: string | null
  category?: string | null
  price: number
  stock: number
  weightKg?: number | null
}

export interface CartItemResponse {
  productId: number
  sku: string
  name: string
  unitPrice: number
  quantity: number
  lineTotal: number
}

export interface CartResponse {
  id: string
  status: string
  items: CartItemResponse[]
  subtotal: number
}

export interface OrderItemResponse {
  productId: number
  name: string
  priceAtPurchase: number
  versionAtPurchase: number
  quantity: number
  lineTotal: number
}

export interface OrderResponse {
  id: number
  status: string
  createdAt: string
  items: OrderItemResponse[]
  orderTotal: number
}

export interface ImportJobResponse {
  id: number
  fileName: string
  executedAt: string
  totalRows: number
  successCount: number
  failureCount: number
  status: string
}

export interface ImportJobErrorResponse {
  id: number
  rowNumber: number
  rawData: string
  errorReason: string
}

export interface ImportJobDetailResponse {
  job: ImportJobResponse
  errors: ImportJobErrorResponse[]
}

export interface ProblemDetailBase {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
}

export interface ValidationProblemDetail extends ProblemDetailBase {
  fieldErrors: Record<string, string>
}

export type ProblemDetail = ProblemDetailBase | ValidationProblemDetail

export function hasFieldErrors(problem: ProblemDetail): problem is ValidationProblemDetail {
  return 'fieldErrors' in problem && problem.fieldErrors != null
}
