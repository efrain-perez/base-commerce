import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Table } from '@/components/ui/Table'
import { useCartQuery } from '@/hooks/useCart'
import { useCheckout } from '@/hooks/useCartMutations'
import {
  generateFakeCard,
  generateFakeShippingAddress,
  PAYMENT_OPTIONS,
  SHIPPING_OPTIONS,
  type PaymentMethodId,
  type ShippingMethodId,
} from '@/lib/fakeCheckout'

export function CheckoutPage() {
  const { data: cart, isLoading, isError, refetch } = useCartQuery()
  const checkout = useCheckout()
  const navigate = useNavigate()

  const [shippingMethod, setShippingMethod] = useState<ShippingMethodId>('standard')
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodId>('card')
  const fakeAddress = useMemo(() => generateFakeShippingAddress(), [])
  const fakeCard = useMemo(() => generateFakeCard(), [])

  const isEmpty = !isLoading && (!cart || cart.items.length === 0)

  useEffect(() => {
    if (isEmpty) navigate('/cart', { replace: true })
  }, [isEmpty, navigate])

  if (isLoading) return <Spinner />
  if (isError || !cart) return <ErrorBanner message="Failed to load your cart." onRetry={refetch} />
  if (isEmpty) return null

  const shippingCost = SHIPPING_OPTIONS.find((option) => option.id === shippingMethod)!.cost
  const total = cart.subtotal + shippingCost

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-gray-900">Review Your Order</h1>
        <p className="mt-1 text-sm text-gray-500">
          This is a demo checkout — the shipping address and card details below are randomly
          generated placeholders. No payment information is collected or charged.
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

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-4 sm:col-span-2">
          <fieldset className="space-y-3 rounded-md border border-gray-200 p-4">
            <legend className="px-1 text-sm font-medium text-gray-900">Shipping method</legend>
            {SHIPPING_OPTIONS.map((option) => (
              <label key={option.id} className="flex items-center justify-between gap-4 text-sm">
                <span className="flex items-center gap-2">
                  <input
                    type="radio"
                    name="shipping-method"
                    checked={shippingMethod === option.id}
                    onChange={() => setShippingMethod(option.id)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500"
                  />
                  <span>
                    {option.label} <span className="text-gray-500">· {option.detail}</span>
                  </span>
                </span>
                <span className="text-gray-700">
                  {option.cost === 0 ? 'Free' : `$${option.cost.toFixed(2)}`}
                </span>
              </label>
            ))}
            <p className="text-xs text-gray-500">Shipping to: {fakeAddress}</p>
          </fieldset>

          <fieldset className="space-y-3 rounded-md border border-gray-200 p-4">
            <legend className="px-1 text-sm font-medium text-gray-900">Payment method</legend>
            {PAYMENT_OPTIONS.map((option) => (
              <label key={option.id} className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="payment-method"
                  checked={paymentMethod === option.id}
                  onChange={() => setPaymentMethod(option.id)}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500"
                />
                {option.label}
              </label>
            ))}
            <p className="text-xs text-gray-500">
              {paymentMethod === 'card'
                ? `Card ${fakeCard.maskedNumber} · exp ${fakeCard.expiry}`
                : 'You will be redirected to PayPal (demo only).'}
            </p>
          </fieldset>
        </div>

        <fieldset className="h-fit space-y-4 rounded-md border border-gray-200 p-4 sm:col-span-1">
          <legend className="px-1 text-sm font-medium text-gray-900">Order summary</legend>
          <div className="space-y-1 text-sm text-gray-600">
            <div className="flex justify-between">
              <span>Subtotal</span>
              <span>${cart.subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span>Shipping</span>
              <span>{shippingCost === 0 ? 'Free' : `$${shippingCost.toFixed(2)}`}</span>
            </div>
          </div>
          <div className="flex justify-between border-t border-gray-200 pt-3 text-base font-semibold text-gray-900">
            <span>Total</span>
            <span>${total.toFixed(2)}</span>
          </div>
          <div className="flex flex-col gap-2 pt-2">
            <Button
              className="w-full"
              loading={checkout.isPending}
              onClick={() =>
                checkout.mutate(undefined, {
                  onSuccess: (order) =>
                    navigate(`/orders/${order.id}`, { state: { justPlaced: true } }),
                })
              }
            >
              Place Order
            </Button>
            <Link to="/cart">
              <Button variant="secondary" className="w-full">
                Back to Cart
              </Button>
            </Link>
          </div>
        </fieldset>
      </div>
    </div>
  )
}
