import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PlusIcon, UsersIcon } from '../../components/icons'
import { useAuth } from '../../features/auth/useAuth'
import { buscarResumo } from '../../features/clientes/clienteService'
import type { ClientesResumo } from '../../features/clientes/types'
import { useTenantTheme } from '../../hooks/useTenantTheme'
import { PageHeader } from '../../layouts/PageHeader'
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

// Página inicial do backoffice: saudação e os números da carteira de clientes.
export function InicioPage() {
  const { usuario } = useAuth()
  const { theme } = useTenantTheme()
  const [resumo, setResumo] = useState<ClientesResumo | null>(null)
  const [erro, setErro] = useState(false)

  useEffect(() => {
    let cancelado = false
    buscarResumo()
      .then((dados) => !cancelado && setResumo(dados))
      .catch(() => !cancelado && setErro(true))
    return () => {
      cancelado = true
    }
  }, [])

  const primeiroNome = (usuario?.nomeCompleto || usuario?.login || '').split(/\s+/)[0]
  const vazio = resumo !== null && resumo.ativos + resumo.inativos === 0

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

      <section className="panel ini-carteira" aria-labelledby="ini-carteira-titulo">
        <div className="ini-carteira__head">
          <h2 id="ini-carteira-titulo">Carteira de clientes</h2>
          <Link to="/app/clientes" className="ini-link">
            Ver todos
          </Link>
        </div>

        {erro ? (
          <p className="ini-carteira__msg">Não foi possível carregar os números agora.</p>
        ) : vazio ? (
          <div className="ini-vazio">
            <span className="ini-vazio__icon" aria-hidden="true">
              <UsersIcon width={26} height={26} />
            </span>
            <div>
              <p className="ini-vazio__titulo">Nenhum cliente cadastrado ainda</p>
              <p className="ini-carteira__msg">
                Comece pelo cadastro dos clientes. Depois é só gerar o token para cada um usar o app.
              </p>
            </div>
          </div>
        ) : (
          <dl className="ini-numeros">
            <Numero rotulo="Clientes ativos" valor={resumo?.ativos} para="/app/clientes" />
            <Numero
              rotulo="Com acesso ao app"
              valor={resumo?.comAcessoApp}
              detalhe={resumo && resumo.ativos > 0 ? `${Math.round((resumo.comAcessoApp / resumo.ativos) * 100)}% dos ativos` : undefined}
            />
            <Numero rotulo="Inativos" valor={resumo?.inativos} para="/app/clientes?status=INATIVOS" />
          </dl>
        )}
      </section>
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
