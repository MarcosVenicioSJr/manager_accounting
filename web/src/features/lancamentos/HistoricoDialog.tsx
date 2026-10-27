import { useEffect, useState } from 'react'
import { AlertIcon } from '../../components/icons'
import { Dialog } from '../../components/ui/Dialog'
import { getErrorMessage } from '../../services/errors'
import { formatarDataHora } from '../clientes/formatar'
import { formatarDia, formatarMoeda } from './formatar'
import { diferencas } from './historico'
import { buscarHistorico, listarCategorias } from './lancamentoService'
import { ROTULO_TIPO, type Categoria, type ItemHistorico, type Lancamento } from './types'

const ACAO = { CRIAR: 'Lançado', EDITAR: 'Alterado', EXCLUIR: 'Excluído' } as const

type Estado = { tipo: 'carregando' } | { tipo: 'erro'; mensagem: string } | { tipo: 'ok'; itens: ItemHistorico[]; categorias: Categoria[] }

// Quem lançou e quem alterou o quê, do mais recente para o mais antigo
export function HistoricoDialog({ lancamento, onClose }: { lancamento: Lancamento | null; onClose: () => void }) {
  if (!lancamento) return null
  return <Historico key={lancamento.id} lancamento={lancamento} onClose={onClose} />
}

function Historico({ lancamento, onClose }: { lancamento: Lancamento; onClose: () => void }) {
  const [estado, setEstado] = useState<Estado>({ tipo: 'carregando' })

  useEffect(() => {
    const controle = new AbortController()
    Promise.all([buscarHistorico(lancamento.id, controle.signal), listarCategorias()])
      .then(([itens, categorias]) => setEstado({ tipo: 'ok', itens, categorias }))
      .catch((error: unknown) => {
        if (!controle.signal.aborted) {
          setEstado({ tipo: 'erro', mensagem: getErrorMessage(error, 'Não foi possível carregar o histórico.') })
        }
      })
    return () => controle.abort()
  }, [lancamento.id])

  return (
    <Dialog
      open
      title="Histórico do lançamento"
      onClose={onClose}
      largura="larga"
      actions={
        <button type="button" className="ui-btn ui-btn--primary" onClick={onClose} autoFocus>
          Fechar
        </button>
      }
    >
      <p className="lc-historico__sobre">{lancamento.descricao}</p>
      {estado.tipo === 'carregando' ? (
        <div className="lc-historico__skeleton" role="status" aria-label="Carregando histórico">
          <span className="ui-skeleton" style={{ height: 48 }} />
          <span className="ui-skeleton" style={{ height: 48 }} />
        </div>
      ) : estado.tipo === 'erro' ? (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">{estado.mensagem}</div>
        </div>
      ) : (
        <ol className="lc-historico">
          {estado.itens
            .map((item, i) => ({ item, anterior: i > 0 ? estado.itens[i - 1] : null }))
            .reverse()
            .map(({ item, anterior }) => {
              const mudancas = anterior ? diferencas(anterior.dados, item.dados, estado.categorias) : []
              const quem = item.origem === 'CLIENTE' ? 'Cliente, pelo app' : (item.usuarioNome ?? 'Escritório')
              return (
                <li key={item.id} className="lc-historico__item" data-acao={item.acao}>
                  <div className="lc-historico__topo">
                    <strong>{ACAO[item.acao]}</strong>
                    <span>
                      {quem} · {formatarDataHora(item.feitoEm)}
                    </span>
                  </div>
                  {item.acao === 'CRIAR' ? (
                    <p className="lc-historico__resumo">
                      {ROTULO_TIPO[item.dados.tipo]} de {formatarMoeda(item.dados.valor)} em {formatarDia(item.dados.data)}
                      {item.dados.comprovante ? `, com comprovante (${item.dados.comprovante})` : ''}
                    </p>
                  ) : mudancas.length > 0 ? (
                    <ul className="lc-historico__mudancas">
                      {mudancas.map((m) => (
                        <li key={m.campo}>
                          <span className="lc-historico__campo">{m.campo}</span>
                          <span className="lc-historico__antes">{m.antes}</span>
                          <span aria-hidden="true">→</span>
                          <span className="sr-only">passou a ser</span>
                          <span className="lc-historico__depois">{m.depois}</span>
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </li>
              )
            })}
        </ol>
      )}
    </Dialog>
  )
}
