import type { ImportJobDetailResponse } from '@/api/types'
import { Table } from '@/components/ui/Table'

export function ImportResultsTable({ result }: { result: ImportJobDetailResponse }) {
  const { job, errors } = result

  return (
    <div className="mt-4 space-y-4">
      <div className="flex flex-wrap gap-3">
        <StatChip label="Total rows" value={job.totalRows} />
        <StatChip label="Succeeded" value={job.successCount} tone="text-green-700 bg-green-50" />
        <StatChip label="Failed" value={job.failureCount} tone="text-red-700 bg-red-50" />
        <StatChip label="Status" value={job.status} />
      </div>

      {errors.length > 0 && (
        <Table>
          <Table.Head>
            <Table.Row>
              <Table.HeaderCell>Row</Table.HeaderCell>
              <Table.HeaderCell>Raw data</Table.HeaderCell>
              <Table.HeaderCell>Reason</Table.HeaderCell>
            </Table.Row>
          </Table.Head>
          <Table.Body>
            {errors.map((error) => (
              <Table.Row key={error.id}>
                <Table.Cell>{error.rowNumber}</Table.Cell>
                <Table.Cell className="font-mono text-xs">{error.rawData}</Table.Cell>
                <Table.Cell>{error.errorReason}</Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </div>
  )
}

function StatChip({ label, value, tone = 'text-gray-700 bg-gray-100' }: { label: string; value: string | number; tone?: string }) {
  return (
    <div className={`rounded-md px-3 py-2 text-sm ${tone}`}>
      <span className="font-medium">{label}:</span> {value}
    </div>
  )
}
