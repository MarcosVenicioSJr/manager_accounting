package br.com.manageraccounting.modules.perfil;

import br.com.manageraccounting.core.exception.BusinessException;

/**
 * Senha atual errada no Meu perfil (HTTP 400). Conta como tentativa errada de login, então a transação não
 * pode ser desfeita por ela (ver {@code noRollbackFor} no {@link PerfilService}).
 */
public class SenhaAtualIncorretaException extends BusinessException {

    public SenhaAtualIncorretaException(String message) {
        super(message);
    }
}
