import axios from 'axios'

// Campos padrão do ProblemDetail (RFC 9457); o resto são mensagens de validação por campo.
const RESERVED_KEYS = new Set(['type', 'title', 'status', 'detail', 'instance', 'codigo'])

// Extrai a mensagem de erro devolvida pelo backend (ProblemDetail) ou usa um texto padrão.
export function getErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError(error)) return fallback
  if (!error.response) return 'Não foi possível conectar ao servidor. Verifique sua conexão.'

  const data: unknown = error.response.data
  if (typeof data !== 'object' || data === null) return fallback

  const problem = data as Record<string, unknown>
  const fieldMessages = Object.entries(problem)
    .filter(([key, value]) => !RESERVED_KEYS.has(key) && typeof value === 'string')
    .map(([, value]) => value as string)

  if (fieldMessages.length > 0) return fieldMessages.join(' ')
  return typeof problem.detail === 'string' && problem.detail ? problem.detail : fallback
}

// Código do erro (campo "codigo" do ProblemDetail), quando o backend informa. Ex.: DOCUMENTO_DUPLICADO.
export function getErrorCode(error: unknown): string | null {
  if (!axios.isAxiosError(error)) return null
  const data: unknown = error.response?.data
  if (typeof data !== 'object' || data === null) return null
  const codigo = (data as Record<string, unknown>).codigo
  return typeof codigo === 'string' ? codigo : null
}

export function getErrorStatus(error: unknown): number | null {
  return axios.isAxiosError(error) ? (error.response?.status ?? null) : null
}
