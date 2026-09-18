import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { ApiError } from '@/api/client'
import type { ProductResponse } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { TextArea, TextInput } from '@/components/ui/TextInput'
import { NumberInput } from '@/components/ui/NumberInput'
import { useCreateProduct, useUpdateProduct } from '@/hooks/useProductMutations'
import { productFormSchema, type ProductFormValues } from './productSchema'

interface ProductFormProps {
  mode: 'create' | 'edit'
  product?: ProductResponse
  onDone: () => void
}

export function ProductForm({ mode, product, onDone }: ProductFormProps) {
  const isEdit = mode === 'edit'
  const queryClient = useQueryClient()
  const createMutation = useCreateProduct()
  const updateMutation = useUpdateProduct()
  const mutation = isEdit ? updateMutation : createMutation

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productFormSchema),
    defaultValues: isEdit && product
      ? {
          name: product.name,
          description: product.description ?? '',
          category: product.category ?? '',
          price: product.price,
          stock: product.stock,
          weightKg: product.weightKg ?? undefined,
        }
      : { name: '', description: '', category: '', price: 0, stock: 0 },
  })

  useEffect(() => {
    if (isEdit && product) {
      reset({
        name: product.name,
        description: product.description ?? '',
        category: product.category ?? '',
        price: product.price,
        stock: product.stock,
        weightKg: product.weightKg ?? undefined,
      })
    }
  }, [isEdit, product, reset])

  const onSubmit = handleSubmit((values) => {
    const body = {
      name: values.name,
      description: values.description || null,
      category: values.category || null,
      price: values.price,
      stock: values.stock,
      weightKg: values.weightKg ?? null,
    }

    let action: Promise<unknown>
    if (isEdit && product) {
      action = updateMutation.mutateAsync({ id: product.id, body })
    } else {
      if (!values.sku) {
        setError('sku', { message: 'SKU is required' })
        return
      }
      action = createMutation.mutateAsync({ sku: values.sku, ...body })
    }

    action.then(onDone).catch((error: unknown) => {
      if (error instanceof ApiError && error.fieldErrors) {
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          setError(field as keyof ProductFormValues, { message })
        }
      }
    })
  })

  const conflictMessage =
    mutation.error instanceof ApiError && !mutation.error.fieldErrors ? mutation.error.message : undefined

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      {conflictMessage && (
        <div className="flex items-center justify-between gap-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          <span>{conflictMessage}</span>
          {isEdit && (
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                queryClient.invalidateQueries({ queryKey: ['products'] })
                onDone()
              }}
            >
              Reload latest
            </Button>
          )}
        </div>
      )}

      {!isEdit && (
        <TextInput label="SKU" {...register('sku')} error={errors.sku?.message as string} />
      )}
      {isEdit && product && (
        <TextInput label="SKU" value={product.sku} disabled className="bg-gray-100 text-gray-500" />
      )}

      <TextInput label="Name" {...register('name')} error={errors.name?.message as string} />
      <TextArea label="Description" rows={3} {...register('description')} />
      <TextInput label="Category" {...register('category')} />

      <div className="grid grid-cols-2 gap-4">
        <NumberInput
          label="Price"
          step="0.01"
          {...register('price', { valueAsNumber: true })}
          error={errors.price?.message as string}
        />
        <NumberInput
          label="Stock"
          {...register('stock', { valueAsNumber: true })}
          error={errors.stock?.message as string}
        />
      </div>

      <NumberInput
        label="Weight (kg)"
        step="0.001"
        {...register('weightKg', { setValueAs: (value) => (value === '' ? undefined : Number(value)) })}
        error={errors.weightKg?.message as string}
      />

      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={mutation.isPending}>
          {isEdit ? 'Save changes' : 'Create product'}
        </Button>
      </div>
    </form>
  )
}
