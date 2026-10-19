import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { solicitarRecuperacaoSenha } from '../../features/auth/authService'
import { AlertIcon, ArrowLeftIcon, MailIcon, SpinnerIcon, UserIcon } from '../../features/auth/icons'
import { getErrorMessage } from '../../services/errors'

// "Esqueceu sua senha?": a pessoa informa o usuário e recebe um link no e-mail cadastrado no primeiro acesso.
// A resposta é sempre a mesma (não revela se o usuário existe).
export function RecuperarSenhaPage() {
  const [usuario, setUsuario] = useState('')
  const [tentouEnviar, setTentouEnviar] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [mensagemEnviada, setMensagemEnviada] = useState<string | null>(null)

  const usuarioInvalido = tentouEnviar && !usuario.trim()

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTentouEnviar(true)
    if (!usuario.trim()) return

    setErro(null)
    setEnviando(true)
    try {
      setMensagemEnviada(await solicitarRecuperacaoSenha(usuario.trim()))
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível enviar o link. Tente novamente.'))
    } finally {
      setEnviando(false)
    }
  }

  function tentarDeNovo() {
    setMensagemEnviada(null)
    setTentouEnviar(false)
  }

  if (mensagemEnviada) {
    return (
      <AuthLayout>
        <div className="auth-form" role="status">
          <div className="auth-status-icon">
            <MailIcon />
          </div>
          <h1 className="auth-title">Verifique seu e-mail</h1>
          <p className="auth-subtitle auth-subtitle--wide">{mensagemEnviada}</p>

          <Link className="auth-submit auth-submit--spaced" to="/login">
            <span>Voltar para o login</span>
          </Link>

          <p className="auth-note">
            <strong>Não chegou?</strong> Confira a caixa de spam. Um novo link pode ser pedido depois de 2
            minutos, e só o link mais recente funciona.
          </p>
          <div className="auth-actions">
            <button type="button" className="auth-text-button" onClick={tentarDeNovo}>
              Enviar novamente
            </button>
          </div>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <h1 className="auth-title">Esqueceu sua senha?</h1>
        <p className="auth-subtitle auth-subtitle--wide">
          Informe seu usuário. Enviaremos um link para criar uma nova senha no e-mail cadastrado.
        </p>

        {erro && (
          <div className="auth-alert" role="alert">
            <AlertIcon />
            <span>{erro}</span>
          </div>
        )}

        <label htmlFor="login" className="sr-only">
          Usuário
        </label>
        <div className="auth-field" data-invalid={usuarioInvalido}>
          <UserIcon />
          <input
            id="login"
            name="login"
            className="auth-input"
            type="text"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            placeholder="Usuário"
            autoFocus
            maxLength={60}
            value={usuario}
            onChange={(event) => setUsuario(event.target.value)}
            aria-invalid={usuarioInvalido}
            aria-describedby={usuarioInvalido ? 'login-erro' : undefined}
          />
        </div>
        {usuarioInvalido && (
          <p id="login-erro" className="auth-field-error">
            Informe seu usuário.
          </p>
        )}

        <button className="auth-submit" type="submit" disabled={enviando} aria-busy={enviando}>
          {enviando ? <SpinnerIcon /> : <MailIcon />}
          <span>{enviando ? 'Enviando…' : 'Enviar link'}</span>
        </button>

        <p className="auth-note">
          <strong>Ainda não fez o primeiro acesso?</strong> Entre com a senha temporária que você recebeu do
          escritório. O e-mail para recuperação é cadastrado no primeiro acesso.
        </p>

        <Link className="auth-link auth-back" to="/login">
          <ArrowLeftIcon />
          Voltar para o login
        </Link>
      </form>
    </AuthLayout>
  )
}
