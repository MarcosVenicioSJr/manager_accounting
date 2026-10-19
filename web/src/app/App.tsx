import { RouterProvider } from 'react-router-dom'
import { AuthProvider } from '../features/auth/AuthProvider'
import { router } from '../routes/router'
import { ThemeProvider } from '../theme/ThemeProvider'

export function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ThemeProvider>
  )
}
