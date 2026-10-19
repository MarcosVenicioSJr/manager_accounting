package br.com.manageraccounting.modules.auth;

import br.com.manageraccounting.core.mail.EmailMessage;

/**
 * Evento com um e-mail da recuperação de senha pronto para envio. Só é enviado depois que a transação que o
 * publicou for confirmada (ver {@link RecuperacaoSenhaEmailListener}).
 */
record EmailPendente(EmailMessage email) {
}
