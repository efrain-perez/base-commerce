import type { ProductResponse } from '@/api/types'
import { Table } from '@/components/ui/Table'
import { Button } from '@/components/ui/Button'
import { useDeleteProduct } from '@/hooks/useProductMutations'

interface ProductTableProps {
  products: ProductResponse[]
  onEdit: (product: ProductResponse) => void
}

export function ProductTable({ products, onEdit }: ProductTableProps) {
  const deleteProduct = useDeleteProduct()

  return (
    <Table>
      <Table.Head>
        <Table.Row>
          <Table.HeaderCell>SKU</Table.HeaderCell>
          <Table.HeaderCell>Name</Table.HeaderCell>
          <Table.HeaderCell>Category</Table.HeaderCell>
          <Table.HeaderCell>Price</Table.HeaderCell>
          <Table.HeaderCell>Stock</Table.HeaderCell>
          <Table.HeaderCell>Updated</Table.HeaderCell>
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
