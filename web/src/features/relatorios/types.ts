import type { TipoPessoa } from '../clientes/types'
import type { OrigemLancamento, TipoLancamento } from '../lancamentos/types'

// Respostas de GET /api/relatorios/extrato e /resumo (ver ExtratoClienteResponse e ResumoEscritorioResponse)

export type ExtratoCliente = {
  escritorio: string
  cliente: {
    id: number
    nome: string
    nomeFantasia: string | null
    tipoPessoa: TipoPessoa
    documento: string
    ativo: boolean
  }
  de: string
  ate: string
  geradoEm: string
  itens: {
    id: number
    data: string
    tipo: TipoLancamento
    descricao: string
    categoria: string
    categoriaRotulo: string
    valor: number
    origem: OrigemLancamento
    temComprovante: boolean
  }[]
  // Os três tipos, sempre na mesma ordem
  totaisPorTipo: { tipo: TipoLancamento; quantidade: number; valor: number }[]
  // Só as categorias com lançamento, do maior valor para o menor
  totaisPorCategoria: { categoria: string; rotulo: string; quantidade: number; valor: number }[]
  quantidade: number
  valorTotal: number
}

export type LinhaResumo = {
  clienteId: number | null
  nome: string | null
  tipoPessoa: TipoPessoa | null
  documento: string | null
  ativo: boolean | null
  quantidade: number
  compras: number
  dividas: number
  despesas: number
  total: number
}

export type ResumoEscritorio = {
  escritorio: string
  de: string
  ate: string
  geradoEm: string
  linhas: LinhaResumo[]
  totais: LinhaResumo
  clientesComMovimento: number
  clientesSemMovimento: number
}

export type Periodo = { de: string; ate: string }

// Identidade visual usada nos arquivos (vem do tema do escritório)
export type Marca = {
  escritorio: string
  logoUrl: string | null
  // Cor com contraste AA sobre branco (a mesma dos botões do backoffice), em #rrggbb
  cor: string
}
