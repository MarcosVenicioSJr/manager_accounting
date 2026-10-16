import { useState, type FormEvent } from 'react'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { checarSenha, senhaValida } from '../../features/auth/passwordPolicy'
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
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const checks = checarSenha(novaSenha)
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

  const regras: { ok: boolean; texto: string }[] = [
    { ok: checks.tamanho, texto: 'Pelo menos 8 caracteres' },
    { ok: checks.letra, texto: 'Letras' },
    { ok: checks.numero, texto: 'Números' },
    { ok: checks.simbolo, texto: 'Símbolos (por exemplo ! @ # $ %)' },
  ]

  return (
    <AuthLayout
      title="Primeiro acesso"
      subtitle="Defina uma nova senha e complete seus dados para continuar."
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label className="field__label" htmlFor="nomeCompleto">
            Nome completo
          </label>
          <input
            id="nomeCompleto"
            className="field__input"
            type="text"
            autoComplete="name"
            required
            autoFocus
            maxLength={150}
            value={nomeCompleto}
            onChange={(event) => setNomeCompleto(event.target.value)}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="email">
            E-mail
          </label>
          <input
            id="email"
            className="field__input"
            type="email"
            autoComplete="email"
            required
            maxLength={150}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="telefone">
            Telefone
          </label>
          <input
            id="telefone"
            className="field__input"
            type="tel"
            autoComplete="tel"
            placeholder="(85) 99999-9999"
            required
            maxLength={30}
            value={telefone}
            onChange={(event) => setTelefone(event.target.value)}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="novaSenha">
            Nova senha
          </label>
          <div className="password-field">
            <input
              id="novaSenha"
              className="field__input"
              type={mostrarSenha ? 'text' : 'password'}
              autoComplete="new-password"
              required
              value={novaSenha}
              onChange={(event) => setNovaSenha(event.target.value)}
              aria-describedby="regras-senha"
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
          <ul id="regras-senha" className="password-rules">
            {regras.map((regra) => (
              <li
                key={regra.texto}
                className={`password-rules__item${regra.ok ? ' password-rules__item--ok' : ''}`}
              >
                {regra.texto}
              </li>
            ))}
          </ul>
          {!checks.limite && <p className="form-error">A senha é longa demais (máximo de 72 bytes).</p>}
        </div>

        <div className="field">
          <label className="field__label" htmlFor="confirmacao">
            Confirmar nova senha
          </label>
          <input
            id="confirmacao"
            className="field__input"
            type={mostrarSenha ? 'text' : 'password'}
            autoComplete="new-password"
            required
            value={confirmacao}
            onChange={(event) => setConfirmacao(event.target.value)}
          />
          {confirmacao.length > 0 && !senhasConferem && (
            <p className="form-error">As senhas não conferem.</p>
          )}
        </div>

        {erro && (
          <p className="form-error" role="alert">
            {erro}
          </p>
        )}

        <button className="btn btn--primary" type="submit" disabled={enviando || !formularioValido}>
          {enviando ? 'Salvando…' : 'Concluir e entrar'}
        </button>
      </form>
    </AuthLayout>
  )
}
