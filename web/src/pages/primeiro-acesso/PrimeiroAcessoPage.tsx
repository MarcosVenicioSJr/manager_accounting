import { useState, type FormEvent } from 'react'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { AlertIcon, SpinnerIcon } from '../../features/auth/icons'
import { NovaSenhaFields } from '../../features/auth/NovaSenhaFields'
import { senhaValida } from '../../features/auth/passwordPolicy'
import { useAuth } from '../../features/auth/useAuth'
import { getErrorMessage } from '../../services/errors'

// Primeiro acesso: troca da senha temporária e preenchimento de nome completo, e-mail e telefone.
export function PrimeiroAcessoPage() {
  const { concluirPrimeiroAcesso } = useAuth()
  const [nomeCompleto, setNomeCompleto] = useState('')
  const [email, setEmail] = useState('')
  const [telefone, setTelefone] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const senhasConferem = novaSenha === confirmacao
  const telefoneDigitos = telefone.replace(/\D/g, '')
  const formularioValido =
    nomeCompleto.trim().length > 0 &&
    email.trim().length > 0 &&
    telefoneDigitos.length >= 10 &&
    telefoneDigitos.length <= 13 &&
    senhaValida(novaSenha) &&
    senhasConferem

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!formularioValido) return
    setErro(null)
    setEnviando(true)
    try {
      await concluirPrimeiroAcesso({
        novaSenha,
        nomeCompleto: nomeCompleto.trim(),
        email: email.trim(),
        telefone,
      })
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível concluir o primeiro acesso. Tente novamente.'))
      setEnviando(false)
    }
  }

  return (
    <AuthLayout>
      <form className="auth-form auth-form--grid" onSubmit={handleSubmit} noValidate>
        <div>
          <h1 className="auth-title">Primeiro acesso</h1>
          <p className="auth-subtitle auth-subtitle--wide">
            Defina uma nova senha e complete seus dados para continuar.
          </p>
        </div>

        {erro && (
          <div className="auth-alert" role="alert">
            <AlertIcon />
            <span>{erro}</span>
          </div>
        )}

        <div>
          <label className="auth-label" htmlFor="nomeCompleto">
            Nome completo
          </label>
          <div className="auth-field">
            <input
              id="nomeCompleto"
              className="auth-input"
              type="text"
              autoComplete="name"
              autoFocus
              maxLength={150}
              value={nomeCompleto}
              onChange={(event) => setNomeCompleto(event.target.value)}
            />
          </div>
        </div>

        <div>
          <label className="auth-label" htmlFor="email">
            E-mail
          </label>
          <div className="auth-field">
            <input
              id="email"
              className="auth-input"
              type="email"
              autoComplete="email"
              maxLength={150}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
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
              autoComplete="tel"
              placeholder="(85) 99999-9999"
              maxLength={30}
              value={telefone}
              onChange={(event) => setTelefone(event.target.value)}
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
          <span>{enviando ? 'Salvando…' : 'Concluir e entrar'}</span>
        </button>
      </form>
    </AuthLayout>
  )
}
