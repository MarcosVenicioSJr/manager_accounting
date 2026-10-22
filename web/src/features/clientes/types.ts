export type TipoPessoa = 'FISICA' | 'JURIDICA'
export type RegimeTributario = 'MEI' | 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL'
export type StatusFiltro = 'ATIVOS' | 'INATIVOS' | 'TODOS'

export const REGIMES: { valor: RegimeTributario; rotulo: string }[] = [
  { valor: 'MEI', rotulo: 'MEI' },
  { valor: 'SIMPLES_NACIONAL', rotulo: 'Simples Nacional' },
  { valor: 'LUCRO_PRESUMIDO', rotulo: 'Lucro Presumido' },
  { valor: 'LUCRO_REAL', rotulo: 'Lucro Real' },
]

// Endereço opcional do cliente (CEP só com dígitos)
export type Endereco = {
  cep: string | null
  logradouro: string | null
  numero: string | null
  complemento: string | null
  bairro: string | null
  cidade: string | null
  uf: string | null
}

export type AcessoApp = {
  ativo: boolean
  geradoEm: string | null
  ultimoUsoEm: string | null
}

export type Cliente = {
  id: number
  tipoPessoa: TipoPessoa
  documento: string
  nome: string
  nomeFantasia: string | null
  email: string | null
  telefone: string | null
  regimeTributario: RegimeTributario | null
  observacoes: string | null
  endereco: Endereco
  ativo: boolean
  dataInclusao: string
  dataAtualizacao: string
  versao: number
  acessoApp: AcessoApp
}

export type ClienteListaItem = {
  id: number
  tipoPessoa: TipoPessoa
  documento: string
  nome: string
  nomeFantasia: string | null
  email: string | null
  telefone: string | null
  cidade: string | null
  uf: string | null
  ativo: boolean
  acessoAppAtivo: boolean
}

export type Pagina<T> = {
  itens: T[]
  pagina: number
  tamanho: number
  total: number
  totalPaginas: number
}

export type ClientesResumo = {
  ativos: number
  inativos: number
  comAcessoApp: number
}

// Corpo do POST/PUT. Na edição, versao é a que a tela carregou.
export type ClienteDados = {
  tipoPessoa: TipoPessoa
  documento: string
  nome: string
  nomeFantasia: string | null
  email: string | null
  telefone: string | null
  regimeTributario: RegimeTributario | null
  observacoes: string | null
  endereco: Endereco
  versao?: number
}

export type TokenGerado = {
  token: string
  geradoEm: string
}
