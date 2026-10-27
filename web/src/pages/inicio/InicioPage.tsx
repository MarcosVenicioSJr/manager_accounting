import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { AlertIcon, ChevronRightIcon, KeyIcon, PlusIcon, ReceiptIcon, UsersIcon } from '../../components/icons'
import { useAuth } from '../../features/auth/useAuth'
import { formatarDocumento } from '../../features/clientes/documento'
import { formatarData, pluralizar } from '../../features/clientes/formatar'
import { REGIMES } from '../../features/clientes/types'
import { buscarDashboard } from '../../features/dashboard/dashboardService'
import { comparacaoMes, mesCurto, mesLongoComAno } from '../../features/dashboard/meses'
import type {
  CadastrosNoMes,
  ClienteResumo,
  Composicao,
  Dashboard,
  LancamentoResumo,
  LancamentosMes,
} from '../../features/dashboard/types'
import { formatarDia, formatarMoeda } from '../../features/lancamentos/formatar'
import { ROTULO_TIPO, TIPOS } from '../../features/lancamentos/types'
import '../../features/lancamentos/lancamentos.css'
import { useTenantTheme } from '../../hooks/useTenantTheme'
import { PageHeader } from '../../layouts/PageHeader'
import '../../features/clientes/clientes.css'
import './inicio.css'

function saudacao(agora = new Date()) {
  const hora = agora.getHours()
  if (hora < 12) return 'Bom dia'
  if (hora < 18) return 'Boa tarde'
  return 'Boa noite'
}

const HOJE = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })

// "segunda-feira, 19 de outubro"
function hojePorExtenso() {
  return HOJE.format(new Date())
}

function porcentagem(parte: number, total: number) {
  return total > 0 ? Math.round((parte / total) * 100) : 0
}

// Página inicial do backoffice: os números da carteira, novos cadastros, composição e o que pede atenção.
export function InicioPage() {
  const { usuario } = useAuth()
  const { theme } = useTenantTheme()
  const [dados, setDados] = useState<Dashboard | null>(null)
  const [erro, setErro] = useState(false)

  const carregar = useCallback((signal?: AbortSignal) => {
    buscarDashboard(signal)
      .then(setDados)
      .catch(() => {
        if (!signal?.aborted) setErro(true)
      })
  }, [])

  useEffect(() => {
    const controle = new AbortController()
    carregar(controle.signal)
    return () => controle.abort()
  }, [carregar])

  const primeiroNome = (usuario?.nomeCompleto || usuario?.login || '').split(/\s+/)[0]
  const carteira = dados?.carteira
  const vazio = carteira !== undefined && carteira.ativos + carteira.inativos === 0
  const serie = dados?.novosPorMes ?? []
  const mesAnterior = serie.length >= 2 ? serie[serie.length - 2].mes : null

  return (
    <>
      <PageHeader
        titulo="Visão geral"
        subtitulo={`${theme.tenantName} · resumo do escritório`}
        acoes={
          <Link to="/app/clientes/novo" className="ui-btn ui-btn--primary">
            <PlusIcon width={18} height={18} />
            Novo cliente
          </Link>
        }
      />

      <div className="page-intro">
        <h2 className="page-intro__title">
          {saudacao()}
          {primeiroNome ? `, ${primeiroNome}` : ''}
        </h2>
        <p className="page-intro__text">{hojePorExtenso()}</p>
      </div>

      {erro ? (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">
            Não foi possível carregar a visão geral agora.
            <div className="ui-alert__actions">
              <button type="button" className="ui-btn ui-btn--secondary ui-btn--sm" onClick={() => {
                  setErro(false)
                  carregar()
                }}>
                Tentar novamente
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="ini">
          <section className="panel ini-carteira" aria-labelledby="ini-carteira-titulo">
            <div className="ini-head">
              <h2 id="ini-carteira-titulo">Carteira de clientes</h2>
              <Link to="/app/clientes" className="ini-link">
                Ver todos
              </Link>
            </div>

            {vazio ? (
              <div className="ini-vazio">
                <span className="ini-vazio__icon" aria-hidden="true">
                  <UsersIcon width={26} height={26} />
                </span>
                <div>
                  <p className="ini-vazio__titulo">Nenhum cliente cadastrado ainda</p>
                  <p className="ini-msg">
                    Comece pelo cadastro dos clientes. Depois é só gerar o token para cada um usar o app.
                  </p>
                </div>
              </div>
            ) : (
              <dl className="ini-numeros">
                <Numero rotulo="Clientes ativos" valor={carteira?.ativos} para="/app/clientes" />
                <Numero
                  rotulo="Novos este mês"
                  valor={carteira?.novosMesAtual}
                  detalhe={
                    carteira && mesAnterior
                      ? comparacaoMes(carteira.novosMesAtual, carteira.novosMesAnterior, mesAnterior)
                      : undefined
                  }
                />
                <Numero
                  rotulo="Com acesso ao app"
                  valor={carteira?.comAcessoApp}
                  detalhe={
                    carteira && carteira.ativos > 0
                      ? `${porcentagem(carteira.comAcessoApp, carteira.ativos)}% dos ativos`
                      : undefined
                  }
                />
                <Numero rotulo="Inativos" valor={carteira?.inativos} para="/app/clientes?status=INATIVOS" />
              </dl>
            )}
          </section>

          {!vazio && (
            <>
              <div className="ini-grade">
                <PainelLancamentosMes dados={dados?.lancamentosMes ?? null} />
                <UltimosLancamentos itens={dados?.ultimosLancamentos ?? null} />
              </div>

              <div className="ini-grade">
                <GraficoNovos serie={dados ? serie : null} />
                <PainelComposicao composicao={dados?.composicao ?? null} />
              </div>

              <div className="ini-grade">
                <ListaClientes
                  id="ini-sem-app"
                  titulo="Sem acesso ao app"
                  descricao={
                    dados
                      ? dados.semAcessoApp.total > 0
                        ? `${pluralizar(dados.semAcessoApp.total, 'cliente ativo ainda não tem', 'clientes ativos ainda não têm')} token para usar o app.`
                        : 'Todos os clientes ativos já têm acesso ao app.'
                      : undefined
                  }
                  itens={dados?.semAcessoApp.itens ?? null}
                  vazio="Nada pendente por aqui."
                  extra={() => (
                    <span className="ini-acao">
                      <KeyIcon width={16} height={16} />
                      Gerar token
                    </span>
                  )}
                />
                <ListaClientes
                  id="ini-recentes"
                  titulo="Cadastrados recentemente"
                  itens={dados?.recentes ?? null}
                  vazio="Nenhum cliente cadastrado."
                  extra={(c) => (
                    <span className="ini-recente__meta">
                      {!c.ativo ? (
                        <span className="badge badge--off">Inativo</span>
                      ) : c.acessoAppAtivo ? (
                        <span className="badge badge--ok">App ativo</span>
                      ) : null}
                      <span className="ini-data">{formatarData(c.dataInclusao)}</span>
                    </span>
                  )}
                />
              </div>
            </>
          )}

          {dados?.equipe && (
            <section className="panel ini-equipe" aria-labelledby="ini-equipe-titulo">
              <span className="ini-vazio__icon" aria-hidden="true">
                <UsersIcon width={22} height={22} />
              </span>
              <div className="ini-equipe__texto">
                <h2 id="ini-equipe-titulo">Equipe do escritório</h2>
                <p className="ini-msg">
                  {pluralizar(dados.equipe.usuariosAtivos, 'usuário ativo', 'usuários ativos')}
                  {dados.equipe.convitesPendentes > 0 &&
                    ` · ${pluralizar(dados.equipe.convitesPendentes, 'convite aguardando aceite', 'convites aguardando aceite')}`}
                </p>
              </div>
              <Link to="/app/usuarios" className="ui-btn ui-btn--secondary ui-btn--sm">
                Gerenciar usuários
              </Link>
            </section>
          )}
        </div>
      )}
    </>
  )
}

function Numero({ rotulo, valor, detalhe, para }: { rotulo: string; valor?: number; detalhe?: string; para?: string }) {
  const numero =
    valor === undefined ? <span className="ui-skeleton" style={{ width: 56, height: 32 }} /> : valor.toLocaleString('pt-BR')
  return (
    <div className="ini-numero">
      <dt>{rotulo}</dt>
      <dd>
        {para && valor !== undefined ? (
          <Link to={para} className="ini-numero__valor">
            {numero}
          </Link>
        ) : (
          <span className="ini-numero__valor">{numero}</span>
        )}
        {detalhe && <span className="ini-numero__detalhe">{detalhe}</span>}
      </dd>
    </div>
  )
}

// Barras dos últimos 6 meses; o mês atual fica na cor do escritório.
function GraficoNovos({ serie }: { serie: CadastrosNoMes[] | null }) {
  const maximo = Math.max(1, ...(serie ?? []).map((s) => s.quantidade))
  const total = (serie ?? []).reduce((soma, s) => soma + s.quantidade, 0)
  return (
    <section className="panel ini-painel" aria-labelledby="ini-novos-titulo">
      <div className="ini-head">
        <h2 id="ini-novos-titulo">Novos clientes por mês</h2>
        {serie && <span className="ini-head__info">{pluralizar(total, 'cadastro', 'cadastros')} em 6 meses</span>}
      </div>
      {serie === null ? (
        <span className="ui-skeleton ini-grafico__skeleton" />
      ) : (
        <ol className="ini-grafico">
          {serie.map((s, i) => {
            const atual = i === serie.length - 1
            return (
              <li key={s.mes} className={atual ? 'ini-barra ini-barra--atual' : 'ini-barra'}>
                <span className="ini-barra__valor" aria-hidden="true">
                  {s.quantidade.toLocaleString('pt-BR')}
                </span>
                <span className="ini-barra__trilho" aria-hidden="true">
                  <span
                    className="ini-barra__fill"
                    style={{ height: s.quantidade === 0 ? 2 : `${(s.quantidade / maximo) * 100}%` }}
                  />
                </span>
                <span className="ini-barra__mes" aria-hidden="true">
                  {mesCurto(s.mes)}
                </span>
                <span className="sr-only">
                  {mesLongoComAno(s.mes)}: {pluralizar(s.quantidade, 'cliente cadastrado', 'clientes cadastrados')}
                </span>
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}

const ROTULO_REGIME = new Map(REGIMES.map((r) => [r.valor, r.rotulo]))

function PainelComposicao({ composicao }: { composicao: Composicao | null }) {
  const total = composicao ? composicao.pessoaFisica + composicao.pessoaJuridica : 0
  const maiorRegime = Math.max(1, ...(composicao?.porRegime ?? []).map((r) => r.quantidade))
  return (
    <section className="panel ini-painel" aria-labelledby="ini-composicao-titulo">
      <div className="ini-head">
        <h2 id="ini-composicao-titulo">Composição da carteira</h2>
        <span className="ini-head__info">clientes ativos</span>
      </div>
      {composicao === null ? (
        <span className="ui-skeleton ini-grafico__skeleton" />
      ) : (
        <>
          <div className="ini-tipos">
            <div className="ini-tipos__barra" aria-hidden="true">
              <span style={{ flexGrow: composicao.pessoaJuridica }} className="ini-tipos__pj" />
              <span style={{ flexGrow: composicao.pessoaFisica }} className="ini-tipos__pf" />
            </div>
            <dl className="ini-tipos__legenda">
              <div>
                <dt>
                  <span className="ini-ponto ini-ponto--pj" aria-hidden="true" />
                  Pessoa jurídica
                </dt>
                <dd>
                  {composicao.pessoaJuridica.toLocaleString('pt-BR')}
                  <span>{porcentagem(composicao.pessoaJuridica, total)}%</span>
                </dd>
              </div>
              <div>
                <dt>
                  <span className="ini-ponto ini-ponto--pf" aria-hidden="true" />
                  Pessoa física
                </dt>
                <dd>
                  {composicao.pessoaFisica.toLocaleString('pt-BR')}
                  <span>{porcentagem(composicao.pessoaFisica, total)}%</span>
                </dd>
              </div>
            </dl>
          </div>

          <h3 className="ini-subtitulo">Regime tributário (pessoa jurídica)</h3>
          {composicao.pessoaJuridica === 0 ? (
            <p className="ini-msg">Nenhuma pessoa jurídica ativa.</p>
          ) : (
            <ul className="ini-regimes">
              {composicao.porRegime.map((r) => (
                <li key={r.regime ?? 'nao-informado'} className="ini-regime">
                  <span className="ini-regime__nome">{r.regime ? ROTULO_REGIME.get(r.regime) : 'Não informado'}</span>
                  <span className="ini-regime__trilho" aria-hidden="true">
                    <span
                      className={r.regime ? 'ini-regime__fill' : 'ini-regime__fill ini-regime__fill--neutro'}
                      style={{ width: `${(r.quantidade / maiorRegime) * 100}%` }}
                    />
                  </span>
                  <span className="ini-regime__qtd">{r.quantidade.toLocaleString('pt-BR')}</span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}

// Totais do mês atual por tipo, com a comparação com o mês anterior; cada tipo leva à lista já filtrada.
function PainelLancamentosMes({ dados }: { dados: LancamentosMes | null }) {
  const maior = Math.max(1, ...(dados?.porTipo ?? []).map((t) => t.valor))
  const [ano, mes] = (dados?.mes ?? '2000-01').split('-').map(Number)
  const ultimoDia = new Date(ano, mes, 0).getDate()
  const periodo = dados ? `de=${dados.mes}-01&ate=${dados.mes}-${String(ultimoDia).padStart(2, '0')}` : ''
  const variacao =
    dados && dados.valorTotalMesAnterior > 0
      ? Math.round(((dados.valorTotal - dados.valorTotalMesAnterior) / dados.valorTotalMesAnterior) * 100)
      : null
  return (
    <section className="panel ini-painel" aria-labelledby="ini-lanc-mes-titulo">
      <div className="ini-head">
        <h2 id="ini-lanc-mes-titulo">Lançamentos de {dados ? mesLongoComAno(dados.mes) : '…'}</h2>
        <Link to={`/app/lancamentos?${periodo}`} className="ini-link">
          Ver lançamentos
        </Link>
      </div>
      {dados === null ? (
        <span className="ui-skeleton ini-grafico__skeleton" />
      ) : dados.quantidade === 0 ? (
        <div className="ini-vazio">
          <span className="ini-vazio__icon" aria-hidden="true">
            <ReceiptIcon width={24} height={24} />
          </span>
          <div>
            <p className="ini-vazio__titulo">Nenhum lançamento neste mês</p>
            <p className="ini-msg">
              {dados.valorTotalMesAnterior > 0
                ? `No mês passado foram ${formatarMoeda(dados.valorTotalMesAnterior)}.`
                : 'Os lançamentos do escritório e dos clientes pelo app aparecem aqui.'}
            </p>
          </div>
        </div>
      ) : (
        <>
          <div className="ini-lanc-total">
            <span className="ini-lanc-total__valor">{formatarMoeda(dados.valorTotal)}</span>
            <span className="ini-msg">
              {pluralizar(dados.quantidade, 'lançamento', 'lançamentos')}
              {variacao !== null &&
                ` · ${variacao === 0 ? 'igual ao' : `${Math.abs(variacao)}% ${variacao > 0 ? 'acima do' : 'abaixo do'}`} mês passado`}
            </span>
          </div>
          <ul className="ini-regimes">
            {TIPOS.map((t) => {
              const total = dados.porTipo.find((p) => p.tipo === t.valor)
              const valor = total?.valor ?? 0
              return (
                <li key={t.valor}>
                  <Link to={`/app/lancamentos?tipo=${t.valor}&${periodo}`} className="ini-regime ini-lanc-tipo">
                    <span className="ini-regime__nome">
                      <span className="lc-ponto" data-tipo={t.valor} aria-hidden="true" /> {t.plural}
                    </span>
                    <span className="ini-regime__trilho" aria-hidden="true">
                      <span className="ini-lanc-tipo__fill" data-tipo={t.valor} style={{ width: `${(valor / maior) * 100}%` }} />
                    </span>
                    <span className="ini-regime__qtd ini-lanc-tipo__valor">{formatarMoeda(valor)}</span>
                  </Link>
                </li>
              )
            })}
          </ul>
        </>
      )}
    </section>
  )
}

function UltimosLancamentos({ itens }: { itens: LancamentoResumo[] | null }) {
  return (
    <section className="panel ini-painel" aria-labelledby="ini-ultimos-lanc-titulo">
      <div className="ini-head">
        <h2 id="ini-ultimos-lanc-titulo">Últimos lançamentos</h2>
        <Link to="/app/lancamentos" className="ini-link">
          Ver todos
        </Link>
      </div>
      {itens === null ? (
        <div className="ini-lista__skeleton">
          {[0, 1, 2].map((i) => (
            <span key={i} className="ui-skeleton" style={{ height: 40 }} />
          ))}
        </div>
      ) : itens.length === 0 ? (
        <p className="ini-msg">Nenhum lançamento ainda.</p>
      ) : (
        <ul className="ini-lista">
          {itens.map((l) => (
            <li key={l.id}>
              <Link to={`/app/clientes/${l.clienteId}`} className="ini-item">
                <span className="ini-item__texto">
                  <span className="ini-item__nome">{l.descricao}</span>
                  <span className="ini-item__doc">
                    {l.clienteNome} · {ROTULO_TIPO[l.tipo]} · {formatarDia(l.data)}
                    {l.origem === 'CLIENTE' ? ' · pelo app' : ''}
                  </span>
                </span>
                <span className="ini-lanc-valor">{formatarMoeda(l.valor)}</span>
                <ChevronRightIcon width={16} height={16} className="ini-item__seta" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function ListaClientes({
  id,
  titulo,
  descricao,
  itens,
  vazio,
  extra,
}: {
  id: string
  titulo: string
  descricao?: string
  itens: ClienteResumo[] | null
  vazio: string
  extra: (c: ClienteResumo) => ReactNode
}) {
  return (
    <section className="panel ini-painel" aria-labelledby={`${id}-titulo`}>
      <div className="ini-head">
        <h2 id={`${id}-titulo`}>{titulo}</h2>
      </div>
      {descricao && <p className="ini-msg ini-descricao">{descricao}</p>}
      {itens === null ? (
        <div className="ini-lista__skeleton">
          {[0, 1, 2].map((i) => (
            <span key={i} className="ui-skeleton" style={{ height: 40 }} />
          ))}
        </div>
      ) : itens.length === 0 ? (
        <p className="ini-msg">{vazio}</p>
      ) : (
        <ul className="ini-lista">
          {itens.map((c) => (
            <li key={c.id}>
              <Link to={`/app/clientes/${c.id}`} className="ini-item">
                <span className="ini-item__texto">
                  <span className="ini-item__nome">{c.nomeFantasia || c.nome}</span>
                  <span className="ini-item__doc cl-mono">
                    {c.tipoPessoa === 'JURIDICA' ? 'CNPJ' : 'CPF'} {formatarDocumento(c.documento, c.tipoPessoa)}
                  </span>
                </span>
                {extra(c)}
                <ChevronRightIcon width={16} height={16} className="ini-item__seta" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
