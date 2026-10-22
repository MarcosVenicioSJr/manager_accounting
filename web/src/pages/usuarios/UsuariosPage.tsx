import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { AlertIcon, PlusIcon, UsersIcon } from '../../components/icons'
import { ConfirmDialog, Dialog } from '../../components/ui/Dialog'
import { RowMenu, type RowMenuItem } from '../../components/ui/RowMenu'
import { useToast } from '../../components/ui/useToast'
import { useAuth } from '../../features/auth/useAuth'
import '../../features/clientes/clientes.css'
import { formatarDataHora, pluralizar } from '../../features/clientes/formatar'
import {
  alterarPerfilUsuario,
  cancelarConvite,
  convidarUsuario,
  desativarUsuario,
  listarUsuarios,
  reativarUsuario,
  reenviarConvite,
  type ConviteDados,
  type UsuarioEquipe,
} from '../../features/equipe/equipeService'
import type { PerfilAcesso } from '../../features/usuario/usuarioService'
import { PageHeader } from '../../layouts/PageHeader'
import { getErrorCode, getErrorMessage } from '../../services/errors'
import { NaoEncontradoPage } from '../nao-encontrado/NaoEncontradoPage'
import './usuarios.css'

const ROTULO_PERFIL: Record<PerfilAcesso, string> = { ADMIN: 'Administrador', USUARIO: 'Usuário' }

type Acao =
  | { tipo: 'perfil'; usuario: UsuarioEquipe; perfil: PerfilAcesso }
  | { tipo: 'desativar' | 'reativar' | 'cancelar'; usuario: UsuarioEquipe }

// Usuários do escritório: só para o administrador (o backend também barra os demais).
export function UsuariosPage() {
  const { usuario } = useAuth()
  if (usuario && usuario.perfil !== 'ADMIN') {
    return (
      <NaoEncontradoPage
        titulo="Acesso só para administradores"
        texto="Peça a um administrador do escritório para incluir ou alterar usuários."
      />
    )
  }
  return <Usuarios />
}

function Usuarios() {
  const { notify } = useToast()
  const [usuarios, setUsuarios] = useState<UsuarioEquipe[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [convidando, setConvidando] = useState(false)
  const [acao, setAcao] = useState<Acao | null>(null)
  const [executando, setExecutando] = useState(false)

  const carregar = useCallback(async () => {
    try {
      setUsuarios(await listarUsuarios())
      setErro(null)
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível carregar os usuários.'))
    }
  }, [])

  useEffect(() => {
    let cancelado = false
    listarUsuarios()
      .then((dados) => !cancelado && setUsuarios(dados))
      .catch((error: unknown) => !cancelado && setErro(getErrorMessage(error, 'Não foi possível carregar os usuários.')))
    return () => {
      cancelado = true
    }
  }, [])

  async function reenviar(u: UsuarioEquipe) {
    try {
      await reenviarConvite(u.id)
      notify(`Convite reenviado para ${u.email}.`)
      await carregar()
    } catch (error) {
      notify(getErrorMessage(error, 'Não foi possível reenviar o convite.'), 'error')
    }
  }

  async function executar() {
    if (!acao) return
    setExecutando(true)
    const { usuario: u } = acao
    try {
      if (acao.tipo === 'perfil') {
        await alterarPerfilUsuario(u.id, acao.perfil)
        notify(`${nomeDe(u)} agora é ${ROTULO_PERFIL[acao.perfil].toLowerCase()}.`)
      } else if (acao.tipo === 'desativar') {
        await desativarUsuario(u.id)
        notify(`${nomeDe(u)} foi desativado.`)
      } else if (acao.tipo === 'reativar') {
        await reativarUsuario(u.id)
        notify(`${nomeDe(u)} foi reativado.`)
      } else {
        await cancelarConvite(u.id)
        notify('Convite cancelado.')
      }
      setAcao(null)
      await carregar()
    } catch (error) {
      setAcao(null)
      notify(getErrorMessage(error, 'Não foi possível concluir a ação.'), 'error')
    } finally {
      setExecutando(false)
    }
  }

  function itensDoMenu(u: UsuarioEquipe): RowMenuItem[] {
    if (u.voce) return []
    const itens: RowMenuItem[] = []
    if (u.situacao === 'PENDENTE') {
      itens.push({ label: 'Reenviar convite', onSelect: () => void reenviar(u) })
    }
    if (u.situacao !== 'INATIVO') {
      const outro: PerfilAcesso = u.perfil === 'ADMIN' ? 'USUARIO' : 'ADMIN'
      itens.push({
        label: outro === 'ADMIN' ? 'Tornar administrador' : 'Tornar usuário comum',
        onSelect: () => setAcao({ tipo: 'perfil', usuario: u, perfil: outro }),
      })
    }
    if (u.situacao === 'PENDENTE') {
      itens.push({ label: 'Cancelar convite', tone: 'danger', onSelect: () => setAcao({ tipo: 'cancelar', usuario: u }) })
    }
    if (u.situacao === 'ATIVO') {
      itens.push({ label: 'Desativar acesso', tone: 'danger', onSelect: () => setAcao({ tipo: 'desativar', usuario: u }) })
    }
    if (u.situacao === 'INATIVO') {
      itens.push({ label: 'Reativar acesso', onSelect: () => setAcao({ tipo: 'reativar', usuario: u }) })
    }
    return itens
  }

  const ativos = usuarios?.filter((u) => u.situacao !== 'INATIVO').length ?? 0

  return (
    <>
      <PageHeader
        titulo="Usuários"
        subtitulo={usuarios ? `${pluralizar(ativos, 'usuário com acesso', 'usuários com acesso')} no escritório` : 'Carregando…'}
        acoes={
          <button type="button" className="ui-btn ui-btn--primary" onClick={() => setConvidando(true)}>
            <PlusIcon width={18} height={18} />
            Convidar usuário
          </button>
        }
      />

      <p className="us-intro">
        <strong>Administradores</strong> fazem tudo, inclusive gerenciar usuários, desativar clientes e revogar o
        acesso ao app. <strong>Usuários</strong> cadastram e editam clientes e geram o token do app.
      </p>

      {erro && !usuarios ? (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">
            {erro}
            <div className="ui-alert__actions">
              <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={() => void carregar()}>
                Tentar novamente
              </button>
            </div>
          </div>
        </div>
      ) : !usuarios ? (
        <div className="panel cl-loading" role="status" aria-label="Carregando usuários">
          {Array.from({ length: 3 }, (_, i) => (
            <div key={i} className="cl-loading__row">
              <span className="ui-skeleton" style={{ width: '34%' }} />
              <span className="ui-skeleton" style={{ width: '16%' }} />
              <span className="ui-skeleton" style={{ width: '22%' }} />
            </div>
          ))}
        </div>
      ) : (
        <div className="panel cl-table-wrap us-table-wrap">
          <table className="cl-table us-table">
            <caption className="sr-only">Usuários do escritório</caption>
            <thead>
              <tr>
                <th scope="col">Usuário</th>
                <th scope="col">Login</th>
                <th scope="col">Perfil</th>
                <th scope="col">Situação</th>
                <th scope="col">
                  <span className="sr-only">Ações</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id} data-inativo={u.situacao === 'INATIVO'} className="us-row">
                  <td data-label="Usuário">
                    <span className="cl-name us-name">
                      {nomeDe(u)}
                      {u.voce && <span className="badge us-voce">Você</span>}
                    </span>
                    <span className="cl-sub">{u.email ?? 'Sem e-mail'}</span>
                  </td>
                  <td data-label="Login">
                    <span className="cl-mono">{u.login}</span>
                  </td>
                  <td data-label="Perfil">
                    <span className={`badge${u.perfil === 'ADMIN' ? ' us-badge-admin' : ''}`}>{ROTULO_PERFIL[u.perfil]}</span>
                  </td>
                  <td data-label="Situação">
                    <Situacao u={u} />
                  </td>
                  <td className="us-actions">
                    <RowMenu label={`Ações para ${nomeDe(u)}`} items={itensDoMenu(u)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {usuarios.length === 1 && (
            <div className="us-empty">
              <span className="cl-empty__icon" aria-hidden="true">
                <UsersIcon width={24} height={24} />
              </span>
              <p>Por enquanto só você acessa o backoffice. Convide quem trabalha com você no escritório.</p>
            </div>
          )}
        </div>
      )}

      <ConviteDialog
        open={convidando}
        onClose={() => setConvidando(false)}
        onConvidado={(novo) => {
          setConvidando(false)
          notify(`Convite enviado para ${novo.email}.`)
          void carregar()
        }}
      />

      <ConfirmDialog
        open={acao !== null}
        title={tituloAcao(acao)}
        confirmLabel={rotuloAcao(acao)}
        tone={acao?.tipo === 'desativar' || acao?.tipo === 'cancelar' ? 'danger' : 'primary'}
        busy={executando}
        onConfirm={() => void executar()}
        onClose={() => setAcao(null)}
      >
        {acao && <p>{textoAcao(acao)}</p>}
      </ConfirmDialog>
    </>
  )
}

function Situacao({ u }: { u: UsuarioEquipe }) {
  if (u.situacao === 'ATIVO') return <span className="badge badge--ok">Ativo</span>
  if (u.situacao === 'INATIVO') return <span className="badge badge--off">Inativo</span>
  return (
    <span className="us-pending">
      <span className="badge us-badge-pending">{u.conviteExpiraEm ? 'Convite enviado' : 'Convite expirado'}</span>
      <span className="cl-sub">
        {u.conviteExpiraEm ? `Vale até ${formatarDataHora(u.conviteExpiraEm)}` : 'Reenvie o convite'}
      </span>
    </span>
  )
}

function nomeDe(u: UsuarioEquipe) {
  return u.nomeCompleto || u.login
}

function tituloAcao(acao: Acao | null) {
  if (!acao) return ''
  if (acao.tipo === 'perfil') return acao.perfil === 'ADMIN' ? 'Tornar administrador?' : 'Tornar usuário comum?'
  if (acao.tipo === 'desativar') return 'Desativar este usuário?'
  if (acao.tipo === 'reativar') return 'Reativar este usuário?'
  return 'Cancelar o convite?'
}

function rotuloAcao(acao: Acao | null) {
  if (!acao) return ''
  if (acao.tipo === 'perfil') return acao.perfil === 'ADMIN' ? 'Tornar administrador' : 'Tornar usuário comum'
  if (acao.tipo === 'desativar') return 'Desativar acesso'
  if (acao.tipo === 'reativar') return 'Reativar acesso'
  return 'Cancelar convite'
}

function textoAcao(acao: Acao) {
  const nome = nomeDe(acao.usuario)
  if (acao.tipo === 'perfil') {
    return acao.perfil === 'ADMIN'
      ? `${nome} passa a gerenciar usuários, desativar clientes e revogar o acesso ao app.`
      : `${nome} deixa de gerenciar usuários e de desativar clientes. Continua cadastrando e editando clientes.`
  }
  if (acao.tipo === 'desativar') {
    return `${nome} perde o acesso na hora: as sessões abertas são encerradas. Os clientes e dados do escritório não mudam.`
  }
  if (acao.tipo === 'reativar') {
    return `${nome} volta a ter acesso com a mesma senha. Se ainda não tinha criado a senha, reenvie o convite em seguida.`
  }
  return `O link enviado para ${acao.usuario.email} deixa de funcionar e o login "${acao.usuario.login}" fica livre para outro convite.`
}

// ---------- Convite ----------

const LOGIN = /^[a-z0-9][a-z0-9._-]{2,59}$/
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// Sugere o login a partir do nome: "Ana Paula Lima" → "ana.lima"
function sugerirLogin(nome: string) {
  const partes = nome
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, '')
    .split(/\s+/)
    .filter((p) => p && !['de', 'da', 'do', 'das', 'dos', 'e'].includes(p))
  if (partes.length === 0) return ''
  return (partes.length > 1 ? `${partes[0]}.${partes[partes.length - 1]}` : partes[0]).slice(0, 60)
}

function ConviteDialog({
  open,
  onClose,
  onConvidado,
}: {
  open: boolean
  onClose: () => void
  onConvidado: (u: UsuarioEquipe) => void
}) {
  const vazio: ConviteDados = { nomeCompleto: '', email: '', login: '', perfil: 'USUARIO' }
  const [dados, setDados] = useState<ConviteDados>(vazio)
  const [loginEditado, setLoginEditado] = useState(false)
  const [tentou, setTentou] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [erroLogin, setErroLogin] = useState<string | null>(null)

  function fechar() {
    setDados(vazio)
    setLoginEditado(false)
    setTentou(false)
    setErro(null)
    setErroLogin(null)
    onClose()
  }

  const login = dados.login.trim().toLowerCase()
  const erros = {
    nomeCompleto: !dados.nomeCompleto.trim() ? 'Informe o nome completo.' : undefined,
    email: !dados.email.trim() ? 'Informe o e-mail.' : !EMAIL.test(dados.email.trim()) ? 'E-mail inválido.' : undefined,
    login:
      erroLogin ??
      (!login
        ? 'Informe o usuário de acesso.'
        : !LOGIN.test(login)
          ? 'Use de 3 a 60 letras minúsculas, números, ponto, hífen ou sublinhado.'
          : undefined),
  }

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTentou(true)
    setErro(null)
    if (erros.nomeCompleto || erros.email || erros.login) return
    setEnviando(true)
    try {
      const novo = await convidarUsuario({ ...dados, nomeCompleto: dados.nomeCompleto.trim(), email: dados.email.trim(), login })
      setDados(vazio)
      setLoginEditado(false)
      setTentou(false)
      onConvidado(novo)
    } catch (error) {
      if (getErrorCode(error) === 'LOGIN_EM_USO') {
        setErroLogin(getErrorMessage(error, 'Este usuário de acesso já está em uso.'))
      } else {
        setErro(getErrorMessage(error, 'Não foi possível enviar o convite. Tente novamente.'))
      }
    } finally {
      setEnviando(false)
    }
  }

  const mostrar = (campo: keyof typeof erros) => (tentou || (campo === 'login' && erroLogin) ? erros[campo] : undefined)

  return (
    <Dialog open={open} title="Convidar usuário" onClose={fechar} busy={enviando} fecharPeloFundo={false}>
      <form className="us-form" onSubmit={enviar} noValidate>
        <p>A pessoa recebe um e-mail com um link para criar a própria senha. O convite vale por 48 horas.</p>
        {erro && (
          <div className="ui-alert ui-alert--error" role="alert">
            <AlertIcon />
            <div className="ui-alert__content">{erro}</div>
          </div>
        )}
        <Campo id="cv-nome" rotulo="Nome completo" erro={mostrar('nomeCompleto')}>
          <input
            id="cv-nome"
            className="cf-input"
            value={dados.nomeCompleto}
            maxLength={150}
            autoComplete="off"
            onChange={(e) => {
              const nomeCompleto = e.target.value
              setDados((d) => ({ ...d, nomeCompleto, login: loginEditado ? d.login : sugerirLogin(nomeCompleto) }))
              if (!loginEditado) setErroLogin(null)
            }}
            aria-invalid={Boolean(mostrar('nomeCompleto'))}
            aria-describedby={mostrar('nomeCompleto') ? 'cv-nome-erro' : undefined}
          />
        </Campo>
        <Campo id="cv-email" rotulo="E-mail" erro={mostrar('email')}>
          <input
            id="cv-email"
            type="email"
            className="cf-input"
            value={dados.email}
            maxLength={150}
            autoComplete="off"
            onChange={(e) => setDados((d) => ({ ...d, email: e.target.value }))}
            aria-invalid={Boolean(mostrar('email'))}
            aria-describedby={mostrar('email') ? 'cv-email-erro' : undefined}
          />
        </Campo>
        <Campo id="cv-login" rotulo="Usuário de acesso" erro={mostrar('login')} dica="É o que a pessoa digita para entrar.">
          <input
            id="cv-login"
            className="cf-input cf-input--mono"
            value={dados.login}
            maxLength={60}
            autoComplete="off"
            autoCapitalize="none"
            spellCheck={false}
            onChange={(e) => {
              setLoginEditado(true)
              setErroLogin(null)
              setDados((d) => ({ ...d, login: e.target.value.toLowerCase() }))
            }}
            aria-invalid={Boolean(mostrar('login'))}
            aria-describedby={mostrar('login') ? 'cv-login-erro' : 'cv-login-dica'}
          />
        </Campo>
        <fieldset className="cf-tipo us-perfil">
          <legend className="cf-label">Perfil</legend>
          <div className="cf-segmented">
            {(['USUARIO', 'ADMIN'] as const).map((p) => (
              <label key={p} className="cf-segmented__option">
                <input
                  type="radio"
                  name="perfil"
                  value={p}
                  checked={dados.perfil === p}
                  onChange={() => setDados((d) => ({ ...d, perfil: p }))}
                />
                <span>{ROTULO_PERFIL[p]}</span>
              </label>
            ))}
          </div>
        </fieldset>
        <div className="ui-dialog__actions">
          <button type="button" className="ui-btn ui-btn--secondary" onClick={fechar} disabled={enviando}>
            Cancelar
          </button>
          <button type="submit" className="ui-btn ui-btn--primary" disabled={enviando} aria-busy={enviando}>
            {enviando ? 'Enviando…' : 'Enviar convite'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}

function Campo({
  id,
  rotulo,
  erro,
  dica,
  children,
}: {
  id: string
  rotulo: string
  erro?: string
  dica?: string
  children: ReactNode
}) {
  return (
    <div className="cf-field" data-invalid={Boolean(erro)}>
      <label htmlFor={id} className="cf-label">
        {rotulo}
      </label>
      {children}
      {erro ? (
        <p id={`${id}-erro`} className="cf-error">
          {erro}
        </p>
      ) : (
        dica && (
          <p id={`${id}-dica`} className="cf-hint">
            {dica}
          </p>
        )
      )}
    </div>
  )
}
