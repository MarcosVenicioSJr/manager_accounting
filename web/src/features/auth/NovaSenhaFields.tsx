import { useState } from 'react'
import { EyeIcon, EyeOffIcon } from './icons'
import { checarSenha } from './passwordPolicy'

type Props = {
  novaSenha: string
  onNovaSenhaChange: (valor: string) => void
  confirmacao: string
  onConfirmacaoChange: (valor: string) => void
  autoFocus?: boolean
}

// Campos "Nova senha" + "Confirmar nova senha" com a lista de regras da política (primeiro acesso e
// redefinição de senha usam os mesmos).
export function NovaSenhaFields({ novaSenha, onNovaSenhaChange, confirmacao, onConfirmacaoChange, autoFocus }: Props) {
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const checks = checarSenha(novaSenha)
  const senhasConferem = novaSenha === confirmacao

  const regras: { ok: boolean; texto: string }[] = [
    { ok: checks.tamanho, texto: 'Pelo menos 8 caracteres' },
    { ok: checks.letra, texto: 'Letras' },
    { ok: checks.numero, texto: 'Números' },
    { ok: checks.simbolo, texto: 'Símbolos (por exemplo ! @ # $ %)' },
  ]

  return (
    <>
      <div>
        <label className="auth-label" htmlFor="novaSenha">
          Nova senha
        </label>
        <div className="auth-field auth-field--with-action">
          <input
            id="novaSenha"
            className="auth-input"
            type={mostrarSenha ? 'text' : 'password'}
            autoComplete="new-password"
            autoFocus={autoFocus}
            value={novaSenha}
            onChange={(event) => onNovaSenhaChange(event.target.value)}
            aria-describedby="regras-senha"
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
        <ul id="regras-senha" className="auth-rules">
          {regras.map((regra) => (
            <li key={regra.texto} className={`auth-rules__item${regra.ok ? ' auth-rules__item--ok' : ''}`}>
              {regra.texto}
            </li>
          ))}
        </ul>
        {!checks.limite && <p className="auth-field-error">A senha é longa demais (máximo de 72 bytes).</p>}
      </div>

      <div>
        <label className="auth-label" htmlFor="confirmacao">
          Confirmar nova senha
        </label>
        <div className="auth-field">
          <input
            id="confirmacao"
            className="auth-input"
            type={mostrarSenha ? 'text' : 'password'}
            autoComplete="new-password"
            value={confirmacao}
            onChange={(event) => onConfirmacaoChange(event.target.value)}
            aria-invalid={confirmacao.length > 0 && !senhasConferem}
          />
        </div>
        {confirmacao.length > 0 && !senhasConferem && (
          <p className="auth-field-error">As senhas não conferem.</p>
        )}
      </div>
    </>
  )
}
