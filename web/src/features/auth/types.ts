// Resposta de /auth/login e /auth/refresh. O refresh token não vem no corpo: está no cookie HttpOnly.
export type AuthResponse = {
  accessToken: string
  expiresIn: number
  primeiroAcessoPendente: boolean
}

export type PrimeiroAcessoDados = {
  novaSenha: string
  nomeCompleto: string
  email: string
  telefone: string
}
