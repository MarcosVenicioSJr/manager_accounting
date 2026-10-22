import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  CloseIcon,
  HomeIcon,
  LogoutIcon,
  MenuIcon,
  ShieldIcon,
  UsersIcon,
} from '../components/icons'
import { useAuth } from '../features/auth/useAuth'
import { useMediaQuery } from '../hooks/useMediaQuery'
import { useTenantTheme } from '../hooks/useTenantTheme'
import { iniciais } from '../utils/texto'
import { HeaderSlotContext } from './headerSlot'
import './backoffice.css'

const NAV = [
  { to: '/app', label: 'Visão geral', icon: HomeIcon, end: true, soAdmin: false },
  { to: '/app/clientes', label: 'Clientes', icon: UsersIcon, end: false, soAdmin: false },
  { to: '/app/usuarios', label: 'Usuários', icon: ShieldIcon, end: false, soAdmin: true },
]

const CHAVE_RECOLHIDO = 'bo.menuRecolhido'

// Preferência de menu recolhido, só deste navegador. Se o armazenamento falhar, o menu abre expandido.
function lerRecolhido(): boolean {
  try {
    return window.localStorage.getItem(CHAVE_RECOLHIDO) === '1'
  } catch {
    return false
  }
}

function salvarRecolhido(valor: boolean) {
  try {
    window.localStorage.setItem(CHAVE_RECOLHIDO, valor ? '1' : '0')
  } catch {
    // Sem armazenamento: vale só até recarregar
  }
}

// Layout do backoffice. Menu lateral: marca do escritório no topo, navegação, contador e "Sair" no rodapé,
// e o botão de recolher. Cabeçalho fixo com o título e as ações da página (cada página usa <PageHeader>).
// No celular e no tablet o menu vira uma gaveta aberta pelo botão do cabeçalho (e não recolhe).
export function BackofficeLayout() {
  const { theme } = useTenantTheme()
  const { usuario, logout } = useAuth()
  const [menuAberto, setMenuAberto] = useState(false)
  const [recolhidoPreferido, setRecolhidoPreferido] = useState(lerRecolhido)
  const [saindo, setSaindo] = useState(false)
  // Endereço da logo que não carregou (arquivo ausente): nesse caso aparece o monograma
  const [logoComErro, setLogoComErro] = useState<string | null>(null)
  const [slot, setSlot] = useState<HTMLDivElement | null>(null)
  const gaveta = useMediaQuery('(max-width: 959px)')
  const fecharRef = useRef<HTMLButtonElement>(null)
  const abrirRef = useRef<HTMLButtonElement>(null)
  const estavaAberto = useRef(false)

  const recolhido = recolhidoPreferido && !gaveta

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

  function alternarRecolhido() {
    setRecolhidoPreferido((atual) => {
      salvarRecolhido(!atual)
      return !atual
    })
  }

  async function sair() {
    setSaindo(true)
    try {
      await logout()
    } finally {
      setSaindo(false)
    }
  }

  const mostrarLogo = Boolean(theme.logoUrl) && theme.logoUrl !== logoComErro
  const nomeUsuario = usuario?.nomeCompleto || usuario?.login || 'Contador'

  return (
    <div className="bo" data-menu-aberto={menuAberto} data-recolhido={recolhido}>
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
              alt=""
              onError={() => setLogoComErro(theme.logoUrl)}
            />
          ) : (
            <span className="bo-brand__monogram" aria-hidden="true">
              {iniciais(theme.tenantName)}
            </span>
          )}
          <div className="bo-brand__text">
            <span className="bo-brand__name" title={theme.tenantName}>
              {theme.tenantName}
            </span>
            <span className="bo-brand__tagline">Gestão de clientes</span>
          </div>
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
          {NAV.filter((item) => !item.soAdmin || usuario?.perfil === 'ADMIN').map(({ to, label, icon: ItemIcon, end }) => (
            // Escolher um item fecha a gaveta no celular. Recolhido, o nome vira dica e rótulo acessível.
            <NavLink
              key={to}
              to={to}
              end={end}
              className="bo-nav__item"
              onClick={() => setMenuAberto(false)}
              title={recolhido ? label : undefined}
              aria-label={recolhido ? label : undefined}
            >
              <ItemIcon />
              <span className="bo-nav__label">{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="bo-account">
          <NavLink
            to="/app/perfil"
            className="bo-account__user"
            onClick={() => setMenuAberto(false)}
            title={recolhido ? `${nomeUsuario} · Meu perfil` : 'Meu perfil'}
            aria-label={`Meu perfil: ${nomeUsuario}`}
          >
            <span className="bo-avatar" aria-hidden="true">
              {iniciais(nomeUsuario)}
            </span>
            <div className="bo-account__text">
              <span className="bo-account__name">{nomeUsuario}</span>
              <span className="bo-account__detail">Meu perfil</span>
            </div>
          </NavLink>
          <button
            type="button"
            className="bo-account__logout"
            onClick={() => void sair()}
            disabled={saindo}
            title={recolhido ? 'Sair' : undefined}
            aria-label={recolhido ? 'Sair' : undefined}
          >
            <LogoutIcon width={16} height={16} />
            <span className="bo-nav__label">{saindo ? 'Saindo…' : 'Sair'}</span>
          </button>
        </div>

        <div className="bo-collapse">
          <button
            type="button"
            className="bo-collapse__button"
            onClick={alternarRecolhido}
            aria-label={recolhido ? 'Expandir menu' : 'Recolher menu'}
            aria-pressed={recolhido}
            title={recolhido ? 'Expandir menu' : 'Recolher menu'}
          >
            {recolhido ? <ChevronRightIcon width={16} height={16} /> : <ChevronLeftIcon width={16} height={16} />}
          </button>
        </div>
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
          {/* Título e ações da página entram aqui (PageHeader) */}
          <div className="bo-header__slot" ref={setSlot} />
        </header>

        <main id="conteudo" className="bo-main" tabIndex={-1}>
          <HeaderSlotContext.Provider value={slot}>
            <Outlet />
          </HeaderSlotContext.Provider>
        </main>
      </div>
    </div>
  )
}
