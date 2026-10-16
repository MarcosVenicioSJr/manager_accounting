import { useAuth } from '../../features/auth/useAuth'
import { useTenantTheme } from '../../hooks/useTenantTheme'

// Página inicial do backoffice, exibida após o login com o tema (nome, logo e cores) do tenant.
export function HomePage() {
  const { theme } = useTenantTheme()
  const { logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="app-header">
        {theme.logoUrl ? (
          <img className="app-header__logo" src={theme.logoUrl} alt={theme.tenantName} />
        ) : (
          <span className="app-header__name">{theme.tenantName}</span>
        )}
        <button type="button" className="btn btn--ghost" onClick={() => void logout()}>
          Sair
        </button>
      </header>
      <main className="app-main">
        <h1>Backoffice</h1>
      </main>
    </div>
  )
}
