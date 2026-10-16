// Tema do tenant (escritório). O padrão é neutro, usado no login genérico.
export type TenantTheme = {
  tenantName: string
  logoUrl: string | null
  primaryColor: string
  secondaryColor: string
}

export const defaultTheme: TenantTheme = {
  tenantName: 'Manager Accounting',
  logoUrl: null,
  primaryColor: '#1f2937',
  secondaryColor: '#6b7280',
}
