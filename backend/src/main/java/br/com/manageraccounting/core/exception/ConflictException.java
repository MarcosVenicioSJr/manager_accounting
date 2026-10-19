package br.com.manageraccounting.core.exception;

/**
 * Conflito com o estado atual do registro (duplicidade, edição simultânea). Vira HTTP 409, com {@code codigo}
 * no corpo para o front saber qual foi o conflito sem depender do texto da mensagem.
 */
public class ConflictException extends RuntimeException {

    private final String codigo;

    public ConflictException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
