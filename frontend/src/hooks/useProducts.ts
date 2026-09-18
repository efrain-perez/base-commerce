import { useQuery } from '@tanstack/react-query'
import { getProduct, listProducts } from '@/api/products'

export function useProductsQuery(params: { name?: string; page: number; size: number }) {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => listProducts(params),
  })
}

export function useProductQuery(id: number | undefined) {
  return useQuery({
    queryKey: ['products', id],
    queryFn: () => getProduct(id as number),
    enabled: id !== undefined,
  })
}
