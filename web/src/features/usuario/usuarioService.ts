import { api } from '../../services/api'

// Contador logado, exibido no cabeçalho do backoffice.
export type UsuarioLogado = {
  login: string
  nomeCompleto: string | null
  email: string | null
}

export async function buscarUsuarioLogado(): Promise<UsuarioLogado> {
  const { data } = await api.get<UsuarioLogado>('/usuario/me')
  return data
}
