import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AlertIcon, DownloadIcon, ReceiptIcon, ReportIcon, SpinnerIcon, UsersIcon } from '../../components/icons'
import { useToast } from '../../components/ui/useToast'
import { buscarCliente } from '../../features/clientes/clienteService'
import '../../features/clientes/clientes.css'
import { ClienteSeletor, type ClienteEscolhido } from '../../features/lancamentos/ClienteSeletor'
import '../../features/lancamentos/lancamentos.css'
import { periodoNoNome, salvarArquivo, slug } from '../../features/relatorios/arquivo'
import { erroDoPeriodo, periodoDoAtalho, type Atalho } from '../../features/relatorios/periodo'
import { PeriodoSeletor } from '../../features/relatorios/PeriodoSeletor'
import { buscarExtrato, buscarPorCategoria, buscarResumo } from '../../features/relatorios/relatorioService'
import '../../features/relatorios/relatorios.css'
import type { Marca, Periodo } from '../../features/relatorios/types'
import { useTenantTheme } from '../../hooks/useTenantTheme'
import { PageHeader } from '../../layouts/PageHeader'
import { getErrorMessage } from '../../services/errors'
import { themeCssVars } from '../../theme/theme'

type Formato = 'pdf' | 'xlsx'

// Relatórios do escritório. Os dados vêm da API; o PDF e o Excel são montados aqui no navegador
// (as bibliotecas só carregam quando a pessoa pede o arquivo).
export function RelatoriosPage() {
  const { theme } = useTenantTheme()
  const marca: Marca = {
    escritorio: theme.tenantName,
    logoUrl: theme.logoUrl,
    cor: String((themeCssVars(theme) as Record<string, string>)['--color-primary-strong']),
  }

  return (
    <>
      <PageHeader titulo="Relatórios" subtitulo="Extrato, resumo do escritório e gastos por categoria, em PDF ou Excel" />
      <div className="rl-grade">
        <CartaoExtrato marca={marca} />
        <CartaoResumo marca={marca} />
        <CartaoCategorias marca={marca} />
      </div>
    </>
  )
}

function useGeracao() {
  const { notify } = useToast()
  const [gerando, setGerando] = useState<Formato | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  async function gerar(formato: Formato, tarefa: (formato: Formato) => Promise<{ blob: Blob; nome: string; vazio: boolean }>) {
    setGerando(formato)
    setErro(null)
    try {
      const { blob, nome, vazio } = await tarefa(formato)
      salvarArquivo(blob, nome)
      notify(vazio ? 'Arquivo gerado. Não há lançamentos no período.' : 'Arquivo gerado.')
    } catch (error) {
      setErro(getErrorMessage(error, 'Não foi possível gerar o relatório.'))
    } finally {
      setGerando(null)
    }
  }
  return { gerando, erro, gerar }
}

function Botoes({ gerando, desabilitado, onGerar }: { gerando: Formato | null; desabilitado: boolean; onGerar: (f: Formato) => void }) {
  return (
    <div className="rl-botoes">
      {(['pdf', 'xlsx'] as const).map((f) => (
        <button
          key={f}
          type="button"
          className={`ui-btn ${f === 'pdf' ? 'ui-btn--primary' : 'ui-btn--secondary'}`}
          disabled={desabilitado || gerando !== null}
          aria-busy={gerando === f}
          onClick={() => onGerar(f)}
        >
          {gerando === f ? <SpinnerIcon width={18} height={18} /> : <DownloadIcon width={18} height={18} />}
          {gerando === f ? 'Gerando…' : f === 'pdf' ? 'Baixar PDF' : 'Baixar Excel'}
        </button>
      ))}
    </div>
  )
}

function CartaoExtrato({ marca }: { marca: Marca }) {
  const [params] = useSearchParams()
  const idInicial = Number(params.get('cliente')) || null
  const [cliente, setCliente] = useState<ClienteEscolhido | null>(null)
  const [carregandoCliente, setCarregandoCliente] = useState(idInicial !== null)
  const [atalho, setAtalho] = useState<Atalho>('mes')
  const [periodo, setPeriodo] = useState<Periodo>(() => periodoDoAtalho('mes'))
  const [tentou, setTentou] = useState(false)
  const { gerando, erro, gerar } = useGeracao()

  // Vindo da ficha do cliente (?cliente=id), já começa com ele escolhido
  useEffect(() => {
    if (idInicial === null) return
    const controle = new AbortController()
    buscarCliente(idInicial, controle.signal)
      .then((c) => setCliente({ id: c.id, nome: c.nome }))
      .catch(() => undefined)
      .finally(() => {
        if (!controle.signal.aborted) setCarregandoCliente(false)
      })
    return () => controle.abort()
  }, [idInicial])

  const erroPeriodo = erroDoPeriodo(periodo)
  const erroCliente = tentou && !cliente ? 'Escolha o cliente.' : null

  return (
    <section className="panel rl-cartao" aria-labelledby="rl-extrato-titulo">
      <div className="rl-cartao__topo">
        <span className="rl-cartao__icone" aria-hidden="true">
          <ReceiptIcon width={22} height={22} />
        </span>
        <div>
          <h2 id="rl-extrato-titulo">Extrato do cliente</h2>
          <p>Todos os lançamentos de um cliente no período, com totais por tipo e por categoria.</p>
        </div>
      </div>

      <div className="cf-field" data-invalid={Boolean(erroCliente)}>
        <label className="cf-label" htmlFor="rl-cliente">
          Cliente
        </label>
        {carregandoCliente ? (
          <span className="ui-skeleton" style={{ height: 42 }} />
        ) : (
          <ClienteSeletor
            id="rl-cliente"
            valor={cliente}
            onChange={setCliente}
            status="TODOS"
            placeholder="Nome ou CPF/CNPJ"
            invalido={Boolean(erroCliente)}
          />
        )}
        {erroCliente && <p className="cf-error">{erroCliente}</p>}
      </div>

      <PeriodoSeletor
        atalho={atalho}
        periodo={periodo}
        erro={erroPeriodo && (tentou || atalho === 'outro') ? erroPeriodo : null}
        onChange={(a, p) => {
          setAtalho(a)
          setPeriodo(p)
        }}
      />

      {erro && (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">{erro}</div>
        </div>
      )}

      <Botoes
        gerando={gerando}
        desabilitado={false}
        onGerar={(formato) => {
          setTentou(true)
          if (!cliente || erroPeriodo) return
          void gerar(formato, async (f) => {
            const dados = await buscarExtrato(cliente.id, periodo)
            const blob =
              f === 'pdf'
                ? await (await import('../../features/relatorios/gerarPdf')).pdfExtrato(dados, marca)
                : await (await import('../../features/relatorios/gerarExcel')).excelExtrato(dados, marca)
            return { blob, nome: `extrato-${slug(dados.cliente.nome)}-${periodoNoNome(periodo)}.${f}`, vazio: dados.quantidade === 0 }
          })
        }}
      />
    </section>
  )
}

function CartaoResumo({ marca }: { marca: Marca }) {
  const [atalho, setAtalho] = useState<Atalho>('mes')
  const [periodo, setPeriodo] = useState<Periodo>(() => periodoDoAtalho('mes'))
  const { gerando, erro, gerar } = useGeracao()
  const erroPeriodo = erroDoPeriodo(periodo)

  return (
    <section className="panel rl-cartao" aria-labelledby="rl-resumo-titulo">
      <div className="rl-cartao__topo">
        <span className="rl-cartao__icone" aria-hidden="true">
          <UsersIcon width={22} height={22} />
        </span>
        <div>
          <h2 id="rl-resumo-titulo">Resumo do escritório</h2>
          <p>Uma linha por cliente com compras, dívidas, despesas e total no período, do maior para o menor.</p>
        </div>
      </div>

      <PeriodoSeletor
        atalho={atalho}
        periodo={periodo}
        erro={atalho === 'outro' ? erroPeriodo : null}
        onChange={(a, p) => {
          setAtalho(a)
          setPeriodo(p)
        }}
      />

      {erro && (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">{erro}</div>
        </div>
      )}

      <Botoes
        gerando={gerando}
        desabilitado={Boolean(erroPeriodo)}
        onGerar={(formato) =>
          void gerar(formato, async (f) => {
            const dados = await buscarResumo(periodo)
            const blob =
              f === 'pdf'
                ? await (await import('../../features/relatorios/gerarPdf')).pdfResumo(dados, marca)
                : await (await import('../../features/relatorios/gerarExcel')).excelResumo(dados, marca)
            return { blob, nome: `resumo-${slug(marca.escritorio)}-${periodoNoNome(periodo)}.${f}`, vazio: dados.totais.quantidade === 0 }
          })
        }
      />
    </section>
  )
}

function CartaoCategorias({ marca }: { marca: Marca }) {
  const [cliente, setCliente] = useState<ClienteEscolhido | null>(null)
  const [atalho, setAtalho] = useState<Atalho>('mes')
  const [periodo, setPeriodo] = useState<Periodo>(() => periodoDoAtalho('mes'))
  const { gerando, erro, gerar } = useGeracao()
  const erroPeriodo = erroDoPeriodo(periodo)

  return (
    <section className="panel rl-cartao" aria-labelledby="rl-categorias-titulo">
      <div className="rl-cartao__topo">
        <span className="rl-cartao__icone" aria-hidden="true">
          <ReportIcon width={22} height={22} />
        </span>
        <div>
          <h2 id="rl-categorias-titulo">Gastos por categoria</h2>
          <p>Quanto foi gasto em cada categoria no período, com compras, dívidas, despesas e a fatia de cada uma.</p>
        </div>
      </div>

      <div className="cf-field">
        <label className="cf-label" htmlFor="rl-categorias-cliente">
          Cliente <span className="cf-required">(opcional)</span>
        </label>
        <ClienteSeletor
          id="rl-categorias-cliente"
          valor={cliente}
          onChange={setCliente}
          status="TODOS"
          placeholder="Todos os clientes, ou busque um"
        />
        <p className="cf-hint">{cliente ? 'Só os lançamentos deste cliente.' : 'Sem cliente escolhido, soma todos os clientes do escritório.'}</p>
      </div>

      <PeriodoSeletor
        atalho={atalho}
        periodo={periodo}
        erro={atalho === 'outro' ? erroPeriodo : null}
        onChange={(a, p) => {
          setAtalho(a)
          setPeriodo(p)
        }}
      />

      {erro && (
        <div className="ui-alert ui-alert--error" role="alert">
          <AlertIcon />
          <div className="ui-alert__content">{erro}</div>
        </div>
      )}

      <Botoes
        gerando={gerando}
        desabilitado={Boolean(erroPeriodo)}
        onGerar={(formato) =>
          void gerar(formato, async (f) => {
            const dados = await buscarPorCategoria(cliente?.id ?? null, periodo)
            const blob =
              f === 'pdf'
                ? await (await import('../../features/relatorios/gerarPdf')).pdfPorCategoria(dados, marca)
                : await (await import('../../features/relatorios/gerarExcel')).excelPorCategoria(dados, marca)
            const quem = dados.cliente ? slug(dados.cliente.nome) : slug(marca.escritorio)
            return { blob, nome: `categorias-${quem}-${periodoNoNome(periodo)}.${f}`, vazio: dados.totais.quantidade === 0 }
          })
        }
      />
    </section>
  )
}
