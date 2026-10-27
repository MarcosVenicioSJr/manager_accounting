import { createBrowserRouter, Navigate } from 'react-router-dom'
import { PrimeiroAcessoRoute, ProtectedRoute } from '../features/auth/ProtectedRoute'
import { BackofficeLayout } from '../layouts/BackofficeLayout'
import { AceitarConvitePage } from '../pages/aceitar-convite/AceitarConvitePage'
import { ClienteDetalhePage } from '../pages/clientes/ClienteDetalhePage'
import { ConfirmarAlteracaoPage } from '../pages/confirmar-alteracao/ConfirmarAlteracaoPage'
import { ClientesPage } from '../pages/clientes/ClientesPage'
import { NovoClientePage } from '../pages/clientes/NovoClientePage'
import { InicioPage } from '../pages/inicio/InicioPage'
import { LancamentosPage } from '../pages/lancamentos/LancamentosPage'
import { LoginPage } from '../pages/login/LoginPage'
import { NaoEncontradoPage } from '../pages/nao-encontrado/NaoEncontradoPage'
import { PerfilPage } from '../pages/perfil/PerfilPage'
import { PrimeiroAcessoPage } from '../pages/primeiro-acesso/PrimeiroAcessoPage'
import { RecuperarSenhaPage } from '../pages/recuperar-senha/RecuperarSenhaPage'
import { RedefinirSenhaPage } from '../pages/redefinir-senha/RedefinirSenhaPage'
import { UsuariosPage } from '../pages/usuarios/UsuariosPage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/recuperar-senha', element: <RecuperarSenhaPage /> },
  // Aberta pelo link do e-mail (/redefinir-senha#token=...)
  { path: '/redefinir-senha', element: <RedefinirSenhaPage /> },
  // Aberta pelo link do e-mail do Meu perfil (/confirmar-alteracao#token=...); funciona logado ou não
  { path: '/confirmar-alteracao', element: <ConfirmarAlteracaoPage /> },
  // Aberta pelo link do convite de usuário (/aceitar-convite#token=...)
  { path: '/aceitar-convite', element: <AceitarConvitePage /> },
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
      { path: 'lancamentos', element: <LancamentosPage /> },
      { path: 'perfil', element: <PerfilPage /> },
      { path: 'usuarios', element: <UsuariosPage /> },
      { path: '*', element: <NaoEncontradoPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/app" replace /> },
])
