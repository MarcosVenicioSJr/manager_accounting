package br.com.manageraccounting.core.exception;

/**
 * Regra de negócio violada (ex.: senha fora da política). Vira HTTP 400 com a mensagem em {@code detail}.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
