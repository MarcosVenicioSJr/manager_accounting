package br.com.manageraccounting.modules.perfil;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Confirmação por e-mail das trocas de senha e de e-mail feitas no Meu perfil.
 *
 * @param tokenTtl         validade do link de confirmação
 * @param intervaloMinimo  tempo mínimo entre dois pedidos do mesmo tipo
 * @param maxEnviosPorHora limite de pedidos do mesmo tipo numa janela de 1 hora
 */
@Validated
@ConfigurationProperties(prefix = "app.perfil.confirmacao")
public record PerfilProperties(
        @DefaultValue("30m") Duration tokenTtl,
        @DefaultValue("1m") Duration intervaloMinimo,
        @DefaultValue("5") @Min(1) int maxEnviosPorHora) {
}
