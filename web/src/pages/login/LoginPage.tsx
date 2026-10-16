import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { FullPageLoading } from '../../components/FullPageLoading'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { useAuth } from '../../features/auth/useAuth'
import { getErrorMessage } from '../../services/errors'

// Tela de login genérica (sem marca de escritório): o tema do contador só carrega após o login.
export function LoginPage() {
  const { status, login } = useAuth()
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  if (status === 'loading') return <FullPageLoading />
  if (status === 'authenticated') return <Navigate to="/app" replace />
  if (status === 'primeiro-acesso') return <Navigate to="/primeiro-acesso" replace />

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await login(usuario.trim(), senha)
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível entrar. Tente novamente.'))
      setEnviando(false)
    }
  }

  return (
    <AuthLayout title="Entrar" subtitle="Acesse o backoffice com seu login e senha.">
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label className="field__label" htmlFor="login">
            Login
          </label>
          <input
            id="login"
            className="field__input"
            type="text"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            required
            autoFocus
            value={usuario}
            onChange={(event) => setUsuario(event.target.value)}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="senha">
            Senha
          </label>
          <div className="password-field">
            <input
              id="senha"
              className="field__input"
              type={mostrarSenha ? 'text' : 'password'}
              autoComplete="current-password"
              required
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
            />
            <button
              type="button"
              className="password-field__toggle"
              onClick={() => setMostrarSenha((atual) => !atual)}
              aria-pressed={mostrarSenha}
            >
              {mostrarSenha ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>
        </div>

        {erro && (
          <p className="form-error" role="alert">
            {erro}
          </p>
        )}

        <button className="btn btn--primary" type="submit" disabled={enviando || !usuario.trim() || !senha}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </AuthLayout>
  )
}
