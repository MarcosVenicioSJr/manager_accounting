import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { FullPageLoading } from '../../components/FullPageLoading'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { redefinirSenha, validarLinkRedefinicao } from '../../features/auth/authService'
import { AlertIcon, CheckCircleIcon, SpinnerIcon } from '../../features/auth/icons'
import { NovaSenhaFields } from '../../features/auth/NovaSenhaFields'
import { senhaValida } from '../../features/auth/passwordPolicy'
import { useAuth } from '../../features/auth/useAuth'
import { getErrorMessage } from '../../services/errors'

type Etapa = 'validando' | 'formulario' | 'invalido' | 'concluido'

const MENSAGEM_LINK_INVALIDO = 'Este link de redefinição é inválido ou expirou. Peça um novo em "Esqueceu sua senha?".'

// O link do e-mail tem o formato /redefinir-senha#token=...: o que vem depois do "#" não é enviado ao
// servidor do front nem vaza no Referer.
function lerTokenDaUrl(): string {
  return new URLSearchParams(window.location.hash.slice(1)).get('token') ?? ''
}

// Tela aberta pelo link do e-mail: confere o link e grava a nova senha.
export function RedefinirSenhaPage() {
  const { logout } = useAuth()
  const [token] = useState(lerTokenDaUrl)
  const [etapa, setEtapa] = useState<Etapa>(token ? 'validando' : 'invalido')
  const [mensagemInvalido, setMensagemInvalido] = useState(MENSAGEM_LINK_INVALIDO)
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    // Tira o token da barra de endereço (histórico, prints de tela)
    if (window.location.hash) {
      window.history.replaceState(null, '', window.location.pathname + window.location.search)
    }
    if (!token) return

    let cancelado = false
    validarLinkRedefinicao(token)
      .then(() => {
        if (!cancelado) setEtapa('formulario')
      })
      .catch((error: unknown) => {
        if (cancelado) return
        setMensagemInvalido(getErrorMessage(error, MENSAGEM_LINK_INVALIDO))
        setEtapa('invalido')
      })
    return () => {
      cancelado = true
    }
  }, [token])

  const formularioValido = senhaValida(novaSenha) && novaSenha === confirmacao

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!formularioValido) return
    setErro(null)
    setEnviando(true)
    try {
      await redefinirSenha(token, novaSenha)
      // O backend encerrou todas as sessões; limpa também a deste navegador, se houver
      await logout()
      setEtapa('concluido')
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível salvar a nova senha. Tente novamente.'))
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
          <h1 className="auth-title">Link inválido</h1>
          <p className="auth-subtitle auth-subtitle--wide">{mensagemInvalido}</p>
          <Link className="auth-submit auth-submit--spaced" to="/recuperar-senha">
            <span>Pedir um novo link</span>
          </Link>
          <div className="auth-actions">
            <Link className="auth-link" to="/login">
              Voltar para o login
            </Link>
          </div>
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
          <h1 className="auth-title">Senha alterada</h1>
          <p className="auth-subtitle auth-subtitle--wide">
            Sua nova senha já está valendo. Por segurança, as sessões abertas em outros navegadores foram
            encerradas.
          </p>
          <Link className="auth-submit auth-submit--spaced" to="/login">
            <span>Ir para o login</span>
          </Link>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <form className="auth-form auth-form--grid" onSubmit={handleSubmit} noValidate>
        <div>
          <h1 className="auth-title">Criar nova senha</h1>
          <p className="auth-subtitle auth-subtitle--wide">Escolha uma senha nova para acessar o sistema.</p>
        </div>

        {erro && (
          <div className="auth-alert" role="alert">
            <AlertIcon />
            <span>{erro}</span>
          </div>
        )}

        <NovaSenhaFields
          novaSenha={novaSenha}
          onNovaSenhaChange={setNovaSenha}
          confirmacao={confirmacao}
          onConfirmacaoChange={setConfirmacao}
          autoFocus
        />

        <button
          className="auth-submit auth-submit--spaced"
          type="submit"
          disabled={enviando || !formularioValido}
          aria-busy={enviando}
        >
          {enviando && <SpinnerIcon />}
          <span>{enviando ? 'Salvando…' : 'Salvar nova senha'}</span>
        </button>
      </form>
    </AuthLayout>
  )
}
