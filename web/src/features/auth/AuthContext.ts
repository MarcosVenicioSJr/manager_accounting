import { createContext } from 'react'
import type { UsuarioLogado } from '../usuario/usuarioService'
import type { PrimeiroAcessoDados } from './types'

// loading: verificando se já existe sessão · anonymous: sem sessão
// primeiro-acesso: logado com a senha temporária, falta trocá-la · authenticated: sessão completa
export type AuthStatus = 'loading' | 'anonymous' | 'primeiro-acesso' | 'authenticated'

export type AuthContextValue = {
  status: AuthStatus
  // Preenchido quando a sessão está completa (status authenticated)
  usuario: UsuarioLogado | null
  login: (usuario: string, senha: string, manterConectado: boolean) => Promise<void>
  concluirPrimeiroAcesso: (dados: PrimeiroAcessoDados) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
