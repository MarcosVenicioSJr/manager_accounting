import { createContext } from 'react'

export type ToastTone = 'success' | 'error'

export type ToastContextValue = {
  notify: (message: string, tone?: ToastTone) => void
}

export const ToastContext = createContext<ToastContextValue | undefined>(undefined)
