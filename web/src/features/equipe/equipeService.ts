import { api } from '../../services/api'
import type { PerfilAcesso } from '../usuario/usuarioService'

// PENDENTE: ainda não criou a senha (convite enviado) · INATIVO: sem acesso
export type SituacaoUsuario = 'ATIVO' | 'PENDENTE' | 'INATIVO'

export type UsuarioEquipe = {
  id: number
  login: string
  nomeCompleto: string | null
  email: string | null
  perfil: PerfilAcesso
  situacao: SituacaoUsuario
  // Validade do convite em aberto (só PENDENTE); null = convite expirado ou nunca enviado
  conviteExpiraEm: string | null
  dataInclusao: string
  // É o administrador logado
  voce: boolean
}

export type ConviteDados = {
  nomeCompleto: string
  email: string
  login: string
  perfil: PerfilAcesso
}

export async function listarUsuarios() {
  const { data } = await api.get<UsuarioEquipe[]>('/usuarios')
  return data
}

export async function convidarUsuario(dados: ConviteDados) {
  const { data } = await api.post<UsuarioEquipe>('/usuarios', dados)
  return data
}

export async function reenviarConvite(id: number) {
  const { data } = await api.post<UsuarioEquipe>(`/usuarios/${id}/convite`)
  return data
}

// Apaga o usuário pendente (o login fica livre de novo)
export async function cancelarConvite(id: number) {
  await api.delete(`/usuarios/${id}/convite`)
}

export async function alterarPerfilUsuario(id: number, perfil: PerfilAcesso) {
  const { data } = await api.put<UsuarioEquipe>(`/usuarios/${id}/perfil`, { perfil })
  return data
}

export async function desativarUsuario(id: number) {
  await api.post(`/usuarios/${id}/desativar`)
}

export async function reativarUsuario(id: number) {
  await api.post(`/usuarios/${id}/reativar`)
}

// ---------- Aceite do convite (público) ----------

export type ConviteInfo = {
  nomeCompleto: string | null
  login: string
  email: string | null
  nomeEscritorio: string
}

// O token vai no corpo, nunca na URL da API.
export async function validarConvite(token: string) {
  const { data } = await api.post<ConviteInfo>('/auth/convite/validar', { token })
  return data
}

export async function aceitarConvite(token: string, nomeCompleto: string, telefone: string, novaSenha: string) {
  const { data } = await api.post<{ login: string }>('/auth/convite/aceitar', { token, nomeCompleto, telefone, novaSenha })
  return data
}
