import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { tokenStore } from '../features/auth/tokenStore'
import type { AuthResponse } from '../features/auth/types'

// Cliente HTTP do backend. O cookie HttpOnly do refresh token exige withCredentials.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// Endpoints de autenticação não passam pela renovação automática (evita laço no próprio refresh).
const AUTH_ENDPOINTS = ['/auth/login', '/auth/refresh', '/auth/logout']

type RetriableRequest = InternalAxiosRequestConfig & { _retry?: boolean }

let refreshInFlight: Promise<AuthResponse> | null = null
let sessionExpiredHandler: (() => void) | null = null

// Chamado quando o refresh falha (sessão de 8h acabou ou foi revogada): o app volta para o login.
export function setSessionExpiredHandler(handler: (() => void) | null): void {
  sessionExpiredHandler = handler
}

// Renova o access token pelo cookie de refresh. Chamadas simultâneas compartilham a mesma requisição.
export function refreshSession(): Promise<AuthResponse> {
  if (!refreshInFlight) {
    refreshInFlight = api
      .post<AuthResponse>('/auth/refresh')
      .then((response) => {
        tokenStore.set(response.data.accessToken)
        return response.data
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableRequest | undefined
    const isAuthEndpoint = AUTH_ENDPOINTS.some((endpoint) => original?.url?.startsWith(endpoint))

    if (error.response?.status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true
      try {
        const session = await refreshSession()
        original.headers.set('Authorization', `Bearer ${session.accessToken}`)
        return api(original)
      } catch {
        sessionExpiredHandler?.()
      }
    }
    return Promise.reject(error)
  },
)
