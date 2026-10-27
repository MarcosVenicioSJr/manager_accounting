import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  AlertIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  CloseIcon,
  HistoryIcon,
  PaperclipIcon,
  PencilIcon,
  PlusIcon,
  ReceiptIcon,
  SearchIcon,
  TrashIcon,
} from '../../components/icons'
import { ConfirmDialog } from '../../components/ui/Dialog'
import { RowMenu } from '../../components/ui/RowMenu'
import { useToast } from '../../components/ui/useToast'
import { getErrorCode, getErrorMessage } from '../../services/errors'
import { pluralizar } from '../clientes/formatar'
import type { ClienteEscolhido } from './ClienteSeletor'
import { ComprovanteDialog } from './ComprovanteDialog'
import { formatarDia, formatarMoeda, hojeIso, mesDe } from './formatar'
import { HistoricoDialog } from './HistoricoDialog'
import { LancamentoDialog } from './LancamentoDialog'
import { excluirLancamento, listarCategorias, listarLancamentos } from './lancamentoService'
import {
  ROTULO_TIPO,
  TIPOS,
  type Categoria,
  type FiltroLancamentos,
  type Lancamento,
  type ListaLancamentos,
  type TipoLancamento,
} from './types'
import './lancamentos.css'

const TAMANHO_PAGINA = 20
const ESPERA_BUSCA_MS = 300
const DATA = /^\d{4}-\d{2}-\d{2}$/

type Props = {
  // Aba do cliente: lista só os lançamentos dele e o formulário já vem com o cliente
  cliente?: ClienteEscolhido & { ativo: boolean }
  // Cabeçalho da página (a página geral põe o botão "Novo lançamento" no topo)
  cabecalho?: (abrirNovo: () => void) => ReactNode
}

function lerTipo(valor: string | null): TipoLancamento | undefined {
  return valor === 'COMPRA' || valor === 'DIVIDA' || valor === 'DESPESA' ? valor : undefined
}

function lerData(valor: string | null): string | undefined {
  return valor && DATA.test(valor) ? valor : undefined
}

// Lista de lançamentos com filtros (na URL, para voltar e compartilhar com a mesma visão), totais por tipo,
// e as ações de cada linha: editar, ver comprovante, histórico e excluir.
export function LancamentosPainel({ cliente, cabecalho }: Props) {
  const { notify } = useToast()
  const [params, setParams] = useSearchParams()
  const filtro: FiltroLancamentos = {
    clienteId: cliente?.id,
    tipo: lerTipo(params.get('tipo')),
    categoria: params.get('categoria') || undefined,
    de: lerData(params.get('de')),
    ate: lerData(params.get('ate')),
    busca: params.get('busca') || undefined,
  }
  const pagina = Math.max(0, Number(params.get('pagina')) || 0)

  const [textoBusca, setTextoBusca] = useState(filtro.busca ?? '')
  const [buscaDaUrl, setBuscaDaUrl] = useState(filtro.busca ?? '')
  if ((filtro.busca ?? '') !== buscaDaUrl) {
    setBuscaDaUrl(filtro.busca ?? '')
    setTextoBusca(filtro.busca ?? '')
  }

  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [tentativa, setTentativa] = useState(0)
  const chave = JSON.stringify([filtro, pagina, tentativa])
  const [resposta, setResposta] = useState<{ chave: string; dados: ListaLancamentos } | null>(null)
  const [falha, setFalha] = useState<{ chave: string; mensagem: string } | null>(null)
  const resultado = resposta?.dados ?? null
  const erro = falha?.chave === chave ? falha.mensagem : null
  const carregando = resposta?.chave !== chave && !erro

  const [formulario, setFormulario] = useState<{ aberto: boolean; lancamento: Lancamento | null }>({
    aberto: false,
    lancamento: null,
  })
  const [verComprovante, setVerComprovante] = useState<Lancamento | null>(null)
  const [verHistorico, setVerHistorico] = useState<Lancamento | null>(null)
  const [excluir, setExcluir] = useState<Lancamento | null>(null)
  const [excluindo, setExcluindo] = useState(false)

  const atualizarParams = useCallback(
    (mudancas: Record<string, string | null>) => {
      setParams(
        (atual) => {
          const proximo = new URLSearchParams(atual)
          for (const [nome, valor] of Object.entries(mudancas)) {
            if (valor === null || valor === '') proximo.delete(nome)
            else proximo.set(nome, valor)
          }
          return proximo
        },
        { replace: true },
      )
    },
    [setParams],
  )

  useEffect(() => {
    listarCategorias()
      .then(setCategorias)
      .catch(() => setCategorias([]))
  }, [])

  // Espera a pessoa parar de digitar antes de buscar
  useEffect(() => {
    if (textoBusca.trim() === (filtro.busca ?? '')) return
    const timer = window.setTimeout(() => {
      atualizarParams({ busca: textoBusca.trim() || null, pagina: null })
    }, ESPERA_BUSCA_MS)
    return () => window.clearTimeout(timer)
  }, [textoBusca, filtro.busca, atualizarParams])

  useEffect(() => {
    const controle = new AbortController()
    const [f, p] = JSON.parse(chave) as [FiltroLancamentos, number]
    listarLancamentos(f, p, TAMANHO_PAGINA, controle.signal)
      .then((dados) => {
        if (dados.itens.length === 0 && dados.pagina > 0 && dados.totalPaginas > 0) {
          atualizarParams({ pagina: dados.totalPaginas > 1 ? String(dados.totalPaginas - 1) : null })
          return
        }
        setResposta({ chave, dados })
      })
      .catch((error: unknown) => {
        if (controle.signal.aborted) return
        setFalha({ chave, mensagem: getErrorMessage(error, 'Não foi possível carregar os lançamentos.') })
      })
    return () => controle.abort()
  }, [chave, atualizarParams])

  const recarregar = () => setTentativa((n) => n + 1)
  const abrirNovo = () => setFormulario({ aberto: true, lancamento: null })
  const rotuloCategoria = (valor: string) => categorias.find((c) => c.valor === valor)?.rotulo ?? valor

  async function confirmarExclusao() {
    if (!excluir) return
    setExcluindo(true)
    try {
      await excluirLancamento(excluir.id, excluir.versao)
      notify('Lançamento excluído.')
    } catch (error) {
      notify(
        getErrorCode(error) === 'EDICAO_SIMULTANEA'
          ? 'Este lançamento mudou desde que a lista foi carregada. Confira e tente de novo.'
          : getErrorMessage(error, 'Não foi possível excluir o lançamento.'),
        'error',
      )
    } finally {
      setExcluindo(false)
      setExcluir(null)
      recarregar()
    }
  }

  const hoje = hojeIso()
  const esteMes = mesDe(hoje)
  const [anoAtual, mesAtual] = hoje.split('-').map(Number)
  const mesPassado = mesDe(`${mesAtual === 1 ? anoAtual - 1 : anoAtual}-${String(mesAtual === 1 ? 12 : mesAtual - 1).padStart(2, '0')}-01`)
  const periodo =
    filtro.de === esteMes.de && filtro.ate === esteMes.ate
      ? 'mes'
      : filtro.de === mesPassado.de && filtro.ate === mesPassado.ate
        ? 'passado'
        : !filtro.de && !filtro.ate
          ? 'tudo'
          : 'outro'
  const comFiltro = Boolean(filtro.tipo || filtro.categoria || filtro.de || filtro.ate || filtro.busca)
  const itens = resultado?.itens ?? []
  const total = resultado?.total ?? 0
  const inicio = total === 0 ? 0 : pagina * TAMANHO_PAGINA + 1
  const fim = Math.min(total, (pagina + 1) * TAMANHO_PAGINA)
  const podeLancar = !cliente || cliente.ativo

  const botaoNovo = (
    <button type="button" className="ui-btn ui-btn--primary" onClick={abrirNovo} disabled={!podeLancar}>
      <PlusIcon width={18} height={18} />
      Novo lançamento
    </button>
  )

  return (
    <>
      {cabecalho?.(abrirNovo)}

      {cliente && (
        <div className="lc-secao-head">
          <div>
            <h2 className="lc-secao-head__titulo">Lançamentos</h2>
            <p className="lc-secao-head__texto">
              {podeLancar
                ? 'Compras, dívidas e despesas do cliente, lançadas pelo escritório ou pelo app.'
                : 'Cliente inativo: os lançamentos continuam aqui, mas não dá para lançar novos.'}
            </p>
          </div>
          {botaoNovo}
        </div>
      )}

      <div className="lc-totais" aria-label="Totais do filtro">
        {TIPOS.map((t) => {
          const dados = resultado?.totais.find((x) => x.tipo === t.valor)
          const selecionado = filtro.tipo === t.valor
          return (
            <button
              key={t.valor}
              type="button"
              className="panel lc-total"
              data-tipo={t.valor}
              aria-pressed={selecionado}
              onClick={() => atualizarParams({ tipo: selecionado ? null : t.valor, pagina: null })}
            >
              <span className="lc-total__rotulo">
                <span className="lc-ponto" data-tipo={t.valor} aria-hidden="true" />
                {t.plural}
              </span>
              {resultado ? (
                <>
                  <span className="lc-total__valor">{formatarMoeda(dados?.valor ?? 0)}</span>
                  <span className="lc-total__qtd">{pluralizar(dados?.quantidade ?? 0, 'lançamento', 'lançamentos')}</span>
                </>
              ) : (
                <span className="ui-skeleton" style={{ width: '70%', height: 26, marginTop: 6 }} />
              )}
            </button>
          )
        })}
        <div className="panel lc-total lc-total--soma">
          <span className="lc-total__rotulo">Total {filtro.tipo ? `(${ROTULO_TIPO[filtro.tipo].toLowerCase()})` : ''}</span>
          {resultado ? (
            <>
              <span className="lc-total__valor">{formatarMoeda(resultado.valorTotal)}</span>
              <span className="lc-total__qtd">{pluralizar(resultado.total, 'lançamento', 'lançamentos')}</span>
            </>
          ) : (
            <span className="ui-skeleton" style={{ width: '70%', height: 26, marginTop: 6 }} />
          )}
        </div>
      </div>

      <div className="lc-filtros">
        <div className="cl-search lc-filtros__busca">
          <label htmlFor="busca-lancamentos" className="sr-only">
            Buscar na descrição
          </label>
          <SearchIcon className="cl-search__icon" />
          <input
            id="busca-lancamentos"
            type="search"
            className="cl-search__input"
            placeholder="Buscar na descrição"
            value={textoBusca}
            onChange={(e) => setTextoBusca(e.target.value)}
            autoComplete="off"
          />
          {textoBusca && (
            <button
              type="button"
              className="cl-search__clear"
              onClick={() => {
                setTextoBusca('')
                atualizarParams({ busca: null, pagina: null })
              }}
              aria-label="Limpar busca"
            >
              <CloseIcon width={16} height={16} />
            </button>
          )}
        </div>

        <label className="sr-only" htmlFor="filtro-categoria">
          Categoria
        </label>
        <select
          id="filtro-categoria"
          className="cf-input lc-filtros__select"
          value={filtro.categoria ?? ''}
          onChange={(e) => atualizarParams({ categoria: e.target.value || null, pagina: null })}
        >
          <option value="">Todas as categorias</option>
          {categorias.map((c) => (
            <option key={c.valor} value={c.valor}>
              {c.rotulo}
            </option>
          ))}
        </select>

        <div className="cl-filter" role="group" aria-label="Período">
          {(
            [
              ['tudo', 'Tudo', null, null],
              ['mes', 'Este mês', esteMes.de, esteMes.ate],
              ['passado', 'Mês passado', mesPassado.de, mesPassado.ate],
            ] as const
          ).map(([valor, rotulo, de, ate]) => (
            <button
              key={valor}
              type="button"
              className="cl-filter__option"
              aria-pressed={periodo === valor}
              onClick={() => atualizarParams({ de, ate, pagina: null })}
            >
              {rotulo}
            </button>
          ))}
        </div>

        <div className="lc-periodo">
          <label className="lc-periodo__campo">
            <span>De</span>
            <input
              type="date"
              className="cf-input"
              value={filtro.de ?? ''}
              max={filtro.ate ?? hoje}
              onChange={(e) => atualizarParams({ de: e.target.value || null, pagina: null })}
            />
          </label>
          <label className="lc-periodo__campo">
            <span>Até</span>
            <input
              type="date"
              className="cf-input"
              value={filtro.ate ?? ''}
              min={filtro.de}
              onChange={(e) => atualizarParams({ ate: e.target.value || null, pagina: null })}
            />
          </label>
        </div>

        {comFiltro && (
          <button
            type="button"
            className="ui-btn ui-btn--ghost ui-btn--sm"
            onClick={() => {
              setTextoBusca('')
              atualizarParams({ tipo: null, categoria: null, de: null, ate: null, busca: null, pagina: null })
            }}
          >
            Limpar filtros
          </button>
        )}
      </div>

      {erro ? (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">
            {erro}
            <div className="ui-alert__actions">
              <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={recarregar}>
                Tentar novamente
              </button>
            </div>
          </div>
        </div>
      ) : carregando && !resultado ? (
        <div className="panel cl-loading" role="status" aria-label="Carregando lançamentos">
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="cl-loading__row">
              <span className="ui-skeleton" style={{ width: '12%' }} />
              <span className="ui-skeleton" style={{ width: '40%' }} />
              <span className="ui-skeleton" style={{ width: '12%' }} />
              <span className="ui-skeleton" style={{ width: '14%' }} />
            </div>
          ))}
        </div>
      ) : itens.length === 0 ? (
        <div className="panel cl-empty">
          <span className="cl-empty__icon" aria-hidden="true">
            <ReceiptIcon width={28} height={28} />
          </span>
          {comFiltro ? (
            <>
              <h2>Nenhum lançamento encontrado</h2>
              <p>Nada com esses filtros. Tente outro período ou limpe a busca.</p>
            </>
          ) : (
            <>
              <h2>Nenhum lançamento ainda</h2>
              <p>
                {cliente
                  ? 'As compras, dívidas e despesas deste cliente aparecem aqui, lançadas pelo escritório ou pelo app.'
                  : 'As compras, dívidas e despesas dos clientes aparecem aqui, lançadas pelo escritório ou pelo app.'}
              </p>
              {podeLancar && botaoNovo}
            </>
          )}
        </div>
      ) : (
        <div className="panel cl-table-wrap" aria-busy={carregando}>
          <table className="cl-table lc-table">
            <caption className="sr-only">Lançamentos</caption>
            <thead>
              <tr>
                <th scope="col">Data</th>
                <th scope="col">Descrição</th>
                <th scope="col">Tipo</th>
                <th scope="col" className="lc-num">
                  Valor
                </th>
                <th scope="col">
                  <span className="sr-only">Ações</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {itens.map((l) => (
                <tr key={l.id} onClick={() => setFormulario({ aberto: true, lancamento: l })}>
                  <td data-label="Data" className="lc-data">
                    {formatarDia(l.data)}
                  </td>
                  <td data-label="Descrição">
                    <button
                      type="button"
                      className="lc-descricao"
                      onClick={(e) => {
                        e.stopPropagation()
                        setFormulario({ aberto: true, lancamento: l })
                      }}
                    >
                      {l.descricao}
                    </button>
                    <span className="cl-sub lc-sub">
                      {rotuloCategoria(l.categoria)}
                      {!cliente && (
                        <>
                          {' · '}
                          <Link
                            to={`/app/clientes/${l.clienteId}`}
                            className="lc-cliente"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {l.clienteNome}
                          </Link>
                        </>
                      )}
                    </span>
                    <span className="lc-selos">
                      {l.origem === 'CLIENTE' && <span className="badge lc-badge-app">Pelo app</span>}
                      {l.editadoEm && <span className="badge">Editado</span>}
                      {l.comprovante && (
                        <button
                          type="button"
                          className="badge lc-badge-anexo"
                          onClick={(e) => {
                            e.stopPropagation()
                            setVerComprovante(l)
                          }}
                          aria-label={`Ver comprovante de ${l.descricao}`}
                        >
                          <PaperclipIcon width={13} height={13} />
                          Comprovante
                        </button>
                      )}
                    </span>
                  </td>
                  <td data-label="Tipo">
                    <span className="lc-tipo" data-tipo={l.tipo}>
                      <span className="lc-ponto" data-tipo={l.tipo} aria-hidden="true" />
                      {ROTULO_TIPO[l.tipo]}
                    </span>
                  </td>
                  <td data-label="Valor" className="lc-num lc-valor">
                    {formatarMoeda(l.valor)}
                  </td>
                  <td className="lc-acoes" onClick={(e) => e.stopPropagation()}>
                    <RowMenu
                      label={`Ações de ${l.descricao}`}
                      items={[
                        {
                          label: 'Editar',
                          icon: <PencilIcon width={16} height={16} />,
                          onSelect: () => setFormulario({ aberto: true, lancamento: l }),
                        },
                        ...(l.comprovante
                          ? [
                              {
                                label: 'Ver comprovante',
                                icon: <PaperclipIcon width={16} height={16} />,
                                onSelect: () => setVerComprovante(l),
                              },
                            ]
                          : []),
                        {
                          label: 'Histórico',
                          icon: <HistoryIcon width={16} height={16} />,
                          onSelect: () => setVerHistorico(l),
                        },
                        {
                          label: 'Excluir',
                          tone: 'danger' as const,
                          icon: <TrashIcon width={16} height={16} />,
                          onSelect: () => setExcluir(l),
                        },
                      ]}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {resultado && resultado.totalPaginas > 1 && (
            <nav className="cl-pagination" aria-label="Páginas">
              <span>
                {inicio}–{fim} de {total.toLocaleString('pt-BR')}
              </span>
              <div className="cl-pagination__buttons">
                <button
                  type="button"
                  className="ui-btn ui-btn--secondary ui-btn--sm"
                  disabled={pagina === 0 || carregando}
                  onClick={() => atualizarParams({ pagina: pagina - 1 > 0 ? String(pagina - 1) : null })}
                >
                  <ChevronLeftIcon width={16} height={16} />
                  Anterior
                </button>
                <button
                  type="button"
                  className="ui-btn ui-btn--secondary ui-btn--sm"
                  disabled={pagina >= resultado.totalPaginas - 1 || carregando}
                  onClick={() => atualizarParams({ pagina: String(pagina + 1) })}
                >
                  Próxima
                  <ChevronRightIcon width={16} height={16} />
                </button>
              </div>
            </nav>
          )}
        </div>
      )}

      <LancamentoDialog
        open={formulario.aberto}
        lancamento={formulario.lancamento}
        clienteFixo={cliente ? { id: cliente.id, nome: cliente.nome } : undefined}
        onClose={() => setFormulario({ aberto: false, lancamento: null })}
        onSalvo={(_, mensagem) => {
          setFormulario({ aberto: false, lancamento: null })
          if (mensagem) notify(mensagem)
          recarregar()
        }}
        onConflito={() => {
          setFormulario({ aberto: false, lancamento: null })
          recarregar()
        }}
        onAviso={(mensagem) => notify(mensagem, 'error')}
      />
      <ComprovanteDialog lancamento={verComprovante} onClose={() => setVerComprovante(null)} />
      <HistoricoDialog lancamento={verHistorico} onClose={() => setVerHistorico(null)} />
      <ConfirmDialog
        open={excluir !== null}
        title="Excluir este lançamento?"
        confirmLabel="Excluir lançamento"
        tone="danger"
        busy={excluindo}
        onConfirm={() => void confirmarExclusao()}
        onClose={() => setExcluir(null)}
      >
        {excluir && (
          <>
            <p>
              <strong>{excluir.descricao}</strong> ({formatarMoeda(excluir.valor)} em {formatarDia(excluir.data)}) sai da
              lista e dos totais{excluir.origem === 'CLIENTE' ? ', inclusive no app do cliente' : ''}.
            </p>
            <p>O registro e o histórico continuam guardados no sistema.</p>
          </>
        )}
      </ConfirmDialog>
    </>
  )
}
