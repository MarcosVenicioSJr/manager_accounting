import { api } from '../../services/api'

export type TipoAlteracao = 'SENHA' | 'EMAIL'

// Pedido esperando o clique no link do e-mail. enviadoPara: quem recebeu o link.
export type TrocaPendente = {
  enviadoPara: string
  novoEmail: string | null
  expiraEm: string
}

export type Perfil = {
  login: string
  nomeCompleto: string | null
  email: string | null
  telefone: string | null
  nomeEscritorio: string
  trocaSenhaPendente: TrocaPendente | null
  trocaEmailPendente: TrocaPendente | null
}

export async function buscarPerfil() {
  const { data } = await api.get<Perfil>('/perfil')
  return data
}

// Nome e telefone mudam na hora.
export async function atualizarPerfil(nomeCompleto: string, telefone: string) {
  const { data } = await api.put<Perfil>('/perfil', { nomeCompleto, telefone })
  return data
}

// A senha só muda depois que a pessoa confirmar pelo link enviado ao e-mail atual.
export async function pedirTrocaSenha(senhaAtual: string, novaSenha: string) {
  const { data } = await api.post<TrocaPendente>('/perfil/senha', { senhaAtual, novaSenha })
  return data
}

// O e-mail só muda depois que a pessoa confirmar pelo link enviado ao NOVO endereço.
export async function pedirTrocaEmail(senhaAtual: string, novoEmail: string) {
  const { data } = await api.post<TrocaPendente>('/perfil/email', { senhaAtual, novoEmail })
  return data
}

export async function cancelarTroca(tipo: TipoAlteracao) {
  await api.delete(`/perfil/alteracoes/${tipo}`)
}

// ---------- Confirmação pelo link do e-mail (público) ----------

export type ConfirmacaoInfo = { tipo: TipoAlteracao; novoEmail: string | null }

// O token vai no corpo, nunca na URL da API.
export async function validarConfirmacao(token: string) {
  const { data } = await api.post<ConfirmacaoInfo>('/auth/confirmar-alteracao/validar', { token })
  return data
}

export async function confirmarAlteracao(token: string) {
  const { data } = await api.post<ConfirmacaoInfo>('/auth/confirmar-alteracao', { token })
  return data
}
