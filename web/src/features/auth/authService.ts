import { api } from '../../services/api'
import { tokenStore } from './tokenStore'
import type { AuthResponse, PrimeiroAcessoDados } from './types'

export async function login(usuario: string, senha: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { login: usuario, senha })
  tokenStore.set(data.accessToken)
  return data
}

export async function logout(): Promise<void> {
  try {
    await api.post('/auth/logout')
  } catch {
    // Mesmo sem conseguir avisar o servidor, a sessão local é encerrada
  } finally {
    tokenStore.set(null)
  }
}

export async function concluirPrimeiroAcesso(dados: PrimeiroAcessoDados): Promise<void> {
  await api.post('/auth/primeiro-acesso', dados)
}
