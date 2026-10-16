import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { refreshSession, setSessionExpiredHandler } from '../../services/api'
import { defaultTheme } from '../../theme/theme'
import { useTenantTheme } from '../../hooks/useTenantTheme'
import { buscarTema } from '../tenant/tenantService'
import { AuthContext, type AuthContextValue, type AuthStatus } from './AuthContext'
import * as authService from './authService'
import { tokenStore } from './tokenStore'
import type { AuthResponse, PrimeiroAcessoDados } from './types'

// Mantém o estado da sessão. Ao abrir/recarregar a página tenta recuperar a sessão pelo cookie de refresh.
export function AuthProvider({ children }: { children: ReactNode }) {
  const { setTheme } = useTenantTheme()
  const [status, setStatus] = useState<AuthStatus>('loading')

  const carregarTema = useCallback(async () => {
    try {
      setTheme(await buscarTema())
    } catch {
      // Sem tema do tenant, fica o padrão neutro
    }
  }, [setTheme])

  const aplicarSessao = useCallback(
    async (resposta: AuthResponse) => {
      if (resposta.primeiroAcessoPendente) {
        setStatus('primeiro-acesso')
        return
      }
      await carregarTema()
      setStatus('authenticated')
    },
    [carregarTema],
  )

  useEffect(() => {
    let cancelado = false
    refreshSession()
      .then((resposta) => (cancelado ? undefined : aplicarSessao(resposta)))
      .catch(() => {
        if (!cancelado) setStatus('anonymous')
      })
    return () => {
      cancelado = true
    }
  }, [aplicarSessao])

  useEffect(() => {
    setSessionExpiredHandler(() => {
      tokenStore.set(null)
      setTheme(defaultTheme)
      setStatus('anonymous')
    })
    return () => setSessionExpiredHandler(null)
  }, [setTheme])

  const login = useCallback(
    async (usuario: string, senha: string, manterConectado: boolean) => {
      const resposta = await authService.login(usuario, senha, manterConectado)
      await aplicarSessao(resposta)
    },
    [aplicarSessao],
  )

  const concluirPrimeiroAcesso = useCallback(
    async (dados: PrimeiroAcessoDados) => {
      await authService.concluirPrimeiroAcesso(dados)
      await carregarTema()
      setStatus('authenticated')
    },
    [carregarTema],
  )

  const logout = useCallback(async () => {
    await authService.logout()
    setTheme(defaultTheme)
    setStatus('anonymous')
  }, [setTheme])

  const value = useMemo<AuthContextValue>(
    () => ({ status, login, concluirPrimeiroAcesso, logout }),
    [status, login, concluirPrimeiroAcesso, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
