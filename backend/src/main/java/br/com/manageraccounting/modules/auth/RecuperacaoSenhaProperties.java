package br.com.manageraccounting.modules.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Configuração do "Esqueceu sua senha?".
 *
 * @param tokenTtl            validade do link enviado por e-mail
 * @param intervaloMinimo     tempo mínimo entre dois e-mails para o mesmo usuário
 * @param maxEnviosPorHora    limite de e-mails por usuário numa janela de 1 hora
 */
@Validated
@ConfigurationProperties(prefix = "app.security.recuperacao-senha")
public record RecuperacaoSenhaProperties(
        @DefaultValue("30m") Duration tokenTtl,
        @DefaultValue("2m") Duration intervaloMinimo,
        @DefaultValue("5") @Min(1) int maxEnviosPorHora) {
}
