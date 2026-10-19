import { useEffect, useState, type ReactNode } from 'react'
import { defaultTheme, themeCssVars, type TenantTheme } from './theme'
import { ThemeContext } from './ThemeContext'

// Guarda o tema do tenant carregado após o login e o aplica como variáveis CSS.
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<TenantTheme>(defaultTheme)

  // Na aba do navegador aparece o nome do escritório depois do login
  useEffect(() => {
    document.title = theme === defaultTheme ? defaultTheme.tenantName : `${theme.tenantName} · Backoffice`
  }, [theme])

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <div style={themeCssVars(theme)}>{children}</div>
    </ThemeContext.Provider>
  )
}
