import { useQuery } from '@tanstack/react-query'
import { getCart } from '@/api/cart'

export const cartQueryKey = ['cart']

export function useCartQuery() {
  return useQuery({
    queryKey: cartQueryKey,
    queryFn: getCart,
  })
}
