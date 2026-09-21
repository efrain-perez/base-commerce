import { useState } from 'react'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Pagination } from '@/components/ui/Pagination'
import { useImportJobsQuery } from '@/hooks/useImportJobs'
import { ImportHistoryRow } from './ImportHistoryRow'

const DEFAULT_PAGE_SIZE = 10
const PAGE_SIZE_OPTIONS = [10, 25, 50]

export function ImportHistoryPanel() {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)
  const { data, isLoading, isError, refetch } = useImportJobsQuery({ page, size: pageSize })

  const handlePageSizeChange = (size: number) => {
    setPageSize(size)
    setPage(0)
  }

  if (isLoading) return <Spinner />
  if (isError || !data) return <ErrorBanner message="Failed to load import history." onRetry={refetch} />
  if (data.content.length === 0) return <p className="py-8 text-center text-gray-500">No import jobs yet.</p>

  return (
    <div className="space-y-3">
      {data.content.map((job) => (
        <ImportHistoryRow key={job.id} job={job} />
      ))}
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
    </div>
  )
}
