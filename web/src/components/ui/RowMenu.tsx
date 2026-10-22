import { useEffect, useId, useRef, useState, type ReactNode } from 'react'

export type RowMenuItem = {
  label: string
  onSelect: () => void
  tone?: 'danger'
  icon?: ReactNode
}

// Menu de ações de uma linha (botão "⋯"). Fecha com Esc, clique fora ou ao escolher; setas navegam entre itens.
export function RowMenu({ label, items }: { label: string; items: RowMenuItem[] }) {
  const [aberto, setAberto] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const botaoRef = useRef<HTMLButtonElement>(null)
  const menuId = useId()

  useEffect(() => {
    if (!aberto) return
    ref.current?.querySelector<HTMLButtonElement>('[role="menuitem"]')?.focus()
    const onClick = (event: MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setAberto(false)
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setAberto(false)
        botaoRef.current?.focus()
      }
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault()
        const itens = Array.from(ref.current?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]') ?? [])
        const atual = itens.indexOf(document.activeElement as HTMLButtonElement)
        const proximo = event.key === 'ArrowDown' ? (atual + 1) % itens.length : (atual - 1 + itens.length) % itens.length
        itens[proximo]?.focus()
      }
    }
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [aberto])

  if (items.length === 0) return null

  return (
    <div className="ui-rowmenu" ref={ref}>
      <button
        ref={botaoRef}
        type="button"
        className="ui-icon-btn ui-rowmenu__button"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={aberto}
        aria-controls={aberto ? menuId : undefined}
        onClick={() => setAberto((v) => !v)}
      >
        <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="5" cy="12" r="1.8" />
          <circle cx="12" cy="12" r="1.8" />
          <circle cx="19" cy="12" r="1.8" />
        </svg>
      </button>
      {aberto && (
        <div className="ui-rowmenu__list" role="menu" id={menuId}>
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={`ui-rowmenu__item${item.tone === 'danger' ? ' ui-rowmenu__item--danger' : ''}`}
              onClick={() => {
                setAberto(false)
                item.onSelect()
              }}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
