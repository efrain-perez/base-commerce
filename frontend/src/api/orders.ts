import { apiFetch } from './client'
import type { OrderResponse } from './types'

export function getOrder(id: number) {
  return apiFetch<OrderResponse>(`/orders/${id}`)
}
