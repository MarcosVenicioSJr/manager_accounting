import { hojeIso, mesDe } from '../lancamentos/formatar'
import type { Periodo } from './types'

export type Atalho = 'mes' | 'passado' | 'ano' | 'outro'

export const DIAS_MAXIMOS = 366

// Atalhos de período a partir de hoje (no navegador)
export function periodoDoAtalho(atalho: Exclude<Atalho, 'outro'>, hoje = hojeIso()): Periodo {
  const [ano, mes] = hoje.split('-').map(Number)
  if (atalho === 'mes') return mesDe(hoje)
  if (atalho === 'passado') {
    const anterior = mes === 1 ? `${ano - 1}-12-01` : `${ano}-${String(mes - 1).padStart(2, '0')}-01`
    return mesDe(anterior)
  }
  return { de: `${ano}-01-01`, ate: hoje }
}

function dias(de: string, ate: string) {
  return Math.round((Date.parse(`${ate}T00:00:00Z`) - Date.parse(`${de}T00:00:00Z`)) / 86_400_000) + 1
}

// Mensagem de erro do período, ou null se estiver certo (as mesmas regras do backend)
export function erroDoPeriodo(p: Periodo): string | null {
  if (!p.de || !p.ate) return 'Informe a data inicial e a final.'
  if (p.de > p.ate) return 'A data inicial é depois da data final.'
  if (dias(p.de, p.ate) > DIAS_MAXIMOS) return 'O período pode ter no máximo um ano.'
  return null
}
