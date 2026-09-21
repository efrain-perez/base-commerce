import { apiFetch } from './client'
import type {
  ImportJobDetailResponse,
  Page,
  ProductCreateRequest,
  ProductResponse,
  ProductUpdateRequest,
} from './types'

export function listProducts(params: { name?: string; page: number; size: number; sort?: string }) {
  return apiFetch<Page<ProductResponse>>('/products', { query: params })
}

export function getProduct(id: number) {
  return apiFetch<ProductResponse>(`/products/${id}`)
}

export function createProduct(body: ProductCreateRequest) {
  return apiFetch<ProductResponse>('/products', { method: 'POST', body })
}

export function updateProduct(id: number, body: ProductUpdateRequest) {
  return apiFetch<ProductResponse>(`/products/${id}`, { method: 'PUT', body })
}

export function deleteProduct(id: number) {
  return apiFetch<void>(`/products/${id}`, { method: 'DELETE' })
}

export function importProducts(file: File) {
  const form = new FormData()
  form.append('file', file)
  return apiFetch<ImportJobDetailResponse>('/products/import', { method: 'POST', body: form })
}
