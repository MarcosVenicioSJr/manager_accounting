package br.com.manageraccounting.modules.equipe;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Convite de usuários do escritório.
 *
 * @param validade        validade do link do convite
 * @param intervaloMinimo tempo mínimo entre dois envios para o mesmo usuário (reenviar)
 */
@Validated
@ConfigurationProperties(prefix = "app.equipe.convite")
public record EquipeProperties(
        @DefaultValue("48h") Duration validade,
        @DefaultValue("1m") Duration intervaloMinimo) {
}
