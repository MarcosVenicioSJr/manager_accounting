import { createBrowserRouter, Navigate } from 'react-router-dom'
import { LoginPage } from '../pages/login/LoginPage'
import { HomePage } from '../pages/home/HomePage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage /> },
  // Rotas autenticadas (backoffice com o tema do tenant) entram aqui
  { path: '/app', element: <HomePage /> },
])
