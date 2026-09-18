import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Table } from '@/components/ui/Table'
import { useCartQuery } from '@/hooks/useCart'
import { useCheckout } from '@/hooks/useCartMutations'

export function CheckoutPage() {
  const { data: cart, isLoading, isError, refetch } = useCartQuery()
  const checkout = useCheckout()
  const navigate = useNavigate()

  const isEmpty = !isLoading && (!cart || cart.items.length === 0)

  useEffect(() => {
    if (isEmpty) navigate('/cart', { replace: true })
  }, [isEmpty, navigate])

  if (isLoading) return <Spinner />
  if (isError || !cart) return <ErrorBanner message="Failed to load your cart." onRetry={refetch} />
  if (isEmpty) return null

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Review Your Order</h1>
        <p className="mt-1 text-sm text-gray-500">
          This is a demo checkout — no payment information is collected or charged.
        </p>
      </div>

      <Table>
        <Table.Head>
          <Table.Row>
            <Table.HeaderCell>SKU</Table.HeaderCell>
            <Table.HeaderCell>Name</Table.HeaderCell>
            <Table.HeaderCell>Unit price</Table.HeaderCell>
            <Table.HeaderCell>Quantity</Table.HeaderCell>
            <Table.HeaderCell>Line total</Table.HeaderCell>
          </Table.Row>
        </Table.Head>
        <Table.Body>
          {cart.items.map((item) => (
            <Table.Row key={item.productId}>
              <Table.Cell>{item.sku}</Table.Cell>
              <Table.Cell>{item.name}</Table.Cell>
              <Table.Cell>${item.unitPrice.toFixed(2)}</Table.Cell>
              <Table.Cell>{item.quantity}</Table.Cell>
              <Table.Cell>${item.lineTotal.toFixed(2)}</Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>

      <div className="flex items-center justify-between border-t border-gray-200 pt-4">
        <span className="text-lg font-semibold text-gray-900">Total: ${cart.subtotal.toFixed(2)}</span>
        <div className="flex gap-2">
          <Link to="/cart">
            <Button variant="secondary">Back to Cart</Button>
          </Link>
          <Button
            loading={checkout.isPending}
            onClick={() =>
              checkout.mutate(undefined, { onSuccess: (order) => navigate(`/orders/${order.id}`) })
            }
          >
            Place Order
          </Button>
        </div>
      </div>
    </div>
  )
}
