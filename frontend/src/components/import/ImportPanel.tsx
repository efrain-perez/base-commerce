import { useRef, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { useImportProducts } from '@/hooks/useProductMutations'
import type { ImportJobDetailResponse } from '@/api/types'
import { ImportResultsTable } from './ImportResultsTable'

export function ImportPanel() {
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<ImportJobDetailResponse | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const importProducts = useImportProducts()

  const onSubmit = () => {
    if (!file) return
    importProducts.mutate(file, {
      onSuccess: (data) => {
        setResult(data)
        setFile(null)
        if (inputRef.current) inputRef.current.value = ''
      },
    })
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <h3 className="text-base font-semibold text-gray-900">Import products from CSV</h3>
      <p className="mt-1 text-sm text-gray-500">
        Columns: name, sku, description, category, price, stock, weight_kg
      </p>
      <div className="mt-3 flex items-center gap-3">
        <input
          ref={inputRef}
          type="file"
          accept=".csv"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          className="text-sm"
        />
        <Button disabled={!file} loading={importProducts.isPending} onClick={onSubmit}>
          Import
        </Button>
      </div>
      {result && <ImportResultsTable result={result} />}
    </div>
  )
}
