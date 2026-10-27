import type { RegimeTributario, TipoPessoa } from '../clientes/types'
import type { OrigemLancamento, TipoLancamento, TotalTipo } from '../lancamentos/types'

// Resposta de GET /api/dashboard (ver DashboardResponse.java)

export type Carteira = {
  ativos: number
  inativos: number
  comAcessoApp: number
  semAcessoApp: number
  novosMesAtual: number
  novosMesAnterior: number
}

// mes no formato "2026-10"
export type CadastrosNoMes = { mes: string; quantidade: number }

// regime null = pessoa jurídica sem regime informado
export type RegimeQuantidade = { regime: RegimeTributario | null; quantidade: number }

export type Composicao = {
  pessoaFisica: number
  pessoaJuridica: number
  porRegime: RegimeQuantidade[]
}

export type ClienteResumo = {
  id: number
  nome: string
  nomeFantasia: string | null
  tipoPessoa: TipoPessoa
  documento: string
  ativo: boolean
  acessoAppAtivo: boolean
  dataInclusao: string
}

// mes no formato "2026-10"; porTipo traz sempre os três tipos
export type LancamentosMes = {
  mes: string
  porTipo: TotalTipo[]
  quantidade: number
  valorTotal: number
  valorTotalMesAnterior: number
}

export type LancamentoResumo = {
  id: number
  clienteId: number
  clienteNome: string
  tipo: TipoLancamento
  data: string
  descricao: string
  valor: number
  categoria: string
  origem: OrigemLancamento
  criadoEm: string
}

export type Dashboard = {
  carteira: Carteira
  novosPorMes: CadastrosNoMes[]
  composicao: Composicao
  semAcessoApp: { total: number; itens: ClienteResumo[] }
  recentes: ClienteResumo[]
  lancamentosMes: LancamentosMes
  ultimosLancamentos: LancamentoResumo[]
  // Só vem para administradores
  equipe: { usuariosAtivos: number; convitesPendentes: number } | null
}
