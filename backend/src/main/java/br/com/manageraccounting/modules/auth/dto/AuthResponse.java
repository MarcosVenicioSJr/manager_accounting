package br.com.manageraccounting.modules.auth.dto;

/**
 * Resposta de login e refresh. O refresh token NÃO vai no corpo: segue em cookie HttpOnly.
 *
 * @param accessToken            JWT de acesso (enviar em Authorization: Bearer)
 * @param expiresIn              validade do access token, em segundos
 * @param primeiroAcessoPendente true se o usuário ainda precisa trocar a senha temporária e completar os dados
 */
public record AuthResponse(String accessToken, long expiresIn, boolean primeiroAcessoPendente) {
}
