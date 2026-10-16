// O access token fica só em memória (não vai para localStorage), para não ser lido por script injetado.
// Ao recarregar a página ele é recuperado pelo refresh token, que está em cookie HttpOnly.
let accessToken: string | null = null

export const tokenStore = {
  get: (): string | null => accessToken,
  set: (token: string | null): void => {
    accessToken = token
  },
}
