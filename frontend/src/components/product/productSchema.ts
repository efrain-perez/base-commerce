import { z } from 'zod'

const optionalWeight = z.number().min(0, 'Weight must be 0 or greater').optional()

const optionalText = z.string().max(2000).optional().or(z.literal(''))

export const productFormSchema = z.object({
  sku: z.string().max(64).optional(),
  name: z.string().min(1, 'Name is required').max(255),
  description: optionalText,
  category: optionalText,
  price: z.number().min(0, 'Price must be 0 or greater'),
  stock: z.number().int('Stock must be a whole number').min(0, 'Stock must be 0 or greater'),
  weightKg: optionalWeight,
})

export type ProductFormValues = z.infer<typeof productFormSchema>
