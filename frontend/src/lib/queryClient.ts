import { MutationCache, QueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { ApiError } from '@/api/client'

export const queryClient = new QueryClient({
  mutationCache: new MutationCache({
    onError: (error, _variables, _context, mutation) => {
      if (mutation.options.onError) return
      const message = error instanceof ApiError ? error.message : 'Something went wrong.'
      toast.error(message)
    },
  }),
})
