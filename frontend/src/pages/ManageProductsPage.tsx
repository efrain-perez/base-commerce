import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Pagination } from '@/components/ui/Pagination'
import { Modal } from '@/components/ui/Modal'
import { Tabs } from '@/components/ui/Tabs'
import { ProductForm } from '@/components/product/ProductForm'
import { ProductTable, type SortState } from '@/components/product/ProductTable'
import { ImportPanel } from '@/components/import/ImportPanel'
import { ImportHistoryPanel } from '@/components/import/ImportHistoryPanel'
import { useProductsQuery } from '@/hooks/useProducts'
import type { ProductResponse } from '@/api/types'

const DEFAULT_PAGE_SIZE = 10
const PAGE_SIZE_OPTIONS = [10, 25, 50, 100]

const TABS = [
  { id: 'products', label: 'Products' },
  { id: 'history', label: 'Import History' },
] as const
type TabId = (typeof TABS)[number]['id']

type ModalState = { mode: 'create' } | { mode: 'edit'; product: ProductResponse } | null

export function ManageProductsPage() {
  const [activeTab, setActiveTab] = useState<TabId>('products')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)
  const [sort, setSort] = useState<SortState | null>(null)
  const [modal, setModal] = useState<ModalState>(null)

  const { data, isLoading, isError, refetch } = useProductsQuery({
    page,
    size: pageSize,
    sort: sort ? `${sort.field},${sort.direction}` : undefined,
  })

  const handleSortChange = (field: string) => {
    setSort((current) =>
      current?.field === field ? { field, direction: current.direction === 'asc' ? 'desc' : 'asc' } : { field, direction: 'asc' },
    )
    setPage(0)
  }

  const handlePageSizeChange = (size: number) => {
    setPageSize(size)
    setPage(0)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Manage Products</h1>
        {activeTab === 'products' && <Button onClick={() => setModal({ mode: 'create' })}>New Product</Button>}
      </div>

      <Tabs tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'products' && (
        <>
          <ImportPanel />
          {isLoading && <Spinner />}
          {isError && <ErrorBanner message="Failed to load products." onRetry={refetch} />}
          {data && (
            <>
              <ProductTable
                products={data.content}
                onEdit={(product) => setModal({ mode: 'edit', product })}
                sort={sort}
                onSortChange={handleSortChange}
              />
              <Pagination
                page={data.number}
                totalPages={data.totalPages}
                first={data.first}
                last={data.last}
                onPageChange={setPage}
                pageSize={pageSize}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                onPageSizeChange={handlePageSizeChange}
              />
            </>
          )}
        </>
      )}

      {activeTab === 'history' && <ImportHistoryPanel />}

      {modal && (
        <Modal title={modal.mode === 'create' ? 'New Product' : 'Edit Product'} onClose={() => setModal(null)}>
          <ProductForm
            mode={modal.mode}
            product={modal.mode === 'edit' ? modal.product : undefined}
            onDone={() => setModal(null)}
          />
        </Modal>
      )}
    </div>
  )
}
