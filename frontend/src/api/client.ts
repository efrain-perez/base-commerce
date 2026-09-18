import { hasFieldErrors, type ProblemDetail } from './types'

export class ApiError extends Error {
  status: number
  problem: ProblemDetail

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
    this.status = status
    this.problem = problem
  }

  get fieldErrors(): Record<string, string> | undefined {
    return hasFieldErrors(this.problem) ? this.problem.fieldErrors : undefined
  }
}

interface ApiFetchOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  query?: Record<string, string | number | boolean | undefined>
}

export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const url = buildUrl(path, options.query)
  const isFormData = options.body instanceof FormData

  const res = await fetch(url, {
    method: options.method ?? 'GET',
    headers: isFormData ? undefined : { 'Content-Type': 'application/json' },
    body: isFormData
      ? (options.body as FormData)
      : options.body !== undefined
        ? JSON.stringify(options.body)
        : undefined,
  })

  if (res.status === 204) {
    return undefined as T
  }

  const contentType = res.headers.get('content-type') ?? ''

  if (!res.ok) {
    if (contentType.includes('application/problem+json')) {
      const problem = (await res.json()) as ProblemDetail
      throw new ApiError(res.status, problem)
    }
    throw new ApiError(res.status, { status: res.status, detail: res.statusText })
  }

  if (contentType.includes('application/json')) {
    return (await res.json()) as T
  }
  return undefined as T
}

function buildUrl(path: string, query?: ApiFetchOptions['query']): string {
  if (!query) return path
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) params.set(key, String(value))
  }
  const qs = params.toString()
  return qs ? `${path}?${qs}` : path
}
