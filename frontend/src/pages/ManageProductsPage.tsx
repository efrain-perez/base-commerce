import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Pagination } from '@/components/ui/Pagination'
import { Modal } from '@/components/ui/Modal'
import { ProductForm } from '@/components/product/ProductForm'
import { ProductTable } from '@/components/product/ProductTable'
import { ImportPanel } from '@/components/import/ImportPanel'
import { useProductsQuery } from '@/hooks/useProducts'
import type { ProductResponse } from '@/api/types'

const PAGE_SIZE = 10

type ModalState = { mode: 'create' } | { mode: 'edit'; product: ProductResponse } | null

export function ManageProductsPage() {
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState<ModalState>(null)

  const { data, isLoading, isError, refetch } = useProductsQuery({ page, size: PAGE_SIZE })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Manage Products</h1>
        <Button onClick={() => setModal({ mode: 'create' })}>New Product</Button>
      </div>

      {isLoading && <Spinner />}
      {isError && <ErrorBanner message="Failed to load products." onRetry={refetch} />}
      {data && (
        <>
          <ProductTable products={data.content} onEdit={(product) => setModal({ mode: 'edit', product })} />
          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            first={data.first}
            last={data.last}
            onPageChange={setPage}
          />
        </>
      )}

      <ImportPanel />

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
