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

// ---------- Esqueceu sua senha? ----------

// A resposta é sempre a mesma, exista ou não o usuário (o backend não revela quem está cadastrado).
export async function solicitarRecuperacaoSenha(usuario: string): Promise<string> {
  const { data } = await api.post<{ mensagem: string }>('/auth/recuperar-senha', { login: usuario })
  return data.mensagem
}

// O token vai no corpo, nunca na URL da API, para não ficar em logs de servidor.
export async function validarLinkRedefinicao(token: string): Promise<void> {
  await api.post('/auth/redefinir-senha/validar', { token })
}

export async function redefinirSenha(token: string, novaSenha: string): Promise<void> {
  await api.post('/auth/redefinir-senha', { token, novaSenha })
}
