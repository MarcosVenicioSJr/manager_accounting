import type { ReactNode } from 'react'
import { BarsLogo, BoltIcon, LockIcon, ShieldCheckIcon, TrendIcon } from './icons'
import './auth.css'

// Layout aprovado das telas de acesso (login, primeiro acesso, recuperação de senha):
// painel azul-marinho com a foto do escritório à esquerda e o formulário à direita.
// Genérico de propósito: sem logo nem cor de escritório (o tema do contador só carrega após o login).
// A foto é web/public/images/login-hero.jpg.
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth-split">
      <aside className="auth-hero">
        <div className="auth-hero__content">
          <div className="auth-brand">
            <BarsLogo />
            <span className="auth-brand__name">Backoffice contábil</span>
          </div>

          <h2 className="auth-hero__title">
            Mais controle para o <span className="auth-hero__highlight">seu negócio.</span>
          </h2>
          <p className="auth-hero__text">
            Organização, segurança e praticidade em todos os processos contábeis.
          </p>

          <ul className="auth-hero__list">
            <li>
              <ShieldCheckIcon />
              Dados protegidos
            </li>
            <li>
              <BoltIcon />
              Processos mais ágeis
            </li>
            <li>
              <TrendIcon />
              Informações sempre atualizadas
            </li>
          </ul>
        </div>
      </aside>

      <main className="auth-main">
        <div className="auth-compact-brand">
          <BarsLogo width={30} height={32} />
          <span>Backoffice contábil</span>
        </div>

        <div className="auth-main__center">
          <div className="auth-card">{children}</div>
        </div>

        <footer className="auth-footer">
          <LockIcon width={16} height={16} />
          Acesso restrito. Somente usuários autorizados.
        </footer>
      </main>
    </div>
  )
}
