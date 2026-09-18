import { useState } from 'react'
import type { CartItemResponse } from '@/api/types'
import { Table } from '@/components/ui/Table'
import { Button } from '@/components/ui/Button'
import { NumberInput } from '@/components/ui/NumberInput'
import { useRemoveCartItem, useUpdateCartItem } from '@/hooks/useCartMutations'

export function CartItemRow({ item }: { item: CartItemResponse }) {
  const [quantity, setQuantity] = useState(item.quantity)
  const updateCartItem = useUpdateCartItem()
  const removeCartItem = useRemoveCartItem()

  const commitQuantity = () => {
    if (quantity < 1) {
      setQuantity(item.quantity)
      return
    }
    if (quantity !== item.quantity) {
      updateCartItem.mutate({ productId: item.productId, quantity })
    }
  }

  return (
    <Table.Row>
      <Table.Cell>{item.sku}</Table.Cell>
      <Table.Cell>{item.name}</Table.Cell>
      <Table.Cell>${item.unitPrice.toFixed(2)}</Table.Cell>
      <Table.Cell>
        <NumberInput
          className="w-20"
          min={1}
          value={quantity}
          onChange={(event) => setQuantity(Number(event.target.value))}
          onBlur={commitQuantity}
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
