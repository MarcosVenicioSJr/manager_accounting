package br.com.manageraccounting.modules.auth;

import br.com.manageraccounting.modules.auth.dto.AuthResponse;

/**
 * Resultado do login: corpo da resposta e o refresh token (que o controller coloca no cookie).
 */
public record LoginResult(AuthResponse response, String refreshToken) {
}
