import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { FullPageLoading } from '../../components/FullPageLoading'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { AlertIcon, CheckCircleIcon, SpinnerIcon } from '../../features/auth/icons'
import { NovaSenhaFields } from '../../features/auth/NovaSenhaFields'
import { senhaValida } from '../../features/auth/passwordPolicy'
import { mascararTelefone } from '../../features/clientes/formatar'
import { aceitarConvite, validarConvite, type ConviteInfo } from '../../features/equipe/equipeService'
import { getErrorMessage } from '../../services/errors'

type Etapa = 'validando' | 'formulario' | 'invalido' | 'concluido'

const MENSAGEM_INVALIDO =
  'Este convite é inválido ou expirou. Peça ao administrador do escritório para reenviar.'

// Link do e-mail: /aceitar-convite#token=... (o que vem depois do "#" não vai para o servidor do front).
function lerTokenDaUrl(): string {
  return new URLSearchParams(window.location.hash.slice(1)).get('token') ?? ''
}

// Tela do convite: a pessoa confere o nome, informa o telefone e cria a senha. Depois entra pelo login normal.
export function AceitarConvitePage() {
  const [token] = useState(lerTokenDaUrl)
  const [etapa, setEtapa] = useState<Etapa>(token ? 'validando' : 'invalido')
  const [info, setInfo] = useState<ConviteInfo | null>(null)
  const [mensagemInvalido, setMensagemInvalido] = useState(MENSAGEM_INVALIDO)
  const [nome, setNome] = useState('')
  const [telefone, setTelefone] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  // Outro link aberto na mesma aba só troca o "#": recarrega para ler o token novo
  useEffect(() => {
    const onHashChange = () => {
      if (lerTokenDaUrl()) window.location.reload()
    }
    window.addEventListener('hashchange', onHashChange)
    return () => window.removeEventListener('hashchange', onHashChange)
  }, [])

  useEffect(() => {
    // Tira o token da barra de endereço (histórico, prints de tela)
    if (window.location.hash) {
      window.history.replaceState(null, '', window.location.pathname + window.location.search)
    }
    if (!token) return
    let cancelado = false
    validarConvite(token)
      .then((dados) => {
        if (cancelado) return
        setInfo(dados)
        setNome(dados.nomeCompleto ?? '')
        setEtapa('formulario')
      })
      .catch((error: unknown) => {
        if (cancelado) return
        setMensagemInvalido(getErrorMessage(error, MENSAGEM_INVALIDO))
        setEtapa('invalido')
      })
    return () => {
      cancelado = true
    }
  }, [token])

  const digitosTelefone = telefone.replace(/\D/g, '')
  const formularioValido =
    nome.trim().length > 0 &&
    digitosTelefone.length >= 10 &&
    senhaValida(novaSenha) &&
    novaSenha === confirmacao

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!formularioValido) return
    setErro(null)
    setEnviando(true)
    try {
      await aceitarConvite(token, nome.trim(), telefone, novaSenha)
      setEtapa('concluido')
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível criar a sua senha. Tente novamente.'))
    } finally {
      setEnviando(false)
    }
  }

  if (etapa === 'validando') return <FullPageLoading />

  if (etapa === 'invalido') {
    return (
      <AuthLayout>
        <div className="auth-form" role="alert">
          <div className="auth-status-icon auth-status-icon--erro">
            <AlertIcon />
          </div>
          <h1 className="auth-title">Convite inválido</h1>
          <p className="auth-subtitle auth-subtitle--wide">{mensagemInvalido}</p>
          <Link className="auth-submit auth-submit--spaced" to="/login">
            <span>Ir para o login</span>
          </Link>
        </div>
      </AuthLayout>
    )
  }

  if (etapa === 'concluido') {
    return (
      <AuthLayout>
        <div className="auth-form" role="status">
          <div className="auth-status-icon auth-status-icon--ok">
            <CheckCircleIcon />
          </div>
          <h1 className="auth-title">Tudo pronto</h1>
          <p className="auth-subtitle auth-subtitle--wide">
            Sua senha foi criada. Entre com o usuário <strong>{info?.login}</strong> e a senha que você acabou de
            escolher.
          </p>
          <Link className="auth-submit auth-submit--spaced" to="/login">
            <span>Entrar no backoffice</span>
          </Link>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <form className="auth-form auth-form--grid" onSubmit={handleSubmit} noValidate>
        <div>
          <h1 className="auth-title">Criar sua senha</h1>
          <p className="auth-subtitle auth-subtitle--wide">
            Você foi convidado para o backoffice de <strong>{info?.nomeEscritorio}</strong>. Seu usuário de acesso é{' '}
            <strong>{info?.login}</strong>.
          </p>
        </div>

        {erro && (
          <div className="auth-alert" role="alert">
            <AlertIcon />
            <span>{erro}</span>
          </div>
        )}

        <div>
          <label className="auth-label" htmlFor="nome">
            Nome completo
          </label>
          <div className="auth-field">
            <input
              id="nome"
              className="auth-input"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              maxLength={150}
              autoComplete="name"
            />
          </div>
        </div>

        <div>
          <label className="auth-label" htmlFor="telefone">
            Telefone
          </label>
          <div className="auth-field">
            <input
              id="telefone"
              className="auth-input"
              type="tel"
              inputMode="tel"
              value={telefone}
              onChange={(e) => setTelefone(mascararTelefone(e.target.value))}
              placeholder="(85) 99999-9999"
              autoComplete="tel"
            />
          </div>
        </div>

        <NovaSenhaFields
          novaSenha={novaSenha}
          onNovaSenhaChange={setNovaSenha}
          confirmacao={confirmacao}
          onConfirmacaoChange={setConfirmacao}
        />

        <button
          className="auth-submit auth-submit--spaced"
          type="submit"
          disabled={enviando || !formularioValido}
          aria-busy={enviando}
        >
          {enviando && <SpinnerIcon />}
          <span>{enviando ? 'Salvando…' : 'Criar senha'}</span>
        </button>
      </form>
    </AuthLayout>
  )
}
