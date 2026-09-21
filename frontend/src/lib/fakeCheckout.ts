const STREET_NAMES = ['Maple Ave', 'Oak St', 'Cedar Ln', 'Elm Dr', 'Pine Ct', 'Birch Rd']

const CITIES = [
  { city: 'Austin', state: 'TX', zip: '73301' },
  { city: 'Denver', state: 'CO', zip: '80202' },
  { city: 'Portland', state: 'OR', zip: '97201' },
  { city: 'Raleigh', state: 'NC', zip: '27601' },
]

export function generateFakeShippingAddress(): string {
  const streetNumber = Math.floor(100 + Math.random() * 9899)
  const street = STREET_NAMES[Math.floor(Math.random() * STREET_NAMES.length)]
  const { city, state, zip } = CITIES[Math.floor(Math.random() * CITIES.length)]
  return `${streetNumber} ${street}, ${city}, ${state} ${zip}`
}

export interface FakeCard {
  maskedNumber: string
  expiry: string
}

export function generateFakeCard(): FakeCard {
  const lastFour = String(Math.floor(1000 + Math.random() * 8999))
  const month = String(1 + Math.floor(Math.random() * 12)).padStart(2, '0')
  const year = String(27 + Math.floor(Math.random() * 4))
  return { maskedNumber: `•••• •••• •••• ${lastFour}`, expiry: `${month}/${year}` }
}

export const SHIPPING_OPTIONS = [
  { id: 'standard', label: 'Standard', detail: '5–7 business days', cost: 0 },
  { id: 'express', label: 'Express', detail: '1–2 business days', cost: 9.99 },
] as const

export type ShippingMethodId = (typeof SHIPPING_OPTIONS)[number]['id']

export const PAYMENT_OPTIONS = [
  { id: 'card', label: 'Credit Card' },
  { id: 'paypal', label: 'PayPal' },
] as const

export type PaymentMethodId = (typeof PAYMENT_OPTIONS)[number]['id']
