import { formatarDia } from '../lancamentos/formatar'
import type { Periodo } from './types'

// Baixa um arquivo gerado no navegador
export function salvarArquivo(conteudo: Blob, nome: string) {
  const url = URL.createObjectURL(conteudo)
  const link = document.createElement('a')
  link.href = url
  link.download = nome
  document.body.appendChild(link)
  link.click()
  link.remove()
  // Dá tempo para o navegador começar o download antes de liberar a memória
  window.setTimeout(() => URL.revokeObjectURL(url), 10_000)
}

// "Padaria Pão Quente LTDA" → "padaria-pao-quente-ltda"
export function slug(texto: string): string {
  return (
    texto
      .normalize('NFD')
      .replace(/\p{M}/gu, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 50) || 'relatorio'
  )
}

// Mês inteiro vira "2026-10"; outros períodos, "2026-10-05_a_2026-10-20"
export function periodoNoNome({ de, ate }: Periodo): string {
  const [ano, mes] = de.split('-').map(Number)
  const ultimo = new Date(ano, mes, 0).getDate()
  if (de.endsWith('-01') && ate === `${de.slice(0, 8)}${String(ultimo).padStart(2, '0')}`) return de.slice(0, 7)
  return `${de}_a_${ate}`
}

export function periodoPorExtenso({ de, ate }: Periodo): string {
  return de === ate ? formatarDia(de) : `${formatarDia(de)} a ${formatarDia(ate)}`
}

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function geradoEmTexto(iso: string): string {
  return DATA_HORA.format(new Date(iso)).replace(',', ' às')
}

// "2026-10-26" → Date à meia-noite UTC (o Excel guarda datas sem fuso; assim o dia não muda)
export function dataExcel(iso: string): Date {
  const [ano, mes, dia] = iso.split('-').map(Number)
  return new Date(Date.UTC(ano, mes - 1, dia))
}

// Logo do escritório como data URL, para ir dentro do PDF. Sem logo ou com erro, segue sem ela.
export async function carregarLogo(url: string | null): Promise<{ dados: string; formato: 'JPEG' | 'PNG'; largura: number; altura: number } | null> {
  if (!url) return null
  try {
    const resposta = await fetch(url)
    if (!resposta.ok) return null
    const blob = await resposta.blob()
    const formato = blob.type === 'image/png' ? 'PNG' : blob.type === 'image/jpeg' ? 'JPEG' : null
    if (!formato) return null
    const dados = await new Promise<string>((resolve, reject) => {
      const leitor = new FileReader()
      leitor.onload = () => resolve(leitor.result as string)
      leitor.onerror = () => reject(leitor.error)
      leitor.readAsDataURL(blob)
    })
    const { largura, altura } = await new Promise<{ largura: number; altura: number }>((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve({ largura: img.naturalWidth, altura: img.naturalHeight })
      img.onerror = reject
      img.src = dados
    })
    return { dados, formato, largura, altura }
  } catch {
    return null
  }
}

export function hexParaRgb(hex: string): [number, number, number] {
  const v = /^#[0-9a-f]{6}$/i.test(hex) ? hex.slice(1) : '1a6bdc'
  return [0, 2, 4].map((i) => parseInt(v.slice(i, i + 2), 16)) as [number, number, number]
}
