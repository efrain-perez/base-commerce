import { apiFetch } from './client'
import type { ImportJobDetailResponse, ImportJobResponse, Page } from './types'

export function listImportJobs(params: { page: number; size: number }) {
  return apiFetch<Page<ImportJobResponse>>('/import-jobs', { query: params })
}

export function getImportJobDetail(id: number) {
  return apiFetch<ImportJobDetailResponse>(`/import-jobs/${id}`)
}
