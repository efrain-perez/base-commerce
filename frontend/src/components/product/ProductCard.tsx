import type { ProductResponse } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { useAddCartItem } from '@/hooks/useCartMutations'

export function ProductCard({ product }: { product: ProductResponse }) {
  const addCartItem = useAddCartItem()
  const outOfStock = product.stock <= 0

  return (
    <div className="flex flex-col justify-between rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div>
        <p className="text-xs uppercase tracking-wide text-gray-400">{product.sku}</p>
        <h3 className="mt-1 text-base font-semibold text-gray-900">{product.name}</h3>
        {product.category && <p className="mt-1 text-sm text-gray-500">{product.category}</p>}
        <p className="mt-2 text-lg font-bold text-gray-900">${product.price.toFixed(2)}</p>
        <p className={`mt-1 text-sm ${outOfStock ? 'text-red-600' : 'text-gray-500'}`}>
          {outOfStock ? 'Out of stock' : `${product.stock} in stock`}
        </p>
      </div>
      <Button
        className="mt-4 w-full"
        disabled={outOfStock}
        loading={addCartItem.isPending}
        onClick={() => addCartItem.mutate({ productId: product.id, quantity: 1 })}
      >
        Add to Cart
      </Button>
    </div>
  )
}
