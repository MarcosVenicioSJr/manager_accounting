package br.com.manageraccounting.modules.auth;

import br.com.manageraccounting.core.mail.EmailMessage;

/**
 * Evento com um e-mail pronto para envio (recuperação de senha e Meu perfil). Só é enviado depois que a transação que o
 * publicou for confirmada (ver {@link RecuperacaoSenhaEmailListener}).
 */
public record EmailPendente(EmailMessage email) {
}
