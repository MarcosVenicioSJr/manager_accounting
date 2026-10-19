package br.com.manageraccounting.core.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Remetente dos e-mails do sistema. O servidor SMTP é configurado em spring.mail (Mailpit em desenvolvimento).
 *
 * @param from     endereço do remetente
 * @param fromName nome exibido do remetente
 */
@Validated
@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(
        @DefaultValue("no-reply@manageraccounting.local") @NotBlank String from,
        @DefaultValue("Backoffice contábil") String fromName) {
}
