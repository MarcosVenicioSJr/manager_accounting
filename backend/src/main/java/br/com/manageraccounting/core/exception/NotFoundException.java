package br.com.manageraccounting.core.exception;

/**
 * Registro não encontrado. Vira HTTP 404.
 * Também é usada quando o registro existe mas pertence a outro tenant, para não revelar que ele existe.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
