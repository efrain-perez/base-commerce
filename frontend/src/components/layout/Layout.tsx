import { NavLink, Outlet } from 'react-router-dom'
import { useCartQuery } from '@/hooks/useCart'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-blue-600 text-white' : 'text-gray-700 hover:bg-gray-100'
  }`

export function Layout() {
  const { data: cart } = useCartQuery()
  const itemCount = cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <span className="text-lg font-semibold text-gray-900">Gila Commerce</span>
          <div className="flex items-center gap-2">
            <NavLink to="/" end className={navLinkClass}>
              Shop
            </NavLink>
            <NavLink to="/manage" className={navLinkClass}>
              Manage Products
            </NavLink>
            <NavLink to="/cart" className={navLinkClass}>
              Cart{itemCount > 0 ? ` (${itemCount})` : ''}
            </NavLink>
          </div>
        </div>
      </nav>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
