import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { AlertIcon, CheckIcon, CloseIcon } from '../icons'
import { ToastContext, type ToastTone } from './ToastContext'

type Toast = { id: number; message: string; tone: ToastTone }

const DURACAO_MS = 5000

// Avisos rápidos de resultado ("Cliente cadastrado"). Ficam numa região aria-live, lida pelo leitor de tela.
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(1)

  const dismiss = useCallback((id: number) => {
    setToasts((atual) => atual.filter((toast) => toast.id !== id))
  }, [])

  const notify = useCallback(
    (message: string, tone: ToastTone = 'success') => {
      const id = nextId.current++
      setToasts((atual) => [...atual.slice(-2), { id, message, tone }])
      window.setTimeout(() => dismiss(id), DURACAO_MS)
    },
    [dismiss],
  )

  const value = useMemo(() => ({ notify }), [notify])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="ui-toasts" role="status" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id} className={`ui-toast ui-toast--${toast.tone}`}>
            {toast.tone === 'success' ? <CheckIcon /> : <AlertIcon />}
            <span>{toast.message}</span>
            <button type="button" className="ui-toast__close" onClick={() => dismiss(toast.id)} aria-label="Fechar aviso">
              <CloseIcon width={16} height={16} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
