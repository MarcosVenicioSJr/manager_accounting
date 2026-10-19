import { api } from '../../services/api'
import type {
  Cliente,
  ClienteDados,
  ClienteListaItem,
  ClientesResumo,
  Pagina,
  StatusFiltro,
  TokenGerado,
} from './types'

export type ListarClientesParams = {
  busca?: string
  status: StatusFiltro
  pagina: number
  tamanho?: number
}

export async function listarClientes(params: ListarClientesParams, signal?: AbortSignal) {
  const { data } = await api.get<Pagina<ClienteListaItem>>('/clientes', {
    params: { ...params, busca: params.busca || undefined },
    signal,
  })
  return data
}

export async function buscarResumo() {
  const { data } = await api.get<ClientesResumo>('/clientes/resumo')
  return data
}

export async function buscarCliente(id: number | string, signal?: AbortSignal) {
  const { data } = await api.get<Cliente>(`/clientes/${id}`, { signal })
  return data
}

export async function criarCliente(dados: ClienteDados) {
  const { data } = await api.post<Cliente>('/clientes', dados)
  return data
}

export async function atualizarCliente(id: number, dados: ClienteDados) {
  const { data } = await api.put<Cliente>(`/clientes/${id}`, dados)
  return data
}

export async function desativarCliente(id: number) {
  await api.post(`/clientes/${id}/desativar`)
}

export async function reativarCliente(id: number) {
  await api.post(`/clientes/${id}/reativar`)
}

// O token só vem nesta resposta; depois disso não dá para consultá-lo de novo.
export async function gerarAcessoApp(id: number) {
  const { data } = await api.post<TokenGerado>(`/clientes/${id}/acesso-app`)
  return data
}

export async function revogarAcessoApp(id: number) {
  await api.delete(`/clientes/${id}/acesso-app`)
}
