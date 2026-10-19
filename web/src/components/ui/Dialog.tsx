import { useEffect, useId, useRef, type ReactNode } from 'react'

type DialogProps = {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  actions?: ReactNode
  // Enquanto uma ação está em andamento o diálogo não fecha pelo Esc nem pelo fundo
  busy?: boolean
  // false: clicar fora não fecha (para conteúdo que não pode se perder por engano, como um token)
  fecharPeloFundo?: boolean
}

// Diálogo modal com o <dialog> nativo: prende o foco, fecha com Esc e devolve o foco a quem abriu.
export function Dialog({
  open,
  title,
  onClose,
  children,
  actions,
  busy = false,
  fecharPeloFundo = true,
}: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      className="ui-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault()
        if (!busy) onClose()
      }}
      onClick={(event) => {
        // Clique no fundo escurecido (fora da caixa) fecha
        if (event.target === ref.current && !busy && fecharPeloFundo) onClose()
      }}
    >
      <div className="ui-dialog__box">
        <h2 id={titleId} className="ui-dialog__title">
          {title}
        </h2>
        <div className="ui-dialog__body">{children}</div>
        {actions && <div className="ui-dialog__actions">{actions}</div>}
      </div>
    </dialog>
  )
}

type ConfirmDialogProps = {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  tone?: 'danger' | 'primary'
  busy?: boolean
  onConfirm: () => void
  onClose: () => void
}

// Confirmação de ação com consequência (desativar, revogar). O botão de cancelar vem primeiro e com o foco.
export function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  tone = 'primary',
  busy = false,
  onConfirm,
  onClose,
}: ConfirmDialogProps) {
  return (
    <Dialog
      open={open}
      title={title}
      onClose={onClose}
      busy={busy}
      actions={
        <>
          <button type="button" className="ui-btn ui-btn--secondary" onClick={onClose} disabled={busy} autoFocus>
            Cancelar
          </button>
          <button
            type="button"
            className={`ui-btn ${tone === 'danger' ? 'ui-btn--danger' : 'ui-btn--primary'}`}
            onClick={onConfirm}
            disabled={busy}
            aria-busy={busy}
          >
            {busy ? 'Aguarde…' : confirmLabel}
          </button>
        </>
      }
    >
      {children}
    </Dialog>
  )
}
