import { RouterProvider } from 'react-router-dom'
import { ToastProvider } from '../components/ui/ToastProvider'
import { AuthProvider } from '../features/auth/AuthProvider'
import { router } from '../routes/router'
import { ThemeProvider } from '../theme/ThemeProvider'

export function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </AuthProvider>
    </ThemeProvider>
  )
}
