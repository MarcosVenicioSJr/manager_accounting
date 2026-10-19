// Política de senha (a mesma do backend): mínimo de 8 caracteres, com letras, números e símbolos.
// O BCrypt só considera 72 bytes, por isso há também um limite máximo.
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_BYTES = 72

export type PasswordChecks = {
  tamanho: boolean
  letra: boolean
  numero: boolean
  simbolo: boolean
  limite: boolean
}

export function checarSenha(senha: string): PasswordChecks {
  return {
    tamanho: senha.length >= PASSWORD_MIN_LENGTH,
    letra: /\p{L}/u.test(senha),
    numero: /\p{Nd}/u.test(senha),
    simbolo: /[^\p{L}\p{Nd}\s]/u.test(senha),
    limite: new TextEncoder().encode(senha).length <= PASSWORD_MAX_BYTES,
  }
}

export function senhaValida(senha: string): boolean {
  return Object.values(checarSenha(senha)).every(Boolean)
}
