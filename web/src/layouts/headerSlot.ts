import { createContext } from 'react'

// Elemento do cabeçalho do backoffice onde cada página coloca o título e as ações (ver PageHeader).
export const HeaderSlotContext = createContext<HTMLElement | null>(null)
