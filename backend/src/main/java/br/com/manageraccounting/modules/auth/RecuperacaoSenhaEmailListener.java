package br.com.manageraccounting.modules.auth;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.manageraccounting.core.mail.EmailService;

/**
 * Envia os e-mails da recuperação de senha:
 * <ul>
 *   <li>só depois do commit: se a gravação do token falhar, nenhum link inválido é enviado;</li>
 *   <li>em segundo plano (@Async): a resposta da API não espera o SMTP, então o tempo de resposta é o mesmo
 *   para usuário existente ou não (não dá para descobrir logins pelo tempo).</li>
 * </ul>
 */
@Component
class RecuperacaoSenhaEmailListener {

    private final EmailService emailService;

    RecuperacaoSenhaEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(EmailPendente evento) {
        emailService.enviar(evento.email());
    }
}
