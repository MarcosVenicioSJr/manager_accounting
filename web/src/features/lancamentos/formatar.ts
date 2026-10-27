// Formatação de valores e datas dos lançamentos (pt-BR).

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const DATA = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })

export function formatarMoeda(valor: number | string): string {
  return MOEDA.format(typeof valor === 'string' ? Number(valor) : valor)
}

// "2026-10-26" → "26/10/2026" sem passar por UTC (que mudaria o dia)
export function formatarDia(iso: string): string {
  const [ano, mes, dia] = iso.split('-').map(Number)
  return DATA.format(new Date(ano, mes - 1, dia))
}

// Data de hoje no navegador, no formato do campo <input type="date">
export function hojeIso(agora = new Date()): string {
  const m = String(agora.getMonth() + 1).padStart(2, '0')
  const d = String(agora.getDate()).padStart(2, '0')
  return `${agora.getFullYear()}-${m}-${d}`
}

// Primeiro e último dia do mês de uma data ISO
export function mesDe(iso: string): { de: string; ate: string } {
  const [ano, mes] = iso.split('-').map(Number)
  const ultimo = new Date(ano, mes, 0).getDate()
  const m = String(mes).padStart(2, '0')
  return { de: `${ano}-${m}-01`, ate: `${ano}-${m}-${String(ultimo).padStart(2, '0')}` }
}

// Digitação do valor em reais: só números, e os dois últimos viram centavos ("1234" → "12,34")
export function mascararValor(texto: string): string {
  const digitos = texto.replace(/\D/g, '').replace(/^0+/, '').slice(0, 14)
  if (!digitos) return ''
  const centavos = digitos.padStart(3, '0')
  const inteiro = centavos.slice(0, -2)
  return `${Number(inteiro).toLocaleString('pt-BR')},${centavos.slice(-2)}`
}

// "1.234,50" → "1234.50" (o formato que a API espera)
export function valorParaApi(mascarado: string): string {
  const digitos = mascarado.replace(/\D/g, '')
  if (!digitos) return ''
  const centavos = digitos.padStart(3, '0')
  return `${String(Number(centavos.slice(0, -2)))}.${centavos.slice(-2)}`
}

// 1500 → "1.500,00" (para preencher o campo na edição)
export function valorParaCampo(valor: number): string {
  return mascararValor(Math.round(valor * 100).toString())
}

export function formatarTamanho(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`
}
