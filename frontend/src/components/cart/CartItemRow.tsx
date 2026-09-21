import { useState, type ChangeEvent } from 'react'
import type { CartItemResponse } from '@/api/types'
import { Table } from '@/components/ui/Table'
import { Button } from '@/components/ui/Button'
import { NumberInput } from '@/components/ui/NumberInput'
import { useRemoveCartItem, useUpdateCartItem } from '@/hooks/useCartMutations'

export function CartItemRow({ item }: { item: CartItemResponse }) {
  const [quantity, setQuantity] = useState(item.quantity)
  const updateCartItem = useUpdateCartItem()
  const removeCartItem = useRemoveCartItem()

  const handleQuantityChange = (event: ChangeEvent<HTMLInputElement>) => {
    const next = Number(event.target.value)
    setQuantity(next)
    if (next >= 1 && next !== item.quantity) {
      updateCartItem.mutate({ productId: item.productId, quantity: next })
    }
  }

  const handleBlur = () => {
    if (quantity < 1) setQuantity(item.quantity)
  }

  return (
    <Table.Row>
      <Table.Cell>{item.sku}</Table.Cell>
      <Table.Cell>{item.name}</Table.Cell>
      <Table.Cell>${item.unitPrice.toFixed(2)}</Table.Cell>
      <Table.Cell>
        <NumberInput
          className="!w-16"
          min={1}
          value={quantity}
          onChange={handleQuantityChange}
          onBlur={handleBlur}
        />
      </Table.Cell>
      <Table.Cell>${item.lineTotal.toFixed(2)}</Table.Cell>
      <Table.Cell>
        <Button
          variant="danger"
          loading={removeCartItem.isPending}
          onClick={() => removeCartItem.mutate(item.productId)}
        >
          Remove
        </Button>
      </Table.Cell>
    </Table.Row>
  )
}
