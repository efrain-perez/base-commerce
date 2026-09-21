import { useQuery } from '@tanstack/react-query'
import { getImportJobDetail, listImportJobs } from '@/api/importJobs'

export function useImportJobsQuery(params: { page: number; size: number }) {
  return useQuery({
    queryKey: ['import-jobs', params],
    queryFn: () => listImportJobs(params),
  })
}

export function useImportJobDetailQuery(id: number | null) {
  return useQuery({
    queryKey: ['import-jobs', id],
    queryFn: () => getImportJobDetail(id as number),
    enabled: id !== null,
  })
}
