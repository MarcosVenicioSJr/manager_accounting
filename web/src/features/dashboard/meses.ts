// Rótulos de mês a partir de "2026-10" (sem passar por Date em UTC, que pode cair no mês anterior).

const CURTO = new Intl.DateTimeFormat('pt-BR', { month: 'short' })
const LONGO = new Intl.DateTimeFormat('pt-BR', { month: 'long' })
const LONGO_ANO = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })

function data(mes: string) {
  const [ano, m] = mes.split('-').map(Number)
  return new Date(ano, m - 1, 1)
}

// "out"
export function mesCurto(mes: string) {
  return CURTO.format(data(mes)).replace('.', '')
}

// "outubro"
export function mesLongo(mes: string) {
  return LONGO.format(data(mes))
}

// "outubro de 2026"
export function mesLongoComAno(mes: string) {
  return LONGO_ANO.format(data(mes))
}

// Texto que compara os novos clientes do mês com o mês anterior
export function comparacaoMes(atual: number, anterior: number, mesAnterior: string) {
  const nome = mesLongo(mesAnterior)
  const diferenca = atual - anterior
  if (diferenca === 0) return anterior === 0 ? `Nenhum em ${nome} também` : `O mesmo número de ${nome}`
  const qtd = Math.abs(diferenca).toLocaleString('pt-BR')
  return diferenca > 0 ? `${qtd} a mais que em ${nome}` : `${qtd} a menos que em ${nome}`
}
