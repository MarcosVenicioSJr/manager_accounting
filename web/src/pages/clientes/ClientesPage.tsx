import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import {
  AlertIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  CloseIcon,
  PlusIcon,
  SearchIcon,
  UsersIcon,
} from '../../components/icons'
import { listarClientes } from '../../features/clientes/clienteService'
import { formatarDocumento } from '../../features/clientes/documento'
import { mascararTelefone, pluralizar } from '../../features/clientes/formatar'
import type { ClienteListaItem, Pagina, StatusFiltro } from '../../features/clientes/types'
import '../../features/clientes/clientes.css'
import { getErrorMessage } from '../../services/errors'

const STATUS: { valor: StatusFiltro; rotulo: string }[] = [
  { valor: 'ATIVOS', rotulo: 'Ativos' },
  { valor: 'INATIVOS', rotulo: 'Inativos' },
  { valor: 'TODOS', rotulo: 'Todos' },
]

const TAMANHO_PAGINA = 20
const ESPERA_BUSCA_MS = 300

function lerStatus(valor: string | null): StatusFiltro {
  return valor === 'INATIVOS' || valor === 'TODOS' ? valor : 'ATIVOS'
}

// Lista de clientes do escritório. Busca, filtro e página ficam na URL, então voltar do detalhe
// mantém a lista como estava e o link pode ser compartilhado com a mesma visão.
export function ClientesPage() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const busca = params.get('busca') ?? ''
  const status = lerStatus(params.get('status'))
  const pagina = Math.max(0, Number(params.get('pagina')) || 0)

  const [textoBusca, setTextoBusca] = useState(busca)
  // Se a URL mudar por fora (voltar/avançar do navegador), o campo acompanha
  const [buscaDaUrl, setBuscaDaUrl] = useState(busca)
  if (busca !== buscaDaUrl) {
    setBuscaDaUrl(busca)
    setTextoBusca(busca)
  }

  // Cada resposta guarda a "chave" (busca + filtro + página) que a pediu. Enquanto a chave atual não chega,
  // a lista anterior continua na tela, esmaecida.
  const [tentativa, setTentativa] = useState(0)
  const chave = `${busca}|${status}|${pagina}|${tentativa}`
  const [resposta, setResposta] = useState<{ chave: string; dados: Pagina<ClienteListaItem> } | null>(null)
  const [falha, setFalha] = useState<{ chave: string; mensagem: string } | null>(null)
  const resultado = resposta?.dados ?? null
  const erro = falha?.chave === chave ? falha.mensagem : null
  const carregando = resposta?.chave !== chave && !erro

  // Sempre parte da URL atual (forma funcional), para duas mudanças seguidas não se sobrescreverem
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

  // Espera a pessoa parar de digitar antes de buscar
  useEffect(() => {
    if (textoBusca.trim() === busca) return
    const timer = window.setTimeout(() => {
      atualizarParams({ busca: textoBusca.trim() || null, pagina: null })
    }, ESPERA_BUSCA_MS)
    return () => window.clearTimeout(timer)
  }, [textoBusca, busca, atualizarParams])

  // Cada mudança cancela a requisição anterior, para uma resposta antiga não sobrescrever a nova
  useEffect(() => {
    const controle = new AbortController()
    listarClientes({ busca, status, pagina, tamanho: TAMANHO_PAGINA }, controle.signal)
      .then((dados) => {
        // Página além do fim (ex.: depois de desativar o último cliente da página): volta para a última
        if (dados.itens.length === 0 && dados.pagina > 0 && dados.totalPaginas > 0) {
          atualizarParams({ pagina: dados.totalPaginas > 1 ? String(dados.totalPaginas - 1) : null })
          return
        }
        setResposta({ chave, dados })
      })
      .catch((error: unknown) => {
        if (controle.signal.aborted) return
        setFalha({ chave, mensagem: getErrorMessage(error, 'Não foi possível carregar os clientes.') })
      })
    return () => controle.abort()
  }, [chave, busca, status, pagina, atualizarParams])

  const itens = resultado?.itens ?? []
  const total = resultado?.total ?? 0
  const inicio = total === 0 ? 0 : pagina * TAMANHO_PAGINA + 1
  const fim = Math.min(total, (pagina + 1) * TAMANHO_PAGINA)
  const semNenhumCliente = !busca && status === 'ATIVOS' && total === 0

  return (
    <>
      <div className="page-head">
        <div>
          <h1 className="page-head__title">Clientes</h1>
          <p className="page-head__subtitle">
            {resultado && !carregando ? pluralizar(total, 'cliente encontrado', 'clientes encontrados') : ' '}
          </p>
        </div>
        <div className="page-head__actions">
          <Link to="/app/clientes/novo" className="ui-btn ui-btn--primary">
            <PlusIcon width={18} height={18} />
            Novo cliente
          </Link>
        </div>
      </div>

      <div className="cl-toolbar">
        <div className="cl-search">
          <label htmlFor="busca-clientes" className="sr-only">
            Buscar clientes
          </label>
          <SearchIcon className="cl-search__icon" />
          <input
            id="busca-clientes"
            type="search"
            className="cl-search__input"
            placeholder="Buscar por nome, CPF/CNPJ ou e-mail"
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

        <div className="cl-filter" role="group" aria-label="Situação">
          {STATUS.map((s) => (
            <button
              key={s.valor}
              type="button"
              className="cl-filter__option"
              aria-pressed={status === s.valor}
              onClick={() => atualizarParams({ status: s.valor === 'ATIVOS' ? null : s.valor, pagina: null })}
            >
              {s.rotulo}
            </button>
          ))}
        </div>
      </div>

      {erro ? (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">
            {erro}
            <div className="ui-alert__actions">
              <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={() => setTentativa((n) => n + 1)}>
                Tentar novamente
              </button>
            </div>
          </div>
        </div>
      ) : carregando && !resultado ? (
        <TabelaCarregando />
      ) : itens.length === 0 ? (
        <div className="panel cl-empty">
          <span className="cl-empty__icon" aria-hidden="true">
            <UsersIcon width={28} height={28} />
          </span>
          {semNenhumCliente ? (
            <>
              <h2>Nenhum cliente cadastrado ainda</h2>
              <p>Cadastre o primeiro cliente do escritório para organizar os dados e liberar o acesso ao app.</p>
              <Link to="/app/clientes/novo" className="ui-btn ui-btn--primary">
                <PlusIcon width={18} height={18} />
                Cadastrar cliente
              </Link>
            </>
          ) : busca ? (
            <>
              <h2>Nenhum cliente encontrado</h2>
              <p>
                Nada encontrado para “{busca}”
                {status !== 'TODOS' ? ` entre os ${status === 'ATIVOS' ? 'ativos' : 'inativos'}` : ''}.
              </p>
              <div className="cl-empty__actions">
                <button
                  type="button"
                  className="ui-btn ui-btn--secondary"
                  onClick={() => {
                    setTextoBusca('')
                    atualizarParams({ busca: null, pagina: null })
                  }}
                >
                  Limpar busca
                </button>
                {status !== 'TODOS' && (
                  <button
                    type="button"
                    className="ui-btn ui-btn--ghost"
                    onClick={() => atualizarParams({ status: 'TODOS', pagina: null })}
                  >
                    Buscar em todos
                  </button>
                )}
              </div>
            </>
          ) : (
            <>
              <h2>{status === 'INATIVOS' ? 'Nenhum cliente inativo' : 'Nenhum cliente ativo'}</h2>
              <p>
                {status === 'INATIVOS'
                  ? 'Os clientes desativados aparecem aqui.'
                  : 'Todos os clientes do escritório estão desativados.'}
              </p>
            </>
          )}
        </div>
      ) : (
        <div className="panel cl-table-wrap" aria-busy={carregando}>
          <table className="cl-table">
            <caption className="sr-only">Clientes do escritório</caption>
            <thead>
              <tr>
                <th scope="col">Cliente</th>
                <th scope="col">CPF/CNPJ</th>
                <th scope="col">Contato</th>
                <th scope="col">App</th>
                {status !== 'ATIVOS' && <th scope="col">Situação</th>}
              </tr>
            </thead>
            <tbody>
              {itens.map((c) => (
                <tr key={c.id} onClick={() => navigate(`/app/clientes/${c.id}`)} data-inativo={!c.ativo}>
                  <td data-label="Cliente">
                    <Link to={`/app/clientes/${c.id}`} className="cl-name" onClick={(e) => e.stopPropagation()}>
                      {c.nome}
                    </Link>
                    <span className="cl-sub">
                      {c.tipoPessoa === 'JURIDICA' ? 'Pessoa jurídica' : 'Pessoa física'}
                      {c.nomeFantasia ? ` · ${c.nomeFantasia}` : ''}
                    </span>
                  </td>
                  <td data-label="CPF/CNPJ">
                    <span className="cl-mono">{formatarDocumento(c.documento, c.tipoPessoa)}</span>
                  </td>
                  <td data-label="Contato">
                    {c.email || c.telefone ? (
                      <div>
                        {c.email && <span className="cl-contact">{c.email}</span>}
                        {c.telefone && <span className="cl-contact cl-sub">{mascararTelefone(c.telefone)}</span>}
                      </div>
                    ) : (
                      <span className="cl-sub">Sem contato</span>
                    )}
                  </td>
                  <td data-label="App">
                    {c.acessoAppAtivo ? (
                      <span className="badge badge--ok">Liberado</span>
                    ) : (
                      <span className="badge">Sem acesso</span>
                    )}
                  </td>
                  {status !== 'ATIVOS' && (
                    <td data-label="Situação">
                      {c.ativo ? <span className="badge badge--ok">Ativo</span> : <span className="badge badge--off">Inativo</span>}
                    </td>
                  )}
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
    </>
  )
}

function TabelaCarregando() {
  return (
    <div className="panel cl-loading" role="status" aria-label="Carregando clientes">
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} className="cl-loading__row">
          <span className="ui-skeleton" style={{ width: '32%' }} />
          <span className="ui-skeleton" style={{ width: '18%' }} />
          <span className="ui-skeleton" style={{ width: '22%' }} />
          <span className="ui-skeleton" style={{ width: '10%' }} />
        </div>
      ))}
    </div>
  )
}
