import { useQuery } from '@tanstack/react-query'
import { getOrder } from '@/api/orders'

export function useOrderQuery(id: number | undefined) {
  return useQuery({
    queryKey: ['orders', id],
    queryFn: () => getOrder(id as number),
    enabled: id !== undefined,
    retry: false,
  })
}
