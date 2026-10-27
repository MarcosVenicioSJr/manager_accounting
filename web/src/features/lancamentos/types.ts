// Tipos do módulo de lançamentos (ver LancamentoResponse.java e ListaLancamentosResponse.java)

export type TipoLancamento = 'COMPRA' | 'DIVIDA' | 'DESPESA'
export type OrigemLancamento = 'ESCRITORIO' | 'CLIENTE'
export type AcaoHistorico = 'CRIAR' | 'EDITAR' | 'EXCLUIR'

// A lista oficial de categorias vem de GET /lancamentos/categorias; aqui só o formato
export type Categoria = { valor: string; rotulo: string }

export const TIPOS: { valor: TipoLancamento; rotulo: string; plural: string }[] = [
  { valor: 'COMPRA', rotulo: 'Compra', plural: 'Compras' },
  { valor: 'DIVIDA', rotulo: 'Dívida', plural: 'Dívidas' },
  { valor: 'DESPESA', rotulo: 'Despesa', plural: 'Despesas' },
]

export const ROTULO_TIPO: Record<TipoLancamento, string> = { COMPRA: 'Compra', DIVIDA: 'Dívida', DESPESA: 'Despesa' }

export type Comprovante = {
  nome: string
  tipo: string
  tamanho: number
}

export type Lancamento = {
  id: number
  clienteId: number
  clienteNome: string
  tipo: TipoLancamento
  // "2026-10-26" (só a data, sem fuso)
  data: string
  descricao: string
  valor: number
  categoria: string
  origem: OrigemLancamento
  comprovante: Comprovante | null
  criadoEm: string
  // Nome de quem lançou no escritório; null quando foi o cliente pelo app
  criadoPor: string | null
  // Preenchido quando o lançamento mudou depois de criado
  editadoEm: string | null
  versao: number
}

export type TotalTipo = { tipo: TipoLancamento; quantidade: number; valor: number }

export type ListaLancamentos = {
  itens: Lancamento[]
  pagina: number
  tamanho: number
  total: number
  totalPaginas: number
  // Os três tipos, sempre nesta ordem: compra, dívida, despesa
  totais: TotalTipo[]
  valorTotal: number
}

export type FiltroLancamentos = {
  clienteId?: number
  tipo?: TipoLancamento
  categoria?: string
  de?: string
  ate?: string
  busca?: string
}

// Corpo do POST/PUT. clienteId só no cadastro; versao só na edição.
export type LancamentoDados = {
  clienteId?: number
  tipo: TipoLancamento
  data: string
  descricao: string
  valor: string
  categoria: string
  versao?: number
}

export type SnapshotLancamento = {
  tipo: TipoLancamento
  data: string
  descricao: string
  valor: string
  categoria: string
  comprovante: string | null
}

export type ItemHistorico = {
  id: number
  acao: AcaoHistorico
  origem: OrigemLancamento
  usuarioNome: string | null
  feitoEm: string
  dados: SnapshotLancamento
}
