import { Button } from './Button'

interface PaginationProps {
  page: number
  totalPages: number
  first: boolean
  last: boolean
  onPageChange: (page: number) => void
  pageSize: number
  pageSizeOptions: number[]
  onPageSizeChange: (size: number) => void
}

export function Pagination({
  page,
  totalPages,
  first,
  last,
  onPageChange,
  pageSize,
  pageSizeOptions,
  onPageSizeChange,
}: PaginationProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-4 py-4">
      <label className="flex items-center gap-2 text-sm text-gray-600">
        Show
        <select
          value={pageSize}
          onChange={(event) => onPageSizeChange(Number(event.target.value))}
          className="rounded-md border border-gray-300 px-2 py-1 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {pageSizeOptions.map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </select>
        per page
      </label>

      {totalPages > 1 && (
        <div className="flex items-center gap-4">
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
      )}
    </div>
  )
}
