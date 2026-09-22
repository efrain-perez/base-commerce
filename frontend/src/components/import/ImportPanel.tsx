import { useRef, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { useImportProducts } from '@/hooks/useProductMutations'

export function ImportPanel() {
  const [file, setFile] = useState<File | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const importProducts = useImportProducts()

  const onSubmit = () => {
    if (!file) return
    importProducts.mutate(file, {
      onSuccess: () => {
        setFile(null)
        if (inputRef.current) inputRef.current.value = ''
      },
    })
  }

  return (
    <div className="flex flex-wrap items-center gap-3 rounded-md border border-gray-200 bg-gray-50 px-4 py-3">
      <span className="text-sm font-medium text-gray-700">Import CSV</span>
      <input
        ref={inputRef}
        type="file"
        accept=".csv"
        onChange={(event) => setFile(event.target.files?.[0] ?? null)}
        className="text-sm text-gray-700 file:mr-3 file:rounded-md file:border-0 file:bg-blue-50 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-blue-700 hover:file:bg-blue-100"
      />
      <Button disabled={!file} loading={importProducts.isPending} onClick={onSubmit}>
        Import
      </Button>
      <span className="text-xs text-gray-500">Columns: name, sku, description, category, price, stock, weight_kg</span>
    </div>
  )
}
