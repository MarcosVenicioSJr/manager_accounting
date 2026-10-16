import { createContext } from 'react'
import type { TenantTheme } from './theme'

export type ThemeContextValue = {
  theme: TenantTheme
  setTheme: (theme: TenantTheme) => void
}

export const ThemeContext = createContext<ThemeContextValue | undefined>(undefined)
