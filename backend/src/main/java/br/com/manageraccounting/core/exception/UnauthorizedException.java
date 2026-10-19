package br.com.manageraccounting.core.exception;

/**
 * Falha de autenticação (login inválido, sessão expirada). Vira HTTP 401.
 * A mensagem é genérica de propósito, para não revelar se um login existe.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
