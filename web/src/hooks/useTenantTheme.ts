import { useContext } from 'react'
import { ThemeContext } from '../theme/ThemeContext'

export function useTenantTheme() {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTenantTheme deve ser usado dentro de ThemeProvider')
  return context
}
