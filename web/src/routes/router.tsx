import { createBrowserRouter, Navigate } from 'react-router-dom'
import { PrimeiroAcessoRoute, ProtectedRoute } from '../features/auth/ProtectedRoute'
import { BackofficeLayout } from '../layouts/BackofficeLayout'
import { ClienteDetalhePage } from '../pages/clientes/ClienteDetalhePage'
import { ClientesPage } from '../pages/clientes/ClientesPage'
import { NovoClientePage } from '../pages/clientes/NovoClientePage'
import { InicioPage } from '../pages/inicio/InicioPage'
import { LoginPage } from '../pages/login/LoginPage'
import { NaoEncontradoPage } from '../pages/nao-encontrado/NaoEncontradoPage'
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
  // Backoffice: exige sessão completa e usa o layout com o tema do escritório
  {
    path: '/app',
    element: (
      <ProtectedRoute>
        <BackofficeLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <InicioPage /> },
      { path: 'clientes', element: <ClientesPage /> },
      { path: 'clientes/novo', element: <NovoClientePage /> },
      { path: 'clientes/:id', element: <ClienteDetalhePage /> },
      { path: '*', element: <NaoEncontradoPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/app" replace /> },
])
