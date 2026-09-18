import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="space-y-4 text-center">
      <p className="text-gray-600">Page not found.</p>
      <Link to="/" className="text-blue-600 hover:underline">
        Back to Shop
      </Link>
    </div>
  )
}
