import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { FullPageLoading } from '../../components/FullPageLoading'
import { useAuth } from './useAuth'

// Rota do backoffice: exige sessão completa. Quem ainda não trocou a senha temporária vai para o primeiro acesso.
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth()

  if (status === 'loading') return <FullPageLoading />
  if (status === 'anonymous') return <Navigate to="/login" replace />
  if (status === 'primeiro-acesso') return <Navigate to="/primeiro-acesso" replace />
  return children
}

// Rota do primeiro acesso: só para quem entrou com a senha temporária.
export function PrimeiroAcessoRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth()

  if (status === 'loading') return <FullPageLoading />
  if (status === 'anonymous') return <Navigate to="/login" replace />
  if (status === 'authenticated') return <Navigate to="/app" replace />
  return children
}
