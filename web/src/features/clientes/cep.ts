// Busca de endereço pelo CEP no ViaCEP (serviço público e gratuito).
// Usa fetch direto, sem o cliente da API: nenhum token ou cookie do backoffice vai para o site externo.

export type EnderecoCep = {
  logradouro: string
  bairro: string
  cidade: string
  uf: string
}

export type ResultadoCep = { tipo: 'ok'; endereco: EnderecoCep } | { tipo: 'nao-encontrado' } | { tipo: 'falha' }

export function mascararCep(valor: string): string {
  const d = valor.replace(/\D/g, '').slice(0, 8)
  return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d
}

export async function buscarCep(cep: string, signal?: AbortSignal): Promise<ResultadoCep> {
  const digitos = cep.replace(/\D/g, '')
  if (digitos.length !== 8) return { tipo: 'nao-encontrado' }
  try {
    const resposta = await fetch(`https://viacep.com.br/ws/${digitos}/json/`, {
      signal,
      credentials: 'omit',
      referrerPolicy: 'no-referrer',
    })
    if (!resposta.ok) return resposta.status === 400 ? { tipo: 'nao-encontrado' } : { tipo: 'falha' }
    const dados = (await resposta.json()) as Record<string, unknown>
    if (dados.erro) return { tipo: 'nao-encontrado' }
    const texto = (v: unknown) => (typeof v === 'string' ? v : '')
    return {
      tipo: 'ok',
      endereco: {
        logradouro: texto(dados.logradouro),
        bairro: texto(dados.bairro),
        cidade: texto(dados.localidade),
        uf: texto(dados.uf).toUpperCase(),
      },
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    return { tipo: 'falha' }
  }
}

export const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
] as const
