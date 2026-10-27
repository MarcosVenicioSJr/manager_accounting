import { api } from '../../services/api'
import type { ExtratoCliente, Periodo, PorCategoria, ResumoEscritorio } from './types'

export async function buscarExtrato(clienteId: number, periodo: Periodo) {
  const { data } = await api.get<ExtratoCliente>('/relatorios/extrato', { params: { clienteId, ...periodo } })
  return data
}

export async function buscarResumo(periodo: Periodo) {
  const { data } = await api.get<ResumoEscritorio>('/relatorios/resumo', { params: periodo })
  return data
}

// Sem clienteId: todos os clientes do escritório
export async function buscarPorCategoria(clienteId: number | null, periodo: Periodo) {
  const { data } = await api.get<PorCategoria>('/relatorios/categorias', {
    params: { ...periodo, clienteId: clienteId ?? undefined },
  })
  return data
}
