import { createBrowserRouter, Navigate } from 'react-router-dom'
import { PrimeiroAcessoRoute, ProtectedRoute } from '../features/auth/ProtectedRoute'
import { HomePage } from '../pages/home/HomePage'
import { LoginPage } from '../pages/login/LoginPage'
import { PrimeiroAcessoPage } from '../pages/primeiro-acesso/PrimeiroAcessoPage'
import { RecuperarSenhaPage } from '../pages/recuperar-senha/RecuperarSenhaPage'
import { RedefinirSenhaPage } from '../pages/redefinir-senha/RedefinirSenhaPage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/recuperar-senha', element: <RecuperarSenhaPage /> },
  // Aberta pelo link do e-mail (/redefinir-senha#token=...)
  { path: '/redefinir-senha', element: <RedefinirSenhaPage /> },
  {
    path: '/primeiro-acesso',
    element: (
      <PrimeiroAcessoRoute>
        <PrimeiroAcessoPage />
      </PrimeiroAcessoRoute>
    ),
  },
  // Rotas autenticadas (backoffice com o tema do tenant) entram aqui
  {
    path: '/app',
    element: (
      <ProtectedRoute>
        <HomePage />
      </ProtectedRoute>
    ),
  },
])
