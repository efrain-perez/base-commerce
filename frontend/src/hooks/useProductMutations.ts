import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { createProduct, deleteProduct, importProducts, updateProduct } from '@/api/products'
import type { ProductCreateRequest, ProductUpdateRequest } from '@/api/types'

export function useCreateProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ProductCreateRequest) => createProduct(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
    },
  })
}

export function useUpdateProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: ProductUpdateRequest }) => updateProduct(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
    },
  })
}

export function useDeleteProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteProduct(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      toast.success('Product deleted.')
    },
  })
}

export function useImportProducts() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => importProducts(file),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      queryClient.invalidateQueries({ queryKey: ['import-jobs'] })
      const { successCount, failureCount } = data.job
      if (failureCount === 0) {
        toast.success(`Successfully imported ${successCount} product${successCount === 1 ? '' : 's'}.`)
      } else {
        toast(`Imported: ${successCount} succeeded, ${failureCount} failed. See Import History for details.`, {
          icon: '⚠️',
        })
      }
    },
  })
}
