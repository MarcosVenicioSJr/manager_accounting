import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { FullPageLoading } from '../../components/FullPageLoading'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { AlertIcon, EnterIcon, EyeIcon, EyeOffIcon, LockIcon, SpinnerIcon, UserIcon } from '../../features/auth/icons'
import { useAuth } from '../../features/auth/useAuth'
import { getErrorMessage } from '../../services/errors'

// Tela de login genérica (sem marca de escritório): o tema do contador só carrega após o login.
export function LoginPage() {
  const { status, login } = useAuth()
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [manterConectado, setManterConectado] = useState(false)
  const [tentouEnviar, setTentouEnviar] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  if (status === 'loading') return <FullPageLoading />
  if (status === 'authenticated') return <Navigate to="/app" replace />
  if (status === 'primeiro-acesso') return <Navigate to="/primeiro-acesso" replace />

  const loginInvalido = tentouEnviar && !usuario.trim()
  const senhaInvalida = tentouEnviar && !senha

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTentouEnviar(true)
    if (!usuario.trim() || !senha) return

    setErro(null)
    setEnviando(true)
    try {
      await login(usuario.trim(), senha, manterConectado)
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível entrar. Tente novamente.'))
      setEnviando(false)
    }
  }

  return (
    <AuthLayout>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <h1 className="auth-title">Bem-vindo!</h1>
        <p className="auth-subtitle">Faça login para acessar o sistema de contabilidade.</p>

        {erro && (
          <div className="auth-alert" role="alert">
            <AlertIcon />
            <span>{erro}</span>
          </div>
        )}

        <label htmlFor="login" className="sr-only">
          Usuário
        </label>
        <div className="auth-field" data-invalid={loginInvalido}>
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
            value={usuario}
            onChange={(event) => setUsuario(event.target.value)}
            aria-invalid={loginInvalido}
            aria-describedby={loginInvalido ? 'login-erro' : undefined}
          />
        </div>
        {loginInvalido && (
          <p id="login-erro" className="auth-field-error">
            Informe seu usuário.
          </p>
        )}

        <label htmlFor="senha" className="sr-only">
          Senha
        </label>
        <div className="auth-field auth-field--with-action" data-invalid={senhaInvalida} style={{ marginTop: 16 }}>
          <LockIcon />
          <input
            id="senha"
            name="senha"
            className="auth-input"
            type={mostrarSenha ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Senha"
            value={senha}
            onChange={(event) => setSenha(event.target.value)}
            aria-invalid={senhaInvalida}
            aria-describedby={senhaInvalida ? 'senha-erro' : undefined}
          />
          <button
            type="button"
            className="auth-eye"
            onClick={() => setMostrarSenha((atual) => !atual)}
            aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
            aria-pressed={mostrarSenha}
          >
            {mostrarSenha ? <EyeOffIcon /> : <EyeIcon />}
          </button>
        </div>
        {senhaInvalida && (
          <p id="senha-erro" className="auth-field-error">
            Informe sua senha.
          </p>
        )}

        <div className="auth-options">
          <label className="auth-remember">
            <input
              className="auth-check"
              type="checkbox"
              name="manter"
              checked={manterConectado}
              onChange={(event) => setManterConectado(event.target.checked)}
            />
            Manter-me conectado
          </label>
          <Link className="auth-link" to="/recuperar-senha">
            Esqueceu sua senha?
          </Link>
        </div>

        <button className="auth-submit" type="submit" disabled={enviando} aria-busy={enviando}>
          {enviando ? <SpinnerIcon /> : <EnterIcon />}
          <span>{enviando ? 'Entrando…' : 'Entrar'}</span>
        </button>

        <p className="auth-note">
          <strong>Primeiro acesso?</strong> Entre com a senha temporária que você recebeu. Em seguida, você
          cria uma nova senha e completa seus dados.
        </p>
      </form>
    </AuthLayout>
  )
}
