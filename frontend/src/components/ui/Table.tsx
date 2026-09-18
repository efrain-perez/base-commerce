import type { ReactNode, TdHTMLAttributes, ThHTMLAttributes } from 'react'

function Root({ children }: { children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-md border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200 text-left text-sm">{children}</table>
    </div>
  )
}

function Head({ children }: { children: ReactNode }) {
  return <thead className="bg-gray-50">{children}</thead>
}

function Body({ children }: { children: ReactNode }) {
  return <tbody className="divide-y divide-gray-100 bg-white">{children}</tbody>
}

function Row({ children }: { children: ReactNode }) {
  return <tr>{children}</tr>
}

function HeaderCell({ children, ...rest }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th className="px-4 py-2 font-medium text-gray-600" {...rest}>
      {children}
    </th>
  )
}

function Cell({ children, ...rest }: TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td className="px-4 py-2 text-gray-800" {...rest}>
      {children}
    </td>
  )
}

export const Table = Object.assign(Root, { Head, Body, Row, HeaderCell, Cell })
