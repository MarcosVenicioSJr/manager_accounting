import { api } from '../../services/api'
import type { Dashboard } from './types'

export async function buscarDashboard(signal?: AbortSignal) {
  const { data } = await api.get<Dashboard>('/dashboard', { signal })
  return data
}
