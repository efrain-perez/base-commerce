import { useState } from 'react'
import type { ImportJobResponse } from '@/api/types'
import { Spinner } from '@/components/ui/Spinner'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { useImportJobDetailQuery } from '@/hooks/useImportJobs'
import { ImportResultsTable } from './ImportResultsTable'

export function ImportHistoryRow({ job }: { job: ImportJobResponse }) {
  const [expanded, setExpanded] = useState(false)
  const { data, isLoading, isError, refetch } = useImportJobDetailQuery(expanded ? job.id : null)

  return (
    <div className="rounded-md border border-gray-200 bg-white">
      <button
        type="button"
        onClick={() => setExpanded((current) => !current)}
        className="flex w-full flex-wrap items-center gap-3 px-4 py-3 text-left text-sm hover:bg-gray-50"
      >
        <span className="text-gray-400">{expanded ? '▾' : '▸'}</span>
        <span className="text-gray-500">{new Date(job.executedAt).toLocaleString()}</span>
        <span className="font-medium text-gray-900">{job.fileName}</span>
        <span className="text-gray-600">
          {job.successCount}/{job.totalRows} succeeded
        </span>
        <span className="text-gray-500">{job.status}</span>
      </button>

      {expanded && (
        <div className="border-t border-gray-100 p-4">
          {isLoading && <Spinner />}
          {isError && <ErrorBanner message="Failed to load import job details." onRetry={refetch} />}
          {data && <ImportResultsTable result={data} />}
        </div>
      )}
    </div>
  )
}
