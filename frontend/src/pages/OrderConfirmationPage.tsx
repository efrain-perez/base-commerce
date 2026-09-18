import { Link, useParams } from 'react-router-dom'
import { Spinner } from '@/components/ui/Spinner'
import { Table } from '@/components/ui/Table'
import { useOrderQuery } from '@/hooks/useOrder'

export function OrderConfirmationPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const id = orderId ? Number(orderId) : undefined
  const { data: order, isLoading, isError } = useOrderQuery(id)

  if (isLoading) return <Spinner />

  if (isError || !order) {
    return (
      <div className="space-y-4 text-center">
        <p className="text-gray-600">Order not found.</p>
        <Link to="/" className="text-blue-600 hover:underline">
          Continue Shopping
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Order #{order.id}</h1>
        <p className="text-sm text-gray-500">
          {order.status} · {new Date(order.createdAt).toLocaleString()}
        </p>
      </div>

      <Table>
        <Table.Head>
          <Table.Row>
            <Table.HeaderCell>Name</Table.HeaderCell>
            <Table.HeaderCell>Price</Table.HeaderCell>
            <Table.HeaderCell>Quantity</Table.HeaderCell>
            <Table.HeaderCell>Line total</Table.HeaderCell>
          </Table.Row>
        </Table.Head>
        <Table.Body>
          {order.items.map((item) => (
            <Table.Row key={item.productId}>
              <Table.Cell>{item.name}</Table.Cell>
              <Table.Cell>${item.priceAtPurchase.toFixed(2)}</Table.Cell>
              <Table.Cell>{item.quantity}</Table.Cell>
              <Table.Cell>${item.lineTotal.toFixed(2)}</Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>

      <div className="flex items-center justify-between border-t border-gray-200 pt-4">
        <span className="text-lg font-semibold text-gray-900">Total: ${order.orderTotal.toFixed(2)}</span>
        <Link to="/" className="text-blue-600 hover:underline">
          Continue Shopping
        </Link>
      </div>
    </div>
  )
}
