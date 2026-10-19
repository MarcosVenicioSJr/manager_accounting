import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { ChevronDownIcon, CloseIcon, HomeIcon, LogoutIcon, MenuIcon, UsersIcon } from '../components/icons'
import { useAuth } from '../features/auth/useAuth'
import { useMediaQuery } from '../hooks/useMediaQuery'
import { useTenantTheme } from '../hooks/useTenantTheme'
import { iniciais } from '../utils/texto'
import './backoffice.css'

const NAV = [
  { to: '/app', label: 'Início', icon: HomeIcon, end: true },
  { to: '/app/clientes', label: 'Clientes', icon: UsersIcon, end: false },
]

// Layout do backoffice: menu lateral com a marca do escritório, cabeçalho com o escritório e o contador,
// e a página atual no meio. No celular o menu vira uma gaveta aberta pelo botão do cabeçalho.
export function BackofficeLayout() {
  const { theme } = useTenantTheme()
  const [menuAberto, setMenuAberto] = useState(false)
  // Endereço da logo que não carregou (arquivo ausente): nesse caso aparece o monograma
  const [logoComErro, setLogoComErro] = useState<string | null>(null)
  const gaveta = useMediaQuery('(max-width: 959px)')
  const fecharRef = useRef<HTMLButtonElement>(null)
  const abrirRef = useRef<HTMLButtonElement>(null)
  const estavaAberto = useRef(false)

  // Foco vai para dentro da gaveta ao abrir e volta para o botão do menu ao fechar
  useEffect(() => {
    if (menuAberto) fecharRef.current?.focus()
    else if (estavaAberto.current) abrirRef.current?.focus()
    estavaAberto.current = menuAberto
  }, [menuAberto])

  useEffect(() => {
    if (!menuAberto) return
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuAberto(false)
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [menuAberto])

  const mostrarLogo = Boolean(theme.logoUrl) && theme.logoUrl !== logoComErro

  return (
    <div className="bo" data-menu-aberto={menuAberto}>
      <a href="#conteudo" className="bo-skip">
        Pular para o conteúdo
      </a>

      <aside
        className="bo-sidebar"
        aria-label="Menu principal"
        // Gaveta fechada no celular: fora da tela e fora da navegação por teclado
        inert={gaveta && !menuAberto}
      >
        <div className="bo-brand">
          {mostrarLogo ? (
            <img
              className="bo-brand__logo"
              src={theme.logoUrl ?? undefined}
              alt={theme.tenantName}
              onError={() => setLogoComErro(theme.logoUrl)}
            />
          ) : (
            <span className="bo-brand__monogram" aria-hidden="true">
              {iniciais(theme.tenantName)}
            </span>
          )}
          {!mostrarLogo && <span className="bo-brand__name">{theme.tenantName}</span>}
          <button
            ref={fecharRef}
            type="button"
            className="ui-icon-btn bo-sidebar__close"
            onClick={() => setMenuAberto(false)}
            aria-label="Fechar menu"
          >
            <CloseIcon />
          </button>
        </div>

        <nav className="bo-nav">
          {NAV.map(({ to, label, icon: ItemIcon, end }) => (
            // Escolher um item fecha a gaveta no celular
            <NavLink key={to} to={to} end={end} className="bo-nav__item" onClick={() => setMenuAberto(false)}>
              <ItemIcon />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <p className="bo-sidebar__foot">Backoffice contábil</p>
      </aside>

      <button
        type="button"
        className="bo-overlay"
        tabIndex={-1}
        aria-hidden="true"
        onClick={() => setMenuAberto(false)}
      />

      <div className="bo-body">
        <header className="bo-header">
          <button
            ref={abrirRef}
            type="button"
            className="ui-icon-btn bo-header__menu"
            onClick={() => setMenuAberto(true)}
            aria-label="Abrir menu"
            aria-expanded={menuAberto}
          >
            <MenuIcon />
          </button>
          <div className="bo-header__office">
            <span className="bo-header__office-label">Escritório</span>
            <span className="bo-header__office-name">{theme.tenantName}</span>
          </div>
          <UserMenu />
        </header>

        <main id="conteudo" className="bo-main" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}

function UserMenu() {
  const { usuario, logout } = useAuth()
  const [aberto, setAberto] = useState(false)
  const [saindo, setSaindo] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!aberto) return
    const onClick = (event: MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setAberto(false)
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setAberto(false)
    }
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [aberto])

  const nome = usuario?.nomeCompleto || usuario?.login || 'Contador'
  const primeiroNome = nome.split(/\s+/)[0]

  async function sair() {
    setSaindo(true)
    try {
      await logout()
    } finally {
      setSaindo(false)
    }
  }

  return (
    <div className="bo-user" ref={ref}>
      <button
        type="button"
        className="bo-user__button"
        aria-haspopup="menu"
        aria-expanded={aberto}
        onClick={() => setAberto((v) => !v)}
      >
        <span className="bo-avatar" aria-hidden="true">
          {iniciais(nome)}
        </span>
        <span className="bo-user__name">{primeiroNome}</span>
        <ChevronDownIcon width={16} height={16} />
      </button>
      {aberto && (
        <div className="bo-user__menu" role="menu">
          <div className="bo-user__info">
            <strong>{nome}</strong>
            {usuario?.email && <span>{usuario.email}</span>}
            {usuario?.login && <span className="bo-user__login">Usuário: {usuario.login}</span>}
          </div>
          <button type="button" role="menuitem" className="bo-user__item" onClick={() => void sair()} disabled={saindo}>
            <LogoutIcon width={18} height={18} />
            {saindo ? 'Saindo…' : 'Sair'}
          </button>
        </div>
      )}
    </div>
  )
}
