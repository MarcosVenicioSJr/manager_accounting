import { mascararCep } from './cep'
import { mascararDocumento } from './documento'
import { mascararTelefone } from './formatar'
import type { Cliente, RegimeTributario, TipoPessoa } from './types'

// Valores do formulário de cliente (tudo texto, como aparece nos campos).
export type ClienteFormValores = {
  tipoPessoa: TipoPessoa
  documento: string
  nome: string
  nomeFantasia: string
  regimeTributario: RegimeTributario | ''
  email: string
  telefone: string
  observacoes: string
  cep: string
  logradouro: string
  numero: string
  complemento: string
  bairro: string
  cidade: string
  uf: string
}

export const VALORES_NOVO: ClienteFormValores = {
  tipoPessoa: 'JURIDICA',
  documento: '',
  nome: '',
  nomeFantasia: '',
  regimeTributario: '',
  email: '',
  telefone: '',
  observacoes: '',
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
}

export function valoresDoCliente(c: Cliente): ClienteFormValores {
  return {
    tipoPessoa: c.tipoPessoa,
    documento: mascararDocumento(c.documento, c.tipoPessoa),
    nome: c.nome,
    nomeFantasia: c.nomeFantasia ?? '',
    regimeTributario: c.regimeTributario ?? '',
    email: c.email ?? '',
    telefone: c.telefone ? mascararTelefone(c.telefone) : '',
    observacoes: c.observacoes ?? '',
    cep: c.endereco.cep ? mascararCep(c.endereco.cep) : '',
    logradouro: c.endereco.logradouro ?? '',
    numero: c.endereco.numero ?? '',
    complemento: c.endereco.complemento ?? '',
    bairro: c.endereco.bairro ?? '',
    cidade: c.endereco.cidade ?? '',
    uf: c.endereco.uf ?? '',
  }
}
