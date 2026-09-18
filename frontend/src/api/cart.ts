import { apiFetch } from './client'
import type { CartResponse, OrderResponse } from './types'

export function getCart() {
  return apiFetch<CartResponse>('/cart')
}

export function addCartItem(productId: number, quantity: number) {
  return apiFetch<CartResponse>('/cart/items', { method: 'POST', body: { productId, quantity } })
}

export function updateCartItem(productId: number, quantity: number) {
  return apiFetch<CartResponse>(`/cart/items/${productId}`, { method: 'PATCH', body: { quantity } })
}

export function removeCartItem(productId: number) {
  return apiFetch<void>(`/cart/items/${productId}`, { method: 'DELETE' })
}

export function checkout() {
  return apiFetch<OrderResponse>('/cart/checkout', { method: 'POST' })
}
