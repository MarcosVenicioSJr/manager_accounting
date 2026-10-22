import { api } from '../../services/api'

export type PerfilAcesso = 'ADMIN' | 'USUARIO'

// Contador logado, exibido no menu do backoffice. perfil ADMIN: gerencia usuários e pode desativar clientes.
export type UsuarioLogado = {
  login: string
  nomeCompleto: string | null
  email: string | null
  perfil: PerfilAcesso
}

export async function buscarUsuarioLogado(): Promise<UsuarioLogado> {
  const { data } = await api.get<UsuarioLogado>('/usuario/me')
  return data
}
