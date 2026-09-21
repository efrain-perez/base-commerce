import { useState } from 'react'
import { TextInput } from '@/components/ui/TextInput'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { ProductCard } from '@/components/product/ProductCard'
import { useInfiniteProductsQuery } from '@/hooks/useProducts'
import { useDebouncedValue } from '@/lib/debounce'

const PAGE_SIZE = 12

export function ShopPage() {
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebouncedValue(search, 300)

  const { data, isLoading, isError, refetch, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteProductsQuery({
    name: debouncedSearch || undefined,
    size: PAGE_SIZE,
  })

  const products = data?.pages.flatMap((page) => page.content) ?? []

  return (
    <div>
      <TextInput
        placeholder="Search products..."
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        className="max-w-sm"
      />

      <div className="mt-6">
        {isLoading && <Spinner />}
        {isError && <ErrorBanner message="Failed to load products." onRetry={refetch} />}
        {!isLoading && products.length === 0 && (
          <p className="py-8 text-center text-gray-500">No products found.</p>
        )}
        {products.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
        {hasNextPage && (
          <div className="flex justify-center py-6">
            <Button variant="secondary" loading={isFetchingNextPage} onClick={() => fetchNextPage()}>
              Load more
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
