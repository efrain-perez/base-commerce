import type { ProductResponse } from '@/api/types'
import { Table } from '@/components/ui/Table'
import { Button } from '@/components/ui/Button'
import { useDeleteProduct } from '@/hooks/useProductMutations'

export interface SortState {
  field: string
  direction: 'asc' | 'desc'
}

interface ProductTableProps {
  products: ProductResponse[]
  onEdit: (product: ProductResponse) => void
  sort: SortState | null
  onSortChange: (field: string) => void
}

const SORTABLE_COLUMNS: { field: string; label: string }[] = [
  { field: 'sku', label: 'SKU' },
  { field: 'name', label: 'Name' },
  { field: 'category', label: 'Category' },
  { field: 'price', label: 'Price' },
  { field: 'stock', label: 'Stock' },
  { field: 'updatedAt', label: 'Updated' },
]

export function ProductTable({ products, onEdit, sort, onSortChange }: ProductTableProps) {
  const deleteProduct = useDeleteProduct()

  return (
    <Table>
      <Table.Head>
        <Table.Row>
          {SORTABLE_COLUMNS.map(({ field, label }) => {
            const isActive = sort?.field === field
            return (
              <Table.HeaderCell key={field}>
                <button
                  type="button"
                  onClick={() => onSortChange(field)}
                  className="cursor-pointer select-none hover:text-gray-900"
                >
                  {label}
                  {isActive && <span className="ml-1">{sort?.direction === 'asc' ? '▲' : '▼'}</span>}
                </button>
              </Table.HeaderCell>
            )
          })}
          <Table.HeaderCell />
        </Table.Row>
      </Table.Head>
      <Table.Body>
        {products.map((product) => (
          <Table.Row key={product.id}>
            <Table.Cell>{product.sku}</Table.Cell>
            <Table.Cell>{product.name}</Table.Cell>
            <Table.Cell>{product.category ?? '—'}</Table.Cell>
            <Table.Cell>${product.price.toFixed(2)}</Table.Cell>
            <Table.Cell>{product.stock}</Table.Cell>
            <Table.Cell>{new Date(product.updatedAt).toLocaleDateString()}</Table.Cell>
            <Table.Cell>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => onEdit(product)}>
                  Edit
                </Button>
                <Button
                  variant="danger"
                  loading={deleteProduct.isPending}
                  onClick={() => {
                    if (window.confirm(`Delete "${product.name}"?`)) {
                      deleteProduct.mutate(product.id)
                    }
                  }}
                >
                  Delete
                </Button>
              </div>
            </Table.Cell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  )
}
