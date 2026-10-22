import { useEffect, useRef, useState, type FormEvent, type ReactNode, type Ref } from 'react'
import { AlertIcon, CheckIcon, KeyIcon, MailIcon } from '../../components/icons'
import { ConfirmDialog } from '../../components/ui/Dialog'
import { useToast } from '../../components/ui/useToast'
import { useAuth } from '../../features/auth/useAuth'
import { checarSenha, senhaValida } from '../../features/auth/passwordPolicy'
import { formatarDataHora, mascararTelefone } from '../../features/clientes/formatar'
import '../../features/clientes/clientes.css'
import {
  atualizarPerfil,
  buscarPerfil,
  cancelarTroca,
  pedirTrocaEmail,
  pedirTrocaSenha,
  type Perfil,
  type TipoAlteracao,
  type TrocaPendente,
} from '../../features/perfil/perfilService'
import { PageHeader } from '../../layouts/PageHeader'
import { getErrorMessage } from '../../services/errors'
import './perfil.css'

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function telefoneValido(valor: string) {
  const d = valor.replace(/\D/g, '')
  return d.length >= 10 && d.length <= 13
}

// Telefone salvo só com dígitos → máscara de exibição (com código do país, mostra como está)
function telefoneParaCampo(telefone: string | null) {
  if (!telefone) return ''
  return telefone.length <= 11 ? mascararTelefone(telefone) : telefone
}

// Pendência que já passou da validade deixa de aparecer, mesmo sem recarregar
function pendenteValida(troca: TrocaPendente | null): TrocaPendente | null {
  return troca && new Date(troca.expiraEm).getTime() > Date.now() ? troca : null
}

// Meu perfil: nome e telefone mudam na hora; e-mail e senha só depois da confirmação pelo link do e-mail.
export function PerfilPage() {
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [tentativa, setTentativa] = useState(0)

  useEffect(() => {
    let cancelado = false
    buscarPerfil()
      .then((dados) => !cancelado && setPerfil(dados))
      .catch((error: unknown) => !cancelado && setErro(getErrorMessage(error, 'Não foi possível carregar o seu perfil.')))
    return () => {
      cancelado = true
    }
  }, [tentativa])

  return (
    <>
      <PageHeader
        titulo="Meu perfil"
        subtitulo={perfil ? `Usuário ${perfil.login} · ${perfil.nomeEscritorio}` : 'Carregando…'}
      />

      {erro && !perfil ? (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">
            {erro}
            <div className="ui-alert__actions">
              <button
                type="button"
                className="ui-btn ui-btn--secondary ui-btn--sm"
                onClick={() => {
                  setErro(null)
                  setTentativa((n) => n + 1)
                }}
              >
                Tentar novamente
              </button>
            </div>
          </div>
        </div>
      ) : !perfil ? (
        <div className="pf" role="status" aria-label="Carregando perfil">
          <span className="ui-skeleton" style={{ height: 200 }} />
          <span className="ui-skeleton" style={{ height: 120 }} />
        </div>
      ) : (
        <div className="pf">
          <DadosPessoais perfil={perfil} onSalvo={setPerfil} />
          <SecaoEmail perfil={perfil} onAlterado={setPerfil} />
          <SecaoSenha perfil={perfil} onAlterado={setPerfil} />
        </div>
      )}
    </>
  )
}

// ---------- Dados pessoais ----------

function DadosPessoais({ perfil, onSalvo }: { perfil: Perfil; onSalvo: (p: Perfil) => void }) {
  const { notify } = useToast()
  const { recarregarUsuario } = useAuth()
  const [nome, setNome] = useState(perfil.nomeCompleto ?? '')
  const [telefone, setTelefone] = useState(telefoneParaCampo(perfil.telefone))
  const [tentouSalvar, setTentouSalvar] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const erroNome = tentouSalvar && !nome.trim() ? 'Informe o nome completo.' : undefined
  const erroTelefone = tentouSalvar && !telefoneValido(telefone) ? 'Informe o DDD e o número.' : undefined
  const alterado =
    nome.trim() !== (perfil.nomeCompleto ?? '') || telefone.replace(/\D/g, '') !== (perfil.telefone ?? '')

  async function salvar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTentouSalvar(true)
    setErro(null)
    if (!nome.trim() || !telefoneValido(telefone)) return
    setSalvando(true)
    try {
      const atualizado = await atualizarPerfil(nome.trim(), telefone)
      onSalvo(atualizado)
      setTentouSalvar(false)
      notify('Dados salvos.')
      void recarregarUsuario()
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível salvar. Tente novamente.'))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form className="panel cf-section" onSubmit={salvar} noValidate aria-labelledby="pf-dados">
      <h2 id="pf-dados" className="cf-section__title">
        Dados pessoais
      </h2>
      {erro && <AlertaErro mensagem={erro} />}
      <div className="cf-grid">
        <div className="cf-field cf-field--wide" data-invalid={Boolean(erroNome)}>
          <label htmlFor="pf-nome" className="cf-label">
            Nome completo
          </label>
          <input
            id="pf-nome"
            className="cf-input"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            maxLength={150}
            autoComplete="name"
            aria-invalid={Boolean(erroNome)}
            aria-describedby={erroNome ? 'pf-nome-erro' : undefined}
          />
          {erroNome && (
            <p id="pf-nome-erro" className="cf-error">
              {erroNome}
            </p>
          )}
        </div>
        <div className="cf-field" data-invalid={Boolean(erroTelefone)}>
          <label htmlFor="pf-telefone" className="cf-label">
            Telefone
          </label>
          <input
            id="pf-telefone"
            type="tel"
            className="cf-input"
            value={telefone}
            onChange={(e) => setTelefone(mascararTelefone(e.target.value))}
            inputMode="tel"
            autoComplete="tel"
            placeholder="(85) 99999-9999"
            aria-invalid={Boolean(erroTelefone)}
            aria-describedby={erroTelefone ? 'pf-telefone-erro' : undefined}
          />
          {erroTelefone && (
            <p id="pf-telefone-erro" className="cf-error">
              {erroTelefone}
            </p>
          )}
        </div>
        <div className="cf-field">
          <span className="cf-label">Usuário de acesso</span>
          <p className="pf-readonly">{perfil.login}</p>
        </div>
      </div>
      <div className="pf-actions">
        <button type="submit" className="ui-btn ui-btn--primary" disabled={salvando || !alterado} aria-busy={salvando}>
          {salvando ? 'Salvando…' : 'Salvar dados'}
        </button>
      </div>
    </form>
  )
}

// ---------- E-mail ----------

function SecaoEmail({ perfil, onAlterado }: { perfil: Perfil; onAlterado: (p: Perfil) => void }) {
  const { notify } = useToast()
  const pendente = pendenteValida(perfil.trocaEmailPendente)
  const [aberto, setAberto] = useState(false)
  const [novoEmail, setNovoEmail] = useState('')
  const [senhaAtual, setSenhaAtual] = useState('')
  const [tentou, setTentou] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const primeiroCampo = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (aberto) primeiroCampo.current?.focus()
  }, [aberto])

  const erroEmail = tentou
    ? !novoEmail.trim()
      ? 'Informe o novo e-mail.'
      : !EMAIL.test(novoEmail.trim())
        ? 'E-mail inválido.'
        : novoEmail.trim().toLowerCase() === perfil.email
          ? 'Este já é o seu e-mail atual.'
          : undefined
    : undefined
  const erroSenha = tentou && !senhaAtual ? 'Informe a senha atual.' : undefined

  function fechar() {
    setAberto(false)
    setNovoEmail('')
    setSenhaAtual('')
    setTentou(false)
    setErro(null)
  }

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTentou(true)
    setErro(null)
    const email = novoEmail.trim()
    if (!email || !EMAIL.test(email) || email.toLowerCase() === perfil.email || !senhaAtual) return
    setEnviando(true)
    try {
      const troca = await pedirTrocaEmail(senhaAtual, email)
      onAlterado({ ...perfil, trocaEmailPendente: troca })
      fechar()
      notify('Link de confirmação enviado.')
    } catch (error) {
      setSenhaAtual('')
      setErro(getErrorMessage(error, 'Não foi possível enviar o link. Tente novamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="panel cf-section" aria-labelledby="pf-email">
      <div className="pf-head">
        <div>
          <h2 id="pf-email" className="cf-section__title pf-head__title">
            E-mail
          </h2>
          <p className="pf-current">{perfil.email ?? 'Não cadastrado'}</p>
          <p className="pf-help">Recebe os avisos do sistema e o link de recuperação de senha.</p>
        </div>
        {!aberto && !pendente && (
          <button type="button" className="ui-btn ui-btn--secondary" onClick={() => setAberto(true)}>
            <MailIcon width={18} height={18} />
            Alterar e-mail
          </button>
        )}
      </div>

      {pendente && (
        <Pendencia
          tipo="EMAIL"
          titulo="Esperando a confirmação do novo e-mail"
          texto={
            <>
              Enviamos um link para <strong>{pendente.novoEmail}</strong>. O e-mail só muda depois que você abrir o
              link e confirmar, até {formatarDataHora(pendente.expiraEm)}.
            </>
          }
          onCancelado={() => onAlterado({ ...perfil, trocaEmailPendente: null })}
        />
      )}

      {aberto && (
        <form className="pf-form" onSubmit={enviar} noValidate>
          {erro && <AlertaErro mensagem={erro} />}
          <div className="cf-grid">
            <CampoTexto
              id="pf-novo-email"
              rotulo="Novo e-mail"
              tipo="email"
              valor={novoEmail}
              onChange={setNovoEmail}
              erro={erroEmail}
              autoComplete="email"
              inputRef={primeiroCampo}
            />
            <CampoTexto
              id="pf-senha-email"
              rotulo="Senha atual"
              tipo="password"
              valor={senhaAtual}
              onChange={setSenhaAtual}
              erro={erroSenha}
              autoComplete="current-password"
            />
          </div>
          <p className="pf-help">
            Vamos enviar um link de confirmação para o novo e-mail. Até você confirmar, continua valendo o atual.
          </p>
          <div className="pf-actions">
            <button type="button" className="ui-btn ui-btn--secondary" onClick={fechar} disabled={enviando}>
              Cancelar
            </button>
            <button type="submit" className="ui-btn ui-btn--primary" disabled={enviando} aria-busy={enviando}>
              {enviando ? 'Enviando…' : 'Enviar link de confirmação'}
            </button>
          </div>
        </form>
      )}
    </section>
  )
}

// ---------- Senha ----------

function SecaoSenha({ perfil, onAlterado }: { perfil: Perfil; onAlterado: (p: Perfil) => void }) {
  const { notify } = useToast()
  const pendente = pendenteValida(perfil.trocaSenhaPendente)
  const [aberto, setAberto] = useState(false)
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [mostrar, setMostrar] = useState(false)
  const [tentou, setTentou] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const primeiroCampo = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (aberto) primeiroCampo.current?.focus()
  }, [aberto])

  const checks = checarSenha(novaSenha)
  const erroAtual = tentou && !senhaAtual ? 'Informe a senha atual.' : undefined
  const erroNova = tentou
    ? !novaSenha
      ? 'Informe a nova senha.'
      : !senhaValida(novaSenha)
        ? 'A nova senha não atende às regras abaixo.'
        : novaSenha === senhaAtual
          ? 'A nova senha deve ser diferente da atual.'
          : undefined
    : undefined
  const erroConfirmacao = tentou && confirmacao !== novaSenha ? 'As senhas não conferem.' : undefined

  function fechar() {
    setAberto(false)
    setSenhaAtual('')
    setNovaSenha('')
    setConfirmacao('')
    setMostrar(false)
    setTentou(false)
    setErro(null)
  }

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTentou(true)
    setErro(null)
    if (!senhaAtual || !senhaValida(novaSenha) || novaSenha === senhaAtual || confirmacao !== novaSenha) return
    setEnviando(true)
    try {
      const troca = await pedirTrocaSenha(senhaAtual, novaSenha)
      onAlterado({ ...perfil, trocaSenhaPendente: troca })
      fechar()
      notify('Link de confirmação enviado.')
    } catch (error) {
      setSenhaAtual('')
      setErro(getErrorMessage(error, 'Não foi possível enviar o link. Tente novamente.'))
    } finally {
      setEnviando(false)
    }
  }

  const regras = [
    { ok: checks.tamanho, texto: 'Pelo menos 8 caracteres' },
    { ok: checks.letra, texto: 'Letras' },
    { ok: checks.numero, texto: 'Números' },
    { ok: checks.simbolo, texto: 'Símbolos (por exemplo ! @ # $ %)' },
  ]

  return (
    <section className="panel cf-section" aria-labelledby="pf-senha">
      <div className="pf-head">
        <div>
          <h2 id="pf-senha" className="cf-section__title pf-head__title">
            Senha
          </h2>
          <p className="pf-help">
            Por segurança, a troca só vale depois que você confirmar pelo link enviado para{' '}
            <strong>{perfil.email}</strong>.
          </p>
        </div>
        {!aberto && !pendente && (
          <button type="button" className="ui-btn ui-btn--secondary" onClick={() => setAberto(true)}>
            <KeyIcon width={18} height={18} />
            Alterar senha
          </button>
        )}
      </div>

      {pendente && (
        <Pendencia
          tipo="SENHA"
          titulo="Esperando a confirmação da troca de senha"
          texto={
            <>
              Enviamos um link para <strong>{pendente.enviadoPara}</strong>. A senha atual continua valendo até
              você abrir o link e confirmar, até {formatarDataHora(pendente.expiraEm)}. Depois da confirmação,
              será preciso entrar de novo.
            </>
          }
          onCancelado={() => onAlterado({ ...perfil, trocaSenhaPendente: null })}
        />
      )}

      {aberto && (
        <form className="pf-form" onSubmit={enviar} noValidate>
          {erro && <AlertaErro mensagem={erro} />}
          <div className="cf-grid">
            <CampoTexto
              id="pf-senha-atual"
              rotulo="Senha atual"
              tipo={mostrar ? 'text' : 'password'}
              valor={senhaAtual}
              onChange={setSenhaAtual}
              erro={erroAtual}
              autoComplete="current-password"
              inputRef={primeiroCampo}
              largo
            />
            <CampoTexto
              id="pf-nova-senha"
              rotulo="Nova senha"
              tipo={mostrar ? 'text' : 'password'}
              valor={novaSenha}
              onChange={setNovaSenha}
              erro={erroNova}
              autoComplete="new-password"
              descricao="pf-regras"
            />
            <CampoTexto
              id="pf-confirmacao"
              rotulo="Confirmar nova senha"
              tipo={mostrar ? 'text' : 'password'}
              valor={confirmacao}
              onChange={setConfirmacao}
              erro={erroConfirmacao}
              autoComplete="new-password"
            />
          </div>
          <ul id="pf-regras" className="pf-rules" aria-label="Regras da nova senha">
            {regras.map((r) => (
              <li key={r.texto} className={r.ok ? 'pf-rules__ok' : undefined}>
                {r.ok && <CheckIcon width={14} height={14} />}
                {r.texto}
                <span className="sr-only">{r.ok ? ' (atendida)' : ' (pendente)'}</span>
              </li>
            ))}
          </ul>
          <label className="pf-check">
            <input type="checkbox" checked={mostrar} onChange={(e) => setMostrar(e.target.checked)} />
            Mostrar senhas
          </label>
          <div className="pf-actions">
            <button type="button" className="ui-btn ui-btn--secondary" onClick={fechar} disabled={enviando}>
              Cancelar
            </button>
            <button type="submit" className="ui-btn ui-btn--primary" disabled={enviando} aria-busy={enviando}>
              {enviando ? 'Enviando…' : 'Enviar link de confirmação'}
            </button>
          </div>
        </form>
      )}
    </section>
  )
}

// ---------- Peças ----------

function Pendencia({
  tipo,
  titulo,
  texto,
  onCancelado,
}: {
  tipo: TipoAlteracao
  titulo: string
  texto: ReactNode
  onCancelado: () => void
}) {
  const { notify } = useToast()
  const [confirmar, setConfirmar] = useState(false)
  const [cancelando, setCancelando] = useState(false)

  async function cancelar() {
    setCancelando(true)
    try {
      await cancelarTroca(tipo)
      setConfirmar(false)
      onCancelado()
      notify('Pedido cancelado. O link do e-mail não funciona mais.')
    } catch (error) {
      setConfirmar(false)
      notify(getErrorMessage(error, 'Não foi possível cancelar. Tente novamente.'), 'error')
    } finally {
      setCancelando(false)
    }
  }

  return (
    <div className="ui-alert ui-alert--info pf-pending">
      <MailIcon width={20} height={20} />
      <div className="ui-alert__content">
        <strong className="pf-pending__title">{titulo}</strong>
        <p className="pf-pending__text">{texto}</p>
        <p className="pf-pending__text pf-pending__muted">Não chegou? Confira a caixa de spam ou cancele e peça de novo.</p>
        <div className="ui-alert__actions">
          <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={() => setConfirmar(true)}>
            Cancelar pedido
          </button>
        </div>
      </div>
      <ConfirmDialog
        open={confirmar}
        title="Cancelar o pedido?"
        confirmLabel="Cancelar pedido"
        tone="danger"
        busy={cancelando}
        onConfirm={() => void cancelar()}
        onClose={() => setConfirmar(false)}
      >
        <p>O link enviado por e-mail deixa de funcionar. Nada é alterado.</p>
      </ConfirmDialog>
    </div>
  )
}

function CampoTexto({
  id,
  rotulo,
  tipo,
  valor,
  onChange,
  erro,
  autoComplete,
  inputRef,
  largo,
  descricao,
}: {
  id: string
  rotulo: string
  tipo: string
  valor: string
  onChange: (v: string) => void
  erro?: string
  autoComplete?: string
  inputRef?: Ref<HTMLInputElement>
  largo?: boolean
  descricao?: string
}) {
  const descritores = [erro ? `${id}-erro` : null, descricao ?? null].filter(Boolean).join(' ') || undefined
  return (
    <div className={`cf-field${largo ? ' cf-field--wide' : ''}`} data-invalid={Boolean(erro)}>
      <label htmlFor={id} className="cf-label">
        {rotulo}
      </label>
      <input
        ref={inputRef}
        id={id}
        type={tipo}
        className="cf-input"
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        autoComplete={autoComplete}
        aria-invalid={Boolean(erro)}
        aria-describedby={descritores}
      />
      {erro && (
        <p id={`${id}-erro`} className="cf-error">
          {erro}
        </p>
      )}
    </div>
  )
}

function AlertaErro({ mensagem }: { mensagem: string }) {
  return (
    <div className="ui-alert ui-alert--error pf-alert" role="alert">
      <AlertIcon />
      <div className="ui-alert__content">{mensagem}</div>
    </div>
  )
}
