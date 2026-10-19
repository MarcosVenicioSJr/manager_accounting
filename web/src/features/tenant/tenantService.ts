import { api } from '../../services/api'
import { defaultTheme, type TenantTheme } from '../../theme/theme'

type TemaDto = {
  nomeEmpresa: string
  logoUrl: string | null
  corPrimaria: string | null
  corSecundaria: string | null
}

// Busca o tema (nome, logo e cores) do tenant do usuário logado. Campos vazios usam o padrão neutro.
export async function buscarTema(): Promise<TenantTheme> {
  const { data } = await api.get<TemaDto>('/tenant/tema')
  return {
    tenantName: data.nomeEmpresa,
    logoUrl: data.logoUrl,
    primaryColor: data.corPrimaria ?? defaultTheme.primaryColor,
    secondaryColor: data.corSecundaria ?? defaultTheme.secondaryColor,
  }
}
