import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AlertIcon, ArrowLeftIcon, BanIcon, RefreshIcon } from '../../components/icons'
import { ConfirmDialog } from '../../components/ui/Dialog'
import { useToast } from '../../components/ui/useToast'
import { AcessoAppCard } from '../../features/clientes/AcessoAppCard'
import { ClienteForm } from '../../features/clientes/ClienteForm'
import { valoresDoCliente } from '../../features/clientes/clienteFormValores'
import {
  atualizarCliente,
  buscarCliente,
  desativarCliente,
  reativarCliente,
} from '../../features/clientes/clienteService'
import { formatarDocumento } from '../../features/clientes/documento'
import { formatarDataHora } from '../../features/clientes/formatar'
import type { Cliente } from '../../features/clientes/types'
import '../../features/clientes/clientes.css'
import { getErrorMessage, getErrorStatus } from '../../services/errors'
import { NaoEncontradoPage } from '../nao-encontrado/NaoEncontradoPage'

type Estado =
  | { tipo: 'carregando' }
  | { tipo: 'erro'; mensagem: string }
  | { tipo: 'nao-encontrado' }
  | { tipo: 'ok'; cliente: Cliente }

// A página recomeça do zero (estado "carregando") ao trocar de cliente.
export function ClienteDetalhePage() {
  const { id } = useParams()
  if (!id || !/^\d+$/.test(id)) return <ClienteNaoEncontrado />
  return <ClienteDetalhe key={id} id={id} />
}

function ClienteNaoEncontrado() {
  return (
    <NaoEncontradoPage
      titulo="Cliente não encontrado"
      texto="Ele não existe ou o link está errado."
      voltarPara="/app/clientes"
      voltarRotulo="Ver clientes"
    />
  )
}

// Detalhe do cliente: edição dos dados, acesso ao app e desativar/reativar.
function ClienteDetalhe({ id }: { id: string }) {
  const { notify } = useToast()
  const [estado, setEstado] = useState<Estado>({ tipo: 'carregando' })
  const [confirmarStatus, setConfirmarStatus] = useState(false)
  const [mudandoStatus, setMudandoStatus] = useState(false)
  // "Recarregar cliente" (conflito de edição) descarta o que foi digitado, mesmo se a versão for a mesma
  const [recargas, setRecargas] = useState(0)

  // Recarrega sob demanda (depois de salvar, gerar token, mudar a situação ou num conflito de edição)
  const carregar = useCallback(async () => {
    try {
      setEstado({ tipo: 'ok', cliente: await buscarCliente(id) })
    } catch (error) {
      setEstado(estadoDeErro(error))
    }
  }, [id])

  // Carga inicial
  useEffect(() => {
    const controle = new AbortController()
    buscarCliente(id, controle.signal)
      .then((cliente) => setEstado({ tipo: 'ok', cliente }))
      .catch((error: unknown) => {
        if (!controle.signal.aborted) setEstado(estadoDeErro(error))
      })
    return () => controle.abort()
  }, [id])

  const cliente = estado.tipo === 'ok' ? estado.cliente : null

  if (estado.tipo === 'nao-encontrado') {
    return <ClienteNaoEncontrado />
  }

  if (estado.tipo === 'erro') {
    return (
      <>
        <VoltarParaClientes />
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">
            {estado.mensagem}
            <div className="ui-alert__actions">
              <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={() => void carregar()}>
                Tentar novamente
              </button>
            </div>
          </div>
        </div>
      </>
    )
  }

  if (!cliente) {
    return (
      <>
        <VoltarParaClientes />
        <div className="cl-detail-loading" role="status" aria-label="Carregando cliente">
          <span className="ui-skeleton" style={{ width: 280, height: 28 }} />
          <span className="ui-skeleton" style={{ width: 180 }} />
          <span className="ui-skeleton" style={{ width: '100%', height: 220, marginTop: 24 }} />
        </div>
      </>
    )
  }

  async function mudarStatus() {
    if (!cliente) return
    setMudandoStatus(true)
    try {
      if (cliente.ativo) {
        await desativarCliente(cliente.id)
        notify('Cliente desativado.')
      } else {
        await reativarCliente(cliente.id)
        notify('Cliente reativado.')
      }
      setConfirmarStatus(false)
      await carregar()
    } catch (error) {
      setConfirmarStatus(false)
      notify(getErrorMessage(error, 'Não foi possível alterar a situação do cliente.'), 'error')
    } finally {
      setMudandoStatus(false)
    }
  }

  return (
    <>
      <VoltarParaClientes />
      <div className="page-head">
        <div className="cl-detail-title">
          <h1 className="page-head__title">{cliente.nome}</h1>
          <p className="page-head__subtitle">
            <span className="cl-mono">{formatarDocumento(cliente.documento, cliente.tipoPessoa)}</span>
            <span aria-hidden="true"> · </span>
            {cliente.tipoPessoa === 'JURIDICA' ? 'Pessoa jurídica' : 'Pessoa física'}
            {' '}
            {cliente.ativo ? (
              <span className="badge badge--ok">Ativo</span>
            ) : (
              <span className="badge badge--off">Inativo</span>
            )}
          </p>
        </div>
        <div className="page-head__actions">
          {cliente.ativo ? (
            <button type="button" className="ui-btn ui-btn--danger-outline" onClick={() => setConfirmarStatus(true)}>
              <BanIcon width={18} height={18} />
              Desativar cliente
            </button>
          ) : (
            <button type="button" className="ui-btn ui-btn--secondary" onClick={() => setConfirmarStatus(true)}>
              <RefreshIcon width={18} height={18} />
              Reativar cliente
            </button>
          )}
        </div>
      </div>

      <div className="cl-detail">
        <ClienteForm
          key={`${cliente.id}-${cliente.versao}-${recargas}`}
          valoresIniciais={valoresDoCliente(cliente)}
          submitLabel="Salvar alterações"
          exigirAlteracao
          onRecarregar={() => {
            setRecargas((n) => n + 1)
            void carregar()
          }}
          aviso={
            !cliente.ativo && (
              <div className="ui-alert ui-alert--warning">
                <AlertIcon />
                <div className="ui-alert__content">
                  Este cliente está inativo: não aparece na lista de ativos e não tem acesso ao app.
                </div>
              </div>
            )
          }
          onSubmit={async (dados) => {
            const salvo = await atualizarCliente(cliente.id, { ...dados, versao: cliente.versao })
            setEstado({ tipo: 'ok', cliente: salvo })
            notify('Alterações salvas.')
          }}
        />

        <aside className="cl-detail__side">
          <AcessoAppCard cliente={cliente} onAlterado={() => void carregar()} />
          <section className="panel side-card" aria-labelledby="registro-titulo">
            <h2 id="registro-titulo" className="side-card__title">
              Registro
            </h2>
            <dl className="side-card__meta">
              <div>
                <dt>Cadastrado em</dt>
                <dd>{formatarDataHora(cliente.dataInclusao)}</dd>
              </div>
              <div>
                <dt>Última alteração</dt>
                <dd>{formatarDataHora(cliente.dataAtualizacao)}</dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>

      <ConfirmDialog
        open={confirmarStatus}
        title={cliente.ativo ? 'Desativar este cliente?' : 'Reativar este cliente?'}
        confirmLabel={cliente.ativo ? 'Desativar cliente' : 'Reativar cliente'}
        tone={cliente.ativo ? 'danger' : 'primary'}
        busy={mudandoStatus}
        onConfirm={() => void mudarStatus()}
        onClose={() => setConfirmarStatus(false)}
      >
        {cliente.ativo ? (
          <>
            <p>
              <strong>{cliente.nome}</strong> sai da lista de ativos
              {cliente.acessoApp.ativo ? ' e perde o acesso ao app na hora' : ''}. Os dados continuam guardados.
            </p>
            <p>Você pode reativar depois{cliente.acessoApp.ativo ? ' (o acesso ao app precisará de um novo token)' : ''}.</p>
          </>
        ) : (
          <p>
            <strong>{cliente.nome}</strong> volta para a lista de ativos. O acesso ao app não volta sozinho: gere
            um novo token se o cliente precisar.
          </p>
        )}
      </ConfirmDialog>
    </>
  )
}

function estadoDeErro(error: unknown): Estado {
  return getErrorStatus(error) === 404
    ? { tipo: 'nao-encontrado' }
    : { tipo: 'erro', mensagem: getErrorMessage(error, 'Não foi possível carregar o cliente.') }
}

function VoltarParaClientes() {
  return (
    <Link to="/app/clientes" className="page-back">
      <ArrowLeftIcon width={16} height={16} />
      Clientes
    </Link>
  )
}
