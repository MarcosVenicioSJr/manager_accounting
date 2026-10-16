import { useState, type CSSProperties, type ReactNode } from 'react'
import { defaultTheme, type TenantTheme } from './theme'
import { ThemeContext } from './ThemeContext'

// Guarda o tema do tenant carregado após o login e o aplica como variáveis CSS.
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<TenantTheme>(defaultTheme)

  const style = {
    '--color-primary': theme.primaryColor,
    '--color-secondary': theme.secondaryColor,
  } as CSSProperties

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <div style={style}>{children}</div>
    </ThemeContext.Provider>
  )
}
