import type { TipoPessoa } from './types'

// Mesmas regras do backend (DocumentoFiscal.java). O CNPJ aceita o formato alfanumérico da Receita
// (12 posições com letras ou números + 2 dígitos verificadores); o valor de cada caractere é o código ASCII menos 48.

export function limparDocumento(valor: string): string {
  return valor.replace(/[\s./-]/g, '').toUpperCase()
}

function todosIguais(valor: string) {
  return [...valor].every((c) => c === valor[0])
}

function dv(valores: number[], pesos: number[]) {
  const soma = valores.reduce((total, v, i) => total + v * pesos[i], 0)
  const resto = soma % 11
  return resto < 2 ? 0 : 11 - resto
}

export function cpfValido(valor: string): boolean {
  const cpf = limparDocumento(valor)
  if (!/^\d{11}$/.test(cpf) || todosIguais(cpf)) return false
  const n = [...cpf].map(Number)
  const d1 = dv(n.slice(0, 9), [10, 9, 8, 7, 6, 5, 4, 3, 2])
  const d2 = dv(n.slice(0, 10), [11, 10, 9, 8, 7, 6, 5, 4, 3, 2])
  return d1 === n[9] && d2 === n[10]
}

export function cnpjValido(valor: string): boolean {
  const cnpj = limparDocumento(valor)
  if (!/^[0-9A-Z]{12}\d{2}$/.test(cnpj) || todosIguais(cnpj)) return false
  const n = [...cnpj].map((c) => c.charCodeAt(0) - 48)
  const d1 = dv(n.slice(0, 12), [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])
  const d2 = dv(n.slice(0, 13), [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])
  return d1 === n[12] && d2 === n[13]
}

export function documentoValido(valor: string, tipo: TipoPessoa) {
  return tipo === 'FISICA' ? cpfValido(valor) : cnpjValido(valor)
}

// Máscara enquanto digita. CPF: 000.000.000-00 · CNPJ: AA.AAA.AAA/AAAA-00 (letras só nas 12 primeiras posições).
export function mascararDocumento(valor: string, tipo: TipoPessoa): string {
  if (tipo === 'FISICA') {
    const d = valor.replace(/\D/g, '').slice(0, 11)
    return d
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2')
  }
  const limpo = limparDocumento(valor).replace(/[^0-9A-Z]/g, '')
  const base = limpo.slice(0, 12)
  const digitos = limpo.slice(12).replace(/\D/g, '').slice(0, 2)
  const c = base + digitos
  let saida = c.slice(0, 2)
  if (c.length > 2) saida += '.' + c.slice(2, 5)
  if (c.length > 5) saida += '.' + c.slice(5, 8)
  if (c.length > 8) saida += '/' + c.slice(8, 12)
  if (c.length > 12) saida += '-' + c.slice(12, 14)
  return saida
}

export function formatarDocumento(documento: string, tipo: TipoPessoa): string {
  return mascararDocumento(documento, tipo)
}
