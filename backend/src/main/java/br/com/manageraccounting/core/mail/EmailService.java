package br.com.manageraccounting.core.mail;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envio de e-mails pelo SMTP configurado em spring.mail.
 * Falhas são registradas em log e não sobem para quem chamou: o envio acontece fora da requisição.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final AppMailProperties properties;

    public EmailService(JavaMailSender mailSender, AppMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /** @return true se o servidor SMTP aceitou a mensagem */
    public boolean enviar(EmailMessage email) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from(), properties.fromName());
            helper.setTo(email.para());
            helper.setSubject(email.assunto());
            helper.setText(email.texto(), email.html());
            mailSender.send(mime);
            return true;
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            // Não loga o destinatário nem o conteúdo (o e-mail de recuperação carrega o link com o token)
            log.error("Falha ao enviar e-mail \"{}\": {}", email.assunto(), ex.getMessage());
            return false;
        }
    }
}
