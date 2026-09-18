import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Table } from '@/components/ui/Table'
import { CartItemRow } from '@/components/cart/CartItemRow'
import { useCartQuery } from '@/hooks/useCart'

export function CartPage() {
  const { data: cart, isLoading, isError, refetch } = useCartQuery()
  const navigate = useNavigate()

  if (isLoading) return <Spinner />
  if (isError || !cart) return <ErrorBanner message="Failed to load your cart." onRetry={refetch} />

  const isEmpty = cart.items.length === 0

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-gray-900">Your Cart</h1>

      {isEmpty ? (
        <p className="text-gray-500">Your cart is empty.</p>
      ) : (
        <Table>
          <Table.Head>
            <Table.Row>
              <Table.HeaderCell>SKU</Table.HeaderCell>
              <Table.HeaderCell>Name</Table.HeaderCell>
              <Table.HeaderCell>Unit price</Table.HeaderCell>
              <Table.HeaderCell>Quantity</Table.HeaderCell>
              <Table.HeaderCell>Line total</Table.HeaderCell>
              <Table.HeaderCell />
            </Table.Row>
          </Table.Head>
          <Table.Body>
            {cart.items.map((item) => (
              <CartItemRow key={item.productId} item={item} />
            ))}
          </Table.Body>
        </Table>
      )}

      <div className="flex items-center justify-between border-t border-gray-200 pt-4">
        <span className="text-lg font-semibold text-gray-900">Subtotal: ${cart.subtotal.toFixed(2)}</span>
        <Button disabled={isEmpty} onClick={() => navigate('/checkout')}>
          Proceed to Checkout
        </Button>
      </div>
    </div>
  )
}
