import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { getProduct, listProducts } from '@/api/products'

export function useProductsQuery(params: { name?: string; page: number; size: number; sort?: string }) {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => listProducts(params),
  })
}

export function useInfiniteProductsQuery(params: { name?: string; size: number }) {
  return useInfiniteQuery({
    queryKey: ['products', 'infinite', params],
    queryFn: ({ pageParam }) => listProducts({ ...params, page: pageParam }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
  })
}

export function useProductQuery(id: number | undefined) {
  return useQuery({
    queryKey: ['products', id],
    queryFn: () => getProduct(id as number),
    enabled: id !== undefined,
  })
}
