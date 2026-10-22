import { useState } from 'react'
import { AlertIcon, CheckIcon, CopyIcon, KeyIcon, PhoneIcon } from '../../components/icons'
import { ConfirmDialog, Dialog } from '../../components/ui/Dialog'
import { useToast } from '../../components/ui/useToast'
import { getErrorMessage } from '../../services/errors'
import { useAuth } from '../auth/useAuth'
import { gerarAcessoApp, revogarAcessoApp } from './clienteService'
import { formatarDataHora } from './formatar'
import type { Cliente, TokenGerado } from './types'

type AcessoAppCardProps = {
  cliente: Cliente
  // Recarrega o cliente depois de gerar ou revogar (atualiza a situação do acesso)
  onAlterado: () => void
}

type Confirmacao = 'regerar' | 'revogar' | null

// Acesso do cliente ao app: gera o token (mostrado uma única vez), gera de novo ou revoga.
export function AcessoAppCard({ cliente, onAlterado }: AcessoAppCardProps) {
  const { notify } = useToast()
  const [confirmacao, setConfirmacao] = useState<Confirmacao>(null)
  const [processando, setProcessando] = useState(false)
  const [tokenGerado, setTokenGerado] = useState<TokenGerado | null>(null)
  const acesso = cliente.acessoApp
  // Revogar é só do administrador; gerar (e gerar de novo) vale para todos
  const ehAdmin = useAuth().usuario?.perfil === 'ADMIN'

  async function gerar() {
    setProcessando(true)
    try {
      const novo = await gerarAcessoApp(cliente.id)
      setConfirmacao(null)
      setTokenGerado(novo)
      onAlterado()
    } catch (error) {
      setConfirmacao(null)
      notify(getErrorMessage(error, 'Não foi possível gerar o acesso. Tente novamente.'), 'error')
    } finally {
      setProcessando(false)
    }
  }

  async function revogar() {
    setProcessando(true)
    try {
      await revogarAcessoApp(cliente.id)
      notify('Acesso ao app revogado.')
      onAlterado()
    } catch (error) {
      notify(getErrorMessage(error, 'Não foi possível revogar o acesso. Tente novamente.'), 'error')
    } finally {
      setConfirmacao(null)
      setProcessando(false)
    }
  }

  return (
    <section className="panel side-card" aria-labelledby="acesso-app-titulo">
      <div className="side-card__head">
        <span className="side-card__icon" aria-hidden="true">
          <PhoneIcon />
        </span>
        <h2 id="acesso-app-titulo" className="side-card__title">
          Acesso ao app
        </h2>
      </div>

      {!cliente.ativo ? (
        <p className="side-card__text">
          Cliente inativo. {ehAdmin ? 'Reative o cliente' : 'Um administrador precisa reativar o cliente'} para
          liberar o acesso ao app.
        </p>
      ) : acesso.ativo ? (
        <>
          <p className="side-card__status side-card__status--ok">
            <CheckIcon width={18} height={18} />
            Acesso liberado
          </p>
          <dl className="side-card__meta">
            <div>
              <dt>Token gerado em</dt>
              <dd>{formatarDataHora(acesso.geradoEm)}</dd>
            </div>
            <div>
              <dt>Último uso no app</dt>
              <dd>{acesso.ultimoUsoEm ? formatarDataHora(acesso.ultimoUsoEm) : 'Ainda não usado'}</dd>
            </div>
          </dl>
          <p className="side-card__text side-card__text--muted">
            Por segurança o token não fica visível. Se o cliente perdeu, gere um novo.
          </p>
          <div className="side-card__actions">
            <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={() => setConfirmacao('regerar')}>
              <KeyIcon width={16} height={16} />
              Gerar novo token
            </button>
            {ehAdmin && (
              <button
                type="button"
                className="ui-btn ui-btn--danger-outline ui-btn--sm"
                onClick={() => setConfirmacao('revogar')}
              >
                Revogar acesso
              </button>
            )}
          </div>
        </>
      ) : (
        <>
          <p className="side-card__text">
            O cliente ainda não tem acesso ao app. Gere um token e envie para ele entrar e lançar compras, dívidas
            e despesas.
          </p>
          <button
            type="button"
            className="ui-btn ui-btn--primary side-card__cta"
            onClick={() => void gerar()}
            disabled={processando}
            aria-busy={processando}
          >
            <KeyIcon width={18} height={18} />
            {processando ? 'Gerando…' : 'Gerar token de acesso'}
          </button>
        </>
      )}

      <ConfirmDialog
        open={confirmacao === 'regerar'}
        title="Gerar um novo token?"
        confirmLabel="Gerar novo token"
        busy={processando}
        onConfirm={() => void gerar()}
        onClose={() => setConfirmacao(null)}
      >
        <p>O token atual deixa de funcionar na hora. O cliente vai precisar entrar no app com o novo.</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={confirmacao === 'revogar'}
        title="Revogar o acesso ao app?"
        confirmLabel="Revogar acesso"
        tone="danger"
        busy={processando}
        onConfirm={() => void revogar()}
        onClose={() => setConfirmacao(null)}
      >
        <p>
          <strong>{cliente.nome}</strong> deixa de conseguir entrar no app. Você pode gerar um novo token depois.
        </p>
      </ConfirmDialog>

      <TokenGeradoDialog token={tokenGerado} onClose={() => setTokenGerado(null)} />
    </section>
  )
}

// Mostra o token recém-gerado com o botão de copiar. Ao fechar, ele não pode mais ser visto.
function TokenGeradoDialog({ token, onClose }: { token: TokenGerado | null; onClose: () => void }) {
  const [copiado, setCopiado] = useState(false)
  const [erroCopia, setErroCopia] = useState(false)

  async function copiar() {
    if (!token) return
    try {
      await navigator.clipboard.writeText(token.token)
      setCopiado(true)
      setErroCopia(false)
    } catch {
      // Sem permissão de área de transferência (ou fora de HTTPS): a pessoa copia selecionando o texto
      setErroCopia(true)
    }
  }

  function fechar() {
    setCopiado(false)
    setErroCopia(false)
    onClose()
  }

  return (
    <Dialog
      open={token !== null}
      title="Token de acesso gerado"
      onClose={fechar}
      fecharPeloFundo={false}
      actions={
        <button type="button" className="ui-btn ui-btn--primary" onClick={fechar}>
          Já copiei, fechar
        </button>
      }
    >
      <p>Envie este token ao cliente por um canal de confiança. Ele digita o token no app para entrar.</p>
      <div className="token-box">
        <code className="token-box__value" aria-label="Token de acesso">
          {token?.token}
        </code>
        <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={() => void copiar()}>
          {copiado ? <CheckIcon width={16} height={16} /> : <CopyIcon width={16} height={16} />}
          {copiado ? 'Copiado' : 'Copiar'}
        </button>
      </div>
      {erroCopia && <p className="cf-hint">Não foi possível copiar automaticamente. Selecione o token e copie.</p>}
      <div className="ui-alert ui-alert--warning">
        <AlertIcon />
        <div className="ui-alert__content">
          Este token aparece <strong>só agora</strong>. Depois de fechar, não dá para ver de novo; se precisar,
          gere outro.
        </div>
      </div>
    </Dialog>
  )
}
