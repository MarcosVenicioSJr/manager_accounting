import { api } from '../../services/api'
import type {
  Categoria,
  FiltroLancamentos,
  ItemHistorico,
  Lancamento,
  LancamentoDados,
  ListaLancamentos,
} from './types'

let categoriasEmCache: Promise<Categoria[]> | null = null

// A lista é fixa no sistema: busca uma vez e reaproveita até recarregar a página
export function listarCategorias(): Promise<Categoria[]> {
  if (!categoriasEmCache) {
    categoriasEmCache = api
      .get<Categoria[]>('/lancamentos/categorias')
      .then((r) => r.data)
      .catch((error: unknown) => {
        categoriasEmCache = null
        throw error
      })
  }
  return categoriasEmCache
}

export async function listarLancamentos(
  filtro: FiltroLancamentos,
  pagina: number,
  tamanho: number,
  signal?: AbortSignal,
) {
  const params = Object.fromEntries(
    Object.entries({ ...filtro, pagina, tamanho }).filter(([, v]) => v !== undefined && v !== ''),
  )
  const { data } = await api.get<ListaLancamentos>('/lancamentos', { params, signal })
  return data
}

export async function criarLancamento(dados: LancamentoDados) {
  const { data } = await api.post<Lancamento>('/lancamentos', dados)
  return data
}

export async function atualizarLancamento(id: number, dados: LancamentoDados) {
  const { data } = await api.put<Lancamento>(`/lancamentos/${id}`, dados)
  return data
}

export async function excluirLancamento(id: number, versao: number) {
  await api.delete(`/lancamentos/${id}`, { params: { versao } })
}

export async function buscarHistorico(id: number, signal?: AbortSignal) {
  const { data } = await api.get<ItemHistorico[]>(`/lancamentos/${id}/historico`, { signal })
  return data
}

export async function enviarComprovante(id: number, versao: number, arquivo: File) {
  const corpo = new FormData()
  corpo.append('arquivo', arquivo)
  const { data } = await api.post<Lancamento>(`/lancamentos/${id}/comprovante`, corpo, { params: { versao } })
  return data
}

export async function removerComprovante(id: number, versao: number) {
  const { data } = await api.delete<Lancamento>(`/lancamentos/${id}/comprovante`, { params: { versao } })
  return data
}

// O arquivo vem com o token (não dá para abrir o link direto no navegador): baixa como blob
export async function baixarComprovante(id: number, signal?: AbortSignal) {
  const { data } = await api.get<Blob>(`/lancamentos/${id}/comprovante`, { responseType: 'blob', signal })
  return data
}
