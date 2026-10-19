import { api } from '../../services/api'
import { tokenStore } from './tokenStore'
import type { AuthResponse, PrimeiroAcessoDados } from './types'

// manterConectado: o cookie de refresh sobrevive ao fechamento do navegador (a sessão continua valendo 8 horas).
export async function login(usuario: string, senha: string, manterConectado: boolean): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { login: usuario, senha, manterConectado })
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
