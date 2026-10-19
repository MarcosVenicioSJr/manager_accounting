// Formatação para exibição (pt-BR).

export function mascararTelefone(valor: string): string {
  const d = valor.replace(/\D/g, '').slice(0, 11)
  if (d.length <= 2) return d.length ? `(${d}` : ''
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
}

const DATA = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatarData(iso: string | null): string {
  return iso ? DATA.format(new Date(iso)) : '—'
}

export function formatarDataHora(iso: string | null): string {
  return iso ? DATA_HORA.format(new Date(iso)).replace(',', ' às') : '—'
}

export function pluralizar(n: number, singular: string, plural: string) {
  return `${n.toLocaleString('pt-BR')} ${n === 1 ? singular : plural}`
}
