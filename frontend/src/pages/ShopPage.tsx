import { useState } from 'react'
import { TextInput } from '@/components/ui/TextInput'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Pagination } from '@/components/ui/Pagination'
import { ProductCard } from '@/components/product/ProductCard'
import { useProductsQuery } from '@/hooks/useProducts'
import { useDebouncedValue } from '@/lib/debounce'

const PAGE_SIZE = 12

export function ShopPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const debouncedSearch = useDebouncedValue(search, 300)

  const { data, isLoading, isError, refetch } = useProductsQuery({
    name: debouncedSearch || undefined,
    page,
    size: PAGE_SIZE,
  })

  return (
    <div>
      <TextInput
        placeholder="Search products..."
        value={search}
        onChange={(event) => {
          setSearch(event.target.value)
          setPage(0)
        }}
        className="max-w-sm"
      />

      <div className="mt-6">
        {isLoading && <Spinner />}
        {isError && <ErrorBanner message="Failed to load products." onRetry={refetch} />}
        {data && data.content.length === 0 && (
          <p className="py-8 text-center text-gray-500">No products found.</p>
        )}
        {data && data.content.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
            {data.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
        {data && (
          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            first={data.first}
            last={data.last}
            onPageChange={setPage}
          />
        )}
      </div>
    </div>
  )
}
