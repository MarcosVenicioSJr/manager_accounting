import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FullPageLoading } from '../../components/FullPageLoading'
import { AuthLayout } from '../../features/auth/AuthLayout'
import { AlertIcon, CheckCircleIcon, MailIcon, SpinnerIcon } from '../../features/auth/icons'
import { useAuth } from '../../features/auth/useAuth'
import { confirmarAlteracao, validarConfirmacao, type ConfirmacaoInfo } from '../../features/perfil/perfilService'
import { getErrorMessage } from '../../services/errors'

type Etapa = 'validando' | 'pergunta' | 'invalido' | 'concluido'

const MENSAGEM_LINK_INVALIDO =
  'Este link de confirmação é inválido ou expirou. Faça o pedido de novo no Meu perfil.'

// Link do e-mail: /confirmar-alteracao#token=... (o que vem depois do "#" não vai para o servidor do front).
function lerTokenDaUrl(): string {
  return new URLSearchParams(window.location.hash.slice(1)).get('token') ?? ''
}

// Tela aberta pelo link do e-mail do Meu perfil. Abrir o link não altera nada: a troca só acontece no clique
// em "Confirmar" (leitores de e-mail que abrem links sozinhos não confirmam nada por engano).
export function ConfirmarAlteracaoPage() {
  const { status, logout } = useAuth()
  const [token] = useState(lerTokenDaUrl)
  const [etapa, setEtapa] = useState<Etapa>(token ? 'validando' : 'invalido')
  const [info, setInfo] = useState<ConfirmacaoInfo | null>(null)
  const [mensagemInvalido, setMensagemInvalido] = useState(MENSAGEM_LINK_INVALIDO)
  const [confirmando, setConfirmando] = useState(false)
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
    validarConfirmacao(token)
      .then((dados) => {
        if (cancelado) return
        setInfo(dados)
        setEtapa('pergunta')
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

  async function confirmar() {
    setErro(null)
    setConfirmando(true)
    try {
      const resultado = await confirmarAlteracao(token)
      // Troca de senha encerra todas as sessões: limpa também a deste navegador
      if (resultado.tipo === 'SENHA') await logout()
      setEtapa('concluido')
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível confirmar. Tente novamente.'))
    } finally {
      setConfirmando(false)
    }
  }

  if (etapa === 'validando') return <FullPageLoading />

  const ehSenha = info?.tipo === 'SENHA'
  const logado = status === 'authenticated'

  if (etapa === 'invalido') {
    return (
      <AuthLayout>
        <div className="auth-form" role="alert">
          <div className="auth-status-icon auth-status-icon--erro">
            <AlertIcon />
          </div>
          <h1 className="auth-title">Link inválido</h1>
          <p className="auth-subtitle auth-subtitle--wide">{mensagemInvalido}</p>
          <Link className="auth-submit auth-submit--spaced" to={logado ? '/app/perfil' : '/login'}>
            <span>{logado ? 'Ir para o Meu perfil' : 'Ir para o login'}</span>
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
          <h1 className="auth-title">{ehSenha ? 'Senha alterada' : 'E-mail alterado'}</h1>
          <p className="auth-subtitle auth-subtitle--wide">
            {ehSenha
              ? 'Sua nova senha já está valendo. Por segurança, todas as sessões foram encerradas: entre de novo com a nova senha.'
              : `A partir de agora, os avisos e a recuperação de senha vão para ${info?.novoEmail}. O endereço antigo recebeu um aviso da troca.`}
          </p>
          <Link
            className="auth-submit auth-submit--spaced"
            to={ehSenha ? '/login' : logado ? '/app/perfil' : '/login'}
          >
            <span>{ehSenha ? 'Entrar com a nova senha' : logado ? 'Voltar para o Meu perfil' : 'Ir para o login'}</span>
          </Link>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <div className="auth-form">
        <div className="auth-status-icon">
          <MailIcon />
        </div>
        <h1 className="auth-title">{ehSenha ? 'Confirmar troca de senha' : 'Confirmar novo e-mail'}</h1>
        <p className="auth-subtitle auth-subtitle--wide">
          {ehSenha ? (
            <>
              Você pediu no Meu perfil para trocar a sua senha. Ao confirmar, a nova senha passa a valer e todas as
              sessões abertas são encerradas.
            </>
          ) : (
            <>
              Ao confirmar, <strong>{info?.novoEmail}</strong> passa a ser o seu e-mail de acesso, usado nos avisos
              e na recuperação de senha.
            </>
          )}
        </p>

        {erro && (
          <div className="auth-alert" role="alert">
            <AlertIcon />
            <span>{erro}</span>
          </div>
        )}

        <button
          type="button"
          className="auth-submit auth-submit--spaced"
          onClick={() => void confirmar()}
          disabled={confirmando}
          aria-busy={confirmando}
        >
          {confirmando && <SpinnerIcon />}
          <span>{confirmando ? 'Confirmando…' : ehSenha ? 'Confirmar troca de senha' : 'Confirmar novo e-mail'}</span>
        </button>

        <p className="auth-note">
          <strong>Não foi você?</strong> Não confirme. Nada muda, e o link expira sozinho.{' '}
          {ehSenha ? 'Como o pedido exige a senha atual, troque-a e avise o seu escritório.' : ''}
        </p>
      </div>
    </AuthLayout>
  )
}
