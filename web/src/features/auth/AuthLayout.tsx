import type { ReactNode } from 'react'
import './auth.css'

type AuthLayoutProps = {
  title: string
  subtitle?: string
  children: ReactNode
}

// Layout aprovado das telas de acesso: foto de escritório de contabilidade à esquerda e formulário à direita.
// A foto é o arquivo web/public/images/login-hero.jpg (sem ele, aparece só o degradê).
export function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  return (
    <div className="auth-split">
      <aside className="auth-hero" aria-hidden="true">
        <span className="auth-hero__brand">Manager Accounting</span>
      </aside>
      <main className="auth-panel">
        <div className="auth-card">
          <h1 className="auth-card__title">{title}</h1>
          {subtitle && <p className="auth-card__subtitle">{subtitle}</p>}
          {children}
        </div>
      </main>
    </div>
  )
}
