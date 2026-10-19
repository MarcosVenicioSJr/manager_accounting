package br.com.manageraccounting.core.security;

/**
 * Usuário autenticado da requisição atual (principal do Spring Security).
 * Os dois valores vêm do token JWT validado no servidor.
 */
public record AuthenticatedUser(Long userId, Long tenantId) {
}
