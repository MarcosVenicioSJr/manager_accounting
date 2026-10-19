const CONECTIVOS = new Set(['de', 'da', 'do', 'das', 'dos', 'e', '&'])

// Iniciais para avatar e monograma: primeira letra da primeira e da última palavra ("Maria da Silva" → "MS").
export function iniciais(nome: string): string {
  const palavras = nome
    .trim()
    .split(/\s+/)
    .filter((p) => p && !CONECTIVOS.has(p.toLowerCase()))
  if (palavras.length === 0) return '?'
  const primeira = palavras[0][0]
  const ultima = palavras.length > 1 ? palavras[palavras.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}
