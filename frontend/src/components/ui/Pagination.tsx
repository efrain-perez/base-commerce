import { Button } from './Button'

interface PaginationProps {
  page: number
  totalPages: number
  first: boolean
  last: boolean
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, first, last, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-center gap-4 py-4">
      <Button variant="secondary" disabled={first} onClick={() => onPageChange(page - 1)}>
        Prev
      </Button>
      <span className="text-sm text-gray-600">
        Page {page + 1} of {totalPages}
      </span>
      <Button variant="secondary" disabled={last} onClick={() => onPageChange(page + 1)}>
        Next
      </Button>
    </div>
  )
}
